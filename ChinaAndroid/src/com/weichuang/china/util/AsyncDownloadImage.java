package com.weichuang.china.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Message;

public class AsyncDownloadImage {
	private static final String TAG = "AsyncDownloadImage";
	// 为了加快速度，在内存中开启缓存（主要应用于重复图片较多时，或者同一个图片要多次被访问，比如在ListView时来回滚动）
	public Map<String, SoftReference<Bitmap>> imageCache = null;
	// 固定五个线程来执行任务
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	// 缓存URL，判断是否有重复的URL
	private HashMap<String, String> imageUrlMap;
	private Context mContext;
	//缓存图片的目录
	public static final String CACHE_IMG_DIR_PATH = "/ecoder/";
	
	public AsyncDownloadImage(Context context) {
		imageCache = new HashMap<String, SoftReference<Bitmap>>();
		imageUrlMap = new HashMap<String, String>();
		mContext = context;
	}

	/**
	 * 
	 * @param url
	 * 		图像url地址
	 * @param appFileDir
	 * 		缓存文件存放地址
	 * @param isUpdate
	 * 		是否更新
	 * @param isLocalStore
	 * 		是否缓存到本地
	 * @param isSetDensity
	 * 		是否设置密度
	 * @param callback
	 * 		回调接口
	 * @return
	 * 		返回内存中缓存的图像，第一次加载返回null
	 */
	public Bitmap loadBitmap(final String url, String appFileDir, boolean isUpdate, final boolean isLocalStore, final boolean isSetDensity,
			final ImageCallback callback) {
		LogUtil.i(TAG,"loadBitmap url = " + url);
		if(url != null){
		
			final String imageUrl = Utils.getMD5Str(url) + ".dat";
			final String cacheImagePath = appFileDir + CACHE_IMG_DIR_PATH + imageUrl;
			LogUtil.i(TAG,"在本地缓存中获取的路径" + cacheImagePath);
			if (isLocalStore) {
				final Bitmap bitmap = Utils.getImgCacheFromLocal(cacheImagePath);
				LogUtil.i(TAG,"在本地缓存中获取的图片" + bitmap);
				if (bitmap != null) {
					if (isSetDensity && Utils.BITMAP_DENSITY != 0) {
						bitmap.setDensity(Utils.BITMAP_DENSITY);
	            	}
					return bitmap;
				} else if (!isUpdate){
					return null;
				}
			} else {
				// 如果缓存过就从缓存中取出数据
				if (imageCache.containsKey(imageUrl)) {
					final SoftReference<Bitmap> softReference = imageCache.get(imageUrl);
					final Bitmap bitmap = softReference.get();
		            if (bitmap != null) {
		                return bitmap;
		            }
				}
			}
			
			final Handler handler = new Handler() {
	            @Override
				public void handleMessage(Message message) {
	            	try{
	            		if(callback != null&& message != null){
		            		callback.imageLoaded((Bitmap) message.obj, url);
		            	}
	            	}catch (Exception e) {
						e.printStackTrace();
					}
	            	
	            	
	            }
	        };
	        
			// 缓存中没有图像，则从网络上取出数据，并将取出的数据缓存到内存中
			executorService.submit(new Runnable() {
				public void run() {
					try {
						if (!imageUrlMap.containsKey(imageUrl)) {
							imageUrlMap.put(imageUrl, imageUrl);
							final Bitmap bitmap = loadImageFromUrlToBitmap(url);
							LogUtil.e(TAG,"bitmap " + bitmap);
							if (isLocalStore) {
								 if (bitmap!=null&&isSetDensity && Utils.BITMAP_DENSITY != 0) {
									 bitmap.setDensity(Utils.BITMAP_DENSITY);
		    	                 }
								 //modify by yangguangfu 
								 if(bitmap!=null){
									 Utils.saveImage(bitmap, cacheImagePath);
								 }
								
							} else {
								imageCache.put(imageUrl, new SoftReference<Bitmap>(bitmap));
							}
							
							if (bitmap != null) {
								final Message message = handler.obtainMessage(0, bitmap);
								handler.sendMessage(message);
							}
						} else {
							// TODO 重复的URL不去下载
							LogUtil.e(TAG,"重复的URL不去下载" + imageUrl);
						}
					} catch (Exception e) {
						e.printStackTrace();
						return;
						//modify by yangguangfu
//						throw new RuntimeException(e);
					}
				}
			});
		}
		return null;
	}
	
	public void closeThreadPool() {
		executorService.shutdown();
	}
	
	// 从网络上取数据方法
	protected Bitmap loadImageFromUrlToBitmap(String imageUrl) {
		try {
			final URL url = new URL(imageUrl);
			Proxy proxy = null;
			HttpURLConnection conn = null;
			if (Utils.getNetMode(mContext).equals(Utils.NET_CMWAP) || Utils.getNetMode(mContext).equals(Utils.NET_WAP_3G)
			        || Utils.getNetMode(mContext).equals(Utils.NET_UNIWAP)) {
				final InetAddress inetAddr = InetAddress.getByAddress(new byte[] {(byte) 10, (byte) 0, (byte) 0, (byte) 172 });
				final SocketAddress socket = new InetSocketAddress(inetAddr, 80);
				proxy = new Proxy(Proxy.Type.HTTP, socket);
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			conn.setDoInput(true);
			conn.connect();
			final InputStream is = conn.getInputStream();
			if (is != null) {
				LogUtil.e("conn.getContentLength() ==" + conn.getContentLength());
				if (conn.getContentLength() > Utils.IMAGE_SIZE_COMPRESS) {
					return BitmapFactory.decodeStream(new FlushedInputStream(is), null, getCompressOpt());
				} else {
					return BitmapFactory.decodeStream(new FlushedInputStream(is));
				}
			} else {
				return null;
			}
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			LogUtil.i(TAG,e.toString());
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			LogUtil.i(TAG,e.toString());
			return null;
		}
	}
	
	/**
	 * BitmapFactory类的decodeStream方法在网络超时或较慢的时候无法获取完整的数据，
	 * 这里我们通过继承FilterInputStream类的skip方法来强制实现flush流中的数据，
	 * 主要原理就是检查是否到文件末端，告诉http类是否继续。
	 */
	public static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	// 对外界开放的回调接口
	public interface ImageCallback {
		// 注意 此方法是用来设置目标对象的图像资源
		public void imageLoaded(Bitmap imageBitmap, String imageUrl);
	}
	//压缩图片
	public static Options getCompressOpt() {
		final Options compressOpt = new BitmapFactory.Options();
		compressOpt.inSampleSize = 3;
		return compressOpt;
	}
	
	/**
	 * 返回一个输入流
	 * @param url
	 * @return
	 */
	public static InputStream loadImageInputStreamFromUrl(String url, Context context) {
		InputStream i = null;
		try {
			final URL u = new URL(url);
			Proxy proxy = null;
			HttpURLConnection conn = null;
			if (Utils.getNetMode(context).equals(Utils.NET_CMWAP) || Utils.getNetMode(context).equals(Utils.NET_WAP_3G)
			        || Utils.getNetMode(context).equals(Utils.NET_UNIWAP)) {
				final InetAddress inetAddr = InetAddress.getByAddress(new byte[] {(byte) 10, (byte) 0, (byte) 0, (byte) 172 });
				final SocketAddress socket = new InetSocketAddress(inetAddr, 80);
				proxy = new Proxy(Proxy.Type.HTTP, socket);
				conn = (HttpURLConnection) u.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) u.openConnection();
			}
			conn.setDoInput(true);
			conn.connect();
			i = conn.getInputStream();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return i;
	}
}

package com.weichuang.china.webkit;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.china.R;
import com.weichuang.china.music.MediaPlaybackService;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.util.ProxyUtils;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.player.SystemPlayer;

@TargetApi(11)
public class BrowserActivity extends Activity {
	private static final String TAG = "BrowserActivity";
	private static WebView mWebView;

	private String mTitle;

	private int mProgress = 0;

	public final static int PROGRESS_SHOW = 0;
	public final static int PROGRESS_HID = 1;
	public final static int PROGRESS_REF = 2;
	private static final int MESSAGE_STATE = 11;
	
	public static final int SCREENSHOT = 3;// 截屏設置
	public static final int BrowsingHistory = 4;
//	private SelectPicPopupWindow menuWindow;

	private Intent mIntent = null;
	private String uri = "http://m.hao123.com";
	private String mHomeUri = "http://m.hao123.com";
	private VideoInfo mVideoInfo = null;
	private int flags = 0;
	private Boolean isOncreat = false;

//	private ImageButton mButtonBack;// 后退按钮
//	private ImageButton mButtonForward;// 前进按钮
//	private ImageButton mButtonHome;// 回到主界面按钮
//
//	private LinearLayout mLoadProgressLayout;
//	private SeekBar mProgressSeekBar;
//
//	private ImageButton mButtonMenu;// menu按钮
//	private ImageButton mButtonMultiwindow;// 截屏按钮
//	private ImageButton searchButton;// 搜索按钮
	private boolean isLoadPlayed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		LogUtil.i(TAG, "onCreate----------------");
		setContentView(R.layout.browser_activity);
		initView();
		setOnlickListener();
		getData();
		setWebView(uri);
	}

	private void setOnlickListener() {
//		mButtonBack.setOnClickListener(itemsOnClick);
//		mButtonForward.setOnClickListener(itemsOnClick);
//		mButtonHome.setOnClickListener(itemsOnClick);
//		mButtonMenu.setOnClickListener(itemsOnClick);
//		mButtonMultiwindow.setOnClickListener(itemsOnClick);
//		
//		
//		searchButton.setOnClickListener(itemsOnClick);
	}

	private void initView() {
		mWebView = (WebView) findViewById(R.id.webview);
		isOncreat = true;
//		mButtonBack = (ImageButton) findViewById(R.id.bottombar_button_back_id);
//		mButtonForward = (ImageButton) findViewById(R.id.bottombar_button_forward_id);
//		mButtonHome = (ImageButton) findViewById(R.id.bottombar_button_home_id);
//		mButtonMenu = (ImageButton) findViewById(R.id.bottombar_button_menu_id);
//		mButtonMultiwindow = (ImageButton) findViewById(R.id.bottombar_button_multiwindow_id);
//		searchButton = (ImageButton) findViewById(R.id.searchButtonId);
//		mLoadProgressLayout = (LinearLayout) findViewById(R.id.loadProgress);
//		mProgressSeekBar = (SeekBar) findViewById(R.id.loadProgressSeekBar);
//		mProgressSeekBar.setMax(100);
	}

	private void getData() {
		mIntent =getIntent();
		if (mIntent == null)
			return;
		Bundle mBundle = mIntent.getBundleExtra("extra");
		if (mBundle != null) {
			mVideoInfo = (VideoInfo) mBundle.getSerializable("VideoInfo");
			if (mVideoInfo != null) {
				uri = mVideoInfo.getUrl();
				mTitle = mVideoInfo.getTitle();

			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case 1:
			if (mWebView != null && mWebView.canGoBack()) {
				if (mHandler != null) {
					mHandler.sendEmptyMessage(PROGRESS_SHOW);
				}
				mWebView.goBack();
			} else if (mWebView != null && !mWebView.canGoBack()) {
				Toast.makeText(this, "不能后退了", 0).show();
			}

			break;
		case 2:
			if (mWebView != null && mWebView.canGoForward()) {
				if (mHandler != null) {
					mHandler.sendEmptyMessage(0);
				}
				mWebView.goForward();
			} else if (mWebView != null && !mWebView.canGoForward()) {
				Toast.makeText(this, "不能前进了", 0).show();
			}
			break;
		case 3:
			if (Utils.isCheckNetAvailable(BrowserActivity.this)) {

				// if(uri ==null)
				// return true;
				//
				// if (uri.contains("rtsp") ||uri.contains("3gp") ||
				// uri.contains("mp4")) {
				// loadurl(mWebView, uri, true);
				// } else {
				// loadurl(mWebView, uri, false);
				// }
				// if (handler != null){
				// handler.sendEmptyMessage(0);
				// }
				if (mHandler != null) {
					mHandler.sendEmptyMessage(0);
				}
			} else {
				// if(!isNetCheckDialog){
				// isNetCheckDialog = true;
				// netCheckDialog();
				// }
				// Utils.netCheckDialog();
			}

			break;
		case 4:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setWebView(String loadUrl) {
		LogUtil.i(TAG, "-----------------loadUrl=" + loadUrl);
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mWebView.requestFocus();
		mWebView.setWebChromeClient(new HiWebChromeClient(this, mHandler));
		mWebView.setWebViewClient(new HiWebViewClient(this, mWebView, loadUrl));
		mWebView.addJavascriptInterface(new JsCheckNet(this, mWebView, null),
				"checknet");
		mWebView.loadUrl(loadUrl);
		mWebView.setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				String info = "uri=" + uri + "--userAgent=" + userAgent
						+ "--contenDisposition=" + contentDisposition
						+ "--mimetype=" + mimetype + "--contentLength="
						+ contentLength;
				Toast.makeText(BrowserActivity.this, info, Toast.LENGTH_SHORT)
						.show();
				
				startloadurl(url, true);

				// if(Utils.isCheckNetAvailable(HotAppActivity.this)){
				// startDownload(url);
				// }else{
				// Toast.makeText(HotAppActivity.this, R.string.net_outage_tip,
				// Toast.LENGTH_SHORT).show();
				// }

			}
		});
	}

	

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (mWebView != null && mWebView.canGoBack()) {
				mWebView.goBack();
			} else {
				onDestroyWebView();
				finish();
				if (flags == 2) {
					mWebView.setWillNotCacheDrawing(true);
				}
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}

			return true;
		}
		return false;
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {// 定义一个Handler，用于处理下载线程与UI间通讯
			switch (msg.what) {

			case MESSAGE_STATE:
				if (null != msg.getData()) {
					Bundle mBundle = (Bundle) msg.getData();
					String title = mBundle.getString("title");
					if (!Utils.isEmpty(title)) {
						// mTitle_txt.setText(title);
					}
				}
				break;

			case PROGRESS_SHOW:
				LogUtil.i(TAG, "View.VISIBLE");
				// if(progressDialog !=null)
				// progressDialog.setVisibility(View.VISIBLE);// 显示进度对话框
				break;
			case PROGRESS_HID:
				LogUtil.i(TAG, "View.GONE");
				// if(progressDialog !=null)
				// progressDialog.setVisibility(View.GONE);//
				// 隐藏进度对话框，不可使用dismiss()、cancel(),否则再次调用show()时，显示的对话框小圆圈不会动。
				break;

			case PROGRESS_REF:

//				if (mLoadProgressLayout != null) {
//
//					if (mProgress >= 100) {
//						mLoadProgressLayout.setVisibility(View.GONE);
//					} else {
//						mLoadProgressLayout.setVisibility(View.VISIBLE);
//					}
//				}
//
//				if (mProgressSeekBar != null) {
//					mProgressSeekBar.setProgress(mProgress);
//				}
//
//				if (mWebView != null && mWebView.canGoBack()) {
//					// if (handler != null){
//					// handler.sendEmptyMessage(PROGRESS_SHOW);
//					// }
//					// wv.goBack();
//					if (mButtonBack != null) {
//						mButtonBack.setEnabled(true);
//						mButtonBack
//								.setImageResource(R.drawable.toolbar_backward);
//					}
//
//				} else if (mWebView != null && !mWebView.canGoBack()) {
//					// Toast.makeText(BrowserActivity.this, "不能后退了", 0).show();
//					if (mButtonBack != null) {
//						mButtonBack.setEnabled(false);
//						mButtonBack
//								.setImageResource(R.drawable.toolbar_backward_disable);
//					}
//
//				}
//
//				if (mWebView != null && mWebView.canGoForward()) {
//					if (mButtonForward != null) {
//						mButtonForward
//								.setImageResource(R.drawable.toolbar_forward);
//						mButtonForward.setEnabled(true);
//					}
//
//					// if (handler != null){
//					// handler.sendEmptyMessage(0);
//					// }
//					// wv.goForward();
//				} else if (mWebView != null && !mWebView.canGoForward()) {
//					// Toast.makeText(BrowserActivity.this, "不能前进了", 0).show();
//					if (mButtonForward != null) {
//						mButtonForward.setEnabled(false);
//						mButtonForward
//								.setImageResource(R.drawable.toolbar_forward_disable);
//					}
//
//				}

				break;
			case SCREENSHOT:// 截屏
//				closePopwindow();
				// View是你需要截图的View
				View view = getWindow().getDecorView();
				view.setDrawingCacheEnabled(true);
				view.buildDrawingCache();
				Bitmap b1 = view.getDrawingCache();

				// //获取状态栏高度
				// Rect frame = new Rect();
				// activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
				// int statusBarHeight = frame.top;
				// System.out.println(statusBarHeight);

				// 去掉标题栏
				// //Bitmap b = Bitmap.createBitmap(b1, 0, 25, 320, 455);
				// Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width,
				// height - statusBarHeight);
				// view.destroyDrawingCache();
				break;
			case BrowsingHistory:// 浏览历史
//				closePopwindow();
//				Intent i = new Intent(BrowserActivity.this,
//						HistoryBookmarksActivity.class);
//				BrowserActivity.this.startActivity(i);
				break;

			}

			super.handleMessage(msg);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			isLoadPlayed = false;
		}
		System.out.println("3333333333");
		// if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode ==
		// RESULT_OK) {
		// // Fill the list view with the strings the recognizer thought it
		// could have heard
		// ArrayList matches = data.getStringArrayListExtra(
		// RecognizerIntent.EXTRA_RESULTS);
		// for(int i = 0;i < matches.size();i++){
		// System.out.println(matches.get(i));
		// }
		// }
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			// 取得语音的字符
			ArrayList<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			String resultString = "";
			for (int i = 0; i < results.size(); i++) {
				resultString += results.get(i);
			}
			Toast.makeText(this, resultString, 1).show();
		}
	}

	/**
	 * 监听点击事件
	 * 
	 * */
	private OnClickListener itemsOnClick = new OnClickListener() {

		public void onClick(View v) {
			switch (v.getId()) {/*
			case R.id.bottombar_button_back_id:// 后退按钮处理事件
				if (mWebView != null && mWebView.canGoBack()) {
					// if (handler != null){
					// handler.sendEmptyMessage(PROGRESS_SHOW);
					// }
					mWebView.goBack();

				} else if (mWebView != null && !mWebView.canGoBack()) {
					Toast.makeText(BrowserActivity.this, "不能后退了", 0).show();
					// if(mButtonBack!= null){
					// mButtonBack.setEnabled(false);
					// mButtonBack.setImageResource(R.drawable.toolbar_backward_disable);
					// }

				}
				break;
			case R.id.bottombar_button_forward_id:// 前进按钮处理事件
				if (mWebView != null && mWebView.canGoForward()) {
					// if (handler != null){
					// handler.sendEmptyMessage(PROGRESS_SHOW);
					// }
					mWebView.goForward();
				} else if (mWebView != null && !mWebView.canGoForward()) {
					Toast.makeText(BrowserActivity.this, "不能前进了", 0).show();
				}
				break;

			case R.id.bottombar_button_home_id:// 回到主界面处理事件
				if (mWebView != null) {
					mWebView.loadUrl(mHomeUri);
				}

				break;
			case R.id.bottombar_button_menu_id:// menu菜单按钮
				// 实例化SelectPicPopupWindow
//				menuWindow = new SelectPicPopupWindow(BrowserActivity.this,
//						itemsOnClick, mHandler);
//				menuWindow.setOnDismissListener(new SelectPicPopupWindow.OnDismissListener() {
//							@Override
//							public void onDismiss() {
//								closePopwindow();
//							}
//						});
//				// 显示窗口
//				menuWindow.showAtLocation(BrowserActivity.this.findViewById(R.id.bottombar),
//						Gravity.BOTTOM, 0, 68); // 设置layout在PopupWindow中显示的位置
				// menuWindow.update(0, 0, , 200);BOTTOM
				break;
			case R.id.bottombar_button_multiwindow_id:

				break;// 截屏按钮
			case R.id.searchButtonId:// 搜索
				// startVoiceRecognitionActivity();
				// try {
				// System.out.println("------------");
				// Intent intent = new
				// Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				// RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				// intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请开始说话");
				// startActivityForResult(intent,
				// VOICE_RECOGNITION_REQUEST_CODE);
				// }catch(ActivityNotFoundException e) {
				// System.out.println("222222222222222222");
				// AlertDialog.Builder builder = new
				// AlertDialog.Builder(BrowserActivity.this);
				// builder.setTitle("语音识别");
				// builder.setMessage("您的手机暂不支持语音搜索功能，点击确定下载安装Google语音搜索软件。您也可以在各应用商店搜索“语音搜索”进行下载安装。");
				// builder.setPositiveButton("确定", new
				// DialogInterface.OnClickListener() {
				// @Override
				// public void onClick(DialogInterface dialog, int which) {
				// // 跳转到下载语音网页
				// }
				// });
				// builder.setNegativeButton("取消", null);
				// builder.show();
				// }

				try {
					// 通过Intent传递语音识别的模式，开启语音
					Intent intent = new Intent(
							RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					// 语言模式和自由模式的语音识别
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
					// 提示语音开始
					intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "开始语音");
					// 开始语音识别
					startActivityForResult(intent,
							VOICE_RECOGNITION_REQUEST_CODE);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "找不到语音设备", 1)
							.show();
				}
				break;

			default:
				break;
			*/}

		}

	};

	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (flags == 2) {
			System.out.println("onPause--------------");
			isOncreat = false;
		}
		super.onPause();
	}

	public static void onDestroyWebView() {
		try {
			if (mWebView != null) {
				mWebView.onPause();
				mWebView.destroy();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onDestroy() {
		System.out.println("onDestroy----------");

		super.onDestroy();

	}

	public void startloadurl( final String url,
			final boolean isVideoUrl) {
		new Thread() {
			public void run() {
				LogUtil.i(TAG, "run()");
				String uris = url;
				if (isVideoUrl && url != null) {
					
					if (url.contains("rtsp") ||uris.contains("3gp") || uris.contains("mp4")|| uris.contains("m3u8")) {
						if(uris != null&&!isLoadPlayed){
							isLoadPlayed = true;
							LogUtil.i(TAG, "uris="+uris);
							
							Intent i = new Intent(BrowserActivity.this, MediaPlaybackService.class);
					        i.setAction(MediaPlaybackService.SERVICECMD);
					        i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
					        startService(i);
							
					        uris = ProxyUtils.getRedirectUrl(uris);
					        
					        mVideoInfo.setUrl(uris);
					        try{
					        	 if(mTitle != null&&mTitle.trim().length()>0)
								        mVideoInfo.setTitle(mTitle);
					        }catch(Exception e){
					        	e.printStackTrace();
					        }
					       
					       
							Intent intent = new Intent(BrowserActivity.this,SystemPlayer.class);
							Bundle mBundle = new Bundle();
							mBundle.putSerializable("VideoInfo", mVideoInfo);
							intent.putExtras(mBundle);
							LogUtil.i(TAG, "startActivity");
							BrowserActivity.this.startActivityForResult(intent, 1);
							overridePendingTransition(R.anim.fade, R.anim.hold);
							if (mHandler != null){ 
								mHandler.sendEmptyMessage(1);
							}
							
						}
						
					}
				} 

			}
		}.start();
	}
	private void startDownload(String downloadUrl) {
		// Intent downloadIntent = new Intent(HotAppActivity.this,
		// DownLoadService.class);
		// Bundle bundle = new Bundle();
		// bundle.putString("Url", downloadUrl);
		// downloadIntent.putExtras(bundle);
		// startService(downloadIntent);
	}

}
package com.weichuang.china.video.local;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.IMediaPlaybackService;
import com.weichuang.china.music.MediaPlaybackService;
import com.weichuang.china.music.MusicUtils;
import com.weichuang.china.music.NowPlayingActivity;
import com.weichuang.china.music.MusicUtils.ServiceToken;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.tv.TVActivity;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.player.SystemPlayer;
import com.weichuang.china.video.view.MyListView;
import com.weichuang.china.video.view.UserPreference;

public class VideoListActivity extends BaseActivity implements ServiceConnection {

	private static final String TAG = "PlayLists";
	private ArrayList<VideoInfo> mLinkedList = new ArrayList<VideoInfo>();
	private ServiceToken mToken = null;
	private IMediaPlaybackService mService = null;
	private MyListView myListView = null;
	private VideoListAdapter listAdapter = null;
	private boolean isLoaded = true;
	
	private boolean isLoadedOk = false;
	private ImageView noSdcard = null;
	private LinearLayout linearLayout = null;
	private TextView mLoadinText = null;
	
	private String path = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("2222222222222222");
		mToken = MusicUtils.bindToService(this, this);
		myListView = (MyListView) findViewById(R.id.list);
		listAdapter = new VideoListAdapter(this, mLinkedList);
		noSdcard = (ImageView) findViewById(R.id.icon_nocard);
		linearLayout = (LinearLayout) findViewById(R.id.video_loading);
		mLoadinText = (TextView) findViewById(R.id.video_loading_text);
		RelativeLayout background_id = (RelativeLayout)findViewById(R.id.background_id);
		myListView.setAdapter(listAdapter);
		myListView.setOnItemClickListener(onItemClickListener);
		myListView.setOnItemLongClickListener(onItemLongClickListener);
		BaseActivity.mBaseActivity = this;
		setTopBarTitle("全部视频");
		setTitleRightButtonHide();
		handler.sendEmptyMessageDelayed(1, 300);
		 
        Utils.setChangeBackground(VideoListActivity.this, background_id); 
	  
		 
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				if (isLoaded) {
					isLoaded = false;
					if(Utils.isFileChange){
						linearLayout.setVisibility(View.VISIBLE);
						mLinkedList = new ArrayList<VideoInfo>();
						myListView = (MyListView) findViewById(R.id.list);
						listAdapter = new VideoListAdapter(VideoListActivity.this, mLinkedList);
						myListView.setAdapter(listAdapter);
						myListView.setOnItemClickListener(onItemClickListener);
					}
					Utils.isFileChange = false;  
//				    runOnUiThread(new Runnable() {
//						
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							
//						}
//					});
//					new Thread(new Runnable() {
//						@Override
//						public void run() {
//							if(Utils.isSDcardExist()){ 
//								getVideoFile(mLinkedList, new File(Utils.SDCARD));
//								handler.sendEmptyMessage(2);	
//							}else{
//								handler.sendEmptyMessage(2);	
//							}
//						}
//					}).start();
				
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						if(Utils.isSDcardExist()){ 
							getVideoFile(mLinkedList, new File(Utils.SDCARD));
							handler.sendEmptyMessage(2);	
						}else{
							handler.sendEmptyMessage(2);	
						}
					}
					});
				
				};

				break;
			case 2:
				if (mLinkedList != null && mLinkedList.size() > 0) {
					isLoadedOk = true;
					linearLayout.setVisibility(View.GONE);
					noSdcard.setVisibility(View.GONE);
					listAdapter.setList(mLinkedList);
					listAdapter.notifyDataSetChanged();
				}else{
					isLoadedOk = true;
					noSdcard.setVisibility(View.VISIBLE);
					linearLayout.setVisibility(View.GONE);
					noSdcard.setVisibility(View.VISIBLE);
				}
				break;
			case 3:
				if(path!=null&&!isLoadedOk){
					mLoadinText.setText(path);
				}else{
					mLoadinText.setText(path);
					linearLayout.setVisibility(View.GONE);
				}
				
				break;
			}
			
		}

	};

	private void getVideoFile(final ArrayList<VideoInfo> mLinkedList, File file) {

		file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
//				Log.v(TAG, "getVideoFile()");
				String end = file.getName();
				int i = end.indexOf('.');
				if (i != -1) {
					end = end.substring(i);
					String fName = file.getName();
					/* 取得扩展名 */
					end = fName.substring(fName.lastIndexOf(".") + 1,
							fName.length()).toLowerCase();
					if (Utils.isVideoFile(end)) {

						VideoInfo mi = new VideoInfo();

//						LogUtil.i(TAG, "file.getName()==" + file.getName());
//						LogUtil.i(TAG, "file.getAbsolutePath()=="
//								+ file.getAbsolutePath());
				
						path = file.getName();
						if(!isLoadedOk){
							handler.removeMessages(3);
							handler.sendEmptyMessage(3);
						}/*else{
							linearLayout.setVisibility(View.GONE);
						}
						*/
						mi.setTitle(file.getName());
						mi.setUrl(file.getAbsolutePath());
						mi.setFileSize(file.length());
//						LogUtil.i(TAG, "mi.getTitle()==" + mi.getTitle());
//						LogUtil.i(TAG, "mi.getUrl()==" + mi.getUrl());
//						LogUtil.i(TAG, "mLinkedList.size()=="
//								+ mLinkedList.size());
						// list.add(mi);
						mLinkedList.add(mi);
						return true;
					}
				} else if (file.isDirectory()) {
					if(isLoadedOk){
						return true;
					}
					getVideoFile(mLinkedList, file);
				}
				return false;
			}
		});
	}

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
			
			if(isLoadedOk){
				VideoInfo videoinfo = (VideoInfo) mLinkedList.get(position);

				LogUtil.i(TAG,"onItemClick~~~~~~~~~~~~~~~~~~~");
				LogUtil.i(TAG, "videoinfo==" + videoinfo);
				LogUtil.i(TAG, "position==" + position);
				if (videoinfo != null) {
					LogUtil.i(TAG, "URi==" + videoinfo.getUrl());
					LogUtil.i(TAG, "URi==" + videoinfo.getTitle());
					File f = new File(videoinfo.getUrl());
					positionTem = position;
					openFile(f,position);
				}
//				overridePendingTransition(R.anim.fade, R.anim.hold);
			}else{
				Toast.makeText(VideoListActivity.this, "正在加载视频，请稍后在试！", 0).show();
			}
			

		}
	};
	
	private OnItemLongClickListener onItemLongClickListener =	new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {

			VideoInfo videoinfo = (VideoInfo) mLinkedList.get(position);

			LogUtil.i(TAG,"onItemClick~~~~~~~~~~~~~~~~~~~");
			LogUtil.i(TAG, "videoinfo==" + videoinfo);
			LogUtil.i(TAG, "position==" + position);
			if (videoinfo != null) {
//				LogUtil.i(TAG, "URi==" + videoinfo.getUrl());
//				LogUtil.i(TAG, "URi==" + videoinfo.getTitle());
//				File f = new File(videoinfo.getUrl());
//				openFile(f);
				positionTem = position;
				showUserOperationDialog(videoinfo);;
			}
			return true;
		}
	};
	private AlertDialog mListOperationDialog;
	private void showUserOperationDialog(VideoInfo videoinfos) {
		final VideoInfo videoinfo = videoinfos;
		if(videoinfo!=null){
			mListOperationDialog = new AlertDialog.Builder(
					VideoListActivity.this).setTitle(videoinfo.getTitle()).setItems(
					R.array.items_for_allVideo_local,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							/* Play */
							case 0:
								startActivity(positionTem);
								break;
							/* Rename */
//							case 1:
//								showRenameDialog(position);
//								break;
							/* Delete */
							case 1:
								deleteVideo(videoinfo);
								break;
							/* default */
							default:
								break;
							}
						}
					}).create();
		
			mListOperationDialog.show();
		}


		
	}
	private AlertDialog mCurrrentActiveDialog;
	private void deleteVideo(final VideoInfo videoinfos) {
//		Log.v(TAG, "deleteVideo  :   " + position);

		mCurrrentActiveDialog = new AlertDialog.Builder(this).setMessage(
				getString(R.string.confirm_deletefile)).setPositiveButton(
				R.string.button_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
		                File file = new File(videoinfos.getUrl());
		                if(file.exists()){
		                	  if (file.delete()) {
//				                	Utils.isFileChange = true;
				                	mLinkedList.remove(videoinfos);
				                    Log.i(TAG, "delete file failure");
				                    listAdapter.setList(mLinkedList);
									listAdapter.notifyDataSetChanged();
				                }
		                }
		              
		            
					}
				}).setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}
	
	private int positionTem = 0;
	private void startActivity(int position) {
		
		 Intent i = new Intent(VideoListActivity.this, MediaPlaybackService.class);
         i.setAction(MediaPlaybackService.SERVICECMD);
         i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
         startService(i);
			
		 Intent intent = new Intent(VideoListActivity.this,SystemPlayer.class);
	     Bundle mBundle = new Bundle();
		 mBundle.putSerializable("MediaIdList", mLinkedList);
		 intent.putExtras(mBundle);
		 intent.putExtra("CurrentPosInMediaIdList", position);
		 startActivity(intent);
		 overridePendingTransition(R.anim.fade, R.anim.hold);
			
	}



	/**手机打开文件的method */
	private void openFile(File f,int position)

	{
		if (f != null) {
			String type = getMIMEType(f);
			if (type.equals("video")) {
				try {
					if (mService != null && mService.isPlaying()) {
						mService.pause();
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				startActivity(position);
//				Intent intent = new Intent(VideoListActivity.this, SystemPlayer.class);
//				intent.putExtra("localuri", f.toString());
//				startActivity(intent);
//				 Intent intent = new Intent(VideoListActivity.this,SystemPlayer.class);
//			     Bundle mBundle = new Bundle();
//				 mBundle.putSerializable("MediaIdList", mLinkedList);
//				 intent.putExtras(mBundle);
//				 intent.putExtra("CurrentPosInMediaIdList", position);
//				overridePendingTransition(R.anim.fade, R.anim.hold);
			} else if (type.equals("audio")) {
				playInFileList(f.toString());
			} else {
				Toast.makeText(VideoListActivity.this, "不是媒体文件", 0).show();
			}
		}

	}

	private void playInFileList(String path) {
		if (MusicUtils.sService != null) {
			try {
				MusicUtils.sService.openFile(path, true);

				MusicUtils.sService.play();

//				Intent intent = new Intent(
//						"com.weichuang.china.music.PLAYBACK_VIEWER")
//						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Intent intent = new Intent(VideoListActivity.this,NowPlayingActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return;
			} catch (RemoteException ex) {
			}
		}
	}

	/** 判断文件MimeType的method */
	private String getMIMEType(File f) {
		// boolean isVideo = false;

		String type = "";
		String fName = f.getName();
		String end = fName
				.substring(fName.lastIndexOf(".") + 1, fName.length())
				.toLowerCase();

		/* 按扩展名的类型决定MimeType */
		if (Utils.isMusicFile(end)) {
			type = "audio";
			// isVideo = true;
		}
		// \ndivx 、xvid 、 wmv 、 flv 、 ts 、 rmvb 、 rm 、 mkv 、 mov 、 m4v 、 avi 、
		// mp4 、 3gp 、 mpg \n\n\n
		else if (Utils.isVideoFile(end)) {
			type = "video";
			// isVideo = true;
		}
		/*
		 * else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||
		 * end.equals("jpeg")||end.equals("bmp")) { type = "image"; }
		 */
		else {
			// isVideo = false;
			type = "*";
		}
		/* 如果无法直接打开，就弹出软件列表给用户选择 */
		// type += "/*";
		return type;
		// return isVideo;
	}

	@Override
	protected void onDestroy() {
		Log.v(TAG, "onDestroy()");
		super.onDestroy();
		MusicUtils.unbindFromService(mToken);
		// finish();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v(TAG, "onPause()()");
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.v(TAG, "onRestart()");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		BaseActivity.mBaseActivity = this;

		if (!isLoaded && mLinkedList != null && mLinkedList.size() <= 0) {
			myListView = (MyListView) findViewById(R.id.list);
			listAdapter = new VideoListAdapter(this, mLinkedList);
			myListView.setAdapter(listAdapter);
			myListView.setOnItemClickListener(onItemClickListener);
			handler.sendEmptyMessage(1);
		}
		if(	Utils.isFileChange){
			isLoaded = true;
			handler.sendEmptyMessage(1);
		}
		Log.v(TAG, "onResume()");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.v(TAG, "onSaveInstanceState()");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.v(TAG, "onStart()");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.v(TAG, "onStop()");
	}

	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.file_list_activity, null);
	}

	protected void titleLeftButton() {
		finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	protected void titlRightButton() {
		AppConnect.getInstance(this).showOffers(this);

	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMediaPlaybackService.Stub.asInterface(service);

	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub

	}

}

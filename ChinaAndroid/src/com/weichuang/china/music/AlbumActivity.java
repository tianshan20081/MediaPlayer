package com.weichuang.china.music;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MusicUtils.ServiceToken;
import com.weichuang.china.music.coverflow.Cover;
import com.weichuang.china.music.coverflow.CoverFlow;
import com.weichuang.china.music.coverflow.CoverFlowAdapter;
import com.weichuang.china.music.coverflow.CoverFlowBackView;
import com.weichuang.china.music.coverflow.CoverFlowSeekBar;
import com.weichuang.china.music.coverflow.CoverFlowWrapper;
import com.weichuang.china.music.coverflow.SongListAdapter;
import com.weichuang.china.music.coverflow.VerticalSeekBar;
import com.weichuang.china.music.coverflow.CoverFlow.onCoverFlipListener;
import com.weichuang.china.music.coverflow.CoverFlow.onCoverSelectedListener;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.video.view.UserPreference;

public class AlbumActivity extends BaseActivity implements ServiceConnection {

	private static final String TAG = "AlbumActivity";
	private CoverFlow mAlbumCoverFlow = null;
 
	
	
	private ServiceToken mToken;
	private CoverFlowWrapper mCoverFlowWrapper;
	private CoverFlowBackView mAlbumCoverFlowBackView;
	private ListView mAlbumSongList;
	private CoverFlowAdapter mCoverFlowAdapter;
	private AlbumSongListQueryHandler mQueryHandler;
	private SongListAdapter mAlbumSongListAdapter;

	private int lastProgress = 0;

	private boolean isInTrackingSeekBar = false;
	
	private CoverFlowSeekBar mSeekBar;
	private VerticalSeekBar mVerSeekBar;

	private Runnable disableTrackingSeekBar = new Runnable() {

		public void run() {
			isInTrackingSeekBar = false;
			
			

		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	
	    setTitleRightButtonHide();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(MediaPlaybackService.META_CHANGED);
		registerReceiver(mIntentReceiver, commandFilter);
		mToken = MusicUtils.bindToService(this, this);
		IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);
//		setContentView(R.layout.album_browser_activity);
    	BaseActivity.mBaseActivity = this;
	    UserPreference.ensureIntializePreference(this); 
	    setTopBarTitle("专辑"); 
		setTitleRightButtonBackbound(R.drawable.action_play_list); 
		
		LinearLayout laytout_beij =(LinearLayout)findViewById(R.id.laytout_beij);
		UserPreference.ensureIntializePreference(AlbumActivity.this);  
	    int defaultColor = UserPreference.read("defaultColor", 0); 
	    System.out.println("defaultColor === "+defaultColor + "   utils ===" );
		
		
		
		mCoverFlowWrapper = (CoverFlowWrapper) findViewById(R.id.albumcoverflowwrapper);
	 
		mAlbumSongList = (ListView) mCoverFlowWrapper.findViewById(R.id.songsofalbum);
		mAlbumCoverFlow = (CoverFlow) findViewById(R.id.albumcoverflow);
		///////////////////
//		if(defaultColor == 0){ 
//			mAlbumCoverFlow.setBackgroundResource(R.drawable.moren_beijing); 
//        }else if(defaultColor == 1){ 
//        	mAlbumCoverFlow.setBackgroundResource(R.drawable.moren_beijing1);  
//        } else{
//        	mAlbumCoverFlow.setBackgroundColor(defaultColor); 
//        }
		
		
		mAlbumCoverFlowBackView = (CoverFlowBackView)findViewById(R.id.songsofalbumcontainer);
		 
		mSeekBar = (CoverFlowSeekBar) findViewById(R.id.albumcoverflowseekbar);
		mVerSeekBar = (VerticalSeekBar)findViewById(R.id.albumcoverflowseekbar_vertical);
		mSeekBar.setThumbOffset(0);
		mVerSeekBar.setThumbOffset(0);
		//added by liuqiang
		mSeekBar.setKeyProgressIncrement(1);
		mVerSeekBar.setKeyProgressIncrement(1);
		//end add
		mAlbumCoverFlow.setOnCoverSelectedListener(new onCoverSelectedListener() {
					public void onCoverSelected(CoverFlow coverflow,
							int position) {
						int progress = position;
						if (progress != lastProgress && !isInTrackingSeekBar) {
							int orientation = getResources().getConfiguration().orientation;
							if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
								mSeekBar.setProgress(progress);
							} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
								mVerSeekBar.setProgress(progress);
							}
						lastProgress = progress;
					}
					}

					public void onCoverCentered(CoverFlow coverflow,
							int position, Cover cover, boolean changed) {
						Log.i("onCoverCentered",position+":"+changed+"");

					}
				});
		mAlbumCoverFlow.setOnCoverFlipListener(new onCoverFlipListener() {
			public void onCoverStartFlip(CoverFlow coverflow, Cover cover,
					boolean flipToBack) {
				if (flipToBack) {
					mSeekBar.mSurpressTouchEvent = true;
					mVerSeekBar.mSurpressTouchEvent = true;
					if (mAlbumSongListAdapter.mCurrenCover != cover) {
						mAlbumSongListAdapter.mCurrenCover = cover;
						mAlbumSongListAdapter.mCurrentCursor = null;
						asyncQueryAlbumSongCursor(cover);
			
					}
					else{
						if(mAlbumSongListAdapter.mCurrentCursor !=null){
						}else{
							asyncQueryAlbumSongCursor(cover);
						}
					}

				} else {
					mAlbumCoverFlowBackView.hide();
					mAlbumSongListAdapter.isWaitingForCursor = false;
					if(mAlbumSongListAdapter.mCurrentCursor == null){
						mQueryHandler.cancelOperation((int)cover.getAlbumId());
					}

				}

			}

			public void onCoverFinishFlip(CoverFlow coverflow, Cover cover,
					boolean flipToBack) {
				if (!flipToBack) {
					mSeekBar.mSurpressTouchEvent = false;
					mVerSeekBar.mSurpressTouchEvent = false;
				} else {
					if(mAlbumSongListAdapter.mCurrentCursor == null){
						mAlbumSongListAdapter.isWaitingForCursor = true;
						mAlbumSongListAdapter.changeCursor(null);
					}else{
						mAlbumSongListAdapter.changeCursor(mAlbumSongListAdapter.mCurrentCursor);
						mAlbumSongListAdapter.isWaitingForCursor =false;
					}
					mAlbumCoverFlowBackView.setTitle(cover);
					mAlbumCoverFlowBackView.show();

				}

			}
		});
		setupSeekBarChangeListener();
		
		

		mQueryHandler = new AlbumSongListQueryHandler(getContentResolver());
		mAlbumSongListAdapter = (SongListAdapter)mAlbumSongList.getAdapter();	
		
		onOrientationChanged(getResources().getConfiguration().orientation);
		//modify by yangguangfu
//		setTitle(R.string.albums_title);

	}

	private void setupSeekBarChangeListener() {
		if ( mSeekBar != null ) {
			mSeekBar.setOnSeekBarChangeListener(mSeekAlbumListener);
		}
		if ( mVerSeekBar != null ) {
			mVerSeekBar.setOnSeekBarChangeListener(mVerSeekAlbumListener);
		}
	}
	
	public void onServiceConnected(ComponentName name, IBinder service) {

	}

	public void onServiceDisconnected(ComponentName name) {
	}

	private void refreshOnResume() {
		if ( mCoverFlowAdapter == null ) {
			mCoverFlowAdapter = new CoverFlowAdapter(this, null);
			getAlbumCursor(new AllAlbumQueryHandler(getContentResolver()),	null);
		} else {
			if ( mAlbumSongListAdapter != null ) {
				mAlbumSongListAdapter.notifyDataSetChanged();
			}
			CoverFlowAdapter adapter = mAlbumCoverFlow.getAdapter();
			if(adapter != null && adapter.isNeedInit()){
				Cursor c = adapter.getCursor();
				if(c != null && !c.isClosed()){
					c.requery();
				}
				int initialPosition = mAlbumCoverFlow.getCurrentSelectedCoverIndex();
				mAlbumCoverFlow.setAdapter(adapter,initialPosition);
				mAlbumCoverFlow.setInitialFlipStatus();
				mCoverFlowWrapper.mCoverFlowBackView.hide();
				adapter.setNeedRefresh(false);
				
			}
			else if(adapter !=null){
				if(mAlbumCoverFlow.isFlipped()){
					mCoverFlowWrapper.mCoverFlowBackView.show();
				}
			}
		}		
		mAlbumCoverFlow.onResume();
	}
	
	@Override
	public void onDestroy() {
		if ( mToken != null ) {
			MusicUtils.unbindFromService(mToken);
		}
		unregisterReceiver(mScanListener);
		unregisterReceiver(mIntentReceiver);
		 ActivityHolder.getInstance().removeActivity(this);
		super.onDestroy();
	}

	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(AlbumActivity.this);
            mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
//                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if ( mCoverFlowAdapter != null ) {
        		//TODO: do we need to new async query handler everytime?
    			getAlbumCursor(new AllAlbumQueryHandler(getContentResolver()),	null);
    		}
        }
    };
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ((MediaPlaybackService.META_CHANGED).equals(action)) {		
				mAlbumSongListAdapter.notifyDataSetChanged();
			}
		}
	};

	private OnSeekBarChangeListener mSeekAlbumListener = new OnSeekBarChangeListener() {
		private int mLastPosition;

		public void onStopTrackingTouch(SeekBar seekBar) {
			mSeekBar.postDelayed(disableTrackingSeekBar, 300);

		}
 
		public void onStartTrackingTouch(SeekBar seekBar) {
			int progress = seekBar.getProgress();
			//modified by liuqiang
			int position = progress;
			//end modification
			mLastPosition = position;

			isInTrackingSeekBar = true;

		}

		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				int position = progress;
				//end modification
				if (position != mLastPosition) {
					mAlbumCoverFlow.scrollToChild(position);
					mLastPosition = position;
				}
			}

		}
	};
	
	private VerticalSeekBar.OnSeekBarChangeListener mVerSeekAlbumListener = new VerticalSeekBar.OnSeekBarChangeListener() {
		private int mLastPosition;

		public void onStopTrackingTouch(VerticalSeekBar seekBar) {
			seekBar.postDelayed(disableTrackingSeekBar, 300);
		}

		public void onStartTrackingTouch(VerticalSeekBar seekBar) {
			int progress = seekBar.getProgress();
			//modified by liuqiang
//			int position = progress * mAlbumCoverFlow.getCount() / 100;
			int position = progress;
			//end modification
			mLastPosition = position;

			isInTrackingSeekBar = true;

		}

		public void onProgressChanged(VerticalSeekBar seekBar,
				int progress, boolean fromUser) {
			if (fromUser) {
				//modified by liuqiang
//				int position = progress * mAlbumCoverFlow.getCount() / 100;
				int position = progress;
				//end add modification
				if (position != mLastPosition) {
					mAlbumCoverFlow.scrollToChild(position);
					mLastPosition = position;
				}
			}

		}

	};
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);		
		menu.add(0, 0, 0, getResources().getString(
				R.string.menu_player));
		menu.getItem(0).setIcon(R.drawable.menu_player);
		return true;
	}
    
	public boolean onOptionsItemSelected(MenuItem item) {
		if ( item.getItemId() == 0 ) {
			MusicUtils.goToPlayer(this);
		}
		return super.onOptionsItemSelected(item);
	}

	public void init(Object cookie, Cursor cursor) {
		CoverFlowAdapter adapter = (CoverFlowAdapter)cookie;
		if ( adapter == null ) {
			return;
		}
		if (cursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		} 
		adapter.setCursor(cursor);
		mAlbumCoverFlow.setAdapter(adapter,0);
		//added by liuqiang
		int count = mAlbumCoverFlow.getCount();
		mSeekBar.setMax(count - 1 );
		mSeekBar.setProgress(mAlbumCoverFlow.getCurrentSelectedCoverIndex());
		mVerSeekBar.setMax(count - 1);
		mVerSeekBar.setProgress(mAlbumCoverFlow.getCurrentSelectedCoverIndex());
	}
	
	
	private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {
		Log.d(TAG, "getAlbumCursor()==================");
		Cursor ret = null;
		if (async != null) {
			async.startQuery(0, mCoverFlowAdapter,
					MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null,
					null, MediaStore.Audio.Albums.LAST_YEAR + " DESC");
		} else {
			ret = MusicUtils.query(this,
					MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null,
					null, MediaStore.Audio.Albums.LAST_YEAR + " DESC");
		}
		return ret;
	}
	
	private class AllAlbumQueryHandler extends AsyncQueryHandler{

		public AllAlbumQueryHandler(ContentResolver cr) {
			super(cr);
		}
		
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			init(cookie, cursor);
		}
	}
	

	public void asyncQueryAlbumSongCursor(Cover cover) {
		long albumId = cover.getAlbumId();
		String where = MediaStore.Audio.Media.ALBUM_ID + "=" + albumId
				+ " AND " + MediaStore.Audio.Media.IS_MUSIC + "=1";

		mQueryHandler.startQuery((int)albumId, cover,
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, where, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

	}

	private class AlbumSongListQueryHandler extends AsyncQueryHandler {

		public AlbumSongListQueryHandler(ContentResolver cr) {
			super(cr);

		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Cover cover = (Cover) cookie;
			if (mAlbumSongListAdapter.mCurrenCover == cover) {
				mAlbumSongListAdapter.mCurrentCursor = cursor;
				if(mAlbumSongListAdapter.isWaitingForCursor){
					mAlbumSongListAdapter.changeCursor(cursor);
					mAlbumSongListAdapter.isWaitingForCursor = false;
				}
			} else {
				cursor.close();
			}
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
		protected void onPause() {
			if ( mAlbumCoverFlow != null ) {
				mAlbumCoverFlow.onPause();
			}
			if ( mReScanHandler != null ) {
				mReScanHandler.removeCallbacksAndMessages(null);
			}
			if ( mLoadingHandler != null ) {
				mLoadingHandler.removeCallbacksAndMessages(null);
			}
			if ( dialog != null && dialog.isShowing() ) {
				try {
					dialog.cancel();
				} catch (Exception ex) {
					Log.d(TAG, "onPause()=======catch exception when cancel dialog:" + ex);
				}
			}
			super.onPause();
		}
	
	@Override
	protected void onResume() {
		super.onResume();
		BaseActivity.mBaseActivity = this;
		if (!MusicUtils.checkSdcardAvailable()) {
			MusicUtils.showSdcardInfo(this);
			return;
		} else {
			refreshOnResumeByScanCheck();
		}	
		
		
	}
	
	private void refreshOnResumeByScanCheck() {
		if ( MusicUtils.isMediaScannerScanning(this) ) {
			Log.d(TAG, "onResume()=============media scanner is scanning....");
			showCancelPopup(AlbumActivity.this, R.string.synchronizing_sdcard,AlbumActivity.this);
			if(mLoadingHandler == null) {
				mLoadingHandler = new LoadingHandler();
			}
			mLoadingHandler.sendEmptyMessageDelayed(LOADING, 200);
		} else {
			refreshOnResume();
		}
	}

	private void onOrientationChanged(int newOrientation) {
		if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			mSeekBar.setVisibility(View.VISIBLE);
			mVerSeekBar.setVisibility(View.GONE);

			int count = mAlbumCoverFlow.getCount();
			//modified by liuqiang
			mSeekBar.setMax(count - 1);
			mSeekBar.setProgress(mAlbumCoverFlow.getCurrentSelectedCoverIndex());
			//end modification
		} else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
			mSeekBar.setVisibility(View.GONE);
			mVerSeekBar.setVisibility(View.VISIBLE);

			int count = mAlbumCoverFlow.getCount();
			//modified by liuqiang
			mVerSeekBar.setMax(count - 1);
			mVerSeekBar.setProgress(mAlbumCoverFlow.getCurrentSelectedCoverIndex());
			//end modification

		}
	}
	@Override
	protected void onStop() {
			if(mAlbumCoverFlow.isFlipped()){
				mCoverFlowWrapper.mCoverFlowBackView.hide();
			}
		super.onStop();
	}
	
	private static final int LOADING = 10001;
	private LoadingHandler mLoadingHandler;
	private ProgressDialog dialog;
	private class LoadingHandler extends Handler {
		public LoadingHandler() {
		}
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOADING:
				if (!MusicUtils.isMediaScannerScanning(AlbumActivity.this)) {
					try {
						dialog.cancel();
					} catch (Exception ex) {
						Log.d(TAG, "LoadingHandler->handleMessage()=======catch exception when cancel dialog:" + ex);
					}
					refreshOnResume();
				} else {
					sendEmptyMessageDelayed(LOADING, 200);
				}
				break;
			}
			
			super.handleMessage(msg);
		}
	}
	public void showCancelPopup(Context ctx, int stringid, final Activity act) {
		dialog = new ProgressDialog(this);
		dialog.setCancelable(true);
		dialog.setTitle(R.string.wait_please);
		dialog.setMessage(ctx.getResources().getString(stringid));
		dialog.show();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_BACK&& event.getRepeatCount() == 0 ) {
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
			return true;
		}
		return false;
	}

	@Override
	protected View setCententView() {
		if(inflater==null){
			return null;
		}
		
		return inflater.inflate(R.layout.album_browser_activity, null);
	}

	 
	@Override
	protected void titleLeftButton() {
		finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	@Override
	protected void titlRightButton() {
		AppConnect.getInstance(this).showOffers(this);
		
	}
}

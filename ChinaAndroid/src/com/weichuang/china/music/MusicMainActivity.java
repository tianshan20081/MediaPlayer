package com.weichuang.china.music;

import android.app.Activity;
import android.app.Service;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.android.china.R;
import com.weichuang.china.music.MusicUtils.ServiceToken;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.video.net.ShowActivity;
import com.weichuang.china.video.view.UserPreference;

public class MusicMainActivity extends TabActivity implements OnTabChangeListener, ServiceConnection{
	private static final String TAG = "MusicMainActivity";
	
	private static final int SONG_TAB = 0;
	
	private static final int ARTIST_TAB = 1;
	
	private static final int ALBUM_TAB = 2;
	
	private static final int MYFAVOURITE_TAB = 3;

	public static Activity instance;
	private TabHost mTabHost;
//	private Resources mRes;

	private LinearLayout mDeleteSection;
	private ToolBarButton mDeleteToolBarButton;
	private OnClickListener mDeleteButtonClickListener;
	private ServiceToken mToken;
	
	
	private final Handler mStartupHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if ( mTabHost != null ) {
				mTabHost.setCurrentTab(getTabIndex(getIntent()));
			}
		}
	};
	private RadioButton allSongs;
	private RadioButton studyEnglsh;
	private RadioButton allAlbum;
	private RadioButton myFavourite;
	public RadioGroup mRadioGroup;
	public static final String ALL_SONG_TAB = "全部歌曲";
	public static final String STUDY_ENGLISH_TAB = "在线音乐";
	public static final String ALL_ALBUM_TAB = "专辑";
	public static final String ALL_MYFAVOURITE_TAB = "我的最爱";
	
	private Intent 	mIntent = null;
	private int flag = 2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		if ( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
//			//getWindow().addFlags(WindowManager.LayoutParams.FLAG_ROCKET_MENU_NOTIFY);
		   UserPreference.ensureIntializePreference(this);   
			int defaultColor = UserPreference.read("defaultColor", 0);  
			if(defaultColor == 0){ 
				setContentView(R.layout.main_music_activity);
	        }else if(defaultColor == 1){ 
	        	setContentView(R.layout.main_music_activity_1);
	        } else{
	        	setContentView(R.layout.main_music_activity_1);
	        } 
		
			instance = this;
			mIntent = getIntent();
			if(mIntent!=null){
				flag = mIntent.getFlags();
			}
			ActivityHolder.getInstance().addActivity(this);
			//
			mTabHost = getTabHost();
			mTabHost.setOnTabChangedListener(this);
//			mRes = getResources();
			mDeleteSection = (LinearLayout) findViewById(R.id.toolbar_section);
			mDeleteToolBarButton = (ToolBarButton) findViewById(R.id.toolbar_btn);
			mDeleteToolBarButton.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.toolbar_delete, 0, 0);
			setupDeleteButtonClickListener();
//			setupTabs();
			mStartupHandler.sendEmptyMessage(0);
			
			//?
			String shuf = getIntent().getStringExtra("autoshuffle");
			if ("true".equals(shuf)) {
				mToken = MusicUtils.bindToService(this, this);
			}
			
			mRadioGroup = (RadioGroup) this.findViewById(R.id.main_radio);
			
		   
			
			allSongs = (RadioButton) findViewById(R.id.radio_button0);
			allSongs.setId(0);
			studyEnglsh = (RadioButton) findViewById(R.id.radio_button1);
			studyEnglsh.setId(1);
			allAlbum = (RadioButton) findViewById(R.id.radio_button2);
			allAlbum.setId(2);
			myFavourite = (RadioButton) findViewById(R.id.radio_button3);
			myFavourite.setId(3); 
			 
			if(defaultColor == 0){ 
				mRadioGroup.setBackgroundResource(R.drawable.title_moren_beijing);
	        } else{
	        	mRadioGroup.setBackgroundResource(R.drawable.tw_bg);  
	        	 
	        }  
	}
	
	
	
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		 
		if(mTabHost!= null){
			TabSpec allSong = mTabHost.newTabSpec(ALL_SONG_TAB);
			allSong.setIndicator(ALL_SONG_TAB);
			allSong.setContent(new Intent(this, TrackBrowserActivity.class));
			mTabHost.addTab(allSong);

			TabSpec hot = mTabHost.newTabSpec(ALL_SONG_TAB);
			hot.setIndicator(ALL_SONG_TAB);
			Intent mIntent =new Intent(this, ShowActivity.class);
			mIntent.setFlags(2);
			hot.setContent(mIntent);
			mTabHost.addTab(hot); 
			
			TabSpec study = mTabHost.newTabSpec(ALL_ALBUM_TAB);
			study.setIndicator(ALL_ALBUM_TAB);
			study.setContent(new Intent(this, AlbumActivity.class));
			mTabHost.addTab(study);
			
			
			TabSpec myFavouite = mTabHost.newTabSpec(ALL_MYFAVOURITE_TAB);
			myFavouite.setIndicator(ALL_MYFAVOURITE_TAB);
			myFavouite.setContent(new Intent(this, PlaylistBrowserActivity.class));
			mTabHost.addTab(myFavouite);

			init();
			if(flag == 0){
				
			}else if(flag == 1){
				mTabHost.setCurrentTab(flag);
//				studyEnglsh.setButtonDrawable(R.drawable.home_btn_bg);
				studyEnglsh.setChecked(true);
				
			}else if(flag == 2){
				mTabHost.setCurrentTab(flag);
//				allAlbum.setButtonDrawable(R.drawable.home_btn_bg);
				allAlbum.setChecked(true);
			}else if(flag == 3){
				
			}
			
			 
		} 
	}




	public void init() {
		mRadioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						switch (checkedId) {
						case 0:
							if(mTabHost != null)
							mTabHost.setCurrentTab(0);
							break;
						case 1:
							if(mTabHost != null)
							mTabHost.setCurrentTab(1);
							break;
						case 2:
							if(mTabHost != null)
							mTabHost.setCurrentTab(2);
							break;
						case 3:
							if(mTabHost != null)
							mTabHost.setCurrentTab(3);
							break;

						}
					}
				});
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
 
    public void onTabChanged(String tabId) {
    	Log.d(TAG, "onTabChanged()==============>tagId=" + tabId);
        Activity activity = getLocalActivityManager().getActivity(tabId);
        if (activity != null) {
            activity.onWindowFocusChanged(true);
        }
        setTitle(tabId);
    }
	
	@Override
	public void finish() {
		Activity a = getLocalActivityManager().getCurrentActivity();
		if ( a instanceof MediaListActivity ) {
			MediaListActivity mla = (MediaListActivity)a;
			if ( mla.isEditState() ) {
				mla.setEditState(false);
				return;
			}
		}
		super.finish();
	}
   
	public void registerDeleteButtonClickListener(OnClickListener l) {
		mDeleteButtonClickListener = l;
	}
	
	public void setDeleteButtonVisibility(int visibility) {
		if ( mDeleteSection != null ) {
			mDeleteSection.setVisibility(visibility);
		}
	}
	
	private void setupDeleteButtonClickListener() {
		if ( mDeleteToolBarButton != null ) {
			mDeleteToolBarButton.setOnClickListener( new View.OnClickListener() {		
				public void onClick(View v) {
					if ( mDeleteButtonClickListener != null ) {
						mDeleteButtonClickListener.onClick(v);
					}
				}
			});
		}
	}
	
	public static void onDestroyWebViewCall(){
		try{
			ShowActivity.onDestroyWebView();
			if(instance!=null)
			instance.finish(); 
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
    
	@Override
	public void onDestroy() {
		if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        } 
		ShowActivity.onDestroyWebView();
		ActivityHolder.getInstance().removeActivity(this);
	
		super.onDestroy();
	} 
	
	@Override
	public void onPause(){
		mStartupHandler.removeCallbacksAndMessages(null);
		super.onPause();  
	}
    
    @Override
	public void onResume(){
		super.onResume();
	}
    
	public void onServiceConnected(ComponentName name, IBinder service) {
		 // we need to be able to bind again, so unbind
        try {
            unbindService(this);
        } catch (IllegalArgumentException e) {
        }
        IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(service);
        if (serv != null) {
            try {
            		serv.shuffleAuto();
            } catch (RemoteException ex) {
            }
        }
	}

	public void onServiceDisconnected(ComponentName name) {		
		Log.i( "MusicPlayer", "MainActivityGroup: Service Disconnected" );
	}
	
	private int getTabIndex(Intent intent){		
		int tag = ALBUM_TAB;
		
		if ( intent == null ) {
			return tag;
		}	
		if ( intent != null ) {
			return flag;
		}	

		String type = intent.getType();
		if ( type == null ) {
			return tag;		
		}

		if (type.equals("vnd.android.cursor.dir/album")) {
			tag = ALBUM_TAB;
		} else if (type.equals("vnd.android.cursor.dir/artistalbum")) {
			tag = ARTIST_TAB;
		} else if (type.equals("vnd.android.cursor.dir/track")) {
			tag = SONG_TAB;
		} else if (type.equals("vnd.android.cursor.dir/playlist")) {
			tag = MYFAVOURITE_TAB;
		}
		return tag;
	}	
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}
 	 
}


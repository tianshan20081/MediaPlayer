package com.weichuang.china.video.local;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.android.china.R;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.video.view.UserPreference;

public class MainLocalVideoActivity extends TabActivity {

	private RadioButton localVideo;
	private RadioButton localFile;
	public TabHost mTabHost;
	public RadioGroup mRadioGroup;
	public static final String LOCAL_ANIMATION = "有图视频";
	public static final String HOT_ANIMATION = "全部视频";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		UserPreference.ensureIntializePreference(this);   
	    int defaultColor = UserPreference.read("defaultColor", 0);  
	    
		if(defaultColor == 0){ 
			setContentView(R.layout.main_local_video_tabl); 
        }else if(defaultColor == 1){
        	setContentView(R.layout.main_local_video_tabl_1); 
        } else{
        	setContentView(R.layout.main_local_video_tabl_1); 
        	
        }  
		localVideo = (RadioButton) findViewById(R.id.radio_button0);
		mRadioGroup = (RadioGroup) this.findViewById(R.id.main_radio);
		localVideo.setId(0);
		localFile = (RadioButton) findViewById(R.id.radio_button1);
		localFile.setId(1);
		mTabHost = this.getTabHost(); 
 
		if(defaultColor == 0){ 
			mRadioGroup.setBackgroundResource(R.drawable.title_moren_beijing);
        }else{
        	mRadioGroup.setBackgroundResource(R.drawable.tw_bg);   
        }  
		
		TabSpec local = mTabHost.newTabSpec(LOCAL_ANIMATION);
		local.setIndicator(LOCAL_ANIMATION);
		local.setContent(new Intent(this, PlayListsActivity.class));
		mTabHost.addTab(local);

		TabSpec hot = mTabHost.newTabSpec(HOT_ANIMATION);
		hot.setIndicator(HOT_ANIMATION);
		hot.setContent(new Intent(this, VideoListActivity.class));
		mTabHost.addTab(hot);
		ActivityHolder.getInstance().addActivity(this);

		init();
		mTabHost.setCurrentTab(0);
//		localVideo.setButtonDrawable(R.drawable.home_btn_bg);
		localVideo.setChecked(true);
	}
//   private long lastTimeonDoubleTap;
	public void init() {
		mRadioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
//						long time = System.currentTimeMillis();
//						if (time - lastTimeonDoubleTap < 300) {
//							return ;
//						}
//						lastTimeonDoubleTap = time;
//						
						switch (checkedId) {
						
						case 0:
							if(mTabHost!=null)
							mTabHost.setCurrentTab(0);
							break;
						case 1:
							if(mTabHost!=null)
							mTabHost.setCurrentTab(1);
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

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		ActivityHolder.getInstance().removeActivity(this);
	}
	
	

}
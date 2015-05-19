package com.weichuang.china.feedback;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.AboutMeActivity;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;

public class FeedbackActivity extends BaseActivity  {
	 private RelativeLayout background_id;
	private EditText nameText;
	private EditText emailText;
	private EditText emailText2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
//		setContentView(R.layout.feedback);
		BaseActivity.mBaseActivity = this;
	    setTopBarTitle("问题反馈"); 
	    setTitleRightButtonHide();
	    setTitleRightButtonBackbound(R.drawable.action_play_list); 
	    ActivityHolder.getInstance().addActivity(this);
		/*
		 * 反馈入口按钮
		 */
//	    TextView  device_information = (TextView)findViewById(R.id. device_information);
//        String deviceInfo = "\t\t您的设备信息如下\n您设备的CPU架构是："+Utils.getDeviceCPUInfo()+"\n是否支持neon指令集："+Utils.isNEONStr+"\n您当前安装的解码器是："+ Utils.checkCurrentEcoder(FeedbackActivity.this)+ "\n该设备建议安装解码器："+Utils.isNEONFit+"\n您的手机型号是："+Utils.getDeviceModel()+"\n您的系统版本是："+Utils.getOSVersion(FeedbackActivity.this)+"\n";
//        device_information.setText(deviceInfo);
//		Button go = (Button) findViewById(R.id.umeng_feedback_golist);
//		go.setOnClickListener(new View.OnClickListener() {
//
//			public void onClick(View v) {
//				UMFeedbackService.setGoBackButtonVisible();
//				UMFeedbackService.openUmengFeedbackSDK(FeedbackActivity.this);
//				 UMFeedbackService.enableNewReplyNotification(FeedbackActivity.this, NotificationType.AlertDialog);
//				Log.i("UIUIUI", ActivityStarter.useGoBackButton?"1":"0");
//			}
//
//		});
		Button gowaps = (Button) findViewById(R.id.waps_feedback);
		gowaps.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				AppConnect.getInstance(FeedbackActivity.this)
				.showFeedback(); 
				
			}
		});
		
	 	UserPreference.ensureIntializePreference(this);  
	    int defaultColor = UserPreference.read("defaultColor", 0); 
	    if(defaultColor == 0){
//	    	go.setBackgroundResource(R.drawable.share_bg);
	    	gowaps.setBackgroundResource(R.drawable.share_bg);
	    }
	}
	
	 private void setBacground(){
		 background_id = (RelativeLayout)findViewById(R.id.background_id); 
		 Utils.setChangeBackground(FeedbackActivity.this, background_id); 
	 }
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK){
//			System.exit(0);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
		BaseActivity.mBaseActivity = this;
		setBacground();
	}


	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.feedback, null);
	}

	@Override
	protected void titleLeftButton() {
		finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}
	@Override
	protected void titlRightButton() {
		// TODO Auto-generated method stub
		AppConnect.getInstance(this).showOffers(this);
		
	}
}
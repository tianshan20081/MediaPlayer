package com.weichuang.china;
 
  
 
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;
public class HelpActivity extends BaseActivity {
	
	 private RelativeLayout background_id;
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);  
	    	BaseActivity.mBaseActivity = this;
	        setTopBarTitle("使用帮助"); 
	        setTitleRightButtonHide();
	        setTitleRightButtonBackbound(R.drawable.action_play_list);
	        background_id = (RelativeLayout)findViewById(R.id.background_id); 
	        Utils.setChangeBackground(HelpActivity.this, background_id);
	        ActivityHolder.getInstance().addActivity(this);
	        TextView tv_describe = (TextView)findViewById(R.id.tv_describe); 
	        
	        TextView  device_information = (TextView)findViewById(R.id. device_information);
	        String deviceInfo = "您设备的CPU架构是："+Utils.getDeviceCPUInfo()+"\n是否支持neon指令集："+Utils.isNEONStr+"\n您的手机型号是："+Utils.getDeviceModel()+"\n您的系统版本是："+Utils.getOSVersion(HelpActivity.this);
	        device_information.setText(deviceInfo);
	       
	        if("Lenovo A60".equals(Build.MODEL)||"Lenovo A60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
	        		"lenovoA60".equals(Build.MODEL)||
	        		"lenovo a60".equals(Build.MODEL)||
	        		"lenovoa60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
		       		"a60".equals(Build.MODEL)){
		    	   tv_describe.setText(R.string.help_describe_a);
	       }else{
	    	   tv_describe.setText(R.string.help_describe);
	       }
	    } 
		@Override
		protected View setCententView() {  
			return  inflater.inflate(R.layout.help_activity, null);
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
		@Override
		protected void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
//			if(this.isFinishing())
//		    setImageView04(R.drawable.btn_more_1);
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
		@Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			BaseActivity.mBaseActivity = this;
		}
		@Override
		public void colorChanged(int color, int flag) {
			// TODO Auto-generated method stub
			
		}
		
		

}

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
public class AboutMeActivity extends BaseActivity {
	
	 private RelativeLayout background_id;
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);  
	    	BaseActivity.mBaseActivity = this;
	        setTopBarTitle("关于"); 
	        setTitleRightButtonHide();
	        setTitleRightButtonBackbound(R.drawable.action_play_list); 
	        ActivityHolder.getInstance().addActivity(this);
	        TextView tv_describe = (TextView)findViewById(R.id.tv_describe); 
	        
	        if("Lenovo A60".equals(Build.MODEL)||"Lenovo A60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
	        		"lenovoA60".equals(Build.MODEL)||
	        		"lenovo a60".equals(Build.MODEL)||
	        		"lenovoa60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
		       		"a60".equals(Build.MODEL)){
		    	   tv_describe.setText(R.string.aboutme_describe_a);
	       }else{
	    	   tv_describe.setText(R.string.aboutme_describe);
	       }
	        
	    } 
	 
	 private void setBacground(){
		 	
			background_id = (RelativeLayout)findViewById(R.id.background_id); 
		    Utils.setChangeBackground(AboutMeActivity.this, background_id); 

	 }
		@Override
		protected View setCententView() {  
//			android:text="@string/aboutme_describe"
			return inflater.inflate(R.layout.about_me, null);
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
			setBacground();
		}
		@Override
		public void colorChanged(int color, int flag) {
			// TODO Auto-generated method stub
			
		} 
}

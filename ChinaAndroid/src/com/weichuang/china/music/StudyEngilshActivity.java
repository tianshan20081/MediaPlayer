package com.weichuang.china.music;
 
  
 
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.video.view.UserPreference;
public class StudyEngilshActivity extends BaseActivity {
	
//	 private TextView tv_copyright;
//	 private TextView tv_describe;
//	 private TextView tv_company;
	 private RelativeLayout background_id;
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);  
	    	BaseActivity.mBaseActivity = this;
	    	setTitleRightButtonHide();
//	        setTopBarTitle("学英语"); 
//	        setTitleRightButtonBackbound(R.drawable.action_play_list);
	    	
	    	//	android:text="@string/study_enghish_describe"
	     
	        UserPreference.ensureIntializePreference(this);  
		    int defaultColor = UserPreference.read("defaultColor", 0);   
			background_id = (RelativeLayout)findViewById(R.id.background_id); 
			if(defaultColor == 0){ 
				background_id.setBackgroundResource(R.drawable.moren_beijing); 
	        }else if(defaultColor == 1){
	        	background_id.setBackgroundResource(R.drawable.moren_beijing1); 
	        }/*else if(defaultColor == 2){
	        	background_id.setBackgroundResource(R.drawable.moren_beijing2); 
	        }else if(defaultColor == 3){
	        	background_id.setBackgroundResource(R.drawable.moren_beijing3); 
	        }*/else{
	    	  background_id.setBackgroundColor(defaultColor); 
	        }
	        ActivityHolder.getInstance().addActivity(this);
	        TextView tv_describe = (TextView)findViewById(R.id.tv_describe); 
	        if("Lenovo A60".equals(Build.MODEL)||"Lenovo A60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
	        		"lenovoA60".equals(Build.MODEL)||
	        		"lenovo a60".equals(Build.MODEL)||
	        		"lenovoa60".equals(Build.MODEL)||
	        		"Lenovo A60".equals(Build.MODEL)||
		       		"a60".equals(Build.MODEL)){
		    	   tv_describe.setText(R.string.study_enghish_describe_a);
	       }else{
	    	   tv_describe.setText(R.string.study_enghish_describe);
	       }
//	        tv_describe =(TextView)findViewById(R.id.tv_describe); 
//	        tv_company =(TextView)findViewById(R.id.tv_company); 
//	        tv_copyright = (TextView)findViewById(R.id.tv_copyright);
//		    tv_describe.setText(getText(R.string.help_describe));
//	        tv_company.setText(getText(R.string.str_company));
//	        tv_copyright.setText(getText(R.string.str_copyright));
		  
	    } 
	   
		@Override
		protected View setCententView() {   
				View view = inflater.inflate(R.layout.study_english_activity, null); 
			return view;
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

}

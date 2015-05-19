package com.weichuang.china.setinfo;
 
  
 
import android.app.Dialog;
import android.content.Intent;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.android.china.R;
import com.mobclick.android.MobclickAgent;
import com.waps.AppConnect;
import com.weichuang.china.AboutMeActivity;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.ColorPickerDialog;
import com.weichuang.china.HelpActivity;
import com.weichuang.china.music.MusicMainActivity;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;
public class SetActivity extends BaseActivity {
	 
	 private RelativeLayout background_id;
	 private RelativeLayout set_button_syte;
	 private RelativeLayout net_video_rl;
	 private RelativeLayout local_video_rl;
	 private RelativeLayout local_music_rl;
	 private RelativeLayout study_english_rl;
	 private RelativeLayout about_me_rl;
	 private RelativeLayout update_version;
	 private RelativeLayout about_me; 
	 private Paint mPaint;
	 private MaskFilter mEmboss;
	 private MaskFilter mBlur;
	 private  int caidan ;
	 private DisplayMetrics dm ;
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);   
	    	BaseActivity.mBaseActivity = this;
    	    dm = new DisplayMetrics(); 
            getWindowManager().getDefaultDisplay().getMetrics(dm); 
	    	ActivityHolder.getInstance().addActivity(this);
	        setTopBarTitle("设置"); 
	        setTitleRightButtonHide();
	        setTitleRightButtonBackbound(R.drawable.action_play_list);
	        initButton(); 
	        UserPreference.ensureIntializePreference(this);  
	        Utils.setChangeBackground(SetActivity.this, background_id);    
		    caidan = UserPreference.read("caidan", 0); 
		    int defaultColor = UserPreference.read("defaultColor", 0);  
	        mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setDither(true); 
			if (defaultColor == 0) {
				mPaint.setColor(Color.TRANSPARENT);
			} else {
				mPaint.setColor(defaultColor);
			}
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(12);
			
			
			mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);
			mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
	 }
	 
	 
	 @Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart(); 

	}



	private void initButton(){
		 set_button_syte = (RelativeLayout)findViewById(R.id.set_button_syte);
		 set_button_syte.setOnClickListener(myOnClick);
		 net_video_rl = (RelativeLayout)findViewById(R.id.net_video_rl);
		 net_video_rl.setOnClickListener(myOnClick);
		 local_video_rl = (RelativeLayout)findViewById(R.id.local_video_rl);
		 local_video_rl.setOnClickListener(myOnClick);
		 local_music_rl = (RelativeLayout)findViewById(R.id.local_music_rl);
		 local_music_rl.setOnClickListener(myOnClick);
		 study_english_rl = (RelativeLayout)findViewById(R.id.study_english_rl);
		 study_english_rl.setOnClickListener(myOnClick);
		 about_me_rl = (RelativeLayout)findViewById(R.id.about_me_rl);
		 about_me_rl.setOnClickListener(myOnClick);
		 
		 update_version = (RelativeLayout)findViewById(R.id.update_version);
		 update_version.setOnClickListener(myOnClick);
		 
		 about_me = (RelativeLayout)findViewById(R.id.about_me);
		 about_me.setOnClickListener(myOnClick);
		 
		 background_id = (RelativeLayout)findViewById(R.id.background_id);
		 
	 }
	 
	 private OnClickListener myOnClick = new OnClickListener() {
		 
		public void onClick(View v) {
		
			if(v == set_button_syte ){//设置按钮风格 
			  
				Dialog myDialog = new ColorPickerDialog(SetActivity.this,
						SetActivity.this, caidan,true,dm.widthPixels,dm.heightPixels);
				myDialog.show();
			}else if(v == net_video_rl){//设置背景风格
				Dialog myDialog = new ColorPickerDialog(
						SetActivity.this, SetActivity.this, caidan,false,dm.widthPixels,dm.heightPixels);
				myDialog.show();
				
			}else if(v == local_video_rl){//视频播放器设置
				Intent i = new Intent(SetActivity.this,SettingActivity.class);
				SetActivity.this.startActivity(i);
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}else if(v == local_music_rl){//音乐播放器设置
				Intent i = new Intent(SetActivity.this,SettingActivity.class);
				SetActivity.this.startActivity(i);
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}else if(v == study_english_rl){//学英语功能简介
				Intent jme = new Intent(SetActivity.this, MusicMainActivity.class);
				jme.setFlags(1);
				startActivity(jme);
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}else if(v == about_me_rl){//使用说明
				Intent i = new Intent(SetActivity.this,HelpActivity.class);
				SetActivity.this.startActivity(i);
				overridePendingTransition(R.anim.fade, R.anim.hold);
				
			}else if(v == about_me){//关于我们
				Intent i = new Intent(SetActivity.this,AboutMeActivity.class);
				SetActivity.this.startActivity(i);
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}else if(v == update_version){
				//版本更新
				MobclickAgent.update(SetActivity.this);
				AppConnect.getInstance(SetActivity.this).checkUpdate();
				SetActivity.this.finish();
			}
		}
	}; 
	 
		@Override
		protected View setCententView() {  
			return inflater.inflate(R.layout.set_activity, null);
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

		/**
		 * 
		 * 设置背景颜色
		 * */
		@Override
		public void colorChanged(int color, int flag) {
			System.out.println("color === "+color + "       flag ==="+flag);
			if (flag == 0) {// 0标示设置背景  
				mPaint.setColor(color); 
				if(color == 0){ 
					background_id.setBackgroundResource(R.drawable.moren_beijing); 
		        }else if(color == 1){ 
		        	background_id.setBackgroundResource(R.drawable.moren_beijing1);  
		        } else{
		    	   background_id.setBackgroundColor(color); 
		        }
				setTitleBarChangeBackground(color);
			} else if (flag == 1) { 
			}

		} 

}

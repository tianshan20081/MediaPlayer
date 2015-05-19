package com.weichuang.china;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.china.R;
import com.mobclick.android.MobclickAgent;
import com.waps.AppConnect;
import com.weichuang.china.feedback.FeedbackActivity;
import com.weichuang.china.music.MusicMainActivity;
import com.weichuang.china.radia.RadiaActivity;
import com.weichuang.china.setinfo.SetActivity;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.share.ShareActivity;
import com.weichuang.china.tv.TVActivity;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.local.MainLocalVideoActivity;
import com.weichuang.china.video.net.NetVideoListActivity;
import com.weichuang.china.video.net.PlayStreamingMediaActivity;
import com.weichuang.china.video.net.ShowActivity;
import com.weichuang.china.video.view.UserPreference;

public class MainActivity extends BaseActivity implements OnClickListener
		 {
	protected static final int DIALOG_ABOUNT = 1005;
	private Dialog reNameDialog;
	private RelativeLayout background_id;
	private RelativeLayout netTvRl= null ;
	private RelativeLayout netVideoRl = null;
	private RelativeLayout localVideoRl = null;
	private RelativeLayout localMusicRl = null;;
	private RelativeLayout studyEnglishRl = null;
	private RelativeLayout aboutMeRl = null;
	private RelativeLayout aboutMoreRl = null;
	private RelativeLayout appPushRl = null;
	private ScrollView mScrollview;
//	private LinearLayout popwindow_layout;
	
//	private String xingzhuang = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState); 
//友盟
		MobclickAgent.update(this);
		MobclickAgent.onError(this);
		
		
		UserPreference.ensureIntializePreference(MainActivity.this);   
		ActivityHolder.getInstance().addActivity(this);
//		setContentView(R.layout.main_activity);
		Utils.initCacheFile(MainActivity.this);
		Utils.intCPUInfo();
		setTopBarTitle(getString(R.string.app_name)); 
		setTitleLeftButtonHide();
//		setTitleRightButtonHide();
//	    setTitleRightButtonBackbound(R.drawable.bottom_tab_myfavourite);
	    setTitleRightButtonBackbound(R.drawable.action_play_list); 
		initView();
		BaseActivity.mBaseActivity = this;

	}


	private void initView() {
		backNum = 0;
		mScrollview = (ScrollView) findViewById(R.id.scrollview);
		 
		netTvRl = (RelativeLayout) findViewById(R.id.net_tv_rl);
		netTvRl.setOnClickListener(this);
		netTvRl.setOnTouchListener(ontouch);
		
		netVideoRl = (RelativeLayout) findViewById(R.id.net_video_rl);
		netVideoRl.setOnClickListener(this);
		netVideoRl.setOnTouchListener(ontouch);
		
		localVideoRl = (RelativeLayout) findViewById(R.id.local_video_rl);
		localVideoRl.setOnClickListener(this);
		localVideoRl.setOnTouchListener(ontouch);
		
		localMusicRl = (RelativeLayout) findViewById(R.id.local_music_rl);
		localMusicRl.setOnClickListener(this);
		localMusicRl.setOnTouchListener(ontouch);
		
		studyEnglishRl = (RelativeLayout) findViewById(R.id.study_english_rl);
		studyEnglishRl.setOnClickListener(this);
		studyEnglishRl.setOnTouchListener(ontouch);
		
		
		aboutMeRl = (RelativeLayout) findViewById(R.id.about_me_rl);
		aboutMeRl.setOnClickListener(this);
		aboutMeRl.setOnTouchListener(ontouch);
		
		
		aboutMoreRl = (RelativeLayout) findViewById(R.id.about_more_rl);
		aboutMoreRl.setOnClickListener(this);
		aboutMoreRl.setOnTouchListener(ontouch);
		
		appPushRl = (RelativeLayout) findViewById(R.id.app_push_rl);
		appPushRl.setOnClickListener(this);
		appPushRl.setOnTouchListener(ontouch); 
		
		Button button0 = (Button)findViewById(R.id.net_tv_btn);
		button0.setOnClickListener(this);
		button0.setOnTouchListener(ontouch);
		Button button1 = (Button)findViewById(R.id.net_video_btn);
		button1.setOnClickListener(this);
		button1.setOnTouchListener(ontouch);
		Button button2 = (Button)findViewById(R.id.local_video_btn);
		button2.setOnClickListener(this);
		button2.setOnTouchListener(ontouch);
		Button button3 = (Button)findViewById(R.id.local_music_btn);
		button3.setOnClickListener(this);
		button3.setOnTouchListener(ontouch);
		Button button4 = (Button)findViewById(R.id.study_english_btn);
		button4.setOnClickListener(this);
		button4.setOnTouchListener(ontouch);
		Button button5 = (Button)findViewById(R.id.about_me_btn);
		button5.setOnClickListener(this);
		button5.setOnTouchListener(ontouch);
		
		Button button6 = (Button)findViewById(R.id.about_more_btn);
		button6.setOnClickListener(this);
		button6.setOnTouchListener(ontouch);
		
		Button button7 = (Button)findViewById(R.id.app_push_btn);
		button7.setOnClickListener(this);
		button7.setOnTouchListener(ontouch);  
		
//		popwindow_layout =(LinearLayout)findViewById(R.id.popwindow_layout);
	}
	
	
    @Override
	protected void onStart() { 
		super.onStart(); 
		setBackground();
 
	}

    
    private void setBackground(){
    	try{
    		UserPreference.ensureIntializePreference(this); 
    		
//    		 System.out.println("xingzhuang MainActivity()()()=="+xingzhuang); 
    		background_id = (RelativeLayout) findViewById(R.id.background_id); 
    		Utils.setChangeBackground(MainActivity.this, background_id);
    		View view[] = new View[8]; 
    		view[0] = netTvRl;
    		view[1] = netVideoRl;
    		view[2] = localVideoRl;
    		view[3] = localMusicRl;
    		view[4] = studyEnglishRl;
    		view[5] = aboutMeRl;
    		view[6] = aboutMoreRl;
    		view[7] = appPushRl;
    		Utils.setChangeButtonBackground(MainActivity.this, view);
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
    }
    
   
	private int backNum = 0;
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			backNum ++;
			if(backNum==1){
				Utils.showToast(MainActivity.this, "再按一下返回键退出程序！", 0);
				return true ;
			}else if(backNum==2){
				MainActivity.this.finish();// 关闭activity
				AppConnect.getInstance(this).finalize();
				overridePendingTransition(R.anim.fade, R.anim.hold);
				ActivityHolder.getInstance().finishAllActivity();
				return true;
			} 
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ABOUNT:
			final View aboutView = LayoutInflater.from(this).inflate(
					R.layout.about_me, null);
			return new AlertDialog.Builder(this).setTitle(R.string.str_about)
					.setView(aboutView).setNegativeButton(R.string.str_ikown,
							null).create();
		}
		return super.onCreateDialog(id);
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		ActivityHolder.getInstance().removeActivity(this);
	}

	@Override
	protected View setCententView() {
		return inflater.inflate(R.layout.main_activity, null);
	}

	@Override
	protected void titleLeftButton() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void titlRightButton() {
		Intent inten = new Intent(MainActivity.this,ShareActivity.class);
		startActivity(inten);

	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		  
			return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		     showDialog();
			return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		
		setBackground();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		backNum = 0; 
		BaseActivity.mBaseActivity = this;
		mScrollview.smoothScrollTo(0, 0);
		

	}
	
	private OnTouchListener ontouch = new OnTouchListener() {
		
		
		public boolean onTouch(View v, MotionEvent event) {
			System.out.println("ontouch---------"); 
			 UserPreference.ensureIntializePreference(MainActivity.this);
			 int caidanss = UserPreference.read("caidan", 0);//菜单颜色设置
			switch(v.getId()){
			case R.id.net_tv_rl:
				setButtonState(netTvRl,event, caidanss);
				break;
			case R.id.net_tv_btn:
				setButtonState(netTvRl,event, caidanss);
				break;
				///////////
			case R.id.net_video_rl:
				setButtonState(netVideoRl,event, caidanss);
				break;
			case R.id.net_video_btn:
				setButtonState(netVideoRl,event, caidanss);
				break;
				//////////
			case R.id.local_video_rl:
				setButtonState(localVideoRl,event, caidanss);
				break; 
			case R.id.local_video_btn:
				setButtonState(localVideoRl,event, caidanss);
				break;
			case R.id.local_music_rl:
				setButtonState(localMusicRl,event, caidanss);
				break;
			case R.id.local_music_btn:
				setButtonState(localMusicRl,event, caidanss);
				break;
				
			case R.id.study_english_rl:
				setButtonState(studyEnglishRl,event, caidanss);
				break;
			case R.id.study_english_btn:
				setButtonState(studyEnglishRl,event, caidanss);
				break; 
			case R.id.about_me_rl:
				setButtonState(aboutMeRl,event, caidanss);
				break;
			case R.id.about_me_btn:
				setButtonState(aboutMeRl,event, caidanss);
				break;
			
				/////////
			case R.id.about_more_rl:
				setButtonState(aboutMoreRl,event, caidanss);
				break;
			case R.id.about_more_btn:
				setButtonState(aboutMoreRl,event, caidanss);
				break; 
				/////
			case R.id.app_push_rl:
				setButtonState(appPushRl,event, caidanss);
				break;
			case R.id.app_push_btn:
				setButtonState(appPushRl,event, caidanss);
				break;
			}
			
			return false;
		}

		
	};
	private void setButtonState(View v,MotionEvent event, int caidanss) 
	{ 
		String xingzhuang = UserPreference.read("xiangzhuang", null);
		if(event.getAction() ==MotionEvent.ACTION_DOWN){ //点击下的效果
			if("正方形".equals(xingzhuang)){ 
		  		Utils.color[0] = this.getResources().getColor(R.color.bg);
		  		Utils.color[1] = this.getResources().getColor(R.color.bg);
	  			v.setBackgroundDrawable(Utils.setBackgroundType("正方形")); 
	  		}else if("半圆形".equals(xingzhuang)){
	  			Utils.color[0] = this.getResources().getColor(R.color.bg);
	  			Utils.color[1] = this.getResources().getColor(R.color.bg);
	  			v.setBackgroundDrawable(Utils.setBackgroundType("半圆形"));  
	  		}else if("圆形".equals(xingzhuang)){
	  			Utils.color[0] = this.getResources().getColor(R.color.bg);
	  			Utils.color[1] = this.getResources().getColor(R.color.bg);
	  			v.setBackgroundDrawable(Utils.setBackgroundType("圆形")); 	 
	  		} 
		}else if(event.getAction() ==MotionEvent.ACTION_UP){
			if(caidanss == 0){ //表示默认的
				Utils.color[0]  = this.getResources().getColor(R.color.lans);
				Utils.color[1]  = this.getResources().getColor(R.color.lans);
				if("正方形".equals(xingzhuang)){  
		  			v.setBackgroundDrawable(Utils.setBackgroundType("正方形")); 
		  		}else if("半圆形".equals(xingzhuang)){ 
		  			v.setBackgroundDrawable(Utils.setBackgroundType("半圆形"));
		  		}else if("圆形".equals(xingzhuang)){
		  			v.setBackgroundDrawable(Utils.setBackgroundType("圆形")); 	 
		  		} 
			}else{
				Utils.color[0] = caidanss;
				Utils.color[1] = caidanss;
				v.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang));
			}
		}else if(event.getAction() ==MotionEvent.ACTION_MOVE){
			if(caidanss == 0){ //表示默认的
				Utils.color[0]  = this.getResources().getColor(R.color.lans);
				Utils.color[1]  = this.getResources().getColor(R.color.lans);
				if("正方形".equals(xingzhuang)){ 
		  			v.setBackgroundDrawable(Utils.setBackgroundType("正方形")); 
		  		}else if("半圆形".equals(xingzhuang)){ 
		  			v.setBackgroundDrawable(Utils.setBackgroundType("半圆形"));
		  		}else if("圆形".equals(xingzhuang)){
		  			v.setBackgroundDrawable(Utils.setBackgroundType("圆形")); 	 
		  		} 
			}else{
				Utils.color[0] = caidanss;
				Utils.color[1] = caidanss;
				v.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang));
			}
		}
	} 
	
	
	
	private boolean isNetCheckDialog = false;
	public  void netCheckDialog() {// 退出确认
		AlertDialog.Builder ad = new AlertDialog.Builder(BaseActivity.mBaseActivity);
		ad.setTitle("提示");
		ad.setMessage("网络不可用，请检查网络再试");
		ad.setPositiveButton("确定", new DialogInterface.OnClickListener() {// 退出按钮
		
					public void onClick(DialogInterface dialog, int i) {
						isNetCheckDialog = false;
					}
				});
		
		ad.show();
		
	}


public void onClick(View v) {
		switch (v.getId()) {

		case R.id.net_tv_rl:

			startNetTV();
			break;
		case R.id.net_tv_btn:
			startNetTV();
			break;
			///////////
		case R.id.net_video_rl:
			startNetVideo();
			break;
		case R.id.net_video_btn:
			startNetVideo();
			break;
			//////////
		case R.id.local_video_rl:
			startLacalVideo();
			break; 
		case R.id.local_video_btn:
			startLacalVideo();
			break;
		case R.id.local_music_rl:
			startLocaMusic();
			break;
		case R.id.local_music_btn:
			startLocaMusic();
			break;
			
		case R.id.study_english_rl:
			if(Utils.isCheckNetAvailable(MainActivity.this)){
				startStudyEnglish();
			}else{
				Toast.makeText(MainActivity.this, "网络不可用，请检查网络再试", 0).show();
//				if(!isNetCheckDialog){
////					isNetCheckDialog = true;
//					netCheckDialog();
//					
//					
//				}
			}
			
//			startRadiaActivity();
			break;
		case R.id.study_english_btn:
			if(Utils.isCheckNetAvailable(MainActivity.this)){
				startStudyEnglish();
			}else{
				
				Toast.makeText(MainActivity.this, "网络不可用，请检查网络再试", 0).show();
//				if(!isNetCheckDialog){
////					isNetCheckDialog = true;
//					netCheckDialog();
//				}
			}
//			startRadiaActivity();
			break; 
			/////
		case R.id.about_me_rl:
//			startHelp();
			startRadiaActivity();
			break;
		case R.id.about_me_btn:
//			startHelp();
			startRadiaActivity();
			break;
		
			/////////
		case R.id.about_more_rl:
			showDialog();;
			break;
		case R.id.about_more_btn:
			showDialog();
			break; 
			/////
		case R.id.app_push_rl:
			AppConnect.getInstance(MainActivity.this).showOffers(
					MainActivity.this);
			break;
		case R.id.app_push_btn:
			AppConnect.getInstance(MainActivity.this).showOffers(
					MainActivity.this);
			break;
		

		default:
			break;
		}

	}

	private void startNetTV() {
		Intent h = new Intent(MainActivity.this, TVActivity.class);
		startActivity(h);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	private void startNetVideo() {
		Intent i = new Intent(MainActivity.this, NetVideoListActivity.class);
		startActivity(i);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	private void startLacalVideo() {
		Intent j = new Intent(MainActivity.this, MainLocalVideoActivity.class);
		startActivity(j);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	private void startLocaMusic() { 
		Intent jme = new Intent(MainActivity.this, MusicMainActivity.class);
		jme.setFlags(2);
		startActivity(jme);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}
	private void startStudyEnglish() {
		
//		Intent jme = new Intent(MainActivity.this, BrowserActivity.class);
		Intent jme = new Intent(MainActivity.this, ShowActivity.class);
		Bundle bundle = new Bundle();
		VideoInfo videoInfo = new VideoInfo();
		videoInfo.setTitle("hao123导航");
		videoInfo.setUrl("http://m.hao123.com/");
		bundle.putSerializable("VideoInfo", videoInfo);
//		bundle.put
//		jme.putExtras(bundle);
		jme.putExtra("extra", bundle);
		jme.setFlags(1);
		startActivity(jme);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}
	
	private void startRadiaActivity() {
		Intent jme = new Intent(MainActivity.this, RadiaActivity.class);
		startActivity(jme);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	private void startAboutMe() {
		Intent aboutMe = new Intent(MainActivity.this,AboutMeActivity.class);
		startActivity(aboutMe);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}
	
	private void startHelp() {
		Intent aboutMe = new Intent(MainActivity.this,HelpActivity.class);
		startActivity(aboutMe);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		System.out.println("iwreuuuuuuuuuuuu-----------------");
		// TODO Auto-generated method stub
		if (reNameDialog != null /* && reNameDialog.isShowing() */) {
			reNameDialog.dismiss();
		}
		
		return super.onTouchEvent(event);

	}

	private PopupWindow pw = null;
	private  int caidan ;
	private void showDialog() {
		RelativeLayout setmenu;
		RelativeLayout set;
		RelativeLayout play_net_uri;
		RelativeLayout feedback; 
		RelativeLayout help_id;
		UserPreference.ensureIntializePreference(MainActivity.this);  
	
		final View myView;
		myView = getLayoutInflater().inflate(
				R.layout.moreactivitydialog, null); 
		FrameLayout linear = (FrameLayout) myView .findViewById(R.id.more_bg_linear);
	    pw = new PopupWindow(myView, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT, true);
		pw.setBackgroundDrawable(new BitmapDrawable());
		pw.setOnDismissListener(new OnDismissListener() {
			public void onDismiss() {
				closePopwindow();
			}
		});
		Utils.setTitleBarChangeBackground(MainActivity.this, linear);  
		pw.showAtLocation(findViewById(R.id.popwindow_layout),    
                   Gravity.BOTTOM, 0, 0);    
		setmenu = (RelativeLayout) myView.findViewById(R.id.setmenu); 
		set = (RelativeLayout) myView.findViewById(R.id.set);
		play_net_uri = (RelativeLayout) myView.findViewById(R.id.play_net_uri);
		feedback = (RelativeLayout) myView.findViewById(R.id.feedback);
		help_id = (RelativeLayout)myView.findViewById(R.id.help_id);
		myView.findViewById(R.id.settingbutton).setOnClickListener(
				new OnClickListener() { 
					public void onClick(View v) {  
						startAboutMe(); 
						closePopwindow();
					}
				});
		myView.findViewById(R.id.playliumeiti).setOnClickListener(
				new OnClickListener() { 
					public void onClick(View v) {
						startPlayNatActivity() ;
						closePopwindow();
					}
				});

		set.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				startAboutMe();
				closePopwindow();
			}
		});

		play_net_uri.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				startPlayNatActivity();
				closePopwindow();
			}  
		});

		feedback.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				AppConnect.getInstance(MainActivity.this).showFeedback(); 
				closePopwindow();
			}
		});
		myView.findViewById(R.id.feedbackButton).setOnClickListener(
				new OnClickListener() { 
					public void onClick(View v) {
//						AppConnect.getInstance(MainActivity.this)
//								.showFeedback(); 
						Intent inten = new Intent(MainActivity.this,FeedbackActivity.class);
						startActivity(inten);
						closePopwindow();
					}
				});
		setmenu.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) { 
				Intent intentSet = new Intent(MainActivity.this,SetActivity.class);
				startActivity(intentSet);
				MainActivity.this.overridePendingTransition(R.anim.fade, R.anim.hold); 
				closePopwindow();
			}
		});
		myView.findViewById(R.id.settingbutton_meun).setOnClickListener(
				new OnClickListener() { 
					public void onClick(View v) { 
						Intent intentSet = new Intent(MainActivity.this,SetActivity.class);
						startActivity(intentSet);
						MainActivity.this.overridePendingTransition(R.anim.fade, R.anim.hold);
						closePopwindow();
					}
				}); 
		help_id.setOnClickListener(new OnClickListener() {//帮助
			
			@Override
			public void onClick(View v) {
				startHelp();
				closePopwindow();
			}
		});
		myView.findViewById(R.id.help_button).setOnClickListener(new OnClickListener() {//帮助
			
			@Override
			public void onClick(View v) {
				startHelp();
				closePopwindow();
			}
		});
		
		myView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closePopwindow();
			}
		});

	} 
	
	private void closePopwindow(){
		if(pw !=null && pw.isShowing()){
			pw.dismiss();
		}
	}
	

	private void startPlayNatActivity() {
		Intent i = new Intent(MainActivity.this, PlayStreamingMediaActivity.class);
		MainActivity.this.startActivity(i);
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}
}

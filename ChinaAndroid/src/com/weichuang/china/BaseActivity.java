package com.weichuang.china;
 
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.android.china.R;
import com.mobclick.android.MobclickAgent;
import com.waps.AppConnect;
import com.weichuang.china.video.view.UserPreference;

/**
 * 
 * @author 阿福
 *
 */
public abstract class BaseActivity extends Activity implements ColorPickerDialog.OnColorChangedListener{
	public static Context mBaseActivity = null;
	private View centerView = null; 
	protected LinearLayout layout_center = null;  
	protected LayoutInflater inflater = null;
	protected LayoutParams layoutParams = null;
    private Paint  mPaint = null;
    private TextView title_text;
    private Button titleBarLeftButton = null; 
    private Button titleBarRightButton = null; 
    private FrameLayout titleBar;//标题栏的背景
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.baseactivity); 
		  //万普广告
		try{
			AppConnect.getInstance("3f42a4b02948a51cc987af6b833d1720", "WAPS", this);
//			AppConnect.getInstance(this);
		}catch(Exception e){
			e.printStackTrace();
		}
			
		initParent(); 
		title_text = (TextView)findViewById(R.id.title_text);
		titleBarRightButton =(Button)findViewById(R.id.title_change_list); 
		titleBarLeftButton =(Button)findViewById(R.id.title_search); 
	    titleBarLeftButton.setOnClickListener(listener); 
		titleBarRightButton.setOnClickListener(listener);  
		layout_center = (LinearLayout) this.findViewById(R.id.linear_cont);   
		titleBar =(FrameLayout)findViewById(R.id.titlebar);  
	    centerView = setCententView(); 
        if (centerView != null) {  
		 layout_center.addView(centerView, layoutParams);  
		}  
    
       
	}
	
	
	 
	private void initParent() {  
		  inflater = LayoutInflater.from(this);
		  DisplayMetrics dm = new DisplayMetrics();       
		  getWindowManager().getDefaultDisplay().getMetrics(dm);     
		  int height = 0;
//		  int widthPixels= dm.widthPixels;      
		  int heightPixels= dm.heightPixels; 
		  height = heightPixels - 60;
		  layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, height);
		  
	}
	private OnClickListener listener = new OnClickListener() { 
		public void onClick(View view) {
			int id = view.getId();
			switch (id) { 
			case R.id.title_search:
				titleLeftButton();
				break;
			case R.id.title_change_list:
				titlRightButton();
				break;
			}
		}
	};
	
	/**
	 * 标题栏的左按钮
	 * */
	protected abstract void titleLeftButton() ;
	/**
	 * 标题栏的右按钮
	 * */
	protected abstract void titlRightButton() ;
	
	
	/**
	 * 标题栏的左按钮内容
	 * */
	protected void titleLeftButtonText(String text){
		titleBarLeftButton.setText(text);
	}
	/**
	 * 标题栏的右按钮内容
	 * */
	protected void titlRightButtonText(String text){
		titleBarRightButton.setText(text);
	}
	 
	
	/**
	 * 
	 * 设置背景颜色
	 * */
	 
	public void colorChanged(int color,int flag) { 
		if(flag == 0){
	        if(color == 0){ 
	        	mPaint.setColor(color);
	        	centerView.setBackgroundResource(R.drawable.bg); 
	        } else{
	        	mPaint.setColor(color);
	    		centerView.setBackgroundColor(color);
	        }
		}else if(flag == 1){
			
		}
		
	}
	
	 /**
	  * 获取每个界面的布局
	  * */
	protected abstract View setCententView();
	
	/**
	 * 设置标题内容
	 * */
	protected void setTopBarTitle(String title){
		title_text.setText(title);
	}
	
	/**
	 * 设置标题文字大小
	 * */
	protected void setTopBarTitleSize(float size){
		title_text.setTextSize(size);
	}
	
	/**
	 * 设置标题宽
	 * */
	protected void setTopBarTitleWidth(int pixels){
		title_text.setWidth(pixels);
	}
	
	/**
	 * 设置左标题按钮的背景
	 * */
	protected void setTitleLeftButtonBackbound(int re){
		titleBarLeftButton.setBackgroundResource(re);
	}
	
	/**
	 * 设置右标题按钮的背景
	 * */
	protected void setTitleRightButtonBackbound(int re){
		titleBarRightButton.setBackgroundResource(re);
	}
	
	/**
	 * 设置左标题按钮的隐藏
	 * */
	protected void setTitleLeftButtonHide(){
		titleBarLeftButton.setVisibility(View.GONE);
	}
	
	/**
	 * 设置右标题按钮的隐藏
	 * */
	protected void setTitleRightButtonHide(){
		titleBarRightButton.setVisibility(View.GONE);
	}

	@Override
	protected void onResume() {
//		UserPreference.ensureIntializePreference(this);  
//        int defaultColor = UserPreference.read("defaultColor", 0);
//        Utils.setTitleBarChangeBackground(BaseActivity.this,titleBar); 
        MobclickAgent.onResume(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		MobclickAgent.onPause(this);
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreatePanelMenu(featureId, menu);
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		UserPreference.ensureIntializePreference(BaseActivity.this);  
	    int defaultColor = UserPreference.read("defaultColor", 0);  
		setTitleBarChangeBackground(defaultColor);
	}
	 
	/**
	 * 标题背景设置
	 * */
	public void setTitleBarChangeBackground( int defaultColor){ 
	   
		if(defaultColor == 0){//默认标题背景设置
			titleBar.setBackgroundResource(R.drawable.title_moren_beijing);	
		}else if(defaultColor == 1){
			titleBar.setBackgroundResource(R.drawable.top_bg);	
		} else{
			titleBar.setBackgroundResource(R.drawable.background_bottom);	
		}
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

}

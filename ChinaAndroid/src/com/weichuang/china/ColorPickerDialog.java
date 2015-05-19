/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weichuang.china;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.china.R;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;

public class ColorPickerDialog extends AlertDialog {

    public interface OnColorChangedListener {
        void colorChanged(int color , int flag);
    } 
    private class ColorPickerView extends View {
        private Paint mPaint;
        private Paint mCenterPaint;
        private final int[] mColors;
        private OnColorChangedListener mListener;
        
        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;
            mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
            };
            Shader s = new SweepGradient(0, 0, mColors, null); 
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(s);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(32); 
            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
            
        }
        
        private boolean mTrackingCenter;
        private boolean mHighlightCenter;

        @Override 
        protected void onDraw(Canvas canvas) {
            float r = CENTER_X - mPaint.getStrokeWidth()*0.5f;
            
            canvas.translate(CENTER_X, CENTER_X); 
            canvas.drawOval(new RectF(-r, -r, r, r), mPaint);            
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint); 
            if (mTrackingCenter) {
                int c = mCenterPaint.getColor();
                mCenterPaint.setStyle(Paint.Style.STROKE); 
                if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
                } else {
                mCenterPaint.setAlpha(0x80);
                }
                canvas.drawCircle(0, 0, CENTER_RADIUS + 
                mCenterPaint.getStrokeWidth(), mCenterPaint); 
                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setColor(c);
            }
        }
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(CENTER_X*2, CENTER_Y*2);
        } 
     
        private int ave(int s, int d, float p) {
            return s + java.lang.Math.round(p * (d - s));
        }
        
        private int interpColor(int colors[], float unit) {
            if (unit <= 0) {
                return colors[0];
            }
            if (unit >= 1) {
                return colors[colors.length - 1];
            }
            
            float p = unit * (colors.length - 1);
            int i = (int)p;
            p -= i;

            // now p is just the fractional part [0...1) and i is the index
            int c0 = colors[i];
            int c1 = colors[i+1];
            int a = ave(Color.alpha(c0), Color.alpha(c1), p);
            int r = ave(Color.red(c0), Color.red(c1), p);
            int g = ave(Color.green(c0), Color.green(c1), p);
            int b = ave(Color.blue(c0), Color.blue(c1), p); 
            return Color.argb(a, r, g, b);
        }
        
        
        private static final float PI = 3.1415926f;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X;
            float y = event.getY() - CENTER_Y;
            boolean inCenter = java.lang.Math.sqrt(x*x + y*y) <= CENTER_RADIUS;
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        float angle = (float)java.lang.Math.atan2(y, x);
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle/(2*PI);
                        if (unit < 0) {
                            unit += 1;
                        }
                        colors = interpColor(mColors, unit);
                        mCenterPaint.setColor(colors);
                        invalidate();
                    }
                    if(isBackgound){//自定义软件风格
	                    if(flag == 1){
	                    	if(mCenterPaint != null){
	                    		Utils.color[0] = mCenterPaint.getColor();
	                    		Utils.color[1] = mCenterPaint.getColor();
	                    	}
	                    
	                    	mInitialColor = mCenterPaint.getColor();
	             			button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	
	                 		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
	                 		button_default.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
	                 		zhenfangxing.setBackgroundDrawable(Utils.setBackgroundType("正方形")); 	
	                 		banyuanxing.setBackgroundDrawable(Utils.setBackgroundType("半圆形")); 
	                 		yuanxing.setBackgroundDrawable(Utils.setBackgroundType("圆形")); 	
	                    }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackingCenter) {
                        if (inCenter) { 
                        	 if(isBackgound){//自定义软件风格 
                        		 mListener.colorChanged(mCenterPaint.getColor(),flag);
                        	 }
                       }
                        mTrackingCenter = false;  
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }

    
    private   int CENTER_X = 130;//130;
    private   int CENTER_Y = 130;//;
    private   int CENTER_RADIUS = 32;//; 25
    private boolean isBackgound = false;//背景风格设置
    public ColorPickerDialog(Context context,
             OnColorChangedListener listener, int initialColor,boolean isBackgound,int widthPixels,
             int heightPixels ) {
        super(context); 
        mListener = listener;
        mInitialColor = initialColor; 
        this.isBackgound = isBackgound;
        if(widthPixels == 320 && heightPixels == 480){
        	CENTER_X = 90;
        	CENTER_Y = 90;
        	CENTER_RADIUS = 25;
        }else {
        	CENTER_X = 130;
        	CENTER_Y = 130;
        	CENTER_RADIUS = 32 ;
        } 
    }

    
    
    //defaultColor  = 0 表示木制的背景图片    1表示 --- 2 表示 --  3 表示 -- 背景图图片
    
    protected LinearLayout layout_center;  
    public static int colors;
    private  Button button_sure;
    private  Button button_cancle;
    private  Button  button_default;
    private Button zhenfangxing;//正方形
    private Button banyuanxing;//半圆形
    private Button yuanxing;//圆形
    private   RelativeLayout relative;
    private OnColorChangedListener mListener;
    private int mInitialColor; 
    private int flag;//0 表示背景
    private ColorPickerDialog dialog;
    private String xingzhuang = null;; 
    private int checkFlag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
	        UserPreference.ensureIntializePreference(getContext());
	    	int defaultColor = UserPreference.read("defaultColor", 0); 
	        xingzhuang = UserPreference.read("xiangzhuang", null);
	        checkFlag = UserPreference.read("checkFlag", 0);
	        if(xingzhuang == null){
	        	xingzhuang = "正方形";
	        }  
	        if(isBackgound){//自定义软件风格
			        	//自定义设置 
			        	setContentView(R.layout.dialog_button); 
				        zhenfangxing = (Button) findViewById(R.id.zhenfangxing);
				        banyuanxing = (Button) findViewById(R.id.banyuanxing);
				        yuanxing = (Button) findViewById(R.id.yuanxing);
				        relative = (RelativeLayout)findViewById(R.id.dialog_bg_id); 
				        layout_center = (LinearLayout) this.findViewById(R.id.lay);   
				        button_sure = (Button)findViewById(R.id.sure);
				        button_cancle = (Button) findViewById(R.id.cancel);
				        button_default = (Button) findViewById(R.id.default_id); 
				        TextView textview = (TextView)findViewById(R.id.lay_testview);  
				        RadioGroup radiogroup = (RadioGroup)findViewById(R.id.radiogroup);
				        RadioButton radiobutton01 = (RadioButton)findViewById(R.id.radiobutton01);
				        RadioButton radiobutton02 = (RadioButton)findViewById(R.id.radiobutton02); 
				        dialog = this;  
						textview.setText("自定义软件风格");  
						Utils.setChangeBackground(getContext(), relative);
						 
						if(checkFlag == 0){
							radiobutton01.setChecked(true);	
							flag = 0; 
						 	zhenfangxing.setVisibility(View.GONE);
			        		banyuanxing.setVisibility(View.GONE);
			        		yuanxing.setVisibility(View.GONE);
				        }else if(checkFlag == 1){
				        	radiobutton02.setChecked(true);
				        	flag = 1;
							setCheckButton();
				        }  
		        		if(mInitialColor == 0){//morend
		 	        		mInitialColor = getContext().getResources().getColor(R.color.lans);
		 	        	}
		        		Utils.color[0] =mInitialColor;
		 	         	Utils.color[1] =mInitialColor;
		 	         	button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	
	             		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
	             		button_default.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
			        	radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
							
							@Override
							public void onCheckedChanged(RadioGroup group, int checkedId) {
								 if(checkedId == R.id.radiobutton01){//表示背景 
									 UserPreference.save("checkFlag", 0);
									 flag = 0; 
								 	zhenfangxing.setVisibility(View.GONE);
					        		banyuanxing.setVisibility(View.GONE);
					        		yuanxing.setVisibility(View.GONE);
								 }else if(checkedId == R.id.radiobutton02){ 
									 UserPreference.save("checkFlag", 1);
									 flag = 1;
									 setCheckButton();
								}
							}
						});
			        	 
			        	 OnColorChangedListener l = new OnColorChangedListener() {
				             public void colorChanged(int color,int flag) { 
				            		//点击圆心出发这个方法
				             	if(flag == 0){//背景设置
				 	            	 UserPreference.save("defaultColor", colors);  
				 					 mListener.colorChanged(colors,flag);  
				 					 colors = 0;
				 	                 dismiss();
				             	}else if(flag == 1){ 
				             		Utils.color[0] = color; 
				             		Utils.color[1] = color; 
				             		button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	;
				             		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	;
				             		button_default.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	; 
				             	  	UserPreference.save("caidan", colors);  
				 					mListener.colorChanged(colors,flag);  
				 				    UserPreference.save("xiangzhuang",xingzhuang); 
				 					colors = 0; 
				 					dismiss();
				             	}
		
				             }
				         }; 
			        	layout_center.addView(new ColorPickerView(getContext(), l, mInitialColor));   
			  	        button_sure.setOnClickListener(new Button.OnClickListener() { 
			  				public void onClick(View v) {  
			  					if(flag == 0){//背景设置
			  						 mListener.colorChanged(colors,flag);  
			  						 UserPreference.save("defaultColor", colors);
			  					}else{//按钮设置
			  						 UserPreference.save("xiangzhuang",xingzhuang); 
			  						 if(colors == 0){ 
			  							 mListener.colorChanged(UserPreference.read("caidan", 0),flag);   
			  						}else{
			  							 UserPreference.save("caidan", colors);  
			  		 					 mListener.colorChanged(colors,flag);   
			  						}  
			  						 colors = 0; 
			  					 }  
			  				  dismiss();
			  				}
			  			});
			  	        button_cancle.setOnClickListener(new Button.OnClickListener() {
			  				 
			  				public void onClick(View v) { 
			  						 colors = 0;
			  						 dismiss(); 
			  				}
			  			});
			  	        button_default.setOnClickListener(new Button.OnClickListener() {
			  			 
			  				public void onClick(View v) {
			  					if(flag == 0){//背景设置  
			  						 UserPreference.save("defaultColor",0); 
			  						 mListener.colorChanged(0,flag); 
			  						 colors = 0;
			  						 dismiss(); 
			  					 }else if(flag == 1){//表示默认按钮设置
			  						 UserPreference.save("xiangzhuang", "正方形");  
			  						 setButtonStyle();
			  						 UserPreference.save("caidan",0); 
			  						 mListener.colorChanged(1,flag); 
			  						 colors = 0;
			  						 dismiss(); 
			  					 } 
			  				}
			  			});
			  	        zhenfangxing.setOnClickListener(new Button.OnClickListener() {
			  				
			  				@Override
			  				public void onClick(View v) {  
			  						xingzhuang = "正方形"; 
				  					setButtonStyle(); 
			  				}
			  			});
			  	        banyuanxing.setOnClickListener(new Button.OnClickListener() {
			  						
			  				@Override
			  				public void onClick(View v) { 
			  					 xingzhuang = "半圆形";
		  						 setButtonStyle();
			  				}
			  			});
			  	        yuanxing.setOnClickListener(new Button.OnClickListener() {
			  				
			  				@Override
			  				public void onClick(View v) {  
			  					 xingzhuang = "圆形";
		  						 setButtonStyle();
			  				}
			  			});
			  	      
			 	 /////////////////////////////////      
	        }else{
			        	//默认背景设置
	        	
			        	flag = 0; 
			        	setContentView(R.layout.dialog_background); 
			        	RadioGroup radioproup_bg =(RadioGroup)findViewById(R.id.radioproup_bg);
			        	RadioButton radioButton0 = (RadioButton)  findViewById(R.id.default_id); 
			        	RadioButton radioButton1 = (RadioButton) findViewById(R.id.zhenfangxing);
			        	RadioButton radioButton2 = (RadioButton) findViewById(R.id.banyuanxing);
			        	RadioButton radioButton3 = (RadioButton) findViewById(R.id.yuanxing);
				        relative = (RelativeLayout)findViewById(R.id.dialog_bg_id); 
				        button_sure = (Button)findViewById(R.id.sure);
				        button_cancle = (Button) findViewById(R.id.cancel);
				        Utils.setChangeBackground(getContext(), relative); 
				        TextView textview = (TextView)findViewById(R.id.lay_testview); 
				        textview.setText("设置软件风格"); 
				        dialog = this;   
				        colors = defaultColor;
				        if(colors == 0){
				        	radioButton0.setChecked(true);	
				        }else if(colors == 1){
				        	radioButton1.setChecked(true);
				        }else if(colors == 2){
				        	radioButton2.setChecked(true);
				        }else if(colors == 3){
				        	radioButton3.setChecked(true);
				        }
				        System.out.println("colors==="+colors);
				    	if(colors == 0){//morend
		 	        		mInitialColor = getContext().getResources().getColor(R.color.lans);
		 	        	}
				     	Utils.color[0] =mInitialColor;
		 	         	Utils.color[1] =mInitialColor;
		 	            System.out.println("mInitialColor==="+mInitialColor);
		 	         	button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	
	             		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang));  
				        radioproup_bg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
							
							@Override
							public void onCheckedChanged(RadioGroup group, int checkedId) {
								if(checkedId == R.id.default_id){
//									relative.setBackgroundResource(R.drawable.moren_beijing);	
									colors = 0;
								} else if(checkedId == R.id.zhenfangxing){
//									relative.setBackgroundResource(R.drawable.moren_beijing1);	
									colors = 1;
								}else if(checkedId == R.id.banyuanxing){
//									relative.setBackgroundResource(R.drawable.moren_beijing2);	
									colors = 2;
								}else if(checkedId == R.id.yuanxing){
//									relative.setBackgroundResource(R.drawable.moren_beijing3);	
									colors = 3;
								}
							}
						});
				        button_sure.setOnClickListener(new Button.OnClickListener() {
			  				 
			  				public void onClick(View v) { 
			  					  //背景设置 
		  						 mListener.colorChanged(colors,0); 
		  						 UserPreference.save("defaultColor", colors); 
		  						 dismiss(); 
			  				}
			  			});
			  	        button_cancle.setOnClickListener(new Button.OnClickListener() {
			  				 
			  				public void onClick(View v) { 
		  						 colors = 0;
		  						 dismiss(); 
			  				}
			  			});
			  	       if(mInitialColor == 0){//morend
		 	        		mInitialColor = getContext().getResources().getColor(R.color.lans);
		 	        	}
			  	    	Utils.color[0] =mInitialColor;
		 	         	Utils.color[1] =mInitialColor;
			  	      button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	;
			  	      button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	;
	        }  
	       
    } 
    
    private void setCheckButton(){
       	zhenfangxing.setVisibility(View.VISIBLE);
		banyuanxing.setVisibility(View.VISIBLE);
		yuanxing.setVisibility(View.VISIBLE);
		zhenfangxing.setText("四方"); 
     	banyuanxing.setText("圆角");
     	yuanxing.setText("圆形"); 
     	if(mInitialColor == 0){//morend
     		mInitialColor = getContext().getResources().getColor(R.color.lans);
     	}
      	Utils.color[0] =mInitialColor;
     	Utils.color[1] =mInitialColor;
     	zhenfangxing.setBackgroundDrawable(Utils.setBackgroundType("正方形")); 	
  		banyuanxing.setBackgroundDrawable(Utils.setBackgroundType("半圆形")); 
  		yuanxing.setBackgroundDrawable(Utils.setBackgroundType("圆形"));  
 	  	int caidan = UserPreference.read("caidan", 0); 
 	  	if(caidan == 0){//表示默认颜色
 	  		setButtonStyle(); 
 	  	}else{////表示当前选中的颜色
 			Utils.color[0] = caidan;
 			Utils.color[1] = caidan;
 			button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	
     		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
     		button_default.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang));  
 	  	}
    }
    private void setButtonStyle(){
    	Utils.color[0] = mInitialColor;
    	Utils.color[1] = mInitialColor;
    	button_sure.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 	
 		button_cancle.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 
 		button_default.setBackgroundDrawable(Utils.setBackgroundType(xingzhuang)); 

    }
    
}

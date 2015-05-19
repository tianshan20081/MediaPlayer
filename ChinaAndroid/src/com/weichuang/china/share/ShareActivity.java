package com.weichuang.china.share;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.china.R;
import com.umeng.api.sns.UMSnsService;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;

public class ShareActivity extends BaseActivity {
	 private RelativeLayout background_id;
	 private   int defaultColor;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
    	UserPreference.ensureIntializePreference(this);  
	    defaultColor = UserPreference.read("defaultColor", 0);  
        BaseActivity.mBaseActivity = this;
        setTopBarTitle("选择分享方式"); 
        setTitleRightButtonHide();
        setTitleRightButtonBackbound(R.drawable.action_play_list);
        Button shareTosina = (Button) findViewById(R.id.shareTosina);
        Button shareTotenx = (Button) findViewById(R.id.shareTotenx);
        Button shareToRenren = (Button) findViewById(R.id.shareToRenren);
        shareTosina.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BufferedInputStream bis = null;
				try {
					bis = new BufferedInputStream(getAssets().open("icon.png"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		        Bitmap bmp = BitmapFactory.decodeStream(bis);

		        ByteArrayOutputStream stream = new ByteArrayOutputStream();
		        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		        final byte[] picture = stream.toByteArray();
		        
		        if (bmp != null && !bmp.isRecycled()){
					bmp.recycle();
				} 
		        
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("place", "王府井");
			    map.put("songName", "发现一款超好的多媒体播放软件,下载地址：http://www.eoemarket.com/apps/76026");
		        UMSnsService.shareToSina(ShareActivity.this, picture, map, null);
				
			}
		});
        shareTotenx.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BufferedInputStream bis = null;
				try {
					bis = new BufferedInputStream(getAssets().open("icon.png"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		        Bitmap bmp = BitmapFactory.decodeStream(bis);

		        ByteArrayOutputStream stream = new ByteArrayOutputStream();
		        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		        final byte[] picture = stream.toByteArray();
		        
		        if (bmp != null && !bmp.isRecycled()){
					bmp.recycle();
				} 
		        
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("place", "王府井");
			    map.put("songName", "发现一款超好的多媒体播放软件,下载地址：http://www.eoemarket.com/apps/76026");
		        UMSnsService.shareToTenc(ShareActivity.this, picture, map, null);
				
			}
		});
        shareToRenren.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BufferedInputStream bis3 = null;
				try {
					bis3 = new BufferedInputStream(getAssets().open("icon.png"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		        Bitmap bmp3 = BitmapFactory.decodeStream(bis3);

		        ByteArrayOutputStream stream3 = new ByteArrayOutputStream();
		        bmp3.compress(Bitmap.CompressFormat.PNG, 100, stream3);
		        final byte[] picture3 = stream3.toByteArray();
		        
		        if (bmp3 != null && !bmp3.isRecycled()){
					bmp3.recycle();
				} 
		        
				HashMap<String, String> map3 = new HashMap<String, String>();
				map3.put("place", "王府井");
			    map3.put("songName", "发现一款超好的多媒体播放软件,下载地址：http://www.eoemarket.com/apps/76026");
		        UMSnsService.shareToRenr(ShareActivity.this, picture3, map3, null);
				
				
			}
		});
        if(defaultColor == 0){ 
        	shareTosina.setBackgroundResource(R.drawable.share_bg);
        	shareTotenx.setBackgroundResource(R.drawable.share_bg);
        	shareToRenren.setBackgroundResource(R.drawable.share_bg);
        }
    }
    
    private void setBacground(){
    	
		background_id = (RelativeLayout)findViewById(R.id.background_id); 
		Utils.setChangeBackground(ShareActivity.this, background_id); 
 }
    
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		BaseActivity.mBaseActivity = this;
		setBacground();
	}
    

	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return  inflater.inflate(R.layout.share_activity, null);
	}

	@Override
	protected void titlRightButton() {
		
		
	}

	@Override
	protected void titleLeftButton() {
		finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
		
	}
}
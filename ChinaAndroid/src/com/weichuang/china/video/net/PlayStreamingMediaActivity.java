package com.weichuang.china.video.net;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MediaPlaybackService;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.player.SystemPlayer;
import com.weichuang.china.video.view.UserPreference;

public class PlayStreamingMediaActivity extends BaseActivity {
	  final String[] COUNTRIES = new String[] {"http://www", "www","http://","rtsp://","file:///","mms://"};
	private String path;
	 private RelativeLayout background_id;
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState); 
		setTopBarTitle("播放流媒体"); 
		 setTitleRightButtonHide();
	    setTitleRightButtonBackbound(R.drawable.action_play_list);
	    UserPreference.ensureIntializePreference(this);  
		BaseActivity.mBaseActivity = this;
	    int defaultColor = UserPreference.read("defaultColor", 0);   
		background_id = (RelativeLayout)findViewById(R.id.background_id);  
		 Utils.setChangeBackground(PlayStreamingMediaActivity.this, background_id);  
		final AutoCompleteTextView edit = (AutoCompleteTextView)findViewById(R.id.editview); 
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        edit.setAdapter(adapter);
		
		Button wancheng = (Button)findViewById(R.id.wancheng);
		 if(defaultColor == 0){
			 wancheng.setBackgroundResource(R.drawable.share_bg); 
		    }
		wancheng.setOnClickListener(new Button.OnClickListener() {
			
			
			public void onClick(View v) { 
				if(edit != null && edit.getText() != null && !edit.getText().toString().equals("")){
				  path = /*"mms://media.crinewsradio.cn/crinewsradio";*/edit.getText().toString(); 
				  Uri uri = Uri.parse(path);
//				  "mms://media.crinewsradio.cn/crinewsradio"

//				  "mms://114.80.221.20/A9";
				if(Utils.isUri(PlayStreamingMediaActivity.this, uri)){
					
					if(Utils.isCheckNetAvailable(PlayStreamingMediaActivity.this)){
						
						Intent i = new Intent(PlayStreamingMediaActivity.this, MediaPlaybackService.class);
				        i.setAction(MediaPlaybackService.SERVICECMD);
				        i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
				        startService(i);
				        
						Intent intent = new Intent(PlayStreamingMediaActivity.this,SystemPlayer.class);
						intent.putExtra("localuri", path);
						startActivity(intent);
						overridePendingTransition(R.anim.fade, R.anim.hold);
					}else{
						 Utils.netCheckDialog();
					}
					
					
					
					
				}else{
					Toast.makeText(PlayStreamingMediaActivity.this, "输入地址有误", 1).show();
				}
				
				}else{
					
					Toast.makeText(PlayStreamingMediaActivity.this, "输入地址为空", 1).show();
					
				}
			}
		});
	     ActivityHolder.getInstance().addActivity(this);
	}
	
	@Override
	protected View setCententView() { 
		return  inflater.inflate(R.layout.play_net_activity, null);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		BaseActivity.mBaseActivity = this;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		ActivityHolder.getInstance().removeActivity(this);
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

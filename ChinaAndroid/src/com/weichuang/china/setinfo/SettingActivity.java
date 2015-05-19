package com.weichuang.china.setinfo;

 
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.KeyEvent;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.util.Utils;

public class SettingActivity extends PreferenceActivity implements
	OnPreferenceChangeListener, OnPreferenceClickListener
{
	/** Called when the activity is first created. */
	private static final String TAG = "SetingUIActivity";

	private SharedPreferences preference = null;
	private CheckBoxPreference updateCheckBoxPreference = null;
	private ListPreference lististPreference = null;
	private CheckBoxPreference isneilflag_CheckBoxPreference = null;
	private CheckBoxPreference isneilflag_boot= null;
	private CheckBoxPreference isneilflag_aout_next= null;
	public static final String key_1 = "set_videplay_key";//默认使用软解码播放
	public static final String key_2 = "auto_update_frequency_key";//跳转间隔
	public static final String key_3 = "set_music_exit_key";//退出时是否提示后台播放
	public static final String key_4 = "set_boot_button";//设置开机启动
	public static final String key_5 = "set_video_auto_next_key";//是否自动播放下一集
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
		preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		initKey();
		BaseActivity.mBaseActivity = this;
		initKeyListener();
		requestCodes = 0;
	}

	private void initKey()
	{
		updateCheckBoxPreference = (CheckBoxPreference) findPreference(key_1);
		lististPreference = (ListPreference) findPreference(key_2);
		isneilflag_CheckBoxPreference = (CheckBoxPreference) findPreference(key_3);
		isneilflag_boot = (CheckBoxPreference)findPreference(key_4);
		isneilflag_aout_next = (CheckBoxPreference)findPreference(key_5);
		boolean iskey1 = preference.getBoolean(key_1, false);
		if(iskey1/*&&Utils.checkPlayerAPPEcoder(SettingActivity.this)*/){
			 updateCheckBoxPreference.setChecked(true);
		  }else{
			 updateCheckBoxPreference.setChecked(false);
		 }
		Log.i(TAG, "key_1===initKey==="+iskey1);
		
	}

	private void initKeyListener()
	{
		updateCheckBoxPreference.setOnPreferenceChangeListener(this);
		updateCheckBoxPreference.setOnPreferenceClickListener(this);

		lististPreference.setOnPreferenceClickListener(this);
		lististPreference.setOnPreferenceChangeListener(this);
		
		
		isneilflag_CheckBoxPreference.setOnPreferenceChangeListener(this);
		isneilflag_CheckBoxPreference.setOnPreferenceClickListener(this); 
		
		isneilflag_boot.setOnPreferenceChangeListener(this);
		isneilflag_boot.setOnPreferenceClickListener(this);
		
		isneilflag_aout_next.setOnPreferenceChangeListener(this);
		isneilflag_aout_next.setOnPreferenceClickListener(this);
	}

	
	public boolean onPreferenceClick(Preference preference)
	{
		// 判断是哪个Preference改变了
		if (preference.getKey().equals(key_1)) {
			boolean isEixt = preference.getSharedPreferences().getBoolean(key_1, false);
//			 if(isEixt&&Utils.getOSVersionSDKINT(SettingActivity.this)>=7){
//				 if(!Utils.checkPlayerAPPEcoder(SettingActivity.this)){
//					 
//					 if(Utils.isNEON){
//							Utils.copyAssetsToSdcard(SettingActivity.this, Utils.ECODER_2,Utils.ECODER_2);
//						}else{
//							Utils.copyAssetsToSdcard(SettingActivity.this, Utils.ECODER_1,Utils.ECODER_1);
//						}
////					
//					 
//					 return false;
//				  }
//					return true;
//				}
			
		} else if (preference.getKey().equals(key_2)) {
			String timer = preference.getSharedPreferences().getString(key_2, "10");
			Log.e(TAG, "timer =="+timer);
		}else if(preference.getKey().equals(key_3)){
			boolean isEixt = preference.getSharedPreferences().getBoolean(key_3, false);
			Log.e(TAG,"isEixt ==="+ isEixt);
		}else if(preference.getKey().equals(key_4)){
			boolean isBoot = preference.getSharedPreferences().getBoolean(key_4, false);	
		}
		// 返回true表示允许改变
		return true;
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

	
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{  
		String key = preference.getKey();
		Log.i(TAG, "key===onPreferenceChange==="+key);
		// 判断是哪个Preference改变了
		 if (key!=null&&key.equals(key_1)) {
			 
//			 if(Utils.getOSVersionSDKINT(SettingActivity.this)>=7){
//					if((Boolean)newValue){
//						 if(!Utils.checkPlayerAPPEcoder(SettingActivity.this)){
//							 Utils.setingCheckAPPEcoder(SettingActivity.this);
//							 return false;
//						  }
//						
//					}
//					Log.i(TAG, "key_1===onPreferenceChange==="+newValue);
//					return true;
//				}else{
//					 Utils.netNoPlayeDialog();
//					Log.i(TAG, "key_1===onPreferenceChange==="+newValue);
//					 return false;
//				}
				
			// 列表
			 return true;
		
		}else if(preference.getKey().equals(key_2)){
			Log.i(TAG, key_2+"key_2===onPreferenceChange=="+newValue);
			return true;
			
		}else if(preference.getKey().equals(key_3)){
			Log.i(TAG, key_3+"key_3====onPreferenceChange=="+newValue);
			return true;
		}else if(preference.getKey().equals(key_4)){
			return true;
		}else if(preference.getKey().equals(key_5)){
			return true;
		}
		// 返回true表示允许改变
		return false;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	
    private int requestCodes = 0;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		requestCodes = requestCode;
		if(requestCode==8){
			if(Utils.checkPlayerAPPEcoder(SettingActivity.this)){
				 updateCheckBoxPreference.setChecked(true);
			  }else{
				 updateCheckBoxPreference.setChecked(false);
			 }
			
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		BaseActivity.mBaseActivity = this;
		if(requestCodes==8){
			if(Utils.checkPlayerAPPEcoder(SettingActivity.this)){
				 updateCheckBoxPreference.setChecked(true);
			  }else{
				 updateCheckBoxPreference.setChecked(false);
			 }
			
		}else{
			initKey();
		}
	}
	
	
}
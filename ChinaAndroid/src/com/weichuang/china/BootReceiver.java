package com.weichuang.china;
 
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.android.china.LoginAtivity;
import com.android.china.R;
import com.weichuang.china.setinfo.SettingActivity;
public class BootReceiver extends BroadcastReceiver { 
	public static NotificationManager nm;
	public static int key = 19172439;
	public void onReceive(Context context, Intent intent) {
		try{
			System.out.println("开机啦开机啦"); 
			SharedPreferences preference = null;
			preference = PreferenceManager.getDefaultSharedPreferences(context);
			boolean key_4 = preference.getBoolean(SettingActivity.key_4, false);
			if (!key_4) {
				nm = (NotificationManager) context
						.getSystemService(context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(R.drawable.icon,
						context.getString(R.string.app_name), System
								.currentTimeMillis());
				notification.contentView = new RemoteViews(
						context.getPackageName(), R.layout.notification);
				// 使用notification.xml文件作VIEW
				// （就是在Android Market下载软件，点击下载但还没获取到目标大小时的状态）
				Intent notificationIntent = new Intent(context, LoginAtivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
						notificationIntent, 0);
				notification.contentIntent = contentIntent;
				nm.notify(key, notification);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public static void cancels(){
		try{
		if(nm !=null){
			nm.cancel(key);
		}}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
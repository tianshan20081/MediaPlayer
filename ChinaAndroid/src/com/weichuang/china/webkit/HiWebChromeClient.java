package com.weichuang.china.webkit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.android.china.R;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.util.Utils;

/**
 * 辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
 * @author jiyx
 *
 */
public class HiWebChromeClient extends WebChromeClient{
	private final String TAG = "HiWebChromeChilent";
	private static int MESSAGE_STATE = 1;
	private Context mContext;
	private Handler mHandler;
	
	public HiWebChromeClient(Context mContext,Handler mHandler){
		this.mContext = mContext;
		this.mHandler = mHandler;
	}
	@Override
	public boolean onJsAlert(WebView view, String url, String message,
			final JsResult result) {
		Dialog dialog = new AlertDialog.Builder(mContext)
				.setTitle(R.string.tip)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								result.confirm();
							}
						}).create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		dialog.show();
		return true;
	}
	@Override
	public void onReceivedTitle(WebView view, String title) {
		if(null != mHandler && !Utils.isEmpty(title)){
			Message msg = new Message();
			msg.what = MESSAGE_STATE;
			Bundle bundle = new Bundle();
			bundle.putString("title", title);
			msg.setData(bundle);
			mHandler.sendMessage(msg);
		}
		super.onReceivedTitle(view, title);
	}
	@Override
	public void onProgressChanged(WebView view, int newProgress) {// 载入进度改变而触发
		
		LogUtil.i(TAG, "progress------------"+newProgress );
		
		String url = view.getUrl();
		mHandler.removeMessages(BrowserActivity.PROGRESS_REF);
		mHandler.sendEmptyMessage(BrowserActivity.PROGRESS_REF);
		
		if (newProgress >= 100) {  
			mHandler.sendEmptyMessage(BrowserActivity.PROGRESS_HID);// 如果全部载入,隐藏进度对话框
		}
		super.onProgressChanged(view, newProgress); 
	}
	
	

	
}

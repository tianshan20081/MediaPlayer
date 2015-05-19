package com.weichuang.china.webkit;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.china.R;
import com.weichuang.china.util.Utils;


/**
 * 帮助WebView处理各种通知、请求事件
 * @author yanggf
 *
 */
public class HiWebViewClient extends WebViewClient {
	
	private static final String TAG = "HiWebViewClient";
	private Context context;
	private WebView mWebView;
	private String mLoadUrl;
	private Dialog mDialog;
	private boolean isFirstShowDialog = false;
	
	public HiWebViewClient(Context context,WebView mWebView,String mLoadUrl){
		this.context = context;
		this.mWebView = mWebView;	
		this.mLoadUrl = mLoadUrl;
	}
	

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		 	if(!Utils.isEmpty(url)){
		 		mWebView.loadUrl(url);
		 	}
			return true;
	}

	@Override
	public void onReceivedError(WebView view, int errorCode,
			String description, String failingUrl) {
		Builder builder = new Builder(context);
		if (errorCode == -2) {
			mWebView.loadUrl(mLoadUrl);
			
			builder.setTitle(R.string.tip)
					.setMessage(R.string.netdown)
					.setNegativeButton(R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(
										DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}

							});
			
			builder.create().show();
		}
		super.onReceivedError(view, errorCode, description, failingUrl);
	}


	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		if(!isFirstShowDialog){
			isFirstShowDialog = true;
			startWaitDialog();
		}		
		super.onPageStarted(view, url, favicon);
	}


	@Override
	public void onPageFinished(WebView view, String url) {
		closeWaitDialog();
		super.onPageFinished(view, url);
	}
	
	private void startWaitDialog(){
		if(null == mDialog){
			mDialog = new Dialog(context, R.style.waiting);
			mDialog.setContentView(R.layout.waiting);
			mDialog.setCanceledOnTouchOutside(false);
			mDialog.show();
		}else{
			if(!mDialog.isShowing()){
				mDialog.show();
			}
		}
	}
	
	private void closeWaitDialog(){
		if (mDialog != null)
			mDialog.dismiss();
	}
}

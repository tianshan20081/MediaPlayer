package com.weichuang.china.webkit;

import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.webkit.WebView;

import com.weichuang.china.util.Utils;

/**
 * @author yanggf
 *
 */
public class JsCheckNet {

	private Handler mHandler;
	private WebView mWebView;
	private Context mContext;
	private final static String DEAFULTURL = "http://i.funshion.com/api/zeus_feedback";
	
	public JsCheckNet(Context mContext,WebView mWebView,Handler mHandler){
		this.mHandler = mHandler;
		this.mWebView = mWebView;
		this.mContext = mContext;
	}
	
	public void setAction(String loadurl){
		if(!Utils.isEmpty(loadurl)){
			if(null != mWebView)
				mWebView.loadUrl("javascript:setAction("+loadurl+")");
		}				
	}
	/*js call the local method  */

	/**获取action的值，并拼加参数字符串*/
	public void getAction(){		
		if(null != mHandler)
			mHandler.sendEmptyMessage(1);		
	}
	
	public void checkWebViewUrl() {
		// 进行一次异步联网检测得到返回状态码
		new AsyncTask<String, Void, Integer>() {

			@Override
			protected Integer doInBackground(String... params) {
				int responseCode = -1;
				try {
					URL url = new URL(params[0]);
					HttpURLConnection connection = (HttpURLConnection) url
							.openConnection();
					responseCode = connection.getResponseCode();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return responseCode;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result != 200) {
					if(null != mHandler)
						mHandler.sendEmptyMessage(0);
				}			
			}
		}.execute(DEAFULTURL);

	}

}

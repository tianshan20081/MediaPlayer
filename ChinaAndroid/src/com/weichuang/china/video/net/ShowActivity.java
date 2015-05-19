package com.weichuang.china.video.net;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.china.R;
import com.waps.AppConnect;
import com.waps.UpdatePointsNotifier;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MediaPlaybackService;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.util.ProxyUtils;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.player.SystemPlayer;
import com.weichuang.china.video.view.UserPreference;

@TargetApi(11)
public class ShowActivity extends BaseActivity implements View.OnClickListener, UpdatePointsNotifier{
	private static final String TAG = "ShowActivity";
	private static WebView wv;
	private ProgressBar progressDialog;
	
	private String mTitle;
	
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {// 定义一个Handler，用于处理下载线程与UI间通讯
			switch (msg.what) {
			case 0:
				LogUtil.i(TAG, "View.VISIBLE");
				if(progressDialog !=null)
				progressDialog.setVisibility(View.VISIBLE);// 显示进度对话框
				break;
			case 1:
				LogUtil.i(TAG, "View.GONE");
				if(progressDialog !=null)
				progressDialog.setVisibility(View.GONE);// 隐藏进度对话框，不可使用dismiss()、cancel(),否则再次调用show()时，显示的对话框小圆圈不会动。
				break;
			}
		
			super.handleMessage(msg);
		}
	};
	
	private Intent mIntent = null;
    private String uri= null;
	private VideoInfo mVideoInfo = null;
	private int flags = 0;
    private Boolean isOncreat = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(wv != null){
			wv.destroy();
		}
		System.out.println("onCreate----------------");
//	    getWindow().requestFeature(Window.FEATURE_PROGRESS);
//		setTitleRightButtonHide();
		setTitleRightButtonBackbound(R.drawable.shuaxin_beij); 
//		setContentView(R.layout.show_activity);
		isOncreat = true;
		setTopBarTitleSize(18);
		setTopBarTitleWidth(200);
		progressDialog = (ProgressBar)findViewById(R.id.progressdialog);
		progressDialog.setVisibility(View.VISIBLE);
	    UserPreference.ensureIntializePreference(this); 
		BaseActivity.mBaseActivity = this;  
        RelativeLayout laytout_beij = (RelativeLayout)findViewById(R.id.laytout_beij); 
		Utils.setChangeBackground(ShowActivity.this, laytout_beij);  
        ActivityHolder.getInstance().addActivity(this); 
        init();// 执行初始化函数
		Bundle mBundle = getData();
		load(mBundle);
	}
	
	
	 


	private Bundle getData() {
		mIntent = getIntent();
		Bundle mBundle = null;
		if(mIntent != null){
			 mBundle = mIntent.getBundleExtra("extra");
			 flags =  mIntent.getFlags();
		}
		return mBundle;
	}
	private void load(Bundle mBundle) {
		if(mBundle != null){
			mVideoInfo = (VideoInfo)mBundle.getSerializable("VideoInfo");
			if(mVideoInfo != null){
				setTopBarTitle(mVideoInfo.getTitle());
				 uri = mVideoInfo.getUrl();
				 mTitle= mVideoInfo.getTitle();
				if(uri != null){
					loadurl(wv,uri,false);
				}else{
					uri = "http://music.baidu.com/?ssid=0&from=381b_w1&bd_page_type=1&uid=21507543AF7056FEAF4F44500476DBC4&pu=sz%401320_1001&itj=420#";
					loadurl(wv, uri,false);
				}
			}
		    
			
		}else{
			uri = "http://music.baidu.com/?ssid=0&from=381b_w1&bd_page_type=1&uid=21507543AF7056FEAF4F44500476DBC4&pu=sz%401320_1001&itj=420#";
			loadurl(wv, uri,false);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		 int groupId = 0; 
		    // The order position of the item 
		    int menuItemOrder = Menu.NONE; 
		 
		    menu.add(groupId, 1, menuItemOrder, "后退") 
		        .setIcon(R.drawable.icon_menu_back_htc); 
		    menu.add(groupId, 2, menuItemOrder, "前进")
		    .setIcon(R.drawable.icon_menu_forward_htc); 
		    menu.add(groupId, 3, menuItemOrder, "刷新")
		    .setIcon(R.drawable.icon_reload_off_htc); 
		    menu.add(groupId, 4, menuItemOrder, "更多")
		    .setIcon(R.drawable.icon_menu_multitab_htc);  
		    return true; 
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
			return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case 1:
//			Toast.makeText(this, "反馈", 1).show();
			if(wv != null&&wv.canGoBack()){
				if (handler != null){ 
					handler.sendEmptyMessage(0);
				}
				wv.goBack();
			}else if(wv != null&&!wv.canGoBack()){
				Toast.makeText(this, "不能后退了", 0).show();
			}
			
			break;
		case 2:
			if(wv != null&&wv.canGoForward()){
				if (handler != null){ 
					handler.sendEmptyMessage(0);
				}
				wv.goForward();
			}else if(wv != null&&!wv.canGoForward()){
				Toast.makeText(this, "不能前进了", 0).show();
			}
			break;
		case 3:
			if(Utils.isCheckNetAvailable(ShowActivity.this)){
				
				if(uri ==null)
					return true;
				
				if (uri.contains("rtsp") ||uri.contains("3gp") || uri.contains("mp4")) {
					loadurl(wv, uri, true);
				} else {
					loadurl(wv, uri, false);
				}
//				if (handler != null){ 
//					handler.sendEmptyMessage(0);
//				}
				if (handler != null){ 
					handler.sendEmptyMessage(0);
				}
			}else{
				if(!isNetCheckDialog){
					isNetCheckDialog = true;
					netCheckDialog();
				}
//				Utils.netCheckDialog();
			}
			
			
			break;
		case 4:
			AppConnect.getInstance(this).showOffers(this);
			break;
		}
		return super.onOptionsItemSelected(item);
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

	public void init() {
		
		// 初始化
		wv = (WebView) findViewById(R.id.webview);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setPluginsEnabled(true);
//		WebSettings webSettings = wv.getSettings();
//		webSettings.setJavaScriptEnabled(true);// 可用JS
//		webSettings.setAllowFileAccess(true);
		wv.setScrollBarStyle(0);//滚动条风格，为0就是不给滚动条留空间，滚动条覆盖在网页上  
		
		wv.setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				
				String info = "uri=" + uri + "--userAgent=" + userAgent
						+ "--contenDisposition=" + contentDisposition
						+ "--mimetype=" + mimetype + "--contentLength="
						+ contentLength;
				Toast.makeText(ShowActivity.this, info, Toast.LENGTH_SHORT)
						.show();
				
				if(Utils.isCheckNetAvailable(ShowActivity.this)){
					
					LogUtil.i(TAG,	"---URL==="		+ url.toString());
					if (handler != null){ 
						handler.sendEmptyMessage(0);
					}
					if (url.contains("rtsp") ||url.contains("3gp") || url.contains("mp4")|| url.contains("m3u8")) {
						uri = url;
						loadurl(null, url, true);
					} 
				}else{
					Toast.makeText(ShowActivity.this, "网络不可用，请检查网络再试", 0).show();
					
				} 



			}
		});
		
		wv.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(final WebView view,
					final String url) {
				
				if(Utils.isCheckNetAvailable(ShowActivity.this)){
					
					LogUtil.i(TAG,	"---URL==="		+ url.toString());
					if (handler != null){ 
						handler.sendEmptyMessage(0);
					}
					if (url.contains("rtsp") ||url.contains("3gp") || url.contains("mp4")|| url.contains("m3u8")) {
						uri = url;
						loadurl(view, url, false);
					} else {
						uri = url;
						loadurl(view, url, false);
					}
				}else{
					Toast.makeText(ShowActivity.this, "网络不可用，请检查网络再试", 0).show();
					
				} 

				return true;
			}

		});
		wv.setWebChromeClient(new WebChromeClient() {
			
			
			
			@Override
			public void onExceededDatabaseQuota(String url,
					String databaseIdentifier, long currentQuota,
					long estimatedSize, long totalUsedQuota,
					QuotaUpdater quotaUpdater) {
				
				String urls = url;
				Log.i("yangguangfu",
						"onExceededDatabaseQuota=="
								+ urls);
				// TODO Auto-generated method stub
				super.onExceededDatabaseQuota(url, databaseIdentifier, currentQuota,
						estimatedSize, totalUsedQuota, quotaUpdater);
			}

			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
				// TODO Auto-generated method stub
				super.onReceivedIcon(view, icon);
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				// TODO Auto-generated method stub
				if(title!= null&&title.trim().length()>0){
					mTitle = title;
					if(mTitle!= null){
						setTopBarTitle(mTitle);
					}
				}

				Log.i("yangguangfu",
						"onReceivedTitle=="
								+ mTitle);
				
				
				super.onReceivedTitle(view, title);
			}

			@Override
			public void onReceivedTouchIconUrl(WebView view, String url,
					boolean precomposed) {
				
				String urls = url;
				Log.i("yangguangfu",
						"onReceivedTouchIconUrl=="
								+ urls);
				// TODO Auto-generated method stub
				super.onReceivedTouchIconUrl(view, url, precomposed);
			}

			@Override
			public void onRequestFocus(WebView view) {
				// TODO Auto-generated method stub
				super.onRequestFocus(view);
			}

			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				// TODO Auto-generated method stub
				super.onShowCustomView(view, callback);
			}

			public void onProgressChanged(WebView view, int progress) {// 载入进度改变而触发
				setProgress(progress*100); 
				
				System.out.println("progress------------"+progress );
				
				String url = view.getUrl();
				Log.i("yangguangfu",
						"onProgressChanged_URL="
								+ url);
				
				Log.i("yangguangfu",
						"setWebChromeClient___onProgressChanged_URL="
								+ progress);
				if (progress >= 100) {  
					isNetCheckDialog = false;
					handler.sendEmptyMessage(1);// 如果全部载入,隐藏进度对话框
				}
				super.onProgressChanged(view, progress); 
			}
		});
		 

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) ) {
			if(wv!=null&& wv.canGoBack()){
				wv.goBack();
			}else{ 
				onDestroyWebView();
				finish();
				if(flags ==2){ 
					wv.setWillNotCacheDrawing(true); 
				}
//				ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);  
//		        manager.restartPackage(getPackageName());  
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}
			
			return true;
		} 
		return false;
	}
    private boolean isLoadPlayed = false;
    
 // 构建Runnable对象，在runnable中更新界面  
    Runnable   runnableUi=new  Runnable(){  
        @Override  
        public void run() {  
            //更新界面  
        	if(mView!= null&&uri!= null)
        	 mView.loadUrl(uri);
        }  
          
    };  

    private WebView mView  = null;
	public void loadurl(final WebView view, final String url,
			final boolean isVideoUrl) {
		new Thread() {
			public void run() {
				LogUtil.i(TAG, "run()");
				String uris = url;
				if (isVideoUrl && url != null) {
					
					if (url.contains("rtsp") ||uris.contains("3gp") || uris.contains("mp4")|| uris.contains("m3u8")) {
						if(uris != null&&!isLoadPlayed){
							isLoadPlayed = true;
							LogUtil.i(TAG, "uris="+uris);
							
							Intent i = new Intent(ShowActivity.this, MediaPlaybackService.class);
					        i.setAction(MediaPlaybackService.SERVICECMD);
					        i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
					        startService(i);
							
//							Intent intent = new Intent(ShowActivity.this,SystemPlayer.class);
//							intent.putExtra("localuri", uris);
//							LogUtil.i(TAG, "startActivity");
//							ShowActivity.this.startActivityForResult(intent, 1);
					        uris = ProxyUtils.getRedirectUrl(uris);
					        
					        mVideoInfo.setUrl(uris);
					        try{
					        	 if(mTitle != null&&mTitle.trim().length()>0)
								        mVideoInfo.setTitle(mTitle);
					        }catch(Exception e){
					        	e.printStackTrace();
					        }
					       
					       
							Intent intent = new Intent(ShowActivity.this,SystemPlayer.class);
							Bundle mBundle = new Bundle();
							mBundle.putSerializable("VideoInfo", mVideoInfo);
							intent.putExtras(mBundle);
							LogUtil.i(TAG, "startActivity");
							ShowActivity.this.startActivityForResult(intent, 1);
							overridePendingTransition(R.anim.fade, R.anim.hold);
							if (handler != null){ 
								handler.sendEmptyMessage(1);
							}
							
						}
						
					}
				} else {
					isLoadPlayed = false;
					LogUtil.i(TAG, "uris="+uris);
					 mView = view;
					if(handler!= null&&runnableUi!= null)
					  handler.post(runnableUi); 
//					view.loadUrl(url);
				}

			}
		}.start();
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==1){
			isLoadPlayed = false;
		}
	}
	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.show_activity, null);
	}

	@Override
	protected void titleLeftButton() {

		ShowActivity.this.finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
	
		
	}

	@Override
	protected void titlRightButton() {
		// TODO Auto-generated method stub
		if(Utils.isCheckNetAvailable(ShowActivity.this)){
			
			if(uri ==null)
				return;
			
			if (uri.contains("rtsp") ||uri.contains("3gp") || uri.contains("mp4")) {
				loadurl(wv, uri, true);
			} else {
				loadurl(wv, uri, false);
			}
			if (handler != null){ 
				handler.sendEmptyMessage(0);
			}
		}else{
			
			Toast.makeText(ShowActivity.this, "网络不可用，请检查网络再试", 0).show();
				
//			if(!isNetCheckDialog){
//				isNetCheckDialog = true;
//				netCheckDialog();
//			}
//			Utils.netCheckDialog();
		}
		
	}
	
	public void getUpdatePoints(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	public void getUpdatePointsFailed(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
//		if(wv != null)
//			wv.onResume();
//		if(!isOncreat){
//			init();// 执行初始化函数
//			loadurl(wv, uri, false);
//		} 
		
		super.onResume();
		BaseActivity.mBaseActivity = this;
		 
	}
	
	 
 
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(flags ==2){ 
			System.out.println("onPause--------------"); 
			isOncreat = false;
		}
		super.onPause();  
	}
	
	
	
	public static void onDestroyWebView(){
		try{
			if(wv != null){
				System.out.println("onDestroyWebView!!!!!!!!!!!!!!!!!!!!!!-------");
				wv.onPause(); 
				wv.destroy();   
			}  
		}catch(Exception e){
			e.printStackTrace();
		}
		
	} 
	
	@Override
	protected void onDestroy() {
		System.out.println("onDestroy----------");
		try{
			AppConnect.getInstance(this).finalize(); 
			ActivityHolder.getInstance().removeActivity(this); 
		}catch(Exception e){
			e.printStackTrace();
		}
		
		super.onDestroy();
	
	}

	

}
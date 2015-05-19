package com.weichuang.china.video.net;
 
 
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.util.ReadXmlByPullService;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.MyListView;
import com.weichuang.china.video.view.ScrollableViewGroup;
import com.weichuang.china.video.view.ScrollableViewGroup.OnCurrentViewChangedListener;
import com.weichuang.china.video.view.UserPreference;
import com.weichuang.china.webkit.BrowserActivity;
public class NetVideoListActivity extends BaseActivity  {
	
	  private static String TAG = "NetVideoListActivity";
	  private GridView gridview;
      private MyListView listview;
      private ArrayList<VideoInfo> videoList; 
  	  private boolean flag = false;
      
      private ScrollableViewGroup mFlipper;  
      private MyGridAdapter gridAdapter;
	  private MyListAdapter listAdapter;
	  public void onCreate(Bundle savedInstanceState) {  
	        super.onCreate(savedInstanceState);  
	        ActivityHolder.getInstance().addActivity(this);
//	        setImageView01(R.drawable.action_stream);
		    setTopBarTitle("网络视频");
		    BaseActivity.mBaseActivity = this;
	        UserPreference.ensureIntializePreference(this); 
	       
	        RelativeLayout laytout_beij = (RelativeLayout)findViewById(R.id.laytout_beij);
	        Utils.setChangeBackground(NetVideoListActivity.this, laytout_beij); 
	        initViews(); 
	        setTitleLeftButtonBackbound(R.drawable.search_bg) ;
	        setTitleRightButtonBackbound(R.drawable.gridview_bg) ;  
	        listview.setOnItemClickListener(listGridListener );
	        gridview.setOnItemClickListener(listGridListener ); 
	        
	      
	    }
	    
	    @Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			  BaseActivity.mBaseActivity = this;
			  handler.sendEmptyMessage(1);
		    
		}
	    private Handler handler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				switch(msg.what){
				case 1:
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							 testPullRead(); 
							 handler.sendEmptyMessage(2);
						}
					}).start(); 
					break;
				case 2:
					 if(videoList != null&& videoList.size() > 0){
						   gridAdapter.setList(videoList);
						   gridAdapter.notifyDataSetChanged();
						   listAdapter .setList(videoList);
						   listAdapter.notifyDataSetChanged();
					  }
					break;
				}
			}
	    	
	    };

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
		protected void onStart() {
			// TODO Auto-generated method stub
			super.onStart();
//		    
		}
	   
		private void initViews() {  
			mFlipper = (ScrollableViewGroup) findViewById(R.id.ViewFlipper);
			mFlipper.setOnCurrentViewChangedListener(mOnCurrentViewChangedListener); 
			LinearLayout gridviewLineary = (LinearLayout) mFlipper.findViewById(R.id.frmMain); 
			gridview = (GridView) gridviewLineary.findViewById(R.id.gridview);  
			LinearLayout listLayout = (LinearLayout) mFlipper.findViewById(R.id.frmList);
			listview = (MyListView)listLayout.findViewById(R.id.myListView);
			videoList = new ArrayList<VideoInfo>();
			gridAdapter = new MyGridAdapter(NetVideoListActivity.this,videoList);
			gridview.setAdapter(gridAdapter);
			listAdapter = new MyListAdapter(NetVideoListActivity.this,videoList); 
			listview.setAdapter(listAdapter); 
			gridview.setOnItemClickListener(listGridListener );
			listview.setOnItemClickListener(listGridListener );  
			  
		} 
	  
	    @Override
		protected void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
			ActivityHolder.getInstance().removeActivity(this);
		}

		private OnCurrentViewChangedListener mOnCurrentViewChangedListener = new OnCurrentViewChangedListener() {
			
			
			public void onCurrentViewChanged(View view, int currentview) {
   
				if(currentview == 0){ 
					   setTitleRightButtonBackbound(R.drawable.gridview_bg) ;  
				}else if(currentview == 1){  
					   setTitleRightButtonBackbound(R.drawable.listitem_bg) ; 
				} 
			}
		};
	     
	    public void testPullRead(){
			InputStream inStream = this.getClass().getClassLoader().getResourceAsStream("video.xml");   
			try {
				videoList = ReadXmlByPullService.ReadXmlByPull(inStream);   
				
			} catch (Exception e) { 
				e.printStackTrace();
			} 
		} 

		private OnItemClickListener listGridListener = new OnItemClickListener() {

			
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				
				if(Utils.isCheckNetAvailable(NetVideoListActivity.this)){
				   LogUtil.i(TAG,"onItemClick~~~~~~~~~~~~~~~~~~~");
					VideoInfo videoinfo = (VideoInfo)arg0.getAdapter().getItem(position);
					
					
//					Intent i = new Intent(NetVideoListActivity.this,BrowserActivity.class); 
					Intent i = new Intent(NetVideoListActivity.this,ShowActivity.class); 
					Bundle bundle = new Bundle();
					bundle.putSerializable("VideoInfo", videoinfo);
					i.putExtra("extra", bundle);
					NetVideoListActivity.this.startActivity(i); 
					overridePendingTransition(R.anim.fade, R.anim.hold);
				}else{
//					 Utils.netCheckDialog();
					Toast.makeText(NetVideoListActivity.this, "网络不可用，请检查网络再试", 0).show();
 					
				}
				
				
			}
		};
		

		
		
		
		class MyGridAdapter extends BaseAdapter{ 
			private ArrayList<VideoInfo>  videoList;
			private Context context;
			
			public MyGridAdapter(Context context,ArrayList<VideoInfo> videoList){
				this.context = context;
				this.videoList = videoList;
			}
			public void setList(ArrayList<VideoInfo> videoList){
				this.videoList = videoList;
			}
			
			public int getCount() { 
				int num = 0;
				if(videoList!=null&&videoList.size()>0){
					num= videoList.size();
				}
				return num;
				
			} 
			
			public Object getItem(int arg0) { 
				if(videoList!=null&&videoList.size()>0){
				 return videoList.get(arg0);
				}
				return null;
			} 
			
			public long getItemId(int position) { 
				return position;
			} 
			
			public View getView(int position, View convertView, ViewGroup parent) { 
				ViewHolder holder;
//				Bitmap bitmap = null;
//				Bitmap bnp = null;
				if(convertView == null){ 
					holder = new ViewHolder();
					convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, null); 
					 
					holder.tx1 = (ImageView) convertView.findViewById(R.id.imageview);
					holder.tx2 = (TextView) convertView.findViewById(R.id.ListItemContent); 
					convertView.setTag(holder);
				}else{ 
					holder = (ViewHolder) convertView.getTag();
				} 
				
				//  
				holder.tx2.setText(videoList.get(position).getTitle());
				int flags = videoList.get(position).getFlags();
				if(flags == 1){
					holder.tx1.setBackgroundResource(R.drawable.youku_icon); 
				}else if(flags == 2){
					holder.tx1.setBackgroundResource(R.drawable.qiyi); 
				}else if(flags == 3){
					holder.tx1.setBackgroundResource(R.drawable.tudou); 
				}else if(flags == 4){
					holder.tx1.setBackgroundResource(R.drawable.leshiwang); 
				}else if(flags == 5){
					holder.tx1.setBackgroundResource(R.drawable.kuliuwang); 
				}else if(flags == 6){
					holder.tx1.setBackgroundResource(R.drawable.wuliuwang); 
				}else if(flags == 7){
					holder.tx1.setBackgroundResource(R.drawable.jidongwang); 
				}else if(flags == 8){
					holder.tx1.setBackgroundResource(R.drawable.pptv); 
				}else if(flags == 9){
					holder.tx1.setBackgroundResource(R.drawable.dianyingwang); 
				}else if(flags == 10){
					holder.tx1.setBackgroundResource(R.drawable.maishiwang); 
				}else if(flags == 11){
					holder.tx1.setBackgroundResource(R.drawable.sina); 
				}else if(flags == 12){
					holder.tx1.setBackgroundResource(R.drawable.fenghuang); 
				}else if(flags == 13){
					holder.tx1.setBackgroundResource(R.drawable.video1); 
				}else if(flags == 14){
					holder.tx1.setBackgroundResource(R.drawable.yibaitv); 
				}else if(flags == 15){
					holder.tx1.setBackgroundResource(R.drawable.maishi); 
				}
				else if(flags == 16){
					holder.tx1.setBackgroundResource(R.drawable.wulongsha); 
				} 
				else if(flags == 17){
					holder.tx1.setBackgroundResource(R.drawable.menhu); 
				} else if(flags == 19){
					holder.tx1.setBackgroundResource(R.drawable.times);
				}else if(flags == 20){
					holder.tx1.setBackgroundResource(R.drawable.kankanxinwenw);
				} else if(flags == 18){
					holder.tx1.setBackgroundResource(R.drawable.shouhuship);
				}else if(flags == 21){
					holder.tx1.setBackgroundResource(R.drawable.wanhuatong);
				}
			 
				return convertView;
			}
		} 
		
		
		class MyListAdapter extends BaseAdapter{ 
			private ArrayList<VideoInfo>  videoList;
			private Context context;
			
			public MyListAdapter(Context context,ArrayList<VideoInfo> videoList){
				this.context = context;
				this.videoList = videoList;
			}
			public void setList(ArrayList<VideoInfo> videoList){
				this.videoList = videoList;
			}
			
			public int getCount() { 
				int num = 0;
				if(videoList!=null&&videoList.size()>0){
					num =  videoList.size();
				}
				return num;
			} 
			
			public Object getItem(int arg0) {
				if(videoList!=null&&videoList.size()>0){
				  return videoList.get(arg0);
				}
				return null;
			} 
			
			public long getItemId(int position) { 
				return position;
			} 
			
			public View getView(int position, View convertView, ViewGroup parent) {
//				Bitmap bitmap = null;
//				Bitmap bnp = null;
				ViewHolder holder;
				if(convertView == null){ 
					holder = new ViewHolder(); 
					convertView = LayoutInflater.from(context).inflate(R.layout.list_item, null);
					holder.tx1 = (ImageView) convertView.findViewById(R.id.imageview);
					holder.tx2 = (TextView) convertView.findViewById(R.id.ListItemContent); 
					convertView.setTag(holder);
				}else{ 
					holder = (ViewHolder) convertView.getTag();
				} 
				holder.tx2.setText(videoList.get(position).getTitle());
				int flags = videoList.get(position).getFlags();
				if(flags == 1){
					holder.tx1.setBackgroundResource(R.drawable.youku_icon); 
				}else if(flags == 2){
					holder.tx1.setBackgroundResource(R.drawable.qiyi); 
				}else if(flags == 3){
					holder.tx1.setBackgroundResource(R.drawable.tudou); 
				}else if(flags == 4){
					holder.tx1.setBackgroundResource(R.drawable.leshiwang); 
				}else if(flags == 5){
					holder.tx1.setBackgroundResource(R.drawable.kuliuwang); 
				}else if(flags == 6){
					holder.tx1.setBackgroundResource(R.drawable.wuliuwang); 
				}else if(flags == 7){
					holder.tx1.setBackgroundResource(R.drawable.jidongwang); 
				}else if(flags == 8){
					holder.tx1.setBackgroundResource(R.drawable.pptv); 
				}else if(flags == 9){
					holder.tx1.setBackgroundResource(R.drawable.dianyingwang); 
				}else if(flags == 10){
					holder.tx1.setBackgroundResource(R.drawable.maishiwang); 
				}else if(flags == 11){
					holder.tx1.setBackgroundResource(R.drawable.sina); 
				}else if(flags == 12){
					holder.tx1.setBackgroundResource(R.drawable.fenghuang); 
				}else if(flags == 13){
					holder.tx1.setBackgroundResource(R.drawable.video1); 
				}else if(flags == 14){
					holder.tx1.setBackgroundResource(R.drawable.yibaitv); 
				}else if(flags == 15){
					holder.tx1.setBackgroundResource(R.drawable.maishi); 
				}
				else if(flags == 16){
					holder.tx1.setBackgroundResource(R.drawable.wulongsha); 
				} 
				else if(flags == 17){
					holder.tx1.setBackgroundResource(R.drawable.menhu); 
				} else if(flags == 19){
					holder.tx1.setBackgroundResource(R.drawable.times);
				}else if(flags == 20){
					holder.tx1.setBackgroundResource(R.drawable.kankanxinwenw);
				} else if(flags == 18){
					holder.tx1.setBackgroundResource(R.drawable.shouhuship);
				}else if(flags == 21){
					holder.tx1.setBackgroundResource(R.drawable.wanhuatong);
				}
				return convertView;
			}
		} 
		class ViewHolder{
			
			public ImageView tx1;
			public TextView tx2;
		}
//		 
		@Override
		protected View setCententView() { 
			return  inflater.inflate(R.layout.net_video_activity, null);
		}

		@Override
		protected void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
//			setImageView01(R.drawable.action_stream);
		}

		@Override
		protected void titleLeftButton() {
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}

		@Override
		protected void titlRightButton() {
			if(!flag) { 
				 setTitleRightButtonBackbound(R.drawable.listitem_bg) ; 
				 mFlipper.scrollRight(); 
			}else{
				 setTitleRightButtonBackbound(R.drawable.gridview_bg) ;  
				 mFlipper.scrollLeft(); 
			}	 
			flag = !flag; 
		}
}
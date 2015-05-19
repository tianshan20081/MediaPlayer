package com.weichuang.china.video.local;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.china.R;
import com.weichuang.china.setinfo.VideoInfo;
import com.weichuang.china.util.Utils;
/**
 * 
 * @author yangguangfu
 *
 */
public class VideoListAdapter extends BaseAdapter
{
  private LayoutInflater mInflater;
  private Bitmap video;
  private ArrayList<VideoInfo> mLinkedList;
  public VideoListAdapter(Context context, ArrayList<VideoInfo> mLinkedList)
  {
    /* 参数初始化 */
    mInflater = LayoutInflater.from(context);
   this.mLinkedList = mLinkedList;
    video = BitmapFactory.decodeResource(context.getResources(),
            R.drawable.video_tag);
  }
  
  /* 继承BaseAdapter，需重写method */
 
  public int getCount()
  
  { 
	  if(mLinkedList!=null){
		  return mLinkedList.size();
     }
    return 0;
  }
 
  public Object getItem(int position)
  {
	  if(mLinkedList!=null){
		  mLinkedList.get(position);
     }
    return null;
  }
  

  public void setList(ArrayList<VideoInfo> mLinkedList)
  {
    this.mLinkedList = mLinkedList;
  }
  
  
 
  public long getItemId(int position)
  {
    return position;
  }
  
  
  public View getView(int position,View convertView,ViewGroup parent)
  {
    ViewHolder holder;
    
    if(convertView == null)
    {
      convertView = mInflater.inflate(R.layout.file_list_item, null);
      holder = new ViewHolder();
      holder.icon = (ImageView) convertView.findViewById(R.id.imageview);
      holder.text = (TextView) convertView.findViewById(R.id.ListItemContent);
      holder.text2 = (TextView) convertView.findViewById(R.id.block_size);
      
     
      
      convertView.setTag(holder);
    }
    else
    {
      holder = (ViewHolder) convertView.getTag();
    }
    
    if(mLinkedList!=null&&mLinkedList.size()>0){
    	VideoInfo videoInfo =mLinkedList.get(position);
    	if(videoInfo!=null){
    		 holder.text.setText(videoInfo.getTitle());
	    	  String  fileStr =  videoInfo.getUrl();
	    	  File file = new File(fileStr);
	    	  if(getMIMEType(file)){
	    		 holder.icon.setImageBitmap(video);
    	      }
	    	 float fileSize =  videoInfo.getFileSize();
	    	if(fileSize>(1024*1024)){
	    		fileSize = (fileSize/1024);
		    	if(fileSize>(1024)){
		    		 fileSize = (fileSize/1024);
		    		 if(fileSize>(1024)){
		    			 fileSize = Math.round((fileSize/1024)*100)/100f;
		    			 holder.text2.setText(fileSize+"GB");
		    		 }else{
		    			 fileSize = Math.round(fileSize*100)/100f;
		    			 holder.text2.setText(fileSize+"MB");
		    		 }
		    		
		    	}else{
		    		 fileSize = Math.round(fileSize*100)/100f;
		    		 holder.text2.setText(fileSize+"MB");
		    	}
	    	}else{
	    		if(fileSize>(1024)){
		    		 fileSize = (fileSize/1024);
	    			fileSize = Math.round(fileSize*100)/100f;
	    			holder.text2.setText(fileSize+"KB");
	    		}else{
	    			fileSize = Math.round(fileSize*100)/100f;
	    			holder.text2.setText(fileSize+"KB");
	    		}
	    		 
	    	}
	    	
	    	
	    	 
    	}
		 
    }

      
    return convertView;
  }
	private boolean getMIMEType(File f) {
		boolean isVideo = false;
		String fName = f.getName();
		String end = fName
				.substring(fName.lastIndexOf(".") + 1, fName.length())
				.toLowerCase();

		if (Utils.isVideoFile(end)) {
			isVideo = true;
		}
		else {
			isVideo = false;
		}
		return isVideo;
	}
  
  private static class ViewHolder
  {
    TextView text;
    TextView text2;
    ImageView icon;
  }
}


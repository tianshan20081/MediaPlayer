package com.weichuang.china.util;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Xml;

import com.weichuang.china.setinfo.VideoInfo;
 
public class ReadXmlByPullService {
	
	public static ArrayList<VideoInfo> ReadXmlByPull (InputStream inputStream)throws Exception
	{
		ArrayList<VideoInfo> videoList = null;
		 
		XmlPullParser xmlpull = Xml.newPullParser();
		 
		xmlpull.setInput(inputStream, "utf-8"); 
		int eventCode = xmlpull.getEventType(); 
		VideoInfo videoInfo = null;
		while(eventCode!=XmlPullParser.END_DOCUMENT)
		{
			
			switch (eventCode)
			{
				case XmlPullParser.START_DOCUMENT:
				{ 
					videoList =new  ArrayList<VideoInfo>();
					break;
				}
				case XmlPullParser.START_TAG:
				{
					if("video".equals(xmlpull.getName())) {
						videoInfo =  new VideoInfo(); 
					}else if (videoInfo!=null) {
						if(("title".equals(xmlpull.getName()))) {
						videoInfo.setTitle(xmlpull.nextText());
						}else if ("url".equals(xmlpull.getName())){ 
						videoInfo.setUrl(xmlpull.nextText());
						}else if("flags".equals(xmlpull.getName())){
						 videoInfo.setFlags(Integer.parseInt(xmlpull.nextText()));
						}else if("image".equals(xmlpull.getName())){
						 videoInfo.setImage(xmlpull.nextText());
						}
					}
					break;
				}
					
				case XmlPullParser.END_TAG:
				{ 
					if("video".equals(xmlpull.getName())&&videoInfo!=null)
					{
						videoList.add(videoInfo);
						videoInfo = null;
					}
					break;
				}
			}
			
			eventCode = xmlpull.next();
			
			
		}
		
		return videoList;
	}

}

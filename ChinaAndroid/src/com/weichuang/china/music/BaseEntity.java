/**
 * <b>File Name:</b>BaseEntity.java<br />
 *
 * <b>Description:</b>This class provides the basic abstraction of the entities used in the
 * application. <br />
 *
 * <b>History:
 * 2009.10.26  /jinhui/modified 
 * 2009-07 - Consuela - created </b>
 * 2009.10.21 - Consuela - Remove unused import
 *
 * Copyright (c) 2009 VanceInfo  Ltd.
 * <br />
 * All Rights Reserved.
 *
 * @author Johnny & consuela.
 * @version 1.0
 * <br />
 */
package com.weichuang.china.music;
import android.graphics.drawable.Drawable;
public abstract class BaseEntity {
	@SuppressWarnings("unused")
	private static String TAG = "BaseEntity";
	public static final int INVALID_ID = -1;
	protected long mId = INVALID_ID;
	protected String mExtId;
	protected String mName;
	//FIXME: if all project used icon. to delete me.
	protected Drawable mIcon;
	protected boolean mIsRemote;
	protected String icon;
	public void setId(long id) {
		mId = id ;
	}
//	public void setIsRemote(boolean isRemote) {
//		mIsRemote = isRemote;
//	}
//	public void setExtId(String extId) {
//		mExtId = extId;
//	}
//	public void setName(String name) {
//		this.mName = name;
//	}
//	public void setIcon(Drawable icon) {
//		mIcon = icon;
//	}
//	public void setIconPath(String icon) {
//		this.icon = icon;
//	}
	/*
	 *
	 * Below are get methods
	 */
	public long getId() {
		return mId;
	}
//	public String getIdStr() {
//		return String.valueOf(mId);
//	}
//	public String getExtId() {
//		return mExtId;
//	}
//	//modified by jinhui
//	public String getName() {
//	    if(mName!=null&&mName.length()>MAX_TRACKNAME_LEN){
//	        return mName.substring(0, MAX_TRACKNAME_LEN-1);
//	    }
//		return mName;
//	}
//	public Drawable getIcon() {
//		return mIcon;
//	}
//	public String getIconPath() {
//		return icon;
//	}
//	public boolean isRemote() {
//		return mIsRemote;
//	}
//	@Override
//	public boolean equals(Object o) {
//		if (o == null || mName == null) {
//			return false;
//		}		
//		return (mId == ((BaseEntity) o).mId) && mName.equals(((BaseEntity) o).mName);
//
//	}
}
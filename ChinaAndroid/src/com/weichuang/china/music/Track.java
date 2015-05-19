/**
 * <b>File Name:</b>TrackNew.java<br />
 *
 * <b>Description:<br />
 * Track is a Data class for Track. <br /></b>
 *
 * <b>History:
 * 2009-09 /Consuela/created </b>
 *
 * Copyright (c) 2009 VanceInfo Ltd.<br />
 * All Rights Reserved.
 *
 * @author consuela.
 * @version 1.0 <br />
 */
package com.weichuang.china.music;

public class Track extends BaseEntity{
	private static final String TAG = "Track";
	private static final long INVALID_ID = -1;
	private long mTrackId;
	private String mUri;
	private String mLyrics;
	private boolean mLyricParsed = false;
	private int mType;
	private long mDuration;
	private long mSize;
	private String mPath;
	private long mAlbumId;
	private long mArtistId;
	private String mAlbumName;
	private String mArtistName;
	private String mTrackName;
	private boolean mMyFavouriteFlag = false;
	private String mFirstAlphabet;
	private String mAlbumArtPath = null;
//	private String mAlbumPhotoUri;
//	private String mArtistPhotoUri;
//	private boolean mIsPlaying;
//	private boolean mIsRecordSoundTrack;
//	private boolean mDeletedFlag;
//	/**
//	 * @param boolean isRemote Is remote : true Is local : false This is a
//	 *        necessary param.
//	 */
//	public Track(boolean isRemote) {
//		this(INVALID_ID, isRemote);
//	}
	public Track() {
		this(INVALID_ID);
	}
	/**
	 * <P>
	 * 1.boolean isRemote Is remote : true Is local : false This is a necessary
	 * param.
	 * </P>
	 * <P>
	 * 2.int id This is a unique param from database.
	 * </P>
	 */
	public Track(long id) {
		mId = id;
	}

//	public void setAlbumPhotoUri(String albumPhotoUri) {
//		mAlbumPhotoUri = albumPhotoUri;
//	}
	public void setLyric(String lyrics) {
		mLyrics = lyrics;
	}
	public void setPath(String path) {
		mPath = path;
	}
	public void setTrackId(long id) {
		mTrackId = id;
	}
	public void setTrackName(String name) {
		mTrackName = name;
	}
	public void setAlbumArtPath(String path) {
		mAlbumArtPath = path;
	}
//	public void setArtistPhotoUri(String artistPhotoUri) {
//		mArtistPhotoUri = artistPhotoUri;
//	}
	public void setArtistName(String artistName) {
		mArtistName = artistName;
	}
	public void setAlbumId(long albumId) {
		mAlbumId = albumId;
	}
	public void setAlbumName(String albumName) {
		mAlbumName = albumName;
	}
	public void setDuration(long time) {
		mDuration = time;
	}
	public void setLyricParsed(boolean parsed) {
		mLyricParsed = parsed;
	}
	public void setMyFavouriteFlag(boolean isMyFavourite) {
		mMyFavouriteFlag = isMyFavourite;
	}
	public void setFirstAlphabet(String alphabet) {
		mFirstAlphabet = alphabet;
	}
//	public void setIsRecordSoundTrack(boolean isRecordSound) {
//		mIsRecordSoundTrack = isRecordSound;
//	}
//	public void setIsPlaying(boolean isPlaying) {
//		mIsPlaying = isPlaying;
//	}
//	public void setDeletedFlag(boolean isDeleted) {
//
//		mDeletedFlag = isDeleted;
//	}
	/**
	 * Set track type .
	 * This Type is file 's type.
	 * @param String type
	 */
	public void setType(int type) {
		mType = type;
	}
	public void setSize(long size) {
		mSize = size;
	}
	public void setUri(String uri) {
		mUri = uri;
	}
	public void setArtistId(long artistId) {
		mArtistId = artistId;
	}

	/*
	 *
	 * Below are get methods
	 */
	public long getTrackId() {
		return mTrackId;
	}
	public long getSize() {
		return mSize;
	}
	public String getAlbumArtPath() {
		return mAlbumArtPath;
	}
	public String getTrackName() {
		return mTrackName;
	}
	public String getArtistName() {
		return mArtistName;
	}
	public String getLyric() {
		return mLyrics;
	}
	public long getAlbumId() {
		return mAlbumId;
	}
	public long getArtistId() {
		return mArtistId;
	}
	public long getDuration() {
		return mDuration;
	}
	public String getUri() {
		return mUri;
	}
	public boolean isLyricParsed () {
		return mLyricParsed;
	}
	public boolean getMyFavouriteFlag() {
		return mMyFavouriteFlag;
	}
//	public String getAlbumPhotoUri() {
//		return mAlbumPhotoUri;
//	}
//	public String getArtistPhotoUri() {
//		return mArtistPhotoUri;
//	}
	public String getAlbumName() {
		return mAlbumName;
	}
	public String getPath() {
		return mPath;
	}
//	public boolean isRecordSoundTrack() {
//		return mIsRecordSoundTrack;
//	}
	public int getType() {
		return mType;
	}
	public String getFirstAlphabet() {
		return mFirstAlphabet;
	}
//	public boolean isPlaying() {
//		return mIsPlaying;
//	}
//	public boolean isDeletedFlag() {
//		return mDeletedFlag;
//	}
//	@Override
//	public String toString() {
//		StringBuilder buffer = new StringBuilder();
//		buffer.append("Track address:" + super.toString()).append(" id:")
//				.append(mId).append("ExtId:").append(getExtId()).append(" trackName:").append(getName()).append(
//						" Url: ").append(getUri()).append(" Path:").append(getPath()).append(" albumname:")
//						.append(getAlbumName()).append(" artistname:")
//						.append(getArtistName()).append(">>>>Icon :").append(getIcon() == null)
//						.append("  icon:").append(icon).append(" mUri:").append(mUri).append(" album path:").append(mAlbumPhotoUri);
//		return buffer.toString();
//	}

}
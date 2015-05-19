package com.weichuang.china.music.coverflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.MediaStore;

import com.android.china.R;
import com.weichuang.china.music.MusicUtils;



public class CoverFlowAdapter {
	private Context mContext;
	// private int[] mDrawableRes;
	// added by liuqiang
	private Cursor mCursor;
	private int mAlbumIDIdx;
	private int mUnknownAlbum;
	// end add
	private List<Cover> mCovers;
//	private ArrayList<Cover> mCovers;
	//private int mItemCount = 0;
	
	private boolean mDataValid;
	private CoverFlow mCoverFlow;
	
	private CoverChangeObserver mChangeObserver = new CoverChangeObserver(new Handler());
	private CoverSetObserver mDatasetObserver = new CoverSetObserver();

	public CoverFlowAdapter(Context context, Cursor cursor) {
		// added by liuqiang
		mContext = context;
		boolean cursorPresent = cursor != null;
		
		if (cursorPresent) {
			mCursor = cursor;
			mAlbumIDIdx = cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
			//mItemCount = cursor.getCount();
			cursor.registerContentObserver(mChangeObserver);
			cursor.registerDataSetObserver(mDatasetObserver);
		}
		mDataValid = cursorPresent;
		mUnknownAlbum = R.drawable.albumart_mp_unknown;
		mCovers = Collections.synchronizedList(new ArrayList<Cover>());

		// end add
		// comment out by liuqiang
		// mDrawableRes = resIds;
		// mContext = c;
		// mItemCount = resIds.length;
	}

	public void setCursor(Cursor cursor) {
		if (cursor != null) {
			mCursor = cursor;
			mAlbumIDIdx = cursor
					.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
			//mItemCount = cursor.getCount();
			mDataValid = true;
			initCovers();
			cursor.registerContentObserver(mChangeObserver);
			cursor.registerDataSetObserver(mDatasetObserver);
			if(mCoverFlow != null){
				mCoverFlow.requestRender();
			}
		}
	}

	public int getCount() {
		 if (mDataValid && mCursor != null) {
	            return mCursor.getCount();
	        } else {
	            return 0;
	        }
	}

	public void initCovers() {
		final int count = getCount();
		final int albumeIdIndex = mAlbumIDIdx;
		Cursor cursor = mCursor;
//		ArrayList<Cover> covers = mCovers;
		List<Cover> covers = mCovers;
		if(!covers.isEmpty()){
			covers.clear();
		}
		int artistColumnIndex = cursor
				.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
		int albumeColumnIndex = cursor
				.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
		for (int i = 0; i < count; i++) {
			if (!cursor.moveToPosition(i)) {
				throw new IllegalStateException(
						"couldn't move cursor to position " + i);
			}
			long album_id = cursor.getLong(albumeIdIndex);
			Cover cover = new Cover(mContext, album_id);
			// modified by liuqiang temp
			cover.setAlbumInfo(MusicUtils.getArtistName(mContext, cursor.getString(artistColumnIndex)), 
					MusicUtils.getAlbumName(mContext, cursor.getString(albumeColumnIndex)));		
			// cover.setAlbumInfo(HzToPy.UnknowntoUTF8(cursor.getString(artistColumnIndex)),HzToPy.UnknowntoUTF8(cursor.getString(albumeColumnIndex)));
			// end modification
			covers.add(i, cover);
		}

	}

	public void prepareCoverTextures(GL10 gl) {
//		ArrayList<Cover> covers = mCovers;
		List<Cover> covers = mCovers;
		for (Cover cover : covers) {
			cover.genCoverTexture(gl);
		}
	}

	public Cover getCover(int position) {
		if (position < 0 || position >= getCount() || mCovers.isEmpty() || position >= mCovers.size())
			return null;
		
		return mCovers.get(position);
	}

	public static final Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
		int srcWidth = bitmap.getWidth();
		int srcHeight = bitmap.getHeight();
		int width = maxSize;
		int height = maxSize;
		boolean needsResize = false;
		if (srcWidth > srcHeight) {
			if (srcWidth > maxSize) {
				needsResize = true;
				height = ((maxSize * srcHeight) / srcWidth);
			}
		} else {
			if (srcHeight > maxSize) {
				needsResize = true;
				width = ((maxSize * srcWidth) / srcHeight);
			}
		}
		if (needsResize) {
			Bitmap retVal = Bitmap.createScaledBitmap(bitmap, width, height,
					true);
			bitmap.recycle();
			return retVal;
		} else {
			return bitmap;
		}
	}

	public void resetCoverTextures(int defaultTextureId) {
//		ArrayList<Cover> covers = mCovers;
		List<Cover> covers = mCovers;
		for (Cover cover : covers) {
			if (cover.mState != Cover.INVALID_TEXTURE) {
				cover.mTextureId = 0;
				if (cover.mBitmap != null && !cover.mBitmap.isRecycled()) {
					cover.mState = Cover.STATE_LOADED;
				} else {
					cover.mState = Cover.STATE_UNLOADED;
				}
			} else {
				cover.mTextureId = defaultTextureId;
				cover.albumTexture.mState = Texture.STATE_UNLOADED;
				cover.artistTexture.mState = Texture.STATE_UNLOADED;
			}

		}
	}
	
	public void setCoverFlow(CoverFlow cf){
		mCoverFlow = cf;
	}
	private void onContentChanged(){
		setNeedRefresh(true);
		//do nothing before implement texture manage
//		 if (mCursor != null && !mCursor.isClosed()) {
//	           mCursor.requery();
//	        }
	}
	private class CoverChangeObserver extends ContentObserver{

		public CoverChangeObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			onContentChanged();
		}
		public boolean deliverSelfNotifications() {
			// TODO Auto-generated method stub
			return true;
		}
	}
	
	private class CoverSetObserver extends DataSetObserver{
		public void onInvalidated() {
			mDataValid = false;
			
		}
		
		public void onChanged() {
			mDataValid = true;
			initCovers();
			if(mCoverFlow != null){
				mCoverFlow.requestRender();
			}
		}
	}
	
	private boolean mNeedInitCover = false;
	public void setNeedRefresh(boolean needInitCover){
		mNeedInitCover = needInitCover;
	}
	
	public boolean isNeedInit(){
		return mNeedInitCover;
	}
	
	public Cursor getCursor(){
		return mCursor;
	}
}

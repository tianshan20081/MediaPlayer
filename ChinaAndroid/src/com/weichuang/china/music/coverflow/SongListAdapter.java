package com.weichuang.china.music.coverflow;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MusicUtils;
import com.weichuang.china.util.Utils;

public class SongListAdapter extends CursorAdapter {
	private boolean mFillBlankItem;
	private Context mContext;
	private Cursor mCursor;

	public Cover mCurrenCover;
	public Cursor mCurrentCursor;
	public boolean isWaitingForCursor;

	private LayoutInflater mInflater;
	private ArrayList<Long> mAddedSongArray;

	private ArrayList<SongTag> mSongTags;

	public CoverFlowBackView mCoverFlowBackView;

	private boolean isAllSongAdded;
	private int mAddedSongsCount;
	protected LayoutParams layoutParams = null;

	private OnCheckedChangeListener mAddIconListener = new OnCheckedChangeListener() {

		public void onCheckedChanged(CompoundButton v, boolean isChecked) {
			SongTag song = (SongTag) v.getTag();
			long id = song.musicId;
			CheckBox cb = (CheckBox) v;
			if (isChecked != song.isAddedToList) {
				if (isChecked) {
					long[] ids = { id };
					MusicUtils.addToPlaylist(mContext, ids, true);
					song.isAddedToList = true;
					mAddedSongsCount++;
					
					updateAlbumAddIndicator();

				} else {
					MusicUtils.removeFromPlaylist(mContext, new long[] { id }, true);
					song.isAddedToList = false;
					mAddedSongsCount--;
					updateAlbumAddIndicator();
				}
			}
		}
	};


	public SongListAdapter(Context context, Cursor c) {
		super(context, c);
		mContext = context;
		mFillBlankItem = false;
		mCursor = c;

		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mAddedSongArray = new ArrayList<Long>(12);
		mSongTags = new ArrayList<SongListAdapter.SongTag>(16);
		isAllSongAdded = false;
		mAddedSongsCount = 0;
		
		TagDataSetObserver observer = new TagDataSetObserver();
		registerDataSetObserver(observer);
	}

	@Override
	public int getCount() {

		int count = super.getCount();
		if (count == 0) {
			return count;
		}
		if (mFillBlankItem) {
			if (count < 9) {
				return 9;
			}
		}
		return count;

	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		if (cursor == null) {
			view.setVisibility(View.INVISIBLE);
		} else {
			TextView song = (TextView) view.findViewById(R.id.songname);
			TextView time = (TextView) view.findViewById(R.id.duration);
			ImageView playicon = (ImageView) view
					.findViewById(R.id.playindicator);
			CheckBox addicon = (CheckBox) view.findViewById(R.id.addicon);
			
			
			int position = cursor.getPosition();
			
			SongTag tag = mSongTags.get(position);
			
			//modified by liuqiang
			StringBuilder title = new StringBuilder();
			title.append(position + 1);
			title.append(".");
			title.append(tag.displayName);
			song.setText(title);
//			song.setText(tag.displayName);
			//end modification
			time.setText(MusicUtils.makeTimeString(context, tag.duration / 1000));
			
			if(tag.isPlaying){
				playicon.setVisibility(View.VISIBLE);
			}else{
				playicon.setVisibility(View.INVISIBLE);
			}
			float widthd = Utils.getWidthDpi(BaseActivity.mBaseActivity);
//add by yangguangfu
			 if( widthd <= 162) {
				 song.setTextSize(16);
				 time.setTextSize(16);
				 
			 }else{
				 song.setTextSize(20);
				 time.setTextSize(20);
				 
			 }

			view.setTag(tag);
			addicon.setTag(tag);
			addicon.setChecked(tag.isAddedToList);


		}

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.songs_of_album_songinfo, parent,
				false);
		CheckBox addicon = (CheckBox) view.findViewById(R.id.addicon);
		addicon.setOnCheckedChangeListener(mAddIconListener);

		return view;
		// return tv;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position < getCursor().getCount()) {
			return super.getView(position, convertView, parent);
		} else {
			View v;
			if (convertView == null) {
				v = newView(mContext, null, parent);
			} else {
				v = convertView;
			}
			bindView(v, mContext, null);
			return v;
		}
	}

	@Override
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return super.isEmpty();
	}

	public class SongListItem extends LinearLayout {
		public boolean isAddedToList;

		public SongListItem(Context context, AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
		}

		public SongListItem(Context context) {
			super(context);
		}

	}

	public class SongTag {
		public boolean isAddedToList;
		public boolean isPlaying;
		public long musicId;
		public String displayName;
		public long duration;

		public SongTag(long id, boolean isplaying, boolean isadded,
				String name, long dur) {
			musicId = id;
			isAddedToList = isadded;
			isPlaying = isplaying;
			displayName = name;
			duration = dur;
		}
	}

	private void updateAlbumAddIndicator() {
		isAllSongAdded = checkAllSongsAdded();
		mCoverFlowBackView.setAddAlbum(isAllSongAdded);

	}

	public long[] getSongList() {
		long[] songlist = MusicUtils.getSongListForCursor(getCursor());
		return songlist;
	}

	public void initTags(Cursor cursor){
		ArrayList<SongTag> tags = mSongTags;
		long playingId = MusicUtils.getCurrentAudioId();
		tags.clear();
		
		if(cursor !=null){
			int count = mAddedSongsCount;
			if(cursor.moveToFirst()){
				int idIndex = cursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				int displayNameIndex= cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				int durationIndex = cursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
				do{
				
				long id = cursor.getLong(idIndex);
				boolean isPlaying = id == playingId;
				boolean isAdded = MusicUtils.hasAddToPlaylist(id);
				if(isAdded)count++;
				//modified by liuqiang
				String displayname = MusicUtils.getSongName(mContext, cursor.getString(displayNameIndex));
//				String displayname = HzToPy.UnknowntoUTF8(cursor.getString(displayNameIndex));
				//end modification
				long duration = cursor.getLong(durationIndex);
				SongTag tag = new SongTag(id, isPlaying, isAdded,displayname,duration);
				mSongTags.add(tag);
				
				}while(cursor.moveToNext());
			}
			mAddedSongsCount = count;
			isAllSongAdded = checkAllSongsAdded();
			updateAlbumAddIndicator();
		}
	}

	public boolean checkAllSongsAdded() {
		int count = getCount();
		if (count == 0) {
			return false;
		}
		return mAddedSongsCount == count;
	}
	
	public class TagDataSetObserver extends DataSetObserver{
		@Override
		public void onChanged() {
			Cursor cursor = getCursor();
			if (cursor != null) {
				mCursor = cursor;
				mAddedSongArray.clear();
				isAllSongAdded = false;
				mAddedSongsCount = 0; 
				initTags(cursor);
			}

		}
		@Override
		public void onInvalidated() {
			mCursor = getCursor();
			mAddedSongArray.clear();
			isAllSongAdded = false;
			mAddedSongsCount = 0; 
			super.onInvalidated();
		}
	}
	
	
}

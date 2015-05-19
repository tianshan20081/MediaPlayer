package com.weichuang.china.music.coverflow;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MusicUtils;
import com.weichuang.china.music.NowPlayingActivity;
import com.weichuang.china.util.Utils;


public class CoverFlowBackView extends LinearLayout {
	private CoverFlow mCoverFlow;
	private ListView mAlbumSongList;
	private RelativeLayout mAlbumInfo;


	private CheckBox mAddAlbum;
	private TextView mArtistName;
	private TextView mAlbumeName;

	private Paint mDividerPaint;
	private SongListAdapter mAlbumSongListAdapter;
	private Context mContext;

	public CoverFlowBackView(Context context) {
		super(context);
		setOrientation(VERTICAL);
		mDividerPaint = new Paint();
		mDividerPaint.setColor(getResources().getColor(R.color.divider_color));
		mContext = context;
	}

	public CoverFlowBackView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);
		mDividerPaint = new Paint();
		mDividerPaint.setColor(getResources().getColor(R.color.divider_color));
		mContext = context;

	}

	@Override
	protected void onFinishInflate() {
		Context context = mContext;
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(50);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(100);
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(
				set, 0.5f);

		mAlbumSongList = (ListView) findViewById(R.id.songsofalbum);
		mAlbumSongList.setLayoutAnimation(controller);
		mAlbumSongList.setChoiceMode(ListView.CHOICE_MODE_NONE);
		
		mAlbumSongListAdapter = new SongListAdapter(context, null);
		mAlbumSongListAdapter.mCoverFlowBackView = this;

		mAlbumSongList.setAdapter(mAlbumSongListAdapter);
		
		mAlbumInfo = (RelativeLayout) findViewById(R.id.albuminfo);

		mAddAlbum = (CheckBox) findViewById(R.id.addalbum);
		mArtistName = (TextView) findViewById(R.id.artistname);
		mAlbumeName = (TextView) findViewById(R.id.albumname);

		mAlbumInfo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mCoverFlow.flipCenterCover();
			}
		});
		
		mAddAlbum.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				CheckBox cb = (CheckBox)v;
				if(cb.isChecked()){
				   MusicUtils.addToPlaylist(mContext, mAlbumSongListAdapter.getSongList(), true);
				}else{
				   MusicUtils.removeFromPlaylist(mContext, mAlbumSongListAdapter.getSongList(), true);
				}
				mAlbumSongListAdapter.notifyDataSetChanged();
			}
		});
		mAlbumSongList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (MusicUtils.sService != null) {
					try {
						SongListAdapter adapter = (SongListAdapter) parent.getAdapter();
						MusicUtils.sService.open(adapter.getSongList(), position);
						registerMonitorCursor();
						MusicUtils.sService.play();

//						Intent intent = new Intent("com.weichuang.china.music.PLAYBACK_VIEWER")
//	                		.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						Intent intent = new Intent(mContext,NowPlayingActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mContext.startActivity(intent);

						((SongListAdapter) (mAlbumSongList.getAdapter()))
								.notifyDataSetChanged();

						return;
					} catch (RemoteException ex) {
					}
				}
			}
		});

		super.onFinishInflate();
	}

	private void registerMonitorCursor() {
		if ( mCoverFlow != null ) {
			long album_id = -1;
			if ( mCoverFlow.getCurrentCenteredCover() != null ) {
				album_id = mCoverFlow.getCurrentCenteredCover().getAlbumId();
			}
			if ( album_id != -1 ) {
				String where = MediaStore.Audio.Media.ALBUM_ID + "=" + album_id
					+ " AND " + MediaStore.Audio.Media.IS_MUSIC + "=1";
				if (MusicUtils.sService != null) {
					try {
						MusicUtils.sService.registerMonitorCursor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(), 
								null, where, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, null, false);
					} catch (RemoteException ex) {
						Log.e("CoverFlowBackView", "registerMonitorCursor()===========album_id=" + album_id + "caught exception: " + ex);
					}
				}
			}
		}
	}
	
	public void setCoverFlow(CoverFlow coverflow) {
		mCoverFlow = coverflow;
	}

	public void setTitle(Cover cover) {
		
		float widthd = Utils.getWidthDpi(BaseActivity.mBaseActivity);
		 if( widthd <= 162) {
			 mArtistName.setText(cover.artistName);
			 mAlbumeName.setText(cover.albumName);
			 mArtistName.setTextSize(12);
			 mAlbumeName.setTextSize(16);
		 }else{
			 mArtistName.setText(cover.artistName);
			 mAlbumeName.setText(cover.albumName);
		 }
		
	}
	

	public void show() {
		mAlbumSongList.scheduleLayoutAnimation();
		
		this.setVisibility(View.VISIBLE);
	//	mCoverFlow.mHoldOn = true;
			
		
	}

	public void hide() {
			this.setVisibility(View.GONE);		
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		// TODO Auto-generated method stub

		super.dispatchDraw(canvas);
	//	mCoverFlow.mHoldOn = false;
		

	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		ListView lv = mAlbumSongList;
		canvas.drawLine(lv.getLeft(), lv.getTop(), lv.getRight(), lv.getTop(),
				mDividerPaint);
	}
	
	public void clearBackground(){
		mAlbumInfo.setBackgroundDrawable(null);
		mAlbumSongList.setBackgroundColor(getResources().getColor(R.color.transparent_color));
		mAddAlbum.setVisibility(View.VISIBLE);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
		
	}
	
	public void setAddAlbum(boolean added){
		mAddAlbum.setChecked(added);
	}
}

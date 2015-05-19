package com.weichuang.china.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.farng.mp3.MP3File;
import org.farng.mp3.id3.AbstractID3v2;
import org.farng.mp3.lyrics3.AbstractLyrics3;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MusicUtils.ServiceToken;
import com.weichuang.china.setinfo.SettingActivity;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.util.LogUtil;
import com.weichuang.china.video.view.UserPreference;

/**
 * TODO:We should support a way to play any resources we caller want.like web
 * resource,local files,even URI. And we will maintain a playing list,that's for
 * sure.
 * 
 * @author Gogo.
 * 
 */

public class NowPlayingActivity extends BaseActivity implements ServiceConnection {
	private static final String TAG = "NowPlayingActivity";
	private static final int QUIT = 2;
	private static final int GET_ALBUM_ART = 3;
	private static final int ALBUM_ART_DECODED = 4;
	private static final int GET_LYRIC = 5;
	private static final int LYRIC_PARSED = 6;
	// add by yangguangfu
	private static final int LOAD_LYRIC = 7;

	private Cursor mCursor;
	private boolean mOneShot = false;
	private boolean isPlaying = true;
	private IMediaPlaybackService mService = null;
	private int mCurrentOrientaion;
	private PageNowPlayingView mNowPlayingView;
	private boolean mIsLyricShown = false;

	private static final int START_MATRIX = 2;
	private static final int STOP_MATRIX = 3;
	private ProgressHandle mProgressHandle;
	// add by yangguangfu
	private LyricsShowHandler lyricsShowHandler;
	private Worker mAlbumArtWorker;
	private AlbumArtHandler mAlbumArtHandler;
	private Resources mResources;
//	private TextView mTrackNameView;
//	private TextView mAlbumNameView;
//	private TextView mArtistNameView;
//	private TextView mPlayTimeView;

//	private Button mPreviousButton;
	private Button mPlayPauseButton;
//	private Button mNextButton;
	private Button mPlayModeButton;
//	private Button mLyricSwitchButton;
//	private SeekBar mPlayProgressSeekBar;
//	private ImageView mAlbumArt;
//	private Bitmap mUnknownAlbum = null;
//	private ScrollView mLyricView = null;
//	private LinearLayout mLyricContainer = null;

	private ServiceToken mToken;
	private static final int REFRESH = 1;
	private long mLastSeekEventTime;
	private BitmapDrawable mLastAlbumArt;
	private String mLastLyric;
	private long mLastId;
	private long mPosOverride = -1;
	private boolean mFromTouch = false;
	private boolean mActive;

	// add by yangguangfu
	private long mPlayedProgress;
    private AudioManager mAudioManager = null;
    private int mAudioMax;
    private int currentVolume = 0;
    private int mAudioDisplayRange;
    private float mTouchY, mVol;
    private boolean mIsAudioChanged;
    private String[] mAudioTracks;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mToken = MusicUtils.bindToService(this, this);
		if (mToken == null) {
			// something went wrong
			// TODO: what need to do here
			finish();
		}
		ActivityHolder.getInstance().addActivity(this);
		setTopBarTitle(getString(R.string.str_music_name));
		 setTitleRightButtonHide();
		IntentFilter f = new IntentFilter();
		f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
		f.addAction(MediaPlaybackService.META_CHANGED);
		f.addAction(MediaPlaybackService.PLAYBACK_COMPLETE);
		registerReceiver(mStatusListener, new IntentFilter(f));
		mOneShot = getIntent().getBooleanExtra("oneshot", false);
		mResources = getResources();
		mCurrentOrientaion = mResources.getConfiguration().orientation;
		mAlbumArtWorker = new Worker("album art worker");
		mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
	    mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

//		if (mCurrentOrientaion == Configuration.ORIENTATION_LANDSCAPE) {
////			setContentView(R.layout.page_now_playing_confirm_land);
//			mNowPlayingView = (PageNowPlayingView) findViewById(R.id.page_now_playing_view_parent_land);
//		} 
//		else {
//			setContentView(R.layout.page_now_playing_confirm);
			mNowPlayingView = (PageNowPlayingView) findViewById(R.id.page_now_playing_view_parent);
//		}
		
		UserPreference.ensureIntializePreference(this);   
	    int defaultColor = UserPreference.read("defaultColor", 0);   
//	    LinearLayout background_id = (LinearLayout)findViewById(R.id.background_id); 
		if(defaultColor == 0){ 
			mNowPlayingView.setBackgroundResource(R.drawable.moren_beijing); 
        }else if(defaultColor == 1){
        	mNowPlayingView.setBackgroundResource(R.drawable.moren_beijing1); 
        }/*else if(defaultColor == 2){
        	mNowPlayingView.setBackgroundResource(R.drawable.moren_beijing2); 
        }else if(defaultColor == 3){
        	mNowPlayingView.setBackgroundResource(R.drawable.moren_beijing3); 
        }*/else{
        	mNowPlayingView.setBackgroundColor(defaultColor); 
        }
		mNowPlayingView.initialize(mCurrentOrientaion);
		mPlayModeButton = mNowPlayingView.getPlayModeButton();
		mPlayPauseButton = mNowPlayingView.getPlayPauseButton();
		setViewListeners();

		mProgressHandle = new ProgressHandle();
		lyricsShowHandler = new LyricsShowHandler();

		updateTrackInfo();
		queueRefresh();
		// setContentView(R.layout.now_playing_port);

		// initialize();
		// mAlbumArt = (ImageView)findViewById(R.id.albumart);
		// mLyricView = (ScrollView) findViewById(R.id.lyricview);
		// mLyricContainer = (LinearLayout) findViewById(R.id.lyric);
		// initDefaultAlbum();
		// mAlbumArt.setImageBitmap(mUnknownAlbum);
		// layoutByOrientation(mResources.getConfiguration().orientation);
	}

	private class ProgressHandle extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case START_MATRIX: {
				mNowPlayingView.startMatrixAnimation();
				break;
			}
			case STOP_MATRIX: {
				mNowPlayingView.stopMatrixAnimation();
				break;
			}
			}
		}
	}

	private void setViewListeners() {
		if (mNowPlayingView == null) {
			return;
		}
		mNowPlayingView.setPausePlayButtonClickListener(mPauseListener);
		mNowPlayingView.setNextPlayButtonClickListener(mNextListener);
		mNowPlayingView.setPreviousPlayButtonClickListener(mPrevListener);
		mNowPlayingView.setConfigModePlayButtonClickListener(mPlayModeListener);
		mNowPlayingView.setOnSeekBarDragListener(mSeekListener);
		mNowPlayingView
				.setOnCoverLyricsSwitcherClickListener(onCoverLyricsSwitcherClicklistener);

		// mNowPlayingView.setOnCoverFlowButtonClickListener(mCoverFlowClickListener);
		// mNowPlayingView.setRatingBarChangeListener(mRatingBarChangeListener);

		// mNowPlayingView.setOnArtistNameLongClickListener(mOnArtistNameLongClickListenter);
		// mNowPlayingView.setOnTrackNameLongClickListener(mOnTrackNameLongClickListener);
		// mNowPlayingView.setOnCoverLyricsSwitcherClickListener(onCoverLyricsSwitcherClicklistener);
	}

	// private void initialize() {
	// mTrackNameView = (TextView) findViewById(R.id.track_name);
	// mAlbumNameView = (TextView) findViewById(R.id.album_name);
	// mArtistNameView = (TextView) findViewById(R.id.artist_name);
	// mPlayTimeView = (TextView) findViewById(R.id.playingtime);
	//		
	// mPreviousButton = (Button) findViewById(R.id.previous_btn);
	// mPlayPauseButton = (Button) findViewById(R.id.play_pause_btn);
	// mNextButton = (Button) findViewById(R.id.next_btn);
	// mPlayModeButton = (Button) findViewById(R.id.play_mode);
	// mLyricSwitchButton = (Button) findViewById(R.id.switch_lyric);
	// setupControlButtonListener();
	//		
	// mPlayProgressSeekBar = (SeekBar) findViewById(R.id.progress_seekbar);
	// mPlayProgressSeekBar.setOnSeekBarChangeListener(mSeekListener);
	// }
	//	
	// private void setupControlButtonListener() {
	// mPreviousButton.setOnClickListener(mPrevListener);
	// mPlayPauseButton.setOnClickListener(mPauseListener);
	// mNextButton.setOnClickListener(mNextListener);
	// mPlayModeButton.setOnClickListener(mPlayModeListener);
	// }
	private static long CLICK_INTERVAL = 800;
	private long lastPauseTime;

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			long time = System.currentTimeMillis();
			if (time - lastPauseTime < CLICK_INTERVAL) {
				return;
			}
			lastPauseTime = time;
			doPauseResume();
		}
	};
	private long lastPrevTime;

	private View.OnClickListener mPrevListener = new View.OnClickListener() {
		public void onClick(View v) {
			long time = System.currentTimeMillis();
			if (time - lastPrevTime < CLICK_INTERVAL) {
				return;
			}
			if (mService == null)
				return;
			try {
				if (mService.isFirstTrack()) {
					MusicUtils.showToast(NowPlayingActivity.this,
							R.string.now_playing_already_first_track);
					return;
				}
				lastPrevTime = time;
				mService.prev();
			} catch (RemoteException ex) {
			}
		}
	};

	private long lastNextTime;

	private View.OnClickListener mNextListener = new View.OnClickListener() {
		public void onClick(View v) {
			long time = System.currentTimeMillis();
			if (time - lastNextTime < CLICK_INTERVAL) {
				return;
			}
			if (mService == null)
				return;
			try {
				if (mService.isLastTrack()) {
					MusicUtils.showToast(NowPlayingActivity.this,
							R.string.now_playing_already_last_track);
					return;
				}
				lastNextTime = time;
				mService.next();
			} catch (RemoteException ex) {
			}
		}
	};

	private View.OnClickListener mPlayModeListener = new View.OnClickListener() {
		public void onClick(View v) {
			changePlayMode();
		}
	};

	private OnClickListener onCoverLyricsSwitcherClicklistener = new OnClickListener() {
		public void onClick(View v) {
			mNowPlayingView.switchCoverLyricsContainer();
			mIsLyricShown = !mIsLyricShown;
		}
	};

	private void doPauseResume() {
		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				queueRefresh();
				setPauseButtonImage();
			}
		} catch (RemoteException ex) {
		}
	}

	// add by yangguangfu
	private class LyricsShowHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOAD_LYRIC:// 
				if (mService != null&&PageNowPlayingView.isFormatedByLrc) {
					
					if (mOneShot) {
						if (isPlaying) {
							// try {
							// mPlayedProgress =mService.position();
							// } catch (RemoteException e) {
							// // TODO Auto-generated catch block
							// e.printStackTrace();
							// }
							try {
								mPlayedProgress =mService.position();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							mNowPlayingView.updateLyrics(mPlayedProgress);
							lyricsShowHandler.removeMessages(LOAD_LYRIC);
							lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);

						} else {
							try {
								mPlayedProgress =mService.position();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							mNowPlayingView.updateLyrics(mPlayedProgress);
						}
						// if (myMediaPlayer.getCurrentPosition() != 0) {
						// curPosition = myMediaPlayer .getCurrentPosition();
						// }
						// if (!myMediaPlayer.isPlaying()) {
						// if (dia.isShowing()) {
						// break;
						// } else {
						// dia.show();
						// }
						// } else {
						// dia.dismiss();
						// }
					} else {
						if (isPlaying) {
							// try {
							// mPlayedProgress =mService.position();
							// } catch (RemoteException e) {
							// // TODO Auto-generated catch block
							// e.printStackTrace();
							// }
							try {
								mPlayedProgress =mService.position();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							mNowPlayingView.updateLyrics(mPlayedProgress);
							lyricsShowHandler.removeMessages(LOAD_LYRIC);
							lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);

						} else {
							try {
								mPlayedProgress =mService.position();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							mNowPlayingView.updateLyrics(mPlayedProgress);
						}
						//
						// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);

					}
					// tv_has_played.setText(toTime(curPosition));
					// seekbar.setProgress(curPosition);

				}
				break;
			case 2:
				// setPath(download_file.getAbsolutePath());
				break;
			}
			super.handleMessage(msg);
		}

	}

	private void changePlayMode() {
		if (mService == null) {
			return;
		}
		try {
			int repeatMode = mService.getRepeatMode();
			int shuffleMode = mService.getShuffleMode();
			Log.d(TAG, "changePlayMode(), repeatMode=" + repeatMode
					+ " shuffleMode=" + shuffleMode + "===========");
			if (repeatMode == MediaPlaybackService.REPEAT_NONE
					&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
				MusicUtils.showToast(this, R.string.play_mode_repeat_current);
			} else if (repeatMode == MediaPlaybackService.REPEAT_CURRENT
					&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
				MusicUtils.showToast(this, R.string.play_mode_repeat_all);
			} else if (repeatMode == MediaPlaybackService.REPEAT_ALL
					&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
				mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
				MusicUtils.showToast(this, R.string.play_mode_shuffle);
			} else {
				mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
				mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
				MusicUtils.showToast(this, R.string.play_mode_sequence);
			}
			Log.d(TAG, "at the end of changePlayMode(), repeatMode="
					+ mService.getRepeatMode() + " shuffleMode="
					+ mService.getShuffleMode() + "===========");
			setPlayModeButtonImage();

		} catch (RemoteException ex) {
		}
	}

	private void setPlayModeButtonImage() {
		Log.d(TAG, "setPlayModeButtonImage()=====================");
		if (mService != null) {
			try {
				int repeatMode = mService.getRepeatMode();
				int shuffleMode = mService.getShuffleMode();
				Log.d(TAG, "setPlayModeButtonImage(), repeatMode=" + repeatMode
						+ " shuffleMode=" + shuffleMode + "===========");
				if (repeatMode == MediaPlaybackService.REPEAT_NONE
						&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
//					mPlayModeButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_normal_order_selector),
//									null, null, null);
//					
					mPlayModeButton.setBackgroundResource(R.drawable.btn_now_playing_normal_order_selector);
				} else if (repeatMode == MediaPlaybackService.REPEAT_CURRENT
						&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
//					mPlayModeButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_single_repeat_selector),
//									null, null, null);
					mPlayModeButton.setBackgroundResource(R.drawable.btn_now_playing_single_repeat_selector);
				} else if (repeatMode == MediaPlaybackService.REPEAT_ALL
						&& shuffleMode == MediaPlaybackService.SHUFFLE_NONE) {
//					mPlayModeButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_all_repeat_selector),
//									null, null, null);
					mPlayModeButton.setBackgroundResource(R.drawable.btn_now_playing_all_repeat_selector);
				} else if (repeatMode == MediaPlaybackService.REPEAT_ALL
						&& shuffleMode == MediaPlaybackService.SHUFFLE_NORMAL) {
//					mPlayModeButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_shuffle_selector),
//									null, null, null);
					mPlayModeButton.setBackgroundResource(R.drawable.btn_now_playing_shuffle_selector);
				} else {
					// mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
					// mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
//					mPlayModeButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_normal_order_selector),
//									null, null, null);
					mPlayModeButton.setBackgroundResource(R.drawable.btn_now_playing_normal_order_selector);
				}
				Log.d(TAG,
						"at the end of setPlayModeButtonImage(), repeatMode="
								+ mService.getRepeatMode() + " shuffleMode="
								+ mService.getShuffleMode() + "===========");

			} catch (RemoteException ex) {
			}
		}
	}

	public void setPauseButtonImage() {
		if (mService != null) {
			Log.d(TAG, "setPauseButtonImage()=============");
			try {
				if (mService.isPlaying()) {
					mProgressHandle.sendEmptyMessage(START_MATRIX);
					setCloudVisible(true);
//					mPlayPauseButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_pause_selector),
//									null, null, null);
					mPlayPauseButton.setBackgroundResource(R.drawable.btn_now_playing_pause_selector);
				} else {
					mProgressHandle.sendEmptyMessage(STOP_MATRIX);
					setCloudVisible(false);
					mPlayPauseButton.setBackgroundResource(R.drawable.btn_now_playing_play_selector);
//					mPlayPauseButton
//							.setCompoundDrawablesWithIntrinsicBounds(
//									mResources
//											.getDrawable(R.drawable.btn_now_playing_play_selector),
//									null, null, null);
				}
			} catch (RemoteException ex) {
			}
		}
	}

	public void queueRefresh() {
		if (mHandler != null)
			mHandler.sendEmptyMessage(REFRESH);
		// //add by yangguangfu
		// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
	}

	private void queueNextRefresh(long delay) {
		try {
			if (mService != null) {
				if (mActive && !mFromTouch && mService.isPlaying()) {
					Message msg = mHandler.obtainMessage(REFRESH);
					mHandler.removeMessages(REFRESH);
					mHandler.sendMessageDelayed(msg, delay);
					// add by yangguangfu
					// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
				}
			}
		} catch (RemoteException ex) {
		}
	}

	private long refreshProgress() {
		Log.d(TAG, "refreshProgress()===============");
		if (mService == null)
			return 500;
		try {
			long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
			long remaining = 1000 - (pos % 1000);
			if (pos >= 0) {
				// add by yangguangfu
//				mPlayedProgress = mService.position();
				lyricsShowHandler.removeMessages(LOAD_LYRIC);
				lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
				long duration = mService.duration();
				mNowPlayingView.updatePlayTime(pos, duration);
				if (duration != 0) {
					mNowPlayingView
							.updatePlayProgress((int) (1000 * pos / duration));
				} else if (duration == 0) {
					mNowPlayingView.updatePlayProgress(0);
				}
			} else if (!MusicUtils.isMusicLoaded()) {
				mNowPlayingView.updatePlayTime(0, 0);
				mNowPlayingView.updatePlayProgress(0);
				setPauseButtonImage();
			}
			// mSoundProgress.setProgress(mSound);
			// setPauseButtonImage();
			return remaining;
		} catch (RemoteException ex) {
		}
		return 500;
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			try{
				switch (msg.what) {
				case REFRESH:
					Log
							.d(TAG,
									"mHandler->handleMessage()===========before refresh progress...");
					if (mActive) {
						long next = refreshProgress();
						queueNextRefresh(next);
					}
					break;
				case ALBUM_ART_DECODED:
					if (msg!= null) {
						Bitmap bm = scaleImage((Bitmap) msg.obj, 435, 427);
						BitmapDrawable bd = new BitmapDrawable(bm);
						mLastAlbumArt = bd;
						setCoverImage(bd);
					}
					break;
				case LYRIC_PARSED:
					if(msg != null){
						String lyric = (String) msg.obj;
						if (lyric == null || lyric.equals("")) {
							lyric = mResources.getString(R.string.not_lyric_found);
						}
						mLastLyric = lyric;
						setLyric(lyric);
					}
					
					
					break;
				default:
					break;
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	};

	// private void initDefaultAlbum()
	// {
	// Bitmap tmp = scaleImage(BitmapFactory.decodeResource(getResources(),
	// R.drawable.albumart_mp_unknown), 440, 430);
	// mUnknownAlbum = createReflectedImage(tmp);
	// }

	// private void addLyric(String lyric)
	// {
	// if ( mLyricContainer != null ) {
	// String tmp = parseLyric(lyric);
	// mLyricContainer.removeAllViews();
	// TextView lyricView = new TextView(this);
	// lyricView.setTextSize(18);
	// lyricView.setText(tmp);
	// lyricView.setLineSpacing(6.0f, 1.0f);
	// lyricView.setGravity(Gravity.CENTER_VERTICAL
	// | Gravity.CENTER_HORIZONTAL);
	// LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
	// LinearLayout.LayoutParams.MATCH_PARENT,
	// LinearLayout.LayoutParams.MATCH_PARENT);
	// mLyricContainer.addView(lyricView, p);
	// }
	// }
	//	
	// private String parseLyric(String lyric)
	// {
	// String regex = "\\[\\d\\d\\:\\d\\d\\.\\d\\d\\]";
	// Pattern p = Pattern.compile(regex);
	// Matcher m = p.matcher(lyric);
	// String tmp = m.replaceAll("");
	// String regex1 = "\\[\\.*\\]";
	// p = Pattern.compile(regex1);
	// m = p.matcher(tmp);
	// String ret = m.replaceAll("");
	// return ret.trim();
	// // StringBuffer sb = new StringBuffer();
	// // while (m.find()) {
	// // m.appendReplacement(sb, state);
	// // }
	// // m.appendTail(sb);
	// // return sb.toString();
	// // return null;
	// }
	    
		@Override
		public boolean onTouchEvent(MotionEvent event) {

	        if (mAudioDisplayRange == 0)
	            mAudioDisplayRange = Math.min(
	                    getWindowManager().getDefaultDisplay().getWidth(),
	                    getWindowManager().getDefaultDisplay().getHeight());

	        switch (event.getAction()) {

	            case MotionEvent.ACTION_DOWN:
	                mTouchY = event.getY();
	                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	                mIsAudioChanged = false;
	                break;

	            case MotionEvent.ACTION_MOVE:
	                float y = event.getY();

	                int delta = (int) (((mTouchY - y) / mAudioDisplayRange) * mAudioMax);
	                int vol = (int) Math.min(Math.max(mVol + delta, 0), mAudioMax);
	                if (delta != 0) {
	                	updateVolume(vol);
//	                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
	                    mIsAudioChanged = true;
	                }
	                break;

	            case MotionEvent.ACTION_UP:
//	                if (!mIsAudioChanged) {
//	                	if (!isControllerShow) {
//	    					isControllerShow= false;
//	    					showController();
//	    					cancelDelayHide();
//	    					hideControllerDelay();
//	    				} else {
//	    					isControllerShow= true;
//	    					hideController();
//	    					cancelDelayHide();
//	    				}
//	                }
	                break;
	        }
	        return mIsAudioChanged;
	    
			
//			if(mGestureDetector!=null){
//				 result = mGestureDetector.onTouchEvent(event);
//				if (!result) {
//					if (event.getAction() == MotionEvent.ACTION_UP) {
//					}
//					result = super.onTouchEvent(event);
//				}
//				return result;
//			}
//			return result;
		}
		
	private void updateVolume(int index) {
		LogUtil.i(TAG, "updateVolume=="+index+"----------currentVolume="+currentVolume);
		if (mAudioManager != null) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index,
					0);
			currentVolume = index;
		}
	}
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMediaPlaybackService.Stub.asInterface(service);
		startPlayback();
		setPlayModeButtonImage();
		try {
			// Assume something is playing when the service says it is,
			// but also if the audio ID is valid but the service is paused.
			if (mService.getAudioId() >= 0 || mService.isPlaying()
					|| mService.getPath() != null) {
				// something is playing now, we're done
				if (mOneShot || mService.getAudioId() < 0) {
					// mRepeatButton.setVisibility(View.INVISIBLE);
					// mShuffleButton.setVisibility(View.INVISIBLE);
					// mQueueButton.setVisibility(View.INVISIBLE);
				} else {
					// mRepeatButton.setVisibility(View.VISIBLE);
					// mShuffleButton.setVisibility(View.VISIBLE);
					// mQueueButton.setVisibility(View.VISIBLE);
					// setRepeatButtonImage();
					// setShuffleButtonImage();
				}
				setPauseButtonImage();
				return;
			}
		} catch (RemoteException ex) {
		}
		// Service is dead or not playing anything. If we got here as part
		// of a "play this file" Intent, exit. Otherwise go to the Music
		// app start screen.
		// TODO: how to handle here
		// if (getIntent().getData() == null) {
		// Intent intent = new Intent(Intent.ACTION_MAIN);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// intent.setClass(NowPlayingActivity.this, AlbumActivity.class);
		// startActivity(intent);
		// }
		if (mOneShot) {
			finish();
		}
	}

	public void onServiceDisconnected(ComponentName name) {
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause==========================");
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop()==========================");
		mActive = false;
		mHandler.removeMessages(REFRESH);
		lyricsShowHandler.removeMessages(LOAD_LYRIC);
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("oneshot", mOneShot);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()==================()");
		boolean from_notification = intent.getBooleanExtra("from_notification",
				false);
		if (!from_notification) {
			mOneShot = intent.getBooleanExtra("oneshot", false);
			setIntent(intent);
			startPlayback();
		} else {
			Log
					.d(TAG,
							"onNewIntent()==================this intent is sent from notification");
		}
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume()==========================");
		super.onResume();
		lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
		mActive = true;
		onActivate();
	}

	private void onActivate() {
		setPlayModeButtonImage();
		Log.d(TAG,
				"onActivate() after call setPlayModeButtonImage()============");
		setPauseButtonImage();
		queueRefresh();
	}

	@Override
	public void onDestroy() {
		mAlbumArtWorker.quit();
		if (mService != null && mOneShot && getChangingConfigurations() == 0) {
			try {
				mService.stop();
			} catch (RemoteException ex) {
			}
		}
		ActivityHolder.getInstance().removeActivity(this);
		unregisterReceiver(mStatusListener);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onDestroy();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_BACK&& event.getRepeatCount() == 0 ) {
			eixtMusicPlayer();
			
			return true;
		}
		return false;
	}

	private void eixtMusicPlayer() {
		boolean key_3 = false;
		SharedPreferences preference  = PreferenceManager.getDefaultSharedPreferences(this);
		if(preference!=null){
			 key_3 = preference.getBoolean(SettingActivity.key_3, false);
		}
		if(key_3){
			ConfirmExit();
		}else{
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
	}
	
	public void ConfirmExit() {// 退出确认
		AlertDialog.Builder ad = new AlertDialog.Builder(NowPlayingActivity.this);
		ad.setTitle("提示");
		ad.setMessage("是否后台播放?");
		ad.setPositiveButton("后台播放", new DialogInterface.OnClickListener() {// 退出按钮
				
					public void onClick(DialogInterface dialog, int i) {
						// TODO Auto-generated method stub
						NowPlayingActivity.this.finish();// 关闭activity
						overridePendingTransition(R.anim.fade, R.anim.hold);

					}
				});
		ad.setNegativeButton("停止播放", new DialogInterface.OnClickListener() {
		
			public void onClick(DialogInterface dialog, int i) {
				try {
					if(mService !=null&&mService.isPlaying()){
						mService.pause();
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				NowPlayingActivity.this.finish();// 关闭activity
				overridePendingTransition(R.anim.fade, R.anim.hold);
			}
		});
		ad.show();// 显示对话框
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		layoutByOrientation(newConfig.orientation);
	}

	private void layoutByOrientation(int orientation) {
		if (orientation == mCurrentOrientaion) {
			Log
					.i(TAG,
							"current orientation is not changed , so we won't change the basic view.");
			return;
		} else {
			mCurrentOrientaion = orientation;
		}
		try {
			/*if (mCurrentOrientaion == Configuration.ORIENTATION_LANDSCAPE) {
				setContentView(R.layout.page_now_playing_confirm_land);
				mNowPlayingView = (PageNowPlayingView) findViewById(R.id.page_now_playing_view_parent_land);
			} else */if (mCurrentOrientaion == Configuration.ORIENTATION_PORTRAIT) {
				setContentView(R.layout.page_now_playing_confirm);
				mNowPlayingView = (PageNowPlayingView) findViewById(R.id.page_now_playing_view_parent);
			} else {
				return;
			}
		} catch (NotFoundException e) {
			Log.d(TAG, "no resource");
			return;
		}
		if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
			mProgressHandle.sendEmptyMessage(STOP_MATRIX);
		}
		mNowPlayingView.initialize(mCurrentOrientaion);
		mPlayModeButton = mNowPlayingView.getPlayModeButton();
		mPlayPauseButton = mNowPlayingView.getPlayPauseButton();
		setViewListeners();
		if (mIsLyricShown) {
			mNowPlayingView.switchCoverLyricsContainer();
		}
		Log
				.d(TAG,
						"layoutByOrientation()==========before refresh progress....");
		refreshProgress();
		// //add by yangguangfu
		// mNowPlayingView.updateLyrics(mPlayedProgress);
		updateTrackInfo();
		setPlayModeButtonImage();
		setPauseButtonImage();
		// if (isLyricsMode ) {
		// mNowPlayingView.switchCoverLyricsContainer();
		// }

		// updateNowPlayingView();
		// try {
		// updateProgress();
		// mNowPlayingView.updateLyrics(mPlayedProgress);
		// } catch (RemoteException e) {
		// // do nothing.
		// }

	}

	private void setLyric(String lyric) {
		if (mNowPlayingView != null) {
			String tempLyric = lyric;
			if (lyric.equals(mResources.getString(R.string.not_lyric_found))
					|| lyric.equals(mResources
							.getString(R.string.getting_lyric))) {
				// a tricky to move this to display nearly center
				tempLyric = "\n\n" + lyric;
			}
			mNowPlayingView.updateLyricsContent(tempLyric);
		}
	}

	private void setCloudVisible(boolean visible) {
		if (mNowPlayingView != null) {
			mNowPlayingView.setBackgroundViewVisible(visible);
		}
	}

	private void setCoverImage(Drawable d) {
		if (mNowPlayingView != null) {
			mNowPlayingView.setCoverImage(d);
		}
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mLastSeekEventTime = 0;
			mHandler.removeMessages(REFRESH);
			mFromTouch = true;
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser || !MusicUtils.isMusicLoaded())
				return;
			long now = SystemClock.elapsedRealtime();
			// if ((now - mLastSeekEventTime) > 250) {
			mLastSeekEventTime = now;
			try {
				long duration = mService.duration();
				mPosOverride = duration * progress / 1000;
				// mService.seek(mPosOverride);
				mNowPlayingView.updatePlayTime(mPosOverride, duration);
//				mPlayedProgress = mService.position();
				// if ( mPlayTimeView != null) {
				// StringBuilder time = new
				// StringBuilder(MusicUtils.stringForTime((int)mPosOverride));
				// time.append("/");
				// time.append(MusicUtils.stringForTime((int)duration));
				// mPlayTimeView.setText(time.toString());
				// }
			} catch (RemoteException ex) {
			}
			// trackball event, allow progress updates
			// if (!mFromTouch) {
			// refreshProgress();
			// mPosOverride = -1;
			// }
			// }
		}

		public void onStopTrackingTouch(SeekBar bar) {
			if (!MusicUtils.isMusicLoaded()) {
				bar.setProgress(0);
			}
			try {
				if (mPosOverride != -1) {
					mService.seek(mPosOverride);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mPosOverride = -1;
			mFromTouch = false;
			queueRefresh();
		}
	};

	private void startPlayback() {

		if (mService == null)
			return;
		Intent intent = getIntent();
		String filename = "";
		Uri uri = intent.getData();
		Log.d(TAG, "startPlayback()========uri="
				+ (uri != null ? uri.toString() : "null"));
		if (uri != null && uri.toString().length() > 0) {
			// If this is a file:// URI, just use the path directly instead
			// of going through the open-from-filedescriptor codepath.
			String scheme = uri.getScheme();
			if ("file".equals(scheme) && uri.toString().length() >= 7) {
				filename = uri.getPath();
				// remove "file://"scheme
				String originUri = uri.toString();
				String decodedUri = Uri.decode(uri.toString());
				Log.d(TAG, "startPlayback()========originUri=" + originUri);
				Log.d(TAG, "startPlayback()========decodedUri=" + decodedUri);
				filename = (Uri.decode(uri.toString())).substring(7);
			} else {
				filename = uri.toString();
			}
			try {
				if (!ContentResolver.SCHEME_CONTENT.equals(scheme)
						|| !MediaStore.AUTHORITY.equals(uri.getAuthority())) {
					mOneShot = true;
				}
				// modified by liuqiang
				if (mOneShot) {
					mService.pause();
				} else {
					mService.stop();
				}
				// mService.stop();
				// end modification
				mService.openFile(filename, mOneShot);
				Log.d(TAG, "startPlayback()=========filename=" + filename
						+ " mOneShot=" + (mOneShot ? "true" : "false"));
				mService.play();
				setIntent(new Intent());
			} catch (Exception ex) {
				Log
						.d("MediaPlaybackActivity", "couldn't start playback: "
								+ ex);
			}
		}
		updateTrackInfo();
		queueRefresh();
	}

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "mStatusListener->onReceive()==============action="
					+ action);
			if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
				// redraw the artist/title info and
				// set new max for progress bar
				updateTrackInfo();
				setPauseButtonImage();
				queueRefresh();
				// add by yangguangfu
				try {
					if (mService.isPlaying()) {
						// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						// mNowPlayingView.updateLyrics(mService.position());
						isPlaying = true;
						lyricsShowHandler.removeMessages(LOAD_LYRIC);
						lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						mNowPlayingView.updateLyrics(mPlayedProgress);

					} else {
						isPlaying = false;
						lyricsShowHandler.removeMessages(LOAD_LYRIC);
						lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						mNowPlayingView.updateLyrics(mPlayedProgress);
						// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (action.equals(MediaPlaybackService.META_CHANGED)) {
				updateTrackInfo();
				setPauseButtonImage();
				queueRefresh();
				// add by yangguangfu
				try {
					if (mService.isPlaying()) {
						// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						// mNowPlayingView.updateLyrics(mService.position());
						isPlaying = true;
						lyricsShowHandler.removeMessages(LOAD_LYRIC);
						lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						mNowPlayingView.updateLyrics(mPlayedProgress);

					} else {
						isPlaying = false;
						lyricsShowHandler.removeMessages(LOAD_LYRIC);
						lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
						mNowPlayingView.updateLyrics(mPlayedProgress);
						// lyricsShowHandler.sendEmptyMessage(LOAD_LYRIC);
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (action.equals(MediaPlaybackService.PLAYBACK_COMPLETE)) {
				if (mOneShot) {
					// just for update UI when play complete in one shot mode
					if (mService != null) {
						try {
							long duration = mService.duration();
							if (mNowPlayingView != null) {
								mNowPlayingView.updatePlayTime(duration,
										duration);
								mNowPlayingView.updatePlayProgress(1000);
							}
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					Log.d(TAG,
							"StatusListener===============>playback complete and finish, mOneShot="
									+ mOneShot);
					finish();
				} else {
					setPauseButtonImage();
				}
			}
		}
	};

	private void updateTrackInfo() {
		if (mService == null) {
			return;
		}
		try {
			String path = mService.getPath();
			if (path == null) {
				// TODO: what to do here?
				// finish();
				return;
			}

			long songid = mService.getAudioId();
			Log.d(TAG, "upddateTrackInfo()===============path=" + path
					+ " song_id=" + songid);
			if (songid < 0 && path.toLowerCase().startsWith("http://")) {
				Log
						.d(TAG,
								"upddateTrackInfo()===============play stream music");
				// Once we can get album art and meta data from MediaPlayer, we
				// can show that info again when streaming.
				mNowPlayingView.setArtistNameText(MusicUtils.getArtistName(
						this, null));
				mNowPlayingView.setTrackNameText(path);
				mNowPlayingView.setAlbumNameText(MusicUtils.getAlbumName(this,
						null));
				setCoverImage(MusicUtils.getDefaultCover(this));
			} else {
				Log.d(TAG, "upddateTrackInfo()===============play local music");
				mNowPlayingView.setArtistNameText(MusicUtils.getArtistName(
						this, mService.getArtistName()));
				long albumid = mService.getAlbumId();

				mNowPlayingView.setAlbumNameText(MusicUtils.getAlbumName(this,
						mService.getAlbumName()));
				mNowPlayingView.setTrackNameText(MusicUtils.getSongName(this,
						mService.getTrackName()));

				if (mLastId == songid && mLastAlbumArt != null) {
					setCoverImage(mLastAlbumArt);
				} else {
					mLastAlbumArt = null;
					mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
					mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
							new AlbumSongIdWrapper(albumid, songid))
							.sendToTarget();
				}
				if (mLastId == songid && mLastLyric != null) {
					setLyric(mLastLyric);
				} else {
					mLastLyric = null;
					mAlbumArtHandler.removeMessages(GET_LYRIC);
					String filename = null;
					if (path.startsWith("content://")) {
						filename = MusicUtils.getFilePathFromUri(this, Uri
								.parse(path));
					} else {
						filename = path;
					}
					setLyric(mResources.getString(R.string.getting_lyric));
					mAlbumArtHandler.obtainMessage(GET_LYRIC, filename)
							.sendToTarget();
				}
				mLastId = songid;
			}
		} catch (RemoteException ex) {
			finish();
		}
	}

	private Bitmap scaleImage(Bitmap bm, int w, int h) {
		Bitmap tmp = Bitmap.createScaledBitmap(bm, w, h, true);
		if (tmp != bm)
			bm.recycle();

		return tmp;
	}

	private MP3File mp3file = null;

	private String getLyric(String filename) {

		String content = null;

		String fileName = filename.substring(0, filename.lastIndexOf('.'));
		File fileLrc = new File(fileName + ".lrc");
		if (fileLrc.exists()) {
			FileInputStream fileInputStream;
			try {
				fileInputStream = new FileInputStream(fileLrc);
				content = LyricsParser.getContent2(fileInputStream, "GBK");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (content == null) {
			
			File fileTxt = new File(fileName + ".txt");
			if (fileTxt.exists()) {
				FileInputStream fileInputStream;
				try {
					fileInputStream = new FileInputStream(fileTxt);
					content = LyricsParser.getContent2(fileInputStream, "GBK");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		

		if (content == null) {
			File f = new File(filename);
			if (f.exists()) {
				try {
					mp3file = new MP3File(f.toString());

					if (mp3file != null) {
						content = getID3v2TagContent(f);
						if (content == null) {
							content = getLyrics3TagContent();
						}
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		return content;
	}

	private String getID3v2_3Lyric(File fileName) {
		String content = null;
		ID3v2Tag tag = new ID3v2Tag(fileName.toString());
		content = tag.get_lyric();
		LogInfo("linxchongwei_AbstractID3v2__2.3" + content);
		return content;
	}

	public void LogInfo(String content) {

		Log.i("TestLRC", content);
	}

	@SuppressWarnings("unchecked")
	private String getID3v2TagContent(File file) {
		String content = null;

		if (mp3file != null) {
			AbstractID3v2 id3v2 = mp3file.getID3v2Tag();
			if (id3v2 != null) {
				String lyric = id3v2.getSongLyric();
				LogInfo("AbstractID3v2_______lyric:  " + lyric);
				if (lyric != null && lyric.length() > 3) {
					content = lyric;
					LogInfo("AbstractID3v2_______getSongLyric:  " + content);
				} else {

					List frameList = mp3file.getFrameAcrossTags("SYLT");
					for (int i = 0; i < frameList.size(); i++) {
						content = frameList.get(i).toString();
						LogInfo("SYLT:" + content);
					}

					if (content == null || content.trim().length() < 3) {
						content = getID3v2_3Lyric(file);
						LogInfo("AbstractID3v2_______ID3v2_3:" + content);
					}

					if (content == null || content.trim().length() < 3) {
						List frameListUSLT = mp3file.getFrameAcrossTags("USLT");
						for (int i = 0; i < frameListUSLT.size(); i++) {
							content = frameListUSLT.get(i).toString();
							LogInfo("AbstractID3v2_______USLT:" + content);

						}
					}

				}
			}

		}

		return content;

	}

	private String getLyrics3TagContent() {
		String content = null;
		if (mp3file != null) {
			AbstractLyrics3 lyrics3 = mp3file.getLyrics3Tag();

			LogInfo("AbstractLyrics3________AbstractLyrics3:" + lyrics3);
			if (lyrics3 != null) {
				String lyrics = lyrics3.getSongLyric();
				if (lyrics != null && lyrics.length() > 3) {
					content = lyrics;
					LogInfo("AbstractLyrics3_______lyrics:" + content);
				} else {
					List frameList = mp3file.getFrameAcrossTags("LYR");

					for (int i = 0; i < frameList.size(); i++) {
						content = frameList.get(i).toString();
						LogInfo("AbstractLyrics3________LYR:" + content);

					}
				}
			}

		}

		return content;

	}

	private void ShowMessage(String string) {
		Toast.makeText(this.getBaseContext(), string, Toast.LENGTH_SHORT)
				.show();
	}

	private static class AlbumSongIdWrapper {
		public long albumid;
		public long songid;

		AlbumSongIdWrapper(long aid, long sid) {
			albumid = aid;
			songid = sid;
		}
	}

	public class AlbumArtHandler extends Handler {
		private long mAlbumId = -1;

		public AlbumArtHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
			// long songid = ((AlbumSongIdWrapper) msg.obj).songid;
			// if (msg.what == GET_ALBUM_ART && (mAlbumId != albumid || albumid
			// < 0)) {

			if (msg.what == GET_ALBUM_ART) {
				long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
				long songid = ((AlbumSongIdWrapper) msg.obj).songid;

				// if ( mAlbumId != albumid || albumid < 0 ) {
				// while decoding the new image, show the default album art
				Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
				mHandler.removeMessages(ALBUM_ART_DECODED);
				mHandler.sendMessageDelayed(numsg, 300);
				Bitmap bm = MusicUtils.getArtwork(NowPlayingActivity.this,
						songid, albumid);
				// Bitmap bm =
				// MusicUtils.getArtworkQuick(NowPlayingActivity.this, albumid,
				// 502, 492);
				if (bm == null) {
					bm = MusicUtils.getArtwork(NowPlayingActivity.this, songid,
							-1);
					albumid = -1;
				}
				if (bm != null) {

					numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
					mHandler.removeMessages(ALBUM_ART_DECODED);
					mHandler.sendMessage(numsg);
				}
				mAlbumId = albumid;
				// }
			}
			if (msg.what == GET_LYRIC) {
				String filename = (String) msg.obj;
				String lyric = getLyric(filename);
				Message lyricmsg = mHandler.obtainMessage(LYRIC_PARSED, lyric);
				mHandler.sendMessage(lyricmsg);
			}
		}
	}

	private static class Worker implements Runnable {
		private final Object mLock = new Object();
		private Looper mLooper;

		/**
		 * Creates a worker thread with the given name. The thread then runs a
		 * {@link android.os.Looper}.
		 * 
		 * @param name
		 *            A name for the new thread
		 */
		Worker(String name) {
			Thread t = new Thread(null, this, name);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			synchronized (mLock) {
				while (mLooper == null) {
					try {
						mLock.wait();
					} catch (InterruptedException ex) {
					}
				}
			}
		}

		public Looper getLooper() {
			return mLooper;
		}

		public void run() {
			synchronized (mLock) {
				Looper.prepare();
				mLooper = Looper.myLooper();
				mLock.notifyAll();
			}
			Looper.loop();
		}

		public void quit() {
			mLooper.quit();
		}
	}

	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.page_now_playing_confirm, null);
	}

	@Override
	protected void titleLeftButton() {
		eixtMusicPlayer();
	}

	@Override
	protected void titlRightButton() {
		AppConnect.getInstance(this).showOffers(this);
		
	}
}
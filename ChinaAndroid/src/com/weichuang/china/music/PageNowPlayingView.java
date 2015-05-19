package com.weichuang.china.music;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.china.R;

public class PageNowPlayingView extends RelativeLayout{
	
	private boolean mIsLandscape;

	private ImageView musicCoverPageImageViewLand;// Added by YiLin for land now playing
	private TextView mTrackNameTextView;//now playing music tack name
	private TextView mArtistNameTextView;//now playing music artist name
	private TextView mAlbumNameTextView;//now playing music album name
	private TextView musicOnlineBufferingTextView;//now playing music online buffering
	private TextView musicPlayingTime;//now playing music time;	
	private Button configModePlayButton;//now_playing_mode
	private Button nextPlayButton;//now_playing_next
	private Button pausePlayButton;//now_playing_pause
	private Button previousPlayButton;//now_playing_previous
	private Button mbtnCoverLyricSwticher;
	private SeekBar musicPlaySeekBar;//now playing music progress
    private AnimationDrawable mMatrixAnimation;
    private Button animationInvoker;
//    private ImageView wideCloudView;    //add by jinhui
    private ImageView middleWideCloud;
    
    
    /**
     * Modified by Gogo,for Lyrics and cover switch.at Aug 5 2009
     */
    private LyricCoverSwitcher coverLyricsContainer;
    private LyricsTextView mLyricsPanel;
    private TextView mPlainLyrics;
    private ScrollView mPlainLyricsScrollPanel;
	private final static String TAG = "NowPlayingView";
	private AtomicBoolean isValidLyricsFormate;
	private AtomicBoolean isValidLRCFormate;
	
	private Resources mRes;
	
	private ViewReadyListener mViewReadyListener;
	
	public void setViewReadyListener(ViewReadyListener viewReadyListener) {
		mViewReadyListener = viewReadyListener;
	}
    
	/**
	 * construct PageNowPlayingView
	 * this class extends LinearLayout
	 * @param Context context
	 **/
	public PageNowPlayingView(Context context) {
		super(context);
		Log.d(TAG , "page now playing view 's constructor");
	}
	/**
	 * construct PageNowPlayingView
	 * this class extends LinearLayout
	 * @param Context context
	 * @param AttributeSet attrs
	 **/
	public PageNowPlayingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG , "page now playing view 's constructor(Context context, AttributeSet attrs)");
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Log.d(TAG , "page now playing view 's onAttachedToWindow();");
		if ( mViewReadyListener != null ) {
			mViewReadyListener.onViewReady();
		}
	}
	
	@Override
	protected void onFinishInflate() {
	    super.onFinishInflate();
	    Log.d(TAG , "page now playing view 's onFinishInflate();");
	}
	
	/**
	 * init the view in the class
	 * the view is in the page_now_playing.xml
	 * */
	public void initialize(int currentOrientation) {
		Log.d(TAG , "-----------------page now playing view init();");
	    mRes = getContext().getResources();
		
		mIsLandscape = ( currentOrientation == Configuration.ORIENTATION_LANDSCAPE );
		
		mTrackNameTextView= (TextView)findViewById(R.id.now_playing_txt_track_name);
		mAlbumNameTextView=(TextView)findViewById(R.id.now_playing_txt_album_name);
		mArtistNameTextView=(TextView)findViewById(R.id.now_playing_txt_artist_name);
		
		musicOnlineBufferingTextView = (TextView)findViewById(R.id.online_buffering);
		musicPlayingTime = (TextView)findViewById(R.id.music_playingtime);
		
		musicPlaySeekBar = (SeekBar)findViewById(R.id.now_playing_seekbar_music_playing);
		musicPlaySeekBar.setThumbOffset(13);
		musicPlaySeekBar.setMax(1000);
		configModePlayButton = (Button)findViewById(R.id.now_playing_btn_shuffle);
		nextPlayButton = (Button)findViewById(R.id.now_playing_btn_next);
		pausePlayButton = (Button)findViewById(R.id.now_playing_btn_play);
		previousPlayButton =(Button)findViewById(R.id.now_playing_btn_previous);
		
		mbtnCoverLyricSwticher = (Button)findViewById(R.id.now_playing_btn_cover_lyric_switcher);
		
		isValidLyricsFormate = new AtomicBoolean(false);
		isValidLRCFormate = new AtomicBoolean(false);
		
		if ( !mIsLandscape ) {
		    coverLyricsContainer = (LyricCoverSwitcher) findViewById(R.id.now_playing_switcher_lyric_cover_final);//switcher container.
		    
			mMatrixAnimation = (AnimationDrawable) findViewById(R.id.now_playing_imgs_matrix_flame).getBackground();
			animationInvoker = (Button) findViewById(R.id.now_playing_matrix_animation_invoker);
			
			animationInvoker.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					Boolean isStart = (Boolean) animationInvoker.getTag();
					if( isStart.booleanValue() ) {
						mMatrixAnimation.start();
					} else {
						mMatrixAnimation.stop();
					}
				}
			});
			
//			wideCloudView = (ImageView) findViewById(R.id.now_playing_wide_cloud);
			middleWideCloud = (ImageView) findViewById(R.id.now_playing_middle_winde);
		} /*else {
		    mLyricsPanel = (LyricsTextView) findViewById(R.id.now_playing_lyrics_panel);
		    Log.d( TAG ," current is landscapse. and mLyricsPanel is nul? " + (mLyricsPanel == null ));
		    mPlainLyrics = (TextView) findViewById(R.id.lyrics_panel);
		    mPlainLyricsScrollPanel = (ScrollView) findViewById(R.id.now_playing_plaintxt_lyrics_panel);
		    musicCoverPageImageViewLand = (ImageView) findViewById(R.id.now_playing_img_top_title_bg);
		}*/
	}
	
	public static interface ViewReadyListener{
		public void onViewReady();
	}
	
	public void startMatrixAnimation(){
		if (mIsLandscape) {
			return;
		}
		animationInvoker.setTag(true);
		animationInvoker.performClick();
	}
	
	public void stopMatrixAnimation(){
		if (mIsLandscape) {
			return;
		}
		animationInvoker.setTag(false);
		animationInvoker.performClick();
	}
	
	//modified by jinhui              change nowplaying default album image.
    public void setCoverImage(Drawable coverImage) {
        if (mIsLandscape) {
        	if ( musicCoverPageImageViewLand != null) {
        		if (coverImage != null) {
        			musicCoverPageImageViewLand.setBackgroundDrawable(coverImage);
        		} else {
        			musicCoverPageImageViewLand.setBackgroundDrawable(MusicUtils.getDefaultDrawable(getContext()));
        		}
        	}
        } else {
            if (coverLyricsContainer != null) {
                if (coverImage != null) {
                    coverLyricsContainer.setCoverImage(coverImage);
                } else {
                    coverLyricsContainer.setCoverImage(MusicUtils.getDefaultDrawable(getContext()));
                }
            }
        }
    }
	
	
	
	public void setOnCoverLyricsSwitcherClickListener(OnClickListener l ) {
	    if ( mbtnCoverLyricSwticher != null ) {
	        mbtnCoverLyricSwticher.setOnClickListener(l);
	    }
	}
	
	/**
	 * set onClick action for configModePlayButton
	 **/
	public void setConfigModePlayButtonClickListener(OnClickListener ocl){
		this.configModePlayButton.setOnClickListener(ocl);
	}
	/**
	 * set onClick action for nextPlayButton
	 **/
	public void setNextPlayButtonClickListener(OnClickListener ocl){
		this.nextPlayButton.setOnClickListener(ocl);
	}
	/**
	 * set onClick action for pausePlayButton 
	 **/
	public void setPausePlayButtonClickListener(OnClickListener ocl){
		this.pausePlayButton.setOnClickListener(ocl);
	}
	/**
	 * set onClick action for previousPlayButton 
	 **/
	public void setPreviousPlayButtonClickListener(OnClickListener ocl){		
		this.previousPlayButton.setOnClickListener(ocl);		
	}	
	
	public void setOnSeekBarDragListener(OnSeekBarChangeListener l){
		this.musicPlaySeekBar.setOnSeekBarChangeListener(l);
	}
	
	/**
	 *set trackName's text
	 **/
	public void setTrackNameText(CharSequence trackname){
		this.mTrackNameTextView.setText(trackname);
	}
	/**
	 *set artistNameTextView's text
	 **/
	public void setArtistNameText(CharSequence artistName){
		this.mArtistNameTextView.setText(artistName);
	}
	/**
	 *set albumNameTextView's text
	 **/
	public void setAlbumNameText(CharSequence albumName){
		this.mAlbumNameTextView.setText(albumName);
	}
	/**
	 *set musicPlayingTimeText's text
	 **/
	public void setMusicPlayingTimeText(CharSequence musicPlayingTime){
		this.musicPlayingTime.setText(musicPlayingTime);
	}
	/**
	 *set musicOnlineBufferingText's text
	 **/
	public void setMusicOnlineBufferingText(CharSequence musicOnlineBuffering){
		this.musicOnlineBufferingTextView.setText(musicOnlineBuffering);
	}

	public void updatePlayTime(long curPosition, long duration) {
//        final String durationStr = mDateFormmater.format(new Date(duration));
//        final String curTimeStr = mDateFormmater.format(new Date(curPosition));
        
        musicPlayingTime.setText(MusicUtils.stringForTime(curPosition) + "/" + MusicUtils.stringForTime(duration));
	}
	
	/**
	 * This method will update the lyrics view if the content is formated by LRC format.
	 * Just give the current playing position.
	 * @param position the current position of music playing status.
	 */
	public void updateLyrics(long position){
	    if ( mIsLandscape ) {
	        if ( mLyricsPanel != null && mLyricsPanel.getVisibility() == VISIBLE) {
	            mLyricsPanel.adjustLyric(position);
	            
	        }
	    } else {
	        if (coverLyricsContainer !=null) {
	            coverLyricsContainer.setLyricsIndex(position);
	        }
	    }
	}
	
		// TODO Auto-generated constructor stub
	
	
	/**
	 * Update the lyrics content including landscape or partial
	 * @param lyric
	 */
	public static Boolean isFormatedByLrc;
	public void updateLyricsContent(String lyric) {
	     isFormatedByLrc=false;
        boolean isValidLyric = !TextUtils.isEmpty(lyric); 
        isValidLyricsFormate.set(isValidLyric);
        
        if (isValidLyric) {
            if(LyricsParser.isFormateByLRC(lyric)){
                isFormatedByLrc = true;
                Log.d(TAG," the lyrics is format by LRC");
            } else {
                Log.d(TAG," the lyrics is normal formate");
            }
            if ( mIsLandscape ) {
                if ( isFormatedByLrc ) {
                	if ( mLyricsPanel != null) {
                		mLyricsPanel.setText("");
                		mLyricsPanel.setLyricContent(lyric);
                		if ( isShowLyrics) {
                			mLyricsPanel.setVisibility(VISIBLE);
                			if ( mPlainLyricsScrollPanel != null) {
                				mPlainLyricsScrollPanel.setVisibility(GONE);
                			}
                		}
                	}
                } else {
                	if ( mPlainLyrics != null ) {
                		mPlainLyrics.setText(lyric);
                		mPlainLyrics.setLineSpacing(18f, 1.0f);
                	}
                    if ( isShowLyrics ) {
                    	if ( mLyricsPanel != null) {
                    		mLyricsPanel.setVisibility(GONE);
                    	}
                    	if ( mPlainLyricsScrollPanel != null) {
                    		mPlainLyricsScrollPanel.setVisibility(VISIBLE);
                    	}
                    }
                }
            } else {
            	if ( coverLyricsContainer != null ) {
            		coverLyricsContainer.setLyricsContent(lyric, isFormatedByLrc);
            	}
            }
            isValidLRCFormate.set(isFormatedByLrc);
        } else {
            Log.d(TAG , "empty lyrics and current is landscape? " + mIsLandscape);
            isValidLRCFormate.set(false);
            if ( mIsLandscape ) {
            	Log.d(TAG , "current is landscape and lyrics is empty ." );
            	if ( mLyricsPanel != null ) {
            		if ( isShowLyrics ) {
            			mLyricsPanel.setVisibility(GONE);
            		}
            	}
            	if ( mPlainLyricsScrollPanel != null && isShowLyrics) {
                    mPlainLyricsScrollPanel.setVisibility(VISIBLE);
                    if ( mPlainLyrics != null) {
                        Log.d(TAG , "mPlainLyrics is visiable?" + (mPlainLyrics.getVisibility() == VISIBLE));
                        mPlainLyrics.setText(mRes.getString(R.string.no_lyrics));
                        mPlainLyrics.setVisibility(VISIBLE);
                    }
                }
            } else {
            	if ( coverLyricsContainer != null) {
            		coverLyricsContainer.setLyricsContent(mRes.getString(R.string.no_lyrics), false);
            	}
            }
        }
	}

    public void updatePlayProgress(long progress) {
        musicPlaySeekBar.setProgress((int) progress);
    }

    public void updatePlayPauseView(boolean isPlaying) {
        int icon = isPlaying ? R.drawable.btn_now_playing_pause_selector : R.drawable.btn_now_playing_play_selector;
        //TODO: this need be deleted.
//        coverLyricsContainer.applyRotation();
        pausePlayButton.setCompoundDrawablesWithIntrinsicBounds(mRes.getDrawable(icon) , null , null , null);
        setBackgroundViewVisible(isPlaying);
    }

    public void updateDownloadProgress(long progress) {
//        musicOnlineBufferingTextView.setText("Buffering " + progress + "%");
        musicPlaySeekBar.setSecondaryProgress((int) progress);
    }
    
//    public void setOnSeekBar
	
    
    
    /**
     * This will switch the container between Cover & Lyrics.
     */
    public void switchCoverLyricsContainer() {
        if ( !mIsLandscape ) {
            if ( coverLyricsContainer != null ) {
                coverLyricsContainer.applyRotation();
            }
        } else {
            switchLyricsModeInLandscape();
        }
    }
    
    private boolean isShowLyrics = false;
    
  //Modify by yangguangfu
    public void switchLyricsModeInLandscape() {
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());  
        anim.setDuration(1500);
        if ( isShowLyrics = !isShowLyrics) {
            Log.d(TAG , "wanna show lyrics,isvalid lyrics?" + isValidLyricsFormate.get() + " is LRC formate?" + isValidLRCFormate.get());
            mAlbumNameTextView.setVisibility(GONE);
            mTrackNameTextView.setVisibility(GONE);
            mArtistNameTextView.setVisibility(GONE);
            if ( isValidLyricsFormate.get() ){//if the lyrics is not empty
                if ( isValidLRCFormate.get() ) {//if the lyrics is formate by LRC.
                    mLyricsPanel.setVisibility(VISIBLE);
                    mLyricsPanel.startAnimation(anim);
                    mPlainLyricsScrollPanel.setVisibility(GONE);
                } else {
                    mLyricsPanel.setVisibility(GONE);
                    mPlainLyricsScrollPanel.setVisibility(VISIBLE);
                    mPlainLyricsScrollPanel.startAnimation(anim);
                }
            } else {
                mPlainLyricsScrollPanel.setVisibility(VISIBLE);
                mPlainLyricsScrollPanel.startAnimation(anim);
                mLyricsPanel.setVisibility(GONE);
            }
        } else {
            mAlbumNameTextView.setVisibility(VISIBLE);
            mTrackNameTextView.setVisibility(VISIBLE);
            mArtistNameTextView.setVisibility(VISIBLE);
            mAlbumNameTextView.startAnimation(anim);
            mTrackNameTextView.startAnimation(anim);
            mArtistNameTextView.startAnimation(anim);
            mPlainLyricsScrollPanel.setVisibility(GONE);
            mLyricsPanel.setVisibility(GONE);
        }
    }
    
    public void setOnArtistNameLongClickListener (OnLongClickListener listener) {
        if ( mArtistNameTextView != null){
            mArtistNameTextView.setOnLongClickListener(listener);
        }
    }
    
    public void setOnTrackNameLongClickListener (OnLongClickListener listener) {
        if ( mTrackNameTextView != null ) {
            mTrackNameTextView.setOnLongClickListener(listener);
        }
    }
    
    public void setOnAlbumNameLongClickListener (OnLongClickListener listener) {
        if ( mAlbumNameTextView != null) {
            mAlbumNameTextView.setOnLongClickListener(listener);
        }
    }
    
    public CharSequence getAlbumName() {
        return mAlbumNameTextView.getText();
    }
    
    public Button getPlayModeButton() {
    	return configModePlayButton;
    }
    
    public Button getPlayPauseButton() {
    	return pausePlayButton;
    }
    
    public CharSequence getTrackName() {
        return mTrackNameTextView.getText();
    }
    
    public CharSequence getArtistName() {
        return mArtistNameTextView.getText();
    }
    
    public void setMusicCoverPageImageViewLand(Drawable d) {
    	this.musicCoverPageImageViewLand.setBackgroundDrawable(d);
    }
    
    //add by jinhui
    public void setBackgroundViewVisible(boolean isVisibile){
//    	if ( wideCloudView != null) {
//    		if(isVisibile){
//    			wideCloudView.setVisibility(VISIBLE);
//    		}else{
//    			wideCloudView.setVisibility(GONE);
//    		}
//    	}
    	
//    	if(middleWideCloud!=null){
//    		if(isVisibile){
//    			middleWideCloud.setVisibility(VISIBLE);
//    		}else{
//    			middleWideCloud.setVisibility(GONE);
//    		}
//    	}
    }
    
	
}

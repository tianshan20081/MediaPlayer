package com.weichuang.china.music;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.china.R;

public class LyricCoverSwitcher extends ViewSwitcher implements OnClickListener,OnLongClickListener{
	
	private static final String TAG = "LyricCoverSwitcher";
//	private boolean isInitialized;
	private DisplayNextView displayNextView;
	private ImageView coverView;
	private ScrollView lyricsScrollPanel;
	private TextView lyricView;
	private LyricsTextView lyricsTextView;
	private LayoutInflater inflater;
	private boolean isLRCFormat = true; //FIXME: This should judged by outside;
		
	public ImageView getCoverView() {
		return coverView;
	}

	public TextView getLyricView() {
		return lyricView;
	}
	
	public LyricCoverSwitcher(Context ctx) {
		super(ctx);
	}
	
	public LyricCoverSwitcher(Context ctx , AttributeSet set) {
		super(ctx, set);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		initialize();
	}
	
	private void initialize() {
		displayNextView = new DisplayNextView();
		inflater = LayoutInflater.from(getContext());
		coverView = (ImageView) inflater.inflate(R.layout.now_plaing_cover_panel, null);
		
		
		LinearLayout lyricContainer = (LinearLayout) inflater.inflate(R.layout.now_plaing_lyric_panel, null);
		lyricsScrollPanel = (ScrollView) lyricContainer.getChildAt(0);
		
		lyricView = (TextView) lyricsScrollPanel.findViewById(R.id.lyrics_panel);
		
		lyricsTextView = (LyricsTextView) lyricContainer.getChildAt(1);
		
		Log.d(TAG, "initizlied on LyricSwitcher.");
		
		if(getChildCount() == 0 ){
		    addView(coverView , 0);
            addView(lyricContainer , 1);
		}
		this.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
//		isInitialized = true;
	}
	
	public void setCoverImage(Drawable drawable){
		coverView.setBackgroundDrawable(drawable);
	}
	
	public void setLyricsContent(String content , boolean isLRCFormat) {
		this.isLRCFormat = isLRCFormat;
		setLyricsMode(isLRCFormat);
		if ( isLRCFormat ) {
			lyricsTextView.setLyricContent(content);
			//lyricsTextView.setText("heheh");
		} else {
			lyricView.setText(content);
			lyricView.setLineSpacing(18f, 1.0f);
		}
	}
	
	
	public boolean isLRCFormat() {
		return this.isLRCFormat;
	}
	

	public void setLyricsIndex(long position) {
		if ( isLRCFormat ){
			lyricsTextView.adjustLyric(position);
		}
	}
	
	/**
	 * 
	 * @param isLRCFormat
	 */
	private void setLyricsMode (boolean isLRCFormat){
		if ( isLRCFormat ) {
			lyricsScrollPanel.setVisibility(GONE);
			lyricsTextView.setVisibility(VISIBLE);
		} else {
			lyricsScrollPanel.setVisibility(VISIBLE);
			lyricsTextView.setVisibility(GONE);
		}
	}
	
	public void onClick(View v) {
		applyRotation();
	}
	
	public boolean onLongClick(View v) {
		Log.d(TAG, "onLongClick event" + v.getClass().getSimpleName());
		applyRotation();
		if(v.getId() == R.id.lyrics_panel){
			return true;
		} else if (v.getId() == R.id.lyrics_text_view) {
			return true;
		}
		return false;
	}
	
	
	
	/**
     * Setup a new 3D rotation on the container view.
     *
     * @param position the item that was clicked to show a picture, or -1 to show the list
     * @param start the start angle at which the rotation must begin
     * @param end the end angle of the rotation
     */
    public void applyRotation() {
        // Create a new 3D rotation with the supplied parameter
        // The animation listener is used to trigger the next animation
    	final float centerX = getWidth() / 2.0f;//this centerX cann't be cached,because the width and height will be changed at any time;
    	final float centerY = getHeight() / 2.0f;
        final Rotate3dAnimation rotation = new Rotate3dAnimation(0 , 90, centerX, centerY, 310.0f, true);
        rotation.setDuration(150);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        rotation.setAnimationListener(displayNextView);

        startAnimation(rotation);
    }
	
	 /**
     * This class listens for the end of the first half of the animation.
     * It then posts a new action that effectively swaps the views when the container
     * is rotated 90 degrees and thus invisible.
     */
    private final class DisplayNextView implements Animation.AnimationListener {
    	public void onAnimationEnd(Animation animation) {
    		LyricCoverSwitcher.this.post(new SwapViews());
    	}
    	public void onAnimationRepeat(Animation animation) {
    		
    	}
    	public void onAnimationStart(Animation animation) {
    		
    	}
    }
    
    /**
     * This class is responsible for swapping the views and start the second
     * half of the animation.
     */
    private final class SwapViews implements Runnable {

        public void run() {
            final float centerX = getWidth() / 2.0f;
        	final float centerY = getHeight() / 2.0f;
        	showNext();
            final Rotate3dAnimation rotation = new Rotate3dAnimation(-90, 0, centerX, centerY, 310.0f, false);
            
            rotation.setDuration(150);
            rotation.setFillAfter(true);
            rotation.setInterpolator(new DecelerateInterpolator());
            LyricCoverSwitcher.this.startAnimation(rotation);
        }
    }
}

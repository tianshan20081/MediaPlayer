package com.weichuang.china.music.coverflow;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.china.R;

public class CoverFlowWrapper extends RelativeLayout {
	private CoverFlow mCoverFlow;
	public CoverFlowBackView mCoverFlowBackView;
	public boolean needDrawBackViewBitmap = false;
	private CoverFlowSeekBar mSeekBar;
	private VerticalSeekBar mVerSeekBar;
	public int mOrientation;

	public CoverFlowWrapper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CoverFlowWrapper(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		mCoverFlow = (CoverFlow) findViewById(R.id.albumcoverflow);
		mCoverFlowBackView = (CoverFlowBackView) findViewById(R.id.songsofalbumcontainer);
		mCoverFlowBackView.setCoverFlow(mCoverFlow);

		mSeekBar = (CoverFlowSeekBar) findViewById(R.id.albumcoverflowseekbar);
		mVerSeekBar = (VerticalSeekBar) findViewById(R.id.albumcoverflowseekbar_vertical);

		LayoutParams params = (LayoutParams) mCoverFlowBackView
				.getLayoutParams();
		int width = getWidth();
		int height = getHeight();
		if (width > height) {
			params.height = CoverFlow.COVER_EDGE_HORIZONTAL;
			params.width = CoverFlow.COVER_EDGE_HORIZONTAL_RESIZED;
		} else {
			params.height = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);
			params.width = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);
		}

		// temp code
		mCoverFlowBackView.clearBackground();
		super.onFinishInflate();

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		CoverFlowBackView backview = mCoverFlowBackView;

		if (needDrawBackViewBitmap) {
			genDefaultAlbumBackView();
			needDrawBackViewBitmap = false;
			backview.clearBackground();
		}
		super.onLayout(changed, l, t, r, b);
	}

	public void genDefaultAlbumBackView() {

		Canvas canvas = new Canvas();
		Bitmap bitmap = Bitmap.createBitmap(mCoverFlowBackView.getWidth(),
				mCoverFlowBackView.getHeight(), Config.ARGB_8888);
		canvas.setBitmap(bitmap);
		mCoverFlowBackView.draw(canvas);

		ImageView im = (ImageView) findViewById(R.id.test);
		im.setImageBitmap(bitmap);

		mCoverFlow.mBackViewBitmap = bitmap;
	}

	
	protected void onConfigurationChanged(Configuration newConfig) {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCoverFlowBackView
				.getLayoutParams();
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			params.height = CoverFlow.COVER_EDGE_HORIZONTAL;
			params.width = CoverFlow.COVER_EDGE_HORIZONTAL_RESIZED;
		} else {
			params.height = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);
			params.width = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);
		}
		mCoverFlowBackView.setLayoutParams(params);
	}

	private void onOrientationChanged(int newOrientation) {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCoverFlowBackView
				.getLayoutParams();
		if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			mOrientation = Configuration.ORIENTATION_LANDSCAPE;
			mSeekBar.setVisibility(View.VISIBLE);
			mVerSeekBar.setVisibility(View.INVISIBLE);

			int count = mCoverFlow.getCount();
			if (count > 1) {
				mSeekBar.setMax(count -1);
				mSeekBar
						.setProgress(mCoverFlow.getCurrentSelectedCoverIndex()
								);
			} else {
				mSeekBar.setProgress(0);
				mSeekBar.setVisibility(View.INVISIBLE);
			}

			params.height = CoverFlow.COVER_EDGE_HORIZONTAL;
			params.width = CoverFlow.COVER_EDGE_HORIZONTAL_RESIZED;
		} else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
			mOrientation = Configuration.ORIENTATION_PORTRAIT;
			mSeekBar.setVisibility(View.INVISIBLE);
			mVerSeekBar.setVisibility(View.VISIBLE);

			int count = mCoverFlow.getCount();
			if (count > 1) {
				mVerSeekBar.setMax(count -1);
				mVerSeekBar.setProgress(mCoverFlow
						.getCurrentSelectedCoverIndex()
						);
				
			} else {
				mVerSeekBar.setProgress(0);
				mVerSeekBar.setVisibility(View.INVISIBLE);
			}

			params.height = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);
			params.width = (int) (CoverFlow.COVER_EDGE_VERTICAL * 1.3f);

		}
		mCoverFlowBackView.setLayoutParams(params);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {

		super.onSizeChanged(width, height, oldw, oldh);
		if (width >= height) {
			mOrientation = Configuration.ORIENTATION_LANDSCAPE;
		} else {
			mOrientation = Configuration.ORIENTATION_PORTRAIT;
		}
		onOrientationChanged(mOrientation);
	}
	
	public void setSeekBar(int progress,int max){
		if(max == 0){
			mSeekBar.setVisibility(View.INVISIBLE);
			mVerSeekBar.setVisibility(View.INVISIBLE);
		}else{
			mSeekBar.setMax(max);
			mVerSeekBar.setMax(max);
			
			mSeekBar.setProgress(progress);
			mVerSeekBar.setProgress(progress);
			
			if(mOrientation == Configuration.ORIENTATION_LANDSCAPE){
				mSeekBar.setVisibility(View.VISIBLE);
				mVerSeekBar.setVisibility(View.INVISIBLE);
			}else{
				mSeekBar.setVisibility(View.INVISIBLE);
				mVerSeekBar.setVisibility(View.VISIBLE);
			}
		}
	}
}

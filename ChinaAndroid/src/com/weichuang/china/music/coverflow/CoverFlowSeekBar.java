package com.weichuang.china.music.coverflow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class CoverFlowSeekBar extends SeekBar {
	public boolean mSurpressTouchEvent;

	public CoverFlowSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CoverFlowSeekBar(Context context) {
		super(context);
		init();

	}

	public CoverFlowSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	public void init() {
		setThumbOffset(0);
		mSurpressTouchEvent = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mSurpressTouchEvent) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}
}

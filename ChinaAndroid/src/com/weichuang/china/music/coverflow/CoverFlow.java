package com.weichuang.china.music.coverflow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.util.Utils;

public class CoverFlow extends GLSurfaceView implements Renderer,
		GestureDetector.OnGestureListener {
	private GestureDetector mGestureDetector;
	private CoverFlowAdapter mAdapter;

	private int mCoverCount;
	/**
	 * the length of center cover displayed on screen, in pixel;
	 */
	private int mCoverEdge;
	/**
	 * distance from center of one cover to the next one's, in pixel
	 */
	private int mCoverSpace;
	/**
	 * right boundary which can scroll to, in pixel
	 */
	private int mRightMost;
	/**
	 * left boundary which can scroll to, in pixel
	 */
	private int mLeftMost;

	private int mWidth;
	private int mHeight;

	/**
	 * the ratio for change distance from GL-coordinate(float) to pixel(int)
	 */
	private float mGLToPixel;

	/**
	 * center of the CoverFlow, which is the center point that user look at, in
	 * pixel
	 */
	private int mCenter;

	/**
	 * drawing state of this GLsurfaceview
	 */
	public int mState;
	/**
	 * the angle that center cover has rotated
	 */
	private int mCenterCoverAngle;

	private MotionEvent mClickEvent;

	private int mDefaultAlbumTexture;
	private int mBackViewTexture;
	private int mBackViewTexture_land;
	private int mBackViewTexture_port;

	private static final String TAG = "CoverFlow";

	private Handler touchEventHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			int clickedCover = msg.what;
			if (clickedCover >= 0 && clickedCover < mAdapter.getCount()) {
				if (mFlingRunnable.mScroller.isFinished()
						&& clickedCover == mSelectedCover) {
					if (mState == STATE_IDLE) {
						mFlipRunnable.startFlip(true);
					} else if (mState == STATE_FLIPPED) {
						mFlipRunnable.startFlip(false);
					}
				} else {
					if (mState == STATE_IDLE)
						scrollToChild(clickedCover);
				}
			}
		};
	};

	/**
	 * Duration in milliseconds from the start of a scroll during which we're
	 * unsure whether the user is scrolling or flinging.
	 */
	private static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 250;

	private boolean mIsFirstScroll;

	/**
	 * Whether to continuously callback on the item selected listener during a
	 * fling.
	 */
	private boolean mShouldCallbackDuringFling = true;

	/**
	 * Whether to callback when an item that is not selected is clicked.
	 */
	private boolean mShouldCallbackOnUnselectedItemClick = true;

	/**
	 * If true, do not callback to item selected listener.
	 */
	private boolean mSuppressSelectionChanged;

	/**
	 * Sets mSuppressSelectionChanged = false. This is used to set it to false
	 * in the future. It will also trigger a selection changed.
	 */
	private Runnable mDisableSuppressSelectionChangedRunnable = new Runnable() {
		public void run() {
			mSuppressSelectionChanged = false;
			// selectionChanged();
		}
	};

	private FlingRunnable mFlingRunnable = new FlingRunnable();
	private FlipRunnable mFlipRunnable = new FlipRunnable();
	private onCoverSelectedListener mOnCoverSelectedListener;
	private onCoverFlipListener mOnCoverFlipListener;

	// bug to be fixed
	public static int COVER_EDGE_HORIZONTAL = 280;
	public static int COVER_SPACE_HORIZONTAL = 280;
	
	//add by yangguangfu负责调CoverFlow间距的
	public static int COVER_EDGE_VERTICAL = 200;
	//add by yangguangfu负责调CoverFlow大小的
	public static int COVER_SPACE_VERTICAL = 200;
	
	//add by yangguangfu负责调重叠效果的
	public static float COVER_DEPTH = 5f;
	//add by yangguangfu负责调滑动的流畅度
	public static float COVER_CENTERDEPTH = 9f;
	
	public static int COVER_EDGE_HORIZONTAL_RESIZED = 390;

	private static final int STATE_IDLE = 0;
	private static final int STATE_SCROLL = 1;
	private static final int STATE_FLING = 2;
	private static final int STATE_FLIPPING = 3;
	private static final int STATE_FLIPPED = 4;

	private static final int CONFIGURATION_LANDSCAP = 0;
	private static final int CONFIGURATION_PORTRAIT = 1;

	private int mOrientation;

	private int mOffsetX = 0;
	// private int mOffsetX = 10;
	private int mOffsetY = 0;

	private CoverQuad mCoverQuad;
	/*
	 * whether draw cover's reflection
	 */
	private boolean mDrawReflection;

	private int mSelectedCover;
	private int mAnimationDuration;
	private int mFlipAnimationDuration;
	private boolean mShouldStopFling;
	private boolean mShouldStopRotate;

	private RelativeLayout mParentView;

	private Cover mCenterCover;

	private boolean mShouldScale;
	public Bitmap mBackViewBitmap;
	private StringTexture.Config mTitleConfig = new StringTexture.Config(
			StringTexture.Config.SIZE_EXACT);
	private StringTexture.Config mArtistConfig = new StringTexture.Config(
			StringTexture.Config.SIZE_EXACT);
	private StringTexture.Config mNoMusicConfig = new StringTexture.Config(
			StringTexture.Config.SIZE_TEXT_TO_BOUNDS);
	private CoverLoadThread mLoadThread;

	private StringTexture mNoMusicMessage;
	
	public CoverFlow(Context context, AttributeSet attrs) {
		super(context, attrs);
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setIsLongpressEnabled(false);
		setEGLConfigChooser(true);
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		init();
	}

	public void init() {
		mCenter = 0;
		mDrawReflection = false;
		mSelectedCover = 0;
		mAnimationDuration = 400;
		mFlipAnimationDuration = 800;
		mGLToPixel = 0f;
		mCoverSpace = 0;
		mCoverEdge = 0;
		float widthd = Utils.getWidthDpi(BaseActivity.mBaseActivity);
		 if( widthd <= 162) {
			 mTitleConfig.fontSize = 12f;
			 COVER_EDGE_VERTICAL = 180;
				//add by yangguangfu负责调CoverFlow大小的
			 COVER_SPACE_VERTICAL = 180;
			 COVER_DEPTH = 5f;
		 }else{
			 mTitleConfig.fontSize = 30f;
			 COVER_EDGE_VERTICAL = 300;
				//add by yangguangfu负责调CoverFlow大小的
			 COVER_SPACE_VERTICAL = 300;
			 COVER_DEPTH = 5f;
		 }
		
		mTitleConfig.bold = true;
//		mTitleConfig.r = 1 - 78 / 255f;
//		mTitleConfig.g = 1 - 78 / 255f;
//		mTitleConfig.b = 1 - 78 / 255f;
		mTitleConfig.r = 1f;
		mTitleConfig.g = 1f;
		mTitleConfig.b = 1f;
		mTitleConfig.xalignment = StringTexture.Config.ALIGN_HCENTER;
		mTitleConfig.overflowMode = StringTexture.Config.OVERFLOW_ELLIPSIZE;
		
		
	
		 if( widthd <= 162) {
			mArtistConfig.fontSize = 12f;
			 
		 }else{
			mArtistConfig.fontSize = 30f;
			 
		 }

		mArtistConfig.bold = true;
		mArtistConfig.r = 1f;
		mArtistConfig.g = 1f;
		mArtistConfig.b = 1f;
		mArtistConfig.xalignment = StringTexture.Config.ALIGN_HCENTER;
		mArtistConfig.overflowMode = StringTexture.Config.OVERFLOW_ELLIPSIZE;
		 if( widthd <= 162) {
			  mNoMusicConfig.fontSize = 10f;
				 
			 }else{
				 mNoMusicConfig.fontSize = 24f;
				 
		}
		
		mNoMusicConfig.bold = true;
		mNoMusicConfig.r = 1f;
		mNoMusicConfig.g = 1f;
		mNoMusicConfig.b = 1f;
		mNoMusicConfig.xalignment = StringTexture.Config.ALIGN_HCENTER;
		mNoMusicConfig.overflowMode = StringTexture.Config.OVERFLOW_FADE;
		
		mLoadThread = new CoverLoadThread();
	}

	public void setAdapter(CoverFlowAdapter adapter) {
		setAdapter(adapter, 0);
	}

	public void setAdapter(CoverFlowAdapter adapter, int initialPosition) {
		mAdapter = adapter;
		int count = adapter.getCount();
		mCoverCount = count;
		//adapter.initCovers();

		if (initialPosition < 0) {
			initialPosition = 0;
		}

		if (initialPosition >= count) {
			initialPosition = count != 0 ? count - 1 : 0;
		}

		setScrollBoundary();
		setInitialPosition(initialPosition);
		setSeekBar(initialPosition, count == 0?0:count -1);
		
		requestRender();
	}

	private void setInitialPosition(int position) {
		mSelectedCover = position;
		mCenter = position * mCoverSpace;

		onFinishedMovement();
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mParentView == null) {
			mParentView = (RelativeLayout) getParent();
		}
		mWidth = w;
		mHeight = h;
		int oldCoverSpace = mCoverSpace;

		if (h > w) {
			mOrientation = CONFIGURATION_PORTRAIT;
			mGLToPixel = 600 * COVER_DEPTH / (2 * COVER_CENTERDEPTH);
			mCoverSpace = COVER_SPACE_VERTICAL;
			mCoverEdge = COVER_EDGE_VERTICAL;

		} else {
			mOrientation = CONFIGURATION_LANDSCAP;
			mGLToPixel = 600 * COVER_DEPTH / (2 * COVER_CENTERDEPTH);
			mCoverSpace = COVER_SPACE_HORIZONTAL;
			mCoverEdge = COVER_EDGE_HORIZONTAL;
		}

		if (oldCoverSpace != mCoverSpace && oldCoverSpace != 0) {
			mCenter = mCenter * mCoverSpace / oldCoverSpace;
		} else if (oldCoverSpace == 0) {
			mCenter = mSelectedCover * mCoverSpace;
		}

		setScrollBoundary();
		super.onSizeChanged(w, h, oldw, oldh);

	}

	private void setScrollBoundary() {
		if (mCoverCount != 0 && mCoverSpace != 0) {
			mRightMost = (int) Math
					.floor((mCoverCount - 0.5) * mCoverSpace - 1);
			mLeftMost = -mCoverSpace / 2 + 1;
		} else {
			mRightMost = 0;
			mLeftMost = 0;
		}
	}

	public boolean onTouchEvent(MotionEvent e) {
		boolean retValue = mGestureDetector.onTouchEvent(e);

		int action = e.getAction();

		if (action == MotionEvent.ACTION_UP) {
			onUp(e);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			onCancel(e);
		}
		if (retValue)
			requestRender();
		return retValue;
	}

	void onUp(MotionEvent e) {
		if (mFlingRunnable.mScroller.isFinished() && mState == STATE_SCROLL) {
			scrollIntoSlots();
		}
		// if (mState ==STATE_SCROLL || mState ==STATE_FLING) {
		// scrollIntoSlots();
		// }
		//
		// dispatchUnpress();

		// testScroll = false;
	}

	void onCancel(MotionEvent e) {
		onUp(e);
	}

	private boolean preloadInitialTexture(int centerCoverIndex) {
		boolean isAllLoaded = true;
		CoverFlowAdapter adapter = mAdapter;

		for (int i = -2; i < 3; i++) {
//			isAllLoaded &= preloadTexture(adapter
//					.getCover(centerCoverIndex + i), true);
			Cover cover = adapter.getCover(centerCoverIndex + i);
			if(cover != null){
				loadCoverBitmap(cover);
				bindCoverTexture(cover);
			}
		}
		return isAllLoaded;
	}

	public void onDrawFrame(GL10 gl) {
		if (isInitialDraw || mAdapter == null || mAdapter.getCount() == 0 ) {
			//gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			clearBackground();
			if (isInitialDraw) {
				mLoadThread.start();
				isInitialDraw = false;
			}
			if(mAdapter != null && mAdapter.getCount() == 0){
				drawNoMusicMessage(gl);
			}else{
				requestRender();
			}
			return;
		}
		if (isFirstDraw) {
			if(mAdapter.isNeedInit()){
				requestRender();
				return;
			}
			if (!preloadInitialTexture(mSelectedCover)) {
		//		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		//		clearBackground();
				requestRender();
				return;
			} else {
				isFirstDraw = false;
			}
		}
		//gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		clearBackground();

		gl.glActiveTexture(GL10.GL_TEXTURE0);

		float center = mCenter;
		float space = mCoverSpace;
		int left = Math.round(center / space) - 2;
		float offset = (left * space - center) / mGLToPixel;
		CoverQuad cover = mCoverQuad;
		int state = mState;
		int orientation = mOrientation;
		processClickEvent(gl, cover, left, offset, space / mGLToPixel, state,
				orientation);
		drawCovers(gl, cover, left, offset, space / mGLToPixel, state,
				orientation);
		if(mState == STATE_IDLE)bufferCover(left + 2);
		// test draw
		// testDraw(gl);

	}

	private void bufferCover(int centerCoverIndex) {
		CoverFlowAdapter adaptr = mAdapter;
		boolean left = false;
		boolean right = false;
		int count = 0;
		int tick = 3;
		while (count <= 6 && tick < 24) {
			if (mLoadingCount < MAX_LOADING_COUNT - 1) {
				Cover cover = adaptr.getCover(centerCoverIndex + tick);
				if (cover != null) {
					if (cover.mState == Cover.STATE_UNLOADED) {
						queueLoad(cover, false);
						count++;
					}
				} else {
					right = true;
				}

				cover = adaptr.getCover(centerCoverIndex - tick);
				if (cover != null) {
					if (cover.mState == Cover.STATE_UNLOADED) {
						queueLoad(cover, false);
						count++;
					}
				} else {
					left = true;
				}
				if (right && left)
					return;
				tick++;

			} else {
				return;
			}
		}
	}

	/*
	 * test code
	 */
	private void testDraw(GL10 gl) {
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnable(GL10.GL_BLEND);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mAdapter.getCover(1)
				.getCoverTexture(gl));

		gl.glLoadIdentity();
		gl.glTranslatef(1, 0, -COVER_CENTERDEPTH);
		CoverQuad coverquad = mCoverQuad;
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, coverquad.mFVertexBuffer);
		FloatBuffer texBuffer = coverquad.mTexBuffer;
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);

		// gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT,
		// coverquad.mIndexBuffer);

		((GL11Ext) gl).glDrawTexfOES(mWidth - 435, mHeight - 435, -5, 435, 435);
		// gl.glBindTexture(GL10.GL_TEXTURE_2D, mBackViewTexture);
		// gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4,
		// GL10.GL_UNSIGNED_SHORT, coverquad.mIndexBuffer);
		// ((GL11Ext) gl).glDrawTexfOES(512, 0, 0, 512, 512);
		gl.glClientActiveTexture(GL10.GL_TEXTURE1);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);
		gl.glClientActiveTexture(GL10.GL_TEXTURE0);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);

		gl.glActiveTexture(GL10.GL_TEXTURE1);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mBackViewTexture);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
				GL11.GL_DECAL);
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
		// GL11.GL_INTERPOLATE);
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA,
		// GL11.GL_INTERPOLATE);
		//
		// // Specify the interpolation factor via the alpha component of
		// // GL_TEXTURE_ENV_COLOR.
		final float[] color = { 1f, 1f, 1f, 0.5f };
		gl.glTexEnvfv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, color, 0);
		//
		// // Wire up the interpolation factor for RGB.
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_SRC2_RGB, GL11.GL_TEXTURE);
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_OPERAND2_RGB,
		// GL11.GL_SRC_ALPHA);
		//
		// // Wire up the interpolation factor for alpha.
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_SRC2_ALPHA,
		// GL11.GL_TEXTURE);
		// gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_OPERAND2_ALPHA,
		// GL11.GL_SRC_ALPHA);

		gl.glLoadIdentity();
		gl.glTranslatef(-1, 0, -COVER_CENTERDEPTH);
		((GL11Ext) gl).glDrawTexfOES(0, 0, 0, 435, 435);
		// gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT,
		// coverquad.mIndexBuffer);

		// gl.glLoadIdentity();
		// gl.glTranslatef(0, 1, -COVER_CENTERDEPTH);
		// // final float[] color1 = { 1f, 1f, 1f, 0.2f };
		// // gl.glTexEnvfv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR,
		// color1, 0);
		// gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT,
		// coverquad.mIndexBuffer);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glDisable(GL10.GL_BLEND);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
	}

	private void processClickEvent(GL10 gl, CoverQuad cover, int firstCover,
			float firstCoverOffset, float space, int state, int orientation) {

		if (mClickEvent != null) {
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			MotionEvent e = mClickEvent;
			int x = (int) e.getX();
			int y = (int) (mHeight - e.getY());

			gl.glDisable(GL10.GL_DITHER);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, cover.mFVertexBuffer);

			ShortBuffer indics = cover.mIndexBuffer;
			int count = mAdapter.getCount();
			int lastCover = firstCover + 5;
			float offsetCross = (orientation == CONFIGURATION_LANDSCAP ? mOffsetY
					: mOffsetX)
					/ mGLToPixel;
			int j = 0;
			for (int i = firstCover; i < lastCover; i++) {
				if (i >= 0 && i < count) {
					float offset = j * space + firstCoverOffset;
					gl.glLoadIdentity();
					if (orientation == CONFIGURATION_LANDSCAP) {
						gl.glTranslatef(offset, offsetCross, getDepth(offset));
					} else {
						gl.glTranslatef(offsetCross, -offset, getDepth(offset));
					}
					gl.glColorPointer(4, GL10.GL_FLOAT, 0,
							cover.mPickColorBuffer[j]);
					gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4,
							GL10.GL_UNSIGNED_SHORT, indics);

				}

				j++;
			}

			ByteBuffer PixelBuffer = ByteBuffer.allocateDirect(4);
			PixelBuffer.order(ByteOrder.nativeOrder());
			PixelBuffer.position(0);
			gl.glReadPixels(x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
					PixelBuffer);

			int red = ((short) (PixelBuffer.get(0) & 0xFF)) / 255;
			int green = ((short) (PixelBuffer.get(1) & 0xFF)) / 255;
			int blue = ((short) (PixelBuffer.get(2) & 0xFF)) / 255;
			// short alpha = (short) (PixelBuffer.get(3) & 0xFF);

			int position = 4 * red + 2 * green + blue;

			//gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			mClickEvent = null;

			gl.glEnable(GL10.GL_DITHER);
			gl.glEnable(GL10.GL_TEXTURE_2D);

			clearBackground();
			if (position != 0) {
				Message message = touchEventHandler.obtainMessage(position
						+ firstCover - 1);
				touchEventHandler.sendMessage(message);
			}
		}
	}

	private void drawCovers(GL10 gl, CoverQuad coverquad, int firstCover,
			float firstCoverOffset, float space, int state, int orientation) {
		int lastCover = firstCover + 5;

		CoverFlowAdapter adapter = mAdapter;
		boolean drawReflection = mDrawReflection;

		float offsetCross = (orientation == CONFIGURATION_LANDSCAP ? mOffsetY
				: mOffsetX)
				/ mGLToPixel;

		ShortBuffer indics = coverquad.mIndexBuffer;

		// boolean fliptoback = true;
		// boolean hasRotated = false;

		int coverIndic = 0;
		boolean drawBackView = false;

		for (int i = firstCover; i < lastCover; i++) {
			Cover cover = adapter.getCover(i);
			// int quarter = 1;
			if (cover != null) {
				float offset = coverIndic * space + firstCoverOffset;
				gl.glLoadIdentity();
				if (orientation == CONFIGURATION_LANDSCAP) {
					gl.glTranslatef(offset, offsetCross, getDepth(offset));
				} else {
					gl.glTranslatef(offsetCross, -offset, getDepth(offset));
				}
			
				int quarter = 1;
				if (i - firstCover == 2
						&& (state == STATE_FLIPPING || state == STATE_FLIPPED)) {

					float angle = mCenterCoverAngle;

					if (angle != 0) {
						float direction = 1f;
						if (angle > 90 && angle <= 180) {
							angle = 180 - angle;
							direction = -direction;
							quarter = 2;
							// fliptoback = true;

						} else if (angle > 180 && angle < 270) {
							angle = angle - 180;
							// fliptoback = false;
							quarter = 3;

						} else if (angle >= 270 && angle <= 360) {
							angle = 360 - angle;
							direction = -direction;
							// fliptoback = false;
							quarter = 4;
						}
						// float des = 5 * 0.6f + 5;
						float des = 10 / 1.3f;
						if (mShouldScale) {
							switch (quarter) {
							case 1:
								gl.glTranslatef(0, 0, -(angle / 90) * 2);
								break;
							case 2:
								gl.glTranslatef(0, 0, -2 + ((90 - angle) / 90)
										* (10 + 2 - des));
								break;
							case 3:
								gl.glTranslatef(0, 0, (10 - des)
										- (angle / 180) * (10 - des));
								break;
							case 4:
								gl.glTranslatef(0, 0, (angle / 180)
										* (10 - des));
								break;
							}
						}

						if (angle != 0) {
							gl.glRotatef((float) angle, 0.0f, direction, 0.0f);
							// hasRotated = true;

						}
					}
					// if (i - firstCover == 2
					// && (state == STATE_FLIPPING || state == STATE_FLIPPED)) {
					//
					// float angle = mCenterCoverAngle;
					// if (angle != 0) {
					// float direction = 1f;
					// if (angle > 90 && angle < 180) {
					// angle = 180 - angle;
					// direction = -direction;
					// quarter = 2;
					// // fliptoback = true;
					//
					// } else if (angle >= 180 && angle < 270) {
					// angle = angle - 180;
					// quarter = 3;
					// // fliptoback = false;
					//
					// } else if (angle >= 270 && angle <= 360) {
					// angle = 360 - angle;
					// direction = -direction;
					// quarter = 4;
					// // fliptoback = false;
					// }
					// if (angle != 0) {
					// gl.glRotatef((float) angle, 0.0f, direction, 0.0f);
					// // hasRotated = true;
					// }
					// }

				}
				int texture;
				// if (cover.mState == Cover.STATE_LOADED) {
				// texture = cover.getCoverTexture(gl);
				// } else {
				// texture = mDefaultAlbumTexture;
				// queueLoad(cover, true);
				// }
				bindCoverTexture(cover);
				// gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);

				FloatBuffer texBuffer = coverquad.mTexBuffer;
				FloatBuffer refTexBuffer = coverquad.mRefTexBuffer;
				FloatBuffer verBuffer = coverquad.mFVertexBuffer;
				if (quarter == 2 || quarter == 3) {
					texBuffer = coverquad.mFlipTexBuffer;
					refTexBuffer = coverquad.mFlipRefTexBuffer;
					if (mOrientation == CONFIGURATION_LANDSCAP)
						verBuffer = coverquad.mResizedVerBuffer;
					drawBackView = true;
				}

				if (drawReflection) {
					gl.glEnable(GL10.GL_BLEND);
					// gl.glBlendFunc(GL10.GL_SRC_ALPHA,
					// GL10.GL_ONE_MINUS_SRC_ALPHA);

					gl.glVertexPointer(3, GL10.GL_FLOAT, 0,
							coverquad.mRefVertexBuffer);
					gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, refTexBuffer);
					gl.glColorPointer(4, GL10.GL_FLOAT, 0,
							coverquad.colorBuffer);

					gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4,
							GL10.GL_UNSIGNED_SHORT, indics);

					gl.glDisable(GL10.GL_BLEND);
				}

				gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verBuffer);

				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);

				gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4,
						GL10.GL_UNSIGNED_SHORT, indics);

				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

				if (drawBackView) {

					gl.glEnable(GL10.GL_BLEND);
					gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mBackViewTexture);
					gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0,
							coverquad.mTexBuffer);
					gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4,
							GL10.GL_UNSIGNED_SHORT, indics);
					gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
					gl.glDisable(GL10.GL_BLEND);

					drawBackView = false;
				} else {
					if (cover.isTitlteQuadInitialed) {
						gl.glEnable(GL10.GL_BLEND);
						gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
						int yoffset = 0;
						yoffset += cover.albumTexture.getHeight() - 18;
						drawCoverTitle(cover.albumTexture, cover.mAlbumVertex,
								cover.mAlbumTex, indics, 0,
								-mCoverEdge / 2 + yoffset);
						yoffset += cover.artistTexture.getHeight() + 10 - 18;
						drawCoverTitle(cover.artistTexture,
								cover.mArtistVertex, cover.mArtistTex, indics,
								0, -mCoverEdge / 2 + yoffset);

						gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
						gl.glDisable(GL10.GL_BLEND);
					}
				}
			}
			coverIndic++;
		}
	}

	private void drawCoverTitle(StringTexture texture, FloatBuffer vertex,
			FloatBuffer tex, ShortBuffer indics, int offsetX, int offsetY) {
		GL11 gl = mGL;
		if (texture.mState != Texture.STATE_LOADED) {
			loadTexture(texture);
			Cover.setTitleQuad(texture, vertex, tex, mGLToPixel);
		}
		if(texture.mState == Texture.STATE_LOADED){
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.mId);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, tex);

		float oX = offsetX / mGLToPixel;
		float oY = offsetY / mGLToPixel;
		gl.glTranslatef(oX, oY, 0.03f);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT,
				indics);

		gl.glTranslatef(-oX, -oY, -0.03f);}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float resize;
		if (height > width) {
			float ratio = (float) height / width;
			resize = width / 600f;
			gl.glFrustumf(-1 * resize, 1 * resize, -ratio * resize, ratio
					* resize, COVER_DEPTH, COVER_DEPTH * 5f);
			mDrawReflection = false;
			mShouldScale = true;
			mBackViewTexture = mBackViewTexture_port;
			//mBackground = mBackground_port;
		} else {
			float ratio = (float) width / height;
			resize = height / 600f;
			gl.glFrustumf(-ratio * resize, ratio * resize, -1 * resize,
					1 * resize, COVER_DEPTH, COVER_DEPTH * 5f);
			mDrawReflection = false;
			mShouldScale = false;
			mBackViewTexture = mBackViewTexture_land;
			//mBackground = mBackground_land;
		}
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_MODELVIEW);

		if (mCoverQuad != null) {
			mCoverQuad.initCoverVertex();
		}
		
		

	}

	private boolean isFirstDraw = true;
	private boolean isInitialDraw = true;

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		setZOrderOnTop(false);
		Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);

		if (mGL == null) {
			mGL = (GL11) gl;
		} else {
			mGL = (GL11) gl;
		}

		isFirstDraw = true;
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		gl.glEnable(GL11.GL_DITHER);
		gl.glDisable(GL11.GL_LIGHTING);

		gl.glClearDepthf(1f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
				GL11.GL_REPLACE);

		gl.glClearColor(73 / 255f, 74 / 255f, 74 / 255f, 1f);
		//gl.glClearColor(0.0f, 0f, 0f, 0.0f);

		// enable the differentiation of which side may be visible
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glCullFace(GL10.GL_BACK);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		gl.glClientActiveTexture(GL11.GL_TEXTURE1);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		gl.glClientActiveTexture(GL11.GL_TEXTURE0);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		mCoverQuad = new CoverQuad(0.15f);
		mCoverQuad.initCoverVertex();

		genDefaultAlbumTexture(gl);
		genBackViewTexture(gl);

		if (mAdapter != null) {
			mAdapter.resetCoverTextures(mDefaultAlbumTexture);
		}
	}

	public boolean onDown(MotionEvent e) {
		// Kill any existing fling/scroll
		mFlingRunnable.stop(false);

		if (mState == STATE_FLING) {
			mState = STATE_SCROLL;
		}

		mIsFirstScroll = true;
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (mState == STATE_FLIPPED || mState == STATE_FLIPPING) {
			return false;
		}
		if (!mShouldCallbackDuringFling) {
			// We want to suppress selection changes

			// Remove any future code to set mSuppressSelectionChanged = false
			removeCallbacks(mDisableSuppressSelectionChangedRunnable);

			// This will get reset once we scroll into slots
			if (!mSuppressSelectionChanged)
				mSuppressSelectionChanged = true;
		}
		float velocity = mOrientation == CONFIGURATION_LANDSCAP ? velocityX
				: velocityY;
		mFlingRunnable.startUsingVelocity((int) -velocity);

		mState = STATE_FLING;

		return true;
	}

	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (mState == STATE_FLIPPED || mState == STATE_FLIPPING) {
			return false;
		}

		getParent().requestDisallowInterceptTouchEvent(true);

		// As the user scrolls, we want to callback selection changes so
		// related-
		// info on the screen is up-to-date with the gallery's selection
		if (!mShouldCallbackDuringFling) {
			if (mIsFirstScroll) {
				/*
				 * We're not notifying the client of selection changes during
				 * the fling, and this scroll could possibly be a fling. Don't
				 * do selection changes until we're sure it is not a fling.
				 */
				if (!mSuppressSelectionChanged)
					mSuppressSelectionChanged = true;
				postDelayed(mDisableSuppressSelectionChangedRunnable,
						SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT);
			}
		} else {
			if (mSuppressSelectionChanged)
				mSuppressSelectionChanged = false;
		}
		// if (testScroll)
		// Log.i("testScroll", "scroll :" + distanceX);
		// Track the motion
		float distance = mOrientation == CONFIGURATION_LANDSCAP ? distanceX
				: distanceY;
		trackMotionScroll(-1 * (int) distance);

		mIsFirstScroll = false;
		mState = STATE_SCROLL;
		return true;
	}

	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	public boolean onSingleTapUp(MotionEvent e) {
		if (mState == STATE_IDLE) {
			final MotionEvent event = MotionEvent.obtain(e);
			mClickEvent = event;
			return true;
		}

		if (mState == STATE_FLIPPED) {
			flipCenterCover();
		}

		return false;
	}

	/**
	 * Tracks a motion scroll. In reality, this is used to do just about any
	 * movement to items (touch scroll, arrow-key scroll, set an item as
	 * selected).
	 * 
	 * @param deltaX
	 *            Change in X from the previous event.
	 */
	void trackMotionScroll(int deltaX) {

		if (mCoverCount == 0) {
			return;
		}

		boolean toLeft = deltaX < 0;

		int limitedDeltaX = getLimitedMotionScrollAmount(toLeft, deltaX);
		scrollby(limitedDeltaX);
		setSelectionToCenterCover();
		if (limitedDeltaX != deltaX) {
			// The above call returned a limited amount, so stop any
			// scrolls/flings
			mFlingRunnable.endFling(false);
			onFinishedMovement();
		}
	}

	public void scrollby(int deltaX) {
		mCenter -= deltaX;
		requestRender();
	};

	public void setSelectionToCenterCover() {
		int centerCover = Math.round((float) mCenter / mCoverSpace);
		if (mSelectedCover != centerCover) {
			mSelectedCover = centerCover;
			selectionChanged();
		}
	}

	int getLimitedMotionScrollAmount(boolean motionToLeft, int deltaX) {
		int extremeItemPosition = motionToLeft ? mRightMost : mLeftMost;
		int distance = extremeItemPosition - mCenter;
		if (motionToLeft) {
			if (distance > -deltaX) {
				return deltaX;
			} else if (distance >= 0) {
				return -distance;
			} else {
				return 0;
			}
		} else {
			if (-distance > deltaX) {
				return deltaX;
			} else if (distance <= 0) {
				return -distance;
			} else {
				return 0;
			}
		}
	}

	private void scrollIntoSlots() {

		int selectedCover = mSelectedCover * mCoverSpace;

		int scrollAmount = mCenter - selectedCover;
		if (scrollAmount != 0) {
			mFlingRunnable.startUsingDistance(scrollAmount);
		} else {
			onFinishedMovement();
		}

	}

	private void onFinishedMovement() {
		if (mSuppressSelectionChanged) {
			mSuppressSelectionChanged = false;

			// We haven't been callbacking during the fling, so do it now
			selectionChanged();

		}
		boolean changed = false;
		int coverPosition = mSelectedCover;
		Cover cover = null;
		if (mAdapter != null) {

			cover = mAdapter.getCover(coverPosition);
			changed = cover != mCenterCover;
			if (changed)
				mCenterCover = cover;
		}
		if (mOnCoverSelectedListener != null) {

			mOnCoverSelectedListener.onCoverCentered(CoverFlow.this,
					coverPosition, cover, changed);
		}
		mState = STATE_IDLE;
	}

	void selectionChanged() {
		if (!mSuppressSelectionChanged) {
			onselectionChanged();
		}
	}

	public void onselectionChanged() {
		if (mOnCoverSelectedListener != null) {
			mOnCoverSelectedListener.onCoverSelected(this, mSelectedCover);
		}
	}

	public interface onCoverSelectedListener {
		void onCoverSelected(CoverFlow coverflow, int position);

		void onCoverCentered(CoverFlow coverflow, int position, Cover cover,
				boolean changed);
	}

	public interface onCoverFlipListener {
		void onCoverStartFlip(CoverFlow coverflow, Cover flippingCover,
				boolean flipToBack);

		void onCoverFinishFlip(CoverFlow coverflow, Cover flippingCover,
				boolean flipToBack);

	}

	public void setOnCoverSelectedListener(onCoverSelectedListener listener) {
		mOnCoverSelectedListener = listener;
	}

	public void setOnCoverFlipListener(onCoverFlipListener listener) {
		mOnCoverFlipListener = listener;
	}

	boolean movePrevious() {
		if (mCoverCount > 0 && mSelectedCover > 0) {
			scrollToChild(mSelectedCover - 1);
			return true;
		} else {
			return false;
		}
	}

	boolean moveNext() {
		if (mCoverCount > 0 && mSelectedCover < mCoverCount - 1) {
			scrollToChild(mSelectedCover + 1);
			return true;
		} else {
			return false;
		}
	}

	public boolean scrollToChild(int cover) {

		int coverPostion = mCoverSpace * cover;
		int distance = mCenter - coverPostion;
		if (distance != 0) {
			mFlingRunnable.startUsingDistance(distance);
			mState = STATE_FLING;
			return true;
		} else {
			onFinishedMovement();
			return false;
		}

	}

	public int getCount() {
		return mCoverCount;
	}

	public int getCurrentSelectedCoverIndex() {
		return mSelectedCover;
	}

	public Cover getCurrentCenteredCover() {
		return mCenterCover;
	}

	public class CoverQuad {

		private final static int VERTS = 4;

		public FloatBuffer mFVertexBuffer;
		public FloatBuffer mRefVertexBuffer;
		public FloatBuffer mTexBuffer;
		public FloatBuffer mFlipTexBuffer;
		public ShortBuffer mIndexBuffer;
		public FloatBuffer mRefTexBuffer;
		public FloatBuffer mFlipRefTexBuffer;
		public FloatBuffer mResizedVerBuffer;

		public float edgeInSurface;

		public FloatBuffer colorBuffer;

		public FloatBuffer[] mPickColorBuffer;

		public float mReflectRatio;

		public CoverQuad(float reflectRatio) {
			mReflectRatio = reflectRatio;
		}

		public void initCoverVertex() {
			float edge = mCoverEdge / (2 * mGLToPixel);
			// edgeInPixel/mSurHeight;

			ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			vbb.order(ByteOrder.nativeOrder());
			mFVertexBuffer = vbb.asFloatBuffer();

			ByteBuffer resizedvbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			resizedvbb.order(ByteOrder.nativeOrder());
			mResizedVerBuffer = resizedvbb.asFloatBuffer();

			ByteBuffer rvbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			rvbb.order(ByteOrder.nativeOrder());
			mRefVertexBuffer = rvbb.asFloatBuffer();
			float reflectEdge = mReflectRatio * edge * 2;
			mRefVertexBuffer.put(edge);
			mRefVertexBuffer.put(-edge);
			mRefVertexBuffer.put(0);
			mRefVertexBuffer.put(-edge);
			mRefVertexBuffer.put(-edge);
			mRefVertexBuffer.put(0);
			mRefVertexBuffer.put(edge);
			mRefVertexBuffer.put(-edge - reflectEdge);
			mRefVertexBuffer.put(0);
			mRefVertexBuffer.put(-edge);
			mRefVertexBuffer.put(-edge - reflectEdge);
			mRefVertexBuffer.put(0);

			ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			mTexBuffer = tbb.asFloatBuffer();
			mTexBuffer.put(1f);
			mTexBuffer.put(0f);
			mTexBuffer.put(0f);
			mTexBuffer.put(0f);
			mTexBuffer.put(1f);
			mTexBuffer.put(1f);
			mTexBuffer.put(0f);
			mTexBuffer.put(1f);

			ByteBuffer ftbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			ftbb.order(ByteOrder.nativeOrder());
			mFlipTexBuffer = ftbb.asFloatBuffer();
			mFlipTexBuffer.put(0f);
			mFlipTexBuffer.put(0f);
			mFlipTexBuffer.put(1f);
			mFlipTexBuffer.put(0f);
			mFlipTexBuffer.put(0f);
			mFlipTexBuffer.put(1f);
			mFlipTexBuffer.put(1f);
			mFlipTexBuffer.put(1f);

			ByteBuffer rtbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			rtbb.order(ByteOrder.nativeOrder());
			mRefTexBuffer = rtbb.asFloatBuffer();
			mRefTexBuffer.put(1f);
			mRefTexBuffer.put(1f);
			mRefTexBuffer.put(0f);
			mRefTexBuffer.put(1f);
			mRefTexBuffer.put(1f);
			mRefTexBuffer.put(1 - mReflectRatio);
			mRefTexBuffer.put(0f);
			mRefTexBuffer.put(1 - mReflectRatio);

			ByteBuffer frtbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			frtbb.order(ByteOrder.nativeOrder());
			mFlipRefTexBuffer = frtbb.asFloatBuffer();
			mFlipRefTexBuffer.put(0f);
			mFlipRefTexBuffer.put(1f);
			mFlipRefTexBuffer.put(1f);
			mFlipRefTexBuffer.put(1f);
			mFlipRefTexBuffer.put(0f);
			mFlipRefTexBuffer.put(1 - mReflectRatio);
			mFlipRefTexBuffer.put(1f);
			mFlipRefTexBuffer.put(1 - mReflectRatio);

			ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
			ibb.order(ByteOrder.nativeOrder());
			mIndexBuffer = ibb.asShortBuffer();

			ByteBuffer cbb = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			cbb.order(ByteOrder.nativeOrder());
			colorBuffer = cbb.asFloatBuffer();
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(0.7f);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(0.7f);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(0.0f);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(1);
			colorBuffer.put(0.0f);

			FloatBuffer firstBuffer;
			FloatBuffer secondBuffer;
			FloatBuffer thirdBuffer;
			FloatBuffer fourthBuffer;
			FloatBuffer fifthBuffer;

			ByteBuffer first = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			first.order(ByteOrder.nativeOrder());
			firstBuffer = first.asFloatBuffer();
			firstBuffer.put(0);
			firstBuffer.put(0);
			firstBuffer.put(1);
			firstBuffer.put(1f);
			firstBuffer.put(0);
			firstBuffer.put(0);
			firstBuffer.put(1);
			firstBuffer.put(1f);
			firstBuffer.put(0);
			firstBuffer.put(0);
			firstBuffer.put(1);
			firstBuffer.put(1f);
			firstBuffer.put(0);
			firstBuffer.put(0);
			firstBuffer.put(1);
			firstBuffer.put(1f);

			ByteBuffer second = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			second.order(ByteOrder.nativeOrder());
			secondBuffer = second.asFloatBuffer();
			secondBuffer.put(0);
			secondBuffer.put(1);
			secondBuffer.put(0);
			secondBuffer.put(1f);
			secondBuffer.put(0);
			secondBuffer.put(1);
			secondBuffer.put(0);
			secondBuffer.put(1f);
			secondBuffer.put(0);
			secondBuffer.put(1);
			secondBuffer.put(0);
			secondBuffer.put(1f);
			secondBuffer.put(0);
			secondBuffer.put(1);
			secondBuffer.put(0);
			secondBuffer.put(1f);

			ByteBuffer third = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			third.order(ByteOrder.nativeOrder());
			thirdBuffer = third.asFloatBuffer();
			thirdBuffer.put(0);
			thirdBuffer.put(1);
			thirdBuffer.put(1);
			thirdBuffer.put(1f);
			thirdBuffer.put(0);
			thirdBuffer.put(1);
			thirdBuffer.put(1);
			thirdBuffer.put(1f);
			thirdBuffer.put(0);
			thirdBuffer.put(1);
			thirdBuffer.put(1);
			thirdBuffer.put(1f);
			thirdBuffer.put(0);
			thirdBuffer.put(1);
			thirdBuffer.put(1);
			thirdBuffer.put(1f);

			ByteBuffer fourth = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			fourth.order(ByteOrder.nativeOrder());
			fourthBuffer = fourth.asFloatBuffer();
			fourthBuffer.put(1);
			fourthBuffer.put(0);
			fourthBuffer.put(0);
			fourthBuffer.put(1f);
			fourthBuffer.put(1);
			fourthBuffer.put(0);
			fourthBuffer.put(0);
			fourthBuffer.put(1f);
			fourthBuffer.put(1);
			fourthBuffer.put(0);
			fourthBuffer.put(0);
			fourthBuffer.put(1f);
			fourthBuffer.put(1);
			fourthBuffer.put(0);
			fourthBuffer.put(0);
			fourthBuffer.put(1f);

			ByteBuffer fifth = ByteBuffer.allocateDirect(VERTS * 4 * 4);
			fifth.order(ByteOrder.nativeOrder());
			fifthBuffer = fifth.asFloatBuffer();
			fifthBuffer.put(1);
			fifthBuffer.put(0);
			fifthBuffer.put(1);
			fifthBuffer.put(1f);
			fifthBuffer.put(1);
			fifthBuffer.put(0);
			fifthBuffer.put(1);
			fifthBuffer.put(1f);
			fifthBuffer.put(1);
			fifthBuffer.put(0);
			fifthBuffer.put(1);
			fifthBuffer.put(1f);
			fifthBuffer.put(1);
			fifthBuffer.put(0);
			fifthBuffer.put(1);
			fifthBuffer.put(1f);

			float[] coords = {
					// X, Y, Z
					edge, edge, 0, -edge, edge, 0, edge, -edge, 0, -edge,
					-edge, 0 };
			float scale = (float) COVER_EDGE_HORIZONTAL_RESIZED
					/ COVER_EDGE_HORIZONTAL;
			for (int i = 0; i < VERTS; i++) {
				for (int j = 0; j < 3; j++) {
					mFVertexBuffer.put(coords[i * 3 + j]);
					if (j == 0) {
						mResizedVerBuffer.put(coords[i * 3 + j] * scale);
					} else {
						mResizedVerBuffer.put(coords[i * 3 + j]);
					}
				}
			}
			for (int i = 0; i < VERTS; i++) {
				mIndexBuffer.put((short) i);
			}

			mFVertexBuffer.position(0);
			mTexBuffer.position(0);
			mIndexBuffer.position(0);
			colorBuffer.position(0);
			mRefVertexBuffer.position(0);
			mRefTexBuffer.position(0);
			mFlipTexBuffer.position(0);
			mFlipRefTexBuffer.position(0);
			mResizedVerBuffer.position(0);

			firstBuffer.position(0);
			secondBuffer.position(0);
			thirdBuffer.position(0);
			fourthBuffer.position(0);
			fifthBuffer.position(0);

			mPickColorBuffer = new FloatBuffer[] { firstBuffer, secondBuffer,
					thirdBuffer, fourthBuffer, fifthBuffer };

		}
	}

	private class FlipRunnable implements Runnable {
		private Scroller mRotateScroller;
		private int mLastAngle;
		private boolean isFlipToBack;

		public FlipRunnable() {
			mRotateScroller = new Scroller(getContext());
		}

		public void endFlip(boolean rotateback) {
			mRotateScroller.forceFinished(true);
			if (rotateback) {
				mCenterCoverAngle = ((mCenterCoverAngle / 180) % 2) * 180;
			}
			if (mOnCoverFlipListener != null) {
				mOnCoverFlipListener.onCoverFinishFlip(CoverFlow.this,
						mCenterCover, isFlipToBack);
			}

			if (isFlipToBack) {

				mState = STATE_FLIPPED;

			} else {
				mState = STATE_IDLE;

			}

		}

		public void startFlip(boolean back) {
			removeCallbacks(this);
			isFlipToBack = back;
			mState = STATE_FLIPPING;

			mLastAngle = mCenterCoverAngle;
			invokeOnFlipListener(back);
			mRotateScroller.startScroll(mLastAngle, 0, 180, 0,
					mFlipAnimationDuration);
			post(this);
		}

		public void run() {
			if (mCoverCount == 0) {
				endFlip(true);
				return;
			}

			mShouldStopRotate = false;

			final Scroller scroller = mRotateScroller;
			boolean more = scroller.computeScrollOffset();
			final int angle = scroller.getCurrX();

			int degree = angle - mLastAngle;

			rotateCenterCover(degree);

			if (more && !mShouldStopRotate) {
				mLastAngle = angle;
				post(this);
			} else {
				endFlip(true);
			}

		}
	}

	// private RotateRunnable mRotateRunnable = new RotateRunnable();
	//
	// private class RotateRunnable implements Runnable {
	// private Scroller mRotateScroller;
	// private int mLastAngle;
	//
	// public RotateRunnable() {
	// mRotateScroller = new Scroller(getContext());
	// }
	//
	// private void startCommon() {
	// // Remove any pending flings
	// removeCallbacks(this);
	// mState = STATE_FLIPPING;
	// }
	//
	// public void startRotate(int angle) {
	// if (angle == 0)
	// return;
	//
	// startCommon();
	//
	// mLastAngle = 0;
	// mRotateScroller
	// .startScroll(0, 0, angle, 0, mAnimationDuration * 10);
	// post(this);
	//
	// }
	//
	// public void endRotate(boolean rotateback) {
	// mRotateScroller.forceFinished(true);
	// if (rotateback) {
	// mCenterCoverAngle = Math.round(mCenterCoverAngle / 180) * 180;
	// }
	// mState = STATE_IDLE;
	// }
	//
	// @Override
	// public void run() {
	//
	// if (mCoverCount == 0) {
	// endRotate(true);
	// return;
	// }
	//
	// mShouldStopRotate = false;
	//
	// final Scroller scroller = mRotateScroller;
	// boolean more = scroller.computeScrollOffset();
	// final int angle = scroller.getCurrX();
	//
	// // Flip sign to convert finger direction to list items direction
	// // (e.g. finger moving down means list is moving towards the top)
	// int degree = angle - mLastAngle;
	//
	// rotateCenterCover(degree);
	//
	// if (more && !mShouldStopRotate) {
	// mLastAngle = angle;
	// post(this);
	// } else {
	// endRotate(true);
	// ;
	// }
	//
	// }
	//
	// }

	public void rotateCenterCover(int degree) {

		mCenterCoverAngle += degree;
		requestRender();
	}

	private class FlingRunnable implements Runnable {
		/**
		 * Tracks the decay of a fling scroll
		 */
		private CoverFlowScroller mScroller;

		/**
		 * X value reported by mScroller on the previous fling
		 */
		private int mLastFlingX;

		public FlingRunnable() {
			mScroller = new CoverFlowScroller(getContext());
		}

		private void startCommon() {
			// Remove any pending flings
			removeCallbacks(this);
		}

		public void startUsingVelocity(int initialVelocity) {
			if (initialVelocity == 0)
				return;

			startCommon();
			// isFlinged = true;

			int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
			mLastFlingX = initialX;
			mScroller.fling(initialX, 0, initialVelocity, 0, 0,
					Integer.MAX_VALUE, 0, Integer.MAX_VALUE, mCoverSpace,
					mCenter % mCoverSpace);
			post(this);
		}

		public void startUsingDistance(int distance) {
			if (distance == 0)
				return;

			startCommon();

			mLastFlingX = 0;
			mScroller.startScroll(0, 0, -distance, 0, Math.min((int) (mAnimationDuration
					* ((float) Math.abs(distance)) / mCoverSpace),800));
			post(this);

		}

		public void stop(boolean scrollIntoSlots) {
			removeCallbacks(this);
			endFling(scrollIntoSlots);

		}

		private void endFling(boolean scrollIntoSlots) {
			/*
			 * Force the scroller's status to finished (without setting its
			 * position to the end)
			 */
			mScroller.forceFinished(true);

			if (scrollIntoSlots)
				scrollIntoSlots();
		}

		public void run() {

			if (mCoverCount == 0) {
				endFling(true);
				return;
			}

			mShouldStopFling = false;

			final CoverFlowScroller scroller = mScroller;
			boolean more = scroller.computeScrollOffset();
			final int x = scroller.getCurrX();

			// Flip sign to convert finger direction to list items direction
			// (e.g. finger moving down means list is moving towards the top)
			int delta = mLastFlingX - x;

			// Pretend that each frame of a fling scroll is a touch scroll
			if (delta > 0) {
				// Moving towards the left. Use first view as mDownTouchPosition

				// mDownTouchPosition = mFirstPosition;

				// Don't fling more than 1 screen
				delta = Math.min(getWidth() - 1, delta);
			} else {
				// Moving towards the right. Use last view as mDownTouchPosition
				// int offsetToLast = getChildCount() - 1;

				// mDownTouchPosition = mFirstPosition + offsetToLast;

				// Don't fling more than 1 screen
				delta = Math.max(-(getWidth() - 1), delta);
			}

			trackMotionScroll(delta);

			if (more && !mShouldStopFling) {
				mLastFlingX = x;
				post(this);
			} else {
				endFling(true);
			}
		}
	}

	public void flipCenterCover() {
		if (mFlingRunnable.mScroller.isFinished()) {
			if (mState == STATE_IDLE) {
				mFlipRunnable.startFlip(true);
			} else if (mState == STATE_FLIPPED) {
				mFlipRunnable.startFlip(false);
			}
		}
	}

	private float getDepth(float offset) {
		return -Math.abs(offset)
				* (mOrientation == CONFIGURATION_LANDSCAP ? 2.5f : 3.6f)
				- COVER_CENTERDEPTH;
	}

	void invokeOnFlipListener(boolean isFlipToBack) {
		if (mOnCoverFlipListener != null) {
			mOnCoverFlipListener.onCoverStartFlip(CoverFlow.this, mCenterCover,
					isFlipToBack);
		}
	}
	
//	private int mBackground_land;
//	private int mBackground_port;
//	private int mBackground;
	private void genBackViewTexture(GL10 gl) {
		mBackViewTexture_land = genResourceTexture(gl, R.drawable.backview_land);
		mBackViewTexture_port = genResourceTexture(gl, R.drawable.backview_portrait);
		
		mNoMusicMessage = new StringTexture(getResources().getString(R.string.all_album_isnull),mNoMusicConfig);
		
	}
	
	private void clearBackground(){
		mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
//		GL11Ext gl = (GL11Ext)mGL;
//		mGL.glBindTexture(GL10.GL_TEXTURE_2D, mBackground);
//		gl.glDrawTexfOES(0f, 0f, 1f, getWidth(), getHeight());
		
		
	}

	private int genResourceTexture(GL10 gl, int resId) {
		Bitmap bitmap = null;
		InputStream is = getContext().getResources().openRawResource(resId);
		try {
			bitmap = BitmapFactory.decodeStream(is);
		} catch (OutOfMemoryError e) {
			// to do in future:

		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
		if (bitmap != null) {
			int textureId[] = new int[1];
			gl.glGenTextures(1, textureId, 0);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
					GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
					GL10.GL_CLAMP_TO_EDGE);

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			bitmap.recycle();
			return textureId[0];
		} else {
			return -1;
		}
	}
	
	private int genTextureWithRect(GL10 gl,Bitmap bitmap,int[] cropRect){
		GL11 newGL = (GL11) gl;
		int textureId[] = new int[1];
		newGL.glGenTextures(1, textureId, 0);
		newGL.glBindTexture(GL11.GL_TEXTURE_2D, textureId[0]);

		newGL.glTexParameteriv(GL11.GL_TEXTURE_2D,
				GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
		newGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
				GL11.GL_CLAMP_TO_EDGE);
		newGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
				GL11.GL_CLAMP_TO_EDGE);
		newGL.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
				GL11.GL_LINEAR);
		newGL.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
				GL11.GL_LINEAR);

		GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, bitmap, 0);

		return textureId[0];

	}

	private void genDefaultAlbumTexture(GL10 gl) {
		Bitmap bitmap;
		InputStream is = getContext().getResources().openRawResource(
				R.drawable.albumart_mp_unknown);
		try {
			bitmap = BitmapFactory.decodeStream(is);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// Ignore.
			}
		}

		int textureId[] = new int[1];
		gl.glGenTextures(1, textureId, 0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		mDefaultAlbumTexture = textureId[0];
		bitmap.recycle();
	}

	public int genTexture(Bitmap bitmap, GL10 gl) {
		int textureId[] = new int[1];
		gl.glGenTextures(1, textureId, 0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId[0]);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		return textureId[0];
	}

	public void getSelectedCover() {
		if (mAdapter != null) {
			mAdapter.getCover(mSelectedCover);
		}
	}

	public int getOffsetY() {
		return mOffsetY;
	}

	public int getOffsetX() {
		return mOffsetX;
	}

	public boolean preloadTexture(Cover cover, boolean highPriority) {
		if (cover != null) {
			switch (cover.mState) {
			case Cover.STATE_UNLOADED:
				queueLoad(cover, highPriority);
				break;
			case Cover.STATE_LOADED:
				cover.bind(mGL);
				return true;
			case Cover.INVALID_TEXTURE:
				return true;
			default:
				break;
			}
			return false;
		} else {
			return true;
		}
	}

	public boolean bindCoverTexture(Cover cover) {
		if (cover != null) {
			switch (cover.mState) {
			case Cover.STATE_UNLOADED:
			case Cover.STATE_QUEUED:
				if (mLoadingCount < MAX_LOADING_COUNT) {
					queueLoad(cover, true);
				}
				mGL.glBindTexture(GL11.GL_TEXTURE_2D, mDefaultAlbumTexture);
				break;
			case Cover.STATE_LOADED:
				cover.bind(mGL);
				return true;
			default:
				mGL.glBindTexture(GL11.GL_TEXTURE_2D, mDefaultAlbumTexture);
				return true;

			}
		}
		return false;

	}

	private void queueLoad(final Cover cover, boolean highPriority) {

		// Push the texture onto the load input queue.
		Deque<Cover> inputQueue = mLoadingQueue;

		synchronized (inputQueue) {
			if (highPriority) {
				inputQueue.addFirst(cover);
				// Enforce the maximum loading count by removing something from
				// the end of
				// the loading queue, if necessary.
				if (mLoadingCount >= MAX_LOADING_COUNT) {
					Cover unLoadCover = inputQueue.pollLast();
					unLoadCover.mState = Cover.STATE_UNLOADED;
					--mLoadingCount;
				}
			} else {
				inputQueue.addLast(cover);
			}
			cover.mState = Cover.STATE_QUEUED;
			inputQueue.notify();
		}
		++mLoadingCount;
	}

	private static int MAX_LOADING_COUNT = 12;
	private int mBufferStart;
	private int mBufferEnd;
	private int mLoadingCount;
	private GL11 mGL;
	private Deque<Cover> mLoadingQueue = new Deque<Cover>();

	private void loadCoverBitmap(Cover cover) {
		if (cover.mState == Cover.STATE_QUEUED || cover.mState == Cover.STATE_UNLOADED) {
			cover.mState = Cover.STATE_LOADING;
			Bitmap bm = cover.loadCoverBitmap();
			if (bm != null) {
				cover.mState = Cover.STATE_LOADED;
			} else {
				cover.albumTexture = new StringTexture(cover.albumName,
						mTitleConfig);
				cover.artistTexture = new StringTexture(cover.artistName,
						mArtistConfig);
				cover.initTitleQuad();
				cover.mTextureId = mDefaultAlbumTexture;
				cover.mState = Cover.INVALID_TEXTURE;
			}

			requestRender();
		}
	}

	public class CoverLoadThread extends Thread {
		@Override
		public void run() {
			while (true) {
				Deque<Cover> queue = mLoadingQueue;
				try {
					Cover cover = null;
					synchronized (queue) {
						while ((cover = queue.pollFirst()) == null) {
							queue.wait();
						}
					}
					loadCoverBitmap(cover);
					--mLoadingCount;
				} catch (InterruptedException e) {

				}
			}
		}
	}

	public void loadTexture(Texture texture) {
		if (texture != null) {
			switch (texture.mState) {
			case Texture.STATE_UNLOADED:
			case Texture.STATE_QUEUED:
				int[] textureId = new int[1];
				texture.mState = Texture.STATE_LOADING;
				loadTextureAsync(texture);
				uploadTexture(texture, textureId);
				break;
			}
		}
	}

	private void loadTextureAsync(Texture texture) {
		try {
			Bitmap bitmap = texture.load(this);
			if (bitmap != null) {
				bitmap = Texture.resizeBitmap(bitmap, 256);
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				texture.mWidth = width;
				texture.mHeight = height;
				// Create a padded bitmap if the natural size is not a power of
				// 2.
				if (!Shared.isPowerOf2(width) || !Shared.isPowerOf2(height)) {
					int paddedWidth = Shared.nextPowerOf2(width);
					int paddedHeight = Shared.nextPowerOf2(height);
					Bitmap.Config config = bitmap.getConfig();
					if (config == null)
						config = Bitmap.Config.RGB_565;
					if (width * height >= 512 * 512)
						config = Bitmap.Config.RGB_565;
					Bitmap padded = Bitmap.createBitmap(paddedWidth,
							paddedHeight, config);
					Canvas canvas = new Canvas(padded);
					canvas.drawBitmap(bitmap, 0, 0, null);
					bitmap.recycle();
					bitmap = padded;
					// Store normalized width and height for use in texture
					// coordinates.
					texture.mNormalizedWidth = (float) width
							/ (float) paddedWidth;
					texture.mNormalizedHeight = (float) height
							/ (float) paddedHeight;
				} else {
					texture.mNormalizedWidth = 1.0f;
					texture.mNormalizedHeight = 1.0f;
				}
			}
			texture.mBitmap = bitmap;
		} catch (Exception e) {
			texture.mBitmap = null;
		} catch (OutOfMemoryError eMem) {
			Log.i("CoverFlow", "Bitmap power of 2 creation fail, outofmemory");

		}
	}

	private void uploadTexture(Texture texture, int[] textureId) {
		Bitmap bitmap = texture.mBitmap;
		GL11 gl = mGL;
		int glError = GL11.GL_NO_ERROR;
		if (bitmap != null) {
			final int width = texture.mWidth;
			final int height = texture.mHeight;

			// Define a vertically flipped crop rectangle for OES_draw_texture.
			int[] cropRect = { 0, height, width, -height };

			// Upload the bitmap to a new texture.
			gl.glGenTextures(1, textureId, 0);
			gl.glBindTexture(GL11.GL_TEXTURE_2D, textureId[0]);
			gl.glTexParameteriv(GL11.GL_TEXTURE_2D,
					GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
			gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
					GL11.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
					GL11.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
					GL11.GL_LINEAR);
			gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
					GL11.GL_LINEAR);
			GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, bitmap, 0);
			glError = gl.glGetError();

			bitmap.recycle();
			if (glError == GL11.GL_OUT_OF_MEMORY) {

			}
			if (glError != GL11.GL_NO_ERROR) {
				// There was an error, we need to retry this texture at some
				// later time
				texture.mId = 0;
				texture.mBitmap = null;
				texture.mState = Texture.STATE_UNLOADED;
			} else {
				// Update texture state.
				texture.mBitmap = null;
				texture.mId = textureId[0];
				texture.mState = Texture.STATE_LOADED;

				// Add to the active list.
				requestRender();
			}
		} else {
			texture.mState = Texture.STATE_ERROR;
		}

	}
	
	public boolean isFlipped(){
		return mState == STATE_FLIPPED || mState == STATE_FLIPPING;
	}
	
	public CoverFlowAdapter getAdapter(){
		return mAdapter;
	}
	
	private void setSeekBar(int progress,int max){
		CoverFlowWrapper cw = (CoverFlowWrapper)getParent();
		cw.setSeekBar(progress,max);
	}

	private void drawNoMusicMessage(GL10 gl){
		if(mNoMusicMessage != null){
			StringTexture text = mNoMusicMessage;
			if(text.mState != Texture.STATE_LOADED){
				loadTexture(text);
			}
			
			if(text.mState == Texture.STATE_LOADED){
				gl.glBindTexture(GL10.GL_TEXTURE_2D, text.mId);
				GL11Ext gl11 = (GL11Ext)gl;
				
				gl.glEnable(GL10.GL_BLEND);
				gl11.glDrawTexfOES((mWidth - text.getWidth())/2, (mHeight-text.getHeight())/2, 0.0f, text.getWidth(), text.getHeight());
				gl.glDisable(GL10.GL_BLEND);
			}
		}
	}
	
	public void setInitialFlipStatus(){
		mCenterCoverAngle = 0;
		mState = STATE_IDLE;
	}
}

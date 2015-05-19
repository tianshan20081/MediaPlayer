package com.weichuang.china.music;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.china.R;
import com.weichuang.china.music.LyricsParser.Sentence;

public class LyricsTextView extends TextView {
	static final String TAG = "LyricsText";
	static final char ENTER = '\n';
	private static List<Sentence> list;

	private AtomicInteger currentLyricIndex;
	private int textHighlightColor;
	private int textNormalColor;
	private int textPitch;

	// add by yangguangfu

//	private Paint mPaint;
	private float mX;
	// private static LyricsParser mLyric;

//	private Path mPath;
//	private Paint mPathPaint;
	public String test = "test";
	public int index = 0;

	private float mTouchStartY;
	private float mTouchCurrY;
	public float mTouchHistoryY;

	private float mY;
	private long currentTime;
	private long currentDunringTime;
	private long sentenctTime;
	private float middleY;
	private String middleContent = "Empty";
	private static final int DY = 30;
	private static final int DZ = 30;

	public LyricsTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.LyricsView);
		textHighlightColor = a.getColor(
				R.styleable.LyricsView_textHighlightColor,
				android.R.color.white);
		textNormalColor = a.getColor(R.styleable.LyricsView_textNormalColor,
				android.R.color.darker_gray);
		textPitch = a.getDimensionPixelSize(R.styleable.LyricsView_textPitch,
				-1);
		Log.d(TAG, "lyric text view get the textPitch is :" + textPitch);
		// paint = new Paint();
		a.recycle();
	}

	public LyricsTextView(Context context, AttributeSet attrs) {

		this(context, attrs, 0);
	}

	public LyricsTextView(Context context) {
		super(context);
		// paint = new Paint();
		initialize();
		Log.d(TAG, "LyricTextView Contruator with Context only.");

	}

	// private int getSentenceX(Paint g, Sentence sen) {
	// int x = 0;
	// // int i = Config.getConfig().getLyricAlignMode();
	// int i = 0;
	// switch (i) {
	// case 0:
	// x = (width - sen.getContentWidth(g)) / 2;
	// break;
	// case 1:
	// x = 0;
	// break;
	// case 2:
	// x = width - sen.getContentWidth(g);
	// break;
	// default:// 
	// x = (width - sen.getContentWidth(g)) / 2;
	// break;
	// }
	// Log.i(TAG, "x:" + x);
	// return x;
	// }

	// private void drawString(Canvas canvas, Paint g, String content, int x,
	// int y) {
	// canvas.drawText(content, x, y, g);
	// }

	public int getCurrentLyricToDraw(long playedTime) {
		if (list == null) {
			return -1;
		}
		int len = list.size();
		for (int i = 0; i < len; i++) {
			final Sentence sen = list.get(i);
			if (sen.getFromTime() > playedTime) {
				currentLyricIndex.set(i - 1);
				return currentLyricIndex.get();
			}
		}
		currentLyricIndex.set(len - 1);
		return currentLyricIndex.get();
	}

	/**
	 * clear the buffered lyrics.
	 */
	public void clearLyricsBuffer() {
		if (list != null && !list.isEmpty()) {
			list.clear();
			
		}
	}

	public int getNowSentenceIndex(long t) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isInTime(t)) {
				return i;
			}
		}
		
		return -1;
	}

	// public void adjustLyrics(long playedTime) {
	// final int current = getCurrentLyricToDraw(playedTime);
	// if (current == lastDrawedIndex || current < 0) {// if this sentence
	// // hasn't
	// return;
	// } else {
	// this.postInvalidate();
	// }
	// }

	public void adjustLyric(long playedTime) {

		if (list == null || list.size() == 0) {
			return;
		}

		int listSize = list.size();
	
		index = getCurrentLyricToDraw(playedTime);
		int ind = index + 1;
		if (index != -1) {
			
			Sentence sen = list.get(index);
			currentDunringTime = sen.getDuring();
			sentenctTime = sen.getFromTime();
			if (ind != listSize) {
				this.currentTime = playedTime;
//				index = ind;
			}

		}
		this.invalidate();

	}

	private void initializeLyricToDraw() {
		currentLyricIndex.set(0);
		this.postInvalidate();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	// @Override
	// public boolean onTouchEvent(MotionEvent event) {
	// float y = event.getY();
	// logInfo("onTouchEvent:y:" + y);
	//
	// switch (event.getAction()) {
	// case MotionEvent.ACTION_DOWN:
	// mTouchHistoryY += mTouchCurrY - mTouchStartY;
	// logInfo("onTouchEvent:mTouchHistoryY:" + mTouchHistoryY);
	// mTouchStartY = mTouchCurrY = y;
	// logInfo("onTouchEvent:mTouchStartY:" + mTouchStartY);
	// invalidate();
	// break;
	// case MotionEvent.ACTION_MOVE:
	// mTouchCurrY = y;
	// logInfo("onTouchEvent:mTouchCurrY:" + mTouchCurrY);
	// invalidate();
	// break;
	// case MotionEvent.ACTION_UP:
	// Log.v("Lyric content", middleContent.length() + "");
	// logInfo("onTouchEvent:middleContent:" + middleContent);
	// CharSequence chars = new CharSequence() {
	//
	// @Override
	// public char charAt(int index) {
	// logInfo("onTouchEvent:charAt:"
	// + middleContent.charAt(index));
	// return middleContent.charAt(index);
	// }
	//
	// @Override
	// public int length() {
	// logInfo("onTouchEvent:length:" + middleContent.length());
	// return middleContent.length();
	// }
	//
	// @Override
	// public CharSequence subSequence(int start, int end) {
	// logInfo("onTouchEvent:subSequence:"
	// + middleContent.subSequence(start, end));
	// return middleContent.subSequence(start, end);
	// }
	//
	// @Override
	// public String toString() {
	// logInfo("onTouchEvent:middleContent:" + middleContent);
	// return middleContent;
	// }
	// };
	// Toast toast = Toast.makeText(SampleView.this.getContext(), chars,
	// 1000);
	// toast.show();
	// invalidate();
	// break;
	// }
	// return true;
	// }
	
	
	
	
//	public synchronized void drawV(Canvas canvas, Paint g) {
//		// if (!enabled) {
//		// Sentence sen = new Sentence(info.getFormattedName());
//		// int x = (width - sen.getContentWidth(g)) / 2;
//		// // int y = (height - sen.getContentHeight(g) +
//		// // Config.getConfig().getV_SPACE()) / 2;
//		// int y = (height - sen.getContentHeight(g) + 0) / 2;
//		// // g.setColor(Config.getConfig().getLyricHilight());
//		// g.setColor(Color.GREEN);
//		// // Util.drawString(g, sen.getContent(), x, y);
//		// drawString(canvas, g, sen.getContent(), x, y);
//		// return;
//		// }
//		// 
//		// if (!initDone) {
//		// Sentence temp = new Sentence("");
//		// int x = getSentenceX(g, temp);
//		// int y = (height - temp.getContentHeight(g)) / 2;
//		// // g.setColor(Config.getConfig().getLyricHilight());
//		// g.setColor(Color.GREEN);
//		// // Util.drawString(g, temp.getContent(), x, y);
//		// drawString(canvas, g, temp.getContent(), x, y);
//		// return;
//		// }
//		// 
//		//
//		if (list.size() == 1) {
//			// Sentence sen = list.get(0);
//			// int x = getSentenceX(g, sen);
//			// int y = (height - sen.getContentHeight(g)) / 2;
//			// // g.setColor(Config.getConfig().getLyricHilight());
//			// g.setColor(Color.GREEN);
//			// drawString(canvas, g, sen.getContent(), x, y);
//		} else {
//			// long t = tempTime;
//			long t = currentTime;
//			// Graphics2D gd = (Graphics2D) g;
//			int index = getNowSentenceIndex(t);
//			Log.i(TAG, "index:" + index);
//			if (index == -1)
//				return;
//			// if (!isMoving) {
//			// currentIndex = index;
//			// }
//			// if (index == -1) {
//			// Sentence sen = new Sentence(info.getFormattedName(),
//			// Integer.MIN_VALUE, Integer.MAX_VALUE);
//			// int x = getSentenceX(g, sen);
//			// int y = (height - sen.getContentHeight(g)) / 2;
//			// // gd.setPaint(Config.getConfig().getLyricHilight());
//			// g.setColor(Color.GREEN);
//			// drawString(canvas, g, sen.getContent(), x, y);
//			// return;
//			// }
//			Sentence now = list.get(index);
//			Log.i(TAG, "now:" + now);
//			// 
//			int y = (height + now.getContentHeight(g)) / 2
//					- now.getVIncrease(g, t);
//			// int x = getSentenceX(g, now);
//			// this.drawKaraoke(canvas, g, now, x, y, t);
//			// gd.setColor(Config.getConfig().getLyricForeground());
//			// 
//			// 
//			int tempY = y;
//			// 
//			for (int i = index - 1; i >= 0; i--) {
//				Sentence sen = list.get(i);
//				int x1 = getSentenceX(g, sen);
//				// tempY = tempY - sen.getContentHeight(g) -
//				// Config.getConfig().getV_SPACE();
//				tempY = tempY - sen.getContentHeight(g) - 0;
//				if (tempY + sen.getContentHeight(g) < 0) {
//					break;
//				}
//				// if (Config.getConfig().isLyricShadow()) {
//				if (true) {
//					if (i == index - 1) {
//						// gd.setColor(sen.getBestOutColor(Config.getConfig().getLyricHilight(),
//						// Config.getConfig().getLyricForeground(), time));
//						g.setColor(Color.GREEN);
//					} else {
//						// gd.setColor(Config.getConfig().getLyricForeground());
//						g.setColor(Color.WHITE);
//					}
//				}
//				drawString(canvas, g, sen.getContent(), x1, tempY);
//			}
//			// gd.setColor(Config.getConfig().getLyricForeground());
//			tempY = y;
//			// 
//			for (int i = index + 1; i < list.size(); i++) {
//				Sentence sen = list.get(i);
//				int x1 = getSentenceX(g, sen);
//				// tempY = tempY + sen.getContentHeight(g) +
//				// Config.getConfig().getV_SPACE();
//				tempY = tempY + sen.getContentHeight(g) + 0;
//				if (tempY > height) {
//					break;
//				}
//				drawString(canvas, g, sen.getContent(), x1, tempY);
//			}
//		}
//	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (list == null || list.size() == 0) {
			return;
		}

//		canvas.drawColor(Color.DKGRAY);
	
		Paint p = getPaint();
		p.setAntiAlias(true);
		p.setTextSize(getTextSize());
		p.setTypeface(Typeface.SERIF);
//		p.setTextAlign(Align.LEFT);
		p.setTextAlign(Paint.Align.CENTER);
		
		float x = mX;
		int len = list.size();

		float plus = currentDunringTime == 0 ? index * DZ
				: index
						* DZ
						+ (((float) currentTime - (float) sentenctTime) / (float) currentDunringTime)
						* (float) DZ;

		float y = mY - plus + mTouchCurrY - mTouchStartY + mTouchHistoryY;

		canvas.translate(0, y);

		for (int i = 0; i < len; i++) {
			String text = list.get(i).getContent();
			
			if ((y + i * DZ) <= middleY && (y + i * DZ + DZ) >= middleY) {
				middleContent = text;
				p.setColor(Color.GREEN);
//				p.setColor(textHighlightColor);
				canvas.drawText(middleContent, x, 0, p);

			} else {

//				p.setColor(textNormalColor);
				p.setColor(Color.WHITE);
				canvas.drawText(text, x, 0, p);

			}

			canvas.translate(0, DY);
		}

	}

	private void initialize() {
		currentLyricIndex = new AtomicInteger(0);
		Log.d(TAG, "Lyric text view .initialize()");
	}

	public List<Sentence> getLyrics() {
		return list;
	}

	private void setLyrics(List<Sentence> lyrics) {
		
		this.list = lyrics;
		initializeLyricToDraw();
	}

	public static void logInfo(String content) {
		Log.i(TAG, content);
	}

	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		
		mX = w * 0.5f; 
		mY = h * 0.6f;
		middleY = h * 0.611f;
	}

	public void setLyricContent(CharSequence content) {
		setLyricContent(content.toString());
	}

	public void setLyricContent(String content) {
		clearLyricsBuffer();
		LyricsParser lyricsParser = new LyricsParser(content);
		setLyrics(lyricsParser.getLyrics());
	}

	public void setLyricContent(InputStream input) {
		
		LyricsParser lyricsParser = new LyricsParser(input);
		setLyrics(lyricsParser.getLyrics());
	}

}

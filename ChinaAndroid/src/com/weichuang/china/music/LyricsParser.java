package com.weichuang.china.music;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.text.TextUtils;
import android.util.Log;


public class LyricsParser {
	static final String TAG = "LyricsParser";
	public List<Sentence> list = new ArrayList<Sentence>();
	// static final String CHARSET = "UTF-8";
	static final String CHARSET = "gbk";
	private static final char NEXT_LINE = '\n';
	private static final Pattern pattern = Pattern
			.compile("(?<=\\[).*?(?=\\])");
	private static final Pattern LRC_PATTERN = Pattern
			.compile("\\[\\d+:[^]]+\\](.*)");

	// add yangguangfu
	private int offset;

	/**
	 * 
	 * add by yangguangfu
	 */
	public LyricsParser() {

	}

	/**
	 * 
	 * @param lyric
	 *            see{@link java.lang.CharSequence}
	 */
	public LyricsParser(CharSequence lyric) {
		parse(lyric.toString());
	}

	/**
	 * 
	 * @param lyric
	 *            see{@link java.lang.String}
	 */
	public LyricsParser(String lyric) {
		parse(lyric);
	}

	/**
	 * 
	 * @param lyric
	 *            see{@link java.io.InputSream}
	 */
	public LyricsParser(InputStream in) {
		this(in, CHARSET);
	}

	public LyricsParser(InputStream in, String charset) {
		parse(getContent(in, charset));
	}

	private String getContent(InputStream inputStream, String charset) {
		BufferedReader br = null;
		StringBuilder sb = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream, CHARSET));
			sb = new StringBuilder();
			String temp = null;
			while ((temp = br.readLine()) != null) {
				sb.append(temp).append("\n");
			}
			if (sb != null) {
				return (sb.toString());
			}
		} catch (Exception ex) {
			ex.printStackTrace();

		} finally {
			try {
				if(br != null){
					br.close();
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				// Logger.getLogger(Lyric.class.getName()).log(Level.SEVERE,
				// null, ex);
			}
		}
			return null;
	}

	private void parse(String content) {

		if (content == null || content.trim().equals("")) {
			// list.add(new Sentence(info.getFormattedName(), Integer.MIN_VALUE,
			// Integer.MAX_VALUE));
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new StringReader(content));
			String temp = null;
			while ((temp = br.readLine()) != null) {
				parseLine(temp.trim());
			}
			br.close();

			Collections.sort(list, new Comparator<Sentence>() {

				public int compare(Sentence o1, Sentence o2) {
					return (int) (o1.getFromTime() - o2.getFromTime());
				}
			});

			if (list.size() == 0) {
				// list.add(new Sentence(info.getFormattedName(), 0,
				// Integer.MAX_VALUE));
				return;
			} else {
				// Sentence first = list.get(0);
				// list.add(0, new Sentence(info.getFormattedName(), 0,
				// first.getFromTime()));
			}

			int size = list.size();
			for (int i = 0; i < size; i++) {
				Sentence next = null;
				if (i + 1 < size) {
					next = list.get(i + 1);
				}
				Sentence now = list.get(i);
				if (next != null) {
					now.setToTime(next.getFromTime() - 1);
				}
			}

			// if (list.size() == 1) {
			// list.get(0).setToTime(Integer.MAX_VALUE);
			// } else {
			// Sentence last = list.get(list.size() - 1);
			// last.setToTime(info == null ? Integer.MAX_VALUE :
			// info.getLength() * 1000 + 1000);
			// }
		} catch (Exception ex) {
			// Logger.getLogger(Lyric.class.getName()).log(Level.SEVERE, null,
			// ex);
			ex.printStackTrace();
		}
	}

	private void parseLine(String line) {
		if (line.equals("")) {
			return;
		}
		Matcher matcher = pattern.matcher(line);
		List<String> temp = new ArrayList<String>();
		int lastIndex = -1;//
		int lastLength = -1;//
		while (matcher.find()) {
			String s = matcher.group();
			int index = line.indexOf("[" + s + "]");
			if (lastIndex != -1 && index - lastIndex > lastLength + 2) {

				String content = line.substring(lastIndex + lastLength + 2,
						index);
				for (String str : temp) {
					long t = parseTime(str);
					if (t != -1) {
						list.add(new Sentence(content, t));
					}
				}
				temp.clear();
			}
			temp.add(s);
			lastIndex = index;
			lastLength = s.length();
		}

		if (temp.isEmpty()) {
			return;
		}
		try {
			int length = lastLength + 2 + lastIndex;
			String content = line.substring(length > line.length() ? line
					.length() : length);
			//           

			if (content.equals("") && offset == 0) {
				for (String s : temp) {
					int of = parseOffset(s);
					if (of != Integer.MAX_VALUE) {
						offset = of;
						// info.setOffset(offset);
						break;
					}
				}
				return;
			}
			for (String s : temp) {
				long t = parseTime(s);
				if (t != -1) {
					list.add(new Sentence(content, t));
				}
			}
		} catch (Exception exe) {
		}
	}

	private long parseTime(String time) {
		String[] ss = time.split("\\:|\\.");

		if (ss.length < 2) {
			return -1;
		} else if (ss.length == 2) {
			try {

				if (offset == 0 && ss[0].equalsIgnoreCase("offset")) {
					offset = Integer.parseInt(ss[1]);
					// info.setOffset(offset);

					return -1;
				}
				int min = Integer.parseInt(ss[0]);
				int sec = Integer.parseInt(ss[1]);
				if (min < 0 || sec < 0 || sec >= 60) {
					throw new RuntimeException("Digital illegal!");
				}
				return (min * 60 + sec) * 1000L;
			} catch (Exception exe) {
				return -1;
			}
		} else if (ss.length == 3) {//
			try {
				int min = Integer.parseInt(ss[0]);
				int sec = Integer.parseInt(ss[1]);
				int mm = Integer.parseInt(ss[2]);
				if (min < 0 || sec < 0 || sec >= 60 || mm < 0 || mm > 99) {
					throw new RuntimeException("Digital illegal !");
				}
				return (min * 60 + sec) * 1000L + mm * 10;
			} catch (Exception exe) {
				return -1;
			}
		} else {
			return -1;
		}
	}

	private int parseOffset(String str) {
		String[] ss = str.split("\\:");
		if (ss.length == 2) {
			if (ss[0].equalsIgnoreCase("offset")) {
				int os = Integer.parseInt(ss[1]);

				return os;
			} else {
				return Integer.MAX_VALUE;
			}
		} else {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * 
	 * add by yangguangfu
	 */
	public static String getContent2(InputStream in, String charset) {
		final StringBuilder buffer = new StringBuilder();
		try {
			BufferedReader reader = null;
			reader = new BufferedReader(new InputStreamReader(in, charset));
			String temp = null;
			while (!isEmpty((temp = reader.readLine()))) {
				buffer.append(temp).append(NEXT_LINE);
			}
			reader.close();
		} catch (IOException io) {
			Log.e(TAG, "IOException when paring lyrics.", io);
		}
		return buffer.toString();
	}

	private static boolean isEmpty(String s) {
		return (s == null || "".equals(s) || "".equals(s.trim()));
	}

	public List<Sentence> getLyrics() {
		return list;
	}

	public static boolean isFormateByLRC(CharSequence input) {
		if (TextUtils.isEmpty(input)) {
			return false;
		}
		return LRC_PATTERN.matcher(input).find();
	}

	public int getNowSentenceIndex(long t) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isInTime(t)) {
				return i;
			}
		}
		return -1;
	}
	
	public class Sentence implements Serializable {

		private static final long serialVersionUID = 20071125L;
		
		private long fromTime;
		
		private long toTime;
		
		private String content;
		private final static long DISAPPEAR_TIME = 1000L;

		public Sentence(String content, long fromTime, long toTime) {
			this.content = content;
			this.fromTime = fromTime;
			this.toTime = toTime;
		}

		public Sentence(String content, long fromTime) {
			this(content, fromTime, 0);
		}

		public Sentence(String content) {
			this(content, 0, 0);
		}

		
		public long getFromTime() {
			return fromTime;
		}

		public void setFromTime(long fromTime) {
			this.fromTime = fromTime;
		}

		
		public long getToTime() {
			return toTime;
		}

		public void setToTime(long toTime) {
			this.toTime = toTime;
		}

		
		public boolean isInTime(long time) {
			return time >= fromTime && time <= toTime;
		}

		
		public String getContent() {
			return content;
		}

		
		public int getVIncrease(Paint g, long time) {
			int height = getContentHeight(g);
			// return (int) ((height + Config.getConfig().getV_SPACE()) * ((time -
			// fromTime) * 1.0 / (toTime - fromTime)));
			return (int) ((height + 0) * ((time - fromTime) * 1.0 / (toTime - fromTime)));
		}

		
		public int getHIncrease(Paint g, long time) {
			int width = getContentWidth(g);
			// return (int) ((width + Config.getConfig().getH_SPACE()) * ((time -
			// fromTime) * 1.0 / (toTime - fromTime)));
			return (int) ((width + 0) * ((time - fromTime) * 1.0 / (toTime - fromTime)));
		}

		
		public int getContentWidth(Paint g) {
			return (int) g.measureText(content);
			// return (int) g.getFontMetrics().getStringBounds(content,
			// g).getWidth();
		}

		
		public long getDuring() {
			return toTime - fromTime;
		}

		
		public long getTimeH(int length, Paint g) {
			return getDuring() * length / getContentWidth(g);
		}

		
		public long getTimeV(int length, Paint g) {
			return getDuring() * length / getContentHeight(g);
		}

		
		public int getContentHeight(Paint g) {

			FontMetrics fm = g.getFontMetrics();// 
			int mFontHeight = (int) (Math.ceil(fm.descent - fm.top) + 2);//
			return mFontHeight;
			// return (int) g.getFontMetrics().getStringBounds(content,
			// g).getHeight() + Config.getConfig().getV_SPACE();
		}

		
		public Color getBestInColor(Color c1, Color c2, long time) {
			float f = (time - fromTime) * 1.0f / getDuring();
			if (f > 0.1f) {// 
				return c1;
			} else {
				long dur = getDuring();
				f = (time - fromTime) * 1.0f / (dur * 0.1f);
				if (f > 1 || f < 0) {
					return c1;
				}
				// return Util.getGradientColor(c2, c1, f);
				return null;
			}
		}

		
		public Color getBestOutColor(Color c1, Color c2, long time) {
			if (isInTime(time)) {
				return c1;
			}
			float f = (time - toTime) * 1.0f / DISAPPEAR_TIME;
			if (f > 1f || f <= 0) {// 
				return c2;
			} else {
				// return Util.getGradientColor(c1, c2, f);
				return null;
			}
		}

		public String toString() {
			return "{" + fromTime + "(" + content + ")" + toTime + "}";
		}

}}

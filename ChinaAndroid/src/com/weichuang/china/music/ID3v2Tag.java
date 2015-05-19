package com.weichuang.china.music;

/*
 $ javac ID3v2_Tag0816.java
 $ java ID3v2_Tag0816 <a.mp3>
 */
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

//import java.util.Map;
//import java.util.TreeMap;

@SuppressWarnings("serial")
class ID3TagException extends Exception {
	public ID3TagException(String reason) {
		super(reason);
	}
}

public class ID3v2Tag {

	private ID3v2_3_0Tag tag; // just point to v2.3.0

	public ID3v2Tag(String filename) {
		try {
			tag = new ID3v2_3_0Tag(filename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ID3TagException e) {
			e.printStackTrace();
		}
	}

	public String get_lyric() {
		// modified by liuqiang
		if (tag != null) {
			return tag.get_lyric();
		}
		return null;
		// return tag.get_lyric();
	}

	static private class ID3v2_3_0Tag {
//		private static final int UNSYNC = 1 << 7;
		private static final int EXTENDED_HEADER = 1 << 6;
//		private static final int EXPERIMENTAL = 1 << 5;

		private byte _majorVersion = 0;
		private byte _minorVersion = 0;
		private byte _flags = 0;
		private int _size = 0; // do not include the header

		private String lyric_contents = null; // 'USLY' content
		private String sylt_lyric_content = null; // 'SYLT'

		public String get_lyric() {
			if (lyric_contents != null)
				return lyric_contents;
			else
				return sylt_lyric_content;
		}

		public ID3v2_3_0Tag(String filename) throws IOException,
				ID3TagException {
			DataInputStream in = new DataInputStream(new BufferedInputStream(
					new FileInputStream(filename)));

			try {
				byte[] tagName = new byte[] { in.readByte(), in.readByte(),
						in.readByte() };
				if ((tagName[0] != 0x49) || (tagName[1] != 0x44)
						|| (tagName[2] != 0x33)) // 'I', 'D', '3'
					throw new ID3TagException("ERROR: No ID3v2 Tag!");

				_majorVersion = in.readByte();
				_minorVersion = in.readByte();

				Log.i("ID3v2Tag", "_majorVersion:" + _majorVersion);
				Log.i("ID3v2Tag", "_minorVersion:" + _minorVersion);

				_flags |= (in.readByte() & 0xff);
				_size = readSyncSafeInt(in);

				if ((_flags & EXTENDED_HEADER) != 0) {
					int extendedSize = readSyncSafeInt(in);
					byte nFlags = in.readByte();

					for (byte i = 0; i < nFlags; ++i)
						in.readByte();

					if (in.skipBytes(extendedSize) != extendedSize) // jump over
																	// the
																	// extended
																	// header
						throw new ID3TagException("ERROR: extended header!!");
				}
				Log.i("ID3v2Tag", "_flags:" + _flags);
				Log.i("ID3v2Tag", "_size:" + _size);
				
				//ע�͵���֧��ID3v2.4
//				if ((_flags & UNSYNC) != 0)
//					throw new ID3TagException(
//							"Unsynchronization not implemented!");
			} catch (EOFException e) {
				throw new ID3TagException("ERROR: corrupted file!");
			}

			/*  */
			while (readFrame(in));
				
		}

		/***************************************************************
		 * +----------- | FrameID // 4 bytes +-------------- | Frame Size // 4
		 * bytes +------------------ | Frame Flags // 2 bytes +-------------- |
		 * <Compressed Size> //4 bytes +------------------------------- |
		 * <Encrypted Symbol> // 1 byte +------------------------------- |
		 * <Grouping> // 1 byte +------------------------------- |***** Frame
		 * Body ***** || <variable bytes>
		 * +-------------------------------------------
		 ***************************************************************/

		/***************************************************************
		 * +---- 'USLT' frame body --- | Text Encoding // 1 byte
		 * +-------------------------- | Language // 3 bytes
		 * +---------------------------- | Content description // variable
		 * bytes; terminated by $00 or $00 00 +---------------------------- |
		 * Lyric text content +-------------------------------------
		 ****************************************************************/
		/***************************************************************
		 * +---- 'SYLT' frame body --- | Text Encoding // 1 byte
		 * +-------------------------- | Language // 3 bytes
		 * +---------------------------- | Timestamp Format // 1 byte
		 * +----------------------------- | Content description // variable
		 * bytes; terminated by $00 or $00 00 +---------------------------- |
		 * Lyric text content +-------------------------------------
		 ****************************************************************/
		private boolean readFrame(DataInputStream in) throws IOException,
				ID3TagException {
			final short GROUPING = 1 << 6;
			final short COMPRESSED = 1 << 3;
			final short ENCRYPTED = 1 << 2;
			final short UNSYNC = 1 << 1;
			final short DATA_LENGTH = 1;

			// [1] -- frame id -- 4 bytes
			byte[] frameName = new byte[] { in.readByte(), in.readByte(),
					in.readByte(), in.readByte() }; // FrameID -- 4bytes
			String name = new String(frameName);
			Log.i("ID3v2Tag", "name:" + name);

			if (frameName[0] == 0) { // padding
				return false;
			}

			if (name.startsWith("3DI")) // footer
			{
				return false;
			}

			// FIXME: debug only!!!
			// System.out.println("[DEBUG] FrameID: " + name);

			// [2] -- frame size -- 4 bytes
			int size;
			if (_majorVersion <= 3)
				size = in.readInt();
			else
				size = readSyncSafeInt(in);

			if (size > _size) { // frame size is larger than tag size
				// FIXME: debug only!!
				// throw new
				// ID3TagException("ERROR parsing ID3 Tag; frame size is larger than tag size!");

				/* a strategy: return null if encounter any error!! */
				return false;
			}

			// [3] -- frame flags -- 2 bytes
			short flags = in.readShort(); //

			if ((flags & DATA_LENGTH) != 0) {
				readSyncSafeInt(in); // ignore
				size -= 4;
			}

			if ((flags & GROUPING) != 0) {
				in.readByte();
				size--;
			}

			if (((flags & COMPRESSED) != 0) || ((flags & ENCRYPTED) != 0)
					|| ((flags & UNSYNC) != 0)) {
				in.skipBytes(size);
				return true;
			}

			String contents = null; // store frame content
			if (name.startsWith("T")) {
				String encoding = readEncoding(in);
				size--;
				contents = readStrings(in, size, encoding);
				Log.i("ID3v2Tag", "T:" + name + "__contents:" + contents+"_encoding"+encoding);
			} else if (name.equals("COMM") || name.equals("USLT")) {
				String encoding = readEncoding(in); // [1] encoding -- byte
				in.skipBytes(3); // [2] language -- 3 bytes
				size -= 4; // size of lyric content
				contents = readStrings(in, size, encoding); //

				if (name.equals("USLT")) {
					lyric_contents = contents;
					Log.i("ID3v2Tag", "USLT:" + name + "__contents:"+ contents+ contents+"_encoding"+encoding);
					return true; // break while(true) and return 'USLT' content;
					
				}
				Log.i("ID3v2Tag", "COMM or USLT:" + name + "__contents:"
						+ contents);
			} else if (name.equals("SYLT")) {
				sylt_lyric_content = getSYLTContent(in, size);
				Log.i("ID3v2Tag", "SYLT:" + name + "_sylt_lyric_content:"
						+ sylt_lyric_content);
				return true; // break while(true) and return 'SYLT' content;
			} else {
				in.skipBytes(size);
				Log.i("ID3v2Tag", "skipBytes:" + name + "__contents:"
						+ contents);
			}

			return true;
		} /* ends of readFrame() */


		private String getSYLTContent(DataInputStream in, int nBytes)
				throws IOException {
			String content = null;
			String _encoding = readEncoding(in); // 1 bytes
			in.skipBytes(3); // language -- 3 bytes
			Log.i("ID3v2Tag", "_encoding:" + _encoding );
			int _timestampFormat = in.readByte(); // 1 byte
			Log.i("ID3v2Tag", "_timestampFormat:" + _timestampFormat );
			int _contentType = in.readByte(); // 1 byte
			Log.i("ID3v2Tag", "_contentType:" + _contentType );
			int _content_size = nBytes - 6 > 0 ? nBytes - 6 : 0;
			
			if(_timestampFormat==2&&_contentType==1){
				content = readStrings(in, _content_size, _encoding);
				if(content!=null){
					return content;
				}
			}
			content = readStrings(in, _content_size, _encoding);
			return content;
		} /* ends of getSYLTContent() */

		private String readStrings(DataInputStream in, int nBytes,
				String encoding) throws IOException {
			int byteSize = nBytes > 0 ? nBytes : 0;
			byte[] bytes = new byte[byteSize];
			in.read(bytes); //

			int len = bytes.length;

			/*
			 * The string will be separated by null characters. In US-ASCII and
			 * UTF-8 this is one 0 byte. In UTF-16 and UTF-16BE, it is two
			 * bytes. So we will convert the null characters into a space, which
			 * basically concatenates the strings.
			 */
			// readStringsUntillNull(int); --- content descriptor

			if (encoding.equals("ISO-8859-1") || encoding.equals("UTF-8")) {
				for (int i = 0; i < len; ++i) {
					if (bytes[i] == 0)
						bytes[i] = 32;
				}
			} else if (encoding.equals("UTF-16") || encoding.equals("UTF-16BE")) {
				for (int i = 0; i < len; ++i) {
					if (bytes[i] == 0 && bytes[i + 1] == 0) {
						bytes[i] = 0;
						bytes[i + 1] = 32;
					}
				}
			}

			if (encoding.equals("ISO-8859-1"))
				return new String(bytes, "gbk"); /*
												 * 'gbk' is compatible with
												 * 'ISO-8859-1'
												 */
			else
				return new String(bytes, encoding); // UTF-16/UTF-16LE is ok to
													// chinese!!!
		} /* ends of readStrings() */

		private String readEncoding(DataInputStream in) throws IOException {
			byte encoding = in.readByte();
			if (encoding == 0)
				return "ISO-8859-1";
			else if (encoding == 1)
				return "UTF16";
			else if (encoding == 2)
				return "UTF-16BE";
			else
				return "UTF-8";
		} /* ends of readEncoding() */

		private int readSyncSafeInt(DataInputStream in) throws IOException {
			int size = 0;
			for (int i = 3; i >= 0; --i) {
				size |= (in.readByte() & 0x7f) << (7 * i);
			}

			return size;
		}

	}
}

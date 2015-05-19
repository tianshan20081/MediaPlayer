package org.farng.mp3.object;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * ID3v2 and Lyrics3v2 tags have individual fields
 * <code>AbstractMP3Fragment</code>s Then each fragment is broken down in to
 * individual <code>AbstractMP3Object</code>s
 * 
 * @author Eric Farng
 * @version $Revision: 1.4 $
 */
public abstract class AbstractMP3Object extends java.lang.Object {

	protected Object value = null;
	protected String identifier = "";

	/**
	 * Creates a new AbstractMP3Object object.
	 */
	public AbstractMP3Object() {
		this.value = null;
		this.identifier = "";
	}

	/**
	 * Creates a new AbstractMP3Object object.
	 */
	public AbstractMP3Object(final AbstractMP3Object copyObject) {
		// no copy constructor in super class
		this.identifier = new String(copyObject.identifier);
		if (copyObject.value == null) {
			this.value = null;
		} else if (copyObject.value instanceof String) {
			this.value = new String((String) copyObject.value);
		} else if (copyObject.value instanceof Boolean) {
			this.value = new Boolean(((Boolean) copyObject.value)
					.booleanValue());
		} else if (copyObject.value instanceof Byte) {
			this.value = new Byte(((Byte) copyObject.value).byteValue());
		} else if (copyObject.value instanceof Character) {
			this.value = new Character(((Character) copyObject.value)
					.charValue());
		} else if (copyObject.value instanceof Double) {
			this.value = new Double(((Double) copyObject.value).doubleValue());
		} else if (copyObject.value instanceof Float) {
			this.value = new Float(((Float) copyObject.value).floatValue());
		} else if (copyObject.value instanceof Integer) {
			this.value = new Integer(((Integer) copyObject.value).intValue());
		} else if (copyObject.value instanceof Long) {
			this.value = new Long(((Long) copyObject.value).longValue());
		} else if (copyObject.value instanceof Short) {
			this.value = new Short(((Short) copyObject.value).shortValue());
		} else if (copyObject.value instanceof boolean[]) {
			this.value = ((boolean[]) copyObject.value).clone();
		} else if (copyObject.value instanceof byte[]) {
			this.value = ((byte[]) copyObject.value).clone();
		} else if (copyObject.value instanceof char[]) {
			this.value = ((char[]) copyObject.value).clone();
		} else if (copyObject.value instanceof double[]) {
			this.value = ((double[]) copyObject.value).clone();
		} else if (copyObject.value instanceof float[]) {
			this.value = ((float[]) copyObject.value).clone();
		} else if (copyObject.value instanceof int[]) {
			this.value = ((int[]) copyObject.value).clone();
		} else if (copyObject.value instanceof long[]) {
			this.value = ((long[]) copyObject.value).clone();
		} else if (copyObject.value instanceof short[]) {
			this.value = ((short[]) copyObject.value).clone();
		} else if (copyObject.value instanceof Object[]) {
			this.value = ((Object[]) copyObject.value).clone();
		} else {
			throw new UnsupportedOperationException(
					"Unable to create copy of class " + copyObject.getClass());
		}
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

	public final void readByteArray(final byte[] arr) {
		readByteArray(arr, 0);
	}

	public final void readString(final String str) {
		readString(str, 0);
	}

	public abstract int getSize();

	/**
	 * Modify by yangguangfu
	 * 
	 * @param arr
	 * @param offset
	 */
	public void readByteArray(final byte[] arr, final int offset) {
		try {
			readString(new String(arr,"GBK"), offset);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String readEncoding(byte bt) {
		byte encoding = bt;
		if (encoding == 0)
			return "ISO-8859-1";
		else if (encoding == 1)
			return "UTF16";
		else if (encoding == 2)
			return "UTF-16BE";
		else
			return "UTF-8";
	} /* ends of readEncoding() */

	private String readStrings(final byte[] arr) throws IOException {
		// int byteSize = nBytes > 0 ? nBytes : 0;
		byte[] bytes = arr;
		// in.read(bytes); //
		// String ;
		String encoding = readEncoding(bytes[0]);
		int len = bytes.length;

		/*
		 * The string will be separated by null characters. In US-ASCII and
		 * UTF-8 this is one 0 byte. In UTF-16 and UTF-16BE, it is two bytes. So
		 * we will convert the null characters into a space, which basically
		 * concatenates the strings.
		 */
		// readStringsUntillNull(int); --- content descriptor

		if (encoding.equals("ISO-8859-1") || encoding.equals("UTF-8")) {
			for (int i = 0; i < len; ++i) {
				if (bytes[i] == 0)
					bytes[i] = 32;
			}
		} else if (encoding.equals("UTF-16") || encoding.equals("UTF16")
				|| encoding.equals("UTF-16BE")) {
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
	}

	public void readString(final String str, final int offset) {
		readByteArray(str.substring(offset).getBytes(), 0);
	}

	public abstract String toString();

	public boolean equals(final Object obj) {
		if ((obj instanceof AbstractMP3Object) == false) {
			return false;
		}
		final AbstractMP3Object abstractMp3Object = (AbstractMP3Object) obj;
		if (this.identifier.equals(abstractMp3Object.identifier) == false) {
			return false;
		}
		if ((this.value == null) && (abstractMp3Object.value == null)) {
			return true;
		} else if ((this.value == null) || (abstractMp3Object.value == null)) {
			return false;
		}

		// boolean[]
		if (this.value instanceof boolean[]
				&& abstractMp3Object.value instanceof boolean[]) {
			if (Arrays.equals((boolean[]) this.value,
					(boolean[]) abstractMp3Object.value) == false) {
				return false;
			}

			// byte[]
		} else if (this.value instanceof byte[]
				&& abstractMp3Object.value instanceof byte[]) {
			if (Arrays.equals((byte[]) this.value,
					(byte[]) abstractMp3Object.value) == false) {
				return false;
			}

			// char[]
		} else if (this.value instanceof char[]
				&& abstractMp3Object.value instanceof char[]) {
			if (Arrays.equals((char[]) this.value,
					(char[]) abstractMp3Object.value) == false) {
				return false;
			}

			// double[]
		} else if (this.value instanceof double[]
				&& abstractMp3Object.value instanceof double[]) {
			if (Arrays.equals((double[]) this.value,
					(double[]) abstractMp3Object.value) == false) {
				return false;
			}

			// float[]
		} else if (this.value instanceof float[]
				&& abstractMp3Object.value instanceof float[]) {
			if (Arrays.equals((float[]) this.value,
					(float[]) abstractMp3Object.value) == false) {
				return false;
			}

			// int[]
		} else if (this.value instanceof int[]
				&& abstractMp3Object.value instanceof int[]) {
			if (Arrays.equals((int[]) this.value,
					(int[]) abstractMp3Object.value) == false) {
				return false;
			}

			// long[]
		} else if (this.value instanceof long[]
				&& abstractMp3Object.value instanceof long[]) {
			if (Arrays.equals((long[]) this.value,
					(long[]) abstractMp3Object.value) == false) {
				return false;
			}

			// Object[]
		} else if (this.value instanceof Object[]
				&& abstractMp3Object.value instanceof Object[]) {
			if (Arrays.equals((Object[]) this.value,
					(Object[]) abstractMp3Object.value) == false) {
				return false;
			}

			// short[]
		} else if (this.value instanceof short[]
				&& abstractMp3Object.value instanceof short[]) {
			if (Arrays.equals((short[]) this.value,
					(short[]) abstractMp3Object.value) == false) {
				return false;
			}
		} else if (this.value.equals(abstractMp3Object.value) == false) {
			return false;
		}
		return true;
	}

	public byte[] writeByteArray() {
		return writeString().getBytes();
	}

	public String writeString() {
		return new String(writeByteArray());
	}
}
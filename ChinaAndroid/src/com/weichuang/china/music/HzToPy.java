package com.weichuang.china.music;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.android.china.R;

public class HzToPy {
	static private ArrayList<String> sortedHzList  = null;
	static private ArrayList<String> sortedPyList  = null;

	private static final String TAG = "HzToPy";
	public HzToPy(Context context) throws IOException {
		if (sortedHzList == null) {
			Log.d("","********** MyHz2Py(): create the hanzi and pinyin list *******");
			Resources res = context.getResources();
			InputStream hz = res.openRawResource(R.raw.sortedhz);
			InputStreamReader hzReader = new InputStreamReader(hz);
			InputStream py = res.openRawResource(R.raw.sortedpy);
			InputStreamReader pyReader = new InputStreamReader(py);
			BufferedReader brHz = new BufferedReader(hzReader);// frHz);
			BufferedReader brPy = new BufferedReader(pyReader);// frPy);

			String sLine;
			sortedHzList = new ArrayList<String>();
			while ((sLine = brHz.readLine()) != null) {
				sortedHzList.add(sLine);
			}
			sortedPyList = new ArrayList<String>();
			while ((sLine = brPy.readLine()) != null) {
				sortedPyList.add(sLine);
			}
		}
	}
	
	public String Char2Py(String sChar) throws UnsupportedEncodingException {
		if(sChar.getBytes("UTF-8").length == 1)
			return sChar.toLowerCase();
		
		String strch = null;
		int nIndex1 = 0;
		int nIndex2 = sortedHzList.size() - 1;
		int nIndex = 0;
				
	   while (nIndex1 <= nIndex2) {
			nIndex = (nIndex2 + nIndex1) / 2;
			int mResult = sortedHzList.get(nIndex).compareTo(sChar);
			if (mResult < 0)
				nIndex1 = nIndex + 1;
			else if (mResult > 0)
				nIndex2 = nIndex - 1;
			else {
				if(strch == null)
					strch = "";
            strch += sortedPyList.get(nIndex);
				break;
			}
		}

	   if(strch == null)
		   return "";
	   
		return strch;
	}
	
	public String Hz2Py(String value) throws UnsupportedEncodingException {		
		if(value.length() == value.getBytes("UTF-8").length)
			return value;
		String strch = new String();
		int strlength = value.length();
		for (int i = 0; i < strlength; i++) {
			String sChar = value.substring(i, i + 1);
			String tempstr = Char2Py(sChar);
			strch += tempstr;
		}
		return strch;
	}
	
	public static String UnknowntoUTF8(String input_string) {
		if ( input_string == null ) {
			return null;
		}
		char[] g_char;
		int index = 0;
		String ret_str = null;
		g_char = input_string.toCharArray();
		byte[] new_byte = new byte[2 * g_char.length];

		try {
			for (int i = 0; i < g_char.length; i++) {
				if ((g_char[i] & 0xff) == g_char[i])
					new_byte[index] = (byte) g_char[i];
				else {
					String temp_str = String.valueOf(g_char[i]);
					new_byte[index] = temp_str.getBytes("GBK")[0];
					index++;
					new_byte[index] = temp_str.getBytes("GBK")[1];
				}
				index++;
			}
			String new_string = new String(new_byte, 0, index, "GBK");
			byte[] temp_byte = new_string.getBytes("UTF-8");
			ret_str = new String(temp_byte, "UTF-8");
		} catch (Exception e) {
			Log.d(TAG, "UnknowntoUTF8()=============catch exception, return the original input string");
			return input_string;
		}
		return ret_str;
	}


}
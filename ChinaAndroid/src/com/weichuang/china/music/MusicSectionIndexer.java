/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weichuang.china.music;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;
import android.widget.SectionIndexer;


/**
 * A section indexer that is configured with precomputed section titles and
 * their respective counts.
 */
public class MusicSectionIndexer extends DataSetObserver implements SectionIndexer {

	private static final String TAG = "MusicSectionIndexer";
	private Cursor mDataCursor;
    private String[] mSections;
    private int[] mPositions;
    private int mCount;
//    private int mColumn;

    /**
     * Constructor.
     *
     * @param sections a non-null array
     * @param counts a non-null array of the same size as <code>sections</code>
     */
    public MusicSectionIndexer(Cursor cursor, int column) {
//    	mColumn = column;
    	mDataCursor = cursor;
    	initFromCursor(cursor);
    	if (cursor != null) {
            cursor.registerDataSetObserver(this);
        }
    }

    public void setCursor(Cursor cursor) {
        if (mDataCursor != null) {
            mDataCursor.unregisterDataSetObserver(this);
        }
        mDataCursor = cursor;
        if (cursor != null) {
            mDataCursor.registerDataSetObserver(this);
        }
        initFromCursor(cursor);
    }
    
    private String getTitle(Cursor cursor) {
    	String title = null;
    	if ( cursor != null ) {
    		title = cursor.getString(3);
    		try {
				title=new HzToPy(TrackBrowserActivity.self).Hz2Py(title);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//    		Log.d(TAG, "getTitle() ================title=" + title + " pos=" + cursor.getPosition());
    		if ( title == null || title.equals("") ) {
        		title = " ";
        	} else {
        		title = title.substring(0, 1).toUpperCase();
        	}
    		if ( !isAlphabet(title.charAt(0)) ) {
    			title = "#";
    		}
    	}
    	return title;
    }
    
    private boolean isAlphabet(char character) {
    	if ( (character >= 'a' && character <= 'z') || 
				(character >= 'A' && character <= 'Z')) {
			return true;
		}
    	return false;
    }
    
    private void initFromCursor(Cursor cursor) {
    	synchronized (this) {
			if (cursor == null) {
				throw new NullPointerException();
			}
			int savedCursorPos = cursor.getPosition();
			List<String> sections = new ArrayList<String>();
			List<Integer> positions = new ArrayList<Integer>();
			int size = cursor.getCount();
			mCount = size;
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String title = getTitle(cursor);
				positions.add(cursor.getPosition());
				sections.add(title);
				while (getTitle(cursor).equals(title)) {
					cursor.moveToNext();
					if (cursor.isAfterLast()) {
						break;
					}
				}
			}
			size = positions.size();
			mPositions = new int[size];
			for (int i = 0; i < size; i++) {
				mPositions[i] = positions.get(i);
			}
			size = sections.size();
			mSections = new String[size];
			for (int i = 0; i < size; i++) {
				mSections[i] = sections.get(i);
			}
			
			cursor.moveToPosition(savedCursorPos);
    	}
    }
    
    public Object[] getSections() {
        return mSections;
    }

    public int getPositionForSection(int section) {
    	synchronized (this) {
    		if (section < 0 || section >= mSections.length) {
                return -1;
            }

            return mPositions[section];
		}    
    }

    public int getSectionForPosition(int position) {
    	synchronized (this) {
    		if (position < 0 || position >= mCount) {
                return -1;
            }

            int index = Arrays.binarySearch(mPositions, position);

            /*
             * Consider this example: section positions are 0, 3, 5; the supplied
             * position is 4. The section corresponding to position 4 starts at
             * position 3, so the expected return value is 1. Binary search will not
             * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
             * To get from that number to the expected value of 1 we need to negate
             * and subtract 2.
             */
            return index >= 0 ? index : -index - 2;
		}      
    }
    
    @Override
    public void onChanged() {
        super.onChanged();
        Log.d(TAG, "onChanged()===================before");
        initFromCursor(mDataCursor);
        Log.d(TAG, "onChanged()===================after");
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
    }
}

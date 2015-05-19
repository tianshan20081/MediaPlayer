package com.weichuang.china.music;


import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;


public class AlphabetSortCursor extends AbstractCursor {	
    private static final String TAG = "AlphabetSortCursor";
    private Cursor mCursor; // updated in onMove
    private int mFirstAlphabetPos = -1;
    private int mLastAlphabetPos = -1;
    private int mSortColumn;

    private DataSetObserver mObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            // Reset our position so the optimizations in move-related code
            // don't screw us over
            mPos = -1;      
        }

        @Override
        public void onInvalidated() {          
        	mPos = -1;          
        }
    };

    public AlphabetSortCursor(Cursor cursor, String sortcolumn)
    {
        mCursor = cursor;
        if ( mCursor != null ) {
        	mSortColumn = mCursor.getColumnIndexOrThrow(sortcolumn);
        	mCursor.registerDataSetObserver(mObserver);
        }  
        initAlphabetPosition(mCursor);
        
    }

    @Override
	public void registerContentObserver(ContentObserver observer) {
		if ( mCursor != null ) {
			mCursor.registerContentObserver(observer);
		}
	}

	@Override
	public void unregisterContentObserver(ContentObserver observer) {
		if ( mCursor != null ) {
			mCursor.unregisterContentObserver(observer);
		}
	}

	public int getNonAlphabetPosition() {
    	int position = -1;
    	if ( mFirstAlphabetPos != -1 && mLastAlphabetPos != -1 ) {
    		position = mLastAlphabetPos - mFirstAlphabetPos + 1;
    	}
    	return position; 
    }
    
    private void initAlphabetPosition(Cursor cursor) {
    	if ( cursor == null || cursor.getCount() == 0 ) {
    		return;
    	}
    	int temppos = cursor.getPosition();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String title = cursor.getString(3);
			try {
				title=new HzToPy(TrackBrowserActivity.self).Hz2Py(title);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if ( title != null && !title.equals("") ) {
				char firstLetter = title.charAt(0);
				if ( (firstLetter >= 'a' && firstLetter <= 'z') || 
						(firstLetter >= 'A' && firstLetter <= 'Z')) {
					mFirstAlphabetPos = cursor.getPosition();
					break;
				}
			}
			cursor.moveToNext();
		}
		if ( cursor.isAfterLast() ) {
			//if we did NOT find any sort key begin with alphabet,
			//point both section index to the last cursor position
			cursor.moveToLast();
			mFirstAlphabetPos = cursor.getPosition();
			mLastAlphabetPos = cursor.getPosition();
		} else {
			//if we get the first section index, then find the last
			//section index
			cursor.moveToLast();
			while ( !cursor.isBeforeFirst() ) {
				int position = cursor.getPosition();
				if ( position <= mFirstAlphabetPos ) {
					mLastAlphabetPos = mFirstAlphabetPos;
					break;
				}
				String title = cursor.getString(3);
				try {
					title=new HzToPy(TrackBrowserActivity.self).Hz2Py(title);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if ( title != null && title != "") {
					char firstLetter = title.charAt(0);
					if ( (firstLetter >= 'a' && firstLetter <= 'z') || 
							(firstLetter >= 'A' && firstLetter <= 'Z')) {
						mLastAlphabetPos = position;
						break;
					}
				}
				cursor.moveToPrevious();
			}
			//here mSectionLast should always have value
		}
		cursor.moveToPosition(temppos);
	}
    
    private int convertDisplayPosToCursorPos(int displayPos) {
		int sectionCount = mLastAlphabetPos - mFirstAlphabetPos;
		int cursorPos = displayPos;
		if ( displayPos <= sectionCount ) {
			cursorPos = mFirstAlphabetPos + displayPos;
		} else if ( displayPos > sectionCount && displayPos <= mLastAlphabetPos ) {
			cursorPos = displayPos - (mLastAlphabetPos - mFirstAlphabetPos + 1);
		}
		return cursorPos;
	}
    
    @Override
    public int getCount()
    {
        int count = 0;
        if ( mCursor != null ) {
        	count += mCursor.getCount();
        }
        return count;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition)
    {
        if (oldPosition == newPosition)
            return true;

        if ( mCursor == null ) {
        	Log.w(TAG, "onMove: cache results in a null cursor.");
            return false;
        }
        int cursorPos = convertDisplayPosToCursorPos(newPosition);
//        Log.d(TAG, "onMove()=================mLastAlphabetPos=" + mLastAlphabetPos + " mFirstAlphabetPos=" + mFirstAlphabetPos + " cursorPos=" + cursorPos);
        mCursor.moveToPosition(cursorPos);
//        Log.d(TAG, "onMove() =============" + "old=" + oldPosition + " new=" + newPosition + "moveto  pos=" + mCursor.getPosition() + "title=" + mCursor.getString(mSortColumn));
        return true;
    }

    @Override
    public String getString(int column)
    {
//    	Log.d(TAG, "getString()===============>pos=" + mCursor.getPosition());
        return mCursor.getString(column);
    }

    @Override
    public short getShort(int column)
    {
        return mCursor.getShort(column);
    }

    @Override
    public int getInt(int column)
    {
        return mCursor.getInt(column);
    }

    @Override
    public long getLong(int column)
    {
        return mCursor.getLong(column);
    }

    @Override
    public float getFloat(int column)
    {
        return mCursor.getFloat(column);
    }

    @Override
    public double getDouble(int column)
    {
        return mCursor.getDouble(column);
    }

    @Override
    public boolean isNull(int column)
    {
        return mCursor.isNull(column);
    }

    @Override
    public byte[] getBlob(int column)
    {
        return mCursor.getBlob(column);
    }

    @Override
    public String[] getColumnNames()
    {
        if (mCursor != null) {
            return mCursor.getColumnNames();
        } else {
            throw new IllegalStateException("No cursor that can return names");
        }
    }

    @Override
    public void deactivate()
    {
    	if ( mCursor != null ) {
    		mCursor.deactivate();
    	}
    }

    @Override
    public void close() {
    	if ( mCursor != null ) {
    		mCursor.close();
    	}
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    	if ( mCursor != null ) {
    		mCursor.registerDataSetObserver(observer);
    	}
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    	if ( mCursor != null ) {
    		mCursor.unregisterDataSetObserver(observer);
    	}
    }
    

    @Override
    public boolean requery()
    {
    	if ( mCursor != null ) {
    		synchronized (mCursor) {
    			Log.d(TAG, "requery()=====================before");
    			mCursor.requery();
    			Log.d(TAG, "requery()=====================after");
        		initAlphabetPosition(mCursor);
			}		
//    		Log.d(TAG, "requery()+++++++++++++++============================pos=" + mCursor.getPosition());    		
    	}
    	return true;
    }
}

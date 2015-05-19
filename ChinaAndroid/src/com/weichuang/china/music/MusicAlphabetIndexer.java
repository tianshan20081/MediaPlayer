/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.database.Cursor;
import android.widget.AlphabetIndexer;

/**
 * Handles comparisons in a different way because the Album, Song and Artist name
 * are stripped of some prefixes such as "a", "an", "the" and some symbols.
 *
 */
class MusicAlphabetIndexer extends AlphabetIndexer {
    
    public MusicAlphabetIndexer(Cursor cursor, int sortedColumnIndex, CharSequence alphabet) {
        super(cursor, sortedColumnIndex, alphabet);
    }
    
    private boolean isAlphabet(char character) {
    	if ( (character >= 'a' && character <= 'z') || 
				(character >= 'A' && character <= 'Z')) {
			return true;
		}
    	return false;
    }
    
    @Override
    protected int compare(String word, String letter) {
    	String firstLetter = word.substring(0, 1);
    	//if the first letter is not an alphabet, return a
    	//positive value to indicate that it is greater than 
    	//the letter
    	if ( !isAlphabet(firstLetter.charAt(0)) ) {
    		return 100;
    	}
    	String wordKey = firstLetter.toLowerCase();
    	String letterKey = letter.toLowerCase();
    	
    	return wordKey.compareTo(letterKey);
    }
}

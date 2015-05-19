/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

/**
 * 
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {


    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int LONG_PRESS_DELAY = 1500;
	private static final String TAG = "MediaButtonIntentReceiver";

    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    
    private static final int DOUBLE_CLICK = 400;
    
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if(msg != null){
        		switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    Log.d(TAG , "it's now long press..we will play previous");
                    	 try{
                    		 Context context = (Context) msg.obj;
                             Intent i = new Intent(context, MediaPlaybackService.class);
                             i.setAction(MediaPlaybackService.SERVICECMD);
                             i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPREVIOUS);
                             context.startService(i);
                    	 }catch (Exception e) {
							e.printStackTrace();
						}
                    	
                   
                    break;
        		}
        	}
        	
            
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if ( !MediaPlaybackService.mHavePlayed ) {
        	return;
        }

        Log.d(TAG, "on receive >>> " + intentAction);
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
        	Intent i = new Intent(context, MediaPlaybackService.class);
          i.setAction(MediaPlaybackService.SERVICECMD);
          i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
          context.startService(i);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();


            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MediaPlaybackService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MediaPlaybackService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MediaPlaybackService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MediaPlaybackService.CMDPREVIOUS;
                    break;
                default:
                	Log.d(TAG, "Other key: " + keycode);
                	return;
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "command: " + command);
                    if (!mDown) {
                        // only if this isn't a repeat event
                        if (MediaPlaybackService.CMDTOGGLEPAUSE.equals(command)) {
                            // We're not using the original time of the event as the
                            // base here, because in some cases it can take more than
                            // one second for us to receive the event, in which case
                            // we would go immediately to auto shuffle mode, even if
                            // the user didn't long press.
                            mHandler.sendMessageDelayed(
                                    mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context),
                                    LONG_PRESS_DELAY);
                        }
                        Intent i = new Intent(context, MediaPlaybackService.class);
                      i.setAction(MediaPlaybackService.SERVICECMD);
                      Log.d(TAG, "before jude if it is double click, eventtime=" + eventtime + " mLastClickTime=" + mLastClickTime);
                        if ( KeyEvent.KEYCODE_HEADSETHOOK == keycode && eventtime - mLastClickTime < DOUBLE_CLICK ) {//single click
                            Log.d(TAG , "it's double click , so we will play next");
                            command = MediaPlaybackService.CMDNEXT;
                            mLastClickTime = 0;
                        } else {
                            mLastClickTime = eventtime;
                        }
                        Log.d(TAG, "is about to start service by action >>> " + command);
                        i.putExtra(MediaPlaybackService.CMDNAME, command);
                        context.startService(i);
                        mDown = true;
                    }
                } else {
                	Log.d(TAG, "mDown set to false , loose the button.");
                	mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
				if (isOrderedBroadcast()) {
					abortBroadcast();
				}
            }
        }
    }
}

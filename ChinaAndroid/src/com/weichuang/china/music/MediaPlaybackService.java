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

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Vector;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.android.china.R;



/**
 * Provides "background" audio playback capabilities, allowing the
 * user to switch between activities without stopping playback.
 */
public class MediaPlaybackService extends Service {
	private static final String TAG = "MediaPlaybackService";
    /** used to specify whether enqueue() should start playing
     * the new list of files right away, next or once all the currently
     * queued files have been played
     */
    public static final int NOW = 1;
    public static final int NEXT = 2;
    public static final int LAST = 3;
    public static final int PLAYBACKSERVICE_STATUS = 1;
    
    public static final int SHUFFLE_NONE = 0;
    public static final int SHUFFLE_NORMAL = 1;
//    public static final int SHUFFLE_AUTO = 2;
    
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_CURRENT = 1;
    public static final int REPEAT_ALL = 2;
    
    //added by liuqiang, indicate playlist style
    public static final int FROM_NONE = 0;
    public static final int FROM_SONGS = 1;
    public static final int FROM_PLAYLIST = 2;
    public static final int FROM_OTHERS = 3;
    //end add

    public static final String SINA_PAUSE_CMD = "com.weichuang.china.music.musicservicecommand.push";
    
    public static final String PLAYSTATE_CHANGED = "com.weichuang.china.music.playstatechanged";
    public static final String META_CHANGED = "com.weichuang.china.music.metachanged";
    public static final String QUEUE_CHANGED = "com.weichuang.china.music.queuechanged";
    public static final String PLAYBACK_COMPLETE = "com.weichuang.china.music.playbackcomplete";
    public static final String ASYNC_OPEN_COMPLETE = "com.weichuang.china.music.asyncopencomplete";
    public static final String ERROR_FILE_BROKEN = "com.weichuang.china.music.filebroken";

    public static final String SERVICECMD = "com.weichuang.china.music.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";

    public static final String TOGGLEPAUSE_ACTION = "com.weichuang.china.music.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION = "com.weichuang.china.music.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION = "com.weichuang.china.music.musicservicecommand.previous";
    public static final String NEXT_ACTION = "com.weichuang.china.music.musicservicecommand.next";

    public static final String BOOKMARK = "bookmark";
    public static final String IS_PODCAST = "is_podcast";
    
    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FADEIN = 4;
    private static final int MAX_HISTORY_SIZE = 100;
    
    //added for sina music temp
    public static final String WIDGET_PLAY = "com.weichuang.china.music.widget.music.play";
    public static final String WIDGET_PAUSE = "com.weichuang.china.music.widget.music.pause";
    //end add
    
    public static boolean mHavePlayed = false;
    private static boolean mUmounted = false;
    private LeMediaPlayer mPlayer;
    //added by liuqiang
    private LeMediaPlayer mOneShotPlayer;
    private MultipleMediaPlayer mMultiplePlayer;
    private int mShuffleMode = SHUFFLE_NONE;
    private int mRepeatMode = REPEAT_NONE;
    private int mMediaMountedCount = 0;
    private boolean mOneShot = false;
    //added for sina music widget temp
    private boolean mFromWidget = false;
    private float mVolume = -1.0f;
    //added by liuqiang
    private int mCurrentPlaylistStyle;
    //end add
    private boolean mIsMute = false;
    private static final String LOGTAG = "MediaPlaybackService";
    String[] mCursorCols = new String[] {
            "audio._id AS _id",             // index must match IDCOLIDX below
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            IS_PODCAST,
            BOOKMARK
    };
    private final static int IDCOLIDX = 0;
    private final static int PODCASTCOLIDX = 8;
    private final static int BOOKMARKCOLIDX = 9;
    private BroadcastReceiver mUnmountReceiver = null;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mPausedByTransientLossOfFocus = false;

    private SharedPreferences mPreferences;
    private static final int IDLE_DELAY = 60000;
    private QueryHandler mQueryHandler = null;
    private Cursor mMonitorCursor;
    class QueryHandler extends AsyncQueryHandler {
		QueryHandler(ContentResolver res) {
			super(res);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie,
				Cursor cursor) {
			CookieWrapper cw  = (CookieWrapper)cookie;
			if ( cw.mAlphabetSorted ) {
				AlphabetSortCursor sortCursor = new AlphabetSortCursor(cursor, cw.mSortKey);
				initMonitorCursor(sortCursor);
			} else {
				initMonitorCursor(cursor);
			}
		}
	}
    
    private ContentObserver mMonitorCursorContentObserver = new ContentObserver (new Handler()) {
		public void onChange(boolean selfChange) {
			mLastRequeryTime = System.currentTimeMillis();
			mRequeryHandler.sendEmptyMessageDelayed(REQUERY, REQUERY_INTERVAL);
		}
	};
	
	private static final int REQUERY = 1;
	private long mLastRequeryTime = 0;
	private static final long REQUERY_INTERVAL = 1000;
	private Handler mRequeryHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == REQUERY ) {
				long time = System.currentTimeMillis();
				if ( time - mLastRequeryTime < REQUERY_INTERVAL ) {
					return;
				}
				doMonitorRequery();
			} 
		}
	};
    
	private void doMonitorRequery() {
		new Thread(
	    	   	new Runnable(){
	    			public void run() {
	    				synchronized (mMonitorCursor) {
		    				if ( mMonitorCursor != null ) {
		    					Log.d(TAG, "onChange()============>cursor content observer, requery()");
		    					mMonitorCursor.requery();
		    					Log.d(TAG, "onChange()============>cursor content observer, requery(), count=" + mMonitorCursor.getCount());
		    					if ( mMultiplePlayer != null && !mUmounted) {
		    						mMultiplePlayer.syncPlaylistWithCursor(mMonitorCursor);
		    					}
		    					Log.d(TAG, "onChange()============>cursor content observer, after sync monitor cursor");
		    				}
						}		
	    			}
	        	}).start();
	}
	
	private void initMonitorCursor(Cursor cursor) {
    	if ( mMonitorCursor != null ) {
    		mMonitorCursor.unregisterContentObserver(mMonitorCursorContentObserver);
    		if ( !mMonitorCursor.isClosed()) {
    			mMonitorCursor.close();
    		}
    	}
    	mMonitorCursor = cursor;
    	if ( mMonitorCursor != null ) {
    		mMonitorCursor.registerContentObserver(mMonitorCursorContentObserver);
    	}
    	
    }
	
    class CookieWrapper {
    	public boolean mAlphabetSorted;
    	public String mSortKey;
    }
    
    private void registerMonitorCursor(String uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, String sortKey, boolean alphabetSorted) {
    	Uri queryUri = Uri.parse(uri);
    	CookieWrapper cw = new CookieWrapper();
    	cw.mAlphabetSorted = alphabetSorted;
    	cw.mSortKey = sortKey;
    	//TODO: use a thread here? or use a handler?
    	mQueryHandler.startQuery(0, cw, queryUri, projection, selection, selectionArgs, sortOrder);
    }
    
    private Handler mMediaplayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;
        @Override
        public void handleMessage(Message msg) {
            MusicUtils.debugLog("mMediaplayerHandler.handleMessage " + msg.what);
            switch (msg.what) {
                case FADEIN:
                    if (!isPlaying()) {
                        mCurrentVolume = 0f;
                        mPlayer.setVolume(mCurrentVolume);
                        play();
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        mPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case SERVER_DIED:
                	Log.d(TAG, "mMediaPlayerHandler->handlerMessage=======================>mIsSupporsedToBePlaying");
                    if (mIsSupposedToBePlaying) {
                        next(true);
                    } else {
                    	mMultiplePlayer.restoreOpen();
                    }
                    break;
                case TRACK_ENDED:
                		if ( mOneShot ) {
                			Log.d(TAG, "one shot mode=====receive TRACK_END, path=" + mPlayer.getPath());
                			Log.d(TAG, "one shot mode=====receive TRACK_END, mRepeatMode=" + mRepeatMode + " mShuffleMode=" + mShuffleMode);
                			boolean from_web = false;
                			if ( mPlayer != null ) {
                				String path = mPlayer.getPath();
                				if ( path != null && path.startsWith("http://") )
                					from_web = true;
                			}
                			if ( (mRepeatMode == REPEAT_CURRENT || mRepeatMode == REPEAT_ALL || 
                					mShuffleMode == SHUFFLE_NORMAL) ) {
                				Log.d(TAG, "one shot mode=====receive TRACK_END, repeat play");
//                				mIsSupposedToBePlaying = false;
                				if ( from_web ) {
                					mPlayer.openAsync(mPlayer.getPath());
                				} else {
                					mPlayer.open(mPlayer.getPath(), mOneShot);
//	                				seek(0);
	                    			play();
                				}
                			} else {
                				Log.d(TAG, "one shot mode=====receive TRACK_END, play complete");
                				mIsSupposedToBePlaying = false;
                				notifyChange(PLAYBACK_COMPLETE);              				
                			}
                			
                		} else {
                			Log.d(TAG, "multiple mode=====receive TRACK_END, path=" + mPlayer.getPath());
                			if (mRepeatMode == REPEAT_CURRENT) {
                				if ( mPlayer != null ) {
                					mPlayer.open(mPlayer.getPath(), mOneShot);
//                            seek(0);
                					play();
                				}
                        } else {
                        		next(false);
                        	}
                        }
//                    if (mRepeatMode == REPEAT_CURRENT) {
//                        seek(0);
//                        play();
//                    } else if (!mOneShot) {
//                        next(false);
//                    } else {
//                        notifyChange(PLAYBACK_COMPLETE);
//                        mIsSupposedToBePlaying = false;
//                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            MusicUtils.debugLog("mIntentReceiver.onReceive " + action + " / " + cmd);
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if ( ERROR_FILE_BROKEN.equals(action) ) {
            	if ( mPlayer != null ) {
            		mPlayer.handlePlayError(ERROR_FILE_BROKEN);
            	}
            }
            //added for sina music temp
            else if (action.equals(ASYNC_OPEN_COMPLETE)) {
            	if ( mFromWidget ) {
            		play();
            	} else {
            		if ( mPlayer != null ) {
            			String path = mPlayer.getPath();
            			if ( path != null && path.startsWith("http://") ) {
            				play();
            			}
            		}
            	}
            }
            //end add
        }
    };

   /* private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on purpose
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                    if(isPlaying()) {
                        mPausedByTransientLossOfFocus = false;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                    if(isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                    if(!isPlaying() && mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        startAndFadeIn();
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown audio focus change code");
            }
        }
    };
*/
    public MediaPlaybackService() {
    }

    @Override
    public void onCreate() {
    	Log.d(TAG, "MediaPlaybackService ================onCreate()");
        super.onCreate();
//        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       /* mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName()));*/
        
        mPreferences = getSharedPreferences("Music", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        //mCardId = MusicUtils.getCardId(this);
        mQueryHandler = new QueryHandler(getContentResolver());
        stopForeground(true);
        registerExternalStorageListener();

        // Needs to be done in this thread, since otherwise ApplicationContext.getPowerManager() crashes.
        // modified by liuqiang
        mMultiplePlayer = new MultipleMediaPlayer(this);
        mOneShotPlayer = new OneShotMediaPlayer(this);
        switchPlayerMode(mOneShot);

        if ( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED )) {
        	reloadQueue();  
        }
        
        
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(ERROR_FILE_BROKEN);
        commandFilter.addAction(ASYNC_OPEN_COMPLETE);
        registerReceiver(mIntentReceiver, commandFilter);
        
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    @Override
    public void onDestroy() {
    	Log.i("onDestroy", "ohoh, I am going to be destroyed");
        // Check that we're not being destroyed while something is still playing.
        if (isPlaying()) {
            Log.e(TAG, "Service being destroyed while still playing.");
        }
        // release all MediaPlayer resources, including the native player and wakelocks
        // modified by liuqiang
        stopForeground(true);
        mPlayer = null;
        mMultiplePlayer.release();
        mMultiplePlayer = null;
        mOneShotPlayer.release();
        mOneShotPlayer = null;
//        mPlayer.release();
//        mPlayer = null;
        // end modification

//        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        //modified by liuqiang
//        if ( mNormalCursor != null ) {
//        	mNormalCursor.close();
//        	mNormalCursor = null;
//        }
//        if ( mOneShotCursor != null ) {
//        	mOneShotCursor.close();
//        	mOneShotCursor = null;
//        }
//        mCursor = null;
//        if (mCursor != null) {
//            mCursor.close();
//            mCursor = null;
//        }
        //end modification

        if ( mMonitorCursor != null ) {
    		mMonitorCursor.unregisterContentObserver(mMonitorCursorContentObserver);
    		if ( !mMonitorCursor.isClosed()) {
    			mMonitorCursor.close();
    		}
    	}
        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        mWakeLock.release();
        super.onDestroy();
    }
    
    
    
    private void switchPlayerMode(boolean oneshot) {
    	synchronized (this) {
	    	if ( mOneShot == oneshot && mPlayer != null )
	    		return;
	    	if ( isPlaying() ) {
	    		pause();
	    	}
	    	if ( !oneshot ) {
	    		mMultiplePlayer.setHandler(mMediaplayerHandler);
	    		mOneShotPlayer.setHandler(null);
	    		mPlayer = mMultiplePlayer;
	    	} else {
	    		mOneShotPlayer.setHandler(mMediaplayerHandler);
	    		mMultiplePlayer.setHandler(null);
	    		mPlayer = mOneShotPlayer;
	    	}
	    	mOneShot = oneshot;
    	}
    }
    
//    private void initMonitorCursor(boolean reload) {
//    	Log.d(TAG, "initMonitorCursor()==================>reload=" + reload);
//    	mPlaylistCursor = MusicUtils.getPlaylistCursor(this, reload);
//    	mSongsCursor = MusicUtils.getSongsCursor(this, reload);
//    	if ( mCurrentPlaylistStyle == FROM_SONGS ) {
//    		Log.d(TAG, "initMonitorCursor()==================>init song cursor monitor");
//    		mCurrentMonitorCursor = mSongsCursor;
////    		mMultiplePlayer.syncPlaylistWithCursor();
////    		mMultiplePlayer.registerMonitorDataSetObserver();
//    	} else if ( mCurrentPlaylistStyle == FROM_PLAYLIST ) {
//    		Log.d(TAG, "initMonitorCursor()==================>init playlist cursor monitor");
//    		mCurrentMonitorCursor = mPlaylistCursor;
////    		mMultiplePlayer.syncPlaylistWithCursor();
////    		mMultiplePlayer.registerMonitorDataSetObserver();
//    	}
//    }

    private void saveQueue(boolean full) {
    	//should always do from multiple player
    	if ( mMultiplePlayer != null ) {
    		mMultiplePlayer.saveQueue(full);
    	}
        //Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void reloadQueue() {
    	//should always do from multiple player
    	if ( mMultiplePlayer != null ) {
    		mMultiplePlayer.reloadQueue();
    	}
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {   	
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            //added for sina music widget temp
            boolean fromWidget = intent.getBooleanExtra("fromwidget", false);
            String path = intent.getStringExtra("path");
            if ( path != null ) {
	            if ( fromWidget && path.startsWith("http://") ) {
	            	if ( !mOneShot ) {
	            		switchPlayerMode(true);
	            	}
	            	if ( !path.equals(getPath()) ) {
	            		openAsync(path);
	            		mFromWidget = true;
	            		return START_STICKY;
	            	}
	            }
            }
            //end add
            MusicUtils.debugLog("onStartCommand " + action + " / " + cmd);

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
            	//modified by liuqiang, just directly play previous song
            	prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            }
        }
        
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return START_STICKY;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	Log.i("onUnbind", "ahah, Iam here");
        mServiceInUse = false;

        // Take a snapshot of the current playlist
        saveQueue(true);

        if (isPlaying() || mPausedByTransientLossOfFocus) {
            // something is currently playing, or will be playing once 
            // an in-progress call ends, so don't stop the service now.
            return true;
        }
        
        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
        //TODO:
//        if (mPlayListLen > 0  || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
        if (mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }
        
        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }
    
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || mPausedByTransientLossOfFocus || mServiceInUse
                    || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            Log.d(TAG, "mDelayedStopHandler->handleMessage()==================");
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    };
    
    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
    	//TODO:not only stop the current play
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
     * The intent will call closeExternalStorageFiles() if the external media
     * is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    	Log.d(TAG, "mUnmountReceiver->onReceive()=============action=" + action);
                    		mUmounted = true;
                    		initMonitorCursor(null);
                        saveQueue(true);
                        //TODO:
//                        mOneShot = true; // This makes us not save the state again later,
                                         // which would be wrong because the song ids and
                                         // card id might not match. 
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if ( action.equals(Intent.ACTION_MEDIA_REMOVED) ||
                    		action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    	Log.d(TAG, "mUnmountReceiver->onReceive()=============action=" + action);
                    		mUmounted = true;
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
							if (mUmounted) {
								mUmounted = false;
								mMediaMountedCount++;
								//mCardId = MusicUtils.getCardId(MediaPlaybackService.this);
								reloadQueue();
								notifyChange(QUEUE_CHANGED);
								notifyChange(META_CHANGED);
							}
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
            iFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.android.music.metachanged"
     * "com.android.music.queuechanged",
     * "com.android.music.playbackcomplete"
     * "com.android.music.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {
        
        Intent i = new Intent(what);
//        i.putExtra("id", Long.valueOf(getAudioId()));
//        i.putExtra("artist", getArtistName());
//        i.putExtra("album",getAlbumName());
//        i.putExtra("track", getTrackName());
        sendBroadcast(i);
        
        if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        
    }
    
    public void broadcastIntent(Intent i) {
    	sendBroadcast(i);
    }
    
    /**
     * Appends a list of tracks to the current playlist.
     * If nothing is playing currently, playback will be started at
     * the first track.
     * If the action is NOW, playback will switch to the first of
     * the new tracks immediately.
     * @param list The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueue(long [] list, int action) {
        synchronized(this) {
        	//the called should want to play in multiple player mode
        	switchPlayerMode(false);
        	mPlayer.enqueue(list, action);
        }
    }
    
    public void playCursor(Cursor cursor, int position) {
    	synchronized (this) {
        	//the called should want to play in multiple player mode
        	switchPlayerMode(false);
        	mPlayer.playCursor(cursor, position);
        }
    }

    /**
     * Replaces the current playlist with a new list,
     * and prepares for starting playback at the specified
     * position in the list, or a random position if the
     * specified position is 0.
     * @param list The new list of tracks.
     */
    public void open(long [] list, int position) {
        synchronized (this) {
        	//the called should want to play in multiple player mode
        	switchPlayerMode(false);
        	mPlayer.open(list, position);
        }
    }
    
    /**
     * Moves the item at index1 to index2.
     * @param index1
     * @param index2
     */
    public void moveQueueItem(int index1, int index2) {
        synchronized (this) {
        	//should always do in multiple player mode
        	mMultiplePlayer.moveQueueItem(index1, index2);
        }
    }

    /**
     * Returns the current play list
     * @return An array of integers containing the IDs of the tracks in the play list
     */
    public long [] getQueue() {
        synchronized (this) {
        	//should always return from multiple player mode
        	return mMultiplePlayer.getQueue();
        }
    }

    public void openAsync(String path) {
        synchronized (this) {
            //added for sina music temp
            mFromWidget = false;
            //end add
            if (path == null) {
                return;
            }
            switchPlayerMode(true);
        	mPlayer.openAsync(path);
        }
    }
    
    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     * @param oneshot when set to true, playback will stop after this file completes, instead
     * of moving on to the next track in the list 
     */
    public void open(String path, boolean oneshot) {
        synchronized (this) {
            //added for sina music temp
            mFromWidget = false;
            //end add
            if (path == null) {
                return;
            }
            switchPlayerMode(oneshot);
            mPlayer.open(path, oneshot);
        }
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
//        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
//                AudioManager.AUDIOFOCUS_GAIN);
//        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(),
//                MediaButtonIntentReceiver.class.getName()));

        if (mPlayer.isInitialized()) {
            Intent sina_intent = new Intent(SINA_PAUSE_CMD);
            broadcastIntent(sina_intent);
            mPlayer.start();
            mHavePlayed = true;

            Log.d(TAG, "play()================>show notification playing");
            int icon = R.drawable.music_detail_playing;
            String trackName = MusicUtils.getSongName(this, getTrackName());
            CharSequence tickerText = String.format(getResources().
            		getString(R.string.notification_music_playing_tip), trackName);
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon, tickerText, when);
            notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
            CharSequence contentTitle = getResources().getString(R.string.now_playing);
            CharSequence contentText = tickerText;
            // 
            Intent notificationIntent = new Intent(this, NowPlayingActivity.class);
            notificationIntent.putExtra("from_notification", true);
//            notificationIntent.putExtra(MainActivity.FORWARD_TO_NOWPLAYING, true);
            //
            PendingIntent contentIntent = PendingIntent.getActivity(this,
            		0, notificationIntent, 0);

            notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
            startForeground(PLAYBACKSERVICE_STATUS, notification);

            if (!mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = true;
                notifyChange(PLAYSTATE_CHANGED);
            }
          //added for sina music temp
            if ( mOneShot && mFromWidget ) {
            	Intent intent = new Intent(WIDGET_PLAY);
            	sendBroadcast(intent);
            }
            //end add

        }  else if (!mOneShot && !mMultiplePlayer.hasPlaylist()) {
        	//TODO: how to deal here
        	Log.d(TAG, "MediaPlaybackService=====>play()========set auto shuffle");
			// This is mostly so that if you press 'play' on a bluetooth headset
			// without every having played anything before, it will still play
			// something.
        	mMultiplePlayer.shuffleAuto();
		}
        //TODO:
    }
    
    public void stop(boolean remove_status_icon) {
    	Log.d(TAG, "MediaPlaybackService==>stop()========remove_status_icon=" + remove_status_icon);
    	if ( mPlayer != null ) {
    		mPlayer.stop();
    	}
        if (remove_status_icon) {
            gotoIdleState();
        } else {
        	//modified to true
            stopForeground(true);
        }
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
        }
        // added by liuqiang
        if ( mOneShot ) {
        	switchPlayerMode(false);
        }
        // end add
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized(this) {
            if (isPlaying()) {
                mPlayer.pause();
                gotoIdleState();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
                if ( mOneShot && mFromWidget ) {
                	Intent intent = new Intent(WIDGET_PAUSE);
                	sendBroadcast(intent);
                }
            }
        }
    }

    /** Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    public void prev() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		mPlayer.prev();
        	}
        }
    }

    public void next(boolean force) {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		mPlayer.next(force);
        	}
        }
    }
    
    public void shuffleAuto() {
    	//should always return from multiple player mode
    	mMultiplePlayer.shuffleAuto();
    }
    
    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(true);
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;
        private Random mRandom = new Random();
        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    };
    
    /**
     * Removes the range of tracks specified from the play list. If a file within the range is
     * the file currently being played, playback will move to the next file after the
     * range. 
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(int first, int last) {
    	//should always return from multiple player mode
    	return mMultiplePlayer.removeTracks(first, last);
    }
    
    /**
     * Removes all instances of the track with the given id
     * from the playlist.
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(long id) {
    	//should always return from multiple player mode
    	return mMultiplePlayer.removeTrack(id);
    }
    
    public void setShuffleMode(int shufflemode) {
        synchronized(this) {
        	if ( mPlayer != null ) {
        		mPlayer.setShuffleMode(shufflemode);
        	}
        }
    }
    public int getShuffleMode() {
        return mShuffleMode;
    }
    
    public void setRepeatMode(int repeatmode) {
        synchronized(this) {
        	//should do the same thing in either mode     	
            mRepeatMode = repeatmode;
            saveQueue(false);
        }
    }
    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if
     * no file is currently playing.
     */
    public String getPath() {
    	if ( mPlayer != null ) {
    		return mPlayer.getPath();
    	}
    	return null;
    }
    
    /**
     * Returns the rowid of the currently playing file, or -1 if
     * no file is currently playing.
     */
    public long getAudioId() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getAudioId();
        	}
        }
        return -1;
    }
    
    /**
     * Returns the position in the queue 
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized(this) {
        	//there is current play list only in multiple player mode,
        	//directly return from multiple player mode here
        	return mMultiplePlayer.getQueuePosition();
        }
    }
    
    /**
     * Starts playing the track at the given position in the queue.
     * and this should only be called if want to play in multiple player mode
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized(this) {
        	switchPlayerMode(false);
        	mPlayer.setQueuePosition(pos);
        }
    }

    public String getArtistName() {
        synchronized(this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getArtistName();
        	}
        	return null;
        }
    }
    
    public long getArtistId() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getArtistId();
        	}
        	return -1;
        }
    }

    public String getAlbumName() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getAlbumName();
        	}
        	return null;
        }
    }

    public long getAlbumId() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getAlbumId();
        	}
        	return -1;
        }
    }

    public String getTrackName() {
        synchronized (this) {
        	if ( mPlayer != null ) {
        		return mPlayer.getTrackName();
        	}
        	return null;
        }
    }

//    private boolean isPodcast() {
//        synchronized (this) {
//        	//added by liuqiang
//        	switchCurrentCursor(mOneShot);
//        	//end add
//            if (mCursor == null) {
//                return false;
//            }
//            return (mCursor.getInt(PODCASTCOLIDX) > 0);
//        }
//    }
//    
//    private long getBookmark() {
//        synchronized (this) {
//        	//added by liuqiang
//        	switchCurrentCursor(mOneShot);
//        	//end add
//            if (mCursor == null) {
//                return 0;
//            }
//            return mCursor.getLong(BOOKMARKCOLIDX);
//        }
//    }
    
    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public long duration() {
    	if ( mPlayer != null ) {
    		return mPlayer.duration();
    	}
    	return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
    	if ( mPlayer != null ) {
    		return mPlayer.position();
    	}
    	return -1;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
    	if ( mPlayer != null ) {
    		return mPlayer.seek(pos);
    	}
    	return -1;
    }
    
    /**
     * Set volume to specific value.
     *
     * @param vol The volume to set to, from 0.0 to 1.0
     */
    public void setVolume(float vol) {
    	float volume = vol < 0 ? 0.0f : vol;
    	if ( mPlayer != null && !mIsMute ) {   
    		mPlayer.setVolume(volume);
    	}
        mVolume = volume;
    }
    
    /**
     * Get the volume. If the volume is not initialized,
     * just return 0.0f
     * 
     */
    public float getVolume() {
    	return mVolume < 0 ? 0.0f : mVolume;
    }
    
    public void mute(boolean mute) {
    	mIsMute = mute;
    	if ( mPlayer != null ) {
	    	if ( mIsMute ) {
	    		if ( mPlayer.isInitialized() )
	    			mPlayer.setVolume(0.0f);
	    	} else {
	    		if ( mPlayer.isInitialized() )
	    			mPlayer.setVolume(mVolume < 0 ? 0.0f : mVolume);
	    	}
    	}
    }
    public boolean isLastTrack() {
    	if ( mPlayer != null ) {
    		return mPlayer.isLastTrack();
    	}
    	return true;
    }
    public boolean isFirstTrack() {
    	if ( mPlayer != null ) {
    		return mPlayer.isFirstTrack();
    	}
    	return true;
    }
    public boolean isMute() {
    	return mIsMute;
    }
    
    public void setCurrentPlaylistStyle(int style)
    {
    	if ( mCurrentPlaylistStyle == style ) {
    		return;
    	}
    	if ( style == FROM_PLAYLIST ) {
    		mCurrentPlaylistStyle = style;
    	} else if ( style == FROM_SONGS ) {
    		mCurrentPlaylistStyle = style;
    	} else {
    		mCurrentPlaylistStyle = FROM_NONE;
    	}   	
    }
    
    public int getCurrentPlaylistStyle()
    {
    	if ( mOneShot ) {
    		return FROM_NONE;
    	}
    	return mCurrentPlaylistStyle;
    }
    
    public void acquireWakelock() {
    	if ( mWakeLock != null ) {
    		mWakeLock.acquire();
    	}
    }
    
    public void releaseWakelock() {
    	if ( mWakeLock != null && mWakeLock.isHeld() ) {
    		mWakeLock.release();
    	}
    }

    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class MultiPlayer {
        private MediaPlayer mMediaPlayer = new MediaPlayer();
        private Handler mHandler;
        // added by liuqiang
        private String mFileToPlay;
        // end add
        private boolean mIsInitialized = false;

        public MultiPlayer() {
            mMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSourceAsync(String path) {
            try {
                mMediaPlayer.reset();
                mIsInitialized = false;
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(preparedlistener);
                mMediaPlayer.prepareAsync();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (Exception ex) {
            	mIsInitialized = false;
            	return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
        }
        
        public void setDataSource(String path) {
            try {
                mMediaPlayer.reset();
                mIsInitialized = false;
                mMediaPlayer.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    mMediaPlayer.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                	  FileInputStream ins = new FileInputStream(path);
                	  mMediaPlayer.setDataSource(ins.getFD());
                }
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
				  Intent i = new Intent(MediaPlaybackService.ERROR_FILE_BROKEN);
				  broadcastIntent(i);
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
            	  Intent i = new Intent(MediaPlaybackService.ERROR_FILE_BROKEN);
				  broadcastIntent(i);
                mIsInitialized = false;
                return;
            } catch (IllegalStateException e) {
            	  Intent i = new Intent(MediaPlaybackService.ERROR_FILE_BROKEN);
            	  broadcastIntent(i);
            	  mIsInitialized = false;
                return;
            } catch (Exception e) {
	            Intent i = new Intent(MediaPlaybackService.ERROR_FILE_BROKEN);
	          	  broadcastIntent(i);
	          	  mIsInitialized = false;
              return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
        }
        
        // added by liuqiang
        public void setFileToPlay(String path) {
        	mFileToPlay = path;
        }
        
        public String getFileToPlay() {
        	return mFileToPlay;
        }
        // end add
        
        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
            MusicUtils.debugLog(new Exception("MultiPlayer.start called"));
            //acquire wake lock, added by liuqiang
            acquireWakelock();
            //end add
            mMediaPlayer.start();
        }

        public void stop() {
            mMediaPlayer.reset();
            mIsInitialized = false;
            //release wake lock, added by liuqiang
            releaseWakelock();
            //end add
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mMediaPlayer.release();
        }
        
        public void pause() {
            mMediaPlayer.pause();
            //release wake lock, added by liuqiang
            releaseWakelock();
            //end add
        }
        
        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
            	Log.d(TAG, "onCompletion()====================");
                mWakeLock.acquire(30000);
                if ( mHandler != null ) {
                	Log.d(TAG, "onCompletion()====================send track end message");
	                mHandler.sendEmptyMessage(TRACK_ENDED);
	                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
            	//this is only for async set data source, after set this flag, everything is ready
            	mIsInitialized = true;
                notifyChange(ASYNC_OPEN_COMPLETE);
            }
        };
 
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
            	Log.d(TAG, "onError()=======================>what=" + what);
                switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    mIsInitialized = false;
                    mMediaPlayer.release();
                    // Creating a new MediaPlayer and settings its wakemode does not
                    // require the media service, so it's OK to do this now, while the
                    // service is still being restarted
                    mMediaPlayer = new MediaPlayer(); 
                    mMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    if ( mHandler != null ) {
                    	mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                    }
                    return true;
                default:
                    Log.d("MultiPlayer", "Error: " + what + "," + extra);
                    break;
                }
                return false;
           }
        };

        public long duration() {
            return mMediaPlayer.getDuration();
        }

        public long position() {
            return mMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mMediaPlayer.setVolume(vol, vol);
        }
    }
    
    
    public class LeMediaPlayer {
    	protected Context mContext;	
        protected MultiPlayer mPlayer;
        protected Cursor mCursor;
        protected int mOpenFailedCounter = 0;
	    protected boolean mQuietMode = false;

        public LeMediaPlayer(Context context) {
        	mContext = context;
        	mPlayer = new MultiPlayer();
        }
        
        public void release() {
        	if ( mPlayer != null ) {
        		mPlayer.release();
        		mPlayer = null;
        	}
        	if ( mCursor != null ) {
        		mCursor.close();
        		mCursor = null;
        	}
        }

        public void handlePlayError(String err) {
        	
        }		
        
        public void saveQueue(boolean full) {
        }

        public void reloadQueue() {
        }
        
        /**
         * Appends a list of tracks to the current playlist.
         * If nothing is playing currently, playback will be started at
         * the first track.
         * If the action is NOW, playback will switch to the first of
         * the new tracks immediately.
         * @param list The list of tracks to append.
         * @param action NOW, NEXT or LAST
         */
        public void enqueue(long [] list, int action) {
        }
        
        public void playCursor(Cursor cursor, int position) {
        }

        /**
         * Replaces the current playlist with a new list,
         * and prepares for starting playback at the specified
         * position in the list, or a random position if the
         * specified position is 0.
         * @param list The new list of tracks.
         */
        public void open(long [] list, int position) {
        }
        
        /**
         * Moves the item at index1 to index2.
         * @param index1
         * @param index2
         */
        public void moveQueueItem(int index1, int index2) {
        }

        /**
         * Returns the current play list
         * @return An array of integers containing the IDs of the tracks in the play list
         */
        public long [] getQueue() {
        	return new long [0];
        }

        public void openAsync(String path) {
        }
        
        /**
         * Opens the specified file and readies it for playback.
         *
         * @param path The full path of the file to be opened.
         * @param oneshot when set to true, playback will stop after this file completes, instead
         * of moving on to the next track in the list 
         */
        public void open(String path, boolean oneshot) {
        }
        
        public void start() {
        	mPlayer.start();
        }

        public boolean isInitialized() {
        	return mPlayer.isInitialized();
        }
        
        public void pause() {
        	mPlayer.pause();
        }

        public void stop() {
        	Log.d(TAG, "LeMediaPlayer=====>stop()============");
	        if (mPlayer.isInitialized()) {
	            mPlayer.stop();
	        }
	        mPlayer.setFileToPlay(null);
	        if (mCursor != null) {
	            mCursor.close();
	            mCursor = null;
	        }
        }
        
        public void prev() {
        }

        public void next(boolean force) {
        }
        
        public void setHandler(Handler handler) {
        	mPlayer.setHandler(handler);
        }
        
        /**
         * Removes the range of tracks specified from the play list. If a file within the range is
         * the file currently being played, playback will move to the next file after the
         * range. 
         * @param first The first file to be removed
         * @param last The last file to be removed
         * @return the number of tracks deleted
         */
        public int removeTracks(int first, int last) {
        	return 0;
        }
        
        /**
         * Removes all instances of the track with the given id
         * from the playlist.
         * @param id The id to be removed
         * @return how many instances of the track were removed
         */
        public int removeTrack(long id) {
        	return 0;
        }
        
        public void setShuffleMode(int shufflemode) {
        }

        /**
         * Returns the path of the currently playing file, or null if
         * no file is currently playing.
         */
        public String getPath() {
        	if ( mPlayer != null ) {
        		return mPlayer.getFileToPlay();
        	}
        	return null;
        }
        
        /**
         * Returns the rowid of the currently playing file, or -1 if
         * no file is currently playing.
         */
        public long getAudioId() {
        	return -1;
        }
        
        /**
         * Returns the position in the queue 
         * @return the position in the queue
         */
        public int getQueuePosition() {
        	return -1;
        }
        
        /**
         * Starts playing the track at the given position in the queue.
         * @param pos The position in the queue of the track that will be played.
         */
        public void setQueuePosition(int pos) {
        }

        public String getArtistName() {
        	return null;
        }
        
        public long getArtistId() {
        	synchronized (this) {
	            if (mCursor == null) {
	                return -1;
	            }
	            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
        	}
        }

        public String getAlbumName() {
        	return null;
        }

        public long getAlbumId() {
        	synchronized (this) {
	            if (mCursor == null) {
	                return -1;
	            }
	            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
	        }
        }

        public String getTrackName() {
        	return null;
        }
        
        /**
         * Returns the duration of the file in milliseconds.
         * Currently this method returns -1 for the duration of MIDI files.
         */
        public long duration() {
            if (mPlayer.isInitialized()) {
                return mPlayer.duration();
            }
            return -1;
        }

        /**
         * Returns the current playback position in milliseconds
         */
        public long position() {
            if (mPlayer.isInitialized()) {
                return mPlayer.position();
            }
            return -1;
        }

        /**
         * Seeks to the position specified.
         *
         * @param pos The position to seek to, in milliseconds
         */
        public long seek(long pos) {
            if (mPlayer.isInitialized()) {
                if (pos < 0) pos = 0;
                if (pos > mPlayer.duration()) pos = mPlayer.duration();
                return mPlayer.seek(pos);
            }
            return -1;
        }
        
        /**
         * Set volume to specific value.
         *
         * @param vol The volume to set to, from 0.0 to 1.0
         */
        public void setVolume(float vol) {
        	float volume = vol < 0 ? 0.0f : vol;
            if (mPlayer.isInitialized()) {      	
                mPlayer.setVolume(volume); 
            }
        }
        
        public boolean isLastTrack() {
        	return true;
        }
        public boolean isFirstTrack() {
        	return true;
        }
    }

    public class MultipleMediaPlayer extends LeMediaPlayer {
	    private long [] mAutoShuffleList = null;
	    private long [] mPlayList = null;
//	    private Cursor mPlayListCursor = null;
	    private int mPlayListLen = 0;
	    private Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);
	    private int mPlayPos = -1;
	    private static final String LOGTAG = "MultipleMediaPlayer";
	    private final Shuffler mRand = new Shuffler();
	    
	    
		public MultipleMediaPlayer(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
	    
	    private final char hexdigits [] = new char [] {
	            '0', '1', '2', '3',
	            '4', '5', '6', '7',
	            '8', '9', 'a', 'b',
	            'c', 'd', 'e', 'f'
	    };

	    public void release() {
	    	saveQueue(true);
	    	super.release();
	    }
	    
	    public void saveQueue(boolean full) {
	        Editor ed = mPreferences.edit();
	        //long start = System.currentTimeMillis();
	        if (full) {
	            StringBuilder q = new StringBuilder();
	            
	            // The current playlist is saved as a list of "reverse hexadecimal"
	            // numbers, which we can generate faster than normal decimal or
	            // hexadecimal numbers, which in turn allows us to save the playlist
	            // more often without worrying too much about performance.
	            // (saving the full state takes about 40 ms under no-load conditions
	            // on the phone)
	            int len = mPlayListLen;
	            for (int i = 0; i < len; i++) {
	                long n = mPlayList[i];
	                if (n == 0) {
	                    q.append("0;");
	                } else {
	                    while (n != 0) {
	                        int digit = (int)(n & 0xf);
	                        n >>= 4;
	                        q.append(hexdigits[digit]);
	                    }
	                    q.append(";");
	                }
	            }
	            //Log.i("@@@@ service", "created queue string in " + (System.currentTimeMillis() - start) + " ms");
	            ed.putString("queue", q.toString());
	           // ed.putInt("cardid", mCardId);
	            if (mShuffleMode != SHUFFLE_NONE) {
	                // In shuffle mode we need to save the history too
	                len = mHistory.size();
	                q.setLength(0);
	                for (int i = 0; i < len; i++) {
	                    int n = mHistory.get(i);
	                    if (n == 0) {
	                        q.append("0;");
	                    } else {
	                        while (n != 0) {
	                            int digit = (n & 0xf);
	                            n >>= 4;
	                            q.append(hexdigits[digit]);
	                        }
	                        q.append(";");
	                    }
	                }
	                ed.putString("history", q.toString());
	            }
	        }
	        ed.putInt("curpos", mPlayPos);
	        Log.d(TAG, "saveQueue()===================================before save seek pos");
	        if ( mPlayer != null && mPlayer.isInitialized() ) {
	        	Log.d(TAG, "saveQueue()===================================save seek pos");
	            ed.putLong("seekpos", mPlayer.position());
	        }
	        ed.putInt("repeatmode", mRepeatMode);
	        ed.putInt("shufflemode", mShuffleMode);
	        //added by liuqiang
	        ed.putFloat("volume", mVolume);
	        ed.putInt("currentplayliststyle", mCurrentPlaylistStyle);
	        //end add
	        ed.commit();
	  
	        //Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
	    }

	    public void reloadQueue() {
	        String q = null;
	        
//	        boolean newstyle = false;
	       // int id = mCardId;
	        if (mPreferences.contains("cardid")) {
//	            newstyle = true;
	           // id = mPreferences.getInt("cardid", ~mCardId);
	        }
//	        if (id == mCardId) {
//	            // Only restore the saved playlist if the card is still
//	            // the same one as when the playlist was saved
//	            q = mPreferences.getString("queue", "");
//	            //added by liuqiang
//	            mCurrentPlaylistStyle = mPreferences.getInt("currentplayliststyle", FROM_NONE);
//	            //end add
//	        }
	        int qlen = q != null ? q.length() : 0;
	        if (qlen > 1) {
	            //Log.i("@@@@ service", "loaded queue: " + q);
	            int plen = 0;
	            int n = 0;
	            int shift = 0;
	            for (int i = 0; i < qlen; i++) {
	                char c = q.charAt(i);
	                if (c == ';') {
	                    ensurePlayListCapacity(plen + 1);
	                    mPlayList[plen] = n;
	                    plen++;
	                    n = 0;
	                    shift = 0;
	                } else {
	                    if (c >= '0' && c <= '9') {
	                        n += ((c - '0') << shift);
	                    } else if (c >= 'a' && c <= 'f') {
	                        n += ((10 + c - 'a') << shift);
	                    } else {
	                        // bogus playlist data
	                        plen = 0;
	                        break;
	                    }
	                    shift += 4;
	                }
	            }
	            mPlayListLen = plen;

	            int pos = mPreferences.getInt("curpos", 0);
	            if (pos < 0 || pos >= mPlayListLen) {
	                // The saved playlist is bogus, discard it
	                mPlayListLen = 0;
	                return;
	            }
	            mPlayPos = pos;
	            
	            // When reloadQueue is called in response to a card-insertion,
	            // we might not be able to query the media provider right away.
	            // To deal with this, try querying for the current file, and if
	            // that fails, wait a while and try again. If that too fails,
	            // assume there is a problem and don't restore the state.
	            Cursor crsr = MusicUtils.query(mContext,
	                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	                        new String [] {"_id"}, "_id=" + mPlayList[mPlayPos] , null, null);
	            if (crsr == null || crsr.getCount() == 0) {
	                // wait a bit and try again
	                SystemClock.sleep(3000);
	                crsr = getContentResolver().query(
	                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	                        mCursorCols, "_id=" + mPlayList[mPlayPos] , null, null);
	            }
	            if (crsr != null) {
	                crsr.close();
	            }

	            // Make sure we don't auto-skip to the next song, since that
	            // also starts playback. What could happen in that case is:
	            // - music is paused
	            // - go to UMS and delete some files, including the currently playing one
	            // - come back from UMS
	            // (time passes)
	            // - music app is killed for some reason (out of memory)
	            // - music service is restarted, service restores state, doesn't find
	            //   the "current" file, goes to the next and: playback starts on its
	            //   own, potentially at some random inconvenient time.
	            mOpenFailedCounter = 20;
	            mQuietMode = true;
	            openCurrent();
	            mQuietMode = false;
	            if (!mPlayer.isInitialized()) {
	                // couldn't restore the saved state
	                mPlayListLen = 0;
	                return;
	            }
	            
	            long seekpos = mPreferences.getLong("seekpos", 0);
	            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
	            Log.d(LOGTAG, "restored queue, currently at position "
	                    + position() + "/" + duration()
	                    + " (requested " + seekpos + ")");
	            
	            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
	            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
	                repmode = REPEAT_NONE;
	            }
	            
	            mRepeatMode = repmode;
	            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
//	            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
	            if (shufmode != SHUFFLE_NORMAL) {
	                shufmode = SHUFFLE_NONE;
	            }
	            if (shufmode != SHUFFLE_NONE) {
	                // in shuffle mode we need to restore the history too
	                q = mPreferences.getString("history", "");
	                qlen = q != null ? q.length() : 0;
	                if (qlen > 1) {
	                    plen = 0;
	                    n = 0;
	                    shift = 0;
	                    mHistory.clear();
	                    for (int i = 0; i < qlen; i++) {
	                        char c = q.charAt(i);
	                        if (c == ';') {
	                            if (n >= mPlayListLen) {
	                                // bogus history data
	                                mHistory.clear();
	                                break;
	                            }
	                            mHistory.add(n);
	                            n = 0;
	                            shift = 0;
	                        } else {
	                            if (c >= '0' && c <= '9') {
	                                n += ((c - '0') << shift);
	                            } else if (c >= 'a' && c <= 'f') {
	                                n += ((10 + c - 'a') << shift);
	                            } else {
	                                // bogus history data
	                                mHistory.clear();
	                                break;
	                            }
	                            shift += 4;
	                        }
	                    }
	                }
	            }
//	            if (shufmode == SHUFFLE_AUTO) {
//	                if (! makeAutoShuffleList()) {
//	                    shufmode = SHUFFLE_NONE;
//	                }
//	            }
	            mShuffleMode = shufmode;
	        }
	        //added by liuqiang
	        mVolume = mPreferences.getFloat("volume", 0.0f);        
	        //end add
	    }

	    private void ensurePlayListCapacity(int size) {
	        if (mPlayList == null || size > mPlayList.length) {
	            // reallocate at 2x requested size so we don't
	            // need to grow and copy the array for every
	            // insert
	            long [] newlist = new long[size * 2];
	            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
	            for (int i = 0; i < len; i++) {
	                newlist[i] = mPlayList[i];
	            }
	            mPlayList = newlist;
	        }
	        // FIXME: shrink the array when the needed size is much smaller
	        // than the allocated size
	    }
	    
	    // insert the list of songs at the specified position in the playlist
	    private void addToPlayList(long [] list, int position) {
	        int addlen = list.length;
	        if (position < 0) { // overwrite
	            mPlayListLen = 0;
	            position = 0;
	        }
	        ensurePlayListCapacity(mPlayListLen + addlen);
	        if (position > mPlayListLen) {
	            position = mPlayListLen;
	        }
	        
	        // move part of list after insertion point
	        int tailsize = mPlayListLen - position;
	        for (int i = tailsize ; i > 0 ; i--) {
	            mPlayList[position + i] = mPlayList[position + i - addlen]; 
	        }
	        
	        // copy list into playlist
	        for (int i = 0; i < addlen; i++) {
	            mPlayList[position + i] = list[i];
	        }
	        mPlayListLen += addlen;
	        //added by liuqiang
//	        setCurrentPlaylistStyle(FROM_OTHERS);
	        //end add
	    }
	    
	    public void enqueue(long [] list, int action) {
	        synchronized(this) {
	        	//added by liuqiang
	        	setCurrentPlaylistStyle(FROM_OTHERS);
	        	//end add
	            if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
	                addToPlayList(list, mPlayPos + 1);
	                notifyChange(QUEUE_CHANGED);
	            } else {
	                // action == LAST || action == NOW || mPlayPos + 1 == mPlayListLen
	                addToPlayList(list, Integer.MAX_VALUE);
	                notifyChange(QUEUE_CHANGED);
	                if (action == NOW) {
	                    mPlayPos = mPlayListLen - list.length;
	                    openCurrent();
	                    play();
	                    notifyChange(META_CHANGED);
	                    return;
	                }
	            }
	            if (mPlayPos < 0) {
	                mPlayPos = 0;
	                openCurrent();
	                play();
	                notifyChange(META_CHANGED);
	            }
	        }
	    }
	    
//	    public void registerMonitorDataSetObserver() {
//	    	if ( mCurrentMonitorCursor != null ) {
//	    		mCurrentMonitorCursor.registerDataSetObserver(mMonitorDataSetObserver);
//	    	}
//	    }
//	    
//	    public void unregisterMonitorDataSetObserver() {
//	    	if ( mCurrentMonitorCursor != null ) {
//	    		mCurrentMonitorCursor.unregisterDataSetObserver(mMonitorDataSetObserver);
//	    	}
//	    }
	    
	    private long [] getPlaylistFromCursor(Cursor cursor) {
	        if (cursor == null) {
	            return new long[0];
	        }
	        int len = cursor.getCount();
	        long [] list = new long[len];
	        cursor.moveToFirst();
	        int colidx = -1;
	        try {
	            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
	        } catch (IllegalArgumentException ex) {
	            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
	        }
	        for (int i = 0; i < len; i++) {
	            list[i] = cursor.getLong(colidx);
	            cursor.moveToNext();
	        }
	        return list;
	    }

//	    private DataSetObserver mMonitorDataSetObserver = new DataSetObserver() {
//			@Override
//			public void onChanged() {
//				Log.d(TAG, "onChanged()====================>");
//				if ( mCurrentMonitorCursor != null ) {
////					syncPlaylistWithCursor();
//				}
//			}
//		};
		
		private int findIndex(long [] list, long value) {
			int index = -1;
			for ( int i = 0; i < list.length; i++ ) {
				if ( value == list[i] ) {
					index = i;
					break;
				}
			}
			return index;
		}
		
		public void syncPlaylistWithCursor(Cursor cursor) {
			synchronized (this) {
			if ( cursor != null ) {
				long [] list = getPlaylistFromCursor(cursor);
				int len = list.length;
				if ( 0 == len ) {
					//TODO:add handle for this situation, add stop()?
					mPlayList = list;
					mPlayListLen = len;
					mPlayPos  = -1;
					MediaPlaybackService.this.stop(true);
					//do we need to do this?
					notifyChange(QUEUE_CHANGED);
					notifyChange(META_CHANGED);
					return;
				}
				boolean gonext = false;
				int newpos = -1;
				if ( mPlayListLen > 0 && mPlayPos >= 0 && mPlayPos < mPlayListLen ) {
					newpos = findIndex(list, mPlayList[mPlayPos]);
					if ( newpos == -1 ) {
						for ( int i = mPlayPos + 1; i < mPlayListLen; i++ ) {
							int temppos = findIndex(list, mPlayList[i]);
							if ( temppos != -1 ) {
								newpos = temppos;
								break;
							}
						}
						gonext = true;
					}
				}
				if ( newpos == -1 ) {
					newpos = 0;
				}
				mPlayList = list;
				mPlayListLen = list.length;
				mPlayPos = newpos;
				notifyChange(QUEUE_CHANGED);
				if ( gonext ) {
					//since this method is called in another thread, so use handler to play next here
					mMultiplePlayerErrorHandler.sendEmptyMessageDelayed(HANDLE_PLAY_BY_DELETE, 500);
				}
			}
			}
		}
	    
	    
	    
	    public void open(long [] list, int position) {
	        synchronized (this) {
	        	//added by liuqiang
	        	setCurrentPlaylistStyle(FROM_OTHERS);
	        	//end add
	            long oldId = getAudioId();
	            int listlength = list.length;
	            boolean newlist = true;
	            if (mPlayListLen == listlength) {
	                // possible fast path: list might be the same
	                newlist = false;
	                for (int i = 0; i < listlength; i++) {
	                    if (list[i] != mPlayList[i]) {
	                        newlist = true;
	                        break;
	                    }
	                }
	            }
	            if (newlist) {
	                addToPlayList(list, -1);
	                notifyChange(QUEUE_CHANGED);
	            }
//	            int oldpos = mPlayPos;
//	            int newpos = position;
	            if (position >= 0) {
	            	mPlayPos = position;
	            } else {
	            	mPlayPos = mRand.nextInt(mPlayListLen);
	            }
	            mHistory.clear();
	            if ( oldId != mPlayList[mPlayPos] ) {
//		            saveBookmarkIfNeeded();
		            openCurrent();
		            if (oldId != getAudioId()) {
		                notifyChange(META_CHANGED);
		            }
	            }
	        }
	    }
	    
	    public void moveQueueItem(int index1, int index2) {
	        synchronized (this) {
	            if (index1 >= mPlayListLen) {
	                index1 = mPlayListLen - 1;
	            }
	            if (index2 >= mPlayListLen) {
	                index2 = mPlayListLen - 1;
	            }
	            if (index1 < index2) {
	                long tmp = mPlayList[index1];
	                for (int i = index1; i < index2; i++) {
	                    mPlayList[i] = mPlayList[i+1];
	                }
	                mPlayList[index2] = tmp;
	                if (mPlayPos == index1) {
	                    mPlayPos = index2;
	                } else if (mPlayPos >= index1 && mPlayPos <= index2) {
	                        mPlayPos--;
	                }
	            } else if (index2 < index1) {
	                long tmp = mPlayList[index1];
	                for (int i = index1; i > index2; i--) {
	                    mPlayList[i] = mPlayList[i-1];
	                }
	                mPlayList[index2] = tmp;
	                if (mPlayPos == index1) {
	                    mPlayPos = index2;
	                } else if (mPlayPos >= index2 && mPlayPos <= index1) {
	                        mPlayPos++;
	                }
	            }
	            notifyChange(QUEUE_CHANGED);
	        }
	    }

	    public long [] getQueue() {
	        synchronized (this) {
	            int len = mPlayListLen;
	            long [] list = new long[len];
	            for (int i = 0; i < len; i++) {
	                list[i] = mPlayList[i];
	            }
	            return list;
	        }
	    }
	    
	    public boolean hasPlaylist() {
	    	return mPlayListLen > 0;
	    }
	    
	    public void restoreOpen() {
	    	openCurrent();
	    }

	    private void openCurrent() {
	        synchronized (this) {
	            if (mCursor != null) {
	            	mCursor.close();
	            	mCursor = null;
	            }
	            if (mPlayListLen == 0) {
	                return;
	            }
	            MediaPlaybackService.this.stop(false);

	            String id = String.valueOf(mPlayList[mPlayPos]);
	            
	            mCursor = getContentResolver().query(
	                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	                    mCursorCols, "_id=" + id , null, null);
	            if ( mCursor != null && mCursor.getCount() > 0 ) {
	            	mCursor.moveToFirst();
	                open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id, false);
	                // go to bookmark if needed
	                if (isPodcast()) {
	                    long bookmark = getBookmark();
	                    // Start playing a little bit before the bookmark,
	                    // so it's easier to get back in to the narrative.
	                    seek(bookmark - 5000);
	                }
	            } 
	        }
	    }
   
	    private static final int HANDLE_FILE_BROKEN = 1;
	    private static final int HANDLE_NEXT = 2;
	    private static final int HANDLE_PLAY_BY_DELETE = 3;
	    
	    private Handler mMultiplePlayerErrorHandler = new Handler() {
	    	private boolean mHandleFileBroken = false;
	        @Override
	        public void handleMessage(Message msg) {
	            Log.d(TAG, "mMultiplePlayerErrorHandler =============>" + msg.what);
	            if ( msg.what == HANDLE_FILE_BROKEN ) {
	            	mHandleFileBroken = true;
		            MediaPlaybackService.this.stop(true);
	                if (!mQuietMode) {
	                	MusicUtils.showToast(mContext, R.string.broken_file);
	                }
	                Message message = mMultiplePlayerErrorHandler.obtainMessage(HANDLE_NEXT);
	                mMultiplePlayerErrorHandler.sendMessageDelayed(message, 2000);
	                mHandleFileBroken = false;
	            } else if ( msg.what == HANDLE_NEXT ) {
	            	if ( !mPlayer.isInitialized() && !mHandleFileBroken ) {
	            		next(false);
	            	}
	            } else if ( msg.what == HANDLE_PLAY_BY_DELETE ) {
					  boolean wasPlaying = isPlaying();
					  MediaPlaybackService.this.stop(false);
					  openCurrent();				  
					  if (wasPlaying) {
						  play();
					  }
					  notifyChange(META_CHANGED);
	            }
	        }
	    };
	    
	    public void handlePlayError(String err) {
	    	if( err.equals(ERROR_FILE_BROKEN) ) {
	    		mMultiplePlayerErrorHandler.obtainMessage(HANDLE_FILE_BROKEN).sendToTarget();
	    	} 
	    }
	    
	    public void open(String path, boolean oneshot) {
	        synchronized (this) {
	        	//since this is in MulipleMediaPlayer, oneshot should not be true
	            if (path == null || oneshot) {
	                return;
	            }           
	            //added by liuqiang
	            setCurrentPlaylistStyle(FROM_OTHERS);
	            //end add
	            // if mCursor is null, try to associate path with a database cursor
	            if (mCursor == null) {

	                ContentResolver resolver = getContentResolver();
	                Uri uri;
	                String where;
	                String selectionArgs[];
	                if (path.startsWith("content://media/")) {
	                    uri = Uri.parse(path);
	                    where = null;
	                    selectionArgs = null;
	                } else {
	                   uri = MediaStore.Audio.Media.getContentUriForPath(path);
	                   where = MediaStore.Audio.Media.DATA + "=?";
	                   selectionArgs = new String[] { path };
	                }
	                
	                try {
	                    mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
	                    if  (mCursor != null) {
	                        if (mCursor.getCount() == 0) {
	                            mCursor.close();
	                            mCursor = null;
	                        } else {
	                            mCursor.moveToNext();
	                            ensurePlayListCapacity(1);
	                            mPlayListLen = 1;
	                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
	                            mPlayPos = 0;
	                        }
	                    }

	                } catch (UnsupportedOperationException ex) {
	                }
	            }
	          //do not use "content://" style path as much as posible here
	            String filePath = path;
	            if (path.startsWith("content://media/")) {
	            	if ( mCursor != null ) {
	            		filePath = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
	            	}
                }
	            mPlayer.setFileToPlay(filePath);
	            mPlayer.setDataSource(filePath);       
	            if (! mPlayer.isInitialized()) {
	            } else {
	                mOpenFailedCounter = 0;
	            }
	        }
	    }

	    /*
	      Desired behavior for prev/next/shuffle:

	      - NEXT will move to the next track in the list when not shuffling, and to
	        a track randomly picked from the not-yet-played tracks when shuffling.
	        If all tracks have already been played, pick from the full set, but
	        avoid picking the previously played track if possible.
	      - when shuffling, PREV will go to the previously played track. Hitting PREV
	        again will go to the track played before that, etc. When the start of the
	        history has been reached, PREV is a no-op.
	        When not shuffling, PREV will go to the sequentially previous track (the
	        difference with the shuffle-case is mainly that when not shuffling, the
	        user can back up to tracks that are not in the history).

	        Example:
	        When playing an album with 10 tracks from the start, and enabling shuffle
	        while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
	        the final play order might be 1-2-3-4-5-8-10-6-9-7.
	        When hitting 'prev' 8 times while playing track 7 in this example, the
	        user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next',
	        a random track will be picked again. If at any time user disables shuffling
	        the next/previous track will be picked in sequential order again.
	     */

	    public void prev() {
	        synchronized (this) {
	            if (mShuffleMode == SHUFFLE_NORMAL) {
	                // go to previously-played track and remove it from the history
	                int histsize = mHistory.size();
	                if (histsize == 0) {
	                    // prev is a no-op
	                    return;
	                }
	                Integer pos = mHistory.remove(histsize - 1);
	                mPlayPos = pos.intValue();
	            } else {
	                if (mPlayPos > 0) {
	                    mPlayPos--;
	                } else {
	                    mPlayPos = mPlayListLen - 1;
	                }
	                if ( mPlayPos < 0 ) {
	                	//here we think that there is nothing to play, stop
		                gotoIdleState();
	                    if (mIsSupposedToBePlaying) {
	                        mIsSupposedToBePlaying = false;
	                        notifyChange(PLAYSTATE_CHANGED);
	                    }
	                }
	            }
	            MediaPlaybackService.this.stop(false);
	            openCurrent();
	            if ( mPlayer.getFileToPlay() == null ) {
	            	prev();
	            } else {
	            play();
	            notifyChange(META_CHANGED);
	            }
	        }
	    }

	    public void next(boolean force) {
	        synchronized (this) {
	            if (mPlayListLen <= 0) {
	                Log.d(LOGTAG, "No play queue");
	                return;
	            }

	            // Store the current file in the history, but keep the history at a
	            // reasonable size
	            if (mPlayPos >= 0) {
	                mHistory.add(Integer.valueOf(mPlayPos));
	            }
	            if (mHistory.size() > MAX_HISTORY_SIZE) {
	                mHistory.removeElementAt(0);
	            }

	            if (mShuffleMode == SHUFFLE_NORMAL) {
	                // Pick random next track from the not-yet-played ones
	                // TODO: make it work right after adding/removing items in the queue.

	                int numTracks = mPlayListLen;
	                int[] tracks = new int[numTracks];
	                for (int i=0;i < numTracks; i++) {
	                    tracks[i] = i;
	                }

	                int numHistory = mHistory.size();
	                int numUnplayed = numTracks;
	                for (int i=0;i < numHistory; i++) {
	                    int idx = mHistory.get(i).intValue();
	                    if (idx < numTracks && tracks[idx] >= 0) {
	                        numUnplayed--;
	                        tracks[idx] = -1;
	                    }
	                }

	                // 'numUnplayed' now indicates how many tracks have not yet
	                // been played, and 'tracks' contains the indices of those
	                // tracks.
	                if (numUnplayed <=0) {
	                    // everything's already been played
	                    if (mRepeatMode == REPEAT_ALL || force) {
	                        //pick from full set
	                        numUnplayed = numTracks;
	                        for (int i=0;i < numTracks; i++) {
	                            tracks[i] = i;
	                        }
	                    } else {
	                        // all done
	                        gotoIdleState();
	                        if (mIsSupposedToBePlaying) {
	                            mIsSupposedToBePlaying = false;
	                            notifyChange(PLAYSTATE_CHANGED);
	                        }
	                        return;
	                    }
	                }
	                int skip = mRand.nextInt(numUnplayed);
	                int cnt = -1;
	                while (true) {
	                    while (tracks[++cnt] < 0)
	                        ;
	                    skip--;
	                    if (skip < 0) {
	                        break;
	                    }
	                }
	                mPlayPos = cnt;
	            } else {
	                if (mPlayPos >= mPlayListLen - 1) {
	                    // we're at the end of the list
	                    if (mRepeatMode == REPEAT_NONE && !force) {
	                        // all done
	                        MediaPlaybackService.this.stop(false);
	                        openCurrent();
	                        gotoIdleState();
	                        notifyChange(PLAYBACK_COMPLETE);
	                        mIsSupposedToBePlaying = false;
	                        return;
	                    } else if (mRepeatMode == REPEAT_ALL || force) {
	                        mPlayPos = 0;
	                    }
	                } else {
	                    mPlayPos++;
	                }
	            }
//	            saveBookmarkIfNeeded();
	            MediaPlaybackService.this.stop(false);
	            openCurrent();
	            if ( mPlayer.getFileToPlay() == null ) {
	            	next(false);
	            } else {
		            play();
		            notifyChange(META_CHANGED);
	            }
	        }
	    }
	    
	    public boolean isLastTrack() {
	    	if ( mRepeatMode == REPEAT_ALL || mShuffleMode == SHUFFLE_NORMAL ) {
	    		return false;
	    	}
	    	return mPlayPos >= mPlayListLen - 1;
	    }
	    
	    public boolean isFirstTrack() {
	    	if ( mRepeatMode == REPEAT_ALL ) {
	    		if ( mShuffleMode == SHUFFLE_NORMAL ) {
	    			return 0 == mHistory.size();
	    		} else {
	    			return false;
	    		}
	    	} 
	    	return mPlayPos <= 0;
	    }
	    
	    private void saveBookmarkIfNeeded() {
	        try {
	            if (isPodcast()) {
	                long pos = position();
	                long bookmark = getBookmark();
	                long duration = duration();
	                if ((pos < bookmark && (pos + 10000) > bookmark) ||
	                        (pos > bookmark && (pos - 10000) < bookmark)) {
	                    // The existing bookmark is close to the current
	                    // position, so don't update it.
	                    return;
	                }
	                if (pos < 15000 || (pos + 10000) > duration) {
	                    // if we're near the start or end, clear the bookmark
	                    pos = 0;
	                }
	                
	                // write 'pos' to the bookmark field
	                ContentValues values = new ContentValues();
//	                values.put(MediaStore.Audio.Media.BOOKMARK, pos);
	                values.put(BOOKMARK, pos);
	                //TODO: switch cursor?
	                Uri uri = ContentUris.withAppendedId(
	                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(IDCOLIDX));
	                getContentResolver().update(uri, values, null, null);
	            }
	        } catch (SQLiteException ex) {
	        }
	    }

	    public void shuffleAuto() {    	
	    	if ( mPlayListLen > 0 ) {
	    		if ( !isPlaying() ) {
		    		if ( getPath() == null ) {
		    			openCurrent();
		    			notifyChange(META_CHANGED);
		    		} 
		    		play();
	    		}
	    	} else {    
	    		setRepeatMode(REPEAT_ALL);
		    	setShuffleMode(SHUFFLE_NORMAL);
	    		makeAutoShuffleList();
	    		openCurrent();
              play();
              notifyChange(META_CHANGED);
	    	}
	    }
	    
	    // Make sure there are at least 5 items after the currently playing item
	    // and no more than 10 items before.
	    private void doAutoShuffleUpdate() {
	        boolean notify = false;
	        // remove old entries
	        if (mPlayPos > 10) {
	            removeTracks(0, mPlayPos - 9);
	            notify = true;
	        }
	        // add new entries if needed
	        int to_add = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
	        for (int i = 0; i < to_add; i++) {
	            // pick something at random from the list
	            int idx = mRand.nextInt(mAutoShuffleList.length);
	            long which = mAutoShuffleList[idx];
	            ensurePlayListCapacity(mPlayListLen + 1);
	            mPlayList[mPlayListLen++] = which;
	            notify = true;
	        }
	        if (notify) {
	            notifyChange(QUEUE_CHANGED);
	        }
	    }

	    private boolean makeAutoShuffleList() {
	        ContentResolver res = getContentResolver();
	        Cursor c = null;
	        try {
	            c = res.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	                    new String[] {MediaStore.Audio.Media._ID}, MediaStore.Audio.Media.IS_MUSIC + "=1",
	                    null, null);
	            if (c == null || c.getCount() == 0) {
	                return false;
	            }
	            int len = c.getCount();
	            long [] list = new long[len];
	            for (int i = 0; i < len; i++) {
	                c.moveToNext();
	                list[i] = c.getLong(0);
	            }
	            mPlayList = list;
	            mPlayListLen = list.length;
	            mPlayPos = 0;
	            return true;
	        } catch (RuntimeException ex) {
	        } finally {
	            if (c != null) {
	                c.close();
	            }
	        }
	        return false;
	    }
	    
	    public int removeTracks(int first, int last) {
	        int numremoved = removeTracksInternal(first, last);
	        if (numremoved > 0) {
	            notifyChange(QUEUE_CHANGED);
	        }
	        return numremoved;
	    }
	    
	    private int removeTracksInternal(int first, int last) {
	        synchronized (this) {
	            if (last < first) return 0;
	            if (first < 0) first = 0;
	            if (last >= mPlayListLen) last = mPlayListLen - 1;

	            boolean gotonext = false;
	            if (first <= mPlayPos && mPlayPos <= last) {
	                mPlayPos = first;
	                gotonext = true;
	            } else if (mPlayPos > last) {
	                mPlayPos -= (last - first + 1);
	            }
	            int num = mPlayListLen - last - 1;
	            for (int i = 0; i < num; i++) {
	                mPlayList[first + i] = mPlayList[last + 1 + i];
	            }
	            mPlayListLen -= last - first + 1;
	            
	            if (gotonext) {
	                if (mPlayListLen == 0) {
	                    MediaPlaybackService.this.stop(true);
	                    mPlayPos = -1;
	                } else {
	                    if (mPlayPos >= mPlayListLen) {
	                        mPlayPos = 0;
	                    }
	                    boolean wasPlaying = isPlaying();
	                    MediaPlaybackService.this.stop(false);
	                    openCurrent();
	                    if (wasPlaying) {
	                        play();
	                    }
	                }
	            }
	            return last - first + 1;
	        }
	    }
	    
	    public int removeTrack(long id) {
	        int numremoved = 0;
	        synchronized (this) {
	            for (int i = 0; i < mPlayListLen; i++) {
	                if (mPlayList[i] == id) {
	                    numremoved += removeTracksInternal(i, i);
	                    i--;
	                }
	            }
	        }
	        if (numremoved > 0) {
	            notifyChange(QUEUE_CHANGED);
	        }
	        return numremoved;
	    }
	    
	    public void setShuffleMode(int shufflemode) {
	        synchronized(this) {
	            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
	                return;
	            }
	            mShuffleMode = shufflemode;
	            saveQueue(false);
	        }
	    }
	    
	    public void setRepeatMode(int repeatmode) {
	        synchronized(this) {
	            mRepeatMode = repeatmode;
	            saveQueue(false);
	        }
	    }
	    
	    public long getAudioId() {
	        synchronized (this) {
	            if (mPlayPos >= 0 && mPlayer.isInitialized()) {
	                return mPlayList[mPlayPos];
	            }
	        }
	        return -1;
	    }
	    
	    public int getQueuePosition() {
	        synchronized(this) {
	            return mPlayPos;
	        }
	    }
	    
	    public void setQueuePosition(int pos) {
	        synchronized(this) {
	        	if ( mPlayPos != pos ) {
		            MediaPlaybackService.this.stop(false);
		            mPlayPos = pos;
		            openCurrent();
		            play();
		            notifyChange(META_CHANGED);
	        	} 
	        	if ( !isPlaying() ) {
	        		play();
	        	}
	        }
	    }

	    public String getArtistName() {
	        synchronized(this) {
	            if (mCursor == null || mCursor.getCount() <= 0 ) {
	                return null;
	            }
	            mCursor.moveToFirst();
	            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
	        }
	    }

	    public String getAlbumName() {
	        synchronized (this) {
	            if (mCursor == null|| mCursor.getCount() <= 0) {
	                return null;
	            }
	            mCursor.moveToFirst();
	            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
	        }
	    }

	    public String getTrackName() {
	        synchronized (this) {
	            if (mCursor == null|| mCursor.getCount() <= 0) {
	                return null;
	            }
	            mCursor.moveToFirst();
	            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
	        }
	    }

	    private boolean isPodcast() {
	        synchronized (this) {
	            if (mCursor == null|| mCursor.getCount() <= 0) {
	                return false;
	            }
	            mCursor.moveToFirst();
	            return (mCursor.getInt(PODCASTCOLIDX) > 0);
	        }
	    }
	    
	    private long getBookmark() {
	        synchronized (this) {
	            if (mCursor == null|| mCursor.getCount() <= 0) {
	                return 0;
	            }
	            mCursor.moveToFirst();
	            return mCursor.getLong(BOOKMARKCOLIDX);
	        }
	    }
    }
    
    public class OneShotMediaPlayer extends LeMediaPlayer {

		public OneShotMediaPlayer(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

        public void openAsync(String path) {
        	Log.d(TAG, "one shot =======>openAsync()====path=" + path);
            synchronized (this) {
                if (path == null) {
                    return;
                }
                mPlayer.setFileToPlay(path);
                if ( mCursor != null ) {
                	mCursor.close();
                }
                mCursor = null;
                mPlayer.setDataSourceAsync(path);
            }
        }
        
        public void handlePlayError(String err) {
        }
        
        /**
         * Opens the specified file and readies it for playback.
         *
         * @param path The full path of the file to be opened.
         * @param oneshot when set to true, playback will stop after this file completes, instead
         * of moving on to the next track in the list 
         */
        public void open(String path, boolean oneshot) {
        	Log.d(TAG, "one shot =======>open()====path=" + path);
        	synchronized (this) {
                if (path == null || !oneshot) {
                    return;
                }
                boolean getCursor = true;
                String oldpath = mPlayer.getFileToPlay();
                if ( mCursor != null && oldpath != null && path.equals(oldpath)) {
                	getCursor = false;
                }
                // if mCursor is null, try to associate path with a database cursor
                if (getCursor) {

                    ContentResolver resolver = getContentResolver();
                    Uri uri;
                    String where;
                    String selectionArgs[];
                    if (path.startsWith("content://media/")) {
                        uri = Uri.parse(path);
                        where = null;
                        selectionArgs = null;
                    } else {
                    	uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                       uri = MediaStore.Audio.Media.getContentUriForPath(path);
                       where = MediaStore.Audio.Media.DATA + "=?";
                       selectionArgs = new String[] { path };
                    }
                    
                    try {
                        mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
                        if  (mCursor != null) {
                            if (mCursor.getCount() == 0) {
                                mCursor.close();
                                mCursor = null;
                            } else {
                                mCursor.moveToNext();
                            }
                        }
                    } catch (UnsupportedOperationException ex) {
                    }
                }
                mPlayer.setFileToPlay(path);
                mPlayer.setDataSource(path);   
                //TODO: need to do as follows?
                if (! mPlayer.isInitialized()) {
					Log.d(TAG, "one shot =======>open()====after set data source failed, stop it");
					stop();
					if (!mQuietMode) {
						Toast.makeText(mContext, R.string.broken_file,
								Toast.LENGTH_SHORT).show();
					}
//                    if (mOpenFailedCounter++ < 10) {
//                        // beware: this ends up being recursive because next() calls open() again.
//                        next(false);
//                    }
//                    if (! mPlayer.isInitialized() && mOpenFailedCounter != 0) {
//                        // need to make sure we only shows this once
//                        mOpenFailedCounter = 0;
//                        if (!mQuietMode) {
//                            Toast.makeText(mContext, R.string.playback_failed, Toast.LENGTH_SHORT).show();
//                        }
//                        Log.d(LOGTAG, "Failed to open file for playback");
//                    }
                } else {
//                    mOpenFailedCounter = 0;
                }
            }
        }
        
        public void prev() {
        	synchronized (this) {
				// we were playing a specific file not part of a playlist, so
				// there is no 'previous'
				seek(0);
				play();
        	}
        }

        public void next(boolean force) {
        	synchronized (this) {
				// we were playing a specific file not part of a playlist, so
				// there is no 'next'
				seek(0);
				play();
        	}
        }

        public void setShuffleMode(int shufflemode) {
        	synchronized(this) {
                if (mShuffleMode == shufflemode) {
                    return;
                }
                mShuffleMode = shufflemode;
                saveQueue(false);
                //do not need to save queue from multiple player?
            }
        }
        
        /**
         * Returns the rowid of the currently playing file, or -1 if
         * no file is currently playing.
         */
        public long getAudioId() {
        	synchronized(this) {
				if (mCursor != null && mCursor.getCount() > 0) {
					mCursor.moveToFirst();
					return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
				}
				return -1;
        	}
		}

        public String getArtistName() {
        	//TODO: add handle for get artist name from http play source in future
            synchronized(this) {
                if (mCursor == null|| mCursor.getCount() <= 0) {
                    return null;
                }
                mCursor.moveToFirst();
                return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            }
        }

        public String getAlbumName() {
        	//TODO: add handle for get album name from http play source in future
        	synchronized (this) {
                if (mCursor == null|| mCursor.getCount() <= 0) {
                    return null;
                }
                mCursor.moveToFirst();
                return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            }
        }

        public String getTrackName() {
        	//TODO: add handle for get track name from http play source in future
            synchronized (this) {
                if (mCursor == null|| mCursor.getCount() <= 0) {
                    return null;
                }
                mCursor.moveToFirst();
                return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            }
        }
    	
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IMediaPlaybackService.Stub {
        WeakReference<MediaPlaybackService> mService;
        
        ServiceStub(MediaPlaybackService service) {
            mService = new WeakReference<MediaPlaybackService>(service);
        }

        public void openFileAsync(String path)
        {
            mService.get().openAsync(path);
        }
        public void openFile(String path, boolean oneShot)
        {
            mService.get().open(path, oneShot);
        }
        public void open(long [] list, int position) {
            mService.get().open(list, position);
        }
        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }
        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }
        public boolean isPlaying() {
            return mService.get().isPlaying();
        }
        public void stop() {
            mService.get().stop();
        }
        public void pause() {
            mService.get().pause();
        }
        public void play() {
            mService.get().play();
        }
        public void prev() {
            mService.get().prev();
        }
        public void next() {
            mService.get().next(true);
        }
        public String getTrackName() {
            return mService.get().getTrackName();
        }
        public String getAlbumName() {
            return mService.get().getAlbumName();
        }
        public long getAlbumId() {
            return mService.get().getAlbumId();
        }
        public String getArtistName() {
            return mService.get().getArtistName();
        }
        public long getArtistId() {
            return mService.get().getArtistId();
        }
        public void enqueue(long [] list , int action) {
            mService.get().enqueue(list, action);
        }
        public long [] getQueue() {
            return mService.get().getQueue();
        }
        public void moveQueueItem(int from, int to) {
            mService.get().moveQueueItem(from, to);
        }
        public String getPath() {
            return mService.get().getPath();
        }
        public long getAudioId() {
            return mService.get().getAudioId();
        }
        public long position() {
            return mService.get().position();
        }
        public long duration() {
            return mService.get().duration();
        }
        public long seek(long pos) {
            return mService.get().seek(pos);
        }
        public void shuffleAuto() {
        	mService.get().shuffleAuto();
        }
        public void setShuffleMode(int shufflemode) {
            mService.get().setShuffleMode(shufflemode);
        }
        public int getShuffleMode() {
            return mService.get().getShuffleMode();
        }
        public int removeTracks(int first, int last) {
            return mService.get().removeTracks(first, last);
        }
        public int removeTrack(long id) {
            return mService.get().removeTrack(id);
        }
        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }
        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }
        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }
        public void setVolume(float vol) {
        	mService.get().setVolume(vol);
        }
        public float getVolume() {
        	return mService.get().getVolume();
        }
        public void mute(boolean mute) {
        	mService.get().mute(mute);
        }
        public boolean isMute() {
        	return mService.get().isMute();
        }
        public boolean isLastTrack() {
        	return mService.get().isLastTrack();
        }
        public boolean isFirstTrack() {
        	return mService.get().isFirstTrack();
        }
        public void setCurrentPlaylistStyle(int style) {
        	mService.get().setCurrentPlaylistStyle(style);
        }
        public int getCurrentPlaylistStyle() {
        	return mService.get().getCurrentPlaylistStyle();
        }
        public void registerMonitorCursor(String uri, String[] projection,
                String selection, String[] selectionArgs, String sortOrder, String sortKey, boolean alphabetSorted) {
        	mService.get().registerMonitorCursor(uri, projection, selection, selectionArgs, sortOrder, sortKey, alphabetSorted);
        }

    }


    private final IBinder mBinder = new ServiceStub(this);
}

package com.weichuang.china.music;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.china.R;

public class MusicApplication extends Application {
	private static final String TAG = "MusicApplication";

	private AlbumArtLoader mArtLoader;
	private MusicContentProvider mProvider;
	private InitWorker mInitWorker;
	private InitHandler mInitHandler;
	private boolean mIsUmounted = false;
	private int mActiveTab = -1;

	@Override
	public void onCreate() {
		Log.d(TAG, "MusicApplication ==============onCreate()");
		super.onCreate();
		mArtLoader = new AlbumArtLoader(this, R.drawable.default_albumart);
		MusicUtils.mContext = this;
		if ( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
			Log.d(TAG, "external storage is mounted, init content provider");
			init();
		}
		IntentFilter cmdFilter = new IntentFilter();
		cmdFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		cmdFilter.addAction(Intent.ACTION_MEDIA_NOFS);
		cmdFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		cmdFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		cmdFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		cmdFilter.addAction(Intent.ACTION_MEDIA_SHARED);
		cmdFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		cmdFilter.addDataScheme("file");
		registerReceiver(mStorageStatusReceiver, cmdFilter);
	}

	@Override
	public void onTerminate() {
		Log.d(TAG, "MusicApplication ================onTerminate()");
		mActiveTab = -1;
		unregisterReceiver(mStorageStatusReceiver);
		mArtLoader.stop();

		super.onTerminate();
	}

	private void init() {
		mInitWorker = new InitWorker("album art init worker");
		mInitHandler = new InitHandler(mInitWorker.getLooper());
		mInitHandler.sendEmptyMessage(0);
	}
	
	public Thread createInitThread() {
        return new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "createInitThread ==============run()");
                MusicUtils.initPlaylist(MusicApplication.this, true);
            }
        });
    }
	
	private static class InitWorker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;
        
        /**
         * Creates a worker thread with the given name. The thread
         * then runs a {@link android.os.Looper}.
         * @param name A name for the new thread
         */
        InitWorker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public Looper getLooper() {
            return mLooper;
        }
        
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
        
        public void quit() {
            mLooper.quit();
        }
    }
	
	public class InitHandler extends Handler {     
        public InitHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg)
        {
        	MusicUtils.initPlaylist(MusicApplication.this, true);
			mInitWorker.quit();
        }
    }
	
	private BroadcastReceiver mStorageStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if ( action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL) || 
        			action.equals(Intent.ACTION_MEDIA_NOFS) || 
        			action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
        			action.equals(Intent.ACTION_MEDIA_REMOVED) ||
        			action.equals(Intent.ACTION_MEDIA_SHARED) ||
        			action.equals(Intent.ACTION_MEDIA_EJECT) ) {
        		mIsUmounted = true;
	        	if ( mArtLoader != null ) {
	        		mArtLoader.stop();
	        	}
        	} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
        		if ( mIsUmounted ) {
        			init();
        		    mIsUmounted = false;
        		}
        	}
        }
    };
	
	public MusicContentProvider getProvider() {
		if(mProvider != null){
			return mProvider;
		}
		return mProvider;
		
	}
	
	public AlbumArtLoader getAlbumArtLoader() {
		return mArtLoader;
	}
	
	public int getActiveTab() {
		return mActiveTab;
	}
	
	public void setActiveTab(int tab) {
		mActiveTab = tab;
	}

}

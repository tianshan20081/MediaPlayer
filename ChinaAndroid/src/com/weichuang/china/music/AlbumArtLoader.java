 package com.weichuang.china.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler.Callback;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronously loads contact photos and maintains cache of photos.  The class is
 * mostly single-threaded.  The only two methods accessed by the loader thread are
 * {@link #cacheBitmap} and {@link #obtainPhotoIdsToLoad}. Those methods access concurrent
 * hash maps shared with the main thread.
 */
public class AlbumArtLoader implements Callback {

	private static final String TAG = "AlbumArtLoader";
    private static final String LOADER_THREAD_NAME = "AlbumArtLoaderThread";

    /**
     * Type of message sent by the UI thread to itself to indicate that some photos
     * need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some photos have
     * been loaded.
     */
    private static final int MESSAGE_PHOTOS_LOADED = 2;


    /**
     * The resource ID of the image to be used when the photo is unavailable or being
     * loaded.
     */
    private final int mDefaultResourceId;

    /**
     * Maintains the state of a particular photo.
     */
    private static class DrawableHolder {
        private static final int NEEDED = 0;
        private static final int LOADING = 1;
        private static final int LOADED = 2;

        int state;
        Drawable drawable;
    }
    
    private static class FastBitmapDrawable extends Drawable {
        private Bitmap mBitmap;
        public FastBitmapDrawable(Bitmap b) {
            mBitmap = b;
        }
        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
        @Override
        public void setAlpha(int alpha) {
        }
        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }

    /**
     * A soft cache for album arts.
     */
    private final ConcurrentHashMap<Long, DrawableHolder> mDrawableCache =
            new ConcurrentHashMap<Long, DrawableHolder>();

    /**
     * A map from ImageView to the corresponding photo ID. Please note that this
     * photo ID may change before the photo loading request is started.
     */
    private final ConcurrentHashMap<ImageView, Long> mPendingRequests =
            new ConcurrentHashMap<ImageView, Long>();

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading photos from the database. Created upon
     * the first request.
     */
    private LoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_PHOTOS_NEEDED at a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the image loading is paused.
     */
    private boolean mPaused;

    private final Context mContext;
    
    private BitmapDrawable mDefaultArt;

    /**
     * Constructor.
     *
     * @param context content context
     * @param defaultResourceId the image resource ID to be used when there is
     *            no photo for a contact
     */
    public AlbumArtLoader(Context context, int defaultResourceId) {
        mDefaultResourceId = defaultResourceId;
        mContext = context;
        initDefaultArtwork(mContext);
    }
    
    /**
     * Load photo into the supplied image view.  If the photo is already cached,
     * it is displayed immediately.  Otherwise a request is sent to load the photo
     * from the database.
     */
    public void setAlbumArt(ImageView view, long id) {
        if (id < 0 ) {
            // No photo is needed
        	view.setImageDrawable(mDefaultArt);
            mPendingRequests.remove(view);
        } else {
            boolean loaded = loadCachedArt(view, id);
            if (loaded) {
                mPendingRequests.remove(view);
            } else {
                mPendingRequests.put(view, id);
                if (!mPaused) {
                    // Send a request to start loading photos
                    requestLoading();
                }
            }
        }
    }
    
    private Drawable getAlbumArt(Context context, long artIndex, BitmapDrawable defaultArt) {
        Drawable d = null;
        final Bitmap icon = defaultArt.getBitmap();
        int w = icon.getWidth();
        int h = icon.getHeight();
        Bitmap b = MusicUtils.getArtworkQuick(context, artIndex, w, h);
        if (b != null) {
            d = new FastBitmapDrawable(b);
        }
        return d;
    }
    
    private void initDefaultArtwork(Context context) {
    	Bitmap b = BitmapFactory.decodeResource(context.getResources(), mDefaultResourceId);
    	mDefaultArt = new BitmapDrawable(context.getResources(), b);
		 //no filter or dither, it's a lot faster and we can't tell the
		 //difference
    	mDefaultArt.setFilterBitmap(false);
    	mDefaultArt.setDither(false);
    }
    
    /**
     * Checks if the photo is present in cache.  If so, sets the photo on the view,
     * otherwise sets the state of the photo to {@link BitmapHolder#NEEDED} and
     * temporarily set the image to the default resource ID.
     */
    private boolean loadCachedArt(ImageView view, long id) {
        DrawableHolder holder = mDrawableCache.get(id);
        if (holder == null) {
            holder = new DrawableHolder();
            mDrawableCache.put(id, holder);
        } else if (holder.state == DrawableHolder.LOADED) {
            // Null bitmap reference means that database contains no bytes for the photo
            if (holder.drawable == null) {
            	view.setImageDrawable(mDefaultArt);
            	return true;
            }
            Drawable drawable = holder.drawable;
            if ( drawable != null ) {
            	view.setImageDrawable(drawable);
            	return true;
            }

            // Null bitmap means that the soft reference was released by the GC
            // and we need to reload the photo.
            holder.drawable = null;
        }
        // The bitmap has not been loaded - should display the placeholder image.
        view.setImageDrawable(mDefaultArt);
        holder.state = DrawableHolder.NEEDED;
        return false;
    }

    /**
     * Stops loading images, kills the image loader thread and clears all caches.
     */
    public void stop() {
        pause();

        if (mLoaderThread != null) {
            mLoaderThread.quit();
            mLoaderThread = null;
        }

        //TODO: how to clean the cache, when how to init the cache?
        mPendingRequests.clear();
        mDrawableCache.clear();
    }

    public void clear() {
        mPendingRequests.clear();
        mDrawableCache.clear();
    }

    public void clearCache() {
    	mDrawableCache.clear();
    }
    
    /**
     * Temporarily stops loading photos from the database.
     */
    public void pause() {
        mPaused = true;
    }

    /**
     * Resumes loading photos from the database.
     */
    public void resume() {
        mPaused = false;
        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Sends a message to this thread itself to start loading images.  If the current
     * view contains multiple image views, all of those image views will get a chance
     * to request their respective photos before any of those requests are executed.
     * This allows us to load images in bulk.
     */
    private void requestLoading() {
        if (!mLoadingRequested) {
            mLoadingRequested = true;
            mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
        }
    }

    /**
     * Processes requests on the main thread.
     */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING: {
                mLoadingRequested = false;
                if (!mPaused) {
                    if (mLoaderThread == null) {
                        mLoaderThread = new LoaderThread();
                        mLoaderThread.start();
                    }

                    mLoaderThread.requestLoading();
                }
                return true;
            }

            case MESSAGE_PHOTOS_LOADED: {
                if (!mPaused) {
                    processLoadedImages();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Goes over pending loading requests and displays loaded photos.  If some of the
     * photos still haven't been loaded, sends another request for image loading.
     */
    private void processLoadedImages() {
        Iterator<ImageView> iterator = mPendingRequests.keySet().iterator();
        while (iterator.hasNext()) {
            ImageView view = iterator.next();
            long id = mPendingRequests.get(view);
            if ( mPaused ) {
            	break;
            }
            boolean loaded = loadCachedArt(view, id);
            if (loaded) {
                iterator.remove();
            }
        }

        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }
    
    /**
     * Stores the supplied bitmap in cache.
     */
    private void cacheDrawable(long id, Drawable d) {
        if (mPaused) {
            return;
        }
        DrawableHolder holder = new DrawableHolder();
        holder.state = DrawableHolder.LOADED;
        if (d != null) {
        	holder.drawable = d;
        }
        mDrawableCache.put(id, holder);
    }

    /**
     * Populates an array of photo IDs that need to be loaded.
     */
    private void obtainArtIdsToLoad(ArrayList<Long> artWorkIds) {
    	artWorkIds.clear();
        /*
         * Since the call is made from the loader thread, the map could be
         * changing during the iteration. That's not really a problem:
         * ConcurrentHashMap will allow those changes to happen without throwing
         * exceptions. Since we may miss some requests in the situation of
         * concurrent change, we will need to check the map again once loading
         * is complete.
         */
        Iterator<Long> iterator = mPendingRequests.values().iterator();
        while (iterator.hasNext()) {
        	Long id = iterator.next();
            DrawableHolder holder = mDrawableCache.get(id);
            if (holder != null && holder.state == DrawableHolder.NEEDED) {
                // Assuming atomic behavior
                holder.state = DrawableHolder.LOADING;
                artWorkIds.add(id);
            }
        }
    }
    
    public interface ArtWorkLoadedListener {
    	public void onArtWorkLoaded(ImageView iv, Bitmap bm);
    }
    
    /**
     * The thread that performs loading of photos from the database.
     */
    private class LoaderThread extends HandlerThread implements Callback {
        private ArrayList<Long> mArtIds = Lists.newArrayList();
        private Handler mLoaderThreadHandler;

        public LoaderThread() {
            super(LOADER_THREAD_NAME);
        }

        /**
         * Sends a message to this thread to load requested photos.
         */
        public void requestLoading() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
            mLoaderThreadHandler.sendEmptyMessage(0);
        }
        
        /**
         * Receives the above message, loads photos and then sends a message
         * to the main thread to process them.
         */
        public boolean handleMessage(Message msg) {
        	  getAlbumArts();
            mMainThreadHandler.sendEmptyMessage(MESSAGE_PHOTOS_LOADED);
            return true;
        }
        
        private void getAlbumArts() {
        	obtainArtIdsToLoad(mArtIds);
        	ArrayList<Long> temp = new ArrayList<Long>();
            int count = mArtIds.size();
            if (count == 0) {
                return;
            }
            for ( int i = 0; i < count; i++ ) {
            	Long id = mArtIds.get(i);
            	Drawable d = getAlbumArt(mContext, id, mDefaultArt);
            	cacheDrawable(id, d);
            	temp.add(id);
            }
            //TODO: remove below?
            for ( int i = 0; i < temp.size(); i++ ) {
            	mArtIds.remove(temp.get(i));
            }
        }
    }
}

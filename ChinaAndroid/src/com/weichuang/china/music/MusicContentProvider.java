/**
 * <b>File Name:</b>MusicContentProvider.java<br />
 *
 * <b>Description:</b><br />
 * This is a MusicContentProvider, which provider function to activity and
 * service to connect with database. <br />
 *
 * <b>History:
 * 2009-07 - Consuela- created </b>
 * 2009-10-21 - Consuela - 
 * Remove unused import
 * Delete commented code.
 * Fixed and test for search from mediastore with chinese.
 * Delete some useless log info.
 * Fix a bug which will throw Exception if there is a quote in SQL
 * 2009-10-21 - stephen - update the function createRemoteTrackFromSinaMusic by stephen he , when the songs is downloading , will return the downloadedpercent.
 * 2009-10-22 - Consuela - update get all songs and sort function,delete added three field data in table trackindex.
 * 2009-10-22 - Consuela - update get record sound in imusic library --all Songs.
 * 2009-10-22 - Consuela - make sure for bugs with order by create time.
 * 2009.10.22 - Consuela - update the saveOneTrack for updateDeleteFlag
 * 2009.10.22 - Consuela - update sortTracks&albumTracks&artistTrack.
 * 2009-10-23 - Consuela - update Get Most Favourite PlayList Tracks.
 * 2009-10-26 - Consuela - Infactor Sychronize to MS and some function about it.
 * 2009-10-27 - Consuela - Added deleteRemoteTrack for favorite request from hukai.
 * 2009-10-27 - Consuela - Fixed bugs for 12002 record sound info.
 * 2009-11-03 - Consuela - 
 * Added Synchronize Downloaded Track when need to synchronize. 
 * Update Track class about type .
 * 
 * 2009-11-03 - Consuela - 
 * Added update properties file data when update download track info.
 * 2009-11-03 - Consuela - 
 * Complete for properties file data when update download track info.
 * Copyright (c) 2009 VanceInfo Ltd. <br /> All Rights Reserved.
 *
 * @author Johnny & consuela.
 * @version 1.0 <br />
 */
package com.weichuang.china.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;


public class MusicContentProvider {
    private static final String TAG = "MusicContentProvider";
    private static MusicContentProvider sInstance;
    private Context mContext;
    private ContentResolver mContentResolver;

    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
    
    private static final Uri SONGS_URI = Uri.parse("content://media/external/audio/media/sort");
    private static final String SONGS_SORT_KEY = "sort_key";
    
    private static final Uri ARTISTS_URI = Uri.parse("content://media/external/audio/artists/sort");
    private static final String ARTISTS_SORT_KEY = "artist_sort_key";

    private HashMap<Integer, Integer> msTracksIndex;
    private HashMap<Integer, Integer> smTracksIndex;
    private HashMap<Integer, Float> tracksRating;
    private HashMap<Long,String> albumArt;
    private List<Track> mAllTracks;
    private List<Track> mAllRecordings;
    private Cursor mSongsCursor;
    private OnSongsQueryCompleteListener mSongsQueryCompleteListener;
    private Cursor mArtistsCursor;
    private Cursor mMyfavouriteCursor;
    private boolean mReloadSongs = true;
    private boolean mReloadArtists = true;
    private boolean mReloadMyfavourite = true;
    private boolean mReloadRecordings = true;
    private boolean mReloadAlbumSongs = true;
    private boolean mReloadAlbums = true;
    private ProgressDialog mLoadingDialog;

    
    String[] mSongsCursorCols = new String[] {
			MediaStore.Audio.Media._ID, // index must match IDCOLIDX below
			MediaStore.Audio.Media.ARTIST, 
			MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.TITLE, 
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.AudioColumns.ALBUM_ID, 
			SONGS_SORT_KEY,
    };
    
    String[] mArtistsCursorCols = new String[] {
			MediaStore.Audio.Artists._ID,            // index must match IDCOLIDX below
			MediaStore.Audio.Artists.ARTIST,
			MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
			MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
			ARTISTS_SORT_KEY,
    };
    
    private class InitPlaylistTask extends AsyncTask {
		@Override
		protected Object doInBackground(Object... params) {
			
			 initAlbumArt();
			return null;
		}
		@Override
		protected void onPostExecute(Object result) {
			Log.d(TAG, "<<<<<onPostExecute>>>>>");
			
			super.onPostExecute(result);
			
		}
	}

    public void setReloadSongs(boolean reload) {
    	mReloadSongs = reload;
    }
    public boolean needReloadSongs() {
    	return mReloadSongs;
    }
    public void setReloadArtists(boolean reload) {
    	mReloadArtists = reload;
    }
    public boolean needReloadArtists() {
    	return mReloadArtists;
    }
    public void setReloadRecordings(boolean reload) {
    	mReloadRecordings = reload;
    }
    public boolean needReloadRecordings() {
    	return mReloadRecordings;
    }
    public void setReloadMyfavourite(boolean reload) {
    	mReloadMyfavourite = reload;
    }
    public boolean needReloadMyfavourite() {
    	return mReloadMyfavourite;
    }
    public void setReloadAlbumSongs(boolean reload) {
    	mReloadAlbumSongs = reload;
    }
    public boolean needReloadAlbumSongs() {
    	return mReloadAlbumSongs;
    }

	private ProgressDialog createDialog(Context ctx, String title, String msg,
			OnCancelListener listener) {
		ProgressDialog dialog = new ProgressDialog(ctx);
		dialog.setMessage(msg);
		dialog.setTitle(title);
		dialog.setOnCancelListener(listener);
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		return dialog;
	}
    
    private class GetSongsCursorTask extends AsyncTask implements OnCancelListener{
    	
		public void onCancel(DialogInterface dialog) {
			cancel(true);
		}
		@Override
		protected void onPreExecute() {
			Resources res = mContext.getResources();
			super.onPreExecute();
		}
		@Override
		protected Object doInBackground(Object... params) {
			getSongsCursor();
			return null;
		}
		@Override
		protected void onPostExecute(Object result) {
			if ( mSongsQueryCompleteListener != null ) {
				mSongsQueryCompleteListener.onSongsQueryComplete(mSongsCursor);
			}
			if (mLoadingDialog != null && mLoadingDialog.isShowing()) { 
				mLoadingDialog.dismiss();
			}
			super.onPostExecute(result);
			
		}
	}
    
    public void registerSongsQueryCompleteListener(OnSongsQueryCompleteListener l) {
    	mSongsQueryCompleteListener = l;
    }
    
    public interface OnSongsQueryCompleteListener {
    	public void onSongsQueryComplete(Cursor cursor);
    }
    
    public void QuerySongs() {
    	if ( mReloadSongs ) {
    		new GetSongsCursorTask().execute();
    	} else {
    		if ( mSongsQueryCompleteListener != null ) {
    			Log.d(TAG, "do not need to reload songs, deactive=" + mSongsCursor.isClosed());
				mSongsQueryCompleteListener.onSongsQueryComplete(mSongsCursor);
			}
    	}
    }
    
    public Cursor getSongsCursor() {
		Cursor cursor = null;
		cursor = MusicUtils.query(mContext, SONGS_URI, mSongsCursorCols,
				null, null, SONGS_SORT_KEY + " COLLATE NOCASE");
		if ( cursor != null ) {			
			mSongsCursor = new AlphabetSortCursor(cursor, SONGS_SORT_KEY);
			mReloadSongs = false;
		} else {
			mSongsCursor = null;
		}
    	return mSongsCursor;
    }
    
    public Cursor getArtistsCursor() {
    	if ( mReloadMyfavourite ) {
			Cursor cursor = null;
			cursor = MusicUtils.query(mContext, ARTISTS_URI, mArtistsCursorCols,
					null, null, ARTISTS_SORT_KEY + " COLLATE NOCASE");
			if ( cursor != null ) {			
				mArtistsCursor = new AlphabetSortCursor(cursor, ARTISTS_SORT_KEY);
				mReloadArtists = false;
			} else {
				mArtistsCursor = null;
			}
    	}
    	return mArtistsCursor;
    }    
    
//    public Cursor getMyfavouriteCursor() {
//    	if ( mReloadArtists ) {
//			Cursor cursor = null;
//			cursor = MusicUtils.query(mContext, ARTISTS_URI, mArtistsCursorCols,
//					null, null, ARTISTS_SORT_KEY + " COLLATE NOCASE");
//			if ( cursor != null ) {			
//				mArtistsCursor = new AlphabetSortCursor(cursor, ARTISTS_SORT_KEY);
//				mReloadArtists = false;
//			} else {
//				mArtistsCursor = null;
//			}
//    	}
//    	return mArtistsCursor;
//    }
   
    
  public void initAlbumArt() {    	
	Cursor c = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums._ID}, null, null, null);
	if(c != null) {
		int size = c.getCount();
		long [] albumId = new long[size];    		   		
		int i = 0;
		while(c.moveToNext()) {
			albumId[i] = c.getInt(0);
			i++;
		}
		c.close();
		albumArt = new HashMap<Long,String>(size);    		
		Uri uri;
		for (i=0;i< albumId.length;i++) {
			if (albumId[i] == -1) {
				continue;
			}
    		uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId[i]);
    		final Cursor cc = mContentResolver.query(uri, new String[]{"_data"}, null, null, null);
    		if(cc != null) {
    			if(cc.getCount() == 1 ) {
    				cc.moveToFirst();        				
        			albumArt.put(albumId[i], cc.getString(0));
//        			getArtwork(mContext, albumId[i], mDefaultAlbum);
        			Log.d(TAG, "Key :" + albumId[i] + "value:" + cc.getString(0));
    			}        			
    			cc.close();         			
    		}
    		       		
    	} 
		
	}
}

	public static MusicContentProvider getInstance(Context ctx) {
		synchronized (TAG) {
			if (sInstance == null) {
				Log.d(TAG, "intance is null, new it****************");
				sInstance = new MusicContentProvider(ctx);
			}
			return sInstance;
		}		
	}
	
    private MusicContentProvider(Context ctx) {
        mContext = ctx;
        mContentResolver = mContext.getContentResolver();
    }
    
    
    
//    public void init() {
//        getAllMSTrackIndex();
//        getAllSMTrackIndex();
//        getAllTrackRating();
//    }
    public void clearMemo() {
    	if(msTracksIndex != null) msTracksIndex.clear();
    	if(smTracksIndex != null) smTracksIndex.clear();
    	if(tracksRating != null) tracksRating.clear();
    	if(albumArt != null ) albumArt.clear();
    }
    
    private Set<Long> getPlaylistIds() {
    	Set<Long> playlist = new HashSet<Long>();
    	long playlist_id = MusicUtils.getPlaylistId(mContext, false);
    	if ( playlist_id > 0 ) {
    		final String[] ccols = new String[] { MediaStore.Audio.Playlists.Members.AUDIO_ID };
    		//NOTE: the sort cursor should be same with the PlaylistActivity
            Cursor playlistCursor = MusicUtils.query(mContext, MediaStore.Audio.Playlists.Members.getContentUri("external", playlist_id),
                    ccols, null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
            if ( playlistCursor != null ) {
                int len = playlistCursor.getCount();
//                long [] list = new long[len];
                playlistCursor.moveToFirst();
                int colidx = -1;
                try {
                    colidx = playlistCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
                } catch (IllegalArgumentException ex) {
                    colidx = playlistCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                }
                for (int i = 0; i < len; i++) {
                    playlist.add(playlistCursor.getLong(colidx));
                    playlistCursor.moveToNext();
                }
        	}
    	}
    	return playlist;
    }
    
    public List<Track> getAllTracks() {
    	if (mReloadSongs || mAllTracks == null) {
    		mAllTracks = getAllTracksInternal();
    		mReloadSongs = false;
    		return mAllTracks;
    	}
    	return mAllTracks;
    }
    private List<Track> getAllTracksInternal() {
    	Uri uri = Uri.parse("content://media/external/audio/media/sort");
        final List<Track> tracks = new ArrayList<Track>(0);
        final Cursor c = mContentResolver.query(uri,
                new String[] { MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.MIME_TYPE,
                        MediaStore.Audio.AudioColumns.DATE_ADDED,
                        MediaStore.Audio.AudioColumns.ALBUM_ID,
                        MediaStore.Audio.AudioColumns.ARTIST_ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        "sort_key",
                        }, null, null, "sort_key COLLATE NOCASE");
        if(c != null) {
        	Set<Long> playlist = getPlaylistIds();
        	while (c.moveToNext()) {
        		Track track = getTrackFromCursor(c);
        		track.setMyFavouriteFlag(playlist.contains(track.getTrackId()));
        		tracks.add(track);    
        	}
            c.close();
           
        }
        return sortAllTracks(tracks);
    }
    public List<Track> getTracksByAlbum(long album_id, long artist_id) {
    	Uri uri = Uri.parse("content://media/external/audio/media/sort");
    	String where = MediaStore.Audio.Media.ALBUM_ID + "=" + album_id + " AND "
					+ MediaStore.Audio.Media.ARTIST_ID + "=" + artist_id;
        final List<Track> tracks = new ArrayList<Track>(0);
        final Cursor c = mContentResolver.query(uri,
                new String[] { MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.MIME_TYPE,
                        MediaStore.Audio.AudioColumns.DATE_ADDED,
                        MediaStore.Audio.AudioColumns.ALBUM_ID,
                        MediaStore.Audio.AudioColumns.ARTIST_ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        "sort_key",
                        }, where, null, "sort_key COLLATE NOCASE");
        if(c != null) {
        	Set<Long> playlist = getPlaylistIds();
        	while (c.moveToNext()) {
        		Track track = getTrackFromCursor(c);
        		track.setMyFavouriteFlag(playlist.contains(track.getTrackId()));
        		tracks.add(track);    
        	}
            c.close();
           
        }
        return sortAllTracks(tracks);
//        return tracks;
    }
    
    public List<Track> getTracksByRecordings() {
    	if ( mReloadRecordings || mAllRecordings == null ) {
    		mAllRecordings = getTracksByRecordingsInternal();
    		mReloadRecordings = false;
    		return mAllRecordings;
    	}
    	return mAllRecordings;
    }
    private List<Track> getTracksByRecordingsInternal() {
    	String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
    	final String where = MediaStore.Audio.AudioColumns.MIME_TYPE + "='"
        		+ "audio/amr" + "'" + "AND " + "_data " +  " LIKE '" 
        		+ sdcard + "/Record" + "%'";
        final List<Track> tracks = new ArrayList<Track>(0);
        final Cursor c = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.MIME_TYPE,
                        MediaStore.Audio.AudioColumns.DATE_ADDED,
                        MediaStore.Audio.AudioColumns.ALBUM_ID,
                        MediaStore.Audio.AudioColumns.ARTIST_ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        }, where, null, MediaStore.Audio.AudioColumns.DATE_ADDED + " DESC");
        if(c != null) {
        	Set<Long> playlist = getPlaylistIds();
        	while (c.moveToNext()) {
        		Track track = getTrackFromCursor(c);
        		track.setMyFavouriteFlag(playlist.contains(track.getTrackId()));
        		tracks.add(track);    
        	}
            c.close();
           
        }
        return tracks;
    }
    private Track getTrackFromCursor(Cursor c) {
        final Track track = new Track();
        final long trackId = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns._ID));        
//        int id = getIdFromMediaStoreMemo(trackId);
        
        if(trackId != -1) {
            track.setTrackId(trackId);
        }
        String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
        track.setTrackName(MusicUtils.getSongName(mContext, name));
        name = c.getString(c.getColumnIndexOrThrow("sort_key")).trim().substring(0, 1).toUpperCase();
        if ( name == null || !isAlphabet(name.charAt(0)) ) {
        	track.setFirstAlphabet("#");
        } else {
        	track.setFirstAlphabet(name);
        }
        name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
        track.setArtistName(MusicUtils.getArtistName(mContext, name));     
        name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
        track.setAlbumName(MusicUtils.getAlbumName(mContext, name));
        final long albumId = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
        track.setAlbumId(albumId);
//        track.setIconPath(getAlbumArtPath(albumId));
        track.setPath(c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
        if(getAlbumArtPath(albumId)!=null)
        	track.setAlbumArtPath(getAlbumArtPath(albumId));
		 return track;
    }
    
    private boolean isAlphabet(char c) {
    	return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    private int getFirstAlphabetPos(List<Track> tracks) {
    	int firstalphabet = -1;
    	if ( tracks == null || tracks.size() == 0 ) 
    		return firstalphabet;
    	
    	for ( int i = 0; i < tracks.size(); i++ ) {
    		String alphabet = tracks.get(i).getFirstAlphabet();
    		Log.d(TAG, "tracks[" + i + "]" + "'s alphabet: " + alphabet);
    		if ( alphabet == null )
    			continue;
    		char firstLetter = alphabet.charAt(0);
    		if ( isAlphabet(firstLetter) ) {
    			firstalphabet = i;
    			break;
    		}
    	}
    	return firstalphabet;
    }
    private int getLastAlphabetPos(List<Track> tracks) {
    	int lastalphabet = -1;
    	if ( tracks == null || tracks.size() == 0 ) 
    		return lastalphabet;
    	
    	for ( int i = tracks.size() -1 ; i >= 0; i-- ) {
    		String alphabet = tracks.get(i).getFirstAlphabet();
    		if ( alphabet == null )
    			continue;
    		char firstLetter = alphabet.charAt(0);
    		if (  isAlphabet(firstLetter) ) {
    			lastalphabet = i;
    			break;
    		}
    	}
    	return lastalphabet;
    }
    
    private List<Track> sortAllTracks(List<Track> tracks) {
    	//TODO: optimize
    	List<Track> sortedTracks = new ArrayList<Track>();
    	Log.d(TAG, "tracks size: " + tracks.size());
    	int firstAlphabetPos = getFirstAlphabetPos(tracks);
    	int lastAlphabetPos = getLastAlphabetPos(tracks);
    	Log.d(TAG, "tracks first: " + firstAlphabetPos + " last: " + lastAlphabetPos);
    	for ( int i = firstAlphabetPos; i <= lastAlphabetPos; i++ ) {
    		sortedTracks.add(tracks.get(i));
    	}
    	for ( int i = 0; i < firstAlphabetPos; i++ ) {
    		sortedTracks.add(tracks.get(i));
    	}
    	for ( int i = lastAlphabetPos + 1; i < tracks.size(); i++ ) {
    		sortedTracks.add(tracks.get(i));
    	}
    	return sortedTracks;
    }
    private String getAlbumArtPath(long albumId) {
    	String path = null;
    	if(albumArt != null ) {
    		 path = albumArt.get(albumId);
    	}    	
    	return path;	
	}
    /**
     * Update First Time Synchronize Flag in para table.
     * 
     * @param flag
     * @return boolean.
     */
//    private boolean updateFirstTimeSynchronizeFlag(boolean flag) {
//        Log.d(TAG, "Update First Time Synchronize flag:" + flag);
//        final String where = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.FIRST_TIME_SYNCHRONIZE_ID;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.SharePara.FLAG, flag);
//        int rows = mContentResolver.update(SinaMusic.SharePara.CONTENT_URI,
//                values, where, null);
//        if (rows != 1)
//            return false;
//        return true;
//    }
//
//    
//    /**
//     * Get is First Time Synchronized Flag.
//     * 
//     * @return boolean.
//     */
//    public boolean getIsFirstTimeSynchronizeFlag() {
//        final String selection = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.FIRST_TIME_SYNCHRONIZE_ID;
//        final Cursor c = mContentResolver.query(
//                SinaMusic.SharePara.CONTENT_URI,
//                new String[] { SinaMusic.SharePara.FLAG }, selection, null,
//                null);
//        if (c == null) {
//            return false;
//        }
//
//        boolean flag = false;
//        if (c.getCount() == 1) {
//            c.moveToFirst();
//            flag = (c.getInt(0) == 0) ? false : true;
//        }
//        
//        Log.d(TAG, "Get is First Time Synchronize flag : " + flag);
//        c.close();
//        return flag;
//    }
//    /**
//     * Synchronize downloaded track from properties to sinamusic.
//     */
//    public void synchronizeToSinaMusic() {
//    	Log.d(TAG, "SynchronizeToSinaMusic");
//        List<String> paths = mFileManager.getAllDownloadPropertiesPath();
////        for(String path:paths) {
////        	Log.d(TAG, "Get all download properties path:" + path);
////        }
//        final List<RemoteTrack> remoteTracks = getAllDownloadedLocalTrackList(paths);
//        saveLocalTracks(remoteTracks);
//    }
//
//    /**
//     * Save local tracks.
//     * 
//     * @param remoteTracks
//     * @return int[] : inserted id [].
//     */
//    private int[] saveLocalTracks(List<RemoteTrack> remoteTracks) {
//        if (remoteTracks == null || remoteTracks.size() == 0) {
//            return null;
//        }
//        Log.d(TAG, "Save local tracks size is :" + remoteTracks.size() + " From properties file.");
//        int[] ids = new int[remoteTracks.size()];
//        ContentValues value;
//        int i = 0;
//        int trackId;
//		final Cursor c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI,
//				new String[] { SinaMusic.Tracks.EXTERNALID }, null, null, null);
//		List<String> externalIds = new ArrayList<String>(c.getCount());
//		while (c.moveToNext()) {
//			externalIds.add(c.getString(0));
//		}
//		c.close();
//        for (RemoteTrack remoteTrack : remoteTracks) {
//        	if(externalIds.contains(remoteTrack.getExtId())) {        	
//        		externalIds.remove((String)remoteTrack.getExtId());
//        		//to do nothing
//        	}else {
//        		//insert into track table
////        		Log.d(TAG, "Synchronized download tracks:" + remoteTrack.toString());
//                value = createTracksContentValuesByTrack(remoteTrack);
//                final Uri uri = mContentResolver.insert(
//                        MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, value);
//                trackId = Integer.parseInt(uri.getLastPathSegment());
//                int id = saveTrackIndex(trackId, remoteTrack.getRating(),
//                        remoteTrack.getType(), remoteTrack.getDownloadedTime());
//                ids[i] = id;
//                i++;
//        	}	
//        }
//        String where ;
//        for(String externalId : externalIds) {
//        	Log.d(TAG, "Delete no exsit track externalId:" + externalId );
//        	if (TextUtils.isEmpty(externalId) || externalId.equals("")) continue;
//        	where = SinaMusic.Tracks.EXTERNALID + "=" + externalId;
//        	mContentResolver.delete(SinaMusic.Tracks.CONTENT_URI, where, null);
//        	
//        }        
//        return ids;
//    }
//    /**
//     * Get all downloaded local track lists.
//     * 
//     * @param paths
//     * @return tracklist.
//     */
//    private List<RemoteTrack> getAllDownloadedLocalTrackList(List<String> paths) {
//        final List<RemoteTrack> tracks = new ArrayList<RemoteTrack>(0);
//        for (String path : paths) {
//            try {
//                RemoteTrack remoteTrack = new RemoteTrack();
//                Configuration config = new Configuration(path);
//                remoteTrack.setId(Integer.parseInt(config
//                        .getValue(SinaMusic.Tracks._ID)));
//                remoteTrack.setExtId(config
//                        .getValue(SinaMusic.Tracks.EXTERNALID));
//                remoteTrack.setUri(config.getValue(SinaMusic.Tracks.URI));
//                remoteTrack.setName(config.getValue(SinaMusic.Tracks.NAME));
//                remoteTrack.setSize(Long.parseLong(config
//                        .getValue(SinaMusic.Tracks.SIZE)));
//                remoteTrack.setDuration(Long.parseLong(config
//                        .getValue(SinaMusic.Tracks.DURATION)));
//                remoteTrack.setRating(Float.parseFloat(config
//                        .getValue(SinaMusic.Tracks.RATING)));
//                remoteTrack.setLyric(config.getValue(SinaMusic.Tracks.LYRIC));
//                remoteTrack.setPath(config.getValue(SinaMusic.Tracks.PATH));
//                remoteTrack.setAlbumName(config
//                        .getValue(SinaMusic.Tracks.ALBUM));
//                remoteTrack.setAlbumExtId(Integer.parseInt(config
//                        .getValue(SinaMusic.Tracks.ALBUMID)));
//                remoteTrack.setAlbumPhotoUri(config
//                        .getValue(SinaMusic.Tracks.ALBUMARTURL));
//                remoteTrack.setArtistExtId(Integer.parseInt(config
//                        .getValue(SinaMusic.Tracks.ARTISTID)));
//                remoteTrack.setArtistName(config
//                        .getValue(SinaMusic.Tracks.ARTIST));
//                remoteTrack.setArtistPhotoUri(config
//                        .getValue(SinaMusic.Tracks.ARTISTARTURL));
//                remoteTrack.setPath(config.getValue(SinaMusic.Tracks.PATH));
//                remoteTrack
//                        .setDeletedFlag((config
//                                .getValue(SinaMusic.Tracks.DELETEFLAG)
//                                .equals("1")) ? true : false);
//                String temp = config.getValue(SinaMusic.Tracks.DOWNLOADEDSIZE);
//                remoteTrack.setDownloadedSize(Integer.parseInt(temp == "" ? "0"
//                        : temp));
//                temp = config.getValue(SinaMusic.Tracks.DOWNLOADEDTIME);
//                remoteTrack.setDownloadedTime(Long.parseLong(temp == "" ? "0"
//                        : temp));
//                remoteTrack.setDownloadStatus(Integer.parseInt(config
//                        .getValue((SinaMusic.Tracks.DOWNLOADSTATUS))));
//                remoteTrack.setType(Integer.parseInt(config
//                        .getValue(SinaMusic.Tracks.TYPE)));
//                remoteTrack.setUserAccount(config.getValue(SinaMusic.Tracks.SINAID));
//                tracks.add(remoteTrack);
//            } catch (NumberFormatException nfe) {
//                // TODO : deal with.
//                MusicUtils
//                        .Log(TAG, "NumberFormatException:" + nfe.getMessage());
//            }
//        }
//        return tracks;
//    }
//
//    /**
//     * Synchronized with data to databases.
//     */
//    public void synchronizeWithMediaStore() {
//        synchronizeSoundRecordToTrackIndex();
//        synchronizeAudioToTrackIndex();
//    }
//
//    private void synchronizeSoundRecordToTrackIndex() {    	
//    	//get all ids from track index table where type = mediastore
//    	String selection = SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//    	final Cursor c = mContentResolver.query(SinaMusic.TracksIndex.CONTENT_URI, new String[]{SinaMusic.TracksIndex.TRACKID}, selection, null, null);
//    	List<Integer> audioIds = new ArrayList(c.getCount());
//    	while(c.moveToNext()) {
//    		audioIds.add(c.getInt(0));
//    	}
//    	c.close();   
//    	final String[] projection = new String[] {
//                MediaStore.Audio.AudioColumns._ID,
//                MediaStore.Audio.AudioColumns.DATE_ADDED };
//        selection = MediaStore.Audio.AudioColumns.MIME_TYPE + "='"
//                + MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE + "'";
//        final Cursor mediaCursor = mContentResolver.query(
//                MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, projection, selection, null,
//                null);
//        if (mediaCursor == null) {
//            Log.e(TAG, "Invalid MediaStore external table cursor: "
//                    + mediaCursor);
//            return;
//        }        
//        final ContentValues[] values = new ContentValues[mediaCursor.getCount()];
//        int i = 0;
//        int trackId = BaseEntity.INVALID_ID;
//        long createTime;
//        while (mediaCursor.moveToNext()) {
//            trackId = mediaCursor.getInt(mediaCursor
//                    .getColumnIndex(MediaStore.Audio.AudioColumns._ID));
//            if(audioIds.contains(trackId)) {        	
//        		audioIds.remove((Integer)trackId);
//        		//exsit
//        		//to do nothing.        		
//        	} else {
//        		//no exsit
//        		//insert it 
//        		createTime = mediaCursor.getLong(mediaCursor
//                        .getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//        		values[i] = new ContentValues();
//                values[i].put(SinaMusic.TracksIndex.TRACKID, trackId);
//                values[i].put(SinaMusic.TracksIndex.TYPE,
//                        MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE);
//                values[i].put(SinaMusic.TracksIndex.CREATETIME, createTime);
//                values[i].put(SinaMusic.TracksIndex.RATING, 0);
//                i++;        		
//        	} 	            
//        }
//        mediaCursor.close();
//        String where;
//        for(Integer deleteId : audioIds) {
//        	where = SinaMusic.TracksIndex.TRACKID + "=" + deleteId 
//        	+ " AND " + SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//        	mContentResolver.delete(SinaMusic.TracksIndex.CONTENT_URI, where, null);
//        }   
//        int rows = mContentResolver.bulkInsert(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values);
//        Log.d(TAG, "Synchronized " + rows + "Rows From MediaStore.");
//    }
//
//    private void synchronizeAudioToTrackIndex() {    	
//    	//get all ids from track index table where type = mediastore
//    	String selection = SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//    	final Cursor c = mContentResolver.query(SinaMusic.TracksIndex.CONTENT_URI, new String[]{SinaMusic.TracksIndex.TRACKID}, selection, null, null);
//    	List<Integer> audioIds = new ArrayList(c.getCount());
//    	while(c.moveToNext()) {
//    		audioIds.add(c.getInt(0));
//    	}
//    	c.close();   	
//        final String[] projection = new String[] {
//                MediaStore.Audio.AudioColumns._ID,
//                MediaStore.Audio.AudioColumns.DATE_ADDED };
//        selection = MediaStore.Audio.AudioColumns.MIME_TYPE
//                + "<>'" + MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE + "'";
//        final Cursor mediaCursor = mContentResolver.query(
//                MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, projection, selection, null,
//                null);
//        if(mediaCursor == null) return ;
//        final ContentValues[] values = new ContentValues[mediaCursor.getCount()];
//       
//        int i = 0;
//        int trackId = BaseEntity.INVALID_ID;
//        long createTime;
//        while (mediaCursor.moveToNext()) {
//        	trackId = mediaCursor.getInt(mediaCursor
//                    .getColumnIndex(MediaStore.Audio.AudioColumns._ID));    	
//        	if(audioIds.contains(trackId)) {        	
//        		audioIds.remove((Integer)trackId);
//        		//exsit
//        		//to do nothing.        		
//        	} else {
//        		//no exsit
//        		//insert it 
//        		createTime = mediaCursor.getLong(mediaCursor
//                        .getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//        		values[i] = new ContentValues();
//                values[i].put(SinaMusic.TracksIndex.TRACKID, trackId);
//                values[i].put(SinaMusic.TracksIndex.TYPE,
//                        MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE);
//                values[i].put(SinaMusic.TracksIndex.CREATETIME, createTime);
//                values[i].put(SinaMusic.TracksIndex.RATING, 0);
//                i++;        		
//        	} 	
//        }
//        mediaCursor.close();
//        String where;
//        for(Integer deleteId : audioIds) {
//        	where = SinaMusic.TracksIndex.TRACKID + "=" + deleteId 
//        	+ " AND " + SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//        	mContentResolver.delete(SinaMusic.TracksIndex.CONTENT_URI, where, null);
//        }        
//        int rows = mContentResolver.bulkInsert(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values);
//        Log.d(TAG, "Synchronized " + rows + "Rows From MediaStore.");     
//    }
//
//    /**
//     * Save one track to database track table.<br />
//     * 
//     * @param track
//     * @return inserted return _ID. If this track is exsit,return _ID.
//     */
//    public int saveOneTrack(RemoteTrack track) {
//    	Log.d(TAG, "saveOneTrack");
//        ContentValues values = new ContentValues();
//        int track_id = BaseEntity.INVALID_ID;
//        if (track == null) {
//            return track_id;
//        }
//        if (TextUtils.isEmpty(track.getExtId())) {
//            return track_id;
//        }
//        String selection = SinaMusic.Tracks.EXTERNALID + "=" + track.getExtId();
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null) {
//            Log.e(TAG, "Invalid track table cursor: " + c);
//            return track_id;
//        }
//        if (c.getCount() == 0) {
//            // close cursor first
//            c.close();
//
//            if (track.getArtistExtId() == BaseEntity.INVALID_ID) {
//                track.setArtistExtId(MusicProviderConstantValues.UNKNOWARTISTID);
//                track.setArtistName(MusicProviderConstantValues.UNKNOWARTISTNAME);
//                track.setArtistPhotoUri(MusicProviderConstantValues.UNKNOWARTISTARTURL);
//            }
//            if (track.getAlbumExtId() == BaseEntity.INVALID_ID) {
//                track.setAlbumExtId(MusicProviderConstantValues.UNKNOWALBUMID);
//                track.setAlbumName(MusicProviderConstantValues.UNKNOWALBUMNAME);
//                track.setAlbumPhotoUri(MusicProviderConstantValues.UNKNOWALBUMARTURL);
//            }
//            track.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE);
//            track.setDeletedFlag(false);
//            values = createTracksContentValuesByTrack(track);
//            final Uri uri = mContentResolver.insert(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, values);
//            track_id = Integer.parseInt(uri.getLastPathSegment());
//            values.clear();
//            int id = saveTrackIndex(track_id, track.getName(), track
//                    .getAlbumName(), track.getArtistName(), track.getRating(),
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE);
//            String fileName = track.getExtId() + MusicProviderConstantValues.FILE_PROP;
//            saveDownloadedTrackToProp(fileName, track);
//            return id;
//        } else {
//            c.moveToFirst();
//            track_id = c.getInt(c.getColumnIndexOrThrow(SinaMusic.Tracks._ID));
//            int id = getTrackIdFromIndex(track_id);
//            updateDeleteFlag(track_id, false);
//            updatePropDeleteFlag(track.getExtId(), false);
//            
//            c.close();
//            return id;
//        }
//    }
//
//    private boolean updateDeleteFlag(int trackId, boolean b) {
//        Log.d(TAG, "Update Delete Flag.");
//        if (trackId == -1) {
//            return false;
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.DELETEFLAG, b);
//        int rows = mContentResolver.update(uri, values, null, null);
//        
//        return (rows == 1);
//    }
//
//    private int getTrackIdFromIndex(int track_id) {
//
//        final String selection = SinaMusic.TracksIndex.TRACKID + "=" + track_id
//                + " AND (" + SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE + " OR "
//                + SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA + ")";
//        final Cursor sinaCursor = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        int trackindex = BaseEntity.INVALID_ID;
//        if (sinaCursor == null) {
//            Log.e(TAG, "Invalid SinaMusic TrackIndex table cursor");
//            return trackindex;
//        }
//        if (sinaCursor.getCount() == 1) {
//            sinaCursor.moveToFirst();
//            trackindex = sinaCursor.getInt(sinaCursor
//                    .getColumnIndex(SinaMusic.TracksIndex._ID));
//        }
//        sinaCursor.close();
//        return trackindex;
//    }
//
//    private int saveTrackIndex(int track_id, String trackName,
//            String albumName, String artistName, float rating, int type) {
//        if (track_id == BaseEntity.INVALID_ID) {
//            return -1;
//        }
//        String selection = SinaMusic.TracksIndex.TRACKID + "=" + track_id
//                + " AND " + SinaMusic.TracksIndex.TYPE + "=" + type;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        int id = BaseEntity.INVALID_ID;
//        int count = c.getCount();
//        if (count == 1) {
//            c.moveToFirst();
//            id = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID));
//        } else if (count == 0) {
//            final ContentValues values = new ContentValues();
//            values.put(SinaMusic.TracksIndex.TRACKID, track_id);
//            values.put(SinaMusic.TracksIndex.TYPE, type);
//            values.put(SinaMusic.TracksIndex.RATING, rating);
//            final Uri uri = mContentResolver.insert(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values);
//            id = Integer.parseInt(uri.getLastPathSegment());
//        }
//
//        c.close();
//        return id;
//    }
//
//    private int saveTrackIndex(int track_id, float rating, int type,
//            long createTime) {
//        if (track_id == BaseEntity.INVALID_ID) {
//            return -1;
//        }
//        int id = -1;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.TracksIndex.TRACKID, track_id);
//        values.put(SinaMusic.TracksIndex.TYPE, type);
//        values.put(SinaMusic.TracksIndex.RATING, rating);
//        values.put(SinaMusic.TracksIndex.CREATETIME, createTime);
//        final Uri uri = mContentResolver.insert(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values);
//        id = Integer.parseInt(uri.getLastPathSegment());
//        return id;
//    }
//    private void updateIndexType(int id, int type) {
//        final String selection = SinaMusic.TracksIndex._ID + "=" + id;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.TracksIndex.TYPE, type);
//        values.put(SinaMusic.TracksIndex.CREATETIME, System.currentTimeMillis());
//        mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values,
//                selection, null);
//    }
//
//    public LocalTrack getLocalTrackFromTable(int id, int trackId, int type) {
//        if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE) {
//            LocalTrack localTrack = getLocalTrackFromMediaStoreById(trackId);
//            if (localTrack == null) {
//                return null;
//            }
//            localTrack.setId(id);
//            return localTrack;
//        } else if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) {
//            LocalTrack localTrack = getLocalTrackFromSinaMusicById(trackId);
//            if (localTrack == null) {
//                return null;
//            }
//            localTrack.setId(id);
//            return localTrack;
//        }
//        return null;
//    }
//
//    private List<LocalTrack> getAllMediaStoreTracks() {
//        final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                new String[] { MediaStore.Audio.AudioColumns._ID,
//                        MediaStore.Audio.AudioColumns.TITLE,
//                        MediaStore.Audio.AudioColumns.ARTIST,
//                        MediaStore.Audio.AudioColumns.MIME_TYPE,
//                        MediaStore.Audio.AudioColumns.DATE_ADDED,
//                        MediaStore.Audio.AudioColumns.ALBUM_ID,
//                        MediaStore.Audio.AudioColumns.DATA
//                        }, null, null,
//                null);
//        if(c != null) {
//        	while (c.moveToNext()) {
//        		localTracks.add(createMiniLocalTrackFromMediaStore(c));    
//        	}
//            c.close();
//           
//        }
//        return localTracks;
//    }
//
//    private List<LocalTrack> getAllMediaStoreTracksOld() {
//        final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE + " OR "
//                + SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        int id;
//        int trackid;
//        LocalTrack localTrack;
//        float rating;
//        while (c.moveToNext()) {
//            id = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID));
//            rating = c.getFloat(c.getColumnIndex(SinaMusic.TracksIndex.RATING));
//            trackid = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TRACKID));
//            localTrack = getLocalTrackFromMediaStoreById(trackid);
//            if (localTrack == null)
//                continue;
//            localTrack.setId(id);
//            localTrack.setRating(rating);
//            localTracks.add(localTrack);
//        }
//        c.close();
//        return localTracks;
//    }
//    private float getRatingById(int id) {
//        if (id == BaseEntity.INVALID_ID)
//            return 0;
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor c = mContentResolver
//                .query(uri, new String[] { SinaMusic.TracksIndex.RATING },
//                        null, null, null);
//        if (c == null || c.getCount() == 0)
//            return 0;
//        c.moveToFirst();
//        final float rating = c.getFloat(0);
//        return rating;
//    }
//    private List<LocalTrack> getAllDownloadedTracks() {
//        final List<LocalTrack> localTrack = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA + " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks._ID,
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.RATING,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.TYPE,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//        		projection, selection, null,
//                SinaMusic.Tracks.TRACK_NAME_ASC_SORT_ORDER);
//        if( c != null) {
//        	 LocalTrack track;             
//             while (c.moveToNext()) {
//                 track = createMiniLocalTrackFromSinaMusic(c);
//                 localTrack.add(track);
//             }
//             c.close();
//        }       
//        Log.d(TAG, "Get All Downloaded Tracks : Size :" + localTrack.size());
//        return localTrack;
//    }
//
//    private LocalTrack createMiniLocalTrackFromSinaMusic(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        int trackId = c.getInt(0);
//        track.setName(c.getString(1));
//        int id = getIdFromSinaMusicMemo(trackId);
//        if(id == -1) {
//            id = getTrackIdFromIndex(trackId,
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);        }
//        track.setId(id);
//        track.setRating(c.getFloat(2));
//        String albumart = c.getString(3);
//        track.setAlbumPhotoUri(albumart);
//        track.setArtistName(c.getString(4));
//        int type = c.getInt(5);
//        track.setType(type);
//        if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE) {
//            track.setIsFromMediaStore(true);
//        } else if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) {
//            track.setIsDownloadFormSina(true);
//        }
//        if(TextUtils.isEmpty(albumart)) {
//        	track.setIconPath(c.getString(6));
//        }else {
//        	track.setIconPath(albumart);
//        }
//        
//        return track;
//    }
//    
//    private int getIdFromSinaMusicMemo(int trackId) {       
//        if (smTracksIndex == null)
//            return -1;
//        if (!smTracksIndex.containsKey(trackId)) {
//            return -1;
//        }
//        Integer o = smTracksIndex.get((int) trackId);
//        return o;
//    }
//    private int getTrackIdFromIndex(int id, int type) {
//        final String selection = SinaMusic.TracksIndex.TRACKID + "=" + id
//                + " AND " + SinaMusic.TracksIndex.TYPE + "=" + type;
//        final Cursor sinaCursor = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, new String []{SinaMusic.TracksIndex._ID}, selection, null, null);
//        int trackindex = BaseEntity.INVALID_ID;
//        if(sinaCursor != null) {
//        	if (sinaCursor.getCount() >= 1) {
//                sinaCursor.moveToFirst();
//                trackindex = sinaCursor.getInt(0);
//            }
//        	sinaCursor.close();
//        }        
//        return trackindex;
//    }
//    private int getIdFromIndex(int id) {
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor sinaCursor = mContentResolver.query(uri, null, null, null,
//                null);
//        int trackindex = BaseEntity.INVALID_ID;
//        if (sinaCursor != null) {
//        	if (sinaCursor.getCount() == 1) {
//                sinaCursor.moveToFirst();
//                trackindex = sinaCursor.getInt(sinaCursor
//                        .getColumnIndex(SinaMusic.TracksIndex.TRACKID));
//            }
//            sinaCursor.close();
//        }       
//        return trackindex;
//    }
//
//    /**
//     * Get track by _ID.
//     * 
//     * @param id
//     * @return track
//     */
//    public Track getTrackById(int id) {
//    	Track track = null;
//        Log.d(TAG, "Get Track By Id : " + id);
//        if (id == BaseEntity.INVALID_ID)
//            return track;
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor c = mContentResolver.query(uri, null, null, null, null);
//        if (c != null) {
//        	if(c.getCount() > 0) {
//        		c.moveToFirst();
//                final int type = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TYPE));
//                final int trackId = c.getInt(c
//                        .getColumnIndex(SinaMusic.TracksIndex.TRACKID));
//                final float rating = c.getFloat(c.getColumnIndex(SinaMusic.TracksIndex.RATING));
//                c.close();
//                switch (type) {
//        		case MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE:
//        			RemoteTrack remoteTrack = getRemoteTrackFromSinaMusicById(trackId);
//                    if (remoteTrack != null) {
//                    	remoteTrack.setId(id);
//                        remoteTrack.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE);
//                        track = remoteTrack;
//                    }   
//        			break;
//        		case MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE:
//        			LocalTrack localTrack = getLocalTrackFromMediaStoreById(trackId);
//                    if (localTrack != null) {
//                    	localTrack.setId(id);
//                        localTrack.setRating(rating);
//                        localTrack.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE);
//                        track = localTrack;
//                    }            
//        			break;
//        		case MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA:
//        			LocalTrack smLocalTrack = getLocalTrackFromSinaMusicById(trackId);
//                    if (smLocalTrack != null) {
//                    	 smLocalTrack.setId(id);
//                         smLocalTrack.setRating(rating);
//                         smLocalTrack.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//                         track = smLocalTrack;
//                    }     
//        			break;
//        		case MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND:
//        			LocalTrack rsTrack = getLocalTrackFromMediaStoreById(trackId);
//                    if (rsTrack != null) {
//                    	 rsTrack.setId(id);
//                         rsTrack.setRating(rating);
//                         rsTrack.setIsRecordSoundTrack(true);
//                         rsTrack.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND);
//                         track = rsTrack;
//                    }
//        			break;
//        		case MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE_INTERNAL:
//        			LocalTrack rsTrackInternal = getLocalTrackFromMediaStoreInternalById(trackId);
//                    if (rsTrackInternal != null) {
//                    	rsTrackInternal.setId(id);
//                    	rsTrackInternal.setRating(rating);
//                    	rsTrackInternal.setType(MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE_INTERNAL);
//                        track = rsTrackInternal;
//                    }
//        			break;
//        		default:
//        			//TODO
//        			break;
//        		}
//        	}
//        }
//        
//        return track;
//    }
//
//    private LocalTrack getLocalTrackFromMediaStoreInternalById(int trackId) {
//    	LocalTrack track = new LocalTrack();
//        final String selection = MediaStore.Audio.AudioColumns._ID + "=" + trackId;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI_INTERNAL,
//                null, selection, null, null);
//        if(c == null) return track;
//        if (c.getCount() == 0) {
//            c.close();
//            return null;
//        }
//        c.moveToFirst();
//        track = createlocalTrackFromMediaStoreInternal(c);
//        c.close();
//        return track;
//	}
//    
//	private LocalTrack getLocalTrackFromSinaMusicById(int trackId) {
//        LocalTrack track = new LocalTrack();
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final Cursor c = mContentResolver.query(uri, null, null, null, null);
//        if(c != null) {
//        	if(c.getCount() > 0 ) {
//        		c.moveToFirst();
//                track = createLocalTrackFromSinaMusic(c);
//                c.close();
//        	}
//        }        
//        return track;
//    }   
//    /**
//     * Get all local tracks from mediastore and downloaded tracks .
//     * 
//     * @return Track List.
//     */
//    private void getAllMSTrackIndex() {
//        msTracksIndex = new HashMap<Integer, Integer>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE + " OR " + SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//        final Cursor c = mContentResolver.query(
//        MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        if (c == null || c.getCount() == 0) return;
//        while (c.moveToNext()) {
//            msTracksIndex.put(c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TRACKID)), c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID)));
//        }
//        c.close();
//    }
//    
//    private void getAllSMTrackIndex() {
//        smTracksIndex = new HashMap<Integer, Integer>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        final Cursor c = mContentResolver.query(
//        MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        if (c == null || c.getCount() == 0) return;
//        while (c.moveToNext()) {
//            smTracksIndex.put(c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TRACKID)), c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID)));
//        }       
//        c.close();
//    }
//    
//    private void getAllTrackRating() {
//        tracksRating = new HashMap<Integer, Float>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "<>" + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE;
//        final Cursor c = mContentResolver.query(
//        MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        if (c == null || c.getCount() == 0) return;
//        while (c.moveToNext()) {
//            tracksRating.put(c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID)), c.getFloat(c.getColumnIndex(SinaMusic.TracksIndex.RATING)));
//        }       
//        c.close();
//    }
//    public List<LocalTrack> getAllLocalTracks() {
//    	init();
//        Log.d(TAG, "Get all local tracks!");    
//        List<LocalTrack> mAllLocalTracks = new ArrayList<LocalTrack>(0);        
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//            for (LocalTrack localTrack : getAllDownloadedTracks()) {
//                mAllLocalTracks.add(localTrack);
//            }
//          if (getIsScannerFinished()) {               
//                for (LocalTrack localTrack : getAllMediaStoreTracks()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//          }
//        } else {
//          if (getIsScannerFinished()) {
//                Log.d(TAG, "Get tracks from MS");
//                for (LocalTrack localTrack : getAllDownloadedTracks()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//                for (LocalTrack localTrack : getAllMediaStoreTracks()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//          }
//        }
//        mAllLocalTracks = sortTracks(mAllLocalTracks);
//        return mAllLocalTracks;
//    }
//
//    public List<LocalTrack> getAllLocalTracksOld() {
//        Log.d(TAG, "Get all local tracks!");
//        List<LocalTrack> mAllLocalTracks = new ArrayList<LocalTrack>(0);
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//            for (LocalTrack localTrack : getAllDownloadedTracks()) {
//                mAllLocalTracks.add(localTrack);
//            }
//            if (getIsScannerFinished()) {
//                Log.d(TAG, "Get tracks from MS");
//                for (LocalTrack localTrack : getAllMediaStoreTracksOld()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//            }
//        } else {
//            if (getIsScannerFinished()) {
//                Log.d(TAG, "Get tracks from MS");
//                for (LocalTrack localTrack : getAllMediaStoreTracksOld()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//                for (LocalTrack localTrack : getAllDownloadedTracks()) {
//                    mAllLocalTracks.add(localTrack);
//                }
//            }
//        }
//        mAllLocalTracks = sortTracks(mAllLocalTracks);
//        return mAllLocalTracks;
//    }
//
//    public boolean getIsEjectAction() {
//    	Log.d(TAG, "Get is Eject action");
//        final String selection = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.ACTION_MEDIA_EJECT_ID;
//        final Cursor c = mContentResolver.query(
//				SinaMusic.SharePara.CONTENT_URI,
//				new String[] { SinaMusic.SharePara.FLAG }, selection, null,
//				null);
//		boolean flag = false;
//		if (c == null)
//			return flag;
//
//		if (c.moveToFirst()) {
//			flag = (c.getInt(0) == 0) ? false : true;
//			Log.d(TAG, "Get Eject action----" + flag);
//		}
//		c.close();		
//		return flag;
//    }
//
//    public boolean getIsMountedAction() {
//
//        final String selection = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.ACTION_MEDIA_MOUNTED_ID;
//        final Cursor c = mContentResolver.query(
//                SinaMusic.SharePara.CONTENT_URI,
//                new String[] { SinaMusic.SharePara.FLAG }, selection, null,
//                null);
//        boolean flag = false;
//        if (c == null)
//            return flag;
//        if(c.moveToFirst()) {
//        	flag = (c.getInt(0) == 0) ? false : true;
//            Log.d(TAG, "Get Mounted action----" + flag);
//        } 
//        return flag;
//    }
//
//    /**
//     * Get all Local albums.
//     * 
//     * @return Album List.
//     */
//    public List<Album> getAllLocalAlbums() {
//    	init();
//        Log.d(TAG, "Get all local Albums !");
//
//        List<Album> albums = new ArrayList<Album>(0);
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//        	albums.addAll(getDownloadedAlbum());
//            if (getIsScannerFinished()) {
//                for (Album album : getMediaStoreAlbum()) {
//                    albums.add(album);
//                }
//            }
//        } else {
//            if (getIsScannerFinished()) {
//            	albums.addAll(getMediaStoreAlbum());
//                for (Album album : getDownloadedAlbum()) {
//                	if(!albums.contains(album))
//                		albums.add(album);
//                }
//            }
//        }
//        if (albums != null)
//            albums = sortAlbums(albums);
//        Log.d(TAG, "All Album size is:" + albums.size());
//        return albums;
//    }
//
//    private List<Album> getMediaStoreAlbum() {
//        final List<Album> albums = new ArrayList<Album>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                new String[]{
//        		MediaStore.Audio.AudioColumns.ALBUM_ID,
//        		MediaStore.Audio.AudioColumns.ALBUM,
//        		MediaStore.Audio.AudioColumns.ARTIST
//        		}, null, null, null);
//        if (c == null) {
//            return albums;
//        }
//        Album album;
//        while (c.moveToNext()) {
//            album = createAlbumByMscursor(c);
//            if (!albums.contains(album)) {
//                albums.add(album);
//            }
//        }
//        c.close();
//        return albums;
//    }
//
//    /**
//     * Get all local artists.
//     * 
//     * @return Artist List.
//     */
//    public List<Artist> getAllLocalArtists() {
//    	init();
//        List<Artist> artists = new ArrayList<Artist>(0);
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//        	artists.addAll(getDownloadedArtist());
//            if (getIsScannerFinished()) {
//                for (Artist artist : getMSArtist()) {
//                    artists.add(artist);
//                }
//            }
//        } else {
//            if (getIsScannerFinished()) {
//            	artists.addAll(getMSArtist());
//                for (Artist artist : getDownloadedArtist()) {
//                	if(!artists.contains(artist))
//                		artists.add(artist);
//                }
//            }
//        }
//        artists = sortArtists(artists);
//        Log.d(TAG, "Get All Artist Size:" + artists.size());
//        return artists;
//    }
//
//    private List<Artist> getMSArtist() {
//        final List<Artist> artists = new ArrayList<Artist>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, null, null, null);
//        if (c == null) return artists;          
//        Artist artist;
//        while (c.moveToNext()) {
//            artist = createAristByMscursor(c);
//            if (!artists.contains(artist)) {
//                artists.add(artist);
//            }
//        }
//        c.close();
//        return artists;
//    }
//
//    private Artist createAristByMscursor(Cursor c) {
//        final Artist artist = new Artist(false);
//        int id = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID));
//        artist.setExtId(String.valueOf(id));
//        artist.setId(id);
//        String name = c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
//        artist.setName(getLocaleName(name));
//        
//        return artist;
//    }
//
//    private Artist createAristBySMcursor(Cursor c) {
//        final Artist artist = new Artist(false);
//        int id = c.getInt(c.getColumnIndex(SinaMusic.Tracks.ARTISTID));
//        artist.setExtId(String.valueOf(id));
//        artist.setId(id);
//        artist.setName(c.getString(c.getColumnIndex(SinaMusic.Tracks.ARTIST)));
//        final String path = c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.ALBUMARTURL));
//        if(TextUtils.isEmpty(path)) {
//        	artist.setIconPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.ICON)));
//        }else {
//        	 artist.setIconPath(path);
//        }
//       
//        return artist;
//    }
//
//    private List<Artist> getDownloadedArtist() {
//        final List<Artist> artists = new ArrayList<Artist>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA +  " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null || c.getCount() == 0)
//            return artists;
//        Artist artist;
//        while (c.moveToNext()) {
//            artist = createAristBySMcursor(c);
//            if (!artists.contains(artist)) {
//                artists.add(artist);
//            }
//        }
//        c.close();
//        return artists;
//    }
//
//    private List<Album> getDownloadedAlbum() {
//        final List<Album> albums = new ArrayList<Album>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA + " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null || c.getCount() == 0)
//            return albums;
//        Album album;
//        while (c.moveToNext()) {
//            album = createAlbumByCursor(c);
//            if (!albums.contains(album)) {
//                albums.add(album);
//            }
//        }
//        c.close();
//        return albums;
//    }
//
//    /**
//     * Get all albums by artist ext id.
//     * 
//     * @param String
//     *            artistExtId : this id from sina.
//     * @return Album List.
//     */
//    public List<Album> getAllAlbumsByArtistExtId(int artistExtId,
//            String artistName) {
//
//        Log.d(TAG, "Get all albums By artist id:" + artistExtId);
//        List<Album> albums = new ArrayList<Album>(0);
//		for (Album album : getAlbumsFromSM(artistExtId, artistName)) {
//			if (!albums.contains(album))
//				albums.add(album);
//		}
//		for (Album album : getAlbumsFromMD(artistExtId, artistName)) {
//			if (!albums.contains(album))
//				albums.add(album);
//		}
//        albums = sortAlbums(albums);
//        return albums;
//    }
//
//    private List<Album> getAlbumsFromMD(int artistExtId,String artistName) {
//    	if(artistName.equals(mContext.getResources().getString(R.string.track_info_unknown))) {
//    		artistName = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//    	}
//        final List<Album> albums = new ArrayList<Album>(0);
//        final String selection = MediaStore.Audio.AudioColumns.ARTIST + "='" + replaceString(artistName) + "'";
//        final Cursor msCursor = mContentResolver.query(
//                MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, null, selection, null, null);
//        Log.d(TAG, "Query MediaStore Albums by ArtistId:" + artistExtId
//                + "selection is :" + selection);
//        if (msCursor == null) {
//            return albums;
//        }
//        Album album;
//        while (msCursor.moveToNext()) {
//            album = createAlbumByMscursor(msCursor);
//            if (!albums.contains(album)) {
//                albums.add(album);
//            }
//        }
//        msCursor.close();
//        return albums;
//    }
//
//    private List<Album> getAlbumsFromSM(int artistExtId, String artistName) {
//        final List<Album> albums = new ArrayList<Album>(0);
//        String selection;
//        if(artistExtId == 0) {
//        	selection = SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }else {
//        	selection = SinaMusic.Tracks.ARTISTID + "=" + artistExtId
//            + " OR " + SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }
//        final Cursor trackCursor = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, null, selection, null, null);
//        if (trackCursor == null || trackCursor.getCount() == 0) {
//            Log.e(TAG, "Invalid track tables cursor");
//            return albums;
//        }
//        while (trackCursor.moveToNext()) {
//            final Album album = createAlbumByCursor(trackCursor);
//            if (!albums.contains(album)) {
//                albums.add(album);
//            }
//        }
//        trackCursor.close();
//        return albums;
//    }
//
//    private Album createAlbumByCursor(Cursor c) {
//        final Album album = new Album(false);
//        album.setId(Integer.parseInt(c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.ALBUMID))));
//        album.setExtId(c.getString(c.getColumnIndex(SinaMusic.Tracks.ALBUMID)));
//        album.setName(c.getString(c.getColumnIndex(SinaMusic.Tracks.ALBUM)));
//        album.setArtistName(c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.ARTIST)));
//       
//        final String path = c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.ALBUMARTURL));
//        if(TextUtils.isEmpty(path)) {
//        	album.setIconPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.ICON)));
//        }else {
//        	album.setIconPath(path);
//        }
//        return album;
//    }
//
//    private Album createAlbumByMscursor(Cursor c) {
//        final Album album = new Album(false);
//        int id = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
//        album.setExtId(String.valueOf(id));
//        album.setId(id);
//        String name = c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM));
//        album.setName(getLocaleName(name));
//        name = c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
//        album.setArtistName(getLocaleName(name));
//        album.setIconPath(getAlbumArtPath(id));
//        return album;
//    }
//
//    /**
//     * Get tracks By album extId.
//     * 
//     * @param String
//     *            extId: this id from sina.
//     * @return Track List.
//     */
//    public List<LocalTrack> getAlbumTracks(String albumId, String albumName,
//            String artist) {
//
//        List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        for (LocalTrack localTrack : getSinaAlbumTracks(albumId, albumName,
//                artist)) {
//            localTracks.add(localTrack);
//        }
//        for (LocalTrack localTrack : getMSTracksByAlbumId(albumId,albumName,artist)) {
//            localTracks.add(localTrack);
//        }
//        localTracks = sortTracks(localTracks);
//        return localTracks;
//    }
//    private List<LocalTrack> getMSTracksByAlbumId(String albumId, String albumName,
//			String artist) {
//    	if(artist.equals(mContext.getResources().getString(R.string.track_info_unknown))) {
//    		artist = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//    	}
//    	if(albumName.equals(mContext.getResources().getString(R.string.track_info_unknown))) {
//    		albumName = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//    	}
//    	 final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//         final String selection = MediaStore.Audio.AudioColumns.ALBUM + "='"
//                 + replaceString(albumName) + "'" + " AND " + MediaStore.Audio.AudioColumns.ARTIST + "='" + replaceString(artist) + "'";
//         final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                 null, selection, null, null);
//         LocalTrack track;
//         if(c == null) return localTracks;
//         while (c.moveToNext()) {
//             track = createMiniLocalTrackFromMediaStore(c);
//             localTracks.add(track);
//         }
//         c.close();
//         return localTracks;
//	}
//	public List<Integer> getAlbumTracksId(String albumId, String albumName,
//            String artist) {
//
//        List<Integer> ids = new ArrayList<Integer>(0);
//        for (int id : getSinaAlbumTracksId(albumId, albumName,
//                artist)) {
//            ids.add(id);
//        }
//        for (int id : getMSTracksIdByAlbumId(albumId,albumName,
//                artist)) {
//            ids.add(id);
//        }
//        return ids;
//    }
//    private String replaceString(String str) {
//        if (str.indexOf("'") != -1) {
//            return str.replace("'", "''");
//        }
//        return str;
//    }
//
//    private List<LocalTrack> getSinaAlbumTracks(String albumId,
//            String albumName, String artistName) { 	
//        final List<LocalTrack> tracks = new ArrayList<LocalTrack>(0);
//        final String selection ;
//        if(albumId.equals("0")) {
//        	selection = SinaMusic.Tracks.ALBUM + "='"
//            + replaceString(albumName) + "' AND " + SinaMusic.Tracks.ARTIST
//            + "='" + replaceString(artistName) + "' AND "
//            + SinaMusic.Tracks.TYPE + "="
//            + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }else {
//        	selection = SinaMusic.Tracks.ALBUMID + "=" + albumId
//            + " OR " + SinaMusic.Tracks.ALBUM + "='"
//            + replaceString(albumName) + "' AND " + SinaMusic.Tracks.ARTIST
//            + "='" + replaceString(artistName) + "' AND "
//            + SinaMusic.Tracks.TYPE + "="
//            + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }    
//        
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks._ID,
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.RATING,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.TYPE,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//        		projection, selection, null, null);
//        if (c == null) {
//            return tracks;
//        }
//        LocalTrack track;
//        while (c.moveToNext()) {
//            track = createMiniLocalTrackFromSinaMusic(c);
//            tracks.add(track);
//            Log.d(TAG, "Get Tracks By Album:" + track.toString());
//        }
//        c.close();
//        return tracks;
//    }
//    private List<Integer> getSinaAlbumTracksId(String albumId,
//            String albumName, String artistName) {
//        final List<Integer> ids = new ArrayList<Integer>(0);
//        final String selection ;
//        if(albumId.equals("0")) {
//         	selection = SinaMusic.Tracks.ALBUM + "='"
//             + replaceString(albumName) + "' AND " + SinaMusic.Tracks.ARTIST
//             + "='" + replaceString(artistName) + "' AND "
//             + SinaMusic.Tracks.TYPE + "="
//             + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//         }else {
//         	selection = SinaMusic.Tracks.ALBUMID + "=" + albumId
//             + " OR " + SinaMusic.Tracks.ALBUM + "='"
//             + replaceString(albumName) + "' AND " + SinaMusic.Tracks.ARTIST
//             + "='" + replaceString(artistName) + "' AND "
//             + SinaMusic.Tracks.TYPE + "="
//             + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//         }    
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                new String[]{SinaMusic.Tracks._ID}, selection, null, null);
//        int id;
//        while (c.moveToNext()) {
//            id = getIdFromSinaMusicMemo(c.getInt(0));
//            ids.add(id);
//        }
//        c.close();
//        return ids;
//    }
//    private List<LocalTrack> getMSTracksByAlbumId(String albumId) {
//        final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        final String selection = MediaStore.Audio.AudioColumns.ALBUM_ID + "="
//                + albumId;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        LocalTrack track;
//        if(c == null) return localTracks;
//        while (c.moveToNext()) {
//            track = createMiniLocalTrackFromMediaStore(c);
//            localTracks.add(track);
//        }
//        c.close();
//        return localTracks;
//    }
//    private List<Integer> getMSTracksIdByAlbumId(String albumId,String albumName,String artist) {
//    	
//    	if(artist.equals(mContext.getResources().getString(R.string.track_info_unknown))) {
//    		artist = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//    	}
//    	if(albumName.equals(mContext.getResources().getString(R.string.track_info_unknown))) {
//    		albumName = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//    	}
//        final String selection = MediaStore.Audio.AudioColumns.ALBUM + "='"
//                + replaceString(albumName) + "'" + " AND " + MediaStore.Audio.AudioColumns.ARTIST + "='" + replaceString(artist) + "'";
//        final List<Integer> ids = new ArrayList<Integer>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                new String[]{MediaStore.Audio.AudioColumns._ID}, selection, null, null);
//        if(c == null) return ids;
//        int id;
//        while (c.moveToNext()) {
//            id = getIdFromMediaStoreMemo(c.getInt(0));
//            ids.add(id);
//        }
//        c.close();
//        return ids;
//    }
//    /**
//     * Get artist tracks by artist ext Id.
//     * 
//     * @param String
//     *            artistExtId:this id from sina.
//     * @return Track List.
//     */
//    public List<LocalTrack> getArtistTracks(String artistId, String artistName) {
//        List<LocalTrack> tracks = new ArrayList<LocalTrack>(0);
//        for (LocalTrack localTrack : getSMArtistTracks(artistId, artistName)) {
//            tracks.add(localTrack);
//        }
//        for (LocalTrack localTrack : getMSTracksByArtistId(artistId,artistName)) {
//            tracks.add(localTrack);
//        }
//        tracks = sortTracks(tracks);
//        return tracks;
//    }
//
//    private List<LocalTrack> getSMArtistTracks(String artistId,
//            String artistName) {
//        final List<LocalTrack> tracks = new ArrayList<LocalTrack>(0);
//        final String selection ;
//        if(artistId.equals("0")) {
//        	selection = SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }else {
//        	selection =  SinaMusic.Tracks.ARTISTID + "=" + artistId
//            + " OR " + SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }   
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks._ID,
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.RATING,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.TYPE,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//        		projection, selection, null, null);
//        LocalTrack track;
//        while (c.moveToNext()) {
//            track = createMiniLocalTrackFromSinaMusic(c);
//            tracks.add(track);
//        }
//        c.close();
//        return tracks;
//    }
//
//    private List<LocalTrack> getMSTracksByArtistId(String artistId,String artist) {
//		final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//		if (artist.equals(mContext.getResources().getString(
//				R.string.track_info_unknown))) {
//			artist = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//		}
//		final String selection = MediaStore.Audio.AudioColumns.ARTIST + "='"
//				+  replaceString(artist) + "'";
//		final Cursor c = mContentResolver.query(
//				MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//				null, selection, null, null);
//		if (c == null)
//			return localTracks;
//		LocalTrack track;
//		while (c.moveToNext()) {
//			track = createMiniLocalTrackFromMediaStore(c);
//			localTracks.add(track);
//		}
//		c.close();
//		return localTracks;
//    }
//
//    private LocalTrack createLocalTrackFromSinaMusic(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        track.setId(c.getInt(c.getColumnIndex(SinaMusic.Tracks._ID)));
//        track.setExtId(c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.EXTERNALID)));
//        track.setUri(c.getString(c.getColumnIndex(SinaMusic.Tracks.URI)));
//        track.setDuration(c
//                .getLong(c.getColumnIndex(SinaMusic.Tracks.DURATION)));
//        track.setName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.NAME)));
//        track
//                .setSize(c.getLong(c
//                        .getColumnIndexOrThrow(SinaMusic.Tracks.SIZE)));
//        track.setRating(c.getFloat(c.getColumnIndex(SinaMusic.Tracks.RATING)));
//        track.setAlbumExtId((c.getInt(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUMID))));
//        track.setAlbumPhotoUri(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUMARTURL)));
//        track.setAlbumName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUM)));
//        track.setArtistPhotoUri((c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTISTARTURL))));
//        track.setArtistExtId(c.getInt(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTISTID)));
//        track.setArtistName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTIST)));
//        track.setPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.PATH)));
//        track.setLyric(c.getString(c.getColumnIndex(SinaMusic.Tracks.LYRIC)));
//        track.setDeletedFlag((c.getInt(c
//                .getColumnIndex(SinaMusic.Tracks.DELETEFLAG)) == 0) ? false
//                : true);
//        
//        if(TextUtils.isEmpty(track.getAlbumPhotoUri())) {
//        	track.setIconPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.ICON)));
//        }else {
//        	track.setIconPath(track.getAlbumPhotoUri());
//        }       
//
//        int type = c.getInt(c.getColumnIndex(SinaMusic.Tracks.TYPE));
//        track.setType(type);
//        if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE) {
//            track.setIsFromMediaStore(true);
//        } else if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) {
//            track.setIsDownloadFormSina(true);
//        }
//        return track;
//    }
//
//    /**
//     * Clear Downloaded Tracks in databases.
//     */
//    public void clearTrackDownloadedDisplayList() {
//
//        final String where = SinaMusic.Tracks.DOWNLOADSTATUS + "="
//                + MusicUtils.DOWNLOAD_COMPLETED + " AND "
//                + SinaMusic.Tracks.DELETEFLAG + "<>" + MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE;
//        final Cursor c = mContentResolver
//                .query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                        new String[] { SinaMusic.Tracks.EXTERNALID }, where,
//                        null, null);
//        String extId;
//        while (c.moveToNext()) {
//            extId = c.getString(0);
//            updatePropDeleteFlag(extId, true);
//        }
//        c.close();
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.DELETEFLAG, MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE);
//        int rows = mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                values, where, null);
//        Log.d(TAG, "Clear" + rows + "downloaded rows");
//    }
//
//    private void updatePropDeleteFlag(String extId, boolean flag) {
//        String fileName = extId + MusicProviderConstantValues.FILE_PROP;
//        String filePath = "";
//        try {
//            filePath = mFileManager.createFile(fileName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Configuration config = new Configuration(filePath);
//        String parameterName;
//        String parameterValue;
//        if (filePath != null && !filePath.equals("")) {
//            parameterName = SinaMusic.Tracks.DELETEFLAG;
//            parameterValue = flag ? "1" : "0";
//            config.setValue(parameterName, parameterValue);
//            config.savaPropertiesToFile("");
//        }
//    }
//
//    /**
//     * Clear Download item by track id .
//     * 
//     * @param int id.
//     */
//    public void clearDownloadingItemById(int id) {
//    	Log.d(TAG, "clearDownloadingItemById" + id);
//        Track track = getTrackById(id);
//        if( track != null) {
//        	String path = mFileManager.getDownloadPath() + "/" + track.getExtId() + MusicProviderConstantValues.FILE_SM;
//            //delete track file
//            deleteFile(path);
//            //delete icon file
//            path = track.getAlbumPhotoUri();        
//            deleteFile(path);       
//            //delete properties
//            path = mFileManager.getDownloadPath() + "/" + track.getExtId() + MusicProviderConstantValues.FILE_PROP;
//            deleteFile(path);
//            path = mFileManager.getDownloadPath() + "/" + track.getExtId() + MusicProviderConstantValues.FILE_CRYPT;
//            deleteFile(path);
//            //delete info from table
//            int trackId = getIdFromIndex(id);
//            if(trackId != -1) {
//            	final Uri uri = Uri.withAppendedPath(SinaMusic.Tracks.CONTENT_URI,String.valueOf(trackId));
//                deleteUri(uri);
//            }
//            
//        }
//    }
//    /**
//     * Get all downloaded list for display.
//     * 
//     * @return Track List.
//     * 
//     */
//    public List<RemoteTrack> getAllDownloadedListForDisplay() {
//
//        final List<RemoteTrack> mDownloadedListTracks = new ArrayList<RemoteTrack>(
//                0);
//        final String selection = SinaMusic.Tracks.DOWNLOADSTATUS + "="
//                + MusicUtils.DOWNLOAD_COMPLETED + " AND "
//                + SinaMusic.Tracks.DELETEFLAG + "<>" 
//                + MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE
//                + " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final Cursor c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI,
//				null, selection, null, null);
//		if (c == null)
//			return mDownloadedListTracks;
//
//		RemoteTrack remoteTrack;
//		while (c.moveToNext()) {
//			remoteTrack = createRemoteTrackFromSinaMusic(c);
//			remoteTrack
//					.setId(getTrackIdFromIndex(
//							remoteTrack.getId(),
//							MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA));
//			mDownloadedListTracks.add(remoteTrack);
//		}
//		c.close();
//		return mDownloadedListTracks;
//    }
//
//    /**
//     * Get all downloading list for display.
//     * 
//     * @return Track List.
//     */
//    public List<RemoteTrack> getAllDownloadingListForDisplay() {
//
//        final List<RemoteTrack> mDownloadingListTracks = new ArrayList<RemoteTrack>(
//                0);
//        final String selection = "(" + SinaMusic.Tracks.DOWNLOADSTATUS + "="
//                + MusicUtils.DOWNLOAD_PASUED + " OR "
//                + SinaMusic.Tracks.DOWNLOADSTATUS + "="
//                + MusicUtils.DOWNLOAD_ING + ")" + " AND "
//                + SinaMusic.Tracks.DELETEFLAG + "<>" + MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE 
//                + " AND " + SinaMusic.Tracks.TYPE + "<>" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA 
//                + " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final Cursor c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI,
//                null, selection, null, null);
//        if (c.getCount() > 0) {
//            RemoteTrack remoteTrack;
//            while (c.moveToNext()) {
//                remoteTrack = createRemoteTrackFromSinaMusic(c);
//                remoteTrack.setId(getTrackIdFromIndex(remoteTrack.getId(),
//                        MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE));
//                if (!mDownloadingListTracks.contains(remoteTrack)) {
//                    mDownloadingListTracks.add(remoteTrack);
//                }
//            }
//            c.close();
//        }
//        return mDownloadingListTracks;
//    }
//
//	public int getDownloadCount() {
//		final String selection = "(" + SinaMusic.Tracks.DOWNLOADSTATUS + "="
//				+ MusicUtils.DOWNLOAD_PASUED + " OR "
//				+ SinaMusic.Tracks.DOWNLOADSTATUS + "="
//				+ MusicUtils.DOWNLOAD_ING + " OR "
//				+ SinaMusic.Tracks.DOWNLOADSTATUS + "="
//				+ MusicUtils.DOWNLOAD_COMPLETED + ")" + " AND "
//				+ SinaMusic.Tracks.DELETEFLAG + "<>"
//				+ MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE;
//		final Cursor c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI,
//				null, selection, null, null);
//		int count = c.getCount();
//		c.close();
//		return count;
//	}
//
//	public int getDownloadingCount() {
//		final String selection = "(" + SinaMusic.Tracks.DOWNLOADSTATUS + "="
//				+ MusicUtils.DOWNLOAD_PASUED + " OR "
//				+ SinaMusic.Tracks.DOWNLOADSTATUS + "="
//				+ MusicUtils.DOWNLOAD_ING + ")" + " AND "
//				+ SinaMusic.Tracks.DELETEFLAG + "<>"
//				+ MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE;
//		final Cursor c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI,
//				null, selection, null, null);
//		int count = c.getCount();
//		c.close();
//		return count;
//	}
//
//    /**
//     * Get now playing track list.
//     * 
//     * @return Track List.
//     */
//    public List<LocalTrack> getNowPlayingTrackListForDisplay() {
//        final List<LocalTrack> nowPlayingListTracks = new ArrayList<LocalTrack>(0);
//        String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + PlayListBean.RECENT_PLAYED_PLAYLIST_ID;
//        final Cursor c = mContentResolver.query(SinaMusic.TrackPlayListMap.CONTENT_URI,
//                null, selection, null,
//                null);      
//        final int[] idlist = new int[c.getCount()];
//        int j = 0;
//        while (c.moveToNext()) {
//            int trackid = c.getInt(c
//                    .getColumnIndex(SinaMusic.TrackPlayListMap.TRACKID));
//            idlist[j++] = trackid;
//        }
//        c.close();
//        
//        for (int id : idlist) {
//            Track track = getMiniTrackById(id);
//            nowPlayingListTracks.add((LocalTrack) track);
//        }
//        //TODO
//        final List<LocalTrack> testnowPlayingListTracks = new ArrayList<LocalTrack>(0);
//        int size = nowPlayingListTracks.size();
//        for(int i= size-1;i>=0;i--) {
//            testnowPlayingListTracks.add(nowPlayingListTracks.get(i));
//        }
//        Log.d(TAG, "Get Now Playing Track list size:" + testnowPlayingListTracks.size());
//        return testnowPlayingListTracks;
//    }
//    public List<LocalTrack> getNowPlayingTrackList() {
//        tracksRating = new HashMap<Integer, Float>(0);
//        String selection = SinaMusic.TracksIndex.TYPE + "<>"
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE;
//        Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                null, selection, null, null);
//        final List<LocalTrack> nowPlayingListTracks = new ArrayList<LocalTrack>(
//                0);
//        while (c.moveToNext()) {
//            tracksRating.put(c.getInt(c
//                    .getColumnIndex(SinaMusic.TracksIndex.TRACKID)), c
//                    .getFloat(c.getColumnIndex(SinaMusic.TracksIndex.RATING)));
//        }
//        selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + PlayListBean.RECENT_PLAYED_PLAYLIST_ID;
//        c = mContentResolver.query(SinaMusic.TrackPlayListMap.CONTENT_URI,
//                null, selection, null,
//                SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME_DESC_SORT_ORDER);
//        while (c.moveToNext()) {
//            int trackid = c.getInt(c
//                    .getColumnIndex(SinaMusic.TrackPlayListMap.TRACKID));
//            Track track = getTrackById(trackid);
//            nowPlayingListTracks.add((LocalTrack) track);
//        }
//        c.close();
//        //TODO
//        final List<LocalTrack> testnowPlayingListTracks = new ArrayList<LocalTrack>(0);
//        int size = nowPlayingListTracks.size();
//        for(int i= size-1;i>=0;i--) {
//            testnowPlayingListTracks.add(nowPlayingListTracks.get(i));
//        }
//        Log.d(TAG, "Get Now Playing Track list size:" + testnowPlayingListTracks.size());
//        return nowPlayingListTracks;
//    }
//    public Track getMiniTrackById(int id) {
//        if (id == BaseEntity.INVALID_ID)
//            return null;
//
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        final String [] projection = new String[] {
//        		SinaMusic.TracksIndex.RATING,
//        		SinaMusic.TracksIndex.TYPE,
//        		SinaMusic.TracksIndex.TRACKID
//        		};
//        final Cursor c = mContentResolver.query(uri, projection, null, null, null);
//        if (c.getCount() == 0) {
//            Log.d(TAG, "No track found for id " + id);
//            c.close();
//            return null;
//        }
//        if(c.moveToFirst()) {
//        	final float rating = c.getFloat(0);
//            final int type = c.getInt(1);
//            final int trackId = c.getInt(2);
//            c.close();
//            if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE) {        	
//                LocalTrack localTrack = getMiniTrackFromMediaStoreById(trackId);
//                if (localTrack != null) {
//                    localTrack.setId(id);
//                    localTrack.setRating(rating);
//                    return localTrack;
//                }
//            } else if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) {
//                LocalTrack localTrack = getMiniTrackFromSinaMusicById(trackId);
//                if (localTrack != null) {
//                    localTrack.setId(id);
//                    localTrack.setRating(rating);
//                    localTrack.setIsDownloadFormSina(true);
//                    return localTrack;
//                }
//            } else if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND) {
//                LocalTrack localTrack = getMiniTrackFromMediaStoreById(trackId);
//                if (localTrack != null) {
//                    localTrack.setId(id);
//                    localTrack.setIsRecordSoundTrack(true);
//                    localTrack.setRating(rating);
//                    return localTrack;
//                }
//            }
//        }       
//        return null;
//    }
//    private LocalTrack getMiniTrackFromSinaMusicById(int trackId) {
//        LocalTrack track = new LocalTrack();
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(uri, projection, null, null, null);
//        if (c.getCount() == 0) {
//        	c.close();
//            return null;
//        }
//        c.moveToFirst();
//        track = createMiniTrackFromSM(c);
//        c.close();
//        return track;
//    }
//    
//    private LocalTrack createMiniTrackFromSM(Cursor c) {
//    	 final LocalTrack track = new LocalTrack();
//         track.setName(c.getString(0));
//         String albumart = c.getString(1);
//         track.setAlbumPhotoUri(albumart);
//         track.setArtistName(c.getString(2));
//         if(TextUtils.isEmpty(albumart)) {
//         	track.setIconPath(c.getString(3));
//         }else {
//         	track.setIconPath(albumart);
//         }         
//         return track;
//	}
//	private LocalTrack getMiniLocalTrackFromMediaStoreById(int id) {
//        LocalTrack track = new LocalTrack();
//        final String selection = MediaStore.Audio.AudioColumns._ID + "=" + id;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if(c == null) return track;
//        if (c.getCount() == 0) {
//        	c.close();
//            return null;
//        }
//        c.moveToFirst();
//        track = createMiniLocalTrackFromMediaStore(c);
//        
//        c.close();
//        return track;
//    }
//    private LocalTrack getMiniTrackFromMediaStoreById(int id) {
//        LocalTrack track = new LocalTrack();
//        final String selection = MediaStore.Audio.AudioColumns._ID + "=" + id;
//        final String[] projection = new String[] {
//        		MediaStore.Audio.AudioColumns.TITLE,
//        		MediaStore.Audio.AudioColumns.ARTIST,
//        		MediaStore.Audio.AudioColumns.ALBUM_ID
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//        		projection, selection, null, null);
//        if(c == null) return track;
//        if (c.getCount() == 0) {
//        	c.close();
//            return null;
//        }
//        c.moveToFirst();
//        track = createMiniTrackFromMS(c);
//        
//        c.close();
//        return track;
//    }
//    private LocalTrack createMiniTrackFromMS(Cursor c) {
//    	final LocalTrack track = new LocalTrack();
//        String name = c.getString(0);
//        track.setName(getLocaleName(name));
//        name = c.getString(1);
//        track.setArtistName(getLocaleName(name));        
//        final int albumId = c.getInt(2);
////        track.setIconPath(getAlbumArtPath(albumId));
//        if(getAlbumArtPath(albumId)!=null)
//        	track.setIconPath(getAlbumArtPath(albumId));
//        else
//        	track.setIconPath(MusicUtils.getIconPathAtSameDir(track.getPath(),track.getName()));
//		return track;
//	}
//	/**
//     * Get all remote tracks.
//     * 
//     * @return Track List .
//     */
//    public ArrayList<RemoteTrack> getAllRemoteTracks() {
//
//        Log.d(TAG, "Get all remote tracks >>>");
//        final ArrayList<RemoteTrack> mRemoteTracks = new ArrayList<RemoteTrack>(
//                0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null, null);
//        int id = BaseEntity.INVALID_ID;
//        while (c.moveToNext()) {
//            id = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TRACKID));
//            mRemoteTracks.add(getRemoteTrackFromSinaMusicById(id));
//        }
//        c.close();
//        return mRemoteTracks;
//    }
//
//    private RemoteTrack getRemoteTrackFromSinaMusicById(int id) {
//        Log.d(TAG, "Get remote track from sinamusic by id:" + id);
//        RemoteTrack track = new RemoteTrack();
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor c = mContentResolver.query(uri, null, null, null, null);
//        if (c.getCount() == 0) {
//            return null;
//        }
//        c.moveToFirst();
//        track = createRemoteTrackFromSinaMusic(c);
//        c.close();
//        return track;
//    }
//
//    private LocalTrack getLocalTrackFromMediaStoreById(int id) {
//        LocalTrack track = new LocalTrack();
//        final String selection = MediaStore.Audio.AudioColumns._ID + "=" + id;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if(c == null) return track;
//        if (c.getCount() == 0) {
//            c.close();
//            return null;
//        }
//        c.moveToFirst();
//        track = createlocalTrackFromMediaStore(c);
//        c.close();
//        return track;
//    }
//    private LocalTrack createMiniLocalTrackFromMediaStore(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        final int trackId = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns._ID));        
//        int id = getIdFromMediaStoreMemo(trackId);
//        
//        if(id != -1) {
//            track.setId(id);
//        }else {         
//            final String mime_type = c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
//            int type;
//            if(mime_type.equals(MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE)) type =MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//            else type = MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//            id = getTrackIdFromIndex(trackId, type);
//            if(id == -1) {
//                final long createTime = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//                id = saveToTrackIndex(trackId, type, createTime);
//            }    
//            track.setId(id);
//            msTracksIndex.put(trackId, id);
//        }
//        
//        float rating = getRatingFromMemo(id);
//        if(rating == -1){
//            track.setRating(getRatingById(track.getId()));
//        }       
//        track.setRating(rating);
//        String name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
//        track.setName(getLocaleName(name));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
//        track.setArtistName(getLocaleName(name));        
//        final int albumId = c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
////        track.setIconPath(getAlbumArtPath(albumId));
//        track.setPath(c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
//        if(getAlbumArtPath(albumId)!=null)
//        	track.setIconPath(getAlbumArtPath(albumId));
//        else
//        	track.setIconPath(MusicUtils.getIconPathAtSameDir(track.getPath(),track.getName()));
//		 return track;
//    }
//    
//    
//  
//	private int getIdFromMediaStoreMemo(int trackId) {
//        if (msTracksIndex == null || !msTracksIndex.containsKey(trackId)) {
//            return -1;
//        }
//
//        return msTracksIndex.get((int) trackId);
//    }
//    private float getRatingFromMemo(int id) {
//        if (tracksRating == null || !tracksRating.containsKey(id)) {
//            return -1;
//        }
//
//        return tracksRating.get((int) id);
//    }
//
//    private RemoteTrack createRemoteTrackFromSinaMusic(Cursor c) {
//        final RemoteTrack track = new RemoteTrack();
//        final long size = c.getLong(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.SIZE));
//        track.setId(c.getInt(c.getColumnIndex(SinaMusic.Tracks._ID)));
//        track.setExtId(c.getString(c
//                .getColumnIndex(SinaMusic.Tracks.EXTERNALID)));
//        track.setUri(c.getString(c.getColumnIndex(SinaMusic.Tracks.URI)));
//        track.setDuration(c
//                .getLong(c.getColumnIndex(SinaMusic.Tracks.DURATION)));
//        track.setName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.NAME)));
//        track.setSize(size);
//        track.setAlbumExtId((c.getInt(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUMID))));
//        track.setAlbumPhotoUri(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUMARTURL)));
//        track.setAlbumName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ALBUM)));
//        track.setArtistPhotoUri((c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTISTARTURL))));
//        track.setArtistExtId(c.getInt(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTISTID)));
//        track.setArtistName(c.getString(c
//                .getColumnIndexOrThrow(SinaMusic.Tracks.ARTIST)));
//        track.setPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.PATH)));
//        final long downloadedsize = c.getLong(c
//                .getColumnIndex(SinaMusic.Tracks.DOWNLOADEDSIZE));
//        track.setDownloadedSize(downloadedsize);
//        final int downloadStatus = c.getInt(c
//                .getColumnIndex(SinaMusic.Tracks.DOWNLOADSTATUS));
//        track.setDownloadStatus(downloadStatus);
//        track
//                .setIsPlayed((c.getInt(c
//                        .getColumnIndex(SinaMusic.Tracks.ISPLAYED)) == 0) ? false
//                        : true);
//        if(TextUtils.isEmpty(track.getAlbumPhotoUri())) {
//        	track.setIconPath(c.getString(c.getColumnIndex(SinaMusic.Tracks.ICON)));
//        } else {
//        	track.setIconPath(track.getAlbumPhotoUri());
//        }
//        track.setDownloadPercent(0);
//        if (downloadStatus == MusicUtils.DOWNLOAD_PASUED) {
//            final int percent = (int) (downloadedsize * 100.0 / size);
//            track.setDownloadPercent(percent);
//        } else if (downloadStatus == MusicUtils.DOWNLOAD_COMPLETED) {
//            track.setDownloadPercent(100);
//        } else if (downloadStatus == MusicUtils.DOWNLOAD_ING) {
//            final int percent = (int) (downloadedsize * 100.0 / size);
//            track.setDownloadPercent(percent);
//        }
//        return track;
//    }
//
//    private void insertRecordPlaylistMap(int playlist_id, int track_id) {
//        if (playlist_id == BaseEntity.INVALID_ID
//                || track_id == BaseEntity.INVALID_ID) {
//            Log.e(TAG, "Invalid playlist record");
//            return;
//        }
//        if(!isExistTrackPlaylistMap(playlist_id, track_id)) {
//        	final ContentValues values = new ContentValues();
//            long currentTime;
//            values.put(SinaMusic.TrackPlayListMap.TRACKID, track_id);
//            values.put(SinaMusic.TrackPlayListMap.PLAYLISTID, playlist_id);
//            currentTime = System.currentTimeMillis();
//            values.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, currentTime);
//            mContentResolver.insert(SinaMusic.TrackPlayListMap.CONTENT_URI, values);
//            values.clear();
//        }        
//    }
//
//    private ContentValues createTracksContentValuesByTrack(RemoteTrack track) {
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.EXTERNALID, track.getExtId());
//        values.put(SinaMusic.Tracks.NAME, track.getName());
//        values.put(SinaMusic.Tracks.LYRIC, track.getLyric());
//        values.put(SinaMusic.Tracks.URI, track.getUri());
//        values.put(SinaMusic.Tracks.ALBUM, track.getAlbumName());
//        values.put(SinaMusic.Tracks.ALBUMID, track.getAlbumExtId());
//        values.put(SinaMusic.Tracks.ARTIST, track.getArtistName());
//        values.put(SinaMusic.Tracks.ARTISTID, track.getArtistExtId());
//        values.put(SinaMusic.Tracks.ARTISTARTURL, track.getArtistPhotoUri());
//        values.put(SinaMusic.Tracks.DOWNLOADSTATUS, track.getDownloadStatus());
//        values.put(SinaMusic.Tracks.DOWNLOADEDSIZE, track.getDownloadedSize());
//        values.put(SinaMusic.Tracks.DURATION, track.getDuration());
//        values.put(SinaMusic.Tracks.ISPLAYED, track.isPlayed());
//        values.put(SinaMusic.Tracks.ISFAVOURITE, track.isFavourite());
//        values.put(SinaMusic.Tracks.RATING, track.getRating());
//        values.put(SinaMusic.Tracks.SIZE, track.getSize());
//        values.put(SinaMusic.Tracks.PATH, track.getPath());
//        values.put(SinaMusic.Tracks.ICON, track.getIconPath());
//        values.put(SinaMusic.Tracks.DOWNLOADEDTIME, track.getDownloadedTime());
//        values.put(SinaMusic.Tracks.ALBUMARTURL, track.getAlbumPhotoUri());
//        values.put(SinaMusic.Tracks.TYPE, track.getType());
//        values.put(SinaMusic.Tracks.DELETEFLAG, track.isDeletedFlag());
//        //TODO : fix will save field 
//        values.put(SinaMusic.Tracks.SINAID, track.getUserAccount());
//        
//        return values;
//    }
//
//    private static final String DEFAULT_PATH = null;
//    private static final long DEFAULT_FILE_SIZE = 0;
//    private static final long DEFAULT_DOWNLOADED_SIZE = 0;
//    private static final boolean DEFAULT_ISREMOTE_STATUS = true;
//
//    public void updateDownloadCompleteStatus(int id, int status, String path,
//            long filesize) {
//    	Log.d(TAG, "updateDownloadCompleteStatus");
//        updateDownloadFieldStatus(id, status, path, filesize,
//                DEFAULT_DOWNLOADED_SIZE, false);
//        updateIndexType(id, MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//        updatePropDownloadFieldStatus(id, status, path, filesize,
//                DEFAULT_DOWNLOADED_SIZE, false);
//    }
//
//    private void saveDownloadedTrackToProp(String fileName, Track localTrack) {
//
//        String filePath = "";
//        try {
//            filePath = mFileManager.createFile(fileName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Configuration config = new Configuration(filePath);
//        String parameterName;
//        String parameterValue;
//        if (filePath != null && !"".equals(filePath)) {
//            parameterName = SinaMusic.Tracks._ID;
//            parameterValue = String.valueOf(localTrack.getId());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.NAME;
//            parameterValue = localTrack.getName();
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.SIZE;
//            parameterValue = String.valueOf(localTrack.getSize());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.DURATION;
//            parameterValue = String.valueOf(localTrack.getDuration());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.EXTERNALID;
//            parameterValue = localTrack.getExtId();
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.LYRIC;
//            parameterValue = localTrack.getLyric();
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ALBUM;
//            parameterValue = localTrack.getAlbumName();
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ALBUMID;
//            parameterValue = String.valueOf(localTrack.getAlbumExtId());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ALBUMARTURL;
//            parameterValue = String.valueOf(localTrack.getAlbumPhotoUri());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ARTIST;
//            parameterValue = String.valueOf(localTrack.getArtistName());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ARTISTID;
//            parameterValue = String.valueOf(localTrack.getArtistExtId());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.ARTISTARTURL;
//            parameterValue = String.valueOf(localTrack.getArtistPhotoUri());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.PATH;
//            parameterValue = String.valueOf(localTrack.getPath());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.RATING;
//            parameterValue = String.valueOf(localTrack.getRating());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.URI;
//            parameterValue = String.valueOf(localTrack.getUri());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.TYPE;
//            parameterValue = String.valueOf(localTrack.getType());
//            config.setValue(parameterName, parameterValue);
//            parameterName = SinaMusic.Tracks.DELETEFLAG;
//            parameterValue = String.valueOf(localTrack.isDeletedFlag() ? "1"
//                    : "0");
//            config.setValue(parameterName, parameterValue);
//            
//            parameterName = SinaMusic.Tracks.SINAID;
//            parameterValue = localTrack.getUserAccount();
//            config.setValue(parameterName, parameterValue);
//            
//            config.savaPropertiesToFile("");
//        }
//    }
//
//    public void updateDownloadDestoryStatus(int id, int status, String path,
//            long downloadedsize) {
//    	Log.d(TAG, "updateDownloadDestoryStatus");
//        updateDownloadFieldStatus(id, status, path, DEFAULT_FILE_SIZE,
//                downloadedsize, DEFAULT_ISREMOTE_STATUS);
//        updatePropDownloadFieldStatus(id, status, path, DEFAULT_FILE_SIZE,
//                downloadedsize, DEFAULT_ISREMOTE_STATUS);
//    }
//
//    private void updatePropDownloadFieldStatus(int id, int status, String path,
//            long defaultFileSize, long downloadedsize,
//            boolean defaultIsremoteStatus) {
//        String extId = getExternalIdByTrackId(id);
//        String fileName = extId + MusicProviderConstantValues.FILE_PROP;
//        String filePath = "";
//        try {
//            filePath = mFileManager.createFile(fileName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Configuration config = new Configuration(filePath);
//        String parameterName;
//        String parameterValue;
//        Log.d(TAG , "------>>>the file path:" + filePath);
//        if (filePath != null && !"".equals(filePath)) {
//            if (path != DEFAULT_PATH) {
//            	Log.d(TAG , "download going to update path:" + path);
//                parameterName = SinaMusic.Tracks.PATH;
//                parameterValue = path;
//                config.setValue(parameterName, parameterValue);
//            }
//            if (downloadedsize != DEFAULT_DOWNLOADED_SIZE) {
//                parameterName = SinaMusic.Tracks.DOWNLOADEDSIZE;
//                parameterValue = String.valueOf(downloadedsize);
//                config.setValue(parameterName, parameterValue);
//            }
//            if (defaultFileSize != DEFAULT_FILE_SIZE) {
//                parameterName = SinaMusic.Tracks.SIZE;
//                parameterValue = String.valueOf(defaultFileSize);
//                config.setValue(parameterName, parameterValue);
//            }
//            if (status == MusicUtils.DOWNLOAD_COMPLETED) {
//                parameterName = SinaMusic.Tracks.TYPE;
//                parameterValue = String
//                        .valueOf(MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//                config.setValue(parameterName, parameterValue);
//
//                parameterName = SinaMusic.Tracks.DOWNLOADEDTIME;
//                parameterValue = String.valueOf(System.currentTimeMillis());
//                config.setValue(parameterName, parameterValue);
//            }
//
//            parameterName = SinaMusic.Tracks.DOWNLOADSTATUS;
//            parameterValue = String.valueOf(status);
//            config.setValue(parameterName, parameterValue);
//            config.savaPropertiesToFile("");
//        }
//    }
//
//    /**
//     * 
//     * @param int id
//     * @param int status
//     * @param long downloadedsize
//     */
//    
//    public void updateDownloadPauseStatus(int id, int status,
//            long downloadedsize) {
//    	Log.d(TAG, "updateDownloadPauseStatus");
//        updateDownloadFieldStatus(id, status, DEFAULT_PATH, DEFAULT_FILE_SIZE,
//                downloadedsize, DEFAULT_ISREMOTE_STATUS);
//        updatePropDownloadFieldStatus(id, status, DEFAULT_PATH,
//                DEFAULT_FILE_SIZE, downloadedsize, DEFAULT_ISREMOTE_STATUS);
//    }
//
//    public void updateDownloadPercent(int id, int status,
//            long downloadedsize) {    	
//    	Log.d(TAG, "updateDownloadPercent");
//    	
//        if (id == Track.INVALID_ID) {
//            Log.e(TAG, "update track id is not exist in databases");
//            return;
//        }  
//        
//        int trackId = getIdFromIndex(id);   
//        
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));     
//        
//        final ContentValues values = new ContentValues();
//        
//        values.put(SinaMusic.Tracks.DOWNLOADEDSIZE, downloadedsize);
//        values.put(SinaMusic.Tracks.DOWNLOADSTATUS, status);
//        int rows = mContentResolver.update(uri, values, null, null);
//        
//        if (rows == 0) {
//            Log.d(TAG, "no record is updated.");
//            return;
//        }
//        if (rows > 1) {
//            Log.e(TAG, "update Track record is wrong by id,return rows :"
//                    + rows);
//            return;
//        }
//    }
//    
//    /**
//     * 
//     * @param int id
//     * @param int status
//     */
//    public void updateDownloadResumeStatus(int id, int status) {
//    	Log.d(TAG, "updateDownloadResumeStatus");
//        updateDownloadFieldStatus(id, status, DEFAULT_PATH, DEFAULT_FILE_SIZE,
//                DEFAULT_DOWNLOADED_SIZE, DEFAULT_ISREMOTE_STATUS);
//        updatePropDownloadFieldStatus(id, status, DEFAULT_PATH,
//                DEFAULT_FILE_SIZE, DEFAULT_DOWNLOADED_SIZE,
//                DEFAULT_ISREMOTE_STATUS);
//    }
//
//    /**
//     * @param int id
//     * @param int status
//     * @param String
//     *            path
//     * @param long filesize
//     * @param long downloadedsize
//     * @param boolean isremote
//     */
//    private void updateDownloadFieldStatus(int id, int status, String path,
//            long filesize, long downloadedsize, boolean isremote) {
//
//        if (id == Track.INVALID_ID) {
//            Log.e(TAG, "update track id is not exist in databases");
//            return;
//        }
//        int trackId = getIdFromIndex(id);
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final ContentValues values = new ContentValues();
//        if (path != DEFAULT_PATH) {
//            values.put(SinaMusic.Tracks.PATH, path);
//        }
//        if (downloadedsize != DEFAULT_DOWNLOADED_SIZE) {
//            values.put(SinaMusic.Tracks.DOWNLOADEDSIZE, downloadedsize);
//        }
//        if (filesize != DEFAULT_FILE_SIZE) {
//            values.put(SinaMusic.Tracks.SIZE, filesize);
//        }
//        if (status == MusicUtils.DOWNLOAD_COMPLETED) {
//            values.put(SinaMusic.Tracks.TYPE, MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//            values.put(SinaMusic.Tracks.DOWNLOADEDTIME, System
//                    .currentTimeMillis());
//        }
//        values.put(SinaMusic.Tracks.DOWNLOADSTATUS, status);
//        int rows = mContentResolver.update(uri, values, null, null);
//        if (rows == 0) {
//            Log.d(TAG, "no record is updated.");
//            return;
//        }
//        if (rows > 1) {
//            Log.e(TAG, "update Track record is wrong by id,return rows :"
//                    + rows);
//            return;
//        }
//    }
//
//    /**
//     * Create a track or update track status for downloadstatus.
//     * 
//     * @param track
//     */
//    public int createNewDownloadTrack(RemoteTrack track) {
//        Log.d(TAG, "Create download track , track name is :" + track.getName()  + "-------" + track.getAlbumPhotoUri() + ">>>" + track.getmLocalIconUri());
//        if (track.getUri() == null || track.getUri().equals("")) {
//            Log.e(TAG, "Create a invalid track!");
//            return Track.INVALID_ID;
//        }
//        deleteDownloadedTrackByExternalId(track.getExtId());
//        track.setDownloadStatus(MusicUtils.DOWNLOAD_ING);
//        track.setUserAccount(MusicUtils.getUserAccount());
//        return saveOneTrack(track);
//    }
//
//    /**
//     * Create a new track or update the track in tables. When is played and Add
//     * to playlist.
//     * 
//     * @param track
//     */
//    public void addTrackToNowPlayingList(int trackId) {
//        insertRecordPlaylistMap(PlayListBean.RECENT_PLAYED_PLAYLIST_ID, trackId);
//    }
//
//    /**
//     * Create or update track in table by track. Main with Favourite Field.
//     */
//    public int updateTrackFavouriteInfo(RemoteTrack track) {
//        Log.d(TAG, "Create download track , track name is :" + track.getName());
//        final int track_id = saveOneTrack(track);
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.ISFAVOURITE, track.isFavourite());
//        mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, values,
//                SinaMusic.Tracks._ID + "=" + track_id, null);
//        return track.getId();
//    }
//
//    /**
//     * For Search
//     * 
//     * @return List : artistNames and trackNames with String List
//     */
//    public List<String> getAllTrackAndArtistNames() {
//
//        final List<String> nameList = new ArrayList<String>(0);
//        Log.d(TAG, "Get all artist and track Names");
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//            for (String name : getSMTrackAndArtistNames()) {
//                nameList.add(name);
//            }
//            if (getIsScannerFinished()) {
//                for (String name : getNameListFromMS()) {
//                    nameList.add(name);
//                }
//            }
//        } else {
//            if (getIsScannerFinished()) {
//                for (String name : getSMTrackAndArtistNames()) {
//                    nameList.add(name);
//                }
//                for (String name : getNameListFromMS()) {
//                    nameList.add(name);
//                }
//            }
//        }
//        return nameList;
//    }
//
//    private List<String> getSMTrackAndArtistNames() {
//        final List<String> nameList = new ArrayList<String>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, null, null, null);
//        String trackName;
//        String artistName;
//        while (c.moveToNext()) {
//            trackName = c
//                    .getString(c.getColumnIndex(SinaMusic.Tracks.NAME));
//            artistName = c.getString(c
//                    .getColumnIndex(SinaMusic.Tracks.ARTIST));
//            if (nameList.contains(trackName)
//                    && nameList.contains(artistName)) {
//                break;
//            }
//            if (!nameList.contains(trackName)) {
//                nameList.add(trackName);
//            }
//            if (!nameList.contains(artistName)) {
//                nameList.add(artistName);
//            }
//        }
//        c.close();
//        return nameList;
//    }
//
//    private List<String> getNameListFromMS() {
//        final List<String> nameList = new ArrayList<String>(0);
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, null, null, null);
//        if(c == null) return nameList;
//        String trackName;
//        String artistName;
//        while (c.moveToNext()) {
//            trackName = c.getString(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.TRACK));
//            artistName = c.getString(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
//            if (nameList.contains(trackName) && nameList.contains(artistName)) {
//                break;
//            }
//            if (!nameList.contains(trackName)) {
//                nameList.add(trackName);
//            }
//            if (!nameList.contains(artistName)) {
//                nameList.add(artistName);
//            }
//        }
//        c.close();
//        return nameList;
//    }
//
//    /**
//     * Query tracks information in table Tracks by key,the key is input by user.
//     * 
//     * @return ArrayList track.
//     */
//    
//    public List<LocalTrack> search(String _key) {
//
//    	String key = check(_key);
//        final List<LocalTrack> tracklist = new ArrayList<LocalTrack>(0);
//        if (!MusicProviderConstantValues.USE_SDCARD) {
//            // search from sinamusic table of downloaded tracks.
//            for (LocalTrack localTrack : searchFromSM(key)) {
//                tracklist.add(localTrack);
//            }
//            if (getIsScannerFinished()) {
//                // search from MS.
//                for (LocalTrack localTrack : searchFromMS(key)) {
//                    tracklist.add(localTrack);
//                }
//            }
//        } else {
//            if (getIsScannerFinished()) {
//                // search from sinamusic table of downloaded tracks.
//                for (LocalTrack localTrack : searchFromSM(key)) {
//                    tracklist.add(localTrack);
//                }
//                // search from MS.
//
//                for (LocalTrack localTrack : searchFromMS(key)) {
//                    tracklist.add(localTrack);
//                }
//            }
//        }
//
//        return tracklist;
//    }
//
//    private String check(String key) {
//		String res = key;
//		if (key.indexOf("'") != -1) {
//			res = key.replace("'", "''");
//		}
//		if (key.indexOf("%") != -1) {
//			res = key.replace("%", "/%");
//		}
//		//TODO : others 
//		return res;
//	}
//	private List<LocalTrack> searchFromSM(String key) {
//        final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        final String selection = "(" + SinaMusic.Tracks.NAME + " LIKE '%" + key
//                + "%' OR " + SinaMusic.Tracks.ARTIST + " LIKE '%" + key + "%'"
//                + ") AND " + SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            localTrack = createLocalTrackFromSinaMusic(c);
//            localTrack.setId(getTrackIdFromIndex(localTrack.getId(),
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA));
//            if (!localTracks.contains(localTrack)) {
//                localTracks.add(localTrack);
//            }
//        }
//        c.close();
//        return localTracks;
//    }
//
//	private List<LocalTrack> searchFromMS(String key) {
//		final String UnkownStr = mContext.getResources().getString(R.string.track_info_unknown);
//		final List<LocalTrack> localTracks = new ArrayList<LocalTrack>();
//		final String selection = MediaStore.Audio.AudioColumns.TITLE
//				+ " LIKE '%" + key + "%' OR "
//				+ MediaStore.Audio.AudioColumns.ARTIST + " LIKE '%" + key
//				+ "%'";
//		final Cursor c = mContentResolver.query(
//				MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//				null, selection, null, null);
//		if (c == null)
//			return localTracks;
//		LocalTrack localTrack;
//		while (c.moveToNext()) {
//			localTrack = createlocalTrackFromMediaStoreForSearch(c);
//			
//			if (localTrack.getArtistName()==UnkownStr) {
//				if (localTrack.getName().contains(key)) {
//					if (!localTracks.contains(localTrack)) {
//						localTracks.add(localTrack);
//					}
//				}
//			} else {
//				if (!localTracks.contains(localTrack)) {
//					localTracks.add(localTrack);
//				}
//			}
//			
//		}
//		c.close();
//		return localTracks;
//	}
//
//    /**
//     * According to album extId to query tracks information in table Tracks.
//     * 
//     * @param String
//     *            albumExtId : this id from sina
//     * @return ArrayList Track.
//     */
//    public List<LocalTrack> getTracksByAlbumId(String albumid) {
//
//        List<LocalTrack> albumTracks = new ArrayList<LocalTrack>(0);
//        // get From MS.
//        for (LocalTrack localTrack : getTracksByAlbumIdFromMS(albumid)) {
//            albumTracks.add(localTrack);
//        }
//        // get From SM.
//        for (LocalTrack localTrack : getTracksByAlbumsIdFromSM(albumid)) {
//            albumTracks.add(localTrack);
//        }
//        albumTracks = sortTracks(albumTracks);
//        return albumTracks;
//    }
//    private ArrayList<LocalTrack> getTracksByAlbumsIdFromSM(String albumid) {
//        final ArrayList<LocalTrack> albumTracks = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.Tracks.ALBUMID + "=" + albumid;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            localTrack = createLocalTrackFromSinaMusic(c);
//            localTrack.setId(getTrackIdFromIndex(localTrack.getId(),
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA));
//            if (!albumTracks.contains(localTrack)) {
//                albumTracks.add(localTrack);
//            }
//        }
//        c.close();
//        return albumTracks;
//    }
//
//    private ArrayList<LocalTrack> getTracksByAlbumIdFromMS(String albumid) {
//        final ArrayList<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        final String selection = MediaStore.Audio.AudioColumns.ALBUM_ID + "="
//                + albumid;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        LocalTrack localTrack;
//        if(c == null) return localTracks;
//        while (c.moveToNext()) {
//            localTrack = createLocalTrackFromSinaMusic(c);
//            localTrack.setId(getTrackIdFromIndex(localTrack.getId(),
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA));
//            if (!localTracks.contains(localTrack)) {
//                localTracks.add(localTrack);
//            }
//        }
//        c.close();
//        return localTracks;
//    }
//
//    /**
//     * Convert file uri to mediastore uri
//     */
//    private String fileToContentUri(String uri) {
//        String filepath = uri.replaceFirst("file://", "").replace("%20", " ");
//        String[] projection = new String[] { "_id" };
//        String selection = "_data=\"" + filepath + "\"";
//        Log.d(TAG, "Query file: " + filepath);
//        Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                projection, selection, null, null);
//        if(c == null) return null;
//        if (c.getCount() != 1) {
//            Log.e(TAG, "Wrong result: " + c.getCount());
//            c.close();
//            return null;
//        }
//        c.moveToFirst();
//        int id = c.getInt(0);
//        Log.d(TAG, "Got id: " + id);
//        String uriStr = ContentUris.withAppendedId(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, id)
//                .toString();
//        c.close();
//        return uriStr;
//    }
//
//    /**
//     * Get track by uri.
//     * 
//     * @param String
//     *            uri : If remote use url.
//     * @return Track
//     */
//    public LocalTrack getTrackByUriOld(String uri) {
//        String selection;
//        LocalTrack track = new LocalTrack();
//        if (uri.startsWith("file://")) {
//            uri = fileToContentUri(uri);
//        }
//        Log.d("MusicPlayRedirect", "fileToContentUri result is " + uri);
//        if (TextUtils.isEmpty(uri)) {
//            return null;
//        }
//        if (uri.startsWith("content://")) {
//            final Uri urireal = Uri.parse(uri);
//            final Cursor c = mContentResolver.query(urireal, null, null, null,
//                    null);
//            if (c == null) {
//                return track;
//            }
//            while (c.moveToNext()) {
//                track = createlocalTrackFromMediaStore(c);
//            }
//            c.close();
//            return track;
//        } else {
//            selection = SinaMusic.Tracks.URI + "='" + uri + "'";
//            final Cursor c = mContentResolver.query(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, null, selection, null, null);
//            if (c == null)
//                return track;
//            if (c.getCount() > 0) {
//                Log.e(TAG, "Got multi data at track table cursor: " + c);
//            }
//            c.moveToFirst();
//            track = createLocalTrackFromSinaMusic(c);
//            track.setId(getTrackIdFromIndex(track.getId(),
//                    MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA));
//            c.close();
//        }
//        return track;
//    }
//    public LocalTrack getTrackByUri(String uri) {
//    	Log.d(TAG, "Get track by Uri : " + uri);
//    	LocalTrack track = null;
//    	if (TextUtils.isEmpty(uri)) {       		
//            return track;
//        }
//        if (uri.startsWith("file://")) {        	
//            track = getMSTrackByFile(uri);
//            return track;
//        }
//        if (uri.startsWith("content://")) {
//        	if(uri.startsWith("content://media/internal")){
//        		track = getMSTrackByUri(uri,MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI_INTERNAL_TYPE);
//        	}else {
//        		track = getMSTrackByUri(uri,MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI_EXTERNAL_TYPE);
//        	}
//        	
//        	
//        	return track;
//        }       
//        File f = new File(uri);
//        if(f.isFile()) {
//        	track = getMSLocalTrackByPath(uri);
//        	return track;
//        }       
//        return track;
//    }
//
//	private LocalTrack getMSTrackByUri(String uri,int type) {
//		LocalTrack track = null;
//		final Uri urireal = Uri.parse(uri);
//		Cursor c;
//		switch (type) {
//		case MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI_EXTERNAL_TYPE: 
//			 c = mContentResolver
//					.query(urireal, null, null, null, null);
//			if (c != null) {
//				while (c.moveToNext()) {
//					track = createlocalTrackFromMediaStore(c);
//				}
//				c.close();
//			}
//			break;
//		case MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI_INTERNAL_TYPE:
//			c = mContentResolver
//					.query(urireal, null, null, null, null);
//			if (c != null) {
//				while (c.moveToNext()) {
//					track = createlocalTrackFromMediaStoreInternal(c);
//				}
//				c.close();
//			}
//			break;
//		}
//		return track;
//	}
//
//	private LocalTrack createlocalTrackFromMediaStoreInternal(Cursor c) {
//		final LocalTrack track = new LocalTrack();
//        final int trackId = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns._ID));   
//        final Uri uri = ContentUris.withAppendedId(
//                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, trackId);
//        track.setUri(uri.toString());
//		final String mime_type = c.getString(c
//				.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
//		int type;
//		if (mime_type
//				.equals(MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE))
//			type = MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//		else
//			type = MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE_INTERNAL;
//		
//		int id = getTrackIdFromIndex(trackId, type);
//		if (id == -1) {
//			final long createTime = c.getLong(c
//					.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//			id = saveToTrackIndex(trackId, type, createTime);
//		}
//		track.setId(id);
//        float rating = getRatingFromMemo(id);
//        if(rating == -1) {
//            track.setRating(getRatingById(track.getId()));
//        }      
//
//        String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
//        track.setName(getLocaleName(name));
//        
//        track
//                .setArtistExtId(c
//                        .getInt(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID)));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
//        track.setArtistName(getLocaleName(name));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
//        track.setAlbumName(getLocaleName(name));
//        track
//                .setDuration(c
//                        .getLong(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)));
//        track.setSize(c.getLong(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)));
//        final int albumId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
//        track.setAlbumExtId(albumId);
//        track.setPath(c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
//        track.setIsDownloadFormSina(false);
////        track.setIconPath(getAlbumArtPath(albumId));
//        if(getAlbumArtPath(albumId)!=null)
//        	track.setIconPath(getAlbumArtPath(albumId));
//        else
//        	track.setIconPath(MusicUtils.getIconPathAtSameDir(track.getPath(),track.getName()));
//        return track;
//	}
//	private LocalTrack getMSTrackByFile(String uri) {
//		String filepath = uri.replaceFirst("file://", "").replace("%20", " ");
//		Log.d(TAG, "Query file: " + filepath);
//		LocalTrack lt = getMSLocalTrackByPath(filepath);
//		return lt;
//	}
//	
//	public LocalTrack getMSLocalTrackByPath(String path) {  
//    	if(TextUtils.isEmpty(path)) {
//    		return null;
//    	}
//    	LocalTrack track = null;
//    	final String selection =  "_data='" + replaceString(path) + "'";
//    	final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, null, selection, null, null);
//    	if(c == null) return null;
//    	while(c.moveToNext()) {
//    		track = createlocalTrackFromMediaStore(c);
//    	}
//    	c.close();
//    	return track;    	
//    }
//	
//	private boolean deleteUri(Uri uri) {
//		if(uri == null) return false;
//		String url = uri.toString();
//		if(url.startsWith("http://")) {
//			final String where = SinaMusic.Tracks.URI + "='" + url + "'";
//			mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, where, null);
//			return true;
//		}
//		if ( uri.getLastPathSegment().equals("-1")) return false;
//        final int deleteCount = mContentResolver.delete(uri, null, null);
//        if (deleteCount != 1) {
//            Log.e(TAG, "Error when delete: " + uri + ", delete count: "
//                    + deleteCount);
//            return false;
//        }
//        return true;
//    }
//
//    public void deleteTrack(LocalTrack track) {
//    	if(track == null || track.getId() == -1) return;
//        Log.d(TAG, "Delete Track." + track.toString());
//        if (track == null) {
//            Log.e(TAG, "Invalid track");
//            return;
//        }
//        Log.d(TAG, "DeleteTrack :" + track.toString() + "IS Download From Sina"
//                + track.isDownloadFormSina());
//        if (track.isDownloadFormSina()) {
//            int trackId = getIdFromIndex(track.getId());
//            if(smTracksIndex != null) {
//            	 smTracksIndex.remove(trackId);
//            }
//            
//            Uri trackUri = Uri.withAppendedPath(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, String.valueOf(trackId));
//            // Get media file path
//            final Cursor c = mContentResolver.query(trackUri, null, null, null,
//                    null);
//            if (c == null) {
//                Log.e(TAG, "Invalid tables cursor for " + trackUri + ", c: "
//                        + c);
//                return;
//            }
//            
//            if (c.getCount() == 0) {
//                c.close();
//                Log.e(TAG, "Invalid tables cursor for " + trackUri + ", c: "
//                        + c);
//                return;
//            }
//
//            c.moveToFirst();
//            // Delete from disk
//            final String path = c.getString(c
//                    .getColumnIndex(SinaMusic.Tracks.PATH));
//            final String extId = c.getString(c
//                    .getColumnIndex(SinaMusic.Tracks.EXTERNALID));
//            c.close();
//            String propPath = mFileManager.getDownloadPath()
//                    + extId + MusicProviderConstantValues.FILE_PROP;
//            deleteFile(propPath);
//            propPath = mFileManager.getDownloadPath() + extId + MusicProviderConstantValues.FILE_CRYPT;
//            deleteFile(propPath);
//            deleteFile(path);
//            // Delete from our own database
//            if (!deleteUri(trackUri)) {
//            	Log.e(TAG, "Can't delete: " + trackUri);
//            }
//        }
//        // If it's from MediaStore, delete from media store
//        if (track.isFromMediaStore()) {
//            track = (LocalTrack)getTrackById(track.getId());
//            if(track == null)  return ;
//            Log.d(TAG, "Delete Track From MediaStore...");
//            Uri mediaStoreUri = Uri.parse(track.getUri());
//            if(msTracksIndex != null) {            
//            	msTracksIndex.remove(mediaStoreUri.getLastPathSegment());
//            }            
//            String path = track.getPath();
//            deleteFile(path);            
//            if (!deleteUri(mediaStoreUri)) {
//            	Log.e(TAG, "Can't delete: " + mediaStoreUri);
//            }
//            mediaStoreUri = Uri.withAppendedPath(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, track.getIdStr());
//            if (!deleteUri(mediaStoreUri)) {
//            	Log.e(TAG, "Can't delete: " + mediaStoreUri);
//            }
//        }
//    }
//    /**
//     * Delete Remote Track From Table.
//     * 
//     * @param int id
//     * @return boolean true : delete successful. false : delete failure.
//     * 
//     */
//    public boolean deleteRemoteTrack(int id) {
//        if (id == BaseEntity.INVALID_ID) {
//            return false;
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        int row = mContentResolver.delete(uri, null, null);
//
//        return (row == 1);
//    }
//
//	private LocalTrack createlocalTrackFromMediaStore(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        final int trackId = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns._ID));   
//        final Uri uri = ContentUris.withAppendedId(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId);
//        track.setUri(uri.toString());
//        int id = getIdFromMediaStoreMemo(trackId);
//        
//        if(id != -1) {
//            track.setId(id);
//        }else {         
//            final String mime_type = c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
//            int type;
//            if(mime_type.equals(MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE)) type =MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//            else type = MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//            id = getTrackIdFromIndex(trackId, type);
//            if(id == -1) {
//                final long createTime = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//                id = saveToTrackIndex(trackId, type, createTime);
//            }           
//            track.setId(id);
//            msTracksIndex.put(trackId, id);
//        }       
//        float rating = getRatingFromMemo(id);
//        if(rating == -1){
//            track.setRating(getRatingById(track.getId()));
//        }      
//
//        String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
//        track.setName(getLocaleName(name));
//        
//        track
//                .setArtistExtId(c
//                        .getInt(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID)));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
//        track.setArtistName(getLocaleName(name));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
//        track.setAlbumName(getLocaleName(name));
//        track
//                .setDuration(c
//                        .getLong(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)));
//        track.setSize(c.getLong(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)));
//        final int albumId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
//        track.setAlbumExtId(albumId);
//        track.setPath(c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
//        track.setIsDownloadFormSina(false);
////        track.setIconPath(getAlbumArtPath(albumId));
//        if(getAlbumArtPath(albumId)!=null)
//        	track.setIconPath(getAlbumArtPath(albumId));
//        else
//        	track.setIconPath(MusicUtils.getIconPathAtSameDir(track.getPath(),track.getName()));
//        return track;
//    }
//	private LocalTrack createlocalTrackFromMediaStoreForSearch(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        final int trackId = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns._ID));   
//        final Uri uri = ContentUris.withAppendedId(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId);
//        track.setUri(uri.toString());
//        int id = getIdFromMediaStoreMemo(trackId);
//        
//        if(id != -1) {
//            track.setId(id);
//        }else {         
//            final String mime_type = c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
//            int type;
//            if(mime_type.equals(MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE)) type =MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//            else type = MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//            id = getTrackIdFromIndex(trackId, type);
//            if(id == -1) {
//                final long createTime = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//                id = saveToTrackIndex(trackId, type, createTime);
//            }           
//            track.setId(id);
//            msTracksIndex.put(trackId, id);
//        }       
//        float rating = getRatingFromMemo(id);
//        if(rating == -1){
//        	rating = getRatingById(track.getId());
//        }
//        track.setRating(rating);
//        String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
//        track.setName(getLocaleName(name));
//        
//        track
//                .setArtistExtId(c
//                        .getInt(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID)));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
//        track.setArtistName(getLocaleName(name));
//        name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
//        track.setAlbumName(getLocaleName(name));
//        track
//                .setDuration(c
//                        .getLong(c
//                                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)));
//        track.setSize(c.getLong(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)));
//        final int albumId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
//        track.setAlbumExtId(albumId);
//        track.setPath(c.getString(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
//        track.setIsDownloadFormSina(false);
////        track.setIconPath(getAlbumArtPath(albumId));
//        if(getAlbumArtPath(albumId)!=null)
//        	track.setIconPath(getAlbumArtPath(albumId));
//        else
//        	track.setIconPath(MusicUtils.getIconPathAtSameDir(track.getPath(),track.getName()));
//
//        return track;
//    }
//
//    private String getLocaleName(String _name) {
//    	String name = _name;
//    	if(MusicProviderConstantValues.TRACK_INFO_UNKOWN.equals(name)) {
//    		name = mContext.getResources().getString(R.string.track_info_unknown);
//        }
//    	return name;
//	}
//	public void deleteTrackByAlbumId(int albumId, String albumName,
//            String artistName) {
//        deleteDownloadedAlbum(albumId, albumName, artistName);
//        deleteMSAlbum(albumId);
//    }
//
//    private void deleteMSAlbum(int albumId) {
//        String selection = MediaStore.Audio.AudioColumns.ALBUM_ID + "="
//                + albumId;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null)
//            return;
//        String path;
//        int trackId;
//        String where;
//        while (c.moveToNext()) {
//        	trackId = c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns._ID));
//        	where = SinaMusic.TracksIndex.TRACKID + "=" + trackId 
//        	+ " AND " 
//        	+ SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE 
//        	+ " OR "
//        	+ SinaMusic.TracksIndex.TRACKID + "=" + trackId 
//        	+ " AND "
//        	+ SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND ;
//        	mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, where, null);
//            path = c.getString(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
//            deleteFile(path);
//        }
//        c.close();
//        mContentResolver.delete(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, selection, null);
//    }
//
//    private void deleteDownloadedAlbum(int albumId, String albumName,
//            String artistName) {
//        String selection = SinaMusic.Tracks.ALBUMID + "=" + albumId + " AND "
//                + SinaMusic.Tracks.ALBUM + "='" + replaceString(albumName)
//                + "' AND " + SinaMusic.Tracks.ARTIST + "='"
//                + replaceString(artistName) + "'" + " AND " + SinaMusic.Tracks.DOWNLOADSTATUS + "=" + MusicUtils.DOWNLOAD_COMPLETED;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null)
//            return;
//        String path;
//        final String downloadPath = mFileManager.getDownloadPath();
//        String externalId;
//        String albumArt;
//        while (c.moveToNext()) {
//        	albumArt = c.getString(c.getColumnIndex(SinaMusic.Tracks.ALBUMARTURL));
//        	deleteFile(albumArt);
//        	externalId = c.getString(c.getColumnIndex(SinaMusic.Tracks.EXTERNALID));
//            path = c.getString(c.getColumnIndex(SinaMusic.Tracks.PATH));
//            deleteFile(path);
//            path = downloadPath + "/" + externalId + MusicProviderConstantValues.FILE_PROP;
//            deleteFile(path);
//            path = downloadPath + "/" + externalId + MusicProviderConstantValues.FILE_CRYPT;
//            deleteFile(path);
//        }
//        Log.d(TAG, "Delete record from :" + selection);
//        int rows = mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                selection, null);
//        Log.d(TAG, "Deleted rows is :" + rows);
//        
//        c.close();
//    }
//
//    private boolean deleteFile(String path) {
//        boolean isDeleted = false;
//        if (path == null || path.equals(""))
//            return isDeleted;
//        Log.d(TAG, "Delete file from :" + path);
//        final File f = new File(path);
//        
//        if(f.isFile()) {
//            isDeleted = f.delete();
//        }
//        return isDeleted;
//        
//    }
//
//    public void deleteTrackByArtistId(int artistId) {
//        deleteDownloadedArtist(artistId);
//        deleteMSArtist(artistId);
//    }
//
//    private void deleteMSArtist(int artistId) {
//        String selection = MediaStore.Audio.AudioColumns.ARTIST_ID + "="
//                + artistId;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null)
//            return;
//        String path;
//        int trackId;
//        String where ;
//        while (c.moveToNext()) {
//        	
//        	trackId = c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns._ID));
//        	where = SinaMusic.TracksIndex.TRACKID + "=" + trackId 
//        	+ " AND " 
//        	+ SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE 
//        	+ " OR "
//        	+ SinaMusic.TracksIndex.TRACKID + "=" + trackId 
//        	+ " AND "
//        	+ SinaMusic.TracksIndex.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND ;
//        	mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, where, null);
//            path = c.getString(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
//            deleteFile(path);
//        }
//        c.close();
//        mContentResolver.delete(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI, selection, null);       
//    }
//
//    private void deleteDownloadedArtist(int artistId) {
//        String selection = SinaMusic.Tracks.ARTISTID + "=" + artistId + " AND " + SinaMusic.Tracks.DOWNLOADSTATUS + "=" + MusicUtils.DOWNLOAD_COMPLETED;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null)
//            return;
//        String path;
//        final String downloadPath = mFileManager.getDownloadPath();
//        String externalId;
//        String albumArt;
//        while (c.moveToNext()) {
//        	albumArt = c.getString(c.getColumnIndex(SinaMusic.Tracks.ALBUMARTURL));
//        	deleteFile(albumArt);
//        	externalId = c.getString(c.getColumnIndex(SinaMusic.Tracks.EXTERNALID));
//            path = c.getString(c.getColumnIndex(SinaMusic.Tracks.PATH));
//            deleteFile(path);
//            path = downloadPath + "/" + externalId + MusicProviderConstantValues.FILE_PROP;
//            deleteFile(path);
//            path = downloadPath + "/" + externalId + MusicProviderConstantValues.FILE_CRYPT;
//            deleteFile(path);
//        }
//        mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, selection, null);
//        c.close();
//    }
//
//    /**
//     * Update Downloaded Track Lyrics By Track Id.
//     * 
//     * @param int trackId Track Id .
//     * @param String
//     *            lyrics Track 's Lyrics.
//     */
//    public void updateTrackLyricsById(int id, String lyrics) {
//        if (TextUtils.isEmpty(lyrics)) {
//            Log.d(TAG, "Update Track Lyrics By Id, lyrics is empty.");
//            return;
//        }
//        int trackId = getIdFromIndex(id);
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.LYRIC, lyrics);
//        int rows = mContentResolver.update(uri, values, null, null);
//        Log.d(TAG, rows + "Rows was updated.");
//        updatePropTrackLyricsById(id, lyrics);
//    }
//
//    private void updatePropTrackLyricsById(int id, String lyrics) {
//        String extId = getExternalIdByTrackId(id);
//        String fileName = extId + MusicProviderConstantValues.FILE_PROP;
//        String filePath = "";
//        try {
//            filePath = mFileManager.createFile(fileName);
//
//            if (filePath != null && !filePath.equals("")) {
//                Configuration config = new Configuration(filePath);
//                String parameterName;
//                parameterName = SinaMusic.Tracks.LYRIC;
//                config.setValue(parameterName, lyrics);
//                config.savaPropertiesToFile("");
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "", e);
//        }
//    }
//
//    /**
//     * Update Downloaded Track Icon By Track Id.
//     * 
//     * @param int trackId
//     * @param InputStream
//     *            is
//     */
//    public void updateTrackIcon(int trackId, Bitmap bitmap) {
//        updateIconPathToTable(trackId, MusicProviderConstantValues.TRACK_ICON_FIELD, bitmap);
//    }
//
//    /**
//     * Update Downloaded Track Icon By Track Id.
//     * 
//     * @param int id
//     * @param byte[] icon
//     */
//    public void updateTrackIcon(int trackId, byte[] icon) {
//        updateIconPathToTable(trackId, MusicProviderConstantValues.TRACK_ICON_FIELD, icon);
//    }
//
//    /**
//     * Update Downloaded Track Album Icon By Track Id.
//     * 
//     * @param int trackId
//     * @param InputStream
//     *            is
//     */
//    public void updateAlbumIcon(int trackId, Bitmap bitmap) {
//    	Log.d(TAG, "---------updateAlbumIcon-----------" + trackId);
//        updateIconPathToTable(trackId, MusicProviderConstantValues.TRACK_ALBUM_ART_FIELD, bitmap);
//    }
//
//    /**
//     * Update Downloaded Track Album Icon By Track Id.
//     * 
//     * @param int id
//     * @param byte[] icon
//     */
//    public void updateAlbumIcon(int trackId, byte[] icon) {
//        updateIconPathToTable(trackId, MusicProviderConstantValues.TRACK_ALBUM_ART_FIELD, icon);
//    }
//
//    /**
//     * Update Downloaded Track Artist Icon By Track Id.
//     * 
//     * @param int trackId
//     * @param InputStream
//     *            is
//     */
//    public void updateArtistIcon(int trackId, Bitmap bitmap) {
//        updateIconPathToTable(trackId, MusicProviderConstantValues.TRACK_ARTIST_ART_FIELD, bitmap);
//    }
//
//    /**
//     * Update Downloaded Track Artist Icon By Track Id.
//     * 
//     * @param int Id
//     * @param byte[] icon
//     */
//    public void updateArtistIcon(int id, byte[] icon) {
//        updateIconPathToTable(id, MusicProviderConstantValues.TRACK_ARTIST_ART_FIELD, icon);
//    }
//
//    private String getImageFileName(String url) {
//    	Log.d(TAG , "cp is going to getImageFileName with url :" + url);
//        if (url == null || url.equals("")) {
//            return "";
//        }
//        if (url.startsWith(MusicProviderConstantValues.REMOTE_IMAGE_URL_BEGINS)) {
//            int pos = url.lastIndexOf("/");
//            return url.substring(pos);
//        } else {
//            return System.currentTimeMillis() + "." + MusicProviderConstantValues.DEFAULT_IMAGE_TYPE;
//        }
//    }
//
//    private void updateIconPathToTable(int id, int tableField, Bitmap bitmap) {
//        final int trackId = getIdFromIndex(id);
//        if (trackId == BaseEntity.INVALID_ID) {
//            Log.e(TAG, "Invalid track id :" + trackId);
//            return;
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final String[] projection;
//        Cursor c;
//        String imageName;
//        String path;
//        final ContentValues values = new ContentValues();
//        switch (tableField) {
//        case MusicProviderConstantValues.TRACK_ICON_FIELD:
//            projection = new String[] { SinaMusic.Tracks.ICON };
//            c = mContentResolver.query(uri, projection, null, null, null);
//            if (c == null) {
//                return;
//            }
//            if (c.getCount() == 0) {
//                c.close();
//                return;
//            }
//            c.moveToFirst();
//            imageName = getImageFileName(c.getString(0));
//            c.close();
//            path = saveImage(bitmap, imageName);
//            values.put(SinaMusic.Tracks.ICON, path);
//            break;
//        case MusicProviderConstantValues.TRACK_ALBUM_ART_FIELD:
//
//            projection = new String[] { SinaMusic.Tracks.ALBUMARTURL };
//            c = mContentResolver.query(uri, projection, null, null, null);
//            if (c == null) {
//                return;
//            }
//            if (c.getCount() == 0) {
//                c.close();
//                return;
//            }
//            c.moveToFirst();
//            imageName = getImageFileName(c.getString(0));
//            c.close();
//            Log.d(TAG, "Before Save Image : Image name is: " + imageName);
//            path = saveImage(bitmap, imageName);
//            Log.d(TAG, "After Save Image : " + path);
//            values.put(SinaMusic.Tracks.ALBUMARTURL, path);
//            updatePropAlbumIconById(id, path);
//            break;
//        case MusicProviderConstantValues.TRACK_ARTIST_ART_FIELD:
//            projection = new String[] { SinaMusic.Tracks.ARTISTARTURL };
//            c = mContentResolver.query(uri, projection, null, null, null);
//            if (c == null) {
//                return;
//            }
//            if (c.getCount() == 0) {
//                c.close();
//                return;
//            }
//            c.moveToFirst();
//            imageName = getImageFileName(c.getString(0));
//            c.close();
//            Log.d(TAG, "Before Save Image : Image name is: " + imageName);
//            path = saveImage(bitmap, imageName);
//            Log.d(TAG, "After Save Image : " + path);
//            values.put(SinaMusic.Tracks.ARTISTARTURL, path);
//            break;
//        }
//        mContentResolver.update(uri, values, null, null);
//    }
//
//    private void updatePropAlbumIconById(int id, String path) {
//        String extId = getExternalIdByTrackId(id);
//        String fileName = extId + MusicProviderConstantValues.FILE_PROP;
//        String filePath = "";
//        try {
//            filePath = mFileManager.createFile(fileName);
//            if (filePath != null && !filePath.equals("")) {
//                Configuration config = new Configuration(filePath);
//                String parameterName;
//                parameterName = SinaMusic.Tracks.ALBUMARTURL;
//                config.setValue(parameterName, path);
//                config.savaPropertiesToFile("");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void updateIconPathToTable(int id, int tableField, byte[] icon) {
//        final int trackId = getIdFromIndex(id);
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(trackId));
//        final String[] projection = new String[] { SinaMusic.Tracks.ARTISTARTURL };
//        final Cursor c = mContentResolver.query(uri, projection, null, null,
//                null);
//        if (c == null) {
//            return;
//        }
//        if (c.getCount() == 0) {
//            c.close();
//            return;
//        }
//        c.moveToFirst();
//        String imageName = getImageFileName(c.getString(0));
//        String path = saveImage(icon, imageName);
//        final ContentValues values = new ContentValues();
//        switch (tableField) {
//        case MusicProviderConstantValues.TRACK_ICON_FIELD:
//            values.put(SinaMusic.Tracks.ICON, path);
//            break;
//        case MusicProviderConstantValues.TRACK_ALBUM_ART_FIELD:
//            values.put(SinaMusic.Tracks.ALBUMARTURL, path);
//            break;
//        case MusicProviderConstantValues.TRACK_ARTIST_ART_FIELD:
//            values.put(SinaMusic.Tracks.ARTISTARTURL, path);
//            break;
//        }
//        mContentResolver.update(uri, values, null, null);
//        c.close();
//    }
//
//    private String saveImage(Bitmap bitmap, String fileName) {
//        if (TextUtils.isEmpty(fileName))
//            return "";
//        try {
//            String localPath = mFileManager.savePhoto(
//                    bitmap, fileName, FileManager.ALBUMTYPE);
//            return localPath;
//        } catch (FileNotFoundException e) {
//            Log.e(TAG , "File out found with" + fileName ,e);
//        } catch (IOException e) {
//            Log.e(TAG , "Save image Error" ,e);
//        }
//        return "";
//    }
//
//    /**
//     * Save Image .
//     * 
//     * @param byte[] imgbuffer
//     * @return image path in filesystem.
//     */
//    private String saveImage(byte[] imgbuffer, String imageName) {
//        FileOutputStream fileout;
//        try {
//            fileout = mContext.openFileOutput(imageName,
//                    Context.MODE_WORLD_WRITEABLE);
//            BufferedOutputStream bout = new BufferedOutputStream(fileout);
//            bout.write(imgbuffer);
//            bout.flush();
//            bout.close();
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            Log.d("TAG", e.getMessage());
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            Log.d("TAG", e.getMessage());
//        }
//        String path = mContext.getFilesDir().getPath() + "/" + imageName;
//        return path;
//    }
//
//    /**
//     * Judgment this track is already exsit.
//     * 
//     * @param String
//     *            externalId
//     * @return int return downloadStatus, if no record in table. return -1
//     */
//    public int isExsitTrack(String externalId) {
//        final String selection = SinaMusic.Tracks.EXTERNALID + "=" + externalId;
//        Log.d(TAG, "Judgment this track is already exsit with:" + selection);
//        final String[] projection = new String[] {
//                SinaMusic.Tracks.DOWNLOADSTATUS, SinaMusic.Tracks.DELETEFLAG };
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                projection, selection, null, null);
//        if (c == null) {
//            Log.e(TAG, "Invalid table cursor or no record in table.");
//            return -1;
//        }
//        if (c.getCount() == 0) {
//            Log.e(TAG, "Invalid table cursor or no record in table.");
//            c.close();
//            return -1;
//        }
//        if (c.getCount() > 1) {
//            Log.e(TAG, "Got a multi cursor.");
//            c.close();
//            return -1;
//        }
//        c.moveToFirst();
//        int deleteFlag = c
//                .getInt(c.getColumnIndex(SinaMusic.Tracks.DELETEFLAG));
//        if (deleteFlag == MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE) {
//            return -1;
//        }
//        int downStatus = c.getInt(c
//                .getColumnIndex(SinaMusic.Tracks.DOWNLOADSTATUS));
//        Log.d(TAG, "return status is :" + downStatus);
//        c.close();
//        return downStatus == 0 ? -1 : downStatus;
//    }
//    /*
//     * get all externalId return a hashmap add by json 0924
//     */
//    public HashMap<Integer, Integer> getAllexternalId() {
//        final HashMap<Integer, Integer> mHashMap = new HashMap<Integer, Integer>(0);
//        final String selection = SinaMusic.Tracks.DOWNLOADSTATUS + "<>0";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null) {
//            return mHashMap;
//        }
//        int mExternalid;
//        int downloadStates;
//        while (c.moveToNext()) {
//            mExternalid = c.getInt(c
//                    .getColumnIndex(SinaMusic.Tracks.EXTERNALID));
//            downloadStates = c.getInt(c
//                    .getColumnIndex(SinaMusic.Tracks.DOWNLOADSTATUS));
//            mHashMap.put(mExternalid, downloadStates);
//        }
//        c.close();
//        return mHashMap;
//    }
//
//    public HashMap<Integer, Integer> getAllPlayedexternalId() {
//        HashMap<Integer, Integer> mHashMap = new HashMap<Integer, Integer>();
//        final String selection = SinaMusic.Tracks.ISPLAYED + "=1" + " AND "
//                + SinaMusic.Tracks.TYPE + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                null, selection, null, null);
//        if (c == null) {
//            return mHashMap;
//        }
//        int mExternalid;
//        int playedStatus;
//        while (c.moveToNext()) {
//            mExternalid = c.getInt(c
//                    .getColumnIndex(SinaMusic.Tracks.EXTERNALID));
//            playedStatus = c
//                    .getInt(c.getColumnIndex(SinaMusic.Tracks.ISPLAYED));
//            mHashMap.put(mExternalid, playedStatus);
//        }
//        c.close();
//        return mHashMap;
//    }
//
//    /*
//     * GET ExternalId by trackid return externalId add by json 0925
//     */
//    public String getExternalIdByTrackId(int trackId) {
//        int id = getIdFromIndex(trackId);
//        if (id == BaseEntity.INVALID_ID) {
//            return "";
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor c = mContentResolver.query(uri,
//                new String[] { SinaMusic.Tracks.EXTERNALID }, null, null, null);
//        
//        String externalId = "";
//        if (c != null && c.moveToFirst()) {
//             externalId = c.getString(0);
//        }
//        
//        if (c!= null) c.close();
//        return externalId;
//    }
//
//    // For Playlist function
//    /**
//     * Judgement this playlist is exist in Playlist table.
//     * 
//     * @param String
//     *            playlistName
//     * @return boolean.
//     */
//    public boolean isExsitPlayList(String _playListName) {
//        Resources mRes = mContext.getResources();
//        final String[] mDefaultPlayListNames = mRes.getStringArray(R.array.music_playlist_default_list);
//        int len = mDefaultPlayListNames.length;
//        final String short_cut = mRes.getString(R.string.short_cut);
//        if(short_cut.trim().equals(_playListName.trim())) {
//            return true;
//        }
//        for (int i = 0; i < len; i++) {
//            if ( mDefaultPlayListNames[i].trim().equals(_playListName.trim())) {
//                return true;
//            }
//        }
//        String playListName = _playListName;
//        if (_playListName.indexOf("'") != -1) {
//            playListName = _playListName.replaceAll("'", "''");
//        }
//        final String selection = SinaMusic.PlayList.NAME + "='"
//                +  replaceString(playListName.trim()) + "'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                null, selection, null, null);
//        
//        boolean isExistedPlaylist = (c.getCount() != 0);
//        c.close();
//        
//        return isExistedPlaylist;
//    }
//
//    /**
//     * Create a User-Defined PlayList By playlist name.
//     * 
//     * @param String
//     *            playListName
//     * @return int playlist id in table.
//     */
//    public int createUserDefinedPlayList(String playListName) {
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.PlayList.NAME, playListName);
//        final Uri uri = mContentResolver.insert(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                values);
//        final int playListId = Integer.parseInt(uri.getLastPathSegment());
//        return playListId;
//    }
//
//    /**
//     * Add Track to exist PlayList.
//     * 
//     * @param int Playlist Id.
//     * @param int localTrack id.
//     * @return boolean. true is add successful. false is failure.
//     */
//    public void addTrackToPlayListById(int playListId, int localTrackId) {
//        if (playListId == BaseEntity.INVALID_ID
//                || localTrackId == BaseEntity.INVALID_ID) {
//            Log.d(TAG, "Invalid Id for Playlist or Track");
//            return;
//        }
//        final boolean exist = isExistTrackPlaylistMap(playListId, localTrackId);
//        if (!exist) {
//            final ContentValues values = new ContentValues();
//            values.put(SinaMusic.TrackPlayListMap.PLAYLISTID, playListId);
//            values.put(SinaMusic.TrackPlayListMap.TRACKID, localTrackId);
//            long currentTime = System.currentTimeMillis();
//            values
//                    .put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME,
//                            currentTime);
//            mContentResolver.insert(MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//                    values);
//        }
//    }
//    /**
//     * Add Tracks to exist PlayList.
//     * 
//     * @param int playListId
//     * @param List
//     *            trackIdList
//     * @return int rows :added count.
//     */
//    public int addTrackListToPlayListById(int playListId,
//            List<Integer> trackIdList) {
//        Log.d(TAG, "Add Tracks to Playlist id:" + playListId);
//        if (playListId == BaseEntity.INVALID_ID || trackIdList.size() == 0) {
//            Log.d(TAG, "Invalid Id for Playlist or Track");
//            return 0;
//        }
//        final String selection =  SinaMusic.TrackPlayListMap.PLAYLISTID + "=" + playListId;
//		final Cursor c = mContentResolver
//				.query(
//						MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//						new String[]{SinaMusic.TrackPlayListMap.TRACKID}, selection, null, null);
//		final List<Integer> ids = new ArrayList<Integer>(0);
//		while(c.moveToNext()) {
//			ids.add(c.getInt(0));
//		}
//		c.close();
//		
//        final ContentValues[] values = new ContentValues[trackIdList.size()];
//        int i = 0;
//        long currentTime;
//        for (Integer trackId : trackIdList) {
//            if(trackId == -1) continue;
//            final boolean exist = ids.contains(trackId);
//            if (!exist) {
//                final ContentValues value = new ContentValues();
//                value.put(SinaMusic.TrackPlayListMap.PLAYLISTID, playListId);
//                value.put(SinaMusic.TrackPlayListMap.TRACKID, trackId);
//                currentTime = System.currentTimeMillis();
//                value.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME,
//                        currentTime);
//                values[i] = value;
//                i++;
//            }
//        }
//        int rows = 0;
//        if (i != 0) {
//            rows = mContentResolver.bulkInsert(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, values);
//        }
//        return rows;
//    }
//
//    private boolean isExistTrackPlaylistMap(int playListId, int localTrackId) {
//        final String selection = SinaMusic.TrackPlayListMap.TRACKID + "="
//                + localTrackId + " AND "
//                + SinaMusic.TrackPlayListMap.PLAYLISTID + "=" + playListId;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, null, selection, null,
//                null);
//        boolean isExistTrackPlaylistMap = (c.getCount() != 0);
//        
//        c.close();
//        return isExistTrackPlaylistMap;
//    }
//
//    /**
//     * Delete User Defined Playlist By PlayList Id.
//     * 
//     * @param int playlistId
//     * @return boolean true : delete successful. false: delete failure.
//     */
//    public boolean deleteUserDefinedPlayList(int playlistId) {
//        if (playlistId == BaseEntity.INVALID_ID) {
//            Log.e(TAG,
//                    "Delete User Defined Playlist failure : Invalid Playlist id:"
//                            + playlistId);
//            return false;
//        }
//        if (playlistId <= PlayListBean.DEFAULT_PLAYLIST_COUNT) {
//            Log.e(TAG, "Can't Delete Default PlayList ");
//            return false;
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                String.valueOf(playlistId));
//        final int deletedCount = mContentResolver.delete(uri, null, null);
//        if (deletedCount != 1)
//            return false;
//        return true;
//    }
//
//    /**
//     * Get all user defined playlist.
//     * 
//     * @return List<PlayListBean>
//     */
//    
//    public List<PlayListBean> getAllUserDefinedPlayList() {
//        final List<PlayListBean> playlists = new ArrayList<PlayListBean>(0);
//        final String selection = SinaMusic.PlayList._ID + ">"
//                + PlayListBean.DEFAULT_PLAYLIST_COUNT;
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                null, selection, null, SinaMusic.PlayList.POSITION_ASC_SORT_ORDER);
//
//        PlayListBean playlist;
//        while (c.moveToNext()) {
//            int id = c.getInt(c.getColumnIndex(SinaMusic.PlayList._ID));
//            String name = c
//                    .getString(c.getColumnIndex(SinaMusic.PlayList.NAME));
//            playlist = new PlayListBean(id, name);
//            playlists.add(playlist);
//        }
//        playlist = new PlayListBean(PlayListBean.SHORT_CUT_PLAYLIST_ID,"");
//        playlists.add(playlist);
//        c.close();
//        return playlists;
//    }
//
//    /**
//     * Delete Tracks From PlayList.
//     * 
//     * @param int playListId
//     * @param List
//     *            <Integer> trackIdList
//     * @return boolean true:delete successful false: delete failure.
//     */
//    public boolean deleteTrackFromPlayList(int playListId,
//            List<Integer> trackIdList) {
//        final String where = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + playListId + " AND " + SinaMusic.TrackPlayListMap.TRACKID
//                + "=?";
//        int rows = 0;
//        for (Integer trackid : trackIdList) {
//            String[] selectionArgs = new String[] { String
//                    .valueOf((int) trackid) };
//            int row = mContentResolver.delete(
//                    MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, where,
//                    selectionArgs);
//            rows = rows + row;
//        }
//        if (rows == trackIdList.size()) {
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Delete All Tracks From PlayList.
//     * 
//     * @param int playListId
//     * @return boolean true:delete successful false: delete failure.
//     */
//    public boolean deleteAllTracksFromPlayList(int playListId) {
//        if (playListId == -1) {
//            Log.e(TAG, "Delete All Tracks From PlayList Id" + playListId);
//            return false;
//        }
//        final String where = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + playListId;
//        int rows = mContentResolver.delete(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, where, null);
//        Log.d(TAG, "Delete " + rows + "rows in PlayList:" + playListId);
//        return true;
//    }
//
//    /**
//     * Re-Define PlayList Name By PlayList Id.
//     * 
//     * @param int playListId
//     * @param String
//     *            name
//     * @return boolean true:re-name successful. false:re-name failure.
//     */
//    public boolean reDefinePlayListName(int playListId, String name) {
//        if (playListId == BaseEntity.INVALID_ID) {
//            Log.e(TAG,
//                    "Re-Name User Defined Playlist failure : Invalid Playlist id:"
//                            + playListId);
//            return false;
//        }
//        if (playListId <= 5) {
//            Log.e(TAG, "Can't Re-Name Default PlayList ");
//            return false;
//        }
//        final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                String.valueOf(playListId));
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.PlayList.NAME, name);
//        int rows = mContentResolver.update(uri, values, null, null);
//        
//        return (rows == 1);
//    }
//
//    /**
//     * Get Tracks By Playlist Id.
//     * 
//     * @param int playListId
//     * @return List<LocalTrack>
//     */
//    public List<LocalTrack> getTracksByPlayListId(int playListId) {
//        List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        if (playListId == -1)
//            return localTracks;
//        switch (playListId) {
//        case PlayListBean.MOST_FAVOURITE_PLAYLIST_ID:
//            localTracks = getMostFavouritePlayListTrackList();
//            break;
//        case PlayListBean.RECENT_SET_PLAYLIST_ID:
//            localTracks = getRecentSetPlayListTrackList();
//            break;
//        case PlayListBean.RECENT_DOWNLOAD_PLAYLIST_ID:
//            localTracks = getRecentDownloadedPlayListTrackList();
//            break;
//        case PlayListBean.RECORD_SOUND_PLAYLIST_ID:
//            localTracks = getRecordSoundPlayListTrackList();
//            break;
//        default:
//            localTracks = getTracksByOtherPlaylistId(playListId);
//        }
//        
//        return localTracks;
//    }
//
//    public List<LocalTrack> getTracksByPlayListIds(List<Integer> playlistIds) {
//        final List<LocalTrack> tracks = new ArrayList<LocalTrack>(0);
//          for(int playlistId : playlistIds) {
//              Log.d(TAG, "Get tracks ids by playlist id:" + playlistId);
//                  for(LocalTrack track : getTracksByPlayListId(playlistId)) {
//                          if(!tracks.contains(track)) tracks.add(track);
//                  }                        
//          }                
//          return tracks;
//    }
//    /*repair bug #38081*/
//    public List<LocalMapTrack> getTrackPlayListMapList(int playlistId) {
//        final List<LocalMapTrack> localTracks = new ArrayList<LocalMapTrack>(0);
//        if (playlistId == -1)
//            return localTracks;
//        final String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + playlistId;
//        Cursor c=null;
//		try
//		{
//			 c = mContentResolver.query(
//	                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//	                new String[] { SinaMusic.TrackPlayListMap._ID,
//	                		SinaMusic.TrackPlayListMap.TRACKID,
//	                		SinaMusic.TrackPlayListMap.PLAYLISTID,
//	                		SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME,
//	                		SinaMusic.TrackPlayListMap.POSITION}, selection,
//	                null, SinaMusic.TrackPlayListMap.POSITION_ASC_SORT_ORDER);
//	        if(c!=null&&c.getCount()>1){
//				while (c.moveToNext()) {
//					Log.d(TAG, "c.getInt(0)=:" + c.getInt(0));
//					localTracks.add(new LocalMapTrack(c.getInt(0),
//							c.getInt(1),c.getInt(2),c.getLong(3),c.getInt(4)));
//				}
//	        }
//		 }
//		 finally 
//		 {
//		     if (c != null) {
//		         c.close();
//		         return localTracks;
//		     }
//		 }	
//		 return localTracks;
//    } 
//    private List<LocalTrack> getTracksByOtherPlaylistId(int playlistId) {
//        final List<LocalTrack> localTracks = new ArrayList<LocalTrack>(0);
//        if (playlistId == -1)
//            return localTracks;
//        final String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + playlistId;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//                new String[] { SinaMusic.TrackPlayListMap.TRACKID }, selection,
//                null, SinaMusic.TrackPlayListMap.POSITION_ASC_SORT_ORDER);
//
//        int indexId;
//		final HashMap<Integer, LocalTrack> mLocalTracks = new HashMap<Integer, LocalTrack>(
//				0);
//		if (c.getCount() > MusicProviderConstantValues.PLAYLIST_TRACKS_COUNT) {
//			for (LocalTrack lt : getAllDownloadedTracks()) {
//				mLocalTracks.put(lt.getId(), lt);
//			}
//			for (LocalTrack lt : getAllMediaStoreTracks()) {
//				mLocalTracks.put(lt.getId(), lt);
//			}
//			while (c.moveToNext()) {
//				indexId = c.getInt(0);
//				if (mLocalTracks.containsKey(indexId)) {
//					localTracks.add(mLocalTracks.get(indexId));
//				} else {
//					LocalTrack track = (LocalTrack) getMiniTrackById(indexId);
//					if (track != null) {
//						localTracks.add(track);
//					}
//				}
//			}
//		} else {
//			while (c.moveToNext()) {
//				indexId = c.getInt(0);
//				LocalTrack track = (LocalTrack) getMiniTrackById(indexId);
//				if (track != null) {
//					localTracks.add(track);
//				}
//			}
//		}
//		Log.d(TAG, "Get " + localTracks.size() + " tracks By PlayList Id"
//				+ playlistId);
//        c.close();
//        return localTracks;
//    }
//    /**
//     * Get recent played track list.
//     * 
//     * @return List<LocalTrack>.
//     */
//    public List<LocalTrack> getRecentPlayedTrackList() {
//        Log.d(TAG, "Get Recent Played TrackList.");
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                + PlayListBean.R_P;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//                new String[] { SinaMusic.TrackPlayListMap.TRACKID }, selection,
//                null, SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME_SORT_ORDER);
//        
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            localTrack = (LocalTrack) getMiniTrackById(c.getInt(0));
//            if (localTrack != null)
//                trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
//
//    /**
//     * Get Recent Set Tracks from Sdcard.
//     * 
//     * @return List.
//     */
//    public List<LocalTrack> getRecentSetPlayListTrackList() {
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, null, selection, null,
//                SinaMusic.TracksIndex.CREATETIME_DESC_SORT_ORDER_LIMIT);
//        int id, trackId;
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            id = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID));
//            trackId = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex.TRACKID));            
//            localTrack = getMiniLocalTrackFromMediaStoreById(trackId);
//            if(localTrack == null) continue;        
//            localTrack.setId(id);
//            localTrack.setRating(c.getFloat(c.getColumnIndex(SinaMusic.TracksIndex.RATING)));
//            if(!trackLists.contains(localTrack))
//            	trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
//
//    /**
//     * Get Recent Downloaded PlayList track list.
//     * 
//     * @return List.
//     */
//    public List<LocalTrack> getRecentDownloadedPlayListTrackList() {
//        Log.d(TAG, "Get Recent Downloaded Playlist .");
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA ;
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks._ID,
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.RATING,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.TYPE,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//        		projection, selection, null,
//                SinaMusic.Tracks.DOWNLOADEDTIME_DESC_SORT_ORDER);
//        
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            localTrack = new LocalTrack();
//            localTrack = createMiniLocalTrackFromSinaMusic(c);
//            trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
//    public List<LocalTrack> getAllRecentDownloadedPlayListTrackList() {
//        Log.d(TAG, "Get All Recent Downloaded Playlist .");
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA 
//                + " AND " + SinaMusic.Tracks.SINAID + "= '" +  MusicUtils.getUserAccount() + "'";
//        final String[] projection = new String[] {
//        		SinaMusic.Tracks._ID,
//        		SinaMusic.Tracks.NAME,
//        		SinaMusic.Tracks.RATING,
//        		SinaMusic.Tracks.ALBUMARTURL,
//        		SinaMusic.Tracks.ARTIST,
//        		SinaMusic.Tracks.TYPE,
//        		SinaMusic.Tracks.ICON
//        		};
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//        		projection, selection, null,
//                SinaMusic.Tracks.DOWNLOADEDTIME_DESC_SORT_ORDER_All);
//        
//        LocalTrack localTrack;
//        while (c.moveToNext()) {
//            localTrack = new LocalTrack();
//            localTrack = createMiniLocalTrackFromSinaMusic(c);
//            trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
//    /**
//     * Set Rating for Track .
//     * 
//     * @param int trackId
//     * @param float rating
//     * @return boolean true : set successful. false : set failure.
//     */
//    public boolean setRatingForTrack(int trackId, String extId, float rating) {
//        Log.d(TAG, "Set Rating :" + "Track Id :" + trackId + ">>>>" + "RATING:"
//                + rating);
//        Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, String
//                .valueOf(trackId));
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.TracksIndex.RATING, rating);
//        int row = mContentResolver.update(uri, values, null, null);
//        if (row != 1)
//            return false;
//        tracksRating.put(trackId, rating);
//        int id = getIdFromIndex(trackId);
//        int type = getTypeById(trackId);
//        if(type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) {
//        	if (id != -1) {
//                uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, String
//                        .valueOf(id));
//                if (extId != null && !extId.equals("")) {
//                    updatePropValuesByKey(extId, SinaMusic.Tracks.RATING, rating);
//                }
//                values.clear();
//                values.put(SinaMusic.Tracks.RATING, rating);
//                row = mContentResolver.update(uri, values, null, null);
//                if (row == 1)
//                    return true;
//            }
//        }
//        
//        return false;
//    }
//
//    private int getTypeById(int trackId) {
//    	int type = 0;
//    	if(trackId == -1) return type;
//    	final Uri uri = Uri.withAppendedPath(SinaMusic.TracksIndex.CONTENT_URI, String.valueOf(trackId));
//    	final Cursor c = mContentResolver.query(uri, new String[] {SinaMusic.TracksIndex.TYPE}, null, null, null);
//    	
//    	if(c != null) {
//    		c.moveToFirst();
//    		type = c.getInt(0);
//    	}
//		return type;
//	}
//	private void updatePropValuesByKey(String fileName, String rating,
//            float value) {
//        String path = mFileManager.getDownloadPath()
//                + fileName + MusicProviderConstantValues.FILE_PROP;
//        Configuration config = new Configuration(path);
//        config.setValue(rating, String.valueOf(value));
//        config.savaPropertiesToFile("update rating value.");
//    }
//    /**
//     * Get most favourite playlist track list.
//     * 
//     * @return List.
//     */
//    public List<LocalTrack> getMostFavouritePlayListTrackList() {
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        String selection = SinaMusic.TracksIndex.TYPE + "<>"
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE + " AND "
//                + SinaMusic.TracksIndex.RATING + "<>0";
//        Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                null, selection, null,
//                SinaMusic.TracksIndex.RATING_DESC_SORT_ORDER);
//        
//        LocalTrack localTrack;
//        int id;
//        while (c.moveToNext()) {
//            id = c.getInt(c.getColumnIndex(SinaMusic.TracksIndex._ID));
//            localTrack = (LocalTrack) getMiniTrackById(id);
//            trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
//
//    /**
//     * Get Record Sound Playlist track list.
//     * 
//     * @return List.
//     */
//    public List<LocalTrack> getRecordSoundPlayListTrackList() {
//        final List<LocalTrack> trackLists = new ArrayList<LocalTrack>(0);
//        final String selection = MediaStore.Audio.AudioColumns.MIME_TYPE + "='"
//                + MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE + "'" 
//                + "AND " 
//                + "_data " 
//                +  " LIKE '" 
//                + MusicUtils.getSdcardPath() + MusicProviderConstantValues.RECORD_SOUND_DIC 
//                + "%'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                null, selection, null,
//                MusicProviderConstantValues.MEDIASTORE_RECORD_SOUND_CREATE_TIME_SORT_ORDER);
//        if(c == null) return trackLists;
//        LocalTrack localTrack;
//        long createTime;
//        int id;     
//        while (c.moveToNext()) {            
//            localTrack = createMiniRecordSoundLocalTrackFromMediaStore(c);
//            createTime = c.getLong(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//            id = getRecordSoundIndexId(localTrack.getId(), createTime);
//            localTrack.setId(id);
//            localTrack.setRating(getRatingById(id));            
//            trackLists.add(localTrack);
//        }
//        c.close();
//        return trackLists;
//    }
// 
//    private LocalTrack createMiniRecordSoundLocalTrackFromMediaStore(Cursor c) {
//        final LocalTrack track = new LocalTrack();
//        final int trackId = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns._ID));        
//        track.setId(trackId);  
//        String name = c.getString(c
//                .getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
//        track.setName(getLocaleName(name));
//        int duration = c.getInt(c
//                .getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
//        track.setArtistName(MusicUtils.convertTimeString(duration)
//                .toString());
//        // setPlayTime will set when it be played.
//        return track;       
//    }
//    private int getRecordSoundIndexId(int trackId, long createTime) {
//        final String selection = SinaMusic.TracksIndex.TRACKID + "=" + trackId
//                + " AND " + SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                new String[] { SinaMusic.TracksIndex._ID }, selection, null,
//                null);
//        int id = -1;
//        if (c.getCount() == 0) {
//            id = saveToTrackIndex(trackId, MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND,
//                    createTime);
//        } else if (c.getCount() >= 1) {
//            c.moveToFirst();
//            id = c.getInt(0);
//        }
//        c.close();
//        return id;
//    }
//
//    private int saveToTrackIndex(int trackId, int trackindexTypeRecordSound,
//            long createTime) {
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.TracksIndex.TRACKID, trackId);
//        values.put(SinaMusic.TracksIndex.TYPE, trackindexTypeRecordSound);
//        values.put(SinaMusic.TracksIndex.CREATETIME, createTime);
//        values.put(SinaMusic.TracksIndex.RATING, 0);
//
//        Uri uri = mContentResolver.insert(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                values);
//        return Integer.parseInt(uri.getLastPathSegment());
//    }
//
//    /**
//     * Update User-Defined Play List Position. For Sort.
//     * 
//     * @param int playListId
//     * @param int position
//     * @return boolean true : update successful. false : update failure.
//     */
//    public boolean updatePlaylistPosition(HashMap<Integer,Integer> playlistPos) {
//        if (playlistPos == null || playlistPos.isEmpty() || playlistPos.size() == 0) 
//            return false;
//        final List<PlayListBean> playlists = getAllUserDefinedPlayList();
//        String where ;
//        final ContentValues values = new ContentValues();
//        int pos;
//        int rows = 0;
//        int playlistId;
//        for(PlayListBean playlist : playlists) {            
//            playlistId = playlist.getPlayListId();
//            if (playlistPos.containsKey(playlistId)) {
//                pos = playlistPos.get(playlistId);
//                where = SinaMusic.PlayList._ID + "=" + playlistId ;
//                values.put(SinaMusic.PlayList.POSITION, pos);
//                int row = mContentResolver.update(
//                        MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI, values, where,
//                        null);
//                if (row == 1)
//                    rows++;
//            }               
//        }               
//        return (rows == playlistPos.size());        
//    }
//
//    /**
//     * Update User-Defined Play List Position. For Sort.
//     * 
//     * @param int playListId
//     * @param int position
//     * @return boolean true : update successful. false : update failure.
//     */
//    public boolean updateTracksPositionInPlayList(int playListId,
//            HashMap<Integer, Integer> trackPosition) {
//        if (playListId == -1 || trackPosition.isEmpty()) {
//            return false;
//        }
//        if (playListId < PlayListBean.DEFAULT_PLAYLIST_COUNT) {
//            Log.e(TAG, "You Can't to set Default PlayList.");
//            return false;
//        }
//        List<LocalTrack> trackList = getTracksByPlayListId(playListId);
//        String where;
//        int rows = 0;
//        final ContentValues values = new ContentValues();
//        for (LocalTrack localTrack : trackList) {
//            if (trackPosition.containsKey(localTrack.getId())) {
//                where = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                        + playListId + " AND "
//                        + SinaMusic.TrackPlayListMap.TRACKID + "="
//                        + localTrack.getId();
//                values.put(SinaMusic.TrackPlayListMap.POSITION, trackPosition
//                        .get(localTrack.getId()));
//                int row = mContentResolver.update(
//                        MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, values, where,
//                        null);
//                if (row == 1)
//                    rows++;
//            }
//        }
//        return (rows == trackPosition.size());
//    }
//
//    public List<LocalTrack> sortTracks(List<LocalTrack> tracks) {
//        if (tracks != null) {
//            Log.d(TAG, "Sort Tracks By name ,tracks size is :" + tracks.size());
//            Collections.sort(tracks, new PinyinComparator());
//        }
//        return tracks;
//    }
//
//    public List<Album> sortAlbums(List<Album> albums) {
//        if (albums != null) {
//            Log.d(TAG, "Sort Albums By name ,Albums size is :" + albums.size());
//            Collections.sort(albums, new PinyinComparator());
//        }
//        return albums;
//    }
//
//    public List<Artist> sortArtists(List<Artist> artists) {
//        if (artists != null) {
//            Log.d(TAG, "Sort Artists By name ,Artists size is :"
//                    + artists.size());
//            Collections.sort(artists, new PinyinComparator());
//        }
//        return artists;
//    }
//
//	public void deleteMSData() {
//        final String where = SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE + " OR "
//                + SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND;
//        int rows = mContentResolver.delete(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                where, null);
//        Log.d(TAG, "Delete MS Data :" + rows + " Rows");
//    }
//
//    public int getPlayListIdByName(String playListName) {
//        final String selection = SinaMusic.PlayList.NAME + "='"
//                + replaceString(playListName) + "'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_PLAYLIST_CONTENT_URI,
//                new String[] { SinaMusic.PlayList._ID }, selection, null, null);
//        int id = PlayListBean.INVALID_PLAYLIST_ID;
//        
//        if (c.moveToFirst()) {
//            id = c.getInt(0);
//        }
//        c.close();
//        return id;
//    }
//
//    public boolean updateFlag(int id, boolean flag) {
//        Log.d(TAG, "Update Flag with :" + flag);
//        final String where = SinaMusic.SharePara._ID + "=" + id;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.SharePara.FLAG, flag);
//        int rows = mContentResolver.update(SinaMusic.SharePara.CONTENT_URI,
//                values, where, null);
//        return (rows == 1);
//    }
//
//    public boolean updateScannerStarted(boolean flag) {
//        Log.d(TAG, "Update Scanner Started Flag with :" + flag);
//        final String where = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.MEDAI_SCANNER_STARTED;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.SharePara.FLAG, flag);
//        int rows = mContentResolver.update(SinaMusic.SharePara.CONTENT_URI,
//                values, where, null);
//        if (rows != 1)
//            return false;
//        return (rows == 1);
//    }
//    
//    public boolean getIsScannnerStarted() {     
//        final String selection = SinaMusic.SharePara._ID + "="+ MusicProviderConstantValues.MEDAI_SCANNER_STARTED;
//        final Cursor c = mContentResolver.query(
//                SinaMusic.SharePara.CONTENT_URI,
//                new String[] { SinaMusic.SharePara.FLAG },
//                selection,
//                null,
//                null
//                );
//        boolean isScannnerStarted = false;
//        if (c.moveToFirst()) {
//            isScannnerStarted = (c.getInt(0) == 0) ? false : true;
//        }
//        c.close();
//        Log.d(TAG, "Get Scanner Started flag:" + isScannnerStarted);
//        return isScannnerStarted;
//    }
//    
//    public boolean updateScannerFinished(boolean flag) {
//        Log.d(TAG, "Update Scanner Finished Flag with :" + flag);
//        final String where = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.MEDAI_SCANNER_FINISHED;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.SharePara.FLAG, flag);
//        int rows = mContentResolver.update(SinaMusic.SharePara.CONTENT_URI,
//                values, where, null);
//        return (rows == 1);
//    }
//
//    public boolean getIsScannerFinished() {
//
//        final String selection = SinaMusic.SharePara._ID + "="
//                + MusicProviderConstantValues.MEDAI_SCANNER_FINISHED;
//        final Cursor c = mContentResolver.query(
//                SinaMusic.SharePara.CONTENT_URI,
//                new String[] { SinaMusic.SharePara.FLAG }, selection, null,
//                null);
//        
//        boolean isScannerFinished = false;
//        if (c.moveToFirst()) {
//            isScannerFinished = (c.getInt(0) == 0) ? false : true;
//        }
//        c.close();
//        Log.d(TAG, "Get Scanner Finished flag:" + isScannerFinished);
//
//        return isScannerFinished;
//    }
//
//    public boolean addNowplayingTrackListToPlaylist(List<Integer> trackIds , int playlistId) {
//    	if(trackIds == null) return false;
//    	for (int id : trackIds) {
//    		 addTrackToPlayListById(playlistId, id);
//    	}
//        return true;
//    }
//    public void addTracksToNowPlayingList(List<LocalTrack> tracks) {
//        if (tracks == null || tracks.size() == 0)
//            return;
//        final ContentValues[] values = new ContentValues[tracks.size()];
//        int i = 0;
//        ContentValues value;
//        for (LocalTrack localTrack : tracks) {
//            value = new ContentValues();
//            value.put(SinaMusic.TrackPlayListMap.TRACKID, localTrack.getId());
//            value.put(SinaMusic.TrackPlayListMap.PLAYLISTID,
//                    PlayListBean.RECENT_PLAYED_PLAYLIST_ID);
//            value.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, System
//                    .currentTimeMillis());
//            values[i] = value;
//            i++;
//        }
//        int rows = mContentResolver.bulkInsert(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, values);
//        Log.d(TAG, "Added Tracks " + rows + " Rows To Now Playing List.");
//    }
//    
//    public void addTrackIdsToNowPlayingList(List<Integer> trackIds) {
//        if (trackIds == null || trackIds.size() == 0)
//            return;
//        final ContentValues[] values = new ContentValues[trackIds.size()];
//        int i = 0;
//        ContentValues value;
//        for (Integer id : trackIds) {
//            value = new ContentValues();
//            value.put(SinaMusic.TrackPlayListMap.TRACKID, id.intValue());
//            value.put(SinaMusic.TrackPlayListMap.PLAYLISTID,
//                    PlayListBean.RECENT_PLAYED_PLAYLIST_ID);
//            value.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, System
//                    .currentTimeMillis());
//            values[i] = value;
//            i++;
//        }
//        int rows = mContentResolver.bulkInsert(
//                MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, values);
//        Log.d(TAG, "Added Tracks " + rows + " Rows To Now Playing List.");
//    }
//
//    public List<Integer> getTracksIdByAlbum(List<Album> albums) {
//        List<Integer> ids = new ArrayList<Integer> (0);
//        for(Album album : albums) { 
//            for(int id : getAlbumTracksId(album.getExtId(), album.getName(), album.getArtistName())){
//                ids.add(id);
//            }
//        }
//        return ids;
//    }
//    
//    public List<Integer> getTracksIdByArtist(List<Artist> artists) {
//        List<Integer> ids = new ArrayList<Integer> (0);
//        for(Artist artist : artists) {  
//            for(int id : getArtistTracksId(artist.getExtId(), artist.getName())){
//                ids.add(id);
//            }
//        }
//        return ids;
//    }
//    
//    public List<Integer> getArtistTracksId(String artistId, String artistName) {    
//        List<Integer> ids = new ArrayList<Integer>(0);
//        for (int id : getSMArtistTracksId(artistId, artistName)) {
//            ids.add(id);
//        }
//        for (int id : getMSTracksIdByArtistId(artistId,artistName)) {
//            ids.add(id);
//        }
//        return ids;
//    }
//    
//    private List<Integer> getSMArtistTracksId(String artistId,
//            String artistName) {
//        final List<Integer> ids = new ArrayList<Integer>(0);
//        final String selection ;
//        if(artistId.equals("0")) {
//        	selection = SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }else {
//        	selection =  SinaMusic.Tracks.ARTISTID + "=" + artistId
//            + " OR " + SinaMusic.Tracks.ARTIST + "='"
//            + replaceString(artistName) + "' AND " + SinaMusic.Tracks.TYPE
//            + "=" + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        }   
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                new String[]{SinaMusic.Tracks._ID}, selection, null, null);
//        int id;
//        while (c.moveToNext()) {            
//            id = getIdFromSinaMusicMemo(c.getInt(0));
//            ids.add(id);
//        }
//        c.close();
//        return ids;
//    }
//    
//    private List<Integer> getMSTracksIdByArtistId(String artistId,String artist) {
//        final List<Integer> ids = new ArrayList<Integer>(0);
//        if (artist.equals(mContext.getResources().getString(
//				R.string.track_info_unknown))) {
//			artist = MusicProviderConstantValues.TRACK_INFO_UNKOWN;
//		}
//		final String selection = MediaStore.Audio.AudioColumns.ARTIST + "='"
//				+ replaceString(artist) + "'";
//        final Cursor c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                new String[]{MediaStore.Audio.AudioColumns._ID}, selection, null, null);
//        int id ;
//        if(c == null) return ids;
//        while (c.moveToNext()) {
//            id = getIdFromMediaStoreMemo(c.getInt(0));
//            ids.add(id);
//        }
//        c.close();
//        return ids;
//    }
//
//  public List<Integer> getTracksIdByPlayListId(List<Integer> playlistIds) {
//      final List<Integer> ids = new ArrayList<Integer>(0);
//      for(int playlistId : playlistIds) {
//          Log.d(TAG, "Get tracks ids by playlist id:" + playlistId);
//              for(int id:getTracksIdByPlaylistId(playlistId)) {
//                      if(!ids.contains(id))ids.add(id);
//              }                        
//      }                
//      return ids;
//  }
//  
//  private List<Integer>  getTracksIdByOtherPlaylistId(int playlistId) {
//      final List<Integer> ids = new ArrayList<Integer>(0);          
//      final String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//                      + playlistId;
//      final Cursor c = mContentResolver.query(
//                      MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//                      new String[] { SinaMusic.TrackPlayListMap.TRACKID }, selection,
//                      null, SinaMusic.TrackPlayListMap.POSITION_ASC_SORT_ORDER);
//      
//      int indexId;
//      while (c.moveToNext()) {
//              indexId = c.getInt(0);
//              ids.add(indexId);
//      }        
//      return ids;
//   }  
//    public List<Integer> getTracksIdByPlaylistId(int playlistId) {
//        List<Integer> ids = new ArrayList<Integer>(0);
//        if (playlistId == -1)
//            return ids;
//
//        switch (playlistId) {
//        case PlayListBean.MOST_FAVOURITE_PLAYLIST_ID:
//            ids = getMostFavouritePlayListTrackIdsList();
//            break;
//        case PlayListBean.RECENT_SET_PLAYLIST_ID:
//            ids = getRecentSetPlayListTrackIdsList();
//            break;
//        case PlayListBean.RECENT_DOWNLOAD_PLAYLIST_ID:
//            ids = getRecentDownloadedPlayListTrackIdsList();
//            break;
//        case PlayListBean.RECORD_SOUND_PLAYLIST_ID:
//            ids = getRecordSoundPlayListTrackIdsList();
//            break;
//        default:
//            ids = getTracksIdByOtherPlaylistId(playlistId);
//        }
//
//        return ids;
//    }
//
//    private List<Integer> getRecordSoundPlayListTrackIdsList() {
//        final List<Integer> trackIdsList = new ArrayList<Integer>(0);
//        final String selection = MediaStore.Audio.AudioColumns.MIME_TYPE + "='"
//                + MusicProviderConstantValues.SOUND_RECORD_MIME_TYPE + "'";
//        final Cursor c = mContentResolver
//                .query(
//                        MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//                        null,
//                        selection,
//                        null,
//                        MusicProviderConstantValues.MEDIASTORE_RECORD_SOUND_CREATE_TIME_SORT_ORDER);
//        if(c == null) return trackIdsList; 
//        long createTime;
//        int id;
//        int trackId;
//        while (c.moveToNext()) {
//            trackId = c.getInt(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns._ID));
//            createTime = c.getLong(c
//                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED));
//            id = getRecordSoundIndexId(trackId, createTime);
//            trackIdsList.add(id);
//        }
//        c.close();
//        return trackIdsList;
//    }
//
//    private List<Integer> getRecentDownloadedPlayListTrackIdsList() {
//        Log.d(TAG, "Get Recent Downloaded Playlist .");
//        final List<Integer> trackIdsLists = new ArrayList<Integer>(0);
//        final String selection = SinaMusic.Tracks.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//                new String[] { SinaMusic.Tracks._ID }, selection, null,
//                SinaMusic.Tracks.DOWNLOADEDTIME_DESC_SORT_ORDER);
//        getAllSMTrackIndex();
//        int id;
//        while (c.moveToNext()) {
//            id = getIdFromSinaMusicMemo(c.getInt(0));
//            trackIdsLists.add(id);
//        }
//        c.close();
//        return trackIdsLists;
//    }
//    private List<Integer> getMostFavouritePlayListTrackIdsList() {
//        final List<Integer> trackIdsList = new ArrayList<Integer>(0);
//        String selection = SinaMusic.TracksIndex.TYPE + "<>"
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_ISREMOTE + " AND "
//                + SinaMusic.TracksIndex.RATING + "<>0";
//        Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                new String[] { SinaMusic.TracksIndex._ID }, selection, null,
//                SinaMusic.TracksIndex.RATING_DESC_SORT_ORDER);
//        
//        int id;
//        while (c.moveToNext()) {
//            id = c.getInt(0);
//            trackIdsList.add(id);
//        }
//        c.close();
//        return trackIdsList;
//    }
//
//    public List<Integer> getRecentSetPlayListTrackIdsList() {
//        final List<Integer> trackIdsList = new ArrayList<Integer>(0);
//        final String selection = SinaMusic.TracksIndex.TYPE + "="
//                + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//        final Cursor c = mContentResolver.query(
//                MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                new String[] { SinaMusic.TracksIndex._ID }, selection, null,
//                SinaMusic.TracksIndex.CREATETIME_DESC_SORT_ORDER_LIMIT);
//        
//        int id;
//        while (c.moveToNext()) {
//            id = c.getInt(0);
//            trackIdsList.add(id);
//        }
//        return trackIdsList;
//    }
//    public int updateDownloadingToPause() {
//        Log.d(TAG, "Update Downloading to pause.");
//        final String selection = 
//            SinaMusic.Tracks.DOWNLOADSTATUS + "=" + MusicUtils.DOWNLOAD_ING 
//            + " AND " 
//            + SinaMusic.Tracks.DELETEFLAG + "<>" + MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.DOWNLOADSTATUS, MusicUtils.DOWNLOAD_PASUED);
//        final int rows = mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, values, selection, null);
//        Log.d(TAG, "Update " + rows + "Rows Downloading To Pause .");
//        return rows;
//    }
//    public void addTrackToRecentPlayedList(int id) {
//        if (id == BaseEntity.INVALID_ID) {
//            Log.e(TAG, "Invalid playlist record");
//            return;
//        }
//        final ContentValues values = new ContentValues();
//        long currentTime = System.currentTimeMillis();
//        final boolean isExist = isExistTrackPlaylistMap(PlayListBean.R_P, id);
//        if(isExist) {
//            final String where = SinaMusic.TrackPlayListMap.TRACKID + "="
//            + id + " AND "
//            + SinaMusic.TrackPlayListMap.PLAYLISTID + "=" + PlayListBean.R_P;
//            values.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, currentTime);
//            mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, values, where, null);
//        }else{          
//            values.put(SinaMusic.TrackPlayListMap.TRACKID, id);
//            values.put(SinaMusic.TrackPlayListMap.PLAYLISTID, PlayListBean.R_P);            
//            values.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, currentTime);
//            mContentResolver.insert(SinaMusic.TrackPlayListMap.CONTENT_URI, values);
//            values.clear();
//        }
//        
//    }
//
//    public boolean isDownloadFromSinaTrack(int id) {
//    	boolean result =  false;
//    	if( id < 1 ) return result;
//    	final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI,
//                String.valueOf(id));
//        final Cursor c = mContentResolver.query(uri, new String[]{SinaMusic.TracksIndex.TYPE}, null, null,
//                null);
//        if (c == null) {
//            Log.e(TAG, "Invalid SinaMusic TrackIndex table cursor");
//            return result;
//        }
//        int type = 0;
//        if (c.getCount() == 1) {
//            c.moveToFirst();
//            type = c.getInt(0);           
//        }
//        if (type == MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA) result = true;    
//        c.close();
//        return result;
//    }    
//    public void initAlbumArt() {    	
//    	Cursor c = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums._ID}, null, null, null);
//    	if(c != null) {
//    		int size = c.getCount();
//    		int [] albumId = new int[size];    		   		
//    		int i = 0;
//    		while(c.moveToNext()) {
//    			albumId[i] = c.getInt(0);
//    			i++;
//    		}
//    		c.close();
//    		albumArt = new HashMap<Long,String>(size);    		
//    		Uri uri;
//    		for (i=0;i< albumId.length;i++) {
//    			if (albumId[i] == -1) {
//    				continue;
//    			}
//        		uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId[i]);
//        		final Cursor cc = mContentResolver.query(uri, new String[]{"_data"}, null, null, null);
//        		if(cc != null) {
//        			if(cc.getCount() == 1 ) {
//        				cc.moveToFirst();        				
//            			albumArt.put(albumId[i], cc.getString(0));
//            			Log.d(TAG, "Key :" + albumId[i] + "value:" + cc.getString(0));
//        			}        			
//        			cc.close();         			
//        		}
//        		       		
//        	} 
//    		
//    	}
//    }
//    public boolean updateTrackDownloadStatus(int id,int status) {
//    	Log.d(TAG, "Update Download track : " + id + " to Status." + status);
//    	if (id == Track.INVALID_ID) {
//            Log.e(TAG, "update track id is not exist in databases");
//            return false;
//        }
//    	int trackId = getIdFromIndex(id);
//    	if(trackId == -1) return false;
//    	final Uri uri = Uri.withAppendedPath(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, String.valueOf(trackId));
//		final ContentValues values = new ContentValues();
//		if (status == MusicUtils.DOWNLOAD_COMPLETED) {
//			values.put(SinaMusic.Tracks.TYPE,MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//			values.put(SinaMusic.Tracks.DOWNLOADEDTIME, System
//					.currentTimeMillis());			
//	    	final Cursor c = mContentResolver.query(uri, new String[] {SinaMusic.Tracks.EXTERNALID}, null, null, null);
//			String externalId = "";
//			while (c.moveToNext()) {
//				externalId = c.getString(0);
//			}
//			c.close();
//			updateTrackIndexType(trackId);
//			updatePropFileType(externalId);
//		}
//		values.put(SinaMusic.Tracks.DOWNLOADSTATUS, status);
//		int rows = mContentResolver.update(uri, values, null, null);    	
//        if (rows == 1) return true;
//        else return false;
//        
//    }
//    public int updateAllCompleteStatus() {
//    	Log.d(TAG, "Update Downloaded to CompleteStatus.");
//        final String selection = 
//        	SinaMusic.Tracks.DELETEFLAG + "<>" + MusicProviderConstantValues.TRACK_DELETEFLAG_TRUE
//            + " AND " + SinaMusic.Tracks.DOWNLOADEDSIZE + ">=" + SinaMusic.Tracks.SIZE;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.Tracks.DOWNLOADSTATUS, MusicUtils.DOWNLOAD_COMPLETED);
//        values.put(SinaMusic.Tracks.TYPE, MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//        final int rows = mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, values, selection, null);
//        Log.d(TAG, "Update " + rows + "Rows Downloaded To DOWNLOAD_COMPLETED .");
//        if(rows > 0) {
//        	final Cursor c = mContentResolver.query(MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI, new String[] {SinaMusic.Tracks._ID,SinaMusic.Tracks.EXTERNALID}, selection, null, null);
//            int trackId = -1;
//            String externalId = "";
//            while(c.moveToNext()) {
//            	trackId = c.getInt(0);
//            	externalId = c.getString(1);
//            	updateTrackIndexType(trackId);
//            	updatePropFileType(externalId);
//            }
//            c.close();
//        }    
//        return rows;
//    }
//    private void updatePropFileType(String externalId) {
//		String fileName = externalId + MusicProviderConstantValues.FILE_PROP;
//		String filePath = "";
//		try {
//			filePath = mFileManager.createFile(fileName);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		Configuration config = new Configuration(filePath);
//		String parameterName;
//		String parameterValue;
//		if (filePath != null && !"".equals(filePath)) {
//
//			parameterName = SinaMusic.Tracks.TYPE;
//			parameterValue = String
//					.valueOf(MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//			config.setValue(parameterName, parameterValue);
//
//			parameterName = SinaMusic.Tracks.DOWNLOADEDTIME;
//			parameterValue = String.valueOf(System.currentTimeMillis());
//			config.setValue(parameterName, parameterValue);
//		}
//
//		parameterName = SinaMusic.Tracks.DOWNLOADSTATUS;
//		parameterValue = String.valueOf(MusicUtils.DOWNLOAD_COMPLETED);
//		config.setValue(parameterName, parameterValue);
//		config.savaPropertiesToFile("");
//	}
//	/**
//     * Update Index type to Complete status
//     * @param trackId
//     * @param type
//     */
//	private void updateTrackIndexType(int trackId) {
//		final String selection = SinaMusic.TracksIndex.TRACKID+ "=" + trackId + " AND " + MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE;
//        final ContentValues values = new ContentValues();
//        values.put(SinaMusic.TracksIndex.TYPE, MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA);
//        values.put(SinaMusic.TracksIndex.CREATETIME, System.currentTimeMillis());
//        mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKINDEX_CONTENT_URI, values,
//                selection, null);
//	}
//
//	public boolean isExsitInArtist(int id,int artistId) {
//
//		boolean isExsit = false;
//		if(id < 0) return false;
//		final Uri uri = Uri.withAppendedPath(SinaMusic.TracksIndex.CONTENT_URI,String.valueOf(id));
//		Cursor c = mContentResolver.query(uri, new String[] { SinaMusic.TracksIndex.TRACKID,SinaMusic.TracksIndex.TYPE }, null, null, null);
//		if(c.moveToFirst()) {
//			int trackId = c.getInt(0);
//			int type = c.getInt(1);
//			switch (type) {
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA:
//					String selection = SinaMusic.Tracks._ID + "=" + trackId + " AND " + SinaMusic.Tracks.ARTISTID + "=" + artistId;
//					c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI, null, selection, null, null);
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;	
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE:
//					selection = MediaStore.Audio.AudioColumns._ID + "=" + trackId + " AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + "=" + artistId ;
//			        c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//			                null, selection, null, null);
//			        if(c == null) return isExsit;
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND:
//					selection = MediaStore.Audio.AudioColumns._ID + "=" + trackId + " AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + "=" + artistId ;
//			        c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//			                null, selection, null, null);
//			        if(c == null) return isExsit;
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;
//			}
//		}
//		c.close();
//		return isExsit;
//		
//	}
//
//	public boolean isExsitInAlbum(int id,int albumId) {
//		boolean isExsit = false;
//		if(id < 0) return isExsit;
//		final Uri uri = Uri.withAppendedPath(SinaMusic.TracksIndex.CONTENT_URI,String.valueOf(id));
//		Cursor c = mContentResolver.query(uri, new String[] { SinaMusic.TracksIndex.TRACKID,SinaMusic.TracksIndex.TYPE }, null, null, null);
//		if(c.moveToFirst()) {
//			int trackId = c.getInt(0);
//			int type = c.getInt(1);
//			switch (type) {
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_DOWNLOADFROMSINA:
//					String selection = SinaMusic.Tracks._ID + "=" + trackId + " AND " + SinaMusic.Tracks.ALBUMID + "=" + albumId;
//					c = mContentResolver.query(SinaMusic.Tracks.CONTENT_URI, null, selection, null, null);
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;	
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_MEDIASTORE:
//					selection = MediaStore.Audio.AudioColumns._ID + "=" + trackId + " AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + albumId ;
//			        c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//			                null, selection, null, null);
//			        if(c == null) return isExsit;
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;
//				case MusicProviderConstantValues.TRACKINDEX_TYPE_RECORD_SOUND:
//					selection = MediaStore.Audio.AudioColumns._ID + "=" + trackId + " AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + albumId ;
//			        c = mContentResolver.query(MusicProviderConstantValues.MEDIASTORE_TRACKS_CONTENT_URI,
//			                null, selection, null, null);
//			        if(c == null) return isExsit;
//					if(c.getCount() >= 1 ) 
//						isExsit = true;
//					break;
//			}
//		}
//		c.close();
//		return isExsit;
//	}
//	public String getLastTimeSychro() {
//		final String selection = SinaMusic.SharePara._ID + "="
//        + MusicProviderConstantValues.LAST_TIME_SYNCHRONIZE_ID;
//		final Cursor c = mContentResolver.query(SinaMusic.SharePara.CONTENT_URI, new String[] { SinaMusic.SharePara.FLAG }, selection, null, null);
//		long lastTimeSychro = 0 ;
//		if(c.moveToFirst()) {
//			lastTimeSychro = c.getLong(0);
//		}
//		c.close();
//		return String.valueOf(lastTimeSychro);
//	}	
//	public boolean setLastTimeSychro(String imetStr) {
//	        final String where = SinaMusic.SharePara._ID + "="
//	                + MusicProviderConstantValues.LAST_TIME_SYNCHRONIZE_ID;
//	        final ContentValues values = new ContentValues();
//	        values.put(SinaMusic.SharePara.FLAG, imetStr);
//	        int rows = mContentResolver.update(SinaMusic.SharePara.CONTENT_URI,
//	                values, where, null);
//	        if (rows != 1)
//	            return false;
//	        return true;
//	}
//	private void deleteDownloadedTrackByExternalId(String extId) {		
//        // Get media file path
//		final String where = SinaMusic.Tracks.EXTERNALID + "=" + extId;
//		int row = mContentResolver.delete(
//				MusicProviderConstantValues.SINAMUSIC_TRACKS_CONTENT_URI,
//				where, null);
//		if (row > 0) {
//			String basePath = mFileManager.getDownloadPath() + extId;
//			// Delete from disk
//			String propPath = basePath + MusicProviderConstantValues.FILE_PROP;
//			deleteFile(propPath);
//			propPath = basePath + MusicProviderConstantValues.FILE_CRYPT;
//			deleteFile(propPath);
//			propPath = basePath + MusicProviderConstantValues.FILE_SM;
//			deleteFile(propPath);
//		}
//	}
//	/*repair bug #38081*/
//	public int UpdateTrackPlayListMap(List<LocalMapTrack> localTracks,int playlistId)
//	{
//		if(localTracks==null||localTracks.size()<1)
//			return 0;
//        final String selection = SinaMusic.TrackPlayListMap.PLAYLISTID + "="
//        + playlistId;
//        final Cursor c = mContentResolver.query(
//        		MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI,
//        		null, selection,
//        		null, SinaMusic.TrackPlayListMap.POSITION_ASC_SORT_ORDER);
//    	c.moveToFirst();
//    	Uri uri;
//    	ContentValues value = new ContentValues(localTracks.size());
//    	int id;
//    	for(int i=0;i<localTracks.size();i++){
//    		id = (int)localTracks.get(i).get_id();
//    		if(localTracks.get(i).gettracktype()!=playlistId)
//    			continue;
//    		
//    		final String select= SinaMusic.TrackPlayListMap._ID + "="
//            + id;
//    		value.put(SinaMusic.TrackPlayListMap._ID, localTracks.get(i).get_id());
//    		value.put(SinaMusic.TrackPlayListMap.TRACKID, localTracks.get(i).gettrackid());
//        	value.put(SinaMusic.TrackPlayListMap.PLAYLISTID, playlistId);
//        	value.put(SinaMusic.TrackPlayListMap.LAST_PLAYED_TIME, localTracks.get(i).getplaytime());
//        	value.put(SinaMusic.TrackPlayListMap.POSITION, localTracks.get(i).getposition());
//        	mContentResolver.update(MusicProviderConstantValues.SINAMUSIC_TRACKPLAYLIST_MAP_CONTENT_URI, value, select, null);
//    	}
//    	c.close();
//		return 1;
//	}
}

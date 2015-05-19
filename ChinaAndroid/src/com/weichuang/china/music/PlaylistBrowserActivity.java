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

import java.util.Iterator;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.util.ActivityHolder;
import com.weichuang.china.video.view.UserPreference;

public class PlaylistBrowserActivity extends MediaListActivity implements
		ListView.OnItemClickListener, MusicUtils.Defs {

	private static final String TAG = "PlaylistBrowserActivity";

	private static final int PLAY_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int SET_RINGTONE_ID = Menu.FIRST + 2;
	private static final int BLUETOOTH_SEND_ID = Menu.FIRST + 3;
	private static final int MAIL_SEND_ID = Menu.FIRST + 4;
	private ListView mListViewPlaylist = null;

	private long mPlaylistID;

	private boolean mAdapterSent;
	private Cursor mPlaylistCursor = null;
	private PlaylistAdapter mPlaylistAdapter = null;

	private Button mCheckAllButton;
	private Button mUncheckAllButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setTitleRightButtonHide();
	    ActivityHolder.getInstance().addActivity(this);
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(MediaPlaybackService.META_CHANGED);
		registerReceiver(mIntentReceiver, commandFilter);
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);
		mPlaylistID = MusicUtils.getPlaylistId(PlaylistBrowserActivity.this,
				false);

		mListViewPlaylist = mMediaListView;
		mListViewPlaylist.setFastScrollEnabled(false);
		mListViewPlaylist.setOnCreateContextMenuListener(this);
		mListViewPlaylist.setTextFilterEnabled(true);
		mCheckAllButton = getCheckAllButton();
		mUncheckAllButton = getUncheckAllButton();
		setupEditButtonClickListener();
		
		UserPreference.ensureIntializePreference(this);   
	    int defaultColor = UserPreference.read("defaultColor", 0);   
	    RelativeLayout background_id = (RelativeLayout)findViewById(R.id.laytout_beij); 
		if(defaultColor == 0){ 
			background_id.setBackgroundResource(R.drawable.moren_beijing); 
		}else if(defaultColor == 1){
			background_id.setBackgroundResource(R.drawable.moren_beijing1); 
	    }/*else if(defaultColor == 2){
	    	background_id.setBackgroundResource(R.drawable.moren_beijing2); 
	    }else if(defaultColor == 3){
	    	background_id.setBackgroundResource(R.drawable.moren_beijing3); 
	    }*/else{
        	background_id.setBackgroundColor(defaultColor); 
        } 
    
		mListViewPlaylist.setOnItemClickListener(this);
		setTopBarTitle("我的最爱");
	    setTitleRightButtonBackbound(R.drawable.action_play_list);
		setEditState(false);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		mAdapterSent = true;
		return mPlaylistAdapter;
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
	}

	public void onServiceDisconnected(ComponentName name) {
		// we can't really function without the service, so don't
		// finish();
	}

	private void refreshOnResume() {
		if (mPlaylistAdapter == null) {
			mPlaylistAdapter = (PlaylistAdapter) getLastNonConfigurationInstance();
		}
		if (mPlaylistAdapter == null) {
			// Log.i("@@@", "starting query");
			mPlaylistAdapter = new PlaylistAdapter(getApplication(), this,
					R.layout.playlist_item, mPlaylistCursor, new String[] {},
					new int[] {});
			mListViewPlaylist.setAdapter(mPlaylistAdapter);
			getPlaylistCursor(mPlaylistAdapter.getQueryHandler(), null);
		} else {
			if (mListViewPlaylist.getAdapter() == mPlaylistAdapter) {

				Log
						.d(TAG,
								"refreshOnResume()==============>just refresh++++++++++");
				mRefreshHandler.sendEmptyMessage(0);
				// MusicUtils.setSpinnerState(this);
			} else {
				mPlaylistAdapter.setActivity(this);
				mListViewPlaylist.setAdapter(mPlaylistAdapter);
				mPlaylistCursor = mPlaylistAdapter.getCursor();
				if (mPlaylistCursor != null) {
					init(mPlaylistCursor);
				} else {
					getPlaylistCursor(mPlaylistAdapter.getQueryHandler(), null);
				}
			}
		}
	}

	public Cursor getListCursor() {
		return mPlaylistCursor;
	}

	@Override
	public String getEmptyString() {
		// TODO Auto-generated method stub
		return this.getResources().getString(R.string.myfavourite_isnull);
	}

	

	private void setupEditButtonClickListener() {
		mCheckAllButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mPlaylistCursor != null) {
					for (int i = 0; i < mPlaylistCursor.getCount(); i++) {
						mPlaylistCursor.moveToPosition(i);
						mMultiSelectedCache
								.add(String
										.valueOf(mPlaylistCursor
												.getLong(mPlaylistCursor
														.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID))));
					}
					mRefreshHandler.sendEmptyMessage(0);
				}
			}
		});
		mUncheckAllButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mMultiSelectedCache.clear();
				mRefreshHandler.sendEmptyMessage(0);
			}
		});
	}

	private void setDeleteButtonVisibility(int visibility) {
		MusicMainActivity mma = (MusicMainActivity) getParent();
		if (mma != null) {
			mma.setDeleteButtonVisibility(visibility);
		}
	}

	View.OnClickListener mOnDeleteButtonClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			onDeleteButtonClick();
		}
	};

	private void registerDeleteButtonButtonClickListener() {
		if (mOnDeleteButtonClickListener != null) {
			MusicMainActivity mma = (MusicMainActivity) getParent();
			if (mma != null) {
				mma
						.registerDeleteButtonClickListener(mOnDeleteButtonClickListener);
			}
		}
	}

	@Override
	public void setEditState(boolean state) {
		if (state == true) {
			((TouchInterceptor) mListViewPlaylist)
					.setDropListener(mDropListener);
			((TouchInterceptor) mListViewPlaylist)
					.setRemoveListener(mRemoveListener);
		} else {
			((TouchInterceptor) mListViewPlaylist).setDropListener(null);
			((TouchInterceptor) mListViewPlaylist).setRemoveListener(null);
		}
		super.setEditState(state);
		if (mEditState) {
			setDeleteButtonVisibility(View.VISIBLE);
		} else {
			setDeleteButtonVisibility(View.GONE);
		}
	}

	private void doDeleteInPosition(int position) {
		if (mPlaylistCursor != null) {
			int temp = mPlaylistCursor.getPosition();
			mPlaylistCursor.moveToPosition(position);
			long id = mPlaylistCursor
					.getLong(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID));
			mPlaylistCursor.moveToPosition(temp);
			MusicUtils.removeFromPlaylist(PlaylistBrowserActivity.this,
					new long[] { id }, false);
			mRefreshHandler.sendEmptyMessage(0);
		}
	}

	private void deleteInPosition(final int position) {
		AlertDialog.Builder alert = new AlertDialog.Builder(
				PlaylistBrowserActivity.this);
		alert.setMessage(R.string.track_library_delete_tips);
		alert.setTitle(R.string.tips);
		alert.setNegativeButton(R.string.cancel, null);
		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new DeleteTrackTask(position, false).execute();
					}
				});
		alert.show();
	}

	private void setRingtoneInPosition(int position) {
		if (mPlaylistCursor != null) {
			int temp = mPlaylistCursor.getPosition();
			mPlaylistCursor.moveToPosition(position);
			long id = mPlaylistCursor
					.getLong(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID));
			String name = mPlaylistCursor
					.getString(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE));
			mPlaylistCursor.moveToPosition(temp);
			Uri uri = ContentUris.withAppendedId(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
			RingtoneManager.setActualDefaultRingtoneUri(this,
					RingtoneManager.TYPE_RINGTONE, uri);

			MusicUtils.showToast(this, String.format(getResources().getString(
					R.string.label_setringtone_success), MusicUtils
					.getSongName(this, name)));
		}
	}

	private void sendViaMail(int position) {
		if (mPlaylistCursor != null) {
			int temp = mPlaylistCursor.getPosition();
			mPlaylistCursor.moveToPosition(position);
			String path = mPlaylistCursor
					.getString(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DATA));
			String artist = mPlaylistCursor
					.getString(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST));
			String title = mPlaylistCursor
					.getString(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE));
			mPlaylistCursor.moveToPosition(temp);
			if (TextUtils.isEmpty(path))
				return;

			String head = "file://";
			String mime = "audio/*";
			String content = head + path;
			Uri uri = Uri.parse(content);
			Intent it = new Intent(Intent.ACTION_SEND);
			it.putExtra(Intent.EXTRA_STREAM, uri);
			it.putExtra(Intent.EXTRA_SUBJECT, artist + "-" + title);
			it.setType(mime);
			startActivity(Intent.createChooser(it, "��ѡ��Email�ͻ������"));
			// Intent intent = new Intent();
			// intent.setAction(Intent.ACTION_SEND);
			// intent.setPackage("com.android.mails");
			// Uri uri = Uri.parse(path);
			// String mime = "audio/*";
			// ArrayList<Uri> uriList = new ArrayList<Uri>();
			// uriList.add(uri);
			// intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
			// intent.setType(mime);
			// startActivity(intent);
			Log.d(TAG, "Uri: sendViaMail>>>>>" + uri.toString());
		}
	}

	private void sendViaBluetooth(int position) {
		if (mPlaylistCursor != null) {
			int temp = mPlaylistCursor.getPosition();
			mPlaylistCursor.moveToPosition(position);
			String path = mPlaylistCursor
					.getString(mPlaylistCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DATA));
			mPlaylistCursor.moveToPosition(temp);
			if (TextUtils.isEmpty(path))
				return;
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setClassName("com.android.bluetooth",
					"com.android.bluetooth.opp.BluetoothOppLauncherActivity");
			intent.setType("audio/*");
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
			Log.d(TAG, "Uri: sendViaBluetooth>>>> filepath: " + path);
			startActivity(intent);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		if (info == null) {
			return false;
		}
		switch (item.getItemId()) {
		case PLAY_ID:
			playInPosition(info.position);
			break;
		case DELETE_ID:
			deleteInPosition(info.position);
			break;
		case SET_RINGTONE_ID:
			setRingtoneInPosition(info.position);
			break;
		case BLUETOOTH_SEND_ID:
			sendViaBluetooth(info.position);
			break;
		case MAIL_SEND_ID:
			sendViaMail(info.position);
			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (!mEditState) {
			AdapterView.AdapterContextMenuInfo info;
			try {
				info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			} catch (ClassCastException e) {
				Log.e(TAG, "***********bad menuInfo************", e);
				return;
			}
			String song = null;
			if (info.targetView != null) {
				song = ((PlaylistItemCache) ((info.targetView).getTag())).songName;
			}
			if (song != null) {
				menu.setHeaderTitle(song);
			} else {
				menu.setHeaderTitle("");
			}
			menu.add(0, PLAY_ID, 0, R.string.contextmenu_item_play);
			menu.add(0, DELETE_ID, 0, R.string.contextmenu_item_delete);
			menu.add(0, SET_RINGTONE_ID, 0,
					R.string.contextmenu_item_setasringtone);
			menu.add(0, BLUETOOTH_SEND_ID, 0,
					R.string.contextmenu_item_sendviabluetooth);
			menu.add(0, MAIL_SEND_ID, 0, R.string.contextmenu_item_sendviamail);
		}
	}

	private void onDeleteButtonClick() {
		if (mMultiSelectedCache.size() == 0) {
			MusicUtils.showToast(this, getResources().getString(
					R.string.label_add_empty_item_to_playing_list));
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.track_library_delete_tips);
			alert.setTitle(R.string.tips);
			alert.setNegativeButton(R.string.cancel, null);
			alert.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							new DeleteTrackTask(true).execute();
						}
					});
			alert.show();
		}
	}

	public class DeleteTrackTask extends AsyncTask {
		ProgressDialog bar;
		private int mDeletePosition = -1;
		private boolean mIsListDelete;
		private int mDeleteCount = -1;

		public DeleteTrackTask(boolean isList) {
			mIsListDelete = isList;
		}

		public DeleteTrackTask(int position, boolean isList) {
			mDeletePosition = position;
			mIsListDelete = isList;
		}

		@Override
		protected void onPreExecute() {
			bar = new ProgressDialog(PlaylistBrowserActivity.this);
			bar.setTitle(getString(R.string.wait_please));
			bar.setCancelable(false);
			// bar.setDefaultButton(false);
			bar.show();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Object result) {
			if (null != bar && bar.isShowing()) {
				try {
					bar.cancel();
				} catch (Exception ex) {
					Log.d(TAG,
							"onPostExecute()========catch exception when cancel progress dialog: "
									+ ex);
				}
			}
			postDelete(mDeleteCount);
			super.onPostExecute(result);
		}

		@Override
		protected Object doInBackground(Object... arg0) {
			if (mIsListDelete) {
				mDeleteCount = doDelete();
			} else if (mDeletePosition >= 0) {
				doDeleteInPosition(mDeletePosition);
				mDeleteCount = 1;
			}
			return null;
		}

	}

	private void postDelete(int count) {
		String message = getResources().getQuantityString(
				R.plurals.NNNtracksdeletefrommyfavourite, count,
				Integer.valueOf(count));
		MusicUtils.showToast(this, message);
		mRefreshHandler.sendEmptyMessage(0);
	}

	private int doDelete() {
		Iterator iterator = mMultiSelectedCache.iterator();
		int count = mMultiSelectedCache.size();
		if (count <= 0) {
			return 0;
		}
		long[] ids = new long[count];
		int index = 0;
		while (iterator.hasNext()) {
			ids[index++] = Long.parseLong(iterator.next().toString());
		}
		MusicUtils.removeFromPlaylist(this, ids, false);
		mMultiSelectedCache.clear();
		return count;
	}

	@Override
	public void onDestroy() {
		if (!mAdapterSent && mPlaylistAdapter != null) {
			mPlaylistAdapter.changeCursor(null);
		}
		if (mListViewPlaylist != null) {
			mListViewPlaylist.setAdapter(null);
		}
		 ActivityHolder.getInstance().removeActivity(this);
		mPlaylistAdapter = null;
		unregisterReceiver(mScanListener);
		unregisterReceiver(mIntentReceiver);
		super.onDestroy();
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume()====================");
		super.onResume();
		if (!MusicUtils.checkSdcardAvailable()) {
			MusicUtils.showSdcardInfo(this);
			return;
		} else {
			if (MusicUtils.isMediaScannerScanning(this)) {
				Log
						.d(TAG,
								"onResume()=============media scanner is scanning.......");
				showCancelPopup(PlaylistBrowserActivity.this,
						R.string.synchronizing_sdcard,
						PlaylistBrowserActivity.this);
				if (mLoadingHandler == null) {
					mLoadingHandler = new LoadingHandler();
				}
				mLoadingHandler.sendEmptyMessageDelayed(LOADING, 200);
			} else {
				refreshOnResume();
			}
		}
		registerDeleteButtonButtonClickListener();
	}

	@Override
	public void onPause() {
		if (mReScanHandler != null) {
			mReScanHandler.removeCallbacksAndMessages(null);
		}
		if (mLoadingHandler != null) {
			mLoadingHandler.removeCallbacksAndMessages(null);
		}
		if (dialog != null && dialog.isShowing()) {
			try {
				dialog.cancel();
			} catch (Exception ex) {
				Log.d(TAG,
						"onPause()=======catch exception when cancel dialog:"
								+ ex);
			}
		}
		super.onPause();
	}

	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MusicUtils.setSpinnerState(PlaylistBrowserActivity.this);
			mReScanHandler.sendEmptyMessage(0);
		}
	};

	private Handler mReScanHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mPlaylistAdapter != null) {
				getPlaylistCursor(mPlaylistAdapter.getQueryHandler(), null);
			}
		}
	};

	private Handler mRefreshHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mPlaylistAdapter != null) {
				mPlaylistAdapter.notifyDataSetChanged();
			}
			refresh();
			showEmptyView();
		}
	};

	public void init(Cursor cursor) {

		if (mPlaylistAdapter == null) {
			return;
		}
		mPlaylistAdapter.changeCursor(cursor); // also sets mSongsCursor
		mRefreshHandler.sendEmptyMessage(0);
		if (mPlaylistCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		setTitle();
	}

	private void setTitle() {
		setTitle(R.string.playlists_title);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case SCAN_DONE:
			if (resultCode == RESULT_CANCELED) {
				finish();
			} else if (mPlaylistAdapter != null) {
				getPlaylistCursor(mPlaylistAdapter.getQueryHandler(), null);
			}
			break;
		}
	}

	private void playInPosition(int position) {
		if (MusicUtils.sService != null) {
			try {
				if (MusicUtils.sService.getCurrentPlaylistStyle() == MediaPlaybackService.FROM_PLAYLIST) {
					MusicUtils.sService.setQueuePosition(position);
				} else {
					MusicUtils.sService.open(MusicUtils
							.getSongListForCursor(mPlaylistCursor), position);
					Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
							"external", mPlaylistID);
					MusicUtils.sService.registerMonitorCursor(uri.toString(),
							null, null, null,
							MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							null, false);
					MusicUtils.sService.play();
					MusicUtils.sService
							.setCurrentPlaylistStyle(MediaPlaybackService.FROM_PLAYLIST);
				}
//				Intent intent = new Intent(
//						"com.weichuang.china.music.PLAYBACK_VIEWER")
//						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Intent intent = new Intent(PlaylistBrowserActivity.this,NowPlayingActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return;
			} catch (RemoteException ex) {
			}
		}

		MusicUtils.playAll(this, mPlaylistCursor, position);
	}

	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		if (!mEditState) {
			playInPosition(position);
		} else {
			PlaylistItemCache cache = (PlaylistItemCache) view.getTag();
			if (mMultiSelectedCache.contains(String.valueOf(cache.audioID))) {
				mMultiSelectedCache.remove(String.valueOf(cache.audioID));
				cache.selectView.setChecked(false);
			} else {
				mMultiSelectedCache.add(String.valueOf(cache.audioID));
				cache.selectView.setChecked(true);
			}
			updateEditButton();
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ((MediaPlaybackService.META_CHANGED).equals(action)) {
				// refresh();
				mRefreshHandler.sendEmptyMessage(0);
			}
		}
	};

	private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
		public void drop(int from, int to) {
			// update a saved playlist
			Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri(
					"external", Long.valueOf(mPlaylistID));
			ContentValues values = new ContentValues();
			String where = MediaStore.Audio.Playlists.Members._ID + "=?";
			String[] wherearg = new String[1];
			ContentResolver res = getContentResolver();

			int colidx = mPlaylistCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
			if (from < to) {
				// move the item to somewhere later in the list
				mPlaylistCursor.moveToPosition(to);
				long toidx = mPlaylistCursor.getLong(colidx);
				mPlaylistCursor.moveToPosition(from);
				values
						.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
								toidx);
				wherearg[0] = mPlaylistCursor.getString(0);
				res.update(baseUri, values, where, wherearg);
				for (int i = from + 1; i <= to; i++) {
					mPlaylistCursor.moveToPosition(i);
					long orginal_order = mPlaylistCursor.getLong(colidx);
					values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							orginal_order - 1);
					wherearg[0] = mPlaylistCursor.getString(0);
					res.update(baseUri, values, where, wherearg);
				}
			} else if (from > to) {
				// move the item to somewhere earlier in the list
				mPlaylistCursor.moveToPosition(to);
				long toidx = mPlaylistCursor.getLong(colidx);
				mPlaylistCursor.moveToPosition(from);
				values
						.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
								toidx);
				wherearg[0] = mPlaylistCursor.getString(0);
				res.update(baseUri, values, where, wherearg);
				for (int i = from - 1; i >= to; i--) {
					mPlaylistCursor.moveToPosition(i);
					long orginal_order = mPlaylistCursor.getLong(colidx);
					values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							orginal_order + 1);
					wherearg[0] = mPlaylistCursor.getString(0);
					res.update(baseUri, values, where, wherearg);
				}
			}
		}
	};

	private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
		public void remove(int which) {
			// removePlaylistItem(which);
		}
	};

	private Cursor getPlaylistCursor(AsyncQueryHandler async, String filter) {
		Log.d(TAG, "getPlaylistCursor()====================");
		Cursor ret = null;

		mPlaylistID = MusicUtils.getPlaylistId(PlaylistBrowserActivity.this,
				false);
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
				mPlaylistID);
		if (async != null) {
			async.startQuery(0, null, uri, null, null, null,
					MediaStore.Audio.Playlists.Members.PLAY_ORDER);
		} else {
			ret = MusicUtils.query(this, uri, null, null, null,
					MediaStore.Audio.Playlists.Members.PLAY_ORDER);
		}
		return ret;
	}

	class TAG_ID {
		public long song_id;
		public long song_id_in_playlist;
	}

	final static class PlaylistItemCache {
		public long audioID;
		public long idInPlaylist;
		public CheckBox selectView;
		public ImageView playIndicator;
		public ImageView moveIndicator;
		public TextView song;
		public TextView artist;
		public ImageView albumArt;
		public String songName;
	}

	class PlaylistAdapter extends SimpleCursorAdapter {
		private Bitmap mPlayIndicator;
		private Bitmap mMoveIndicator;
		private final BitmapDrawable mDefaultAlbumIcon = null;
		private int mArtistIdx;
		private int mAlbumIdx;
		private int mAlbumIDIdx;
		private int mTitleIdx;
		private int mDurationIdx;
		private int mAudioIDIdx;
		private int mAudioIDInPlaylistIdx;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;
		private PlaylistBrowserActivity mActivity;
		private boolean mShowCheckbox = false;

		class QueryHandler extends AsyncQueryHandler {
			QueryHandler(ContentResolver res) {
				super(res);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// Log.i("@@@", "query complete");
				mActivity.init(cursor);
			}
		}

		public PlaylistAdapter(Context context,
				PlaylistBrowserActivity currentactivity, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());
			mPlayIndicator = BitmapFactory.decodeResource(context
					.getResources(), R.drawable.indicator_ic_mp_playing_list);
			mMoveIndicator = BitmapFactory.decodeResource(context
					.getResources(), R.drawable.ic_mp_move);

			getColumnIndices(c);
		}

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST);
				mAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM);
				mAlbumIDIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM_ID);
				mTitleIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE);
				mDurationIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DURATION);
				mAudioIDIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
				mAudioIDInPlaylistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members._ID);
			}
		}

		public void setActivity(PlaylistBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public AsyncQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		public void ShowCheckbox(boolean bShow) {
			mShowCheckbox = bShow;
		}

		public boolean isShowCheckbox() {
			return mShowCheckbox;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View convertView = super.newView(context, cursor, parent);

			PlaylistItemCache cache = new PlaylistItemCache();
			cache.playIndicator = (ImageView) convertView
					.findViewById(R.id.play_indicator);
			cache.moveIndicator = (ImageView) convertView
					.findViewById(R.id.move_indicator);
			cache.song = (TextView) convertView.findViewById(R.id.songname);
			cache.artist = (TextView) convertView.findViewById(R.id.artistname);
			cache.albumArt = (ImageView) convertView
					.findViewById(R.id.albumart);
			cache.selectView = (CheckBox) convertView
					.findViewById(R.id.selectview);

			convertView.setTag(cache);

			return convertView;
		}

		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			PlaylistItemCache cache = (PlaylistItemCache) view.getTag();
			cache.idInPlaylist = cursor.getLong(mAudioIDInPlaylistIdx);
			cache.audioID = cursor.getLong(mAudioIDIdx);
			String songname = MusicUtils.getSongName(context, cursor
					.getString(mTitleIdx));
			cache.songName = songname;
			Random random = new Random();
			String temp = "" + random.nextInt();
			cache.song.setText(songname);
			cache.artist.setText(MusicUtils.getArtistName(context, cursor
					.getString(mArtistIdx)));

			if (mEditState) {
				cache.selectView.setVisibility(View.VISIBLE);
				cache.moveIndicator.setVisibility(View.VISIBLE);
				if (mMultiSelectedCache.contains(String.valueOf(cache.audioID))) {
					cache.selectView.setChecked(true);
				} else {
					cache.selectView.setChecked(false);
				}
			} else {
				cache.selectView.setVisibility(View.GONE);
				cache.moveIndicator.setVisibility(View.GONE);
			}
			if (cache.audioID == MusicUtils.getCurrentAudioId()) {
				cache.playIndicator.setVisibility(View.VISIBLE);
			} else {
				cache.playIndicator.setVisibility(View.INVISIBLE);
			}
			long album_id = cursor.getLong(mAlbumIDIdx);
			mArtLoader.setAlbumArt(cache.albumArt, album_id);

		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mPlaylistCursor) {
				mActivity.mPlaylistCursor = cursor;
				getColumnIndices(cursor);
				super.changeCursor(cursor);
			}
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String s = constraint.toString();
			if (mConstraintIsValid
					&& ((s == null && mConstraint == null) || (s != null && s
							.equals(mConstraint)))) {
				return getCursor();
			}
			Cursor c = mActivity.getPlaylistCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}
	}

	private static final int LOADING = 10001;
	private LoadingHandler mLoadingHandler;
	private ProgressDialog dialog;

	private class LoadingHandler extends Handler {
		public LoadingHandler() {
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOADING:
				if (!MusicUtils
						.isMediaScannerScanning(PlaylistBrowserActivity.this)) {
					try {
						dialog.cancel();
					} catch (Exception ex) {
						Log.d(TAG,
								"LoadingHandler->handleMessage()=======catch exception when cancel dialog:"
										+ ex);
					}
					new Runnable() {
						public void run() {
							refreshOnResume();
						}
					}.run();
				} else {
					sendEmptyMessageDelayed(LOADING, 200);
				}
				break;
			}
			super.handleMessage(msg);
		}
	}

	public void showCancelPopup(Context ctx, int stringid, final Activity act) {
		dialog = new ProgressDialog(this);
		dialog.setCancelable(true);
		dialog.setTitle(R.string.wait_please);
		dialog.setMessage(ctx.getResources().getString(stringid));
		dialog.show();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_BACK&& event.getRepeatCount() == 0 ) {
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
			return true;
		}
		return false;
	}
//	@Override
//	public int getContentViewId() {
//		return R.layout.media_tab_activity;
//	}
	
	
	@Override
	protected View setCententView() {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.media_tab_activity, null);
	}

	 
	@Override
	protected void titleLeftButton() {
		finish();
		overridePendingTransition(R.anim.fade, R.anim.hold);
	}

	@Override
	protected void titlRightButton() {
		AppConnect.getInstance(this).showOffers(this);
		
	}
}

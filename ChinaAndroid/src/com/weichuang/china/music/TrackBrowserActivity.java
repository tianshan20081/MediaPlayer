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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.china.R;
import com.waps.AppConnect;
import com.weichuang.china.util.Utils;
import com.weichuang.china.video.view.UserPreference;

public class TrackBrowserActivity extends MediaListActivity implements
		ListView.OnItemClickListener, ServiceConnection {
	private static final String TAG = "TrackBrowserActivity";
	public static TrackBrowserActivity self = null;
	private static final int PLAY_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int SET_RINGTONE_ID = Menu.FIRST + 2;
	private static final int BLUETOOTH_SEND_ID = Menu.FIRST + 3;
	private static final int MAIL_SEND_ID = Menu.FIRST + 4;

	public static final int DISPLAY_TYPE_NONE = 0;
	public static final int DISPLAY_TYPE_ALL = 1;
	public static final int DISPLAY_TYPE_ALBUM = 2;
	public static final int DISPLAY_TYPE_RECORDINGS = 3;

	// public static final Uri TRACK_SORT_URI =
	// Uri.parse("content://media/external/audio/media/sort");
	public static final Uri TRACK_SORT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	// public static final String TRACK_SORT_KEY = "sort_key";
	public static final String TRACK_SORT_KEY = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

	private Cursor mSongsCursor = null;
	private SongsListAdapter mSongsAdapter = null;
	private ListView mSongsListView = null;

	private Button mCheckAllButton;
	private Button mUncheckAllButton;
	private ToolBarButton mDeleteToolBarButton;
	private AlertDialog mContextMenuDialog;

	private ProgressDialog mProgressDialog;
	private Resources mRes;

	private boolean mAdapterSent;

	protected String[] mCursorCols = new String[] {
			MediaStore.Audio.Media._ID, // index must match IDCOLIDX below
			MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.AudioColumns.ALBUM_ID,
			MediaStore.Audio.Media.ALBUM_ID, TRACK_SORT_KEY };

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "TrackBrowserActivity =====================onCreate()");
		mRes = getResources();
		if (self == null) {
			self = TrackBrowserActivity.this;
		}
		 setTitleRightButtonHide();
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(MediaPlaybackService.META_CHANGED);
		registerReceiver(mIntentReceiver, commandFilter);
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);
		mSongsListView = mMediaListView;
		mSongsListView.setSelector(R.color.transparent_color);
		mSongsListView.setOnCreateContextMenuListener(this);
		mCheckAllButton = getCheckAllButton();
		mUncheckAllButton = getUncheckAllButton();
		setTopBarTitle("全部歌曲");
		RelativeLayout background_id = (RelativeLayout)findViewById(R.id.laytout_beij); 
		Utils.setChangeBackground(TrackBrowserActivity.this, background_id);
 	
	    setTitleRightButtonBackbound(R.drawable.action_play_list);
		setupEditButtonClickListener();
		mSongsListView.setOnItemClickListener(this);
		Log
				.d(TAG,
						"TrackBrowserActivity =====================after onCreate()");
	}

	private void refreshOnResume() {
		if (mSongsAdapter == null) {
			mSongsAdapter = (SongsListAdapter) getLastNonConfigurationInstance();
		}
		if (mSongsAdapter == null) {
			// Log.i("@@@", "starting query");
			mSongsAdapter = new SongsListAdapter(getApplication(), this,
					R.layout.songs_list_item, mSongsCursor, new String[] {},
					new int[] {});
			mSongsListView.setAdapter(mSongsAdapter);
			getSongsCursor(mSongsAdapter.getQueryHandler(), null);
		} else {
			if (mSongsListView.getAdapter() == mSongsAdapter) {
				mRefreshHandler.sendEmptyMessage(REFRESH);
				MusicUtils.setSpinnerState(this);
			} else {
				mSongsAdapter.setActivity(this);
				mSongsListView.setAdapter(mSongsAdapter);
				mSongsCursor = mSongsAdapter.getCursor();
				if (mSongsCursor != null) {
					init(mSongsCursor);
				} else {
					getSongsCursor(mSongsAdapter.getQueryHandler(), null);
				}
			}
		}
	}

	@Override
	public void setEmptyView(boolean show) {
		super.setEmptyView(show);
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		super.onConfigurationChanged(config);
	}

	@Override
	public String getEmptyString() {
		return getResources().getString(R.string.all_track_isnull);
	}

	public int getDisplayType() {
		return DISPLAY_TYPE_ALL;
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

	@Override
	public Object onRetainNonConfigurationInstance() {
		mAdapterSent = true;
		return mSongsAdapter;
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ((MediaPlaybackService.META_CHANGED).equals(action)) {
				mRefreshHandler.sendEmptyMessage(REFRESH);
			}
		}
	};

	private void setupEditButtonClickListener() {
		mCheckAllButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mSongsCursor != null) {
					for (int i = 0; i < mSongsCursor.getCount(); i++) {
						mSongsCursor.moveToPosition(i);
						mMultiSelectedCache
								.add(String
										.valueOf(mSongsCursor
												.getLong(mSongsCursor
														.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))));
					}
					mRefreshHandler.sendEmptyMessage(REFRESH);
				}
			}
		});
		mUncheckAllButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mMultiSelectedCache.clear();
				mRefreshHandler.sendEmptyMessage(REFRESH);
			}
		});
	}

	private void onDeleteButtonClick() {
		if (mMultiSelectedCache.size() == 0) {
			MusicUtils.showToast(this, getResources().getString(
					R.string.label_add_empty_item_to_playing_list));
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			int displayType = getDisplayType();
			if (displayType != DISPLAY_TYPE_RECORDINGS) {
				alert
						.setMessage(R.string.label_delete_single_track_from_list_to_sure);
			} else {
				alert
						.setMessage(R.string.label_delete_recordings_from_list_to_sure);
			}
			alert.setTitle(R.string.tips);
			alert.setNegativeButton(R.string.cancel, null);
			alert.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// deleteInThread(true, -1);
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

		public DeleteTrackTask(boolean isList) {
			mIsListDelete = isList;
		}

		public DeleteTrackTask(int position, boolean isList) {
			mDeletePosition = position;
			mIsListDelete = isList;
		}

		@Override
		protected void onPreExecute() {
			Log.d(TAG, "onPreExecute()===============");
			bar = new ProgressDialog(TrackBrowserActivity.this);
			bar.setTitle(getString(R.string.wait_please));
			bar.setCancelable(false);
			// bar.setDefaultButton(false);
			bar.show();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Object result) {
			Log.d(TAG, "onPostExecute()===============");
			if (null != bar && bar.isShowing()) {
				try {
					bar.cancel();
				} catch (Exception ex) {
					Log.d(TAG,
							"onPostExecute()========catch exception when cancel progress dialog: "
									+ ex);
				}
			}
			Log.d(TAG, "onPostExecute()===============end");
			postDelete();
			super.onPostExecute(result);
		}

		@Override
		protected Object doInBackground(Object... arg0) {
			Log.d(TAG, "doInBackground()===============begin");
			if (mIsListDelete) {
				doDelete();
			} else if (mDeletePosition >= 0) {
				doDeleteInPosition(mDeletePosition);
			}
			Log.d(TAG, "doInBackground()===============end");
			return null;
		}

	}

	private void postDelete() {
		int displayType = getDisplayType();
		if (displayType != DISPLAY_TYPE_RECORDINGS) {
			MusicUtils.showToast(this, R.string.label_delete_multi_tracks);
		} else {
			MusicUtils.showToast(this, R.string.label_delete_multi_recordings);
		}
		// if ( mSongsCursor != null ) {
		// mSongsCursor.requery();
		// }
		mRefreshHandler.sendEmptyMessage(REFRESH);
	}

	private void doDelete() {
		Iterator<String> iterator = mMultiSelectedCache.iterator();
		int count = mMultiSelectedCache.size();
		long[] ids = new long[count];
		int index = 0;
		while (iterator.hasNext()) {
			ids[index++] = Long.parseLong(iterator.next().toString());
		}
		MusicUtils.deleteTracks(this, ids);
		mMultiSelectedCache.clear();
	}

	private void doDeleteInPosition(int position) {
		if (mSongsCursor != null) {
			int temp = mSongsCursor.getPosition();
			mSongsCursor.moveToPosition(position);
			long id = mSongsCursor.getLong(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
			mSongsCursor.moveToPosition(temp);
			MusicUtils.deleteTracks(TrackBrowserActivity.this,
					new long[] { id });
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
				song = ((ViewHolder) ((info.targetView).getTag())).songName;
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

	private String getTitleInPosition(int position) {
		// TODO: use other way to get title?
		if (mSongsCursor != null) {
			int temp = mSongsCursor.getPosition();
			mSongsCursor.moveToPosition(position);
			String name = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
			mSongsCursor.moveToPosition(temp);
			return name;
		}
		return null;
	}

	private void deleteInPosition(final int position) {
		AlertDialog.Builder alert = new AlertDialog.Builder(
				TrackBrowserActivity.this);
		int displayType = getDisplayType();
		if (displayType != DISPLAY_TYPE_RECORDINGS) {
			alert
					.setMessage(R.string.label_delete_single_track_from_list_to_sure);
		} else {
			alert
					.setMessage(R.string.label_delete_recordings_from_list_to_sure);
		}
		alert.setTitle(R.string.tips);
		alert.setNegativeButton(R.string.cancel, null);
		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Log.d(TAG,
								"onClick()================start delete thread");
						// deleteInThread(false, position);
						new DeleteTrackTask(position, false).execute();
					}
				});
		alert.show();
	}

	private void setRingtoneInPosition(int position) {
		if (mSongsCursor != null) {
			int temp = mSongsCursor.getPosition();
			mSongsCursor.moveToPosition(position);
			long id = mSongsCursor.getLong(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
			String name = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
			mSongsCursor.moveToPosition(temp);
			Uri uri = ContentUris.withAppendedId(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
			RingtoneManager.setActualDefaultRingtoneUri(this,
					RingtoneManager.TYPE_RINGTONE, uri);

			if (getDisplayType() == DISPLAY_TYPE_RECORDINGS) {
				MusicUtils.showToast(this, String.format(getResources()
						.getString(R.string.label_record_setringtone_success),
						MusicUtils.getSongName(this, name)));
			} else {
				MusicUtils.showToast(this, String.format(getResources()
						.getString(R.string.label_setringtone_success),
						MusicUtils.getSongName(this, name)));
			}
		}
	}

	private void sendViaMail(int position) {
		if (mSongsCursor != null) {
			int temp = mSongsCursor.getPosition();
			mSongsCursor.moveToPosition(position);
			String path = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
			String title = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
			String artist = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
			mSongsCursor.moveToPosition(temp);
			if (TextUtils.isEmpty(path))
				return;
			String head = "file://";
			String mime = "audio/*";
			String content = head + path;
			Uri uri = Uri.parse(content);
			Intent it = new Intent(Intent.ACTION_SEND);
			it.putExtra(Intent.EXTRA_STREAM, uri);
			it.putExtra(Intent.EXTRA_SUBJECT, artist+"-"+title);
			it.setType(mime);
			startActivity(Intent.createChooser(it, "��ѡ��Email�ͻ������"));
			/*
			 * Intent intent = new Intent();
			 * intent.setAction(Intent.ACTION_SEND);
			 * intent.setPackage("com.android.mails"); Uri uri =
			 * Uri.parse(path); String mime = "audio/*"; ArrayList<Uri> uriList
			 * = new ArrayList<Uri>(); uriList.add(uri);
			 * intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
			 * intent.setType(mime); startActivity(intent);
			 */
			Log.d(TAG, "Uri: sendViaMail>>>>>" + uri.toString());
		}
	}

	private void sendViaBluetooth(int position) {
		if (mSongsCursor != null) {
			int temp = mSongsCursor.getPosition();
			mSongsCursor.moveToPosition(position);
			String path = mSongsCursor.getString(mSongsCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
			mSongsCursor.moveToPosition(temp);
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

	private void invalidateListview() {
		if (mSongsListView != null) {
			mSongsListView.invalidateViews();
		}
	}

	public void onServiceConnected(ComponentName name, IBinder service) {

	}

	public void onServiceDisconnected(ComponentName name) {
		// we can't really function without the service, so don't
		// finish();
	}

	private long[] getSongsList() {
		if (mSongsCursor != null) {
			return MusicUtils.getSongListForCursor(mSongsCursor);
		}
		return new long[0];
	}

	private void playInPosition(int position) {
		if (MusicUtils.sService != null) {
			int displayType = getDisplayType();
			try {
				if (displayType == DISPLAY_TYPE_ALL
						&& MusicUtils.sService.getCurrentPlaylistStyle() == MediaPlaybackService.FROM_SONGS) {
					MusicUtils.sService.setQueuePosition(position);
				} else {
					MusicUtils.sService.open(getSongsList(), position);
					// registerMonitorCursor();
					MusicUtils.sService.play();
					if (displayType == DISPLAY_TYPE_ALL) {
						MusicUtils.sService
								.setCurrentPlaylistStyle(MediaPlaybackService.FROM_SONGS);
					}
				}
//				Intent intent = new Intent(
////						
//						"com.weichuang.china.music.PLAYBACK_VIEWER")
//						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Intent intent = new Intent(TrackBrowserActivity.this,NowPlayingActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return;
			} catch (RemoteException ex) {
			}
		}
	}

	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		if (!mEditState) {
			playInPosition(position);
		} else {
			ViewHolder vh = (ViewHolder) view.getTag();
			if (mMultiSelectedCache.contains(String.valueOf(vh.audioID))) {
				mMultiSelectedCache.remove(String.valueOf(vh.audioID));
				vh.selectview.setChecked(false);
			} else {
				mMultiSelectedCache.add(String.valueOf(vh.audioID));
				vh.selectview.setChecked(true);
			}
			updateEditButton();
		}
	}

	@Override
	public void onDestroy() {
		// If we have an adapter and didn't send it off to another activity yet,
		// we should
		// close its cursor, which we do by assigning a null cursor to it. Doing
		// this
		// instead of closing the cursor directly keeps the framework from
		// accessing
		// the closed cursor later.
		if (!mAdapterSent && mSongsAdapter != null) {
			mSongsAdapter.changeCursor(null);
		}
		// Because we pass the adapter to the next activity, we need to make
		// sure it doesn't keep a reference to this activity. We can do this
		// by clearing its DatasetObservers, which setListAdapter(null) does.
		if (mSongsListView != null) {
			mSongsListView.setAdapter(null);
		}
		mSongsAdapter = null;
		unregisterReceiver(mScanListener);
		unregisterReceiver(mIntentReceiver);
		super.onDestroy();
	}

	@Override
	public void onResume() {
		Log.d(TAG, "TrackBrowserActivity =====================onResume()");
		super.onResume();
		// mApp.registerMediaStorageStatusChangeListener(storageStatusChangedListener);
		if (!MusicUtils.checkSdcardAvailable()) {
			int displayType = getDisplayType();
			if (displayType == DISPLAY_TYPE_ALBUM) {
				finish();
			} else {
				MusicUtils.showSdcardInfo(this);
			}
			return;
		} else {
			if (MusicUtils.isMediaScannerScanning(this)) {
				Log
						.d(TAG,
								"onResume()=============media scanner is scanning.......");
				showCancelPopup(TrackBrowserActivity.this,
						R.string.synchronizing_sdcard,
						TrackBrowserActivity.this);
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

//	@Override
//	public int getContentViewId() {
//		return R.layout.media_tab_activity;
//	}

	@Override
	public Cursor getListCursor() {
		return mSongsCursor;
	}

	public void setDeleteButtonVisibility(int visibility) {
		int displayType = getDisplayType();
		if (displayType == DISPLAY_TYPE_ALL) {
			MusicMainActivity mma = (MusicMainActivity) getParent();
			if (mma != null) {
				mma.setDeleteButtonVisibility(visibility);
			}
		} else {
			if (mDeleteToolBarButton != null) {
				mDeleteToolBarButton.setVisibility(visibility);
			}
		}
	}

	protected View.OnClickListener mOnDeleteButtonClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			onDeleteButtonClick();
		}
	};

	public void registerDeleteButtonButtonClickListener() {
		if (mOnDeleteButtonClickListener != null) {
			int displayType = getDisplayType();
			if (displayType == DISPLAY_TYPE_ALL) {
				MusicMainActivity mma = (MusicMainActivity) getParent();
				if (mma != null) {
					mma
							.registerDeleteButtonClickListener(mOnDeleteButtonClickListener);
				}
			} else {
				if (mDeleteToolBarButton != null) {
					mDeleteToolBarButton
							.setOnClickListener(mOnDeleteButtonClickListener);
				}
			}
		}
	}

	@Override
	public void setEditState(boolean edit) {
		super.setEditState(edit);
		if (mEditState) {
			setDeleteButtonVisibility(View.VISIBLE);
		} else {
			setDeleteButtonVisibility(View.GONE);
		}
	}

	/*
	 * This listener gets called when the media scanner starts up or finishes,
	 * and when the sd card is unmounted.
	 */
	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)
					|| Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
				MusicUtils.setSpinnerState(TrackBrowserActivity.this);
			}
			mReScanHandler.sendEmptyMessage(0);
		}
	};

	private Handler mReScanHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mSongsAdapter != null) {
				getSongsCursor(mSongsAdapter.getQueryHandler(), null);
			}
			// if the query results in a null cursor, onQueryComplete() will
			// call init(), which will post a delayed message to this handler
			// in order to try again.
		}
	};

	@Override
	public void showEmptyView() {
		super.showEmptyView();
		int displayType = getDisplayType();
		if (displayType == DISPLAY_TYPE_ALBUM && mSongsCursor != null) {
			if (mSongsCursor.getCount() == 0) {
				mRefreshHandler.sendEmptyMessageDelayed(FINISH, 100);
			}
		}
	}

	private static final int REFRESH = 1;
	private static final int FINISH = 2;
	private Handler mRefreshHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == REFRESH) {
				Log
						.d(TAG,
								"handleMessage()===============receive REFRESH message+++++++++++");
				if (mSongsAdapter != null) {
					mSongsAdapter.notifyDataSetChanged();
				}
				refresh();
				showEmptyView();
			} else if (msg.what == FINISH) {
				finish();
			}
		}
	};

	public void onSaveInstanceState(Bundle outcicle) {
		// need to store the selected item so we don't lose it in case
		// of an orientation switch. Otherwise we could lose it while
		// in the middle of specifying a playlist to add the item to.
		super.onSaveInstanceState(outcicle);
	}

	public void init(Cursor newCursor) {
		Log
				.d(TAG,
						"init()=====finish query, dismiss progress dialog===============");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		if (mSongsAdapter == null) {
			return;
		}
		mSongsAdapter.changeCursor(newCursor); // also sets mTrackCursor
		mRefreshHandler.sendEmptyMessage(REFRESH);
		if (mSongsCursor == null) {
			MusicUtils.displayDatabaseError(this);
			closeContextMenu();
			mReScanHandler.sendEmptyMessageDelayed(0, 1000);
			return;
		}

		MusicUtils.hideDatabaseError(this);

		setTitle();
	}

	public void setTitle() {
		setTitle(R.string.tracks_title);
	}

	private ProgressDialog createDialog(Context ctx, String title, String msg) {
		ProgressDialog dialog = new ProgressDialog(ctx);
		dialog.setMessage(msg);
		dialog.setTitle(title);
		// dialog.setOnCancelListener(listener);
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		return dialog;
	}

	public Cursor getSongsCursor(AsyncQueryHandler async, String filter) {
		Log.d(TAG, "getSongsCursor()====================");
		// NOTE: the sort order is same with the monitor cursor in
		// MediaPlaybackService
		Cursor cursor = null;
		if (async != null) {
			Log
					.d(TAG,
							"getSongsCursor()=====before start query===============");
			async.startQuery(0, null, TRACK_SORT_URI, mCursorCols, null, null,
					TRACK_SORT_KEY + " COLLATE NOCASE");
			Log.d(TAG, "getSongsCursor()=====after start query===============");

		} else {
			Log
					.d(TAG,
							"else ====getSongsCursor()=====before start query===============");
			cursor = MusicUtils.query(this, TRACK_SORT_URI, mCursorCols, null,
					null, TRACK_SORT_KEY + " COLLATE NOCASE");
		}
		return cursor != null ? new AlphabetSortCursor(cursor, TRACK_SORT_KEY)
				: null;

		// Log.d(TAG, "getSongsCursor()====================");
		// NOTE: the sort order is same with the monitor cursor in
		// MediaPlaybackService
		// Cursor cursor = null;
		// if (async != null) {
		// Log.d(TAG, "getSongsCursor()=====before start query===============");
		// async.startQuery(0, null, TRACK_SORT_URI, mCursorCols, null, null,
		// TRACK_SORT_KEY + " COLLATE NOCASE");
		// Log.d(TAG, "getSongsCursor()=====after start query===============");
		//
		// } else {
		// cursor = MusicUtils.query(this, TRACK_SORT_URI, mCursorCols, null,
		// null, TRACK_SORT_KEY + " COLLATE NOCASE");
		// }
		// return cursor != null ? new AlphabetSortCursor(cursor,
		// TRACK_SORT_KEY) : null;

	}

	final static class ViewHolder {
		View header;
		TextView header_text;
		ImageView albumart;
		TextView song;
		TextView artist;
		TextView album;
		TextView time;
		ImageView playicon;
		CheckBox selectview;
		CheckBox addicon;
		long audioID;
		String songName;
	}

	class SongsListAdapter extends SimpleCursorAdapter implements
			SectionIndexer {
		private int mAlbumIdx;
		private int mTitleIdx;
		private int mAudioIDIdx;
		private int mArtistIdx;
		private int mDurationIdx;
		private int mAlbumArtIndex;
		private int mSortKeyIdx;
		private int mAlbumIDIdx;
		private final Resources mResources;
		private final StringBuilder mStringBuilder = new StringBuilder();
		private final String mUnknownAlbum;
		private final String mUnknownArtist;
		private final String mAlbumSongSeparator;
		private final Object[] mFormatArgs = new Object[1];
		// private AlphabetIndexer mIndexer;
		private TrackBrowserActivity mActivity;
		private AsyncQueryHandler mQueryHandler;
		private String mConstraint = null;
		private boolean mConstraintIsValid = false;
		private int mNonAlphabetPosition = -1;
		private BitmapDrawable mDefaultAlbumIcon;
		private Bitmap mDefaultBitmap;
		private MusicSectionIndexer mIndexer;
		private Context mContext;

		private Bitmap play_indicator;

		class QueryHandler extends AsyncQueryHandler {
			QueryHandler(ContentResolver res) {
				super(res);
			}

			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				// Log.i("@@@", "query complete");
				// TODO: do we need to handle the case that cursor is null?
				if (cursor != null) {
					int displayType = getDisplayType();
					if (displayType == DISPLAY_TYPE_RECORDINGS) {
						mActivity.init(cursor);
					} else {
						AlphabetSortCursor sortCursor = new AlphabetSortCursor(
								cursor, TRACK_SORT_KEY);
						mActivity.init(sortCursor);
					}
				} else {
					mActivity.init(null);
				}
			}
		}

		SongsListAdapter(Context context, TrackBrowserActivity currentactivity,
				int layout, Cursor cursor, String[] from, int[] to) {
			super(context, layout, cursor, from, to);

			mContext = context;
			mActivity = currentactivity;
			mQueryHandler = new QueryHandler(context.getContentResolver());

			mUnknownAlbum = context.getString(R.string.unknown_album_name);
			mUnknownArtist = context.getString(R.string.unknown_artist_name);
			mAlbumSongSeparator = context
					.getString(R.string.albumsongseparator);

			Resources r = context.getResources();
			mResources = context.getResources();
			mDefaultBitmap = BitmapFactory.decodeResource(mResources,
					R.drawable.default_albumart);
			Bitmap b = BitmapFactory.decodeResource(mResources,
					R.drawable.default_albumart);
			mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);
			getColumnIndices(cursor);
		}

		private View.OnClickListener mAddIconListener = new View.OnClickListener() {
			public void onClick(View v) {
				CheckBox cb = (CheckBox) v;
				int type = getDisplayType();
				long id = ((Long) v.getTag()).longValue();
				if (cb.isChecked()) {
					if (type == DISPLAY_TYPE_RECORDINGS) {
						MusicUtils.addToPlaylist(mContext, new long[] { id },
								false);
						String message = mContext.getResources()
								.getQuantityString(
										R.plurals.NNNrecordstomyfavourite, 1,
										Integer.valueOf(1));
						MusicUtils.showToast(mContext, message);
					} else {
						MusicUtils.addToPlaylist(mContext, new long[] { id },
								true);
					}
				} else {
					if (type == DISPLAY_TYPE_RECORDINGS) {
						MusicUtils.removeFromPlaylist(mContext,
								new long[] { id }, false);
						String message = mContext
								.getResources()
								.getQuantityString(
										R.plurals.NNNrecordsdeletefrommyfavourite,
										1, Integer.valueOf(1));
						MusicUtils.showToast(mContext, message);
					} else {
						MusicUtils.removeFromPlaylist(mContext,
								new long[] { id }, true);
					}
				}
			}
		};

		private void getColumnIndices(Cursor cursor) {
			if (cursor != null) {
				mTitleIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
				mArtistIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
				mAlbumIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
				mAlbumIDIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID);

				mAudioIDIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				mDurationIdx = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
				// TODO: need to use the new api here
				int displayType = getDisplayType();
				if (displayType != DISPLAY_TYPE_RECORDINGS) {
					mSortKeyIdx = cursor.getColumnIndexOrThrow(TRACK_SORT_KEY);
					updateSectionIndexer(cursor);
				}
			}
		}

		private void updateSectionIndexer(Cursor cursor) {
			if (mIndexer != null) {
				mIndexer.setCursor(cursor);
			} else {
				mIndexer = new MusicSectionIndexer(cursor, mSortKeyIdx);
			}
		}

		public void setActivity(TrackBrowserActivity newactivity) {
			mActivity = newactivity;
		}

		public AsyncQueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Cursor cursor = getCursor();
			if (!cursor.moveToPosition(position)) {
				throw new IllegalStateException(
						"couldn't move cursor to position " + position);
			}

			View v;
			if (convertView == null) {
				v = newView(mContext, cursor, parent);
			} else {
				v = convertView;
			}

			bindView(v, mContext, cursor);
			int displayType = getDisplayType();
			if (displayType == DISPLAY_TYPE_ALL) {
				bindSectionHeader(v, position);
			}

			return v;
		}

		private void bindSectionHeader(View view, int position) {
			final ViewHolder vh = (ViewHolder) view.getTag();
			final int section = getSectionForPosition(position);
			String title = mIndexer.getSections()[section].toString().trim();
			if (getPositionForSection(section) == position) {

				if (!TextUtils.isEmpty(title)) {
					vh.header_text.setText(title);
					vh.header.setVisibility(View.VISIBLE);
				} else {
					vh.header.setVisibility(View.GONE);
				}
			} else {
				vh.header.setVisibility(View.GONE);
			}
		}

		public String getTitleForPosition(int position) {
			String title = "";
			if (mIndexer != null) {
				int section = getSectionForPosition(position);
				title = mIndexer.getSections()[section].toString().trim();
			}

			return title;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View convertView = super.newView(context, cursor, parent);

			ViewHolder holder = new ViewHolder();
			holder.header = (View) convertView.findViewById(R.id.header);
			holder.header_text = (TextView) convertView
					.findViewById(R.id.header_text);
			holder.albumart = (ImageView) convertView
					.findViewById(R.id.albumart);
			holder.song = (TextView) convertView.findViewById(R.id.songname);
			holder.artist = (TextView) convertView
					.findViewById(R.id.artistname);
			holder.playicon = (ImageView) convertView
					.findViewById(R.id.play_indicator);
			holder.selectview = (CheckBox) convertView
					.findViewById(R.id.selectview);
			holder.addicon = (CheckBox) convertView.findViewById(R.id.addicon);

			convertView.setTag(holder);

			return convertView;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder vh = (ViewHolder) view.getTag();
			String name = cursor.getString(mTitleIdx);
			String displayname = MusicUtils.getSongName(context, name);
			vh.song.setText(displayname);
			vh.songName = displayname;

			name = cursor.getString(mArtistIdx);
			vh.artist.setText(MusicUtils.getArtistName(context, name));

			vh.playicon.setImageBitmap(play_indicator);
			Long id = new Long(cursor.getLong(mAudioIDIdx));
			vh.audioID = id.longValue();
			if (mEditState) {
				vh.selectview.setVisibility(View.VISIBLE);
				if (mMultiSelectedCache.contains(String.valueOf(id))) {
					vh.selectview.setChecked(true);
				} else {
					vh.selectview.setChecked(false);
				}
			} else {
				vh.selectview.setVisibility(View.GONE);
			}
			if (id.longValue() == MusicUtils.getCurrentAudioId()) {
				vh.playicon.setVisibility(View.VISIBLE);
			} else {
				vh.playicon.setVisibility(View.INVISIBLE);
			}
			vh.addicon.setChecked(MusicUtils.hasAddToPlaylist(id.longValue()));
			vh.addicon.setTag(id);
			vh.addicon.setOnClickListener(mAddIconListener);
			long album_id = cursor.getLong(mAlbumIDIdx);
			mArtLoader.setAlbumArt(vh.albumart, album_id);
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (mActivity.isFinishing() && cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (cursor != mActivity.mSongsCursor) {
				mActivity.mSongsCursor = cursor;
				getColumnIndices(cursor);
				super.changeCursor(cursor);
			}
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			Log.d(TAG, "runQueryOnBackgroundThread=======================>");
			String s = constraint.toString();
			if (mConstraintIsValid
					&& ((s == null && mConstraint == null) || (s != null && s
							.equals(mConstraint)))) {
				return getCursor();
			}
			Cursor c = mActivity.getSongsCursor(null, s);
			mConstraint = s;
			mConstraintIsValid = true;
			return c;
		}

		public Object[] getSections() {
			synchronized (mIndexer) {
				if (mIndexer == null) {
					return new String[] { " " };
				} else {
					return mIndexer.getSections();
				}
			}
		}

		public int getPositionForSection(int section) {
			synchronized (mIndexer) {
				if (mIndexer == null) {
					return -1;
				}

				return mIndexer.getPositionForSection(section);
			}
		}

		public int getSectionForPosition(int position) {
			synchronized (mIndexer) {
				if (mIndexer == null) {
					return -1;
				}

				return mIndexer.getSectionForPosition(position);
			}
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
						.isMediaScannerScanning(TrackBrowserActivity.this)) {
					if (dialog != null && dialog.isShowing()) {
						try {
							dialog.cancel();
						} catch (Exception ex) {
							Log.d(TAG,
									"LoadingHandler->handleMessage()=======catch exception when cancel dialog:"
											+ ex);
						}
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

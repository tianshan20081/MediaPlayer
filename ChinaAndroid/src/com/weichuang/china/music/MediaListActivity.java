package com.weichuang.china.music;

import java.util.HashSet;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.android.china.R;
import com.weichuang.china.BaseActivity;
import com.weichuang.china.music.MusicUtils.ServiceToken;

public abstract class MediaListActivity extends BaseActivity implements OnScrollListener, ServiceConnection {
	private static final String TAG = "MediaListActivity";
	private static final int EDIT_BUTTON_WIDTH_LAND = 306;
	private static final int EDIT_BUTTON_WIDTH_PORT = 178;
	protected ListView mMediaListView = null;	
	protected ListView mInvisibleList = null;
	protected InvisibleAdapter mInvisibleAdapter = null;
	protected boolean mEditState = false;
	protected HashSet<String>  mMultiSelectedCache = new HashSet<String>();
	protected AlbumArtLoader mArtLoader;
	
	private Button mCheckAll;
	private Button mUncheckAll;
	private LinearLayout mEditSection;
	protected TextView mEmptyText;
	private ServiceToken mToken;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mArtLoader = ((MusicApplication)getApplication()).getAlbumArtLoader();
//       requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
         setVolumeControlStream(AudioManager.STREAM_MUSIC);	
         //modify by yangguangfu
		mToken = MusicUtils.bindToService(this, this);
//		setContentView(getContentViewId());
		mMediaListView = (ListView) findViewById(R.id.medialist);
		mMediaListView.setFocusableInTouchMode(false);
		mCheckAll = (Button) findViewById(R.id.edit_checkall);
		mUncheckAll = (Button) findViewById(R.id.edit_uncheck);
		mEditSection = (LinearLayout) findViewById(R.id.edit_section);	

		mEmptyText = (TextView) findViewById(R.id.panel_text_notes);
		mInvisibleList = (ListView) findViewById(R.id.invisiblelist);
		mInvisibleAdapter = new InvisibleAdapter(this, R.layout.invisible_list_item);
		mInvisibleList.setAdapter(mInvisibleAdapter);
		mMediaListView.setOnScrollListener(this);
		int orientation = getOrientation();
//		layoutByOrientation(orientation);
	}
    
    @Override
	public void finish() {
    	if ( mEditState ) {
			setEditState(false);
		} else {
			super.finish();
		}
		
	}  

    private int getOrientation() {
		return getResources().getConfiguration().orientation;
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged()===============orientation=" + newConfig.orientation);
		
//		layoutByOrientation(newConfig.orientation);
	}
	
//	private void layoutByOrientation(int orientation) {
//		if ( orientation == Configuration.ORIENTATION_LANDSCAPE) {
//			RelativeLayout.LayoutParams checkAllParams = new RelativeLayout.LayoutParams( EDIT_BUTTON_WIDTH_LAND, LinearLayout.LayoutParams.WRAP_CONTENT);
//			checkAllParams.setMargins( 89, 8, 10, 0 );
//			checkAllParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//			if ( mCheckAll != null ) {
//				mCheckAll.setLayoutParams(checkAllParams);
//			}
//			checkAllParams = new RelativeLayout.LayoutParams( EDIT_BUTTON_WIDTH_LAND, LinearLayout.LayoutParams.WRAP_CONTENT);
//			checkAllParams.addRule(RelativeLayout.RIGHT_OF, R.id.edit_checkall);
//			checkAllParams.setMargins( 0, 8, 89, 0 );
//			checkAllParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//			if ( mUncheckAll != null ) {
//				mUncheckAll.setLayoutParams(checkAllParams);
//			}
//		} else if ( orientation == Configuration.ORIENTATION_PORTRAIT) {
//			RelativeLayout.LayoutParams checkAllParams = new RelativeLayout.LayoutParams( EDIT_BUTTON_WIDTH_PORT, LinearLayout.LayoutParams.WRAP_CONTENT);
//			checkAllParams.setMargins( 52, 8, 10, 0 );
//			checkAllParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//			
//			if ( mCheckAll != null ) {
//				mCheckAll.setLayoutParams(checkAllParams);
//			}
//			checkAllParams = new RelativeLayout.LayoutParams( EDIT_BUTTON_WIDTH_PORT, LinearLayout.LayoutParams.WRAP_CONTENT);
//			checkAllParams.addRule(RelativeLayout.RIGHT_OF, R.id.edit_checkall);
//			checkAllParams.setMargins( 0, 8, 52, 0 );
//			checkAllParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//			if ( mUncheckAll != null ) {
//				mUncheckAll.setLayoutParams(checkAllParams);
//			}
//		}
//	}

	public void refresh() {
		if ( mInvisibleAdapter != null ) {
			mInvisibleAdapter.notifyDataSetChanged();
		}
		updateEditButton();
    }

	public void setEmptyView(boolean show) {
		if ( show ) {
			mEmptyText.setText(getEmptyString());
			mEmptyText.setVisibility(View.VISIBLE);
		} else {
			mEmptyText.setText(getEmptyString());
			mEmptyText.setVisibility(View.GONE);
		}
	}
	
	public void showEmptyView() {
		Cursor cursor = getListCursor();
    	if ( cursor != null ) {
    		if ( cursor.getCount() == 0 ) {
    			if ( mEditState ) {
    				setEditState(false);
    			}
    			setEmptyView(true);
    		} else {
    			setEmptyView(false);
    		}
		}
	}
	
    protected Button getCheckAllButton() {
    	return mCheckAll;
    }
    
    protected Button getUncheckAllButton() {
    	return mUncheckAll;
    }
    
    
	abstract public void onServiceConnected(ComponentName name, IBinder service);
	abstract public void onServiceDisconnected(ComponentName name);
	abstract public Cursor getListCursor();
//	abstract public int getContentViewId();
	abstract public String getEmptyString();
    
    @Override
    public void onDestroy() {
    	if ( mToken != null ) {
    		MusicUtils.unbindFromService(mToken);
    	}
        super.onDestroy();
    }

	@Override
	protected void onResume() {
		super.onResume();
		mArtLoader.resume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mArtLoader.pause();
	}
	
	public boolean isEditState() {
		return mEditState;
	}
	
    public void updateEditButton() {
    	Cursor cursor = getListCursor();
    	if ( cursor != null ) {
			if ( mEditState ) {
				mUncheckAll.setEnabled(!mMultiSelectedCache.isEmpty());
				mCheckAll.setEnabled(mMultiSelectedCache.size() != cursor.getCount());
			} 		
		}
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	Cursor cursor = getListCursor();
    	if ( cursor != null ) {
			boolean disable = cursor.getCount() <= 0 || mEditState;
			menu.getItem(0).setEnabled(!disable);
			return true;
		}
        return false;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);		
		menu.add(0, 0, 0, getResources().getString(
				R.string.menu_edit));
		menu.getItem(0).setIcon(R.drawable.menu_edit);
		menu.add(0, 1, 0, getResources().getString(
				R.string.menu_player));
		menu.getItem(1).setIcon(R.drawable.menu_player);
		return true;
	}
    
	public boolean onOptionsItemSelected(MenuItem item) {
		if ( item.getItemId() == 0 ) {
			if ( !mEditState ) {
				setEditState(!mEditState);
			}
		} else if ( item.getItemId() == 1 ) {
			MusicUtils.goToPlayer(this);
		}
		return super.onOptionsItemSelected(item);
}

	public void setEditState(boolean edit) {
    	if ( mEditState != edit ) {
    		mEditState = edit;
    		if ( mEditState ) {
    			if ( mEditSection != null ) {
    				mEditSection.setVisibility(View.VISIBLE);
    			}
    		} else {
    			if ( mEditSection != null ) {
    				mEditSection.setVisibility(View.GONE);
    			}
    			mMultiSelectedCache.clear();
    		}
    		updateEditButton();
    	}
    }
	
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    	if ( scrollState == OnScrollListener.SCROLL_STATE_IDLE ) {
    		mArtLoader.resume();
    	} else {
    		mArtLoader.pause();
    	}
    }

}

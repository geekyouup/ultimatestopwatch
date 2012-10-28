package com.geekyouup.android.ustopwatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.geekyouup.android.ustopwatch.fragments.*;

public class UltimateStopwatchActivity extends SherlockFragmentActivity {

	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock;

	public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
	public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
    public static final String MSG_STATE_CHANGE = "msg_state_change";
    private static final String KEY_AUDIO_STATE = "key_audio_state";
	private static final String WAKE_LOCK_KEY = "ustopwatch";
    public static final String PREFS_NAME="USW_SWFRAG_PREFS";

	public static final boolean IS_HONEYCOMB_OR_ABOVE=android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	private LapTimesFragment mLapTimesFragment;
    private CountdownFragment mCountdownFragment;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private static boolean mAudioOn = true;
    private Menu mMenu;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        getSupportActionBar().setIcon(R.drawable.icon);
        setTitle(getString(R.string.app_name_caps));
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setOffscreenPageLimit(2);

        ActionBar.Tab tab1 = getSupportActionBar().newTab().setText(getString(R.string.stopwatch));
        ActionBar.Tab tab2 = getSupportActionBar().newTab().setText(getString(R.string.laptimes));
        ActionBar.Tab tab3 = getSupportActionBar().newTab().setText(getString(R.string.countdown));

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(tab1,StopwatchFragment.class,null);
        mTabsAdapter.addTab(tab2,LapTimesFragment.class,null);
        mTabsAdapter.addTab(tab3, CountdownFragment.class,null);

		mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// stop landscape more on QVGA/HVGA
		int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_AUDIO_STATE,mAudioOn);
        editor.commit();

		LapTimeRecorder.getInstance().saveTimes(this);
	}

    @Override
	protected void onResume() {
		super.onResume();

		LapTimeRecorder.getInstance().loadTimes(this);

		mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				WAKE_LOCK_KEY);
		mWakeLock.acquire();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAudioOn = settings.getBoolean(KEY_AUDIO_STATE,true);

        if(mMenu!= null)
        {
            MenuItem audioButton = mMenu.findItem(R.id.menu_audiotoggle);
            if(audioButton!=null) audioButton.setIcon(mAudioOn?R.drawable.audio_on:R.drawable.audio_off);
        }
    }

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getSupportMenuInflater();

        int currentTab = mTabsAdapter.getCurrentTabNum();
        if(currentTab == 2)
        {
            inflater.inflate(R.menu.menu_countdown, menu);
        }else
        {
            inflater.inflate(R.menu.menu, menu);
        }

        //get audio icon and set correct varient
        MenuItem audioButton = menu.findItem(R.id.menu_audiotoggle);
        if(audioButton!=null) audioButton.setIcon(mAudioOn?R.drawable.audio_on:R.drawable.audio_off);
        mMenu=menu;
        return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_rateapp)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.play_store_uri)));
            startActivity(intent);
        }else if(item.getItemId() == R.id.menu_clearlaps)
        {
            LapTimeRecorder.getInstance().reset(this);
        }else if(item.getItemId() == R.id.menu_resettime)
        {
            //get hold of countdown fragment and call reset, call back to here?
            if(mCountdownFragment!=null) mCountdownFragment.requestTimeDialog();
        }else if(item.getItemId() == R.id.menu_audiotoggle)
        {
            mAudioOn = !mAudioOn;
            item.setIcon(mAudioOn?R.drawable.audio_on:R.drawable.audio_off);
        }

        return true;
    }

    public void registerLapTimeFragment(LapTimesFragment ltf)
    {
        mLapTimesFragment = ltf;
    }

    public LapTimesFragment getLapTimeFragment()
    {
        return mLapTimesFragment;
    }

    public void registerCountdownFragment(CountdownFragment cdf)
    {
        mCountdownFragment = cdf;
    }

    public static boolean isAudioOn()
    {
        return mAudioOn;
    }

}
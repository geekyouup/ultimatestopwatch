package com.geekyouup.android.ustopwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.geekyouup.android.ustopwatch.fragments.*;

public class UltimateStopwatchActivity extends AppCompatActivity {

    private PowerManager mPowerMan;
    private PowerManager.WakeLock mWakeLock;

    public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
    public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
    public static final String MSG_STATE_CHANGE = "msg_state_change";
    private static final String KEY_AUDIO_STATE = "key_audio_state";
    private static final String KEY_JUMP_TO_PAGE = "key_start_page";
    private static final String WAKE_LOCK_KEY = "ustopwatch:wl";
    public static final String PREFS_NAME = "USW_PREFS";
    private static final int INTENT_SETTINGS = 0;

    private LapTimesFragment mLapTimesFragment;
    private CountdownFragment mCountdownFragment;
    private StopwatchFragment mStopwatchFragment;
    private SoundManager mSoundManager;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private Menu mMenu;
    private boolean isLapTimesEnabled = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().setBackgroundDrawable(null);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        setTitle("");

        mSoundManager = SoundManager.getInstance(this);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setOffscreenPageLimit(2);

        setupTabs();

        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // stop landscape on QVGA/HVGA
        int screenSize = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //If launched from Countdown notification then goto countdown clock directly
        if (getIntent() != null && getIntent().getBooleanExtra(AlarmUpdater.INTENT_EXTRA_LAUNCH_COUNTDOWN, false)) {
            mViewPager.setCurrentItem(2);
        }
    }

    private void setupTabs() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SettingsActivity.loadSettings(settings);

        TabLayout tl = (TabLayout) findViewById(R.id.tablayout);//new TabLayout(this);

        mTabsAdapter = new TabsAdapter(this);
        mTabsAdapter.addTab(getString(R.string.stopwatch), StopwatchFragment.class, null);
        if (SettingsActivity.isLaptimerEnabled()) {
            mTabsAdapter.addTab(getString(R.string.laptimes), LapTimesFragment.class, null);
        }
        mTabsAdapter.addTab(getString(R.string.countdown), CountdownFragment.class, null);

        mViewPager.setAdapter(mTabsAdapter);
        //make sure the menu updates when changing tabs
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        tl.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWakeLock.release();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_AUDIO_STATE, mSoundManager.isAudioOn());

        //if we're quitting with the countdown running and not the stopwatch then jump to countdown on relaunch
        if ((mCountdownFragment != null && mCountdownFragment.isRunning()) &&
                (mStopwatchFragment != null && !mStopwatchFragment.isRunning())) {
            editor.putInt(KEY_JUMP_TO_PAGE, 2);
        } else {
            editor.putInt(KEY_JUMP_TO_PAGE, -1);
        }

        editor.commit();

        LapTimeRecorder.getInstance().saveTimes(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON
        mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_KEY);
        mWakeLock.acquire();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mSoundManager.setAudioState(settings.getBoolean(KEY_AUDIO_STATE, true));
        SettingsActivity.loadSettings(settings);

        isLapTimesEnabled = SettingsActivity.isLaptimerEnabled();
        if (isLapTimesEnabled) LapTimeRecorder.getInstance().loadTimes(this);

        if (mMenu != null) {
            MenuItem audioButton = mMenu.findItem(R.id.menu_audiotoggle);
            if (audioButton != null)
                audioButton.setIcon(mSoundManager.isAudioOn() ? R.drawable.ic_volume_black_24dp : R.drawable.ic_volume_mute_24dp);
        }

        //jump straight to countdown if it was only item left running
        int jumpToPage = settings.getInt(KEY_JUMP_TO_PAGE, -1);
        if (jumpToPage != -1) mViewPager.setCurrentItem(2, false);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();

        final int currentTab = mViewPager.getCurrentItem();
        if (SettingsActivity.isLaptimerEnabled()) {
            switch (currentTab) {
                case 1:
                    inflater.inflate(R.menu.menu_laptimes, menu);
                    break;
                case 2:
                    inflater.inflate(R.menu.menu_countdown, menu);
                    break;
                case 0:
                default:
                    inflater.inflate(R.menu.menu_stopwatch, menu);
                    break;
            }
        } else {
            switch (currentTab) {
                case 1:
                    inflater.inflate(R.menu.menu_countdown, menu);
                    break;
                case 0:
                default:
                    inflater.inflate(R.menu.menu_stopwatch, menu);
                    break;
            }
        }

        //get audio icon and set correct variant
        MenuItem audioButton = menu.findItem(R.id.menu_audiotoggle);
        if (audioButton != null)
            audioButton.setIcon(mSoundManager.isAudioOn() ? R.drawable.ic_volume_black_24dp : R.drawable.ic_volume_mute_24dp);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clearlaps) {
            LapTimeRecorder.getInstance().reset(this);
        } else if (item.getItemId() == R.id.menu_audiotoggle) {
            mSoundManager.setAudioState(!(mSoundManager.isAudioOn()));
            item.setIcon(mSoundManager.isAudioOn() ? R.drawable.ic_volume_black_24dp : R.drawable.ic_volume_mute_24dp);
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, INTENT_SETTINGS);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == INTENT_SETTINGS && isLapTimesEnabled != SettingsActivity.isLaptimerEnabled()) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    public void registerLapTimeFragment(LapTimesFragment ltf) {
        mLapTimesFragment = ltf;
    }

    public LapTimesFragment getLapTimeFragment() {
        return mLapTimesFragment;
    }

    public void registerCountdownFragment(CountdownFragment cdf) {
        mCountdownFragment = cdf;
    }

    public void registerStopwatchFragment(StopwatchFragment swf) {
        mStopwatchFragment = swf;
    }
}
package com.geekyouup.android.ustopwatch.fragments;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.fragments.LapTimeRecorder;
import com.geekyouup.android.ustopwatch.fragments.LapTimesFragment;
import com.geekyouup.android.ustopwatch.fragments.StopwatchFragment;
import com.geekyouup.android.ustopwatch.fragments.TimeFragment;
import com.geekyouup.android.ustopwatch.fragments.TimeUtils;

public class UltimateStopwatchFragments extends Activity implements LapTimeRecorder {

	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock;

	private SoundPool soundPool;
	public static final int SOUND_ALARM = 1;
	private HashMap<Integer, Integer> soundPoolMap;
	public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
	public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
	public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
	private static final String WAKE_LOCK_KEY="ustopwatch";

	private TimeFragment mCounterView;
	private StopwatchFragment mStopwatchFragment;
	private LapTimesFragment mLapTimesFragment; 
	private double mCurrentTimeMillis = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		//stop landscape more on QVGA/HVGA
		int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		if(screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		
		// not in all views
		mCounterView = (TimeFragment) getFragmentManager().findFragmentById(R.id.fragment_time);
		mCounterView.setLapTimeRecorder(this);

		mStopwatchFragment = (StopwatchFragment) getFragmentManager().findFragmentById(R.id.stopwatch_fragment);
		mStopwatchFragment.setApplication(this);
		mStopwatchFragment.setHandler(new Handler() {
			@Override
			public void handleMessage(Message m) {
				if (m.getData().getBoolean(MSG_REQUEST_COUNTDOWN_DLG, false)) {
					requestTimeDialog();
				} else if (mCounterView != null && m.getData().getBoolean(MSG_UPDATE_COUNTER_TIME, false)) {
					mCurrentTimeMillis = m.getData().getDouble(MSG_NEW_TIME_DOUBLE);
					if (mCounterView != null)
						mCounterView.setTime(mCurrentTimeMillis);
				}
			}
		});
		
		mLapTimesFragment = (LapTimesFragment) getFragmentManager().findFragmentById(R.id.laptimes_fragment);
		
		soundPool = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 100);
		soundPoolMap = new HashMap<Integer, Integer>();
		soundPoolMap.put(SOUND_ALARM, soundPool.load(this, R.raw.alarm, 1));
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// cancel next alarm if there is one, and clear notification bar
		AlarmUpdater.cancelCountdownAlarm(this);

		mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, WAKE_LOCK_KEY);
		mWakeLock.acquire();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_playpause) {
			mStopwatchFragment.startStop();
		} else if (item.getItemId() == R.id.menu_switchmode) {
			int newMode = (mStopwatchFragment.getMode()==StopwatchFragment.MODE_STOPWATCH)?StopwatchFragment.MODE_COUNTDOWN:StopwatchFragment.MODE_STOPWATCH;
			mStopwatchFragment.setMode(newMode);
			mLapTimesFragment.setMode(newMode);

			if (newMode==StopwatchFragment.MODE_COUNTDOWN)
			{
				requestTimeDialog();
			}

		} else if (item.getItemId() == R.id.menu_reset) {
			reset();
		}else if (item.getItemId()== R.id.menu_laptime)
		{
			recordTime();
		}

		return true;
	}

	private void reset() {
		mStopwatchFragment.reset();
		mLapTimesFragment.reset();
		
		if (mCounterView != null)
			mCounterView.resetTime();
		
		if (mStopwatchFragment.getMode()==StopwatchFragment.MODE_COUNTDOWN)
			requestTimeDialog();
	}

	private boolean mDialogOnScreen = false;
	public void requestTimeDialog() {
		// stop stacking of dialogs
		if (mDialogOnScreen)
			return;

		LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View ll = TimeUtils.createTimeSelectDialogLayout(this, inflator);

		AlertDialog mSelectTime = new AlertDialog.Builder(this).create();
		mSelectTime.setView(ll);
		mSelectTime.setTitle(getString(R.string.timer_title));
		mSelectTime.setButton(getString(R.string.timer_start), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mDialogOnScreen = false;
				mStopwatchFragment.setTime(TimeUtils.getDlgHours(), TimeUtils.getDlgMins(), TimeUtils.getDlgSecs());
			}
		});
		mSelectTime.setButton2(getString(R.string.timer_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mDialogOnScreen = false;
			}
		});
		mSelectTime.show();

		mDialogOnScreen = true;
	}

	public void playAlarm() {
		AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		float streamVolume = mgr.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
		soundPool.play(soundPoolMap.get(SOUND_ALARM), streamVolume, streamVolume, 1, 0, 1f);
	}

	public void notifyCountdownComplete() {
		playAlarm();

		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(1000);
	}

	@Override
	public void recordTime() {
		mLapTimesFragment.recordLapTime(mCurrentTimeMillis);
	}
}
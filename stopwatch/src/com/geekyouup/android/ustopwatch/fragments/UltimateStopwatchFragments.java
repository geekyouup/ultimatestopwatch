package com.geekyouup.android.ustopwatch.fragments;

import java.util.HashMap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import android.widget.NumberPicker;

import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.fragments.LapTimeRecorder;
import com.geekyouup.android.ustopwatch.fragments.LapTimesFragment;
import com.geekyouup.android.ustopwatch.fragments.StopwatchFragment;
import com.geekyouup.android.ustopwatch.fragments.TimeFragment;

public class UltimateStopwatchFragments extends Activity implements
		LapTimeRecorder {

	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock;

	private SoundPool soundPool;
	public static final int SOUND_ALARM = 1;
	private HashMap<Integer, Integer> soundPoolMap;
	public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
	public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
	public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
	private static final String WAKE_LOCK_KEY = "ustopwatch";

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

		soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
		soundPoolMap = new HashMap<Integer, Integer>();
		soundPoolMap.put(SOUND_ALARM, soundPool.load(this, R.raw.alarm, 1));
		
		// stop landscape more on QVGA/HVGA
		int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		// not in all views
		mCounterView = (TimeFragment) getFragmentManager().findFragmentById(
				R.id.fragment_time);
		mCounterView.setLapTimeRecorder(this);

		mStopwatchFragment = (StopwatchFragment) getFragmentManager()
				.findFragmentById(R.id.stopwatch_fragment);
		mStopwatchFragment.setApplication(this);
		mStopwatchFragment.setHandler(new Handler() {
			@Override
			public void handleMessage(Message m) {
				if (m.getData().getBoolean(MSG_REQUEST_COUNTDOWN_DLG, false)) {
					requestTimeDialog();
				} else if (mCounterView != null
						&& m.getData().getBoolean(MSG_UPDATE_COUNTER_TIME,
								false)) {
					mCurrentTimeMillis = m.getData().getDouble(
							MSG_NEW_TIME_DOUBLE);
					if (mCounterView != null)
						mCounterView.setTime(mCurrentTimeMillis);
				}
			}
		});

		mLapTimesFragment = (LapTimesFragment) getFragmentManager()
				.findFragmentById(R.id.laptimes_fragment);
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

		mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				WAKE_LOCK_KEY);
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
			final int newMode = (mStopwatchFragment.getMode() == StopwatchFragment.MODE_STOPWATCH) ? StopwatchFragment.MODE_COUNTDOWN
					: StopwatchFragment.MODE_STOPWATCH;
			mStopwatchFragment.setMode(newMode);

			ObjectAnimator oa = ObjectAnimator.ofFloat(mLapTimesFragment.getView(), "rotationY", 0,90);
		    oa.setDuration(250);
		    oa.addListener(new AnimatorListenerAdapter() {
		    	@Override
		    	public void onAnimationEnd(Animator animation) {
		    		super.onAnimationEnd(animation);
		    		mLapTimesFragment.setMode(newMode);
		    		ObjectAnimator oa2 = ObjectAnimator.ofFloat(mLapTimesFragment.getView(), "rotationY", -90,0)
		    			.setDuration(250);
		    		
		    		oa2.addListener(new AnimatorListenerAdapter() {
		    			@Override
		    			public void onAnimationEnd(Animator animation) {
		    				super.onAnimationEnd(animation);
		    				if (newMode == StopwatchFragment.MODE_COUNTDOWN) {
		    					requestTimeDialog();
		    				}
		    			}
		    		});
		    		oa2.start();
		    	}
		    });
			oa.start();
		} else if (item.getItemId() == R.id.menu_reset) {
			reset();
		}

		return true;
	}

	private void reset() {
		mStopwatchFragment.reset();
		mLapTimesFragment.reset();

		if (mCounterView != null)
			mCounterView.resetTime();

		if (mStopwatchFragment.getMode() == StopwatchFragment.MODE_COUNTDOWN)
			requestTimeDialog();
	}

	private boolean mDialogOnScreen = false;

	private static int mHoursValue = 0;
	private static int mMinsValue = 0;
	private static int mSecsValue = 0;
	public void requestTimeDialog() {
		// stop stacking of dialogs
		if (mDialogOnScreen)
			return;

		LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View ll = inflator.inflate(R.layout.countdown_picker,null);
		
		final NumberPicker npHours = (NumberPicker) ll.findViewById(R.id.numberPickerHours);
		npHours.setMaxValue(99);
		npHours.setValue(mHoursValue);
		
		final NumberPicker npMins = (NumberPicker) ll.findViewById(R.id.numberPickerMins);
		npMins.setMaxValue(59);
		npMins.setValue(mMinsValue);

		final NumberPicker npSecs = (NumberPicker) ll.findViewById(R.id.numberPickerSecs);
		npSecs.setMaxValue(59);
		npSecs.setValue(mSecsValue);
		
		AlertDialog mSelectTime = new AlertDialog.Builder(this).create();
		mSelectTime.setView(ll);
		mSelectTime.setTitle(getString(R.string.timer_title));
		mSelectTime.setButton(getString(R.string.timer_start),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mDialogOnScreen = false;
						mHoursValue = npHours.getValue();
						mMinsValue = npMins.getValue();
						mSecsValue = npSecs.getValue();
						mStopwatchFragment.setTime(mHoursValue,
								mMinsValue, mSecsValue);
					}
				});
		mSelectTime.setButton2(getString(R.string.timer_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mDialogOnScreen = false;
					}
				});
		mSelectTime.show();

		mDialogOnScreen = true;
	}

	public void playAlarm() {
		try
		{
			AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			float streamVolume = mgr
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			soundPool.play(soundPoolMap.get(SOUND_ALARM), streamVolume,
					streamVolume, 1, 0, 1f);
		}catch(Exception e){}
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
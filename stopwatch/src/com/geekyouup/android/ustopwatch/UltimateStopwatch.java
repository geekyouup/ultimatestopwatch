package com.geekyouup.android.ustopwatch;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.fragments.StopwatchFragment;
import com.geekyouup.android.ustopwatch.fragments.TimeFragment;

public class UltimateStopwatch extends FragmentActivity implements OnClickListener {

	private TextView mTextView;
	private static final String PREFS_NAME = "USTOPWATCH_PREFS";
	private static final String KEY_LAPTIME_X = "LAPTIME_";

	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock;

	private SoundPool soundPool;
	public static final int SOUND_ALARM = 1;
	private HashMap<Integer, Integer> soundPoolMap;
	private ImageView mResetBtn;
	public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
	public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
	public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";

	private TimeFragment mCounterView;
	private StopwatchFragment mStopwatchFragment;
	private MenuItem mModeMenuItem;
	private boolean mJustLaunched = false;

	private double mCurrentTimeMillis = 0;
	private ArrayList<Double> mLapTimes = new ArrayList<Double>();
	private LapTimesBaseAdapter mLapTimeAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mJustLaunched = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			if (editor != null) {
				mStopwatchFragment.saveState(editor);
				
                if(mLapTimes!= null && mLapTimes.size()>0)
                {
                	for(int i=0;i<mLapTimes.size();i++) editor.putLong(KEY_LAPTIME_X+i,mLapTimes.get(i).longValue());
                }
				
				editor.commit();
			}
		}

		mStopwatchFragment.pause(); // pause when Activity pauses
		mWakeLock.release();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!mJustLaunched) {
			removeSplashText();
			mJustLaunched = false;
		}

		// cancel next alarm if there is one, and clear notification bar
		AlarmUpdater.cancelCountdownAlarm(this);

		setContentView(R.layout.main);
		
		try {
//			mTextView = (TextView) findViewById(R.id.text);
		} catch (Exception e) {
		}
		
		try {
//			mResetBtn = (ImageView) findViewById(R.id.resetButton);
//			mResetBtn.setOnClickListener(this);
		} catch (Exception e) {
		}

		// not in all views
		try {
			mCounterView = (TimeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_time);
		} catch (Exception e) {
		}

		mStopwatchFragment = (StopwatchFragment) getSupportFragmentManager().findFragmentById(R.id.stopwatch_fragment);
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

		// if vars stored then use them
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if (settings != null && settings.contains("state")) {
			removeSplashText();
			mStopwatchFragment.restoreState(settings);
			setToMode(mStopwatchFragment.getMode());
			
            int lapTimeNum=0;
            mLapTimes = new ArrayList<Double>();
            while(settings.getLong(KEY_LAPTIME_X+lapTimeNum,-1L) != -1L)
            {
            	mLapTimes.add((double) settings.getLong(KEY_LAPTIME_X+lapTimeNum,0L));
            	lapTimeNum++;
            }
		}
		
		mLapTimeAdapter = new LapTimesBaseAdapter(this, mLapTimes);
		try {
			ListFragment mLapTimesFragment = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.laptimes_fragment);
			mLapTimesFragment.setListAdapter(mLapTimeAdapter);
		} catch (Exception e) {
			Log.e("USW", "LapTime fail!!!", e);
		}

		mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Stopwatch");
		mWakeLock.acquire();

		soundPool = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 100);
		soundPoolMap = new HashMap<Integer, Integer>();
		soundPoolMap.put(SOUND_ALARM, soundPool.load(this, R.raw.alarm, 1));
	}

	private void setToMode(int mode) {
		if (mModeMenuItem != null) {
			if (mode==StopwatchFragment.MODE_STOPWATCH) {
				mModeMenuItem.setIcon(R.drawable.countdown);
				mModeMenuItem.setTitle("Countdown");
			} else {
				mModeMenuItem.setIcon(R.drawable.stopwatch);
				mModeMenuItem.setTitle("Stopwatch");
			}
		}
	}
	
	public void storeLapTime(double lapTime)
	{
		Log.d("USW","New LapTime: " + lapTime);
		mLapTimes.add(0,lapTime);
		mLapTimeAdapter.notifyDataSetChanged();
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
			mStopwatchFragment.goPauseUnpause();
		} else if (item.getItemId() == R.id.menu_switchmode) {
			int newMode = (mStopwatchFragment.getMode()==StopwatchFragment.MODE_STOPWATCH)?StopwatchFragment.MODE_COUNTDOWN:StopwatchFragment.MODE_STOPWATCH;
			mStopwatchFragment.setMode(newMode);
			setToMode(newMode);
			if (newMode==StopwatchFragment.MODE_COUNTDOWN)
			{
				requestTimeDialog();
			}

		} else if (item.getItemId() == R.id.menu_reset) {
			reset();
		}else if (item.getItemId()== R.id.menu_laptime)
		{
			storeLapTime(mCurrentTimeMillis);
		}

		return true;
	}

	public void onClick(View v) {
		removeSplashText();

		if (v == mResetBtn)
			reset();
	}

	private void reset() {
		mStopwatchFragment.reset();
		
		mLapTimes.clear();
		mLapTimeAdapter.notifyDataSetChanged();
		
		if (mCounterView != null)
			mCounterView.resetTime();
		
		if (mStopwatchFragment.getMode()==StopwatchFragment.MODE_COUNTDOWN)
			requestTimeDialog();
	}

	public void removeSplashText() {
		if (mTextView != null)
			mTextView.setVisibility(View.GONE);
	}


	private boolean mDialogOnScreen = false;
	public void requestTimeDialog() {
		// stop stacking of dialogs
		if (mDialogOnScreen)
			return;

		try {
			removeSplashText();
		} catch (Exception e) {
		}


		LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View ll = TimeUtils.createTimeSelectDialogLayout(this, inflator);

		AlertDialog mSelectTime = new AlertDialog.Builder(this).create();
		mSelectTime.setView(ll);
		mSelectTime.setTitle(getString(R.string.timer_title));
		mSelectTime.setButton(getString(R.string.timer_start), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				removeSplashText();
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

	/*** Capture Back Button to clear prefs when user is quitting on purpose, else quit ****/
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			try {
				getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
			} catch (Exception e) {
			}
			return super.onKeyDown(keyCode, event);
		} else // not back button or no history to go back to
		{
			return super.onKeyDown(keyCode, event);
		}
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
}
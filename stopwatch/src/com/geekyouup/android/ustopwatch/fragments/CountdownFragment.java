package com.geekyouup.android.ustopwatch.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.TimeUtils;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;
import com.geekyouup.android.ustopwatch.fragments.StopwatchView.StopwatchThead;

import java.util.HashMap;

public class CountdownFragment extends SherlockFragment {

	private StopwatchView mCountdownView;
	private StopwatchThead mWatchThread;
    private double mCurrentTimeMillis;

    private Button mResetButton;
    private Button mStartButton;
    private Button mSaveLapTimeButton;
    private TextView mTimerText;

    private SoundPool soundPool;
    public static final int SOUND_ALARM = 1;
    private HashMap<Integer, Integer> soundPoolMap;

    public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
    public static final String MSG_COUNTDOWN_COMPLETE = "msg_countdown_complete";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View cdView = inflater.inflate(R.layout.countdown_fragment, null);
        mCountdownView = (StopwatchView) cdView.findViewById(R.id.cdview);
        mTimerText = (TextView) cdView.findViewById(R.id.time_counter);
        mTimerText.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent m)
            {
               if(mCurrentTimeMillis == 0) requestTimeDialog();
               return true;
            }
        });

        mResetButton = (Button) cdView.findViewById(R.id.resetButton);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        mStartButton = (Button) cdView.findViewById(R.id.startButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStop();
            }
        });

        mSaveLapTimeButton = (Button) cdView.findViewById(R.id.saveButton);
        mSaveLapTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LapTimeRecorder.getInstance().recordLapTime(mCountdownView.getWatchTime(),(UltimateStopwatchActivity)getSherlockActivity());
            }
        });

        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_ALARM, soundPool.load(getSherlockActivity(), R.raw.alarm, 1));

		return cdView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d("USW","onPause StopwatchFragment");
		SharedPreferences settings = getActivity().getSharedPreferences(UltimateStopwatchActivity.PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		mWatchThread.saveState(editor);
		editor.commit();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d("USW","onStop StopwatchFragment");
	}
	
	@Override
	public void onResume() {
		super.onResume();

        // cancel next alarm if there is one, and clear notification bar
        AlarmUpdater.cancelCountdownAlarm(getSherlockActivity());

		mWatchThread = mCountdownView.createNewThread(false);
        mWatchThread.setIsStopwatchMode(false);

        if(mWatchThread!=null)
        {
            mWatchThread.setHandler(new Handler() {
                @Override
                public void handleMessage(Message m) {
                    if (m.getData().getBoolean(MSG_REQUEST_COUNTDOWN_DLG, false)) {
                        requestTimeDialog();
                    } else if(m.getData().getBoolean(MSG_COUNTDOWN_COMPLETE, false))
                    {
                        playAlarm();

                        Vibrator vibrator = (Vibrator) getSherlockActivity().getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(1000);
                    } else if (m.getData().getBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME,false)) {
                        mCurrentTimeMillis = m.getData().getDouble(
                                UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE);
                        setTime(mCurrentTimeMillis);
                    } else if(m.getData().getBoolean(UltimateStopwatchActivity.MSG_STATE_CHANGE,false))
                    {
                        setUIState();
                    }
                }
            });
        }

		SharedPreferences settings = getActivity().getSharedPreferences(UltimateStopwatchActivity.PREFS_NAME, Context.MODE_PRIVATE);
		Log.d("USW","Resume settings has state set to: " + settings.getInt("state", -1));
        mCountdownView.restoreState(settings);
        mCurrentTimeMillis = mCountdownView.getWatchTime();

        ((UltimateStopwatchActivity) getSherlockActivity()).registerCountdownFragment(this);

       //now handled by the StopwatchView callback to stateChanged
       // mResetButton.setEnabled(true);
       // mStartButton.setText(isRunning()?getString(R.string.pause):getString(R.string.start));
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("USW","StopwatchFragment: onDestroyView");

	}
	
	public void startStop()
	{
		mWatchThread.startStop();
        mResetButton.setEnabled(true);
        mStartButton.setText(isRunning()?getString(R.string.pause):getString(R.string.start));
	}
	
	public void reset()
	{
		mWatchThread.reset();
        mResetButton.setEnabled(false);
        mStartButton.setText(getString(R.string.start));
        setTime(mLastHour,mLastMin,mLastSec);
	}

    int mLastHour = 0;
    int mLastMin = 0;
    int mLastSec = 0;
    public void setTime(int hour, int minute, int seconds)
    {
        mLastHour=hour;
        mLastMin=minute;
        mLastSec=seconds;
        mWatchThread.setTime(hour, minute, seconds,false);
    }

	public void setTimeAndStart(int hour, int minute, int seconds)
	{
		mWatchThread.setTime(hour, minute, seconds,true);
        mResetButton.setEnabled(true);
        mStartButton.setText(getString(R.string.pause));
	}

    private void setTime(double millis)
    {
        if(mTimerText!=null)
        {
             mTimerText.setText(TimeUtils.createStyledSpannableString(getSherlockActivity(), millis,false));
        }
    }

    public boolean isRunning()
    {
        return mWatchThread!=null && (mWatchThread.isRunning() && !mWatchThread.isPaused());
    }

    private void setUIState()
    {
        boolean isRunning = isRunning();
        mResetButton.setEnabled(isRunning || (mCurrentTimeMillis!=0));
        mStartButton.setText(isRunning?getString(R.string.pause):getString(R.string.start));
    }

    public void playAlarm() {
        try
        {
            AudioManager mgr = (AudioManager) getSherlockActivity().getSystemService(Context.AUDIO_SERVICE);
            float streamVolume = mgr
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundPoolMap.get(SOUND_ALARM), streamVolume,
                    streamVolume, 1, 0, 1f);
        }catch(Exception e){}
    }

    private boolean mDialogOnScreen = false;
    private static int mHoursValue = 0;
    private static int mMinsValue = 0;
    private static int mSecsValue = 0;

    public void requestTimeDialog()
    {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            requestAPI11TimeDialog();
        }else
        {
            requestPreAPI11TimeDialog();
        }
    }

    private void requestAPI11TimeDialog() {
        // stop stacking of dialogs
        if (mDialogOnScreen)
            return;

        LayoutInflater inflator = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        AlertDialog mSelectTime = new AlertDialog.Builder(getSherlockActivity()).create();
        mSelectTime.setView(ll);
        mSelectTime.setTitle(getString(R.string.timer_title));
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.timer_start),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogOnScreen = false;
                        mHoursValue = npHours.getValue();
                        mMinsValue = npMins.getValue();
                        mSecsValue = npSecs.getValue();
                        setTime(mHoursValue,
                                mMinsValue, mSecsValue);
                    }
                });
        mSelectTime.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.timer_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogOnScreen = false;
                    }
                });
        mSelectTime.show();

        mDialogOnScreen = true;
    }

    private void requestPreAPI11TimeDialog()
    {
        SherlockFragmentActivity activity = getSherlockActivity();
        //stop stacking of dialogs
        if(mDialogOnScreen) return;

        //try{removeSplashText();}catch(Exception e){}

        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View countdownView = TimeUtils.createTimeSelectDialogLayout(activity, inflator);

        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.addView(countdownView);
        ll.setGravity(Gravity.CENTER);

        AlertDialog mSelectTime = new AlertDialog.Builder(activity).create();
        mSelectTime.setView(ll);
        mSelectTime.setTitle(getString(R.string.timer_title));
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.timer_start), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                //removeSplashText();
                mDialogOnScreen=false;
                mHoursValue = TimeUtils.getDlgHours();
                mMinsValue = TimeUtils.getDlgMins();
                mSecsValue = TimeUtils.getDlgSecs();
                setTime(mHoursValue,
                        mMinsValue, mSecsValue);
            }});
        mSelectTime.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.timer_cancel), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                mDialogOnScreen=false;
            }});
        mSelectTime.show();

        mDialogOnScreen=true;
    }

}

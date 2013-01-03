package com.geekyouup.android.ustopwatch.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.geekyouup.android.ustopwatch.*;
import com.geekyouup.android.ustopwatch.fragments.StopwatchView.StopwatchThead;

public class CountdownFragment extends SherlockFragment {

	private StopwatchView mCountdownView;
	private StopwatchThead mWatchThread;
    private double mCurrentTimeMillis;

    private Button mResetButton;
    private Button mStartButton;
    private TextView mTimerText;
    private SoundManager mSoundManager;

    private int mLastHour = 0;
    private int mLastMin = 0;
    private int mLastSec = 0;

    private static final String COUNTDOWN_PREFS = "USW_CDFRAG_PREFS";
    private static final String KEY_LAST_HOUR = "key_last_hour";
    private static final String KEY_LAST_MIN = "key_last_min";
    private static final String KEY_LAST_SEC = "key_last_sec";

    public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
    public static final String MSG_COUNTDOWN_COMPLETE = "msg_countdown_complete";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSoundManager = SoundManager.getInstance(getSherlockActivity());

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
                mSoundManager.stopEndlessAlarm();
                mSoundManager.playSound(SoundManager.SOUND_RESET);
            }
        });

        mStartButton = (Button) cdView.findViewById(R.id.startButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStop();
            }
        });

		return cdView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getActivity().getSharedPreferences(COUNTDOWN_PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		mWatchThread.saveState(editor);
        editor.putInt(KEY_LAST_HOUR,mLastHour);
        editor.putInt(KEY_LAST_MIN,mLastMin);
        editor.putInt(KEY_LAST_SEC,mLastSec);
		editor.commit();
	}
	
	@Override
	public void onStop() {
		super.onStop();
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
                        mSoundManager.playSound(SoundManager.SOUND_COUNTDOWN_ALARM, SettingsActivity.isEndlessAlarm());

                        if(SettingsActivity.isVibrate()){
                            Vibrator vibrator = (Vibrator) getSherlockActivity().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(1000);
                        }

                        reset(SettingsActivity.isEndlessAlarm());
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

		SharedPreferences settings = getSherlockActivity().getSharedPreferences(COUNTDOWN_PREFS, Context.MODE_PRIVATE);
        mLastHour=settings.getInt(KEY_LAST_HOUR,0);
        mLastMin=settings.getInt(KEY_LAST_MIN,0);
        mLastSec=settings.getInt(KEY_LAST_SEC,0);
        mCountdownView.restoreState(settings);
        mCurrentTimeMillis = mCountdownView.getWatchTime();

        ((UltimateStopwatchActivity) getSherlockActivity()).registerCountdownFragment(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	public void startStop()
	{
        if(!isRunning() && mCurrentTimeMillis == 0)
        {
            requestTimeDialog();
        }else
        {
            mWatchThread.startStop();
            mResetButton.setEnabled(true);
            mStartButton.setText(isRunning()?getString(R.string.pause):getString(R.string.start));
        }
	}

    public void reset(boolean endlessAlarmSounding)
    {
        mWatchThread.reset();
        mResetButton.setEnabled(endlessAlarmSounding);
        mStartButton.setText(getString(R.string.start));
        setTime(mLastHour, mLastMin, mLastSec);
    }

	public void reset()
	{
		reset(false);
	}

    public void setTime(int hour, int minute, int seconds)
    {
        mLastHour=hour;
        mLastMin=minute;
        mLastSec=seconds;
        mWatchThread.setTime(hour, minute, seconds,false);
        setUIState();
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
        mResetButton.setEnabled(mSoundManager.isEndlessAlarmSounding() || isRunning || (mCurrentTimeMillis!=0));
        if(!isRunning && mCurrentTimeMillis==0 && mHoursValue==0 && mMinsValue==0 && mSecsValue==0)
        {
            mStartButton.setText(getString(R.string.start));
            mSoundManager.stopCountdownTicking();
        }else
        {
            mStartButton.setText(isRunning?getString(R.string.pause):getString(R.string.start));

            if(isRunning())
            {
                mSoundManager.playSound(SoundManager.SOUND_START);
                mSoundManager.startCountDownTicking();
            }else
            {
                mSoundManager.playSound(SoundManager.SOUND_STOP);
                mSoundManager.stopCountdownTicking();
            }
        }
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

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getSherlockActivity(), android.R.style.Theme_Holo);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = inflater.inflate(R.layout.countdown_picker,null);

        final NumberPicker npHours = (NumberPicker) ll.findViewById(R.id.numberPickerHours);
        npHours.setMaxValue(99);
        npHours.setValue(mHoursValue);

        final NumberPicker npMins = (NumberPicker) ll.findViewById(R.id.numberPickerMins);
        npMins.setMaxValue(59);
        npMins.setValue(mMinsValue);

        final NumberPicker npSecs = (NumberPicker) ll.findViewById(R.id.numberPickerSecs);
        npSecs.setMaxValue(59);
        npSecs.setValue(mSecsValue);

        AlertDialog mSelectTime = new AlertDialog.Builder(wrapper).create();
        mSelectTime.setView(ll);
        mSelectTime.setTitle(getString(R.string.timer_title));
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.timer_start),
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
        mSelectTime.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
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

        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = TimeUtils.createTimeSelectDialogLayout(activity, inflator);

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

        mSelectTime.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mDialogOnScreen = false;
            }
        });
        mSelectTime.show();

        mDialogOnScreen=true;
    }

}

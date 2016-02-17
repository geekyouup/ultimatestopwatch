package com.geekyouup.android.ustopwatch.fragments;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.view.*;
import android.widget.*;
import android.support.v4.app.Fragment;
import com.geekyouup.android.ustopwatch.*;
import android.graphics.Rect;

public class CountdownFragment extends Fragment {

	private StopwatchCustomVectorView mCountdownView;
    private double mCurrentTimeMillis;

    private FloatingActionButton mResetFAB;
    private TextView mTimerText;
    private SoundManager mSoundManager;

    private int mLastHour = 0;
    private int mLastMin = 0;
    private int mLastSec = 0;

    private boolean mRunningState = false;
    private boolean isReset = false;

    private static final String COUNTDOWN_PREFS = "USW_CDFRAG_PREFS";
    private static final String PREF_IS_RUNNING = "key_countdown_is_running";
    private static final String KEY_LAST_HOUR = "key_last_hour";
    private static final String KEY_LAST_MIN = "key_last_min";
    private static final String KEY_LAST_SEC = "key_last_sec";

    public static final String MSG_REQUEST_TIME_PICKER = "msg_request_time_picker";
    public static final String MSG_COUNTDOWN_COMPLETE = "msg_countdown_complete";
    public static final String MSG_APP_RESUMING = "msg_app_resuming";
    private int mLastSecondTicked =0;

    //countdown picker dialog variables
    private boolean mDialogOnScreen = false;
    private static int mHoursValue = 0;
    private static int mMinsValue = 0;
    private static int mSecsValue = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSoundManager = SoundManager.getInstance(getContext());

		View cdView = inflater.inflate(R.layout.countdown_fragment, null);
        mCountdownView = (StopwatchCustomVectorView) cdView.findViewById(R.id.cdview);
        mTimerText = (TextView) cdView.findViewById(R.id.time_counter);
        mTimerText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent m) {
                if (!isRunning()) requestTimeDialog();
                return true;
            }
        });

        //resetFAB has 2 states, if stopped it is a time picker, else it is reset
        mResetFAB = (FloatingActionButton) cdView.findViewById(R.id.resetfab);
        if(!isRunning() && isReset)
            mResetFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_set_countdown_time_24dp));
        else
            mResetFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_countdown_reset_24dp));

        mResetFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isReset) reset();
                else requestTimeDialog();

                mSoundManager.stopEndlessAlarm();
                mSoundManager.playSound(SoundManager.SOUND_RESET);
            }
        });

        return cdView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getContext().getSharedPreferences(COUNTDOWN_PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_IS_RUNNING, mRunningState);
		mCountdownView.saveState(editor);
        editor.putInt(KEY_LAST_HOUR, mLastHour);
        editor.putInt(KEY_LAST_MIN,mLastMin);
        editor.putInt(KEY_LAST_SEC, mLastSec);
		editor.commit();

        mCountdownView.stop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
        // cancel next alarm if there is one, and clear notification bar
        AlarmUpdater.cancelCountdownAlarm(getContext());

        mCountdownView.setHandler(new Handler() {
            @Override
            public void handleMessage(Message m) {
                if (m.getData().getBoolean(MSG_REQUEST_TIME_PICKER, false)) {
                    requestTimeDialog();
                } else if(m.getData().getBoolean(MSG_COUNTDOWN_COMPLETE, false))
                {
                    boolean appResuming = m.getData().getBoolean(MSG_APP_RESUMING, false);
                    if(!appResuming)
                    {
                        mSoundManager.playSound(SoundManager.SOUND_COUNTDOWN_ALARM, SettingsActivity.isEndlessAlarm());

                        if(SettingsActivity.isVibrate() && getActivity()!=null){
                            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(1000);
                        }
                    }

                    reset(!appResuming && SettingsActivity.isEndlessAlarm());
                } else if (m.getData().getBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME,false)) {
                    mCurrentTimeMillis = m.getData().getDouble(
                            UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE);

                    //If we've crossed into a new second then make the tick sound
                    int currentSecond = (int) mCurrentTimeMillis/1000;
                    if(currentSecond> mLastSecondTicked)
                    {
                        mSoundManager.doTick();
                    }
                    mLastSecondTicked = currentSecond;


                    setTime(mCurrentTimeMillis);
                } else if(m.getData().getBoolean(UltimateStopwatchActivity.MSG_STATE_CHANGE,false))
                {
                    if(isRunning()) isReset=false;
                    setUIState();
                }
            }
        });

		SharedPreferences settings = getContext().getSharedPreferences(COUNTDOWN_PREFS, Context.MODE_PRIVATE);
        mLastHour=settings.getInt(KEY_LAST_HOUR,0);
        mLastMin=settings.getInt(KEY_LAST_MIN,0);
        mLastSec=settings.getInt(KEY_LAST_SEC,0);
        mRunningState = settings.getBoolean(PREF_IS_RUNNING,false);
        mCountdownView.restoreState(settings);
        mCurrentTimeMillis = mCountdownView.getWatchTime();

        ((UltimateStopwatchActivity) getActivity()).registerCountdownFragment(this);

        Paint paint = new Paint();
        Rect bounds = new Rect();
        paint.setTypeface(Typeface.SANS_SERIF);// your preference here
        paint.setTextSize(getResources().getDimension(R.dimen.counter_font));// have this the same as your text size
        String text = "-00:00:00.000";
        paint.getTextBounds(text, 0, text.length(), bounds);
        int text_width =  bounds.width();
        int width = getResources().getDisplayMetrics().widthPixels;
        if(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE) width = width/2;

        mTimerText.setPadding((width-text_width)/2,0,0,0);
	}

    public void reset(boolean endlessAlarmSounding) {
        mCountdownView.setTime(mLastHour, mLastMin, mLastSec, true);
        isReset=true;
        setUIState();
    }

	public void reset() {
		reset(false);
	}

    public void setTime(int hour, int minute, int seconds) {
        mLastHour=hour;
        mLastMin=minute;
        mLastSec=seconds;
        mCountdownView.setTime(hour, minute, seconds,false);
        setUIState();
    }

    private void setTime(double millis) {
        if(mTimerText!=null) {
             mTimerText.setText(TimeUtils.createStyledSpannableString(getContext(), millis,false));
        }
    }

    public boolean isRunning() {
        return (mCountdownView!=null && mCountdownView.isRunning());
    }

    private void setUIState() {
        boolean stateChanged = (mRunningState != isRunning());
        mRunningState = isRunning();

        if(!isRunning() && isReset){
            mResetFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_set_countdown_time_24dp));
        }else {
            mResetFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_countdown_reset_24dp));
            if(stateChanged)
                mSoundManager.playSound(isRunning() ? SoundManager.SOUND_START : SoundManager.SOUND_STOP);
        }
    }

    public void requestTimeDialog() {
        if(mDialogOnScreen) return;

        if(mHoursValue==0) mHoursValue=mLastHour;
        if(mMinsValue==0) mMinsValue=mLastMin;
        if(mSecsValue==0) mSecsValue=mLastSec;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            requestAPI11TimeDialog();
        }else
        {
            requestPreAPI11TimeDialog();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void requestAPI11TimeDialog() {
        // stop stacking of dialogs
        if (mDialogOnScreen)
            return;

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getContext(), R.style.Theme_AppCompat);
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
                        setTime(mHoursValue, mMinsValue, mSecsValue);
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

    private void requestPreAPI11TimeDialog() {
        Context context = getContext();
        //stop stacking of dialogs
        if(mDialogOnScreen) return;

        LayoutInflater inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = TimeUtils.createTimeSelectDialogLayout(context, inflator,mHoursValue,mMinsValue,mSecsValue);

        AlertDialog mSelectTime = new AlertDialog.Builder(context).create();
        mSelectTime.setView(ll);
        mSelectTime.setTitle(getString(R.string.timer_title));
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.timer_start), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                mDialogOnScreen=false;
                mHoursValue = TimeUtils.getDlgHours();
                mMinsValue = TimeUtils.getDlgMins();
                mSecsValue = TimeUtils.getDlgSecs();
                setTime(mHoursValue, mMinsValue, mSecsValue);
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

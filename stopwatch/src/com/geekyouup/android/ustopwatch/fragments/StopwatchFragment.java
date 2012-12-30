package com.geekyouup.android.ustopwatch.fragments;

import android.content.Intent;
import android.os.Message;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.geekyouup.android.ustopwatch.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.geekyouup.android.ustopwatch.fragments.StopwatchView.StopwatchThead;
import org.w3c.dom.Text;

public class StopwatchFragment extends SherlockFragment {

	private StopwatchView mStopwatchView;
	private StopwatchThead mWatchThread;
	private Button mResetButton;
    private Button mStartButton;
    private View mSaveLapTimeButton;
    private TextView mTimerText;
    private double mCurrentTimeMillis=0;
    private SoundManager mSoundManager;
	
	private static final String PREFS_NAME="USW_SWFRAG_PREFS";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSoundManager = SoundManager.getInstance(getSherlockActivity());

        View swView = inflater.inflate(R.layout.stopwatch_fragment, null);
        mTimerText = (TextView) swView.findViewById(R.id.counter_text);
        mStopwatchView = (StopwatchView) swView.findViewById(R.id.swview);

        mStartButton = (Button) swView.findViewById(R.id.startButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStop();
            }
        });

        mResetButton = (Button) swView.findViewById(R.id.resetButton);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LapTimeRecorder.getInstance().stopwatchReset();
                reset();
            }
        });

        mSaveLapTimeButton = (View) swView.findViewById(R.id.saveButton);
        mSaveLapTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning())
                {
                    LapTimeRecorder.getInstance().recordLapTime(mStopwatchView.getWatchTime(),(UltimateStopwatchActivity)getSherlockActivity());
                    mSoundManager.playSound(SoundManager.SOUND_LAPTIME);
                }
            }
        });

        return swView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d("USW","onPause StopwatchFragment");
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		mWatchThread.saveState(editor);
		editor.commit();

        try
        {
            if(isRunning() && mCurrentTimeMillis>0)
                AlarmUpdater.showChronometerNotification(getSherlockActivity(), (long) mCurrentTimeMillis);
        }catch (Exception e){}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d("USW","onStop StopwatchFragment");
	}
	
	@Override
	public void onResume() {
		super.onResume();

		mWatchThread = mStopwatchView.createNewThread(true);
        if(mWatchThread!=null)
        {
            mWatchThread.setHandler(new Handler() {
                @Override
                public void handleMessage(Message m) {
                    if (m.getData().getBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME,false)) {
                        mCurrentTimeMillis = m.getData().getDouble(UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE);
                        setTime(mCurrentTimeMillis);
                    }else if(m.getData().getBoolean(UltimateStopwatchActivity.MSG_STATE_CHANGE,false))
                    {
                        setUIState();
                    }
                }
            });
        }

        AlarmUpdater.cancelChronometerNotification(getSherlockActivity());

		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		Log.d("USW","Resume settings has state set to: " + settings.getInt("state", -1));
		mStopwatchView.restoreState(settings);
    }

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("USW","StopwatchFragment: onDestroyView");

	}
	
	public void startStop()
	{
		mWatchThread.startStop();
        setUIState();
	}

    private boolean mRunningState = false;
    private void setUIState()
    {
        boolean stateChanged = (mRunningState != isRunning());
        mRunningState = isRunning();
        mResetButton.setEnabled(mRunningState || (mCurrentTimeMillis!=0));
        mStartButton.setText(mRunningState?getString(R.string.pause):getString(R.string.start));

        if(stateChanged)
        {
            if(mRunningState)
            {
                mSoundManager.playSound(SoundManager.SOUND_START);
                mSoundManager.startStopwatchTicking();
            }else
            {
                mSoundManager.playSound(SoundManager.SOUND_STOP);
                mSoundManager.stopStopwatchTicking();;
            }
        }
    }

	public void reset()
	{
		mWatchThread.reset();

        mSoundManager.playSound(SoundManager.SOUND_RESET);
        mSoundManager.stopStopwatchTicking();

        mResetButton.setEnabled(false);
        mStartButton.setText(getString(R.string.start));
	}

    private void setTime(double millis)
    {
        if(mTimerText!=null)
            mTimerText.setText(TimeUtils.createStyledSpannableString(getSherlockActivity(), millis,true));
    }

    public boolean isRunning()
    {
        return mWatchThread!=null && (mWatchThread.isRunning() && !mWatchThread.isPaused());
    }
	
}

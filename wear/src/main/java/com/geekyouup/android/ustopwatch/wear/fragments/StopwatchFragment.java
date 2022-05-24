package com.geekyouup.android.ustopwatch.wear.fragments;

import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.geekyouup.android.ustopwatch.wear.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.WearActivity;

public class StopwatchFragment extends Fragment {

    private StopwatchCustomVectorView mStopwatchView;
    private double mCurrentTimeMillis = 0;
    private boolean mRunningState = false;

    private static final String PREFS_NAME = "USW_SWFRAG_PREFS";
    private static final String PREF_IS_RUNNING = "key_stopwatch_is_running";
    private int mLastSecond = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View swView = inflater.inflate(R.layout.stopwatch_fragment, null);
        //mTimerText = (TextView) swView.findViewById(R.id.counter_text);
        mStopwatchView = (StopwatchCustomVectorView) swView.findViewById(R.id.swview);
        return swView;
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_IS_RUNNING, mRunningState);
        mStopwatchView.saveState(editor);
        editor.commit();

        try {
            if (isRunning() && mCurrentTimeMillis > 0)
                AlarmUpdater.showChronometerNotification(getActivity(), (long) mCurrentTimeMillis);
        } catch (Exception ignored) {
        }

        mStopwatchView.stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        mStopwatchView.setHandler(new Handler() {
            @Override
            public void handleMessage(Message m) {
                if (m.getData().getBoolean(WearActivity.MSG_UPDATE_COUNTER_TIME, false)) {
                    mCurrentTimeMillis = m.getData().getDouble(WearActivity.MSG_NEW_TIME_DOUBLE);
                    setTime(mCurrentTimeMillis);

                    int currentSecond = (int) mCurrentTimeMillis / 1000;
                    mLastSecond = currentSecond;

                } else if (m.getData().getBoolean(WearActivity.MSG_STATE_CHANGE, false)) {
                    setUIState();
                } else if (m.getData().getBoolean(WearActivity.MSG_RESET, false)) {
                    reset();
                }
            }
        });


        AlarmUpdater.cancelChronometerNotification(getActivity());

        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mRunningState = settings.getBoolean(PREF_IS_RUNNING, false);
        mStopwatchView.restoreState(settings);
        // ((UltimateStopwatchActivity) getActivity()).registerStopwatchFragment(this);

        //center the timer text in a fixed position, stops wiggling numbers
        Paint paint = new Paint();
        Rect bounds = new Rect();
        paint.setTypeface(Typeface.SANS_SERIF);// your preference here
        paint.setTextSize(getResources().getDimension(R.dimen.counter_font));// have this the same as your text size
        String counterText = getString(R.string.default_time); //00:00:00.000
        paint.getTextBounds(counterText, 0, counterText.length(), bounds);
        int text_width = bounds.width();
        int width = getResources().getDisplayMetrics().widthPixels;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            width = width / 2;

        // mTimerText.setPadding((width - text_width) / 2, 0, 0, 0);
    }

    public void startStop() {
        mStopwatchView.startStop();
        setUIState();
    }

    private void setUIState() {
        boolean stateChanged = (mRunningState != isRunning());
        mRunningState = isRunning();
        // mResetButton.setEnabled(mRunningState || (mCurrentTimeMillis != 0));
        //mSaveLapTimeButton.setEnabled(mRunningState || (mCurrentTimeMillis != 0));

        // if (isAdded()) mStartButton.setText(mRunningState ? getString(R.string.pause) : getString(R.string.start));
    }

    public void reset() {
        mStopwatchView.setTime(0, 0, 0, true);

        // mResetButton.setEnabled(false);
        //mSaveLapTimeButton.setEnabled(false);
        // mStartButton.setText(getString(R.string.start));
    }

    private void setTime(final double millis) {
        /*if (mTimerText != null)
            mTimerText.setText(TimeUtils.createStyledSpannableString(getActivity(), millis, false));*/
    }

    public boolean isRunning() {
        return (mStopwatchView != null && mStopwatchView.isRunning());
    }

}

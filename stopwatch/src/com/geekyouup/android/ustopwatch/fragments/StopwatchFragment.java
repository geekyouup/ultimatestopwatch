package com.geekyouup.android.ustopwatch.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.geekyouup.android.ustopwatch.StopwatchView;
import com.geekyouup.android.ustopwatch.StopwatchView.StopwatchThead;
import com.geekyouup.android.ustopwatch.UltimateStopwatch;

public class StopwatchFragment  extends Fragment {

	private StopwatchView mStopwatchView;
	private StopwatchThead mWatchThread;
	public static final int MODE_STOPWATCH=0;
	public static final int MODE_COUNTDOWN=1;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mStopwatchView = new StopwatchView(getActivity(), null);
		mWatchThread = mStopwatchView.getThread();
//		mWatchThread.setApplication(/*TODO.*/);
		
		
		return mStopwatchView;
	}
	
	public void pause()
	{
		mWatchThread.pause();
	}
	
	public void goPauseUnpause()
	{
		mWatchThread.goPauseUnpause();
	}
	
	public void reset()
	{
		mWatchThread.reset();
	}
	
	public void saveState(SharedPreferences.Editor editor)
	{
		mWatchThread.saveState(editor);
	}
	
	public void restoreState(SharedPreferences settings)
	{
		mWatchThread.restoreState(settings);
	}
	
	public int getMode()
	{
		return mWatchThread.isStopwatchMode()?MODE_STOPWATCH:MODE_COUNTDOWN;
	}
	
	public void setMode(int mode)
	{
		mWatchThread.setIsStopwatchMode(mode==MODE_STOPWATCH);
	}
	
	public void setTime(int hour, int minute, int seconds)
	{
		mWatchThread.setTime(hour, minute, seconds);
	}
	
	public void setApplication(UltimateStopwatch app)
	{
		mWatchThread.setApplication(app);
	}
	
	public void setHandler(Handler h)
	{
		mWatchThread.setHandler(h);
	}
	
}

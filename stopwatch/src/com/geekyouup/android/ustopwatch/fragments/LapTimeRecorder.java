package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

import android.util.Log;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.geekyouup.android.ustopwatch.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;

public class LapTimeRecorder {
	
	private static final ArrayList<Double> mLapTimes = new ArrayList<Double>();
	private static final String PREFS_NAME_LAPTIMES = "usw_prefs_laptimes";
	private static final String KEY_LAPTIME_X = "LAPTIME_";
	private static LapTimeRecorder mSelf;

    private int mCurrentLapTimeIndex = 0;

    //if a 0 laptime is stored then it is a reset signal and start of new block
	
	public static LapTimeRecorder getInstance()
	{
		if(mSelf == null) mSelf = new LapTimeRecorder();
		return mSelf;
	}
	
	public void loadTimes(Context cxt)
	{
		SharedPreferences settings = cxt.getSharedPreferences(PREFS_NAME_LAPTIMES, Context.MODE_PRIVATE);
		if (settings != null) {
            int lapTimeNum=0;
            mLapTimes.clear();
            double lt=0;
            while((lt = settings.getLong(KEY_LAPTIME_X+lapTimeNum,-1L)) != -1L)
            {
            	mLapTimes.add(lt);
                if(lt==0) mCurrentLapTimeIndex++;
            	lapTimeNum++;
            }
		}
	}
	
	public void saveTimes(Context cxt)
	{
		SharedPreferences settings = cxt.getSharedPreferences(PREFS_NAME_LAPTIMES, Context.MODE_PRIVATE);
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			if (editor != null) {
				editor.clear();
                if(mLapTimes!= null && mLapTimes.size()>0)
                {
                	for(int i=0;i<mLapTimes.size();i++) editor.putLong(KEY_LAPTIME_X+i,mLapTimes.get(i).longValue());
                }
				editor.commit();
			}
		}
	}
	
	public void recordLapTime(double time, UltimateStopwatchActivity activity)
	{
        Log.d("USW", "Recording lap time " + time);
		mLapTimes.add(0,time);
        if(time == 0) mCurrentLapTimeIndex++;

        if(activity!=null)
        {
            Log.d("USW", "Notifying LTF of new time");
            LapTimesFragment ltf = activity.getLapTimeFragment();
            if(ltf != null) ltf.lapTimesUpdated();
        }
    }

    public void stopwatchReset()
    {
        mLapTimes.add(0,0d);
    }

	public ArrayList<LapTimeBlock> getTimes()
	{
        int numTimes = mLapTimes.size();
        Log.d("USW", "LTF updating times total times "+ numTimes);
        ArrayList<LapTimeBlock> lapTimeBlocks = new ArrayList<LapTimeBlock>();
        LapTimeBlock ltb = new LapTimeBlock();
        for(int i=0;i<numTimes;i++)
        {
            double laptime = mLapTimes.get(i);
            if(laptime == 0)
            {
                if(i==0) continue; //skip if the first element is a 0
                Log.d("USW", "LTB created " + laptime);
                lapTimeBlocks.add(ltb);
                ltb = new LapTimeBlock();
            }else
            {
                Log.d("USW", "LTB added "+ laptime);
                ltb.addLapTime(laptime);
            }
        }

        if(numTimes > 0) lapTimeBlocks.add(ltb);

		return lapTimeBlocks;
	}
	
	public void reset(UltimateStopwatchActivity activity)
	{
		mLapTimes.clear();
        mCurrentLapTimeIndex=0;
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME_LAPTIMES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.commit();

        if(activity!=null)
        {
            LapTimesFragment ltf = activity.getLapTimeFragment();
            if(ltf != null) ltf.lapTimesUpdated();
        }
	}
}

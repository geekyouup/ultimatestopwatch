package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: rhyndman
 * Date: 11/23/12
 * Time: 3:58 PM
 */
public class LapTimeBlock {

    ArrayList<Double> mLapTimes;

    public LapTimeBlock()
    {
        mLapTimes = new ArrayList<Double>();
    }

    public void addLapTime(Double lapTime)
    {
        mLapTimes.add(lapTime);
    }

    public ArrayList<Double> getLapTimes()
    {
        return mLapTimes;
    }




}

package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.geekyouup.android.ustopwatch.R;


public class LapTimesFragment extends ListFragment{

	private LapTimesBaseAdapter mAdapter;
	private final ArrayList<Double> mLapTimes = new ArrayList<Double>();
	private static final String PREFS_NAME_LAPTIMES = "usw_prefs_laptimes";
	private static final String KEY_LAPTIME_X = "LAPTIME_";
	private ViewFlipper mViewFlipper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.laptimes_fragment, container, false);
		try{mViewFlipper = (ViewFlipper) v.findViewById(R.id.laptimes_viewflipper);}
		catch(Exception e){}
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getListView().setCacheColorHint(Color.WHITE);
		mAdapter=new LapTimesBaseAdapter(getActivity(), mLapTimes);
		setListAdapter(mAdapter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMES, Context.MODE_PRIVATE);
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
	
	@Override
	public void onResume() {
		super.onResume();
		
		// if vars stored then use them
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMES, Context.MODE_PRIVATE);
		if (settings != null) {
			
            int lapTimeNum=0;
            mLapTimes.clear();
            while(settings.getLong(KEY_LAPTIME_X+lapTimeNum,-1L) != -1L)
            {
            	mLapTimes.add((double) settings.getLong(KEY_LAPTIME_X+lapTimeNum,0L));
            	lapTimeNum++;
            }
            mAdapter.notifyDataSetChanged();
		}
	}

	public void reset()
	{
		mLapTimes.clear();
		mAdapter.notifyDataSetChanged();
	}
	
	public void recordLapTime(double time)
	{
		mLapTimes.add(0,time);
		mAdapter.notifyDataSetChanged();
	}
	
	public void setMode(int mode)
	{
		//if(mode==mCurrentMode) return;
		
		if(mViewFlipper!= null)mViewFlipper.showNext();
		
		if(mode == StopwatchFragment.MODE_STOPWATCH)
		{
			
		}else
		{
			
		}
	}
	
}

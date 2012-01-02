package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.geekyouup.android.ustopwatch.R;


public class LapTimesFragment extends ListFragment{

	private LapTimesBaseAdapter mAdapter;
	private ArrayList<Double> mLapTimes = new ArrayList<Double>();
	private static final String PREFS_NAME_LAPTIMESFRAG = "usw_prefs_laptimesfrag";
	private static final String KEY_CURRENT_VIEW = "current_view";
	private ViewFlipper mViewFlipper;
	private LapTimeRecorder mLapTimeRecorder;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLapTimeRecorder = LapTimeRecorder.getInstance();
		
		Log.d("USW","LaptimesFragment.onCreate()");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.laptimes_fragment, container, false);
		try{mViewFlipper = (ViewFlipper) v.findViewById(R.id.laptimes_viewflipper);}
		catch(Exception e){}
		
		Log.d("USW","LaptimesFragment.onCreateView()");
		
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getListView().setCacheColorHint(Color.WHITE);
		mAdapter=new LapTimesBaseAdapter(getActivity(), mLapTimes);
		setListAdapter(mAdapter);
		
		Log.d("USW","LaptimesFragment.onStart()");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMESFRAG, Context.MODE_PRIVATE);
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			if (editor != null) {
				if(mViewFlipper!=null) editor.putInt(KEY_CURRENT_VIEW, mViewFlipper.getDisplayedChild());
				editor.commit();
			}
		}
		
		Log.d("USW","LaptimesFragment.onPause()");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.d("USW","LaptimesFragment.onResume()");
		
		// if vars stored then use them
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMESFRAG, Context.MODE_PRIVATE);
		if (settings != null) {
            mLapTimes.clear();
            mLapTimes.addAll(mLapTimeRecorder.getTimes());
            mAdapter.notifyDataSetChanged();
            
            setMode(settings.getInt(KEY_CURRENT_VIEW, 0));
		}
	}

	public void reset()
	{
		mLapTimes.clear();
		mAdapter.notifyDataSetChanged();
	}

	public void notifyDataSetChanged()
	{
		mAdapter.notifyDataSetChanged();
	}
	
	public void setMode(int mode)
	{
		//if(mode==mCurrentMode) return;
		
		if(mViewFlipper!= null)
		{
			mViewFlipper.setDisplayedChild(mode);
		}
	}
	
}

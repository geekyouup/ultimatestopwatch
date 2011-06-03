package com.geekyouup.android.ustopwatch.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.TimeUtils;

public class TimeFragment extends Fragment {

	private TextView mCounter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View timeView = inflater.inflate(R.layout.time_fragment, container, false);
		mCounter = (TextView) timeView.findViewById(R.id.counter_digits);
		return timeView;
	}

	public void setTime(double time) {
		if(mCounter!=null) mCounter.setText(TimeUtils.createTimeString(time));
	}
	
	public void resetTime()
	{
		if(mCounter!=null)mCounter.setText(TimeUtils.createTimeString(0));
	}
	
}

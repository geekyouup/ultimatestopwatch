package com.geekyouup.android.ustopwatch.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.TimeUtils;

public class TimeFragment extends Fragment implements OnClickListener{

	private TextView mCounter;
	private LapTimeRecorder mLapTimer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View timeView = inflater.inflate(R.layout.time_fragment, container, false);
		mCounter = (TextView) timeView.findViewById(R.id.counter_digits);
		if(mCounter!=null)mCounter.setOnClickListener(this);
		return timeView;
	}

	public void setTime(double time) {
		if(mCounter!=null) mCounter.setText(TimeUtils.createTimeString(time));
	}
	
	public void resetTime()
	{
		if(mCounter!=null)mCounter.setText(TimeUtils.createTimeString(0));
	}

	@Override
	public void onClick(View v) {
		if(mLapTimer!=null) mLapTimer.recordTime();
		Toast.makeText(getActivity(), "Laptime Recorded", Toast.LENGTH_SHORT).show();
	}
	
	public void setLapTimeRecorder(LapTimeRecorder ltr)
	{
		mLapTimer=ltr;
	}
}

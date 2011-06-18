package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.R;

public class LapTimesBaseAdapter extends BaseAdapter {

	private ArrayList<Double> mDataSet;
	private LayoutInflater mLayoutInflator;
	private Context mContext;
	
	public LapTimesBaseAdapter(Context cxt, ArrayList<Double> dataSet)
	{
		mContext = cxt;
		mDataSet=dataSet;
	}
	
	@Override
	public int getCount() {
		return mDataSet==null?0:mDataSet.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if(v == null)
		{
			if(mLayoutInflator == null) mLayoutInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = mLayoutInflator.inflate(R.layout.laptime_item,null);
			
		}
		
		TextView t = (TextView) v.findViewById(R.id.laptime_text);
		if(position<mDataSet.size()-1 && mDataSet.size()>1)
		{
			double laptime= mDataSet.get(position)-mDataSet.get(position+1);
			if(laptime<0) laptime = mDataSet.get(position);
			t.setText(TimeUtils.createTimeString(laptime));
		}else{
			t.setText(TimeUtils.createTimeString(mDataSet.get(position)));
		}

		TextView t2 = (TextView) v.findViewById(R.id.laptime_text2);
		t2.setText(TimeUtils.createTimeString(mDataSet.get(position)));
		
		return v;
	}
	
	
}

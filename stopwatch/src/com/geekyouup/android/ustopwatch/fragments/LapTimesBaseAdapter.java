package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.TimeUtils;

public class LapTimesBaseAdapter extends BaseAdapter {

	private ArrayList<LapTimeBlock> mDataSet;
	private LayoutInflater mLayoutInflator;
	private Context mContext;
	
	public LapTimesBaseAdapter(Context cxt, ArrayList<LapTimeBlock> dataSet)
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
        if(mLayoutInflator == null) mLayoutInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = convertView;
		if(v == null) v = mLayoutInflator.inflate(R.layout.laptimes_holder_list_item,null);

        LinearLayout listItemHolder = (LinearLayout) v.findViewById(R.id.laptimes_list_item_holder);
        LapTimeBlock ltb = mDataSet.get(position);
        ArrayList<Double> lapTimes  = ltb.getLapTimes();

        for(int i=0;i<lapTimes.size();i++)
        {
            View lapItemView =  mLayoutInflator.inflate(R.layout.laptime_item,null);

            if(i==0)
            {
                TextView t = (TextView) lapItemView.findViewById(R.id.laptime_text);
                t.setText(TimeUtils.createStyledSpannableString(mContext,lapTimes.get(i),true));
            }

            TextView t2 = (TextView) lapItemView.findViewById(R.id.laptime_text2);
            if(i<lapTimes.size()-1 && lapTimes.size()>1)
            {
                double laptime= lapTimes.get(i)-lapTimes.get(i+1);
                if(laptime<0) laptime = lapTimes.get(i);
                t2.setText(TimeUtils.createStyledSpannableString(mContext,laptime,true));
            }else{
                t2.setText(TimeUtils.createStyledSpannableString(mContext,lapTimes.get(i),true));
            }

            listItemHolder.addView(lapItemView);

        }
		return v;
	}
	
	
}

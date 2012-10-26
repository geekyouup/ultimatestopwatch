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
        if(mLayoutInflator == null) mLayoutInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = convertView;
		if(v == null) v = mLayoutInflator.inflate(R.layout.laptimes_holder_list_item,null);

        LinearLayout listItemHolder = (LinearLayout) v.findViewById(R.id.laptimes_list_item_holder);

        for(int i=0;i<3;i++)
        {
            View lapItemView =  mLayoutInflator.inflate(R.layout.laptime_item,null);

            if(i==0)
            {
                TextView t = (TextView) lapItemView.findViewById(R.id.laptime_text);
                t.setText(TimeUtils.createStyledSpannableString(mContext,mDataSet.get(position),true));
            }

            TextView t2 = (TextView) lapItemView.findViewById(R.id.laptime_text2);
            if(position<mDataSet.size()-1 && mDataSet.size()>1)
            {
                double laptime= mDataSet.get(position)-mDataSet.get(position+1);
                if(laptime<0) laptime = mDataSet.get(position);
                t2.setText(TimeUtils.createStyledSpannableString(mContext,laptime,true));
            }else{
                t2.setText(TimeUtils.createStyledSpannableString(mContext,mDataSet.get(position),true));
            }

            listItemHolder.addView(lapItemView);

        }



		return v;
	}
	
	
}

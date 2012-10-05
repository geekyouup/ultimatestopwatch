package com.geekyouup.android.ustopwatch.fragments;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.TimeUtils;

public class TimeFragment extends SherlockFragment {

	private TextView mCounter;
	private double mCurrentTimeMillis=0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View timeView = inflater.inflate(R.layout.time_fragment, container, false);
		mCounter = (TextView) timeView.findViewById(R.id.counter_digits);
		return timeView;
	}

	public void setTime(double time) {
		mCurrentTimeMillis = time;
		if(mCounter!=null) mCounter.setText(TimeUtils.createTimeString(time));

        String timeText = TimeUtils.createTimeString(time);
        if(timeText!=null)
        {
            //calculate the span for the text colouring
            int lastLightChar = 0;
            if(timeText!=null)
            {
                for(int i=0;i<12;i++)
                {
                    if(timeText.charAt(i)=='0' || timeText.charAt(i)==':' || timeText.charAt(i)=='.')
                    {
                        lastLightChar=i+1;
                    }else
                    {
                        break;
                    }
                }
            }

            if(lastLightChar>0)
            {
                SpannableString text = new SpannableString(timeText);

                text.setSpan(new TextAppearanceSpan(getSherlockActivity(), R.style.TimeTextLight),0,lastLightChar, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new TextAppearanceSpan(getSherlockActivity(), R.style.TimeTextDark),lastLightChar,12, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                mCounter.setText(text, TextView.BufferType.SPANNABLE);
            }else
            {
                mCounter.setText(timeText);
            }
        }
	}
	
	public void resetTime()
	{
		if(mCounter!=null)mCounter.setText(TimeUtils.createTimeString(0));
	}
}

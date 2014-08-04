package com.geekyouup.android.ustopwatch;

import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimeUtils {

	private static final String START_TIME = "00:00:00.000";
	private static int mSecsValue = 0;
	private static int mMinsValue = 0;
	private static int mHoursValue = 0;

	public static String createTimeString(double time) {
		if (time == 0) return START_TIME;
		boolean isNeg = false;
		if(time<0)
		{
			isNeg=true;
			time=-time;
		}
		
		int numHours = (int) Math.floor(time / 3600000);
		int numMins = (int) Math.floor(time / 60000 - numHours * 60);
		int numSecs = (int) Math.floor(time / 1000 - numMins * 60 - numHours * 3600);
		int numMillis = ((int) (time - numHours * 3600000 - numMins * 60000 - numSecs * 1000));

		return (isNeg?"-":"")+((numHours < 10 ? "0" : "") + numHours) + ":" + ((numMins < 10 ? "0" : "") + numMins) + ":"
				+ ((numSecs < 10 ? "0" : "") + numSecs) + "." + (numMillis < 10 ? "00" : (numMillis < 100 ? "0" : ""))
				+ numMillis;
	}

    public static SpannableString createStyledSpannableString(Context context, double time, boolean lightTheme)
    {
        String text  = createTimeString(time);
        return createSpannableString(context, text, lightTheme);
    }

	public static View createTimeSelectDialogLayout(Context cxt, LayoutInflater layoutInflater, int hours, int mins, int secs) {
		View countdownView = layoutInflater.inflate(R.layout.countdown, null);

        if(mSecsValue==0) mSecsValue=secs;
        if(mMinsValue==0) mMinsValue=mins;
        if(mHoursValue==0) mHoursValue=hours;

		final TextView mSecsText = (TextView) countdownView.findViewById(R.id.secsTxt);
		final TextView mMinsText = (TextView) countdownView.findViewById(R.id.minsTxt);
		final TextView mHoursText = (TextView) countdownView.findViewById(R.id.hoursTxt);
		mSecsText.setText(mSecsValue + "");
		mSecsText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.length() == 0) {
					mSecsValue = 0;
				} else  {
					try {
						if(s!=null) mSecsValue = Integer.parseInt(s.toString());
					} catch (Exception ignored) {}
                }
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mMinsText.setText(mMinsValue + "");
		mMinsText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.length() == 0) {
					mMinsValue = 0;
				} else {
					try {
						if(s!=null) mMinsValue = Integer.parseInt(s.toString());
					} catch (Exception ignored) {}
                }
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mHoursText.setText(mHoursValue + "");
		mHoursText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.length() == 0) {
					mHoursValue = 0;
				} else
					try {
						if(s!=null) mHoursValue = Integer.parseInt(s.toString());
					} catch (Exception ignored) {}
            }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		Button mSecsIncr = (Button) countdownView.findViewById(R.id.secsBtnUp);
		mSecsIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSecsValue = (mSecsValue + 1) % 60;
				mSecsText.setText(mSecsValue + "");
			}
		});

		Button mSecsDown = (Button) countdownView.findViewById(R.id.secsBtnDn);
		mSecsDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSecsValue--;
				if (mSecsValue < 0)
					mSecsValue = 60 + mSecsValue;
				mSecsText.setText(mSecsValue + "");
			}
		});

		Button mMinsIncr = (Button) countdownView.findViewById(R.id.minsBtnUp);
		mMinsIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mMinsValue = (mMinsValue + 1) % 60;
				mMinsText.setText(mMinsValue + "");
			}
		});

		Button mMinsDown = (Button) countdownView.findViewById(R.id.minsBtnDn);
		mMinsDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mMinsValue--;
				if (mMinsValue < 0) mMinsValue = 60 + mMinsValue;
				mMinsText.setText(mMinsValue + "");
			}
		});

		Button mHoursIncr = (Button) countdownView.findViewById(R.id.hoursBtnUp);
		mHoursIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mHoursValue = (mHoursValue + 1) % 100;
				mHoursText.setText(mHoursValue + "");
			}
		});

		Button mHoursDown = (Button) countdownView.findViewById(R.id.hoursBtnDn);
		mHoursDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mHoursValue--;
				if (mHoursValue < 0)
					mHoursValue = 100 + mHoursValue;
				mHoursText.setText(mHoursValue + "");
			}
		});

		LinearLayout ll = new LinearLayout(cxt);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.addView(countdownView);
		ll.setGravity(Gravity.CENTER);

		return ll;
	}
	
	public static int getDlgMins()
	{
		return mMinsValue;
	}
	
	public static int getDlgSecs()
	{
		return mSecsValue;
	}
	
	public static int getDlgHours()
	{
		return mHoursValue;
	}

    private static SpannableString createSpannableString(Context context, String timeText, boolean lightTheme) {
        SpannableString sString = null;

        try
        {
            if(timeText!=null && context!=null)
            {
                int textLength = timeText.length();
                //calculate the span for the text colouring
                int lastLightChar = 0;
                for(int i=0;i<textLength;i++)
                {
                    if(timeText.charAt(i)=='0' || timeText.charAt(i)==':' || timeText.charAt(i)=='.' || timeText.charAt(i)=='-')
                    {
                        lastLightChar=i+1;
                    }else
                    {
                        break;
                    }
                }

                sString = new SpannableString(timeText);

                if(lastLightChar>0)
                {
                    if(lastLightChar>0) sString.setSpan(new TextAppearanceSpan(context, lightTheme?R.style.TimeTextLight:R.style.TimeTextDarkThemeDark),0,lastLightChar, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(lastLightChar<textLength) sString.setSpan(new TextAppearanceSpan(context, lightTheme?R.style.TimeTextDark:R.style.TimeTextDarkThemeLight), lastLightChar, textLength, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }catch(Exception e)
        {
            Log.e("USW","Switched Fragment Error",e);
        }
        return sString;
    }
}

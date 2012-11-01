package com.geekyouup.android.ustopwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import com.actionbarsherlock.app.SherlockActivity;

public class SettingsActivity extends SherlockActivity {

    private CompoundButton mSwitchSoundTicking;
    private CompoundButton mSwitchEndlessAlarm;
    private static boolean isTicking=false;
    private static boolean isEndlessAlarm = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final Context context = this;
        mSwitchSoundTicking = (CompoundButton) findViewById(R.id.settings_seconds_sound);
        mSwitchSoundTicking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isTicking=b;
                if(b) SoundManager.getInstance(context).unmuteTicking();
                else SoundManager.getInstance(context).muteTicking();
            }
        });

        mSwitchEndlessAlarm = (CompoundButton) findViewById(R.id.settings_endless_alert);
        mSwitchEndlessAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isEndlessAlarm=b;
            }
        });

        mSwitchEndlessAlarm.setChecked(isEndlessAlarm);
        mSwitchSoundTicking.setChecked(isTicking);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getSharedPreferences(UltimateStopwatchActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(UltimateStopwatchActivity.KEY_TICKING,SettingsActivity.isTicking());
        editor.putBoolean(UltimateStopwatchActivity.KEY_ENDLESS_ALARM, SettingsActivity.isEndlessAlarm());
        editor.commit();

    }

    public static boolean isTicking() {
        return isTicking;
    }

    public static boolean isEndlessAlarm() {
        return isEndlessAlarm;
    }

    public static void setTicking(boolean ticking) {
        isTicking = ticking;
    }

    public static void setEndlessAlarm(boolean endlessAlarm) {
        isEndlessAlarm = endlessAlarm;
    }
}

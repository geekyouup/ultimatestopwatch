package com.geekyouup.android.ustopwatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity {

    private static boolean isTicking = false;
    private static boolean isLaptimerEnabled = false;
    private static boolean isEndlessAlarm = false;
    private static boolean isVibrate = true;
    private static boolean isAnimating = true;
    private static final String KEY_TICKING = "key_ticking_on";
    private static final String KEY_ENDLESS_ALARM = "key_endless_alarm_on";
    private static final String KEY_VIBRATE = "key_vibrate_on";
    private static final String KEY_ANIMATING = "key_animations_on";
    private static final String KEY_LAP_TIMER = "key_laptimer_on";

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        CompoundButton mSwitchLaptimer = (CompoundButton) findViewById(R.id.settings_seconds_laptimer);
        mSwitchLaptimer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaptimerEnabled = b;
            }
        });


        /*CompoundButton mSwitchSoundTicking = (CompoundButton) findViewById(R.id.settings_seconds_sound);
        mSwitchSoundTicking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isTicking = b;
            }
        });*/

        CompoundButton mSwitchAnimating = (CompoundButton) findViewById(R.id.settings_animations);
        mSwitchAnimating.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isAnimating = b;
            }
        });

        CompoundButton mSwitchEndlessAlarm = (CompoundButton) findViewById(R.id.settings_endless_alert);
        mSwitchEndlessAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isEndlessAlarm = b;
            }
        });

        CompoundButton mSwitchVibrate = (CompoundButton) findViewById(R.id.settings_vibrate);
        mSwitchVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isVibrate = b;
            }
        });

        mSwitchLaptimer.setChecked(isLaptimerEnabled);
        mSwitchEndlessAlarm.setChecked(isEndlessAlarm);
        //mSwitchSoundTicking.setChecked(isTicking);
        mSwitchVibrate.setChecked(isVibrate);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB
                && !((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            mSwitchVibrate.setChecked(false);
            mSwitchVibrate.setEnabled(false);
        }

        mSwitchAnimating.setChecked(isAnimating);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getSharedPreferences(UltimateStopwatchActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        //editor.putBoolean(KEY_TICKING, isTicking);
        editor.putBoolean(KEY_ENDLESS_ALARM, isEndlessAlarm);
        editor.putBoolean(KEY_VIBRATE, isVibrate);
        editor.putBoolean(KEY_ANIMATING, isAnimating);
        editor.putBoolean(KEY_LAP_TIMER, isLaptimerEnabled);
        editor.commit();
    }

    //Called from parent Activity to ensure all settings are always loaded
    public static void loadSettings(SharedPreferences prefs) {
        isLaptimerEnabled = prefs.getBoolean(KEY_LAP_TIMER, false);
        //isTicking = prefs.getBoolean(KEY_TICKING, false);
        isEndlessAlarm = prefs.getBoolean(KEY_ENDLESS_ALARM, false);
        isVibrate = prefs.getBoolean(KEY_VIBRATE, false);
        isAnimating = prefs.getBoolean(KEY_ANIMATING, true);
    }

    public static boolean isTicking() {
        return isTicking;
    }

    public static boolean isEndlessAlarm() {
        return isEndlessAlarm;
    }

    public static boolean isVibrate() {
        return isVibrate;
    }

    public static boolean isAnimating() {
        return isAnimating;
    }

    public static boolean isLaptimerEnabled() {
        return isLaptimerEnabled;
    }

}

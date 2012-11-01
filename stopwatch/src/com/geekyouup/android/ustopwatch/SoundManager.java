package com.geekyouup.android.ustopwatch;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundManager {

    private Context mContext;
    private static SoundManager mSoundManagerInstance;
    private SoundPool soundPool;

    public static final int SOUND_COUNTDOWN_ALARM = 1;
    public static final int SOUND_LAPTIME = 2;
    public static final int SOUND_RESET = 3;
    public static final int SOUND_START = 4;
    public static final int SOUND_STOP = 5;
    public static final int SOUND_TICK = 6;

    private static boolean mAudioOn = true;
    private boolean isCountdownTicking = false;
    private boolean isStopwatchTicking = false;

    private int mTickStreamId = 0;


    private HashMap<Integer, Integer> soundPoolMap;

    private SoundManager(Context cxt)
    {
        this.mContext=cxt;

        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();

        soundPoolMap.put(SOUND_COUNTDOWN_ALARM, soundPool.load(mContext, R.raw.countdown_alarm, 1));
        soundPoolMap.put(SOUND_LAPTIME, soundPool.load(mContext, R.raw.lap_time, 1));
        soundPoolMap.put(SOUND_RESET, soundPool.load(mContext, R.raw.reset_watch, 1));
        soundPoolMap.put(SOUND_START, soundPool.load(mContext, R.raw.start, 1));
        soundPoolMap.put(SOUND_STOP, soundPool.load(mContext, R.raw.stop, 1));
        soundPoolMap.put(SOUND_TICK, soundPool.load(mContext, R.raw.tok_repeatit, 2));
    }

    public static SoundManager getInstance(Context cxt)
    {
        if(mSoundManagerInstance==null) mSoundManagerInstance = new SoundManager(cxt);
        return mSoundManagerInstance;
    }

    public void playSound(int soundId)
    {
        if(mAudioOn)
        {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            float streamVolume = mgr
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundPoolMap.get(soundId), streamVolume,
                    streamVolume, 1, 0, 1f);
        }
    }

    public void startCountDownTicking()
    {
        isCountdownTicking=true;
        startTicking();
    }

    public void startStopwatchTicking()
    {
        isStopwatchTicking=true;
        startTicking();
    }

    public void stopCountdownTicking()
    {
        isCountdownTicking=false;
        stopTicking();
    }

    public void stopStopwatchTicking()
    {
        isStopwatchTicking=false;
        stopTicking();
    }

    private void startTicking()
    {
        if(mAudioOn && mTickStreamId==0 && SettingsActivity.isTicking())
        {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            float streamVolume = mgr
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            mTickStreamId = soundPool.play(soundPoolMap.get(SOUND_TICK), streamVolume,
                    streamVolume, 1, -1, 1f);
        }
    }


    private void stopTicking()
    {
        if(!isCountdownTicking && !isStopwatchTicking && mTickStreamId!=0)
        {
            try{soundPool.stop(mTickStreamId);}catch(Exception e){}
            mTickStreamId=0;
        }
    }

    public void muteTicking()
    {
        if(mTickStreamId!=0)
        {
            try{soundPool.stop(mTickStreamId);}catch(Exception e){}
            mTickStreamId=0;
        }
    }

    public void unmuteTicking()
    {
        if(mAudioOn && (isCountdownTicking || isStopwatchTicking)) startTicking();
    }

    public void setAudioState(boolean on)
    {
        mAudioOn=on;

        if(!on && (isCountdownTicking || isStopwatchTicking)) muteTicking();
        else if(on && (isCountdownTicking || isStopwatchTicking)) startTicking();
    }

    public boolean isAudioOn()
    {
        return mAudioOn;
    }

}

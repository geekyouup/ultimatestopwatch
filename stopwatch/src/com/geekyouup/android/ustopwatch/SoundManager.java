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
        playSound(soundId, false);
    }

    int mLoopingSoundId = -1;
    public void playSound(int soundId, boolean endlessLoop)
    {
        if(mAudioOn)
        {
            if(endlessLoop) stopEndlessAlarm();
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            float streamVolume = mgr
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            int playingSoundId = soundPool.play(soundPoolMap.get(soundId), streamVolume,
                    streamVolume, 1, endlessLoop?35:0, 1f);

            if(endlessLoop) mLoopingSoundId = playingSoundId;

        }
    }

    public void stopEndlessAlarm()
    {
        try
        {
            if(mLoopingSoundId != -1) soundPool.stop(mLoopingSoundId);
            mLoopingSoundId=-1;
        }catch(Exception ignored){}
    }

    public void doTick()
    {
        if(mAudioOn && SettingsActivity.isTicking())
        {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            float streamVolume = mgr
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundPoolMap.get(SOUND_TICK), streamVolume,
                    streamVolume, 1, 0, 1f);
        }
    }

    public void setAudioState(boolean on)
    {
        mAudioOn=on;
    }

    public boolean isAudioOn()
    {
        return mAudioOn;
    }

    public boolean isEndlessAlarmSounding()
    {
      return (mLoopingSoundId!=-1);
    }
}

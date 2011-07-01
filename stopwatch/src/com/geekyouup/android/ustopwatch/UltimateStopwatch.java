package com.geekyouup.android.ustopwatch;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.StopwatchView.StopwatchThead;

public class UltimateStopwatch extends Activity implements OnClickListener {
	
	private TextView mTextView;
	private static final String PREFS_NAME="USTOPWATCH_PREFS";
	
	private StopwatchView mStopWatchView;
	private StopwatchThead mWatchThread;
	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock; 
	
    private SoundPool soundPool; 
    public static final int SOUND_ALARM = 1;
    private HashMap<Integer, Integer> soundPoolMap;
    private ImageView mResetBtn;
	public static final String MSG_REQUEST_COUNTDOWN_DLG = "msg_usw_counter";
	
	private static final int MENU_STARTPAUSE = 0;
	private static final int MENU_MODE = 2;
	private MenuItem mModeMenuItem;
	private boolean mJustLaunched = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mJustLaunched=true;
    }
    
    @Override
    protected void onPause() {
        super.onPause();       
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings != null)
        {
	        SharedPreferences.Editor editor = settings.edit();
	        if(editor != null)
	        {
		        mWatchThread.saveState(editor);
		        editor.commit();
	        }
        }

        mStopWatchView.getThread().pause(); // pause when Activity pauses
        mWakeLock.release();
    }    
    
    @Override
    protected void onResume() {
        super.onResume();
        
    	if(!mJustLaunched)
    	{
    		removeSplashText();
    		mJustLaunched=false;
    	}
        
        //cancel next alarm if there is one, and clear notification bar
        AlarmUpdater.cancelCountdownAlarm(this);
        
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mStopWatchView = (StopwatchView) findViewById(R.id.swview);
        mTextView = (TextView) findViewById(R.id.text);
        mResetBtn = (ImageView) findViewById(R.id.resetButton);
        mResetBtn.setOnClickListener(this);
        
        mWatchThread = mStopWatchView.getThread();
        mWatchThread.setApplication(this);
        mWatchThread.setHandler(new Handler() {
            @Override
            public void handleMessage(Message m) {
            	if(m.getData().getBoolean(MSG_REQUEST_COUNTDOWN_DLG, false))
            	{
            		requestTimeDialog();
            	}
            }
        });
        
        mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.stopwatch));
        mWakeLock.acquire();
        
        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_ALARM, soundPool.load(this, R.raw.alarm, 1));

        //if vars stored then use them
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings != null && settings.contains("state"))
        {
        	removeSplashText();
        	mWatchThread.restoreState(settings);
        	setToMode(mWatchThread.isStopwatchMode());
        }
    }

    private void setToMode(boolean isStopwatch)
    {
    	if(mModeMenuItem != null)
    	{
	    	if(isStopwatch)
	    	{
	    		mModeMenuItem.setIcon(R.drawable.countdown);
	    		mModeMenuItem.setTitle(getString(R.string.countdown));
	    	}
	    	else 
	    	{
	    		mModeMenuItem.setIcon(R.drawable.stopwatch);
	    		mModeMenuItem.setTitle(getString(R.string.stopwatch));
	    	}
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0,MENU_STARTPAUSE,0,getString(R.string.startstop)).setIcon(R.drawable.play_pause);
        if(mWatchThread.isStopwatchMode())
        {
        	mModeMenuItem = menu.add(0,MENU_MODE,2,getString(R.string.countdown)).setIcon(R.drawable.countdown);
        }else
        {
        	mModeMenuItem = menu.add(0,MENU_MODE,2,getString(R.string.stopwatch)).setIcon(R.drawable.stopwatch);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == MENU_STARTPAUSE)
    	{
    		mWatchThread.goPauseUnpause();
    	}else if(item.getItemId() == MENU_MODE)
    	{
			boolean newMode = !mWatchThread.isStopwatchMode();
			mWatchThread.setIsStopwatchMode(newMode);
			setToMode(newMode);
			if(!newMode) //newMode = true is stopwatch, false=countdown
			{
			    requestTimeDialog();
			}
			
    	}
    	
    	return true;
    }
    
	public void onClick(View v) {
		removeSplashText();
		if(v==mResetBtn)
		{
			mWatchThread.reset();
			if(!mWatchThread.isStopwatchMode()) requestTimeDialog();
		}
	}
	
	public void removeSplashText()
	{
		mTextView.setVisibility(View.GONE);
	}
	
	private boolean mDialogOnScreen = false;
	public void requestTimeDialog()
	{
		//stop stacking of dialogs
		if(mDialogOnScreen) return;
		
		try{removeSplashText();}catch(Exception e){}
		
        LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View countdownView = TimeUtils.createTimeSelectDialogLayout(this, inflator);
	    
	    LinearLayout ll = new LinearLayout(this);
	    ll.setOrientation(LinearLayout.HORIZONTAL);
	    ll.addView(countdownView);
	    ll.setGravity(Gravity.CENTER);

	    AlertDialog mSelectTime = new AlertDialog.Builder(this).create();
	    mSelectTime.setView(ll);
	    mSelectTime.setTitle(getString(R.string.timer_title));
	    mSelectTime.setButton(getString(R.string.timer_start), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				removeSplashText();
				mDialogOnScreen=false;
				mWatchThread.setTime(TimeUtils.getDlgHours(), TimeUtils.getDlgMins(),TimeUtils.getDlgSecs());
			}});
	    mSelectTime.setButton2(getString(R.string.timer_cancel), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				mDialogOnScreen=false;
			}});
	    mSelectTime.show();
	    
	    mDialogOnScreen=true;
	}

    public void playAlarm() {
        try
        {
	    	AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	        float streamVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	        soundPool.play(soundPoolMap.get(SOUND_ALARM), streamVolume, streamVolume, 1, 0, 1f);
        }catch(Exception e){}
    } 
    
	public void notifyCountdownComplete()
	{
        playAlarm();
		
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);
	}
}
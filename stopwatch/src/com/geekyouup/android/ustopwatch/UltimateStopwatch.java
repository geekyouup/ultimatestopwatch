package com.geekyouup.android.ustopwatch;

import java.util.HashMap;
import com.geekyouup.android.ustopwatch.StopwatchView.StopwatchThead;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
	private static final int MENU_EXIT = 3;
	private MenuItem mModeMenuItem;
	private boolean mJustLaunched = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
        
        //if vars stored then use them
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings != null && settings.contains("state"))
        {
        	removeSplashText();
        	mWatchThread.restoreState(settings);
        	setToMode(mWatchThread.isStopwatchMode());
        }

        mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Stopwatch");
        mWakeLock.acquire();
        
        soundPool = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_ALARM, soundPool.load(this, R.raw.alarm, 1));
     }

    private void setToMode(boolean isStopwatch)
    {
    	if(mModeMenuItem != null)
    	{
	    	if(isStopwatch)
	    	{
	    		mModeMenuItem.setIcon(R.drawable.countdown);
	    		mModeMenuItem.setTitle("Countdown");
	    	}
	    	else 
	    	{
	    		mModeMenuItem.setIcon(R.drawable.stopwatch);
	    		mModeMenuItem.setTitle("Stopwatch");
	    	}
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0,MENU_STARTPAUSE,0,"Start/Pause").setIcon(R.drawable.play_pause);
        if(mWatchThread.isStopwatchMode())
        {
        	mModeMenuItem = menu.add(0,MENU_MODE,2,"Countdown").setIcon(R.drawable.countdown);
        }else
        {
        	mModeMenuItem = menu.add(0,MENU_MODE,2,"Stopwatch").setIcon(R.drawable.stopwatch);
        }
        menu.add(0, MENU_EXIT, 5, "Exit").setIcon(android.R.drawable.ic_lock_power_off );

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
			
    	}else if(item.getItemId() == MENU_EXIT)
    	{
    		try{getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();}catch(Exception e){}
			finish();
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
	
	private int mSecsValue = 0;
	private int mMinsValue = 0;
	private int mHoursValue = 0;
	private boolean mDialogOnScreen = false;
	public void requestTimeDialog()
	{
		//stop stacking of dialogs
		if(mDialogOnScreen) return;
		
		try{removeSplashText();}catch(Exception e){}
		
        LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View countdownView = inflator.inflate(R.layout.countdown, null);

		final TextView mSecsText = (TextView) countdownView.findViewById(R.id.secsTxt);
		final TextView mMinsText = (TextView) countdownView.findViewById(R.id.minsTxt);
		final TextView mHoursText = (TextView) countdownView.findViewById(R.id.hoursTxt);
		mSecsText.setText(mSecsValue+"");
		mSecsText.addTextChangedListener(new TextWatcher() {
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s != null && s.length()==0){mSecsValue=0;} 
				else try{mSecsValue = Integer.parseInt(s.toString());}catch(Exception e){};
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});
		
		mMinsText.setText(mMinsValue+"");
		mMinsText.addTextChangedListener(new TextWatcher() {
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s != null && s.length()==0){mMinsValue=0;} 
				else try{mMinsValue = Integer.parseInt(s.toString());}catch(Exception e){};
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});
		
		mHoursText.setText(mHoursValue+"");
		mHoursText.addTextChangedListener(new TextWatcher() {
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s != null && s.length()==0){mHoursValue=0;} 
				else try{mHoursValue = Integer.parseInt(s.toString());}catch(Exception e){};
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});
		
	    Button mSecsIncr = (Button) countdownView.findViewById(R.id.secsBtnUp);
	    mSecsIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSecsValue=(mSecsValue+1)%60;
				mSecsText.setText(mSecsValue+"");
			}
		});
	    
	    Button mSecsDown= (Button) countdownView.findViewById(R.id.secsBtnDn);
	    mSecsDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSecsValue--;
				if(mSecsValue<0) mSecsValue=60+mSecsValue;
				mSecsText.setText(mSecsValue+"");
			}
		});

	    Button mMinsIncr = (Button) countdownView.findViewById(R.id.minsBtnUp);
	    mMinsIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mMinsValue=(mMinsValue+1)%60;
				mMinsText.setText(mMinsValue+"");
			}
		});
	    
	    Button mMinsDown= (Button) countdownView.findViewById(R.id.minsBtnDn);
	    mMinsDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mMinsValue--;
				if(mMinsValue<0)mMinsValue=60+mMinsValue;
				mMinsText.setText(mMinsValue+"");
			}
		});
	    
	    Button mHoursIncr = (Button) countdownView.findViewById(R.id.hoursBtnUp);
	    mHoursIncr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mHoursValue=(mHoursValue+1)%100;
				mHoursText.setText(mHoursValue+"");
			}
		});
	    
	    Button mHoursDown= (Button) countdownView.findViewById(R.id.hoursBtnDn);
	    mHoursDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mHoursValue--;
				if(mHoursValue<0)mHoursValue=100+mHoursValue;
				mHoursText.setText(mHoursValue+"");
			}
		});
	    
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
				mWatchThread.setTime(mHoursValue, mMinsValue,mSecsValue);
			}});
	    mSelectTime.setButton2(getString(R.string.timer_cancel), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				mDialogOnScreen=false;
			}});
	    mSelectTime.show();
	    
	    mDialogOnScreen=true;
	}

	/*** Capture Back Button and use for browser back, else quit ****/
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			try{getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();}catch(Exception e){}
			return super.onKeyDown(keyCode, event);
		}else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
        	try{((AudioManager)getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);}catch(Exception e){}
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
        	try{((AudioManager)getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);}catch(Exception e){}
			return true;
		}else //not back button or no history to go back to
		{
			return super.onKeyDown(keyCode, event);
		}
	}
	
    public void playAlarm() {
        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float streamVolume = mgr.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        soundPool.play(soundPoolMap.get(SOUND_ALARM), streamVolume, streamVolume, 1, 0, 1f);
    } 
    
	public void notifyCountdownComplete()
	{
        playAlarm();
		
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);
	}
}
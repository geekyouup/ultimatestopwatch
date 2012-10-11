package com.geekyouup.android.ustopwatch;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.Menu;
import com.geekyouup.android.ustopwatch.fragments.*;

public class UltimateStopwatchActivity extends SherlockFragmentActivity {

	private PowerManager mPowerMan;
	private PowerManager.WakeLock mWakeLock;

	public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
	public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
	private static final String WAKE_LOCK_KEY = "ustopwatch";

	public static final boolean IS_HONEYCOMB_OR_ABOVE=android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	private LapTimesFragment mLapTimesFragment;
	private double mCurrentTimeMillis = 0;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        getSupportActionBar().setIcon(R.drawable.actionbaricon);
        setTitle(getString(R.string.app_name_caps));
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        ActionBar.Tab tab1 = getSupportActionBar().newTab().setText("STOPWATCH");
        ActionBar.Tab tab2 = getSupportActionBar().newTab().setText("LAP TIMES");
        ActionBar.Tab tab3 = getSupportActionBar().newTab().setText("COUNTDOWN");
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(tab1,StopwatchFragment.class,null);
        mTabsAdapter.addTab(tab2,LapTimesFragment.class,null);
        mTabsAdapter.addTab(tab3, CountdownFragment.class,null);



		mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// stop landscape more on QVGA/HVGA
		int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		// not in all views
		try
        {
            mLapTimesFragment = (LapTimesFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.laptimes_fragment);

            if(mLapTimesFragment!=null) LapTimeRecorder.getInstance().setLaptimeListener(mLapTimesFragment);
        }catch(Exception e)
        {
            Log.e("USW","Fragments error",e);
        }
	}
	
	private static final String PREFS_NAME="usw_main_prefs";
	private static final String KEY_MODE="usw_mode";	
	
	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();
		
		LapTimeRecorder.getInstance().saveTimes(this);
	}

    @Override
	protected void onResume() {
		super.onResume();

		LapTimeRecorder.getInstance().loadTimes(this);

		mWakeLock = mPowerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				WAKE_LOCK_KEY);
		mWakeLock.acquire();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getSupportMenuInflater();
		//if(mStopwatchFragment.getMode() == StopwatchFragment.MODE_STOPWATCH)
		//{
			inflater.inflate(R.menu.menu, menu);
		//}else
		//{/
		//	inflater.inflate(R.menu.countdown_menu, menu);
		//}
		return true;
	}
     /*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_switchmode) {
			final int newMode = (mStopwatchFragment.getMode() == StopwatchFragment.MODE_STOPWATCH) ? StopwatchFragment.MODE_COUNTDOWN
					: StopwatchFragment.MODE_STOPWATCH;
			mStopwatchFragment.setMode(newMode);
			mCounterView.setMode(newMode);

			if (mStopwatchFragment.getMode() == StopwatchFragment.MODE_COUNTDOWN) requestTimeDialog();
			
			if(IS_HONEYCOMB_OR_ABOVE)
			{
				invalidateOptionsMenu();
			} 
			
			
			if(IS_HONEYCOMB_OR_ABOVE && mLapTimesFragment!=null && mLapTimesFragment.getView()!=null)
			{
				ObjectAnimator oa = ObjectAnimator.ofFloat(mLapTimesFragment.getView(), "rotationY", 0,90);
			    oa.setDuration(250);
			    oa.addListener(new AnimatorListenerAdapter() {
			    	@Override
			    	public void onAnimationEnd(Animator animation) {
			    		super.onAnimationEnd(animation);
			    		mLapTimesFragment.setMode(newMode);
			    		ObjectAnimator oa2 = ObjectAnimator.ofFloat(mLapTimesFragment.getView(), "rotationY", -90,0)
			    			.setDuration(250);
			    		
			    		oa2.addListener(new AnimatorListenerAdapter() {
			    			@Override
			    			public void onAnimationEnd(Animator animation) {
			    				super.onAnimationEnd(animation);
			    				if (newMode == StopwatchFragment.MODE_COUNTDOWN) {
			    					requestTimeDialog();
			    				}
			    			}
			    		});
			    		oa2.start();
			    	}
			    });
				oa.start();
			}
		} else if (item.getItemId() == R.id.menu_laptimes) {
			//this menu item is only available on non-xlarge
			Intent startLaptimes = new Intent(this, LapTimesActivity.class);
			startActivity(startLaptimes);
		}else if (item.getItemId() == R.id.menu_clearlaps) {
			LapTimeRecorder.getInstance().reset(this);
		}

		return true;
	}    */

	/*private void reset() {
		mStopwatchFragment.reset();
		mLapTimesFragment.reset();

		if (mCounterView != null)
			mCounterView.resetTime();

		if (mStopwatchFragment.getMode() == StopwatchFragment.MODE_COUNTDOWN)
			requestTimeDialog();
	}*/


}
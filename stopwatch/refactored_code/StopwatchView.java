/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geekyouup.android.ustopwatch;

import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


/**
 * Animated view that draws the stopwatch, takes keystrokes, etc.
 */
class StopwatchView extends SurfaceView implements SurfaceHolder.Callback {
    class StopwatchThead extends Thread implements OnTouchListener {
        /*
         * State-tracking constants
         */
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        
        private static final String KEY_STATE = "state";
        private static final String KEY_LASTTIME = "lasttime";
        private static final String KEY_NOWTIME = "currenttime";
        private static final String KEY_STOPWATCH_MODE = "stopwatchmode";
        private static final String KEY_LAPTIME_X = "laptime_";
        
        private boolean mTouching=false;
        
        /*
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;   
        private Bitmap mBackgroundImageClick;  
        //private Bitmap mBackgroundBottomImage; 
        private int mBackgroundStartY;
        private int mAppOffsetX=0;
        private double mMinsAngle = 0;
        private double mSecsAngle = 0;
        private double mDisplayTimeMillis = 0;
        private double twoPI = Math.PI*2.0;
        private boolean mStopwatchMode=true;
        
        private String mMillisPart ="000";
        private String mSecsPart="00";
        private String mMinsPart = "00";
        private String mHoursPart = "00"; 

        private int mCanvasWidth = 320;
        private int mCanvasHeight = 480;
        private int mCanvasCenterX = 156;
        private int mSecsCenterY = 230;
        private int mSecsHandLength = 0;
        private int mSecsHandHalfWidth=10;
        private int mMinsHandHalfWidth=10;
        
        private int mMinsCenterY = 185;
        private int mMinsHandLength = 0;
        
        private boolean isWVGA = false;
        
        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        /** Paint to draw the lines on screen. */
        private Paint mTextPaint;
        //private Paint mSmallTextPaint;
        
        //private Drawable mUpArrow;
       // private Drawable mDownArrow;
        
        private Drawable mSecHand;
        private Drawable mMinHand;
        
        private ArrayList<String> mLapTimes = new ArrayList<String>(); 
        private int mTopLaptime = 0;
        
        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode=STATE_READY;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;
        private UltimateStopwatch mApp;
        private Handler mHandler;
        private Context mContext;

        public StopwatchThead(SurfaceHolder surfaceHolder, Context context) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            Resources res = context.getResources();
            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res,R.drawable.background);
            mBackgroundImageClick = BitmapFactory.decodeResource(res,R.drawable.background_click);
            
            //mUpArrow = context.getResources().getDrawable(R.drawable.up);
            //mDownArrow = context.getResources().getDrawable(R.drawable.down);
            mSecHand = context.getResources().getDrawable(R.drawable.sechand);
            mMinHand = context.getResources().getDrawable(R.drawable.minhand);
            
            mMinsHandLength = mMinHand.getIntrinsicHeight();
            mSecsHandLength = mSecHand.getIntrinsicHeight();
            mSecsHandHalfWidth = mSecHand.getIntrinsicWidth()/2;
            mMinsHandHalfWidth = mMinHand.getIntrinsicWidth()/2;
            
            mBackgroundStartY = (mCanvasHeight-mBackgroundImage.getHeight())/2;
            mAppOffsetX = (mCanvasWidth-mBackgroundImage.getWidth())/2;
           
            // Initialize paints for speedometer
            
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(32);
            mTextPaint.setARGB(255,128,128,128);
            mTextPaint.setDither(true);
            
            /*mSmallTextPaint = new Paint();
            mSmallTextPaint.setAntiAlias(true);
            mSmallTextPaint.setTextSize(10);
            mSmallTextPaint.setARGB(255,128,128,128);
            mSmallTextPaint.setDither(true);*/
        }
        
        public void setApplication(UltimateStopwatch mApp)
        {
        	this.mApp = mApp;
        }
        
        public void setHandler(Handler handler)
        {
        	this.mHandler = handler;
        }
        
        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                // First set the game for Medium difficulty
                mLastTime = System.currentTimeMillis();
                setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        public void reset()
        {
        	resetVars();
        }
        
        private void resetVars()
        {
        	synchronized (mSurfaceHolder) {
        		setState(STATE_PAUSE);
	        	mLastTime = 0;
	            mMinsAngle=0;
	            mSecsAngle=0;
	            mDisplayTimeMillis=0;
	            mLapTimes = new ArrayList<String>();
	            mMillisPart ="000";
	            mSecsPart="00";
	            mMinsPart = "00";
	            mHoursPart = "00";
	            mTopLaptime=0;
        	}
        }
        
        public void setTime(int hour, int minute, int seconds)
        {
        	synchronized (mSurfaceHolder) {
	        	setState(STATE_READY);
	        	mLastTime = System.currentTimeMillis();
	            mMinsAngle=(Math.PI*2*((double) minute/30.0));
	            mSecsAngle=(Math.PI*2*((double) seconds/60.0));
	            mDisplayTimeMillis=hour*3600000+minute*60000+seconds*1000;
	            mLapTimes = new ArrayList<String>();
	            mMillisPart ="000";
	            mSecsPart=(seconds<10?"0":"") + seconds;
	            mMinsPart = (minute<10?"0":"") + minute;
	            mHoursPart = (hour<10?"0":"")+hour;
	            
	            doStart();
        	}
        }
        
        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        doDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                    try {
						sleep(30);
					} catch (InterruptedException e) {}
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         * 
         * @return Bundle with this view's state
         */      
        public void saveState(SharedPreferences.Editor map) {
            synchronized (mSurfaceHolder) {
                //if (map != null) {
                	if(!isStopwatchMode() || mDisplayTimeMillis>0 || (mLapTimes!=null&& mLapTimes.size()>0))
                	{
                		if(!isStopwatchMode() && mDisplayTimeMillis>0 && mMode == STATE_RUNNING)
                		{
                			AlarmUpdater.setCountdownAlarm(mContext, (long)mDisplayTimeMillis);
                		}
                		
	                	map.putInt(KEY_STATE,mMode);
	                    map.putLong(KEY_LASTTIME,mLastTime);
	                    map.putLong (KEY_NOWTIME, (long) mDisplayTimeMillis);
	                    if(mLapTimes!= null && mLapTimes.size()>0)
	                    {
	                    	for(int i=0;i<mLapTimes.size();i++) map.putString(KEY_LAPTIME_X+i,mLapTimes.get(i));
	                    }
	                    map.putBoolean(KEY_STOPWATCH_MODE, mStopwatchMode);
                	}else
                	{
                		map.clear();
                //		map.putBoolean(KEY_STOPWATCH_MODE, mStopwatchMode);
                	}
                //}
            }
        }
        
        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         * 
         * @param savedState Bundle containing the game state
         */       
        public synchronized void restoreState(SharedPreferences savedState) {
            synchronized (mSurfaceHolder) {
            	if(savedState!=null)
            	{
            		setState(savedState.getInt(KEY_STATE,STATE_PAUSE));
	                mLastTime = savedState.getLong(KEY_LASTTIME,System.currentTimeMillis());
	                mDisplayTimeMillis = savedState.getLong(KEY_NOWTIME,0);
	                int lapTimeNum=0;
	                mLapTimes = new ArrayList<String>();
	                while(savedState.getString(KEY_LAPTIME_X+lapTimeNum,null) != null)
	                {
	                	mLapTimes.add(savedState.getString(KEY_LAPTIME_X+lapTimeNum,""));
	                	lapTimeNum++;
	                }
	                
	                mStopwatchMode = savedState.getBoolean(KEY_STOPWATCH_MODE, true);
	                updatePhysics();
	            }
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        public boolean isRunning()
        {
        	return mRun;
        }
        
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         * 
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                mMode = mode;    
            }
        }
       
        /**
         * Resumes from a pause.
         */
        public void unpause() {
            //stop timer at end
            if(!isStopwatchMode() && mDisplayTimeMillis<=0)
            {
            	resetVars(); //applies pause state
    			if(mHandler != null)
    			{
            		Message msg = mHandler.obtainMessage();
        	        Bundle b = new Bundle();
        	        b.putBoolean(UltimateStopwatch.MSG_REQUEST_COUNTDOWN_DLG, true);
        	        msg.setData(b);
        	        mHandler.sendMessage(msg);
    			}
            }else
            {
	            // Move the real time clock up to now
	            synchronized (mSurfaceHolder) {
	                mLastTime = System.currentTimeMillis();
	            }
	            setState(STATE_RUNNING);
            }
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
        	canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(mTouching?mBackgroundImageClick:mBackgroundImage, mAppOffsetX,mBackgroundStartY, null);
            //canvas.drawBitmap(mBackgroundBottomImage, 0,mCanvasHeight-mBackgroundBottomImage.getHeight(), null);
            
            /*
             //see if we should draw up arrow on lap times
             
            if(mTopLaptime>0)
            {
                mUpArrow.setBounds(44+mAppOffsetX, mCanvasHeight-70-(isWVGA?36:0), 54+mAppOffsetX, mCanvasHeight-64-(isWVGA?36:0));
                mUpArrow.draw(canvas);
            }*/
            
            
            
            if(mMode!=STATE_READY)
            {
            	canvas.drawText(mHoursPart+":"+mMinsPart+":"+mSecsPart+"."+mMillisPart,120+mAppOffsetX+(isWVGA?55:0), mCanvasHeight-25-(isWVGA?16:0), mTextPaint);
            	
            	/*
            	 //Laptimes moved to new activity
            	 //draw lap times
            	if(isStopwatchMode())
            	{
	            	canvas.drawText("lap"+(mTopLaptime+1)+" "+ (mLapTimes.size()>mTopLaptime?mLapTimes.get(mTopLaptime):""),13+mAppOffsetX+(isWVGA?8:0),mCanvasHeight-54-(isWVGA?28:0),mSmallTextPaint);
	            	canvas.drawText("lap"+(mTopLaptime+2) + " " + (mLapTimes.size()>mTopLaptime+1?mLapTimes.get(mTopLaptime+1):""),13+mAppOffsetX+(isWVGA?8:0),mCanvasHeight-34-(isWVGA?19:0),mSmallTextPaint);
	            	canvas.drawText("lap"+(mTopLaptime+3) + " "+ (mLapTimes.size()>mTopLaptime+2?mLapTimes.get(mTopLaptime+2):""),13+mAppOffsetX+(isWVGA?8:0),mCanvasHeight-14-(isWVGA?10:0),mSmallTextPaint);
            	}*/
            }
            
            /*
            //see if we should draw down arrow on lap times
            if(mTopLaptime<mLapTimes.size()-3)
            {
                mDownArrow.setBounds(44+mAppOffsetX, mCanvasHeight-10, 54+mAppOffsetX, mCanvasHeight-4);
                mDownArrow.draw(canvas);
            }*/
            
            // Draw the secs hand with its current rotation
            canvas.save();
            canvas.rotate((float) Math.toDegrees(mSecsAngle), mCanvasCenterX,mSecsCenterY);
            mSecHand.setBounds(mCanvasCenterX-mSecsHandHalfWidth, mSecsCenterY-mSecsHandLength+(isWVGA?38:26), mCanvasCenterX+mSecsHandHalfWidth, mSecsCenterY+(isWVGA?38:26));
            mSecHand.draw(canvas);
            canvas.restore();
            
            //draw the mins hand with its current rotatiom
            canvas.save();
            canvas.rotate((float) Math.toDegrees(mMinsAngle), mCanvasCenterX,mMinsCenterY);
            mMinHand.setBounds(mCanvasCenterX-mMinsHandHalfWidth, mMinsCenterY-mMinsHandLength+(isWVGA?15:10), mCanvasCenterX+mMinsHandHalfWidth, mMinsCenterY+(isWVGA?15:10));
            mMinHand.draw(canvas);
            canvas.restore();
        }

        
        /**
         * Figures the bike state (x, y, ...) based on the passage of
         * realtime. Does not invalidate(). Called at the start of draw().
         * Detects the end-of-game and sets the UI to the next state.
         */
        private void updatePhysics() {
            long now = System.currentTimeMillis();
            
			if(mMode == STATE_RUNNING)
			{
	            if(isStopwatchMode())mDisplayTimeMillis += (now - mLastTime);
	            else mDisplayTimeMillis -= (now - mLastTime);
			}else
			{
				mLastTime=now;
			}
            
            mMinsAngle=twoPI*(mDisplayTimeMillis/1800000.0); //mins is 0 to 30
            mSecsAngle=twoPI*(mDisplayTimeMillis/60000.0);
            
            int numHours = (int) Math.floor(mDisplayTimeMillis/3600000);
            mHoursPart = (numHours<10?"0":"")+numHours;
            
            int numMins = (int) Math.floor(mDisplayTimeMillis/60000 - numHours*60);
            mMinsPart = (numMins<10?"0":"") + numMins;
            
            int numSecs = (int) Math.floor(mDisplayTimeMillis/1000 - numMins*60-numHours*3600);
            mSecsPart = (numSecs<10?"0":"") + numSecs;
            
            int numMillis = ((int)(mDisplayTimeMillis-numHours*3600000-numMins*60000-numSecs*1000));
            mMillisPart = (numMillis<10?"00":(numMillis<100?"0":"")) + numMillis;
            
            mLastTime = now;
            
            //stop timer at end
            if(mMode == STATE_RUNNING && !isStopwatchMode() && mDisplayTimeMillis<=0)
            {
            	resetVars(); //applies pause state
            	if(mApp!=null)
            	{
            		mApp.notifyCountdownComplete();
            		//mApp.requestTimeDialog(); //need a handler to pass this back
        			if(mHandler != null)
        			{
	            		Message msg = mHandler.obtainMessage();
	        	        Bundle b = new Bundle();
	        	        b.putBoolean(UltimateStopwatch.MSG_REQUEST_COUNTDOWN_DLG, true);
	        	        msg.setData(b);
	        	        mHandler.sendMessage(msg);
        			}
            	}
            	
            }
        }

		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction()==MotionEvent.ACTION_DOWN)
			{
				mTouching = true;
				if(event.getY()<mCanvasHeight-80) //pause at top of screen
				{
					goPauseUnpause();
				}else if(isStopwatchMode() && mDisplayTimeMillis!=0)//lap time at bottom
				{
					if(event.getX()>mCanvasWidth/2)
					{
						mLapTimes.add(mHoursPart+":"+mMinsPart+":"+mSecsPart+"."+mMillisPart);
						if(mLapTimes.size()>3)mTopLaptime=mLapTimes.size()-3;
					}else if(event.getY()>mCanvasHeight-30)
					{
						if(mTopLaptime<mLapTimes.size()-3) mTopLaptime++;
					}else
					{
						if(mTopLaptime>0) mTopLaptime--;
					}

				}
			}else if(event.getAction()==MotionEvent.ACTION_UP)
			{
				mTouching = false;
			}
			return true;
		}
		
		public void goPauseUnpause()
		{
			if(mApp != null) mApp.removeSplashText();
			
			if(mMode == STATE_PAUSE)
			{
				unpause();
			}else if(mMode == STATE_RUNNING)
			{
				pause();
			}else{
				doStart();
			}
		}
		
		public ArrayList<String> getLaptimes()
		{
			return mLapTimes;
		}
		
		public String getTime()
		{
			return mHoursPart+":"+mMinsPart+":"+mSecsPart+"."+mMillisPart;
		}
		
		public boolean isStopwatchMode()
		{
			return mStopwatchMode;
		}
		
		public void setIsStopwatchMode(boolean isStopwatchMode)
		{
			this.mStopwatchMode = isStopwatchMode;
			resetVars();
		}
		
		public boolean doTrackBall(MotionEvent event)
		{
			if(event.getAction()==MotionEvent.ACTION_DOWN)
			{
				//track click, record lap time
				mLapTimes.add(getTime());
			}else
			{
				float yPos = event.getY();
				if(yPos<0)
				{
					if(mTopLaptime>0) mTopLaptime--;
				}
				else if(yPos>0)
				{
					if(mTopLaptime<mLapTimes.size()-3) mTopLaptime++;
				}
			}
			
			return true;
		}
		
		//none trackball devices
		public boolean doKeypress(int keyCode)
		{
			if(keyCode ==KeyEvent.KEYCODE_DPAD_UP)
			{
				if(mTopLaptime>0) mTopLaptime--;
				return true;
			}
			else if(keyCode ==KeyEvent.KEYCODE_DPAD_DOWN)
			{
				if(mTopLaptime<mLapTimes.size()-3) mTopLaptime++;
				return true;
			}else if(keyCode ==KeyEvent.KEYCODE_DPAD_CENTER || keyCode ==KeyEvent.KEYCODE_SPACE)
			{
				goPauseUnpause();
				return true;
			}
			return false;
		}
		
        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                
                isWVGA = mCanvasWidth>=480;
                
                mSecsCenterY = mCanvasHeight/2;
                mMinsCenterY = mSecsCenterY-(isWVGA?75:43);
                mBackgroundStartY = (mCanvasHeight-mBackgroundImage.getHeight())/2;
                mAppOffsetX = (mCanvasWidth-mBackgroundImage.getWidth())/2;
                
                mCanvasCenterX = width/2;
                
                //mTextPaint.setTextSize(isWVGA?48:32);
                //mSmallTextPaint.setTextSize(isWVGA?15:10);
            }
        }
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    private StopwatchThead thread;
    public StopwatchView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new StopwatchThead(holder, context);
        setOnTouchListener(thread);
        setFocusableInTouchMode(true);
        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     * 
     * @return the animation thread
     */
    public StopwatchThead getThread() {
        return thread;
    }
    
    public StopwatchThead createNewThread(Context context)
    {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    	
        thread = new StopwatchThead(holder, context);
        setOnTouchListener(thread);
        return thread;
    }

    @Override 
    public boolean onTrackballEvent(MotionEvent event) {
    	return thread.doTrackBall(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	boolean consumed = thread.doKeypress(keyCode);
    	if(!consumed) return super.onKeyDown(keyCode, event);
    	else return consumed;
    }
    
    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
    	try
    	{
	    	if(!thread.isRunning())
	    	{
		    	thread.setRunning(true);
		        thread.start();
	    	}
    	}catch(Exception e)
    	{		
    	}
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}

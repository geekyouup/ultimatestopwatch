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

package com.geekyouup.android.ustopwatch.fragments;

import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Animated view that draws the stopwatch, takes keystrokes, etc.
 */
public class StopwatchView extends SurfaceView implements SurfaceHolder.Callback {
	public class StopwatchThead extends Thread implements OnTouchListener {
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
		private double mScaleFactor = 1; //how much to scale the images up or down by
		
		private boolean mTouching=false;
		/*
		 * Member (state) fields
		 */
		/** The drawable to use as the background of the animation canvas */
		private Bitmap mBackgroundImage;
		private Bitmap mBackgroundImageTouched;
		private int mBackgroundStartY;
		private int mAppOffsetX = 0;
		private int mAppOffsetY = 0;
		private double mMinsAngle = 0;
		private double mSecsAngle = 0;
		private double mDisplayTimeMillis = 0;
		private double twoPI = Math.PI * 2.0;
		private boolean mStopwatchMode = true;

		private int mCanvasWidth = 320;
		private int mCanvasHeight = 480;
		private int mSecsCenterX = 156;
		private int mSecsCenterY = 230;
		private int mMinsCenterX = 156;
		private int mMinsCenterY = 185;

		private int mSecsHalfWidth = 0;
		private int mSecsHalfHeight = 0;
		private int mMinsHalfWidth = 0;
		private int mMinsHalfHeight = 0;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		private Drawable mSecHand;
		private Drawable mMinHand;

		/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
		private int mMode = STATE_READY;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;
		private boolean mSkipDraw = false;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;
		private UltimateStopwatchActivity mApp;
		private Handler mHandler;
		private Context mContext;

		public StopwatchThead(SurfaceHolder surfaceHolder, Context context) {
			// get handles to some important objects
			mSurfaceHolder = surfaceHolder;
			mContext = context;
			
			Resources res = mContext.getResources();
			loadGraphics(res, isStopwatchMode());
		}

		private void loadGraphics(Resources res, boolean isStopwatch)
		{
			if(res==null) res = mContext.getResources();

			mSkipDraw=true;
			//switch background graphic
			if(isStopwatch)
			{
				mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background);
				mBackgroundImageTouched = BitmapFactory.decodeResource(res, R.drawable.background_click);
				mSecHand = res.getDrawable(R.drawable.sechand);
				mMinHand = res.getDrawable(R.drawable.minhand);
			}else
			{
				mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background_cd);
				mBackgroundImageTouched = BitmapFactory.decodeResource(res, R.drawable.background_cd_click);
				mSecHand = res.getDrawable(R.drawable.sechand_cd);
				mMinHand = res.getDrawable(R.drawable.minhand_cd);
			}
			
			mSecsHalfWidth = mSecHand.getIntrinsicWidth()/2;
			mSecsHalfHeight = mSecHand.getIntrinsicHeight()/2;
			
			mMinsHalfWidth = mMinHand.getIntrinsicWidth()/2;
			mMinsHalfHeight = mMinHand.getIntrinsicHeight()/2;
			
			mBackgroundStartY = (mCanvasHeight - mBackgroundImage.getHeight()) / 2;
			mAppOffsetX = (mCanvasWidth - mBackgroundImage.getWidth()) / 2;
			
			scaleImages();
			mSkipDraw=false;
		}
		
		public void setApplication(UltimateStopwatchActivity mApp) {
			this.mApp = mApp;
		}

		public void setHandler(Handler handler) {
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
				if (mMode == STATE_RUNNING)
					setState(STATE_PAUSE);
			}
		}

		public void reset() {
			resetVars();
			if (!isStopwatchMode() && mDisplayTimeMillis <= 0) requestCountdownDialog();
			broadcastClockTime(0);
		}

		private void resetVars() {
			synchronized (mSurfaceHolder) {
				setState(STATE_PAUSE);
				mLastTime = 0;
				mMinsAngle = 0;
				mSecsAngle = 0;
				mDisplayTimeMillis = 0;
				
				broadcastClockTime(0);
			}
		}

		public void setTime(int hour, int minute, int seconds) {
			synchronized (mSurfaceHolder) {
				setState(STATE_READY);
				mLastTime = System.currentTimeMillis();
				mMinsAngle = (Math.PI * 2 * ((double) minute / 30.0));
				mSecsAngle = (Math.PI * 2 * ((double) seconds / 60.0));
				mDisplayTimeMillis = hour * 3600000 + minute * 60000 + seconds * 1000;
				
				doStart();
			}
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					//auto double buffer by locking canvas
					c = mSurfaceHolder.lockCanvas(null);
					if(c!= null)
					{
						synchronized (mSurfaceHolder) {
							if (mMode == STATE_RUNNING) updatePhysics();
							if(!mSkipDraw) doDraw(c);
						}
					}
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
					try {
						sleep(30);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		/**
		 * Update the time
		 */
		private void updatePhysics() {
			long now = System.currentTimeMillis();

			if(mMode == STATE_RUNNING)
			{
				if (isStopwatchMode())
					mDisplayTimeMillis += (now - mLastTime);
				else
					mDisplayTimeMillis -= (now - mLastTime);
			}else
			{
				mLastTime=now;
			}

			// mins is 0 to 30
			mMinsAngle = twoPI * (mDisplayTimeMillis / 1800000.0);
			mSecsAngle = twoPI * (mDisplayTimeMillis / 60000.0);

			if(mDisplayTimeMillis<0) mDisplayTimeMillis=0;
			
			// send the time back to the Activity to update the other views
			broadcastClockTime(isStopwatchMode()?mDisplayTimeMillis:-mDisplayTimeMillis);
			mLastTime = now;

			// stop timer at end
			if (mMode == STATE_RUNNING && !isStopwatchMode() && mDisplayTimeMillis <= 0) {
				resetVars(); // applies pause state
				if (mApp != null) {
					mApp.notifyCountdownComplete();
					requestCountdownDialog();
				}

			}
		}		
		
		/**
		 * Draws the background and hands on the Canvas.
		 */
		private void doDraw(Canvas canvas) {
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			canvas.drawColor(Color.WHITE);
			canvas.drawBitmap(mTouching?mBackgroundImageTouched:mBackgroundImage, mAppOffsetX, mBackgroundStartY + mAppOffsetY, null);

			// Draw the secs hand with its current rotation
			canvas.save();
			canvas.rotate((float) Math.toDegrees(mSecsAngle), mSecsCenterX, mSecsCenterY + mAppOffsetY);
			mSecHand.setBounds(mSecsCenterX - mSecsHalfWidth, mSecsCenterY - mSecsHalfHeight + mAppOffsetY,
					mSecsCenterX + mSecsHalfWidth, mSecsCenterY + mAppOffsetY + mSecsHalfHeight);
			mSecHand.draw(canvas);
			canvas.restore();

			// draw the mins hand with its current rotatiom
			canvas.save();
			canvas.rotate((float) Math.toDegrees(mMinsAngle), mMinsCenterX, mMinsCenterY + mAppOffsetY);
			mMinHand.setBounds(mMinsCenterX - mMinsHalfWidth, mMinsCenterY - mMinsHalfHeight + mAppOffsetY,
					mMinsCenterX + mMinsHalfWidth, mMinsCenterY + mAppOffsetY + mMinsHalfHeight);
			mMinHand.draw(canvas);
			canvas.restore();
		}

		/**
		 * Dump game state to the provided Bundle. Typically called when the
		 * Activity is being suspended.
		 * 
		 * @return Bundle with this view's state
		 */
		public void saveState(SharedPreferences.Editor map) {
			synchronized (mSurfaceHolder) {
				if (!isStopwatchMode() || mDisplayTimeMillis > 0) {
					if (!isStopwatchMode() && mDisplayTimeMillis > 0 && mMode == STATE_RUNNING) {
						AlarmUpdater.setCountdownAlarm(mContext, (long) mDisplayTimeMillis);
					}else
					{
						AlarmUpdater.cancelCountdownAlarm(mContext); //just to be sure
					}

					map.putInt(KEY_STATE, mMode);
					map.putLong(KEY_LASTTIME, mLastTime);
					map.putLong(KEY_NOWTIME, (long) mDisplayTimeMillis);
					map.putBoolean(KEY_STOPWATCH_MODE, mStopwatchMode);
				} else {
					map.clear();
				}
			}
		}

		/**
		 * Restores state from the indicated Bundle. Called when
		 * the Activity is being restored after having been previously
		 * destroyed.
		 */
		private synchronized void restoreState(SharedPreferences savedState) {
			synchronized (mSurfaceHolder) {
				if (savedState != null) {
					setState(savedState.getInt(KEY_STATE, STATE_PAUSE));
					mLastTime = savedState.getLong(KEY_LASTTIME, System.currentTimeMillis());
					mDisplayTimeMillis = savedState.getLong(KEY_NOWTIME, 0);
					mStopwatchMode = savedState.getBoolean(KEY_STOPWATCH_MODE, true);
					loadGraphics(null, mStopwatchMode);
					updatePhysics();
				}
				
				AlarmUpdater.cancelCountdownAlarm(mContext); //just to be sure
			}
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		public boolean isRunning() {
			return mRun;
		}

		/**
		 * Sets the  mode. That is, whether we are running, paused, in the
		 * failure state etc.
		 */
		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				mMode = mode;
				Log.d("USW", "Mode set to " + mMode);
			}
		}

		/**
		 * Resumes from a pause.
		 */
		public void unpause() {
			// stop timer at end
			if (!isStopwatchMode() && mDisplayTimeMillis <= 0) {
				resetVars(); // applies pause state
				requestCountdownDialog();
			} else {
				// Move the real time clock up to now
				synchronized (mSurfaceHolder) {
					mLastTime = System.currentTimeMillis();
				}
				setState(STATE_RUNNING);
			}
		}

		private void broadcastClockTime(double mTime)
		{
			if (mHandler != null) {
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME, true);
				b.putDouble(UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE, mTime);
				msg.setData(b);
				mHandler.sendMessage(msg);
			}
		}
		
		private void requestCountdownDialog()
		{
			if (mHandler != null) {
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putBoolean(UltimateStopwatchActivity.MSG_REQUEST_COUNTDOWN_DLG, true);
				msg.setData(b);
				mHandler.sendMessage(msg);
			}
		}

		public void startStop() {
			if (mMode == STATE_PAUSE) {
				unpause();
			} else if (mMode == STATE_RUNNING) {
				pause();
			} else {
				doStart();
			}
		}

		public boolean isStopwatchMode() {
			return mStopwatchMode;
		}

		public void setIsStopwatchMode(boolean isStopwatchMode) {
			this.mStopwatchMode = isStopwatchMode;
			resetVars();
			loadGraphics(null, isStopwatchMode);
		}

		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mTouching = true;
				startStop();
			}else if (event.getAction() == MotionEvent.ACTION_UP) {
				mTouching = false;
			}
			
			return true;
		}
		
		// none trackball devices
		public boolean doKeypress(int keyCode) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
				startStop();
				return true;
			}
			return false;
		}

		//trackball device
		public boolean doTrackBall(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				startStop();
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

				loadGraphics(null,isStopwatchMode());
				
				Log.d("USW", "AppXOffset: " + mAppOffsetX + ", bgImageWidth: " + mBackgroundImage.getWidth());
			}
		}
		
		private void scaleImages()
		{
			// need to check 2 things. If image is narrower than canvas
			// scale it down, then if image is taller than canvas shift it down
			int bgImageWidth = mBackgroundImage.getWidth();
			int bgImageHeight = mBackgroundImage.getHeight();
			mScaleFactor=1;
			
			if(bgImageWidth > mCanvasWidth) mScaleFactor = ((double) mCanvasWidth / (double) bgImageWidth);
			if(bgImageHeight > mCanvasHeight) mScaleFactor = ((double)mCanvasHeight / (double) bgImageHeight);
			
			Log.d("USW","ScaleFactor " + mScaleFactor);
			
			if(mScaleFactor != 1)
			{
				mBackgroundImage = Bitmap
				.createScaledBitmap(mBackgroundImage, (int) ((double) bgImageWidth * mScaleFactor),
						(int) ((double) bgImageHeight * mScaleFactor),
						false);
				
				mBackgroundImageTouched = Bitmap
				.createScaledBitmap(mBackgroundImageTouched, (int) ((double) bgImageWidth * mScaleFactor),
						((int) ((double) bgImageHeight * mScaleFactor)),
						false);
				
				mMinsHalfHeight = (int) ((double) mMinsHalfHeight * mScaleFactor);
				mMinsHalfWidth = (int) ((double) mMinsHalfWidth * mScaleFactor);
				mSecsHalfHeight= (int) ((double) mSecsHalfHeight * mScaleFactor);
				mSecsHalfWidth= (int) ((double) mSecsHalfWidth * mScaleFactor);
				
				bgImageWidth = mBackgroundImage.getWidth();
				bgImageHeight = mBackgroundImage.getHeight();
			}
			
			mBackgroundStartY = (mCanvasHeight - mBackgroundImage.getHeight()) / 2;
			if (mBackgroundStartY < 0)
				mAppOffsetY = -mBackgroundStartY;

			mSecsCenterY = mBackgroundStartY + (bgImageHeight * 6 / 10); //new graphics have height at 60% down image
			mMinsCenterY = mBackgroundStartY + (bgImageHeight * 23 / 50);//mSecsCenterY - 44;
			
			mAppOffsetX = (mCanvasWidth - mBackgroundImage.getWidth()) / 2;

			mSecsCenterX = mCanvasWidth/2;
			mMinsCenterX = mCanvasWidth/2;
		}
	}

	
	/** Handle to the application context, used to e.g. fetch Drawables. */
	private StopwatchThead thread;
	private SurfaceHolder sHolder;
	private Context mContext;
	private SharedPreferences mRestoreState;

	public StopwatchView(Context context, AttributeSet attrs) {
		super(context, attrs);

		Log.d("USW", "New StopwatchView instantiated");

		mContext = context;

		// register our interest in hearing about changes to our surface
		sHolder = getHolder();
		sHolder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		setFocusableInTouchMode(true);
		setFocusable(true);
	}

	/**
	 * Fetches the animation thread corresponding to this LunarView.
	 * 
	 * @return the animation thread
	 */
	public StopwatchThead getThread() {
		return thread;
	}

	public StopwatchThead createNewThread() {
		if(thread==null) thread = new StopwatchThead(sHolder, mContext);
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
		if (!consumed)
			return super.onKeyDown(keyCode, event);
		else
			return consumed;
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		Log.d("USW", "Surface Created");
		try {
			if(thread==null) createNewThread();
			thread.setRunning(true);
			thread.start();
		
			if (mRestoreState != null) {
				thread.restoreState(mRestoreState);
			}
		} catch (Exception e) {
			Log.e("USW", "StopwatchView error", e);
		}
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("USW", "Surface DESTORYED");

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
		thread=null;
	}

	public void restoreState(SharedPreferences savedState) {
		Log.d("USW", "Restore state received");
		mRestoreState = savedState;
	}
}

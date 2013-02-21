package com.geekyouup.android.ustopwatch.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.SoundManager;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;

/**
 * Created with IntelliJ IDEA.
 * User: rhyndman
 * Date: 2/21/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class StopwatchCustomView extends View {

    private boolean mIsStopwatch = true; //true=stopwatch, false=countdown
    private int mBGColor = 0xfff7f7f7;

    private boolean mIsRunning = false;

    private static final String KEY_STATE = "state_bool";
    private static final String KEY_LASTTIME = "lasttime";
    private static final String KEY_NOWTIME = "currenttime";
    private static final String KEY_COUNTDOWN_SUFFIX = "_cd";

    private Bitmap mBackgroundImage;
    private int mBackgroundStartY;
    private int mAppOffsetX = 0;
    private int mAppOffsetY = 0;
    private double mMinsAngle = 0;
    private double mSecsAngle = 0;
    private double mDisplayTimeMillis = 0;
    private final double twoPI = Math.PI * 2.0;
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

    /**
     * Used to figure out elapsed time between frames
     */
    private long mLastTime = 0;
    private Drawable mSecHand;
    private Drawable mMinHand;

    private Handler mHandler;

    public StopwatchCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StopwatchCustomView,
                0, 0);

        try {
            mIsStopwatch = a.getBoolean(R.styleable.StopwatchCustomView_watchType, true);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        Resources res = getResources();
        mBGColor = getResources().getColor(mIsStopwatch ? R.color.stopwatch_background : R.color.countdown_background);

        int minDim = Math.min(mCanvasHeight, mCanvasWidth);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        double handsScaleFactor = 1;

        if (minDim >= 1000) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background1000 : R.drawable.background1000_cd, options);
            handsScaleFactor = 1.388;
        } else if (minDim >= 720) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background720 : R.drawable.background720_cd, options);
            handsScaleFactor = 1;
        } else if (minDim >= 590) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background590 : R.drawable.background590_cd, options);
            handsScaleFactor = 0.82;
        } else if (minDim >= 460) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background460 : R.drawable.background460_cd, options);
            handsScaleFactor = 0.64;
        } else if (minDim >= 320) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background320 : R.drawable.background320_cd, options);
            handsScaleFactor = 0.444;
        } else if (minDim >= 240) {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background240 : R.drawable.background240_cd, options);
            handsScaleFactor = 0.333;
        } else {
            mBackgroundImage = BitmapFactory.decodeResource(res, mIsStopwatch ? R.drawable.background150 : R.drawable.background150_cd, options);
            handsScaleFactor = 0.208;
        }

        mSecHand = res.getDrawable(mIsStopwatch ? R.drawable.sechand : R.drawable.sechand_cd);
        mMinHand = res.getDrawable(mIsStopwatch ? R.drawable.minhand : R.drawable.minhand_cd);

        mSecsHalfWidth = mSecHand.getIntrinsicWidth() / 2;
        mSecsHalfHeight = mSecHand.getIntrinsicHeight() / 2;

        mMinsHalfWidth = mMinHand.getIntrinsicWidth() / 2;
        mMinsHalfHeight = mMinHand.getIntrinsicHeight() / 2;

        mMinsHalfHeight = (int) ((double) mMinsHalfHeight * handsScaleFactor);
        mMinsHalfWidth = (int) ((double) mMinsHalfWidth * handsScaleFactor);
        mSecsHalfHeight = (int) ((double) mSecsHalfHeight * handsScaleFactor);
        mSecsHalfWidth = (int) ((double) mSecsHalfWidth * handsScaleFactor);

        mBackgroundStartY = (mCanvasHeight - mBackgroundImage.getHeight()) / 2;
        mAppOffsetX = (mCanvasWidth - mBackgroundImage.getWidth()) / 2;

        if (mBackgroundStartY < 0)
            mAppOffsetY = -mBackgroundStartY;

        mSecsCenterY = mBackgroundStartY + (mBackgroundImage.getHeight() / 2); //new graphics have watch center in center
        mMinsCenterY = mBackgroundStartY + (mBackgroundImage.getHeight() * 314 / 1000);//mSecsCenterY - 44;
        mSecsCenterX = mCanvasWidth / 2;
        mMinsCenterX = mCanvasWidth / 2;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    private void notifyStateChanged() {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean(UltimateStopwatchActivity.MSG_STATE_CHANGE, true);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    private void notifyCountdownComplete(boolean appResuming)
    {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean(CountdownFragment.MSG_COUNTDOWN_COMPLETE, true);
            b.putBoolean(CountdownFragment.MSG_APP_RESUMING, appResuming);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Account for padding
        int xpad = (getPaddingLeft() + getPaddingRight());
        int ypad = (getPaddingTop() + getPaddingBottom());

        mCanvasWidth = w - xpad;
        mCanvasHeight = h - ypad;
        init();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the background image. Operations on the Canvas accumulate
        //canvas.drawColor(mBGColor);
        if (mBackgroundImage != null)
            canvas.drawBitmap(mBackgroundImage, mAppOffsetX, mBackgroundStartY + mAppOffsetY, null);


        // draw the mins hand with its current rotatiom
        if (mMinHand != null && mSecHand != null) {
            canvas.save();
            canvas.rotate((float) Math.toDegrees(mMinsAngle), mMinsCenterX, mMinsCenterY + mAppOffsetY);
            mMinHand.setBounds(mMinsCenterX - mMinsHalfWidth, mMinsCenterY - mMinsHalfHeight + mAppOffsetY,
                    mMinsCenterX + mMinsHalfWidth, mMinsCenterY + mAppOffsetY + mMinsHalfHeight);
            mMinHand.draw(canvas);
            canvas.restore();

            // Draw the secs hand with its current rotation
            canvas.save();
            canvas.rotate((float) Math.toDegrees(mSecsAngle), mSecsCenterX, mSecsCenterY + mAppOffsetY);
            mSecHand.setBounds(mSecsCenterX - mSecsHalfWidth, mSecsCenterY - mSecsHalfHeight + mAppOffsetY,
                    mSecsCenterX + mSecsHalfWidth, mSecsCenterY + mAppOffsetY + mSecsHalfHeight);
            mSecHand.draw(canvas);
            canvas.restore();
        }
    }

    public void setTime(int hour, int minute, int seconds, boolean start) {
        mIsRunning = false;
        mLastTime = System.currentTimeMillis();
        mMinsAngle = (Math.PI * 2 * ((double) minute / 30.0));
        mSecsAngle = (Math.PI * 2 * ((double) seconds / 60.0));
        mDisplayTimeMillis = hour * 3600000 + minute * 60000 + seconds * 1000;

        if (start) start();
        else updatePhysics(false);
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            updatePhysics(false);
            invalidate();

            if (mIsRunning)
                postDelayed(this, 15);

        }
    };

    /**
     * Update the time
     */
    private void updatePhysics(boolean appResuming) {
        long now = System.currentTimeMillis();

        if (mIsRunning) {
            if (mIsStopwatch)
                mDisplayTimeMillis += (now - mLastTime);
            else
                mDisplayTimeMillis -= (now - mLastTime);
        } else {
            mLastTime = now;
        }

        // mins is 0 to 30
        mMinsAngle = twoPI * (mDisplayTimeMillis / 1800000.0);
        mSecsAngle = twoPI * (mDisplayTimeMillis / 60000.0);

        if (mDisplayTimeMillis < 0) mDisplayTimeMillis = 0;

        // send the time back to the Activity to update the other views
        broadcastClockTime(mIsStopwatch ? mDisplayTimeMillis : -mDisplayTimeMillis);
        mLastTime = now;

        // stop timer at end
        if (mIsRunning && !mIsStopwatch && mDisplayTimeMillis <= 0) {
            reset(); // applies pause state
            notifyCountdownComplete(appResuming);
        }
    }

    private void broadcastClockTime(double mTime) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME, true);
            b.putDouble(UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE, mTime);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    long mTouching = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            SoundManager sm = SoundManager.getInstance(getContext());
            if(sm.isEndlessAlarmSounding())
            {
                sm.stopEndlessAlarm();
            }else
            {
                mTouching = System.currentTimeMillis();
            }
        }else if(event.getAction() == MotionEvent.ACTION_MOVE)
        {
            if(mTouching>0 && System.currentTimeMillis()-mTouching > 1000)
                mTouching=0L;   //reset touch if user is swiping
        }
        else if(event.getAction() == MotionEvent.ACTION_UP)
        {
            if(mTouching>0)  startStop();
            mTouching=0L;
        }
        return true;
    }

    public boolean startStop() {
        if (mIsRunning) {
            stop();
        } else {
            start();
        }

        notifyStateChanged();
        return (mIsRunning);
    }

    /**
     * Resumes from a pause.
     */
   /* public void unpause() {
        // stop timer at end
        if (!mIsStopwatch && mDisplayTimeMillis <= 0) {
            reset(); // applies pause state
        } else {
            // Move the real time clock up to now
            mLastTime = System.currentTimeMillis();
            mMode = STATE_RUNNING;
            removeCallbacks(animator);
            post(animator);
        }
    }*/

    public void start() {
        mLastTime = System.currentTimeMillis();
        mIsRunning = true;
        removeCallbacks(animator);
        post(animator);
    }

    /**
     * Pauses the physics update & animation.
     */
    public void stop() {
        if (mIsRunning){
            mIsRunning = false;
            removeCallbacks(animator);
        }
    }

    public void reset() {
        mIsRunning = false;
        mLastTime = 0;
        mMinsAngle = 0;
        mSecsAngle = 0;
        mDisplayTimeMillis = 0;

        broadcastClockTime(0);
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public double getWatchTime() {
        return mDisplayTimeMillis;
    }

    /**
     * Dump state to the provided Bundle. Typically called when the
     * Activity is being suspended.
     */
    public void saveState(SharedPreferences.Editor map) {
        if (!mIsStopwatch || mDisplayTimeMillis > 0) {
            if (!mIsStopwatch && mDisplayTimeMillis > 0 && mIsRunning) {
                AlarmUpdater.setCountdownAlarm(getContext(), (long) mDisplayTimeMillis);
            } else {
                AlarmUpdater.cancelCountdownAlarm(getContext()); //just to be sure
            }

            map.putBoolean(KEY_STATE + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), mIsRunning);
            map.putLong(KEY_LASTTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), mLastTime);
            map.putLong(KEY_NOWTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), (long) mDisplayTimeMillis);
        } else {
            map.clear();
        }
    }

    /**
     * Restores state from the indicated Bundle. Called when
     * the Activity is being restored after having been previously
     * destroyed.
     */
    public synchronized void restoreState(SharedPreferences savedState) {
        if (savedState != null) {
            mIsRunning = (savedState.getBoolean(KEY_STATE + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), false));
            mLastTime = savedState.getLong(KEY_LASTTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), System.currentTimeMillis());
            mDisplayTimeMillis = savedState.getLong(KEY_NOWTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), 0);
            updatePhysics(true);

            removeCallbacks(animator);
            if(mIsRunning) post(animator);
        }
        notifyStateChanged();
        AlarmUpdater.cancelCountdownAlarm(getContext()); //just to be sure
    }

}

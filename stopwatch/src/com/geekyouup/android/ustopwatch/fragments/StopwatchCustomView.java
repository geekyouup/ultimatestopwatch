package com.geekyouup.android.ustopwatch.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.geekyouup.android.ustopwatch.*;

/**
 * Created with IntelliJ IDEA.
 * User: rhyndman
 * Date: 2/21/13
 * Time: 10:32 AM
 */
public class StopwatchCustomView extends View {

    private boolean mIsStopwatch = true; //true=stopwatch, false=countdown
    private boolean mIsRunning = false;

    private static final String KEY_STATE = "state_bool";
    private static final String KEY_LASTTIME = "lasttime";
    private static final String KEY_NOWTIME = "currenttime_int";
    private static final String KEY_COUNTDOWN_SUFFIX = "_cd";

    private Bitmap mBackgroundImage;
    private int mBackgroundStartY;
    private int mAppOffsetX = 0;
    private int mAppOffsetY = 0;
    private float mMinsAngle = 0;
    private float mSecsAngle = 0;
    private int mDisplayTimeMillis = 0;  //max value is 100hours, 360000000ms
    private final float twoPI = (float) (Math.PI * 2.0);
    private boolean mStopwatchMode = true;
    private long mTouching = 0;

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
    private static final boolean USE_VSYNC = (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);

    /**
     * Used to figure out elapsed time between frames
     */
    private long mLastTime = 0;
    private Drawable mSecHand;
    private Drawable mMinHand;
    //pass back messages to UI thread
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
        //the stopwatch graphics are square, so find the smallest dimension they must fit in and load appropriately
        int minDim = Math.min(mCanvasHeight, mCanvasWidth);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        double handsScaleFactor;

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
        // Draw the background image
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

    //set the time on the stopwatch/countdown face, animating the hands if resettings countdown
    //To make the animation feel right, we always wind backwards when resetting
    public void setTime(int hours, int minutes, int seconds, boolean resetting) {
        mIsRunning = false;
        mLastTime = System.currentTimeMillis();
        if (SettingsActivity.isAnimating()) {
            animateWatchTo(hours, minutes, seconds, resetting);
        } else {
            mDisplayTimeMillis = hours * 3600000 + minutes * 60000 + seconds * 1000;
            mMinsAngle = (twoPI * ((float) minutes / 30.0f));
            mSecsAngle = (twoPI * ((float) seconds / 60.0f));
            broadcastClockTime(mDisplayTimeMillis);
            updatePhysics(false);
        }
    }

    private void animateWatchTo(final int hours, final int minutes, final int seconds, boolean resetting) {

        mSecsAngle = mSecsAngle % twoPI; //avoids more than 1 rotation
        mMinsAngle = mMinsAngle % twoPI; //avoids more than 1 rotation

        //forces hands to go back to 0 not forwards
        final float toSecsAngle = shortestAngleToDestination(mSecsAngle, twoPI * seconds / 60f, resetting);
        //avoid multiple minutes hands rotates as face is 0-29 not 0-59
        final float toMinsAngle = shortestAngleToDestination(mMinsAngle, twoPI * ((minutes > 30 ? minutes - 30 : minutes) / 30f + seconds / 1800f), resetting);

        float maxAngleChange = Math.max(Math.abs(mSecsAngle - toSecsAngle), Math.abs(toMinsAngle - mMinsAngle));
        int duration;
        if (maxAngleChange < Math.PI / 2) duration = 300;
        else if (maxAngleChange < Math.PI) duration = 750;
        else duration = 1250;

        final ValueAnimator secsAnimation = ValueAnimator.ofFloat(mSecsAngle, toSecsAngle);
        secsAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        secsAnimation.setDuration(duration);
        secsAnimation.start();

        final ValueAnimator minsAnimation = ValueAnimator.ofFloat(mMinsAngle, toMinsAngle);
        minsAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        minsAnimation.setDuration(duration);
        minsAnimation.start();

        final ValueAnimator clockAnimation = ValueAnimator.ofInt(mDisplayTimeMillis, (hours * 3600000 + minutes * 60000 + seconds * 1000));
        clockAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        clockAnimation.setDuration(duration);
        clockAnimation.start();

        //approach is to go from xMs to yMs using the standard updatePhysics routine and speeding up time.
        post(new Runnable() {
            @Override
            public void run() {

                if (secsAnimation.isRunning() || minsAnimation.isRunning() || clockAnimation.isRunning()) {
                    mSecsAngle = (Float) secsAnimation.getAnimatedValue();
                    mMinsAngle = (Float) minsAnimation.getAnimatedValue();
                    broadcastClockTime(mIsStopwatch ? (Integer) clockAnimation.getAnimatedValue() : -(Integer) clockAnimation.getAnimatedValue());
                    invalidate();
                    postDelayed(this, 15);
                } else {
                    mSecsAngle = toSecsAngle; //ensure the hands have ended at correct position
                    mMinsAngle = toMinsAngle;
                    mDisplayTimeMillis = hours * 3600000 + minutes * 60000 + seconds * 1000;
                    broadcastClockTime(mIsStopwatch ? mDisplayTimeMillis : -mDisplayTimeMillis);
                    invalidate();
                }

            }
        });
    }

    //To get from -6 rads to 1 rads, shortest distance is clockwise through 0 rads
    //From 1 rads to 5 rads shortest distance is CCW back through 0 rads
    //This method returns the angle in rads closest to fromAngle that is equivalent to toAngle
    //unless we are animating a reset, as it feels better to always reset by reversing the hand direction
    //e.g. toAngle+2*Pi may be closer than toAngle
    private float shortestAngleToDestination(final float fromAngle, final float toAngle, boolean resetting) {
        if (resetting && mIsStopwatch) // hands must always go backwards
        {
            return toAngle; // stopwatch reset always returns to 0,
        } else if (resetting && !mIsStopwatch) //hands must always go forwards
        {
            //countdown reset can be to any clock position, ensure CW rotation
            if (toAngle > fromAngle) return toAngle;
            else return (toAngle + twoPI);
        } else //not restting hands must take shortest route
        {
            float absFromMinusTo = Math.abs(fromAngle - toAngle);
            //toAngle-twoPi, toAngle, toAngle+twoPi
            if (absFromMinusTo < Math.abs(fromAngle - (toAngle + twoPI))) {
                if (Math.abs(fromAngle - (toAngle - twoPI)) < absFromMinusTo) {
                    return (toAngle - twoPI);
                } else {
                    return toAngle;
                }
            } else return toAngle + twoPI;
        }
    }

    //Stopwatch and countdown animation runnable
    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            updatePhysics(false);

            if(USE_VSYNC) postInvalidateOnAnimation();
            else invalidate();

            if (mIsRunning) postDelayed(this, 15);
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
        mMinsAngle = twoPI * (mDisplayTimeMillis / 1800000.0f);
        mSecsAngle = twoPI * mDisplayTimeMillis / 60000.0f;

        if (mDisplayTimeMillis < 0) mDisplayTimeMillis = 0;

        // send the time back to the Activity to update the other views
        broadcastClockTime(mIsStopwatch ? mDisplayTimeMillis : -mDisplayTimeMillis);
        mLastTime = now;

        // stop timer at end
        if (mIsRunning && !mIsStopwatch && mDisplayTimeMillis <= 0) {
            notifyCountdownComplete(appResuming);
        }
    }

    // Deal with touch events, either start/stop or swipe
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            SoundManager sm = SoundManager.getInstance(getContext());
            if (sm.isEndlessAlarmSounding()) {
                sm.stopEndlessAlarm();
            } else {
                mTouching = System.currentTimeMillis();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mTouching > 0 && System.currentTimeMillis() - mTouching > 1000)
                mTouching = 0L;   //reset touch if user is swiping
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mTouching > 0) startStop();
            mTouching = 0L;
        }
        return true;
    }

    public boolean startStop() {
        if (mIsRunning) {
            stop();
            notifyStateChanged();
        } else if (mIsStopwatch || mDisplayTimeMillis != 0) { // don't start the countdown if it is 0
            start();
            notifyStateChanged();
        } else { //mDisplayTimeMillis == 0
            notifyIconHint();
            return false;
        }
        return (mIsRunning);
    }

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
        mIsRunning = false;
        removeCallbacks(animator);
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
            map.putInt(KEY_NOWTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), mDisplayTimeMillis);
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
            mDisplayTimeMillis = savedState.getInt(KEY_NOWTIME + (mStopwatchMode ? "" : KEY_COUNTDOWN_SUFFIX), 0);
            updatePhysics(true);

            removeCallbacks(animator);
            if (mIsRunning) post(animator);
        }
        notifyStateChanged();
        AlarmUpdater.cancelCountdownAlarm(getContext()); //just to be sure
    }

    //for optimization purposes
    @Override
    public boolean isOpaque() {
        return true;
    }

    //Message Handling between Activity/Fragment and View
    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    private void notifyStateChanged() {
        Bundle b = new Bundle();
        b.putBoolean(UltimateStopwatchActivity.MSG_STATE_CHANGE, true);
        sendMessageToHandler(b);
    }

    private void notifyIconHint() {
        Bundle b = new Bundle();
        b.putBoolean(CountdownFragment.MSG_REQUEST_ICON_FLASH, true);
        sendMessageToHandler(b);
    }

    private void notifyCountdownComplete(boolean appResuming) {
        Bundle b = new Bundle();
        b.putBoolean(CountdownFragment.MSG_COUNTDOWN_COMPLETE, true);
        b.putBoolean(CountdownFragment.MSG_APP_RESUMING, appResuming);
        sendMessageToHandler(b);
    }

    //send the latest time to the parent fragment to populate the digits
    private void broadcastClockTime(double mTime) {
        Bundle b = new Bundle();
        b.putBoolean(UltimateStopwatchActivity.MSG_UPDATE_COUNTER_TIME, true);
        b.putDouble(UltimateStopwatchActivity.MSG_NEW_TIME_DOUBLE, mTime);
        sendMessageToHandler(b);
    }

    private void sendMessageToHandler(Bundle b) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }


}

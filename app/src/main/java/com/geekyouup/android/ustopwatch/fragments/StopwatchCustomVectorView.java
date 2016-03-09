package com.geekyouup.android.ustopwatch.fragments;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.geekyouup.android.ustopwatch.AlarmUpdater;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.SettingsActivity;
import com.geekyouup.android.ustopwatch.SoundManager;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;

public class StopwatchCustomVectorView extends View {

    private boolean mIsStopwatch = true; //true=stopwatch, false=countdown
    private boolean mIsRunning = false;

    private static final String KEY_STATE = "state_bool";
    private static final String KEY_LASTTIME = "lasttime";
    private static final String KEY_NOWTIME = "currenttime_int";
    private static final String KEY_COUNTDOWN_SUFFIX = "_cd";

    private float mMinsAngle = 0;
    private float mSecsAngle = 0;
    private int mDisplayTimeMillis = 0;  //max value is 100hours, 360000000ms
    private final float twoPI = (float) (Math.PI * 2.0);
    private boolean mStopwatchMode = true;
    private long mTouching = 0;

    private int mCanvasWidth = 320;
    private int mCanvasHeight = 480;
    private int mWatchfaceCenterX = 156;
    private int mWatchfaceCenterY = 230;
    private int mMinsCenterY = 185;

    //the paint styles
    private Paint mPrimaryDashPaint;
    private Paint mSecondaryDashPaint;
    private Paint mTertiaryDashPaint;
    private Paint mClockNumeralsPaint;
    private Paint mPatternCircleSolidPaint;
    private Paint mPatternCircleDashPaint;
    private Paint m100thsPrimaryDashPaint;
    private Paint m100thsSecondaryDashPaint;
    private Paint m100thsNumeralsPaint;
    private Paint mMinsCirclePaint;
    private Paint mMinsNumeralsPaint;
    private Paint mWatchhandsPaint;
    //private Paint mLogoTextPaint;
    //private Path mLogoTextPath;

    //bitmaps
    private Bitmap mWatchBackground;
    private float mWatchBMPMarginTop = 0;
    private float mWatchBMPMarginLeft = 0;

    //metrics
    private static final int FULL_CANVAS_SIZE = 1000; //basing the custom view on a 1000x1000px canvas and scaling as needed
    private static final int FULL_WATCHFACE_OUTER_RADIUS = 400;
    private static final float FULL_PRIMARY_DASH_WIDTH = 3;
    private static final float FULL_SECONDARY_DASH_WIDTH = 2;
    private static final float FULL_60TH_1_DASH_HEIGHT = 50;
    private static final float FULL_60TH_2_DASH_HEIGHT = 50;
    private static final float FULL_60TH_3_DASH_HEIGHT = 40;
    private static final float FULL_PATTERN_CIRCLE_RADIUS = FULL_WATCHFACE_OUTER_RADIUS - FULL_60TH_1_DASH_HEIGHT - 18;
    private static final float FULL_PATTERN_CIRCLE_SOLID_THICKNESS = 18;
    private static final float FULL_PATTERN_CIRCLE_DASH_THICKNESS = 12;
    private static final float FULL_100TH_OUTER_RADIUS = FULL_PATTERN_CIRCLE_RADIUS - 30;
    private static final float FULL_100TH_OUTER_DIAMETER = FULL_100TH_OUTER_RADIUS * 2;
    private static final float FULL_100th_1_DASH_HEIGHT = 40;
    private static final float FULL_100th_2_DASH_HEIGHT = 20;
    private static final float FULL_60th_NUMERALS_SIZE = 50;
    private static final float FULL_60TH_NUMERALS_RADIUS = FULL_WATCHFACE_OUTER_RADIUS + FULL_60th_NUMERALS_SIZE - 10; //-10 for kerning
    private static final float FULL_100th_NUMERALS_SIZE = 30;
    private static final float FULL_100TH_NUMERALS_RADIUS = FULL_100TH_OUTER_RADIUS - FULL_100th_1_DASH_HEIGHT - FULL_100th_NUMERALS_SIZE;
    private static final float FULL_NAMEPLATE_Y = 600;
    private static final float FULL_NAMEPLATE_X = 451;
    private static final float FULL_NAMEPLATE_WIDTH = 98;//114;
    private static final float FULL_NAMEPLATE_HEIGHT = 116;//135;
    private static final float FULL_MINS_CENTER_Y = 345;
    private static final float FULL_MINS_CIRCLE_RADIUS = 94;
    private static final float FULL_MINS_DASH_HEIGHT = 10;
    private static final float FULL_MINS_NUMERALS_RADIUS = FULL_MINS_CIRCLE_RADIUS - FULL_MINS_DASH_HEIGHT * 2.5f;
    private static final float FULL_MINS_NUMERALS_SIZE = 20;
    private static final float FULL_SECHAND_HALFBASEWIDTH = 10;
    private static final float FULL_MINHAND_HALFBASEWIDTH = 5;
    private float mScaleFactor = 0;

    //colors
    private final int COLOR_BACKGROUND;
    private final int COLOR_HANDS;
    private final int COLOR_60TH_NUMERALS;
    private final int COLOR_100TH_NUMERALS;
    private final int COLOR_60TH_PRI_DASH;
    private final int COLOR_60TH_SEC_DASH;
    private final int COLOR_100TH_PRI_DASH;
    private final int COLOR_100TH_SEC_DASH;
    private final int COLOR_MINS;
    private final int COLOR_PATTERN_CIRCLE_SOLID;
    private final int COLOR_PATTERN_CIRCLE_DASHED;

    //arrays of verticies for the watch hands
    private float[] mSecHandVerticies;
    private float[] mMinHandVerticies;

    //Used to figure out elapsed time between frames
    private long mLastTime = 0;
    //pass back messages to UI thread
    private Handler mHandler;

    public static final boolean IS_HONEYCOMB_OR_ABOVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    public StopwatchCustomVectorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //find out if this view is specificed as a stopwatch or countdown view
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

        Resources res = getResources();

        //for Lollipop+ add in the Ripple effect on touch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setBackground(res.getDrawable(mIsStopwatch ? R.drawable.ripple_touch : R.drawable.ripple_touch_countdown, context.getTheme()));
            setClickable(true);
        }

        COLOR_BACKGROUND = res.getColor(mIsStopwatch ? R.color.stopwatch_background : R.color.countdown_background);
        COLOR_HANDS = mIsStopwatch ? Color.BLACK : Color.WHITE;
        COLOR_60TH_NUMERALS = mIsStopwatch ? Color.rgb(52, 52, 52) : Color.rgb(238, 238, 238);
        COLOR_100TH_NUMERALS = mIsStopwatch ? Color.rgb(193, 103, 91) : Color.rgb(0, 172, 228);
        COLOR_MINS = mIsStopwatch ? Color.rgb(85, 85, 85) : Color.rgb(85, 85, 85);

        COLOR_60TH_PRI_DASH = mIsStopwatch ? Color.rgb(52, 52, 52) : Color.WHITE;
        COLOR_60TH_SEC_DASH = mIsStopwatch ? Color.rgb(197, 197, 197) : Color.rgb(63, 63, 63);
        COLOR_100TH_PRI_DASH = mIsStopwatch ? Color.rgb(193, 103, 91) : Color.rgb(0, 172, 228);
        COLOR_100TH_SEC_DASH = mIsStopwatch ? Color.rgb(232, 205, 200) : Color.rgb(96, 96, 96);

        COLOR_PATTERN_CIRCLE_SOLID = mIsStopwatch ? Color.rgb(52, 52, 52) : Color.rgb(112, 112, 112);
        COLOR_PATTERN_CIRCLE_DASHED = COLOR_BACKGROUND;

    }

    private void init() {
        Resources res = getResources();
        //the stopwatch graphics are square, so find the smallest dimension they must fit in and load appropriately
        float minDim = Math.min(mCanvasHeight, mCanvasWidth);
        mScaleFactor = minDim / FULL_CANVAS_SIZE;

        mWatchBMPMarginTop = (mCanvasHeight - minDim) / 2;
        mWatchBMPMarginLeft = (mCanvasWidth - minDim) / 2;

        float watchfaceOuterRadius = FULL_WATCHFACE_OUTER_RADIUS * mScaleFactor;
        double mClockDiameter = watchfaceOuterRadius * 2;

        mWatchfaceCenterX = mCanvasWidth / 2;
        mWatchfaceCenterY = mCanvasHeight / 2;
        mMinsCenterY = (int) ((FULL_MINS_CENTER_Y * mScaleFactor) + mWatchBMPMarginTop);

        float primaryDashWidth = FULL_PRIMARY_DASH_WIDTH * mScaleFactor;
        float secondaryDashWidth = FULL_SECONDARY_DASH_WIDTH * mScaleFactor;

        mPrimaryDashPaint = new Paint();
        mPrimaryDashPaint.setColor(COLOR_60TH_PRI_DASH);
        mPrimaryDashPaint.setStrokeWidth(primaryDashWidth);
        mPrimaryDashPaint.setAntiAlias(true);

        mSecondaryDashPaint = new Paint();
        mSecondaryDashPaint.setColor(COLOR_60TH_SEC_DASH);
        mSecondaryDashPaint.setStrokeWidth(primaryDashWidth);
        mSecondaryDashPaint.setAntiAlias(true);

        mTertiaryDashPaint = new Paint();
        mTertiaryDashPaint.setColor(COLOR_60TH_SEC_DASH);
        mTertiaryDashPaint.setStrokeWidth(secondaryDashWidth);
        mTertiaryDashPaint.setAntiAlias(true);

        mClockNumeralsPaint = new Paint();
        mClockNumeralsPaint.setColor(COLOR_60TH_NUMERALS);
        mClockNumeralsPaint.setTextSize(FULL_60th_NUMERALS_SIZE * mScaleFactor);
        mClockNumeralsPaint.setAntiAlias(true);
        mClockNumeralsPaint.setTextAlign(Paint.Align.CENTER);

        mPatternCircleSolidPaint = new Paint();
        mPatternCircleSolidPaint.setColor(COLOR_PATTERN_CIRCLE_SOLID);
        mPatternCircleSolidPaint.setStrokeWidth(FULL_PATTERN_CIRCLE_SOLID_THICKNESS * mScaleFactor);
        mPatternCircleSolidPaint.setStyle(Paint.Style.STROKE);
        mPatternCircleSolidPaint.setAntiAlias(true);

        mPatternCircleDashPaint = new Paint();
        mPatternCircleDashPaint.setColor(COLOR_PATTERN_CIRCLE_DASHED);
        mPatternCircleDashPaint.setStrokeWidth(FULL_PATTERN_CIRCLE_DASH_THICKNESS * mScaleFactor);
        mPatternCircleDashPaint.setStyle(Paint.Style.STROKE);
        mPatternCircleDashPaint.setAntiAlias(true);
        mPatternCircleDashPaint.setPathEffect(new DashPathEffect(new float[]{20 * mScaleFactor, 30 * mScaleFactor}, 0));

        m100thsPrimaryDashPaint = new Paint();
        m100thsPrimaryDashPaint.setColor(COLOR_100TH_PRI_DASH);
        m100thsPrimaryDashPaint.setStrokeWidth(primaryDashWidth);
        m100thsPrimaryDashPaint.setAntiAlias(true);

        m100thsSecondaryDashPaint = new Paint();
        m100thsSecondaryDashPaint.setColor(COLOR_100TH_SEC_DASH);
        m100thsSecondaryDashPaint.setStrokeWidth(secondaryDashWidth);
        m100thsSecondaryDashPaint.setAntiAlias(true);

        m100thsNumeralsPaint = new Paint();
        m100thsNumeralsPaint.setColor(COLOR_100TH_NUMERALS);
        m100thsNumeralsPaint.setTextSize(FULL_100th_NUMERALS_SIZE * mScaleFactor);
        m100thsNumeralsPaint.setAntiAlias(true);
        m100thsNumeralsPaint.setTextAlign(Paint.Align.CENTER);

        mMinsCirclePaint = new Paint();
        mMinsCirclePaint.setColor(COLOR_MINS);
        mMinsCirclePaint.setStrokeWidth(2);
        mMinsCirclePaint.setStyle(Paint.Style.STROKE);
        mMinsCirclePaint.setAntiAlias(true);

        mMinsNumeralsPaint = new Paint();
        mMinsNumeralsPaint.setColor(COLOR_MINS);
        mMinsNumeralsPaint.setTextSize(FULL_MINS_NUMERALS_SIZE * mScaleFactor);
        mMinsNumeralsPaint.setAntiAlias(true);
        mMinsNumeralsPaint.setTextAlign(Paint.Align.CENTER);

        mWatchhandsPaint = new Paint();
        mWatchhandsPaint.setColor(COLOR_HANDS);
        mWatchhandsPaint.setAntiAlias(true);

        /*mLogoTextPaint = new Paint();
        mLogoTextPaint.setColor(COLOR_60TH_NUMERALS);
        mLogoTextPaint.setTextSize(FULL_100th_NUMERALS_SIZE * mScaleFactor);
        mLogoTextPaint.setTextAlign(Paint.Align.CENTER);*/

        mSecHandVerticies = new float[6];
        mSecHandVerticies[0] = mWatchfaceCenterX - FULL_SECHAND_HALFBASEWIDTH * mScaleFactor;
        mSecHandVerticies[1] = mWatchfaceCenterY;
        mSecHandVerticies[2] = mWatchfaceCenterX + FULL_SECHAND_HALFBASEWIDTH * mScaleFactor;
        mSecHandVerticies[3] = mWatchfaceCenterY;
        mSecHandVerticies[4] = mWatchfaceCenterX;
        mSecHandVerticies[5] = mWatchfaceCenterY - (FULL_WATCHFACE_OUTER_RADIUS - FULL_60TH_3_DASH_HEIGHT) * mScaleFactor;

        mMinHandVerticies = new float[6];
        mMinHandVerticies[0] = mWatchfaceCenterX - FULL_MINHAND_HALFBASEWIDTH * mScaleFactor;
        mMinHandVerticies[1] = mMinsCenterY;
        mMinHandVerticies[2] = mWatchfaceCenterX + FULL_MINHAND_HALFBASEWIDTH * mScaleFactor;
        mMinHandVerticies[3] = mMinsCenterY;
        mMinHandVerticies[4] = mWatchfaceCenterX;
        mMinHandVerticies[5] = mMinsCenterY - (FULL_MINS_CIRCLE_RADIUS) * mScaleFactor;

        //draw the watchface
        mWatchBackground = Bitmap.createBitmap((int) minDim, (int) minDim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(mWatchBackground);
        drawWatchface(canvas);
    }

    //assumed that the incoming canvas is square
    private void drawWatchface(Canvas canvas) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        float canvasCenter = canvasWidth / 2.0f;
        float watchfaceOuterRadius = FULL_WATCHFACE_OUTER_RADIUS * mScaleFactor;
        float watchfaceDiameter = watchfaceOuterRadius * 2.0f;
        float pri60thDashHeight = FULL_60TH_1_DASH_HEIGHT * mScaleFactor;
        float sec60thDashHeight = FULL_60TH_2_DASH_HEIGHT * mScaleFactor;
        float ter60thDashHeight = FULL_60TH_3_DASH_HEIGHT * mScaleFactor;
        float yOffset60thCircle = (canvasHeight - watchfaceDiameter) / 2.0f + pri60thDashHeight;
        float yOffset100thCircle = (canvasHeight - (FULL_100TH_OUTER_DIAMETER * mScaleFactor)) / 2.0f;
        float pri100thDashHeight = FULL_100th_1_DASH_HEIGHT * mScaleFactor;
        float sec100thDashHeight = FULL_100th_2_DASH_HEIGHT * mScaleFactor;
        float patternCircleRadius = FULL_PATTERN_CIRCLE_RADIUS * mScaleFactor;
        float text60thHalfHeight = FULL_60th_NUMERALS_SIZE * mScaleFactor / 3; //one third height lines up better
        float text60thRadius = FULL_60TH_NUMERALS_RADIUS * mScaleFactor;
        float text100thRadius = FULL_100TH_NUMERALS_RADIUS * mScaleFactor;
        float text100thHalfHeight = FULL_100th_NUMERALS_SIZE * mScaleFactor / 2;

        //minutes
        float minsCircleRadius = FULL_MINS_CIRCLE_RADIUS * mScaleFactor;
        float minsCenterY = FULL_MINS_CENTER_Y * mScaleFactor;
        float minsDashHeight = FULL_MINS_DASH_HEIGHT * mScaleFactor;
        float textMinsRadius = FULL_MINS_NUMERALS_RADIUS * mScaleFactor;
        float textMinsHalfHeigh = FULL_MINS_NUMERALS_SIZE * mScaleFactor / 3;

        /*
        float logoTextY = FULL_NAMEPLATE_Y*mScaleFactor;
        mLogoTextPath = new Path();
        mLogoTextPath.moveTo(canvasCenter / 2, logoTextY);
        mLogoTextPath.cubicTo(canvasCenter / 2, logoTextY, canvasCenter, logoTextY + watchfaceOuterRadius / 2, 3 * canvasCenter / 2, logoTextY);

        //Draw the Ultimate Stopwatch Text on a curve
        canvas.drawTextOnPath("Ultimate Stopwatch",mLogoTextPath,0,0,mLogoTextPaint);
        */

        //draw the logo
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = (int) (FULL_NAMEPLATE_HEIGHT * mScaleFactor);
        options.outWidth = (int) (FULL_NAMEPLATE_WIDTH * mScaleFactor);

        Bitmap watchLogo = BitmapFactory.decodeResource(getResources(), R.drawable.logo, null);
        watchLogo = Bitmap.createScaledBitmap(watchLogo, (int) (FULL_NAMEPLATE_WIDTH * mScaleFactor), (int) (FULL_NAMEPLATE_HEIGHT * mScaleFactor), true);

        canvas.drawBitmap(watchLogo, FULL_NAMEPLATE_X * mScaleFactor, FULL_NAMEPLATE_Y * mScaleFactor, null);

        //draw the minutes
        canvas.drawCircle(canvasCenter, minsCenterY, minsCircleRadius, mMinsCirclePaint);
        //draw the minute ticks
        canvas.save();
        for (int a = 0; a < 360; a += 12) {
            //draw the 60ths etc.. lines
            canvas.drawLine(canvasCenter, minsCenterY - minsCircleRadius, canvasCenter, minsCenterY - minsCircleRadius + minsDashHeight, mMinsCirclePaint);
            canvas.rotate(12, canvasCenter, minsCenterY);
        }
        canvas.restore();

        //draw the minutes numerals
        for (int a = 0; a < 6; a++) {
            double xPos = canvasCenter + Math.cos(Math.toRadians(a * 60 - 30)) * textMinsRadius;
            double yPos = minsCenterY + Math.sin(Math.toRadians(a * 60 - 30)) * textMinsRadius + textMinsHalfHeigh;
            int numeral = (a + 1) * 5;
            canvas.drawText((numeral < 10 ? " " : "") + numeral, (float) xPos, (float) yPos, mMinsNumeralsPaint);
        }

        //draw the clock face
        canvas.save();
        for (int a = 0; a < 360; a += 2) {
            //draw the 60ths etc.. lines
            if (a % 30 == 0) {//+60,+70,+90
                canvas.drawLine(canvasCenter, yOffset60thCircle - pri60thDashHeight, canvasCenter, yOffset60thCircle, mPrimaryDashPaint);
            } else if (a % 6 == 0) {
                canvas.drawLine(canvasCenter, yOffset60thCircle - sec60thDashHeight, canvasCenter, yOffset60thCircle, mSecondaryDashPaint);
            } else if (a % 2 == 0) {
                canvas.drawLine(canvasCenter, yOffset60thCircle - ter60thDashHeight, canvasCenter, yOffset60thCircle, mTertiaryDashPaint);
            }

            canvas.rotate(2, canvasCenter, canvasCenter);
        }
        canvas.restore();

        //draw the black circle
        canvas.drawCircle(canvasCenter, canvasCenter, patternCircleRadius, mPatternCircleSolidPaint);
        canvas.drawCircle(canvasCenter, canvasCenter, patternCircleRadius, mPatternCircleDashPaint);

        //draw the 100ths lines
        canvas.save();
        for (int a = 0; a < 200; a++) {
            if (a % 10 == 0) {
                canvas.drawLine(canvasCenter, yOffset100thCircle, canvasCenter, yOffset100thCircle + pri100thDashHeight, m100thsPrimaryDashPaint);
            } else if (a % 2 == 0) {
                canvas.drawLine(canvasCenter, yOffset100thCircle, canvasCenter, yOffset100thCircle + sec100thDashHeight, m100thsPrimaryDashPaint);
            } else {
                canvas.drawLine(canvasCenter, yOffset100thCircle, canvasCenter, yOffset100thCircle + sec100thDashHeight, m100thsSecondaryDashPaint);
            }
            canvas.rotate(1.8f, canvasCenter, canvasCenter);//200ths
        }
        canvas.restore();

        //draw the 60ths numerals
        for (int a = 0; a < 360; a += 30) {
            double xPos = canvasCenter + Math.cos(Math.toRadians(a)) * text60thRadius;
            double yPos = canvasCenter + Math.sin(Math.toRadians(a)) * text60thRadius + text60thHalfHeight;
            int numeral = ((a / 6 + 15) % 60);
            if (numeral == 0) numeral = 60;
            canvas.drawText((numeral < 10 ? " " : "") + numeral, (float) xPos, (float) yPos, mClockNumeralsPaint);
        }

        //draw the 100ths numerals
        for (int a = 0; a < 10; a++) {
            if (a == 2 || a == 7) continue; //no text at 0 or 50

            double xPos = canvasCenter + Math.cos(Math.toRadians(a * 36 + 18)) * text100thRadius;
            double yPos = canvasCenter + Math.sin(Math.toRadians(a * 36 + 18)) * text100thRadius + text100thHalfHeight;
            String numeral = (a * 10 + 30) % 100 + "";
            canvas.drawText(numeral, (float) xPos, (float) yPos, m100thsNumeralsPaint);
        }

    }

    private void drawFilledTriangle(Canvas canvas, float[] verticies, Paint p) {
        Path path = new Path();
        path.moveTo(verticies[0], verticies[1]);
        path.lineTo(verticies[2], verticies[3]);
        path.lineTo(verticies[4], verticies[5]);
        canvas.drawPath(path, p);
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

        //draw the watch background
        canvas.drawBitmap(mWatchBackground, mWatchBMPMarginLeft, mWatchBMPMarginTop, null);

        // draw the mins hands with its current rotatiom
        canvas.save();
        canvas.rotate((float) Math.toDegrees(mMinsAngle), mWatchfaceCenterX, mMinsCenterY);
        drawFilledTriangle(canvas, mMinHandVerticies, mWatchhandsPaint);
        canvas.restore();
        canvas.drawCircle(mWatchfaceCenterX, mMinsCenterY, FULL_MINHAND_HALFBASEWIDTH * 2 * mScaleFactor, mWatchhandsPaint);

        // Draw the secs hand with its current rotation
        canvas.save();
        canvas.rotate((float) Math.toDegrees(mSecsAngle), mWatchfaceCenterX, mWatchfaceCenterY);
        drawFilledTriangle(canvas, mSecHandVerticies, mWatchhandsPaint);
        canvas.restore();
        canvas.drawCircle(mWatchfaceCenterX, mWatchfaceCenterY, FULL_SECHAND_HALFBASEWIDTH * 2 * mScaleFactor, mWatchhandsPaint);
    }

    //set the time on the stopwatch/countdown face, animating the hands if resettings countdown
    //To make the animation feel right, we always wind backwards when resetting
    public void setTime(final int hours, final int minutes, final int seconds, boolean resetting) {
        mIsRunning = false;
        mLastTime = System.currentTimeMillis();
        if (SettingsActivity.isAnimating() && IS_HONEYCOMB_OR_ABOVE) {
            animateWatchToAPI11(hours, minutes, seconds, resetting);
        } else {
            //to fix bug #42, now the hands reset even when paused
            removeCallbacks(animator);
            post(new Runnable() {
                @Override
                public void run() {
                    //during the animation also roll back the clock time to the current hand times.
                    mSecsAngle = (twoPI * ((float) seconds / 60.0f)); //ensure the hands have ended at correct position
                    mMinsAngle = (twoPI * ((float) minutes / 30.0f));
                    mDisplayTimeMillis = hours * 3600000 + minutes * 60000 + seconds * 1000;
                    broadcastClockTime(mIsStopwatch ? mDisplayTimeMillis : -mDisplayTimeMillis);
                    invalidate();
                }
            });
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animateWatchToAPI11(final int hours, final int minutes, final int seconds, boolean resetting) {

        mSecsAngle = mSecsAngle % twoPI; //avoids more than 1 rotation
        mMinsAngle = mMinsAngle % twoPI; //avoids more than 1 rotation

        //forces hands to go back to 0 not forwards
        final float toSecsAngle = shortestAngleToDestination(mSecsAngle, twoPI * seconds / 60f, resetting);
        //avoid multiple minutes hands rotates as face is 0-29 not 0-59
        final float toMinsAngle = shortestAngleToDestination(mMinsAngle, twoPI * ((minutes > 30 ? minutes - 30 : minutes) / 30f + seconds / 1800f), resetting);

        float maxAngleChange = Math.max(Math.abs(mSecsAngle - toSecsAngle), Math.abs(toMinsAngle - mMinsAngle));
        long duration = (long) (maxAngleChange / twoPI * 1000) + 250;

        FastOutSlowInInterpolator fosiInterp = new FastOutSlowInInterpolator();
        final ValueAnimator secsAnimation = ValueAnimator.ofFloat(mSecsAngle, toSecsAngle);
        secsAnimation.setInterpolator(fosiInterp);
        secsAnimation.setDuration(duration);
        secsAnimation.start();

        final ValueAnimator minsAnimation = ValueAnimator.ofFloat(mMinsAngle, toMinsAngle);
        minsAnimation.setInterpolator(fosiInterp);
        minsAnimation.setDuration(duration);
        minsAnimation.start();

        final ValueAnimator clockAnimation = ValueAnimator.ofInt(mDisplayTimeMillis, (hours * 3600000 + minutes * 60000 + seconds * 1000));
        clockAnimation.setInterpolator(fosiInterp);
        clockAnimation.setDuration(duration);
        clockAnimation.start();

        //approach is to go from xMs to yMs
        removeCallbacks(animator);
        post(new Runnable() {
            @Override
            public void run() {

                //during the animation also roll back the clock time to the current hand times.
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

    //This method returns the angle in rads closest to fromAngle that is equivalent to toAngle
    //unless we are animating a reset, as it feels better to always reset by reversing the hand direction
    //e.g. toAngle+2*Pi may be closer than toAngle
    //To get from -6 rads to 1 rads, shortest distance is clockwise through 0 rads
    //From 1 rads to 5 rads shortest distance is CCW back through 0 rads
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
    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            updateWatchState(false);

            if (mIsRunning) {
                invalidate();
                removeCallbacks(this);
                ViewCompat.postOnAnimation(StopwatchCustomVectorView.this, this);
            }
        }
    };

    /**
     * Update the time
     */
    private void updateWatchState(boolean appResuming) {
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
        super.onTouchEvent(event);

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
            if (mTouching > 0) {
                startStop();
            }
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
            notifyRequestTimePicker();
            return false;
        }
        return (mIsRunning);
    }

    private void start() {
        mLastTime = System.currentTimeMillis();
        mIsRunning = true;

        //vibrate
        if (SettingsActivity.isVibrate()) {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(20);
        }

        removeCallbacks(animator);
        post(animator);
    }

    protected void stop() {
        mIsRunning = false;

        //vibrate
        if (SettingsActivity.isVibrate()) {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(20);
        }

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
            updateWatchState(true);

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

    private void notifyRequestTimePicker() {
        Bundle b = new Bundle();
        b.putBoolean(CountdownFragment.MSG_REQUEST_TIME_PICKER, true);
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

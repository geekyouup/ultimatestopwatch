package com.geekyouup.android.ustopwatch;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class AlarmUpdater {
	
	public static void cancelCountdownAlarm(Context context)
	{
		try
		{
			AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			
			Intent defineIntent = new Intent(context,UpdateService.class);
			defineIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent piWakeUp = PendingIntent.getService(context,0, defineIntent, PendingIntent.FLAG_NO_CREATE);
	        
			if(piWakeUp != null) alarmMan.cancel(piWakeUp);
		}catch(Exception e){}
        
		try
		{
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(R.layout.main);
		}catch(Exception e){}
	}
	//cancels alarm then sets new one
	public static void setCountdownAlarm(Context context, long inMillis)
	{
		AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		Intent defineIntent = new Intent(context,UpdateService.class);
		defineIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent piWakeUp = PendingIntent.getService(context,0, defineIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //alarmMan.cancel(piWakeUp);
        
		if(inMillis != -1) alarmMan.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+inMillis, piWakeUp);
	}
	
    public static class UpdateService extends Service {
    	
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            // Build the widget update for today
			//no need for a screen, this just has to refresh all content in the background
        	//cancelCountdownAlarm(this);
        	notifyStatusBar();
			stopSelf();
            return START_NOT_STICKY;
        }

        private void notifyStatusBar() {
            // The PendingIntent to launch our activity if the user selects this notification
            Intent launcher = new Intent(this,UltimateStopwatchActivity.class);
            launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,launcher,PendingIntent.FLAG_ONE_SHOT);

            // Set the icon, scrolling text and timestamp
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Ultimate Stopwatch")
                    .setSubText(getString(R.string.countdown_complete))
                    .setSmallIcon(R.drawable.notification)
                    .setContentIntent(contentIntent)
                    .build();

            //Notification notification = new Notification(R.drawable.icon,getString(R.string.countdown_complete),System.currentTimeMillis());
            try
            {
	            notification.ledARGB=0xFF808080;
	            notification.ledOnMS=500;
	            notification.ledOffMS=1000;
	            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
	            notification.audioStreamType=AudioManager.STREAM_NOTIFICATION;
	            //notification.sound= Uri.parse("android.resource://com.geekyouup.android.ustopwatch/" + R.raw.alarm);
            }catch(Exception e)
            {}
            
            notification.defaults |= (Notification.DEFAULT_ALL);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // We use a layout id because it is a unique number.  We use it later to cancel.
            notificationManager.notify(R.layout.main, notification);
        }
        
		@Override
		public IBinder onBind(Intent arg0) {return null;}
    }

    public static void showChronometerNotification(Context context, long startTime)
    {
        // The PendingIntent to launch our activity if the user selects this notification
        Intent launcher = new Intent(context,UltimateStopwatchActivity.class);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,launcher,PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle("Ultimate Stopwatch")
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis()-startTime)
                .setSmallIcon(R.drawable.notification)
                .setContentIntent(contentIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // We use a layout id because it is a unique number.  We use it later to cancel.
        notificationManager.notify(R.layout.countdown, notification);
    }

    public static void cancelChronometerNotification(Context context)
    {
        try
        {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(R.layout.countdown);
        }catch(Exception e){}
    }
}

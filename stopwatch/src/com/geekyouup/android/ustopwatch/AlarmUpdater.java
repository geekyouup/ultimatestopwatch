package com.geekyouup.android.ustopwatch;

import com.geekyouup.android.ustopwatch.fragments.UltimateStopwatchFragments;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

public class AlarmUpdater {

	public static void cancelCountdownAlarm(Context context)
	{
		try
		{
			AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent defineIntent = new Intent(context,UpdateService.class);
			PendingIntent piWakeUp = PendingIntent.getService(context,0, defineIntent, 0);
	        alarmMan.cancel(piWakeUp);
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
		PendingIntent piWakeUp = PendingIntent.getService(context,0, defineIntent, 0);
        alarmMan.cancel(piWakeUp);
        
		if(inMillis != -1) alarmMan.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+inMillis, piWakeUp);
	}
	
    public static class UpdateService extends Service {
    	
        @Override
        public void onStart(Intent intent, int startId) {
            // Build the widget update for today
			//no need for a screen, this just has to refresh all content in the background
			notifyStatusBar();
        }

        private void notifyStatusBar() {
            // Set the icon, scrolling text and timestamp
            Notification notification = new Notification(R.drawable.icon,getString(R.string.countdown_complete),System.currentTimeMillis());
            
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
            
            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,new Intent(this,UltimateStopwatchFragments.class),0);

            // Set the info for the views that show in the notification panel.
            notification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.countdown_complete), contentIntent);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // We use a layout id because it is a unique number.  We use it later to cancel.
            notificationManager.notify(R.layout.main, notification);
        }
        
		@Override
		public IBinder onBind(Intent arg0) {return null;}
    }
}

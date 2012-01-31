package uk.ac.nott.mrl.jef.TextBot;
/**
 * @author jef@cs.nott.ac.uk
 * The service that schedules the reminders by using an @see AlarmManager. 
 * The PendingIntent is broadcasted and handled by @see Text_BroadcastReceiver.
 */

import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class TextSchedulerService extends Service {
	
	private SQLiteDatabase db;
	private long now;
	private Date today;
	private int morningHr;
	private int morningMin;
	private PendingIntent reminder;
	private boolean done = false;
	
	public void onCreate() {
		
        db = new ScheduleSQLHelper(TextSchedulerService.this).getWritableDatabase();
        now = System.currentTimeMillis();
        today = new Date(now);
        
		
		//spawn a new Thread that does the work
		Thread thread = new Thread(null, task, "TextScheduler_Service");
		thread.start();
	}

	Runnable task = new Runnable() {
		public void run() {
			
//			synchronized (mBinder) {
			Log.d("TextSchedulerService","today: "+today.toLocaleString());
			Cursor c = db.rawQuery("SELECT schedule_hr, schedule_min FROM schedule", null);
			if (c.getCount()>0) {
				c.moveToFirst();
				
				try {
					morningHr = c.getInt(0);
				}catch (NullPointerException npe) {
					Log.e("TextSchedulerService", npe.toString());
				}
				try {
					morningMin = c.getInt(1);
				}catch (NullPointerException npe) {
					Log.e("TextSchedulerService", npe.toString());
				}
				
				if ( (today.getHours()<morningHr) || ( (today.getHours()==morningHr) && (today.getMinutes()<morningMin)) ) {
					//schedule the reminder for this day
					//the intent we use to fire the reminders
					Intent intent = new Intent(TextSchedulerService.this, Text_BroadcastReceiver.class)
						.putExtra("mod", "morning");
			        reminder = PendingIntent.getBroadcast(TextSchedulerService.this,
			                0, intent, PendingIntent.FLAG_ONE_SHOT);
					
					
					long reminderTime = getNextReminderTime(morningHr, morningMin);
					Log.d("TextSchedulerService","next text scheduled...");
        			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        			am.set(AlarmManager.RTC_WAKEUP, reminderTime, reminder);
        			done = true;
					
				}
				
				else if ( 
						(!done && today.getHours()>morningHr) || 
						( (today.getHours()==morningHr) && (today.getMinutes()==morningMin))
				) {
					//schedule the morning reminder for the next day
					//the intent we use to fire the reminders
					Intent intent = new Intent(TextSchedulerService.this, Text_BroadcastReceiver.class)
						.putExtra("mod", "morning");
			        reminder = PendingIntent.getBroadcast(TextSchedulerService.this,
			                0, intent, PendingIntent.FLAG_ONE_SHOT);
					
					
					long reminderTime = getNextReminderTime(morningHr, morningMin);
					Log.d("TextSchedulerService","next texting for tomorrow scheduled...");
        			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        			am.set(AlarmManager.RTC_WAKEUP, reminderTime, reminder);
					done = true;
				}
			}
			c.close();
			db.close();
			TextSchedulerService.this.stopSelf();	
			
			
//			}
			
			
		}
    };
    
    private long getNextReminderTime(int hr, int min) {
    	GregorianCalendar gc = new GregorianCalendar();
		Date nextReminder = new Date();
		nextReminder.setHours(hr);
		nextReminder.setMinutes(min);
		nextReminder.setDate(today.getDate());
		if ( (!nextReminder.after(today)) ||  (today.getHours()==hr && today.getMinutes()==min) ) {
			//date is in the past or now, need to push off till tomorrow
			Log.d("TextScheduler_Service","nextReminder:"+nextReminder+" is not after today:"+today );
			gc.setTime(nextReminder);
			gc.roll(GregorianCalendar.DAY_OF_MONTH, true);
			nextReminder = gc.getTime();
			Log.d(getClass().getSimpleName(), "corrected date:"+nextReminder);
		}
		if (nextReminder.before(today)) {
			//if it is still before today here, means the month has changed, too. 
			gc.setTime(nextReminder);
			gc.roll(GregorianCalendar.MONTH, true);
			nextReminder = gc.getTime();
			Log.d(getClass().getSimpleName(), "corrected month:"+nextReminder);	
		}
		if (nextReminder.before(today)) {
			//if it is still before today here, means the year has changed. 
			gc.setTime(nextReminder);
			gc.roll(GregorianCalendar.MONTH, true);
			nextReminder = gc.getTime();
			Log.d(getClass().getSimpleName(), "corrected year:"+nextReminder);	
		}
		Log.d("TextSchedulerService","nextReminder:"+nextReminder);
		
		long scheduleTime = nextReminder.getTime();
		return scheduleTime;
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	private final IBinder mBinder = new Binder() {
        @Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
		        int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

}

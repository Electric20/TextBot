package uk.ac.nott.mrl.jef.TextBot;
/**
 * @author jef@cs.nott.ac.uk 
 * This receiver catches the intents issued by an AlarmManager, which is used to fire the reminders. 
 * It triggers the @see ReminderSchedulerService, which instantiates the next AlarmManager. 
 * Also, it directly notifies the user with the reminders in showNotification(). 
 */


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;


public class Text_BroadcastReceiver extends BroadcastReceiver {
	
	private final static int WAKE_TIME = 5000;  //wake the device up for 5s to make sure text gets send

	@Override
	public void onReceive(Context context, Intent intent) {
		
		
		PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
        PowerManager.WakeLock w = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Text_BroadcastReceiver");
        w.acquire(WAKE_TIME);
        
		Log.d("Text_BroadcastReceiver","fired");
		
		//schedule next text
		context.startService(new Intent (context, TextSchedulerService.class).putExtra("today", false));
		
		//send text
		context.startService(new Intent( context, TextSenderService.class));
		
	}

}
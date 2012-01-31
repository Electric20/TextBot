package uk.ac.nott.mrl.jef.TextBot;

import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

public class LogDeliveredMessages extends BroadcastReceiver {
	
	private final static String TAG = "LogDeliveredMessages";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		int resultCode = this.getResultCode();
		int requestCode = intent.getIntExtra("requestCode", 0);
		
		Log.d(TAG, "onReceive()");
		Log.d(TAG, "requestCode: "+requestCode);
		Log.d(TAG, "resultCode: "+resultCode);
		
		if (requestCode == TextSenderService.SENT_INTENT) {
			
			Log.d(TAG, "sent.");

			switch (resultCode) {
				case Activity.RESULT_OK: 
					Log.d(TAG, "resultCode: RESULT_OK");
					break; 
				case Activity.RESULT_CANCELED:
					Log.d(TAG, "resultCode: RESULT_CANCELED");
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Log.d(TAG, "resultCode: RESULT_ERROR_GENERIC_FAILURE");
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Log.d(TAG, "resultCode: RESULT_ERROR_NO_SERVICE");
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Log.d(TAG, "resultCode: RESULT_ERROR_NULL_PDU");
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Log.d(TAG, "resultCode: RESULT_ERROR_RADIO_OFF");
					break;
				default: 
					Log.d(TAG, "unknown resultCode");
					break;
			}
		
		}
		
		else if (requestCode == TextSenderService.DELIVERY_INTENT) {
			
			Log.d(TAG, "delivered.");
			
			switch (resultCode) {
			case Activity.RESULT_OK: 
				Log.d(TAG, "resultCode: RESULT_OK");
				break; 
			case Activity.RESULT_CANCELED:
				Log.d(TAG, "resultCode: RESULT_CANCELED");
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				Log.d(TAG, "resultCode: RESULT_ERROR_GENERIC_FAILURE");
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				Log.d(TAG, "resultCode: RESULT_ERROR_NO_SERVICE");
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				Log.d(TAG, "resultCode: RESULT_ERROR_NULL_PDU");
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				Log.d(TAG, "resultCode: RESULT_ERROR_RADIO_OFF");
				break;
			default: 
				Log.d(TAG, "unknown resultCode");
				break;
			}
			
			String address = intent.getStringExtra("deliveredTo");
			String text = intent.getStringExtra("text");
			
			Log.d(TAG, "deliveredTo: "+address +", text: "+text +", at: " + new Date());
		}
	}

}

package uk.ac.nott.mrl.jef.TextBot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class TextSenderService extends Service {
	
	
	private Messenger mService; //to talk to PullData_Service
	private SmsManager smsMngr;  
	private static final String TAG = "TextSenderService";
	public static final int SENT_INTENT = 1;
	public static final int DELIVERY_INTENT = 2;
	public static final double KWH_COST = 0.1285; //average price according to OFGEM
	public static final int CONSTANT = 2; //relating to the format of the JSON callback
	
	
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	
	public void onCreate() {
	
		smsMngr = SmsManager.getDefault();
		
		/** bind to the remote PullData_Service */
		Log.d(TAG, "attempting to bind.");
		doBindService();
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
				
	
	
	private void processAndSend(String json) {
		
		Log.d(TAG, "attempting to send msg.");
		
		int hubId = 0;
		int data = 0;
		String name = "";
		double total = 0;
		
		//TODO: fix PendingIntents 
		//TODO: weekly text (when?)
		//TODO: different texts (see twitter)?
		
		try {
			
			JSONObject jObject = (JSONObject) new JSONTokener(json).nextValue();
			JSONObject dataO = jObject.getJSONObject("data");
			JSONObject yesterdayO = dataO.getJSONObject("yesterday");
			int i;
			Log.d(TAG, "yesterday.length(): "+yesterdayO.length());
			double average = yesterdayO.getDouble("average");
			
			for (i = 0; i<yesterdayO.length()-CONSTANT; i++) {
				JSONObject home = yesterdayO.getJSONObject(""+i);
//				JSONObject hubIdO = home.getJSONObject("hubId");
				hubId = home.getInt("hubId");
				name = home.getString("description");
				total = home.getDouble("total");
				
				Log.d(TAG, "JSON::: home: "+i + ", hubId: "+hubId +", name: "+ name + ", consumption:" +total);
				
				
				String text = getText(hubId, total, average);
				
				Intent catchDeliveredIntent = new Intent(this, LogDeliveredMessages.class).putExtra("requestCode", DELIVERY_INTENT);
				catchDeliveredIntent.putExtra("text", text);
				
				Intent catchSentIntent = new Intent(this, LogSentMessages.class).putExtra("requestCode", SENT_INTENT);
				catchSentIntent.putExtra("text", text);
			
				
				String number = "";
				int phones = Participant.getPhones(hubId);
				
				if (phones == 1) {
					
					number = Participant.getPhoneNumber(hubId);
					
					catchDeliveredIntent.putExtra("deliveredTo", number);
					catchSentIntent.putExtra("sentTo", number);
					
					//TODO: fix pending intent (seem to only get the last one in the broadcast receiver) 
					
					PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, catchDeliveredIntent, PendingIntent.FLAG_ONE_SHOT);
					PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, catchSentIntent, PendingIntent.FLAG_ONE_SHOT);

					sendMsg(number, text, sentIntent, deliveredIntent);
				}
				
				else if (phones == 2) {
					
					number = Participant.getPhoneNumber(hubId);
					catchDeliveredIntent.putExtra("deliveredTo", number);
					catchSentIntent.putExtra("sentTo", number);
					
					PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, catchDeliveredIntent, PendingIntent.FLAG_ONE_SHOT);
					PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, catchSentIntent, PendingIntent.FLAG_ONE_SHOT);

					sendMsg(number, text, sentIntent, deliveredIntent);
					
					/** send also to second number */
					number = Participant.getSecondPhoneNumber(hubId);
					catchDeliveredIntent.putExtra("deliveredTo", number);
					catchSentIntent.putExtra("sentTo", number);
					
					deliveredIntent = PendingIntent.getBroadcast(this, 0, catchDeliveredIntent, PendingIntent.FLAG_ONE_SHOT);
					sentIntent = PendingIntent.getBroadcast(this, 0, catchSentIntent, PendingIntent.FLAG_ONE_SHOT);

					sendMsg(number, text, sentIntent, deliveredIntent);
				}
				
			}
			
		} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
		}
		
		//debugging
//		String action = jObject.getString("action");
//		hubId = jObject.getInt("hubId");
//		data = jObject.getInt("data");
//		
//		Log.d(TAG, "JSON Tokens: action: "+action+"; hubId: "+hubId+"; data: "+data);
		
		//We're done
		doUnbindService();	
	}
	
	public String getText(int hubId, double data, double average) {
		String text = "Hello "+Participant.getName(hubId)+". ";
		
		double differenceInPercent = 100-(data/average*100);
		String posNeg = "";
		if (differenceInPercent > 0)
			posNeg = "% below";
		if (differenceInPercent < 0) {
			posNeg = "% above";
			differenceInPercent = differenceInPercent - (2*differenceInPercent);
		}		
		else if (differenceInPercent == 0) {
			posNeg = "exactly";
		}
		
		double cost = roundTwoDecimals(KWH_COST*data); 
		double usage = roundThreeDecimals(data);
		differenceInPercent = roundTwoDecimals(differenceInPercent);
		int diff = (int)differenceInPercent; //drop decimals for texting
		
		text += "The electricity you used yesterday ("+usage+" kwh) may cost you around £"+cost
		+". Compared to your neighbours this is "+diff + posNeg +" average."; 
		
		return text;
		
	}
	
	double roundTwoDecimals(double d) {
    	DecimalFormat twoDForm = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
    	return Double.valueOf(twoDForm.format(d));
	} 
	
	double roundThreeDecimals(double d) {
    	DecimalFormat twoDForm = new DecimalFormat("#,###.###", new DecimalFormatSymbols(Locale.US));
    	return Double.valueOf(twoDForm.format(d));
	} 
	
		
	public void sendMsg(String number, String text, PendingIntent sentIntent, PendingIntent deliveredIntent) {
		
		Log.d(TAG, "sending to: "+number + ", text: "+text);

		//debugging for now only send to Ben, JAMES AND me.
		if (number.equals(Participant.NUMBER_9) || //James
				number.equals(Participant.NUMBER_10_1) || //Ben
				number.equals(Participant.NUMBER_10_2) ||
				number.equals(Participant.NUMBER_22_1) || // me 
				//Participants to start MONDAY, 8.8.2011	
				number.equals(Participant.NUMBER_14) ||
				number.equals(Participant.NUMBER_16) ||
				number.equals(Participant.NUMBER_17_1) ||
				number.equals(Participant.NUMBER_17_2) ||
				number.equals(Participant.NUMBER_18) ||
				number.equals(Participant.NUMBER_19) ||
				number.equals(Participant.NUMBER_20_1) ||
				number.equals(Participant.NUMBER_20_2) ||
				number.equals(Participant.NUMBER_21) || //FOR TUESDAY ||
				number.equals(Participant.NUMBER_24_1) ||
				number.equals(Participant.NUMBER_24_2) ||
				number.equals(Participant.NUMBER_25) ||
				number.equals(Participant.NUMBER_68)  ||

				number.equals(Participant.NUMBER_23) ||  //FOR FRIDAY
				number.equals(Participant.NUMBER_58_1) || //Not receiving til 17.08.
				number.equals(Participant.NUMBER_58_2) || //Not receiving til 17.08.
				number.equals(Participant.NUMBER_56) ||
				number.equals(Participant.NUMBER_60_1) ||
				number.equals(Participant.NUMBER_60_2) ||
				number.equals(Participant.NUMBER_55) ||
				number.equals(Participant.NUMBER_59) ||
				number.equals(Participant.NUMBER_57)  ||
				
				number.equals(Participant.NUMBER_63_1) || //For Monday, 15.08.11
				number.equals(Participant.NUMBER_63_2) ||

				number.equals(Participant.NUMBER_12)  ||  //Not receiving til 17.08.
				
				number.equals(Participant.NUMBER_61_1) ||  //For Wednesday, 17.08.
				number.equals(Participant.NUMBER_61_2) ||
				
				number.equals(Participant.NUMBER_62) || //for saturday, 20.08.
				number.equals(Participant.NUMBER_68) ||
				
				number.equals(Participant.NUMBER_66)  || //for Wednesday, 24.08.
				
				number.equals(Participant.NUMBER_48_1) || //started Wednesday, 7.09.
				number.equals(Participant.NUMBER_48_2) ||
				
				number.equals(Participant.NUMBER_67)	//Nadia
		) 
		
					
		{
			//to prevent ghost texts
			int hrs = new Date().getHours();
			Log.d(TAG, "IDIOTPROOF HOURS: "+hrs);
			
			if ( (!number.equals("")) && (hrs==8) ) 
			{	
				Log.d(TAG, "SENDING NOW");
				smsMngr.sendTextMessage(number, null, text, sentIntent, deliveredIntent);
			}
		}
		

	}

	
	
	/**
	 * Handler of incoming messages from service.
	 */
	 private Handler mHandler = new Handler() {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case PullData_Service.MSG_SET_VALUE:
	            	Log.d(TAG, "Received from service: " + (String)msg.obj);
	            	if (msg.obj != null) {
	            		//we have received a json array back from the PullData_Service
	            		processAndSend((String)msg.obj);
	            	}
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	};
	    
	    /**
	     * Target we publish for clients to send messages to IncomingHandler.
	     */
	    final Messenger mMessenger = new Messenger(mHandler);

	    /**
	     * Class for interacting with the main interface of the service.
	     */
	    private ServiceConnection mConnection = new ServiceConnection() {
	        public void onServiceConnected(ComponentName className,
	                IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  We are communicating with our
	            // service through an IDL interface, so get a client-side
	            // representation of that from the raw service object.
	            mService = new Messenger(service);
	            Log.d(TAG, "Attached.");

	            // We want to monitor the service for as long as we are
	            // connected to it.
	            try {
	                Message msg = Message.obtain(null,
	                		PullData_Service.MSG_REGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);

	                // Give it some value as an example. -- debugging
//	                msg = Message.obtain(null,
//	                		PullData_Service.MSG_SET_VALUE, this.hashCode(), 0);
//	                mService.send(msg);
	            } catch (RemoteException e) {
	                // In this case the service has crashed before we could even
	                // do anything with it; we can count on soon being
	                // disconnected (and then reconnected if it can be restarted)
	                // so there is no need to do anything here.
	            }

	            // As part of the sample, tell the user what happened.
	            Toast.makeText(TextSenderService.this, R.string.remote_service_connected,
	                    Toast.LENGTH_SHORT).show();
	        }

	        public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            mService = null;
	            Log.d(TAG, "Disconnected.");

	            // As part of the sample, tell the user what happened.
	            Toast.makeText(TextSenderService.this, R.string.remote_service_disconnected,
	                    Toast.LENGTH_SHORT).show();
	        }
	    };

	    void doBindService() {
	        // Establish a connection with the service.  We use an explicit
	        // class name because there is no reason to be able to let other
	        // applications replace our component.
	        bindService(new Intent(this, 
	        		PullData_Service.class), mConnection, Context.BIND_AUTO_CREATE);
	        mIsBound = true;
	        Log.d(TAG, "Binding.");
	    }

	    void doUnbindService() {
	        if (mIsBound) {
	            // If we have received the service, and hence registered with
	            // it, then now is the time to unregister.
	            if (mService != null) {
	                try {
	                    Message msg = Message.obtain(null,
	                    		PullData_Service.MSG_UNREGISTER_CLIENT);
	                    msg.replyTo = mMessenger;
	                    mService.send(msg);
	                } catch (RemoteException e) {
	                    // There is nothing special we need to do if the service
	                    // has crashed.
	                }
	            }

	            // Detach our existing connection.
	            unbindService(mConnection);
	            mIsBound = false;
	            Log.d(TAG, "Unbinding.");
	        }
	    }


}

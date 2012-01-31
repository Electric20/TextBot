package uk.ac.nott.mrl.jef.TextBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class PullData_Service extends Service {
	
	private static final String SERVER = "http://79.125.20.47"; //"http://www.electric20.com";
	private static final String TEST_REQ = "/dataStore/request.php?u=jef@cs.nott.ac.uk&p=orchid&action=currentHubLoad&hubId=20";
	private static final String REQ = "/bzb/sms.php";
	private static final String TAG = "PullData_Service";
	private static final String WEEK = "week";
	private static final String DAY = "day";
	private DefaultHttpClient client;
	private HttpGet httpGet;
	private ResponseHandler <String> responseHandler;
	
	
	 /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

	
	/**
    * Handler of incoming messages from clients.
    */
   private final Handler mHandler = new Handler() {
       @Override
       public void handleMessage(Message msg) {
           switch (msg.what) {
               case MSG_REGISTER_CLIENT:
                   mClients.add(msg.replyTo);
                   break;
               case MSG_UNREGISTER_CLIENT:
                   mClients.remove(msg.replyTo);
                   break;
               case MSG_SET_VALUE:
                   mValue = msg.arg1;
                   Log.d(TAG, "(String)msg.obj:" +(String)msg.obj);
                   Log.d(TAG, "mValue:" +mValue);
                   for (int i=mClients.size()-1; i>=0; i--) {
                       try {
                           mClients.get(i).send(Message.obtain(null,
                                   MSG_SET_VALUE, msg.obj));
                       } catch (RemoteException e) {
                           // The client is dead.  Remove it from the list;
                           // we are going through the list from back to front
                           // so this is safe to do inside the loop.
                           mClients.remove(i);
                       }
                   }
                   break;
               default:
                   super.handleMessage(msg);
           }
       }
   };


	final Messenger mMessenger = new Messenger(mHandler);

	
	 @Override
	    public void onCreate() {
	        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

	        // Display a notification about us starting.
	        showNotification();
	    }

	    @Override
	    public void onDestroy() {
	        // Cancel the persistent notification.
	        mNM.cancel(R.string.remote_service_started);

	        // Tell the user we stopped.
	        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
	    }

	    /**
	     * When binding to the service, we return an interface to our messenger
	     * for sending messages to the service.
	     */
	    @Override
	    public IBinder onBind(Intent intent) {
	    	
	    	//do the work in a thread
	    	new Thread(new WorkerThread()).start();
	    	
	        return mMessenger.getBinder();
	    }
	    
	    public class WorkerThread implements Runnable {
     		
			public void run() { 
				
				client = new DefaultHttpClient();
		        responseHandler = new BasicResponseHandler();
		    	client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		        
				//TODO: dynamic param handling of week/day
				getMessagesFromServer(DAY);
			}
		}
	    
	    private void getMessagesFromServer(String param) {
	    	
	    	Log.d(TAG, "downloading data from "+ SERVER);
			//next, also get the accompanying text file
//			httpGet = new HttpGet(SERVER+"/....?param="+param);
	    	//debug
	    	httpGet = new HttpGet(SERVER+REQ);
			HttpResponse response = null;
			HttpEntity resEntity;
			InputStream returnedStream;
			JSONArray jArray;
			String json = ""; 
			boolean failed = false;
			
			try {
				Log.d(TAG, "executing GET: " + httpGet.getRequestLine());
				response = client.execute(httpGet);
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			
			Log.d(TAG, "response: "+response);
			Log.d(TAG, "statusLine: "+response.getStatusLine());
			
			if (response != null) {
				resEntity = response.getEntity();
				if (resEntity != null) {
					try {
						//the InpuStream holding the text file data
						returnedStream = resEntity.getContent();
						//TODO: read lines and write to db...
						BufferedReader br = new BufferedReader(new InputStreamReader(returnedStream));
						String line;
						try {
							//read the lines and write to db
							while ((line = br.readLine()) != null) {
								json += line;
								Log.d(TAG, "JSON: "+json);
							}
						} catch (Exception e) {
								Log.e(TAG, e.toString());
						}
							
					} catch (Exception e) {
						Log.e(TAG, e.toString());
					}
					
					
					if (!json.equals("")) {
						
						/** post to messenger to hand over to calling clients */
						try {
							mMessenger.send(Message.obtain(mHandler, MSG_SET_VALUE, ""+json));
						} catch (RemoteException e) {
							Log.e(TAG, e.getMessage());
						}
					}		
				}
			}
	    
	    	
	    	
	    	
	    }

	    /**
	     * Show a notification while this service is running.
	     */
	    private void showNotification() {
	        // In this sample, we'll use the same text for the ticker and the expanded notification
	        CharSequence text = getText(R.string.remote_service_started);

	        // Set the icon, scrolling text and timestamp
	        Notification notification = new Notification(R.drawable.downloading, text,
	                System.currentTimeMillis());

	        // The PendingIntent to launch our activity if the user selects this notification
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	                new Intent(this, TextBot.class), 0);

	        // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
	                       text, contentIntent);

	        // Send the notification.
	        // We use a string id because it is a unique number.  We use it later to cancel.
	        mNM.notify(R.string.remote_service_started, notification);
	    }
	 
}


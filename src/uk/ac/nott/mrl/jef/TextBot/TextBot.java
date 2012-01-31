package uk.ac.nott.mrl.jef.TextBot;

import android.app.Activity;
import android.os.Bundle;
	import android.app.Activity;
	import android.app.Dialog;
	import android.app.TimePickerDialog;
	import android.content.Intent;
	import android.database.Cursor;
	import android.database.sqlite.SQLiteDatabase;
	import android.os.Bundle;
	import android.util.Log;
	import android.view.View;
	import android.widget.Button;
	import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

	public class TextBot extends Activity {
		
		private TextView morningTime;
		private Button setMorningTime;
		private Button doneButton;
		
		private int morningHr;
		private int morningMin;
		
		static final int TIME_DIALOG_ID_M = 0;
		
		private SQLiteDatabase db;
		
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			setContentView(R.layout.reminders);
			
			morningTime = (TextView) findViewById(R.id.morningTimeDisplay);
			setMorningTime = (Button) findViewById(R.id.pickMorningTime);
			doneButton = (Button) findViewById(R.id.done_button);
			
			setMorningTime.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					showDialog(TIME_DIALOG_ID_M);
				}
			});
			
			
			//default to show when nothing in db
	        morningHr = 0;
	        morningMin = 0;
	      
	       
	
			db = new ScheduleSQLHelper(this).getWritableDatabase();
			Cursor cu = db.rawQuery("SELECT schedule_hr, schedule_min FROM schedule", null);
			if (cu.getCount()!=0) {
				//we have some data
				cu.moveToFirst();
				try {
					morningHr = cu.getInt(0);
					morningMin = cu.getInt(1);
				}catch (Exception e) {
					Log.e("SetSchedule", e.toString());
				}
			}
			cu.close();
	        updateMorningDisplay();

	      
	        
	        doneButton.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					db.close();
					startService(new Intent(TextBot.this, TextSchedulerService.class).putExtra("today", true));
					Toast.makeText(TextBot.this, "Text scheduled", Toast.LENGTH_SHORT).show();
//					startActivity(new Intent(this, OverviewMap.class));
					
				}
			});
		
	        
		}
		
		private static String pad(int c) {
			if (c>=10)
				return String.valueOf(c);
			else 
				return "0" + String.valueOf(c);
		}
		
	    private void updateMorningDisplay() {
	    	morningTime.setText(
	    			new StringBuilder()
	    			.append("Schedule your recurring text message. \n \n ")
	    			.append(
	    					"Please set it to a time when you want the text message to be send each day. \n \n Currently set to: ")
	    			.append(pad(morningHr)).append(":")
	    			.append(pad(morningMin)));
	    }
	    
	    
	 // the callback received when the user "sets" the time in the dialog
	    private TimePickerDialog.OnTimeSetListener morningTimeSetListener =
	        new TimePickerDialog.OnTimeSetListener() {
	            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	                morningHr = hourOfDay;
	                morningMin = minute;
	                
	                if (!db.isOpen())
	                	db = new ScheduleSQLHelper(TextBot.this).getWritableDatabase();
	                
	                Cursor c = db.rawQuery("SELECT schedule_hr FROM schedule", null);
	                if (c.getCount()==0) {
	                	db.execSQL("INSERT INTO schedule (schedule_hr,schedule_min) VALUES ("+morningHr+","+ morningMin+")");
	                }
	                else if (c.getCount()>0) {
	                	db.execSQL("UPDATE schedule SET schedule_hr="+morningHr+",schedule_min="+morningMin+" WHERE id=1");
	                }
	                c.close();
	                
	                updateMorningDisplay();
	            }
	        };
	        
	  
	        
	        @Override
			protected Dialog onCreateDialog(int id) {
			    switch (id) {
			    case TIME_DIALOG_ID_M:
			        return new TimePickerDialog(this,
			                morningTimeSetListener, morningHr, morningMin, true);
			    }
			    return null;
			}
	        
	        public void onWindowFocusChanged(boolean hasFocus) {
	        	super.onWindowFocusChanged(hasFocus);
	        	
	        	Log.d("TextBot", "hasFocus="+hasFocus);
	        	if (hasFocus) {
	        		if (db==null) {
	        			db = (new ScheduleSQLHelper(TextBot.this)).getWritableDatabase();
	        			Log.d("Settings","just created new DB");
	        		}
	                
	        	}
	        	else if (!hasFocus) {
	        		//db.close();
	        	}
	        }

	        public void onPause() {
	        	super.onPause();
	        	Log.d("TextBot","onPause");
//	        	db.close();
	        }
	    

			
			
		

	}
package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.EventLog;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class PerAppService extends Service implements SensorEventListener {
	private final IncomingHandler incomingHandler = new IncomingHandler();
	private final Messenger messenger = new Messenger(incomingHandler);
	private LinearLayout ll = null;
	private WindowManager wm;
	private PackageManager pm;
	private SensorManager sm;
	private AudioManager am;
	protected WindowManager.LayoutParams lp;
	private SharedPreferences options;
	private VolumeController vc;
	private static final int windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // PHONE, ALERT, DIALOG, OVERLAY, ERROR
	private Setting[] settings;
	private Thread logThread = null;
	private boolean interruptReader;
	private Process logProcess;
	private float[] gravity = { 0f, 0f, 0f };
	private int requestedOrientation = -1;
	private ScreenReceiver screenReceiver = null;
	private boolean hardOrientation = false;
	private boolean optionalHardOrientation = true;
	public boolean screenOn = true;

	public class IncomingHandler extends Handler {
		public static final int MSG_OFF = 0;
		public static final int MSG_ON = 1;
		public static final int MSG_SET_HARD_ORIENTATION = 2;
		public static final int MSG_CLOSE_HARD_ORIENTATION = 3;
		public static final int MSG_ADJUST = 4;
		public static final int MSG_BOOST = 5;
		public static final int MSG_RELOAD_SETTINGS = 6;
		
		@Override 
		public void handleMessage(Message m) {
			PerApp.log("Message: "+m.what);
			switch(m.what) {
			case MSG_RELOAD_SETTINGS:
				PerAppService.this.getSettings();
				break;
			case MSG_ON:
				if (ll != null) {
					ll.setVisibility(View.VISIBLE);
				}
				break;
			case MSG_OFF:
			if (ll != null) {
				ll.setVisibility(View.GONE);
			}
			break;
			case MSG_ADJUST:
				if (ll != null) {
					adjustParams();
					wm.updateViewLayout(ll, lp);
				}
				break;
			case MSG_BOOST:
				if (vc != null) {
					vc.reset();
					setBoost();
				}
				break;
			case MSG_SET_HARD_ORIENTATION:
				hardOrientation = true;
				setHardOrientation();
				break;
			case MSG_CLOSE_HARD_ORIENTATION:
				hardOrientation = false;
				pauseHardOrientation(true);
				break;
			default:
				super.handleMessage(m);
			}
		}
	}
	
	private void setBoost() {
		vc = null; //new VolumeController(PerAppService.this, options.getBoolean(Options.PREF_BOOST, false) ? BOOST: 0f);
	}
	

	private void activityResume(String packageName) {
		for (Setting s: settings) {
			PerApp.log("setting "+s.getName()+" for "+packageName);
			s.set(packageName);
		}
	}
	
	@SuppressWarnings("static-access")
	private void setOptionalHardOrientation(String activity) {
		PerApp.log("Activity: "+activity);
		ActivityInfo info;
		
		try {
			info = pm.getActivityInfo(ComponentName.unflattenFromString(activity), 0);
		} catch (NameNotFoundException e) {
			hardOrientation = false;
			pauseHardOrientation(true);
			return;
		}
		
		if (info.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR ||
				info.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR ||
				info.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			PerApp.log("hard orientation on");
			try {
				messenger.send(Message.obtain(null, incomingHandler.MSG_SET_HARD_ORIENTATION, 0,0));
			} catch (RemoteException e) {
				PerApp.log(""+e);
			}
			
		}
		else {
			PerApp.log("hard orientation off "+info.screenOrientation);
			try {
				messenger.send(Message.obtain(null, incomingHandler.MSG_CLOSE_HARD_ORIENTATION, 0,0));
			} catch (RemoteException e) {
				PerApp.log(""+e);
			}
		}
	}

	@SuppressLint("NewApi")
	private void monitorLog() {
		Random x = new Random();
		BufferedReader logReader;

		for(;;) {
			logProcess = null;
			
			String marker;
			if (Build.VERSION.SDK_INT >= 8) {
				marker = "mobi.omegacentauri.PerApp:marker:"+System.currentTimeMillis()+":"+x.nextLong()+":";
				EventLog.writeEvent(12345, marker);
			}
			else {
				// TODO: use clock
				marker = null;
			}
			String app = null;

			try {
				PerApp.log("logcat monitor starting");
//				Log.v("PerApp", marker);
				String[] cmd2 = { "logcat", "-b", "events", "[12345]:I", 
						"am_resume_activity:I", "am_restart_activity:I", "*:S" };
				logProcess = Runtime.getRuntime().exec(cmd2);
				logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
				PerApp.log("reading");

				String line;
				Pattern pattern = Pattern.compile
					("I/am_(resume|restart)_activity.*?:\\s+\\[.*,(([^/]*)/[^//,]*).*\\]");

				while (null != (line = logReader.readLine())) {
					if (interruptReader)
						break;
					Matcher m = pattern.matcher(line);
					if (m.find()) {						
						if (marker == null) {
							activityResume(m.group(3));						
							setOptionalHardOrientation(m.group(2));
						}
					}
					else if (marker != null && line.contains(marker)) {
						PerApp.log("Marker found");
						marker = null;
						if (app != null)
							activityResume(app);
						app = null;
					}
				}

				logReader.close();
				logReader = null;
			}
			catch(IOException e) {
				PerApp.log("logcat: "+e);

				if (logProcess != null)
					logProcess.destroy();
			}

            
			if (interruptReader) {
				PerApp.log("reader interrupted");
			    return;
			}

			PerApp.log("logcat monitor died");
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return messenger.getBinder();
	}

	protected void getSettings() {
		settings = PerApp.getSettings(PerAppService.this, options);

		for (Setting s: settings)
			s.activate();
	}

	@Override
	public void onCreate() {
		PerApp.log("Creating service");
		options = PreferenceManager.getDefaultSharedPreferences(this);

		getSettings();
		
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);        
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);        
        am = (AudioManager)getSystemService(AUDIO_SERVICE);
        pm = getPackageManager();

		setBoost();

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    	
		ll = new LinearLayout(this);
        ll.setClickable(false);
        ll.setFocusable(false);
        ll.setFocusableInTouchMode(false);
        ll.setLongClickable(false);
		
        lp = new WindowManager.LayoutParams(
        	WindowManager.LayoutParams.WRAP_CONTENT,
        	WindowManager.LayoutParams.WRAP_CONTENT,
        	windowType,
         	WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
         	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
         	PixelFormat.RGBA_8888);        
                
        adjustParams();
        
        wm.addView(ll, lp);
        ll.setVisibility(View.GONE);
        
        int icon = R.drawable.brightnesson;
		
        if (Options.getNotify(options) == Options.NOTIFY_NEVER)
			icon = 0;

        Notification n = new Notification(
        		icon,
        		"PerApp", 
        		System.currentTimeMillis());
        Intent i = new Intent(this, PerApp.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        n.setLatestEventInfo(this, "PerApp", "PerApp is on", 
        		PendingIntent.getActivity(this, 0, i, 0));
        PerApp.log("notify from service "+n.toString());

        startForeground(PerApp.NOTIFICATION_ID, n);

        Runnable logRunnable = new Runnable(){
        	@Override
        	public void run() {
                interruptReader = false;
				monitorLog();
			}};  
		logThread = new Thread(logRunnable);
		
		logThread.start();
		PerApp.log("Ready");
		
		screenReceiver = new ScreenReceiver();
    	registerReceiver(screenReceiver, 
    			new IntentFilter(Intent.ACTION_SCREEN_ON));    	
    	registerReceiver(screenReceiver, 
    			new IntentFilter(Intent.ACTION_SCREEN_OFF));
    	
    	screenOn = true;
    	
		optionalHardOrientation = options.getBoolean(Options.PREF_HARD_ORIENTATION, false);
	}
	
	private void pauseHardOrientation(boolean stop) {
		hardOrientation = false;
		
		sm.unregisterListener(this);		
		if (stop && ll != null)
			ll.setVisibility(View.GONE);
	}
	
	private void setHardOrientation() {
		if (!hardOrientation || !screenOn)
			return;
		
		lp.screenOrientation = wm.getDefaultDisplay().getOrientation();
		wm.updateViewLayout(ll, lp);
		ll.setVisibility(View.VISIBLE);
		
		gravity[0] = 0f;
		gravity[1] = 0f;
		gravity[2] = 0f;
		
//		ll.setVisibility(View.GONE);
//		wm.updateViewLayout(ll, lp);

		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		
    	hardOrientation = true;
	}
	
	private void adjustParams() {
//        lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT; 
	}
	
	@Override
	public void onDestroy() {
		if (screenReceiver != null) {
			unregisterReceiver(screenReceiver);
			screenReceiver = null;
		}
		
		pauseHardOrientation(true);
		
		if (ll != null) {
			wm.removeView(ll);
			ll = null;
		}
		
		for (Setting s: settings)
			s.onDestroy();
		
		if (logThread != null) {
			interruptReader = true;
			try {
				if (logProcess != null) {
					PerApp.log("Destroying service, killing reader");
					logProcess.destroy();
				}
//				logThread = null;
			}
			catch (Exception e) {
			}  
		}
		
		PerApp.log("Destroying service, destroying notification =" + (Options.getNotify(options) != Options.NOTIFY_ALWAYS));
		stopForeground(Options.getNotify(options) != Options.NOTIFY_ALWAYS);
	}
	
	@Override
	public void onStart(Intent intent, int flags) {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, flags);
		return START_STICKY;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event)
    {
          gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
          gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
          gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];
          
          float sq0 = gravity[0]*gravity[0];
          float sq1 = gravity[1]*gravity[1];
          float sq2 = gravity[2]*gravity[2];
          
          if (sq1 >= 3*(sq0 + sq2) && gravity[1] > 4f) {
        	  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          }
          else if (Build.VERSION.SDK_INT >= 9 && sq1 >= 3*(sq0 + sq2) && gravity[1] < -4f) {
        	  PerApp.log("hardOrientation + reverse_portrait + "+event.values[0]+" "+event.values[1]+" "+event.values[2]);
        	  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          }
          else if (sq0 >= 3*(sq1 + sq2) && gravity[0] > 4f ) {
        	  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          }
          else if (Build.VERSION.SDK_INT >= 9 && sq0 >= 3*(sq1 + sq2) && gravity[0] < -4f ) {
        	  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          }
          
          if (requestedOrientation >= 0 && ll != null && lp != null &&
            	( lp.screenOrientation != requestedOrientation ||
            			ll.getVisibility() == View.GONE)) {
        	  
        	  lp.screenOrientation = requestedOrientation;
			  wm.updateViewLayout(ll, lp);
			  ll.setVisibility(View.VISIBLE);
          }
     }
	
	 protected class ScreenReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				PerApp.log("screen on");
				screenOn = true;
				if (hardOrientation) {
					setHardOrientation();
				}
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				PerApp.log("screen off");
				screenOn = false;
				pauseHardOrientation(false);
			}
		}
		 
	 }
}

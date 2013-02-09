package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
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
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class PerAppService extends Service implements SensorEventListener {
	private final IncomingHandler incomingHandler = new IncomingHandler();
	private final Messenger messenger = new Messenger(incomingHandler);
	private LinearLayout orientationChanger = null;
	protected WindowManager.LayoutParams orientationLayout;
	private WindowManager wm;
	private PackageManager pm;
	private SensorManager sm;
	private AudioManager am;
	private SharedPreferences options;
	private VolumeController vc;
	private static final int windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // PHONE, ALERT, DIALOG, OVERLAY, ERROR
	private Setting[] settings;
	private Thread logThread = null;
	private boolean interruptReader;
	private Process logProcess = null;
	private float[] gravity = { 0f, 0f, 0f };
	private int requestedOrientation = -1;
	private ScreenReceiver screenReceiver = null;
	private boolean activeHardOrientation = false;
	public boolean screenOn = true;
	private long lastSensorTime = Long.MIN_VALUE;
	private static final long MAX_FILTER_DELTA_MS = 2000;
	private static final float FILTER_CONSTANT_MS = 200;
	private static final float HARD_ORIENTATION_ANGLE = 20;
	private static final float HARD_ORIENTATION_COS_SQ
	    = (float)(Math.cos(HARD_ORIENTATION_ANGLE * Math.PI / 180) *
	    	Math.cos(HARD_ORIENTATION_ANGLE * Math.PI / 180));
	private static int OUR_MARKER_NUMBER = 713273919;
//	private static final String logcatCommandLine =
//		"logcat -b events";
	private static final String logcatCommandLine =
		"logcat -b events ["+OUR_MARKER_NUMBER+"]:I am_resume_activity:I am_restart_activity:I *:S";
	private static final int LOGCAT_IDENTIFYING_ARGS = 4;

	public class IncomingHandler extends Handler {
		public static final int MSG_OFF = 0;
		public static final int MSG_ON = 1;
		public static final int MSG_SET_HARD_ORIENTATION = 2;
		public static final int MSG_CLOSE_HARD_ORIENTATION = 3;
		public static final int MSG_ADJUST = 4;
		public static final int MSG_BOOST = 5;
		public static final int MSG_RELOAD_SETTINGS = 6;
		public static final int MSG_FORCE_ORIENTATION = 7;
		public static final int MSG_OS_ORIENTATION = 8;
		
		@Override 
		public void handleMessage(Message m) {
			PerApp.log("Message: "+m.what);
			switch(m.what) {
			case MSG_RELOAD_SETTINGS:
				PerAppService.this.getSettings();
				break;
			case MSG_ON:
				if (orientationChanger != null) {
					orientationChanger.setVisibility(View.VISIBLE);
				}
				break;
			case MSG_OFF:
			if (orientationChanger != null) {
				orientationChanger.setVisibility(View.GONE);
			}
			break;
			case MSG_ADJUST:
				if (orientationChanger != null) {
					adjustParams();
					wm.updateViewLayout(orientationChanger, orientationLayout);
				}
				break;
			case MSG_BOOST:
				if (vc != null) {
					vc.reset();
					setBoost();
				}
				break;
			case MSG_SET_HARD_ORIENTATION:
				activeHardOrientation = true;
				setHardOrientation(true);
				break;
			case MSG_CLOSE_HARD_ORIENTATION:
				activeHardOrientation = false;
				if (orientationChanger != null)
					orientationChanger.setVisibility(View.GONE);
				break;
			case MSG_FORCE_ORIENTATION:
				pauseHardOrientation(false);
				activeHardOrientation = false;
				requestedOrientation = m.arg1;
				orientationLayout.screenOrientation = requestedOrientation;
				wm.updateViewLayout(orientationChanger, orientationLayout);
				orientationChanger.setVisibility(View.VISIBLE);
				break;
			case MSG_OS_ORIENTATION:
				pauseHardOrientation(true);
				activeHardOrientation = false;
				if (orientationChanger != null)
					orientationChanger.setVisibility(View.GONE);
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
			s.setAfter();
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
			try {
				messenger.send(Message.obtain(null, incomingHandler.MSG_CLOSE_HARD_ORIENTATION, 0,0));
			} catch (RemoteException e1) {
			}
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
		
		activityResume(getPackageName());

		for(;;) {
			logProcess = null;
			
			String marker;
			if (Build.VERSION.SDK_INT >= 8) {
				marker = "mobi.omegacentauri.PerApp:marker:"+System.currentTimeMillis()+":"+x.nextLong()+":";
				EventLog.writeEvent(OUR_MARKER_NUMBER, marker);
				PerApp.log("will look for "+marker);
			}
			else {
				// TODO: use clock
				marker = null;
			}
			String app = null;
			
//			try {
//				Runtime.getRuntime().exec(new String[] { "ln", "-s", "/data/test-ARP-123", "/data/log/u123" });
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}

			try {
				PerApp.log("logcat monitor starting");
				
				
				String cmd2;
				if (Build.VERSION.SDK_INT >= 16) {
					cmd2 = "su -c "+logcatCommandLine;
				}
				else {
					cmd2 = logcatCommandLine;
					
				}
				
//				String noorphan = installBlob("noorphan");
//				
//				if (noorphan == null) {
//					PerApp.log("Cannot install noorphan");
//					return;
//				}

				logProcess = Runtime.getRuntime().exec(
						cmd2.split(" ")
//						new String[] {
//						noorphan,
//						cmd2
//						}
						);
				logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));

				String line;
				Pattern pattern = Pattern.compile
					("I/am_(resume|restart)_activity.*?:\\s+\\[.*,(([^/]*)/[^//,]*).*\\]");

				PerApp.log("reading");
				while (null != (line = logReader.readLine())) {
					if (interruptReader)
						break;
					Matcher m = pattern.matcher(line);
					if (marker == null) {
						if (m.find()) 						
							activityResume(m.group(3));
					}
					else if (line.contains(marker)) {
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
			}

			if (logProcess != null) {
				logProcess.destroy();
				logProcess = null;
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

	@SuppressLint({ "NewApi", "NewApi" })
	private String installBlob(String blobName) {
		File cacheDir = getCacheDir();
		File blob = new File(cacheDir.getPath() + "/" + blobName);
		File tmp = new File(blob.getPath() + ".tmp");
		if (true || !blob.exists()) {
			AssetManager assets = getAssets();
			InputStream in = null;
			FileOutputStream out = null;
			try {
				in = assets.open(blobName);
				out = new FileOutputStream(tmp);
				byte[] buffer = new byte[1024];
				int s;
				while(0<=(s = in.read(buffer))) {
					out.write(buffer, 0, s);
				}
				out.close();
				out = null;
				in.close();
				in = null;
			} catch (IOException e) {
				if (in != null)
					try {
						in.close();
					} catch (IOException e1) {
					}
				if (out != null)
					try {
						out.close();
					} catch (IOException e1) {
					}
				tmp.delete();
				return null;
			}
			tmp.renameTo(blob);
		}
		if (VERSION.SDK_INT >= 9)
			blob.setExecutable(true);
		else
			try {
				Runtime.getRuntime().exec(new String[] { "chmod ", "755", blob.getPath() }).waitFor();
			} catch (InterruptedException e) {
			} catch (IOException e) {
			}
		
		return blob.getPath();
	}


	@Override
	public IBinder onBind(Intent arg0) {
		return messenger.getBinder();
	}

	protected void getSettings() {
		settings = PerApp.getSettings(PerAppService.this, options);

		for (Setting s: settings) {
			s.activate();
			s.setMessenger(messenger);
		}
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
    	
		orientationChanger = new LinearLayout(this);
        orientationChanger.setClickable(false);
        orientationChanger.setFocusable(false);
        orientationChanger.setFocusableInTouchMode(false);
        orientationChanger.setLongClickable(false);
		
        orientationLayout = new WindowManager.LayoutParams(
        	WindowManager.LayoutParams.WRAP_CONTENT,
        	WindowManager.LayoutParams.WRAP_CONTENT,
        	windowType,
         	WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
         	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
         	PixelFormat.RGBA_8888);        
                
        adjustParams();
        
        wm.addView(orientationChanger, orientationLayout);
        orientationChanger.setVisibility(View.GONE);
        
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

//    	Setting.haveSetting(settings, OrientationSetting.ID);
	}
	
	private void pauseHardOrientation(boolean stop) {
		sm.unregisterListener(this);		

		if (stop && orientationChanger != null)
			orientationChanger.setVisibility(View.GONE);
	}
	
	private void setHardOrientation(boolean set) {
		if (!screenOn)
			return;

		if (requestedOrientation < 0) {
			PerApp.log("orientation "+wm.getDefaultDisplay().getOrientation());
			requestedOrientation = wm.getDefaultDisplay().getOrientation();
		}
		
		if (set) {
			orientationLayout.screenOrientation = requestedOrientation;
			wm.updateViewLayout(orientationChanger, orientationLayout);
			orientationChanger.setVisibility(View.VISIBLE);
		}
		
		gravity[0] = 0f;
		gravity[1] = 0f;
		gravity[2] = 0f;
		
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	private void adjustParams() {
//        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT; 
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (screenReceiver != null) {
			unregisterReceiver(screenReceiver);
			screenReceiver = null;
		}
		
		pauseHardOrientation(true);
		
		if (orientationChanger != null) {
			wm.removeView(orientationChanger);
			orientationChanger = null;
		}
		
		for (Setting s: settings)
			s.onDestroy();

		closeLogThread();
		PerApp.log("Destroying service, destroying notification =" + (Options.getNotify(options) != Options.NOTIFY_ALWAYS));
		stopForeground(Options.getNotify(options) != Options.NOTIFY_ALWAYS);
	}
	
	private void closeLogThread() {
			try {
				if (logProcess != null) {
					PerApp.log("Destroying service, killing reader "+logProcess);
					logProcess.destroy();
					logProcess = null;
				}
			}
			catch (Exception e) {
			}  
			interruptReader = true;
	}
	
//	@Override
//	protected void finalize() throws Throwable {
//		try {
//			closeLogThread();
//		} finally {
//			super.finalize();
//		}
//	}
	
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
		  float alpha = 0.8f;
		  
		  long t = System.currentTimeMillis();
		  long delta = t - lastSensorTime;
		  
		  if (t < lastSensorTime || lastSensorTime + MAX_FILTER_DELTA_MS < t) {
			  gravity[0] = gravity[1] = gravity[2] = 0f;
			  lastSensorTime = t;
			  
			  return;
		  }

		  alpha = delta / (FILTER_CONSTANT_MS + delta);
		  gravity[0] = alpha * gravity[0] + (1-alpha) * event.values[0];
		  gravity[1] = alpha * gravity[1] + (1-alpha) * event.values[1];
		  gravity[2] = alpha * gravity[2] + (1-alpha) * event.values[2];
		  		  
		  lastSensorTime = t;
		
          float sq0 = gravity[0]*gravity[0];
          float sq1 = gravity[1]*gravity[1];
          float sq2 = gravity[2]*gravity[2];
          
          float sumSq = sq0 + sq1 + sq2;
          
          if (sumSq >= 9f * 9f) {
        	  float sumSqCosSq = sumSq * HARD_ORIENTATION_COS_SQ;
        	  
        	  if (sq1 >= sumSqCosSq) {
        		  if (gravity[1] > 0f)
        			  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        		  else if (Build.VERSION.SDK_INT >= 9)
        			  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        	  }
        	  else if (sq0 >= sumSqCosSq) {
        		  if (gravity[0] > 0f)
        			  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        		  else if (Build.VERSION.SDK_INT >= 9)
        			  requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;        		  
        	  }
          }
          
          if (requestedOrientation >= 0 && activeHardOrientation &&
        		  orientationChanger != null && orientationLayout != null &&
            	( orientationLayout.screenOrientation != requestedOrientation ||
            			orientationChanger.getVisibility() == View.GONE)) {
        	  
        	  orientationLayout.screenOrientation = requestedOrientation;
			  wm.updateViewLayout(orientationChanger, orientationLayout);
			  orientationChanger.setVisibility(View.VISIBLE);
          }
     }
	
	 protected class ScreenReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				PerApp.log("screen on");
				screenOn = true;
				if (activeHardOrientation) {
					setHardOrientation(false);
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

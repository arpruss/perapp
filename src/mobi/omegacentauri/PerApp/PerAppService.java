package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.omegacentauri.PerApp.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.location.Address;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Gravity;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PerAppService extends Service implements OnTouchListener {
	private int startVolume;
	private float startY;
	private Info info;
	
	private boolean defaultHidden = true;
	private static final int HEIGHT_DELTA_DP = 50;
	private static final float BOOST = 0.4f;
	private int height_delta;
	private int[] widths = {30, 50, 80, 20};
	private final Messenger messenger = new Messenger(new IncomingHandler());
	private LinearLayout ll = null;
	private WindowManager wm;
	private AudioManager am;
	protected WindowManager.LayoutParams lp;
	private SharedPreferences options;
	private int height;
	private VolumeController vc;
	private float scale;
	private static final int windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // PHONE, ALERT, DIALOG, OVERLAY, ERROR
	private Setting[] settings;
	private Thread logThread = null;
	private Runnable logRunnable;
	
	private static final String[] intercept = {
		"com.amazon.avod.client.activity.MediaPlayerActivity",
		"com.netflix.mediaclient.PlayerActivityPlus",
		"me.abitno.vplayer.VideoActivity",
		"com.amazon.mp3.",
		"mobi.omegacentauri.PerApp.PerApp",
		"com.cooliris.media.MovieView",
		"com.hulu.plus.activity.PlayerActivity",
		"com.videon.android.mediaplayer.",
		"de.stohelit.audiobookplayer.MainPlayer",
		"de.stohelit.folderplayer.",
	};
	
	public class IncomingHandler extends Handler {
		public static final int MSG_OFF = 0;
		public static final int MSG_ON = 1;
		public static final int MSG_VISIBLE = 2;
		public static final int MSG_HIDDEN = 3;
		public static final int MSG_ADJUST = 4;
		public static final int MSG_BOOST = 5;
		public static final int MSG_RELOAD_SETTINGS = 6;
		
		@Override 
		public void handleMessage(Message m) {
			PerApp.log("Message: "+m.what);
			switch(m.what) {
			case MSG_RELOAD_SETTINGS:
				settings = PerApp.getSettings(PerAppService.this, options);
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
			case MSG_HIDDEN:
				if (ll != null) {
					hide();
					defaultHidden = true;
				}
				break;
			case MSG_VISIBLE:
				if (ll != null) {
					show();
					defaultHidden = false;
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
			default:
				super.handleMessage(m);
			}
		}
	}
	
	private void setBoost() {
		vc = null; // new VolumeController(PerAppService.this, options.getBoolean(Options.PREF_BOOST, false) ? BOOST: 0f);		
	}
	
	private boolean activeFor(String s) {
		if (options.getBoolean(Options.PREF_ACTIVE_IN_ALL, false))
			return true;
		
		for(String i: intercept) {
			if (s.startsWith(i))
				return true;
		}
		return false;
	}
	
	private void activityResume(String packageName) {
		for (Setting s: settings) {
			PerApp.log("setting "+s.getName()+" for "+packageName);
			s.set(packageName);
		}
	}
	
	private void monitorLog() {
		for(;;) {
			
			BufferedReader reader = null;
			Process p = null;
			
			try {
				PerApp.log("logcat monitor starting");
				String[] cmd = { "logcat", "-b", "events", "-c" };
				p = Runtime.getRuntime().exec(cmd);
				try {
					p.waitFor();
				} catch (InterruptedException e) {
				}
				String[] cmd2 = { "logcat", "-b", "events" };
				p = Runtime.getRuntime().exec(cmd2);
				reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				PerApp.log("reading");
				
				String line;
				Pattern pattern = Pattern.compile
					("I/am_(resume|restart)_activity.*?:\\s+\\[.*,([^/]*).*\\]");  
				
				while (null != (line = reader.readLine())) {
					Matcher m = pattern.matcher(line);
					if (m.find()) {
						activityResume(m.group(2));
					}
					else {
						if (line.contains("am_resume_activity") || line.contains("am_restart_activity"))
							PerApp.log("Should have matched "+line);
					}
				}
				
				PerApp.log("logcat done?!");
				reader.close();
			}
			catch(IOException e) {
				PerApp.log("logcat: "+e);
				if (p != null)
					p.destroy();
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
	
	@Override
	public void onCreate() {
		PerApp.log("Creating service");
		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		settings = PerApp.getSettings(this, options);
		
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);        
        am = (AudioManager)getSystemService(AUDIO_SERVICE);

		setBoost();
	
		ll = new LinearLayout(this);
        ll.setClickable(true);
        ll.setFocusable(false);
        ll.setFocusableInTouchMode(false);
        ll.setLongClickable(false);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        
        scale = getResources().getDisplayMetrics().density;
        
        height_delta = (int) (HEIGHT_DELTA_DP * scale);
		
        lp = new WindowManager.LayoutParams(        	
        	50,
        	WindowManager.LayoutParams.FILL_PARENT,
        	windowType,
         	WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
         	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
         	PixelFormat.RGBA_8888);
        
        adjustParams();
        
        wm.addView(ll, lp);
        ll.setVisibility(View.GONE);
        
//        ll.setOnTouchListener(this);
        
        if (Options.getNotify(options) != Options.NOTIFY_NEVER) {
        	
			Notification n = new Notification(
					R.drawable.brightnesson,
					"PerApp", 
					System.currentTimeMillis());
			Intent i = new Intent(this, PerApp.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
			n.setLatestEventInfo(this, "PerApp", "PerApp is on", 
					PendingIntent.getActivity(this, 0, i, 0));
			PerApp.log("notify from service "+n.toString());
	
			startForeground(PerApp.NOTIFICATION_ID, n);
        }
		
		Runnable logRunnable = new Runnable(){
			@Override
			public void run() {
				monitorLog();
			}};  
		logThread = new Thread(logRunnable);
		
		logThread.start();
		PerApp.log("Ready");
		
//		if (!defaultHidden)
//			show();
		hide();
	}
	
	private void adjustParams() {
        lp.gravity = options.getBoolean(Options.PREF_LEFT, true) ? Gravity.LEFT : Gravity.RIGHT;
        lp.width = (int) (widths[options.getInt(Options.PREF_WIDTH, Options.OPT_MEDIUM)] * getResources().getDisplayMetrics().density);
	}
	
	@Override
	public void onDestroy() {
		if (ll != null) {
			wm.removeView(ll);
			ll = null;
		}
		if (logThread != null) {
			logThread.stop();
			logThread = null;
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

	private void show() {
		if (ll != null)
			ll.setBackgroundColor(Color.argb(64,128,128,128));		
	}
	
	private void hide() {
		if (ll != null)
			ll.setBackgroundColor(Color.argb(0,0,0,0));		
	}
	
	private float interpolate(float x0, float x1, float x2, float y0, float y2) {
		if (x2 == x0)
			return (y0+y2)/2;
		return (x1-x0)/(x2-x0)*(y2-y0)+y0;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			startVolume = vc.getVolume();

			info = new Info();
			
			startY = event.getY() - height_delta;
			height = ll.getHeight() - height_delta;
			if (startY < 0)
				startY = 0;
			if (height < startY)
				startY = height;
			show();
			return true;
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			int newVolume;

			float y = event.getY() - height_delta;
			if (y <0)
				y = 0;
			if (height < y)
				y = height;
			
			int maxVolume = vc.getMaxVolume();
			
			if (startY * startVolume < (height-startY)*(maxVolume-startVolume)) {
				PerApp.log("A:"+startY+" "+y+" 0 "+startVolume+" "+maxVolume);
				newVolume = (int)interpolate(startY, y, 0, startVolume, maxVolume);
			}
			else {
				PerApp.log("B:"+height+" "+y+" "+startY+" "+0+" "+startVolume);
				newVolume = (int)interpolate(height, y, startY, 0, startVolume);
			}
			
			if (newVolume < 0)
				newVolume = 0;
			else if (maxVolume < newVolume)
				newVolume = maxVolume;
			
			vc.setVolume(newVolume);
			
			info.move(event.getY(), vc.getPercent());
			
			return true;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (defaultHidden)
				hide();
			info.close();
		}
		return false;
	}
	
	private class Info {
		TextView tv;
		WindowManager.LayoutParams lp;
		
		public Info() {
			tv = new TextView(PerAppService.this);

			lp = new WindowManager.LayoutParams(        	
					WindowManager.LayoutParams.WRAP_CONTENT,
	            	WindowManager.LayoutParams.WRAP_CONTENT,
	            	WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
	             	WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
	             	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
	             	PixelFormat.RGBA_8888);
			
			lp.gravity = PerAppService.this.lp.gravity | Gravity.TOP;
			
			tv.setBackgroundColor(Color.argb(200, 128, 128, 128));
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
			tv.setPadding((int)(PerAppService.this.scale*10), 
					(int)(PerAppService.this.scale*10),
					(int)(PerAppService.this.scale*10),
					(int)(PerAppService.this.scale*10));

			tv.setGravity(Gravity.CENTER_VERTICAL);			

			lp.x = PerAppService.this.lp.width + (int) (40 * PerAppService.this.scale);
			
			tv.setVisibility(View.GONE);
			
			wm.addView(tv, lp);
		}
		
		public void move(float y, int percent) {
			tv.setText(""+percent+"%");
			if (percent > 100) {
				tv.setTextColor(Color.RED);
				tv.setTypeface(null, Typeface.BOLD);
			}
			else {
				tv.setTextColor(Color.WHITE);
				tv.setTypeface(null, Typeface.NORMAL);
			}

			lp.y = (int) y;
			tv.setVisibility(View.VISIBLE);
			wm.updateViewLayout(tv, lp);
		}
		
		public void close() {
			tv.setVisibility(View.GONE);
		}
	}
}

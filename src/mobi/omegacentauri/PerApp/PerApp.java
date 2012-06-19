package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DecimalFormat;

import mobi.omegacentauri.PerApp.R;
import mobi.omegacentauri.PerApp.PerAppService.IncomingHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class PerApp extends Activity implements ServiceConnection {
	public static final boolean DEBUG = true;
	static final String MARKET = "Market";
	
	public static final DecimalFormat decimal = new DecimalFormat("0.0");
	

	private CheckBox activeBox;
	private boolean active;

	private Messenger messenger = null;

	private SharedPreferences options;
	private NotificationManager notificationManager;
	private boolean getOut = false;
	private ListView appsList;

	static final int NOTIFICATION_ID = 1;
	
	public static Setting[] getSettings(Context context, SharedPreferences pref) {
		Setting[] allSettings = new Setting[]{ 
				new OrientationSetting(context, pref),
				new TimeoutSetting(context, pref),
				new BoostSetting(context, pref),
		};
		
		int count = 0;
		for (int i=0; i<allSettings.length; i++)
			if (allSettings[i].isActive())
				count++;
		
		Setting[] settings = new Setting[count];

		count = 0;
		for (int i=0; i<allSettings.length; i++) {
			if (allSettings[i].isActive())
				settings[count++] = allSettings[i];
		}
		
		log(""+count+" settings active");
		
		return settings;
	}

	public static void log(String s) {
		if (DEBUG)
			Log.v("PerApp", s);
	}
	
	public void onNarrow(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_NARROW).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onVNarrow(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_VNARROW).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onMedium(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_MEDIUM).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onWide(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_WIDE).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onLeft(View v) {
		options.edit().putBoolean(Options.PREF_LEFT, true).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onRight(View v) {
		options.edit().putBoolean(Options.PREF_LEFT, false).commit();		
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void contextMenuOnClick(View v) {
		v.showContextMenu();
	}
	
	public void adOnClick(View v) {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (MARKET.contains("arket"))
    		i.setData(Uri.parse("market://search?q=pub:\"Omega Centauri Software\""));
    	else
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.ScreenDim.Full&showAll=1"));            		
    	startActivity(i);
	}
	
	private void message(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(Html.fromHtml(msg));
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getText(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();
	}
	
	private void firstTime() {
		if (! options.getBoolean(Options.PREF_FIRST_TIME, true))
			return;

		SharedPreferences.Editor ed = options.edit();
		ed.putBoolean(Options.PREF_FIRST_TIME, false);
		ed.commit();           
		
		String msg;
		msg = "With PerApp, you can have some different settings in different applications";
		message("Welcome", msg);
	}

	public void menuButton(View v) {
		openOptionsMenu();
	}
	
	public void helpButton(View v) {
		help();
	}
	
	private void saveOS() {
	}
	
	private void restoreOS() {
	}
	
	void stopService() {
		log("stop service");
		stopService(new Intent(this, PerAppService.class));
		restoreOS();
	}
	
	void saveSettings() {
	}
	
	void bind() {
		log("bind");
		Intent i = new Intent(this, PerAppService.class);
		bindService(i, this, 0);
	}
	
	void restartService(boolean bind) {
		stopService();
		saveSettings();		
		Intent i = new Intent(this, PerAppService.class);
		startService(i);
		if (bind) {
			bind();
		}
	}
	
	void setActive(boolean value, boolean bind) {
		SharedPreferences.Editor ed = options.edit();
		ed.putBoolean(Options.PREF_ACTIVE, value);
		ed.commit();
		if (value) {
			restartService(bind);
		}
		else {
			stopService();
		}
		active = value;
		updateNotification();
	}
	
	public void PleaseBuy(String title, String text, final boolean exit) {		
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle(title);        
        alertDialog.setMessage(Html.fromHtml(text));
        
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"Go to "+MARKET, 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	market();
            	if (exit) {
        			stopService();
            		finish();
            	}
            } });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"Not now", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	if (exit) {
        			stopService();
            		finish();
            	}
            	
            } });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (exit)
					finish();
			}
		});
        alertDialog.show();				
	}
	
	private void market() {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (MARKET.contains("Appstore")) {
          // string split up to fool switcher.sh
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	else if (MARKET.contains("AndroidPIT")) {
            // string split up to fool switcher.sh
    		i.setData(Uri.parse("http://www.androidpit.com/en/android/market/apps/app/mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	else {
          // string split up to fool switcher.sh
    		i.setData(Uri.parse("market://details?id=mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	startActivity(i);
	}
	
	private void changeLog() {
		message("Changes", getAssetFile("changelog.html"));
	}
	
	private void help() {
		message("Questions and Answers", getAssetFile(isKindle()?"kindlehelp.html":"help.html"));
	}
	
	private void versionUpdate() {
		int versionCode;
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			versionCode = 0;
		} 
		if (options.getInt(Options.PREF_LAST_VERSION, 0) != versionCode) {
			options.edit().putInt(Options.PREF_LAST_VERSION, versionCode).commit();
			changeLog();
		}
			
	}
	
	private void chooseSetting(final String app) {
		final Setting[] settings = getSettings(this, options);
		String[] settingNames = new String[settings.length];

		for (int i=0; i<settings.length; i++)
			settingNames[i] = settings[i].getName(); 
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final AlertDialog dialog; 
//		builder.setTitle("Choose setting");
		
		View v = View.inflate(this, R.layout.choose_setting, null);
		
		ListView list = (ListView)v.findViewById(R.id.settings);
		list.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, settingNames));

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				settings[position].dialog(PerApp.this, app);
			}        	
        });
		
		builder.setView(v);

		dialog = builder.create();
		dialog.show();		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log("onCreate");
		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		versionUpdate();
		firstTime();
		
    	setContentView(R.layout.main);

    	notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		active = options.getBoolean(Options.PREF_ACTIVE, false);

		if (active) {			
			restartService(true);
		}
		else {
			stopService();
		}
		
		activeBox = (CheckBox)findViewById(R.id.active);
		activeBox.setChecked(active);
		activeBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean value) {
				if (value && !active) {
					saveOS();
				}
				setActive(value, true);
			}});
		
        appsList = (ListView)findViewById(R.id.apps);
        
        appsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				MyApplicationInfo info = (MyApplicationInfo) appsList.getAdapter().getItem(position);
				chooseSetting(info.packageName);
			}        	
        });
		
	}

	public void setNotification(Context c, NotificationManager nm, boolean active) {
		int icon = active?R.drawable.brightnesson:R.drawable.brightnessoff;
		
		if (Options.getNotify(options) == Options.NOTIFY_NEVER)
			icon = 0;
		
		Notification n = new Notification(
				icon,
				"PerApp", 
				System.currentTimeMillis());
		Intent i = new Intent(c, PerApp.class);		
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
		n.setLatestEventInfo(c, "PerApp", "PerApp is "+(active?"on":"off"), 
				PendingIntent.getActivity(c, 0, i, 0));
		nm.notify(NOTIFICATION_ID, n);
		log("notify "+n.toString());
	}
	
	private void updateNotification() {
		updateNotification(this, options, notificationManager, active);
	}
	
	public void updateNotification(Context c, 
			SharedPreferences options, NotificationManager nm, boolean active) {
		log("notify "+Options.getNotify(options));
		switch(Options.getNotify(options)) {
		case Options.NOTIFY_AUTO:
			if (active)
				setNotification(c, nm, active);
			else {
				log("trying to cancel notification");
				nm.cancelAll();
			}
			break;
		case Options.NOTIFY_NEVER:
		case Options.NOTIFY_ALWAYS:
			setNotification(c, nm, active);
			break;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		updateNotification();
		
		if (getOut) { 
			log("getting out");
			return;
		}
		
        (new GetApps(this, appsList)).execute();
        
        if (active) {
			restartService(true);
		}			
	}

	@Override
	public void onPause() {
		super.onPause();
		
		if (getOut)
			return;
		
		saveSettings();

		if (messenger != null) {
			log("unbind");
			unbindService(this);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		log("onStop");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		log("onDestroy");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
//		case R.id.change_log:
//			changeLog();
//			return true;
//		case R.id.help:
//			help();
//			return true;
//		case R.id.please_buy:
//			new OtherApps(this, true);
//			return true;
		case R.id.options:
			startActivity(new Intent(this, Options.class));			
			return true;
		default:
			return false;
		}
	}
	
	private boolean largeScreen() {
		int layout = getResources().getConfiguration().screenLayout;
		return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_LARGE;		
	}
	
	private boolean isKindle() {
		return Build.MODEL.equalsIgnoreCase("Kindle Fire");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}

	public void sendMessage(int n, int arg1, int arg2) {
		if (messenger == null) 
			return;
		
		try {
			log("message "+n+" "+arg1+" "+arg2);
			messenger.send(Message.obtain(null, n, arg1, arg2));
		} catch (RemoteException e) {
		}
	}
	
	@Override
	public void onServiceConnected(ComponentName classname, IBinder service) {
		log("connected");
		messenger = new Messenger(service);
		try {
			messenger.send(Message.obtain(null, IncomingHandler.MSG_RELOAD_SETTINGS, 0, 0));
			messenger.send(Message.obtain(null, IncomingHandler.MSG_VISIBLE, 0, 0));
		} catch (RemoteException e) {
		} 
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		log("disconnected"); 
//		stopService(new Intent(this, PerAppService.class));
		messenger = null;		
	}

	static private String getStreamFile(InputStream stream) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(stream));

			String text = "";
			String line;
			while (null != (line=reader.readLine()))
				text = text + line;
			return text;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
	
	public String getAssetFile(String assetName) {
		try {
			return getStreamFile(getAssets().open(assetName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
}


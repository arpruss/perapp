package mobi.omegacentauri.PerApp;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.client.utils.URIUtils;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;

public abstract class Setting {
	public String id;
	protected String name;
	protected int intValue;
	protected String defaultValue;
	protected int defaultMode = DEFAULT;
	private SharedPreferences pref;
	public static final int SKIP = 0;
	public static final int DEFAULT = 1;
	public static final int SET = 2;
	public static final int REMEMBER_PER_APP = 3;
	public static final int GLOBAL = 3;
	private Spinner spin;
	protected Context context;
	private Messenger messenger = null;
	
	public static final String modes[] = { "Ignore launch", "Defaults", "Customize" };
	public static final int modeIds[] = { SKIP, DEFAULT, SET };
	public static final String modesRemembered[] = { "Ignore launch", "Remember", "Global" };
	public static final int modesRememberedIds[] = { SKIP, REMEMBER_PER_APP, GLOBAL };
	
	public Setting(Context context, SharedPreferences pref) {
		this.context = context;
		this.pref = pref;
	}
	
	public void activate() {		
	}
	
	public boolean isSupported() {
		return true;
	}
	
	public String getName() {
		return this.name;
	}
	
	protected String getId() {
		return this.id;
	}
	
	protected String getDefaultValue() {
		// May want to get this from preferences
		return defaultValue;
	}
	
	public Boolean isActive() {
		return isSupported() && pref.getBoolean(getId() + "..active", defaultActive());
	}
	
	protected boolean defaultActive() {
		return false;
	}

	private String getValuePrefName(String app) {
		if (app == null || app.equals(MyApplicationInfo.DEFAULT))
			app = "";
		
		return getId() + "..app.." + app;  
	}
	
	protected int getMode(String app) {
		return pref.getInt(getModePrefName(app), DEFAULT);
	}
	
	protected void setMode(String app, int mode) {
		pref.edit().putInt(getModePrefName(app), mode).commit();
	}
	
	protected String getModePrefName(String app) {
		if (app == null)
			app = "";
		
		return getId() + "..mode.." + app;  
	}
	
	public void load(String app) {
		String v = pref.getString(getValuePrefName(app), null);
		if (v == null) {
			PerApp.log("Getting default from "+getValuePrefName(null));
			v = pref.getString(getValuePrefName(null), getDefaultValue());
		}
		
		PerApp.log("decoding "+v);
		
		decode(v);
	}
	
	public void save(String app) {
		PerApp.log("saving to "+getValuePrefName(app));
		pref.edit().putString(getValuePrefName(app), encode()).commit();
	}
	
	protected String unparse() {
		return ""+intValue;
	}
	
	protected void parse(String s) {
		try {
			intValue = Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			intValue = Integer.parseInt(defaultValue);
		}
		PerApp.log("Value "+intValue);
	}
	
	private void decode(String in) {
		parse(URLDecoder.decode(in));
	}
	
	private String encode() {
		return URLEncoder.encode(unparse());
	}
	
	public String toString() {
		return ""+intValue; 
	}
	
	protected void set() {		
	}
	
	public void set(String app) {
		int mode = pref.getInt(getModePrefName(app), DEFAULT);
		
		PerApp.log("mode "+mode);

		if (mode == SKIP) 
			return;
		
		if (mode == SET)
			load(app);
		else {
			PerApp.log("loading default");
			load(null);
		}
		
		set();
	}
	
	protected View getDialogView(PerApp activity, Builder builder, int id, final String app) {
		return getDialogView(activity, builder, id, app, modes, modeIds);
	}
	
	protected void updateToDefault() {
	}
	
	protected View getDialogView(final PerApp activity, Builder builder, int id, final String app,
			String[] modeNames, final int[] modeIds) {
		load(app);
		
		PerApp.log("getDialogView()");

		builder.setTitle(name);
		
		builder.setOnCancelListener(new OnCancelListener(){

			@Override
			public void onCancel(DialogInterface dialog) {
				((BaseAdapter)activity.appsList.getAdapter()).notifyDataSetChanged();
			}});
		
		View v = View.inflate(activity, id, null);
		builder.setView(v);
		
		Button allToDefaults = (Button)v.findViewById(R.id.all_to_default);
		if (app.equals(MyApplicationInfo.DEFAULT)) {
			allToDefaults.setVisibility(View.VISIBLE);
			allToDefaults.setOnClickListener(new Button.OnClickListener(){

				@Override
				public void onClick(View arg0) {
					setAllToDefaults();
				}});
		}
		else
			allToDefaults.setVisibility(View.GONE);
		
		spin = (Spinner)v.findViewById(R.id.mode_spinner);
		ArrayAdapter<String> aa = new ArrayAdapter<String>(activity, 
				android.R.layout.simple_spinner_item,
				modeNames);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(aa);
		spin.setSelection(getMode(app));
		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int mode, long arg3) {
				setMode(app, modeIds[mode]);
				if (mode == DEFAULT) {
					parse(defaultValue); 
					updateToDefault();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		return v;
	}

	public void dialog(PerApp activity, String app) {				
	}
	
	protected void saveCustom(String app) {
		save(app);
		spin.setSelection(SET);
		setMode(app, SET);
	}
	
	protected void updateSystemSetting(ContentResolver cr, String s, String v) {
		if (android.provider.Settings.System.getString(cr, s) == v)
			return;
		PerApp.log("setting "+s+" to "+v);
		android.provider.Settings.System.putString(cr,s,v);
	}

	protected void updateSystemSetting(ContentResolver cr, String s, int v) {
		try {
			if (android.provider.Settings.System.getInt(cr, s) == v)
				return;
		} catch (SettingNotFoundException e) {
		}
		android.provider.Settings.System.putInt(cr,s,v);
	}
	
	public String describe(String app) {
		String out = "";
		int mode = getMode(app);
		
		out += modes[mode];
		
		if (mode != SKIP) {
			load(app);
			String c = describeValue();
			if (c != null) {
				out += ": " + c;
			}
		}
		
		return out;
	}
	
	protected String describeValue() {
		return null;
	}

	protected void onDestroy() {
	}

	public String describeForList(String app) {
		int mode = getMode(app);

		if (mode == SET || mode == DEFAULT) {
			load(app);
			return name + ": " + describeValue(); 
		}
		else
			return "";
	}
	
	protected void setAllToDefaults() {
		Map<String,?> allPrefs = pref.getAll();
		ArrayList<String> toDelete = new ArrayList<String>();
		String start1 = getId() + "..app..";
		String start2 = getId() + "..mode..";
		String notEnd = ".." + MyApplicationInfo.DEFAULT;
		for (String option: allPrefs.keySet()) {
			if ((option.startsWith(start1) ||
					option.startsWith(start2)) &&
					!option.endsWith(notEnd)) {
				toDelete.add(option);
			}
		}
		SharedPreferences.Editor ed = pref.edit();
		for (String s: toDelete) 
			ed.remove(s);
		ed.commit();
	}
	
	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;		
	}
	
	protected boolean sendMessage(int message, int arg1, int arg2) {
		try {
			messenger.send(Message.obtain(null, message, arg1, arg2));
			return true;
		} catch (RemoteException e1) {
			PerApp.log("sending message "+e1);
			return false;
		}		
	}
}

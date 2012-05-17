package mobi.omegacentauri.PerApp;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.http.client.utils.URIUtils;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public abstract class Setting {
	protected String id;
	protected String name;
	protected int intValue;
	protected String defaultValue; 
	private SharedPreferences pref;
	private static final int SKIP = 0;
	private static final int DEFAULT = 1;
	private static final int SET = 2;
	private Spinner spin;
	protected Context context;
	
	public static final String modes[] = { "Keep previous", "Defaults", "Customize" };
	
	public Setting(Context context, SharedPreferences pref) {
		this.context = context;
		this.pref = pref;
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
		return isSupported() && pref.getBoolean(getId() + "..active", true);
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
	
	protected View getDialogView(Activity activity, Builder builder, int id, final String app) {
		load(app);

		builder.setTitle(name);
		
		View v = View.inflate(activity, id, null);
		builder.setView(v);
		
		spin = (Spinner)v.findViewById(R.id.mode_spinner);
		ArrayAdapter<String> aa = new ArrayAdapter<String>(activity, 
				android.R.layout.simple_spinner_item,
				Setting.modes);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(aa);
		spin.setSelection(getMode(app));
		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int mode, long arg3) {
				setMode(app, mode);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		return v;
	}

	public void dialog(Activity activity, String app) {				
	}
	
	protected void saveCustom(String app) {
		save(app);
		spin.setSelection(SET);
		setMode(app, SET);
	}
	
	protected void updateSystemSetting(ContentResolver cr, String s, int v) {
		try {
			if (android.provider.Settings.System.getInt(cr, s) == v)
				return;
		} catch (SettingNotFoundException e) {
		}
		android.provider.Settings.System.putInt(cr,s,v);
	}
}

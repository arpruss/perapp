package mobi.omegacentauri.PerApp;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.http.client.utils.URIUtils;

import android.app.Activity;
import android.content.SharedPreferences;

public abstract class Setting {
	protected String id;
	protected String name;
	protected int intValue;
	protected String defaultValue; 
	private SharedPreferences pref;
	private static final int SKIP = 0;
	private static final int DEFAULT = 1;
	private static final int SET = 2;
	public static final String modes[] = { "Keep previous", "Defaults", "Customize" };
	
	public Setting(SharedPreferences pref) {
		this.pref = pref;
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
	
	private String getValuePrefName(String app) {
		if (app == null)
			app = "";
		
		return getId() + "..app.." + app;  
	}
	
	public static int getMode(SharedPreferences pref, String app) {
		return pref.getInt(getModePrefName(app), DEFAULT);
	}
	
	public static void setMode(SharedPreferences pref, String app, int mode) {
		pref.edit().putInt(getModePrefName(app), mode).commit();
	}
	
	private static String getModePrefName(String app) {
		if (app == null)
			app = "";
		
		return "mode.." + app;  
	}
	
	public void load(String app) {
		String v = pref.getString(getValuePrefName(app), null);
		if (v == null) {
			v = pref.getString(getValuePrefName(null), getDefaultValue());
		}
		
		decode(v);
	}
	
	public void save(String app) {		
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

		if (mode == SKIP) 
			return;
		
		if (mode == SET)
			load(app);
		else
			load(null);
		
		set();
	}

	public void dialog(Activity activity, String app) {				
	}
}

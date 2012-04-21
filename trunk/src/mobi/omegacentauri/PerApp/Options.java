package mobi.omegacentauri.PerApp;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity {
//	public final static String PREF_FIX_SLEEP = "fixSleep";
	public static final String PREF_ACTIVE = "active";
	public static final String PREF_NOTIFY = "notification";
	public static final String PREF_FIRST_TIME = "firstTime";
	public static final int NOTIFY_NEVER = 0;
	public static final int NOTIFY_AUTO = 1;
	public static final int NOTIFY_ALWAYS = 2;
	public static final String PREF_PREVIOUS_MODE = "previousMode";
	public static final String PREF_TRIAL_START = "trialStarted_106b3";
	public static final String PREF_FORCE_ON_WAKE = "forceOnWake";
	public static final String PREF_LAST_VERSION = "lastVersion";
	public static final String PREF_DID_COMPACT = "didCompact";
	public static final String PREF_ACTIVE_IN_ALL = "activeInAll";
	public static final String PREF_LEFT = "left";
	public static final int OPT_NARROW = 0;
	public static final int OPT_MEDIUM = 1;
	public static final int OPT_WIDE = 2;
	public static final int OPT_VNARROW = 3;
	public static final String PREF_WIDTH = "width";
	public static final String PREF_BOOST = "doBoost";
	public static final String PREF_AD = "lastAd";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	public static int getNotify(SharedPreferences options) {
		int n = Integer.parseInt(options.getString(PREF_NOTIFY, "1"));
		if (n == NOTIFY_NEVER)
			return NOTIFY_AUTO;
		else
			return n;
	}
}

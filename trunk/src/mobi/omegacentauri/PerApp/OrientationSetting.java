package mobi.omegacentauri.PerApp;

import android.content.SharedPreferences;

public class OrientationSetting extends Setting {
	public OrientationSetting(SharedPreferences pref) {
		super(pref);
		
		name = "Orientation";
		id = "orientationSetting";
	}
}

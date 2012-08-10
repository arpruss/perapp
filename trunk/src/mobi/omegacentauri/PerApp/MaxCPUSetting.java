package mobi.omegacentauri.PerApp;

import android.content.Context;
import android.content.SharedPreferences;

public class MaxCPUSetting extends MinMaxCPUSetting {
	public MaxCPUSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		if (speeds == null)
			return;

		defaultValue = ""+speeds[speeds.length-1];
		min = false;

		name = "Max CPU Speed";
		id = "MaxCPUSetting";
	}

	@Override
	protected void set() {
		CPUUtils.writeValue("scaling_max_freq", intValue);
	}
}

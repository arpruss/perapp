package mobi.omegacentauri.PerApp;

import android.content.Context;
import android.content.SharedPreferences;

public class MinCPUSetting extends MinMaxCPUSetting {
	public MinCPUSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		if (speeds == null)
			return;

		defaultValue = ""+speeds[0];
		min = true;

		name = "Min CPU Speed";
		id = "MinCPUSetting";
	}

	@Override
	protected void set() {
		CPUUtils.writeValue("scaling_min_freq", intValue);
	}
}

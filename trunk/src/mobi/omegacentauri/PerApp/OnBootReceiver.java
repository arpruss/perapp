package mobi.omegacentauri.PerApp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {		
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
		if (options.getBoolean(Options.PREF_ACTIVE, false) &&
				options.getBoolean(Options.PREF_START_ON_BOOT, false)
		) {
			Intent i = new Intent(context, PerAppService.class);
			context.startService(i);
		}
	}
}

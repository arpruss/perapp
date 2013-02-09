package mobi.omegacentauri.PerApp;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GPSSetting extends Setting {
	static final int GPS_NO_CHANGE = 0;
	static final int GPS_ON = 1;
	static final int GPS_OFF = 2;
	static final int GPS_ON_OFF = 3;
	static final int GPS_OFF_ON = 4;
	
	static final String ID = "GPSSetting";
	
	static final String[] descriptions = { "no change",
		"turn on", "turn off", "turn on, then off",
		"turn off, then on"
	};
	
	public GPSSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		name = "GPS";
		id = ID;
		defaultValue = "0";
	}
	
	@Override
	public void dialog(PerApp activity, final String app) {
		PerApp.log("dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.gps_setting, app); 
		
		Spinner spin = (Spinner)v.findViewById(R.id.gps_spinner);
		spin.setSelection(intValue);
		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int value, long arg3) {
				GPSSetting.this.intValue = value;
				saveCustom(app);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		builder.create().show();
	}
	
	@Override
	protected void set() {
		switch(intValue) {
		case GPS_ON:
		case GPS_ON_OFF:
			setGPSWrapper(true);
			break;
		case GPS_OFF:
		case GPS_OFF_ON:
			setGPSWrapper(false);
			break;
		}
	}
	
	private void setGPSWrapper(boolean b) {
		PerApp.log("GPS "+b);
	}

	@Override
	protected void doSetAfter() {
		switch(intValue) {
		case GPS_ON_OFF:
			setGPSWrapper(false);
			break;
		case GPS_OFF_ON:
			setGPSWrapper(true);
			break;
		default:
			break;
		}
	}
	
	@Override
	protected String describeValue() {
		if (intValue < descriptions.length && 0 <= intValue)
			return descriptions[intValue];
		else
			return "?";
	}
	
	@Override
	protected boolean defaultActive() {
		return true;
	}

	public static void setGPS(boolean b) {
//		try {
//			Settings.Secure.putString(context.getContentResolver(), 
//					Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "network,gps");
//		} catch(Exception e) {
//		}
		
	}
	
}

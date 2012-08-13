package mobi.omegacentauri.PerApp;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class OrientationSetting extends Setting {
	static final int ORIENT_AUTO = 0;
	static final int ORIENT_LOCKED = 1;
	static final int ORIENT_PORTRAIT = 2;
	static final int ORIENT_LANDSCAPE = 3;
	static final int ORIENT_REVERSE_PORTRAIT = 4;
	static final int ORIENT_REVERSE_LANDSCAPE = 5;
	static final int ORIENT_HARD_AUTO = 6;
	static final int ORIENT_HARD_FORCE = 7;
	
	static final String ID = "OrientationSetting2";
	
	static final String[] descriptions = { "automatic",
		"locked", "portrait", "landscape", "rev. portrait",
		"rev. landscape", "hard auto", "hard force"
	};
	
	public OrientationSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		name = "Orientation Lock";
		id = ID;
		defaultValue = "0";
	}
	
	@Override
	public void dialog(PerApp activity, final String app) {
		PerApp.log("dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.orientation_setting, app); 
		
		Spinner spin = (Spinner)v.findViewById(R.id.orientation_spinner);
		spin.setSelection(intValue);
		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int value, long arg3) {
				OrientationSetting.this.intValue = value;
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
		ContentResolver cr = context.getContentResolver();
		switch (intValue) {
		case ORIENT_AUTO:
			sendMessage(PerAppService.IncomingHandler.MSG_OS_ORIENTATION, 0, 0);
			updateSystemSetting(cr, android.provider.Settings.System.ACCELEROMETER_ROTATION,				
					1);
			break;
		case ORIENT_LOCKED:
			sendMessage(PerAppService.IncomingHandler.MSG_OS_ORIENTATION, 0, 0);
			updateSystemSetting(cr, android.provider.Settings.System.ACCELEROMETER_ROTATION,				
					0);
			break;
		case ORIENT_PORTRAIT:
			sendMessage(PerAppService.IncomingHandler.MSG_FORCE_ORIENTATION, 
					ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, 0);
			break;
		case ORIENT_LANDSCAPE:
			sendMessage(PerAppService.IncomingHandler.MSG_FORCE_ORIENTATION, 
					ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, 0);
			break;
		case ORIENT_REVERSE_LANDSCAPE:
			sendMessage(PerAppService.IncomingHandler.MSG_FORCE_ORIENTATION, 
					ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, 0);
			break;
		case ORIENT_REVERSE_PORTRAIT:
			sendMessage(PerAppService.IncomingHandler.MSG_FORCE_ORIENTATION, 
					ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, 0);
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
	
}

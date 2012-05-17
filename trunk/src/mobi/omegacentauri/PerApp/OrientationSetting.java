package mobi.omegacentauri.PerApp;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
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
	public OrientationSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		name = "Orientation Lock";
		id = "OrientationSetting";
		defaultValue = "0";
	}
	
	public void dialog(Activity activity, final String app) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.orientation_setting, app); 
		
		CheckBox cb = (CheckBox)v.findViewById(R.id.lock);
		cb.setChecked(intValue != 0);
		
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean value) {
				OrientationSetting.this.intValue = value ? 1 : 0;
				saveCustom(app);
			}
		});
		
		builder.create().show();
	}
	
	@Override
	protected void set() {
		ContentResolver cr = context.getContentResolver();
		updateSystemSetting(cr, android.provider.Settings.System.ACCELEROMETER_ROTATION,				
				1-intValue);
	}
}

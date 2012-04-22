package mobi.omegacentauri.PerApp;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
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
	public OrientationSetting(SharedPreferences pref) {
		super(pref);
		
		name = "Orientation";
		id = "orientationSetting";
		defaultValue = "0";
	}
	
	public void dialog(Activity activity, final String app) {
		load(app);
		
		View v = getDialogView(activity, R.layout.orientation_setting);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(v);
		
		CheckBox cb = (CheckBox)v.findViewById(R.id.lock);
			
		cb.setChecked(intValue != 0);
		
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean value) {
				OrientationSetting.this.intValue = value ? 1 : 0;
				save(app);
			}
		});
		
		builder.create().show();
	}
}

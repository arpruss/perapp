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

public class ButtonLightSetting extends Setting {
        protected float floatValue;

	public ButtonLightSetting(SharedPreferences pref) {
		super(pref);

		name = "ButtonLight";
		id = "ButtonLightSetting";
		defaultValue = "1.0";
	}

	@Override
	public void parse(String s) {
            try {
                floatValue = Float.ParseFloat(s);
            }
            catch (NumberFormatException e) {
                floatValue = Float.ParseFloat(defaultValue);
            }
        }

        @Override
        public String unparse() {
            return "" + floatValue;
        }

	public void dialog(Activity activity, final String app) {
		load(app);
	}
}

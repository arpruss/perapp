package mobi.omegacentauri.PerApp;

import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class FontSize extends Setting {
	
	public FontSize(Context context, SharedPreferences pref) {
		super(context, pref);
		
		if (!isSupported())
			return;
		
		name = "Font Size";
		id = "FontSizeSetting";
		defaultValue = "100";
	}
	
	private String getPrintableValue(int v) {
		return "" + v + "%";
	}
	
	@Override
	protected String describeValue() {
		return getPrintableValue(intValue);
	}

	public void dialog(PerApp activity, final String app) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.font_size_setting, app);
		
		final TextView tv = (TextView)v.findViewById(R.id.font_size_value);

		SeekBar bar = (SeekBar)v.findViewById(R.id.font_size);
		bar.setProgress((intValue-60+3)/5);
		tv.setText(getPrintableValue(intValue));
		
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				FontSize.this.intValue = 5*progress + 60;
				tv.setText(getPrintableValue(FontSize.this.intValue));
				
				PerApp.log("Timeout set to "+FontSize.this.intValue);
				if (fromUser)
					saveCustom(app);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		builder.create().show();
	}
	
	@Override
	protected void set() {
		ContentResolver cr = context.getContentResolver();
		Configuration config = new Configuration();
		Class cActivityManagerNative;
		try {
			
			cActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
			Object def = cActivityManagerNative.getMethod("getDefault").invoke(null);
			config.updateFrom((Configuration) cActivityManagerNative.getMethod("getConfiguration").invoke(def));
			if ((int)(100 * config.fontScale+.5f) != intValue) {
				PerApp.log("changing fontScale");
				config.fontScale = intValue / 100f;
				cActivityManagerNative.getMethod("updateConfiguration", Configuration.class).invoke(def, config);
			}
		}
		catch (InvocationTargetException e) {
			PerApp.log("wrapped "+e.getCause());
		}
		catch (Exception e) {
			PerApp.log(""+e);
		} 
	}

	@Override
	protected boolean defaultActive() {
		return false;
	}
}

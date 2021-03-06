package mobi.omegacentauri.PerApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

public class TimeoutSetting extends Setting {
	
	public TimeoutSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		if (!isSupported())
			return;
		
		name = "Screen Timeout";
		id = "TimeoutSetting";
		defaultValue = "60000";
	}
	
	private String getPrintableValue(int ms) {
		int minutes = ms / 60000;
		int seconds = (ms / 1000) % 60;
		return String.format("%d:%02d", minutes, seconds);
	}
	
	@Override
	protected String describeValue() {
		return getPrintableValue(intValue);
	}

	public void dialog(PerApp activity, final String app) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.timeout_setting, app);
		
		final TextView tv = (TextView)v.findViewById(R.id.timeout_value);

		SeekBar bar = (SeekBar)v.findViewById(R.id.timeout);
		bar.setProgress(intValue/10000-1);
		tv.setText(getPrintableValue(intValue));
		
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TimeoutSetting.this.intValue = (progress+1) * 10000;
				tv.setText(getPrintableValue(TimeoutSetting.this.intValue));
				
				PerApp.log("Timeout set to "+TimeoutSetting.this.intValue);
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
		if (intValue < 30000)
			intValue = 30000;
		updateSystemSetting(cr, android.provider.Settings.System.SCREEN_OFF_TIMEOUT,				
				intValue);
	}

	@Override
	protected boolean defaultActive() {
		return true;
	}
}

package mobi.omegacentauri.PerApp;

import java.text.DecimalFormat;

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

public class MinMaxCPUSetting extends Setting {
	protected int[] speeds;
	protected boolean min;
	private SeekBar bar;
	private TextView tv;
	
	public MinMaxCPUSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		if (!isSupported())
			return;
		
		speeds = CPUUtils.getSpeeds();
		
		if (speeds == null)
			return;				
	}
	
	private int getIndexOfSpeed(int speed) {
		for (int i=0; i<speeds.length; i++) {
			if(speeds[i] == speed)
				return i;
		}
		
		if (min)
			return 0;
		else
			return speeds.length - 1;
	}
	
	private String describeValue(int v) {
		v /= 1000;
		if (v<1000) {
			return ""+v+" MHz";
		}
		else {
			return ""+(new DecimalFormat("#.00").format(v/1000.))+" GHz";
		}
	}
	
	@Override
	public String describeValue() {
		return describeValue(intValue);
	}
	
	@Override
	protected void updateToDefault() {
		tv.setText(describeValue(intValue));
		bar.setProgress(getIndexOfSpeed(intValue));		
	}
	

	public void dialog(PerApp activity, final String app) {
		if (speeds == null)
			return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.cpu_setting, app);
		
		tv = (TextView)v.findViewById(R.id.cpu_value);

		bar = (SeekBar)v.findViewById(R.id.cpu);
		bar.setMax(speeds.length - 1);
		bar.setProgress(getIndexOfSpeed(intValue));
		tv.setText(""+intValue);
		
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				MinMaxCPUSetting.this.intValue = speeds[progress];
				tv.setText(describeValue(speeds[progress]));
				
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
	
	protected boolean defaultActive() {
		return false;
	}
}

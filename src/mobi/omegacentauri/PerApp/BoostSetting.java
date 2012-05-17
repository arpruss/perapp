package mobi.omegacentauri.PerApp;

import android.app.Activity;
import android.app.AlertDialog;
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

public class BoostSetting extends Setting {
	private AudioManager am;
	private Equalizer eq = null;
	private static final int MAX_BOOST = 15;
	
	public BoostSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		if (!isSupported())
			return;
		
		am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);		
		
		name = "Volume Boost";
		id = "BoostSetting";
		defaultValue = "0";
	}

	@Override
	public boolean isSupported() {
		return Build.VERSION.SDK_INT>=9;
	}
	
	private String entryName(int i, boolean selected) {
		String s;
		
		s = ""+i;
		
		if (i>7) 
			s += " [be careful]";
		
		if (selected)
			s = "active: "+s;
		
		return s; 
	}
	
	private String getWarning(int i) {
		if (i<8)
			return "";
		else
			return "Warning: May damage speakers/hearing.";
	}
	
	public void dialog(Activity activity, final String app) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.boost_setting, app);
		
		final TextView tv = (TextView)v.findViewById(R.id.boost_value);
		final TextView warn = (TextView)v.findViewById(R.id.warning);

		SeekBar bar = (SeekBar)v.findViewById(R.id.boost);
		bar.setProgress(intValue);
		tv.setText(""+intValue);
		warn.setText(getWarning(intValue));
		
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				BoostSetting.this.intValue = progress;
				tv.setText(""+progress);
				warn.setText(getWarning(progress));
				
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
		if (intValue == 0) {
			if (eq != null)
				eq.setEnabled(false);
			return;
		}
		
		try {
			if (eq == null)
				eq = new Equalizer(0,0);				

			PerApp.log("max boost "+eq.getBandLevelRange()[1]);
			if (intValue > eq.getBandLevelRange()[1]/100)
				intValue = eq.getBandLevelRange()[1]/100;
			if (intValue > MAX_BOOST)
				intValue = MAX_BOOST;
			
			for (int i=eq.getNumberOfBands()-1; i>=0; i--)
				eq.setBandLevel((short)i, (short)(intValue*100));
			
			eq.setEnabled(true);
		}
		catch (UnsupportedOperationException e) {
			eq = null;
		}
	}
}

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
	private boolean shape;
	
	public BoostSetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		if (!isSupported())
			return;
		
		am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);		
		
		name = "Volume Boost";
		id = "BoostSetting";
		defaultValue = "0";
		
		shape = pref.getBoolean("shapeBoost", true);
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
	
	public void dialog(Activity activity, final String app) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.boost_setting, app);
		
		final TextView tv = (TextView)v.findViewById(R.id.boost_value);

		SeekBar bar = (SeekBar)v.findViewById(R.id.boost);
		bar.setProgress(intValue);
		tv.setText(""+intValue);
		
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				BoostSetting.this.intValue = progress;
				tv.setText(""+progress);
				
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
		if (eq == null)
			eq = new Equalizer(0,0);				

		try {
			if (intValue == 0) {
				PerApp.log("turn off boost");
				eq.setEnabled(false);
				return;
			}
			
			PerApp.log("max boost "+eq.getBandLevelRange()[1]+ " actual boost "+(intValue*100));
			if (intValue > eq.getBandLevelRange()[1]/100)
				intValue = eq.getBandLevelRange()[1]/100;
			if (intValue > MAX_BOOST)
				intValue = MAX_BOOST;
			
			short v = (short)(intValue*100);

			for (int i=eq.getNumberOfBands()-1; i>=0; i--) {
				
				short adj = v;

				if (shape) {
					int hz = eq.getCenterFreq((short)i)/1000;
					if (hz < 150)
						adj = 0;
					else if (hz < 250)
						adj = (short)(v/2);
					else if (hz > 8000)
						adj = (short)(3*(int)v/4);
				}
				eq.setBandLevel((short)i, adj);
			}
			
			eq.setEnabled(true);
		}
		catch (UnsupportedOperationException e) {
			eq = null;
		}
	}
	
	@Override
	protected String describeValue() {
		return ""+intValue;
	}
	
	@Override
	protected void onDestroy() {
		if (eq != null) {
			eq.setEnabled(false);
			eq = null;
		}
	}
}

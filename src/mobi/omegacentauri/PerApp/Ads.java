package mobi.omegacentauri.PerApp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

public class Ads {
	private static final int[] ids = {
		R.drawable.screendimad
	};
	private static final String[] packages = {
		"mobi.omegacentauri.ScreenDim.Full"
	};
	private static final int NUM_ADS = ids.length;
	SharedPreferences options;
	int ad;
	private Context context;
	
	public Ads(Context context, SharedPreferences options) {
		this.context = context;
		this.options = options;
		ad = 0;
	}
	
	private void setAd(ImageView v) {
		int ad = getNext(options.getInt(Options.PREF_AD, -1));
		options.edit().putInt(Options.PREF_AD, ad).commit();
		if (ad < 0) {
			v.setVisibility(View.GONE);
		}
		else {
			v.setImageResource(ids[ad]);
			v.setVisibility(View.VISIBLE);
		}
	}
	
	public void goToAd() {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (PerApp.MARKET.contains("arket"))
    		i.setData(Uri.parse("market://search?q=id:"+packages[ad]));
    	else
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p="+packages[ad]+"&showAll=1"));            		
    	context.startActivity(i);		
	}
	
	private int getNext(int start) {
		if (start >= NUM_ADS)
			start = 0;
		else
			start++;
		
		int i = start;
		
		do {
			try {
				if (context.getPackageManager().getPackageInfo(packages[i], 0) == null) {
					return i;
				}
			} catch (NameNotFoundException e) {
				return i;
			}
			i++;
			if (i >= NUM_ADS)
				i = 0;
		} while (i != start);
		
		return -1;
	}
}

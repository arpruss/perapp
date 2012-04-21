package mobi.omegacentauri.PerApp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

public class OtherApps {
	public OtherApps(final Context c, boolean always) {
		if (!always) {
			SharedPreferences p = c.getSharedPreferences("OtherApps", 0);
			int v;
			try {
				v = c.getPackageManager()
					.getPackageInfo(c.getPackageName(),0).versionCode;
			} catch (NameNotFoundException e) {
				v = 0;
			}
			if (p.getInt("version", 0) == v) {
				return;
			}
			SharedPreferences.Editor ed = p.edit();
			ed.putInt("version", v);
			ed.commit();
		}
		
        AlertDialog alertDialog = new AlertDialog.Builder(c).create();

        alertDialog.setTitle("Other applications?");
        
        alertDialog.setMessage("Do you wish to visit the Android Market "+
        		"to find other applications from Omega Centauri Software?  You will "+
        		"be able to return to PerApp with the BACK button.  (You will "+
        		"only be asked this once when you install a new version, but you "+
        		"can always come back to this option by pulling up the menu.)");
        
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"See other apps", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	Intent i = new Intent(Intent.ACTION_VIEW);
            	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	i.setData(Uri.parse("market://search?q=pub:\"Omega Centauri Software\""));
            	c.startActivity(i);
            } });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"Not now", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.show();				
	}
}

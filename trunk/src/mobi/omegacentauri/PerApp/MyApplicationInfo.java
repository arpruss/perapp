package mobi.omegacentauri.PerApp;

import java.util.Comparator;
import java.util.Locale;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class MyApplicationInfo {
	public static final Comparator<MyApplicationInfo> LabelComparator = 
		new Comparator<MyApplicationInfo>() {

		public int compare(MyApplicationInfo a, MyApplicationInfo b) {
//			Log.v("DoublePower", a.component+" "+b.component);
			if (a.component.startsWith(" ")) {
				if (b.component.startsWith(" ")) {
					return a.label.compareToIgnoreCase(b.label);
				}
				else {
					return -1;
				}
			}
			else if (b.component.startsWith(" ")) {
				return 1;
			}
			else {
				return a.label.compareToIgnoreCase(b.label);
			}
		}
	};
	
	public static final String DEFAULT = " default";
	private String label;
	private String component;
	private int versionCode;
	private int uid;
	public String packageName;
	
	String getKey() {
		return Locale.getDefault().toString() + "." + uid + "." + versionCode + "." + component;
	}
	
	public String getComponent() {
		return component;
	}
	
	public MyApplicationInfo(String command) {
		packageName = command;
		component = command;
		versionCode = 0;
		uid = 0;
		if (command == DEFAULT) {
			 label = "Defaults";
		} 
	}
	
	public MyApplicationInfo(MyCache cache, PackageManager pm, ResolveInfo r) {
		packageName = r.activityInfo.packageName;
		component = (new ComponentName(packageName, r.activityInfo.name)).flattenToString();
		uid = r.activityInfo.applicationInfo.uid;
		
		try {
			versionCode = (pm.getPackageInfo(packageName, 0)).versionCode;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			versionCode = 0;
		}
		
		if (cache != null) {
			String cached = cache.lookup(getKey());
			if (cached != null) {
				label = cached;
				return;
			}
		}
		
		CharSequence l = r.activityInfo.loadLabel(pm); 
		if (l == null) {
			label = component;
		}
		else {			
			label = (String)l;
			if (label.equals("Angry Birds")) {
				if(packageName.startsWith("com.rovio.angrybirdsrio")) {
					label = label + " Rio";
				}
				else if (packageName.startsWith("com.rovio.angrybirdsseasons")) {
					label = label + " Seasons";
				}
			}
			if (cache != null)
				cache.add(getKey(), label);
		}
	}
	
	public String getLabel() {
		return label;
	}
}


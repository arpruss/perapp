package mobi.omegacentauri.PerApp;

import java.util.ArrayList;
import java.util.Map;

import android.widget.CheckBox;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class GetApps extends AsyncTask<Void, Integer, List<MyApplicationInfo>> {
	final PackageManager pm;
	final Context	 context;
	final ListView listView;
	final Setting[] settings;
	public final static String cachePath = "app_labels"; 
	ProgressDialog progress;
	
	GetApps(Context c, ListView lv, Setting[] settings) {
		context = c;
		this.settings = settings;
		pm = context.getPackageManager();
		listView = lv;
	}

	private boolean profilable(ApplicationInfo a) {
		return true;
	}

	@Override
	protected List<MyApplicationInfo> doInBackground(Void... c) {
		Log.v("getting", "installed");
		
		Intent launchIntent = new Intent(Intent.ACTION_MAIN);
		launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		
		List<ResolveInfo> list = 
			pm.queryIntentActivities(launchIntent, 0);
		
		List<MyApplicationInfo> myList = new ArrayList<MyApplicationInfo>();
		
		MyCache cache = new MyCache(MyCache.genFilename(context, cachePath));
		
		for (int i = 0 ; i < list.size() ; i++) {
			publishProgress(i, list.size());
			MyApplicationInfo myAppInfo;
			myAppInfo = new MyApplicationInfo(
					cache, pm, list.get(i));
			if (myAppInfo.packageName != context.getPackageName())
				myList.add(myAppInfo);
		}
		cache.commit();
		
		publishProgress(list.size(), list.size());
		
		myList.add(new MyApplicationInfo(MyApplicationInfo.DEFAULT));

		return myList;
	}
	
	@Override
	protected void onPreExecute() {
//		listView.setVisibility(View.GONE);
		progress = new ProgressDialog(context);
		progress.setCancelable(false);
		progress.setMessage("Getting applications...");
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setIndeterminate(true);
		progress.show();
	}
	
	protected void onProgressUpdate(Integer... p) {
		progress.setIndeterminate(false);
		progress.setMax(p[1]);
		progress.setProgress(p[0]);
	}
	
	@Override
	protected void onPostExecute(final List<MyApplicationInfo> appInfo) {
		
		ArrayAdapter<MyApplicationInfo> appInfoAdapter = 
			new ArrayAdapter<MyApplicationInfo>(context, 
					android.R.layout.simple_list_item_2, 
					appInfo) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				
				
				if (convertView == null) {
	                v = View.inflate(context, android.R.layout.simple_list_item_2, null);
	            }
				else {
					v = convertView;
				}

				final MyApplicationInfo a = appInfo.get(position);
				TextView tv = ((TextView)v.findViewById(android.R.id.text1));
				tv.setText(a.getLabel());
				if (a.packageName.equals(MyApplicationInfo.DEFAULT)) {
					tv.setTypeface(Typeface.DEFAULT_BOLD);
				}
				else {
					tv.setTypeface(Typeface.DEFAULT);
				}
				tv = ((TextView)v.findViewById(android.R.id.text2));
				String settingsInfo = "";
				for (int i=0; i<settings.length; i++) {
					if (a.packageName.equals(MyApplicationInfo.DEFAULT) ||
						settings[i].getMode(a.packageName) == Setting.SET) {
						if (settingsInfo.length() > 0)
							settingsInfo += ", ";
						settingsInfo += settings[i].describeForList(a.packageName);
					}
				}
				tv.setText(settingsInfo);
				return v;
			}				
		};
		
		appInfoAdapter.sort(MyApplicationInfo.LabelComparator);
		listView.setAdapter(appInfoAdapter);
		
		try {
			progress.dismiss();
		} 
		catch (IllegalArgumentException e) {			
		}
//		listView.setVisibility(View.VISIBLE);
	}
}

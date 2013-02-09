package mobi.omegacentauri.PerApp;

import java.util.List;

import mobi.omegacentauri.PerApp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.provider.Contacts.Settings;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SpinnerAdapter;

public class IMESetting extends Setting {
	static final String ID = "IMESetting";
	private InputMethodManager imm;
	Context context;
	String stringValue;
	
	public IMESetting(Context context, SharedPreferences pref) {
		super(context, pref);
		
		this.context = context;
		name = "Input Method";
		id = ID;
		defaultValue = "(unchanged)";
		imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE); 
	}
	
	@Override
	protected void parse(String s) {
		stringValue = s;
	}
	
	@Override
	protected String unparse() {
		return stringValue;
	}
	
	@Override
	public void dialog(PerApp activity, final String app) {
		PerApp.log("dialog");
		PackageManager pm = activity.getPackageManager();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		View v = getDialogView(activity, builder, R.layout.ime_setting, app); 
		
		Spinner spin = (Spinner)v.findViewById(R.id.ime_spinner);
		final List<InputMethodInfo> inputMethods = imm.getEnabledInputMethodList();
		String[] labels = new String[inputMethods.size()];
		int index = -1;
		
		for (int i=0; i<labels.length; i++) {
			labels[i] = getLabel(pm, inputMethods.get(i).getPackageName());
			if (inputMethods.get(i).getId().equals(stringValue))
				index = i;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, 
				android.R.layout.simple_spinner_item, labels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(adapter);
		spin.setSelection(index);
		spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int index, long arg3) {
				IMESetting.this.stringValue = inputMethods.get(index).getId();
				saveCustom(app);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		builder.create().show();
	}
	
	private String getLabel(PackageManager pm, String packageName) {
		try {
		CharSequence s = pm.getPackageInfo(packageName, 0).applicationInfo.loadLabel(pm);
		    return (String)s;
		}
		catch(Exception e) {
            return packageName;
		}
	}

	@Override
	protected void set() {
		if (stringValue.length() == 0 || stringValue.equals("(unchanged)"))
			return;
		if (Secure.getString(context.getContentResolver(), Secure.DEFAULT_INPUT_METHOD).equals(stringValue))
			return;
		Root.runOne("ime set "+stringValue);
	}
	
	@Override
	protected String describeValue() {
		PackageManager pm = context.getPackageManager();
		List<InputMethodInfo> inputMethods = imm.getEnabledInputMethodList();
		
		for (int i=0; i<inputMethods.size(); i++) 
			if (inputMethods.get(i).getId().equals(stringValue))
				return getLabel(pm, inputMethods.get(i).getPackageName());

		return stringValue;
	}
	
	@Override
	protected boolean defaultActive() {
		return true;
	}
	
}

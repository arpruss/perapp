package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;
	private static final boolean LOG_SU = false;
	public boolean valid;
	
	public Root() {
		this(false);
	}

	public Root(boolean output) {
		try {
			if (output) {
				rootShell = Runtime.getRuntime().exec("su");
			}
			else {
				String[] cmds = { "sh", "-c", 
				LOG_SU ?		
				"su >> /tmp/GalacticNight.txt 2>> /tmp/GalacticNight.txt" 
				: "su > /dev/null 2> /dev/null" 		
				};
				rootShell = Runtime.getRuntime().exec(cmds);
			}
			
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
			valid = true;
		}
		catch (Exception e) {
			rootCommands = null;
			valid = false;
		}
	}
	
	public static boolean test() {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public static boolean runOne(String cmd) {
		try {
			String[] cmds = { "sh", "-c", 
					LOG_SU ?		
					"su >> /tmp/GalacticNight.txt 2>> /tmp/GalacticNight.txt" 
					: "su > /dev/null 2> /dev/null" 		
					};
			Process p = Runtime.getRuntime().exec(cmds);

			DataOutputStream shell = new DataOutputStream(p.getOutputStream());
			Log.v("root", cmd);
			shell.writeBytes(cmd + "\n");
			shell.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch(Exception e) {
			Log.e("root", ""+e);
			return false;
		}
	}
	
	public void close() {
		if (rootCommands != null) {
			try {
				rootCommands.close();
			}
			catch (Exception e) {
			}
			rootCommands = null;
		}
	}
	
	public void exec( String s ) {
		try {
			Log.v("root", s);
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
		}
		catch (Exception e) {
			Log.e("Error executing",s);
		}
	}
}

package mobi.omegacentauri.PerApp;

import java.io.DataOutputStream;

import android.content.Context;


public class RunAsRoot {
	public static void runAsRoot(Context context, String commandline) {
		Root.runOne("LD_LIBRARY_PATH=/vendor/lib:/system/lib "+
				"CLASSPATH=\""+context.getPackageCodePath()+"\" app_process . "+
				RunAsRoot.class.getName()+" "+commandline);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error: No arguments");
			return;
		}
		if (args[0].equalsIgnoreCase("gps")) {
			if (args.length < 2) {
				System.out.println("Error: Too few arguments");
				return;
			}
			int v;
			try {
				v = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException e) {
				System.out.println("Error: Invalid argument");
				return;
			}
			GPSSetting.setGPS(v!=0);
		}
	}		
}

package mobi.omegacentauri.PerApp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class CPUUtils {
	public static final String DIR = "/sys/devices/system/cpu/cpu0/cpufreq/";
	
	public static int[] getSpeeds() {
		int minFreq = get1Value("cpuinfo_min_freq");
		int maxFreq = get1Value("cpuinfo_max_freq");
		ArrayList<Integer> values = new ArrayList<Integer>();
		if (minFreq > 0 && minFreq < maxFreq);
			values.add(minFreq);
		if (maxFreq > 0)
			values.add(maxFreq);
		if (values.size() == 0)
			return null;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(DIR + "stats/time_in_state")));
			String line;
			
			while (null != (line = reader.readLine())) {
				Scanner scanner = new Scanner(line);
				try {
					int value = scanner.nextInt();
					if (0 < value) {
						if (! values.contains(value))
							values.add(value);
					}
				}
				catch (Exception e) {
				}
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		
		Collections.sort(values);
		
		int[] v = new int[values.size()];
		for (int i=0; i<v.length; i++)
			v[i] = values.get(i);

		return v;
	}

	public static int get1Value(String filename) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(new File(DIR + filename)));
			String line = reader.readLine();
			if (line == null)
				return -1;
			return Integer.parseInt(line);
		} catch (Exception e) {
			return -1;
		}
	}
	
	public static boolean rootMakeWriteable(String path) {
		try {
			Process p = Runtime.getRuntime().exec(new String[] {"su", "-c", "chmod", "666", path});
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

	public static void writeValue(String filename, int value) {
		File f = new File(DIR + filename);
		if (!f.canWrite() && !rootMakeWriteable(filename)) {
			PerApp.log("Error making "+filename+" writeable");
			return;
		}
		try {
			FileWriter w = new FileWriter(f);
			w.write(""+value+"\n");
			w.close();
			PerApp.log("Wrote "+value+" to "+f);
		} catch (IOException e) {
			PerApp.log("Error writing to "+filename);
		}
	}
}

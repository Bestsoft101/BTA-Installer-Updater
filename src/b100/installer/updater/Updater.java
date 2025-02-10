package b100.installer.updater;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Updater {
	
	private static boolean offline = false;
	private static File installerDirectory = null;
	
	public static boolean isOffline() {
		return offline;
	}
	
	public static Map<String, String> queryLatestVersion() {
		JFrame frame = Utils.createTextFrame("Checking for updates...");
		
		long startTime = System.currentTimeMillis();
		
		Map<String, String> properties = null;
		
		if(!offline) {
			String url = "https://raw.githubusercontent.com/Bestsoft101/BTA-Installer-Updater/main/src/latest.info";
			File file = new File(installerDirectory, "latest.info");
			Utils.downloadFileAndPrintProgress(url, file);
			properties = ConfigUtil.loadPropertiesFile(file, '=');
		}else {
			properties = ConfigUtil.loadPropertiesInternal("/latest.info", '=');
		}
		
		// Give the user a moment to actually read the text
		long endTime = System.currentTimeMillis();
		long delta = endTime - startTime;
		System.out.println("Update check took " + delta + " ms");
		long sleepTime = 500 - delta;
		if(sleepTime > 0) {
			System.out.println("Sleep for " + sleepTime + " ms");
			try {
				Thread.sleep(sleepTime);
			}catch (Exception e) {}
		}
		
		frame.dispose();
		
		return properties;
	}
	
	public static void launch() {
		System.out.println("Launch!");
		Map<String, String> current = ConfigUtil.loadPropertiesFile(new File(installerDirectory, "version.info"), '=');
		
		File file = new File(installerDirectory, current.get("filename"));
		System.out.println("Launch: " + file.getAbsolutePath());
		
		ClassLoader classLoader = null;
		try {
			URL[] urls = new URL[1];
			urls[0] = new URL("jar:file:" + file.getAbsolutePath() + "!/");
			classLoader = new URLClassLoader(urls);
			// TODO read main class from META-INF
			Class<?> mainClass = classLoader.loadClass("b100.installer.gui.InstallerGUI");
			Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
			Object instance = null;
			String[] stringArgs = new String[0];
			Object[] methodArgs = new Object[] {stringArgs};
			mainMethod.invoke(instance, methodArgs);
		}catch (Throwable e) {
			// TODO error handling
			e.printStackTrace();
		}
	}
	
	public static void main(String... args) {
		Utils.setSystemStyle();
		
		// Process args
		for(int i=0; i < args.length; i++) {
			if(args[i].equals("--offline")) {
				offline = true;
			}else if(args[i].equals("--dir")) {
				installerDirectory = new File(args[++i]);
			}
		}
		
		if(offline) {
			System.out.println("Running in Offline Mode!");
		}
		
		if(installerDirectory == null) {
			installerDirectory = Utils.getAppDirectory("bta-installer");
		}
		System.out.println("Installer Directory: " + installerDirectory.getAbsolutePath());
		
		// Get latest version info
		Map<String, String> latest = queryLatestVersion();
		String latestVersion = latest.get("latest");
		System.out.println("Latest Version: " + latest.get("version"));
		
		// Read current version info
		Map<String, String> current = null;
		File currentVersionInfoFile = new File(installerDirectory, "version.info");
		if(currentVersionInfoFile.exists()) {
			current = ConfigUtil.loadPropertiesFile(currentVersionInfoFile, '=');
		}
		boolean update;
		if(current != null) {
			String currentVersion = current.get("version");
			System.out.println("Current version: " + currentVersion);
			update = !currentVersion.equals(latestVersion);
		}else {
			System.out.println("No installed version found!");
			update = true;
		}
		if(!update) {
			System.out.println("No update required!");
			launch();
			return;
		}
		
		// Update
		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		JProgressBar progressBar = new JProgressBar();
		panel.add(new JLabel("Downloading Update..."));
		panel.add(progressBar);
		frame.add(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		
		String filename = "installer.jar";
		Utils.downloadFileAndPrintProgress(latest.get("url"), new File(installerDirectory, filename));
		
		current = new HashMap<>();
		current.put("filename", filename);
		current.put("version", latestVersion);
		ConfigUtil.saveProperties(new File(installerDirectory, "version.info"), current, '=');
		
		frame.dispose();
		
		launch();
	}
}

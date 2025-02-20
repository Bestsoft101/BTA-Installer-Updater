package b100.installer.updater;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JProgressBar;

import b100.installer.updater.Utils.JarFileInfo;
import b100.installer.updater.Utils.ProgressListener;
import net.minecraft.client.Minecraft;

public class Updater {
	
	private static boolean offline = false;
	private static File installerDirectory = null;
	private static File logFile;
	private static Window window;
	private static File installerFileOverride = null;
	private static File localVersionFile;
	
	public static boolean isOffline() {
		return offline;
	}
	
	public static Map<String, String> queryLatestVersion() {
		long startTime = System.currentTimeMillis();
		
		Map<String, String> properties = null;
		
		if(!offline) {
			String url = "https://raw.githubusercontent.com/Bestsoft101/BTA-Installer-Updater/main/latest.info";
			File file = new File(installerDirectory, "latest.info");
			Utils.downloadFileAndPrintProgress(url, file, null);
			properties = ConfigUtil.loadPropertiesFile(file, '=');
		}else {
			properties = ConfigUtil.loadPropertiesFile(localVersionFile, '=');
		}
		
		long endTime = System.currentTimeMillis();
		long delta = endTime - startTime;
		System.out.println("Update check took " + delta + " ms");
		long sleepTime = 500 - delta;
		if(sleepTime > 0) {
			System.out.println("Sleep for " + sleepTime + " ms");
			Utils.sleep(sleepTime);
		}
		
		return properties;
	}
	
	public static void launchInstaller() {
		System.out.println("Launch Installer!");
		
		File versionsInfoFile = new File(installerDirectory, "version.info");
		if(!versionsInfoFile.exists()) {
			throw new RuntimeException("No installed version available to launch!");
		}
		
		Map<String, String> current = ConfigUtil.loadPropertiesFile(versionsInfoFile, '=');
		
		String filename = current.get("filename");
		if(filename == null) {
			throw new RuntimeException("Missing filename in " + versionsInfoFile.getName() + "!");
		}
		
		File file = new File(installerDirectory, filename);
		if(installerFileOverride != null) {
			file = installerFileOverride;
		}
		System.out.println("Launch: " + file.getAbsolutePath());
		window.setText("Launching installer");
		
		URLClassLoader classLoader = null;
		try {
			if(!file.exists()) {
				throw new RuntimeException("File does not exist: " + file.getAbsolutePath() + "!");
			}
			
			JarFileInfo jarFileInfo = Utils.readJarFile(file);
			
			classLoader = new URLClassLoader(new URL[] {new URL("jar:file:" + file.getAbsolutePath() + "!/")});
			
			for(String className : jarFileInfo.allClassNames) {
				classLoader.loadClass(className);
			}
			
			Class<?> mainClass = classLoader.loadClass(jarFileInfo.mainClass);
			System.out.println("Main Class: " + mainClass);
			
			Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
			String[] stringArgs = new String[] {
				"--run-directory",
				installerDirectory.getAbsolutePath()
			};
			
			window.dispose();
			
			try {
				Log.disable();
				mainMethod.invoke(null, new Object[] {stringArgs});	
			}catch (Throwable e) {
				Log.enable();
				throw e;
			}
			
			Log.close();
		}catch (Throwable e) {
			throw new RuntimeException("Couldn't launch!", e);
		}finally {
			try {
				classLoader.close();
			}catch (Exception e) {}
		}
	}
	
	/**
	 * @return false if the window has been closed, otherwise true
	 */
	public static boolean run() {
		// Get latest version info
		Map<String, String> latest = null;
		try {
			latest = queryLatestVersion();	
		}catch (Exception e) {
			e.printStackTrace();
			System.err.println("Update check failed!");
			window.setText("Update check failed!");
			Utils.sleep(2500);
			
			if(window.isClosed()) {
				return false;
			}
			
			launchInstaller();
			return true;
		}
		
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
			
			File file = new File(installerDirectory, current.get("filename"));
			if(file.exists()) {
				update = !currentVersion.equals(latestVersion);	
			}else {
				System.out.println("Installer file is missing!");
				update = true;
			}
		}else {
			System.out.println("No installed version found!");
			update = true;
		}

		if(window.isClosed()) {
			return false;
		}
		
		if(!update) {
			System.out.println("No update required!");
			launchInstaller();
			return true;
		}
		
		if(window.isClosed()) {
			return false;
		}
		
		JProgressBar progressBar = window.setText("Downloading update...");
		
		ProgressListener progressListener = progress -> {
			progressBar.setValue((int) (progress * 100));
		};
		
		String filename = latest.get("filename");
		if(filename == null) {
			filename = "installer.jar";
		}
		
		Utils.downloadFileAndPrintProgress(latest.get("url"), new File(installerDirectory, filename), progressListener);
		
		current = new HashMap<>();
		current.put("filename", filename);
		current.put("version", latestVersion);
		ConfigUtil.saveProperties(new File(installerDirectory, "version.info"), current, '=');
		
		if(window.isClosed()) {
			return false;
		}
		
		launchInstaller();
		return true;
	}
	
	public static void main(String... args) {
		Utils.setSystemStyle();
		
		File multiMcDirectory = Minecraft.getMultiMcDirectory();
		if(multiMcDirectory != null) {
			if(multiMcDirectory.getName().equals(".minecraft")) {
				File parent = multiMcDirectory.getAbsoluteFile().getParentFile();
				if(parent != null) {
					installerDirectory = new File(parent, ".installer");
				}else {
					installerDirectory = multiMcDirectory;
				}
			}
		}
		
		// Process args
		for(int i=0; i < args.length; i++) {
			if(args[i].equals("--offline")) {
				offline = true;
			}else if(args[i].equals("--dir")) {
				installerDirectory = new File(args[++i]);
			}else if(args[i].equals("--installerfile")) {
				installerFileOverride = new File(args[++i]);
			}
		}
		
		if(installerDirectory == null) {
			installerDirectory = Utils.getAppDirectory("bta-installer");
		}
		
		logFile = new File(installerDirectory, "updater.log");
		Log.setup(logFile);
		Log.enable();
		
		System.out.println("Installer directory: " + installerDirectory.getAbsolutePath());
		
		if(installerFileOverride != null) {
			System.out.println("Using installer file: " + installerFileOverride.getAbsolutePath());
		}
		if(offline) {
			localVersionFile = new File(installerDirectory, "latest.info");
			
			System.out.println("Running in offline mode!");
			System.out.println("Version check will use local file: " + localVersionFile.getAbsolutePath());
		}
		
		try {
			window = new Window("Checking for update...");
			
			if(!run()) {
				System.out.println("Window was closed!");
			}
			
			window.dispose();
		}catch (Exception e) {
			e.printStackTrace();
			
			try {
				window.dispose();
			}catch (Exception e1) {}
			
			StringBuilder msg = new StringBuilder();
			
			msg.append("The updater has crashed!\n\n");
			
			CrashHandler.createErrorLog(msg, e);
			
			msg.append("\n\nThe full log has been saved at " + logFile.getAbsolutePath());
			
			new CrashHandler(msg.toString(), window.getFrame());
		}
	}
}

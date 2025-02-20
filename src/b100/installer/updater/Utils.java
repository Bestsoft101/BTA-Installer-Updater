package b100.installer.updater;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class Utils {
	
	public static final int OS_WINDOWS = 0;
	public static final int OS_MAC = 1;
	public static final int OS_LINUX = 2;
	public static final int OS_UNKNOWN = 3;
	
	public static File getAppDirectory(String appName) {
		int operatingSystem = OS_UNKNOWN;
		
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("win")) operatingSystem = OS_WINDOWS;
		if(osName.contains("mac")) operatingSystem = OS_MAC;
		if(osName.contains("linux") || osName.contains("unix") || osName.contains("sunos") || osName.contains("solaris")) operatingSystem = OS_LINUX;
		
		File appDir;
		String userHome = System.getProperty("user.home", ".");
		
		if(operatingSystem == OS_LINUX) {
			appDir = new File(userHome, "." + appName + "/");
		}else if(operatingSystem == OS_WINDOWS) {
			String appdata = System.getenv("APPDATA");
			if(appdata != null) {
				appDir = new File(appdata, "." + appName + "/");
			}else {
				appDir = new File(userHome, "." + appName + "/");
			}
		}else if(operatingSystem == OS_MAC) {
			appDir = new File(userHome, "Library/Application Support/" + appName + "/");
		}else {
			appDir = new File(userHome, appName + "/");
		}
		
		return appDir;
	}
	
	public static void setSystemStyle() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static JFrame createTextFrame(String text) {
		JFrame frame = new JFrame();
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		int padding = 8;
		c.insets = new Insets(padding, padding, padding, padding);
		
		panel.add(new JLabel(text), c);
		frame.add(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		return frame;
	}
	
	public static void downloadFileAndPrintProgress(String url, File file, ProgressListener progressListener) {
		file = file.getAbsoluteFile();
		File parent = file.getParentFile();
		if(!parent.exists()) {
			parent.mkdirs();
		}
		
		HttpURLConnection connection = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			
			System.out.println("Downloading: " + connection.getURL());
			
			long completeFileSize = connection.getContentLengthLong();
			long downloadedFileSize = 0;
			long lastPrint = 0;
			
			in = new BufferedInputStream(connection.getInputStream());
			out = new BufferedOutputStream(new FileOutputStream(file));
			
			byte[] cache = new byte[4096];
			
			while(true) {
				int read = in.read(cache, 0, cache.length);
				if(read == -1) {
					break;
				}
				downloadedFileSize += read;
				out.write(cache, 0, read);

				float progress = (float) (downloadedFileSize / (double) completeFileSize);
				
				long now = System.currentTimeMillis();
				if(now > lastPrint + 500) {
					lastPrint = now;
					
					int percent = (int) (progress * 100);
					System.out.println("Downloading: " + percent + "%");
				}
				
				if(progressListener != null) {
					progressListener.progressChanged(progress);
				}
			}
			
			System.out.println("Finished Downloading!");
			if(progressListener != null) {
				progressListener.progressChanged(1.0f);
			}
		}catch (Exception e) {
			throw new RuntimeException("Error downloading file from '" + url + "' to '" + file.getAbsolutePath() + "'!", e);
		}finally {
			try {
				connection.disconnect();
			}catch (Exception e) {}
			try {
				in.close();
			}catch (Exception e) {}
			try {
				out.close();
			}catch (Exception e) {}
		}
	}
	
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		}catch (Exception e) {}
	}
	
	public static JarFileInfo readJarFile(File file) {
		JarFileInfo jarFileInfo = new JarFileInfo();
		jarFileInfo.allClassNames = new ArrayList<>();
		
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);

			// Find all class names
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				if(entry.getName().toLowerCase().endsWith(".class")) {
					String className = entry.getName().replace('/', '.');
					className = className.substring(0, className.length() - 6);
					jarFileInfo.allClassNames.add(className);
				}
			}
			
			// Find main class
			ZipEntry entry = zipFile.getEntry("META-INF/MANIFEST.MF");
			if(entry == null) {
				throw new RuntimeException("Jar file is missing manifest!");
			}
			InputStream in = null;
			try {
				in = zipFile.getInputStream(entry);
				
				Map<String, String> manifestData = ConfigUtil.loadProperties(in, ':');
				String mainClass = manifestData.get("Main-Class");
				if(mainClass == null) {
					throw new RuntimeException("Manifest is missing main class!");
				}
				
				jarFileInfo.mainClass = mainClass.trim();
			}finally {
				in.close();
			}
		}catch (Exception e) {
			throw new RuntimeException("Could not read main class: " + file.getAbsolutePath(), e);
		}finally {
			try {
				zipFile.close();
			}catch (Exception e) {}
		}
		
		return jarFileInfo;
	}
	
	public static class JarFileInfo {
		public List<String> allClassNames;
		public String mainClass;
	}
	
	public static interface ProgressListener {
		
		public void progressChanged(float progress);
		
	}
	
}

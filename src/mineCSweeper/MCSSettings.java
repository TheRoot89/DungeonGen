package mineCSweeper;

import java.io.File;

public class MCSSettings {
	private final static String configFile = "MCSConfig.yml";
	private File settingsFile;
	private boolean loadedSuccessfully;
	
	
	public static MCSSettings getSettingsHandler(File pluginDirectory ) {
		MCSSettings settings = new MCSSettings();
		try {
	        settings.settingsFile = new File(pluginDirectory, configFile);
	        if (!settings.settingsFile.exists()) {
	            settings.saveConfig();
	            settings.loadedSuccessfully = true;
	        }else {
	        	settings.loadConfig();
	        	settings.loadedSuccessfully = true;
	        }
		} catch (Exception e) {
			e.printStackTrace();
			settings.loadedSuccessfully = false;
		}
		return settings;
	
	}

	private void loadConfig() {
		// TODO Auto-generated method stub
		
	}

	
	
	private void saveConfig() {
		// TODO Auto-generated method stub
		
	}
	
	public void setOption(String key, String value) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isLoadedSuccessfully() {
		return loadedSuccessfully;
	}
	
	public boolean hasOptionKey(String key) {
		// TODO check for all option keys
		return false;
	}
}

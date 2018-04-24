package mineCSweeper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MCSSettings {
	private final static String configFile = "MCSConfig.yml";
	private File settingsFile;
	private boolean loadedSuccessfully;
	private Hashtable<String, Integer> integerSettings;
	private Hashtable<String, Boolean> booleanSettings;
	
	public static MCSSettings getSettingsHandler(File pluginDirectory ) {
		MCSSettings settings = new MCSSettings();
		settings.integerSettings = new Hashtable<>();
		settings.booleanSettings = new Hashtable<>();
		
		// ######### Default settings #########
		settings.integerSettings.put("bombCount", 10);
		settings.integerSettings.put("boardWidth", 10);
		settings.integerSettings.put("boardHeight", 10);
		settings.booleanSettings.put("safeBombFlags", false);
		
		try {
	        settings.settingsFile = new File(pluginDirectory, configFile);
	        if (!settings.settingsFile.exists()) {
	            settings.createConfig();
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

	private void createConfig() throws IOException {
		FileConfiguration newConfig = new YamlConfiguration();
		for (String key : integerSettings.keySet()) {
			newConfig.set(key, integerSettings.get(key));
		}
		for (String key : booleanSettings.keySet()) {
			newConfig.set(key, booleanSettings.get(key));
		}
		newConfig.save(settingsFile);
	}

	private void loadConfig() throws FileNotFoundException, IOException, InvalidConfigurationException {
		FileConfiguration config = new YamlConfiguration();
		config.load(settingsFile);
		for (String key : integerSettings.keySet()) {
			if (config.contains(key)) integerSettings.put(key, config.getInt(key));
		}
		for (String key : booleanSettings.keySet()) {
			if (config.contains(key)) booleanSettings.put(key, config.getBoolean(key));
		}
	}

	
	
	boolean saveConfig() { //visib. is "package" by declaring none in java
		try {
			createConfig();
			return true;
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean isLoadedSuccessfully() {
		return loadedSuccessfully;
	}
	
	public boolean hasOptionKey(String key) {
		return (integerSettings.containsKey(key) || booleanSettings.containsKey(key));
	}
	
	public boolean setOption(String key, String value) {
		
		// Handle integer type options:
		if (integerSettings.containsKey(key)) {
			try {
				Integer intValue = Integer.parseInt(value);
				integerSettings.put(key, intValue);
				return true;
			}catch (NumberFormatException e) {
				// TODO: handle exception
			}
		}
		
		// Handle bool type options:
		if (booleanSettings.containsKey(key)) {
			Boolean boolValue;
			if (value == "1") {
				boolValue = true;
			}else if (value == "0") {
				boolValue = false;
			}else {
				boolValue = Boolean.parseBoolean(value);
				booleanSettings.put(key, boolValue);
			}
			return true;
		}
		
		return false;
	}

	public String[] getCurrentSettingsAsStringList() {
		String[] settingsList = new String[integerSettings.size() + booleanSettings.size()];
		int i = 0;
		for (String key : integerSettings.keySet()) {
			settingsList[i] = key + ": " + integerSettings.get(key).toString();
			i++;
		}
		for (String key : booleanSettings.keySet()) {
			settingsList[i] = key + ": " + booleanSettings.get(key).toString();
			i++;
		}
		
		return settingsList;
	}

	public File getSettingsFile() {
		return settingsFile;
	}
}

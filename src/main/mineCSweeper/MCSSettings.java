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
	private Hashtable<Key, Integer> integerSettings;
	private Hashtable<Key, Boolean> booleanSettings;
	
	// All possible settings, add new setting here:
	public enum Key{
		BOMBCOUNT,
		BOARDWIDTH,
		BOARDHEIGHT,
		BOARDSPAWNDIST,
		SAFEBOMBFLAGS
	}
	
	
	public static MCSSettings getSettingsHandler(File pluginDirectory ) throws MCSException{
		MCSSettings settings = new MCSSettings();
		settings.integerSettings = new Hashtable<>();
		settings.booleanSettings = new Hashtable<>();
		
		// ######### Default settings, init new settings here #########
		settings.integerSettings.put(Key.BOMBCOUNT, 10);
		settings.integerSettings.put(Key.BOARDWIDTH, 10);
		settings.integerSettings.put(Key.BOARDHEIGHT, 10);
		settings.integerSettings.put(Key.BOARDSPAWNDIST, 2);
		settings.booleanSettings.put(Key.SAFEBOMBFLAGS, false);
		
		try {
	        settings.settingsFile = new File(pluginDirectory, configFile);
	        if (!settings.settingsFile.exists()) {
	            settings.createConfig();
	        }else {
	        	settings.loadConfig();
	        }
		} catch (Exception e) {
			e.printStackTrace();
			throw new MCSException(e.getMessage());
		}
		return settings;
	
	}

	private void createConfig() throws IOException {
		FileConfiguration newConfig = new YamlConfiguration();
		for (Key key : integerSettings.keySet()) {
			newConfig.set(key.toString(), integerSettings.get(key));
		}
		for (Key key : booleanSettings.keySet()) {
			newConfig.set(key.toString(), booleanSettings.get(key));
		}
		newConfig.save(settingsFile);
	}

	private void loadConfig() throws FileNotFoundException, IOException, InvalidConfigurationException {
		FileConfiguration config = new YamlConfiguration();
		config.load(settingsFile);
		for (Key key : integerSettings.keySet()) {
			if (config.contains(key.toString()))
				integerSettings.put(key, config.getInt(key.toString()));
		}
		for (Key key : booleanSettings.keySet()) {
			if (config.contains(key.toString()))
				booleanSettings.put(key, config.getBoolean(key.toString()));
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
	
	
	public boolean hasOptionKey(Key key) {
		return (integerSettings.containsKey(key) || booleanSettings.containsKey(key));
	}
	
	public void setOption(String keyAsString, String value) throws MCSException{
		// Parse key string:
		keyAsString = keyAsString.toUpperCase();
		Key key;
		try {
			key = Key.valueOf(keyAsString);
		} catch (IllegalArgumentException e) {
			throw new MCSException("No such option key: " + keyAsString);
		}
		
		// Handle integer type options:
		if (integerSettings.containsKey(key)) {
			try {
				Integer intValue = Integer.parseInt(value);
				integerSettings.put(key, intValue);
				return;
			}catch (NumberFormatException e) {
				throw new MCSException(e.getMessage());
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
			return;
		}
		
		throw new MCSException("TILT: Unknown Options should have been handled above!");
	}

	public String[] getCurrentSettingsAsStringList() {
		String[] settingsList = new String[integerSettings.size() + booleanSettings.size()];
		int i = 0;
		for (Key key : integerSettings.keySet()) {
			settingsList[i] = key.toString() + ": " + integerSettings.get(key).toString();
			i++;
		}
		for (Key key : booleanSettings.keySet()) {
			settingsList[i] = key.toString() + ": " + booleanSettings.get(key).toString();
			i++;
		}
		
		return settingsList;
	}

	public File getSettingsFile() {
		return settingsFile;
	}
	
	public int getIntegerSetting(Key key){
		if (integerSettings.containsKey(key)) {
			return integerSettings.get(key);
		} else {
			return 0;
		}
	}
	
	public boolean getBooleanSetting(Key key){
		if (booleanSettings.containsKey(key)) {
			return booleanSettings.get(key);
		} else {
			return false;
		}
	}
}

package hardcoreRevive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import mcPluginHelpers.MsgLevel;


public class HCRsettings {
	private static HCRsettings instance = null;
	private final static String fileName = "HCRsettings.yml";
	
	private File file;
	private Hashtable<Key, Integer> integerSettings;
	private Hashtable<Key, Boolean> booleanSettings;
	private Hashtable<Key, String>  stringSettings;
	
	/// All possible settings, add new setting here:
	/// These are used as the tag names as well
	public enum Key{
		RESPAWN_DIST,
		RESPAWN_RAND_OFFSET_MAX,
		RESPAWN_MAX_TRIES,
		OFFER_MATERIAL,
		OFFER_NUM_BLOCKS,
		RESPAWN_TIME_SEC
	}
	
	
	/// Creates the save file and provides the singleton for the get() method.
	/// Called during initialization by the plugin.
	public static void init() throws Exception
	{
		if (instance != null)
		{
			HCRPlugin.get().sendMessage(MsgLevel.WARNING,"HCRsettings::init(): instance already set and init called!");
			return;
		}
		HCRsettings newInstance = new HCRsettings();
		newInstance.integerSettings = new Hashtable<>();
		newInstance.booleanSettings = new Hashtable<>();
		newInstance.stringSettings  = new Hashtable<>();
		
		// ######### Default settings, init new settings here #########
		newInstance.integerSettings.put(Key.RESPAWN_DIST, 1000);
		newInstance.integerSettings.put(Key.RESPAWN_RAND_OFFSET_MAX, 400);
		newInstance.stringSettings.put(Key.OFFER_MATERIAL, "DIAMOND_BLOCK");
		newInstance.integerSettings.put(Key.OFFER_NUM_BLOCKS, 2);
		newInstance.integerSettings.put(Key.RESPAWN_TIME_SEC, 60);
		newInstance.integerSettings.put(Key.RESPAWN_MAX_TRIES, 1000);
		
		// #############################################################
		
		try {
			newInstance.file = new File(HCRPlugin.get().getPluginDir(), fileName);
	        if (!newInstance.file.exists()) {
	        	newInstance.createFile();
	        	HCRPlugin.get().sendMessage(MsgLevel.DEBUG, "HCRsettings:init(): created new file " + fileName);
	        }else {
	        	newInstance.loadFile();
	        	HCRPlugin.get().sendMessage(MsgLevel.DEBUG, "HCRsettings::init(): loaded existing file " + fileName);
	        }
	        instance = newInstance;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	
	/// Returns the singleton instance of this class.
	public static HCRsettings get()
	{
		if (instance == null) {
			HCRPlugin.get().sendMessage(MsgLevel.ERROR,"HCRsettings::get(): instance not yet set up!");
		}
		return instance;
	}

	
	/// Initial creation of the save file or actual save by overwriting
	private void createFile() throws IOException
	{
		FileConfiguration newConfig = new YamlConfiguration();
		for (Key key : integerSettings.keySet()) {
			newConfig.set(key.toString(), integerSettings.get(key));
		}
		for (Key key : booleanSettings.keySet()) {
			newConfig.set(key.toString(), booleanSettings.get(key));
		}
		for (Key key : stringSettings.keySet()) {
			newConfig.set(key.toString(), stringSettings.get(key));
		}
		
		newConfig.save(file);
	}

	
	/// Loads our custom data structure from the file.
	private void loadFile() throws FileNotFoundException, IOException, InvalidConfigurationException
	{
		FileConfiguration config = new YamlConfiguration();
		config.load(file);
		for (Key key : integerSettings.keySet()) {
			if (config.contains(key.toString()))
				integerSettings.put(key, config.getInt(key.toString()));
		}
		for (Key key : booleanSettings.keySet()) {
			if (config.contains(key.toString()))
				booleanSettings.put(key, config.getBoolean(key.toString()));
		}
		for (Key key : stringSettings.keySet()) {
			if (config.contains(key.toString()))
				stringSettings.put(key, config.getString(key.toString()));
		}
	}
	
	
	/// visibility is "package" by declaring no scope in java
	boolean save()
	{ 
		try {
			createFile();
			return true;
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean hasOptionKey(Key key) {
		return (integerSettings.containsKey(key) || booleanSettings.containsKey(key) || stringSettings.containsKey(key));
	}
	
	
	public void setOption(String keyAsString, String value) throws Exception
	{
		// Parse key string:
		keyAsString = keyAsString.toUpperCase();
		Key key;
		try {
			key = Key.valueOf(keyAsString);
		} catch (IllegalArgumentException e) {
			throw new Exception("No such option key: " + keyAsString);
		}
		
		// Handle integer type options:
		if (integerSettings.containsKey(key)) {
			try {
				Integer intValue = Integer.parseInt(value);
				integerSettings.put(key, intValue);
				return;
			}catch (NumberFormatException e) {
				throw new Exception(e.getMessage());
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
			}
			booleanSettings.put(key, boolValue);
			return;
		}
		
		// Handle string type options:
		if(stringSettings.containsKey(key)) {
			stringSettings.put(key, value);
			return;
		}
		
		// Execution should not reach here:
		throw new Exception("TILT: Unknown Options should have been handled above!");
	}

	
	public String[] getCurrentSettingsAsStringList()
	{
		String[] settingsList = new String[ integerSettings.size()
		                                   + booleanSettings.size()
		                                   + stringSettings.size() ];
		int i = 0;
		for (Key key : integerSettings.keySet()) {
			settingsList[i] = key.toString() + ": " + integerSettings.get(key).toString();
			i++;
		}
		for (Key key : booleanSettings.keySet()) {
			settingsList[i] = key.toString() + ": " + booleanSettings.get(key).toString();
			i++;
		}
		for (Key key : stringSettings.keySet()) {
			settingsList[i] = key.toString() + ": " + stringSettings.get(key).toString();
			i++;
		}
		
		return settingsList;
	}

	
	public File getSettingsFile() {
		return file;
	}
	
	
	public int getIntegerSetting(Key key)
	{
		assert(integerSettings.containsKey(key));
		return integerSettings.get(key);
	}
	
	
	public boolean getBooleanSetting(Key key)
	{
		assert(booleanSettings.containsKey(key));
		return booleanSettings.get(key);
	}
	
	
	public String getStringSetting(Key key)
	{
		assert(stringSettings.containsKey(key));
		return stringSettings.get(key);
	}
	
	
	public Material getMaterialSetting(Key key) throws Exception
	{
		assert(stringSettings.containsKey(key));
		String name = stringSettings.get(key).toUpperCase();
		Material m = Material.getMaterial(name);
		if (m == null)
			throw new Exception("Material setting invalid: " + name);
		return m;
	}
	
	
	public String[] getMaterialListSettingString(Key key) throws Exception
	{
		assert(stringSettings.containsKey(key));
		String matNames = stringSettings.get(key).toUpperCase();
		String[] namesWithDataArray = matNames.split(",");
		return namesWithDataArray;
	}
}

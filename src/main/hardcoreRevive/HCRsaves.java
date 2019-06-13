package hardcoreRevive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import mcPluginHelpers.MsgLevel;

public class HCRsaves
{
	/// Internal data representation, mapped to the file here
	public class PlayerData
	{
		public String UUID;
		public World  lastAliveWorld;
		public Vector lastAlivePos;
		public SaveReason saveReason;
	}
	
	/// Part of the internal data structure, saved as string in file.
	public enum SaveReason
	{
		DEATH,
		SERVER_STOPPED,
		PLAYER_QUIT,
		SNAPSHOT,
		DEBUG,
		RESPAWN
	}
	
	private static HCRsaves instance = null;
	
	private static String   fileName = "HCRsaves.yml";
	private static String   uuidTag  = "UUID";
	private static String   worldNameTag = "lastAliveWorldName";
	private static String   posTag   = "lastAlivePos";
	private static String 	reasonTag= "saveReason";
	
	private File file;
	private List<PlayerData> playerData = new ArrayList<PlayerData>(); ///< Here the players' positions are saved on SW side
	
	
	/// Creates the save file and provides the singleton for the get() method.
	/// Called during initialization by the plugin.
	public static void init() throws Exception
	{
		if (instance != null)
		{
			HCRPlugin.get().onStateMessage(MsgLevel.WARNING,"HCRsaves::init(): instance already set and init called!");
			return;
		}
		HCRsaves newInstance = new HCRsaves();
		
		// Here we add some test dummy data
		PlayerData dummyData = newInstance.new PlayerData();
		dummyData.UUID = "THIS_IS_DUMMY_DATA_FOR_DEBUG";
		dummyData.lastAliveWorld = Bukkit.getWorlds().get(0);
		dummyData.lastAlivePos = new Vector(11, 22, 33);
		dummyData.saveReason = SaveReason.DEBUG;
		newInstance.playerData.add(dummyData);
		
		try {
			newInstance.file = new File(HCRPlugin.get().getPluginDir(), fileName);
	        if (!newInstance.file.exists()) {
	        	newInstance.createFile();
	        	HCRPlugin.get().onStateMessage(MsgLevel.DEBUG, "HCRsaves::init(): created new file " + fileName);
	        }else {
	        	newInstance.loadFile();
	        	HCRPlugin.get().onStateMessage(MsgLevel.DEBUG, "HCRsaves::init(): loaded existing file " + fileName);
	        }
	        instance = newInstance;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	
	/// Returns the singleton instance of this class.
	public static HCRsaves get()
	{
		if (instance == null) {
			HCRPlugin.get().onStateMessage(MsgLevel.ERROR,"HCRsaves::get(): instance not yet set up!");
		}
		return instance;
	}
	
	
	/// Adds new content to the local representation, to be saved to the save file during quit.
	public void addOrUpdatePlayerEntry(Player p, SaveReason reason)
	{
		HCRPlugin.get().onStateMessage(MsgLevel.DEBUG, "Updating locataion of " + p.getName() + ". Reason: " + reason.toString());
		PlayerData save = new PlayerData();
		save.UUID = p.getUniqueId().toString();
		save.lastAliveWorld = p.getWorld();
		save.lastAlivePos = p.getLocation().toVector();
		save.saveReason = reason;
		
		// Delete old entry for this player if any:
		for (PlayerData d : playerData) {
			if (d.UUID.equals(save.UUID)) {
				playerData.remove(d);
				break;
			}
		}
		
		// add new entry. This is an update if the old one was removed above.
		playerData.add(save);
	}
	
	
	/// Initial creation of the save file or actual save by overwriting
	private void createFile() throws IOException
	{
		FileConfiguration newConfig = new YamlConfiguration();
		
		int i = 0;
		ConfigurationSection section;
		for (PlayerData p : playerData)
		{
			section = newConfig.createSection("Player"+i);
			section.set(uuidTag, p.UUID);
			section.set(worldNameTag, p.lastAliveWorld.getName());
			section.set(posTag, p.lastAlivePos);
			section.set(reasonTag, p.saveReason.toString());
			i++;
		}
		
		newConfig.save(file);
	}

	
	/// Loads our custom data structure from the file
	private void loadFile() throws FileNotFoundException, IOException, InvalidConfigurationException
	{
		FileConfiguration config = new YamlConfiguration();
		config.load(file);
		
		int i = 0;
		ConfigurationSection section;
		boolean dataPresent = config.contains("Player"+i);
		while (dataPresent)
		{
			PlayerData p = new PlayerData();
			section = config.getConfigurationSection("Player"+i);
			p.UUID = section.getString(uuidTag);
			p.lastAliveWorld = Bukkit.getWorld(section.getString(worldNameTag));
			p.lastAlivePos = section.getVector(posTag);
			p.saveReason = SaveReason.valueOf(section.getString(reasonTag,"Debug").toUpperCase());
			
			i++;
			dataPresent = config.contains("Player"+i);
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

	
	public File getSettingsFile() {
		return file;
	}
	
	
	public List<PlayerData> getPlayerData() {
		return playerData;
	}
}

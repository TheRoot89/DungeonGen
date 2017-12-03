package dungeonGen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.permissions.BroadcastPermissions;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;


public final class DungeonGen extends JavaPlugin {
	
	public File dir;							// directory of this plugin
	public WorldEditPlugin worldEdit;			// the worldEdit plugin pointer uses for saving/loading schematics
	public World world = null;					// the world this plugin is started in
	
	// Settings:
	private String confFileName = "config.yml";	// file this plugin saves its settings in
	private boolean initYmlCheck = false;		// Flag to test YAML files on startup, set in config
	
	// The lists of module names per module type, loaded from config:
	private List<String> entryModules;				
	private List<String> passageWayModules;
	private List<String> roomModules;
	
	// working variables and pointers:
	private State state = State.NOT_STARTED; // state variable marks status of the plugin
	private PassageWay curPassWay1 = null;
	private PassageWay curPassWay2 = null;
	private Room curRoom = null;
	private Entry entry = null;
	
	private Random randGen = new Random();
	private long seed;
	
	public List<? extends Player> activePlayers;
	public List<GameMode> playersSnapshotAtStart;
	

	
	
	@Override
    public void onEnable() {
		// Check dependencies:
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
        	setStateAndNotify(State.ERROR, "WorldEdit not found!");
        	return;
        }
        worldEdit = (WorldEditPlugin) plugin;
		
        // Set executors (test feature for later code sorting):
        //this.getCommand("test").setExecutor(new DunGenCommandExecutor(this));
        
        // setup directory:
        dir = getDataFolder();
        if (!dir.exists())
        	if(!dir.mkdir()) {
        		setStateAndNotify(State.ERROR, "Could not create directory for plugin!");
        		return;
			}
        
        // setup Seed:
        seed = randGen.nextLong();
        randGen.setSeed(seed);
        
        // setup the configuration:
        File configFile = new File(dir, confFileName);
        if (!configFile.exists()) {
            getLogger().info("config.yml not found, creating default.");
            saveDefaultConfig();
        } else {
            getLogger().info("config.yml found, loading.");
        }
        // and load:
        boolean success = loadConfig();
        if (success)
        	getLogger().info("Loading successful.");
        else {
        	setStateAndNotify(State.ERROR, "Loading of config failed!");
        	return;
        }
        	
    	// YAML check if active:
    	if (initYmlCheck) {
    		getLogger().info("Checking YAML files ...");
    		try {
				checkModuleYmlFiles();
				getLogger().info("Check ok.");
			} catch (Exception e) {
				e.printStackTrace();
				setStateAndNotify(State.ERROR, "YAML check failed!");
    			return;
			}
    	}	
        
        /*
        // DEBUG ONLY (remove if not needed any more): /////////
        // generates dummy yaml file to see syntax for different saves
        FileConfiguration tstC = new YamlConfiguration();
        tstC.set("testVector.erster", new Vector(1,2,3));
        tstC.set("testVector.zweiter", new Vector(4,5,6));
        
        tstC.set("testEntity", EntityType.CREEPER);
        
        List<EntityType> tstEntityList = new ArrayList<>();
        tstEntityList.add(EntityType.CREEPER);
        tstEntityList.add(EntityType.ZOMBIE);
        tstC.set("testEntityList", tstEntityList);
        
        tstC.set("tstVecList.1", new Vector(1, 2, 3));
        tstC.set("tstVecList.2", new Vector(4, 5, 6));
        tstC.set("tstVecList.3", null);
        
        try {
        	tstC.save(new File(dir,"test.yml"));
		} catch (IOException e) {

			e.printStackTrace();
		}
        */
    }
    
	class ConfigException extends Exception{
		private static final long serialVersionUID = 1L; // serializeable, suppresses warning
		public ConfigException(String message) {super(message);} // allows a message to be used, getMessage() gets it back.
	}
	
    private void checkModuleYmlFiles() throws ConfigException{
    	YamlConfiguration curConf;
    	String moduleName = "";

    	// Check for keys, if clauses check special room keys. If a checks fails, an exception with the current key
    	// is thrown.
    	//TODO: check the values of keys
    	String key = ""; // empty key serves as flag everything went well
    	
    	// keys contained in "Module":
    	List<String> inspectionList = new ArrayList<String>();
    	inspectionList.addAll(entryModules);
    	inspectionList.addAll(passageWayModules);
    	inspectionList.addAll(roomModules);
		for (String curName : inspectionList) {
			moduleName = curName; // cannot iterate using 'name' directly, saves the name for the exception
			curConf = Module.getConfig(this, moduleName);
			
			if (!curConf.contains("description")) 		{key = "description"; break;}
			if (!curConf.contains("schematic")) 		{key = "schematic"; break;}
			if (!curConf.contains("type")) 				{key = "type"; break;}
				// entry
			if (!curConf.contains("entry.placementLoc")){key = "entry.placementLoc"; break;}	
			if (!curConf.contains("entry.doorLoc")) 	{key = "entry.doorLoc"; break;}	
			if (!curConf.contains("entry.width")) 		{key = "entry.width"; break;}
			if (!curConf.contains("entry.height")) 		{key = "entry.height"; break;}
				// exit
			if (!curConf.contains("exit.placementLoc")) {key = "exit.placementLoc"; break;}	
			if (!curConf.contains("exit.doorLoc")) 		{key = "exit.doorLoc"; break;}	
			if (!curConf.contains("exit.width")) 		{key = "exit.width"; break;}
			if (!curConf.contains("exit.height")) 		{key = "exit.height"; break;}
		}	
		if (key == "") throw new ConfigException("Key missing: " + key + ", in " + moduleName);	
			
		// keys contained in "Passageway" or "Entry":
		// (no differences at the moment, add new block if things are added to Entries alone)
		inspectionList.clear();
		inspectionList.addAll(entryModules);
		inspectionList.addAll(passageWayModules);
		for (String curName : inspectionList) {
			moduleName = curName; // cannot iterate using 'name' directly
			curConf = Module.getConfig(this, moduleName);
			
			if (!curConf.contains("entry.type")) 			{key = "entry.type"; break;}
			if (PassageWay.DoorType.fromInteger(curConf.getInt("entry.type")) == PassageWay.DoorType.PISTON) {
				if (!curConf.contains("entry.redstoneLoc"))	{key = "entry.redstoneLoc"; break;}
			}else { // APPEARING and FALLING
				if (!curConf.contains("entry.doorMaterial")){key = "entry.doorMaterial"; break;}
			}
			if (!curConf.contains("exit.type")) 			{key = "exit.type"; break;}
			if (PassageWay.DoorType.fromInteger(curConf.getInt("exit.type")) == PassageWay.DoorType.PISTON) {
				if (!curConf.contains("exit.redstoneLoc"))	{key = "exit.redstoneLoc"; break;}
			}else { // APPEARING and FALLING
				if (!curConf.contains("exit.doorMaterial"))	{key = "exit.doorMaterial"; break;}
			}
			if (!curConf.contains("respawnLoc"))			{key = "respawnLoc"; break;}
		}
		if (key == "") throw new ConfigException("Key missing: " + key + ", in " + moduleName);
		
		// keys contained in "Room":
		for (String curName : roomModules) {
			moduleName = curName; // cannot iterate using 'name' directly
			curConf = Module.getConfig(this, moduleName);
			
			// TODO: add room key checks when changing mechanism to "tasks"
			//if (!curConf.contains("respawnLoc"))			{key = "respawnLoc"; break;}
		}
		if (key == "") throw new ConfigException("Key missing: " + key + ", in " + moduleName);
		
		// everything ok, if code reached here, returns no value
	}

	@SuppressWarnings("unchecked")
	private boolean loadConfig() {
    	entryModules 		= (List<String>) getConfig().getList("entryModules",new ArrayList<String>());
    	passageWayModules 	= (List<String>) getConfig().getList("passageWayModules",new ArrayList<String>());
    	roomModules	= (List<String>) getConfig().getList("roomModules",new ArrayList<String>());
    	if (entryModules.isEmpty() || passageWayModules.isEmpty() || roomModules.isEmpty()) {
    		getLogger().severe("No module names given in config for some types!");
    		return false;
    	}
    	initYmlCheck = getConfig().getBoolean("initYmlCheck", true);
    	return true;
	}

    
	@Override
    public void onDisable() {
    	//saveConfig();
    }
	
	
	/**
	 * Checks command strings and acts upon found commands
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {	
		//////////////////////////////////////////////////////////
		if (cmd.getName().equalsIgnoreCase("instructions")) {
			if (sender instanceof Player ) {
				Player player = (Player) sender;
				player.sendMessage("Please use worldEdit to save schematics!");
				player.sendMessage("Use //wand to get the selection item!");
				player.sendMessage("Select an area via leftclick and rightclick.");
				player.sendMessage("//copy to copy the selection to the clipboard.");
				player.sendMessage("//schematic save <fileName> to save the clipboard.");
			}else {
				sender.sendMessage("Cannot be executed from console!");
			}
			return true;
		//////////////////////////////////////////////////////////
		}else if (cmd.getName().equalsIgnoreCase("start")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Cannot be executed from console!");
				return true;
			}
			Player player = (Player) sender;
			this.world = player.getWorld();
			
			// checking whether plugin is ready:
			if (state == State.ERROR) {
				player.sendMessage("Initialization of plugin was not successful! See log.");
				return true;
			}
			if (state == State.STARTUP || state == State.RUNNING) {
				player.sendMessage("Dungeon has already been created!");
				return true;
			}
			
			startDungeon(player);
			return true;
		///////////////////////////////////////////////////////////
		}else if (cmd.getName().equalsIgnoreCase("stop")){
			if (state == State.NOT_STARTED) {
				if (sender instanceof Player) {
					Player p = (Player) sender;
					p.sendMessage("Dungeon was not started yet!");
				}
			}else {
				for (Player p : activePlayers) {
					p.sendMessage("Stopping dungeon.");
				}
				stopDungeon();
			}
			return true;
		}
		///////////////////////////////////////////////////////////
		return false;
	}
	
	private void setStateAndNotify(State state, String message) {
		setStateSilenty(state,message);
		if (this.state == State.ERROR)
			getLogger().info(this.state.statusMessage);
		else
			getLogger().severe(this.state.statusMessage);
	}
	
	private void setStateSilenty(State state) {
		setStateSilenty(state,"");
	}
	
	private void setStateSilenty(State state, String message) {
		this.state = state;
		this.state.statusMessage = message;
	}
	
	
	private void startDungeon(Player p) {
		// calc starting location in front of player on level ground:
		// also check if ground is solid. if not then paste over the gras etc.
		int initDist = 10; // distance to player
		Vector start = new Vector(p.getLocation().getBlockX(),p.getLocation().getBlockY(),p.getLocation().getBlockZ());
		Direc playerDirec = Helper.getPlayerDirec(p);
		int deltaX = (int)Math.round(-Helper.sind(playerDirec.degree())*initDist);
		int deltaZ = (int)Math.round(+Helper.cosd(playerDirec.degree())*initDist);
		start.add(new Vector(deltaX,0,deltaZ));
		int solidOffset = 0;
		if (world.getHighestBlockAt(start.getBlockX(), start.getBlockZ()).getType().isSolid()) {
			solidOffset = 1;
		}
		start.setY(world.getHighestBlockYAt(start.getBlockX(), start.getBlockZ())+solidOffset);
		
		genEntry(start, playerDirec);
	}

	
	private void stopDungeon() {
		// state 0 is catched during the command call
		if (state == State.STARTUP) {		// during startup
			curPassWay2 = null;
			entry.unregister();
			entry.delete();
			entry = null;
			state = State.NOT_STARTED;
		}else if (state == State.RUNNING) {	// during running dungeon
			curPassWay1.unregister();
			curPassWay1.delete();
			curPassWay1 = null;
			curRoom.unregister();
			curRoom.delete();
			curRoom = null;
			curPassWay2.unregister();
			curPassWay2.delete();
			curPassWay2 = null;
			state = State.NOT_STARTED;
		}	

		// resetting GameModes of players
		for (int i=0; i< activePlayers.size(); i++) {
			if (activePlayers.get(i).isValid())
				activePlayers.get(i).setGameMode(playersSnapshotAtStart.get(i));
		}
		activePlayers.clear();
		playersSnapshotAtStart.clear();
	}

	
	/**
	 * Starts the dungeon, generating the entry area and setting up listeners for player button press actions.
	 * The STARTUP status will lead to the execution of the startup() member as soon as the entry's button
	 * is pressed.
	 * @param start		Vector where the dungeon entry is generated
	 * @param towardsD	Dungeon Entry direction
	 */
	public void genEntry(Vector start, Direc towardsD) {
		state = State.STARTUP;

		// generate entry:
		String name = getRandomModule(entryModules);
		entry = new Entry(this,name,BukkitUtil.toVector(start),towardsD); //generate the named entry. 'This' is given for attribute access.
		curPassWay2 = entry;	// start chain of module tripplets
		entry.place();
		entry.register();	//listener for button now active
		entry.toggleExit(false);
		entry.toggleEntry(true);
		getServer().broadcastMessage("Dungeon started, enter on your own risk.");
	}
	
	public String getRandomModule(List<String> type) {
		return type.get(randGen.nextInt(type.size()));
	}
	
	/**
	 * Callback of the switch in each PassageWay.
	 * Generates the next room after the previous was finished, i.e. a player used the button within a passageWay.
	 * It is therefore called by event listeners within the passageWays.
	 */
	public void genNextRoom() {
		//check for startup phase:
		if (state == State.STARTUP)
			startup();
		
		// prep: close this passageWay and delete old:
		curPassWay2.toggleEntry(false);
		if (curPassWay1 != null && curPassWay2.type != ModuleType.ENTRY) //entry should not be deleted, should it?
			curPassWay1.delete();
		
		// go one step further with our dungeon objects and delete old room:
		curPassWay1 = curPassWay2;
		if (curRoom != null) {
			curRoom.delete();
		}
		
		
		// get new room name and place it:
		com.sk89q.worldedit.Vector nextEntry = curPassWay1.getNextEntryPos();
		//getServer().broadcastMessage("Next room at:" + nextEntry.toString());
		// better "structured" randomness has to start here
		String name = getRandomModule(roomModules);
		ModuleType type = Module.getType(this, name);
		switch (type) {
		case PLATFORMROOM:	curRoom = new PlatformRoom(this, name, nextEntry, curPassWay1.exit.afterPasteDirec);
							break;
		case BATTLEROOM:	curRoom = new BattleRoom(this, name, nextEntry, curPassWay1.exit.afterPasteDirec);
							break;
		default:			getLogger().severe("Error with room type during instantiation!");
							stopDungeon();
							return;
		}	
		curRoom.place();
		
		
		// Gen new passageWay name after that room, place and close its entry:
		name = getRandomModule(passageWayModules);
		nextEntry = curRoom.getNextEntryPos();
		//getServer().broadcastMessage("Next passageWay at:" + nextEntry.toString());
		curPassWay2 = new PassageWay(this, name, nextEntry, curRoom.exit.afterPasteDirec);
		curPassWay2.place();
		curPassWay2.toggleEntry(false);
		curPassWay2.toggleExit(false);
		
		// move respawn:
		for (Player p : activePlayers) {
			com.sk89q.worldedit.Vector spawn = curPassWay1.toGlobal(curPassWay1.respawnLoc).add(0.5,0,0.5); // spawn at middle of block!
			p.setBedSpawnLocation(BukkitUtil.toLocation(world, spawn), true); // sets respawn even without bed, needs Location :(
		}
		
		// and go! (open the door to the room):
		curRoom.register();
		curPassWay1.toggleExit(true);
		getServer().broadcastMessage("Entering " + curRoom.description);
	}
    
	
	/**
	 * Called when the first button within the entry Module was just pushed
	 * Take appropriate actions.
	 */
	private void startup() {
		activePlayers = new LinkedList<>(getServer().getOnlinePlayers());
		
		// Backing up player modes to restore upon dungeon stop
		playersSnapshotAtStart = new ArrayList<GameMode>();// backup copy of gamemode
		for (int i=0; i<activePlayers.size(); i++) {
			playersSnapshotAtStart.add(activePlayers.get(i).getGameMode());
		}
		
		for (Player p : activePlayers) {
			p.setFoodLevel(18);
			p.setGameMode(GameMode.ADVENTURE);
			p.sendMessage("Your mode was set to adventure...");
		}
		//TODO move everything? take their items? Give them starting gear?
		
		state = State.RUNNING;
	}
	
	
	/**
	 * Callback of the room clear Listener or checking Task in each normal dungeon room.
	 */
	public void roomClear() {
		curPassWay2.toggleEntry(true);
		curPassWay2.register();
		getServer().broadcastMessage("Room solved. Well done.");
	}
	
	private enum State {
		NOT_STARTED,			// no dungeon was started yet
		STARTUP,				// entry generated but not started yet
		RUNNING,				// running fully
		ERROR;					// something happened :/
		
		public String statusMessage; // an additional message can be stored
	}
}

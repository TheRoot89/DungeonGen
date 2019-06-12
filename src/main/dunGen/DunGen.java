package dunGen;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import dunGen.Module.ModuleType;
import dunGen.utils.ConfigChecker;
import dunGen.utils.Util_BlockName;
import javaHelpers.MathHelpers;
import mcPluginHelpers.Direc;
import net.md_5.bungee.api.ChatColor;


public final class DunGen extends JavaPlugin implements Listener{
	
	
	/**Represents the current plugin status. A message may be associated, especially with the ERROR state.
	 * The state variable of the plugin is set via the setState[...]() functions.
	 */
	public enum State {
		NOT_STARTED,		// no dungeon was started yet
		STARTUP,			// entry generated but not started yet
		RUNNING,			// running fully
		ERROR;				// something happened :/
		
		public String statusMessage; // an additional message can be stored
	}
	
	
	
	// ########################## Member variables ############################
	
	// Held fixed references and information:
	private String  		confFileName 	= "config.yml";	// file this plugin saves its settings in	
	public  File   			dir;							// directory of this plugin
	private Random 			randGen 		= new Random(); // RNG object.
	public  World 		   	world   	 	= null;			// the world this plugin is started in
	public  WorldEditPlugin worldEdit;						// the worldEdit plugin pointer used for saving/loading schematics
	private ConfigChecker 	configChecker;					// Reference to the yml check utility instance (can receive commands)
	private Util_BlockName  blockNameUtility;				// Reference to the block name utility instance
	// Settings, loaded from config:
	public 	List<String> 	entryModules;					// The lists of module names per module type
	public 	List<String> 	passagewayModules;
	public 	List<String> 	roomModules;
	private boolean 	 	initYmlCheck 	= false;		// Flag to test YAML files on startup, loaded from config file, else false
	private long 			seed;							// Seed for random generation.
	// working variables and references:
	private Passageway 		curPassway1 	= null;			// References to the current module triplet.
	private Passageway 		curPassway2 	= null;			//
	private Room 			curRoom 		= null;			//
	private Entry 			entry 			= null;			// Reference to the placed entry module
	public  List<? extends 	Player> activePlayers;			// List of players playing.
	public  List<GameMode> 	playersSnapshotAtStart;			// Backup of player GameModes for restoration.
	public 	State 			state = State.NOT_STARTED; 		// state variable marks status of the plugin. Not started initially.

	
	
	// ############################ Member functions ############################
	
    /**Checks the yml existence and keys for all module names listed in config.yml.
     * This way, the keys don't have to be checked in every loadConfig() function throuhout the plugin.
     * */
   

    
	/**
	 * Starts the dungeon, generating the entry area and setting up listeners for player button press actions.
	 * The STARTUP status will lead to the execution of the startup() member as soon as the entry's button is pressed.
	 * @param start		Vector where the dungeon entry is generated
	 * @param towardsD	Dungeon Entry direction
	 */
	public void genEntry(Vector start, Direc towardsD) {
		state = State.STARTUP;

		// generate entry:
		String name = getRandomModule(entryModules);
		entry = new Entry(this,name,BukkitUtil.toVector(start),towardsD); //generate the named entry. 'This' is given for attribute access.
		curPassway2 = entry;		// start chain of module tripplets
		entry.place();
		entry.register();			//listener for button now active
		entry.toggleExit(false);
		entry.toggleEntry(true);
		getServer().broadcastMessage("Dungeon started, enter on your own risk."); // ok to broadcast to whole server?
	}

    
	/**
	 * Callback of the switch in each Passageway. Pressed in curPassageWay2 after completing a room.
	 * Generates the next room after the previous was finished.
	 * It is therefore called by the onButtonPress event listener within the passageWays.
	 */
	public void genNextRoom() {
		//check for startup phase:
		if (state == State.STARTUP)
			startup();
		
		// preparation: close this passageWay and delete old:
		curPassway2.toggleEntry(false);
		if (curPassway1 != null && curPassway2.type != ModuleType.ENTRY) //entry should not be deleted, should it? Config option?
			curPassway1.delete();
		
		// go one step further with our dungeon triplet and delete old room:
		// This also unregisters pending tasks of the old room.
		curPassway1 = curPassway2;
		if (curRoom != null)
			curRoom.delete();
		
		// get new room name and place it:
		com.sk89q.worldedit.Vector nextEntry = curPassway1.getNextEntryPos();
		
		String name = getRandomModule(roomModules); 	// better "structured" randomness e.g. to difficulty has to start here
		ModuleType type = Module.getType(this, name);
		switch (type) {
		case PLATFORMROOM:	curRoom = new PlatformRoom(this, name, nextEntry, curPassway1.exit.afterPasteDirec);
							break;
							
		case BATTLEROOM:	curRoom = new BattleRoom(this, name, nextEntry, curPassway1.exit.afterPasteDirec);
							break;
							
		default:			setStateAndNotify(State.ERROR, "Error with room type during instantiation: Room type not known.");
							reset();
							return;
		}
		curRoom.place();
		
		
		// Gen new passageWay name after that room, place and close its entry:
		name = getRandomModule(passagewayModules);
		nextEntry = curRoom.getNextEntryPos();
		//getServer().broadcastMessage("Next passageWay at:" + nextEntry.toString());
		curPassway2 = new Passageway(this, name, nextEntry, curRoom.exit.afterPasteDirec);
		curPassway2.place();
		curPassway2.toggleEntry(false);
		curPassway2.toggleExit(false);
		
		// move respawn:
		for (Player p : activePlayers) {
			com.sk89q.worldedit.Vector spawn = curPassway1.toGlobal(curPassway1.respawnLoc).add(0.5,0,0.5); // spawn at middle of block!
			p.setBedSpawnLocation(BukkitUtil.toLocation(world, spawn), true); // sets respawn even without bed, needs Location :(
		}
		
		// and go! (open the door to the room):
		curRoom.register();
		curPassway1.toggleExit(true);
		getServer().broadcastMessage("Entering " + curRoom.description);
	}
	
	
	/**Chooses a module name from the given List semi-randomly, according to the current seed.
	 * @param type 	String-List of a certain module type.
	 * @return		A name string of the chosen module.
	 */
	public String getRandomModule(List<String> type) {
		return type.get(randGen.nextInt(type.size()));
	}
	
	
	/**Loads the standard configuration file of the plugin, config.yml. Also checks the module lists contain entries.*/
	@SuppressWarnings("unchecked") // cast from yml loaded list is ok
	private void loadConfig() {
    	entryModules 		= (List<String>) getConfig().getList(	"entryModules",		new ArrayList<String>());
    	passagewayModules 	= (List<String>) getConfig().getList(	"passagewayModules",new ArrayList<String>());
    	roomModules			= (List<String>) getConfig().getList(	"roomModules",		new ArrayList<String>());
    	initYmlCheck 		= 				 getConfig().getBoolean("initYmlCheck", 	true);
    	
    	if (entryModules.isEmpty() || passagewayModules.isEmpty() || roomModules.isEmpty())
    		setStateAndNotify(State.ERROR, "No names given in config for some module types needed!");
	}
	
  
	/**Gives the players starting gear, called during dungeon startup.
     * @param p 	The player to give stuff to.
     */
	private void giveStartingGear(Player p) {
		PlayerInventory i = p.getInventory();
		i.clear();
		i.addItem( new ItemStack(Material.STONE_SWORD, 	 1));
		i.addItem( new ItemStack(Material.BOW, 			 1));
		i.addItem( new ItemStack(Material.ARROW, 		 1));
		i.addItem( new ItemStack(Material.MUSHROOM_STEW, 1));

		i.setBoots(new ItemStack(Material.LEATHER_BOOTS, 1));
  }
  
  
	/**Checks command strings and acts upon found commands. Called by the server.*/
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {	
		
		//##################### instructions ########################
		if (cmd.getName().equalsIgnoreCase("instructions")) {
			if (sender instanceof Player ) {
				Player player = (Player) sender;
				player.sendMessage("Please use worldEdit to save schematics!");
				player.sendMessage("Use //wand to get the selection item!");
				player.sendMessage("Select an area via leftclick and rightclick.");
				player.sendMessage("Be sure to face east when looking at the new module's entry.");
				player.sendMessage("//copy to copy the selection to the clipboard.");
				player.sendMessage("//schematic save <fileName> to save the clipboard.");
			}else {
				sender.sendMessage("Cannot be executed from console!");
			}
			return true;
			
			
		//##################### start ###############################
		}else if (cmd.getName().equalsIgnoreCase("start")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Cannot be executed from console!");
				return true;
			}
			Player player = (Player) sender;
			
			// Register in which world the DunGen is started:
			this.world = player.getWorld();
			
			// checking whether plugin is ready:
			if (state == State.ERROR) {
				player.sendMessage("Initialization of plugin was not successful! See server log and message:");
				player.sendMessage(state.statusMessage);
				return true;
			}
			if (state == State.STARTUP || state == State.RUNNING) {
				player.sendMessage("Dungeon has already been created!");
				return true;
			}
			
			startDungeon(player);
			return true;
			
			
		//####################### end ##############################
		}else if (cmd.getName().equalsIgnoreCase("end")){
			
			switch (state) {
			case NOT_STARTED:
			case STARTUP:
				if (sender instanceof Player) {
					Player p = (Player) sender;
					p.sendMessage("Dungeon was not started yet!");
				}else {
					getLogger().info("Dungeon was not started yet!");
				}
				break;
			case RUNNING:
				for (Player p : activePlayers) {
					p.sendMessage("Stopping dungeon.");
				}
				stopDungeon();
				break;
			case ERROR:
				if (sender instanceof Player) {
					Player p = (Player) sender;
					p.sendMessage("There has been an error during DunGen plugin execution:");
					p.sendMessage(state.statusMessage);
					p.sendMessage("Please use /reset");
				}else {
					getLogger().info("There has been an error during DunGen plugin execution:");
					getLogger().info(state.statusMessage);
					getLogger().info("Please use /reset");
				}
				break;
			}
			return true;  // meaning: we handled with one of our commands
		}
		
		// none of our commands fitted: return false. Will make the server search on in other plugins and mc itself
		return false;
	}
	
	
	/**Shutdown operations, called by the server. */
	@Override
    public void onDisable() {
		stopDungeon();
    	//saveConfig(); if changeable in-game
    }
	
	
	/**Boots of the plugin, called by the server upon start. We set up directories, the config and do general checks.*/
	@Override
    public void onEnable() {
		// Check dependencies:
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
        	setStateAndNotify(State.ERROR, "WorldEdit not found!");
        	return;
        }
        worldEdit = (WorldEditPlugin) plugin;
		
        // Set executors (used to wrap away plugin parts with commands):
        blockNameUtility = new Util_BlockName(this);
        getCommand("BlockName").setExecutor(blockNameUtility);
        configChecker = new ConfigChecker(this);
        getCommand("checkConfig").setExecutor(configChecker);
        
        
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
            getLogger().info("config.yml found, loading...");
        }
        // and load:
        try {
			loadConfig();
			getLogger().info("Loading successful.");
		} catch (Exception e) {
			setStateAndNotify(State.ERROR, "Loading of config failed:");
			e.printStackTrace();
			return;
		}

    	// YAML check if active:
    	if (initYmlCheck) {
    		getLogger().info("Starting YAML file check...");
    		try {
				configChecker.checkModuleYmlFiles();
				getLogger().info("Check ok.");
			} catch (Exception e) {
				setStateAndNotify(State.ERROR, "YAML check failed!");
				e.printStackTrace();
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

  
	/**Handles actions to be taken upon respawn.
	 * Currently gives new starting gear.
	 * @param event The event given to this handler by the event manager.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		//Player p = event.getPlayer();
		if (state == State.RUNNING) {
			giveStartingGear(event.getPlayer());
			event.getPlayer().updateInventory();
		}
	}
	
  
	/**This should be called when the plugin is in ERROR state to reset it to a hopefully working state. */
	private void reset() {
		if (curPassway1 != null) {
			curPassway1.unregister();
			curPassway1.delete();
			curPassway1 = null;
		}
		if (curPassway2 != null) {
			curPassway2.unregister();
			curPassway2.delete();
			curPassway2 = null;
		}
		if (curRoom != null) {
			curRoom.unregister();
			curRoom.delete();
			curRoom = null;
		}
		
		resetActivePlayers();
		
		onEnable();
		setStateSilenty(State.NOT_STARTED);
		
		// anything else to reset?
		// problem is, the remaining code from where reset() was called will still be executed. E.g. calls to curRoom will crash then.
		// How can this be handled?
	}

	
	
	/**Resets the players to how they were before they entered the dungeon. */
	private void resetActivePlayers() {
		// reset their Gamemode:
		for (int i=0; i< activePlayers.size(); i++) {
			if (activePlayers.get(i).isValid())
				activePlayers.get(i).setGameMode(playersSnapshotAtStart.get(i));
		}
		activePlayers.clear();
		playersSnapshotAtStart.clear();
		
		// upcoming: reset their gear:
		// ...
	}
	
	
	/**Callback of the room task checking for victory conditions in each normal dungeon room. */
	public void roomClear() {
		curPassway2.toggleEntry(true);
		curPassway2.register();
		getServer().broadcastMessage("Room solved. Well done.");
	}
	
	
	/**Sets the plugins status with a message. Used for control flow and signalizing errors.
	 * This overload also prints the message to the console.
	 * @param state		The state to be set.
	 * @param message	The message to be written to console, e.g. an error message.
	 */
	public void setStateAndNotify(State state, String message) { 
		this.state = state;
		this.state.statusMessage = message;
		if (this.state == State.ERROR)
			//getLogger().severe(this.state.statusMessage);
			getServer().getConsoleSender().sendMessage(ChatColor.RED + "[DunGen] " + this.state.statusMessage);
		else
			getLogger().info(this.state.statusMessage);
	}
	
	
	/**Overloaded set state function with empty message as default parameter.
	 * @param state The state to set.
	 */
	private void setStateSilenty(State state) {
		setStateSilenty(state,"");
	}
	
	
	/**Overloaded set state function with the given message. Does not print to console.
	 * @param state 	The state to set.
	 * @param message	The message to set but not instantly print.
	 */
	private void setStateSilenty(State state, String message) {
		this.state = state;
		this.state.statusMessage = message;
	}
    
	
	/**Dungeon activation function called when a player gives the /start command. Calls the genEntry() function at that players position.
	 * @param p		The player that gave the /start command.
	 */
	private void startDungeon(Player p) {
		// calc starting location in front of player on level ground:
		// also check if ground is solid. if not then paste over the gras etc.
		int initDist = 10; // distance to player
		Vector start = new Vector(p.getLocation().getBlockX(),p.getLocation().getBlockY(),p.getLocation().getBlockZ());
		Direc playerDirec = Direc.getPlayerDirec(p);
		int deltaX = (int)Math.round(-MathHelpers.sind(playerDirec.degree())*initDist);
		int deltaZ = (int)Math.round(+MathHelpers.cosd(playerDirec.degree())*initDist);
		start.add(new Vector(deltaX,0,deltaZ));
		int solidOffset = 0;
		if (world.getHighestBlockAt(start.getBlockX(), start.getBlockZ()).getType().isSolid()) {
			solidOffset = 1;
		}
		start.setY(world.getHighestBlockYAt(start.getBlockX(), start.getBlockZ())+solidOffset);
		
		genEntry(start, playerDirec);
	}
	
	
	 /**Called when the first button within the entry Module was just pushed
	  * Takes appropriate startup actions and then sets the state to RUNNING.
	  */
	private void startup() {
		getServer().getPluginManager().registerEvents(this, this);
		
		activePlayers = new LinkedList<>(getServer().getOnlinePlayers());
		
		//filter for players in the entry building:
		for(Player p: activePlayers) {
			com.sk89q.worldedit.Vector playerPos = BukkitUtil.toVector(p.getLocation().toVector());
			if (!entry.modVolume.contains(playerPos))
				activePlayers.remove(p);
		}
		
		// Backing up player modes to restore upon dungeon stop
		playersSnapshotAtStart = new ArrayList<GameMode>();// backup copy of gamemode
		for (int i=0; i<activePlayers.size(); i++) {
			playersSnapshotAtStart.add(activePlayers.get(i).getGameMode());
		}
		
		for (Player p : activePlayers) {
			p.setFoodLevel(18);
			p.setGameMode(GameMode.ADVENTURE);
			p.sendMessage("Your mode was set to adventure...");
			
			giveStartingGear(p);

		}
		
		state = State.RUNNING;
	}
	
	
	/**Stops the dungeon in any state, resetting the players and modules. Will also try to reset the plugin if it is in error state. */
	private void stopDungeon() {
		// state 0 is catched during the command call
		switch (state) {
		case NOT_STARTED:
			reset();					// alternative is to do nothing, but resetting may be better
			break;
			
		case STARTUP:					// only entry exists during startup
			curPassway2 = null;
			entry.unregister();
			entry.delete();
			entry = null;
			state = State.NOT_STARTED;
			break;
			
		case RUNNING:					// all the modules exist, how about entry?
			curPassway1.unregister();
			curPassway1.delete();
			curPassway1 = null;
			curRoom.unregister();
			curRoom.delete();
			curRoom = null;
			curPassway2.unregister();
			curPassway2.delete();
			curPassway2 = null;
			state = State.NOT_STARTED;
			break;

		case ERROR:						// try and reset
			reset();
			break;
		}

		resetActivePlayers();
	}
	
	
}

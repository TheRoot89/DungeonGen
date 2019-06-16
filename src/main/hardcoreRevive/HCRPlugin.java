package hardcoreRevive;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import hardcoreRevive.HCRsaves.PlayerData;
import hardcoreRevive.HCRsaves.SaveReason;
import hardcoreRevive.HCRsettings.Key;
import mcPluginHelpers.MsgLevel;

/**Minecraft Plugin to host game of MineCSweeper. This enables testing and
 * playing outside of DunGen. The full game code shall be included in its own
 * class, so the game can be added to any plugin.
 */
public class HCRPlugin extends JavaPlugin implements Listener {

	// ######################### Settings #####################################
	final static MsgLevel playerMessageLevel = MsgLevel.INFO;
	final static MsgLevel consoleMessageLevel = MsgLevel.DEBUG;
	

	// ########################## Member variables ############################
	private static HCRPlugin instance = null; 	///< Singleton instance of this
	private File pluginDir = null; 				///< Directory of this plugin
	private World serverWorld = null;			///< Std world of everyone to respawn in

	/// Singleton access
	public static HCRPlugin get() {
		return instance;
	}
	
	
	// ############################ Member functions ############################
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Input check:
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage("Command can be used by player for Debug purpose only! TODO: remove this.");
		}

		return interpretCommand(command.getName(), args);
	}

	/// Helper function to parse the given command.
	private boolean interpretCommand(String command, String[] args) {
		if (command.equalsIgnoreCase("revive")) {
			getServer().getConsoleSender().sendMessage("Reviving Players...");
			reviveAllPlayers();
			return true;
		}

		// Command was not found if execution reached here:
		// Something must have gone wrong then, check your plugin.yml for consistency!
		getServer().getConsoleSender().sendMessage("Command not found!");
		return false;
	}

	
	/// Method to identify and revive all players
	private void reviveAllPlayers()
	{
		Collection<? extends Player> players = getServer().getOnlinePlayers();
		for (Player player : players)
		{
			if (player.getGameMode() == GameMode.SPECTATOR)   // these are the 'dead' ones.
			{
				revivePlayer(player);
			}
		}
	}
	
	
	/// Method executing the actual plugin behaviour.
	/// Hardcore death behaves the following: (1.14.2)
	/// If a player dies, he may exit the server or directly respawn as spectator
	/// fixed in position.
	/// If he dis- and re-connects, he will start at this game-over screen again.
	/// Usual mc commands: gamemode <survival,spectator> Root89
	/// The idea is to revive the player far from:
	///  1. Other players, online or offline
	///  2. His place of death
	public void revivePlayer(Player p)
	{
		// TODO what happens when still on the death screen? Can we detect this?
		if (p.getGameMode() != GameMode.SPECTATOR) {
			sendMessage(MsgLevel.WARNING, "Tried to revive " + p.getName() + ", but the player is not in observer mode! Aborting.");
			return;
		}
		
		p.sendMessage("You are being revived...OMMMMMM");
		getServer().getConsoleSender().sendMessage("Reviving: " + p.getName());
		Location spawnLoc = getRespawnLoc(); 
		p.teleport(spawnLoc);
		p.setCompassTarget(spawnLoc);
		p.setGameMode(GameMode.SURVIVAL);
		
		updateOnlinePlayerSaves(SaveReason.RESPAWN);
	}
	
	
	/// Algorithm to find respawn location.
	/// 1. Get all constraints, including death location.
	/// 2. Get bounding box
	/// 3. Get valid location in bbox regarding constraints
	/// 4. Get height so it is on ground and deliver.
	private Location getRespawnLoc()
	{
		// Load settings for this:
		int spawnDist = HCRsettings.get().getIntegerSetting(Key.RESPAWN_DIST);
		int respawnMaxTries = HCRsettings.get().getIntegerSetting(Key.RESPAWN_MAX_TRIES);
		int respawnRandOffsetMax = HCRsettings.get().getIntegerSetting(Key.RESPAWN_RAND_OFFSET_MAX);
		
		
		sendMessage(MsgLevel.DEBUG, "Calculating respawn location: Getting constraints.");
		// 1: constraints, all reasons valid apart from 'DEBUG'
		// Online Players are snapshoted as well. This is ok I think.
		// Also sort out different worlds than the one the player died in.
		updateOnlinePlayerSaves(SaveReason.SNAPSHOT);
		List<PlayerData> constraints = new ArrayList<PlayerData>();
		for (PlayerData pd : HCRsaves.get().getPlayerData())
		{
			if ( (pd.saveReason != SaveReason.DEBUG) &&
				 (pd.lastAliveWorld.getName().equals(serverWorld.getName())) // check for whether these coords are form the correct world.
			   )
			{
				constraints.add(pd);
				sendMessage(MsgLevel.DEBUG, "Found constraint: " + pd.lastAlivePos.toString() );
			}
		}
		sendMessage(MsgLevel.DEBUG, "Found " + constraints.size() +" constraints.");
		// Handle no constraints found:
		if (constraints.size() == 0) {
			Location spawnLoc = new Location(serverWorld,0,0,0);
			spawnLoc.setY(serverWorld.getHighestBlockYAt(spawnLoc));
			sendMessage(MsgLevel.DEBUG, "Found no constraints! Starting at 0,y,0!");
			return spawnLoc;
		}
		
		// 2. Get bounding box for possible spawn locations
		Vector bboxMin = new Vector();
		Vector bboxMax = new Vector();
		int constrX, constrZ;
		for (PlayerData pd : constraints)
		{
			constrX = pd.lastAlivePos.getBlockX();
			if ((constrX + spawnDist) > bboxMax.getBlockX()) {
				bboxMax.setX(constrX + spawnDist);
			}
			if ((constrX - spawnDist) < bboxMin.getBlockX()) {
				bboxMin.setX(constrX - spawnDist);
			}
			
			constrZ = pd.lastAlivePos.getBlockZ();
			if ((constrZ + spawnDist) > bboxMax.getBlockZ()) {
				bboxMax.setZ(constrZ + spawnDist);
			}
			if ((constrZ - spawnDist) < bboxMin.getBlockZ()) {
				bboxMin.setZ(constrZ - spawnDist);
			}
		}
		sendMessage(MsgLevel.DEBUG, "BoundingBox: " + bboxMin.toString() + " , " + bboxMax.toString());
		
		// 3. Find a suitable spawn by trying:
		Vector candidate = new Vector();
		Random rnd = new Random();
		int j;
		for (j = 0; j < respawnMaxTries; j++)
		{
			// Get a random pos in the bounding box:
			candidate = new Vector(
				rnd.nextInt(bboxMax.getBlockX() - bboxMin.getBlockX()) + bboxMin.getBlockX(),
				0,
				rnd.nextInt(bboxMax.getBlockZ() - bboxMin.getBlockZ()) + bboxMin.getBlockZ()
				);
			// Further skew result by shifting randomly within +/- randOffsetMax
			candidate.setX(candidate.getBlockX() + 
					       rnd.nextInt(2*respawnRandOffsetMax) - respawnRandOffsetMax);
			candidate.setZ(candidate.getBlockZ() + 
				       rnd.nextInt(2*respawnRandOffsetMax) - respawnRandOffsetMax);
			
			// Check the candidate:
			boolean allValid = true;
			for (PlayerData pd : constraints)
			{
				if (candidate.distance(pd.lastAlivePos) < spawnDist) { // not valid
					allValid = false;
					break;
				}
			}
			if (allValid) { // Yeah, found a good position!
				break;
			}
		}
		Location spawnLoc = new Location(serverWorld, candidate.getBlockX(), 0, candidate.getBlockZ());
		sendMessage(MsgLevel.DEBUG, "Found spawn with " + j + " tries.");
		
		/// 4. Add height and deliver
		spawnLoc.setY(serverWorld.getHighestBlockYAt(spawnLoc));
		sendMessage(MsgLevel.DEBUG, "Final spawn at: " + spawnLoc.toString() );
		return spawnLoc;
	}
	
	
	/// Write the current positions of online players to the save class
	private void updateOnlinePlayerSaves(SaveReason reason)
	{
		Collection<? extends Player> onPlayers = getServer().getOnlinePlayers();
		for (Player p : onPlayers) {
			if (  (p.getGameMode() == GameMode.SURVIVAL) &&
				  (p.getWorld()    == serverWorld) // Locations outside the normal world don't count.
			   )
			{
				HCRsaves.get().addOrUpdatePlayerEntry(p,reason);
			}
		}
		HCRsaves.get().save();
	}
	
	
	/// Supervised access to the pluginDir member.
	public File getPluginDir() {
		if (pluginDir == null) {
			sendMessage(MsgLevel.ERROR, "HCRPlugin::getPluginDir(): dir not set up but requested!");
		}
		return pluginDir;
	}
	
	
	/// Takes care not the last position, but his death location is saved when a player dies and is set to observer.
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		int respawnTime = HCRsettings.get().getIntegerSetting(Key.RESPAWN_TIME_SEC);
		Player player = event.getEntity();
		if (player.getWorld() == serverWorld)
		{
			HCRsaves.get().addOrUpdatePlayerEntry(player, SaveReason.DEATH);
			HCRsaves.get().save();
		}
		
		player.sendMessage("You are very dead. Respawn in: " + respawnTime + "s");
		player.sendMessage("Do NOT quit, go to observer mode!");
		ReviveTask task = new ReviveTask(respawnTime, player);
		task.schedule();
	}
	
	
	/// Takes care that the last player position is saved when he disconnects.
	/// Only if this player is currently playing alive.
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if (  (player.getGameMode() == GameMode.SURVIVAL) &&
			  (player.getWorld()    == serverWorld)
		   )
		{
			HCRsaves.get().addOrUpdatePlayerEntry(player, SaveReason.PLAYER_QUIT);
			HCRsaves.get().save();
		}
	}
	
	
	/// To prevent use of compasses too quickly, they are deactivated from crafting here.
//	@EventHandler
//    public void onPlayerCraft(CraftItemEvent e) {
//        if(e.getRecipe().getResult().getType() == Material.COMPASS){      // where "test" is random itemstack
//            e.getInventory().setResult(new ItemStack(Material.AIR));
//            sendMessage(MsgLevel.DEBUG, "Prevented crafting of a compass.");
//        }
//    }
	
	
	/// Called when the plugin is closed or disabled or the server shuts down.
	@Override
	public void onDisable() {
		// Backup the current player locations. only alive players count here, because deaths are tracked when they occur.
		// Offline players are tracked when they disconnect.
		updateOnlinePlayerSaves(SaveReason.SERVER_STOPPED);
	}

	
	/// Called when the plugin is loaded
	/// Creates the plugins directory on the server.
	@Override
	public void onEnable() {
		// Instantiations:
		instance = this;
		pluginDir = getDataFolder();
		if (!pluginDir.exists()) {
			if (!pluginDir.mkdir()) {
				getLogger().severe("Could not create directory for plugin!");
				return;
			}
		}
		
		try {
			HCRsaves.init();
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(MsgLevel.ERROR, "HCRsaves initialization failed. Aborting further init.");
			return;
		}
		
		try {
			HCRsettings.init();
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(MsgLevel.ERROR, "HCRsettings initialization failed. Aborting further init.");
			return;
		}
		
		serverWorld = Bukkit.getWorlds().get(0);
		
		// Register so we get Events: (This is why we need to implement Listener)
		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(this, this);
	}


	/// This callback can be used as-is, or registered as a callback for a messaging
	/// system.
	public Void sendMessage(MsgLevel level, String message) {
		String formatedMessage = level.getChatColor() + "[HCR] " + message;
		if (level.isAtLeastAsSeriousAs(consoleMessageLevel)) {
			getServer().getConsoleSender().sendMessage(formatedMessage);
		}

		if (level.isAtLeastAsSeriousAs(playerMessageLevel)) {
			Collection<? extends Player> onPlayers = getServer().getOnlinePlayers();
			for (Player p : onPlayers) {
				p.sendMessage(formatedMessage);
			}
		}
		return null; // to comply with the java Function handlers, objects need to be returned
	}
}

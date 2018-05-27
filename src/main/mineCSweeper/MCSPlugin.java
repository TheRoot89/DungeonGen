package mineCSweeper;

import java.io.File;
import java.util.Set;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mcPluginHelpers.MsgLevel;

/**Minecraft Plugin to host game of MineCSweeper. This enables testing and playing outside of DunGen.
 * The full game code shall be included in its own class, so the game can be added to any plugin.*/
public class MCSPlugin extends JavaPlugin {

	// ######################### Settings #####################################
	final static MsgLevel playerMessageLevel = MsgLevel.INFO;
	final static MsgLevel consoleMessageLevel = MsgLevel.DEBUG;
	
	// ########################## Member variables ############################
	private MCSGame game 		= null;	// Reference to the actual object handling the game
										// Also serves here as flag, whether everything was set up correctly
	private File	pluginDir;			// Directory of this plugin
	
	// ############################ Member functions ############################
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Input check:
		if (!(sender instanceof Player)) {
			sender.sendMessage("Cannot be executed from console!");
			return false;
		}
		Player player = (Player)sender;
		
		// check status:
		if (game.isInErrorState()) {
			player.sendMessage("Game is in Error state: " + game.getState().getMessage());
			player.sendMessage("Trying to reset...");
			resetPlugin();
			player.sendMessage("Plugin was reset. You may now try again. View the concole error log if persistent.");
			return true;
		}
		
		return interpretCommand(command.getName(), player, args);
	}

	private boolean interpretCommand(String command, Player player, String[] args) {
		if (command.equalsIgnoreCase("MCS_start")) {
			game.startInFrontOfPlayer(player);
			return true;
		}else if (command.equalsIgnoreCase("MCS_add")) {
			if (args.length < 1) {
				player.sendMessage("No player name given to add!");
				player.sendMessage("Usage:"); 
				return false;
			}
			// Now we know we have enough arguments:
			for (String playerName : args) {
				Player p = getServer().getPlayer(playerName);
				if (p == null) {
					player.sendMessage("No such player: " + playerName);
				}else {
					game.addPlayer(player);
				}
			}
			return true;
		}else if (command.equalsIgnoreCase("MCS_stop")) {
			game.stop();
			return true;
		}else if (command.equalsIgnoreCase("MCS_save")) {
			player.sendMessage("Saving settings to " + game.getSettings().getSettingsFile().toString() + "...");
			boolean result = game.getSettings().saveConfig();
			if (result) {
				player.sendMessage("Saved successfully.");
			}else {
				player.sendMessage("Failed!");
			}
			return true;
		}else if (command.equalsIgnoreCase("MCS_set")) {
			if (args.length < 2) {
				player.sendMessage("No settings keyword given, displaying current settings:");
				String[] settingsList = game.getSettings().getCurrentSettingsAsStringList();
				for (String setting : settingsList) {
					player.sendMessage(setting);
				}
				player.sendMessage("Usage:"); 
				return false;
			}
			// Now we know we have enough arguments:
			game.setOptionAndRestart(args[0], args[1]);
			return true;
		}
		
		// Command was not found if reached here:
		// Something must have gone wrong then, check your plugin.yml for consistency!
		player.sendMessage("Command not found!");
		return false;
	}
	
	
	@Override
	public void onDisable() {
		if (game != null)
			game.onDisable();
	}

	
	@Override
	public void onEnable() {
		initializePlugin();
	}

	
	private void initializePlugin() {
		//	1. Setup the plugin directory
		//	2. Upon success, create the game object
		pluginDir = getDataFolder();
		if (!pluginDir.exists())
			if(!pluginDir.mkdir()) {
				getLogger().severe("Could not create directory for plugin!");
				return;
			}
		game = new MCSGame(this);
		game.getState().registerMessageCallback(this::onStateMessage);;
	}
	
	
	public void resetPlugin() {
		if (game != null)
			game.onDisable();
		initializePlugin();
	}
	
	
	private Void onStateMessage(MsgLevel level, String message) {
		String formatedMessage = level.getChatColor() + "[MCS] " + message;
		if (level.isAtLeastAsSeriousAs(consoleMessageLevel)) {
			getServer().getConsoleSender().sendMessage(formatedMessage);
		}
		
		if (level.isAtLeastAsSeriousAs(playerMessageLevel)) {
			Set<Player> players = game.getPlayers();
			for (Player p : players) {
				p.sendMessage(formatedMessage);
			}
		}
		return null; // to comply with the java Function handlers, objects need to be returned
	}
	
	
	
	
}

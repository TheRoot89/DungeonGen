package mineCSweeper;



import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**Minecraft Plugin to host game of MineCSweeper. This enables testing and playing outside of DunGen.
 * The full game code shall be included in its own class, so the game can be added to any plugin.*/
public class MCSPlugin extends JavaPlugin {

	
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
		
		// check state:
		if (game == null) {
			player.sendMessage("The game was not initialized correctly! Trying to reset...");
			resetPlugin();
			player.sendMessage("Plugin was reset. You may now try again.");
			return false;
		}
		// React to defined commands:
		if (command.getName().equalsIgnoreCase("MCS_start")) {
			player.sendMessage("MineCraftSweeper started!");
			game.start();
			return true;
		}else if (command.getName().equalsIgnoreCase("MCS_stop")) {
			player.sendMessage("MineCraftSweeper stopped.");
			game.stop();
			return true;
		}else if (command.getName().equalsIgnoreCase("MCS_restart")) {
			player.sendMessage("MineCraftSweeper restarted.");
			game.restart();
			return true;
		}else if (command.getName().equalsIgnoreCase("MCS_set")) {
			boolean keyExists = game.setOptionIfKeyExists(args[0], args[1]);
			if (!keyExists)
				player.sendMessage("This setting keyword is unknown!");
			return true;
		}
		// Command was not found if reached here:
		return false;
	}

	
	@Override
	public void onDisable() {
		if (game != null) game.onDisable();
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
	}
	
	
	public void resetPlugin() {
		if (game != null) game.onDisable();
		initializePlugin();
	}
	
	
	
}

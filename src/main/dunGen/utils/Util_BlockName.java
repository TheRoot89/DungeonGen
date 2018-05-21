package dunGen.utils;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


/** Utility able to receive mc commands. Shows the name of a block looked at.*/
public class Util_BlockName implements CommandExecutor {
	
	private JavaPlugin plugin;		// the reference to the main plugin, for player access.
	
	/**The utility has to be instantiated to receive commands form the server.
	 * @param plugin the reference to the main plugin, for player access.*/
	public Util_BlockName(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("BlockName")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(plugin.getName() + ": Utility BlockName may only be used from ingame!");
			} else {
				Player player = (Player) sender;
				Block b = player.getTargetBlock(null, 10);
				player.sendMessage(b.getType().name());
				player.sendMessage("Block data: " + b.getData());
				
			}
			return true;
		}
		
		return false;
	}

}

package dunGen.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


/** Receives commands to start and stop the utility. */
public class Util_BlockName implements CommandExecutor {
	
	private JavaPlugin plugin;
	
	public Util_BlockName(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("BlockName")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(plugin.getName() + ": Utility BlockName may only be used from ingame!");
			} else {
				Player player = (Player) sender;
				player.sendMessage(player.getTargetBlock(null, 10).getType().name());
			}
			return true;
		}
		
		return false;
	}

}

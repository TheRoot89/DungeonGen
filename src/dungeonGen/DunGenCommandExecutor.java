package dungeonGen;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class DunGenCommandExecutor implements CommandExecutor {
	private final DungeonGen plugin;
	
	public DunGenCommandExecutor(DungeonGen plugin) {
		this.plugin = plugin;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("test")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(plugin.getName() + ": Executing test from console.");
			} else {
				Player player = (Player) sender;
				player.sendMessage(plugin.getName() + ": Executing test from ingame.");
			}
			return true;
		}
		
		return false;
	}

}

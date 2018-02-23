package dunGen;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**Listener to handle in-game events. Some utility functions can be implemented here and not as callbacks in the plugin.
 * This will further decrease the clutter in the main class.
 * It is also possible to set the behavior for events in a config and have this class load its settings.
 * The listener may also refer to a gameRule object to get this data.
 */
public class InGameListener implements Listener {

	DunGen parent;
	
	public InGameListener(DunGen parent) {
		this.parent = parent;
	}
	
	
	/**Registers this Listener to receive events needed.*/
	public void register() {
		parent.getServer().getPluginManager().registerEvents(this, parent);
	}
	
	/**Removes this Listener from receiving any events.*/
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
	
	/**Handles actions to be taken upon respawn.
	 * Currently gives new starting gear.
	 * @param event The event given to this handler by the event manager.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerRespawn(PlayerRespawnEvent event) {

		giveStartingGear(event.getPlayer());
		event.getPlayer().updateInventory();

	}
	
	
	/**Gives the players starting gear, called during dungeon startup.
     * @param p 	The player to give stuff to.
     */
	void giveStartingGear(Player p) {
		PlayerInventory i = p.getInventory();
		i.clear();
		i.addItem( new ItemStack(Material.STONE_SWORD, 	 1));
		i.addItem( new ItemStack(Material.BOW, 			 1));
		i.addItem( new ItemStack(Material.ARROW, 		 1));
		i.addItem( new ItemStack(Material.MUSHROOM_SOUP, 1));

		i.setBoots(new ItemStack(Material.LEATHER_BOOTS, 1));
  }
  
	
}

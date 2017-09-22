package dungeonGen;

// TODO Use Gate-Class or adaptor modules to adapt different door sizes
// TODO add spawnlocations

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.Region;

public class PassageWay extends Module {
	
	private Vector respawnLoc;
	
	//automatically call Module-Contructor upon creation.
	public PassageWay(DungeonGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	/**
	 * Method is called before it is placed during the place() method within 'Module'.
	 * It is meant to perfom specific tasks for this Module Subclass, like registering Listeners.
	 */
	@Override
	public void prePlacementActions() {
		// nothing special for passageWays atm
	}

	@Override
	public void register() {
		parent.getServer().getPluginManager().registerEvents(this, parent);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onNextRoomButtonPress(PlayerInteractEvent event) {
	    //Player p = event.getPlayer();
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
	        Block clicked = event.getClickedBlock();
	        if (   clicked.getType() == Material.STONE_BUTTON 
	        	&& modVolume.contains(BukkitUtil.toVector(clicked.getLocation())) ) {
	        	unregister(); // button cannot be pushed twice
	        	parent.genNextRoom();
	        }
	    }
	}
	
	public void toggleEntry(boolean open) {
		// Code for only setting/removing blocks in the way:
		Vector curV;
		for (int h = 0; h < entryHeight; h++) {
			for (int w = 0; w < entryWidth; w++) {
				curV = toGlobal(entryLoc.add(0, h, w));// to right:z, up:y  (doors always start lower left)
				if (open)
					parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(Material.AIR);
				else
					parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(Material.COBBLESTONE);
			}
		}
	}
	
	public void toggleExit(boolean open) {
		// Code for only setting/removing blocks in the way:
		Vector curV;
		for (int h = 0; h < exitHeight; h++) {
			for (int w = 0; w < exitWidth; w++) {
				curV = toGlobal(exitLoc.add(0, h, w));// to right:z, up:y  (doors always start lower left)
				if (open)
					parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(Material.AIR);
				else
					parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(Material.COBBLESTONE);
			}
		}
	}

	@Override
	public void unregister() {
		PlayerInteractEvent.getHandlerList().unregister(this);
	}

	// loads properties special for passageWays
	@Override
	public void loadConfig() {
		super.loadConfig();
		respawnLoc = BukkitUtil.toVector(conf.getVector("respawnLoc"));
	}

	@Override
	public void postPlacementActions() {
		// no spawning etc. to be done here atm
		// TODO Add Loot in passageWays
		
	}
	
}

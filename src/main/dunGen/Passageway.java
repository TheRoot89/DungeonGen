package dunGen;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import dunGen.DunGen.State;
import mcPluginHelpers.Direc;

/**Represents a Module connecting two actual rooms. Intermediate things may take place here.
 * Doors are a part of Passageways, not rooms. Entry and exit may be set separately and may have different mechanisms.
 * Also, players will respawn in the previous Passageway.
 */
public class Passageway extends Module {
	
	
	/** Describes how a door opens and closes. Switches code flow in the appropriate sections. */
	public enum DoorType{
		APPEARING,
		FALLING,
		PISTON;
	}
	
	
	// ############# Member variables ##############
	private DoorType entryType;
	private DoorType exitType;
	public  Vector 	 respawnLoc;
	
	
	/**Constructor, calls Module-Contructor. Then loads config values.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public Passageway(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	
	/**Convenience function to generate a wall of a certain block material. Gets the world from parent to do so.
	 * @param relLowerLeft	Relative coordinate of the lower left of the wall to be generated.
	 * @param height		The height of the wall.
	 * @param width			The width of the wall.
	 * @param material		Material = Block type to be generated.
	 */
	private void genWall(Vector relLowerLeft, int height, int width, Material material) {
		Vector curV;
		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {
				curV = toGlobal(relLowerLeft.add(0, h, w));// to right:z, up:y  (doors always start lower left)
				parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(material,true);
			}
		}
		parent.world.getBlockAt(relLowerLeft.getBlockX(),relLowerLeft.getBlockY(),relLowerLeft.getBlockZ()).getState().update(true, true);
	}

	
	/**Calls the Module::loadConfig() for basic values. Then loads the properties special for passageWays.
	 * Checks are not needed any more due to initial yml checks.*/
	@Override
	public void loadConfig() {
		super.loadConfig();
		
		respawnLoc = BukkitUtil.toVector(conf.getVector("respawnLoc"));
		
		// entry:
		entryType  = DoorType.valueOf(getConfig(parent, name).getString("entry.type").toUpperCase());
		
		switch (entryType) {
		case APPEARING:
		case FALLING:
			String matName = conf.getString("entry.doorMaterial").toUpperCase(Locale.ENGLISH); //to upper case
			entry.doorMaterial = Material.getMaterial(matName); //this is a lookup 'string' -> 'enum value'
			break;
		case PISTON: // do not load Material if redstone powered door (case PISTON)
			entry.redstonePos = BukkitUtil.toVector(conf.getVector("entry.redstoneLoc"));
			break;
		}
		
		// exit:
		exitType = DoorType.valueOf(getConfig(parent, name).getString("exit.type").toUpperCase());
		switch (exitType) {
		case APPEARING:
		case FALLING:
			String matName = conf.getString("exit.doorMaterial").toUpperCase(Locale.ENGLISH);//to upper case
			exit.doorMaterial = Material.getMaterial(matName); //this is a lookup 'string' -> 'enum value'
			break;
		case PISTON: // do not load Material if redstone powered door (case PISTON)
			exit.redstonePos = BukkitUtil.toVector(conf.getVector("exit.redstoneLoc"));
			break;
		}
	}
	
	
	/**Event handler for a player pressing the button of this Passageway. Will callback to DunGen to generate the next DunGen part.
	 * Also moves all other players to this Passageway.
	 * @param event	The event to be handled, given by the event system.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onNextRoomButtonPress(PlayerInteractEvent event) {
	    //Player p = event.getPlayer();
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
	        Block clicked = event.getClickedBlock();
	        if (   clicked.getType() == Material.STONE_BUTTON 
	        	&& modVolume.contains(BukkitUtil.toVector(clicked.getLocation())) ) {
	        	
	        	unregister(); // button cannot be pushed twice
	        	// If double button push appears more often, then use flag here
	        	// Here we need the activePlayers list. This is not present in the entry, so don't teleport during STARTUP phase.
	        	// This might be solvable better somehow...
	        	
	        	// only teleport if the dungeon runs, not during entry startup state
	        	if (parent.state == State.RUNNING) 
		        	for (Player p : parent.activePlayers) {
		        		if (!modVolume.contains(BukkitUtil.toVector(p.getLocation()))) {
		        			p.teleport(BukkitUtil.toLocation(parent.world, toGlobal(respawnLoc).add(0.5, 0, 0.5)));
		        		}
		        	}
	        	
	        	parent.genNextRoom();
	        }
	    }
	}
	
	
	@Override
	public void postPlacementActions() {
		// no spawning etc. to be done here atm
		// register() could be called here?
	}
	
	
	@Override
	public void prePlacementActions() {
		// nothing special for passageWays atm
	}
	
	
	@Override
	public void register() {
		parent.getServer().getPluginManager().registerEvents(this, parent);
	}

	
	/** Opens or closes the entry to this Passageway according to its DoorType.
	 * @param open	true: opens, false: closes
	 */
	public void toggleEntry(boolean open) {
		switch (entryType) {
		case APPEARING:
			if (open)
				genWall(entry.doorLoc,entry.height,entry.width,Material.AIR);
			else//close
				genWall(entry.doorLoc,entry.height,entry.width,entry.doorMaterial);
			break;
		case FALLING:
			Vector v_above = entry.doorLoc.add(0,entry.height,0); // the door location but height higher for stuff to fall down
			Vector v_below = entry.doorLoc.add(0,-entry.height,0); // the door location but height deeper
			if (open) {
				genWall(v_below,entry.height,entry.width,Material.AIR);
				genWall(v_above,entry.height,entry.width,Material.LEGACY_SMOOTH_BRICK);
			}else{//close
				genWall(v_above,entry.height,entry.width,entry.doorMaterial);
			}
			break;
		case PISTON:
			Vector v = toGlobal(entry.redstonePos);
			if (open)
				parent.world.getBlockAt(v.getBlockX(),v.getBlockY(),v.getBlockZ()).setType(Material.AIR);
			else //close
				parent.world.getBlockAt(v.getBlockX(),v.getBlockY(),v.getBlockZ()).setType(Material.REDSTONE_BLOCK);
			break;
		}
	}

	
	/** Opens or closes the exit of this Passageway according to its DoorType.
	 * @param open	true: opens, false: closes
	 */
	public void toggleExit(boolean open) {
		switch (exitType) {
		case APPEARING:
			if (open)
				genWall(exit.doorLoc,exit.height,exit.width,Material.AIR);
			else//close
				genWall(exit.doorLoc,exit.height,exit.width,exit.doorMaterial);
			break;
		case FALLING:
			Vector v_above = exit.doorLoc.add(0,exit.height,0); // the door location but height higher for stuff to fall down
			Vector v_below = exit.doorLoc.add(0,-exit.height,0); // the door location but height deeper
			if (open) {
				genWall(v_below,exit.height,exit.width,Material.AIR);
				genWall(v_above,exit.height,exit.width,Material.LEGACY_SMOOTH_BRICK);
			}else{//close
				genWall(v_above,exit.height,exit.width,exit.doorMaterial);
			}
			break;
		case PISTON:
			Vector v = toGlobal(exit.redstonePos);
			if (open)
				parent.world.getBlockAt(v.getBlockX(),v.getBlockY(),v.getBlockZ()).setType(Material.AIR);
			else //close
				parent.world.getBlockAt(v.getBlockX(),v.getBlockY(),v.getBlockZ()).setType(Material.REDSTONE_BLOCK);
			break;
		}
	}

	
	@Override
	public void unregister() {
		PlayerInteractEvent.getHandlerList().unregister(this);
	}
}

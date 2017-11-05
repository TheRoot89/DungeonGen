package dungeonGen;

import java.util.Locale;

// TODO Use Gate-Class or adaptor modules to adapt different door sizes

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.Region;

public class PassageWay extends Module {
	
	public Vector respawnLoc;
	private DoorType entryType;
	private DoorType exitType;
	
	protected enum DoorType{
		APPEARING,
		FALLING,
		PISTON
	}
	
	
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
	        	// TODO if double button push appears more often, then use flag here
	        	for (Player p : parent.activePlayers) {
	        		if (!modVolume.contains(BukkitUtil.toVector(p.getLocation()))) {
	        			p.teleport(BukkitUtil.toLocation(parent.world, toGlobal(respawnLoc).add(0.5, 0, 0.5)));
	        		}
	        	}
	        	parent.genNextRoom();
	        }
	    }
	}
	
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
				genWall(v_above,entry.height,entry.width,Material.SMOOTH_BRICK);
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
	
	private void genWall(Vector relLowerLeft, int height, int width, Material mat) {
		Vector curV;
		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {
				curV = toGlobal(relLowerLeft.add(0, h, w));// to right:z, up:y  (doors always start lower left)
				parent.world.getBlockAt(curV.getBlockX(),curV.getBlockY(),curV.getBlockZ()).setType(mat);
			}
		}
	}
	
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
				genWall(v_above,exit.height,exit.width,Material.SMOOTH_BRICK);
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

	// loads properties special for passageWays, add more door stuff here
	@Override
	public void loadConfig() {
		super.loadConfig();
		if (conf.contains("respawnLoc"))
			respawnLoc = BukkitUtil.toVector(conf.getVector("respawnLoc"));
		else
			respawnLoc = entry.doorLoc.add(new Vector(1,0,0));
		
		// entry:
		if (conf.contains("entry.type"))
			entryType = DoorType.values()[conf.getInt("entry.type")];
		else
			entryType = DoorType.APPEARING;
		
		switch (entryType) {
		case APPEARING:
		case FALLING:
			String matName = conf.getString("entry.doorMaterial").toUpperCase(Locale.ENGLISH);//to upper case
			entry.doorMaterial = Material.getMaterial(matName); //this is a lookup 'string' -> 'enum value'
			break;
		case PISTON: // do not load Material if redstone powered door (case PISTON)
			entry.redstonePos = BukkitUtil.toVector(conf.getVector("entry.redstoneLoc"));
			break;
		}
		
		// exit:
		if (conf.contains("exit.type"))
			exitType = DoorType.values()[conf.getInt("exit.type")];
		else
			exitType = DoorType.APPEARING;
		
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

	@Override
	public void postPlacementActions() {
		// no spawning etc. to be done here atm
	}
	
}

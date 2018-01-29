package dunGen;

import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper.Direc;

public class PlatformRoom extends Room {
	
	CuboidRegion targetReg; 		// Region to be reached as victory condition for PlatformRooms
	Vector 		 targetRegCorner1;
	Vector 		 targetRegCorner2;
	
	
	// ######################### Member functions: ##########################
	
	/**Constructor takes same arguments as a Module and Room and forwards these. Then loads config.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public PlatformRoom(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	
	/**
	 * PlatformRoom checks whether one player has reached the target area.
	 * @return Void only for method reference implementation. Return null.
	 */
	@Override
	public Void checkRoomDone(Void v) {
		for (Player p : parent.activePlayers) { // for loop with iterator over collection in short syntax
			Vector ppos = BukkitUtil.toVector(p.getLocation()).floor(); // round downwards as player coords and block coords off by 0.5
			if (targetReg.contains(ppos)) {
				checkTask.cancel();
				parent.roomClear();
				break;
			}
		}
		return null;
	}

	
	@Override
	public void loadConfig() {
		super.loadConfig();
		targetRegCorner1 = BukkitUtil.toVector(conf.getVector("targetRegCorner1"));
		targetRegCorner2 = BukkitUtil.toVector(conf.getVector("targetRegCorner2"));
	}
	
	
	@Override
	public void prePlacementActions() {
		// nothing atm
	}


	@Override
	public void register() {
		super.register();
	}

	
	@Override
	public void postPlacementActions() {
		super.postPlacementActions(); // nothing is done there at the moment.
		targetReg = new CuboidRegion(toGlobal(targetRegCorner1), toGlobal(targetRegCorner2)); // can only be converted to global now
	}
}

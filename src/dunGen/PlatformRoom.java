package dunGen;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper.Direc;

public class PlatformRoom extends Room {
	
	// target area is given to the checking task and saved there in global coordinates
	Vector targetRegCorner1;
	//Vector corner1Glob;
	Vector targetRegCorner2;
	//Vector corner2Glob;
	CuboidRegion targetReg;
	
	public PlatformRoom(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		targetRegCorner1 = BukkitUtil.toVector(conf.getVector("targetRegCorner1"));
		targetRegCorner2 = BukkitUtil.toVector(conf.getVector("targetRegCorner2"));
	}

	@Override
	public void register() {
		super.register();
		// prepare the region check:
		//corner1Glob = toGlobal(targetRegCorner1);
		//corner2Glob = toGlobal(targetRegCorner2);
		targetReg = new CuboidRegion(toGlobal(targetRegCorner1), toGlobal(targetRegCorner2));
	}
	
	@Override
	public void prePlacementActions() {
		//Evtl. Scripte und Timer anregen die irgendwelche dinge tun, die genaue Variante könnte in Config stehen?
	}


	/**
	 * PlatformRoom checks, whether one player has reached the target area.
	 */
	@Override
	public void checkRoomDone() {
		for (Player p : parent.activePlayers) { // for loop with iterator over collection in short syntax
			Vector ppos = BukkitUtil.toVector(p.getLocation()).floor(); // round downwards as player coords and block coords off by 0.5
			if (targetReg.contains(ppos)) {
				checkTask.cancel();
				parent.roomClear();
			}
		}
	}

	/*@Override
	public void postPlacementActions() {
		// special stuff here will have to call super.postPlacementActions() and remove its final
	}*/
}

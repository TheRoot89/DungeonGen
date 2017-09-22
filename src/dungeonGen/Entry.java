package dungeonGen;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;

public class Entry extends PassageWay{
	
	//automatically calls PassageWay-Contructor upon creation
	public Entry(DungeonGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}
	
	@Override
	public void prePlacementActions() {
		// nothing to do at the moment
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		// entries can be placed relative to a different pos., than entryLoc:
		if (conf.contains("placementLoc"));
			placementLoc  = BukkitUtil.toVector(conf.getVector("placementLoc"));
	}
	
}

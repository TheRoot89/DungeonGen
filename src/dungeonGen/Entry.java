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
	}

}

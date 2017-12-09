package dunGen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper.Direc;

public class BattleRoom extends Room {
	
	public BattleRoom(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
	}

	@Override
	public void prePlacementActions() {
		// nothing atm
	}

	@Override
	public Void checkRoomDone(Void v) {
		Iterator<Entity> iter = trackedEnemies.iterator();
		while (iter.hasNext()) {
			if (iter.next().isDead())
				iter.remove(); // makes list shorter for each removed so this stays efficient
			else
				return null;
		}
				
		// all are dead if this point is reached:
		unregister();
		parent.roomClear();
		return null; //return Void is needed
	}
}

package dunGen;

import java.util.Iterator;

import org.bukkit.entity.Entity;

import com.sk89q.worldedit.Vector;

import dunGen.Helper.Direc;

/**A Room with victory condition that all target entities are dead.*/
public class BattleRoom extends Room {
	
	/**Constructor takes same arguments as a Module and Room and forwards these. Then loads config.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public BattleRoom(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	
	@Override
	public Void checkRoomDone(Void v) {
		// check if all enemies that are a target are dead:
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

	
	@Override
	public void loadConfig() {
		super.loadConfig();
		// nothing special here.
	}

	
	@Override
	public void prePlacementActions() {
		// nothing atm
	}
}

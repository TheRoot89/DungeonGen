package scheduledTasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;

import dunGen.EnemyGroup;
import dunGen.Room;

public class SpawnGroupTask extends BukkitRunnable {
	
	Room parent;	// to invoke the room-specific check-method
	EnemyGroup grp;
	
	public SpawnGroupTask(Room parent, EnemyGroup grp) {
		this.parent = parent;
		this.grp = grp;
	}
	
	@Override
	public void run() {
		parent.spawnGroup(grp);
	}
}

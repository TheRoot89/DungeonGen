package scheduledTasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;

import dunGen.Room;

public class EmpowerTask extends BukkitRunnable {
	
	Room parent;	// to invoke the room-specific check-method
	
	public EmpowerTask(Room parent) {
		this.parent = parent;
	}
	
	@Override
	public void run() {
		parent.empower();
	}
}

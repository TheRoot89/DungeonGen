package scheduledTasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;

import dunGen.Room;

public class DepowerTask extends BukkitRunnable {
	
	Room parent;	// to invoke the room-specific check-method
	
	public DepowerTask(Room parent) {
		this.parent = parent;
	}
	
	@Override
	public void run() {
		parent.depower();
	}
}

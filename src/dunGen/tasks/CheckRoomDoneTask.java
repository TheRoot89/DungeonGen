package dunGen.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;

import dunGen.Room;

public class CheckRoomDoneTask extends BukkitRunnable {
	
	Room parent;	// to invoke the room-specific check-method
	
	public CheckRoomDoneTask(Room parent) {
		this.parent = parent;
	}
	
	@Override
	public void run() {
		parent.checkRoomDone();
	}
}

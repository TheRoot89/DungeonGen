package dunGen.tasks;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Room;

public abstract class RoomTask extends BukkitRunnable {
	
	protected Room 			parent;			// For being able to register itself
	protected int 			taskNr;			// Nr of this taks in the room's list
	protected TaskType 		type;			// type of this RoomTask, fix in subtypes
	protected CuboidRegion  targetRegion;  	// region this task applies to. Depends on the specification of this class,
											// whether this made global or stays relative. Loaded as relative here.
	private   double 		delay;			// Values for timing the runnable. [s]
	protected double 		period;			// A value of zero means no delay / no period = single shot, [s]
	
	
	public RoomTask(Room parent, FileConfiguration conf,int taskNr) {
		this.parent = parent;
		this.taskNr = taskNr;
		
		// Load RoomTask values for Task i values from config:
		String path = "tasks.task" + this.taskNr + ".";
		
		type   =   			TaskType.valueOf(conf.getString(path + "type"));
		delay  =  							 conf.getDouble(path + "delay",  0);
		period = 							 conf.getDouble(path + "period", 0);
		targetRegion = new CuboidRegion(BukkitUtil.toVector(conf.getVector(path + "regionCorner1",new org.bukkit.util.Vector())),
				                        BukkitUtil.toVector(conf.getVector(path + "regionCorner2",new org.bukkit.util.Vector())));
	}
	
	
	/**
	 * Activates this runnable with the settings it loaded from the config given during contruction.
	 * No repetition if period == 0
	 */
	public void register() {
		if (period == 0)
			runTaskTimer(parent.getPlugin(), Math.round(delay*20), Math.round(period*20));
		else
			runTaskLater(parent.getPlugin(), Math.round(delay*20));
	}
	
	
	public enum TaskType {
		ENTITYSPAWN,
		BLOCKSPAWN,
		POWER;
	}
}

package dunGen.tasks;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Room;

/**The task superclass, implementing the general task properties, such as delay, period etc.
 * This is inherited by tasks actually doing something in run() and loading meaningfull
 * additional data from the config.
 */
public class RoomTask extends BukkitRunnable {
	

	/**The Task type defines what the task does. This is fixed in the RoomTask subclasses. */
	public enum TaskType {
		BLOCKSPAWN,
		ENTITYSPAWN,
		POWER,
		MAZE;
	}
	
	
	// ############################ Member variables ##########################
	
	protected double 		delay;			// Values for timing the runnable. [s]
	protected Room 			parent;			// For being able to register itself
	protected double 		period;			// A value of zero means no delay / no period = single shot, [s]
	protected int			executionCount;	// Number of times this task shall be executed
	protected CuboidRegion  targetRegion;  	// Region this task applies to. Depends on the specification of this class,
											// whether this made global or stays relative. Loaded as relative initially.
	protected int 			taskNr;			// Nr of this task in the room's list
	protected TaskType 		type;			// type of this RoomTask, fix in subtypes
	
	private int				runCounter = 0; // Counts how many times run() was executed
	
	
	// ############################## Member functions ##############################
	
	/**Constructor takes a back-reference to do actions and loads itself via config and TaskNr.
	 * @param parent	The Room this Task belongs to
	 * @param conf		Given config file of this room has entries on tasks.
	 * @param taskNr	Task number is needed to load keys correctly.
	 */
	public RoomTask(Room parent, FileConfiguration conf,int taskNr) {
		this.parent = parent;
		this.taskNr = taskNr;
		
		// Load RoomTask values for Task i values from config:
		String path = "tasks.task" + this.taskNr + ".";
		
		type   =   			TaskType.valueOf(conf.getString(path + "type").trim().toUpperCase());
		delay  =  							 conf.getDouble(path + "delay",  0);
		period = 							 conf.getDouble(path + "period", 0);
		executionCount = 					 conf.getInt(path + "executionCount", 1);
		targetRegion = new CuboidRegion(BukkitUtil.toVector(conf.getVector(path + "regionCorner1",new org.bukkit.util.Vector())),
				                        BukkitUtil.toVector(conf.getVector(path + "regionCorner2",new org.bukkit.util.Vector())));
	}
	
	
	/**Activates this runnable with the settings it loaded from the config given during contruction.
	 * No repetition if period == 0
	 */
	public void register() {
		if (period == 0 || executionCount <= 1)
			runTaskLater(parent.getPlugin(), Math.round(delay*20));
		else
			runTaskTimer(parent.getPlugin(), Math.round(delay*20), Math.round(period*20));
	}


	@Override
	public void run() {
		runCounter++;
		if (runCounter >= executionCount) cancel();
	}
	
}

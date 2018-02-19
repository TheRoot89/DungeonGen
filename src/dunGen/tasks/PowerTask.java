package dunGen.tasks;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper;
import dunGen.Room;

/**A task to toggle power at a specific location by providing a redstone block there.
 * The on-time where the block is there may be given, where the rest of the period will have an air block at the given position.
 */
public class PowerTask extends RoomTask {

	private boolean convertedToGlobal = false;	// Flag whether targetRegion has been converted
	private TaskWithCallback offTask  = null;			// Internal Task to switch off the redstone again after some time given by onTime [%].
	private double onTime;			    		// percentage of period, where the redstone is active
	
	
	/**Constructor, passes arguments to super class and loads special values from config.
	 * @param parent	The Room this Task belongs to
	 * @param conf		Given config file of this room has entries on tasks.
	 * @param taskNr	Task number is needed to load keys correctly.
	 */
	public PowerTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.POWER;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		onTime  = conf.getDouble(path + "onTime", 0.0);
	}

	
	/**Sets the targetRegion to AIR, removing any redstone blocks there
	 * @param v Void object to make this usable as method reference.
	 * @return Void object to make this usable as method reference.
	 */
	private Void depower(Void v) {
		Helper.fillVolume(parent.getPlugin().world, targetRegion.getPos1(), targetRegion.getPos2(), Material.AIR);
		return null;
	}

	
	/** Basically creates redstone blocks in the targetRegion. */
	private void empower() {
		Helper.fillVolume(parent.getPlugin().world, targetRegion.getPos1(), targetRegion.getPos2(), Material.REDSTONE_BLOCK);
	}
	
	
	/**The activation for PowerTasks also triggers a deactivation if so configured.
	 * No repetition if period == 0
	 */
	@Override
	public void register() {
		if (period == 0 || onTime == 1 || onTime == 0) {
			runTaskLater(parent.getPlugin(), Math.round(delay*20));
		} else {
			runTaskTimer(parent.getPlugin(), Math.round(delay*20), Math.round(period*20));
			// Also run the task for switching off:
			offTask = new TaskWithCallback(this::depower);	
			long endOfOn = Math.round(delay*20) + Math.round(period*onTime*20);
			offTask.runTaskTimer(parent.getPlugin(), endOfOn, Math.round(period*20)); // calls depower()
		}
	}
	
	@Override
	public void run() {
		// This only runs once if period is zero
		
		// convert only once, after placement:
		//TODO: do in RoomTask!?
		if (!convertedToGlobal) {
			targetRegion = new CuboidRegion(parent.toGlobal(targetRegion.getPos1()), parent.toGlobal(targetRegion.getPos2()));
			convertedToGlobal = true;
		}
		
		// Spawns air only if onTime == 0
		// Redstone only if onTime == 1
		// Redstone and Air after it if onTime between 0 and 1 and a period is present
		if (onTime == 0)	
			depower(null);
		else
			empower();
		
		// parent run has to be called for counter!
		super.run();
	}
}

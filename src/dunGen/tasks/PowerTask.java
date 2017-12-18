package dunGen.tasks;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper;
import dunGen.Room;

public class PowerTask extends RoomTask {

	private double onTime;			 // percentage of period, where the redstone is active
	private TaskWithCallback offTask;
	
	public PowerTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.POWER;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		onTime = conf.getDouble(path + "onTime", 0.0);
		
		// Initialize the callback task for switching off after a certain time:
		offTask = new TaskWithCallback(this::depower);
		
		// Conversion of targetRegion to global coordinates:
		// This conversion thus happens only once (more efficient)
		Vector pos1_glob = parent.toGlobal(targetRegion.getPos1());
		Vector pos2_glob = parent.toGlobal(targetRegion.getPos2());
		targetRegion = new CuboidRegion(pos1_glob, pos2_glob);
	}

	
	@Override
	public void run() {
		// This only runs once if period is zero
		// Spawns air only if onTime == 0
		// Redstone only if onTime == 1
		// Redstone and Air after it if onTime between 0 and 1 and a period is present
		if (onTime == 0)	
			depower(null);
		else {
			empower();
			if (onTime < 1 && period != 0) {
				long onTicks = Math.round(period*onTime*20);
				offTask.runTaskLater(parent.getPlugin(), onTicks); // calls depower()
			}
		}
	}

	private void empower() {
		Helper.fillVolume(parent.getPlugin().world, targetRegion.getPos1(), targetRegion.getPos2(), Material.REDSTONE_BLOCK);
	}
	
	// Special void formulation to be useable as callback:
	private Void depower(Void v) {
		Helper.fillVolume(parent.getPlugin().world, targetRegion.getPos1(), targetRegion.getPos2(), Material.AIR);
		return null;
	}
	
}

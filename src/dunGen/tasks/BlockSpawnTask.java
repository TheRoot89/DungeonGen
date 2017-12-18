package dunGen.tasks;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.RegionOperationException;

import dunGen.Helper;
import dunGen.Room;

public class BlockSpawnTask extends RoomTask {

	private Material blockMaterial;
	private Vector incrementVec;
	
	public BlockSpawnTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.BLOCKSPAWN;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		blockMaterial = Material.getMaterial(path + "blockType"); // this is a lookup 'string' -> 'enum value'
		incrementVec = BukkitUtil.toVector(conf.getVector(path + "incrementVector",new org.bukkit.util.Vector())); // default no increment
		
		// The targetRegion is not converted to global here, as it is potentially incremented each cycle.
	}

	
	@Override
	public void run() {
		World world = parent.getPlugin().world;

		Helper.fillVolume(world, parent.toGlobal(targetRegion.getPos1()),
				                 parent.toGlobal(targetRegion.getPos2()), blockMaterial);
		try {
			targetRegion.shift(incrementVec);
		} catch (RegionOperationException e) {
			parent.getPlugin().getLogger().info("Shifting target region for room task failded!");
			e.printStackTrace();
		}
	}

}

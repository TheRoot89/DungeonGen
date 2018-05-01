package dunGen.tasks;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.RegionOperationException;

import dunGen.Room;
import mcPluginHelpers.MCHelpers;

/**A special Task that spawns blocks in the targetRegion. This may be shifted with each iteration. */
public class BlockSpawnTask extends RoomTask {

	private Material blockMaterial;		// The block type to be spawned
	private Vector incrementVec;		// Shift of the targetRegion. Enables rising water etc.
	
	/**Constructor, passes arguments to super class and loads special values from config.
	 * @param parent	The Room this Task belongs to
	 * @param conf		Given config file of this room has entries on tasks.
	 * @param taskNr	Task number is needed to load keys correctly.
	 */
	public BlockSpawnTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.BLOCKSPAWN;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		blockMaterial = Material.getMaterial(conf.getString(path + "blockType").toUpperCase(Locale.ENGLISH)); // this is a lookup 'string' -> 'enum value'
		incrementVec =   BukkitUtil.toVector(conf.getVector(path + "incrementVector",new org.bukkit.util.Vector())); // default no increment
		
		// The targetRegion is not converted to global here, as it is potentially incremented each cycle with a relative vector.
	}

	
	@Override
	public void run() {
		World world = parent.getPlugin().world;

		MCHelpers.fillVolume(world, parent.toGlobal(targetRegion.getPos1()),
				                 parent.toGlobal(targetRegion.getPos2()), blockMaterial);
		try {
			targetRegion.shift(incrementVec);
		} catch (RegionOperationException e) {
			parent.getPlugin().getLogger().info("Shifting target region for room task failded!");
			e.printStackTrace();
		}
		
		// parent run has to be called for counter!
		super.run();
	}

}

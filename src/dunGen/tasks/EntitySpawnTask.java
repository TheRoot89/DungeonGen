package dunGen.tasks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.Helper;
import dunGen.Room;

public class EntitySpawnTask extends RoomTask {

	/**
	 * Struct-like internal class representing an enemy group in a room (with default values)
	 */
	public class EntityGroup {
		public int count = 0;
		public boolean isTarget = false;
		public int maxCount = 10;
		public EntityType type = EntityType.ZOMBIE;
	}
	
	// ################## Members: ######################
	
	EntityGroup grp;	// The group of enemies to be spawned with this Task
	
	/**Constructor, passes arguments to super class and loads special values from config.
	 * @param parent	The Room this Task belongs to
	 * @param conf		Given config file of this room has entries on tasks.
	 * @param taskNr	Task number is needed to load keys correctly.
	 */
	public EntitySpawnTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.ENTITYSPAWN;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		grp = new EntityGroup();
		grp.type = EntityType.valueOf(conf.getString( path + "entityType"));
		grp.count = 				  conf.getInt(    path + "count");
		//TODO: maxCount noch einbauen -> bei jedem Spawning z�hlen, bei Tod runterz�hlen
		grp.maxCount =				  conf.getInt(    path + "maxCount");
		grp.isTarget = 				  conf.getBoolean(path + "isTarget");
		
		// Conversion of targetRegion to global coordinates:
		// This conversion thus happens only once (more efficient)
		Vector pos1_glob = parent.toGlobal(targetRegion.getPos1());
		Vector pos2_glob = parent.toGlobal(targetRegion.getPos2());
		targetRegion = new CuboidRegion(pos1_glob, pos2_glob);
	}
	
	
	@Override
	public void run() {
		// spawn as many entities as given by count:
		World world = parent.getPlugin().world;
		for (int nr=0; nr<grp.count; nr++) { 
			// use an individual random location, defined by the region of this task:
			Location spawnL = BukkitUtil.toLocation(world, Helper.getRandVector(targetRegion));
			spawnL = spawnL.add(new org.bukkit.util.Vector(0.5,0,0.5)); // full qualified name again, meh// 0.5 added for world coord!
			Entity thisEnemy = world.spawnEntity(spawnL, grp.type);		// spawn and get pointer to track it
			if (grp.isTarget) parent.addTrackedEntity(thisEnemy); 		// add to List of tracked entities for BattleRooms to monitor
		}
	}
}

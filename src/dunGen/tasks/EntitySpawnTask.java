package dunGen.tasks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.sk89q.worldedit.bukkit.BukkitUtil;

import dunGen.Helper;
import dunGen.Room;

public class EntitySpawnTask extends RoomTask {

	EntityGroup grp;
	
	public EntitySpawnTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		this.type = TaskType.ENTITYSPAWN;
		
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		grp = new EntityGroup();
		grp.type = EntityType.valueOf(conf.getString( path + "entityType"));
		grp.count = 				  conf.getInt(    path + "count");
		grp.isTarget = 				  conf.getBoolean(path + "isTarget");
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
	
	
	/**
	 * Struct-like internal class representing an enemy group in a room (with default values)
	 */
	public class EntityGroup {
		public EntityType type = EntityType.ZOMBIE;
		public int count = 0;
		public boolean isTarget = false;
	}
}

package dunGen;
import org.bukkit.entity.EntityType;

import com.sk89q.worldedit.Vector;

import dunGen.tasks.SpawnGroupTask;

/**
 * Struct-like class representing an enemy group in a room (with default values)
 */
public class EnemyGroup {
	// loaded from config, values are defaults if not set:
	public Vector spawnCorner1 = new Vector();
	public Vector spawnCorner2 = new Vector();
	public EntityType type = EntityType.ZOMBIE;
	public int count = 0;
	public boolean isTarget = false;
	public int spawnInterval = -1;
	
	// if spawning is scheduled, each group has a reference to its spawn task:
	public SpawnGroupTask spawnTask = null;
}

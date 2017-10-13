package dungeonGen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;

import scheduledTasks.CheckRoomDoneTask;
import scheduledTasks.DepowerTask;
import scheduledTasks.EmpowerTask;
import scheduledTasks.SpawnGroupTask;

public abstract class Room extends Module {
	
	// loaded from config:
	private List<EnemyGroup> enemyGroups;
	private Vector powerBlockLoc;
	int onTime = 0;
	int offTime = 0;
	
	// work variables:
	protected List<Entity> trackedEnemies;
	protected CheckRoomDoneTask checkTask;
	private EmpowerTask empowerTask;
	private DepowerTask depowerTask;
	

	public Room(DungeonGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}

	
	public abstract void checkRoomDone();
	
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		// enemies:
		enemyGroups = new ArrayList<EnemyGroup>();
		int grpCounter = 1;
		EnemyGroup grp;
		while (conf.contains("group"+grpCounter)) {
			String path = "group"+grpCounter+".";
			grp = new EnemyGroup();
			if (conf.contains(path + "type"))
				grp.type = EntityType.valueOf(conf.getString(path + "type"));
			if (conf.contains(path + "count"))
				grp.count = conf.getInt(path + "count");
			if (conf.contains(path + "isTarget"))
				grp.isTarget = conf.getBoolean(path + "isTarget");
			if (conf.contains(path + "spawnInterval"))
				grp.spawnInterval = conf.getInt(path + "spawnInterval");
			if (conf.contains(path + "spawnRegion")) {
				grp.spawnCorner1 = BukkitUtil.toVector(conf.getVector(path + "spawnRegion.corner1"));
				grp.spawnCorner2 = BukkitUtil.toVector(conf.getVector(path + "spawnRegion.corner2"));
			}
			enemyGroups.add(grp);
			grpCounter++;
		}
		
		// redstone stuff (if set in config):
		org.bukkit.util.Vector temp= conf.getVector("powerBlockLoc");
		if (temp != null)
			powerBlockLoc = toGlobal(BukkitUtil.toVector(temp));
		else
			powerBlockLoc = null;
		
		onTime = conf.getInt("onTimeTicks");
		offTime = conf.getInt("offTimeTicks");
	}


	@Override
	public final void postPlacementActions() {
		// spawn enemies:
		trackedEnemies = new LinkedList<>();
		for (int i=0; i< enemyGroups.size(); i++) {
			spawnGroup(enemyGroups.get(i));
		}
	}


	@Override
	public void register() {
		// done condition check:
		checkTask = new CheckRoomDoneTask(this);
		checkTask.runTaskTimer(parent, 50, 50);// 20 ticks = 1 sec -> 50 is a good two seconds in between each execution
		
		// redstone periodic activation:
		if (powerBlockLoc != null && onTime != 0 && offTime != 0) {
			empowerTask = new EmpowerTask(this);
			empowerTask.runTaskTimer(parent, 0, onTime+offTime);
			
			depowerTask = new DepowerTask(this);
			depowerTask.runTaskTimer(parent, onTime, onTime+offTime);
		}
		
		// periodic spawning of groups:
		for (int i=0; i<enemyGroups.size(); i++) {
			EnemyGroup grp = enemyGroups.get(i);
			if (grp.spawnInterval > 0) { // so -1 or 0 deactivate the function
				grp.spawnTask = new SpawnGroupTask(this, grp);
				grp.spawnTask.runTaskTimer(parent, 0, grp.spawnInterval);
			}
		}
	}


	@Override
	public void unregister() {
		if (checkTask != null)
			checkTask.cancel();
		if (empowerTask != null)
			empowerTask.cancel();
		if (depowerTask != null) {
			depower();
			depowerTask.cancel();
		}
		for (int i=0; i<enemyGroups.size(); i++) {
			if (enemyGroups.get(i).spawnTask != null)
				enemyGroups.get(i).spawnTask.cancel();
		}
	}
	
	
	/**
	 * Basically creates a redstone block at the powerBlockLoc location
	 */
	public void empower() {
		parent.world.getBlockAt(powerBlockLoc.getBlockX(),powerBlockLoc.getBlockY(),powerBlockLoc.getBlockZ()).setType(Material.REDSTONE_BLOCK);
	}

	
	/**
	 * Sets the powerBlockLoc Location to AIR, removing any redstone block there
	 */
	public void depower() {
		parent.world.getBlockAt(powerBlockLoc.getBlockX(),powerBlockLoc.getBlockY(),powerBlockLoc.getBlockZ()).setType(Material.AIR);
	}


	public void spawnGroup(EnemyGroup grp) {
		// values valid for whole group:
		CuboidRegion spawnReg = new CuboidRegion(toGlobal(grp.spawnCorner1), toGlobal(grp.spawnCorner2));
		EntityType type = grp.type;
		boolean tracked = grp.isTarget;
		for (int nr=0; nr<grp.count; nr++) {// repeat according to number of entities of this type
			// individual random spawn:
			Location spawnL = BukkitUtil.toLocation(parent.world, Helper.getRandVector(spawnReg));
			spawnL = spawnL.add(new org.bukkit.util.Vector(0.5,0,0.5)); // full qualified name again, meh// 0.5 added for world coord!
			Entity thisEnemy = parent.world.spawnEntity(spawnL, type);	// spawn and get pointer to track it
			if (tracked)
				trackedEnemies.add(thisEnemy);
		}
	}
}

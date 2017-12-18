package dunGen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Entity;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import dunGen.DunGen.State;
import dunGen.Helper.Direc;
import dunGen.tasks.TaskWithCallback;
import dunGen.tasks.RoomTask.TaskType;
import dunGen.tasks.BlockSpawnTask;
import dunGen.tasks.EntitySpawnTask;
import dunGen.tasks.PowerTask;
import dunGen.tasks.RoomTask;

/**
 * Rooms feature an arbitrary number of Tasks, configured in its yaml file.
 * These are activated (registered) automatically after placement in the postPlacementActions().
 */
public abstract class Room extends Module {
	
	// loaded from config:
	private List<RoomTask> tasks;
	private Vector powerBlockLoc;
	int onTime = 0;
	int offTime = 0;
	
	// work variables:
	protected List<Entity>      trackedEnemies;
	protected TaskWithCallback 	checkTask;
	private   TaskWithCallback 	empowerTask;
	private   TaskWithCallback 	depowerTask;
	

	public Room(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
		trackedEnemies = new LinkedList<>();
	}

	
	/**
	 * This member function will be called by Room periodically using a TaskWithCallback.
	 * @param v A Void object that will not be used. Only necessary to use the method reference here.
	 * @return  Enter return null; in your Implementation. Only needed for method reference implementation.
	 */
	public abstract Void checkRoomDone(Void v);
	
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		
		// load and add RoomTasks:
		tasks = new ArrayList<RoomTask>();
		int taskNr = 1;
		RoomTask newTask;
		while (conf.contains("tasks.task"+taskNr)) {
			TaskType type = TaskType.valueOf(conf.getString("tasks.task" + taskNr + "." + "type"));
			switch (type) {
			case BLOCKSPAWN:
				newTask = new BlockSpawnTask(this, conf, taskNr);
				break;
			case ENTITYSPAWN:
				newTask = new EntitySpawnTask(this, conf, taskNr);
				break;
			case POWER:
				newTask = new PowerTask(this, conf, taskNr);
				break;
			default:
				parent.setStateAndNotify(State.ERROR, "Task type could not be loaded for room: " + this.name + ". Skipping Task.");
				continue;
			}
			tasks.add(newTask);
			taskNr++;
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
		// nothing atm
	}


	@Override
	public void register() {
		// victory condition check:
		checkTask = new TaskWithCallback(this::checkRoomDone);
		checkTask.runTaskTimer(parent, 50, 50);// 20 ticks = 1 sec -> 50 is a good two seconds in between each execution
		
		// activate tasks:
		for (RoomTask task : tasks) {
			task.register();
		}
		
		// redstone periodic activation:
		if (powerBlockLoc != null && onTime != 0 && offTime != 0) {
			empowerTask = new TaskWithCallback(this::empower);
			empowerTask.runTaskTimer(parent, 0, onTime+offTime);
			
			depowerTask = new TaskWithCallback(this::depower);
			depowerTask.runTaskTimer(parent, onTime, onTime+offTime);
		}
	}


	public void addTrackedEntity(Entity e) {
		trackedEnemies.add(e);
	}
	
	
	@Override
	public void unregister() {
		for (RoomTask task : tasks) {
			task.cancel();
		}
	}
	
	
	/** Basically creates a redstone block at the powerBlockLoc location
	 * @param v Void object to make this usable as method reference.
	 * @return Void object to make this usable as method reference.
	 */
	public Void empower(Void v) {
		parent.world.getBlockAt(powerBlockLoc.getBlockX(),powerBlockLoc.getBlockY(),powerBlockLoc.getBlockZ()).setType(Material.REDSTONE_BLOCK);
		return null;
	}

	
	/**Sets the powerBlockLoc Location to AIR, removing any redstone block there
	 * @param v Void object to make this usable as method reference.
	 * @return Void object to make this usable as method reference.
	 */
	public Void depower(Void v) {
		parent.world.getBlockAt(powerBlockLoc.getBlockX(),powerBlockLoc.getBlockY(),powerBlockLoc.getBlockZ()).setType(Material.AIR);
		return null;
	}
}

package dunGen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Entity;

import com.sk89q.worldedit.Vector;

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
	
	
	
	// ################### Member variables ###################
	protected 	TaskWithCallback 	checkTask;		// the Task used to check whether the room is solved, only calls checkRoomDone()
	private 	List<RoomTask> 		tasks;			// The list of tasks this room is configured with.
	protected 	List<Entity>      	trackedEnemies; // The list of enemies to track, only needed by BattleRooms. Move this?
	

	
	// ###################### Member functions #######################
	
	/**Constructor takes same arguments as a Module and forwards these. Then loads config.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public Room(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
		trackedEnemies = new LinkedList<>();
	}

	
	/**Adds an entity to be tracked as target (victory condition of BattleRooms).
	 * @param e	The entity to be tracked.
	 */
	public void addTrackedEntity(Entity e) {
		trackedEnemies.add(e);
	}
	
	
	/**
	 * This member function will be called by Room periodically using a TaskWithCallback.
	 * Will callback to DunGen::roomClear() if successful.
	 * @param v A Void object that will not be used. Only necessary to use the method reference here.
	 * @return  Enter "return null;" in your Implementation. Void is only needed for method reference implementation.
	 */
	public abstract Void checkRoomDone(Void v);

	/**Calls the Module::loadConfig() for basic values. Then loads the properties special for Rooms.
	 * For Room, this means initializing the Task objects.
	 * Checks are not needed any more due to initial yml checks.*/
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
	}
	
	
	@Override
	public void postPlacementActions() {
		// nothing atm
	}
	
	
	@Override
	public void register() {
		// victory condition check, using reference method implementation:
		checkTask = new TaskWithCallback(this::checkRoomDone);
		checkTask.runTaskTimer(parent, 50, 50);// 20 ticks = 1 sec -> 50 is a good two seconds in between each execution
		
		// activate room tasks:
		for (RoomTask task : tasks) {
			task.register();
		}
	}

	
	@Override
	public void unregister() {
		checkTask.cancel();
		for (RoomTask task : tasks) {
			task.cancel();
		}
	}
}

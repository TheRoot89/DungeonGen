package dunGen.tasks;

import java.util.function.Function;

import org.bukkit.scheduler.BukkitRunnable;

/**
 *	A specification of BukkitRunnable to run a single method given during contruction.
 */
public class TaskWithCallback extends BukkitRunnable {

	Function<Void,Void> callback;	
	
	/**
	 * Constructs as BukkitRunnable, that will only call the given function when activated.
	 * @param callback The function to be called
	 */
	public TaskWithCallback(Function<Void,Void> callback) {
		this.callback = callback;
	}
	
	@Override
	public void run() {
		callback.apply(null);
		
	}

}

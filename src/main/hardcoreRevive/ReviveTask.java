package hardcoreRevive;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/// This class just counts down and reschedules itself each message interval.
public class ReviveTask extends BukkitRunnable {
	
	private static final int messageInterval = 10;
	private static final int ticksPerSec = 20;
	
	private int remainingTime = 0;
	private Player player;
	private boolean remainingTimeWillBeReached = false;
	
	public ReviveTask(int remainingTime, Player player) {
		this.remainingTime = remainingTime;
		this.player = player;
	}
	
	
	@Override
	public void run() {
		if (remainingTimeWillBeReached) {	//remaining time has run down. Execute:
			HCRPlugin.get().revivePlayer(player);
		} else {
			ReviveTask newTask = new ReviveTask(remainingTime-messageInterval, player);
			newTask.schedule();
			return;
		}
	}
	
	
	/// Schedules itself and sets a flag for whether to reschedule later or execute
	public void schedule() {
		if (remainingTime < messageInterval) {
			remainingTimeWillBeReached = true;
			this.runTaskLater(HCRPlugin.get(),remainingTime * ticksPerSec);
		} else {
			player.sendMessage(remainingTime + "s until revive.");
			remainingTimeWillBeReached = false;
			this.runTaskLater(HCRPlugin.get(),messageInterval * ticksPerSec);
		}
	}

}

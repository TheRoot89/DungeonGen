package dunGen;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

public class ScoreListener implements Listener {

	private DunGen parent;
	private Objective scoreObjective;
	
	private int totalScore = 0;
	
	// Settings:
	private final int deathIncr = -1;
	private final int killIncr = 1;
	private final String totalScoreName = "Total";
	
	public ScoreListener(DunGen parent) {
		this.parent = parent;
	}

	public void setScoreObjective(Objective scoreObjective) {
		this.scoreObjective = scoreObjective;
	}
	
	
	/**Registers this Listener to receive events needed.*/
	public void register() {
		parent.getServer().getPluginManager().registerEvents(this, parent);
	}
	
	/**Removes this Listener from receiving any events.*/
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
	
	private void updateTotalScore(int increment) {
		totalScore += increment;
		scoreObjective.getScore(totalScoreName).setScore(totalScore);
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		// getKiller() of a LivingEntity returns a Player, so you don't have to check if it's a player:
		// May be null if no killer found!
		Player p = event.getEntity().getKiller();
		if (p != null) {
			Score sc = scoreObjective.getScore(p.getName());
			sc.setScore(sc.getScore() + killIncr);
			updateTotalScore(killIncr);
		}
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerRespawn(PlayerDeathEvent event) {
		Score sc = scoreObjective.getScore(event.getEntity().getName());
		sc.setScore(sc.getScore() + deathIncr);
		
		updateTotalScore(deathIncr);
	}
	
}

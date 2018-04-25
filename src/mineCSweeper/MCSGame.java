package mineCSweeper;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import mineCSweeper.MCSGameStateHandler;
import dunGen.Helper.Direc;

/**Represents a game of MineCraftSweeper. It accesses its own configuration in the hosting plugins folder
 * and is handed its own commands. It consists of a state machine and game board if spawned.
 * It implements a Listener to react to player interaction.*/
public class MCSGame implements Listener {
	
	final static int SPAWNDISTANCE = 2; //TODO make a setting of this
	
	// ########################## Member variables ############################
	private MCSGameStateHandler gameState;
	
	private JavaPlugin plugin;
	private MCSBoard board;
	private Direc boardDirec;
	private MCSSettings settings;
	private Player player;
	
	// ############################ Member functions ############################
	public MCSGame(JavaPlugin plugin) {
		// TODO initialize everything needed
		this.plugin = plugin;
		
		try {
			settings = MCSSettings.getSettingsHandler(plugin.getDataFolder());
			gameState = MCSGameStateHandler.getNewlyInitializedGameState();
		} catch (MCSException e) {
			gameState.logErrorKeepState("Game settings initialization failed: " + e.getMessage());
		}
		
	}
	

	public void start(Player player) {
		switch (gameState.getState()) {
		case ERROR:
			//TODO, see diagrams
			break;
		case NOT_STARTED:
			//start normally: get pose of player, add him, and create the board
			Location playerPose = player.getLocation();
			Direc playerDirec = Direc.fromDeg(playerPose.getYaw());
			Location boardPose = playerPose.add(playerDirec.toBukkitVec(SPAWNDISTANCE));
			board = new MCSBoard(boardPose);
			board.placeEmpty();
			break;
		case STARTUP:
			//TODO: generate new board at new position
			gameState.setMessage("Generating new board at new position", MsgLevel.INFO);
			break;
		case RUNNING:
			gameState.setMessage("The game is already running! Type 'restart' to start over.", MsgLevel.WARNING);
		}
		
		
	}
	
	
	
	private void placeBoard(Vector targetCorner, Direc dir) {
		// TODO stub
	}
	
	private void deleteBoard() {
		// TODO stub
	}
	
	private void resetGame() {
		// TODO stub
	}
	
	private void update() {
		// TODO stub
	}
	
	private void gameOver(boolean success) {
		// TODO stub
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		// TODO stub
	}
	
	private void markBomb() {
		// TODO stub		
	}
	
	private void register() {
		// TODO stub
	}
	
	private void unregister() {
		// TODO stub
	}
	
	private void setState() {
		// TODO stub
	}
	
	public void onDisable() {
		settings.saveConfig();
	}



	public void stop() {
		// TODO Auto-generated method stub
		
	}

	public void restart() {
		// TODO Auto-generated method stub
		
	}

	public boolean setOptionIfKeyExists(String key, String value) {
		if (!settings.hasOptionKey(key))
			return false;
		settings.setOption(key, value);
		return true;
	}
	
	public MCSSettings getSettings() {
		return settings;
	}


	public boolean isInErrorState() {
		return (gameState == MCSGameStateHandler.ERROR);
	}


	public MCSGameStateHandler getState() {
		return gameState;
	}
	
}

package mineCSweeper;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import mineCSweeper.MCSGameState;
import dunGen.Helper.Direc;

/**Represents a game of MineCraftSweeper. It accesses its own configuration in the hosting plugins folder
 * and is handed its own commands. It consists of a state machine and game board if spawned.
 * It implements a Listener to react to player interaction.*/
public class MCSGame implements Listener {
	
	// ########################## Member variables ############################
	private MCSGameState state;
	
	private JavaPlugin plugin;
	private Vector boardPos;
	private MCSBoard board;
	private Direc boardDirec;
	private Logger log;
	private MCSSettings settings;
	
	// ############################ Member functions ############################
	public MCSGame(JavaPlugin plugin) {
		// TODO initialize everything needed
		this.plugin = plugin;
		log = plugin.getLogger();
		settings = MCSSettings.getSettingsHandler(plugin.getDataFolder());
		if (!settings.isLoadedSuccessfully()) {
			state = MCSGameState.newState(MCSGameState.ERROR, "Game initialization failed!");
		}
		
		state = MCSGameState.newState(MCSGameState.NOT_STARTED,"Game initialization successfull.");
	}
	

	public void start(Location boardPose) {
		switch (state) {
		case ERROR:
			//TODO, see diagrams
			break;
		case NOT_STARTED:
			//start normally
			board = new MCSBoard(boardPose);
			board.placeEmpty();
			break;
		case STARTUP:
			//TODO: generate new board at new position
			break;
		case RUNNING:
			state.setMessage("The game is already running! Type 'restart' to start over.");
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
		return (state == MCSGameState.ERROR);
	}


	public MCSGameState getState() {
		return state;
	}
	
}

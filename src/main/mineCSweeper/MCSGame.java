package mineCSweeper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import mcPluginHelpers.Direc;
import mineCSweeper.MCSGameStateHandler;

/**Represents a game of MineCraftSweeper. It accesses its own configuration in the hosting plugins folder
 * and is handed its own commands. It consists of a state machine and game board if spawned.
 * It implements a Listener to react to player interaction.*/
public class MCSGame implements Listener {
	
	final static int SPAWNDISTANCE = 2; //TODO make a setting of this
	
	// ########################## Member variables ############################
	private World world;
	private MCSGameStateHandler gameState;
	private JavaPlugin plugin;
	private MCSBoard board;
	private MCSSettings settings;
	private MCSGameMechanic gameMechanic;
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
		Location boardPose = calcBoardSpawn(player);
		//TODO start with gameMechanic!
		switch (gameState.getState()) {
		case STARTUP:
			gameState.logInfo("Deleting old board and starting at new position.");
			if (board != null) {
				board.delete();
			}
			//TODO: stop old GameMechanic here?
			// no "break" so this continues with the placement in NOT_STARTED:	
		case NOT_STARTED:
			if (this.player == null) {
				this.player = player;
			}
			world = player.getWorld();
			board = new MCSBoard(world, boardPose, settings);
			gameMechanic = new MCSGameMechanic(board, settings);
			board.placeCleanBoard();
			gameState.setState(GameState.STARTUP);
			break;
		case RUNNING:
			gameState.logWarning("The game is already running! Type 'restart' to start over.");
			break;
		default:
			gameState.setToError("TILT: This line should not be reachable!");
		}
		
	}
	
	private Location calcBoardSpawn(Player player) {
		Location playerPose = player.getLocation();
		Direc playerDirec = Direc.fromDeg(playerPose.getYaw());
		int boardSpawnDist = settings.getIntegerSetting(MCSSettings.Key.BOARDSPAWNDIST);
		return playerPose.add(playerDirec.toBukkitVec(boardSpawnDist).add(new Vector(0,-1,0)));
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
	
	public MCSSettings getSettings() {
		return settings;
	}


	public boolean isInErrorState() {
		return gameState.isError();
	}


	public MCSGameStateHandler getState() {
		return gameState;
	}
	
	public Player getPlayer() {
		return player;
	}
}

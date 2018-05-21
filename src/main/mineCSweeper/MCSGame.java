package mineCSweeper;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitUtil;

import dunGen.DunGen.State;
import mcPluginHelpers.Direc;
import mineCSweeper.MCSGameStateHandler;
import mineCSweeper.MCSSettings.Key;

/**Represents a game of MineCraftSweeper. It accesses its own configuration in the hosting plugins folder
 * and is handed its own commands. It consists of a state machine and game board if spawned.
 * It implements a Listener to react to player interaction.*/
public class MCSGame implements Listener{
	
	final static int SPAWNDISTANCE = 2;
	
	// ########################## Member variables ############################
	private World world;
	private MCSGameStateHandler gameState;
	private JavaPlugin plugin;
	private MCSBoard board;
	private MCSSettings settings;
	private Player player; //TODO add multiple players
	
	// ############################ Member functions ############################
	public MCSGame(JavaPlugin plugin) {
		this.plugin = plugin;
		gameState = MCSGameStateHandler.getNewlyInitializedGameState();
		try {
			settings = MCSSettings.getSettingsHandler(plugin.getDataFolder());
		} catch (MCSException e) {
			gameState.setToError("Game settings initialization failed: " + e.getMessage());
			e.printStackTrace(); // Additional ouput, since gameState does not have the callback!
		}
		
	}
	

	public void start(Player player) {
		Location boardPose = calcBoardSpawn(player);
		start(player, boardPose);
	}
	
	public void start(Player player, Location boardPose) {
		switch (gameState.getState()) {
		case STARTUP:
		case RUNNING:
			unregister();
			deleteGameObjectsIfExistent();
			// no "break" so this continues with the placement in NOT_STARTED:	
		case NOT_STARTED:
			this.player = player;
			world = player.getWorld();
			try {
				board = new MCSBoard(world, boardPose, settings);
				board.placeCleanBoard();
				gameState.setState(GameState.STARTUP);
				register();
			} catch (MCSException e) {
				gameState.setToError("Exception while loading and placing game board:\n" + e.getMessage());
				deleteGameObjectsIfExistent();
			}
			
			break;
		default:
			gameState.setToError("TILT: This line should not be reachable!");
		}
	}
	
	private void deleteGameObjectsIfExistent() {
		gameState.logInfo("Deleting old board and starting at new position.");
		if (board != null) {
			board.delete();
			board = null;
		}
		world = null;
		player = null;
	}


	private Location calcBoardSpawn(Player player) {
		Location playerPose = player.getLocation();
		Direc playerDirec = Direc.fromDeg(playerPose.getYaw());
		int boardSpawnDist = settings.getIntegerSetting(MCSSettings.Key.BOARDSPAWNDIST);
		return playerPose.add(playerDirec.toBukkitVec(boardSpawnDist).add(new Vector(0,-1,0)));
	}
	
	private void register() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}	
	
	private void unregister() {
		PlayerInteractEvent.getHandlerList().unregister(this);
	}
	
	private void gameOver(boolean success) {
		// TODO stub
	}
	

	public void setOptionAndRestart(String key, String value) {
		try {
			settings.setOption(key, value);
			restart();
		} catch (MCSException e) {
			gameState.logErrorKeepState("Option could not be set: " + key + " = " + value);
			gameState.logErrorKeepState(e.getMessage());
		}
	}
	
	private void restart() {
		start(player, board.getBoardPose());
	}


	public void onDisable() {
		settings.saveConfig();
		deleteGameObjectsIfExistent();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (!isPlaying(p)) {
			return;
		}
		
	    if (event.getAction() == Action.PHYSICAL) {	// Handle activation of fields
	        Block clicked = event.getClickedBlock();
	        if (clicked.getType() == Field.pressurePlateMat) {
	        	Vector plateGlobCoord = clicked.getLocation().toVector();
	        	if (gameState.getState() == GameState.STARTUP) {
	        		board.initializeBoard(BukkitUtil.toVector(plateGlobCoord));
	        		gameState.setState(GameState.RUNNING);
	        	}
	        	board.activatePressurePlate(BukkitUtil.toVector(plateGlobCoord));
	        	if (board.isExploded())
	        		gameOver(false);
	        }
	    }else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {	// Handle flagging
	    	Block clicked = event.getClickedBlock();
	    	Vector globCoord = clicked.getLocation().toVector();
	    	if (clicked.getType() == Field.pressurePlateMat) {
	    		board.placeFlag(BukkitUtil.toVector(globCoord));
	    		
	    	}else if (clicked.getType() == Field.bombFlagMat) {
	    		board.removeFlag(BukkitUtil.toVector(globCoord));
	    	}
	    	if (board.allMinesCorrectlyFlagged()) {
	    		gameOver(true);
    			//TODO replace by toggleFlag method, 
    			
    		}
	    }
	}
	
	private boolean isPlaying(Player p) {
		return p.equals(player);
	}


	public void stop() {
		switch (gameState.getState()) {
		case NOT_STARTED:
			gameState.logWarning("No game running that could be stopped.");
			break;
		case STARTUP:
		case RUNNING:
			unregister();
			deleteGameObjectsIfExistent();
			gameState.setState(GameState.NOT_STARTED);
			break;
		default:
			gameState.setToError("TILT: This line should not be reachable!");
		}
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

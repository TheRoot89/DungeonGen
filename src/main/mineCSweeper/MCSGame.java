package mineCSweeper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
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

import mcPluginHelpers.Direc;
import mcPluginHelpers.MsgLevel;
import mineCSweeper.MCSGameStateHandler;

/**Represents a game of MineCraftSweeper. It accesses its own configuration in the hosting plugins folder
 * and is handed its own commands. It consists of a state machine and game board if spawned.
 * It implements a Listener to react to player interaction.*/
public class MCSGame implements Listener{
	
	final static int SPAWNDISTANCE = 2;
	
	// ########################## Member variables ############################
	private World world;
	private MCSGameStateHandler gameState;
	private JavaPlugin plugin;
	private MCSBoard board = null;
	private MCSSettings settings;
	private Set<Player> players;
	
	// ############################ Member functions ############################
	public MCSGame(JavaPlugin plugin) {
		this.plugin = plugin;
		players = new LinkedHashSet<Player>();
		gameState = MCSGameStateHandler.getNewlyInitializedGameState();
		try {
			settings = MCSSettings.getSettingsHandler(plugin.getDataFolder());
		} catch (MCSException e) {
			gameState.setToError("Game settings initialization failed: " + e.getMessage());
			e.printStackTrace(); // Additional ouput, since gameState does not have the callback!
		}
		
	}

	public void startInFrontOfPlayer(Player player) {
		Location boardCenterPose = calcBoardCenterSpawn(player);
		start(player, boardCenterPose);
	}
	
	public void addPlayer(Player player) {
		players.add(player);
		if (players.size() == 1)
			world = player.getWorld();
	}
	
	public void clearPlayers() {
		players.clear();
	}
	
	public void start(Player firstPlayer, Location boardCenterPose) {
		addPlayer(firstPlayer);
		world = firstPlayer.getWorld();
		start(boardCenterPose);
	}
	
	public void start(Location boardCenterPose) {
		assert(players.size() > 0);
		unregister();
		deleteBoardIfExistent();
		
		switch (gameState.getState()) {
		case STARTUP:
		case RUNNING:
		case NOT_STARTED:
			
			try {
				board = new MCSBoard(world, boardCenterPose, settings);
				board.placeCleanBoard();
				gameState.setState(GameState.STARTUP);
				register();
			} catch (MCSException e) {
				gameState.setToError("Exception while loading and placing game board:\n" + e.getMessage());
				deleteBoardIfExistent();
			}
			
			break;
		default:
			gameState.setToError("TILT: This line should not be reachable!");
		}
	}
	
	private void deleteBoardIfExistent() {
		if (board != null) {
			gameState.logDebug("Deleting board.");
			board.delete();
			board = null;
		}
	}


	private Location calcBoardCenterSpawn(Player player) {
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
		unregister();
		if (success) {
			gameState.setState(GameState.NOT_STARTED, "All the bombs were found! Congratulations!", MsgLevel.INFO);
			gameState.logInfo("You may delete the board via start or stop command.");
		}else {
			gameState.setState(GameState.NOT_STARTED, "You hit a bomb!", MsgLevel.INFO);
			gameState.logInfo("Try again via the start command :) ");
		}
	}
	

	public void setOptionAndRestart(String key, String value) {
		try {
			settings.setOption(key, value);
			restartIfBoardExists();
		} catch (MCSException e) {
			gameState.logErrorKeepState("Option could not be set: " + key + " = " + value);
			gameState.logErrorKeepState(e.getMessage());
		}
	}
	
	private void restartIfBoardExists() {
		if (board != null)
			start(board.getBoardCenter());
	}


	public void onDisable() {
		settings.saveConfig();
		deleteBoardIfExistent();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (!isPlaying(p)) {
			return;
		}
		
		try {
			if (event.getAction() == Action.PHYSICAL) {	// Handle activation of fields
		        Block clicked = event.getClickedBlock();
		        if (clicked.getType() == Field.pressurePlateMat) {
		        	Vector plateGlobCoord = clicked.getLocation().toVector();
		        	if (gameState.getState() == GameState.STARTUP) {
		        		board.initializeBoard(BukkitUtil.toVector(plateGlobCoord));
		        		gameState.setState(GameState.RUNNING);
		        	}
		        	board.activatePressurePlate(BukkitUtil.toVector(plateGlobCoord));
		        	board.updateFieldLater(plugin, 5, BukkitUtil.toVector(plateGlobCoord));
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
	    		}
		    }
		} catch (Exception e) {
			stop();
			gameState.setToError(e.getMessage());
		}
	    
	}
	
	private boolean isPlaying(Player player) {
		return players.contains(player);
	}


	public void stop() {
		unregister();
		deleteBoardIfExistent();
		gameState.setState(GameState.NOT_STARTED);
		clearPlayers();
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
	
	public Set<Player> getPlayers() {
		return players;
	}
}

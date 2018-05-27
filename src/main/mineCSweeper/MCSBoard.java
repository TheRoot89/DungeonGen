package mineCSweeper;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import mcPluginHelpers.Direc;
import mcPluginHelpers.Transform;
import mineCSweeper.MCSSettings.Key;

/**Represents the physical board the game of MCS is played upon. Regarding coordinates it is treated as
 * looking east, i.e. coordinates can counted positively forward, up and to the right in x, y and z respectively.
 */
public class MCSBoard {
	
	// ########################## Member variables ############################
	private World		world;		// needed for block manipulation
	private Transform	transform; 	// wraps world position transform of the board
	private int			relZOffsetForBoardCentering;
	private Field[][] 	curFields;
	private int			width;
	private int			height;
	private int			bombCount;
	private int			correctlyFlaggedCount;
	private int			falselyFlaggedCount;
	private boolean 	bombWasTriggered = false;
	private Vector    	boardOrigin;
	private Location 	boardCenter;
	
	
	// ############################ Member functions ############################
	public MCSBoard(World world, Location boardCenterPose, MCSSettings settings) throws MCSException{
		this.world = world;
		this.boardCenter = boardCenterPose;
		correctlyFlaggedCount = 0;
		falselyFlaggedCount = 0;
		// load n copy the settings now, these will thus not change until a new board is generated:
		loadSettings(settings);
		Field.loadStaticConfigForAllFields(settings, world);
		// get correct Vector for the real board origin. An own transform is needed to substract the shift in z:
		relZOffsetForBoardCentering = width/2;
		Vector boardCenterGlob = BukkitUtil.toVector(boardCenterPose.toVector()); // conversion from bukkit to worldEdit vector class
		Direc boardDirec = Direc.fromDeg(boardCenterPose.getYaw());
		Transform boardCenterBasedTransf = new Transform(boardCenterGlob,boardDirec);
		boardOrigin = boardCenterBasedTransf.toGlobal(0, 0, -relZOffsetForBoardCentering);
		
		transform = new Transform(boardOrigin, boardDirec);
		curFields = new Field[height][width];
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				Vector global = transform.toGlobal(x,0,z);
				curFields[x][z] = Field.newCleanField(global);
			}
		}
	}

	private void loadSettings(MCSSettings settings) {
		width 	  = settings.getIntegerSetting(Key.BOARDWIDTH);
		height 	  = settings.getIntegerSetting(Key.BOARDHEIGHT);
		bombCount = settings.getIntegerSetting(Key.BOMBCOUNT);
	}
	
	public void placeCleanBoard() {
		cleanBoard();
		updateAllFields();
	}
	
	private void cleanBoard() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				curFields[x][z].clean();
			}
		}
	}
	
	private void updateAllFields() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				updateField(x, z);
			}
		}
	}
	
	public void updateField(int x, int z) {
		curFields[x][z].update();;
	}

	public void updateFieldGlobal(Vector globPos) {
		Vector rel = transform.toRelative(globPos);
		updateField(rel.getBlockX(), rel.getBlockZ());
	}
	
	public void delete() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				getField(x,z).delete();
			}
		}
	}
	
	public Field getField(int x, int z) {
		return curFields[x][z];
	}
	
	public void setMineAndUpdate(int x, int z, boolean hasMine) {
		curFields[x][z].setMine(hasMine);
		updateField(x, z);
	}
	
	public void placeFlagAndUpdate(int x, int z) {
		if (fieldIsInvalid(x, z)) return;
		
		Field field = getField(x, z);
		if (!field.isUnknown()) return;
		
		if (field.isMined())
			correctlyFlaggedCount++;
		else
			falselyFlaggedCount++;
		field.flagIfPossible();
		
		// somehow, the field is not updated correctly
		updateField(x, z);
	}
	
	public void removeFlagAndUpdate(int x, int z) {
		if (fieldIsInvalid(x, z)) return;
		
		Field field = getField(x, z);
		if (field.isMined())
			correctlyFlaggedCount--;
		else
			falselyFlaggedCount--;
		field.unFlagIfPossible();
		updateField(x, z);
	}
	
	public boolean allMinesCorrectlyFlagged() {
		return (bombCount == correctlyFlaggedCount) && (falselyFlaggedCount == 0);
	}

	public void initializeBoard(Vector globCoordPlayer) throws MCSException {
		Vector relCoord = transform.toRelative(globCoordPlayer);
		shuffleWithNoBombAdjacentTo(relCoord);
		calcAndSafeBombHints();
	}
	
	private void calcAndSafeBombHints() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				int bombSum = 0;
				if (fieldIsValidAndMined(x-1, z-1)) bombSum++;
				if (fieldIsValidAndMined(x-1, z)) 	bombSum++;
				if (fieldIsValidAndMined(x-1, z+1)) bombSum++;
				if (fieldIsValidAndMined(x, z-1)) 	bombSum++;
				if (fieldIsValidAndMined(x, z+1)) 	bombSum++;
				if (fieldIsValidAndMined(x+1, z-1)) bombSum++;
				if (fieldIsValidAndMined(x+1, z)) 	bombSum++;
				if (fieldIsValidAndMined(x+1, z+1)) bombSum++;
				
				curFields[x][z].bombHintNr = bombSum;
			}
		}
		
	}

	private boolean fieldIsValidAndMined(int x, int z) {
		if (fieldIsInvalid(x, z)) return false;
		return curFields[x][z].isMined();
	}

	public void shuffleWithNoBombAdjacentTo(Vector relCoord) throws MCSException {
		int bombfreeX = relCoord.getBlockX();
		int bombfreeZ = relCoord.getBlockZ();
		cleanBoard();
		
		// Setting up the start area:
		if (!fieldIsInvalid(bombfreeX-1, bombfreeZ-1)) 	getField(bombfreeX-1, bombfreeZ-1).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX-1, bombfreeZ)) 	getField(bombfreeX-1, bombfreeZ).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX-1, bombfreeZ+1)) 	getField(bombfreeX-1, bombfreeZ+1).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX, bombfreeZ-1)) 	getField(bombfreeX, bombfreeZ-1).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX, bombfreeZ))		getField(bombfreeX, bombfreeZ).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX, bombfreeZ+1)) 	getField(bombfreeX, bombfreeZ+1).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX+1, bombfreeZ-1)) 	getField(bombfreeX+1, bombfreeZ-1).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX+1, bombfreeZ)) 	getField(bombfreeX+1, bombfreeZ).setStartRegion(true);
		if (!fieldIsInvalid(bombfreeX+1, bombfreeZ+1)) 	getField(bombfreeX+1, bombfreeZ+1).setStartRegion(true);
		
		Random randGen = new Random();
		int x, z;
		int placedBombs = 0;
		int maxTries = 1000;
		int tries = 0;
		while ((placedBombs < bombCount) && (tries < maxTries)) {
			x = randGen.nextInt(width);
			z = randGen.nextInt(height);
			if (!getField(x, z).isMined() && getField(x, z).bombIsAllowed()) {
				setMineAndUpdate(x, z, true);
				placedBombs++;
			}else {
				tries++;
			}
		}
		
		if (tries == maxTries)
			throw new MCSException("Bombs cannot be placed! Maybe too many?");
	}

	public void activatePressurePlate(Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		int x = relCoord.getBlockX();
		int z = relCoord.getBlockZ();
		if (fieldIsInvalid(x, z)) return;
		// if a pressure plate is activated if will either:
		// - trigger a bomb, then the game is lost, no update needed:
		if (curFields[x][z].isMined()) {
			bombWasTriggered = true;
			return;
		}
		// - or a field is now "known" (FieldState) and the board hints need to be updated
		if (curFields[x][z].isUnknown())
			revealUpdateAndPropagate(x,z);
		// somehow the fiels is not updated correctly while it is pressed: -> we need to update after release
		
	}
	
	public void updateFieldLater(JavaPlugin plugin, long delay, Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		int x = relCoord.getBlockX();
		int z = relCoord.getBlockZ();
		MCSBoard board = this;	// for anonymous inner class access
		if (fieldIsInvalid(x, z)) return;
		
		BukkitRunnable updateFieldSingleShot = new BukkitRunnable() {
			@Override
			public void run() {
				board.updateField(x, z);
			}
		};
		updateFieldSingleShot.runTaskLater(plugin, delay);
	}

	private void revealUpdateAndPropagate(int x, int z) {
		if (fieldIsInvalid(x, z) || !curFields[x][z].isUnknown()) return;	// recursion end condition
		
		curFields[x][z].reveal();				// recursion action
		updateField(x, z);						// recursion action
		if (curFields[x][z].bombHintNr == 0) {	// recursion
			revealUpdateAndPropagate(x-1, z-1);
			revealUpdateAndPropagate(x-1, z);
			revealUpdateAndPropagate(x-1, z+1);
			revealUpdateAndPropagate(x, z-1);
			revealUpdateAndPropagate(x, z+1);
			revealUpdateAndPropagate(x+1, z-1);
			revealUpdateAndPropagate(x+1, z);
			revealUpdateAndPropagate(x+1, z+1);
		}
	}

	private boolean fieldIsInvalid(int x, int z) {
		if (x < 0 || x >= height) return true;
		if (z < 0 || z >= width) return true;
		return false;
	}
	
	public void placeFlag(Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		placeFlagAndUpdate(relCoord.getBlockX(), relCoord.getBlockZ());
	}

	public void removeFlag(Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		removeFlagAndUpdate(relCoord.getBlockX(), relCoord.getBlockZ());
	}

	public boolean isExploded() {
		return bombWasTriggered;
	}
	
	public Vector getBoardOrigin() {
		return boardOrigin;
	}
	
	public Location getBoardCenter() {
		return boardCenter;
	}
}

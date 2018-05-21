package mineCSweeper;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockData;
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
	private boolean		safeBombFlags;
	private Location    boardPose;
	
	
	// ############################ Member functions ############################
	public MCSBoard(World world, Location boardPose, MCSSettings settings) throws MCSException{
		this.world = world;
		this.boardPose = boardPose;
		correctlyFlaggedCount = 0;
		falselyFlaggedCount = 0;
		// load n copy the settings now, these will thus not change until a new board is generated:
		loadSettings(settings);
		Field.updateMaterialsfromSettings(settings);
		relZOffsetForBoardCentering = width/2;
		Vector origin = BukkitUtil.toVector(boardPose.toVector()); // conversion from bukkit to worldEdit vector class
		transform = new Transform(origin, Direc.fromDeg(boardPose.getYaw()));
		
		curFields = new Field[height][width];
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				Vector global = transform.toGlobal(x,0,z-relZOffsetForBoardCentering);
				curFields[x][z] = Field.newCleanField(global);
			}
		}
	}

	private void loadSettings(MCSSettings settings) {
		width 	  = settings.getIntegerSetting(Key.BOARDWIDTH);
		height 	  = settings.getIntegerSetting(Key.BOARDHEIGHT);
		bombCount = settings.getIntegerSetting(Key.BOMBCOUNT);
		safeBombFlags = settings.getBooleanSetting(Key.SAFEBOMBFLAGS);
	}
	
	private void updateField(int x, int z) {
		Field f = curFields[x][z];
		Block baseBlock = world.getBlockAt(f.globX, f.globY, f.globZ);
		baseBlock.setType(f.getCurrentBaseMat());
		Block topBlock = world.getBlockAt(f.globX, f.globY+1, f.globZ);
		topBlock.setType(f.getCurrentTopMat());
		Block mineBlock = world.getBlockAt(f.globX, f.globY-1, f.globZ);
		if (f.isMined() &&!(f.isFlagged() && safeBombFlags))
			mineBlock.setType(Material.TNT);
		else
			mineBlock.setType(f.getCurrentBaseMat());
	}
	
	private void updateAllFields() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				updateField(x, z);
			}
		}
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
	
	public void delete() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				Field f = getField(x,z);
				Block topBlock = world.getBlockAt(f.globX, f.globY+1, f.globZ);
				topBlock.setType(Material.AIR);
				Block baseBlock = world.getBlockAt(f.globX, f.globY, f.globZ);
				baseBlock.setType(Material.AIR);
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
		if (field.isMined())
			correctlyFlaggedCount++;
		else
			falselyFlaggedCount++;
		field.flagIfPossible();
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

	public void initializeBoard(Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		shuffleWithNoBombHere(relCoord);
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

	public void shuffleWithNoBombHere(Vector relCoord) {
		int bombfreeX = relCoord.getBlockX();
		int bombfreeZ = relCoord.getBlockZ();
		
		cleanBoard();
		
		Random randGen = new Random();
		int x, z;
		int placedBombs = 0;
		while (placedBombs < bombCount) {
			x = randGen.nextInt(width);
			z = randGen.nextInt(height);
			if (!curFields[x][z].isMined() && !(x == bombfreeX && z == bombfreeZ)) {
				setMineAndUpdate(x, z, true);
				placedBombs++;
			}
		}
		//TODO too many bombs warning if more bombs than fields-1?? -> move sanity check to settings
	}

	public void activatePressurePlate(Vector globCoord) {
		Vector relCoord = transform.toRelative(globCoord);
		int x = relCoord.getBlockX();
		int z = relCoord.getBlockZ();
		// if a pressure plate is activated if will either:
		// - trigger a bomb, then the game is lost, no update needed:
		if (curFields[x][z].isMined()) {
			bombWasTriggered = true;
			return;
		}
		// - or a field is now "known" (FieldState) and the board hints need to be updated
		revealUpdateAndPropagate(x,z);
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
	
	public Location getBoardPose() {
		return boardPose;
	}
}

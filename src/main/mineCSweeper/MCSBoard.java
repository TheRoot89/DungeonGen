package mineCSweeper;

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
	
	//final BlockData[] 	bombClueColors = null; //TODO he? so, how to do this?
	final Material		defaultBoardMaterial = Material.STONE;
	
	// ########################## Member variables ############################
	private World		world;		// needed for block manipulation
	private Transform	transform; 	// wraps world position transform of the board
	private int			relZOffsetForBoardCentering;
	private Field[][] 	curFields;
	private int			width;
	private int			height;
	private int			bombCount;
	private int			correctlyFlaggedCount;
	
	
	// ############################ Member functions ############################
	public MCSBoard(World world, Location boardPose, MCSSettings settings) {
		this.world = world;
		correctlyFlaggedCount = 0;
		// load the settings now, these will thus not change until a new board is generated:
		loadSettings(settings);
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
	}
	
	private void updateField(int x, int z) {
		Field f = curFields[x][z];
		Block baseBlock = world.getBlockAt(f.globX, f.globY, f.globZ);
		baseBlock.setType(f.getBaseMat());
		Block topBlock = world.getBlockAt(f.globX, f.globY+1, f.globZ);
		topBlock.setType(f.getTopMat());
	}
	
	private void updateAllFields() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				updateField(x, z);
			}
		}
	}
	
	
	private void updateFields(int[] x, int[] z) {
		// TODO stub, is this even needed?
	}
	
	public void shuffle(int playerX, int playerZ) {
		// TODO stub
	}

	public void placeCleanBoard() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				curFields[x][z].clean();
			}
		}
		updateAllFields();
	}
	
	public void delete() {
		for (int x=0; x<height; x++) {
			for (int z=0; z<width; z++) {
				Field f = curFields[x][z];
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
		Field field = curFields[x][z];
		if (field.isMined())
			correctlyFlaggedCount++;
		field.flagIfPossible();
		updateField(x, z);
	}
	
	public void removeFlagAndUpdate(int x, int z) {
		//TODO to what field type should it be set now?? -> gameMechanic has to do that!
		Field field = curFields[x][z];
		if (field.isMined())
			correctlyFlaggedCount--;
		field.unFlagIfPossible();
		updateField(x, z);
	}
	
	public boolean allMinesFlagged() {
		return bombCount == correctlyFlaggedCount;
	}
}

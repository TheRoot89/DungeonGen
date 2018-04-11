package mineCSweeper;

import org.bukkit.Material;

import com.sk89q.worldedit.blocks.BlockData;

/**Represents the physical board the game of MCS is played upon. Regarding coordinates it is treated as
 * looking east, i.e. coordinates can counted positively forward, up and to the right in x, y and z respectively.
 */
public class MCSBoard {
	
	final BlockData[] bombClueColors = null; //TODO he?
	// ########################## Member variables ############################
	private Material[][] 	curFields;
	private boolean[][] 	fieldHasBomb;
	private boolean[][]		fieldIsFlagged;
	private boolean[][]		fieldIsDirty;
	public 	int				width;
	public 	int				height;
	public 	int				nrBombs;
	
	// ############################ Member functions ############################
	public MCSBoard() {
		// TODO stub
	}
	
	public void updateFields(int x, int z) {
		// TODO stub
	}
	
	public void shuffle(int playerX, int playerZ) {
		// TODO stub
	}
}

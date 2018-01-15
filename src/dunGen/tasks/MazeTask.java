/**
 * 
 */
package dunGen.tasks;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import dunGen.Room;
import dunGen.utils.MazeWall;
import dunGen.utils.Maze_RecursBacktr;

/**
 */
public class MazeTask extends RoomTask {

	private MazeWall[][] mazeWalls;
	private Material 	 mazeMaterial;
	//private Vector   	 mazeEntry;
	//private Vector   	 mazeExit;
	private int		 	 wayWidth;
	private int		 	 wallWidth;
	private int			 wallHeight;
	
	public MazeTask(Room parent, FileConfiguration conf, int taskNr) {
		super(parent, conf, taskNr);
		assert(period == 0);	// This task is not allowed to run multiple times -> period MUST be zero!
		this.type = TaskType.MAZE;
		// loading values for this Task type:
		String path = "tasks.task" + this.taskNr + ".";
		mazeMaterial = Material.getMaterial(conf.getString(path + "mazeMaterial").toUpperCase(Locale.ENGLISH)); // this is a lookup 'string' -> 'enum value'
		//mazeEntry =   BukkitUtil.toVector(conf.getVector(path + "entry"));
		//mazeExit  =   BukkitUtil.toVector(conf.getVector(path + "exit"));
		wayWidth  =	  conf.getInt(path + "wayWidth");
		wallWidth =	  conf.getInt(path + "wallWidth");
		wallHeight=	  conf.getInt(path + "wallHeight");
	}


	@Override
	public void run() {
		// 1. Generate the maze using recursive backtracking. First, calc the size.
		int cellSize = wayWidth + wallWidth;
		int width = Math.abs(targetRegion.getPos1().getBlockZ() - targetRegion.getPos2().getBlockZ()) + 1;	// width in MC blocks
		int height = Math.abs(targetRegion.getPos1().getBlockX() - targetRegion.getPos2().getBlockX())+ 1;	// height in MC blocks
		double width_MazeCellsD  = (double)(width+wallWidth)/cellSize;    //length = n*(wayWidth + wallWidth) -wallWidth   =>  n = (length+wallWidth)/(wayWidth+wallWidth)
		double height_MazeCellsD = (double)(height+wallWidth)/cellSize;	// double value trying to 
		int width_MazeCells  = (int)Math.floor(width_MazeCellsD);
		int height_MazeCells = (int)Math.floor(height_MazeCellsD);
		
		if (width_MazeCellsD != width_MazeCells || height_MazeCellsD != height_MazeCells ) {
			//TODO give error or exception upwards
			return;
		}
		mazeWalls = Maze_RecursBacktr.genMaze(height_MazeCells, width_MazeCells);
		
		// Debug Output to see maze generated:
		/*
		String s = "\n";
		for (int r=mazeWalls.length-1; r>=0; r--) {
			//north wall:
			s += "#";
			for (int c=0; c < mazeWalls[0].length; c++) {
				if (mazeWalls[r][c].north)	s += "###";
				else						s += "  #";
			}
			s += "\n";
			//middle row:
			s += "#";
			for (int c=0; c < mazeWalls[0].length; c++) {
				if (mazeWalls[r][c].east)	s += "  #";
				else						s += "   ";
			}
			s += "\n";
			//bottom row:
			s += "#";
			for (int c=0; c < mazeWalls[0].length; c++) {
				if (mazeWalls[r][c].east)	s += "  #";
				else						s += "   ";
			}
			s += "\n";
		}
		// Last line:
		s += "#";
		for (int c=0; c < mazeWalls[0].length; c++) {
			s += "###";
		}
		parent.getPlugin().getLogger().info(s);
		*/
				
		
		
		// 2. Generate a building plan kind of map:
		boolean[][] buildPlan = new boolean[height+wallWidth][width+wallWidth]; // top and right outer wall included. Already built in the model though! (stripped later from build plan)
			// go over the maze cells from the algo, row by row:
		for (int cellX = 0; cellX < height_MazeCells; cellX++) {	// directions like in minecraft: rows are X (up), cols are Z (to the right)
			for (int cellZ = 0; cellZ < width_MazeCells; cellZ++) {
				int bpCellX = cellX*cellSize; // conversion to position in real grid. Always lower left block. 'bp' for 'BuildPlan'
				int bpCellZ = cellZ*cellSize;
				// set the blocks with walls to true in the building plan grid:
				if (mazeWalls[cellX][cellZ].north) {	// northern
					for (int bpX=bpCellX+wayWidth; 	bpX < (bpCellX+cellSize); bpX++)		// only top of this cell
						for (int bpZ=bpCellZ; 		bpZ < (bpCellZ+cellSize); bpZ++)		// from whole left to right in this cell
							buildPlan[bpX][bpZ] = true;
				}
				if (mazeWalls[cellX][cellZ].east) {		// eastern
					for (int bpX=bpCellX; 	    		bpX < (bpCellX+cellSize); bpX++)		// from whole bottom to top of this cell
						for (int bpZ=bpCellZ+wayWidth;	bpZ < (bpCellZ+cellSize); bpZ++)		// only right "wall" part of this cell
							buildPlan[bpX][bpZ] = true;
				}
				if (!mazeWalls[cellX][cellZ].north && !mazeWalls[cellX][cellZ].east) {	// only the corner top right
					for (int bpX=bpCellX+wayWidth;	    bpX < (bpCellX+cellSize); bpX++)		// only top of this cell
						for (int bpZ=bpCellZ+wayWidth;	bpZ < (bpCellZ+cellSize); bpZ++)		// only right "wall" part of this cell
							buildPlan[bpX][bpZ] = true;
				}
					
			}
		}
		
		// if we only generate the inner walls, we do not need to specify the entry and exit ourselves, right?
		// Skip the last row and column, as they are part of the outer wall-> set to false:
		for (int bpX=0; bpX < (height+wallWidth); bpX++)	//eastern wall
			for (int bpZ=width; bpZ < (width+wallWidth);  bpZ++)
				buildPlan[bpX][bpZ] = false;
		
		for (int bpX=height; bpX < (height+wallWidth); bpX++)	//northern wall
			for (int bpZ=0; bpZ < (width+wallWidth);  bpZ++)
				buildPlan[bpX][bpZ] = false;
		
		// Debug output:
		/*
		String s = "\n";
		for (int r=buildPlan.length-1; r>=0; r--) {
			for (int c=0; c < buildPlan[0].length; c++) {
				if (buildPlan[r][c])	s += "#";
				else					s += " ";
			}
			s += "\n";
		}
		parent.getPlugin().getLogger().info(s);
		*/
		
		// Debug buildplan: only one block in lower left
		//buildPlan = new boolean[height+wallWidth][width+wallWidth];
		//buildPlan[0][0] = true;
		
		
		// 3. Have it built:
		parent.placeBuildPlan2D(targetRegion.getMinimumPoint(), buildPlan, mazeMaterial, wallHeight);
		
		
		/*
			// add gap where the entry should be:
		Vector areaWallMinP = targetRegion.getMinimumPoint().subtract(1, 0, 1);
		Vector areaWallMaxP = targetRegion.getMaximumPoint().add(1, 0, 1);
		if (mazeEntry.getBlockZ() == areaWallMinP.getBlockZ()) {			// left wall has entry
			//TO BE FILLED
			
		}else if (mazeEntry.getBlockZ() == areaWallMaxP.getBlockZ()) {		// right wall has it
			
		}else {																// ok, must be top or bottom:
			if (mazeEntry.getBlockX() == areaWallMinP.getBlockX()) {				// yeah, its bottom
				
			}else if (mazeEntry.getBlockX() == areaWallMaxP.getBlockX()) {			// else its top
				
			}else {																	// none fit -> we have an error!
				// THROW ERROR
			}
		}
		
			// add gap where the exit should be:
		if (mazeExit.getBlockZ() == areaWallMinP.getBlockZ()) {				// left wall has exit
			
		}else if (mazeExit.getBlockZ() == areaWallMaxP.getBlockZ()) {		// right wall has it
			
		}else {																// ok, must be top or bottom:
			if (mazeExit.getBlockX() == areaWallMinP.getBlockX()) {					// yeah, its bottom
				
			}else if (mazeExit.getBlockX() == areaWallMaxP.getBlockX()) {			// else its top
				
			}else {																	// none fit -> we have an error!
				//THROW ERROR
			}
		}
		*/
	}
	
	

}

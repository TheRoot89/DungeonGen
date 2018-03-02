/**
 * 
 */
package dunGen.tasks;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.Vector;

import dunGen.Helper.Direc;
import dunGen.Room;
import dunGen.utils.MazeWall;
import dunGen.utils.Maze_RecursBacktr;

/**A Task to generate a maze in a given area. The area has to exactly fit the maze inside!
 * The number n of maze cells in one dimension fulfills: length_in_blocks = n*(wayWidth + wallWidth) - wallWidth
 */
public class MazeTask extends RoomTask {

	private int			 carveBestExit;
	private int			 entryCell;
	private int[][]		 distToEntry;
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
		wayWidth  =	  	conf.getInt(path + "wayWidth");
		wallWidth =	  	conf.getInt(path + "wallWidth");
		wallHeight=	  	conf.getInt(path + "wallHeight");
		carveBestExit = conf.getInt(path + "carveBestExit");
		if (carveBestExit != 0)
			entryCell = conf.getInt(path + "entryCell");
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
			parent.getPlugin().getLogger().warning("Given maze dimensions don't fit! Aborting generation.");
			parent.getPlugin().getLogger().warning("Width in blocks: " + width + "\n" +
												   "Height in blocks: " + height + "\n" +
												   "Cell width calculated: " + width_MazeCellsD + "\n" +
												   "Cell height calculated: " + height_MazeCellsD);
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
		
		// we only generate the inner walls (so we do not need to specify the entry and exit ourselves if option not set)
		// Skip the last row and column, as they are part of the outer wall-> trim by not copying
		boolean[][] finalBuildPlan = new boolean[height][width];
		for (int bpX=0; bpX < height; bpX++)
			for (int bpZ=0; bpZ < width; bpZ++)
				finalBuildPlan[bpX][bpZ] = buildPlan[bpX][bpZ];	
		
		// Debug output:
		String s = "Final build plan:\n";
		for (int r=finalBuildPlan.length-1; r>=0; r--) {
			for (int c=0; c < finalBuildPlan[0].length; c++) {
				if (finalBuildPlan[r][c])	s += "#";
				else					s += " ";
			}
			s += "\n";
		}
		parent.getPlugin().getLogger().info(s);
		
		// 3. Have it built: (overwriting with air, maybe have a setting for that)
		parent.placeBuildPlan2D(targetRegion.getMinimumPoint(), finalBuildPlan, mazeMaterial, wallHeight, true);
		
		// 4. Carve best exit if set
		if (carveBestExit != 0) {
			// get the dists of every cell to the entry cell:
			distToEntry = Maze_RecursBacktr.getDistToEntry(mazeWalls, entryCell);
			
			// Debug output:
			s = "Dist to entry:\n";
			for (int r=distToEntry.length-1; r>=0; r--) {
				for (int c=0; c < distToEntry[0].length; c++) {
					int dist = distToEntry[r][c];
					if (dist < 10) {
						s =s + "|0" + dist;
					}else {
						s = s + "|" + dist;
					}
					
				}
				s += "\n";
			}
			parent.getPlugin().getLogger().info(s);
			
			
			// check allowed sides and the dists of every cell there to get the longest one:
			// 5-Bit Bitfield for outher walls allowed to be carved: [floor hole(16) left(8) upfront(4) right(2) start(1)] -> sum, 0 switches off
			int bitfield = carveBestExit;
			int highestDist = 0;
			int bestCellX = 0;
			int bestCellZ = 0;
			Direc exitDirec = Direc.WEST; //just initial value if 0/0 were the result
			if (bitfield >= 16) { // any cell is ok as florr hole is activated
				for (int cellX = 0; cellX < height_MazeCells; cellX++) {
					for (int cellZ = 0; cellZ < width_MazeCells; cellZ++) {
						if (distToEntry[cellX][cellZ] > highestDist) {
							highestDist = distToEntry[cellX][cellZ];
							bestCellX = cellX;
							bestCellZ = cellZ;
							exitDirec = null; // additional flag for "hole mode"
						}
						bitfield = 0; // deactivates further searches
					}
				}
			}
			
			if (bitfield >= 8) { // left wall is allowed to have exit, search:
				for (int cellX = 0; cellX < height_MazeCells; cellX++)
					if (distToEntry[cellX][0] > highestDist) {
						highestDist = distToEntry[cellX][0];
						bestCellX = cellX;
						bestCellZ = 0;
						exitDirec = Direc.WEST;
					}
				bitfield -= 8;
			}
			
			if (bitfield >= 4) { // upfront wall is allowed to have exit, search:
				for (int cellZ = 0; cellZ < width_MazeCells; cellZ++)
					if (distToEntry[height_MazeCells-1][cellZ] > highestDist) {
						highestDist = distToEntry[height_MazeCells-1][cellZ];
						bestCellX = height_MazeCells-1;
						bestCellZ = cellZ;
						exitDirec = Direc.NORTH;
					}
				bitfield -= 4;
			}
			
			if (bitfield >= 2) { // right wall is allowed to have exit, search:
				for (int cellX = 0; cellX < height_MazeCells; cellX++)
					if (distToEntry[cellX][width_MazeCells-1] > highestDist) {
						highestDist = distToEntry[cellX][width_MazeCells-1];
						bestCellX = cellX;
						bestCellZ = width_MazeCells-1;
						exitDirec = Direc.EAST;
					}
				bitfield -= 2;
			}
			
			if (bitfield >= 1) { // start wall is allowed to have exit, search:
				for (int cellZ = 0; cellZ < width_MazeCells; cellZ++)
					if (distToEntry[0][cellZ] > highestDist) {
						highestDist = distToEntry[0][cellZ];
						bestCellX = 0;
						bestCellZ = cellZ;
						exitDirec = Direc.SOUTH;
					}
				bitfield -= 1;
			}
			
			// now, the best exit and its direction were found (or 0/0 = lower left if something went wrong)
			// carve a path as a small building plan that overwrites with air
			// a picture is needed to see the coordinate relations below
			Vector bpOrigin = targetRegion.getMinimumPoint(); //lower left point inside the outer walls
			if (exitDirec != null) { // wall mode (a normal doorway)
				switch (exitDirec) {
				case WEST:
					buildPlan = new boolean[wayWidth][wallWidth];
					bpOrigin = bpOrigin.add(bestCellX*cellSize,0,-wallWidth);
					break;
				case NORTH:
					buildPlan = new boolean[wallWidth][wayWidth];
					bpOrigin = bpOrigin.add((bestCellX+1)*cellSize-wallWidth,0,bestCellZ*cellSize);
					break;
				case EAST:
					buildPlan = new boolean[wayWidth][wallWidth];
					bpOrigin = bpOrigin.add(bestCellX*cellSize,0,(bestCellZ+1)*cellSize-wallWidth);
					break;
				case SOUTH:
					buildPlan = new boolean[wallWidth][wayWidth];
					bpOrigin = bpOrigin.add(-wallWidth,0,bestCellZ*cellSize);
					break;
				}
				parent.placeBuildPlan2D(bpOrigin, buildPlan, Material.AIR, wallHeight, true);
			}else { //hole mode
				buildPlan = new boolean[wayWidth][wayWidth];
				bpOrigin = bpOrigin.add(bestCellX*cellSize,-1,bestCellZ*cellSize); //floor ->  y-1
				parent.placeBuildPlan2D(bpOrigin, buildPlan, Material.AIR, 1, true);
			}
			
		} // end if carve best exit
		
		
		// parent run() has to be called for counter!
		super.run();
	}
	
	

}

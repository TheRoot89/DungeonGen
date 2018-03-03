package dunGen.utils;

import java.util.Random;

import dunGen.Helper;
import dunGen.Helper.Direc;


public class Maze_RecursBacktr {
	
	//############### Working Variables: ################
	private static Random       rand = new Random();
	private static int          w;			// width of the maze in cells
	private static int          h;			// height of the maze in cells
	private static MazeWall[][] walls;		// a matrix = maze map of MazeWall structs, describing the walls we have here
	private static boolean[][]  connected;	// a matrix = maze map saving which cells of the maze have already been connected to the whole
	private static int[][]      distToEntry;// a matric = maze map showing how far cells are to the entry -> exit can be placed with min distance
	
	//################### Methods: ######################
	
	/**Applies the recursive backtracking algorithm to generate a maze map (specific data structure).
	 * This function initializes the recursive process.
	 * @param height	height in maze cells (maze hallway + nothern and eastern walls)
	 * @param width		width in maze cells
	 * @return			a matrix of maze cells, giving the existence of northern and eastern walls each.
	 */
	public static MazeWall[][] genMaze(int height, int width) {
		// Initialization, using mc coordinate system:
		w = width;
		h = height;
		walls = new MazeWall[h][w];
		for(int r=0; r<walls.length;r++)
			for(int c=0; c<walls[0].length;c++)
				walls[r][c] = new MazeWall();// as can be seen above, all values are 'true' = walls exist
		connected = new boolean[h][w];	// all values are 'false' initially
		int x = rand.nextInt(height);
		int z = rand.nextInt(width);
		
		// Recursion start:
		addCell2Maze(x, z);
		
		return walls;
	}
	
	
	/**Calculates for each maze cell the distance to the given entry point. Call only AFTER a maze was generated!
	 * This allows to estimate how difficult it is to reach each cell of the maze, including the exit cell.
	 * @param maze
	 * @param entryCell
	 * @return
	 */
	public static int[][] getDistToEntry(MazeWall[][] maze, int entryCell){
		// Init, using mc coordinate system and static variables to access during the process:
		walls = maze;
		h = maze.length;
		w = maze[0].length;
		distToEntry = new int[h][w];
		int z = entryCell-1; // counting starts from 0
		int x = 0; // as we always start at the southern wall here
		
		// Recursion start:
		addCell2Path(x, z, 1); // the first cell has distance '1' from the entry
		
		return distToEntry;
	}
	

	/**Add this cell to the maze (make it connected). This cell will recursively try to add other cells randomly.
	 * Abort condition is, if all directions were tried.
	 * @param x The cell x coordinate within the maze.
	 * @param y The cell y coordinate within the maze.
	 */
	private static void addCell2Maze(int x, int z) {
		connected[x][z] = true;
		
		Direc[] direcs = {Direc.NORTH, Direc.EAST, Direc.SOUTH, Direc.WEST};
		shuffleArray(direcs);
		
		int newX;
		int newZ;
		for (Direc d : direcs) { 	// short syntax to interate over array without counter
			newX = x - (int)Helper.cosd(d.degree()); // The associations of MC Direcs to degrees does not fit to mathematical degrees!
			newZ = z - (int)Helper.sind(d.degree()); // A picture helps most with this, see Direc definition (F3 on Direc)
			// also add if not added yet and in scope:
			if (newX >= 0 && newX < h &&  newZ >= 0 && newZ < w && !connected[newX][newZ]) {
				// carve a passage: grid always saves north and east walls:
				switch (d) {
				case NORTH:
					walls[x][z].north = false;
					break;
				case SOUTH:
					walls[x-1][z].north = false;
					break;
				case EAST:
					walls[x][z].east = false;
					break;
				case WEST:
					walls[x][z-1].east = false;
					break;
				}
				addCell2Maze(newX, newZ);
			}
		}
		
		// recursion ends here after all directions have been tried.
	}
	
	
	/**
	 * @param x
	 * @param z
	 * @param newDist
	 */
	private static void addCell2Path(int x, int z, int newDist) {
		distToEntry[x][z] = newDist;// mark this cell with the distance travelled, also marks whether visited already
		int nextDist = newDist+1;
		// Iterate over the neighboring cells:
		// North:
		int newX = x+1;
		if ( !walls[x][z].north && 
			 newX >= 0 && newX < h &&	
			 (distToEntry[newX][z] == 0 || distToEntry[newX][z] > nextDist) ) {
			addCell2Path(newX, z, nextDist);
		}
		// East:
		int newZ = z+1;
		if ( !walls[x][z].east && 
			 newZ >= 0 && newZ < w &&	
			 (distToEntry[x][newZ] == 0 || distToEntry[x][newZ] > nextDist) ) {
			addCell2Path(x, newZ, nextDist);
		}
		// South:
		newX = x-1;
		if ( newX >= 0 && newX < h &&
			 !walls[newX][z].north && 
			 (distToEntry[newX][z] == 0 || distToEntry[newX][z] > nextDist) ) {
			addCell2Path(newX, z, nextDist);
		}
		// West:
		newZ = z-1;
		if ( newZ >= 0 && newZ < w &&	
			 !walls[x][newZ].east && 
			 (distToEntry[x][newZ] == 0 || distToEntry[x][newZ] > nextDist) ) {
			addCell2Path(x, newZ, nextDist);
		}
		// recursion ends here after all directions have been tried.
	}
	
	
	/**Implements a Fisher–Yates shuffle of a given array. Changing the passed object here without return. (call by reference)
	 * @param array   An array of Directions
	 */
	private static void shuffleArray(Direc[] array) {
	    int swapIndex;
	    for (int i = array.length - 1; i > 0; i--) {
	        swapIndex = rand.nextInt(i + 1);
	        if (swapIndex != i) {
	        	Direc temp = array[i];
	        	array[i] = array[swapIndex];
	        	array[swapIndex] = temp;
	        }
	    }
	}
	
}


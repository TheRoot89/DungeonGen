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
	
	public static MazeWall[][] genMaze(int height, int width) {
		// Initialization:
		w = width;
		h = height;
		walls = new MazeWall[height][width];
		for(int r=0; r<walls.length;r++)
			for(int c=0; c<walls[0].length;c++)
				walls[r][c] = new MazeWall();// as can be seen above, all values are 'true' = walls exist
		connected = new boolean[height][width];	// all values are 'false' initially
		int x = rand.nextInt(width);
		int y = rand.nextInt(height);
		
		// Recursion start:
		addCell2Maze(x, y);
		
		return walls;
	}
	
	
	/**Add this cell to the maze (make it connected). This cell will recursively try to add other cells randomly.
	 * Abort condition is, if all directions were tried.
	 * @param x The cell x coordinate within the maze.
	 * @param y The cell y coordinate within the maze.
	 */
	private static void addCell2Maze(int x, int y) {
		connected[x][y] = true;
		
		Direc[] direcs = {Direc.NORTH, Direc.EAST, Direc.SOUTH, Direc.WEST};
		shuffleArray(direcs);
		
		int newX;
		int newY;
		for (Direc d : direcs) { 	// short syntax to interate over array without counter
			newX = x - (int)Helper.sind(d.degree()); // The associations of MC Direcs to degrees does not fit to mathematical degrees!
			newY = y - (int)Helper.cosd(d.degree());
			// also add if not added yet and in scope:
			if (newX >= 0 && newX < w &&  newY >= 0 && newY < h && !connected[newX][newY]) {
				// carve a passage: grid always saves north and east walls:
				switch (d) {
				case NORTH:
					walls[y][x].north = false;
					break;
				case SOUTH:
					walls[y-1][x].north = false;
					break;
				case EAST:
					walls[y][x].east = false;
					break;
				case WEST:
					walls[y][x-1].east = false;
					break;
				}
				addCell2Maze(newX, newY);
			}
		}
		
		// recursion ends here after all directions have been tried.
	}
	

	/**Calculates for each maze cell the distance to the given entry point. Call only AFTER a maze was generated!
	 * This allows to estimate how difficult it is to reach each cell of the maze, including the exit cell.
	 * @param entryX	The cell from which distances are to be calculated, x direction
	 * @param entryY	The cell from which distances are to be calculated, y direction
	 */
	public static void calcDistToEntry(int entryX, int entryY) {
		distToEntry = new int[w][h];	// all values are 0 initially
		// TODO eval
	}
	
	
	/**Implements a Fisher–Yates shuffle of a given array. Changing the passed object here without return.
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


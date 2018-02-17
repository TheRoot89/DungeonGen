package dunGen;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

/** Wrapps different convenience functions. */
public class Helper {
	
	// Direc describes the four global directions. The degree count rises clockwise: south -> west -> north -> east
	public enum Direc {
		SOUTH (0),
		WEST (90),
		NORTH (180),
		EAST (270);
		
		private int degree;	// holds the degree associated with this direction.
		
		/**Factory to get a Direction from a degree. Does the rounding.
		 * @param deg	The degree you look at.
		 * @return		The direction you look at.
		 */
		public static Direc fromDeg(float deg) {
			deg = (deg+360) % 360;
			if (deg < 45) {
				return Direc.SOUTH;
			}else if (deg < 135) {
				return Direc.WEST;
			}else if (deg < 225) {
				return Direc.NORTH;
			}else if (deg < 315) {
				return Direc.EAST;
			}else {
				return Direc.SOUTH;
			}
		}

		
		/**
		 * Rotates a vector by a certain degree. Only steps of 90 degrees are allowed!
		 * @param vec	The vector object (reference!) to be changed.
		 * @param deg	
		 */
		public static Vector rotatedBy(Vector vec, int deg) {
			//System.out.println("Vec to turn:" + vec.toString());
			// getting positive values for the angle:
			deg = (deg+360) % 360; // modulo
			// input check:
			if ((deg < 0)||((deg%90) != 0)) {
				System.out.println("Invalid rotation angle!");
				return new Vector();
			}
			
			// rotation:
			Vector ret = new Vector(vec);
			if (deg == 0) {
				//nothing to do
			}else if (deg == 90) {
				ret = new Vector(-vec.getBlockZ(),vec.getBlockY(),vec.getBlockX());
			}else if(deg == 180) {
				ret = new Vector(-vec.getBlockX(),vec.getBlockY(),-vec.getBlockZ());
			}else if(deg == 270) {
				ret = new Vector(vec.getBlockZ(),vec.getBlockY(),-vec.getBlockX());
			}
			return ret;
		}
		
		
		/**Constructor, saving the degree. Called implicitely by the enum values.
		 * @param degree	0, 90, 180 or 270
		 */
		Direc(int degree){
			this.degree = degree;
		}
		
		/**Getter */
		public int degree() {return degree;}
		
		/**Return the unity vector associated with this direction.
		 * Used to go one block in that direction.*/
		public Vector toUnityVec() {
			return new Vector(-Helper.sind(degree),0,Helper.cosd(degree));
		}
	}	
	
	
	
	/**Cosine function taking degrees.
	 * @param deg	a degree in int.
	 * @return 		a double.
	 */
	public static double cosd(int deg) {
		return Math.cos(Math.toRadians(deg));
	}
	
	
	/**Fills the volum between v1 and v2 in world w with Material m
	 * @param w		The worls were this is to be generated.
	 * @param v1	One volume corner.
	 * @param v2	The other corner.
	 * @param m		The block type to use.
	 */
	public static void fillVolume(World w, Vector v1, Vector v2, Material m) {
		for (int x = Math.min(v1.getBlockX(), v2.getBlockX()); x <= Math.max(v1.getBlockX(), v2.getBlockX()); x++)
		for (int y = Math.min(v1.getBlockY(), v2.getBlockY()); y <= Math.max(v1.getBlockY(), v2.getBlockY()); y++)
		for (int z = Math.min(v1.getBlockZ(), v2.getBlockZ()); z <= Math.max(v1.getBlockZ(), v2.getBlockZ()); z++)
			w.getBlockAt(x, y, z).setType(m);
	}
	
	
	/**Get the Direc enum for a player.
	 * @param player
	 * @return			The Direc, rounded.
	 */
	public static Direc getPlayerDirec(Player player) {
		float playerYaw = player.getLocation().getYaw();
		return Direc.fromDeg(playerYaw);
	}
	
	
	/**Get a random position (Vector) from a target region. Dos not use the DunGen seed!
	 * @param r 	The region.
	 * @return		The vector.
	 */
	public static Vector getRandVector(CuboidRegion r) {
		// will be a half point off to be in center of a block (block vs. world coordinates!) meh
		Random rand = new Random();
		Vector targetV = new Vector();
		Vector minP = r.getMinimumPoint();
		Vector maxP = r.getMaximumPoint();
		targetV = targetV.setX(minP.getBlockX() + rand.nextInt(maxP.getBlockX() - minP.getBlockX() + 1)); //plus 1 needed for inclusive boundary of values
		targetV = targetV.setY(minP.getBlockY() + rand.nextInt(maxP.getBlockY() - minP.getBlockY() + 1));
		targetV = targetV.setZ(minP.getBlockZ() + rand.nextInt(maxP.getBlockZ() - minP.getBlockZ() + 1));
		return targetV;
	}
	
	
	/**Rotates a matrix clockwise. Returns null if the degrees are not multiple of 90!
	 * It works by copying over the values but doing this in reverse or rotated order.
	 * @param mat		The matrix to be rotated
	 * @param degree	Valid are of course only multiples of 90!
	 * @return			The rotated matrix, a copy.
	 */
	public static boolean[][] rotateBoolMatrixClockw(boolean[][] mat, int degree) {
		degree = degree%360;
	    final int M = mat.length;
	    final int N = mat[0].length;
	    boolean[][] ret;
	    switch (degree) {							// 'break' statements not needed as we return in each case.
		case 0:				// nothing to turn
			return mat;
		case 90:			// clockwise
			ret = new boolean[N][M];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[N-1-c][r] = mat[r][c];
		        }
		    }
		    return ret;
		case 180:			// on its head (not flipped!)
			ret = new boolean[M][N];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[M-1-r][N-1-c] = mat[r][c];
		        }
		    }
		    return ret;
		case 270:			// counter-clockwise
			ret = new boolean[N][M];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[c][M-1-r] = mat[r][c];
		        }
		    }
		    return ret;
		default:			// degree must be off
			return null;
		}
	}
	
	
	/**Sine function taking degrees.
	 * @param deg	a degree in int.
	 * @return 		a double.
	 */
	public static double sind(int deg) {
		return Math.sin(Math.toRadians(deg));
	}
	
}

package mcPluginHelpers;

import javaHelpers.MathHelpers;

import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;

/**Direc describes the four global directions.
 * The degree count rises clockwise: south (zero) to west (90) to north to east*/
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

	
	/**Get the Direc enum for a player.
	 * @param player	The player whos heading shall be read out.
	 * @return			The Direc, rounded.
	 */
	public static Direc getPlayerDirec(Player player) {
		float playerYaw = player.getLocation().getYaw();
		return Direc.fromDeg(playerYaw);
	}
	
	
	/**
	 * Rotates a vector by a certain degree. Only steps of 90 degrees are allowed!
	 * @param vec	The vector object (reference!) to be changed.
	 * @param deg	The degrees the vector should be rotated by, in steps of 90°!
	 * @return		The rotated vector. The original vector if the input degrees were invalid.
	 * 				A message is printed to System.out in that case.
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
	 * @see MCHelpers.Direc
	 */
	Direc(int degree){
		this.degree = degree;
	}
	
	/**Getter
	 * @return the degrees associated with this direction.
	 */
	public int degree() {return degree;}
	
	public int rotationFromEast() {
		int rotation = degree - Direc.EAST.degree;
		return (rotation +360) % 360;
	}
	
	/**For example used to go one block in this direction.
	 * @return the unity vector associated with this direction.
	 */
	public Vector toUnityVec() {
		return new Vector(-MathHelpers.sind(degree),0,MathHelpers.cosd(degree));
	}
	
	public org.bukkit.util.Vector toBukkitUnityVec() {
		return new org.bukkit.util.Vector(-MathHelpers.sind(degree),0,MathHelpers.cosd(degree));
	}
	
	public org.bukkit.util.Vector toBukkitVec(int length) {
		org.bukkit.util.Vector vec = toBukkitUnityVec();
		vec = vec.multiply(length);
		return vec;
	}
}	

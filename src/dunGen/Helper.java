package dunGen;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public class Helper {
	
	public enum Direc {
		NORTH (180),
		WEST (90),
		SOUTH (0),
		EAST (270);
		
		private int degree;

		Direc(int degree){
			this.degree = degree;
		}
		
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
		
		public int degree() {return degree;}
		
		/**
		 * Rotates a vector by a certain degree. Only steps of 90° are allowed!
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
		
		public Vector toUnityVec() {
			return new Vector(-Helper.sind(degree),0,Helper.cosd(degree));
		}
		
	}
	
	
	
	
	
	public static Direc getPlayerDirec(Player player) {
		float playerYaw = player.getLocation().getYaw();
		return Direc.fromDeg(playerYaw);
	}
	
	public static double sind(int deg) {
		return Math.sin(Math.toRadians(deg));
	}
	
	public static double cosd(int deg) {
		return Math.cos(Math.toRadians(deg));
	}
	
	// Fills the volum between v1 and v2 in world w with Material m
	public static void fillVolume(World w, Vector v1, Vector v2, Material m) {
		for (int x = Math.min(v1.getBlockX(), v2.getBlockX()); x <= Math.max(v1.getBlockX(), v2.getBlockX()); x++)
		for (int y = Math.min(v1.getBlockY(), v2.getBlockY()); y <= Math.max(v1.getBlockY(), v2.getBlockY()); y++)
		for (int z = Math.min(v1.getBlockZ(), v2.getBlockZ()); z <= Math.max(v1.getBlockZ(), v2.getBlockZ()); z++)
			w.getBlockAt(x, y, z).setType(m);
	}
	
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
	
}

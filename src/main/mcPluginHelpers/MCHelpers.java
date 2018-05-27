package mcPluginHelpers;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

/** Wrapps different convenience functions. */
public class MCHelpers {
	
	/**Fills the volum between v1 and v2 in world w with Material m
	 * @param w		The worls were this is to be generated.
	 * @param v1	One volume corner.
	 * @param v2	The other corner.
	 * @param m		The block type to use.
	 */
	public static void fillVolume(World w, Vector v1, Vector v2, Material m) {
		int x = 0,y = 0,z = 0;
		for (x = Math.min(v1.getBlockX(), v2.getBlockX()); x <= Math.max(v1.getBlockX(), v2.getBlockX()); x++)
		for (y = Math.min(v1.getBlockY(), v2.getBlockY()); y <= Math.max(v1.getBlockY(), v2.getBlockY()); y++)
		for (z = Math.min(v1.getBlockZ(), v2.getBlockZ()); z <= Math.max(v1.getBlockZ(), v2.getBlockZ()); z++) {
			w.getBlockAt(x, y, z).setType(m);
		}
		// Trigger block update to not miss redstone being set or stuff should start falling etc.
		w.getBlockAt(x, y, z).getState().update(true, true);	
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
	
	

	
}

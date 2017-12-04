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

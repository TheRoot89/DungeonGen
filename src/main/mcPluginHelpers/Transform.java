package mcPluginHelpers;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

import dunGen.DunGen.State;
import javafx.scene.chart.StackedAreaChart;

public class Transform {
	
	private Vector origin;
	private int rotatedBy;
	
	public Transform(Vector origin, Direc globalDirec) {
		this.origin = new Vector(origin.getBlockX(), origin.getBlockY(),origin.getBlockZ());
		System.out.println("DEBUG, pos reached");
		rotatedBy = globalDirec.rotationFromEast();
	}
	
	
	public Vector toGlobal(Vector relativePt) {
			Vector v_rotated = Direc.rotatedBy(relativePt, rotatedBy);       // rotate according to rotation of clipboard
			return v_rotated.add(origin);
	}
	
	public Vector toGlobal(int x, int y, int z) {
		return toGlobal(new Vector(x,y,z));
	}
	
	
	public CuboidRegion toGlobal(CuboidRegion volume) {
		Vector corner1 = toGlobal(volume.getMinimumPoint());
		Vector corner2 = toGlobal(volume.getMaximumPoint());
		return new CuboidRegion(corner1, corner2);
	}
	
	public Vector toRelative(Vector globalPt) {
		Vector globMinusOrig = globalPt.subtract(origin);
		Vector v_rel = new Vector(globMinusOrig);
		return Direc.rotatedBy(v_rel, -rotatedBy);
	}
	
}

package mcPluginHelpers;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;

import junit.framework.TestCase;

import org.junit.jupiter.api.Test;

class Test_Transform extends TestCase{

	@Test
	void testToGlobalVector() {
		Vector origin = new Vector(1,2,3);
		Transform t = new Transform(origin, Direc.EAST);
		
		// Test getOrigin coverage:
		
		
		Vector global = t.toGlobal(new Vector(4,5,6));
		assertTrue(global.compareTo(new Vector(5,7,9)) == 0);
		
		t = new Transform(origin, Direc.WEST);
		global = t.toGlobal(new Vector(4,5,6));
		assertTrue(global.compareTo(new Vector(1-4,2+5,3-6)) == 0);
		
		t = new Transform(origin, Direc.NORTH);
		global = t.toGlobal(new Vector(4,5,6));
		assertTrue(global.compareTo(new Vector(1+6,2+5,3-4)) == 0);
	}
	
	@Test
	void testToGlobalCuboidRegion() {
		Vector origin = new Vector(1,2,3);
		Transform t = new Transform(origin, Direc.NORTH);
		CuboidRegion r = new CuboidRegion(new Vector(1,1,1), new Vector(3,2,4));
		
		CuboidRegion global = t.toGlobal(r);
		
		Vector expectedMin = new Vector(2,3,0);
		assertTrue(global.getMinimumPoint().compareTo(expectedMin) == 0);
		Vector expectedMax = new Vector(5,4,2);
		assertTrue(global.getMaximumPoint().compareTo(expectedMax) == 0);
	}
	
	@Test
	void testToRelative() {
		Vector origin = new Vector(1,2,3);
		Transform t = new Transform(origin, Direc.WEST);
		Vector global = new Vector(1-4,2+5,3-6);
		Vector relative = t.toRelative(global);
		assertTrue(relative.compareTo(new Vector(4,5,6)) == 0);
	}

}

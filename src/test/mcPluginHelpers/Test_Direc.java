package mcPluginHelpers;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;

class Test_Direc extends TestCase{

	public Test_Direc() {
	}
	
	@Test
	void testRotationFromEast() {
		Direc d = Direc.NORTH;
		int rotation = d.rotationFromEast();
		assertEquals(270, rotation);
		
		d = Direc.EAST;
		rotation = d.rotationFromEast();
		assertEquals(0, rotation);
	}

}

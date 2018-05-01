package mcPluginHelpers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.netty.util.concurrent.SucceededFuture;

class Test_Direc {

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

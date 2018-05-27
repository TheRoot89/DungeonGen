package mcPluginHelpers;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;

class Test_MsgLevel extends TestCase{

	@Test
	void testMsgLevelComparison() {
		MsgLevel lower = MsgLevel.DEBUG;
		MsgLevel higher = MsgLevel.WARNING;
		assertEquals(true, lower.isAtLeastAsSeriousAs(MsgLevel.DEBUG));
		assertEquals(false, lower.isAtLeastAsSeriousAs(MsgLevel.INFO));
		assertEquals(true, higher.isAtLeastAsSeriousAs(lower));
		assertEquals(false, lower.isAtLeastAsSeriousAs(higher));
	}

}

package dunGen;

import com.sk89q.worldedit.Vector;

import dunGen.Helper.Direc;

public class Entry extends Passageway{
	
	//automatically calls Passageway-Contructor upon creation
	public Entry(DunGen parent, String name, Vector targetL, Direc towardsD) {
		super(parent, name, targetL, towardsD);
		loadConfig();
	}
	
	@Override
	public void prePlacementActions() {
		// nothing to do at the moment
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
	}

}

package dunGen;

import com.sk89q.worldedit.Vector;

import dunGen.Helper.Direc;

public class Entry extends Passageway{
	
	/**Special Passageway, passes constructor arguments up and loads config.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
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

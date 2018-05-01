package mineCSweeper;

import org.bukkit.event.Listener;

/**Responsible for updating the game's progress according to the settings.
 * It does set the individual field of the board.
 */
public class MCSGameMechanic implements Listener{

	private MCSBoard board;
	
	public MCSGameMechanic(MCSBoard board, MCSSettings settings) {
		this.board = board;
		
		
	}
	
	
	
}

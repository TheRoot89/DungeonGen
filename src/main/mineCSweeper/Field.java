package mineCSweeper;

import org.bukkit.Material;

import com.sk89q.worldedit.Vector;

public class Field {

	public enum FieldState{
		UNKNOWN,
		CLEARED,
		NUMBERED,//TODO: add materials to each state and get these
		FLAGGED;
	}
	//=======================================================
	
	private FieldState state;
	private boolean hasMine;
	private Material baseMaterial = Material.STONE;
	private Material topMaterial = Material.STONE_PLATE; //TODO: this has to be changed, maybe in the enum?
	public int globX;
	public int globY;
	public int globZ;
	//========================================================
	
	public static Field newCleanField(Vector globalPosition) {
		Field f = new Field(globalPosition);
		f.clean();
		return f;
	}
	
	private Field(Vector globalPosition) {
		this.globX = globalPosition.getBlockX();
		this.globY = globalPosition.getBlockY();
		this.globZ = globalPosition.getBlockZ();
	}
	
	public void clean() {
		state = FieldState.UNKNOWN;
		hasMine = false;
	}
	
	public Material getBaseMat() {
		return baseMaterial;
	}
	
	public Material getTopMat() {
		return topMaterial;
	}
	
	public void setMine(boolean hasMine) {
		this.hasMine = hasMine;
	}
	
	public boolean isMined() {
		return hasMine;
	}
	
	public void flagIfPossible() {
		if (state == FieldState.UNKNOWN)
			state = FieldState.FLAGGED;
	}
	
	public void unFlagIfPossible() {
		if (state == FieldState.FLAGGED)
			state = FieldState.UNKNOWN;
	}
}


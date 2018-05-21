package mineCSweeper;

import org.bukkit.Material;

import com.sk89q.worldedit.Vector;

import mineCSweeper.MCSSettings.Key;

public class Field {

	public enum FieldState{
		UNKNOWN,
		KNOWN,
		FLAGGED;
	}
	//=======================================================
	
	public static Material baseMat;
	public static Material pressurePlateMat;
	public static Material bombFlagMat;
	public static Material[] bombClueMaterials; 
	public static byte[] bombClueMaterialsData; 
	public static boolean materialsAreInitialized = false;
	
	private FieldState state;
	private boolean hasMine;
	public int bombHintNr = 0;
	
	public int globX;
	public int globY;
	public int globZ;
	//========================================================
	
	public static void updateMaterialsfromSettings(MCSSettings settings) throws MCSException {
		baseMat = settings.getMaterialSetting(Key.BASEMATERIAL);
		pressurePlateMat = settings.getMaterialSetting(Key.PRESSUREPLATEMATERIAL);
		bombFlagMat = settings.getMaterialSetting(Key.BOMBFLAGMATERIAL);
		
		String[] bombClueMaterialsStrings = settings.getMaterialListSettingString(Key.BOMBCLUEMATERIALS);
		decodeBombClueMaterials(bombClueMaterialsStrings);
		// The size of the bombClueMaterials has to be 9 ! (zero to eight bombs close):
		if (bombClueMaterialsStrings.length != 9)
			throw new MCSException("List of loaded clue materials does not have length 9!");
		materialsAreInitialized = true;
	}
	
	
	private static void decodeBombClueMaterials(String[] bombClueMaterialsStrings) throws MCSException {
		bombClueMaterials = new Material[bombClueMaterialsStrings.length];
		bombClueMaterialsData = new byte[bombClueMaterialsStrings.length];
		for (int i=0; i<bombClueMaterialsStrings.length; i++) {
			String[] nameAndDataIfPresent = bombClueMaterialsStrings[i].split("/");
			String matName = nameAndDataIfPresent[0];
			bombClueMaterials[i] = Material.getMaterial(matName);
			if (bombClueMaterials[i] == null)
				throw new MCSException("Material setting invalid: " + matName);
			if (nameAndDataIfPresent.length == 2) {
				bombClueMaterialsData[i] = Byte.parseByte(nameAndDataIfPresent[1]);
			}else {
				bombClueMaterialsData[i] = 0;
			}
		}
	}


	public static Field newCleanField(Vector globalPosition){
		assert(materialsAreInitialized);
		
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

	
	public void setMine(boolean hasMine) {
		this.hasMine = hasMine;
	}
	
	public boolean isMined() {
		return hasMine;
	}
	
	public boolean isFlagged() {
		return (state == FieldState.FLAGGED);
	}
	
	public void flagIfPossible() {
		if (state == FieldState.UNKNOWN)
			state = FieldState.FLAGGED;
	}
	
	public void unFlagIfPossible() {
		if (state == FieldState.FLAGGED)
			state = FieldState.UNKNOWN;
	}


	public Material getCurrentBaseMat() {
		switch (state) {
		case FLAGGED:
		case UNKNOWN:
			return baseMat;
		case KNOWN:
			return bombClueMaterials[bombHintNr];
		default:
			return null;
		}
	}


	public Material getCurrentTopMat() {
		switch (state) {
		case KNOWN:
			return Material.AIR;
		case FLAGGED:
			return bombFlagMat;
		case UNKNOWN:
			return pressurePlateMat;
		default:
			return null;
		}
	}


	public boolean isUnknown() {
		return (state == FieldState.UNKNOWN);
	}


	public void reveal() {
		state = FieldState.KNOWN;
	}
	
}


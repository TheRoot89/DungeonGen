package mineCSweeper;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.sk89q.worldedit.Vector;

import mineCSweeper.MCSSettings.Key;

public class Field {

	public enum FieldState{
		UNKNOWN,
		KNOWN,
		FLAGGED;
	}
	//=======================================================
	private static World world;
	
	public static Material baseMat;
	public static Material pressurePlateMat;
	public static Material bombFlagMat;
	public static Material[] bombClueMaterials; 
	public static byte[] bombClueMaterialsData; 
	public static boolean configLoaded = false;
	private static boolean safeBombFlags;
	
	private FieldState state;
	private boolean hasMine;
	private boolean bombFreeAsIsStartRegion = false;
	public int bombHintNr = 0;
	
	public int globX;
	public int globY;
	public int globZ;
	private Block baseBlock;
	private Block topBlock;
	private Block mineBlock;
	//========================================================
	
	public static void loadStaticConfigForAllFields(MCSSettings settings, World w) throws MCSException {
		world = w;
		safeBombFlags = settings.getBooleanSetting(Key.SAFEBOMBFLAGS);
		
		baseMat = settings.getMaterialSetting(Key.BASEMATERIAL);
		pressurePlateMat = settings.getMaterialSetting(Key.PRESSUREPLATEMATERIAL);
		bombFlagMat = settings.getMaterialSetting(Key.BOMBFLAGMATERIAL);
		
		String[] bombClueMaterialsStrings = settings.getMaterialListSettingString(Key.BOMBCLUEMATERIALS);
		decodeBombClueMaterials(bombClueMaterialsStrings);
		// The size of the bombClueMaterials has to be 9 ! (zero to eight bombs close):
		if (bombClueMaterialsStrings.length != 9)
			throw new MCSException("List of loaded clue materials does not have length 9!");
		configLoaded = true;
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
		assert(configLoaded);
		
		Field f = new Field(globalPosition);
		f.clean();
		return f;
	}
	
	private Field(Vector globalPosition) {
		globX = globalPosition.getBlockX();
		globY = globalPosition.getBlockY();
		globZ = globalPosition.getBlockZ();
		baseBlock = world.getBlockAt(globX, globY, globZ);
		topBlock = world.getBlockAt(globX, globY+1, globZ);
		mineBlock = world.getBlockAt(globX, globY-1, globZ);
	}
	
	public void clean() {
		state = FieldState.UNKNOWN;
		hasMine = false;
	}

	
	@SuppressWarnings("deprecation")
	public void update() {
		baseBlock.setType(getCurrentBaseMat());
		//TODO this is not working any more: baseBlock.setBlockData(getCurrentBaseMatData());
		topBlock.setType(getCurrentTopMat());
		if (hasMine && !(isFlagged() && safeBombFlags))
			mineBlock.setType(Material.TNT);
		else
			mineBlock.setType(getCurrentBaseMat());
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


	public byte getCurrentBaseMatData() {
		switch (state) {
		case FLAGGED:
		case UNKNOWN:
			return 0;
		case KNOWN:
			return bombClueMaterialsData[bombHintNr];
		default:
			return 0;
		}
	}
	
	public void delete() {
		topBlock.setType(Material.AIR);
		baseBlock.setType(Material.AIR);
		mineBlock.setType(Material.AIR);
	}
	
	public boolean hasAdjacentBomb() {
		return (bombHintNr > 0);
	}
	
	public void setStartRegion(boolean isBombFree) {
		bombFreeAsIsStartRegion = isBombFree;
	}
	
	public boolean bombIsAllowed() {
		return !bombFreeAsIsStartRegion;
	}
	
}


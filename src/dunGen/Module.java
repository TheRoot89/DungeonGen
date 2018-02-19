package dunGen;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.DataException;

import dunGen.DunGen.State;
import dunGen.Helper.Direc;

/**A base class representing a physical model in minecraft. It is loaded from a schematic file and placed according to
 * the information given there. 
 */
@SuppressWarnings("deprecation")
public abstract class Module implements Listener {

	
	/**Wraps the info on entry and exit of this module, e.g. direction and dimensions. */
	class Connector{
		public Direc afterPasteDirec;		// direction after placement (init direc is rotated)
											// typ. this will be entryLoc, but it is possible be set differently after the constructor, before placement
		public Vector doorLoc = null;		// where the actual door is located
		// for entries: needed direc of the module after placement
												// for exits: the direc the exit faces after placement
		public Material doorMaterial;		// Material of the door being generated or falls down
		public int height;					// free space upwards
		public Direc initDirec; 			// initial direction this connector is facing (entries must be EAST!)
		public Vector placementLoc = null;	// (x,y,z) = (forward, up, right). rel. pos within this module, which will correspond to the given origin, once placed
		public Vector redstonePos;			// Pos where redstone needs to spawn to close the door or disappear for opening
		public int width;					// free space to the right
	}
	
	
	/** Describes the types a module may be of. */
	public enum ModuleType {
		ENTRY,
		PASSAGEWAY,
		PLATFORMROOM,
		BATTLEROOM
	}
	
	
	
	// ##################### Member variables #############################
	
	// set in constructor:
	public    	FileConfiguration 	conf;			// file config the discr. of this module is saved in
	protected 	String 				name;			// short name, also name of the yml file
	protected 	Vector 				origin;			// will coincide will the placementLoc of the entry Connector
	protected 	DunGen 				parent;			// the DunGen plugin (pointer)
	// Loaded from config:
	protected 	String 				description;	// in-game name of this module
	protected 	Connector 			entry;
	protected 	Connector 			exit;
	protected 	String 				fileName;		// name of the schematic
	public 	  	ModuleType 			type;
	// set during placement:
	public    	CuboidRegion 		modVolume;		// the volume occupied by this module
	protected 	int 				turnedBy;		// e.g. 90 degrees from EAST to SOUTH
	// Working variables:
	public    	CuboidClipboard 	cc     = null;	// the clipboard with it's own coordinates before placement
	private   	boolean 			placed = false;
	
	
	
	// ########################### Member functions ########################
	
	/**Static method to get a modules config alone, returns null if failed.
	 * The loading is tested during the initial yml test and should therefore work during DunGen runtime.
	 * @param parent 	The parent plugin
	 * @param name 		The modules name for witch the config should be loaded (file 'name'.yml)
	 * @return			The config object. Returns null if errors occured and sets plugin state to ERROR.
	 */
	public static YamlConfiguration getConfig(DunGen parent, String name) {
		File confFile = new File(parent.getDataFolder(),name+".yml");
		if (!confFile.exists()) {
			parent.setStateAndNotify(State.ERROR, "Config file for module " + name + " could not be found!");
			return null;
		}
		
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.load(confFile);
		}catch (IOException | InvalidConfigurationException e) {
			parent.setStateAndNotify(State.ERROR, "Loading of config file for module " + name + " failed:");
			e.printStackTrace();
			return null;
		}
		// everything ok, if code reached here. Enable for debug if needed:
		//parent.getLogger().info("YML file for module " + name + " loaded.");
		return conf;
	}
	
	
	/**Static method to get a moduel type before constructor has been executed.
	 * Config loading is tested during initial yml test and is not catched here.
	 * @param parent	The parent plugin, needed for config loading.
	 * @param name		The name of the module.
	 * @return			The ModuleType enum with the value for this name.
	 */
	public static ModuleType getType(DunGen parent, String name) {
		return ModuleType.valueOf(getConfig(parent, name).getString("type").toUpperCase()); // get valid Enum from string
	}
	
	
	/**Contructs the new module object and initializes it by reading its 'name'.yml file.
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public Module(DunGen parent, String name, Vector targetL, Direc towardsD) {
		this.name = name;
		this.entry = new Connector();
		this.entry.initDirec = Direc.EAST; //fixed at the moment!
		this.exit = new Connector();
		this.parent = parent;
		this.origin = new Vector(targetL);
		this.entry.afterPasteDirec = towardsD;
		this.turnedBy = (towardsD.degree()-entry.initDirec.degree()+360)%360;
		
		// load config file, loading tested during initial yml test so should work:
		conf = getConfig(parent, name);
	}
	
	
	/**Fills the bounding box occupied by this module with air.
	 * To be implemented: actual restoring of the destroyed landscape. This would need one clipboard per module.
	 */
	public void delete() {
		Helper.fillVolume(parent.world, modVolume.getPos1(), modVolume.getPos2(), Material.AIR);
	}
	
	
	/**Fills the volum between the relative coords v1 and v2 with Material m
	 * @param v1	One volume corner, relative.
	 * @param v2	The other corner, relative.
	 * @param m		The block type to use.
	 */
	public void fillVolumeRelative(Vector v1, Vector v2, Material m) {
		v1 = toGlobal(v1);
		v2 = toGlobal(v2);
		for (int x = Math.min(v1.getBlockX(), v2.getBlockX()); x <= Math.max(v1.getBlockX(), v2.getBlockX()); x++)
		for (int y = Math.min(v1.getBlockY(), v2.getBlockY()); y <= Math.max(v1.getBlockY(), v2.getBlockY()); y++)
		for (int z = Math.min(v1.getBlockZ(), v2.getBlockZ()); z <= Math.max(v1.getBlockZ(), v2.getBlockZ()); z++)
			parent.world.getBlockAt(x, y, z).setType(m);
	}
	
	
	/** Gives the the next block after the exit of this module.
	 * @return the global position vector, where the next modules global origin should be placed.
	 */
	public Vector getNextEntryPos() {
		return toGlobal(exit.placementLoc).add(exit.afterPasteDirec.toUnityVec());
	}
	
	
	/**@return the plugin this module belongs to*/
	public DunGen getPlugin() {
		return parent;
	}

	
	/** Loads all basic properties common for every module.
	 * The initial yml check makes sure all properties are already in there.
	 * This superclass function has to be called by every subclass upon loading its own config!
	 */
	public void loadConfig() {
		description			= conf.getString("description");
		fileName 			= conf.getString("schematic") + ".schematic";
		type 				= ModuleType.valueOf(getConfig(parent, name).getString("type").toUpperCase()); // valid Enum from string
		entry.placementLoc 	= BukkitUtil.toVector(conf.getVector("entry.placementLoc"));
		entry.doorLoc 		= BukkitUtil.toVector(conf.getVector("entry.doorLoc"));
		entry.width  		= conf.getInt("entry.width");
		entry.height		= conf.getInt("entry.height");
		exit.placementLoc	= BukkitUtil.toVector(conf.getVector("exit.placementLoc"));
		exit.doorLoc 		= BukkitUtil.toVector(conf.getVector("exit.doorLoc"));
		exit.width  		= conf.getInt("exit.width");
		exit.height			= conf.getInt("exit.height");
		exit.initDirec 		= Direc.fromDeg(conf.getInt("exit.initDirec"));
	}
	
	
	/**Places the module by loading it from its associated .schematic file.
	 * Does shifting and rotation according to the values given during object construction and conf loading.
	 * Also invokes the prePlacementActions() and postPlacementActions() accordingly.
	 * The used CuboidClipboard is saved for later use.
	 */
	public void place() {
		
		/*
		// Use new WorldEdit API, FAWE gives out warnings: :/
		Clipboard clipboard;
		ClipboardHolder holder;
		EditSession es;
		try {
		es = new EditSession(new BukkitWorld(parent.world), 999999999);
		File schemFile = new File(parent.dir,fileName);
		FileInputStream fis = new FileInputStream(schemFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(bis));
		ClipboardReader reader = new SchematicReader(nbtStream);
		WorldData worldData = LegacyWorldData.getInstance();
		clipboard = reader.read(worldData);
		holder = new ClipboardHolder(clipboard, worldData);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
		// compensate the offset for the entry lower left:
		clipboard.setOrigin(new Vector(-entryLoc.getBlockX(),-entryLoc.getBlockY(),-entryLoc.getBlockZ()));
    	Vector size = clipboard.getDimensions(); // get size before module is turned
    	
    	// rotate:
    	AffineTransform  tr = new AffineTransform();
    	tr = tr.rotateY(turnedBy);
    	holder.setTransform(tr);
    	exitDirec = Direc.fromDeg(initExitDirec.degree() + turnedBy);
    	
    	// do stuff before placement
    	prePlacementActions();
		
    	// place:
    	try {
    		Region region = clipboard.getRegion();
            Vector to = origin.add(clipboard.getOrigin());
            Operation operation = holder
                    .createPaste(es, es.getWorld().getWorldData())
                    .to(to)
                    .ignoreAirBlocks(false) //set here if air does not write
                    .build();
            Operations.completeLegacy(operation);
        } catch (MaxChangedBlocksException ex) {
            ex.printStackTrace();
        }
    	
    	// save volume:
    	modVolume = new CuboidRegion(toGlobal(new Vector(0,0,0)), toGlobal(size.subtract(new Vector(1,1,1)))); // -1 for each dim as (0,0,0) already counts
    	
    	// do stuff after placement
    	postPlacementActions();
    	*/
    	
    	
		// Laden & vorbereiten der geladenen Region
		EditSession es;
    	try {
    	// load to clipboard:
    	File schemFile = new File(parent.dir,fileName);
    	es = new EditSession(new BukkitWorld(parent.world), 999999999);
    	es.enableQueue();
    	SchematicFormat schematic = SchematicFormat.getFormat(schemFile);
    	cc = schematic.load(schemFile);
        } catch (DataException | IOException ex) {
            ex.printStackTrace();
            return;
        }
    	
		
    	// compensate the offset for the placement pos. (typ. entry placement loc = lower left air block of entry 'hole'):
    	cc.setOffset(new Vector(-entry.placementLoc.getBlockX(),-entry.placementLoc.getBlockY(),-entry.placementLoc.getBlockZ()));
    	// --> rotate the module and determine exit direction:
    	Vector size = cc.getSize(); // get size before module is turned
    	cc.rotate2D(turnedBy);
    	exit.afterPasteDirec = Direc.fromDeg(exit.initDirec.degree() + turnedBy);
    	
    	// do stuff before placement
    	prePlacementActions();
    	
    	// place:
    	try {
    	cc.paste(es, origin, false);
    	es.flushQueue();
        } catch (MaxChangedBlocksException ex) {
            ex.printStackTrace();
        }
    	placed = true;
    	
    	// save volume:
    	modVolume = new CuboidRegion(toGlobal(new Vector(0,0,0)), toGlobal(size.subtract(new Vector(1,1,1)))); // -1 for each dim as (0,0,0) already counts
    	
    	// do stuff after placement
    	postPlacementActions();
	}
	
	
	/**Places the given material where the plan is true, starting from the given relative origin within this module.
	 * The plan and origin are rotated beforehand, to avoid conversion toGlobal for each block!
	 * @param origin	A relative coordinate specifying the minimum Point of the given build plan
	 * @param plan		A grid of boolean values. Material is set if true at that position.
	 * @param m			The Material to be placed.
	 * @param height	How high the plan should be duplicated. (stacked up)
	 */
	public void placeBuildPlan2D(Vector origin, boolean[][] plan, Material m, int height) {
		plan = Helper.rotateBoolMatrixClockw(plan, turnedBy); //clockwise turning as is defined in mc for sky directions
		// The origin has to be at another corner of the matrix now:
		origin = toGlobal(origin);
		switch (turnedBy) {
		case 0:
			break;
		case 90:
			origin = origin.add(-(plan.length-1), 0, 0);
			break;
		case 180:
			origin = origin.add(-(plan.length-1),0,-(plan[0].length-1));
			break;
		case 270:
			origin = origin.add(0, 0, -(plan[0].length-1));
			break;
		default:
			parent.setStateAndNotify(State.ERROR, "Module: turnedBy is out of range: " + turnedBy + ". Plan is not placed.");
			return;
		}
		
		// now we have a building plan matrix looking east, with the origin in the lower left. Easy:
		int x = origin.getBlockX();
		for (int row=0; row < plan.length; row++, x++) {
			int z = origin.getBlockZ();
			for (int col=0; col< plan[0].length; col++, z++)
				for (int y = origin.getBlockY(); y < (origin.getBlockY()+height); y++)
					if (plan[row][col]) parent.world.getBlockAt(x,y,z).setType(m);	
		}
			
	}
	
	
	/**Stuff done automatically and immediatelly after placement is to be specified here. */
	public abstract void postPlacementActions();
	
	
	/**Stuff done while not yet placed, but obejct and clipboad exist. */
	public abstract void prePlacementActions();
	
	
	/**Activation of listeners or scheduled tasks. */
	public abstract void register();


	/** Converts the module's relative coordinates to global points (possible only after placement!)
	 * @param relativePt A relative position, measured from the module origin, facing EAST.
	 * @return 			 A global position, according to where and in which rotation this module was placed, null if called before placement.
	 */
	public Vector toGlobal(Vector relativePt) {
		if (placed) {
			Vector relPlusOff = relativePt.subtract(entry.placementLoc);
			Vector v_glob = Direc.rotatedBy(relPlusOff, turnedBy);       // rotate according to rotation of clipboard
			return v_glob.add(origin);
		}else {
			parent.setStateAndNotify(State.ERROR, "Module::toGlobal was called before placement!");
			return null; // will crash the plugin, above error message for debug
		}
	}
	
	
	/** Converts global world coordinates to the relative coordinate frame of this module, only valid after placement!
	 * @param globalPt	A global world position.
	 * @return			A relative position, measured from the module origin, facing EAST. Calculated according to placement and rotation.
	 */
	public Vector toRelative(Vector globalPt) {
		if (placed) {
			Vector globMinusOrig = globalPt.subtract(origin);
			// rotate back according to rotation of clipboard:
			Vector v_rel = new Vector(globMinusOrig);
			Direc.rotatedBy(v_rel, -turnedBy);
			return v_rel.add(entry.placementLoc);	
		}else {
			parent.setStateAndNotify(State.ERROR, "Module::toRelative was called before placement!");
			return null; // will crash the plugin, above error message for debug
		}
	}
	
	
	/**Deactivation of listeners and scheduled tasks */
	public abstract void unregister();
}

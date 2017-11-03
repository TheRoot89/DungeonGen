package dungeonGen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.SchematicReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;

public abstract class Module implements Listener {
	
	
	//////////////////////// Properties ///////////////////////////
	// Loaded from config:
	public ModuleType type;
	protected Connector entry;
	protected Connector exit;
	
	// set in constructor:
	protected String name;				// (file) name of this module
	protected String description;		// in-game name of this module
	protected String fileName;			// name of the schematic
	protected FileConfiguration conf;	// file config the discr. of this module is saved in
	protected DungeonGen parent;		// the DungeonGen plugin (pointer)
	protected int turnedBy;				// e.g. 90° from EAST to SOUTH
	protected Vector origin;			// where the entryLoc shall be placed in global coord
	
	// set during placement:
	public CuboidClipboard cc=null;		// the clipboard with it's own coordinates before placement
	public CuboidRegion modVolume;		// the volume occupied by this module
	
	
	/////////////////// Inner class as struct: ////////////////////////
	class Connector{
		public Vector placementLoc = null;	// (x,y,z) = (forward, up, right). rel. pos within this module, which will correspond to the given origin, once placed
												// typ. this will be entryLoc, but it is possible be set differently after the constructor, before placement
		public Vector doorLoc = null;		// where the actual door is located
		public int width;					// free space to the right
		public int height;					// free space upwards
		public Direc initDirec; 			// initial direction this connector is facing (entries must be EAST!)
		public Direc afterPasteDirec;		// direction after placement (init direc is rotated)
												// for entries: needed direc of the module after placement
												// for exits: the direc the exit faces after placement
	}
	
	
	//////////////////////// Methods ////////////////////////////////////
	
	// These methods are abstract and have to be implemented (often empty) by subclasses:
	public abstract void prePlacementActions();		// stuff done while not yet placed, but obejct and clipboad exist
	public abstract void postPlacementActions();	// stuff done automatically and immediatelly after placement
	public abstract void register();				// activation of (victory)condition surveillance by a module
	public abstract void unregister();				// deactivation of above
	
	
	/**
	 * Contructs the new module object and initializes it by reading its yml file.
	 * 
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public Module(DungeonGen parent, String name, Vector targetL, Direc towardsD) {
		this.name = name;
		this.entry = new Connector();
		this.entry.initDirec = Direc.EAST; //fixed at the moment!
		this.exit = new Connector();
		//TODO: add 'name' here and make name property to description, so debug has the name info!
		this.parent = parent;
		this.origin = new Vector(targetL);
		this.entry.afterPasteDirec = towardsD;
		this.turnedBy = towardsD.degree()-entry.initDirec.degree();
		
		// check and load config file:
		File confFile = new File(parent.getDataFolder(),name+".yml");
		if (!confFile.exists()) {
			parent.getLogger().severe("YML file for module " + name + " could not be found!");
			return;
		}

		conf = new YamlConfiguration();
		try {
			conf.load(confFile);
		}catch (IOException | InvalidConfigurationException e) {
			parent.getLogger().severe("Loading of config file for module " + name + " failed!");
			e.printStackTrace();
			return;
		}
		parent.getLogger().info("YML file for module " + name + " loaded.");
	}
	
	// static method to get a moduel type before constructor has been executed
	public static ModuleType getType(DungeonGen parent, String name) {
		File confFile = new File(parent.getDataFolder(),name+".yml");
		if (!confFile.exists()) {
			parent.getLogger().severe("Config file for module " + name + " could not be found!");
			return null;
		}
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.load(confFile);
		}catch (IOException | InvalidConfigurationException e) {
			parent.getLogger().severe("Loading of config file for module " + name + " failed!");
			e.printStackTrace();
			return null;
		}
		return ModuleType.values()[conf.getInt("type")]; // valid Enum from int
	}
	

	/** Loads all basic properties common for every module.
	 * This superclass function has to be called by every subclass upon loading its own config!
	 */
	public void loadConfig() {
		// basic values for placement:
		if (conf.contains("description")      	&&
			conf.contains("schematic") 			&&
			conf.contains("type")      			&&
			conf.contains("entry.placementLoc")	&&
			conf.contains("entry.doorLoc")      &&
			conf.contains("entry.width")		&&
			conf.contains("entry.height")		&&
			conf.contains("exit.placementLoc") 	&&
			conf.contains("exit.doorLoc") 		&&
			conf.contains("exit.width") 		&&
			conf.contains("exit.height")	 	&&
			conf.contains("exit.initDirec") ) {
			
			// actual loading:
			name 				= conf.getString("description");
			fileName 			= conf.getString("schematic") + ".schematic";
			type 				= ModuleType.values()[conf.getInt("type")]; // valid Enum from int
			entry.placementLoc 	= BukkitUtil.toVector(conf.getVector("entry.placementLoc"));
			entry.doorLoc 		= BukkitUtil.toVector(conf.getVector("entry.doorLoc"));
			entry.width  		= conf.getInt("entry.width");
			entry.height		= conf.getInt("entry.height");
			exit.placementLoc	= BukkitUtil.toVector(conf.getVector("exit.placementLoc"));
			exit.doorLoc 		= BukkitUtil.toVector(conf.getVector("exit.doorLoc"));
			exit.width  		= conf.getInt("exit.width");
			exit.height			= conf.getInt("exit.height");
			exit.initDirec 		= Direc.fromDeg(conf.getInt("exit.initDirec"));
		}else {
			parent.getLogger().severe("Unable to load config fields for " + name + ". Something is wrong with:");
			// Debug output, to see witch property in the file needs to be fixed:
			parent.getLogger().info("description: " + conf.contains("description") );
			parent.getLogger().info("schematic: " + conf.contains("schematic") );
			parent.getLogger().info("entry.placementLoc: " + conf.contains("entry.placementLoc") );
			parent.getLogger().info("entry.doorLoc: " + conf.contains("entry.doorLoc") );
			parent.getLogger().info("entry.width: " + conf.contains("entry.width") );
			parent.getLogger().info("entry.height: " + conf.contains("entry.height") );
			parent.getLogger().info("exit.placementLoc: " + conf.contains("exit.placementLoc") );
			parent.getLogger().info("exit.doorLoc: " + conf.contains("exit.doorLoc") );
			parent.getLogger().info("exit.width: " + conf.contains("exit.width") );
			parent.getLogger().info("exit.height: " + conf.contains("exit.height") );
			parent.getLogger().info("exit.initDirec: " + conf.contains("exit.initDirec") );
		}
	}
	
	
	@SuppressWarnings("deprecation")
	public void place() {
		
		/*
		// new and lag free(?):
		// TODO use new WorldEdit API
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
    	
    	// save volume:
    	modVolume = new CuboidRegion(toGlobal(new Vector(0,0,0)), toGlobal(size.subtract(new Vector(1,1,1)))); // -1 for each dim as (0,0,0) already counts
    	
    	// do stuff after placement
    	postPlacementActions();
	}
	
	
	/**
	 * Fills the bounding box occupied by this module with air.
	 * Idea: actual restoring of the destroyed landscape
	 */
	public void delete() {
		Helper.fillVolume(parent.world, modVolume.getPos1(), modVolume.getPos2(), Material.AIR);
	}
	
	
	/** Converts the module's relative coordinates to global points (only after placement!)
	 * @param relativePt A relative position, measured from the module origin, facing EAST.
	 * @return A global position, according to where and in which rotation this module was placed.
	 */
	public Vector toGlobal(Vector relativePt) {
		Vector relPlusOff = relativePt.subtract(entry.placementLoc);
		// rotate according to rotation of clipboard:
		Vector v_glob = Direc.rotatedBy(relPlusOff, turnedBy);
		return v_glob.add(origin);
	}

	
	public Vector toRelative(Vector globalPt) {
		Vector globMinusOrig = globalPt.subtract(origin);
		// rotate back according to rotation of clipboard:
		Vector v_rel = new Vector(globMinusOrig);
		Direc.rotatedBy(v_rel, -turnedBy);
		return v_rel.add(entry.placementLoc);	
	}
	
	// Return the the next block after the exit (where the next module global origin should be placed)
	public Vector getNextEntryPos() {
		return toGlobal(exit.placementLoc).add(exit.afterPasteDirec.toUnityVec());
	}
}

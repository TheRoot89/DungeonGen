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
	
	//Fix:
	private final Direc initEntryDirec = Direc.EAST;	// aktuell zwingende(!) Richtung bei Speicherung des Moduls
	
	//Loaded from config:
	public ModuleType type;
	protected Vector placementLoc;	// rel. pos within this module, which will correspond to the given origin, once placed
									// typ. this will be entryLoc, but it is possible be set differently after the constructor, before placement
	//entry:
	protected Vector entryLoc;		// (x,y,z) = (forward, up, right).
	protected int entryWidth;		// entry free space to the right
	protected int entryHeight;  	// entry free space upwards
	//exit:
	protected Direc initExitDirec;
	protected Vector exitLoc;
	protected int exitWidth;
	protected int exitHeight;
	
	// set in constructor:
	protected String name;				// name of this module
	protected String fileName;			// name of the schematic
	protected FileConfiguration conf;	// file config the discr. of this module is saved in
	protected Direc entryDirec;		// the Direction this module should face after placement
	protected DungeonGen parent;	// the DungeonGen plugin (pointer)
	protected int turnedBy;			// e.g. 90° from EAST to SOUTH
	protected Vector origin;		// where the entryLoc shall be placed in global coord
	
	// set during placement:
	public Direc exitDirec 	 =null;	// the Direction the exit will face after placement
	public CuboidClipboard cc=null;	// the clipboard with it's own coordinates before placement
	public CuboidRegion modVolume;	// the volume occupied by this module
	
	// These methods are abstract and have to be implemented (often empty) by subclasses:
	public abstract void prePlacementActions();		// stuff done while not yet placed, but obejct and clipboad exist
	public abstract void postPlacementActions();	// stuff done automatically and immediatelly after placement
	public abstract void register();				// activation of (victory)condition surveillance by a module
	public abstract void unregister();				// deactivation of above
	
	
	/**
	 * Contructs the new module object, initializes and places it.
	 * In a future release, the entry may be selected randomly, or the name might be read from a setup file. Here it is hardcoded, as well as its attributes.
	 * Also the geometric relations may be in settings-files for each module
	 * 
	 * @param parent	The parent Plugin for member access
	 * @param name 		The name of this module, as well as .schematic and .yml files
	 * @param targetL	The location of the entry as global vector (lower left free(air or door) block)
	 * @param towardsD	Direction the dungeon is facing (inwards)
	 */
	public Module(DungeonGen parent, String name, Vector targetL, Direc towardsD) {
		this.parent = parent;
		this.fileName = name + ".schematic";
		this.origin = new Vector(targetL);
		this.entryDirec = towardsD;
		this.turnedBy = entryDirec.degree()-initEntryDirec.degree();
		
		// check and load config file:
		File confFile = new File(parent.getDataFolder(),name+".yml");
		if (!confFile.exists()) {
			parent.getLogger().severe("Config file for module " + name + " could not be found!");
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
	}
	
	// static method to get type before constructor is 
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
	
	// loads all basic properties common for every module
	public void loadConfig() {
		name			= conf.getString("name");
		type			= ModuleType.values()[conf.getInt("type")]; // valid Enum from int
		entryLoc  		= BukkitUtil.toVector(conf.getVector("entryLoc"));
		entryWidth  	= conf.getInt("entryWidth");
		entryHeight 	= conf.getInt("entryHeight");
		initExitDirec 	= Direc.fromDeg(conf.getInt("initExitDirec"));
		exitLoc   		= BukkitUtil.toVector(conf.getVector("exitLoc"));
		exitWidth    	= conf.getInt("exitWidth");
		exitHeight    	= conf.getInt("exitHeight");
		if (entryLoc == null || initExitDirec == null || exitLoc == null) {
			parent.getLogger().severe("Unable to load config fields for " + name);
		}
		// set initial placement loc to entryLoc (connecting passageWays via their doors):
		placementLoc = new Vector(entryLoc);
	}
	
	
	@SuppressWarnings("deprecation")
	public void place() {
		
		/*
		// new and lag free(?):
		// TODO implement lag-free loading
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
    	
		
    	// compensate the offset for the placement pos. (typ. entry lower left):
    	cc.setOffset(new Vector(-placementLoc.getBlockX(),-placementLoc.getBlockY(),-placementLoc.getBlockZ()));
    	// --> rotate the module and determine exit direction:
    	Vector size = cc.getSize(); // get size before module is turned
    	cc.rotate2D(turnedBy);
    	exitDirec = Direc.fromDeg(initExitDirec.degree() + turnedBy);
    	
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
	
	
	public void delete() {
		Helper.fillVolume(parent.world, modVolume.getPos1(), modVolume.getPos2(), Material.AIR);
	}
	
	
	public Vector toGlobal(Vector relativePt) {
		Vector relPlusOff = relativePt.subtract(placementLoc);
		//System.out.println(relPlusOff.toString());
		// rotate according to rotation of clipboard:
		Vector v_glob = Direc.rotatedBy(relPlusOff, turnedBy);
		return v_glob.add(origin);
	}

	public Vector toRelative(Vector globalPt) {
		Vector globMinusOrig = globalPt.subtract(origin);
		// rotate back according to rotation of clipboard:
		Vector v_rel = new Vector(globMinusOrig);
		Direc.rotatedBy(v_rel, -turnedBy);
		return v_rel.add(placementLoc);
				
				
	}
}

package dunGen.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//########################## Not used yet! #############################

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import dunGen.DunGen;
import dunGen.Module;
import dunGen.Passageway;
import dunGen.Module.ModuleType;


/** Able to handle commands to the plugin in as wrapped way. */
public class ConfigChecker implements CommandExecutor {
	
	/**Exception thrown if something during yml config loading failed. Contains an error message.*/
	class ConfigException extends Exception{
		private static final long serialVersionUID = 1L; // serializeable, suppresses warning
		public ConfigException(String message) {super(message);} // allows a message to be used, getMessage() gets it back.
	}
	
	private final DunGen plugin;
	
	public ConfigChecker(DunGen plugin) {
		this.plugin = plugin;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("checkConfig")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(plugin.getName() + ": Starting YAML file check...");
			} else {
				Player player = (Player) sender;
				player.sendMessage(plugin.getName() + ": Starting YAML file check...");
			}
			
			return true;
		}
		
		return false;
	}
	
	 public void checkModuleYmlFiles() throws ConfigException {
	    	YamlConfiguration curConf;
	    	String moduleName = "";
	    	String errTxt = "";		// empty errTxt serves as Flag everything went well.
	    	String key;				// contains temporary key names for convenience
	    	
	    	// ################# keys contained in "Module": ##################
	    	List<String> inspectionList = new ArrayList<String>();
	    	inspectionList.addAll(plugin.entryModules);
	    	inspectionList.addAll(plugin.passagewayModules);
	    	inspectionList.addAll(plugin.roomModules);
			for (String curName : inspectionList) {
				moduleName = curName; // cannot iterate using 'curName' directly, saves the name for the exception
				curConf = Module.getConfig(plugin, moduleName);
				if (curConf == null) {errTxt += "Config file missing: " + moduleName + ".yml\n"; continue;} // abort this file
				
				// Go through all the values, this consists of:
				// 1. Check key existence
				// 2. Check key read is not null
				// 3. Check key value range, if applicable
				// Each pass allows for the next step. Errors are added to a summed error text.
				// Enums can only have a certain list of values, others can be freely set.
				
				key = "description";
				if (!curConf.contains(key)) 				errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
				else if (curConf.getString(key) == null)	errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
				
				key = "schematic";
				if (!curConf.contains(key)) 				errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
				else if (curConf.getString(key) == null)	errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
				else {
					File f = new File(plugin.dir,curConf.getString(key)+".schematic");
					if (!f.exists()) 						errTxt += "["+moduleName+".yml] File does not exist: "+key + ".schematic\n";
				};
				
				key = "type";
				if (!curConf.contains(key)) 				errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
				else if (curConf.getString(key) == null)	errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
				else if (ModuleType.valueOf(curConf.getString(key).toUpperCase()) == null) errTxt += "["+ moduleName +".yml] Key value invalid: "+key + "\n";
				
				
				for(String parentKey: new String[] {"entry" , "exit"}) {
					if (!curConf.contains(parentKey)) 			errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
					else {
						key = parentKey+".placementLoc";
						if (!curConf.contains(key)) 			errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
						else if (curConf.getVector(key) == null)errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
						
						key = parentKey+".doorLoc";
						if (!curConf.contains(key)) 			errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
						else if (curConf.getVector(key) == null)errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
						
						key = parentKey+".width";
						if (!curConf.contains(key)) 			errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
						else if (curConf.getInt(key) == 0)		errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
						
						key = parentKey+".height";
						if (!curConf.contains(key)) 			errTxt += "["+moduleName+".yml] Key missing: "+key + "\n";
						else if (curConf.getInt(key) == 0)		errTxt += "["+moduleName+".yml] Key invalid: "+key + "\n";
					}
				}
			} // for loop over modules
			
			// ########### keys contained in "Passageway" or "Entry": #############
			// (no differences at the moment, add new block if things are added to Entries alone)
			inspectionList.clear();
			inspectionList.addAll(plugin.entryModules);
			inspectionList.addAll(plugin.passagewayModules);
			for (String curName : inspectionList) {
				moduleName = curName; // cannot iterate using 'name' directly
				curConf = Module.getConfig(this, moduleName);
				if (curConf == null) throw new ConfigException("Config file missing: " + moduleName + ".yml");
				
				if (!curConf.contains("entry.type")) 			{key = "entry.type"; break;}
				if (Passageway.DoorType.values()[curConf.getInt("entry.type")] == Passageway.DoorType.PISTON) {
					if (!curConf.contains("entry.redstoneLoc"))	{key = "entry.redstoneLoc"; break;}
				}else { // APPEARING and FALLING
					if (!curConf.contains("entry.doorMaterial")){key = "entry.doorMaterial"; break;}
				}
				if (!curConf.contains("exit.type")) 			{key = "exit.type"; break;}
				if (Passageway.DoorType.values()[curConf.getInt("exit.type")] == Passageway.DoorType.PISTON) {
					if (!curConf.contains("exit.redstoneLoc"))	{key = "exit.redstoneLoc"; break;}
				}else { // APPEARING and FALLING
					if (!curConf.contains("exit.doorMaterial"))	{key = "exit.doorMaterial"; break;}
				}
				if (!curConf.contains("respawnLoc"))			{key = "respawnLoc"; break;}
			}
			if (!key.equalsIgnoreCase("")) throw new ConfigException("Key missing: " + key + ", in " + moduleName);
			
			// ################## keys contained in "Room": ########################
			for (String curName : roomModules) {
				moduleName = curName; // cannot iterate using 'name' directly
				curConf = Module.getConfig(this, moduleName);
				if (curConf == null) throw new ConfigException("Config file missing: " + moduleName + ".yml");
				
				//TODO: Add room key checks here, when changing mechanism to "tasks"
				//if (!curConf.contains("respawnLoc"))			{key = "respawnLoc"; break;}
			}
			if (!key.equalsIgnoreCase("")) throw new ConfigException("Key missing: " + key + ", in " + moduleName);
			
			if (!errTxt.equalsIgnoreCase("")) throw new ConfigException("Not all configs are valid! Failed checks:\n" + errTxt);
			// everything ok, if code reached here, returns no value
	 }

}

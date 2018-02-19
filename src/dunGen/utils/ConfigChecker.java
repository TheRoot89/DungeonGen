package dunGen.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import dunGen.DunGen;
import dunGen.DunGen.State;
import dunGen.Module;
import dunGen.Passageway.DoorType;
import dunGen.tasks.RoomTask.TaskType;
import dunGen.Module.ModuleType;


/**A class able to accept mc commands to wrap its functionality away.
 * It checks all yml files for rooms given in config.yml for correctness and raises errors with helpful text.
 */
public class ConfigChecker implements CommandExecutor {
	
	/**Exception thrown if something during yml config loading failed. Contains an error message.*/
	class ConfigException extends Exception{
		private static final long serialVersionUID = 1L; // serializeable, suppresses warning
		public ConfigException(String message) {super(message);} // allows a message to be used, getMessage() gets it back.
	}
	
	private final DunGen plugin;
	// working variables:
	private String curModuleName;
	private YamlConfiguration curConf;
	private String errTxt;				// current error text 
	
	public ConfigChecker(DunGen plugin) {
		this.plugin = plugin;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("checkConfig")) {
			sender.sendMessage(plugin.getName() + ": Starting YAML file check...");
			try {
				checkModuleYmlFiles();
				sender.sendMessage(plugin.getName() + ": Check ok!");
			} catch (Exception e) {
				plugin.setStateAndNotify(State.ERROR, "YAML check failed!");
				e.printStackTrace();
			}
			return true;
		}
		
		return false;
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * @param key	Key string to be printed out as invalid.
	 */
	private void addInvalidErrMsg(String key) {
		errTxt += "["+ curModuleName +".yml] Key value invalid: "+key + "\n";
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * Test the config file of a module for existence.*/
	private boolean configCheckAndSet() {
		curConf = Module.getConfig(plugin, curModuleName);
		if (curConf == null) {
			errTxt += "Config file missing: " + curModuleName + ".yml\n";
			return false;
		} else return true;
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * Checks a key for existence.*/
	private boolean keyCheck(String key) {
		if (!curConf.contains(key)) {
			errTxt += "["+curModuleName+".yml] Key missing: "+key + "\n";
			return false;
		} else return true;
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * Checks a key representing a string.*/
	private boolean stringCheck(String key) {
		if (!keyCheck(key)) return false;
		if ((curConf.getString(key) == null)) {
			addInvalidErrMsg(key);
			return false;
		} else return true;
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * Checks a key representing a vector.*/
	private boolean vecCheck(String key) {
		if (!keyCheck(key)) return false;
		if (curConf.getVector(key) == null) {
			addInvalidErrMsg(key);
			return false;
		} else return true;
	}
	
	/**Function to make the actual test code more readable by wrapping the string formatting.
	 * Checks a key representing a material.*/
	private boolean matCheck(String key) {
		if (!keyCheck(key)) return false;
		if (Material.getMaterial(curConf.getString(key).toUpperCase()) == null) {
			addInvalidErrMsg(key);
			return false;
		} else return true;
	}
	
	
	/**Main function of the utility. Checks all yml files and their keys for correctness, where possible.
	 * It relies on the set plugin reference and its lists of modules per type.
	 * @throws ConfigException
	 */
	public void checkModuleYmlFiles() throws ConfigException {
		curModuleName = "";
		errTxt = "";		// empty errTxt serves as Flag everything went well.
		String key = "";			// temporary key name

		// to append info on specific file if things go wrong, add try-catch:
		try {
			// ################# keys contained in "Module": ##################
			List<String> inspectionList = new ArrayList<String>();
			inspectionList.addAll(plugin.entryModules);
			inspectionList.addAll(plugin.passagewayModules);
			inspectionList.addAll(plugin.roomModules);
			for (String curName : inspectionList) {
				curModuleName = curName; // cannot iterate using 'curName' directly, saves the name for the exception
				if (!configCheckAndSet()) continue; // abort this file

				// Go through all the values, this consists of:
				// 1. Check key existence
				// 2. Check key read is not null
				// 3. Check key value range, if applicable
				// Each pass allows for the next step. Errors are added to a summed error text.
				// Enums can only have a certain list of values, others can be freely set.
				// Set and check methods are used to save on typing here.

				stringCheck("description");

				key = "schematic";
				if (stringCheck(key)) {
					File f = new File(plugin.dir,curConf.getString(key)+".schematic");
					if (!f.exists()) errTxt += "["+curModuleName+".yml] File does not exist: "+key + ".schematic\n";
				}

				key = "type";
				if (stringCheck(key) && ModuleType.valueOf(curConf.getString(key).toUpperCase()) == null) addInvalidErrMsg(key);				

				for(String parentKey: new String[] {"entry" , "exit"}) {
					if (keyCheck(parentKey)) {
						vecCheck(parentKey+".placementLoc");
						vecCheck(parentKey+".doorLoc"); 
						key = parentKey+".width";
						if (keyCheck(key) && (curConf.getInt(key) == 0)) addInvalidErrMsg(key);
						key = parentKey+".height";
						if (keyCheck(key) && (curConf.getInt(key) == 0)) addInvalidErrMsg(key);
					}
				}
			} // END for loop over all modules

			// ########### keys contained in "Passageway" or "Entry": #############
			// (no differences at the moment, add new block if things are added to Entries alone)
			inspectionList.clear();
			inspectionList.addAll(plugin.entryModules);
			inspectionList.addAll(plugin.passagewayModules);
			for (String curName : inspectionList) {
				curModuleName = curName; // cannot iterate using 'name' directly
				if (!configCheckAndSet()) continue; // abort this file

				vecCheck("respawnLoc");
				key = "exit.initDirec";
				if (keyCheck(key) && ((curConf.getInt(key) + 360)%90 != 0)) addInvalidErrMsg(key);

				for(String parentKey: new String[] {"entry" , "exit"}) {
					if (keyCheck(parentKey)) {
						key = parentKey+".type";
						if (stringCheck(key)) {
							if (DoorType.valueOf(curConf.getString(key).toUpperCase()) == null)
								addInvalidErrMsg(key);
							else if ((DoorType.valueOf(curConf.getString(key).toUpperCase()) == DoorType.PISTON)) { // PISTON data check
								vecCheck(parentKey+".redstoneLoc");
							} else { //APPEARING and FALLING data check
								matCheck(parentKey+".doorMaterial");
							}
						}
					}
				}
			} // END for loop over all entries + passageways


			// ################## keys contained in "Room": ########################
			for (String curName : plugin.roomModules) {
				curModuleName = curName; // cannot iterate using 'name' directly
				if (!configCheckAndSet()) continue; // abort this file

				// Individual room types:
				key = "type";
				if (stringCheck(key) && ModuleType.valueOf(curConf.getString(key).toUpperCase()) == ModuleType.PLATFORMROOM) {
					vecCheck("targetRegCorner1");
					vecCheck("targetRegCorner2");
				}

				// Checking RoomTasks:
				String parentKey = "tasks";
				keyCheck(parentKey);
				int taskNr = 1;

				while (curConf.contains(parentKey+".task"+taskNr)) {
					String subKey = parentKey+".task"+taskNr;

					keyCheck(subKey+".delay");
					keyCheck(subKey+".period");
					vecCheck(subKey+".regionCorner1");
					vecCheck(subKey+".regionCorner2");

					key = subKey+".type";
					if (stringCheck(key) && (TaskType.valueOf(curConf.getString(key).toUpperCase()) == null)) {
						addInvalidErrMsg(key);
					} else {
						TaskType type = TaskType.valueOf(curConf.getString(key).toUpperCase());
						switch (type) {
						case BLOCKSPAWN:
							matCheck(subKey+".blockType");
							vecCheck(subKey+".incrementVector");
							break;

						case ENTITYSPAWN:
							key = subKey+".entityType";
							if (stringCheck(key) && (EntityType.valueOf(curConf.getString(key)) == null)) addInvalidErrMsg(key);
							key = subKey+".count";
							if (keyCheck(key) && curConf.getInt(key) < 0) addInvalidErrMsg(key);
							key = subKey+".maxCount";
							if (keyCheck(key) && curConf.getInt(key) < 0) addInvalidErrMsg(key);
							keyCheck(subKey+".isTarget");
							break;

						case MAZE:
							key = subKey+".wayWidth";
							if (keyCheck(key) && curConf.getInt(key) <= 0) addInvalidErrMsg(key);
							key = subKey+".wallWidth";
							if (keyCheck(key) && curConf.getInt(key) <= 0) addInvalidErrMsg(key);
							key = subKey+".wallHeight";
							if (keyCheck(key) && curConf.getInt(key) <= 0) addInvalidErrMsg(key);
							matCheck(subKey+".mazeMaterial");
							break;

						case POWER:
							key = subKey+".onTime";
							if (keyCheck(key) && curConf.getDouble(key) < 0) addInvalidErrMsg(key);
							break;

						default:
							addInvalidErrMsg(key); // key here points to ".type"
						}
					}

					taskNr++;
				} // END task checks
			} // END room checks
		} catch (Exception e) {
			e.printStackTrace();
			if(errTxt.isEmpty()) errTxt = "<empty>";
			throw new ConfigException(	"Exception during test, failed at module: "+curModuleName+"\n"+
					     				"At key: "+key+"\n"+
										"Further testing was stopped.\n"+
					     				"Error text up to that point: "+errTxt);
		}

		// Final evaluation of accumulated messages:
		if (!errTxt.equalsIgnoreCase("")) throw new ConfigException("Not all configs are valid! Failed checks:\n" + errTxt);
	 }



}

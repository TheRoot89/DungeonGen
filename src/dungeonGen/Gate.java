package dungeonGen;

import org.bukkit.Material;

import com.sk89q.worldedit.Vector;

public class Gate {

	private Module parent;
	public Vector pos;
	public Direc direc;
	public int width;
	public int height;
	public Material mat;
	
	public Gate(Module parent){
		this.parent = parent;
	}
}

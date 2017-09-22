package dungeonGen;

import com.sk89q.worldedit.Vector;

public enum Direc {
	NORTH (180),
	WEST (90),
	SOUTH (0),
	EAST (270);
	
	private int degree;

	Direc(int degree){
		this.degree = degree;
	}
	
	public static Direc fromDeg(float deg) {
		deg = (deg+360) % 360;
		if (deg < 45) {
			return Direc.SOUTH;
		}else if (deg < 135) {
			return Direc.WEST;
		}else if (deg < 225) {
			return Direc.NORTH;
		}else if (deg < 315) {
			return Direc.EAST;
		}else {
			return Direc.SOUTH;
		}
	}
	
	public int degree() {return degree;}
	
	/**
	 * Rotates a vector by a certain degree. Only steps of 90° are allowed!
	 * @param vec	The vector object (reference!) to be changed.
	 * @param deg	
	 */
	public static Vector rotatedBy(Vector vec, int deg) {
		//System.out.println("Vec to turn:" + vec.toString());
		// getting positive values for the angle:
		deg = (deg+360) % 360; // modulo
		// input check:
		if ((deg < 0)||((deg%90) != 0)) {
			System.out.println("Invalid rotation angle!");
			return new Vector();
		}
		
		// rotation:
		Vector ret = new Vector(vec);
		if (deg == 0) {
			//nothing to do
		}else if (deg == 90) {
			ret = new Vector(-vec.getBlockZ(),vec.getBlockY(),vec.getBlockX());
		}else if(deg == 180) {
			ret = new Vector(-vec.getBlockX(),vec.getBlockY(),-vec.getBlockZ());
		}else if(deg == 270) {
			ret = new Vector(vec.getBlockZ(),vec.getBlockY(),-vec.getBlockX());
		}
		return ret;
	}
	
	public Vector toUnityVec() {
		return new Vector(-Helper.sind(degree),0,Helper.cosd(degree));
	}
	
}

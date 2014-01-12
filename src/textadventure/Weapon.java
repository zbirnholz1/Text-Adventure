package textadventure;

import org.json.JSONObject;

import textadventure.*;

public class Weapon extends StaticObject {
	private int damage;
	//private int maxRange?
	private double accuracy, slope;
	private Material material;
	private Structure structure;
	private boolean isASpell;
	
	public Weapon(String name, String adjective, String description) {
		
	}
	
	public Weapon(JSONObject sourceObj) {
		super(sourceObj);
		//TODO (Zachary will do this)
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}
	
	public int getDamage() {
		return damage;
	}
	
	public double getAccuracy() {
		return accuracy;
	}
	
	public double getSlope() {
		return slope;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public Structure getStructure() {
		return structure;
	}
	
	public boolean isASpell() {
		return isASpell;
	}

}

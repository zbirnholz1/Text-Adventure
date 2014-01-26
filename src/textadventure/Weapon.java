package textadventure;

import org.json.*;

public class Weapon extends StaticObject {
	private int damage;
	private int numAttacks;
	//private int maxRange?
	private double accuracy, rangeSlope, critSlope;
	private Material material;
	private Structure structure;
	private boolean isASpell;

	public Weapon(String name, String adjective, String description) {

	}

	public Weapon(JSONObject source) {
		super(source);
		try {
			damage=source.getInt("damage");
			numAttacks=source.getInt("numAttacks");
			accuracy=source.getDouble("accuracy");
			rangeSlope=source.getDouble("rangeSlope");
			material=Material.valueOf(source.getString("material"));
			structure=Structure.valueOf(source.getString("structure"));
			isASpell=source.getBoolean("isASpell");
		} catch (JSONException e) {
			Main.game.getView().println("Something went wrong: "+e);
		}
	}

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			obj.put("damage", damage);
			obj.put("numAttacks", numAttacks);
			obj.put("accuracy", accuracy);
			obj.put("rangeSlope", rangeSlope);
			obj.put("material", material.toString());
			obj.put("structure", structure.toString());
			obj.put("isASpell", isASpell);
		} catch(JSONException e) {
			Main.game.getView().println("Something went wrong: "+e);
		}
		return obj;
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

	public double getRangeSlope() {
		return rangeSlope;
	}

	public double getCritSlope() {
		return critSlope;
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

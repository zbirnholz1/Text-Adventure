package textadventure;

import org.json.*;

public class Weapon extends StaticObject {
	private int damage;
	private int numAttacks;
	//private int maxRange?
	private double accuracy, rangeSlope, critSlope;
	private AttackType type;
	private boolean isASpell;

	public Weapon(int damage, int numAttacks, double accuracy, double rangeSlope, double critSlope, AttackType type, boolean isASpell) {
		this.damage=damage;
		this.numAttacks=numAttacks;
		this.accuracy=accuracy;
		this.rangeSlope=rangeSlope;
		this.critSlope=critSlope;
		this.type=type;
		this.isASpell=isASpell;
	}

	public Weapon(JSONObject source) {
		super(source);
		try {
			damage=source.getInt("damage");
			numAttacks=source.getInt("numAttacks");
			accuracy=source.getDouble("accuracy");
			rangeSlope=source.getDouble("rangeSlope");
			type=AttackType.valueOf(source.getString("structure"));
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
			obj.put("structure", type.toString());
			obj.put("isASpell", isASpell);
		} catch(JSONException e) {
			Main.game.getView().println("Something went wrong: "+e);
		}
		return obj;
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}
	
	public String getDescription() {
		return super.getDescription()+"\nWeapon type: "+getType().toString()+"\nBase damage: "+getDamage()+"\nBase accuracy: "+getAccuracy();
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

	public AttackType getType() {
		return type;
	}

	public boolean isASpell() {
		return isASpell;
	}
	
	public int getNumAttacks() {
		return numAttacks;
	}
	
	public boolean isMelee() {
		return rangeSlope>=0.45;
	}
}

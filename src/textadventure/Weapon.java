package textadventure;
import org.json.*;

public class Weapon extends Attack {
	private int power;
	private double accuracy;
	private boolean isRanged, isCounterAttack;
	
	public Weapon(JSONObject source) {
		super(source);
		try {
			power=source.getInt("power");
			accuracy=source.getDouble("accuracy");
			isRanged=source.getBoolean("isRanged");
			isCounterAttack=source.getBoolean("isCounterAttack");
		} catch(JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
	}
	
	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			obj.put("power", power);
			obj.put("accuracy", accuracy);
			obj.put("isRanged", isRanged);
			obj.put("isCounterAttack", isCounterAttack);
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public int getPower() {
		return power;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public boolean isRanged() {
		return isRanged;
	}
	
	public boolean isCounterAttack() {
		return isCounterAttack;
	}
	
	public void setPower(int power) {
		this.power = power;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public void setIsRanged(boolean isRanged) {
		this.isRanged = isRanged;
	}

	public void setIsCounterAttack(boolean isCounterAttack) {
		this.isCounterAttack = isCounterAttack;
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}

}

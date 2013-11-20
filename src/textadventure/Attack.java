package textadventure;
import org.json.JSONObject;

public abstract class Attack extends TAObject {	
	
	public Attack(JSONObject source) {
		super(source);
	}
	
	public abstract int getPower();
	
	public abstract double getAccuracy();
	
	public abstract boolean isRanged();
	
	public abstract boolean isCounterAttack();
}

package textadventure;
import org.json.JSONObject;

public class StaticObject extends TAObject {
	
	public StaticObject() {
		super();
	}
	
	public StaticObject(String name, String description, String adjective, boolean takeable) {
		super(name, description, adjective);
		this.takeable=takeable;
	}
	
	public StaticObject(JSONObject sourceObj) {
		super(sourceObj);
	}
	
	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}
}

package textadventure;

import org.json.JSONObject;

import textadventure.*;

public class Weapon extends StaticObject {
	
	
	public Weapon(String name, String adjective, String description) {
		
	}
	
	public Weapon(JSONObject sourceObj) {
		super(sourceObj);
		//TODO (Zachary will do this)
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}

}

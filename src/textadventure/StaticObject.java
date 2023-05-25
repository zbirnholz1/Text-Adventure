package textadventure;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StaticObject extends TAObject {
	private Map<String, String> commands;

	public StaticObject() {
		super();
	}

	public StaticObject(String name, String description, String adjective, boolean takeable) {
		super(name, description, adjective);
		this.takeable=takeable;
	}

	public StaticObject(JSONObject sourceObj) {
		super(sourceObj);
		if(!sourceObj.has("isPrinted"))
			isPrinted=false;
		if(!sourceObj.has("takeable"))
			takeable=false;
		try {
			if(sourceObj.has("commands")) {
				commands=new HashMap<String, String>();
				JSONArray JSONCommands=sourceObj.getJSONArray("commands");
				for(int i=0; i<JSONCommands.length(); i++) {
					JSONArray JSONCommand=JSONCommands.getJSONArray(i);
					commands.put(JSONCommand.getString(0), JSONCommand.getString(1));
				}
			}
		} catch(JSONException e){e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return super.process(verb, otherObject, thisIsDO);
	}
}

package textadventure;
import java.util.*;
import org.json.*;

public class DynamicObject extends TAObject {
	protected List<Map<String, Object[]>> commands;
	//maps in commands have the verb that
	//does something, which points to the effect:
	//"+/-" indicates a state change, [...] means
	//that ... should be printed, {...} is the command
	//that should be processed back in Game that
	//results from the state change
	//Object[0] is the effect,
	//Object[1] is the adjective of the other object (DO/IO),
	//Object[2] is the name of the other object (DO/IO),
	//Object[3] is whether or not this is the DO in the specific command
	protected List<String> names;
	protected List<Set<String>> otherNames;
	protected List<String> descriptions;
	protected int state;

	public DynamicObject(JSONObject source) {
		super(source);
		try {
			int numStates=3;
			if(source.has("numStates"))
				numStates=source.getInt("numStates");
			commands=new ArrayList<Map<String, Object[]>>(numStates);
			otherNames=new ArrayList<Set<String>>(numStates);
			for(int i=0; i<numStates; i++) {
				commands.add(i, new HashMap<String, Object[]>());
				otherNames.add(i, new HashSet<String>());
			}
			state=source.getInt("state");
			names=new ArrayList<String>(numStates);
			descriptions=new ArrayList<String>(numStates);
			if(source.has("names")) {
				JSONArray JSONNames=source.getJSONArray("names");
				for(int i=0; i<JSONNames.length(); i++)
					names.add(JSONNames.getString(i));
			}
			else if(source.has("name")) {
				for(int i=0; i<numStates; i++)
					names.add(source.getString("name"));
			}
			if(source.has("descriptions")) {
				JSONArray JSONDescriptions=source.getJSONArray("descriptions");
				for(int i=0; i<JSONDescriptions.length(); i++)
					descriptions.add(JSONDescriptions.getString(i));
			}
			else if(source.has("description")) {
				for(int i=0; i<numStates; i++)
					descriptions.add(source.getString("description"));
			}
			if(source.has("adjective"))
				adjective=source.getString("adjective");
			if(source.has("otherNames")) {
				JSONArray JSONOtherNames=source.getJSONArray("otherNames");
				try {
					for(int i=0; i<numStates; i++) {
						if(JSONOtherNames.get(i)==null||JSONOtherNames.getJSONArray(i).length()==0) {
							otherNames.add(i, null);
							continue;
						}
						for(int j=0; j<JSONOtherNames.getJSONArray(i).length(); j++) {
							String otherName=JSONOtherNames.getJSONArray(i).getString(j);
							otherNames.get(i).add(otherName);
						}
					}
				} catch(JSONException e) { //then it is only a 1-D array
					for(int i=0; i<numStates; i++) {
						for(int j=0; j<JSONOtherNames.length(); j++)
							otherNames.get(i).add(JSONOtherNames.getString(j));
					}
				}
			}
			JSONArray commandsArray=source.getJSONArray("commands");
			for(int i=0; i<numStates; i++) {
				if(commandsArray.getJSONArray(i).length()==0) {
					commands.set(i, null);
					continue;
				}
				for(int j=0; j<commandsArray.getJSONArray(i).length(); j++) {
					String JSONCommand=commandsArray.getJSONArray(i).getString(j);
					String command=JSONCommand.substring(0, JSONCommand.indexOf("|"));
					String effect=JSONCommand.substring(JSONCommand.indexOf("|")+1, JSONCommand.indexOf("||"));
					String otherObjectAdjective=JSONCommand.substring(JSONCommand.indexOf("||")+2, JSONCommand.indexOf("|||"));
					String otherObjectName=JSONCommand.substring(JSONCommand.indexOf("|||")+3, JSONCommand.indexOf("||||"));
					boolean thisIsDO=JSONCommand.contains("||||true");
					commands.get(i).put(command, new Object[]{effect, otherObjectAdjective, otherObjectName, thisIsDO});
				}
			}
		} catch(JSONException e){e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
	}

	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("state", state);
			obj.put("visible", visible);
			obj.put("takeable", takeable);
			obj.put("names", names);
			obj.put("descriptions", descriptions);
			obj.put("adjective", adjective);
			obj.put("numStates", names.size());
			obj.put("isPrinted",  isPrinted);
			if(otherNames!=null) {
				JSONArray JSONOtherNames=new JSONArray();
				for(Set<String> set:otherNames)
					JSONOtherNames.put(set);
				obj.put("otherNames", JSONOtherNames);
			}
			if(commands!=null) {
				JSONArray JSONCommands=new JSONArray();
				for(Map<String, Object[]> map:commands) {
					JSONArray JSONCommand=new JSONArray();
					if(map!=null) {
						for(String str:map.keySet()) {
							JSONCommand.put(str+"|"+map.get(str)[0]+"||"+map.get(str)[1]+"|||"+map.get(str)[2]+"||||"+map.get(str)[3]);
						}
					}
					JSONCommands.put(JSONCommand);
				}
				obj.put("commands", JSONCommands);
			}
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public String process(String command, TAObject otherObject, boolean thisIsDO) {
		if(commands.get(state)==null)	  //should otherObject be a String or a TAObject?
			return null;
		Map<String, Object[]> map=commands.get(state);
		String effect;
		if(map.containsKey(command))
			effect=(String)map.get(command)[0];
		else
			return null;
		String expectedOtherObjectAdjective=(String)commands.get(state).get(command)[1];
		String expectedOtherObjectName=(String)commands.get(state).get(command)[2];
		boolean shouldBeDO=(Boolean)commands.get(state).get(command)[3];
		if(!expectedOtherObjectName.equals("null")) {
			if(otherObject==null||(expectedOtherObjectAdjective.equals("null")&&otherObject.getAdjective()!=null))
				return null;
			if(effect==null||(!expectedOtherObjectName.equals(otherObject.getName())&&!otherObject.alsoKnownAs(expectedOtherObjectName))
					||(!expectedOtherObjectAdjective.equals(otherObject.getAdjective())&&otherObject.getAdjective()!=null)
					||thisIsDO!=shouldBeDO)
				return null;
		}
		char plusOrMinus=effect.charAt(0);
		if(plusOrMinus=='-')
			state--;
		else if(plusOrMinus=='+')
			state++;
		if(effect.contains("||"))
			effect=effect.substring(effect.indexOf(plusOrMinus)+1, effect.indexOf("||"));
		return effect;
	}

	public boolean alsoKnownAs(String otherName) {
		if(otherNames.get(state)==null)
			return false;
		for(String str:otherNames.get(state))
			if(str.equalsIgnoreCase(otherName))
				return true;
		return false;
	}
	
	public boolean hasEffect(String verb) {
		Map<String, Object[]> map=commands.get(state);
		if(map==null)
			return false;
		return map.containsKey(verb);
	}

	public String getName() {
		return names.get(state);
	}

	public String getDescription() {
		return descriptions.get(state);
	}
	
	public int getState() {
		return state;
	}

	public int hashCode() {
		int hash=names.hashCode();
		if(getAdjective()!=null)
			hash+=getAdjective().hashCode();
		else
			hash+=descriptions.hashCode();
		if(otherNames.size()!=0)
			hash*=otherNames.size();
		return hash;
	}
}

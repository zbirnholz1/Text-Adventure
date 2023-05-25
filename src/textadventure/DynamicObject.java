package textadventure;
import java.util.*;

import org.json.*;

public class DynamicObject extends TAObject {
	protected List<Map<List<String>, String>> commands;
	//Commands maps commands (e.g. open chest) to their effects.
	//Multiple commands that do the same thing are stored in a List that maps
	//to their shared effect.
	//The effects start with +/-/= to indicate how the DynamicObject's
	//state should change after the command is processed.
	//Each index in the list corresponds to the available command/effect pairs
	//for each state. Values in the maps take their usual form of [...] prints
	//and {...} gets processed in Game.

	protected List<String> names;
	protected List<Set<String>> otherNames;
	protected List<String> descriptions;
	protected int state;

	@SuppressWarnings("serial")
	public DynamicObject(JSONObject source) {
		super(source);
		try {
			int numStates=3;
			if(source.has("numStates"))
				numStates=source.getInt("numStates");
			commands=new ArrayList<Map<List<String>, String>>(numStates);
			otherNames=new ArrayList<Set<String>>(numStates);
			for(int i=0; i<numStates; i++) {
				commands.add(i, new HashMap<List<String>, String>(){
					//allow for both a String and a List to be passed into get for commands
					public String get(Object key) {
						if(key instanceof List<?>)
							return super.get(key);
						else if(key instanceof String) {
							for(List<String> list:keySet())
								if(list.contains(key))
									return get(list);
							return null;
						}
						else {
							return null;
						}
					}
				});
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
			//System.out.println(commandsArray); //for debugging when redesigning command formatting
			for(int i=0; i<numStates; i++) {
				if(commandsArray.getJSONArray(i).length()==0) {
					commands.set(i, null);
					continue;
				}
				JSONArray JSONStateCommands=commandsArray.getJSONArray(i);
				for(int j=0; j<commandsArray.getJSONArray(i).length(); j++) {
					JSONArray JSONCommandEffectPair = JSONStateCommands.getJSONArray(j);
					String command = JSONCommandEffectPair.getString(0);
					String effect = JSONCommandEffectPair.getString(1);
					String[] commandArray = command.split("/"); //break up command over "/"'s to get each individual command that maps to the same effect
					List<String> commandList = new ArrayList<String>();
					for(int z = 0; z < commandArray.length; z++) {
						commandList.add(commandArray[z]);
					}
					commands.get(i).put(commandList, effect);
				}
			}
		} catch(JSONException e){System.out.println(name);e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
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
				for(Map<List<String>, String> map:commands) {
					JSONArray JSONStateCommands=new JSONArray();
					if(map!=null) {
						for(List<String> str:map.keySet()) {
							String commandsList="";
							for(String com:str)
								commandsList+="/"+com;
							JSONArray JSONCommandEffectPair = new JSONArray();
							JSONCommandEffectPair.put(commandsList); //command(s)
							JSONCommandEffectPair.put(map.get(str)); //effect
							JSONStateCommands.put(JSONCommandEffectPair);
						}
					}
					JSONCommands.put(JSONStateCommands);
				}
				obj.put("commands", JSONCommands);
			}
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public String process(String command, TAObject otherObject, boolean thisIsDO) {
		//for multiple verbs doing the same thing, the JSON format is verb1/verb2|...
		if(commands.get(state)==null)
			return null;
		Map<List<String>, String> map=commands.get(state);
		String expectedCommand = command;
		String expectedCommandWithoutOtherObjectIO = command; //in cases like "knock on flimsy door" where an IO isn't necessary but could be supplied anyways
		if(thisIsDO) {
			expectedCommand += " " + getFullName();
			expectedCommandWithoutOtherObjectIO += " " + getFullName();
			if(otherObject != null) {
				expectedCommand += " " + otherObject.getFullName();
			}
		} else {
			expectedCommand += " " + otherObject.getFullName() + " " + getFullName();
		}
		String effect=null;
		for(List<String> list:map.keySet()) { //find the command given by the parameters in the map
			if(list.contains(expectedCommand)) {
				effect=map.get(expectedCommand);
				break;
			} else if(thisIsDO && list.contains(expectedCommandWithoutOtherObjectIO)) {
				effect = map.get(expectedCommandWithoutOtherObjectIO);
				break;
			}
		}
		if(effect==null)
			return null;
		char plusOrMinus=effect.charAt(0);
		if(plusOrMinus=='-') {
			state--;
			effect = effect.substring(1);
		}
		else if(plusOrMinus=='+') {
			state++;
			effect = effect.substring(1);
		} else if(plusOrMinus == '=') {
			//state stays the same
			effect = effect.substring(1);
		}
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
		Map<List<String>, String> map=commands.get(state);
		if(map==null)
			return false;
		for(List<String> list:map.keySet()) {
			for(String str : list) {
				if(str.startsWith(verb)) {
					return true;
				}
			}
		}
		return false;
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

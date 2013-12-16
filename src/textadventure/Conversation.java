package textadventure;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Conversation {
		private List<String> NPCStatements;
		private List<String> playerPrompts;
		private List<List<String>> correctPlayerResponses;
		private List<String> NPCIncorrectResponses;
		private List<String> playerStatements;
		private List<String> playerResponseEffects; //like setting the Player's name, etc.
		private String effect;

		public Conversation(JSONObject source) {
			try {
				JSONArray JSONNPCStatements=source.getJSONArray("NPCStatements");
				NPCStatements=new ArrayList<String>();
				for(int i=0; i<JSONNPCStatements.length(); i++) {
					try {
						NPCStatements.add(JSONNPCStatements.getString(i));
					} catch(JSONException ex) {NPCStatements.add(null);}
				}
				JSONArray JSONPlayerPrompts=source.getJSONArray("playerPrompts");
				playerPrompts=new ArrayList<String>();
				for(int i=0; i<JSONPlayerPrompts.length(); i++) {
					try {
						playerPrompts.add(JSONPlayerPrompts.getString(i));
					} catch(JSONException ex) {playerPrompts.add(null);}
				}
				JSONArray JSONAllCorrectResponses=source.getJSONArray("correctPlayerResponses");
				correctPlayerResponses=new ArrayList<List<String>>();
				for(int i=0; i<JSONAllCorrectResponses.length(); i++) {
					JSONArray JSONCorrectResponses=JSONAllCorrectResponses.getJSONArray(i);
					List<String> responses=new ArrayList<String>();
					for(int j=0; j<JSONCorrectResponses.length(); j++)
						responses.add(JSONCorrectResponses.getString(j));
					correctPlayerResponses.add(responses);
				}
				JSONArray JSONNPCIncorrectResponses=source.getJSONArray("NPCIncorrectResponses");
				NPCIncorrectResponses=new ArrayList<String>();
				for(int i=0; i<JSONNPCIncorrectResponses.length(); i++) {
					try {
						NPCIncorrectResponses.add(JSONNPCIncorrectResponses.getString(i));
					} catch(JSONException ex){NPCIncorrectResponses.add(null);}
				}
				JSONArray JSONPlayerStatements=source.getJSONArray("playerStatements");
				playerStatements=new ArrayList<String>(JSONPlayerStatements.length());
				for(int i=0; i<JSONPlayerStatements.length(); i++) {
					try {
						playerStatements.add(JSONPlayerStatements.getString(i));
					} catch(JSONException ex){playerStatements.add(null);}
				}
				if(source.has("playerResponseEffects")) {
					JSONArray JSONPlayerResponseEffects=source.getJSONArray("playerResponseEffects");
					playerResponseEffects=new ArrayList<String>(JSONPlayerResponseEffects.length());
					for(int i=0; i<JSONPlayerResponseEffects.length(); i++) {
						try {
							playerResponseEffects.add(JSONPlayerResponseEffects.getString(i));
						} catch(JSONException ex){playerResponseEffects.add(null);}
					}
				}
				if(source.has("effect"))
					effect=source.getString("effect");
				else
					effect="";
			} catch(JSONException e) {e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
		}

		public JSONObject toJSONObject() {
			JSONObject obj=new JSONObject();
			try {
				for(String str:NPCStatements)
					obj.accumulate("NPCStatements", str);
				for(String str:playerPrompts)
					obj.accumulate("playerPrompts", str);
				for(List<String> list:correctPlayerResponses) {
					JSONArray responses=new JSONArray();
					for(String str:list)
						responses.put(str);
					obj.accumulate("correctPlayerResponses", responses);
				}
				if(playerResponseEffects!=null)
					for(String str:playerResponseEffects)
						obj.accumulate("playerResponseEffects", str);
				for(String str:NPCIncorrectResponses)
					obj.accumulate("NPCIncorrectResponses", str);
				obj.put("effect", effect);
			} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
			return obj;
		}

		public String getEffect() {
			return effect;
		}

		public List<String> getNPCStatements() {
			return NPCStatements;
		}

		public List<String> getPlayerPrompts() {
			return playerPrompts;
		}

		public List<String> getCorrectPlayerResponses(int index) {
			return correctPlayerResponses.get(index);
		}

		public List<String> getNPCIncorrectResponses() {
			return NPCIncorrectResponses;
		}

		public List<String> getPlayerStatements() {
			return playerStatements;
		}

		public List<String> getPlayerResponseEffects() {
			return playerResponseEffects;
		}

		public void setEffect(String newEffect) {
			effect=newEffect;
		}
	}
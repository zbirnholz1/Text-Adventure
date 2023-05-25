package textadventure;

import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Conversation {

	public static final String INDENTATION = "    ";

	private String title;
	private Set<Integer> rooms;
	private boolean isAnywhere;
	private ArrayList<String> conversationStarters;
	private ArrayList<Vertex> vertices;
	private boolean isReachable;

	public Conversation(JSONObject source) {
		try {
			//title
			title = source.getString("title");
			//rooms
			rooms = new TreeSet<Integer>();
			if(source.has("rooms")) {
				JSONArray JSONRooms = source.getJSONArray("rooms");
				for (int i = 0; i < JSONRooms.length(); i++) {
					if(JSONRooms.getInt(i) < 0 && i > 0) {
						//then it's a range between the previous number and this number
						for (int rangeNum = JSONRooms.getInt(i-1); rangeNum <= Math.abs(JSONRooms.getInt(i)); rangeNum++) {
							rooms.add(rangeNum);
						}
					} else {
						rooms.add(JSONRooms.getInt(i));
					}
				}
				isAnywhere = false;
			} else {
				isAnywhere = true;
			}

			//conversation starters
			conversationStarters = new ArrayList<String>();
			if(source.has("conversationStarters")) {
				JSONArray JSONConvoStarters = source.getJSONArray("conversationStarters");
				for (int i = 0; i < JSONConvoStarters.length(); i++) {
					conversationStarters.add(JSONConvoStarters.getString(i));
				}
			}

			//vertices
			vertices = new ArrayList<Vertex>();
			JSONArray JSONVertices = source.getJSONArray("vertices");
			for (int i = 0; i < JSONVertices.length(); i++) {
				Vertex vertex = new Vertex(JSONVertices.getJSONObject(i));
				vertices.add(vertex);
			}

			//reachable
			isReachable = source.getBoolean("reachable");
		} catch (JSONException e) {
			Main.game.getView().println("Something went wrong loading a conversation: "+e);
		}
	}

	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("title", title);
			if(!isAnywhere) {
				for (int room : rooms) {
					obj.accumulate("rooms", room);
				}
			}
			for (String conversationStarter : conversationStarters) {
				obj.accumulate("conversationStarters", conversationStarter);
			}
			obj.put("reachable", isReachable);
			for (Vertex v : vertices) {
				obj.accumulate("vertices", v.toJSONObject());
			}
		} catch(JSONException e) {
			Main.game.getView().println("Something went wrong saving a conversation: "+e);
			e.printStackTrace();
		}
		return obj;
	}

	public void doConversation(NPC NPCParticipant) {
		doConversation(NPCParticipant, 0);
	}

	public void doConversation(NPC NPCParticipant, int startingVertexIDNum) {
		NPCParticipant.setCurrentConversation(this);
		Vertex currentVertex = vertices.get(startingVertexIDNum);

		while(true) {
			//process the effect
			processEffect(currentVertex.getEffect());

			//player statement, if there is one
			String playerStatement = currentVertex.getPlayerStatement();
			if(playerStatement != null && !playerStatement.isEmpty()) {
				//surround entire statement with parentheses if it's a thought
				String toPrint = playerStatement.startsWith("(") ? "(You: "+playerStatement.substring(1) : "You: "+playerStatement;
				toPrint = toPrint.replace("playerName", Main.game.getPlayer().getFullName());
				Main.game.getView().printlnNPC(INDENTATION + toPrint + "\n");
			}

			//NPC statement, if there is one
			String NPCStatement = currentVertex.getNPCStatement();
			if(NPCStatement != null && !NPCStatement.isEmpty()) {
				//surround NPC's statement with quotes
				String toPrint = NPCParticipant.getFullName() + ": \"" + NPCStatement + "\"";
				toPrint = toPrint.replace("playerName", Main.game.getPlayer().getFullName());
				Main.game.getView().printlnNPC(INDENTATION + toPrint + "\n");
			}

			//print the player prompt if there is one
			String playerPrompt = currentVertex.getPlayerPrompt();
			int nextVertexID = -1;

			if(currentVertex.takesUserInput() && playerPrompt != null) {
				String toPrint = "You: "+ playerPrompt;
				if(!playerPrompt.isEmpty() && !playerPrompt.endsWith(" ")) {
					//add a space after the prompt if it isn't already there
					toPrint += " ";
				}
				toPrint = toPrint.replace("playerName", Main.game.getPlayer().getFullName());
				Main.game.getView().printNPC(INDENTATION + toPrint);

				//wait for the player's response and then process it with a GameListener
				GameListener responseListener = new ConversationGameListener(this, NPCParticipant, currentVertex);
				Main.game.getView().setGameListener(responseListener);
				return;
			} else if(currentVertex.isEndOfConversation()) {
				break;
			}
			
			nextVertexID = currentVertex.getPlayerInputResult(null);
			currentVertex = vertices.get(nextVertexID);
		}
	}
	
	private static void processEffect(String effect) {
		if(effect!=null && !effect.isEmpty()) {
			StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
			char firstCharOfLastToken=' ';
			while(tokenizer.hasMoreTokens()) {
				String toProcess=tokenizer.nextToken();
				if(firstCharOfLastToken=='[')
					Main.game.getView().printlnNPC(toProcess+"\n", false);
				else if(firstCharOfLastToken=='{') {
					Main.game.getCommandParser().parse(toProcess);
				}
				firstCharOfLastToken=toProcess.charAt(0);
			}
		}
	}

	/*
	 * Getter/setter methods
	 */
	public String getTitle() {
		return title;
	}

	public boolean isInRoom(int roomID) {
		return rooms.contains(roomID);
	}

	public boolean isAnywhere() {
		return isAnywhere;
	}

	public ArrayList<String> getConversationStarters() {
		return conversationStarters;
	}

	public boolean isReachable() {
		return isReachable;
	}
	
	public void setReachable(boolean newReachable) {
		isReachable = newReachable;
	}
	
	public Vertex getVertex(int ID) {
		return vertices.get(ID);
	}


	/*
	 * Vertex class implementation
	 * Used for storing each step in the conversation as well as the links between steps
	 * and reactions to correct/incorrect responses
	 */
	class Vertex {

		private int ID;
		private String effect; //whatever happens upon REACHING this vertex, may be null
		private String playerStatement;
		private String NPCStatement;
		private String playerPrompt;
		private ArrayList<Object[]> playerInputResults; //each array takes the form of ("input", nextVertexID)
		private boolean losable; //indicates whether inputs not included in the results above are considered incorrect (and would fail/end the conversation)
		private String incorrectResult; //whatever gets printed when the player puts in an invalid input
		private String incorrectEffect; //whatever happens when they put in an invalid input
		private String correctEffect; //whatever happens when they put in a valid input (one that leads to another vertex); HAPPENS RIGHT BEFORE THAT VERTEX IS REACHED

		public Vertex(JSONObject source) {
			try {
				//ID
				ID = source.getInt("ID");

				//effect
				if(source.has("effect") && !source.isNull("effect")) {
					effect = source.getString("effect");
				} else {
					effect = null;
				}

				//player statement
				playerStatement = source.isNull("playerStatement") ? null : source.getString("playerStatement");

				//NPC statement
				NPCStatement = source.isNull("NPCStatement") ? null : source.getString("NPCStatement");

				//player prompt
				playerPrompt = source.isNull("playerPrompt") ? null : source.getString("playerPrompt");

				//player input results
				playerInputResults = new ArrayList<Object[]>();
				JSONArray JSONPlayerInputResults = source.isNull("playerInputResults") ? null : source.getJSONArray("playerInputResults");
				if (JSONPlayerInputResults != null) {
					for (int i = 0; i < JSONPlayerInputResults.length(); i++) {
						JSONArray JSONOrderedPair = JSONPlayerInputResults.getJSONArray(i);
						Object[] orderedPair = new Object[2];
						orderedPair[0] = JSONOrderedPair.isNull(0) ? null : JSONOrderedPair.getString(0); //the input
						orderedPair[1] = JSONOrderedPair.getInt(1); //the resulting next vertex
						playerInputResults.add(orderedPair);
					}
				}

				//losable
				losable = source.getBoolean("losable");

				//unlosable text--message when the user puts in the wrong answer for an unlosable vertex
				if(source.has("incorrectResult") && !source.isNull("incorrectResult")) {
					incorrectResult = source.getString("incorrectResult");
				} else {
					incorrectResult = null;
				}

				//incorrect effect
				if(source.has("incorrectEffect") && !source.isNull("incorrectEffect")) {
					incorrectEffect = source.getString("incorrectEffect");
				} else {
					incorrectEffect = null;
				}
				
				//correct effect
				if(source.has("correctEffect") && !source.isNull("correctEffect")) {
					correctEffect = source.getString("correctEffect");
				} else {
					correctEffect = null;
				}
			} catch (JSONException e) {
				Main.game.getView().println("Something went wrong loading a vertex: "+e);
			}
		}

		public JSONObject toJSONObject() {
			JSONObject obj = new JSONObject();
			try {
				//ID
				obj.put("ID", ID);

				//effect
				if(effect != null) {
					obj.put("effect", effect);
				}

				//playerStatement
				obj.put("playerStatement", playerStatement);

				//NPC statement
				obj.put("NPCStatement", NPCStatement);

				//player prompt
				obj.put("playerPrompt", playerPrompt);

				//player input results
				for(Object[] orderedPair : playerInputResults) {
					JSONArray JSONOrderedPair = new JSONArray();
					JSONOrderedPair.put(orderedPair[0]);
					JSONOrderedPair.put(orderedPair[1]);
					obj.accumulate("playerInputResults", JSONOrderedPair);
				}

				//losable
				obj.put("losable", losable);

				//unlosable text
				if(incorrectResult != null && !incorrectResult.isEmpty()) {
					obj.put("incorrectResult", incorrectResult);
				}

				//incorrect effect
				if(incorrectEffect != null) {
					obj.put("incorrectEffect", incorrectEffect);
				}
			} catch(JSONException e) {
				Main.game.getView().println("Something went wrong saving a vertex: "+e);
				e.printStackTrace();
			}
			return obj;
		}

		public int getID() {
			return ID;
		}

		public String getPlayerStatement() {
			return playerStatement;
		}

		public String getNPCStatement() {
			return NPCStatement;
		}

		public String getPlayerPrompt() {
			return playerPrompt;
		}

		public int getPlayerInputResult(String input) {
			if (input == null) {
				if (!takesUserInput()) {
					return (int)playerInputResults.get(0)[1];
				} else {
					return -1;
				}
			}
			
			input = input.toLowerCase();

			for (Object[] orderedPair : playerInputResults) { //orderedPair is ("input", nextVertexID)
				String regexAcceptableResponse = ((String)orderedPair[0]).replace("*", ".*");
				if (regexAcceptableResponse.equals(".*")) {
					//exclude "" as a valid input because it's reserved for wimping out of no loss answer loops
					//"" MUST BE EXPLICITLY INCLUDED AS A VALID INPUT!
					regexAcceptableResponse = ".+";
				}
				if(Pattern.matches(regexAcceptableResponse, input)) {
					return (int)orderedPair[1];
				}
			}
			return -1; //-1 indicates that the input did not match ANY of the expected inputs
		}

		public boolean takesUserInput() {
			//is only false when there is only one ordered pair and it contains a null string
			return !(playerInputResults.size() == 1 && playerInputResults.get(0)[0] == null);
		}
		
		public boolean isValidInput(String input) {
			input = input.toLowerCase();
			for (Object[] orderedPair : playerInputResults) { //orderedPair is ("input", nextVertexID)
				String regexAcceptableResponse = ((String)orderedPair[0]).replace("*", ".*");
				if (regexAcceptableResponse.equals(".*")) {
					//exclude "" as a valid input because it's reserved for wimping out of no loss answer loops
					//"" MUST BE EXPLICITLY INCLUDED AS A VALID INPUT!
					regexAcceptableResponse = ".+";
				}
				if(Pattern.matches(regexAcceptableResponse, input)) {
					return true;
				}
			}
			return false;
		}

		public boolean isLosable() {
			return losable;
		}
		
		public String getEffect() {
			return effect;
		}
		
		public void addToEffect(String toAppend) {
			effect += toAppend;
		}
		
		public void removeFromEffect(String toRemove) {
			effect = effect.replace(toRemove, "");
		}

		public String getIncorrectResult() {
			return incorrectResult;
		}

		public String getIncorrectEffect() {
			return incorrectEffect;
		}
		
		public void setIncorrectEffect(String newIncorrectEffect) {
			incorrectEffect = newIncorrectEffect;
		}
		
		public String getCorrectEffect() {
			return correctEffect;
		}
		
		public void setCorrectEffect(String newCorrectEffect) {
			correctEffect = newCorrectEffect;
		}

		public boolean isEndOfConversation() {
			return playerInputResults == null || playerInputResults.isEmpty();
		}
	}

	private class ConversationGameListener implements GameListener {

		private Conversation conversation;
		NPC NPCParticipant;
		Vertex currentVertex;

		public ConversationGameListener(Conversation c, NPC n, Vertex v) {
			conversation = c;
			NPCParticipant = n;
			currentVertex = v;
		}

		//Only called when currentVertex actually takes user input
		public void textTyped(String text) {
			text = text.toLowerCase();
			text = text.replace("\"", "");
			text = text.replace(".", "");

			int nextVertexID = currentVertex.getPlayerInputResult(text);

			Main.game.getView().println("\n");
			
			if(currentVertex.isLosable()) {
				if(nextVertexID == -1) {
					//then they put in the wrong answer
					Main.game.getView().printlnNPC(currentVertex.getIncorrectResult());
					Main.game.getView().setGameListener(null);

					if(currentVertex.getIncorrectEffect() != null) {
						Conversation.processEffect(currentVertex.getIncorrectEffect().replace("playerResponse", text).replace("playerName", Main.game.getPlayer().getFullName()));
					}

					Main.game.getView().print(">");
				} else {
					//they put in a right answer!
					Main.game.getView().setGameListener(null);
					
					if(currentVertex.getCorrectEffect() != null) {
						Conversation.processEffect(currentVertex.getCorrectEffect().replace("playerResponse", text).replace("playerName", Main.game.getPlayer().getFullName()));
					}
					conversation.doConversation(NPCParticipant, nextVertexID);
				}
			} else {
				if (text.isEmpty() && !currentVertex.isValidInput("")) {
					//then the player wimped out of a no-loss answer loop
					//TODO write an actual description here
					Main.game.getView().printlnNPC("With nothing to say, you hold your tongue.\n");
					Main.game.getView().setGameListener(null);
				} else if(nextVertexID == -1) {
					//no-loss: try this vertex again
					Main.game.getView().printlnNPC(currentVertex.getIncorrectResult());
					//reprint the player prompt
					String playerPrompt = currentVertex.getPlayerPrompt();
					String toPrint = "You: "+ playerPrompt;
					if(!playerPrompt.isEmpty() && !playerPrompt.endsWith(" ")) {
						//add a space after the prompt if it isn't already there
						toPrint += " ";
					}
					Main.game.getView().printNPC(INDENTATION + toPrint);
				} else {
					//then they put in a valid response! :)
					Main.game.getView().setGameListener(null);
					
					if(currentVertex.getCorrectEffect() != null) {
						Conversation.processEffect(currentVertex.getCorrectEffect().replace("playerResponse", text).replace("playerName", Main.game.getPlayer().getFullName()));
					}
					conversation.doConversation(NPCParticipant, nextVertexID);
				}
			}
		}

	}

}





/********
 * Old version of this class is below
 * (before implementing branching conversation and updating JSON formatting)
 ********/

//package textadventure;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class Conversation {
//	private List<String> conversationStarters;
//	private List<Integer> conversationRooms;
//	private boolean isAnywhere;
//	private List<String> NPCStatements;
//	private List<String> playerPrompts;
//	private List<List<String>> correctPlayerResponses;
//	private List<String> incorrectResponseEffects; //could end the conversation or give the player another chance
//	private List<String> playerStatements;
//	private List<String> playerResponseEffects; //like setting the Player's name, etc.
//	private List<String> enterToEndEffects; //for when the prayer wimps out of a no-loss answer loop
//	private String effect; //for when the conversation is successfully completed
//	private String title; //how it will appear in lists of multiple conversation options
//	private boolean isReachable;
//	private List<String> nextConversations; //list of the titles of the conversations that are made reachable by finishing this one
//	private List<String> eliminatedConversations; //list of conversations that are no longer reachable after finishing this one
//
//	public Conversation(JSONObject source) {
//		try {
//			if(source.has("title")) {
//				title = source.getString("title");
//			} else {
//				title = null;
//			}
//			if(source.has("reachable")) {
//				isReachable = source.getBoolean("reachable");
//			} else {
//				isReachable = true;
//			}
//			if(source.has("nextConversations")) {
//				JSONArray JSONNextConversations = source.getJSONArray("nextConversations");
//				nextConversations = new ArrayList<String>();
//				for(int i=0; i<JSONNextConversations.length(); i++) {
//					nextConversations.add(JSONNextConversations.getString(i));
//				}
//			} else {
//				nextConversations = new ArrayList<String>();
//			}
//			if(source.has("eliminatedConversations")) {
//				JSONArray JSONEliminatedConversations = source.getJSONArray("eliminatedConversations");
//				nextConversations = new ArrayList<String>();
//				for(int i=0; i<JSONEliminatedConversations.length(); i++) {
//					eliminatedConversations.add(JSONEliminatedConversations.getString(i));
//				}
//			} else {
//				eliminatedConversations = new ArrayList<String>();
//			}
//			if(source.has("conversationStarters")) {
//				JSONArray JSONConversationStarters=source.getJSONArray("conversationStarters");
//				NPCStatements=new ArrayList<String>();
//				for(int i=0; i<JSONConversationStarters.length(); i++) {
//					try {
//						NPCStatements.add(JSONConversationStarters.getString(i));
//					} catch(JSONException ex) {NPCStatements.add(null);}
//				}
//			}
//			else {
//				//todo ?
//				conversationStarters=new ArrayList<String>();
//				conversationStarters.add("talk");
//			}
//			if(source.has("conversationRooms")) {
//				JSONArray JSONConversationRooms=source.getJSONArray("conversationRooms");
//				conversationRooms=new ArrayList<Integer>();
//				for(int i=0; i<JSONConversationRooms.length(); i++) {
//					conversationRooms.add(JSONConversationRooms.getInt(i));
//				}
//			}
//			else if(source.has("minRoom")&&source.has("maxRoom")) {
//				int max=source.getInt("maxRoom");
//				int min=source.getInt("minRoom");
//				conversationRooms=new ArrayList<Integer>(max-min);
//				for(int i=min; i<=max; i++)
//					conversationRooms.add(i);
//			}
//			else
//				isAnywhere=true;
//			JSONArray JSONNPCStatements=source.getJSONArray("NPCStatements");
//			NPCStatements=new ArrayList<String>();
//			for(int i=0; i<JSONNPCStatements.length(); i++) {
//				try {
//					NPCStatements.add(JSONNPCStatements.getString(i));
//				} catch(JSONException ex) {NPCStatements.add(null);}
//			}
//			JSONArray JSONPlayerPrompts=source.getJSONArray("playerPrompts");
//			playerPrompts=new ArrayList<String>();
//			for(int i=0; i<JSONPlayerPrompts.length(); i++) {
//				try {
//					playerPrompts.add(JSONPlayerPrompts.getString(i));
//				} catch(JSONException ex) {playerPrompts.add(null);}
//			}
//			JSONArray JSONAllCorrectResponses=source.getJSONArray("correctPlayerResponses");
//			correctPlayerResponses=new ArrayList<List<String>>();
//			for(int i=0; i<JSONAllCorrectResponses.length(); i++) {
//				JSONArray JSONCorrectResponses=JSONAllCorrectResponses.getJSONArray(i);
//				List<String> responses=new ArrayList<String>();
//				for(int j=0; j<JSONCorrectResponses.length(); j++)
//					responses.add(JSONCorrectResponses.getString(j));
//				correctPlayerResponses.add(responses);
//			}
//			JSONArray JSONIncorrectResponseEffects=source.getJSONArray("incorrectResponseEffects");
//			incorrectResponseEffects=new ArrayList<String>();
//			for(int i=0; i<JSONIncorrectResponseEffects.length(); i++) {
//				try {
//					incorrectResponseEffects.add(JSONIncorrectResponseEffects.getString(i));
//				} catch(JSONException ex){incorrectResponseEffects.add(null);}
//			}
//			JSONArray JSONPlayerStatements=source.getJSONArray("playerStatements");
//			playerStatements=new ArrayList<String>(JSONPlayerStatements.length());
//			for(int i=0; i<JSONPlayerStatements.length(); i++) {
//				try {
//					playerStatements.add(JSONPlayerStatements.getString(i));
//				} catch(JSONException ex){playerStatements.add(null);}
//			}
//			if(source.has("playerResponseEffects")) {
//				JSONArray JSONPlayerResponseEffects=source.getJSONArray("playerResponseEffects");
//				playerResponseEffects=new ArrayList<String>(JSONPlayerResponseEffects.length());
//				for(int i=0; i<JSONPlayerResponseEffects.length(); i++) {
//					try {
//						playerResponseEffects.add(JSONPlayerResponseEffects.getString(i));
//					} catch(JSONException ex){playerResponseEffects.add(null);}
//				}
//			}
//			if(source.has("effect"))
//				effect=source.getString("effect");
//			else
//				effect="";
//			if(source.has("enterToEndEffects")) {
//				JSONArray JSONEnterToEndEffects=source.getJSONArray("enterToEndEffects");
//				enterToEndEffects=new ArrayList<String>(JSONEnterToEndEffects.length());
//				for(int i=0; i<JSONEnterToEndEffects.length(); i++) {
//					try {
//						enterToEndEffects.add(JSONEnterToEndEffects.getString(i));
//					} catch(JSONException ex){enterToEndEffects.add(null);}
//				}
//			}
//		} catch(JSONException e) {e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
//	}
//
//	public JSONObject toJSONObject() {
//		JSONObject obj=new JSONObject();
//		try {
//			obj.put("title", title);
//			obj.put("reachable", isReachable);
//			for(Integer id:conversationRooms)
//				obj.accumulate("conversationRooms", id);
//			for(String starter:conversationStarters)
//				obj.accumulate("conversationStarters", starter);
//			for(String str:NPCStatements)
//				obj.accumulate("NPCStatements", str);
//			for(String str:playerPrompts)
//				obj.accumulate("playerPrompts", str);
//			for(List<String> list:correctPlayerResponses) {
//				JSONArray responses=new JSONArray();
//				for(String str:list)
//					responses.put(str);
//				obj.accumulate("correctPlayerResponses", responses);
//			}
//			if(playerResponseEffects!=null)
//				for(String str:playerResponseEffects)
//					obj.accumulate("playerResponseEffects", str);
//			for(String str:incorrectResponseEffects)
//				obj.accumulate("incorrectResponseEffects", str);
//			if(enterToEndEffects!=null)
//				for(String str:enterToEndEffects)
//					obj.accumulate("enterToEndEffects", str);
//			obj.put("effect", effect);
//		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
//		return obj;
//	}
//	
//	public boolean isInRoom(int roomNumber) {
//		return (conversationRooms!=null&&conversationRooms.contains(roomNumber))||isAnywhere;
//	}
//	
//	public String getTitle() {
//		return title;
//	}
//
//	public String getEffect() {
//		return effect;
//	}
//	
//	public boolean isReachable() {
//		return isReachable;
//	}
//	
//	public List<String> getNextConversations() {
//		return nextConversations;
//	}
//	
//	public List<String> getEliminatedConversations() {
//		return eliminatedConversations;
//	}
//
//	public List<String> getNPCStatements() {
//		return NPCStatements;
//	}
//
//	public List<String> getPlayerPrompts() {
//		return playerPrompts;
//	}
//
//	public List<String> getCorrectPlayerResponses(int index) {
//		return correctPlayerResponses.get(index);
//	}
//
//	public List<String> getIncorrectResponseEffects() {
//		return incorrectResponseEffects;
//	}
//
//	public List<String> getPlayerStatements() {
//		return playerStatements;
//	}
//
//	public List<String> getPlayerResponseEffects() {
//		return playerResponseEffects;
//	}
//
//	public List<String> getConversationStarters() {
//		return conversationStarters;
//	}
//
//	public List<Integer> getConversationRooms() {
//		return conversationRooms;
//	}
//	
//	public boolean isAnywhere() {
//		return isAnywhere;
//	}
//	
//	public List<String> getEnterToEndEffects() {
//		return enterToEndEffects;
//	}
//
//	public void setEffect(String newEffect) {
//		effect=newEffect;
//	}
//	
//	public void setConversationStarters(List<String> newStarters) {
//		conversationStarters=newStarters;
//	}
//	
//	public void setReachable(boolean newReachable) {
//		isReachable = newReachable;
//	}
//}
package textadventure;
import java.io.*;
import java.util.*;

import org.json.*;

public class NPC extends TACharacter {
	private List<Conversation> conversations;
	private List<List<String>> conversationStarters;
	private List<Integer> conversationRooms;
	private int nextConversationNumber;

	public NPC(JSONObject source) {
		super(source);
		takeable=false;
		try {
			if(source.has("currentConversation"))
				nextConversationNumber=source.getInt("currentConversation");
			else
				nextConversationNumber=0;
			if(source.has("conversationStarters")) {
				JSONArray JSONAllConversationStarters=source.getJSONArray("conversationStarters");
				conversationStarters=new ArrayList<List<String>>();
				for(int i=0; i<JSONAllConversationStarters.length(); i++) {
					JSONArray JSONConversationStarters=JSONAllConversationStarters.getJSONArray(i);
					List<String> list=new ArrayList<String>(JSONConversationStarters.length());
					for(int j=0; j<JSONConversationStarters.length(); j++)
						list.add(JSONConversationStarters.getString(j));
					conversationStarters.add(list);
				}
				JSONArray JSONConversationRooms=source.getJSONArray("conversationRooms");
				conversationRooms=new ArrayList<Integer>(JSONConversationRooms.length());
				for(int i=0; i<JSONConversationRooms.length(); i++)
					conversationRooms.add(JSONConversationRooms.getInt(i));
			}
		} catch(JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		String fileName=name;
		if(adjective!=null)
			fileName=adjective+name;
		try {
			conversations=new ArrayList<Conversation>(conversationRooms.size());
			BufferedReader reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/npcs/"+fileName+".taf")));
			for(int i=0; i<=nextConversationNumber&&i<conversationRooms.size(); i++)
				conversations.add(new Conversation(new JSONObject(reader.readLine())));
			reader.close();
		} catch(Exception e) {e.printStackTrace(System.out);Main.game.getView().println("Something went wrong: "+e);}
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		String expected=verb+"|";
		if(otherObject==null)
			expected+="null||null|||";
		else {
			if(otherObject.getAdjective()==null)
				expected+="null||";
			else
				expected+=otherObject.getAdjective()+"||";
			expected+=otherObject.getName()+"|||";
		}
		expected+=thisIsDO;
		if(nextConversationNumber<conversationRooms.size()
				&&conversationRooms.get(nextConversationNumber)==room.getID()
				&&(conversationStarters.get(nextConversationNumber).contains(expected))) {
			doConversation(nextConversationNumber);
			return "";
		}
		for(int i=nextConversationNumber-1; i>=0; i--) {
			if(conversationRooms.get(i)==room.getID()&&(conversationStarters.get(i).contains(expected)||(verb.equals("talk")&&thisIsDO))) {
				doConversation(i);
				return "";
			}
		}
		return null;
		//check each Conversation's conversationStarters to see if the NPC should talk
	}

	private void doConversation(final int number) {
		final Conversation conversation=conversations.get(number);
		if(conversation.getNPCStatements().get(0)!=null)
			Main.game.getView().printlnNPC("\n"+getFullName()+": \""+conversation.getNPCStatements().get(0)+"\"");
		int i;
		for(i=0; conversation.getPlayerPrompts().get(i)==null; i+=0) {
			if(conversation.getPlayerStatements().get(i)!=null)
				Main.game.getView().printlnNPC("\nYou: \""+conversation.getPlayerStatements().get(i)+"\"");
			i++;
			if(conversation.getNPCStatements().get(i)!=null)
				Main.game.getView().printlnNPC("\n"+getFullName()+": \""+conversation.getNPCStatements().get(i)+"\"");
		}
		Main.game.getView().printNPC("\nYou: \""+conversation.getPlayerPrompts().get(i));
		final boolean parentheses=conversation.getPlayerPrompts().get(i).charAt(0)=='(';
		final int index=i;
		GameListener listener=new GameListener() {
			private int conversationIndex=index;
			private boolean isInParentheses=parentheses;

			public void textTyped(String text) {
				if(isInParentheses)
					Main.game.getView().println(")");
				else
					Main.game.getView().print("\"");
				Main.game.getView().printlnNPC();
				text=text.trim()/*.toLowerCase()*/;
				while(text.contains("  "))
					text=text.replaceAll("  ", " ");
				int lastIndex=text.length();
				if(!text.equals("")) {
					for(int i=text.length()-1; !(text.charAt(i)>='a'&&text.charAt(i)<='z')&&!(text.charAt(i)>='0'&&text.charAt(i)<='9')&&!(text.charAt(i)>='A'&&text.charAt(i)<='Z'); i--)
						lastIndex--;
					int firstIndex=0;
					for(int i=0; !(text.charAt(i)>='a'&&text.charAt(i)<='z')&&!(text.charAt(i)>='0'&&text.charAt(i)<='9')&&!(text.charAt(i)>='A'&&text.charAt(i)<='Z'); i++)
						firstIndex++;
					text=text.substring(firstIndex, lastIndex);
				}
				if(!conversation.getCorrectPlayerResponses(conversationIndex).contains(text.toLowerCase())&&conversation.getCorrectPlayerResponses(conversationIndex).size()!=0) {
					Main.game.getView().printlnNPC("\n"+getFullName()+": \""+conversation.getNPCIncorrectResponses().get(conversationIndex)+"\"");
					Main.game.getView().setGameListener(null);
					return;
				}
				else {
					if(conversation.getPlayerResponseEffects()!=null&&conversation.getPlayerResponseEffects().get(conversationIndex)!=null) {
						String effectsStr=conversation.getPlayerResponseEffects().get(conversationIndex);
						StringTokenizer tokenizer=new StringTokenizer(effectsStr, "}{");
						List<String> effects=new ArrayList<String>(tokenizer.countTokens());
						while(tokenizer.hasMoreTokens())
							effects.add(tokenizer.nextToken().replaceAll("response", text));
						for(String str:effects)
							Main.game.getCommandParser().parse(str);
					}
					if(conversation.getPlayerStatements().get(conversationIndex)!=null)
						Main.game.getView().printlnNPC("\nYou: \""+conversation.getPlayerStatements().get(conversationIndex)+"\"");
					conversationIndex++;
					if(conversationIndex==conversation.getNPCStatements().size()) {
						processEffect(number);
						return;
					}
					if(conversation.getNPCStatements().get(conversationIndex)!=null)
						Main.game.getView().printlnNPC("\n"+getFullName()+": \""+conversation.getNPCStatements().get(conversationIndex)+"\"");
					while(conversationIndex<conversation.getPlayerPrompts().size()&&conversation.getPlayerPrompts().get(conversationIndex)==null) {
						if(conversation.getPlayerStatements().get(conversationIndex)!=null)
							Main.game.getView().printlnNPC("\nYou: \""+conversation.getPlayerStatements().get(conversationIndex)+"\"");
						conversationIndex++;
						if(conversationIndex==conversation.getNPCStatements().size()) {
							processEffect(number);
							return;
						}
						if(conversation.getNPCStatements().get(conversationIndex)!=null)
							Main.game.getView().printlnNPC("\n"+getFullName()+": \""+conversation.getNPCStatements().get(conversationIndex)+"\"");
					}
					if(conversationIndex<conversation.getPlayerPrompts().size()) {
						Main.game.getView().printNPC("\nYou: \""+conversation.getPlayerPrompts().get(conversationIndex));
						isInParentheses=conversation.getPlayerPrompts().get(conversationIndex).charAt(0)=='(';
					}
				}
			}

		};
		Main.game.getView().setGameListener(listener);
	}

	private void processEffect(int number) {
		//only called when the conversation was completed
		Main.game.getView().setGameListener(null);
		Main.game.getView().println();
		if(number!=nextConversationNumber)
			return;
		nextConversationNumber++;
		String fileName=name;
		if(adjective!=null)
			fileName=adjective+name;
		try {
			//read in the next Conversation
			BufferedReader reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/npcs/"+fileName+".taf")));
			for(int i=0; i<nextConversationNumber; i++)
				reader.readLine();
			String conversationLine=reader.readLine();
			if(conversationLine!=null)
				conversations.add(new Conversation(new JSONObject(conversationLine)));
			reader.close();
		} catch(Exception e) {Main.game.getView().println("Something went wrong: "+e);}
		String effect=conversations.get(number).getEffect();
		if(effect.equals(""))
			return;
		if(effect!=null) {
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

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			if(nextConversationNumber!=0)
				obj.put("currentConversation", nextConversationNumber);
			JSONArray JSONAllConversationStarters=new JSONArray();
			for(List<String> list:conversationStarters) {
				JSONArray JSONConversationStarters=new JSONArray();
				for(String str:list)
					JSONConversationStarters.put(str);
				JSONAllConversationStarters.put(JSONConversationStarters);
			}
			obj.put("conversationStarters", JSONAllConversationStarters);
			JSONArray JSONConversationRooms=new JSONArray();
			for(Integer i:conversationRooms)
				JSONConversationRooms.put(i);
			obj.put("conversationRooms", JSONConversationRooms);
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public List<String> go(int direction) {
		Room newRoom=room.getAdjacent(direction);
		if(newRoom==null)
			return null;
		room.remove(this);
		room=newRoom;
		room.add(this);
		for(TACharacter c:followingCharacters)
			c.setRoom(room);
		return null;
	}
	
	public void attack(TACharacter defender) {
		//TODO
	}

	public int getCurrentConversationNumber() {
		return nextConversationNumber;
	}

	public Conversation getConversation(int number) {
		return conversations.get(number);
	}

	public boolean isHostile() {
		return proximity>=0;
	}
}

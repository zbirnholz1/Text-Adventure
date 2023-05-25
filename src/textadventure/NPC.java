package textadventure;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.json.*;

public class NPC extends TACharacter {
	private List<Conversation> conversations;
	private String deathEffect; //for when the player wins the battle
	private String lossEffect; //for when the player loses the battle
	private String battleMusicName;
	private String genderPronoun; //i.e. he, she, or it
	private String genderAdjective; //i.e. his, her, or its
	private List<Weapon> attackOrder;
	private int turnNumber; //an index for attackOrder
	private Conversation currentConversation;
	//TODO private String AI; would dictate the probability or specific order of attacks (e.g. saying that the King will use psychic on the 3rd turn)

	public NPC(JSONObject source) {
		super(source);
		takeable=false;
		try {
			if(proximity>0) {
				HP=source.getInt("HP");
				strength=source.getInt("strength");
				intelligence=source.getInt("intelligence");
				speed=source.getInt("speed");
				if(source.has("armor"))
					armor=new Armor(source.getJSONObject("armor"));
				if(source.has("deathEffect"))
					deathEffect=source.getString("deathEffect");
				if(source.has("lossEffect"))
					lossEffect=source.getString("lossEffect");
				if(source.has("battleMusicName"))
					battleMusicName=source.getString("battleMusicName");
				if(source.has("attackOrder")) {
					JSONArray JSONAttackOrder=source.getJSONArray("attackOrder");
					attackOrder=new ArrayList<Weapon>();
					for(int i=0; i<JSONAttackOrder.length(); i++) {
						for(TAObject o:inventory) {
							if(o instanceof Weapon&&(o.getName().equals(JSONAttackOrder.getString(i))||o.getFullName().equals(JSONAttackOrder.getString(i))))
								attackOrder.add((Weapon)o);
						}
					}
					turnNumber=0;
				}
			}
			if(source.has("genderPronoun"))
				genderPronoun=source.getString("genderPronoun");
			else
				genderPronoun="it";
			if(genderPronoun.equals("he"))
				genderAdjective="his";
			else if(genderPronoun.equals("she"))
				genderAdjective="her";
			else
				genderAdjective="its";
		} catch(JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		String fileName=name;
		if(adjective!=null)
			fileName=adjective+name;
		try {
			conversations=new ArrayList<Conversation>();
			BufferedReader reader;
			try {
				reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/npcs/"+fileName+".taf")));
			} catch(Exception e) {
				Main.game.getView().println("Something went wrong loading the conversations for NPC "+name+": "+e);
				e.printStackTrace(System.out);
				return;
			}
			while(reader.ready()) {
				String line=reader.readLine();
				if (line.isEmpty() || line.trim().startsWith("#")) { //to allow for nice spacing and comments
					continue;
				}
				String JSONString=line;
				while(line!=null && (!line.endsWith("}") || Character.isWhitespace(line.charAt(0)))) { //continue reading lines until the end of the object
					line=reader.readLine();
					if (line.trim().isEmpty() || line.trim().startsWith("#")) {
						continue;
					}
					JSONString+=line.trim();
				}
				Conversation conversation = new Conversation(new JSONObject(JSONString));
				//add a default conversation starter in case none were provided in the JSON
				if(conversation.getConversationStarters().isEmpty()) {
					conversation.getConversationStarters().add("talk "+this.getFullName());
				}
				conversations.add(conversation);
			}
			reader.close();
		} catch(Exception e) {e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
	}

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			if(proximity>0) {
				obj.put("HP", HP);
				obj.put("strength", strength);
				obj.put("intelligence", intelligence);
				obj.put("speed", speed);
				obj.put("armor", armor.toJSONObject());
				if(deathEffect!=null)
					obj.put("deathEffect", deathEffect);
				if(lossEffect!=null)
					obj.put("lossEffect", lossEffect);
			}
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		String superProcess=super.process(verb, otherObject, thisIsDO);
		if(superProcess!=null)
			return superProcess;
		if(conversations==null||conversations.size()==0)
			return null;
		final List<Conversation> primeCandidates=new ArrayList<Conversation>();
		List<Conversation> secondaryCandidates=new ArrayList<Conversation>();
		for(int i=0; i<conversations.size(); i++) {
			if(conversations.get(i).isReachable()) {
				if(conversations.get(i).isAnywhere())
					secondaryCandidates.add(conversations.get(i));
				else if(conversations.get(i).isInRoom(room.getID()))
					primeCandidates.add(conversations.get(i));
			}
		}
		String otherObjectString;
		if(otherObject==null)
			otherObjectString="";
		else {
			if(otherObject.getAdjective()==null)
				otherObjectString="";
			else
				otherObjectString=otherObject.getAdjective();
			otherObjectString+=" "+otherObject.getName();
		}
		
		if(verb.equals("talk") && thisIsDO && primeCandidates.size() > 1) {
			/* if there is more than one prime candidate that has already been completed or
			 * that can be started with a simple "talk NPCName", then list all of the options
			 * for conversation and prompt the player to choose a topic to talk about. */
			Main.game.getView().println("What would you like to talk to "+this.getFullName()+" about? (Choose 1-"+primeCandidates.size()+")\n");
			for(int i = 0; i < primeCandidates.size(); i++) {
				Main.game.getView().println("    " + (i+1) + ". " + primeCandidates.get(i).getTitle());
			}
			Main.game.getView().print("\n>");
			Main.game.getView().setGameListener(new GameListener() {
				private List<Conversation> candidates = primeCandidates;

				public void textTyped(String text) {
					Main.game.getView().println("\n");
					text = text.trim();
					//check if the user selected an index
					boolean isInteger = !text.isEmpty();
					for(int i = 0; i < text.length(); i++) {
						if(!Character.isDigit(text.charAt(i))) {
							if(text.charAt(i) == '.' && i == text.length() - 1 && text.length() > 1) {
								//if they typed the number of a choice with a period at the end,
								//remove the period from their input but still count it as an integer
								text = text.substring(0, i);
								isInteger = true;
							} else {
								isInteger = false;
							}
							break;
						}
					}
					Conversation selectedConversation = null;
					if(isInteger) {
						int selectedIndex = Integer.parseInt(text);
						if(selectedIndex <= candidates.size()) {
							selectedConversation = candidates.get(selectedIndex-1);
						} else {
							Main.game.getView().println("Nobody knows what you mean by "+selectedIndex+".\n");
							Main.game.getView().print(">");
							return;
						}
					} else {
						//check if they selected one of the conversation titles
						for(Conversation potentialConversation : candidates) {
							if(text.equalsIgnoreCase(potentialConversation.getTitle())) {
								selectedConversation = potentialConversation;
								break;
							}
						}
					}
					Main.game.getView().setGameListener(null);
					//either they selected a valid conversation or they didn't
					if(selectedConversation != null) {
						selectedConversation.doConversation(NPC.this);
					} else {
						Main.game.getCommandParser().parse(text, true);
						if(Main.game.getView().getGameListener() == null) {
							Main.game.getView().println();
						}
					}
				}
				
			});
			return "{_donothing}";
		}
		
		Conversation conversation=null;
		for(Conversation c:primeCandidates) {
			for(String starter:c.getConversationStarters()) {
				boolean works=starter.equals(verb)&&thisIsDO;
				if(!works) {
					if(thisIsDO)
						works=starter.equalsIgnoreCase(verb+" "+this.getFullName());
					else
						works=starter.equalsIgnoreCase(verb+" "+otherObjectString+" "+this.getFullName());
				}
				if(works) {
					conversation=c;
					break;
				}
			}
			if(conversation!=null)
				break;
		}
		if(conversation==null) {
			//for random comments about the scenery, etc. e.g. the half-elf saying "these grapes look tasty" or something
			List<Conversation> randomSelection=new ArrayList<Conversation>();
			for(Conversation c:secondaryCandidates) {
				for(String starter:c.getConversationStarters()) {
					//check each Conversation's conversationStarters to see if the NPC should talk
					boolean works=starter.equals(verb)&&thisIsDO;
					if(!works) {
						if(thisIsDO)
							works=starter.equalsIgnoreCase(verb+" "+this.getFullName());
						else
							works=starter.equalsIgnoreCase(verb+" "+otherObjectString+" "+this.getFullName());
					}
					if(works) {
						randomSelection.add(c);
					}
				}
			}
			if(randomSelection.size()!=0)
				conversation=randomSelection.get(((int)Math.random())*randomSelection.size());
		}
		if(conversation!=null) {
			conversation.doConversation(this);
			return "{_donothing}";
		}
		return null;
	}

/**
 * Old version of the conversation process--now implemented as a directed graph traversal
 * within the Conversation class. Syntax is myConversation.doConversation();
 */
//	private void doConversation(final Conversation conversation) {
//		currentConversation=conversation;
//		if(conversation.getNPCStatements().get(0)!=null)
//			Main.game.getView().printlnNPC("    "+getFullName()+": \""+conversation.getNPCStatements().get(0)+"\"");
//		int i;
//		for(i=0; conversation.getPlayerPrompts().get(i)==null; i+=0) {
//			if(conversation.getPlayerStatements().get(i)!=null)
//				Main.game.getView().printlnNPC("\n    You: \""+conversation.getPlayerStatements().get(i)+"\"");
//			i++;
//			if(conversation.getNPCStatements().get(i)!=null)
//				Main.game.getView().printlnNPC("\n    "+getFullName()+": \""+conversation.getNPCStatements().get(i)+"\"");
//		}
//		Main.game.getView().printNPC("\n    You: \""+conversation.getPlayerPrompts().get(i));
//		final boolean parentheses=conversation.getPlayerPrompts().get(i).charAt(0)=='(';
//		final int index=i;
//		GameListener listener=new GameListener() {
//			private int conversationIndex=index;
//			private boolean isInParentheses=parentheses;
//			private boolean redo=false;
//			private boolean enterToEnd=false;
//
//			public void textTyped(String text) {
//				redo=false;
//				enterToEnd=false;
//				if (isInParentheses && !text.endsWith(")")) {
//					Main.game.getView().println(")");
//				}
//				else if (!text.endsWith("\"")){
//					Main.game.getView().print("\"");
//				}
//				Main.game.getView().printlnNPC();
//				text=text.trim()/*.toLowerCase()*/;
//				while(text.contains("  "))
//					text=text.replaceAll("  ", " ");
//				int lastIndex=text.length();
//				if(!text.equals("")) {
//					for(int i=text.length()-1; !(text.charAt(i)>='a'&&text.charAt(i)<='z')&&!(text.charAt(i)>='0'&&text.charAt(i)<='9')&&!(text.charAt(i)>='A'&&text.charAt(i)<='Z'); i--)
//						lastIndex--;
//					int firstIndex=0;
//					for(int i=0; !(text.charAt(i)>='a'&&text.charAt(i)<='z')&&!(text.charAt(i)>='0'&&text.charAt(i)<='9')&&!(text.charAt(i)>='A'&&text.charAt(i)<='Z'); i++)
//						firstIndex++;
//					text=text.substring(firstIndex, lastIndex);
//				}
//				if(!isCorrectResponse(text.toLowerCase(), conversation, conversationIndex)) { //if it was an INCORRECT response
//					//todo rework this to make it so the effect is more versatile--the player could say something, the NPC could say something, or the player could get another chance: {redo}
//					//did I already do that? {redo} is definitely already done
//					String effect=conversation.getIncorrectResponseEffects().get(conversationIndex);
//					if(effect!=null) {
//						StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
//						Queue<String> queue=new LinkedList<String>();
//						while(tokenizer.hasMoreTokens())
//							queue.add(tokenizer.nextToken());
//						char firstCharOfLastToken=' ';
//						while(!queue.isEmpty()) {
//							String toProcess=queue.poll();
//							if(toProcess.charAt(0)=='[') {
//								String toPrint=queue.poll();
//								boolean shouldAddEndQuote=false;
//								if(toProcess.startsWith("You:")||toProcess.startsWith("NPC")||toProcess.startsWith(getFullName())) {
//									toProcess="    \""+toProcess.replace("NPC", getFullName());
//									shouldAddEndQuote=true;
//								}
//								while(queue.peek().charAt(0)!=']')
//									toPrint+=queue.poll();
//								if(shouldAddEndQuote)
//									toPrint+="\"";
//								Main.game.getView().printlnNPC("\n"+toPrint+"\n", false);
//								continue;
//							}
//							if(firstCharOfLastToken=='[') {
//								if(toProcess.startsWith("You:")||toProcess.startsWith("NPC")||toProcess.startsWith(getFullName()))
//									toProcess="    \""+toProcess.replace("NPC", getFullName())+"\"";
//								else if(toProcess.startsWith("(You:")||toProcess.startsWith("\""))
//									toProcess="    "+toProcess;
//								Main.game.getView().printlnNPC(toProcess+"\n", false);
//							}
//							else if(firstCharOfLastToken=='{') {
//								if(toProcess.equals("redo")) {
//									redo=true;
//									enterToEnd=true;
//									if(text.equals("")) {
//										Main.game.getView().println();
//										break;
//									}
//								}
//								else if(toProcess.equalsIgnoreCase("redoWithEnter")) {
//									redo=true;
//								}
//								else
//									Main.game.getCommandParser().parse(toProcess);
//							}
//							firstCharOfLastToken=toProcess.charAt(0);
//						}
//					} else {
//						Main.game.getView().printlnNPC("Your response leaves "+NPC.this.getFullName()+" speechless.\n");
//					}
//					//Main.game.getView().printlnNPC("\n    "+getFullName()+": \""+conversation.getIncorrectResponseEffects().get(conversationIndex)+"\"\n");
//					if(enterToEnd&&text.equals("")) { //if they just pressed enter
//						Main.game.getView().printlnNPC("With nothing to say, you decide to hold your tongue.");
//						if(conversation.getEnterToEndEffects()!=null&&conversation.getEnterToEndEffects().get(conversationIndex)!=null) {
//							StringTokenizer tokenizer=new StringTokenizer(conversation.getEnterToEndEffects().get(conversationIndex), "][}{", true);
//							char firstCharOfLastToken=' ';
//							while(tokenizer.hasMoreTokens()) {
//								String toProcess=tokenizer.nextToken();
//								if(firstCharOfLastToken=='[')
//									Main.game.getView().printlnNPC(toProcess);
//								else if(firstCharOfLastToken=='{') {
//									Main.game.getCommandParser().parse(toProcess);
//								}
//								firstCharOfLastToken=toProcess.charAt(0);
//							}
//						}
//						Main.game.getView().printlnNPC();
//						Main.game.getView().setGameListener(null);
//						if(Main.game.getSoundPlayer().soundIsOn()) {
//							Main.game.getSoundPlayer().stop(true);
//							if(Main.game.getPlayer().getRoom().getSoundName()!=null)
//								Main.game.getSoundPlayer().loop(Main.game.getPlayer().getRoom().getSoundName(), SoundPlayer.OFFSETS.get(Main.game.getPlayer().getRoom().getSoundName()), true);
//						}
//						return;
//					}
//					else if(redo) {
//						//conversationIndex--;
//						if(!enterToEnd&&text.equals("")) {
//							Main.game.getView().printlnNPC("Unfortunately it looks like staying quiet won't get you off the hook here.\n");
//						}
//						Main.game.getView().printNPC("    You: \""+conversation.getPlayerPrompts().get(conversationIndex));
//						return;
//					}
//					else {
//						Main.game.getView().setGameListener(null);
//						if(Main.game.getSoundPlayer().soundIsOn()) {
//							Main.game.getSoundPlayer().stop(true);
//							if(Main.game.getPlayer().getRoom().getSoundName()!=null) {
//								Main.game.getSoundPlayer().loop(Main.game.getPlayer().getRoom().getSoundName(), SoundPlayer.OFFSETS.get(Main.game.getPlayer().getRoom().getSoundName()), true);
//							}
//						}
//						return;
//					}
//				}
//				else { //if it was a CORRECT response
//					if(conversation.getPlayerResponseEffects()!=null&&conversation.getPlayerResponseEffects().get(conversationIndex)!=null) {
//						//process the effect from the response
//						String effectsStr=conversation.getPlayerResponseEffects().get(conversationIndex);
//						StringTokenizer tokenizer=new StringTokenizer(effectsStr, "}{");
//						List<String> effects=new ArrayList<String>(tokenizer.countTokens());
//						while(tokenizer.hasMoreTokens())
//							effects.add(tokenizer.nextToken().replaceAll("response", text));
//						for(String str:effects)
//							Main.game.getCommandParser().parse(str);
//					}
//					if(conversation.getPlayerStatements().get(conversationIndex)!=null) {
//						if(conversation.getPlayerPrompts().get(conversationIndex)!=null)
//							Main.game.getView().printlnNPC();
//						Main.game.getView().printlnNPC("    You: \""+conversation.getPlayerStatements().get(conversationIndex)+"\"");
//					}
//					conversationIndex++;
//					if(conversationIndex==conversation.getNPCStatements().size()) {
//						processEffect(conversation);
//						return;
//					}
//					if(conversation.getNPCStatements().get(conversationIndex)!=null)
//						Main.game.getView().printlnNPC("\n    "+getFullName()+": \""+conversation.getNPCStatements().get(conversationIndex)+"\"");
//					while(conversationIndex<conversation.getPlayerPrompts().size()&&conversation.getPlayerPrompts().get(conversationIndex)==null) {
//						//continue the conversation either until it ends or until the player actually has to input something again
//						if(conversation.getPlayerStatements().get(conversationIndex)!=null)
//							Main.game.getView().printlnNPC("\n    You: \""+conversation.getPlayerStatements().get(conversationIndex)+"\"");
//						conversationIndex++;
//						if(conversationIndex==conversation.getNPCStatements().size()) {
//							//when the conversation reaches its end
//							processEffect(conversation);
//							return;
//						}
//						if(conversation.getNPCStatements().get(conversationIndex)!=null)
//							Main.game.getView().printlnNPC("\n    "+getFullName()+": \""+conversation.getNPCStatements().get(conversationIndex)+"\"");
//					}
//					if(conversationIndex<conversation.getPlayerPrompts().size()) {
//						Main.game.getView().printNPC("\n    You: \""+conversation.getPlayerPrompts().get(conversationIndex));
//						isInParentheses=conversation.getPlayerPrompts().get(conversationIndex).charAt(0)=='(';
//					}
//				}
//			}
//		};
//		Main.game.getView().setGameListener(listener);
//	}
//	
//	private boolean isCorrectResponse(String response, Conversation conversation, int index) {
//		if(conversation.getCorrectPlayerResponses(index).isEmpty()) return true;
//		if(response == null) return false;
//		
//		for(String acceptableResponse : conversation.getCorrectPlayerResponses(index)) {
//			//decide whether response is a permutation of acceptableResponse when including * wildcards, e.g. "map*" is one for "mapmaking" and "*map*" is one for "making maps".
//			//* can also count as "" (blank wildcard).
//			String regexAcceptableResponse = acceptableResponse.replace("*", ".*");
//			if(Pattern.matches(regexAcceptableResponse, response)) return true;
//		}
//		return false;
//	}
//
//	private void processEffect(Conversation conversation) {
//		//only called when the conversation was completed
//		Main.game.getView().setGameListener(null);
//		Main.game.getView().printlnNPC("", false);
//		
//		String effect=conversation.getEffect();
//		if(effect==null)
//			return;
//		
//		//make the next conversations reachable (only if this conversation hasn't been completed,
//		//which is why this takes place after the return above
//		for(String nextConversationTitle : conversation.getNextConversations()) {
//			for(Conversation c : conversations) {
//				if(nextConversationTitle.equalsIgnoreCase(c.getTitle())) {
//					c.setReachable(true);
//					break;
//				}
//			}
//		}
//		//and make the eliminated conversations unreachable
//		for(String eliminatedConversationTitle : conversation.getEliminatedConversations()) {
//			for(Conversation c : conversations) {
//				if(eliminatedConversationTitle.equalsIgnoreCase(c.getTitle())) {
//					c.setReachable(false);
//					break;
//				}
//			}
//		}
//
//		//process the effect
//		if(effect!=null && !effect.isEmpty()) {
//			StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
//			char firstCharOfLastToken=' ';
//			while(tokenizer.hasMoreTokens()) {
//				String toProcess=tokenizer.nextToken();
//				if(firstCharOfLastToken=='[')
//					Main.game.getView().printlnNPC(toProcess+"\n", false);
//				else if(firstCharOfLastToken=='{') {
//					Main.game.getCommandParser().parse(toProcess);
//				}
//				firstCharOfLastToken=toProcess.charAt(0);
//			}
//		}
//		conversation.setEffect(null); //don't allow for repeat effects
//		if(Main.game.getSoundPlayer().soundIsOn()&&!Main.game.getSoundPlayer().musicIsEverywhere()&&!Main.game.getSoundPlayer().getCurrentSoundName().equals(Main.game.getPlayer().getRoom().getSoundName())) {
//			Main.game.getSoundPlayer().stop(true);
//			if(Main.game.getPlayer().getRoom().getSoundName()!=null) {
//				Main.game.getSoundPlayer().loop(Main.game.getPlayer().getRoom().getSoundName(), SoundPlayer.OFFSETS.get(Main.game.getPlayer().getRoom().getSoundName()), true);
//			}
//		}
//	}

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
		Weapon attack=selectWeapon();
		int damage=BattleCalculator.calculateDamage(this, defender, attack);
		String wait="";
		//if(!room.getCharactersBySpeed().get(0).equals(this))
		wait="{1000}";
		if(damage==0)
			Main.game.getView().printlnNPC(wait+getFullName()+" misses you with "+genderAdjective+" "+attack.getFullName()+".");
		else{
			if(damage<0)
				Main.game.getView().printlnNPC(wait+getFullName()+" attacks you with "+genderAdjective+" "+attack.getFullName()+" and lands a critical hit!\n"+getCapitalizedGenderPronoun()+" deals "+Math.min(Math.abs(damage), Main.game.getPlayer().getHP())+" damage.");
			else
				Main.game.getView().printlnNPC(wait+getFullName()+" attacks you with "+genderAdjective+" "+attack.getFullName()+" and deals "+Math.min(Math.abs(damage), Main.game.getPlayer().getHP())+" damage.");
			int playerHP=Main.game.getPlayer().getHP();
			if(damage>=Main.game.getPlayer().getHP()&&lossEffect!=null) {
				StringTokenizer tokenizer=new StringTokenizer(lossEffect, "][}{", true);
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
			if(playerHP==Main.game.getPlayer().getHP()) //if they are not equal, the player's health was restored because they lost a non-loss battle (e.g. the villager)
				Main.game.getPlayer().takeDamage(damage);
		}
	}

	private Weapon selectWeapon() {
		if(attackOrder!=null&&attackOrder.get(turnNumber%attackOrder.size())!=null) {
			return attackOrder.get(turnNumber%attackOrder.size());
		}
		else {
			List<Weapon> weapons=getWeapons();
			List<Weapon> melee=new ArrayList<Weapon>();
			List<Weapon> ranged=new ArrayList<Weapon>();
			for(Weapon w:weapons) {
				if(w.isMelee())
					melee.add(w);
				else
					ranged.add(w);
			}
			try {
				double random=Math.random();
				if(proximity==1)
					return melee.get((int)(Math.random()*melee.size()));
				else if(proximity==2) {
					if(random<0.6)
						return melee.get((int)(Math.random()*melee.size()));
					else
						return ranged.get((int)(Math.random()*ranged.size()));
				}
				else if(proximity==3) {
					if(random<0.2)
						return melee.get((int)(Math.random()*melee.size()));
					else
						return ranged.get((int)(Math.random()*ranged.size()));
				}
				else //if(proximity>=4)
					return ranged.get((int)(Math.random()*ranged.size()));
			} catch(NullPointerException e) {
				return weapons.get((int)(Math.random()*weapons.size()));
			}
		}
	}

	public void takeDamage(int amount) {
		super.takeDamage(amount);
		if(HP==0) {
			if(deathEffect!=null) {
				StringTokenizer tokenizer=new StringTokenizer(deathEffect, "][}{", true);
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
			if(Main.game.getPlayer().getRoom().getHostileCharacters().size()==1&&Main.game.getSoundPlayer().soundIsOn()&&!Main.game.getSoundPlayer().musicIsEverywhere()&&!Main.game.getSoundPlayer().getCurrentSoundName().equals(Main.game.getPlayer().getRoom().getSoundName())) {
				final javax.swing.Timer timer=new javax.swing.Timer(50, null);
				ActionListener actionListener=new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						synchronized(Main.game.getView().getPauseQueue()) {
							if(!Main.game.getView().getPauseQueue().isEmpty()) {
								return;
							}
						}
						timer.stop();
						Main.game.getSoundPlayer().stop(true);
						if(Main.game.getPlayer().getRoom().getSoundName()!=null)
							Main.game.getSoundPlayer().loop(Main.game.getPlayer().getRoom().getSoundName(), SoundPlayer.OFFSETS.get(Main.game.getPlayer().getRoom().getSoundName()), true);
					}
				};
				timer.addActionListener(actionListener);
				timer.start();
			}
		}
	}

	public Conversation getConversation(int number) {
		return conversations.get(number);
	}

	public Conversation getCurrentConversation() {
		return currentConversation;
	}
	
	public void setCurrentConversation(Conversation newConversation) {
		currentConversation = newConversation;
	}

	public String getDeathEffect() {
		return deathEffect;
	}

	public String getBattleMusicName() {
		return battleMusicName;
	}

	public String getGenderPronoun() {
		return genderPronoun;
	}

	public String getCapitalizedGenderPronoun() {
		return Character.toUpperCase(genderPronoun.charAt(0))+genderPronoun.substring(1);
	}

	public String getGenderAdjective() {
		return genderAdjective;
	}

	public String getFullName() {
		if(genderPronoun.equals("it"))
			return "The "+super.getFullName();
		return super.getFullName();
	}

	public String getFullNameLowerCase() {
		if(genderPronoun.equals("it"))
			return "the "+super.getFullName();
		return super.getFullName();
	}
	
	public List<Conversation> getConversations() {
		return conversations;
	}
}

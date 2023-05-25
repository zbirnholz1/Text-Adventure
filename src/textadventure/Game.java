package textadventure;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.awt.event.*;

import org.json.JSONObject;

import textadventure.Conversation.Vertex;

public class Game {
	private RoomBlock block;
	private Player player;
	private CommandParser parser;
	private View view;
	private SoundPlayer soundPlayer;

	public static final String GO_DIRECTIONS="NorthSouthEast West Up   Down ";

	public static final String supportPath=initializeSupportPath();

	public Game() {
		view=new View();
		File tempSave=new File(supportPath+"saves/temp.taf");
		tempSave.mkdirs();
		deleteDirectoryContents(supportPath+"saves/temp.taf");
		//RoomBlock.initializeBlockLocations();
		block=new RoomBlock(0, "temp");
		parser=new CommandParser();
		player=new Player();
		soundPlayer=new SoundPlayer();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				deleteDirectoryContents(supportPath+"saves/temp.taf");
				new File(supportPath+"saves/temp.taf").delete();
				if(new File(supportPath+"saves/temp2.taf").exists()) {
					deleteDirectoryContents(supportPath+"saves/temp2.taf");
					new File(supportPath+"saves/temp2.taf").delete();
				}
			}
		});
	}

	public static String initializeSupportPath() {
		String home=System.getProperty("user.home");
		String os=System.getProperty("os.name").toLowerCase();
		if(os.contains("mac"))
			home+="/Library/Application Support/Text Adventure/";
		//TODO figure out a support path for Windows
		return home;
	}

	public void play() {
		view.println(getStartText());
		soundPlayer.setSoundIsOn(true);
		//soundPlayer.loop("titletheme.mp3", false); //TODO pick a new title theme
		view.print("Would you like to load a saved game (yes or no)? ");
		player.setRoom(block.getFirstRoom());
		view.setGameListener(new GameListener() {
			public void textTyped(String yn) {
				yn=yn.toLowerCase();
				if(yn!=null&&(yn.equals("y")||yn.equals("yes"))) {
					view.print("\n\nPlease enter the name of the saved game: ");
					view.setGameListener(new GameListener() {
						public void textTyped(String text) {
							view.print("#");
							//Room originalRoom=player.getRoom();
							parser.parse("load "+text);
							view.println();
							view.setGameListener(null);
							if(player.getNumMoves()==0) {
								view.println("Starting over{500}.{500}.{500}.{500}\n");
								play();
							}
						}
					});
				}
				else {
					view.print("#");
					start();
				}
			}
		});
	}

	private String getStartText() {
		return "Text Adventure (will change to real name later)\n" +
				"Copyright \u00a9 2016 Zachary Birnholz\n\n" +
				"Story by Zachary Birnholz and Jonathan Burns\n" +
				"Programmed by Zachary Birnholz\n" + 
				//"Battle system designed by Daniel Palumbo\n" +
				//"Battle system enhanced by Zachary Birnholz and Jonathan Burns\n" +
				"Music credits will appear in the upper left corner throughout the game.\n" +
				"All music is licensed under Creative Commons.\n\n";
	}

	public void start() {
		view.setGameListener(null);
		player.setRoom(block.getFirstRoom());
		view.println(getStartText());
		//view.println("Welcome to Text Adventure! Regardless of whether you're a seasoned veteran of text adventures or a new player, it is recommended that you type \"help\" for the basics of how to play once the game begins. Have fun, and thanks for playing!\n\n");
		view.print("Please enter your name: ");
		view.setGameListener(new GameListener() {
			public void textTyped(String name) {
				if(name.equals(""))
					view.print("\n\nI didn't catch your name.\nPlease type it here: ");
				else if(name.equals("me"))
					view.print("\n\nVery funny.\nPlease enter your name: ");
				else {
					parser.parse("_setfield me name:"+name);
					view.setGameListener(null);
					view.updateStatsText();
					view.printNPC("\n\nWelcome to Text Adventure, {playerName}! Would you like to learn the basics of how to play (yes or no)? ");
					view.setGameListener(new GameListener() {
						public void textTyped(String yn) {
							yn=yn.toLowerCase();
							if(!((yn.equals("y")||yn.equals("yes"))))
								view.println("\n\nYou actually don't have a choice in the matter.");
							else
								view.print("\n\n");
							view.setGameListener(null);
							view.println("Here are the basics of how to play! Please read this carefully!\n");
							view.println("\nIMPORTANT NOTE:\n\nWhenever you see "+'\u25B6'+" moving back and forth below text (like right now), you may press ENTER to advance the text once you've finished reading.\nIn this case, pressing ENTER will start the game. Adventure awaits!^");
							view.print("#The basics of how to play:\n\n"+getHelpText()+"^");
							view.println("#{stopSound}    You: \"Yes... there it is, just across the pasture!\"\n\n" +
									"    (You: I'm visiting my former master who taught me everything I know about cartography. I was his apprentice until last year, when I went off to do my own work. " +
									"Now I'm out here to visit him like he wanted me to, but it was a much longer journey than I remembered... and I've walked all the way from the last town!)^{!beginning.mp3}");
							parser.parse("_addtoinventory backpack");
							view.println(player.getRoom().getFullText());
							player.getRoom().setIsVisited(true);
							//soundPlayer.loop(player.getRoom().getSoundName(), SoundPlayer.OFFSETS.get(player.getRoom().getSoundName()), false);
							view.println();
							view.updateStatsText();
						}
					});
				}
			}
		});
	}

	public void processCommand(String verb) {
		if(player.getRoom().getVerbEffects()!=null) { //the Room may know what to do
			String command=verb;
			if(parser.getObject()!=null) {
				command+=" "+parser.getObject().getFullName();
			} else if(parser.getObjectName()!=null) {
				command+=" "+parser.getObjectName();
			}
			String effect=null;
			//testCommand is NOT lowercased--all casing in block#.txt must be accurate
			String testCommand = command;
			//try all the possible command formats (e.g. including IO, including only DO, just verb, etc.)
			//to see if the room reacts to any of them, starting with the most specific (i.e. with the most words)
			effect = player.getRoom().getVerbEffects().get(testCommand);
			while(effect == null && testCommand.contains(" ")) {
				testCommand = testCommand.substring(0, testCommand.lastIndexOf(" "));
				effect = player.getRoom().getVerbEffects().get(testCommand);
			}
			if(effect!=null) {
				String formattedEffect = "";
				boolean quoteIsOpen = false;
				boolean printingBracketIsOpen = false;
				for(int i = 0; i < effect.length(); i++) {
					if(quoteIsOpen) {
						if(effect.charAt(i) == '{')
							formattedEffect+="`";
						else if(effect.charAt(i) == '}')
							formattedEffect+="~";
						else if(effect.charAt(i) == '[')
							formattedEffect+="ˆ";
						else if(effect.charAt(i) == ']')
							formattedEffect+="˜";
						else
							formattedEffect+=effect.charAt(i);
					} else if(printingBracketIsOpen) {
						if(effect.charAt(i) == '{')
							formattedEffect+="`";
						else if(effect.charAt(i) == '}')
							formattedEffect+="~";
						else
							formattedEffect+=effect.charAt(i);
					} else {
						formattedEffect+=effect.charAt(i);
					}
					if(effect.charAt(i) == '"')
						quoteIsOpen = !quoteIsOpen;
					else if(effect.charAt(i) == '[')
						printingBracketIsOpen = true;
					else if(effect.charAt(i) == ']')
						printingBracketIsOpen = false;
				}
				StringTokenizer tokenizer=new StringTokenizer(formattedEffect, "][}{", true);
				boolean parsedACommand=false;
				char firstCharOfLastToken=' ';
				String tempVerbEffect = effect;
				player.getRoom().getVerbEffects().remove(testCommand); //to avoid recursive stack overflows (e.g. "dig" in the pasture, which gets expanded to "dig dirt shovel" but then first gets tested as "dig" in the above loop)
				while(tokenizer.hasMoreTokens()) {
					String toProcess=tokenizer.nextToken();
					char firstCharOfCurrentToken = toProcess.charAt(0); //to avoid {, etc. in the printed sections to count towards the {} command processing tokens
					toProcess = toProcess.replace("`", "{");
					toProcess = toProcess.replace("~", "}");
					toProcess = toProcess.replace("ˆ", "[");
					toProcess = toProcess.replace("˜", "]");
					//System.out.println(toProcess);
					if(firstCharOfLastToken=='[')
						view.println(toProcess);
					else if(firstCharOfLastToken=='{') {
						parser.parse(toProcess);
						parsedACommand=true;
					}
					firstCharOfLastToken=firstCharOfCurrentToken;
				}
				if(!player.getRoom().getVerbEffects().containsKey(testCommand)) {
					//if the command still isn't in the verb effects, i.e. hasn't been added back in with a different effect by the current verb effect (e.g. "examine haystack" in the barn, which gets mapped to search haystack the SECOND time)
					player.getRoom().getVerbEffects().put(testCommand, tempVerbEffect);
				} else if(player.getRoom().getVerbEffects().get(testCommand).equals("remove")) {
					//if it's been marked for removal in removeverbeffect (i.e. mapped to an empty string), remove it
					player.getRoom().getVerbEffects().remove(testCommand);
				}
				if(parsedACommand) {
					return; //don't do anything after the Room has dealt with the command
				}
			}
		}
		if(CommandParser.TAKE_FIRST_VERBS.contains(verb)) {
			if(parser.getObject()!=null&&parser.getObject().isTakeable()&&!player.has(parser.getObject())) {
				view.println("(First taking the "+parser.getObject().getFullName()+")");
				parser.parse("take "+parser.getObject().getFullName());
				parser.revertToLastCommand();
				view.println();
			}
		}
		if(parser.getObject()!=null) { //then let the object process the command
			String effect=parser.getObject().process(verb, parser.getIndirectObject(), true);
			if(effect!=null) {
				StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
				char firstCharOfLastToken=' ';
				while(tokenizer.hasMoreTokens()) {
					String toProcess=tokenizer.nextToken();
					if(firstCharOfLastToken=='[')
						view.println(toProcess);
					else if(firstCharOfLastToken=='{') {
						parser.parse(toProcess);
					}
					firstCharOfLastToken=toProcess.charAt(0);
				}
				player.addMove();
				//it is NOT necessary to include {_donothing} in a DynamicObject's command effect as long as there is something else to be processed (even just a print statement works)
				return; //don't do anything after the DO has dealt with the command
			}
			else if(parser.getObject() instanceof DynamicObject&&((DynamicObject)parser.getObject()).hasEffect(verb)) {
				if(parser.getIndirectObject()==null&&!parser.justLookedJustForIO()) {
					//view.println("You can't "+verb+" the "+parser.getObject().getFullName()+" with your bare hands.");
					view.println("What do you want to "+verb+" the "+parser.getObject().getFullName()+" with?");
					parser.lookJustForIndirectObject();
					return;
				}
				else if(parser.getIndirectObject()==null&&parser.justLookedJustForIO()) {
					view.println("I don't see "+parser.getIndirectObjectNameWithArticle()+" here.");
					return;
				}
			}
		}
		if(parser.getIndirectObject()!=null) { //can't be null because there is a DO
			String effect=parser.getIndirectObject().process(verb, parser.getObject(), false);
			if(effect!=null) {
				StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
				char firstCharOfLastToken=' ';
				while(tokenizer.hasMoreTokens()) {
					String toProcess=tokenizer.nextToken();
					if(firstCharOfLastToken=='[')
						view.println(toProcess);
					else if(firstCharOfLastToken=='{') {
						parser.parse(toProcess);
					}
					firstCharOfLastToken=toProcess.charAt(0);
				}
				player.addMove();
				return; //don't do anything after the IO has dealt with the command
			}
		}
		if(verb.charAt(0)=='_')
			verb=verb.substring(1);
		Method method;
		try {
			method = Game.class.getMethod(verb, new Class<?>[]{});
			method.invoke(this, new Object[]{});
			if(!CommandParser.NON_MOVE_ADDING_VERBS.contains(verb))
				player.addMove();
		} catch (NoSuchMethodException e) {
			if(parser.getObject()==null) {
				if(!parser.justLookedJustForObject()&&!parser.hasMultipleWords()) {
					view.println("What do you want to "+verb+"?");
					parser.lookJustForObject();
				}
				else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
					view.println(getAdjectivePossibilities(parser.getObjectName()));
					parser.lookJustForObjectAdjective();
				}
				else
					view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else if(parser.indirectObjectIsVague()&&!parser.justLookedJustForIOAdjective()) {
				view.println(getAdjectivePossibilities(parser.getIndirectObjectName()));
				parser.lookJustForIndirectObjectAdjective();
			}
			else {
				if(parser.getIndirectObject()!=null)
					view.println("You can't "+verb+" the "+parser.getObject().getFullName()+" with the "+parser.getIndirectObject().getFullName()+".");
				else
					view.println("You can't "+verb+" the "+parser.getObject().getFullName()+".");
			}
		} catch (Exception e) {
			view.println("Something went wrong: "+e);
			e.printStackTrace();
		}
	}

	public void go(int direction) {
		if(direction==-1) {
			view.println("You can only go north, south, east, west, up, or down.");
			player.addMove();
		}
		else {
			List<TACharacter> fighters=player.getRoom().getCharactersBySpeed();
			if(player.getRoom().getAdjacent(direction)!=null||(player.getRoom().getDoor(direction)!=null&&!player.getRoom().getDoor(direction).isOpen())) {
				if(fighters.size()>1) {
					view.println("As you turn your back to leave:");
					for(TACharacter c:fighters) {
						if(c==player)
							continue;
						c.setProximity(Math.max(1, c.getProximity()-1));
					}
					BattleCalculator.beginCombat(fighters, true);
					if(player.getHP()==0)
						return;
					else
						view.println();
				}
				List<String> effects=player.go(direction);
				if(effects!=null) {
					for(String str:effects)
						parser.parse(str);
				}
				//player.addMove();
			}
			else { //the Player didn't go anywhere
				view.println("You can't go that way.");
				if(fighters.size()>1) {
					player.addMove();
					if(player.getHP()==0)
						return;
				}
				return;
			}
			for(TACharacter c:player.getRoom().getHostileCharacters()) {
				if(((NPC)c).getBattleMusicName()!=null/*&&soundPlayer.soundIsOn()*/) {
					soundPlayer.stop(true);
					soundPlayer.loop(((NPC)c).getBattleMusicName(), SoundPlayer.OFFSETS.get(((NPC)c).getBattleMusicName()), false);
				}
			}
			if(!player.getRoom().getCharactersBySpeed().get(0).equals(player))
				BattleCalculator.beginCombat(false);
		}
	}

	public void go() {
		/*if(parser.getObjectName()!=null) {
			int direction=(int)Math.floor(GO_DIRECTIONS.indexOf(Character.toUpperCase(parser.getObjectName().charAt(0))+parser.getObjectName().substring(1))/5.0);
			go(direction);
		}
		else*/
		if(parser.getObject()!=null) {
			int direction = -1;
			//check to see if the object is a room or an enemy
			for(int i=0; i<6; i++) {
				Room room=player.getRoom().getAdjacent(i, true, true);
				if(room!=null&&(room.getName().equalsIgnoreCase(parser.getObjectName())||room.alsoKnownAs(parser.getObjectName()))) {
					if(direction==-1)
						direction=i;
					else {
						view.println("That was a little too vague. Please specify which direction you want to go.");
						return;
					}
				}
			}
			if(direction != -1) {
				go(direction);
			} else {
				approach();
			}
		} else {
			if(parser.hasMultipleWords()) {
				view.println("In what direction do you want to go? (Try typing \"go into [destination]\" if you're not sure of the cardinal direction.");
			} else {
				view.println("In what direction do you want to go?");
			}
		}
		//parser.lookJustForObject() is unnecessary because the directions are already verbs
	}

	public void north() {
		go(Room.NORTH);
	}

	public void south() {
		go(Room.SOUTH);
	}

	public void east() {
		go(Room.EAST);
	}

	public void west() {
		go(Room.WEST);
	}

	public void up() {
		go(Room.UP);
	}

	public void down() {
		go(Room.DOWN);
	}

	public void take() {
		if(parser.getObjectName()==null) {
			if(parser.hasMultipleWords()) {
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
			else if(player.getRoom().getNumTakeables()==1) {
				for(TAObject obj:player.getRoom().getContents()) {
					if(obj.isTakeable()&&obj.isVisible()) {
						view.println("(the "+obj.getFullName()+")");
						//if(player.getRoom().getVerbEffects()!=null&&player.getRoom().getVerbEffects().keySet().contains("take "+obj.getFullName())) {
						parser.parse("take "+obj.getFullName());
						return;
						/*}
						player.take(obj);
						return;*/
					}
				}
				for(Chest chest:player.getRoom().getChests()) {
					if(!chest.isOpen())
						continue;
					if(chest.getNumTakeables()==1) {
						TAObject obj=chest.removeTakeable();
						view.println("(the "+obj.getFullName()+")");
						/*if(player.getRoom().getVerbEffects()!=null&&player.getRoom().getVerbEffects().keySet().contains("take "+obj.getFullName())) {*/
						chest.addObject(obj);
						parser.parse("take "+obj.getFullName());
						return;
						/*}
						player.take(obj);
						return;*/
					}
				}
			}
			else if(player.getRoom().getNumTakeables()==0)
				view.println("There is nothing to take.");
			else {
				if(!parser.justLookedJustForObject()) {
					view.println("What do you want to take?");
					parser.lookJustForObject();
				}
				else
					view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
		}
		else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
			view.println(getAdjectivePossibilities(parser.getObjectName()));
			parser.lookJustForObjectAdjective();
		}
		else if(parser.getObjectName().equals("all")||parser.getObjectName().equals("everything")) {
			if(!player.take("all"))
				view.println("There is nothing to take.");
		}
		else if(player.getRoom().contains(parser.getObject())) {
			player.take(parser.getObject());
		}
		else {
			if(player.has(parser.getObjectName())) {
				if(parser.getObject().isPlural())
					view.println("You already have the "+parser.getObject().getFullName()+". Why are you trying to take them again?");
				else
					view.println("You already have the "+parser.getObject().getFullName()+". Why are you trying to take it again?");
			}
			else if(parser.getObjectAdjective()!=null)
				view.println("I don't see "+parser.getObjectAdjectiveWithArticle()+" "+parser.getObjectName()+" here.");
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
	}

	public void drop() {
		if(parser.getObjectName()==null) {
			if(parser.hasMultipleWords()) {
				view.println("You don't have that.");
			}
			else if(player.getInventorySize()==1&&player.drop("all"))
				return;
			else if(player.getInventorySize()==0)
				view.println("You don't have anything in your inventory.");
			else if(!parser.justLookedJustForObject()) {
				view.println("What do you want to drop?");
				parser.lookJustForObject();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
			view.println(getAdjectivePossibilities(parser.getObjectName()));
			parser.lookJustForObjectAdjective();
		}
		else if(parser.getObjectName().equals("all")||parser.getObjectName().equals("everything")) {
			if(!player.drop("all"))
				view.println("You don't have anything in your inventory.");
		}
		else if(parser.getObjectName().equals("bass"))
			view.println("Wub wub wub.");
		else if(!player.drop(parser.getObject()))
			view.println("You don't have a "+parser.getObject().getFullName()+".");
	}

	public void look() {
		if(parser.getObject()==null||parser.getObject().equals(player.getRoom()))
			view.println(player.getRoom().getFullText());
		else
			parser.parse("examine "+parser.getObject().getFullName());
	}

	public void examine() {
		if(parser.getObject()!=null) {
			if(parser.getObject().equals(player.getRoom())) 
				look();
			else
				view.println(parser.getObject().getDescription());
			if(parser.getObject().equals(player.get("backpack"))) {
				view.println();
				inventory();
			}
		}
		else {
			if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else if(parser.hasMultipleWords()) {
				if(parser.getObjectName().equals("inventory"))
					inventory();
				else {
					//check if the object name is an adjacent room
					String[] roomNames = new String[6];
					int numRoomsFound = 0;
					for (int i = 0; i < 6; i++) {
						Room adjacentRoom = player.getRoom().getAdjacent(i, true);
						if(adjacentRoom != null
								&& (adjacentRoom.getName().equalsIgnoreCase(parser.getObjectName()) || adjacentRoom.alsoKnownAs(parser.getObjectName())
								|| (adjacentRoom.getName().equalsIgnoreCase(parser.getObjectName() + " " + parser.getIndirectObjectName()) || adjacentRoom.alsoKnownAs(parser.getObjectName() + " " + parser.getIndirectObjectName())))) {
							roomNames[i] = adjacentRoom.getFullNameWithArticle();
							numRoomsFound++;
						}
					}
					if(numRoomsFound == 0) {
						view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
					} else {
						String toPrint = "There is ";
						for (int i = 0; i < 6; i++) {
							if(roomNames[i] != null) {
								toPrint += roomNames[i];
								if (i < 4) {
									toPrint += " to the " + Room.DIRECTIONS.get(i);
								} else if (i == Room.DOWN) {
									toPrint += " below";
								} else {
									toPrint += " above";
								}
							}
							if(numRoomsFound > 2) {
								toPrint += ", ";
							} else if(numRoomsFound == 2) {
								toPrint += " and ";
							}
							numRoomsFound--;
						}
						view.println(toPrint + ".");
					}
				}
			}
			else if(!parser.justLookedJustForObject()) {
				view.println("What do you want to examine?");
				parser.lookJustForObject();
			}
			else {
				if(parser.getObjectAdjective()!=null)
					view.println("I don't see "+parser.getObjectAdjectiveWithArticle()+" "+parser.getObjectName()+" here.");
				else
					view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
		}
	}

	public void search() {
		if(parser.getObject()!=null) {
			if(parser.getObject() instanceof Chest) {
				if(((Chest)parser.getObject()).isOpen())
					view.println("The "+parser.getObject().getFullName()+" contains: "+((Chest)parser.getObject()).getInventoryText());
				else
					parser.parse("examine "+parser.getObject().getFullName());
			} else if(parser.getObject().equals(player.get("backpack"))) {
				inventory();
			} else {
				view.println("You can't search the "+parser.getObject().getFullName()+".");
			}
		}
		else {
			if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else if(parser.hasMultipleWords()) {
				if(parser.getObjectName().equals("inventory"))
					inventory();
				else
					view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
			else if(!parser.justLookedJustForObject()) {
				view.println("What do you want to search?");
				parser.lookJustForObject();
			}
			else {
				if(parser.getObjectAdjective()!=null)
					view.println("I don't see "+parser.getObjectAdjectiveWithArticle()+" "+parser.getObjectName()+" here.");
				else
					view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
			}
		}
	}

	public void inventory() {
		view.println(player.getInventoryText());
	}

	public void show() { //only gets called if the NPC could not process it
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()) {
				view.println("What do you want to show?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(!player.has(parser.getObject()))
			view.println("You don't have a "+parser.getObject().getFullName()+".");
		else if(parser.getIndirectObject()==null) {
			if(!parser.justLookedJustForIO()) {
				parser.lookJustForIndirectObject();
				view.println("To whom do you want to show the "+parser.getObject().getFullName()+"?");
			}
			else if(parser.indirectObjectIsVague()&&!parser.justLookedJustForIOAdjective()) {
				view.println(getAdjectivePossibilities(parser.getIndirectObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getIndirectObjectNameWithArticle()+" here.");
		}
		else {
			if(parser.getIndirectObject() instanceof TACharacter)
				view.println("The "+parser.getIndirectObject().getFullName()+" doesn't have anything to say about the "+parser.getObject().getFullName()+".");
			else
				view.println("You can't show the "+parser.getObject().getFullName()+" to the "+parser.getIndirectObject().getFullName()+".");
		}
	}

	public void talk() { //only gets called if the NPC could not process it
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()&&!parser.hasMultipleWords()) {
				view.println("Whom do you want to talk to?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForIndirectObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else {
			if(parser.getObject() instanceof TACharacter)
				view.println(parser.getObject().getFullName()+" doesn't have anything to say.");
			else
				view.println("Obviously you can't talk to the "+parser.getObject().getFullName()+".");
		}
	}

	public void enter() {
		//MAKE SURE THAT STATIC OBJECTS REPRESENTING ROOMS IN THEIR NEIGHBORING ROOMS
		//HAVE THE SAME NAME AND ADJECTIVE AS THE ROOM THEMSELVES!
		//(e.g. "small cottage" StaticObject vs. "cottage" Room)
		int direction = -1;
		boolean objectMatchedCurrentRoom = false;
		if (parser.getObjectName() != null) {
			String objectName = parser.getObjectName();
			if (parser.getObject() != null) {
				objectName = parser.getObject().getFullName();
			}
			String testObjectName = objectName+" "; //extra space to be stripped off in the first iteration of the loop
			//try all the possible object name formats and strip off one word at a time:
			//the enter command has " as a keyword to allow for multi-word objects, so we have to account for this
			while (direction == -1 && testObjectName.contains(" ")) {
				testObjectName = testObjectName.substring(0, testObjectName.lastIndexOf(" "));
				if (player.getRoom().getName().equalsIgnoreCase(testObjectName) || player.getRoom().alsoKnownAs(testObjectName)) {
					objectMatchedCurrentRoom = true;
				}
				for (int i = 0; i < 6; i++) {
					Room room = player.getRoom().getAdjacent(i, true, true);
					if(room != null && (room.equals(parser.getObject()) || (room.getName() != null && (room.getName().equalsIgnoreCase(testObjectName) || room.getFullName().equalsIgnoreCase(testObjectName) || room.alsoKnownAs(testObjectName))))) {
						if(direction == -1)
							direction = i;
						else {
							view.println("That was a little too vague. Please specify which direction you want to go.");
							return;
						}
					}
				}
			}
		}
		if (objectMatchedCurrentRoom && direction == -1) {
			view.println("You're already there.");
		} else if(direction == -1) {
			if(parser.getObject() == null && !parser.justLookedJustForObject() && !parser.hasMultipleWords()) {
				view.println("What do you want to enter?");
				parser.lookJustForObject();
			}
			else if(parser.getObject() == null)
				view.println("There isn't "+parser.getObjectNameWithArticle()+" here to enter.");
			else if(parser.getObject() instanceof NPC)
				view.println("I don't think so.");
			else
				view.println("You can't enter the "+parser.getObject().getFullName()+".");
		} else {
			view.println("(go "+Room.DIRECTIONS.get(direction)+")\n");
			parser.parse(Room.DIRECTIONS.get(direction));
		}
	}


	public void exit() {
		int direction=-1;
		int numPossible=0;
		boolean[] possible=new boolean[6];
		for(int i=0; i<6; i++) {
			possible[i]=player.getRoom().getAdjacent(i)!=null;
			if(possible[i]==true)
				numPossible++;
		}
		if(numPossible==0) {
			view.println("There are no obvious exits. It appears that you are trapped.");
			return;
		}
		else if(numPossible==1) {
			for(int i=0; i<6; i++) {
				if(possible[i]==true) {
					direction=i;
				}
			}
		}
		if(direction==-1) {
			view.println("Please specify which direction you want to go.");
			view.print("You can go ");
			String directions="";
			int numCounted=0;
			for(int i=0; i<6; i++) {
				if(possible[i]==true) {
					if(numCounted<numPossible-1) {
						directions += Room.DIRECTIONS.get(i); //say the direction
						directions += " (to the " + player.getRoom().getAdjacent(i, true).getName() + ")"; //say what's in that direction
						directions += ", ";
					}
					else {
						directions = directions.substring(0, directions.length()-2) //substring off the comma
								   + " or " + Room.DIRECTIONS.get(i)
								   + " (to the " + player.getRoom().getAdjacent(i, true).getName() + ").";
					}
					numCounted++;
				}
			}
			view.println(directions);
		}
		else {
			view.println("(go "+Room.DIRECTIONS.get(direction)+")\n");
			parser.parse(Room.DIRECTIONS.get(direction));
		}
	}

	public void jump() {
		int response=(int)(Math.random()*3)+1;
		if(response==1)
			view.println("You spontaneously jump into the air but realize that it was pointless on your way down.");
		else if(response==2)
			view.println("You leap upwards with your arms outstretched. What are you trying to do, touch the sky?");
		else if(response==3)
			view.println("You jump on the spot. Nothing happens.");
	}

	public void put() {
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()&&!parser.hasMultipleWords()) {
				view.println("What do you want to put?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
			view.println(getAdjectivePossibilities(parser.getObjectName()));
			parser.lookJustForObjectAdjective();
		}
		else if(parser.getIndirectObject()==null) {
			view.println("Where do you want to put the "+parser.getObject().getFullName()+"?");
			parser.lookJustForIndirectObject();
		}
		else if(parser.indirectObjectIsVague()&&!parser.justLookedJustForIOAdjective()) {
			view.println(getAdjectivePossibilities(parser.getIndirectObjectName()));
			parser.lookJustForIndirectObjectAdjective();
		}
		else {
			if(parser.getIndirectObject() instanceof Chest) {
				if(parser.getObjectName().equals("all")||parser.getObjectName().equals("everything")) {
					Iterator<TAObject> iter=player.getInventory().iterator();
					while(iter.hasNext()) {
						((Chest)parser.getIndirectObject()).addObject(iter.next());
						iter.remove();
					}
					view.println("You put the contents of your inventory into the "+parser.getIndirectObject().getFullName()+".");
				}
				else {
					((Chest)parser.getIndirectObject()).addObject(parser.getObject());
					player.getInventory().remove(parser.getObject());
					view.println("You put the "+parser.getObject().getFullName()+" into the "+parser.getIndirectObject().getFullName()+".");
				}
			}
			else if(new ArrayList<String>(Arrays.asList("down", "ground", "floor")).contains(parser.getIndirectObjectName()))
				parser.parse("drop "+parser.getObject().getFullName());
			else if(parser.getIndirectObject()!=null)
				view.println("You can't put the "+parser.getObject().getFullName()+" in or on the "+parser.getIndirectObject().getFullName()+".");
			else
				view.println("You can't put the "+parser.getObject().getFullName()+".");
		}
	}

	public void dig() {
		if(player.has("shovel")) {
			if(parser.getObject() == null) {
				if(player.getRoom().contains("ground"))
					parser.parse("dig ground shovel");
				else
					view.println("Digging here reveals nothing.");
			} else if (parser.getIndirectObject() == null) {
				parser.parse("dig" + parser.getObject().getFullName() + " shovel");
			} else {
				if(parser.getObject()!=null)
					view.println("You can't dig the "+parser.getObject().getFullName()+".");
				else if(parser.getObjectName()!=null)
					view.println("You can't dig the "+parser.getObjectName()+".");
			}
		}
		else {
			//check if they supplied an object that wasn't the shovel
			if(parser.getIndirectObject() != null)
				view.println("The "+parser.getIndirectObject().getFullName()+" is not suitable for digging.");
			else
				view.println("You have nothing to dig with.");
		}
	}

	public void think() {
		view.println("Think for yourself!");
	}

	public void sleep() {
		if(parser.getObject()!=null) {
			if(parser.getObject().getName().equals("bed")||parser.getObject().alsoKnownAs("bed")) {
				player.setHP(player.getMaxHP());
				view.println("You sleep in the "+parser.getObject().getFullName()+". Resting restores your health!");
			}
			else
				view.println("You can't sleep in the "+parser.getObject().getFullName()+"!");
		}
		else {
			if(!parser.justLookedJustForObject()) {
				view.println("You can't sleep standing up. What do you want to sleep in?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
	}

	public void delay() {
		view.println("Time passes.");
	}

	public void attack() {
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()) {
				view.println("Whom do you want to attack?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(parser.getIndirectObject()==null) {
			if(!parser.justLookedJustForIO()) {
				parser.lookJustForIndirectObject();
				view.println("What do you want to attack the "+parser.getObject().getFullName()+" with?");
			}
			else if(parser.indirectObjectIsVague()&&!parser.justLookedJustForIOAdjective()) {
				view.println(getAdjectivePossibilities(parser.getIndirectObjectName()));
				parser.lookJustForIndirectObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getIndirectObjectNameWithArticle()+" here.");
		}
		else if(parser.getObject().equals(player))
			view.println("Why, oh why, would you ever want to attack yourself?");
		else if(parser.getObject() instanceof TACharacter) {
			TACharacter object=(TACharacter)parser.getObject();
			if(object.getProximity()>0)
				player.attack(object);
			else {
				view.println("You can't attack the "+parser.getObject().getFullName()+"!!");
			}
		}
		else
			view.println("The "+parser.getObject().getFullName()+" isn't hurting you... so there's no reason to attack it!");
	}

	public void approach() {
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()) {
				view.println("Whom do you want to approach?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(parser.getObject() instanceof NPC) {
			NPC object=(NPC)parser.getObject();
			if(object.getProximity()<=0)
				view.println("I suppose you could move even closer to "+object.getFullNameLowerCase()+", but it would probably just make them feel uncomfortable.");
			else if(object.getProximity()==1) {
				view.println(parser.getObject().getFullName()+" is already within arm's reach!");
			}
			else {
				boolean justMoved=player.justChangedProximity();
				player.setJustChangedProximity(true);
				object.setProximity(object.getProximity()-1);
				view.println("You move closer to "+object.getFullNameLowerCase()+".");
				view.println(object.getCapitalizedGenderPronoun()+" is now at proximity "+object.getProximity()+".");
				//TODO describe how close they are now
				List<TACharacter> hostileCharacters=player.getRoom().getHostileCharacters();
				if(!hostileCharacters.isEmpty()) {
					String result="In doing so, you move away from:\n";
					boolean shouldPrint=false;
					for(int i=0; i<hostileCharacters.size(); i++) {
						if(hostileCharacters.get(i).equals(object))
							continue;
						if(hostileCharacters.get(i).getProximity()<BattleCalculator.MAX_PROXIMITY) {
							hostileCharacters.get(i).setProximity(hostileCharacters.get(i).getProximity()+1);
							result+="    "+hostileCharacters.get(i).getFullName()+", who is now at proximity "+hostileCharacters.get(i).getProximity()+".\n";
							shouldPrint=true;
						}
					}
					if(shouldPrint)
						view.println(result);
				}
				if(justMoved)
					player.addMove();
				else {
					view.println(object.getFullName()+" is watching carefully!");
					//TODO describe what the enemies are doing after the Player changes proximity for the first time
				}
			}

		} else {
			view.println("You can see the "+parser.getObject().getFullName()+" just fine from where you are.");
		}
	}

	public void retreat() {
		if(parser.getObject()==null) {
			if(!parser.justLookedJustForObject()) {
				view.println("From whom do you want to retreat?");
				parser.lookJustForObject();
			}
			else if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else if(parser.getObject() instanceof NPC) {
			NPC object=(NPC)parser.getObject();
			if(object.getProximity()<=0)
				view.println("Why do you want to move away from "+object.getFullName()+"? "+object.getCapitalizedGenderPronoun()+" won't bite!");
			else if(object.getProximity()==BattleCalculator.MAX_PROXIMITY) {
				view.println("There is more than enough distance between you and "+parser.getObject().getFullName()+" (although you may disagree).");
			}
			else {
				boolean justMoved=player.justChangedProximity();
				player.setJustChangedProximity(true);
				object.setProximity(object.getProximity()+1);
				view.println("You move away from "+object.getFullName()+".");
				view.println(object.getCapitalizedGenderPronoun()+" is now at proximity "+object.getProximity()+".");
				//TODO describe how close they are now
				List<TACharacter> hostileCharacters=player.getRoom().getHostileCharacters();
				if(!hostileCharacters.isEmpty()) {
					String result="In doing so, you move closer to:\n";
					boolean shouldPrint=false;
					for(int i=0; i<hostileCharacters.size(); i++) {
						if(hostileCharacters.get(i).equals(object))
							continue;
						if(hostileCharacters.get(i).getProximity()>1) {
							hostileCharacters.get(i).setProximity(hostileCharacters.get(i).getProximity()-1);
							result+="    "+hostileCharacters.get(i).getFullName()+", who is now at proximity "+hostileCharacters.get(i).getProximity()+".\n";
							shouldPrint=true;
						}
					}
					if(shouldPrint)
						view.println(result);
				}
				if(justMoved)
					player.addMove();
				else {
					view.println(object.getFullName()+" is watching carefully!");
					//TODO describe what the enemies are doing after the Player changes proximity for the first time
				}
			}
		}
		else
			view.println("There is no need to move away from the "+parser.getObject().getFullName()+".");
	}

	public void sound() {
		soundPlayer.setSoundIsOn(!soundPlayer.soundIsOn());
		if(soundPlayer.soundIsOn())
			view.println("Sound is now on.");
		else
			view.println("Sound is now off.");
		if(soundPlayer.soundIsOn()&&player.getRoom().getSoundName()!=null)
			soundPlayer.loop(player.getRoom().getSoundName(), SoundPlayer.OFFSETS.get(player.getRoom().getSoundName()), true);
		else
			soundPlayer.stop(true);
		view.updateStatsText();
	}

	public void map() {
		if(!player.has("map")) {
			if(player.getRoom().contains("map")) {
				view.println("(First taking the map)");
				parser.parse("take map");
			}
			else {
				view.println("You don't have a map to look at.");
				return;
			}
		}
		//view.println("Please wait for a later version of this game to use the map. Thank you for your patience.");

		String text="  ";
		BufferedReader reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/maps/map"+player.getMapNumber()+".taf")));
		try {
			String line=reader.readLine();
			while(line!=null) {
				text+=line+"\n  ";
				line=reader.readLine();
			}
			reader.close();
		} catch(IOException e) {
			view.println("There is something wrong with the map. :(");
			e.printStackTrace();
		}
		view.showASCIIFrame(text, true);
		view.println("You look at the map.");
	}

	private String getHelpText() {
		//TODO rewrite this
		return "To move around in the world, just type a direction (\"north\", \"south\", \"east\", \"west\", \"up\", and \"down\" or n, s, e, w, u, d for short). " +
		"If you forget which way to go, you can also usually type \"enter\" or \"exit\" along with the place you want to go to or from. " +
		"To look around type \"look\" (l for short). To take items and carry them with you, type \"take\" and type \"drop\" to drop items from your inventory. " +
		"Type \"examine\" or \"x\" to look at certain objects more closely and type \"inventory\" or \"i\" to see what items you have. \n\n" +
		"You can also save and load games with \"save\" and \"load\". If you are done playing, type \"quit\", or type \"restart\" to start a new game. " +
		"You can toggle sound on/off with \"sound\". Some synonyms also work for most verbs, so don't worry if you forget exactly what to type. " +
		"Try out other commands and see what happens! If you are stuck, look around! \n\n" +
		"Whenever you see "+'\u25B6'+" moving back and forth below text, you may press ENTER to advance the text once you've finished reading.\n\n" +
		"To see this message again, just type \"help\" or \"h\".";
	}

	public void help() {
		view.println(getHelpText());
	}

	/*public void think() {
		if(player.getObjectives().isEmpty())
			view.println("You don't have anything particular on your mind at the moment. Perhaps now's the time for a little exploring.");
		else {
			view.println("You consider your present situation...");
			for(String str:player.getObjectives())
				view.println("\n--"+str);
		}
	}*/

	private String getAdjectivePossibilities(String str) {
		List<String> adjectives=player.getRoom().getAdjectives(str);
		String text="Do you mean the ";
		if(adjectives.size()==2)
			text+=adjectives.get(0)+" "+parser.getObjectName()+" or the "+adjectives.get(1)+" "+parser.getObjectName()+"?";
		else {
			Iterator<String> iter=adjectives.iterator();
			int index=0;
			while(index<adjectives.size()-1)
				text+="the "+iter.next()+" "+parser.getObjectName()+", ";
			text+="or the "+iter.next()+" "+parser.getObjectName()+"?";
		}
		return text;
	}

	public void quit() {
		//called from parsing a normal command
		quit(true);
	}

	public void quit(final boolean shouldConfirm) {
		if(!shouldConfirm) {
			view.stopResponding();
			soundPlayer.stop(false);
			System.exit(0);
		}
		view.print("Are you sure you want to quit? (yes or no): ");
		view.setGameListener(new GameListener() {
			public void textTyped(String text) {
				view.println();
				String yn=text.trim().toLowerCase();
				yn=yn.toLowerCase().trim();
				if(yn==null||yn.length()==0||(!yn.equals("y")&&!yn.equals("yes"))) {
					view.println("Okay.\n");
				}
				else {
					/*if(player.getNumMoves()!=1)
						view.println("You quit after "+player.getNumMoves()+" moves.");
					else
						view.println("You quit after 1 move.");*/
					view.println("\nThe game has finished. You may now close the window.");
					view.stopResponding();
					soundPlayer.stop(false);
				}
				view.setGameListener(null);
			}
		});
	}

	public void die() {
		player.setHP(0);
		player.setIsDead(true);
		final javax.swing.Timer timer=new javax.swing.Timer(50, null);
		ActionListener actionListener=new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized(view.getPauseQueue()) {
					if(!view.getPauseQueue().isEmpty()) {
						return;
					}
				}
				timer.stop();
				soundPlayer.stop(true);
				view.println("\n\n***YOU HAVE DIED***\n\nNice going. Maybe you should be more careful next time.");
				view.setGameListener(new GameListener() {
					public void textTyped(String text) {
						view.println();
						String yn=text.trim().toLowerCase();
						yn=yn.toLowerCase().trim();
						if(yn==null||yn.length()==0||(!yn.equals("y")&&!yn.equals("yes"))) {
							view.println("\nThe game has finished. You may now close the window.");
							view.stopResponding();
						}
						else {
							deleteDirectoryContents(supportPath+"saves/temp.taf");
							block=new RoomBlock(0, "temp");
							parser=new CommandParser();
							if(player.has("map"))
								view.removeMapButton();
							player=new Player();
							view.resetStatsText();
							soundPlayer.stop(false);
							soundPlayer.setSoundIsOn(false);
							view.print("#");
							view.setGameListener(null);
							play();
						}
						//view.setGameListener(null);
					}
				});
				view.print("Would you like to start again? (yes or no): ");
			}
		};
		timer.addActionListener(actionListener);
		timer.start();
	}

	public void restart() {
		view.print("Are you sure you want to restart? (yes or no): ");
		view.setGameListener(new GameListener() {
			public void textTyped(String text) {
				view.println();
				String yn=text.trim().toLowerCase();
				yn=yn.toLowerCase().trim();
				if(yn==null||yn.length()==0||(!yn.equals("y")&&!yn.equals("yes"))) {
					view.println("Okay.\n");
					view.setGameListener(null);
				}
				else {
					File tempSave=new File(supportPath+"saves/temp.taf");
					tempSave.mkdirs();
					deleteDirectoryContents(supportPath+"saves/temp.taf");
					//RoomBlock.initializeBlockLocations();
					block=new RoomBlock(0, "temp");
					parser=new CommandParser();
					if(player.has("map"))
						view.removeMapButton();
					player=new Player();
					view.resetStatsText();
					soundPlayer.stop(false);
					soundPlayer.setSoundIsOn(false);
					view.print("#");
					view.setGameListener(null);
					play();
				}
			}
		});
	}

	private void deleteDirectoryContents(String path) {
		File directory=new File(path);
		if(directory.exists()) {
			File[] files=directory.listFiles();
			for(int i=0; i<files.length; i++)
				files[i].delete();
		}
	}

	public void save() {
		if(!player.getRoom().getHostileCharacters().isEmpty()) {
			view.println("You have more pressing matters on your hands right now!");
			return;
		}
		if(parser.getObjectName()==null) {
			view.println("What would you like to name your save file?");
			parser.lookJustForObject();
			return;
		}
		else if(parser.getObjectName().equals("temp")||parser.getObjectName().equals("temp2")) {
			view.println("That name is reserved for the system.");
			return;
		}
		String saveName=parser.getObjectName();
		player.saveInfo();
		soundPlayer.saveInfo();
		saveBlock(block);
		File currentSaveLoc=new File(supportPath+"saves/temp.taf");
		File newSaveLoc=new File(supportPath+"saves/"+saveName+".taf");
		newSaveLoc.mkdirs();
		deleteDirectoryContents(supportPath+"saves/"+saveName+".taf");
		File[] files=currentSaveLoc.listFiles();
		for(int i=0; i<files.length; i++) {
			File f=files[i];
			try {
				BufferedReader reader = new BufferedReader(new FileReader(supportPath+"saves/temp.taf/"+f.getName()));
				PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(supportPath+"saves/"+saveName+".taf/"+f.getName())));
				String line=reader.readLine();
				while(line!=null) {
					out.write(line+"\n");
					line=reader.readLine();
				}
				reader.close();
				out.close();
			} catch(IOException e) {
				view.println("There was a problem saving your game: "+e);
				return;
			}
		}
		view.println("Saved.");
	}

	private void saveBackup() {
		player.saveInfo();
		soundPlayer.saveInfo();
		saveBlock(block);
		File currentSaveLoc=new File(supportPath+"saves/temp.taf");
		File newSaveLoc=new File(supportPath+"saves/temp2.taf");
		newSaveLoc.mkdirs();
		deleteDirectoryContents(supportPath+"saves/temp2.taf");
		File[] files=currentSaveLoc.listFiles();
		for(int i=0; i<files.length; i++) {
			File f=files[i];
			try {
				BufferedReader reader = new BufferedReader(new FileReader(supportPath+"saves/temp.taf/"+f.getName()));
				PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(supportPath+"saves/temp2.taf/"+f.getName())));
				String line=reader.readLine();
				while(line!=null) {
					out.write(line+"\n");
					line=reader.readLine();
				}
				reader.close();
				out.close();
			} catch(IOException e) {return;}
		}
	}

	public void load() { //copies all files from the save folder into temp and loads the block and Player position from there.
		if(parser.getObjectName()==null) {
			view.println("What game would you like to load?");
			parser.lookJustForObject();
			return;
		}
		else if(parser.getObjectName().equals("temp")) {
			view.println("The specified save file does not exist.");
			return;
		}
		String saveName=parser.getObjectName();
		if(new File(supportPath+"saves/"+saveName+".taf").exists()) {
			saveBackup();
			File currentSaveLoc=new File(supportPath+"saves/"+saveName+".taf");
			File newSaveLoc=new File(supportPath+"saves/temp.taf");
			deleteDirectoryContents(supportPath+"saves/temp.taf");
			newSaveLoc.mkdirs();
			File[] files=currentSaveLoc.listFiles();
			for(int i=0; i<files.length; i++) {
				File f=files[i];
				try {
					BufferedReader reader = new BufferedReader(new FileReader(supportPath+"saves/"+saveName+".taf/"+f.getName()));
					PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(supportPath+"saves/temp.taf/"+f.getName())));
					String line=reader.readLine();
					while(line!=null) {
						out.write(line+"\n");
						line=reader.readLine();
					}
					reader.close();
					out.close();
				} catch(IOException e){
					view.println("There was a problem loading your game: "+e);
					loadBackup();
					deleteDirectoryContents(supportPath+"saves/temp2.taf");
					new File(supportPath+"saves/temp2.taf").delete();
					return;
				}
			}
			Player tempPlayer=player;
			SoundPlayer tempSoundPlayer=soundPlayer;
			try {
				BufferedReader soundReader=new BufferedReader(new FileReader(supportPath+"saves/"+saveName+".taf/soundPlayer.taf"));
				soundPlayer=new SoundPlayer(new JSONObject(soundReader.readLine()));
				if(tempSoundPlayer.soundIsOn())
					soundPlayer.setSoundIsOn(true);
				boolean musicIsPlaying=tempSoundPlayer.musicIsPlaying();
				tempSoundPlayer.stop(musicIsPlaying);
				if(/*soundPlayer.soundIsOn()&&*/soundPlayer.getCurrentSoundName()!=null&&!soundPlayer.getCurrentSoundName().equals(""))
					soundPlayer.loop(soundPlayer.getCurrentSoundName(), SoundPlayer.OFFSETS.get(soundPlayer.getCurrentSoundName()), musicIsPlaying);
				soundReader.close();
				BufferedReader playerReader=new BufferedReader(new FileReader(supportPath+"saves/"+saveName+".taf/player.taf"));
				player=new Player(new JSONObject(playerReader.readLine()));
				playerReader.close();
				if(player.getLoadedBlockNumber()!=-1)
					loadBlock(player.getLoadedBlockNumber());
				else
					throw new IOException();
				player.setRoom(block.get(player.getLoadedRoomID()));
				view.println("#Saved game loaded from "+saveName+".\n");
				saveName="temp";
				look();
				view.println();
				inventory();
				if(player.has("map"))
					view.addMapButton();
				view.updateStatsText();
			} catch(Exception e) {
				view.println("There was a problem loading your game: "+e);
				e.printStackTrace();
				loadBackup();
				deleteDirectoryContents(supportPath+"saves/temp2.taf");
				new File(supportPath+"saves/temp2.taf").delete();
				player=tempPlayer;
				soundPlayer=tempSoundPlayer;
			}
		}
		else
			view.println("The specified save file does not exist.");
	}

	private void loadBackup() {
		if(new File(supportPath+"saves/temp2.taf").exists()) {
			File currentSaveLoc=new File(supportPath+"saves/temp2.taf");
			File newSaveLoc=new File(supportPath+"saves/temp.taf");
			newSaveLoc.mkdirs();
			File[] files=currentSaveLoc.listFiles();
			for(int i=0; i<files.length; i++) {
				File f=files[i];
				try {
					BufferedReader reader = new BufferedReader(new FileReader(supportPath+"saves/temp2.taf/"+f.getName()));
					PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(supportPath+"saves/temp.taf/"+f.getName())));
					String line=reader.readLine();
					while(line!=null) {
						out.write(line+"\n");
						line=reader.readLine();
					}
					reader.close();
					out.close();
				} catch(IOException e){
					view.println("There was a problem loading your game: "+e);
					return;
				}
			}
			Player tempPlayer=player;
			SoundPlayer tempSoundPlayer=soundPlayer;
			try {
				BufferedReader soundReader=new BufferedReader(new FileReader(supportPath+"saves/temp.taf/soundPlayer.taf"));
				boolean soundIsOn=soundPlayer.soundIsOn();
				soundPlayer=new SoundPlayer(new JSONObject(soundReader.readLine()));
				soundPlayer.setSoundIsOn(soundIsOn);
				soundReader.close();
				BufferedReader playerReader=new BufferedReader(new FileReader(supportPath+"saves/temp.taf/player.taf"));
				player=new Player(new JSONObject(playerReader.readLine()));
				playerReader.close();
				if(player.getLoadedBlockNumber()!=-1)
					loadBlock(player.getLoadedBlockNumber());
				else
					throw new IOException();
				player.setRoom(block.get(player.getLoadedRoomID()));
			} catch(Exception e) {
				view.println("There was a problem loading your game: "+e);
				player=tempPlayer;
				soundPlayer=tempSoundPlayer;
			}
		}
		else
			view.println("There was a problem restoring your current game.");
	}

	public void saveBlock(RoomBlock block) {
		try {
			PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(supportPath+"saves/temp.taf/block"+block.getNumber()+".taf")));
			out.write(block.getMinID()+"\n");
			out.write(block.getMaxID()+"\n");
			Set<Door> doors=new HashSet<Door>();
			for(Integer i:block.getRooms().keySet()) {
				out.write(block.getRooms().get(i).toString()+"\n");
				for(int j=0; j<6; j++)
					if(block.getRooms().get(i).getDoor(j)!=null)
						doors.add(block.getRooms().get(i).getDoor(j));
			}
			out.write("-----\n");
			for(Door d:doors)
				out.write(d.toString()+"\n");
			out.close();
		} catch(Exception e) {
			view.println("There was a problem saving your progress: "+e);
			view.println("This is usually caused by playing two copies of this game at the same time or by overly protective permissions.");
		}
	}

	public void loadBlock(int blockNum) {
		RoomBlock tempBlock=block;
		try {
			block=new RoomBlock(blockNum, "temp");
		} catch(Exception e) {
			block=tempBlock;
			view.println("There was a problem loading your game: "+e);
		}
	}

	public RoomBlock getBlock() {
		return block;
	}

	public Player getPlayer() {
		return player;
	}

	public View getView() {
		return view;
	}

	public CommandParser getCommandParser() {
		return parser;
	}

	public SoundPlayer getSoundPlayer() {
		return soundPlayer;
	}

	public void setBlock(int newBlockNumber) {
		setBlock(new RoomBlock(newBlockNumber, "temp")); //used to be saveName
	}

	public void setBlock(RoomBlock block) {
		this.block = block;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	//Methods used secretly by the game are below

	public void teleport() { //should only be used for TACharacters
		TACharacter obj=(TACharacter)player.getRoom().getObject(parser.getObjectName());
		player.getRoom().remove(obj);
		String roomID=parser.getIndirectObjectName();
		if(roomID.contains("/")) { //room IDs should be block number/ID or just ID if it's in the current block
			saveBlock(block); //TODO test whether or not this is a good idea (saving the current block before teleporting out of it)
			loadBlock(Integer.parseInt(roomID.substring(0, roomID.indexOf("/"))));
			roomID=roomID.substring(roomID.indexOf("/")+1);
		}
		Room room=block.get(Integer.parseInt(roomID));
		obj.setRoom(room);
		for(TACharacter c:obj.getFollowingCharacters())
			c.setRoom(room);
	}

	public void delete() {
		if(player.has(parser.getObject()))
			player.delete(parser.getObject());
		if(parser.getIndirectObjectName()==null||!(((TACharacter)parser.getIndirectObject()).delete(parser.getObjectName())))
			player.getRoom().remove(parser.getObject());	
	}

	public void setfield() {
		try {
			String valueStr=parser.getIndirectObjectName().substring(parser.getIndirectObjectName().indexOf(":")+1);
			Field field;
			try {
				field=parser.getObject().getClass().getDeclaredField(parser.getIndirectObjectName().substring(0, parser.getIndirectObjectName().indexOf(":")));
			} catch(NoSuchFieldException ex) {
				try {
					field=parser.getObject().getClass().getSuperclass().getDeclaredField(parser.getIndirectObjectName().substring(0, parser.getIndirectObjectName().indexOf(":")));
				} catch(NoSuchFieldException exc) {
					Class<?> c=parser.getObject().getClass().getSuperclass().getSuperclass();
					while(!c.getName().equals("textadventure.TAObject"))
						c=c.getSuperclass();
					field=c.getDeclaredField(parser.getIndirectObjectName().substring(0, parser.getIndirectObjectName().indexOf(":")));
				}
			}
			if(valueStr.equals("null")) {
				field.set(parser.getObject(), null);
				return;
			}
			Object value;
			if(valueStr.length()==0)
				value="";
			else if(valueStr.charAt(0)=='#') { //for numbers
				value=new Integer(Integer.parseInt(valueStr.substring(1)));
			}
			else if(valueStr.charAt(0)=='&') { //for booleans
				value=new Boolean(Boolean.parseBoolean(valueStr.substring(1)));
			}
			/*else if(valueStr.charAt(0)==',') { //for collections of strings
				valueStr = valueStr.substring(1);
				System.out.println("type: "+field.getType());
				System.out.println("generic type: "+field.getGenericType());
				value = field.getGenericType().getClass().newInstance();
				StringTokenizer tokenizer = new StringTokenizer(valueStr);
				while(tokenizer.hasMoreTokens()) {
					((Collection<String>)value).add(tokenizer.nextToken());
				}
			}*/
			else //for strings
				value=valueStr;
			field.set(parser.getObject(), value);
		} catch(Exception e) {view.println("Something went wrong behind the scenes: "+e);e.printStackTrace();}
	}
	
	public void addothername() {
		//warning: otherNames works differently for DynamicObjects (and chests) because it has a set for each state
		parser.getObject().getOtherNames().add(parser.getIndirectObjectName());
	}
	
	public void removeothername() {
		//see above warning
		parser.getObject().getOtherNames().remove(parser.getIndirectObjectName());
	}

	public void removeentereffect() {
		player.getRoom().setEnterEffect(null);
	}

	public void setentereffect() {
		char[] newEffectChars=parser.getObjectName().toCharArray();
		boolean openBracket=true;
		for(int i=0; i<newEffectChars.length; i++) {
			if(newEffectChars[i]=='|') {
				if(openBracket)
					newEffectChars[i]='{';
				else
					newEffectChars[i]='}';
				openBracket=!openBracket;
			}
			else if(newEffectChars[i]=='$') {
				if(openBracket)
					newEffectChars[i]='[';
				else
					newEffectChars[i]=']';
				openBracket=!openBracket;
			}
		}
		String newEffect=new String(newEffectChars);
		newEffect=newEffect.replace("\\\"", "\"");
		player.getRoom().setEnterEffect(newEffect);
	}

	public void removeverbeffect() {
		/*String toRemove=null;
		for(String str:player.getRoom().getVerbEffects().keySet()) {
			if(str.equalsIgnoreCase(parser.getObjectName())||(str.toLowerCase().contains(parser.getObjectName().toLowerCase())&&str.toLowerCase().contains(" "+parser.getIndirectObjectName().toLowerCase()))) {
				toRemove=str;
				break;
			}
		}*/
		if(player.getRoom().getVerbEffects().containsKey(parser.getObjectName())) {
			player.getRoom().getVerbEffects().remove(parser.getObjectName()); //allows for more precision when specifying which verb effect to remove
		} else {
			player.getRoom().getVerbEffects().put(parser.getObjectName(), "remove"); //mark it for removal back in processCommand
		}
	}

	public void addverbeffect() {
		//parser.getObjectName() should return something that's formatted:
		//"command for the new verb effect" "new verb effect"
		String toProcess = parser.getObjectName();
		boolean startedWithQuote = false; //in case the command didn't need/have quotes around it
		if(toProcess.charAt(0) == '\"') {
			toProcess = toProcess.substring(1); //strip off opening quote
			startedWithQuote = true;
		}
		String commandToAdd = toProcess.substring(0, toProcess.indexOf("\"")).trim();
		String newVerbEffect;
		if(startedWithQuote)
			newVerbEffect = toProcess.substring(toProcess.indexOf("\"")+3, toProcess.length()-1);
		else
			newVerbEffect = toProcess.substring(toProcess.indexOf("\"")+1, toProcess.length()-1); //exclude quotes around effect
		player.getRoom().getVerbEffects().put(commandToAdd, newVerbEffect);
	}
	
	public void makeconversationreachable() {
		changeConversationReachable(true);
	}
	
	public void makeconversationuneachable() {
		changeConversationReachable(false);
	}
	
	public void changeConversationReachable(boolean newReachable) {
		//parser.getObjectName() should return something that's formatted:
		//npcName "title of conversation"
		String toProcess = parser.getObjectName();
		boolean startedWithQuote = false; //in case the NPC's name didn't need/have quotes around it
		if(toProcess.charAt(0) == '\"') {
			toProcess = toProcess.substring(1); //strip off opening quote
			startedWithQuote = true;
		}
		String NPCName = toProcess.substring(0, toProcess.indexOf("\"")).trim();
		NPC npc = (NPC)player.getRoom().getObject(NPCName);
		String conversationTitle;
		if(startedWithQuote)
			conversationTitle = toProcess.substring(toProcess.indexOf("\"")+3, toProcess.length()-1);
		else
			conversationTitle = toProcess.substring(toProcess.indexOf("\"")+1, toProcess.length()-1); //exclude quotes around effect
		for(Conversation c : npc.getConversations()) {
			if(conversationTitle.equalsIgnoreCase(c.getTitle())) {
				c.setReachable(newReachable);
				break;
			}
		}
	}

	public void setroomindirection() {
		player.getRoom().setAdjacent(block.get(Integer.parseInt(parser.getIndirectObjectName())), Integer.parseInt(parser.getObjectName()));
	}

	public void makefollow() {
		((TACharacter)parser.getObject()).follow(player);
	}

	public void makeunfollow() {
		((TACharacter)player.getRoom().getObject(parser.getObjectName())).stopFollowing(player);
	}

	/*public void makehostile() {
		((TACharacter)player.getRoom().getObject(parser.getObjectName())).becomeHostileTo(player);
	}

	public void makefriendly() {
		((TACharacter)player.getRoom().getObject(parser.getObjectName())).becomeFriendlyTo(player);
	}*/

	public void setproximity() {
		((TACharacter)player.getRoom().getObject(parser.getObjectName())).setProximity(Integer.parseInt(parser.getIndirectObjectName()));
	}

	public void addtoinventory() {
		player.take(parser.getObject(), false);
	}

	public void addtovertexeffect() {
		NPC npc=null;
		if(parser.getObject() instanceof NPC)
			npc=(NPC)parser.getObject();
		else if(parser.getIndirectObject() instanceof NPC)
			npc=(NPC)parser.getIndirectObject();
		else if(parser.getLastObject() instanceof NPC)
			npc=(NPC)parser.getLastObject();
		else if(parser.getLastIndirectObject() instanceof NPC)
			npc=(NPC)parser.getLastIndirectObject();
		Conversation conversation = npc.getCurrentConversation();
		String[] IDAndEffect = parser.getIndirectObjectName().split(":");
		Vertex targetVertex = conversation.getVertex(Integer.parseInt(IDAndEffect[0]));
		targetVertex.addToEffect(IDAndEffect[1]);
	}
	
	public void removefromvertexeffect() {
		NPC npc=null;
		if(parser.getObject() instanceof NPC)
			npc=(NPC)parser.getObject();
		else if(parser.getIndirectObject() instanceof NPC)
			npc=(NPC)parser.getIndirectObject();
		else if(parser.getLastObject() instanceof NPC)
			npc=(NPC)parser.getLastObject();
		else if(parser.getLastIndirectObject() instanceof NPC)
			npc=(NPC)parser.getLastIndirectObject();
		Conversation conversation = npc.getCurrentConversation();
		String[] IDAndEffect = parser.getIndirectObjectName().split(":");
		Vertex targetVertex = conversation.getVertex(Integer.parseInt(IDAndEffect[0]));
		targetVertex.removeFromEffect(IDAndEffect[1]);
	}

	public void stopsound() {
		soundPlayer.stop(false);
	}

	public void playsound() {
		soundPlayer.play(parser.getObjectName(), false);
	}

	public void loopsound() {
		if(parser.getIndirectObjectName()!=null)
			soundPlayer.loop(parser.getObjectName(), Integer.parseInt(parser.getIndirectObjectName()), SoundPlayer.OFFSETS.get(parser.getObjectName()), false);
		else
			soundPlayer.loop(parser.getObjectName(), SoundPlayer.OFFSETS.get(parser.getObjectName()), false);
	}

	public void addobjective() {
		player.getObjectives().add(parser.getObjectName());
	}

	public void removeObjective() {
		player.getObjectives().remove(parser.getObjectName());
	}

	public void addmapbutton() {
		view.addMapButton();
	}

	public void cleartextarea() {
		view.clearTextArea();
	}

	public void setmapnumber() {
		player.setMapNumber(Integer.parseInt(parser.getObjectName()));
	}

	public void restoreheath() {
		((TACharacter)parser.getObject()).setHP(((TACharacter)parser.getObject()).getMaxHP());
	}

	public void fadetowhite() {
		if(parser.getObjectName()!=null)
			view.fadeToColor(Color.WHITE, Color.WHITE, Integer.parseInt(parser.getObjectName()));
		else
			view.fadeToColor(Color.WHITE, Color.WHITE);
	}
	
	public void fadetocolor() {
		String[] RGB = parser.getObjectName().split(",");
		view.fadeToColor(new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2])));
	}

	public void setbackgroundcolor() {
		String[] RGB = parser.getObjectName().split(",");
		view.setBackgroundColor(new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2])));
		if(parser.getIndirectObjectName() != null) {
			String[] textRGB = parser.getIndirectObjectName().split(",");
			view.setTextColor(new Color(Integer.parseInt(textRGB[0]), Integer.parseInt(textRGB[1]), Integer.parseInt(textRGB[2])));
		}
	}

	public void settextcolor() {
		String[] RGB = parser.getObjectName().split(",");
		view.setTextColor(new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2])));
	}

	public void donothing() {
		//I love this method :)
	}
}

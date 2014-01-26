package textadventure;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.json.JSONObject;

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
			return home+"/Library/Application Support/Text Adventure/";
		return home;
		//TODO figure out a support path for Windows
	}

	public void play() {
		view.print("Would you like to load a saved game (yes or no)? ");
		player.setRoom(block.getFirstRoom());
		view.setGameListener(new GameListener() {
			public void textTyped(String yn) {
				yn=yn.toLowerCase();
				if(yn!=null&&(yn.equals("y")||yn.equals("yes"))) {
					view.print("\n\nPlease enter the name of the saved game.\n\n>");
					view.setGameListener(new GameListener() {
						public void textTyped(String text) {
							view.print("#");
							Room originalRoom=player.getRoom();
							parser.parse("load "+text);
							if(player.getRoom().equals(originalRoom)) {
								view.println("Starting a new game...\n\n");
								start();
							}
							view.setGameListener(null);
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

	public void start() {
		view.setGameListener(null);
		//player.setRoom(block.getFirstRoom());
		view.println("Text Adventure (will change to real name later)\n" +
				"Story by Zachary Birnholz and Jonathan Burns\n" +
				"Programmed by Zachary Birnholz\n" + 
				"Battle system designed by Daniel Palumbo\n" +
				"Music credits TBD\n");
		view.println("Welcome to Text Adventure! Regardless of whether you're a seasoned veteran of text adventures or a new player, it is recommended that you type \"help\" for the basics of how to play once the game begins. Have fun, and thanks for playing!\n\n");
		view.print("Please enter your name: ");
		view.setGameListener(new GameListener() {
			public void textTyped(String name) {
				if(name.equals(""))
					view.print("\n\nI didn't catch your name.\nPlease type it here: ");
				else {
					parser.parse("_setfield me name:"+name.toLowerCase());
					view.setGameListener(null);
					view.println("#    You: \"Yes... there it is, just across the pasture!\"\n\n" +
							"    (You: I'm visiting my former master who taught me everything I know about cartography. I was his apprentice until last year, when I went off to do my own work. "
							+ "Now I'm out here to visit him like he wanted me to, but it was a much longer journey than I remembered… and I've walked all the way from the last town!)^");
					view.println(player.getRoom().getFullText());
					player.getRoom().setIsVisited(true);
					view.println();
					view.updateStatsText();
				}
			}
		});
	}

	public void processCommand(String verb) {
		if(player.getRoom().getVerbEffects()!=null) { //the Room may know what to do
			String command=verb;
			if(parser.getObject()!=null) {
				command+=" "+parser.getObject().getFullName();
			}
			else if(parser.getObjectName()!=null)
				command+=" "+parser.getObjectName();
			String effect=player.getRoom().getVerbEffects().get(command);
			if(effect!=null) {
				StringTokenizer tokenizer=new StringTokenizer(effect, "][}{", true);
				boolean parsedACommand=false;
				char firstCharOfLastToken=' ';
				while(tokenizer.hasMoreTokens()) {
					String toProcess=tokenizer.nextToken();
					if(firstCharOfLastToken=='[')
						view.println(toProcess);
					else if(firstCharOfLastToken=='{') {
						parser.parse(toProcess);
						parsedACommand=true;
					}
					firstCharOfLastToken=toProcess.charAt(0);
				}
				if(parsedACommand)
					return; //don't do anything after the Room has dealt with the command
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
				return; //don't do anything after the DO has dealt with the command
			}
			else if(parser.getObject() instanceof DynamicObject&&((DynamicObject)parser.getObject()).hasEffect(verb)) {
				if(parser.getIndirectObject()==null) {
					//view.println("You can't "+verb+" the "+parser.getObject().getFullName()+" with your bare hands.");
					view.println("What do you want to "+verb+" the "+parser.getObject().getFullName()+" with?");
					parser.lookJustForIndirectObject();
					return;
				}
			}
		}
		if(parser.getIndirectObject()!=null) {										//can't be null because there is a DO
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
		else if(!CommandParser.NON_MOVE_ADDING_VERBS.contains(verb))
			player.addMove();
		Method method;
		try {
			method = Game.class.getMethod(verb, new Class<?>[]{});
			method.invoke(this, new Object[]{});
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
		if(direction==-1)
			view.println("You can only go north, south, east, west, up, or down.");
		else {
			List<String> effects=player.go(direction);
			if(effects==null)
				return;
			for(String str:effects)
				parser.parse(str);
		}
	}

	public void go() {
		/*if(parser.getObjectName()!=null) {
			int direction=(int)Math.floor(GO_DIRECTIONS.indexOf(Character.toUpperCase(parser.getObjectName().charAt(0))+parser.getObjectName().substring(1))/5.0);
			go(direction);
		}
		else*/
		if(parser.getObject()!=null)
			approach();
		else
			view.println("In what direction do you want to go?");
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
						if(player.getRoom().getVerbEffects()!=null&&player.getRoom().getVerbEffects().keySet().contains("take "+obj.getFullName())) {
							parser.parse("take "+obj.getFullName());
							return;
						}
						player.take(obj);
						return;
					}
				}
				for(Chest chest:player.getRoom().getChests()) {
					if(!chest.isOpen())
						continue;
					if(chest.getNumTakeables()==1) {
						TAObject obj=chest.removeTakeable();
						view.println("(the "+obj.getFullName()+")");
						if(player.getRoom().getVerbEffects()!=null&&player.getRoom().getVerbEffects().keySet().contains("take "+obj.getFullName())) {
							chest.addObject(obj);
							parser.parse("take "+obj.getFullName());
							return;
						}
						player.take(obj);
						return;
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
		else if(player.getRoom().contains(parser.getObject()))
			player.take(parser.getObject());
		else {
			if(player.has(parser.getObjectName()))
				view.println("You already have the "+parser.getObjectName()+". Why are you trying to take it again?");
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
			examine();
	}

	public void examine() {
		if(parser.getObject()!=null) {
			if(parser.getObject().equals(player.getRoom())) 
				look();
			else
				view.println(parser.getObject().getDescription());
		}
		else {
			if(parser.objectIsVague()&&!parser.justLookedJustForObjectAdjective()) {
				view.println(getAdjectivePossibilities(parser.getObjectName()));
				parser.lookJustForObjectAdjective();
			}
			else if(parser.hasMultipleWords()) {
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
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
				parser.lookJustForObjectAdjective();
			}
			else
				view.println("I don't see "+parser.getObjectNameWithArticle()+" here.");
		}
		else {
			if(parser.getObject() instanceof TACharacter)
				view.println("The "+parser.getObject().getFullName()+" doesn't have anything to say.");
			else
				view.println("Obviously you can't talk to the "+parser.getObject().getFullName()+".");
		}
	}

	public void enter() {
		int direction=-1;
		for(int i=0; i<6; i++) {
			Room room=player.getRoom().getAdjacent(i);
			if(room!=null&&(room.getName().equalsIgnoreCase(parser.getObjectName())||room.alsoKnownAs(parser.getObjectName()))) {
				if(direction==-1)
					direction=i;
				else {
					view.println("That was a little too vague. Please specify which direction you want to go.");
					return;
				}
			}
		}
		if(direction==-1) {
			if(parser.getObject()==null)
				view.println("There isn't "+parser.getObjectNameWithArticle()+" here to enter.");
			else if(parser.getObject() instanceof NPC)
				view.println("I don't think so.");
			else
				view.println("You can't enter the "+parser.getObject().getFullName()+".");
		}
		else
			go(direction);
	}

	public void attack() {
		//TODO
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
		else if(parser.getObject() instanceof TACharacter) {
			TACharacter object=(TACharacter)parser.getObject();
			if(object.getProximity()<=0)
				view.println("I suppose you could move even closer to the "+parser.getObject().getFullName()+", but it would probably just make them feel uncomfortable.");
			else if(object.getProximity()==1) {
				view.println("The "+parser.getObject().getFullName()+" is already within arm's reach!");
			}
			else {
				object.setProximity(object.getProximity()+1);
				view.println("You move closer to the "+object.getFullName());
				//TODO describe how close they are now
				List<TACharacter> hostileCharacters=player.getRoom().getHostileCharacters();
				if(!hostileCharacters.isEmpty()) {
					String result="In doing so, you move away from:\n";
					boolean shouldPrint=false;
					for(int i=0; i<hostileCharacters.size(); i++) {
						if(hostileCharacters.get(i).getProximity()<BattleCalculator.MAX_PROXIMITY) {
							hostileCharacters.get(i).setProximity(hostileCharacters.get(i).getProximity()+1);
							result+="    The"+hostileCharacters.get(i).getFullName()+"\n";
							shouldPrint=true;
						}
					}
					if(shouldPrint)
						view.println(result);
				}
			}
		}
		else
			view.println("You can see the "+parser.getObject().getFullName()+" just fine from where you are.");
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
		else if(parser.getObject() instanceof TACharacter) {
			TACharacter object=(TACharacter)parser.getObject();
			if(object.getProximity()<=0)
				view.println("Why do you want to move away from the "+parser.getObject().getFullName()+"? They won't bite!");
			else if(object.getProximity()==BattleCalculator.MAX_PROXIMITY) {
				view.println("There is more than enough distance between you and the "+parser.getObject().getFullName()+" (although you may disagree).");
			}
			else {
				object.setProximity(object.getProximity()-1);
				view.println("You move away from the "+object.getFullName());
				//TODO describe how close they are now
				List<TACharacter> hostileCharacters=player.getRoom().getHostileCharacters();
				if(!hostileCharacters.isEmpty()) {
					String result="In doing so, you move closer to:\n";
					boolean shouldPrint=false;
					for(int i=0; i<hostileCharacters.size(); i++) {
						if(hostileCharacters.get(i).getProximity()>1) {
							hostileCharacters.get(i).setProximity(hostileCharacters.get(i).getProximity()-1);
							result+="    The"+hostileCharacters.get(i).getFullName()+"\n";
							shouldPrint=true;
						}
					}
					if(shouldPrint)
						view.println(result);
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
			soundPlayer.loop(player.getRoom().getSoundName(), SoundPlayer.OFFSETS.get(player.getRoom().getSoundName()));
		else
			soundPlayer.stop();
	}

	public void map() {
		if(!player.has("map")) {
			view.println("You don't have a map to look at.");
			return;
		}
		view.println("Please wait for a later version of this game to use the map. Thank you for your patience.");
		//TODO display the map
	}

	public void help() {
		view.println("To move around in the world, just type a direction (\"north\", \"south\", \"east\", \"west\", \"up\", and \"down\" or n, s, e, w, u, d for short). " +
				"To look around type \"look\" (l for short). To take items and carry them with you, type \"take\" and type \"drop\" to drop items from your inventory. " +
				"Type \"examine\" or \"x\" to look at certain objects more closely and type \"inventory\" or \"i\" to see what items you have. \n\n" +
				"You can also save and load games with \"save\" and \"load\". If you are done playing, type \"quit\", or type \"restart\" to start a new game. " +
				"You can toggle sound on/off with \"sound\". In this game, typing \"think\" lets you see your current objectives. " +
				"Some synonyms also work for most verbs, so don't worry if you forget exactly what to type. " +
				"Try out other commands and see what happens! If you are stuck, look around! \n\n" +
				"To see this message again, just type \"help\" or \"h\".");
	}

	public void think() {
		if(player.getObjectives().isEmpty())
			view.println("You don't have anything particular on your mind at the moment. Perhaps now's the time for a little exploring.");
		else {
			view.println("You consider your present situation...");
			for(String str:player.getObjectives())
				view.println("\n--"+str);
		}
	}

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
			soundPlayer.stop();
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
					soundPlayer.stop();
				}
				view.setGameListener(null);
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
				boolean soundIsOn=soundPlayer.soundIsOn();
				soundPlayer=new SoundPlayer(new JSONObject(soundReader.readLine()));
				soundPlayer.setSoundIsOn(soundIsOn);
				soundReader.close();
				BufferedReader playerReader=new BufferedReader(new FileReader(supportPath+"saves/"+saveName+".taf/player.taf"));
				player=new Player(new JSONObject(playerReader.readLine()));
				playerReader.close();
				if(player.getLoadedBlockNumber()!=-1)
					loadBlock(player.getLoadedBlockNumber());
				else
					throw new IOException();
				player.setRoom(block.get(player.getLoadedRoomID()));
				view.println("Saved game loaded from "+saveName+".\n");
				saveName="temp";
				look();
				view.println();
				inventory();
				if(soundPlayer.soundIsOn()&&soundPlayer.getCurrentSoundName()!=null&&!soundPlayer.getCurrentSoundName().equals(""))
					soundPlayer.loop(soundPlayer.getCurrentSoundName(), SoundPlayer.OFFSETS.get(soundPlayer.getCurrentSoundName()));
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
		} catch(Exception e){view.println("There was a problem saving your progress: "+e);}
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
			block=new RoomBlock(Integer.parseInt(roomID.substring(0, roomID.indexOf("/"))), "temp");
			roomID=roomID.substring(roomID.indexOf("/"));
		}
		Room room=block.get(Integer.parseInt(roomID));
		obj.setRoom(room);
		for(TACharacter c:((TACharacter)obj).getFollowingCharacters())
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
			else if(valueStr.charAt(0)=='#')
				value=new Integer(Integer.parseInt(valueStr.substring(1)));
			else if(valueStr.charAt(0)=='&')
				value=new Boolean(Boolean.parseBoolean(valueStr.substring(1)));
			else
				value=valueStr;
			field.set(parser.getObject(), value);
		} catch(Exception e) {view.println("Something went wrong behind the scenes: "+e);e.printStackTrace();}
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

	public void die() {
		view.println("\n\n***YOU HAVE DIED***\nNice going. Maybe you should be more careful next time.");
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
					player=new Player();
					block=new RoomBlock(0, "temp");
					player.setRoom(block.getFirstRoom());
					view.println(player.getRoom().getFullText());
					player.getRoom().setIsVisited(true);
					view.setGameListener(null);
				}
				view.setGameListener(null);
			}
		});
		view.print("Would you like to start again? (yes or no): ");
	}

	public void removeverbeffect() {
		String toRemove=null;
		for(String str:player.getRoom().getVerbEffects().keySet()) {
			if(str.equals(parser.getObjectName())||(str.contains(parser.getObjectName())&&str.contains(" "+parser.getIndirectObjectName()))) {
				toRemove=str;
				break;
			}
		}
		player.getRoom().getVerbEffects().remove(toRemove);
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

	public void addtoconversationeffect() {
		NPC npc=null;
		if(parser.getLastObject() instanceof NPC)
			npc=(NPC)parser.getLastObject();
		else if(parser.getLastIndirectObject() instanceof NPC)
			npc=(NPC)parser.getLastIndirectObject();
		npc.getConversation(npc.getCurrentConversationNumber()).setEffect(npc.getConversation(npc.getCurrentConversationNumber()).getEffect()+parser.getObjectName());
	}

	public void stopsound() {
		soundPlayer.stop();
	}

	public void playsound() {
		soundPlayer.play(parser.getObjectName());
	}

	public void loopsound() {
		if(parser.getIndirectObjectName()!=null)
			soundPlayer.loop(parser.getObjectName(), Integer.parseInt(parser.getIndirectObjectName()), SoundPlayer.OFFSETS.get(parser.getObjectName()));
		else
			soundPlayer.loop(parser.getObjectName(), SoundPlayer.OFFSETS.get(parser.getObjectName()));
	}

	public void addobjective() {
		player.getObjectives().add(parser.getObjectName());
	}

	public void removeObjective() {
		player.getObjectives().remove(parser.getObjectName());
	}

	public void addmapbutton() {
		view.addMapButton();
		System.out.println("map");
	}

	public void cleartextarea() {
		view.clearTextArea();
	}

	public void donothing() {
		//I love this method :)
	}
}

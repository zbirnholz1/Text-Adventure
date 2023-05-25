package textadventure;
import java.io.*;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

public class Player extends TACharacter {
	private int numMoves;
	private List<String> objectives;
	private int loadedRoomID;
	private int loadedBlockNumber;
	private int mapNumber;
	private boolean justChangedProximity;
	private boolean isDead;
	private boolean shouldPrintTake;

	public static final int INVENTORY_CAPACITY=30;
	public static final Map<String, Integer> LIGHT_OBJECTS=initializeLightObjects();

	public Player() {
		name="me";
		otherNames=new HashSet<String>(Arrays.asList("myself", "me", "traveler"));
		description="Looking good.";
		inventory=new TreeSet<TAObject>();
		numMoves=0;
		//objectives=new LinkedList<String>();
		//objectives.add("You've finally made it to your former master's house, but he's clearly not home and something seems amiss. You should investigate a little and see what you can find around here.");
		//objectives.add("That huge oak tree looks suspicious to you and deserves a closer inspection.");
		isPrinted=false;
		HP=15;
		maxHP=20;
		strength=4;
		intelligence=6;
		speed=7;
		mapNumber=0;
		proximity=0;
		justChangedProximity=false;
		shouldPrintTake=true;
	}

	public Player(JSONObject source) {
		super(source);
		isPrinted=false;
		proximity=0;
		justChangedProximity=false;
		shouldPrintTake=true;
		try {
			numMoves=source.getInt("numMoves");
			loadedRoomID=source.getInt("roomID");
			loadedBlockNumber=source.getInt("blockNumber");
			mapNumber=source.getInt("mapNumber");
			//JSONArray JSONObjectives=source.getJSONArray("objectives");
			//objectives=new LinkedList<String>();
			//for(int i=0; i<JSONObjectives.length(); i++)
				//objectives.add(JSONObjectives.getString(i));
		} catch(JSONException e){Main.game.getView().println("Something went wrong: "+e);}
	}

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			obj.put("numMoves", numMoves);
			obj.put("roomID", room.getID());
			obj.put("blockNumber", Main.game.getBlock().getNumber());
			obj.put("mapNumber", mapNumber);
			obj.put("HP", HP);
			obj.put("strength", strength);
			obj.put("intelligence", intelligence);
			obj.put("speed", speed);
			if(armor!=null)
				obj.put("armor", armor.toJSONObject());
			//JSONArray JSONObjectives=new JSONArray();
			//for(String str:objectives)
				//JSONObjectives.put(str);
			//obj.put("objectives", JSONObjectives);
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public static Map<String, Integer> initializeLightObjects() {
		Map<String, Integer> lightObjects=new HashMap<String, Integer>();
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(Player.class.getResourceAsStream("/lightobjects.taf")));
			String line=reader.readLine();
			while(line.charAt(0)=='#')
				line=reader.readLine();
			while(line!=null) {
				lightObjects.put(line.substring(0, line.indexOf(":")), Integer.parseInt(line.substring(line.indexOf(":")+2)));
				line=reader.readLine();
			}
			reader.close();
		} catch(IOException e){e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
		return lightObjects;
	}

	public boolean take(TAObject toTake) {
		return take(toTake, true);
	}

	public boolean take(TAObject toTake, boolean shouldPrint) {
		if(!toTake.isTakeable()) {
			if(toTake.getTakeMessage()==null)
				Main.game.getView().println("You can't take the "+toTake.getFullName()+"!");
			else
				Main.game.getView().println(toTake.takeMessage);
			return false;
		}
		if(inventory.size()<INVENTORY_CAPACITY) {
			if(inventory.add(toTake)) {
				/*if(toTake instanceof Weapon)
					attacks.add((Weapon)toTake);*/
				if(shouldPrint&&shouldPrintTake)
					Main.game.getView().println("Taken.");
				toTake.setIsPrinted(true);
				room.remove(toTake);
				addMove();
				return true;
			}
			else
				Main.game.getView().println("You already have the "+toTake.getFullName()+". How is that possible?");
		}
		else
			Main.game.getView().println("You don't have enough room in your inventory to take the "+toTake.getFullName()+".");
		return false;
	}

	public boolean take(String toTakeStr) {
		return take(toTakeStr, true);
	}

	public boolean take(String toTakeStr, boolean shouldPrint) {
		if(toTakeStr.equals("all")) {
			int numAdded=0;
			int originalNumItemsInRoom=room.getNumTakeables();
			if(originalNumItemsInRoom==0) {
				return false;
			}
			List<TAObject> added=new LinkedList<TAObject>();
			for(TAObject obj:room.getContents()) {
				if(obj.isVisible()&&obj.isTakeable()&&inventory.size()<INVENTORY_CAPACITY&&inventory.add(obj)) {
					inventory.remove(obj);
					numAdded++;
					if(shouldPrint&&shouldPrintTake)
						Main.game.getView().println(obj.getFullNameWithCapitalizedArticle()+": taken");
					added.add(obj);
					/*if(obj instanceof Weapon)
						attacks.add((Weapon)obj);*/
				}
			}
			for(Chest c:room.getChests()) {
				if(!c.isOpen())
					continue;
				for(TAObject obj:c.getContents()) {
					if(obj.isVisible()&&obj.isTakeable()&&inventory.size()<INVENTORY_CAPACITY&&inventory.add(obj)) {
						inventory.remove(obj);
						numAdded++;
						if(shouldPrint&&shouldPrintTake)
							Main.game.getView().println(obj.getFullNameWithCapitalizedArticle()+": taken");
						added.add(obj);
						/*if(obj instanceof Weapon)
							attacks.add((Weapon)obj);*/
					}
					else if(obj.isVisible()&&!obj.isTakeable()&&shouldPrint) {
						if(obj.getTakeMessage()==null)
							Main.game.getView().println(obj.getFullNameWithCapitalizedArticle()+": You can't take the "+obj.getFullName()+"!");
						else
							Main.game.getView().println(obj.getFullNameWithCapitalizedArticle()+": "+obj.getTakeMessage());
					}
				}
			}
			shouldPrintTake=false;
			for(TAObject obj:added) {
				Main.game.getCommandParser().parse("take "+obj.getFullName());
				//room.remove(obj);
			}
			shouldPrintTake=true;
			if(numAdded<originalNumItemsInRoom) {
				Main.game.getView().println("You aren't able to take everything, but you take what you can.");
				addMove();
				return true;
			}
			else if(numAdded==originalNumItemsInRoom) {
				//if(shouldPrint)
				//Main.game.getView().println("Taken.");
				addMove();
				return true;
			}
			return false;
		}
		TAObject toTake=room.getObject(toTakeStr);
		return take(toTake);
	}

	public boolean drop(TAObject toDrop) {
		return drop(toDrop, true);
	}

	public boolean drop(TAObject toDrop, boolean shouldPrint) {
		if(toDrop.getName().equals("backpack")) {
			if(shouldPrint)
				Main.game.getView().println("You can't drop your backpack! If you would like to drop everything in your inventory, you may type \"drop everything\" or \"drop all\".");
			return true;
		}
		if(toDrop instanceof Weapon&&((Weapon)toDrop).isASpell()) {
			Main.game.getView().println("That's a spell you know, not an item you have.");
			return true;
		}
		if(inventory.remove(toDrop)) {
			/*if(toDrop instanceof Weapon)
				attacks.remove((Weapon)toDrop);*/
			if(shouldPrint)
				Main.game.getView().println("Dropped.");
			room.add(toDrop);
			addMove();
			return true;
		}
		return false;
	}

	public boolean drop(String item) {
		return drop(item, true);
	}

	public boolean drop(String item, boolean shouldPrint) {
		if(item.equals("backpack")) {
			if(shouldPrint)
				Main.game.getView().println("You can't drop your backpack! If you would like to drop everything in your inventory, you may type \"drop everything\" or \"drop all\".");
			return true;
		}
		if(item.equals("all")) {
			int originalInventorySize=inventory.size();
			Iterator<TAObject> iter=inventory.iterator();
			if((inventory.size()==1&&!has("backpack"))||(inventory.size()==2&&has("backpack"))) {
				TAObject onlyItem=iter.next();
				if(onlyItem.getName().equals("backpack"))
					onlyItem=iter.next();
				if(onlyItem instanceof Weapon&&((Weapon)onlyItem).isASpell())
					Main.game.getView().println("You have nothing to drop.");
				if(shouldPrint)
					Main.game.getView().println("(the "+onlyItem.getFullName()+")");
				room.add(onlyItem);
				iter.remove();
			}
			while(iter.hasNext()) {
				TAObject obj=iter.next();
				if(obj.getName().equals("backpack"))
					continue;
				else if(obj instanceof Weapon&&((Weapon)obj).isASpell())
					continue;
				room.add(obj);
				iter.remove();
			}
			if(originalInventorySize>0) {
				if(shouldPrint)
					Main.game.getView().println("Dropped.");
				addMove();
				return true;
			}
			return false;
		}
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject obj=iter.next();
			if(obj.getName().equalsIgnoreCase(item)&&!(obj instanceof Weapon&&((Weapon)obj).isASpell())) {
				iter.remove();
				/*if(obj instanceof Weapon)
					attacks.remove((Weapon)obj);*/
				addMove();
				return true;
			}
		}
		return false;
	}

	public List<String> go(int direction) {
		Room newRoom=room.getAdjacent(direction);
		if(newRoom==null) {
			if(room.getDoor(direction)!=null&&!room.getDoor(direction).isOpen())
				Main.game.getView().println(room.getDoor(direction).getClosedMessage(room.getDoor(direction).getNumber(room.getID())));
			else
				Main.game.getView().println("You can't go that way.");
			return null;
		}
		if(newRoom.isDark()&&!hasLight()) {
			Main.game.getView().println(newRoom.getDarkMessage());
			return null;
		}
		if(newRoom.getSoundName()!=null&&Main.game.getSoundPlayer().soundIsOn()&&(room==null||!newRoom.getSoundName().equals(room.getSoundName())))
			Main.game.getSoundPlayer().loop(newRoom.getSoundName(), SoundPlayer.OFFSETS.get(newRoom.getSoundName()), true);
		else if(newRoom.getSoundName()==null)
			Main.game.getSoundPlayer().stop(true);
		setRoom(newRoom);
		if(HP!=0)
			restoreHealth(Math.max((int)(getMaxHP()*0.05), 1));
		if(room.isVisited())
			Main.game.getView().println(room.getShortText());
		else {
			room.setIsVisited(true);
			Main.game.getView().println(room.getFullText());
		}
		String str=room.getEnterEffect();
		if(str==null||str.length()==0)
			return new ArrayList<String>();
		List<String> effects=new ArrayList<String>();
		if(str!=null) {
			StringTokenizer tokenizer=new StringTokenizer(str, "][}{", true);
			char firstCharOfLastToken=' ';
			while(tokenizer.hasMoreTokens()) {
				String toProcess=tokenizer.nextToken();
				if(firstCharOfLastToken=='[')
					Main.game.getView().println(toProcess);
				else if(firstCharOfLastToken=='{') {
					effects.add(toProcess);
				}
				firstCharOfLastToken=toProcess.charAt(0);
			}
		}
		return effects;
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		if(verb.equals("talk") && thisIsDO) {
			return "[Nice weather we're having, isn't it?]";
		}
		return null;
	}

	public void saveInfo() {
		try {
			PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(Game.supportPath+"saves/temp.taf/player.taf")));
			out.println(this.toJSONObject());
			out.close();
		} catch(Exception e){Main.game.getView().println("There was a problem saving your progress: "+e);e.printStackTrace();}
	}

	public void attack(TACharacter defender) {
		Weapon attack;
		if(Main.game.getCommandParser().getIndirectObject() instanceof Weapon)
			attack=(Weapon)Main.game.getCommandParser().getIndirectObject();
		else {
			Main.game.getView().println("The "+Main.game.getCommandParser().getIndirectObject().getFullName()+" is not a suitable weapon whatsoever... but you elect to use it anyways! (Hopefully it made more sense in your head.)");
			attack=BattleCalculator.BASIC_ATTACK;
		}
		String wait="";
		//if(!room.getCharactersBySpeed().get(0).equals(this))
			wait="{1000}";
		for(int i=0; i<attack.getNumAttacks(); i++) {
			int damage=BattleCalculator.calculateDamage(this, defender, attack);
			if(damage==0) {
				Main.game.getView().printlnNPC(wait+defender.getFullName()+" dodged your attack.");
				//TODO print real descriptions
			}
			else {
				if(damage<0)
					Main.game.getView().printlnNPC(wait+"You attack "+defender.getFullName()+" with your "+attack.getFullName()+" and land a critial hit!\nYou deal "+Math.abs(damage)+" damage.");
				else
					Main.game.getView().printlnNPC(wait+"You hit "+defender.getFullName()+" with your "+attack.getFullName()+" and deal "+Math.abs(damage)+" damage.");
				//TODO print real descriptions
				defender.takeDamage(damage);

				if(defender.getHP()==0) {
					Main.game.getView().printlnNPC("{1000}Your attack kills "+defender.getFullName()+"!");
					//TODO print real descriptions
					room.remove(defender);
					break;
				}
			}
		}
	}

	public String getInventoryText() {
		if(inventory.size()==0||(inventory.size()==1&&has("backpack")))
			return "You have nothing in your inventory.";
		String text="Your backpack contains:";
		for(TAObject i:inventory) {
			if(i.getName().equals("backpack"))
				continue;
			text+="\n    "+i.getFullNameWithCapitalizedArticle();
			if(LIGHT_OBJECTS.containsKey(i.getFullName())) {
				if(LIGHT_OBJECTS.get(i.getFullName())==-1)
					text+=" (providing light)";
				else if(i instanceof DynamicObject&&LIGHT_OBJECTS.get(i.getFullName())==((DynamicObject)i).getState())
					text+=" (providing light)";
			}
			if(i.equals(armor))
				text+=" (being worn)";
		}
		return text;
	}
	
	public int getInventorySize() {
		if(has("backpack"))
			return super.getInventorySize()-1;
		return super.getInventorySize();
	}

	public int getNumMoves() {
		return numMoves;
	}

	public int getLoadedRoomID() {
		return loadedRoomID;
	}

	public int getLoadedBlockNumber() {
		return loadedBlockNumber;
	}

	public void addMove() {
		if(Main.game.getCommandParser().isLookingJustForObject()||Main.game.getCommandParser().isLookingJustForObjectAdjective()||Main.game.getCommandParser().isLookingJustForIO()||Main.game.getCommandParser().isLookingJustForIOAdjective())
			return;
		numMoves++;
		justChangedProximity=false;
		BattleCalculator.beginCombat(true);
	}

	public void takeDamage(int amount) {
		super.takeDamage(amount);
		if(HP==0)
			Main.game.getCommandParser().parse("_die");
	}

	public boolean hasLight() {
		for(TAObject obj:inventory) {
			if(LIGHT_OBJECTS.containsKey(obj.getFullName())) {
				if(LIGHT_OBJECTS.get(obj.getFullName())==-1)
					return true;
				else if(obj instanceof DynamicObject&&LIGHT_OBJECTS.get(obj.getFullName())==((DynamicObject)obj).getState())
					return true;
			}
		}
		return false;
	}

	public List<String> getObjectives() {
		return objectives;
	}

	public void setHP(int newHP) {
		super.setHP(newHP);
		Main.game.getView().updateStatsText();
	}

	public void setStrength(int newStrength) {
		super.setStrength(newStrength);
		Main.game.getView().updateStatsText();
	}


	public void setIntelligence(int newIntelligence) {
		super.setIntelligence(newIntelligence);
		Main.game.getView().updateStatsText();
	}


	public void setSpeed(int newSpeed) {
		super.setSpeed(newSpeed);
		Main.game.getView().updateStatsText();
	}


	public void setArmor(Armor newArmor) {
		super.setArmor(newArmor);
		Main.game.getView().updateStatsText();
	}
	
	public void setJustChangedProximity(boolean p) {
		justChangedProximity=p;
	}
	
	public boolean justChangedProximity() {
		return justChangedProximity;
	}

	public void setMapNumber(int num) {
		mapNumber=num;
	}

	public int getMapNumber() {
		return mapNumber;
	}
	
	public void setIsDead(boolean d) {
		isDead=d;
	}
	
	public boolean isDead() {
		return isDead;
	}
	
	public String getFullName() {
		return name;
	}
}

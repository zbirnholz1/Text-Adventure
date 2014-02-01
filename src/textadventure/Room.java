package textadventure;
import java.util.*;

import org.json.*;

public class Room extends TAObject {
	private Room[] adjacentRooms;
	private Door[] doors;
	private boolean visited;
	//private boolean usedInConversation;
	protected int ID;
	private Set<TAObject> contents;
	private Set<StaticObject> staticObjects;
	private Set<DynamicObject> dynamicObjects;
	private Set<Chest> chests;
	private Set<TACharacter> characters;
	private List<List<String>> directionEquivalents;
	private String enterEffect, darkMessage;
	private Map<String, String> verbEffects;
	private String soundName;

	public static final int NORTH=0;
	public static final int SOUTH=1;
	public static final int EAST=2;
	public static final int WEST=3;
	public static final int UP=4;
	public static final int DOWN=5;
	public static final List<String> DIRECTIONS=new ArrayList<String>(Arrays.asList("north", "south", "east", "west", "up", "down", "n", "s", "e", "w", "u", "d"));

	public Room(String n, String d, int north, int s, int e, int w, int u, int down, boolean blah1, boolean blah2, int id) {
		this();
		name=n;
		description=d;
		ID=id;
		String numbers="[";
		if(north==-1)
			numbers+="null,";
		else
			numbers+=north+",";
		if(s==-1)
			numbers+="null,";
		else
			numbers+=s+",";
		if(e==-1)
			numbers+="null,";
		else
			numbers+=e+",";
		if(w==-1)
			numbers+="null,";
		else
			numbers+=w+",";
		if(u==-1)
			numbers+="null,";
		else
			numbers+=u+",";
		if(down==-1)
			numbers+="null";
		else
			numbers+=down;
		numbers+="]";
		String toPrint=toString().replace("[null,null,null,null,null,null]", numbers);
		toPrint=toPrint.substring(1, toPrint.length()-1).replace("\"staticObjects\":[],","").replace("\"dynamicObjects\":[],","").replace("\"NPCs\":[],","").replace("\"chests\":[],","")+",\"staticObjects\":[],\"dynamicObjects\":[],\"NPCs\":[],\"chests\":[]}";
		toPrint="{\"name\":\""+n+"\","+toPrint.substring(0,toPrint.indexOf("\"name"))+toPrint.substring(toPrint.indexOf("\"ID"));
		System.out.println(toPrint);
	}

	public static void main(String[] args) {
		Room roadM16=new Room("Road", "A large road stretching from north to south. The tree with the secret passageway is to the east and a vineyard is to the west.", 19, 17, 15, 28, -1, -1, true, false,16);
		Room roadMS17=new Room("Road", "A large road stretching from north to south. There are some hills in the east and a vineyard to the west.", 16, 18, 22, 27, -1, -1, true, false,17);
		Room roadS18=new Room("Road", "A large road stretching from north to south. There are some hills in the east.", 17, 18, 23, -1, -1, -1, true, false,18);
		Room roadMN19=new Room("Road", "A large road stretching from north to south. There are some hills in the east and a vineyard to the west.", 26, 16, 20, 29, -1, -1, true, false,19);
		Room hillsN20=new Room("Hills", "A hilly area that extends to the south. The road is to the west.", -1, 21, -1, 19, -1, -1, true, false,20);
		Room hillsMN21=new Room("Hills", "A hilly area that runs from north to south. The road is to the west.", 20, 22, -1, 16, -1, -1, true, false,21);
		Room hillsMS22=new Room("Hills", "A hilly area that runs from north to south. The road is to the west. The remains of an avalanche cover a hill in the east.", 21, 23, 24, 17, -1, -1, true, false,22);
		Room hillsS23=new Room("Hills", "A hilly area that runs from north to south. The road is to the west.", 22, 23, -1, 18, -1, -1, true, false,23);
		//make sure to change the description for outsideMine24 once magic is added
		Room outsideMine24=new Room("Avalanche Site", "A large hill strewn with rocks and boulders from an avalanche. The rocks make the hill impassable.", -1, -1, -1, 22, -1, -1, false, true,24);
		Room roadN25=new Room("Road", "The north part of the road at the edge of Anca Canyon. It looks like the bridge across the canyon has been burned down.", 26, 19, -1, 30, -1, -1, true, false,25);
		Room bridge26=new Room("Bridge", "The edge of Anca Canyon. The bridge across the canyon looks like it has been burnt down. There is no other way of crossing.", -1, 25, -1, 30, -1, -1, false, true,26);
		Room vineyardSE27=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north.", 28, 27, 17, 31, -1, -1, true, false,27);
		Room vineyardMSE28=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north.", 29, 27, 16, 32, -1, -1, true, false,28);
		Room vineyardMNE29=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the northwest.", 30, 28, 19, 33, -1, -1, true, false,29);
		Room vineyardNE30=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village in the west.", -1, 29, 25, 38, -1, -1, false, false,30);
		Room vineyardSM31=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north.", 32, 31, 27, 34, -1, -1, true, false,31);
		Room vineyardMM32=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north.", 33, 31, 28, 35, -1, -1, true, false,32);
		Room vineyardNM33=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. A few small huts make up a village directly to the north.", 38, 32, 29, 36, -1, -1, true, false,33);
		Room vineyardSW34=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north. The vines end west of here.", 35, 34, 31, 45, -1, -1, true, false,34);
		Room vineyardMSW35=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village farther to the north. The vines end west of here.", 36, 34, 32, 45, -1, -1, true, false,35);
		Room vineyardMNW36=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village in the northeast. The vines end west of here.", 37, 35, 33, 45, -1, -1, true, false,36);
		Room vineyardNW37=new Room("Vineyard", "A huge vineyard with seemingly endless rows of grape vines. You can see a small village in the east.", -1, 36, 38, 44, -1, -1, false, false,37);
		Room vineyardVillage38=new Room("Village", "A small village with a few huts. There is a fighting dojo at the east end a larger round hut in the north. The doors to all the huts except for the dojo appear to be locked.", -1, 33, 40, -1, -1, -1, false, true,38);
		Room vineyardVillageHut39=new Room("Hut", "A tiny hut with only a table, chairs, and a stove inside.", -1, -1, 38, -1, -1, -1, false, false,39);
		Room vineyardVillageDojo40=new Room("Dojo", "A fighting dojo with many boards and staffs for training.", -1, -1, -1, 38, -1, -1, true, false,40);
		Room vineyardVillageElderHut41=new Room("Village Elder's Hut", "A rather large hut decorated with several scrolls filled with ancient sayings and a few plants. A door opens to a balcony on the north wall.", 42, 38, -1, -1, -1, -1, false, false,41);
		Room vineyardVillageElderBalcony42=new Room("Balcony", "A platform overlooking the vast Anca Canyon. It has a fabulous view, and you can see a river flowing at the bottom of the canyon coming from the snow in the mountains.", -1, 41, -1, -1, -1, 43, true, false,42);
		Room ancaCanyonVillageElderLadder43=new Room("Ladder", "You are on a ladder halfway down the side of Anca Canyon. There is a river below.", -1, -1, -1, -1, 42, 47, true, false,43);
		Room vineyardFoothills44=new Room("Foothills", "The ground begins to slope upward towards the icy mountains, leaving the vineyard behind in the east. There is a temple to the south.", -1, 45, 37, -1, -1, -1, true, true,44);
		Room vineyardTempleDoor45=new Room("Temple Door", "The outside of a circular temple that is surrounded by cryptic pillars. The mountains loom menacingly in the west.", 44, -1, 35, -1, -1, -1, false, true,45);
		Room vineyardTemple46 = new Room("Temple", "The main room of the temple. It is round and contains several statues of different gods. There are other rooms in the temple, but they are only for the monks.", -1, -1, 45, -1, -1, -1, false, true,46);
		Room ancaCanyonM47=new Room("Anca Canyon", "A canyon that continues to the east and gets slightly narrower in the west. A river flows eastward under your feet. A ladder on the rock face ascends to the Village Elder's hut.", -1, -1, 48, 50, 43, -1, true, false,47);
		Room ancaCanyonME48=new Room("Anca Canyon", "A canyon that continues to the east and west. A river flows eastward under your feet. It looks like the workmen are repairing the bridge directly above you.", -1, -1, 49, 47, -1, -1, true, false,48);
		Room ancaCanyonE49=new Room("Anca Canyon", "A canyon that continues to the east and west. A river flows eastward under your feet.", -1, -1, 49, 48, -1, -1, false, false,49);
		Room ancaCanyonMNW50=new Room("Anca Canyon", "A relatively narrow part of the canyon. The canyon continues to the east and takes a sharp turn to the south. The river falls down from a ledge above you in the south.", -1, -1, 47, -1, -1, -1, false, true,50);
		Room ancaCanyonMSW51=new Room("Anca Canyon", "You are at the top of the waterfall which goes down the ledge in the north. The canyon continues to the west.", 50, -1, -1, 52, -1, -1, false, false,51);
		Room ancaCanyonW52=new Room("Anca Canyon", "Anca Canyon abruptly ends at a rock wall in the west with a towering dwarf carved into it. The river emerges from the mouth of a tunnel beneath the dwarf.", -1, -1, 51, 53, -1, -1, true, false,52);
	}

	public Room() {
		adjacentRooms=new Room[6];
		doors=new Door[6];
		contents=new TreeSet<TAObject>();
		contents.add(this);
		staticObjects=new TreeSet<StaticObject>();
		dynamicObjects=new TreeSet<DynamicObject>();
		chests=new TreeSet<Chest>();
		characters=new TreeSet<TACharacter>();
		directionEquivalents=null;
		visited=false;
		isPrinted=false;
		//rest of initialization takes place in RoomBlock
	}

	public boolean equals(Object other) {
		return other instanceof Room && ID==((Room)other).getID();
	}

	public JSONObject toJSONObject() {
		JSONObject obj=new JSONObject();
		try {
			obj.put("name", name);
			obj.put("description", description);
			if(visited)
				obj.put("visited", visited);
			obj.put("ID", ID);
			JSONArray JSONAdjacentIDs=new JSONArray();
			for(Room r:adjacentRooms) {
				if(r==null)
					JSONAdjacentIDs.put((Object)null);
				else
					JSONAdjacentIDs.put(r.getID());
			}
			obj.put("adjacentIDs", JSONAdjacentIDs);
			JSONArray JSONStaticObjects=new JSONArray();
			JSONArray JSONWeapons=new JSONArray();
			JSONArray JSONArmor=new JSONArray();
			for(StaticObject s:staticObjects) {
				if(s instanceof Armor)
					JSONArmor.put(s.toJSONObject());
				else if(s instanceof Weapon)
					JSONWeapons.put(s.toJSONObject());
				else
					JSONStaticObjects.put(s.toJSONObject());
			}
			obj.put("staticObjects", JSONStaticObjects);
			obj.put("weapons", JSONWeapons);
			obj.put("armor", JSONArmor);
			if(verbEffects!=null) {
				for(String str:verbEffects.keySet()) {
					JSONArray JSONVerbEffectPair=new JSONArray();
					JSONVerbEffectPair.put(str);
					JSONVerbEffectPair.put(verbEffects.get(str));
					obj.accumulate("verbEffects", JSONVerbEffectPair);
				}
			}
			JSONArray JSONDynamicObjects=new JSONArray();
			for(DynamicObject d:dynamicObjects)
				JSONDynamicObjects.put(d.toJSONObject());
			obj.put("dynamicObjects", JSONDynamicObjects);
			JSONArray JSONChests=new JSONArray();
			for(Chest c:chests)
				JSONChests.put(c.toJSONObject());
			obj.put("chests", JSONChests);
			JSONArray JSONCharacters=new JSONArray();
			for(TACharacter c:characters) {
				if(!(c instanceof Player))
					JSONCharacters.put(c.toJSONObject());
			}
			obj.put("NPCs", JSONCharacters);
			if(directionEquivalents!=null) {
				JSONArray JSONDirectionEquivalents=new JSONArray();
				for(List<String> list:directionEquivalents) {
					if(list==null) {
						JSONDirectionEquivalents.put((Collection<String>)null);
						continue;
					}
					JSONDirectionEquivalents.put(list);
				}
				obj.put("directionEquivalents", JSONDirectionEquivalents);
			}
			obj.put("otherNames", otherNames);
			if(enterEffect!=null) {
				obj.put("enterEffect", enterEffect);
			}
			if(darkMessage!=null)
				obj.put("darkMessage", darkMessage);
			if(soundName!=null)
				obj.put("soundName", soundName);
		} catch (JSONException e) {Main.game.getView().println("Something went wrong. :"+e);}
		return obj;
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		return null;
	}

	public void add(TAObject obj) {
		if(obj instanceof Door)
			return;
		contents.add(obj);
		if(obj instanceof StaticObject) staticObjects.add((StaticObject)obj);
		else if(obj instanceof TACharacter) characters.add((TACharacter)obj);
		else if(obj instanceof Chest) chests.add((Chest)obj);
		else if(obj instanceof DynamicObject) dynamicObjects.add((DynamicObject)obj);
	}

	public void remove(String obj) {
		Iterator<TAObject> iter=contents.iterator();
		while(iter.hasNext()) {
			TAObject o=iter.next();
			if(o.getName().equals(obj)) {
				remove(o);
				return;
			}
		}
		for(Chest chest:chests) {
			if(chest.isOpen())
				chest.remove(obj);
		}
	}

	public void remove(TAObject obj) {
		contents.remove(obj);
		if(obj instanceof StaticObject) staticObjects.remove((StaticObject)obj);
		else if(obj instanceof TACharacter) characters.remove((TACharacter)obj);
		else if(obj instanceof DynamicObject) dynamicObjects.remove((DynamicObject)obj);
		else if(obj instanceof Chest) chests.remove((Chest)obj);
		for(Chest chest:chests) {
			if(chest.isOpen())
				chest.remove(obj);
		}
	}

	public TAObject getObject(String name) {
		for(TAObject ta:contents)
			if(ta.getName().equalsIgnoreCase(name)) //prioritize the actual name
				return ta;
		for(TAObject ta:contents)
			if(ta.alsoKnownAs(name)) //then check the other names
				return ta;
		for(Chest chest:chests) {
			TAObject obj=chest.getObject(name);
			if(chest.isOpen()&&obj!=null)
				return obj;
		}
		for(int i=0; i<6; i++)
			if(doors[i]!=null&&(doors[i].getName().equalsIgnoreCase(name)||doors[i].alsoKnownAs(name)))
				return doors[i];
		return null;
	}

	public TAObject getObject(String name, String adjective) {
		for(TAObject ta:contents) //prioritize the actual name
			if(ta.getName().equalsIgnoreCase(name)&&((ta.getAdjective()==null&&adjective==null)||(ta.getAdjective()!=null&&ta.getAdjective().equalsIgnoreCase(adjective))))
				return ta;
		for(TAObject ta:contents) //then check the other names
			if(ta.alsoKnownAs(name)&&((ta.getAdjective()==null&&adjective==null)||(ta.getAdjective()!=null&&ta.getAdjective().equalsIgnoreCase(adjective))))
				return ta;
		for(Chest chest:chests) {
			TAObject obj=chest.getObject(name, adjective);
			if(chest.isOpen()&&obj!=null)
				return obj;
		}
		for(int i=0; i<6; i++)
			if(doors[i]!=null&&(doors[i].getName().equalsIgnoreCase(name)||doors[i].alsoKnownAs(name))&&doors[i].getAdjective().equalsIgnoreCase(adjective))
				return doors[i];
		return null;
	}

	public boolean contains(TAObject obj) {
		return contains(obj, false);
	}

	public boolean contains(TAObject obj, boolean systemCall) {
		if(obj==null)
			return false;
		for(TAObject o:contents) {
			if(obj.equals(o)&&(o.isVisible()||systemCall))
				return true;
		}
		for(Chest chest:chests)
			if((chest.isOpen()||systemCall)&&chest.contains(obj))
				return true;
		for(int i=0; i<6; i++)
			if(doors[i]!=null&&doors[i].equals(obj))
				return true;
		return false;
		//return contents.contains(obj)&&obj.isVisible(); <--This doesn't work for some reason
	}

	public boolean contains(String name) {
		return contains(name, false);
	}

	public boolean contains(String name, boolean systemCall) {
		for(TAObject ta:contents)
			if((ta.getName().equalsIgnoreCase(name)||ta.alsoKnownAs(name))&&(ta.isVisible()||systemCall))
				return true;
		for(Chest chest:chests)
			if((chest.isOpen()||systemCall)&&chest.contains(name))
				return true;
		for(int i=0; i<6; i++)
			if(doors[i]!=null&&(doors[i].getName().equalsIgnoreCase(name)||doors[i].alsoKnownAs(name)))
				return true;
		return false;
	}

	public boolean containsMultiple(String name) {
		int count=0;
		for(TAObject ta:contents)
			if((ta.getName().equals(name)||ta.alsoKnownAs(name))&&ta.isVisible())
				count++;
		if(count>1)
			return true;
		for(Chest chest:chests) {
			if(chest.isOpen()&&chest.containsMultiple(name))
				return true;
			else if(chest.isOpen()&&chest.contains(name))
				count++;
		}
		for(int i=0; i<6; i++)
			if(doors[i]!=null&&(doors[i].getName().equalsIgnoreCase(name)||doors[i].alsoKnownAs(name)))
				count++;
		return count>1;
	}

	public List<String> getAdjectives(String name) {
		List<String> adjectives=new LinkedList<String>();
		for(TAObject ta:contents)
			if(ta.getName().equalsIgnoreCase(name)||ta.alsoKnownAs(name)&&ta.isVisible())
				adjectives.add(ta.getAdjective());
		for(Chest chest:chests)
			if(chest.isOpen())
				adjectives.addAll(chest.getAdjectives(name));
		return adjectives;
	}

	public int getEquivalentDirection(String verb, TAObject DO, TAObject IO) { //command is command typed in by the user, so it likely contains prepositions
		if(directionEquivalents==null)
			return -1;
		String str=verb;
		if(DO!=null) {
			str+=" "+DO.getFullName();
		}
		if(IO!=null)
			str+=" "+IO.getFullName();
		for(int i=0; i<directionEquivalents.size(); i++) {
			if(directionEquivalents.get(i)==null)
				continue;
			if(directionEquivalents.get(i).contains(str)&&(doors[i]==null||doors[i].isOpen()))
				return i;
		}
		return -1;
	}

	public int getEquivalentDirection(String cmd) {
		if(directionEquivalents==null)
			return -1;
		for(int i=0; i<6; i++) {
			if(directionEquivalents.get(i)==null)
				continue;
			for(String str:directionEquivalents.get(i)) {
				String[] target=str.split(" ");
				String[] command=cmd.split(" ");
				if(!command[0].equals(target[0]))
					continue;
				for(int commandIndex=1, targetIndex=1; commandIndex<command.length; commandIndex++) {
					if(command[commandIndex].equals(target[targetIndex]))
						targetIndex++;
					if(targetIndex==target.length)
						return i;
				}
			}
		}
		return -1;
	}

	public Set<StaticObject> getStaticObjects() {
		return staticObjects;
	}

	public Set<DynamicObject> getDynamicObjects() {
		return dynamicObjects;
	}

	public Set<Chest> getChests() {
		return chests;
	}

	public Set<TACharacter> getCharacters() {
		return characters;
	}

	public List<TACharacter> getHostileCharacters() {
		List<TACharacter> toReturn=new ArrayList<TACharacter>();
		for(TACharacter t:characters)
			if(t.getProximity()>0)
				toReturn.add(t);
		return toReturn;
	}
	
	public List<TACharacter> getCharactersBySpeed() {
		Set<TACharacter> toReturn=new TreeSet<TACharacter>(new Comparator<TACharacter>() {
			public int compare(TACharacter one, TACharacter two) {
				return two.getSpeed()-one.getSpeed();
			}
		});
		toReturn.addAll(getHostileCharacters());
		if(Main.game.getPlayer().getRoom().equals(this))
			toReturn.add(Main.game.getPlayer());
		return new ArrayList<TACharacter>(toReturn);
	}

	public Set<TAObject> getContents() {
		Set<TAObject> fullContents=contents;
		for(Chest c:chests)
			if(c.isOpen())
				fullContents.addAll(c.getContents());
		return contents;
	}

	public String getFullText() {
		String text=getName()+":\n"+getDescription();
		for(TAObject obj:contents) {
			if(obj.isVisible()) {
				if(obj.isPrinted()) {
					if(obj.isPlural())
						text+="\nThere are some "+obj.getFullNameWithArticle()+" here.";
					else
						text+="\nThere is "+obj.getFullNameWithArticle()+" here.";
				}
				if(obj instanceof Chest&&((Chest)obj).isOpen()&&((Chest)obj).hasItems()) {
					text+="\nThe ";
					if(obj.getAdjective()!=null)
						text+=obj.getAdjective()+" ";
					text+=obj.getName()+" contains:"+((Chest)obj).getInventoryText();
				}
			}

		}
		for(TACharacter c:characters)
			if(!c.equals(Main.game.getPlayer())&&c.isVisible()&&c.isPrinted()&&!Main.game.getPlayer().getFollowingCharacters().contains(c))
				text+="\nThe "+c.getFullName()+" is standing here.";
		return text;
	}

	public String getShortText() {
		return getFullText();
		/*String text=getName();
		for(TAObject obj:contents) {
			if(obj.isPrinted()&&obj.isVisible()) {
				text+="\nThere is a";
				if(obj.startsWithVowel())
					text+="n";
				text+=" "+obj.getFullName()+" here.";
			}
		}
		return text;*/
	}

	public Room getAdjacent(int direction) throws IllegalArgumentException {
		try {
			if(doors[direction]!=null&&!doors[direction].isOpen()) {
				//Main.game.getView().println(doors[direction].getClosedMessage(doors[direction].getNumber(ID)));
				return null;
			}
			Room newRoom=adjacentRooms[direction];
			if(newRoom instanceof BoundaryRoom) {
				Main.game.saveBlock(Main.game.getBlock());
				Main.game.getPlayer().saveInfo();
				RoomBlock newBlock=new RoomBlock(((BoundaryRoom)newRoom).getNextBlockNumber(), "temp" /*Main.game.getSaveName()*/);
				Main.game.setBlock(newBlock);
				return newBlock.get(((BoundaryRoom)newRoom).getNextRoomID());
			}
			return newRoom;
		} catch(ArrayIndexOutOfBoundsException e) {throw new IllegalArgumentException("Direction must be between 0 (NORTH) and 5 (DOWN)");}
	}

	public boolean isVisited() {
		return visited;
	}

	/*public boolean isUsedInConversation() {
		return usedInConversation;
	}*/

	public int getNumTakeables() {
		int numTakeables=0;
		for(TAObject obj:contents) {
			if(obj.isTakeable()&&obj.isVisible())
				numTakeables++;
		}
		for(Chest chest:chests)
			numTakeables+=chest.getNumTakeables();
		return numTakeables;
	}

	public int getID() {
		return ID;
	}

	public Door getDoor(int direction) throws IllegalArgumentException {
		try {
			return doors[direction];
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("0<=direction<6");}
	}

	public boolean isDark() {
		return darkMessage!=null;
	}

	public String getDarkMessage() {
		return darkMessage;
	}

	public String getEnterEffect() {
		return enterEffect;
	}

	public Map<String, String> getVerbEffects() {
		return verbEffects;
	}

	public String getSoundName() {
		return soundName;
	}

	public void setAdjacent(Room newRoom, int direction) throws IllegalArgumentException {
		try {
			adjacentRooms[direction]=newRoom;
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("0<=direction<6");}
	}

	public void setDoor(Door newDoor, int direction) throws IllegalArgumentException {
		try {
			doors[direction]=newDoor;
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("0<=direction<6");}
	}

	public void setIsVisited(boolean visited) {
		this.visited = visited;
	}

	/*public void setUsedInConversation(boolean usedInConversation) {
		this.usedInConversation = usedInConversation;
	}*/

	public void setID(int ID) {
		this.ID = ID;
	}

	public void setDirectionEquivalents(List<List<String>> list) {
		directionEquivalents=list;
	}

	public void setEnterEffect(String str) {
		enterEffect=str;
	}

	public void setVerbEffects(Map<String, String> map) {
		verbEffects=map;
	}
	public void setDarkMessage(String m) {
		darkMessage=m;
	}

	public void setSoundName(String s) {
		soundName=s;
	}

	public int hashCode() {
		return ID;
	}
}

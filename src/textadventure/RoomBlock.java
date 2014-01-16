package textadventure;
import java.io.*;
import java.util.*;
import org.json.*;

public class RoomBlock {
	private Map<Integer, Room> rooms;
	private BufferedReader reader;
	private int maxID, minID;
	private int blockNumber;

	public static final Map<Integer, Integer> blockLocations=initializeBlockLocations(); //contains Room IDs (switch to actual Rooms?) that
	//point to the numbers of the blocks they begin
	//will initialize this when the game begins
	//RoomBlock files are formatted:
	//minID
	//maxID
	//One room per line...
	//-----
	//One door per line...
	public RoomBlock(int number, String saveName) {
		blockNumber=number;
		String path="block"+blockNumber+".taf";
		if(new File(Game.supportPath+"saves/"+saveName+".taf/"+path).exists())
			path=Game.supportPath+"saves/"+saveName+".taf/"+path;
		else if(new File(Game.supportPath+"saves/temp.taf/"+path).exists())
			path=Game.supportPath+"saves/temp.taf/"+path;
		else
			path="/blocks/"+path;
		try {
			if(path.contains(Game.supportPath))
				reader=new BufferedReader(new FileReader(path));
			else
				reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path))); //name Block files "block#.taf"
			rooms=new HashMap<Integer, Room>();
			Map<Integer, List<Integer>> adjacentIDs=new HashMap<Integer, List<Integer>>();
			minID=Integer.parseInt(reader.readLine());
			maxID=Integer.parseInt(reader.readLine());
			//load the Rooms from a file
			String line=reader.readLine(); //or however many lines constitute one Room
			while(line!=null&&!line.equals("-----")) {
				JSONObject source=new JSONObject(line);
				Room room=new Room();
				//initialize room's instance variables:
				room.setName(source.getString("name"));
				room.setDescription(source.getString("description"));
				if(source.has("visited"))
					room.setIsVisited(source.getBoolean("visited"));
				else
					room.setIsVisited(false);
				if(source.has("directionEquivalents")) {
					JSONArray JSONDirectionEquivalents=source.getJSONArray("directionEquivalents");
					ArrayList<List<String>> directionEquivalents=new ArrayList<List<String>>(6);
					for(int i=0; i<6; i++) {
						if(!JSONDirectionEquivalents.isNull(i)) {
							JSONArray specificDirection=JSONDirectionEquivalents.getJSONArray(i);
							ArrayList<String> specificDirectionEquivalents=new ArrayList<String>(specificDirection.length());
							for(int j=0; j<specificDirection.length(); j++)
								specificDirectionEquivalents.add(specificDirection.getString(j));
							directionEquivalents.add(specificDirectionEquivalents);
						}
						else
							directionEquivalents.add(null);
					}
					room.setDirectionEquivalents(directionEquivalents);
				}
				if(source.has("otherNames")) {
					JSONArray JSONOtherNames=source.getJSONArray("otherNames");
					HashSet<String> otherNames=new HashSet<String>();
					for(int i=0; i<JSONOtherNames.length(); i++)
						otherNames.add(JSONOtherNames.getString(i));
					room.setOtherNames(otherNames);
				}
				if(source.has("enterEffect")) {
					room.setEnterEffect(source.getString("enterEffect"));
				}
				if(source.has("verbEffects")) {
					JSONArray JSONVerbEffects=source.getJSONArray("verbEffects");
					Map<String, String> verbEffects=new HashMap<String, String>(JSONVerbEffects.length());
					for(int i=0; i<JSONVerbEffects.length(); i++)
						verbEffects.put(JSONVerbEffects.getJSONArray(i).getString(0), JSONVerbEffects.getJSONArray(i).getString(1));
					room.setVerbEffects(verbEffects);
				}
				if(source.has("darkMessage"))
					room.setDarkMessage(source.getString("darkMessage"));
				if(source.has("soundName"))
					room.setSoundName(source.getString("soundName"));
				room.setID(source.getInt("ID"));
				rooms.put(new Integer(room.getID()), room);
				List<Integer> IDs=new ArrayList<Integer>(6);
				JSONArray JSONIDs=source.getJSONArray("adjacentIDs");
				for(int i=0; i<6; i++) {
					if(!JSONIDs.isNull(i))
						IDs.add(JSONIDs.getInt(i));
					else
						IDs.add(null);
				}
				adjacentIDs.put(new Integer(room.getID()), IDs);
				if(source.has("staticObjects")) {
					try { //put all the items into the Room
						JSONArray JSONStaticObjects=source.getJSONArray("staticObjects");
						for(int i=0; i<JSONStaticObjects.length(); i++)
							room.add(new StaticObject(JSONStaticObjects.getJSONObject(i)));
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
				}
				if(source.has("weapons")) {
					try { //put all the Weapons into the Room
						JSONArray JSONWeapons=source.getJSONArray("weapons");
						for(int i=0; i<JSONWeapons.length(); i++)
							room.add(new Weapon(JSONWeapons.getJSONObject(i)));
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
				}
				if(source.has("armor")) {
					try { //put all the Weapons into the Room
						JSONArray JSONArmor=source.getJSONArray("armor");
						for(int i=0; i<JSONArmor.length(); i++)
							room.add(new Armor(JSONArmor.getJSONObject(i)));
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
				}
				if(source.has("dynamicObjects")) {
					try { //put all the DynamicObjects into the Room
						JSONArray JSONDynamicObjects=source.getJSONArray("dynamicObjects");
						for(int i=0; i<JSONDynamicObjects.length(); i++)
							room.add(new DynamicObject(JSONDynamicObjects.getJSONObject(i)));
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
				}
				if(source.has("chests")) {
					try { //put all the chests into the Room
						JSONArray JSONChests=source.getJSONArray("chests");
						for(int i=0; i<JSONChests.length(); i++)
							room.add(new Chest(JSONChests.getJSONObject(i)));
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
				}
				if(source.has("NPCs")) {
					try { //put all the NPCs into the Room
						JSONArray JSONNPCs=source.getJSONArray("NPCs");
						for(int i=0; i<JSONNPCs.length(); i++) {
							NPC npc=new NPC(JSONNPCs.getJSONObject(i));
							npc.setRoom(room);
						}
					} catch(JSONException ex){Main.game.getView().println("Something went wrong: "+ex);}
					for(TACharacter c:room.getCharacters()) {
						for(String f:c.getFollowingCharacterNames())
							((TACharacter)room.getObject(f)).follow(c);
						/*for(String h:c.getHostileCharacterNames())
							((TACharacter)room.getObject(h)).becomeHostileTo(c);*/
					}
				}
				line=reader.readLine(); //or however many lines constitute one Room
			}
			if(line!=null&&line.equals("-----"))
				line=reader.readLine();
			while(line!=null) {
				//put the doors in the rooms
				Door door=null;
				try{door=new Door(new JSONObject(line));}catch(JSONException e){Main.game.getView().println("Something went wrong: "+e);}
				if(door.getID(1)>=minID&&door.getID(1)<=maxID) {
					rooms.get(door.getID(1)).setDoor(door, door.getDirection(1));
					rooms.get(door.getID(1)).add(door);
				}
				if(door.getID(2)>=minID&&door.getID(2)<=maxID) {
					rooms.get(door.getID(2)).setDoor(door, door.getDirection(2));
					rooms.get(door.getID(2)).add(door);
				}
				line=reader.readLine();
			}
			//connect the Rooms
			for(Integer currentRoomID:rooms.keySet()) {
				Room room=rooms.get(currentRoomID);
				List<Integer> IDs=adjacentIDs.get(currentRoomID);
				for(int direction=0; direction<6; direction++) {
					if(IDs.get(direction)!=null&&(IDs.get(direction).intValue()>maxID||IDs.get(direction).intValue()<minID)) { //if the adjacent Room in the current direction is out of this RoomBlock
						int blockDeterminingID=-1; //represents the ID of the first Room in the block that the next Room is in
						for(Integer blockDeterminingIDFinder:blockLocations.keySet()) //goes through the RoomBlocks to find which block the Room is in
							if(blockDeterminingIDFinder<=IDs.get(direction).intValue()&&blockDeterminingIDFinder>blockDeterminingID)
								blockDeterminingID=blockDeterminingIDFinder;
						room.setAdjacent(new BoundaryRoom(currentRoomID, IDs.get(direction), blockLocations.get(blockDeterminingID)), direction);
					}
					else
						room.setAdjacent(rooms.get(IDs.get(direction)), direction);
				}
			}
		} catch (Exception e){System.out.println("Something went wrong RB: "+e);e.printStackTrace(System.out);}
	}

	public Room getFirstRoom() {
		return rooms.get(minID);
	}

	public Room get(int ID) {
		return rooms.get(ID);
	}

	public int getNumber() {
		return blockNumber;
	}

	public int getMinID() {
		return minID;
	}

	public int getMaxID() {
		return maxID;
	}

	public Map<Integer, Room> getRooms() {
		return rooms;
	}

	public static Map<Integer, Integer> initializeBlockLocations() {
		Map<Integer, Integer> toReturn=new HashMap<Integer, Integer>();
		try {
			String path="/blocklocations.taf";
			BufferedReader br=new BufferedReader(new InputStreamReader(RoomBlock.class.getResourceAsStream(path)));
			String line=br.readLine();
			while(line!=null) {
				//lines will read: "ID of first Room, number of Block it starts"
				Integer startID=new Integer(Integer.parseInt(line.substring(0, line.indexOf(","))));
				Integer blockNumber=new Integer(Integer.parseInt(line.substring(line.indexOf(" ")+1)));
				toReturn.put(startID, blockNumber);
				line=br.readLine();
			}
			br.close();
		} catch(IOException e){e.printStackTrace();Main.game.getView().println("Something went wrong: "+e);}
		return toReturn;
	}
}

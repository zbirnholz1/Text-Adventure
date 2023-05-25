package textadventure;
import java.util.*;

import org.json.*;

public abstract class TACharacter extends TAObject {
	protected Set<TAObject> inventory;
	//protected Set<Weapon> attacks;
	protected List<TACharacter> followingCharacters;
	protected List<String> followingCharacterNames;
	/*protected List<TACharacter> hostileCharacters;
	protected List<String> hostileCharacterNames;*/
	protected int proximity;
	protected Room room;
	//protected int HP, etc. OR protected int[] stats (which is better?)
	protected int HP;
	protected int maxHP;
	protected int strength;
	protected int intelligence;
	protected int speed;
	protected Armor armor;

	public TACharacter() {
		super();
		inventory=new TreeSet<TAObject>();
		followingCharacters=new LinkedList<TACharacter>();
		followingCharacterNames=new LinkedList<String>();
		/*hostileCharacters=new LinkedList<TACharacter>();
		hostileCharacterNames=new LinkedList<String>();*/
		//attacks=new TreeSet<Weapon>();
	}

	public TACharacter(JSONObject source) {
		super(source);
		try {
			inventory=new TreeSet<TAObject>();
			if(source.has("staticObjects")) {
				JSONArray JSONStaticObjects=source.getJSONArray("staticObjects");
				for(int i=0; i<JSONStaticObjects.length(); i++)
					inventory.add(new StaticObject(JSONStaticObjects.getJSONObject(i)));
			}
			if(source.has("dynamicObjects")) {
				JSONArray JSONDynamicObjects=source.getJSONArray("dynamicObjects");
				for(int i=0; i<JSONDynamicObjects.length(); i++)
					inventory.add(new DynamicObject(JSONDynamicObjects.getJSONObject(i)));
			}
			if(source.has("chests")) {
				JSONArray JSONChests=source.getJSONArray("chests");
				for(int i=0; i<JSONChests.length(); i++)
					inventory.add(new Chest(JSONChests.getJSONObject(i)));
			}
			if(source.has("weapons")) {
				JSONArray JSONWeapons=source.getJSONArray("weapons");
				for(int i=0; i<JSONWeapons.length(); i++)
					inventory.add(new Weapon(JSONWeapons.getJSONObject(i)));
			}
			followingCharacters=new LinkedList<TACharacter>();
			followingCharacterNames=new LinkedList<String>();
			if(source.has("followingCharacters")) {

				JSONArray JSONFollowingCharacterNames=source.getJSONArray("followingCharacters");
				for(int i=0; i<JSONFollowingCharacterNames.length(); i++)
					followingCharacterNames.add(JSONFollowingCharacterNames.getString(i));
			}
			/*hostileCharacters=new LinkedList<TACharacter>();
			hostileCharacterNames=new LinkedList<String>();
			if(source.has("hostileCharacters")) {
				JSONArray JSONHostileCharacterNames=source.getJSONArray("hostileCharacters");
				for(int i=0; i<JSONHostileCharacterNames.length(); i++)
					hostileCharacterNames.add(JSONHostileCharacterNames.getString(i));
			}*/
			if(source.has("proximity"))
				proximity=source.getInt("proximity");
			else
				proximity=-1;
			if(proximity>=0) {
				HP=source.getInt("HP");
				strength=source.getInt("strength");
				if(source.has("maxHP"))
					maxHP=source.getInt("maxHP");
				else
					maxHP=strength*10;
				intelligence=source.getInt("intelligence");
				speed=source.getInt("speed");
				if(source.has("armor"))
					armor=new Armor(source.getJSONObject("armor"));
			}
			//attacks=new TreeSet<Weapon>();
			if(source.has("weapons")) {
				JSONArray JSONWeapons=source.getJSONArray("weapons");
				for(int i=0; i<JSONWeapons.length(); i++) {
					Weapon w=new Weapon(JSONWeapons.getJSONObject(i));
					//attacks.add(w);
					inventory.add(w);
				}
			}
			//TODO stats, money, etc.
		} catch(JSONException e){Main.game.getView().println("Something went wrong: "+e);}
		//Adds in the following and hostile characters in RoomBlock
	}

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			JSONArray JSONStaticObjects=new JSONArray();
			JSONArray JSONDynamicObjects=new JSONArray();
			JSONArray JSONChests=new JSONArray();
			JSONArray JSONWeapons=new JSONArray();
			for(TAObject o:inventory) {
				if(o instanceof StaticObject)
					JSONStaticObjects.put(o.toJSONObject());
				else if(o instanceof DynamicObject)
					JSONDynamicObjects.put(o.toJSONObject());
				else if(o instanceof Chest)
					JSONChests.put(o.toJSONObject());
				else if(o instanceof Weapon)
					JSONWeapons.put(o.toJSONObject());
			}
			obj.put("staticObjects", JSONStaticObjects);
			obj.put("dynamicObjects", JSONDynamicObjects);
			obj.put("chests", JSONChests);
			obj.put("weapons", JSONWeapons);
			for(TACharacter c:followingCharacters)
				obj.accumulate("followingCharacters", c.getName());
			/*for(TACharacter c:hostileCharacters)
				obj.accumulate("hostileCharacters", c.getName());*/
			obj.put("proximity", proximity);
			if(proximity>=0) {
				obj.put("HP", HP);
				obj.put("maxHP", maxHP);
				obj.put("strength", strength);
				obj.put("intelligence", intelligence);
				obj.put("speed", speed);
				if(armor!=null)
					obj.put("armor", armor.toJSONObject());
			}
			/*for(Weapon w:attacks)
				obj.accumulate("weapons", w.toJSONObject());*/
			//TODO stats, money, etc.
		} catch(JSONException e){Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public void follow(TACharacter otherCharacter) {
		if(!followingCharacters.contains(otherCharacter)) //to avoid infinite loops
			otherCharacter.getFollowingCharacters().add(this);
	}

	public void stopFollowing(TACharacter otherCharacter) {
		otherCharacter.getFollowingCharacters().remove(this);
	}

	/*public void becomeHostileTo(TACharacter otherCharacter) {
		otherCharacter.getHostileCharacters().add(this);
		this.getHostileCharacters().add(otherCharacter);
	}

	public void becomeFriendlyTo(TACharacter otherCharacter) {
		otherCharacter.getHostileCharacters().remove(this);
		this.getHostileCharacters().remove(otherCharacter);
	}*/

	public abstract List<String> go(int direction);

	public boolean has(TAObject item) {
		return inventory.contains(item);
	}

	public boolean has(String name) {
		for(TAObject i:inventory)
			if(i.getName().equals(name)||i.alsoKnownAs(name))
				return true;
		return false;
	}

	public boolean has(String name, String adjective) {
		for(TAObject i:inventory)
			if((i.getName().equals(name)||i.alsoKnownAs(name))&&(i.getAdjective()==null&&adjective==null)||(i.getAdjective()!=null&&i.getAdjective().equals(adjective)))
				return true;
		return false;
	}

	public TAObject get(String name) {
		for(TAObject i:inventory) {
			if(i.getName().equalsIgnoreCase(name)||i.alsoKnownAs(name))
				return i;
		}
		return null;
	}

	public TAObject get(String name, String adjective) {
		for(TAObject i:inventory) //prioritize the actual name
			if(i.getName().equals(name)&&(i.getAdjective()==null&&adjective==null)||(i.getAdjective()!=null&&i.getAdjective().equals(adjective)))
				return i;
		for(TAObject i:inventory) //then check the other names
			if(i.alsoKnownAs(name)&&(i.getAdjective()==null&&adjective==null)||(i.getAdjective()!=null&&i.getAdjective().equals(adjective)))
				return i;
		return null;
	}
	
	public List<Weapon> getWeapons() {
		List<Weapon> toReturn=new LinkedList<Weapon>();
		for(TAObject o:inventory) {
			if(o instanceof Weapon)
				toReturn.add((Weapon)o);
		}
		return toReturn;
	}

	public boolean delete(String toDelete) {
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject obj=iter.next();
			if(obj.getName().equals(toDelete)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	public boolean delete(TAObject toDelete) {
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject obj=iter.next();
			if(obj.equals(toDelete)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	public void setRoom(Room newRoom) throws IllegalArgumentException {
		if(room!=null)
			room.remove(this);
		if(newRoom==null)
			throw new IllegalArgumentException("A TACharacter can't be in a null Room.");
		room=newRoom;
		room.add(this);
		for(TACharacter c:followingCharacters)
			c.setRoom(room);
	}

	public String getFullName() {
		if(name.length()==0)
			return "traveler";
		String toReturn=Character.toUpperCase(name.charAt(0))+name.substring(1);
		if(adjective!=null)
			toReturn=Character.toUpperCase(adjective.charAt(0))+adjective.substring(1)+" "+toReturn;
		return toReturn;
	}

	public int getInventorySize() {
		return inventory.size();
	}
	
	public Set<TAObject> getInventory() {
		return inventory;
	}

	public Room getRoom() {
		return room;
	}

	public List<TACharacter> getFollowingCharacters() {
		return followingCharacters;
	}

	public List<String> getFollowingCharacterNames() {
		return followingCharacterNames;
	}

	/*public List<TACharacter> getHostileCharacters() {
		return hostileCharacters;
	}

	public List<String> getHostileCharacterNames() {
		return hostileCharacterNames;
	}*/

	public abstract void attack(TACharacter defender); //only called if it's the player or isHostile()==true

	public void setProximity(int newProximity) {
		proximity=newProximity;
	}

	public int getProximity() {
		return proximity;
	}
	
	public boolean isHostile() {
		return proximity>0;
	}

	public int getHP() {
		return HP;
	}

	public void setHP(int newHP) {
		HP=newHP;
	}

	public int getMaxHP() {
		return maxHP;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int newStrength) {
		strength=newStrength;
	}

	public int getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(int newIntelligence) {
		intelligence=newIntelligence;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int newSpeed) {
		speed=newSpeed;
	}

	public Armor getArmor() {
		return armor;
	}

	public void setArmor(Armor newArmor) {
		armor=newArmor;
	}

	public void takeDamage(int amount) {
		setHP(Math.max(0, getHP()-Math.abs(amount)));
	}

	public void restoreHealth(int amount) {
		setHP(Math.min(getMaxHP(), HP+amount));
	}
}

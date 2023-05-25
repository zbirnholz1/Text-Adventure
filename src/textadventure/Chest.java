package textadventure;
import java.util.*;

import org.json.*;

public class Chest extends DynamicObject {
	private Set<TAObject> inventory;
	//state==0 means locked, state==1 means closed, state==2 means open

	public Chest(JSONObject source) {
		super(source);
		try {
			inventory=new TreeSet<TAObject>();
			if(source.has("staticObjects")) {
				JSONArray JSONItems=source.getJSONArray("staticObjects");
				for(int i=0; i<JSONItems.length(); i++)
					inventory.add(new StaticObject(JSONItems.getJSONObject(i)));
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
		} catch(JSONException e){Main.game.getView().println("Something went wrong C: "+e);}
	}

	public Set<TAObject> emptyInventory() {
		Set<TAObject> inventoryCopy=new HashSet<TAObject>(inventory.size());
		inventoryCopy.addAll(inventory);
		inventory.clear();
		return inventoryCopy;
	}

	public JSONObject toJSONObject() {
		LinkedList<JSONObject> staticObjects=new LinkedList<JSONObject>();
		LinkedList<JSONObject> dynamicObjects=new LinkedList<JSONObject>();
		LinkedList<JSONObject> chests=new LinkedList<JSONObject>();
		for(TAObject obj:inventory) {
			if(obj instanceof StaticObject)
				staticObjects.add(obj.toJSONObject());
			else if(obj instanceof Chest)
				chests.add(obj.toJSONObject());
			else if(obj instanceof DynamicObject)
				dynamicObjects.add(obj.toJSONObject());
		}
		JSONObject obj=super.toJSONObject();
		try {
			if(staticObjects.size()!=0)
				obj.put("staticObjects", staticObjects);
			if(chests.size()!=0)
				obj.put("chests", chests);
			if(dynamicObjects.size()!=0)
				obj.put("dynamicObjects", dynamicObjects);
		} catch(JSONException e){Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public String process(String command, TAObject otherObject, boolean thisIsDO) {
		int startingState=state;
		String toReturn=super.process(command, otherObject, thisIsDO);
		if(state!=startingState&&state==2) {
			if(inventory.size()==0)
				toReturn+="[There is nothing in the "+getFullName()+".]";
			else
				toReturn+="[The "+getFullName()+" contains: "+getInventoryText()+"]";
		}
		return toReturn;
	}

	public boolean contains(TAObject obj) {
		for(TAObject ta:inventory) {
			//if(ta.getName().equalsIgnoreCase(obj.getName())&&((ta.getAdjective()==null&&obj.getAdjective()==null)||(ta.getAdjective()!=null&&ta.getAdjective().equalsIgnoreCase(obj.getAdjective()))))
			if(ta.equals(obj))
				return true;
			else if(ta instanceof Chest)
				if(((Chest)ta).isOpen()&&((Chest)ta).contains(obj))
					return true;
		}
		for(TAObject ta:inventory) {
			if(ta.alsoKnownAs(obj.getName())&&((ta.getAdjective()==null&&obj.getAdjective()==null)||(ta.getAdjective()!=null&&ta.getAdjective().equalsIgnoreCase(obj.getAdjective()))))
				return true;
			else if(ta instanceof Chest)
				if(((Chest)ta).isOpen()&&((Chest)ta).contains(obj))
					return true;
		}
		return false;
	}

	public boolean contains(String name) {
		for(TAObject ta:inventory) {
			if(ta.getName().equalsIgnoreCase(name)||ta.alsoKnownAs(name))
				return true;
			else if(ta instanceof Chest)
				if(((Chest)ta).isOpen()&&((Chest)ta).contains(name))
					return true;
		}
		return false;
	}

	public boolean containsMultiple(String name) {
		int count=0;
		for(TAObject ta:inventory) {
			if(ta.getName().equalsIgnoreCase(name)||ta.alsoKnownAs(name))
				count++;
			if(ta instanceof Chest) {
				if(((Chest)ta).isOpen()) {
					if(((Chest)ta).contains(name))
						count++;
					else if(((Chest)ta).containsMultiple(name))
						return true;
				}
			}
		}
		return count>1;
	}
	
	public void addObject(TAObject obj) {
		inventory.add(obj);
	}

	public TAObject getObject(String name) {
		if(!isOpen())
			return null;
		for(TAObject ta:inventory)
			if(ta.getName().equalsIgnoreCase(name)) //prioritize the actual name
				return ta;
		for(TAObject ta:inventory) {
			if(ta.alsoKnownAs(name)) //then check the other names
				return ta;
			if(ta instanceof Chest) {
				TAObject possibleReturn=((Chest)ta).getObject(name);
				if(possibleReturn!=null)
					return possibleReturn;
			}
		}
		return null;
	}

	public TAObject getObject(String name, String adjective) {
		if(!isOpen())
			return null;
		for(TAObject ta:inventory)
			if(ta.getName().equalsIgnoreCase(name)&&((ta.getAdjective()==null&&adjective==null)||(ta.getAdjective()!=null&&ta.getAdjective().equals(adjective))))
				return ta;
		for(TAObject ta:inventory) {
			if(ta.alsoKnownAs(name)&&((ta.getAdjective()==null&&adjective==null)||(ta.getAdjective()!=null&&ta.getAdjective().equals(adjective))))
				return ta;
			if(ta instanceof Chest) {
				TAObject possibleReturn=((Chest)ta).getObject(name, adjective);
				if(possibleReturn!=null)
					return possibleReturn;
			}
		}
		return null;
	}

	public TAObject remove(TAObject obj) {
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject o=iter.next();
			if(o.equals(obj)) {
				iter.remove();
				return o;
			}
			if(o instanceof Chest) {
				TAObject potentialReturn=((Chest)o).remove(obj);
				if(potentialReturn!=null)
					return potentialReturn;
			}
		}
		return null;
	}

	public TAObject remove(String obj) {
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject o=iter.next();
			if(o.getName().equals(obj)||o.alsoKnownAs(obj)) {
				iter.remove();
				return o;
			}
			if(o instanceof Chest) {
				TAObject potentialReturn=((Chest)o).remove(obj);
				if(potentialReturn!=null)
					return potentialReturn;
			}
		}
		return null;
	}

	/*
	 * Precondition: this Chest only contains one takeable object
	 * Removes and returns the first (and only) takeable encountered
	 */
	public TAObject removeTakeable() {
		Iterator<TAObject> iter=inventory.iterator();
		while(iter.hasNext()) {
			TAObject o=iter.next();
			if(o.isTakeable()) {
				iter.remove();
				return o;
			}
			if(o instanceof Chest) {
				TAObject potentialReturn=((Chest)o).removeTakeable();
				if(potentialReturn!=null)
					return potentialReturn;
			}
		}
		return null;
	}

	public List<String> getAdjectives(String name) {
		List<String> adjectives=new LinkedList<String>();
		for(TAObject ta:inventory) {
			if((ta.getName().equals(name)||ta.alsoKnownAs(name)&&ta.isVisible()))
				adjectives.add(ta.getAdjective());
			if(ta instanceof Chest)
				adjectives.addAll(((Chest)ta).getAdjectives(name));
		}
		return adjectives;
	}

	public int getNumTakeables() {
		if(!isOpen())
			return 0;
		int numTakeables=0;
		for(TAObject obj:inventory) {
			if(obj.isTakeable()&&obj.isVisible())
				numTakeables++;
			if(obj instanceof Chest)
				numTakeables+=((Chest)obj).getNumTakeables();
		}
		return numTakeables;
	}

	public String getInventoryText() {
		return getInventoryText(1);
	}

	public String getInventoryText(int numTabs) {
		String text="";
		String tabs="";
		for(int i=0; i<numTabs; i++)
			tabs+="    ";
		for(TAObject obj:inventory) {
			text+="\n"+tabs+Character.toUpperCase(obj.getFullNameWithArticle().charAt(0))+obj.getFullNameWithArticle().substring(1);
			if(obj instanceof Chest&&((Chest)obj).isOpen()) {
				if(!((Chest)obj).hasItems())
					text+="\n"+tabs+"There is nothing in the "+obj.getFullName()+".";
				else {
					text+="\n"+tabs+"The "+obj.getFullName()+" contains:";
					text+=((Chest)obj).getInventoryText(numTabs+1);
				}
			}
		}
		return text;
	}

	public boolean isUnlocked() {
		return state!=0;
	}

	public boolean isOpen() {
		return state==2;
	}

	public boolean hasItems() {
		return inventory.size()>0;
	}

	public Set<TAObject> getContents() {
		Set<TAObject> fullContents=inventory;
		for(TAObject c:inventory)
			if(c instanceof Chest&&((Chest)c).isOpen())
				fullContents.addAll(((Chest)c).getContents());
		return fullContents;
	}

	public String getDescription() {
		if(isOpen()) {
			if(inventory.size()==0)
				return super.getDescription()+"\nThere is nothing in the "+getFullName()+".";
			else
				return super.getDescription()+"\nThe "+getFullName()+" contains: "+getInventoryText();
		}
		return super.getDescription();
	}
}

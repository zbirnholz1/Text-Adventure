package textadventure;
import java.util.HashSet;
import java.util.Set;
import org.json.*;

public abstract class TAObject implements Comparable<TAObject> {
	protected String name, description, adjective;
	protected Set<String> otherNames;
	protected boolean visible, takeable, isPrinted, isPlural;

	public static final String VOWELS="aeiouAEIOU";

	public TAObject() {
		this (null, null, null);
	}

	public TAObject(JSONObject source) {
		initialize(source);
	}

	private void initialize(JSONObject source) {
		try {
			if(source.has("name"))
				name=source.getString("name");
			else
				name=null;
			if(source.has("description"))
				description=source.getString("description");
			else
				description=null;
			if(source.has("adjective"))
				adjective=source.getString("adjective");
			else
				adjective=null;
			if(source.has("otherNames")&&!(this instanceof DynamicObject)) {
				otherNames=new HashSet<String>();
				JSONArray namesArr=source.getJSONArray("otherNames");
				for(int i=0; i<namesArr.length(); i++)
					otherNames.add(namesArr.getString(i));
			}
			else
				otherNames=null;
			if(source.has("visible"))
				visible=source.getBoolean("visible");
			else
				visible=true;
			if(source.has("takeable"))
				takeable=source.getBoolean("takeable");
			else
				takeable=true;
			if(source.has("isPrinted"))
				isPrinted=source.getBoolean("isPrinted");
			else
				isPrinted=true;
			if(source.has("isPlural"))
				isPlural=source.getBoolean("isPlural");
			else
				isPlural=false;
		} catch(JSONException e){Main.game.getView().println("Something went wrong TAO: "+e);}
	}

	public TAObject(String name, String description, String adjective) {
		this.name=name;
		this.description=description;
		this.adjective=adjective;
		otherNames=null;
		visible=true;
		takeable=false;
	}

	public boolean alsoKnownAs(String otherName) {
		if(otherNames==null)
			return false;
		for(String str:otherNames)
			if(str.equalsIgnoreCase(otherName))
				return true;
		return false;
	}

	public String toString() {
		return toJSONObject().toString();
	}

	public JSONObject toJSONObject() {
		JSONObject object= new JSONObject();
		try {
			if(name!=null)
				object.put("name", name);
			if(description!=null)
				object.put("description", description);
			if(adjective!=null)
				object.put("adjective", adjective);
			object.put("visible", visible);
			if(!takeable)
				object.put("takeable", takeable);
			if(!isPrinted)
				object.put("isPrinted", isPrinted);
			if(isPlural)
				object.put("isPlural", isPlural);
			if(otherNames!=null) {
				object.put("otherNames", otherNames);
			}
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return object;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getAdjective() {
		return adjective;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isTakeable() {
		return takeable;
	}

	public boolean isPrinted() {
		return isPrinted;
	}
	
	public boolean isPlural() {
		return isPlural;
	}

	public boolean startsWithVowel() {
		if(getAdjective()==null)
			return VOWELS.indexOf(getName().charAt(0))!=-1;
		return VOWELS.indexOf(getAdjective().charAt(0))!=-1;
	}

	public String getFullName() {
		if(getAdjective()==null)
			return getName();
		else
			return getAdjective()+" "+getName();
	}

	public String getFullNameWithArticle() {
		if(isPlural)
			return "some "+getFullName();
		if(startsWithVowel())
			return "an "+getFullName();
		return "a "+getFullName();
	}
	
	public String getFullNameWithCapitalizedArticle() {
		String str=getFullNameWithArticle();
		return Character.toUpperCase(str.charAt(0))+str.substring(1);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setOtherNames(Set<String> otherNames) {
		this.otherNames=otherNames;
	}

	public Set<String> getOtherNames() {
		return otherNames;
	}

	public void setAdjective(String adjective) {
		this.adjective = adjective;
	}

	public void setIsVisible(boolean visible) {
		this.visible = visible;
	}

	public void setIsTakeable(boolean takeable) {
		this.takeable = takeable;
	}

	public void setIsPrinted(boolean isPrinted) {
		this.isPrinted = isPrinted;
	}

	public void setIsPlural(boolean isPlural) {
		this.isPlural = isPlural;
	}

	public int hashCode() {
		int hash=getName().hashCode();
		if(getAdjective()!=null)
			hash+=getAdjective().hashCode();
		else
			hash+=getDescription().hashCode();
		if(otherNames.size()!=0)
			hash*=otherNames.size();
		return hash;
	}

	public boolean equals(Object other) {
		boolean toReturn=other instanceof TAObject&&((TAObject)other).getName().equals(getName())&&((TAObject)other).getDescription().equals(getDescription());
		if(getAdjective()!=null)
			return toReturn&&getAdjective().equals(((TAObject)other).getAdjective());
		return toReturn&&((TAObject)other).getAdjective()==null;
	}

	public int compareTo(TAObject other) {
		if(getName().equals(other.getName())) {
			if(getAdjective()==null&&other.getAdjective()!=null)
				return -1;
			else if(getAdjective()!=null&&other.getAdjective()==null)
				return 1;
			else if(getAdjective()==null&&other.getAdjective()==null)
				return 0;
			return getAdjective().compareToIgnoreCase(other.getAdjective());
		}
		return getName().compareToIgnoreCase(other.getName());
	}

	public abstract String process(String verb, TAObject otherObject, boolean thisIsDO);
}

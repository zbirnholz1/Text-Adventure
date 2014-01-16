package textadventure;

import org.json.*;

public class Armor extends StaticObject {
	private int weight, rating;
	private Material material;
	private Structure structure;
	
	public Armor(JSONObject source) {
		//TODO
	}

	public String process(String verb, TAObject otherObject, boolean thisIsDO) {
		if(verb.equals("wear")&&thisIsDO)
			Main.game.getPlayer().setArmor(this);
		return "";
	}
	
	public int getWeight() {
		return weight;
	}
	
	public int getRating() {
		return rating;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public Structure getStructure() {
		return structure;
	}

}

package textadventure;

import org.json.*;

public class Armor extends StaticObject {
	private int weight, rating;
	private ArmorType type;
	
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
	
	public ArmorType getType() {
		return type;
	}

}

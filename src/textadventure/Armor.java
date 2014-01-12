package textadventure;

public class Armor extends TAObject {
	private int weight, rating;
	private Material material;
	private Structure structure;

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

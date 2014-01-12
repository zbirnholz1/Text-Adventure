package textadventure;

public enum Structure {
	//weapon structures:
	PIERCE, IMPACT,
	//armor structures:
	CHAIN, PLATE;
	
	public String toString() {
		return super.toString().toLowerCase();
	}
}

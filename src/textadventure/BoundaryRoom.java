package textadventure;

public class BoundaryRoom extends Room {
	private int nextRoomID, previousRoomID, nextBlockNumber;
	
	public BoundaryRoom(int previousID, int nextID, int blockNumber) {
		previousRoomID=previousID;
		nextRoomID=nextID;
		nextBlockNumber=blockNumber;
		ID=nextID;
	}
	
	public int getNextRoomID() {
		return nextRoomID;
	}
	
	public int getPreviousRoomID() {
		return previousRoomID;
	}
	
	public int getNextBlockNumber() {
		return nextBlockNumber;
	}
}

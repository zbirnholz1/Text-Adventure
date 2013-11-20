package textadventure;
import org.json.*;

public class Door extends DynamicObject {
	//state==0 means locked, state==1 means closed, state==2 means open
	private int[] roomIDs, roomDirections;
	private String[] closedMessages;

	public Door(JSONObject source) {
		super(source);
		isPrinted=false;
		try {
			JSONArray JSONRoomIDs=source.getJSONArray("roomIDs");
			roomIDs=new int[JSONRoomIDs.length()];
			for(int i=0; i<JSONRoomIDs.length(); i++)
				roomIDs[i]=JSONRoomIDs.getInt(i);
			JSONArray JSONRoomDirections=source.getJSONArray("roomDirections");
			roomDirections=new int[JSONRoomDirections.length()];
			for(int i=0; i<JSONRoomDirections.length(); i++)
				roomDirections[i]=JSONRoomDirections.getInt(i);
			JSONArray JSONClosedMessages=source.getJSONArray("closedMessages");
			closedMessages=new String[JSONClosedMessages.length()];
			for(int i=0; i<JSONClosedMessages.length(); i++)
				closedMessages[i]=JSONClosedMessages.getString(i);
		} catch(JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
	}

	public JSONObject toJSONObject() {
		JSONObject obj=super.toJSONObject();
		try {
			for(int i=0; i<roomIDs.length; i++)
				obj.accumulate("roomIDs", roomIDs[i]);
			for(int i=0; i<roomDirections.length; i++)
				obj.accumulate("roomDirections", roomDirections[i]);
			for(int i=0; i<closedMessages.length; i++)
				obj.accumulate("closedMessages", closedMessages[i]);
		} catch (JSONException e) {Main.game.getView().println("Something went wrong: "+e);}
		return obj;
	}

	public boolean isUnlocked() {
		return state!=0;
	}

	public boolean isOpen() {
		return state==2;
	}

	public int getID(int number) throws IllegalArgumentException {
		try {
			return roomIDs[number-1];
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("A door only connects 2 rooms!");}
	}

	public int getDirection(int number) throws IllegalArgumentException {
		try {
			return roomDirections[number-1];
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("A door only connects 2 rooms!");}
	}

	public String getClosedMessage(int number) throws IllegalArgumentException {
		try {
			return closedMessages[number];
		} catch(ArrayIndexOutOfBoundsException e){throw new IllegalArgumentException("A door only connects 2 rooms!");}
	}

	public int getNumber(int ID) {
		for(int i=0; i<roomIDs.length; i++)
			if(roomIDs[i]==ID)
				return i;
		return -1;
	}
}

package textadventure;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import mp3.*;

public class SoundPlayer {
	public static final Map<String, Long> OFFSETS=SoundPlayer.initializeOffsets();

	private Decoder decoder;
	private String currentSoundName;
	private boolean soundIsOn;
	private boolean musicIsEverywhere;

	public SoundPlayer() {
		currentSoundName="";
		soundIsOn=true;
		musicIsEverywhere=false;
	}

	public SoundPlayer(JSONObject source) {
		try {
			currentSoundName=source.getString("currentSoundName");
			soundIsOn=source.getBoolean("soundIsOn");
			musicIsEverywhere=source.getBoolean("musicIsEverywhere");
		} catch(JSONException e){
			e.printStackTrace();
			Main.game.getView().println("Something went wrong while loading the sound.");
		}
	}

	public JSONObject toJSONObject() {
		JSONObject obj=new JSONObject();
		try {
			obj.put("currentSoundName", currentSoundName);
			obj.put("soundIsOn", soundIsOn);
			obj.put("musicIsEverywhere", musicIsEverywhere);
		} catch(JSONException e){
			e.printStackTrace();
			Main.game.getView().println("Something went wrong while saving the sound.");
		}
		return obj;
	}

	public void play(final String name) {
		if(musicIsEverywhere)
			return;
		stop();
		currentSoundName=name;
		decoder=new Decoder();
		try {
			InputStream in = getClass().getResourceAsStream("/music/"+name);
			final BufferedInputStream bin=new BufferedInputStream(in, 128*1024);
			final ExecutorService service=Executors.newSingleThreadExecutor();
			service.execute(
					new Runnable() {
						public void run() {
							try {
								service.shutdown();
								decoder.play("/music/"+name, bin);
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
					);
		} catch(Exception e){e.printStackTrace();}
	}

	public void loop(final String name, final int numTimes) {
		if(musicIsEverywhere)
			return;
		stop();
		currentSoundName=name;
		decoder=new Decoder();
		final InputStream in;
		try {
			in = getClass().getResourceAsStream("/music/"+name);
			final ExecutorService service=Executors.newSingleThreadExecutor();
			service.execute(
					new Runnable() {
						public void run() {
							try {
								service.shutdown();
								decoder.loop("/music/"+name, in, numTimes);
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
					);
		} catch(Exception e){e.printStackTrace();}
	}

	public void loop(final String name) {
		if(musicIsEverywhere)
			return;
		stop();
		currentSoundName=name;
		decoder=new Decoder();
		final InputStream in;
		try {
			in = getClass().getResourceAsStream("/music/"+name);
			final ExecutorService service=Executors.newSingleThreadExecutor();
			service.execute(
					new Runnable() {
						public void run() {
							try {
								service.shutdown();
								decoder.loop("/music/"+name, in);
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
					);
		} catch(Exception e){e.printStackTrace();}
	}

	public void loop(final String name, final long offset) {
		if(musicIsEverywhere)
			return;
		if(offset==0) {
			loop(name);
			return;
		}
		stop();
		currentSoundName=name;
		decoder=new Decoder();
		final InputStream in;
		try {
			in = getClass().getResourceAsStream("/music/"+name);
			final ExecutorService service=Executors.newSingleThreadExecutor();
			service.execute(
					new Runnable() {
						public void run() {
							try {
								service.shutdown();
								decoder.loop("/music/"+name, in, offset);
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
					);
		} catch(Exception e){e.printStackTrace();}
	}

	public void loop(final String name, final int numTimes, final long offset) {
		if(musicIsEverywhere)
			return;
		stop();
		currentSoundName=name;
		decoder=new Decoder();
		final InputStream in;
		try {
			in = getClass().getResourceAsStream("/music/"+name);
			final ExecutorService service=Executors.newSingleThreadExecutor();
			service.execute(
					new Runnable() {
						public void run() {
							try {
								service.shutdown();
								decoder.loop("/music/"+name, in, numTimes, offset);
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
					);
		} catch(Exception e){e.printStackTrace();}
	}

	public void stop() {
		musicIsEverywhere=false;
		if(decoder!=null) {
			decoder.stop();
			currentSoundName="";
		}
	}

	public void setSoundIsOn(boolean s) {
		soundIsOn=s;
		if(!soundIsOn) {
			if(!musicIsEverywhere)
				currentSoundName="";
			boolean m=musicIsEverywhere;
			stop();
			musicIsEverywhere=m;
		}
		else if(musicIsEverywhere)
			loop(currentSoundName, OFFSETS.get(currentSoundName));
	}

	public void setMusicEverywhere(boolean m) {
		musicIsEverywhere=m;
	}

	public String getCurrentSoundName() {
		return currentSoundName;
	}

	public boolean soundIsOn() {
		return soundIsOn;
	}

	public boolean musicIsEverywhere() {
		return musicIsEverywhere;
	}
	
	public void saveInfo() {
		try {
			PrintWriter out=new PrintWriter(new BufferedWriter(new FileWriter(Game.supportPath+"saves/temp.taf/soundPlayer.taf")));
			out.println(this.toJSONObject());
			out.close();
		} catch(Exception e){Main.game.getView().println("There was a problem saving your progress: "+e);e.printStackTrace();}
	}

	public static final Map<String, Long> initializeOffsets() {
		@SuppressWarnings("serial")
		Map<String, Long> map=new HashMap<String, Long>() {
			public Long get(Object key) {
				Long toReturn=super.get(key);
				if(toReturn==null)
					return new Long(0);
				return toReturn;
			}
		};
		//read in from offsets.taf
		return map;
	}
}

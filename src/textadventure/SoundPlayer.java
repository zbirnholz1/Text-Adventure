package textadventure;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
		soundIsOn=false;
		musicIsEverywhere=false;
		decoder=new Decoder();
	}

	public SoundPlayer(JSONObject source) {
		try {
			currentSoundName=source.getString("currentSoundName");
			soundIsOn=source.getBoolean("soundIsOn");
			musicIsEverywhere=source.getBoolean("musicIsEverywhere");
			decoder=new Decoder();
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

	public void manageDecoder(final String name, final Method method, final Object[] args) {
		//TODO deal with .wav (intro loop) and .mp3 (main loop) parts of music
		//should change rooms' musicNames accordingly (i.e. remove the file extensions)
		if(musicIsEverywhere)
			return;
		boolean shouldFade=false;
		for(int i=0; i<args.length; i++) {
			if(args[i] instanceof Boolean) {
				shouldFade=(Boolean)args[i];
				break;
			}
		}
		stop(shouldFade);
		currentSoundName=name;
		decoder=new Decoder();
		try {
			InputStream in = getClass().getResourceAsStream("/music/"+name);
			final BufferedInputStream bin=new BufferedInputStream(in, 128*1024);
			args[0]="/music/"+args[0];
			args[1]=bin;
			//final ExecutorService service=Executors.newSingleThreadExecutor();
			//service.execute(
					//new Runnable() {
			new Thread() {
						public void run() {
							try {
								//service.shutdown();
								method.invoke(decoder, args);
								//decoder.play("/music/"+name, bin);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}.start();
					//);
		} catch(Exception e){e.printStackTrace();}
	}

	public void play(final String name, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("play", new Class<?>[]{name.getClass(), Class.forName("java.io.InputStream"), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(final String name, final int numTimes, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{name.getClass(), Class.forName("java.io.InputStream"), new Integer(numTimes).getClass(), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, numTimes, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(final String name, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{name.getClass(), Class.forName("java.io.InputStream"), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(final String name, final long offset, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{name.getClass(), Class.forName("java.io.InputStream"), new Long(offset).getClass(), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, offset, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(final String name, final int numTimes, final long offset, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{name.getClass(), Class.forName("java.io.InputStream"), new Integer(numTimes).getClass(), new Long(offset).getClass(), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, numTimes, offset, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(boolean shouldFade) {
		musicIsEverywhere=false;
		if(decoder!=null) {
			decoder.stop(shouldFade);
			currentSoundName="";
		}
	}

	public void setSoundIsOn(boolean s) {
		soundIsOn=s;
		if(!soundIsOn) {
			if(!musicIsEverywhere)
				currentSoundName="";
			boolean m=musicIsEverywhere;
			stop(true);
			musicIsEverywhere=m;
		}
		else if(musicIsEverywhere)
			loop(currentSoundName, OFFSETS.get(currentSoundName), true);
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

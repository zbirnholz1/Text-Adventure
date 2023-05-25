package textadventure;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import org.json.JSONException;
import org.json.JSONObject;

import mp3.*;

public class SoundPlayer {
	public static final Map<String, Long> OFFSETS=SoundPlayer.initializeOffsets();
	public static final Map<String, String> CREDITS=SoundPlayer.initializeCredits();
	public static final int CREDIT_TIMER_DELAY_MILLISECONDS = 10000;

	private Decoder decoder;
	private String currentSoundName;
	private boolean soundIsOn;
	private boolean musicIsEverywhere;
	private CreditTimer creditTimer;

	public SoundPlayer() {
		currentSoundName="";
		soundIsOn=false;
		musicIsEverywhere=false;
		decoder=new Decoder();
	}

	public SoundPlayer(JSONObject source) {
		try {
			currentSoundName=source.getString("currentSoundName");
			creditTimer=new CreditTimer(currentSoundName);
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

	//shouldFade refers to if it should fade out
	public void manageDecoder(final String name, final Method method, final Object[] args) {
		//TODO deal with .wav (intro loop) and .mp3 (main loop) parts of music
		//should change rooms' musicNames accordingly (i.e. remove the file extensions)
		if(musicIsEverywhere)
			return;
		boolean shouldFadeNotFinal=false;
		for(int i=0; i<args.length; i++) {
			if(args[i] instanceof Boolean) {
				shouldFadeNotFinal=(Boolean)args[i];
				break;
			}
		}
		final boolean shouldFade=shouldFadeNotFinal;
		if(soundIsOn)
			stop(shouldFade);
		creditTimer=new CreditTimer(name);
		currentSoundName=name;
		if(!soundIsOn)
			return;
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
						if(shouldFade) {
							try{sleep(310);} catch(InterruptedException e){e.printStackTrace();}
						}
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
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{String.class, InputStream.class, new Long(offset).getClass(), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, offset, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(final String name, final int numTimes, final long offset, final boolean shouldFade) {
		try {
			Method method=decoder.getClass().getDeclaredMethod("loop", new Class<?>[]{String.class, InputStream.class, new Integer(numTimes).getClass(), new Long(offset).getClass(), new Boolean(shouldFade).getClass()});
			manageDecoder(name, method, new Object[]{name, null, numTimes, offset, shouldFade});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(boolean shouldFade) {
		musicIsEverywhere=false;
		if(decoder!=null) {
			if(creditTimer!=null&&creditTimer.isRunning())
				creditTimer.stop();
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

	public boolean musicIsPlaying() {
		return !(currentSoundName==null&&currentSoundName.equals(""))&&soundIsOn;
	}

	public CreditTimer getCreditTimer() {
		return creditTimer;
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
		//map.put("finalBattle.mp3", new Long(0)); //TODO figure out the offset
		//read in from offsets.taf (maybe not)
		return map;
	}

	public static final Map<String, String> initializeCredits() {
		Map<String, String> map=new HashMap<String, String>();
		BufferedReader reader=new BufferedReader(new InputStreamReader(SoundPlayer.class.getResourceAsStream("/musicCredits.taf")));
		try {
			String line=reader.readLine();
			while(line!=null) {
				map.put(line.substring(0, line.indexOf(" ")), line.substring(line.indexOf(" ")+1));
				line=reader.readLine();
			}
			reader.close();
		} catch (Exception e) {e.printStackTrace();Main.game.getView().println("Something went wrong loading the music credits: "+e.toString());}
		return map;
	}

	@SuppressWarnings("serial")
	class CreditTimer extends Timer {

		public CreditTimer(String name) {
			super(CREDIT_TIMER_DELAY_MILLISECONDS, new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					Main.game.getView().updateStatsText();
				}
			});
			this.setRepeats(false);
			String credit=CREDITS.get(name);
			if(credit!=null)
				Main.game.getView().addMusicCredit("Music:\n"+credit);
			start();
		}

		public void stop() {
			super.stop();
			Main.game.getView().updateStatsText();
		}

	}
}

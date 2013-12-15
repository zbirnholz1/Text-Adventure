package topicsproject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JButton;
import javax.swing.JFrame;

import textadventure.SoundPlayer;

public class LoopTester {
	
	public static final SoundPlayer s=new SoundPlayer();

	public static void main(String[] args) throws Exception {
		File soundFile = new File("knights.wav");
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
		final Clip clip=AudioSystem.getClip();
		LoopLineListener lineListener=new LoopLineListener("knights.mp3");
		clip.addLineListener(lineListener);
		JFrame f=new JFrame("Loop Tester");
		JButton button=new JButton("End Intro");
		button.addActionListener(lineListener);
		f.add(button);
		f.setSize(100, 100);
		f.setVisible(true);
		clip.open(audioIn);
		clip.start();
		//Thread.sleep(Long.MAX_VALUE);
	}

}

class LoopLineListener implements LineListener, ActionListener {
	private String loopName;
	private boolean shouldKeepPlaying;
	
	public LoopLineListener(String l) {
		loopName=l;
		shouldKeepPlaying=true;
	}
	
	public void update(LineEvent e) {
		if(e.getType()==LineEvent.Type.STOP) {
			if(shouldKeepPlaying) {
				((Clip)e.getSource()).setFramePosition(0);
				((Clip)e.getSource()).start();
			}
			else {
				((Clip)e.getSource()).close();
				LoopTester.s.loop(loopName);
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		shouldKeepPlaying=false;
	}
}

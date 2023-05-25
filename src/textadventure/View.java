package textadventure;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;

@SuppressWarnings("serial")
public class View extends JFrame {
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private JPanel leftPanel;
	private JScrollPane leftScrollPane;
	private JTextPane playerStats;
	private JButton mapButton;
	private JTextArea ASCIIFrame, ASCIIExitArea;
	private JPanel tutorialPanel;
	private JEditorPane tutorialTextArea;
	private int promptPosition;
	private WindowListener exitListener;
	private KeyListener keyListener;
	private PressAnyKeyListener pressAnyKeyListener;
	private GameListener gameListener;
	private Queue<TypewriterTimer> timerQueue;
	private Queue<String> printQueue; //used for "press any key to continue"
	private boolean isWaitingForKeyPress;
	private Queue<String> pauseQueue; //used for {###} pauses
	private String musicCredit;
	private ArrowTimer arrowTimer;
	private Color lastBackground;
	private Color lastTextColor;

	public static final int COLOR_FADE_DELAY = 45;

	public View() {
		super("Text Adventure");
		setLayout(new BorderLayout());
		exitListener = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(JOptionPane.showConfirmDialog(View.this, "Your game is still active.\nAre you sure you want to quit?\nAny unsaved progress will be lost.", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE)==JOptionPane.YES_OPTION)
					Main.game.quit(false);
			}
		};
		addWindowListener(exitListener);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		promptPosition=0;
		keyListener=new KeyListener() {
			public void keyPressed(KeyEvent event) {
				if(isWaitingForKeyPress)
					return;
				if(event.getKeyCode()==KeyEvent.VK_ENTER) {
					try {
						int inputLength=textArea.getText().length()-promptPosition;
						String toParse=textArea.getText(promptPosition, inputLength);
						promptPosition+=inputLength;
						if(gameListener!=null) {
							gameListener.textTyped(textArea.getText().substring(promptPosition-inputLength));
							if(gameListener==null)
								printNPC(">");
							return;
						}
						println();
						println();
						Main.game.getCommandParser().parse(toParse, true);
						if(gameListener!=null)
							return;
						if(!Main.game.getPlayer().isDead())
							printNPC("\n>");
					} catch (BadLocationException e) {e.printStackTrace();}
				}
			}

			public void keyReleased(KeyEvent event) {

			}
			public void keyTyped(KeyEvent event) {
				if(isWaitingForKeyPress)
					return;
				if(textArea.getCaretPosition()<promptPosition)
					textArea.setCaretPosition(textArea.getText().length());
			}
		};
		pressAnyKeyListener=new PressAnyKeyListener();

		playerStats=new JTextPane();
		StyledDocument doc = playerStats.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		doc.setParagraphAttributes(0, doc.getLength(), center, false);
		resetStatsText();
		playerStats.setEditable(false);
		playerStats.setBackground(getBackground());
		playerStats.setMargin(new Insets(0, 10, 0, 10));
		playerStats.setFocusable(false);
		leftPanel=new JPanel(new BorderLayout());
		leftScrollPane=new JScrollPane(playerStats);
		leftPanel.add(leftScrollPane, BorderLayout.CENTER);
		leftPanel.validate();
		leftPanel.setFocusable(false);

		//statsPanel.add(mapButton, BorderLayout.SOUTH);
		//statsPanel.setBackground(textArea.getBackground().darker());
		add(leftPanel, BorderLayout.WEST);

		textArea=new JTextArea() {
			private boolean waiting=false;

			public void append(String text) {
				synchronized(pauseQueue) {
					if(waiting) {
						pauseQueue.add(text);
						return;
					}
				}
				if(!text.equals("")&&text.charAt(0)=='+') {
					waiting=true;
					Timer timer=new Timer(Integer.parseInt(text.substring(1)), new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							waiting=false;
							synchronized(pauseQueue) {
								if(!pauseQueue.isEmpty())
									append(pauseQueue.poll());
							}
						}

					});
					timer.setRepeats(false);
					timer.start();
					//try{Thread.sleep(Integer.parseInt(text.substring(1)));}catch(InterruptedException e){e.printStackTrace();}

				}
				else if(text.length()>4&&text.substring(0, 5).equals("color")) {
					String[] RGB = text.substring(text.indexOf(" ")+1).split(",");
					setBackgroundColor(new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2])));
					if(!pauseQueue.isEmpty())
						append(pauseQueue.poll());
				}
				else if(text.length()>8&&text.substring(0, 9).equals("textcolor")) {
					String[] RGB = text.substring(text.indexOf(" ")+1).split(",");
					setTextColor(new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2])));
					if(!pauseQueue.isEmpty())
						append(pauseQueue.poll());
				}
				else if(text.length()>8&&text.substring(0, 9).contains("Caret")) {
					if(text.startsWith("show"))
						getCaret().setVisible(true);
					else
						getCaret().setVisible(false);
					if(!pauseQueue.isEmpty())
						append(pauseQueue.poll());
				}
				else {
					super.append(text);
					promptPosition+=text.length();
					textArea.setCaretPosition(promptPosition);
					if(!pauseQueue.isEmpty())
						append(pauseQueue.poll());					
				}
			}
		};
		textArea.setMargin(new Insets(10, 10, 10, 10));
		//textArea.setFont(new Font("Gill Sans", Font.PLAIN, 14));
		textArea.setBackground(new Color(238, 203, 173));
		textArea.setForeground(Color.BLACK);
		textArea.setCaretColor(Color.BLACK);
		textArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"doNothing");
		textArea.addKeyListener(keyListener);
		textArea.addKeyListener(pressAnyKeyListener);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		((AbstractDocument) textArea.getDocument()).setDocumentFilter(new Filter());
		scrollPane=new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
		setSize(625, 425);
		setMinimumSize(getSize());
		setLocation(30, 30);
		setVisible(true);
		leftPanel.setPreferredSize(leftPanel.getSize());
		textArea.requestFocusInWindow();
		isWaitingForKeyPress=false;
		timerQueue=new LinkedList<TypewriterTimer>();
		printQueue=new LinkedList<String>();
		pauseQueue=new LinkedList<String>();
		lastBackground=textArea.getBackground();
		lastTextColor=textArea.getForeground();
		
		mapButton=new JButton("Display map");
		mapButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(gameListener!=null)
					return;
				int oldPromptPosition=promptPosition;
				promptPosition=0;
				textArea.setText(textArea.getText().substring(0, oldPromptPosition)+"map");
				promptPosition=oldPromptPosition;
				keyListener.keyPressed(new KeyEvent(textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
				textArea.requestFocusInWindow();
			}
		});
	}

	private class PressAnyKeyListener implements KeyListener {

		private int numEnters=0;
		private boolean shouldPressEnter=false;

		public void keyPressed(KeyEvent event) {
			if(!isWaitingForKeyPress||event.getKeyCode()!=KeyEvent.VK_ENTER)
				return;
			if(numEnters==0) { //The first one doesn't count because it's the enter
				numEnters++;   //after their input. The next enter is the real one.
				return;
			}
			numEnters=0;
			textArea.getCaret().setVisible(true);
			stopWaitingForKeyPress(shouldPressEnter);
		}

		public void keyReleased(KeyEvent event) {

		}

		public void keyTyped(KeyEvent event) {
			if(!isWaitingForKeyPress)
				return;
			event.consume();
		}

		public void setNumEnters(int num) {
			numEnters=num;
		}

		public int getNumEnters() {
			return numEnters;
		}

		public void setShouldPressEnter(boolean b) {
			shouldPressEnter=b;
		}
	}

	private class ArrowTimer extends Timer {
		final String[] ARROWS={'\u25B6'+"  ", " "+'\u25B6'+" ", "  "+'\u25B6'+"", " "+'\u25B6'+" "};
		private int index;
		private double width;
		private String spacesBeforeArrow;
		private static final double PIXELS_PER_SPACE = 4.5;

		public ArrowTimer(int arg0, ActionListener arg1) {
			super(arg0, arg1);
			String toPrint="\n";
			width = textArea.getWidth();
			for(int i = 0; i < width/PIXELS_PER_SPACE; i++)
				toPrint+=" ";
			print(toPrint);
			print(ARROWS[0]);
			spacesBeforeArrow = toPrint.substring(1);
			pressAnyKeyListener.setNumEnters(1); //because print(toPrint) reset it to 0.
			textArea.getCaret().setVisible(false);
			setRepeats(true);
			index=1;
			fireActionPerformed(null);
			start();
		}

		protected void fireActionPerformed(ActionEvent e) {
			if(!pauseQueue.isEmpty())
				return;
			promptPosition=0;
			int numCharactersInArrowSequence = spacesBeforeArrow.length() + 3;
			if(width != textArea.getWidth()) {
				width = textArea.getWidth();
				spacesBeforeArrow = "";
				for(int i = 0; i < width/PIXELS_PER_SPACE; i++)
					spacesBeforeArrow+=" ";
			}
			textArea.setText(textArea.getText().substring(0, textArea.getText().length() - numCharactersInArrowSequence) + spacesBeforeArrow + ARROWS[index]);
			promptPosition=textArea.getText().length();
			print("");
			textArea.getCaret().setVisible(false);
			index++;
			index=index%ARROWS.length;
		}

	}

	private class TypewriterTimer extends Timer {		
		private int charNum;
		private String str;
		private boolean shouldPause;

		public TypewriterTimer(String s, boolean p) {
			super(25, null);
			str=s;
			shouldPause=p;
			if(!shouldPause) {
				//this.setDelay(0);
				printBasic(str, shouldPause);
				return;
			}
			charNum=0;
			timerQueue.add(this);
			if(timerQueue.size()==1)
				start();
		}

		public void stop() {
			super.stop();
			timerQueue.remove();
			if(timerQueue.peek()!=null)
				timerQueue.poll().start();
		}

		protected void fireActionPerformed(ActionEvent e) {
			if(!shouldPause) {
				print(str);
				stop();
				return;
			}
			if(charNum>str.length()-1) {
				stop();
				return;
			}
			print(""+str.charAt(charNum));
			charNum++;
		}
	}

	public void printASCII(String str) {
		textArea.append(str);
		promptPosition+=str.length();
		textArea.setCaretPosition(promptPosition);
	}

	public void printlnASCII(String str) {
		printASCII(str+"\n");
	}

	public void printBasic(String str, boolean shouldPause) {
		if(!textArea.isEditable())
			return;
		if(str.equals(""))
			return;
		/*final StringTokenizer tokenizer=new StringTokenizer(str, "}{", true);
		char firstCharOfLastToken=' ';
		while(tokenizer.hasMoreTokens()) {
			final String token=tokenizer.nextToken();
			if(firstCharOfLastToken=='{') {
				if(token.equals("playerName"))
					new TypewriterTimer(Main.game.getPlayer().getFullName(), shouldPause);
				else if(token.contains(".mp3")) {
					Main.game.getSoundPlayer().stop(true);
					Main.game.getSoundPlayer().loop(token, SoundPlayer.OFFSETS.get(token), false);
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else if(token.equals("stopSound")) {
					Main.game.getSoundPlayer().stop(true);
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else //then it must be a number
					printOld("+"+token, shouldPause);
			}
			firstCharOfLastToken=token.charAt(0);
		}*/
		if(shouldPause) {
			new TypewriterTimer(str, true);
		}
		if(!isWaitingForKeyPress) {
			/*if(str.charAt(0)=='#') {
				clearTextArea();
				str=str.substring(1);
			}
			int clearIndex=str.indexOf("#");
			if(clearIndex!=-1) {
				printOld(str.substring(0, clearIndex), shouldPause);
				printOld(str.substring(clearIndex), shouldPause);
				return;
			}*/
			if(str.contains("#")) //textArea will be cleared anyways in print()
				return;
			else
				textArea.append(str);

		}
		else
			printQueue.add(str);
	}

	public void print(String str, boolean shouldPause) {
		if(!textArea.isEditable())
			return;
		if(str.equals(""))
			return;
		if(str.charAt(0)=='`') {
			pressAnyKeyListener.setNumEnters(1);
			str=str.substring(1);
		}
		else
			pressAnyKeyListener.setNumEnters(0);
		int waitIndex=str.indexOf("^");
		boolean shouldPressEnter=false;
		int pressEnterIndex=str.indexOf("$");
		if(pressEnterIndex!=-1&&(waitIndex==-1||pressEnterIndex<waitIndex)) {
			waitIndex=pressEnterIndex;
			shouldPressEnter=true;
		}
		if(isWaitingForKeyPress) {
			if(waitIndex!=-1)
				str="`"+str;
			printQueue.add(str);
			return;
		}
		if(waitIndex!=-1) {
			((LinkedList<String>)printQueue).addFirst(str.substring(waitIndex+1));
			str=str.substring(0, waitIndex);
		}
		if(str.contains(": \"(")) { //then it should be printed in parentheses as an aside
			str="("+str.replace(": \"(", ": ").replace(")\"", ")");
		}
		else if(str.contains(": \"")) { //if it's a part of a conversation
			str=/*"    "+*/str.replace("^", "^    "+str.substring(0, str.indexOf(":")+1)+" \""); //indent and resupply the speaker's name for each part of a multi-part statement
			str=/*"    "+*/str.replace("$", "$    "+str.substring(0, str.indexOf(":")+1)+" \"");
		}
		final StringTokenizer tokenizer=new StringTokenizer(str, "}{", true);
		char firstCharOfLastToken=' ';
		while(tokenizer.hasMoreTokens()) {
			String token=tokenizer.nextToken();
			if(firstCharOfLastToken=='{') {
				//System.out.println(token);
				if(token.equals("playerName"))
					new TypewriterTimer(Main.game.getPlayer().getFullName(), shouldPause);
				else if(token.contains(".mp3")) {
					Main.game.getSoundPlayer().stop(true);
					boolean shouldFadeIn=token.charAt(0)!='!';
					if(!shouldFadeIn) //if there is a ! at the start of the sound name, remove it
						token=token.substring(1);
					Main.game.getSoundPlayer().loop(token, SoundPlayer.OFFSETS.get(token), shouldFadeIn); //! at the start of the sound name means suddenly play the music
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else if(token.equals("stopSound")) {
					Main.game.getSoundPlayer().stop(true);
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else if(token.charAt(0)=='_') {
					String command = token;
					token = tokenizer.nextToken();
					while(!token.equals("}")) {
						command += " "+token;
						token = tokenizer.nextToken();
					}
					firstCharOfLastToken = '}';
					Main.game.getCommandParser().parse(command);
				}
				else if(token.contains("color")) {
					//format is {color R,G,B waitTime}
					String[] info = token.split(" ");
					String[] RGB = info[1].split(",");
					Color color = new Color(Integer.parseInt(RGB[0]), Integer.parseInt(RGB[1]), Integer.parseInt(RGB[2]));
					boolean differentColorAlreadyQueuedUp = false;
					for(String potentialColorChangeCommand : pauseQueue) { //iterating through a queue, oh well
						if(potentialColorChangeCommand.startsWith("color")) {
							differentColorAlreadyQueuedUp = true;
							break;
						}
					}
					if(!color.equals(textArea.getBackground()) || differentColorAlreadyQueuedUp) {
						if(info.length==3) {
							if(info[2].contains(",")) {
								String[] textRGB = info[2].split(",");
								Color textColor = new Color(Integer.parseInt(textRGB[0]), Integer.parseInt(textRGB[1]), Integer.parseInt(textRGB[2]));
								fadeToColor(color, textColor);
							}
							else
								fadeToColor(color, Integer.parseInt(info[2]));
						}
						else if(info.length==4) {
							String[] textRGB = info[2].split(",");
							Color textColor = new Color(Integer.parseInt(textRGB[0]), Integer.parseInt(textRGB[1]), Integer.parseInt(textRGB[2]));
							fadeToColor(color, textColor, Integer.parseInt(info[3]));
						}
						else
							fadeToColor(color);
						//firstCharOfLastToken = '}';
					}
				}
				else { //then it must be a number
					print("+"+token);
				}
			}
			else if(!token.equals("{")&&!token.equals("}")){
				//print(token, shouldPause);
				new TypewriterTimer(token, shouldPause);
				if(token.equals(str))
					break;
			}
			firstCharOfLastToken=token.charAt(0);
		}
		if(shouldPause) {
			new TypewriterTimer(str, true);
		}
		if(str.equals("")) {
			if(waitIndex!=-1)
				waitForKeyPress(shouldPressEnter);
			return;
		}
		if(!isWaitingForKeyPress) {
			boolean shouldAppend=false;
			if(str.charAt(0)=='#') {
				clearTextArea();
				str=str.substring(1);
				shouldAppend=true;
			}
			int clearIndex=str.indexOf("#");
			if(clearIndex!=-1) {
				print(str.substring(0, clearIndex));
				print(str.substring(clearIndex));
				return;
			}
			if(shouldAppend)
				textArea.append(str);

		}
		else
			printQueue.add(str);
		if(waitIndex!=-1) {
			waitForKeyPress(shouldPressEnter);
		}
	}

	public void print(Object toPrint) {
		print(toPrint.toString());
	}

	public void print(String str) {
		print(str, false);
	}

	public void println(Object toPrint) {
		println(toPrint.toString());
	}

	public void println(String str) {
		print(str+"\n");
	}

	public void println() {
		print("\n");
	}

	public void printNPC(String str) {
		print(str, /*true*/false);
	}

	public void printlnNPC(String str) {
		print(str+"\n", /*true*/false);
	}

	public void printNPC(String str, final boolean shouldPause) {
		print(str, shouldPause);
		/*if(str.contains(": \"(")) { //then it should be printed in parentheses as an aside
			str="("+str.replace(": \"(", ": ").replace(")\"", ")");
		}
		if(str.contains(":")) {
			str=str.replace("^", "^    "+str.substring(0, str.indexOf(":")+1)+" \""); //indent the statements
		}
		final StringTokenizer tokenizer=new StringTokenizer(str, "}{", true);
		char firstCharOfLastToken=' ';
		while(tokenizer.hasMoreTokens()) {
			final String token=tokenizer.nextToken();
			if(firstCharOfLastToken=='{') {
				if(token.equals("playerName"))
					new TypewriterTimer(Main.game.getPlayer().getFullName(), shouldPause);
				else if(token.contains(".mp3")) {
					Main.game.getSoundPlayer().stop(true);
					Main.game.getSoundPlayer().loop(token, SoundPlayer.OFFSETS.get(token), token.charAt(0)!='!'); //! at the start of the sound name means suddenly play the music
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else if(token.equals("stopSound")) {
					Main.game.getSoundPlayer().stop(true);
					str=str.substring(0, str.indexOf("{"))+str.substring(str.indexOf("}")+1);
				}
				else { //then it must be a number
					print("+"+token);
				}
			}
			else if(!token.equals("{")&&!token.equals("}")){
				//print(token, shouldPause);
				new TypewriterTimer(token, shouldPause);
			}
			firstCharOfLastToken=token.charAt(0);
		}*/
	}

	public void printlnNPC() {
		print("\n");
	}

	public void printlnNPC(String str, boolean shouldPause) {
		print(str+"\n", shouldPause);
	}

	public void fadeToColor(Color color) {
		fadeToColor(color, COLOR_FADE_DELAY);
	}

	public void fadeToColor(Color color, int waitTime) {
		int numSteps = 20;
		Color original = lastBackground;
		lastBackground = color;
		textArea.append("hideCaret");
		for(int i = 0; i < numSteps; i++) {
			int newRed = original.getRed() + i*(color.getRed() - original.getRed())/numSteps;
			int newGreen = original.getGreen() + i*(color.getGreen() - original.getGreen())/numSteps;
			int newBlue = original.getBlue() + i*(color.getBlue() - original.getBlue())/numSteps;
			textArea.append("color "+newRed+","+newGreen+","+newBlue);
			textArea.append("+"+waitTime);
		}
		textArea.append("color "+color.getRed()+","+color.getGreen()+","+color.getBlue());
		textArea.append("showCaret");
	}

	public void fadeToColor(Color color, Color textColor) {
		fadeToColor(color, textColor, COLOR_FADE_DELAY);
	}

	public void fadeToColor(Color color, Color textColor, int waitTime) {
		int numSteps = 20;
		Color original = lastBackground;
		Color originalText = lastTextColor;
		lastBackground = color;
		lastTextColor = textColor;
		textArea.append("hideCaret");
		for(int i = 0; i < numSteps; i++) {
			int newRed = original.getRed() + i*(color.getRed() - original.getRed())/numSteps;
			int newGreen = original.getGreen() + i*(color.getGreen() - original.getGreen())/numSteps;
			int newBlue = original.getBlue() + i*(color.getBlue() - original.getBlue())/numSteps;
			int newTextRed = originalText.getRed() + i*(textColor.getRed() - originalText.getRed())/numSteps;
			int newTextGreen = originalText.getGreen() + i*(textColor.getGreen() - originalText.getGreen())/numSteps;
			int newTextBlue = originalText.getBlue() + i*(textColor.getBlue() - originalText.getBlue())/numSteps;
			textArea.append("color "+newRed+","+newGreen+","+newBlue);
			textArea.append("textcolor "+newTextRed+","+newTextGreen+","+newTextBlue);
			textArea.append("+"+waitTime);
		}
		textArea.append("color "+color.getRed()+","+color.getGreen()+","+color.getBlue());
		textArea.append("textcolor "+textColor.getRed()+","+textColor.getGreen()+","+textColor.getBlue());
		textArea.append("showCaret");
	}

	public void setBackgroundColor(Color color) {
		textArea.setBackground(color);
		//textArea.repaint();
	}

	public void setTextColor(Color color) {
		textArea.setForeground(color);
		textArea.setCaretColor(color);
	}

	public void addMusicCredit(String credit) {
		musicCredit=credit;
		playerStats.setText(credit+"\n\n"+getStatsText());
	}

	private String getStatsText() {
		if(Main.game.getPlayer().getRoom() == null || (Main.game.getPlayer().getName().equals("me") && Main.game.getPlayer().getRoom().getID() == 0))
			return getResetStatsText();
		String stats=Main.game.getPlayer().getFullName()+":\n\n"
				+"HP: "+Main.game.getPlayer().getHP()+"/"+Main.game.getPlayer().getMaxHP()+"\n"
				+"Strength: "+Main.game.getPlayer().getStrength()+"\n"
				+"Intelligence: "+Main.game.getPlayer().getIntelligence()+"\n"
				+"Speed: "+Main.game.getPlayer().getSpeed()+"\n\n"
				+"Armor:\n";
		if(Main.game.getPlayer().getArmor()==null)
			stats+="None equipped";
		else {
			stats+=Main.game.getPlayer().getArmor().getFullName()+"\n"
					+"("+Main.game.getPlayer().getArmor().getType().toString()+")\n"
					+"Rating: ";
			if(Main.game.getPlayer().getArmor().getRating()>=0)
				stats+="+";
			stats+=Main.game.getPlayer().getArmor().getRating();
		}
		if(!Main.game.getSoundPlayer().soundIsOn())
			stats="(Sound is off)\n\n"+stats;
		return stats;
	}

	public void updateStatsText() {
		try {
			if(Main.game.getSoundPlayer().getCreditTimer().isRunning())
				addMusicCredit(musicCredit);
			else if(Main.game.getPlayer().getRoom() == null || (Main.game.getPlayer().getName().equals("me") && Main.game.getPlayer().getRoom().getID() == 0))
				resetStatsText();
			else
				playerStats.setText(getStatsText());
		} catch(NullPointerException e){playerStats.setText(getStatsText());}
	}

	/*public void updateLeftPanel() {
		boolean timerIsRunning=Main.game.getSoundPlayer().getCreditTimer().isRunning();
		playerStats.setText(getStatsText());
		//leftPanel.revalidate();
		if(timerIsRunning) {
			addMusicCredit(musicCredit);
		}
	}*/
	
	public String getResetStatsText() {
		return "About you:\n\nYou don't have\nany stats yet!\n\nPlease load a\nsaved game or\nstart a new one.";
	}

	public void resetStatsText() {
		playerStats.setText(getResetStatsText());
	}

	public void addMapButton() {
		leftPanel.add(mapButton, BorderLayout.SOUTH);
		leftPanel.validate();
		//updateLeftPanel();
	}

	public void removeMapButton() {
		leftPanel.remove(mapButton);
		leftPanel.validate();
		updateStatsText();
	}

	public void clearTextArea() {
		promptPosition=0;
		textArea.setText("");
	}

	public void stopResponding() {
		textArea.setEditable(false);
		//textArea.setEnabled(false);
		textArea.setForeground(new Color(80, 80, 80));
		textArea.getCaret().setVisible(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		removeWindowListener(exitListener);
	}

	public void waitForKeyPress(boolean shouldPressEnter) {
		//if(isWaitingForKeyPress)
		//return;
		int numEnters=pressAnyKeyListener.getNumEnters();
		if(shouldPressEnter)
			println("\n\n[Press enter to continue.]\n");
		else
			arrowTimer=new ArrowTimer(100, null);
		pressAnyKeyListener.setNumEnters(numEnters);
		textArea.getCaret().setVisible(false);
		isWaitingForKeyPress=true;
		pressAnyKeyListener.setShouldPressEnter(shouldPressEnter);
	}

	public void stopWaitingForKeyPress(boolean shouldPressEnter) {
		//if(!isWaitingForKeyPress)
		//return;
		if(arrowTimer!=null)
			arrowTimer.stop();
		if(shouldPressEnter) {
			promptPosition=0;
			textArea.replaceRange("", textArea.getText().length()-28, textArea.getText().length());
			promptPosition=textArea.getText().length();
		}
		else {
			promptPosition=0;
			textArea.replaceRange("\n", textArea.getText().length()-3, textArea.getText().length());
			promptPosition=textArea.getText().length();
		}
		textArea.setCaretPosition(textArea.getText().length());
		isWaitingForKeyPress=false;
		pressAnyKeyListener.setShouldPressEnter(false);
		if(!printQueue.isEmpty())
			print("`"+printQueue.remove());
		while(!isWaitingForKeyPress&&!printQueue.isEmpty())
			print(printQueue.remove());
	}

	public void showASCIIFrame(String ASCII, boolean enterToExit) {
		remove(scrollPane);
		remove(leftPanel);
		ASCIIFrame=new JTextArea();
		ASCIIFrame.setFont(new Font("Courier", Font.PLAIN, 12));
		ASCIIFrame.setLineWrap(true);
		ASCIIFrame.setWrapStyleWord(true);
		ASCIIFrame.getCaret().setVisible(false);
		ASCIIFrame.setForeground(Color.BLACK);
		ASCIIFrame.setEditable(false);
		ASCIIFrame.setText(ASCII);
		add(ASCIIFrame, BorderLayout.CENTER);
		validate();
		if(enterToExit) {
			ASCIIFrame.setText(ASCIIFrame.getText()+"\n\n  Press enter to continue.");
			ASCIIExitArea=new JTextArea();
			ASCIIExitArea.setBackground(ASCIIFrame.getBackground());
			ASCIIExitArea.setCaretColor(ASCIIExitArea.getBackground());
			ASCIIExitArea.setForeground(ASCIIExitArea.getBackground());
			ASCIIExitArea.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode()==KeyEvent.VK_ENTER)
						hideASCIIFrame();
				}

				public void keyReleased(KeyEvent e){}
				public void keyTyped(KeyEvent e){}
			});
			ASCIIFrame.setFocusable(false);
			add(ASCIIExitArea, BorderLayout.SOUTH);
			ASCIIExitArea.requestFocusInWindow();
			validate();
		}
		validate();
	}

	public void hideASCIIFrame() {
		remove(ASCIIFrame);
		remove(ASCIIExitArea);
		add(scrollPane, BorderLayout.CENTER);
		validate();
		Main.game.getSoundPlayer().getCreditTimer().stop();
		add(leftPanel, BorderLayout.WEST);
		leftPanel.revalidate();
		validate();
		textArea.setCaretColor(Color.BLACK);
		textArea.setCaretPosition(textArea.getDocument().getLength()-1);
		textArea.setCaretPosition(textArea.getDocument().getLength());
		textArea.requestFocusInWindow();
		updateStatsText();
	}

	public void setGameListener(GameListener g) {
		gameListener=g;
	}

	public GameListener getGameListener() {
		return gameListener;
	}

	public JTextArea getTextArea() {
		return textArea;
	}

	public Queue<String> getPauseQueue() {
		return pauseQueue;
	}

	private class Filter extends DocumentFilter {
		public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr) throws BadLocationException {
			if (offset>=promptPosition) {
				super.insertString(fb, offset, string, attr);
			}
		}

		public void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException {
			if (offset>=promptPosition) {
				super.remove(fb, offset, length);
			}
		}

		public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs) throws BadLocationException {
			if (offset>=promptPosition) {
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}
}

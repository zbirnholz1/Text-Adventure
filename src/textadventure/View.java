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
	private JTextPane playerStats;
	private JButton mapButton;
	private JTextArea ASCIIFrame, ASCIIExitArea;
	private int promptPosition;
	private WindowListener exitListener;
	private KeyListener keyListener;
	private PressAnyKeyListener pressAnyKeyListener;
	private GameListener gameListener;
	private Queue<TypewriterTimer> timerQueue;
	private Queue<String> printQueue; //used for "press any key to continue"
	private boolean isWaitingForKeyPress;

	public View() {
		super("Text Adventure");
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
						print("\n>");
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
		leftPanel.add(playerStats, BorderLayout.CENTER);
		leftPanel.validate();
		leftPanel.setFocusable(false);

		//statsPanel.add(mapButton, BorderLayout.SOUTH);
		//statsPanel.setBackground(textArea.getBackground().darker());
		add(leftPanel, BorderLayout.WEST);

		textArea=new JTextArea();
		textArea.setMargin(new Insets(10, 10, 10, 10));
		//textArea.setFont(new Font("GillSans", Font.PLAIN, 14));
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
		textArea.requestFocusInWindow();
		isWaitingForKeyPress=false;
		timerQueue=new LinkedList<TypewriterTimer>();
		printQueue=new LinkedList<String>();
	}

	private class PressAnyKeyListener implements KeyListener {
		private int numEnters=0;
		public void keyPressed(KeyEvent event) {
			if(!isWaitingForKeyPress||event.getKeyCode()!=KeyEvent.VK_ENTER)
				return;
			if(numEnters==0) { //The first one doesn't count because it's the enter
				numEnters++;   //after their input. The next enter is the real one.
				return;
			}
			/*if(event.isActionKey()||event.isShiftDown()||event.isAltDown()||event.isControlDown()||event.isMetaDown()||event.isAltGraphDown())
					isWaitingForKeyPress=false;
				String str=textArea.getText().substring(textArea.getText().length()-"[Press enter to continue.]".length()-2);
				if(!str.equals("[Press enter to continue.]\n\n"))
					return;*/
			numEnters=0;
			textArea.getCaret().setVisible(true);
			stopWaitingForKeyPress();
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

	}

	private class TypewriterTimer extends Timer {		
		private int charNum;
		private String str;
		private boolean shouldPause;

		public TypewriterTimer(String s, boolean p) {
			super(25, null);
			str=s;
			shouldPause=p;
			if(!shouldPause)
				this.setDelay(0);
			charNum=0;
			timerQueue.add(this);
			if(timerQueue.size()==1)
				start();
		}

		public void stop() {
			super.stop();
			timerQueue.remove();
			if(timerQueue.peek()!=null)
				timerQueue.peek().start();
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

	public void print(String str, boolean shouldPause) {
		if(!textArea.isEditable())
			return;
		if(str.equals(""))
			return;
		if(str.charAt(0)=='`') {
			pressAnyKeyListener.setNumEnters(1);
			str=str.substring(1);
		}
		int waitIndex=str.indexOf("^");
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
		if(shouldPause) {
			new TypewriterTimer(str, true);
		}
		if(!isWaitingForKeyPress) {
			if(str.charAt(0)=='#') {
				clearTextArea();
				str=str.substring(1);
			}
			int clearIndex=str.indexOf("#");
			if(clearIndex!=-1) {
				print(str.substring(0, clearIndex));
				print(str.substring(clearIndex));
				return;
			}
			textArea.append(str);
			promptPosition+=str.length();
			textArea.setCaretPosition(promptPosition);
		}
		else
			printQueue.add(str);
		if(waitIndex!=-1)
			waitForKeyPress();
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
		printNPC(str, /*true*/false);
	}

	public void printlnNPC(String str) {
		printlnNPC(str, /*true*/false);
	}

	public void printNPC(String str, boolean shouldPause) {
		if(str.contains(": \"(")) { //then it should be printed in parentheses as an aside
			str="("+str.replace(": \"(", ": ").replace(")\"", ")");
		}
		if(str.contains(":")) {
			str="    "+str.replace("^", "^    "+str.substring(0, str.indexOf(":")+1)+" \""); //indent the statements
		}
		StringTokenizer tokenizer=new StringTokenizer(str, "}{", true);
		char firstCharOfLastToken=' ';
		while(tokenizer.hasMoreTokens()) {
			String token=tokenizer.nextToken();
			if(firstCharOfLastToken=='{') {
				if(token.equals("playerName"))
					new TypewriterTimer(Main.game.getPlayer().getFullName(), shouldPause);
				else if(token.contains(".mp3"))
					Main.game.getSoundPlayer().loop(token, SoundPlayer.OFFSETS.get(token), false);
				else if(token.equals("stopSound"))
					Main.game.getSoundPlayer().stop(true);
				else { //then it must be a number
					try {
						Thread.sleep(Integer.parseInt(token));
					} catch(InterruptedException e){firstCharOfLastToken=token.charAt(0);}
				}
			}
			else if(!token.equals("{")&&!token.equals("}")){
				//print(token, shouldPause);
				new TypewriterTimer(token, shouldPause);
			}
			firstCharOfLastToken=token.charAt(0);
		}
	}

	public void printlnNPC() {
		println();
	}

	public void printlnNPC(String str, boolean shouldPause) {
		printNPC(str+"\n", shouldPause);
	}

	public void updateStatsText() {
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
		playerStats.setText(stats);
	}
	
	public void resetStatsText() {
		playerStats.setText("About you:\n\nYou don't have\nany stats yet!\n\nPlease load a\nsaved game or\nstart a new one.");
	}

	public void addMapButton() {
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
		leftPanel.add(mapButton, BorderLayout.SOUTH);
		updateStatsText();
	}
	
	public void removeMapButton() {
		leftPanel.remove(mapButton);
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

	public void waitForKeyPress() {
		//if(isWaitingForKeyPress)
		//return;
		println("\n\n[Press enter to continue.]\n");
		textArea.getCaret().setVisible(false);
		isWaitingForKeyPress=true;
	}

	public void stopWaitingForKeyPress() {
		//if(!isWaitingForKeyPress)
		//return;
		isWaitingForKeyPress=false;
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
		add(leftPanel, BorderLayout.WEST);
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

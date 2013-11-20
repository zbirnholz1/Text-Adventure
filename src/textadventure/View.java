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
				if(JOptionPane.showConfirmDialog(View.this, "Your game is still active.\nAre you sure you want to quit?\nYour progress will be lost.", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE)==JOptionPane.YES_OPTION)
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
		JScrollPane scrollPane=new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		add(scrollPane);
		validate();
		setSize(575, 375);
		setMinimumSize(new Dimension(575, 375));
		setLocation(30, 30);
		setVisible(true);
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

	public void print(String str, boolean shouldPause) {
		if(!textArea.isEditable())
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
					print(Main.game.getPlayer().getFullName(), true);
				else if(token.contains(".mp3"))
					Main.game.getSoundPlayer().loop(token, SoundPlayer.OFFSETS.get(token));
				else if(token.equals("stopSound"))
					Main.game.getSoundPlayer().stop();
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

package topicsproject;

import javax.swing.*;

@SuppressWarnings("serial")
public class TAObjectMakerWindow extends JFrame {
	private JPanel panel;
	
	public TAObjectMakerWindow(String className) {
		super(className+" Maker"); //need to make more classes
		panel=new ObjectPanel(className);
		JScrollPane scrollPane=new JScrollPane(panel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		add(scrollPane);
		setSize(400, 400);
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new TAObjectMakerWindow("Room");
	}
}
package topicsproject;

import java.awt.GridLayout;
import javax.swing.*;

@SuppressWarnings("serial")
public class RoomMakerWindow extends JFrame {
	private JPanel panel;
	
	public RoomMakerWindow() {
		super("Room Maker");
		panel=new JPanel();
		panel.setLayout(new GridLayout(0, 2));
		panel.add(new JLabel("Name: "));
		panel.add(new JTextField());
		panel.add(new JLabel("Description: "));
		panel.add(new JTextField());
		JScrollPane scrollPane=new JScrollPane(panel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		add(scrollPane);
		setSize(400, 400);
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new RoomMakerWindow();
	}
}
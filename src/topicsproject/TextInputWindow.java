package topicsproject;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class TextInputWindow extends JFrame {
	
	public TextInputWindow(final JTextField textField) {
		//setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(400, 400);
		final JTextArea textArea=new JTextArea();
		textArea.setText(textField.getText());
		add(textArea);
		JButton button=new JButton("Confirm");
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				textField.setText(textArea.getText());
				textField.postActionEvent();
				setVisible(false);
				dispose();
			}
			
		});
		add(button, BorderLayout.SOUTH);
		setVisible(true);
	}
}

package topicsproject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import topicsproject.ObjectPanel.FieldTextField;

public class DefaultActionListener implements ActionListener {
	private ObjectPanel panel;
	
	public DefaultActionListener(ObjectPanel p) {
		panel=p;
	}

	public void actionPerformed(ActionEvent e) {
		FieldTextField source=(FieldTextField)e.getSource();
		String text=source.getText();
		Class<?> type=source.getField().getType();
		try {
			if(type.getName().equals("boolean")) {
				if(!text.equalsIgnoreCase("true")&&!text.equalsIgnoreCase("false")) {
					JOptionPane.showMessageDialog(panel, "Please enter a boolean value for "+source.getField().getName());
					return;
				}
				source.getField().set(panel.getInstance(), Boolean.parseBoolean(text));
			}
			else if(type.getName().equals("int"))
				source.getField().set(panel.getInstance(), Integer.parseInt(text));
			else if(type.getName().equals("java.lang.String"))
				source.getField().set(panel.getInstance(), text);
			else
				System.out.println("For some reason the field for "+text+" of type "+type.getName()+" was not set.");
			System.out.println("Unless it says otherwise above, "+source.getField().getName()+" has been assigned the value of "+text);
		} catch(NumberFormatException e1) {
			JOptionPane.showMessageDialog(panel, "Please enter an int value for "+source.getField().getName());
			return;
		} catch(IllegalAccessException e1) {
			panel.setBasicField(source.getField(), text, e1);
		}
	}
}
package topicsproject;

import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ObjectPanel extends JPanel {
	
	private Class<?> objectClass;
	
	public ObjectPanel(String className) {
		super();
		try {
			objectClass=Class.forName("textadventure."+className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		Stack<Class<?>> classes=new Stack<Class<?>>();
		Class<?> c=objectClass;
		classes.add(c);
		while(!c.getName().equals("textadventure.TAObject")) {
			c=c.getSuperclass();
			classes.add(c);
		}
		Queue<Field> fields=new LinkedList<Field>();
		while(!classes.isEmpty()) {
			Field[] f=classes.pop().getDeclaredFields();
			for(int i=0; i<f.length; i++) {
				if(!f[i].toGenericString().contains("static")&&!f[i].toGenericString().contains("final"))
					fields.add(f[i]);
			}
		}
		setLayout(new GridLayout(0, 2));
		while(!fields.isEmpty()) {
			add(new JLabel(fields.poll().getName(), JLabel.CENTER));
			add(new JTextField());
		}
		//use reflection to create JLabels and JTextFields for the instance variables
	}
	
}

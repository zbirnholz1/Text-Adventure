package topicsproject;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import textadventure.TAObject;

@SuppressWarnings("serial")
public class ObjectPanel extends JPanel {
	private Class<?> objectClass;
	private TAObject instance;

	private final Map<String, MouseListener> mouseListeners=initializeMouseListeners();

	public ObjectPanel(String className) {
		super();
		try {
			objectClass=Class.forName("textadventure."+className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		try {
			instance=(TAObject)objectClass.newInstance();
		} catch (Exception e) {
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
			Field f=fields.poll();
			add(new JLabel(f.getName(), JLabel.CENTER));
			if(f.getType().getName().equals("boolean")) {
				JCheckBox checkBox=new JCheckBox();
				checkBox.addActionListener(new CheckBoxListener(f));
				add(checkBox);
			}
			else {
				FieldTextField textField=new FieldTextField(f);
				textField.addActionListener(new DefaultActionListener(this));
				if(mouseListeners.get(f.getName())!=null)
					textField.addMouseListener(mouseListeners.get(f.getName()));
				add(textField);
			}
		}
	}

	public void setBasicField(Field field, String text, Exception e1) {
		Class<?> c=objectClass;
		Class<?> type=field.getType();
		String methodName=Character.toUpperCase(field.getName().charAt(0))+field.getName().substring(1);
		if(type.getName().equals("boolean")&&!methodName.substring(0, 2).equals("Is"))
			methodName="Is"+methodName;
		while(!c.getName().equals("java.lang.Object")) {
			try {
				Method method=c.getDeclaredMethod("set"+methodName, new Class<?>[]{type});
				if(type.getName().equals("boolean"))
					method.invoke(instance, new Object[]{Boolean.parseBoolean(text)});
				else if(type.getName().equals("int"))
					method.invoke(instance, new Object[]{Integer.parseInt(text)});
				else if(type.getName().equals("java.lang.String"))
					method.invoke(instance, new Object[]{text});
				else
					System.out.println("For some reason the field for "+text+" of type "+type.getName()+" was not set.");
			} catch (Exception e2) {
				//e2.printStackTrace();
				c=c.getSuperclass();
				continue;
			}
			break;
		}
		try {
			if(text.equalsIgnoreCase(""+field.get(instance)))
				System.out.println("Unless it says otherwise above, "+field.getName()+" has been assigned the value of "+text);
			else {
				if(e1!=null)
					e1.printStackTrace();
				else
					System.out.println("There was an error setting "+field+" with the value "+text);
			}
		} catch(Exception e2) {
			Method getMethod=null;
			String getMethodName="get"+methodName;
			if(type.getName().equals("boolean")) {
				if(methodName.substring(0, 2).equals("Is"))
					getMethodName="i"+methodName.substring(1);
				else
					getMethodName="is"+methodName;
			}
			c=objectClass;
			while(!c.getName().equals("java.lang.Object")) {
				try {
					getMethod=c.getDeclaredMethod(getMethodName, new Class<?>[]{});
				} catch (Exception e3) {
					//e2.printStackTrace();
					c=c.getSuperclass();
					continue;
				}
				break;
			}
			try {
				if(text.equalsIgnoreCase(""+getMethod.invoke(instance, new Object[]{})))
					System.out.println("Unless it says otherwise above, "+field.getName()+" has been assigned the value of "+text);
				else {
					if(e1!=null)
						e1.printStackTrace();
					else
						System.out.println("Error! Field: "+field+", Value: "+text);
				}
			} catch(Exception e3) {
				if(e1!=null)
					e1.printStackTrace();
				else 
					System.out.println("Error! Field: "+field+", Value: "+text);
				e3.printStackTrace();
			}
		}
	}

	public TAObject getInstance() {
		return instance;
	}

	private Map<String, MouseListener> initializeMouseListeners() {
		Map<String, MouseListener> map=new HashMap<String, MouseListener>();
		map.put("description", new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				new TextInputWindow((JTextField)e.getSource());
			}
			public void mouseEntered(MouseEvent arg0) {
			}
			public void mouseExited(MouseEvent arg0) {
			}
			public void mousePressed(MouseEvent arg0) { 
			}
			public void mouseReleased(MouseEvent arg0) {
			}

		});
		//TODO add special listeners for fields like description, etc.
		return map;
	}

	class FieldTextField extends JTextField {
		private Field field;

		public FieldTextField(Field f) {
			super();
			field=f;
		}

		public Field getField() {
			return field;
		}
	}

	class CheckBoxListener implements ActionListener {
		private Field field;

		public CheckBoxListener(Field f) {
			field=f;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				String value=new Boolean(((JCheckBox)e.getSource()).isSelected()).toString();
				setBasicField(field, value, null);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}
}

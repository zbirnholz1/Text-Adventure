package topicsproject;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import textadventure.TAObject;

@SuppressWarnings("serial")
public class ObjectPanel extends JPanel {
	private Class<?> objectClass;
	private TAObject instance;
	private JTextField fieldInFocus;

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
			FieldTextField textField=new FieldTextField(f);
			textField.addActionListener(new DefaultActionListener());
			if(mouseListeners.get(f.getName())!=null)
				textField.addMouseListener(mouseListeners.get(f.getName()));
			add(textField);
		}
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

	class DefaultActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			FieldTextField source=(FieldTextField)e.getSource();
			String text=source.getText();
			Class<?> type=source.getField().getType();
			try {
				if(type.getName().equals("boolean")) {
					if(!text.equalsIgnoreCase("true")&&!text.equalsIgnoreCase("false")) {
						JOptionPane.showMessageDialog(ObjectPanel.this, "Please enter a boolean value for "+source.getField().getName());
						return;
					}
					source.getField().set(instance, Boolean.parseBoolean(text));
				}
				else if(type.getName().equals("int"))
					source.getField().set(instance, Integer.parseInt(text));
				else if(type.getName().equals("java.lang.String"))
					source.getField().set(instance, text);
				else
					System.out.println("For some reason the field for "+text+" of type "+type.getName()+" was not set.");
				System.out.println("Unless it says otherwise above, "+source.getField().getName()+" has been assigned the value of "+text);
			} catch(NumberFormatException e1) {
				JOptionPane.showMessageDialog(ObjectPanel.this, "Please enter an int value for "+source.getField().getName());
				return;
			} catch(IllegalAccessException e1) {
				String methodName=Character.toUpperCase(source.getField().getName().charAt(0))+source.getField().getName().substring(1);
				Class<?> c=objectClass;
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
					if(text.equalsIgnoreCase(""+source.getField().get(instance)))
						System.out.println("Unless it says otherwise above, "+source.getField().getName()+" has been assigned the value of "+text);
					else
						e1.printStackTrace();
				} catch(Exception e2) {
					Method getMethod=null;
					String getMethodName="get"+methodName;
					if(type.getName().equals("boolean"))
						getMethodName="is"+methodName;
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
							System.out.println("Unless it says otherwise above, "+source.getField().getName()+" has been assigned the value of "+text);
						else
							e1.printStackTrace();
					} catch(Exception e3) {
						e1.printStackTrace();
						e3.printStackTrace();
					}
				}
			}
		}
	}
}

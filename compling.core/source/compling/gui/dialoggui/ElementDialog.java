//----------------------------------------------------------------------------------
//ElementDialog.java 

//based on 
//Carter Wendelken's ShrutiAgent : ElementDialog.java 
//August 2000
//----------------------------------------------------------------------------------

package compling.gui.dialoggui;

import javax.swing.*;

import java.util.*;
import java.beans.*; //Property change stuff
import java.awt.*;
import java.awt.event.*;
import compling.util.*;
import compling.gui.*;


/**
 * The ElementDialog encapsulates basic properties of all the
 * element-construction dialog. An ElementDialog is a set of key-field pairs,
 * where a String key is followed in the display by an input field of some form
 * (currently, fields can be of type DialogCell, JTextField, Choice, JComboBox,
 * or JList). The ElementDialog also includes an "OK" button and a "Cancel"
 * button, with appropriate functionality.
 */
public abstract class ElementDialog extends JDialog implements PropertyChangeListener {

   private JOptionPane optionPane;

   public class Options extends HashMap<String, Triplet<Component, Object, Class<?>>> {

      private static final long serialVersionUID = -1658773774780600885L;

      public Options() {         
         super(); 
      }
      
      public void put(String key, Component component, Object value, Class<?> valueType) {         
         Triplet<Component, Object, Class<?>> triplet = new Triplet<Component, Object, Class<?>>(component, value, valueType);
         put(key, triplet);
      }
   }
   
   protected Options options;

   public ElementDialog(JFrame frame, String title) {
      super(frame, true);
      setTitle(title);
      setupParameters();
      initialize(options);
   }

   abstract protected void setupParameters();


   /** Construct this dialog with the given keys and fields. */
   protected void initialize(Options options) {
      this.options = options;

      ArrayList<Object> message = new ArrayList<Object>();
      for (String field : options.keySet()) {
         message.add(field);
         message.add(options.get(field));
      }

      ArrayList<Object> response = new ArrayList<Object>();
      response.add("OK");
      response.add("Cancel");

      optionPane = new JOptionPane(message.toArray(), JOptionPane.QUESTION_MESSAGE,
         JOptionPane.YES_NO_OPTION, null, response.toArray(), response.get(0));
      setContentPane(optionPane);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      addWindowListener(new WindowAdapter() {

         public void windowClosing(WindowEvent we) {
            optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
         }
      });

      optionPane.addPropertyChangeListener(this);
   }


   /**
    * Returns the data entered into the designated field, 
    * or null if no valid data was entered and confirmed.
    */
   public Pair<Object, Class<?>> getValidatedField(String key) {
      return options.get(key) == null ? null : 
         new Pair<Object, Class<?>>(options.get(key).getSecond(), options.get(key).getThird());
   }


   /**
    * Handles a property change event, eg a change in optionPane value
    * resulting from a button press.
    */
   public void propertyChange(PropertyChangeEvent e) {
      // String prop = e.getPropertyName();

      if (isVisible() && (e.getSource() == optionPane)) {
         Object value = optionPane.getValue();
         if (value == null || value == JOptionPane.UNINITIALIZED_VALUE)
            return;
         optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

         if (value.equals("OK")) {
            for (String key : options.keySet()) {
               if (options.get(key) == null) {
                  throw new GUIException("no components defined for the parameter option " + key);
               }
               Component field = options.get(key).getFirst();
               Object val;
               if (field instanceof JTextField)
                  val = ((JTextField) field).getText();
               else if (field instanceof Choice)
                  val = ((Choice) field).getSelectedItem();
               else if (field instanceof JComboBox)
                  val = ((JComboBox) field).getSelectedItem();
               else if (field instanceof JList)
                  val = ((JList) field).getSelectedValues();
               else if (field instanceof DialogCell)
                  val = ((DialogCell) field).getSelectedValues();
               else if (field instanceof JToggleButton)
                  val = ((JCheckBox) field).isSelected() ? new Boolean(true) : new Boolean(false);
               else if (field instanceof JSlider)
                  val = new Integer(((JSlider) field).getValue());
               else
                  throw new Error("Invalid field type");

               /*if (val == null
                || (val instanceof String && val.equals(""))) {
                voidAllFields();
                return;
                }*/
               options.get(key).setSecond(val);
            }
            // todo: actually *set* the parameters!
            //System.out.println(values.toString());
            updateParams();
            setVisible(false);
         } else { // user closed dialog or clicked cancel
            //voidAllFields();
            setVisible(false);
         }
      }
   }


   
   /** Set all stored values to null. */
   /*
   private void voidAllFields() {
      for (int i = 0; i < values.size(); i++)
         values.set(i, null);
   }
*/

   protected void addListCell(String key, Vector<?> elements) {
      if (!elements.isEmpty()) {
         JList list = new JList(elements);
         list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         options.put(key, list, null, elements.get(0).getClass());
      }      
   }

   
   protected <T> void addChoiceCell(String key, java.util.List<T> choices, T defaultChoice) {
      Choice c = new Choice();
      for (T choice : choices) {
         c.add(choice.toString());
      }
      c.select(defaultChoice.toString());
      options.put(key, c, null, defaultChoice.getClass());
   }


   // I don't think this will work
   protected void addDialogCell(String key, DialogCell cell) {
      options.put(key, cell, null, cell.getClass());
   }


   protected void addRadioPanel(String key, java.util.List<String> choices, String defaultChoice) {
      JPanel panel = new JPanel();
      ButtonGroup group = new ButtonGroup();
      for (String choice : choices) {
         JRadioButton b = new JRadioButton(choice);
         b.setActionCommand(choice);
         if (choice.equals(defaultChoice)) {
            b.setSelected(true);
         }
         panel.add(b);
         group.add(b);
      }
      options.put(key, panel, null, defaultChoice.getClass());
   }
    
   
   protected void addBoolean(String key, boolean defaultValue) {
      ArrayList<String> choices = new ArrayList<String>();
      choices.add("yes");
      choices.add("no");
      addRadioPanel(key, choices, defaultValue? "yes":"no"); 
   }

   protected void addIntSlider(String key, int min, int max, int major, int minor, int defaultValue) {
      JSlider slider = new JSlider(min, max, defaultValue);
      slider.setMajorTickSpacing(major);
      slider.setMinorTickSpacing(minor);
      slider.setSnapToTicks(true);
      slider.setPaintTicks(true);
      slider.setPaintLabels(true);
      options.put(key, slider, null, Integer.class);
   }


   abstract public void updateParams();


   public void display() {
      pack();
      setVisible(true);
   }
}

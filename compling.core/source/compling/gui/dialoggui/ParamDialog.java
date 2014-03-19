//----------------------------------------------------------------------------------
//ParamDialog.java

//Nancy Chang

//(based on Carter Wendelken's ShrutiAgent : ParamDialog.java) 
//----------------------------------------------------------------------------------

package compling.gui.dialoggui;

import java.awt.Component;
import java.util.*;
import javax.swing.*;
import java.util.prefs.Preferences;

abstract public class ParamDialog extends ElementDialog {

   private static final long serialVersionUID = 1L;
   protected Preferences preferences;

   // -------------------- CONSTRUCTORS --------------------
   public ParamDialog(JFrame frame) {
      this(frame, "Parameters", Preferences.userNodeForPackage(ParamDialog.class));;
   }

   public ParamDialog(JFrame frame, String string, Preferences preferences) {
      super(frame, string);
      this.preferences = preferences;      
      keys = new Vector<String>(); 
      fields = new Vector<Component>();
      setupParameters();
      initialize(keys,fields);
   }


   /**
    * subclasses fill in all actual parameters
    */
   abstract protected void setupParameters();

   /** @overrides ... */
   protected Object getKey(Vector keys, int i) {
/*
      Object pname = super.getKey(keys, i); // Object
      if (params == null)
         return pname;
      Object pdesc = params.getParamDescription((String) pname);
      if (pdesc == null)
         return pname;
      else return pdesc;
      */
      return null;
   }

   // -------------------- CREATING PARAM DISPLAYS --------------------

   protected JSlider createDoubleSlider( double value ) {
      JSlider slider = new JSlider(0,100);
      slider.setValue( (int)(100.0*value) );
      slider.setMajorTickSpacing(25);
      slider.setMinorTickSpacing(5);
      slider.setSnapToTicks(true);
      slider.setPaintTicks(true);
      slider.setPaintLabels(true);
      return slider;
   } 

   protected  JSlider createIntSlider( int value, int min, int max ) {
      return createIntSlider(value, min, max, 4);	
   }

   /** todo allow major, minor to be set as well. */
   protected  JSlider createIntSlider( int value, int min, int max, int major) {
      JSlider slider = new JSlider(min,max,value);
      slider.setMajorTickSpacing(major);
      slider.setMinorTickSpacing(1);
      slider.setSnapToTicks(true);
      slider.setPaintTicks(true);
      slider.setPaintLabels(true);
      return slider;
   }

   protected  JSlider createBooleanSlider( boolean b) {
      JSlider slider = new JSlider(0,1,((b)?1:0));
      slider.setPaintLabels(true);
      return slider;
   }

   // text, selected? // this doesn't work. bad value.
   protected  JPanel createBooleanCheckBox(boolean b, String str) {
      JPanel panel = new JPanel();
      JCheckBox checkbox = new JCheckBox(str, b);
      panel.add(checkbox);
      return panel;
   }

   // not really necessary? See ElementDialog
   protected  JComboBox createComboBox( int valueNum, Vector choices) {
      //	Create the combo box, select item at index 4.
      JComboBox combo = new JComboBox(choices);
      combo.setSelectedIndex(valueNum);
      return combo;
   }

   // also doesn't work
   protected  JPanel createRadioPanel( int valueNum, Vector choices) {
      JPanel panel = new JPanel();
      ButtonGroup group = new ButtonGroup();		
      for (int i = 0; i < choices.size(); i++) {
         JRadioButton b = new JRadioButton((String) choices.get(i));
         panel.add(b);
         group.add(b); // unnecessary?
         if (i == valueNum)
            b.setSelected(true);
      }
      return panel;
   }

   // -------------------- ADDING COMMON TYPES --------------------



   // -------------------- PARAMETER ACCESS --------------------

   public double getDoubleParameter( String param ) {
      Integer val = (Integer)getValidatedField( param );
      return (double)val.intValue()/100.0;           
   }

   public int getIntParameter( String param ) {
      Integer val = (Integer)getValidatedField( param );
      return val.intValue();  
   }

   public boolean getBooleanParameter( String param ) {
      int val = ((Integer)getValidatedField(param)).intValue();
      if( val == 0 ) return false; else return true;
   }

   public String getStringParameter( String param) {
      String str = (String)getValidatedField(param);
      return str;
   }

   //public int getCycleWidth() {
   //  String str = (String)getValidatedField("Cycle width");
   //  return Integer.parseInteger(str);   
   //}

}

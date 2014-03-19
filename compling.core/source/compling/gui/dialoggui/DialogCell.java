//----------------------------------------------------------------------------------
// DialogCell.java   
//
// based on Carter Wendelken's ShrutiAgent : DialogCell.java
// August 2000
//----------------------------------------------------------------------------------

package compling.gui.dialoggui;

import java.awt.Panel;

/** The DialogCell provides a superclass for all of the specialized dialog field classes.  */
abstract class DialogCell extends Panel {

  abstract public Object[] getSelectedValues();
  
}

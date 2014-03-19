//----------------------------------------------------------------------------------
// ECGS : ListDialog.java   
//
// based on Carter Wendelken's SAS.ShrutiGUI.Dialogs.ListDialog
//----------------------------------------------------------------------------------

package compling.gui.dialoggui;

import java.awt.Component;
import java.util.*;
import javax.swing.*;

/** The List Dialog allows for selection of elements from a list. */
public class ListDialog extends ElementDialog {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
String key;
  
  public ListDialog( JFrame top, String key, Vector listElements ) {
    super( top, "List Dialog" );
    this.key = key;
    Vector<String> keys = new Vector<String>();
    Vector<Component> fields = new Vector<Component>();
    keys.add(key);
    JList iList = new JList( listElements );
    fields.add( iList );
    initialize( keys, fields );  
  }    
  
  public Vector<Object> selection() {
    Object[] arr = (Object[])getValidatedField(key);
    Vector<Object> res = new Vector<Object>();
    for( int i = 0; i < arr.length; i++ ) res.add( arr[i] );
    return res;    
  }

}







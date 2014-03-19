/**
 * 
 */
package compling.gui.grammargui;

import java.util.EventObject;

/**
 * A even describing change in the application's model
 *  
 * @author lucag
 */
@SuppressWarnings("serial")
public class ModelChangedEvent extends EventObject {

	public ModelChangedEvent(Object source) {
		super(source);
	}
}

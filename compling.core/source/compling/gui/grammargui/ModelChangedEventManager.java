/**
 * A specialized event manager. 
 */
package compling.gui.grammargui;

import org.eclipse.core.commands.common.EventManager;

/**
 * Used in GrammarBrowser to keep track of 
 * objects (manus) listening for changes in the underlying data.
 * 
 * @author lucag
 */
public class ModelChangedEventManager extends EventManager {

	public void addModelChangeListener(IModelChangedListener listener) {
		addListenerObject(listener);
	}
	
	public void removeModelChangeListener(IModelChangedListener listener) {
		removeListenerObject(listener);
	}
	
	public void notifyModelChanged(Object model) {
		ModelChangedEvent event = new ModelChangedEvent(model);
		for (Object l : getListeners()) {
			((IModelChangedListener) l).modelChanged(event);
		}
	}
}

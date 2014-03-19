/**
 * 
 */
package compling.gui.grammargui.model;

import compling.gui.grammargui.util.ModelChangedEvent;

/**
 * A listener for changes in a model. Used to notify menus
 * 
 * @see compling.gui.grammargui.GrammarBrowserAction
 * @author lucag
 */
public interface IModelChangedListener {
	public void modelChanged(ModelChangedEvent event);
}

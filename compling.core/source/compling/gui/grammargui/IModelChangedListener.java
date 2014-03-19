/**
 * 
 */
package compling.gui.grammargui;

/**
 * A listener for changes in a model. Used to notify menus 
 * @see compling.gui.grammargui.GrammarBrowserAction
 * @author lucag
 */
public interface IModelChangedListener {
	public void modelChanged(ModelChangedEvent event);
}

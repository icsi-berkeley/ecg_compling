/**
 * 
 */
package compling.gui.grammargui;

import org.eclipse.swt.widgets.Widget;


/**
 * A Controller for the application. It receives notifications about selection
 * changes in the two tab groups a reroutes the messages to the relevant
 * windows.
 * 
 * @author lucag
 * 
 */
public interface IGrammarBrowserController {

	/**
	 * Called when a tab in the left pane has to be activated following the
	 * selection of a node in some of the HTML views
	 * @param nodeType
	 *          node to activate in the Tree View
	 * @param nodeName TODO
	 */
	void notifyActivationChanged(String nodeType, String nodeName);

	/**
	 * Create a tabbed view (typically an HTML browser with construction and
	 * schemas turned into links) in the right part.
	 * @param id
	 * 					node to activate in the Tree View
	 * @param view TODO
	 */
	void registerView(String id, Widget view);

	/**
	 * Destroy a tabbed view in the right part.
	 * @param id
	 * 					node to activate in the Tree View
	 * @param view 
	 */
	void unregisterView(String id, Widget view);
}

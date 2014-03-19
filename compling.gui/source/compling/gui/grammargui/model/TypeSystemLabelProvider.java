/**
 * 
 */
package compling.gui.grammargui.model;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.util.Constants;
import compling.gui.util.TypeSystemNodeType;

// TODO: update javadoc

/**
 * @author lucag
 */
public class TypeSystemLabelProvider extends LabelProvider {

	protected ImageRegistry registry;

	protected String nodeToKey(Object element) {
		return Constants.nodeToKey(TypeSystemNodeType.fromNode((TypeSystemNode) element));
	}

	protected String nodeToKey(String nodeType) {
		return Constants.nodeToKey(TypeSystemNodeType.fromString(nodeType));
	}

	public TypeSystemLabelProvider() {
		registry = EcgEditorPlugin.getDefault().getImageRegistry();
	}

	public Image getImage(Object element) {
		return registry.get(nodeToKey(element));
	}

	public String getText(Object element) {
		return ((TypeSystemNode) element).getType();
	}

}

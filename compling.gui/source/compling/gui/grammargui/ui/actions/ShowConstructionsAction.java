package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.EcgEditorPlugin;

public class ShowConstructionsAction extends Action {

	public ShowConstructionsAction() {
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EcgEditorPlugin.PLUGIN_ID, "icons/constr16.png"));
		setToolTipText("Show Constructions");
	}
}

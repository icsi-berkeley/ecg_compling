package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.EcgEditorPlugin;

public class ShowSchemasAction extends Action {

	public ShowSchemasAction() {
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EcgEditorPlugin.PLUGIN_ID, "icons/schema16.png"));
		setToolTipText("Show Schemas");
	}

}

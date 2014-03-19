package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.EcgEditorPlugin;

public class ShowMapsAction extends Action {

	public ShowMapsAction() {
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EcgEditorPlugin.PLUGIN_ID, "icons/map16.png"));
		setToolTipText("Show Maps");
	}

}

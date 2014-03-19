package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.EcgEditorPlugin;

public class ShowSituationsAction extends Action {

	public ShowSituationsAction() {
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EcgEditorPlugin.PLUGIN_ID, "icons/sample.gif"));
		setToolTipText("Show Situations");
	}

}

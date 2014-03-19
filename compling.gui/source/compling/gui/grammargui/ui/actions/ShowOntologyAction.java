package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.EcgEditorPlugin;

public class ShowOntologyAction extends Action {

	public ShowOntologyAction() {
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EcgEditorPlugin.PLUGIN_ID, "icons/ont16.png"));
		setToolTipText("Show the Ontology");
	}

}

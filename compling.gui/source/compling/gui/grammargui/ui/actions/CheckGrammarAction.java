package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;

import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ModelChangedEvent;

public class CheckGrammarAction extends Action implements IModelChangedListener {

	public static final String ID = "compling.gui.grammargui.actions.CheckGrammar";

	private PrefsManager manager;

	public CheckGrammarAction() {
		super();
		manager = PrefsManager.instance();
		manager.addModelChangeListener(this);
		setId(ID);
		setActionDefinitionId(ID);
		setText("Check");
		setEnabled(manager.getProject() != null);
	}

	public void dispose() {
		manager.removeModelChangeListener(this);
	}

	public void modelChanged(ModelChangedEvent event) {
		setEnabled(((PrefsManager) event.getSource()).getProject() != null);
	}

	@Override
	public void run() {
		manager.checkGrammar();
	}

}

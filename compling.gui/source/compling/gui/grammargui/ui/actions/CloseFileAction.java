package compling.gui.grammargui.ui.actions;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.SafeRunnable;

import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ModelChangedEvent;

public class CloseFileAction extends Action implements IModelChangedListener {

	private PrefsManager manager;

	public static final String ID = "compling.gui.grammargui.commands.CloseFile";

	public CloseFileAction() {
		super();
		setId(ID);
		setActionDefinitionId(ID);
		manager = PrefsManager.instance();
		manager.addModelChangeListener(this);
		setText("Close &Preferences File");
		setEnabled(manager.getProject() != null);
	}

	public void dispose() {
		manager.removeModelChangeListener(this);
	}

	public void run() {
		SafeRunner.run(new SafeRunnable() {
			public void run() throws Exception {
				manager.setPreferences(null);
			}
		});
	}

	public void modelChanged(ModelChangedEvent event) {
		setEnabled(((PrefsManager) event.getSource()).getProject() != null);
	}

}

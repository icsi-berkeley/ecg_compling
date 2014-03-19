/**
 * 
 */
package compling.gui.grammargui.ui.actions;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import compling.gui.grammargui.model.PrefsManager;

/**
 * @author lucag
 */
public class OpenFileAction extends Action implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void dispose() {
		this.window = null;
	}

	private String queryPath() {
		FileDialog fileDialog = new FileDialog(window.getShell(), SWT.OPEN);
		fileDialog.setFilterNames(new String[] { "ECG Preference Files", "All Files", });
		fileDialog.setFilterExtensions(new String[] { "*.prefs", "*.*" });

		return fileDialog.open();
	}

	@Override
	public void run() {
		final String path = queryPath();
		if (path != null)
			SafeRunner.run(new SafeRunnable() {
				public void run() throws Exception {
					PrefsManager.instance().setPreferences(path);
				}
			});
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}

package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

import compling.gui.grammargui.ui.views.TypeSystemTreeView;

public class OpenEditorActionDelegate implements IViewActionDelegate {

	private TypeSystemTreeView view;
	private IStructuredSelection selection;

	public void init(IViewPart view) {
		this.view = (TypeSystemTreeView) view;
	}

	public void run(IAction action) {
		try {
			view.openEditorFor(selection.getFirstElement());
		}
		catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		this.selection = (IStructuredSelection) selection;
	}

}

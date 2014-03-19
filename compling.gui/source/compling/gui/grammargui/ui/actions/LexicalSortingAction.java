package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.custom.BusyIndicator;

import compling.gui.grammargui.EcgEditorPlugin;

public class LexicalSortingAction extends Action {

	protected TreeViewer treeViewer;
	protected ViewerComparator comparator;
	protected ViewerComparator sourcePositonComparator;
	
	public LexicalSortingAction(TreeViewer viewer /*, ViewerComparator comparator, ViewerComparator sourViewerComparator*/) {
		super("Sort", EcgEditorPlugin.getImageDescriptor("icons/sort.gif"));

		this.treeViewer = viewer;
		this.comparator = comparator;
		this.sourcePositonComparator = sourcePositonComparator;
		
		setToolTipText("Sort in descending order");
		setDescription("Sort membersin descending order");

//		boolean checked = EcgEditorPlugin.getDefault().getPreferenceStore().getBoolean("LexicalSortingAction.isChecked"); //$NON-NLS-1$
		boolean checked = true;
		
		valueChanged(checked, false);
	}

	@Override
	public void run() {
		valueChanged(isChecked(), true);
	}

	private void valueChanged(final boolean on, boolean store) {
		setChecked(on);
		BusyIndicator.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (on) {
//					viewer.setComparator(comparator);
//					fDropSupport.setFeedbackEnabled(false);
				}
				else {
//					viewer.setComparator(sourcePositonComparator);
//					fDropSupport.setFeedbackEnabled(true);
				}
			}
		});

//		if (store)
//			EcgEditorPlugin.getDefault().getPreferenceStore().setValue("LexicalSortingAction.isChecked", on); //$NON-NLS-1$
	}

}

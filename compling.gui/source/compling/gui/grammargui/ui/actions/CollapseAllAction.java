package compling.gui.grammargui.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

import compling.gui.grammargui.EcgEditorPlugin;

/**
 * An {@link Action} that will collapse all nodes in a given {@link TreeViewer}.
 *
 * @since 3.4
 */
public class CollapseAllAction extends Action {

	private final TreeViewer viewer;

	public CollapseAllAction(TreeViewer viewer) {
		super("Collapse All", EcgEditorPlugin.getImageDescriptor("icons/collapseall.png"));

		// TODO: tooltips!
//		setToolTipText(ActionMessages.CollapsAllAction_tooltip);
//		setDescription(ActionMessages.CollapsAllAction_description);
		
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
		
		Assert.isNotNull(viewer);
		
		this.viewer = viewer;
	}

	@Override
	public void run() {
		try {
			viewer.getControl().setRedraw(false);
			viewer.collapseAll();
		} finally {
			viewer.getControl().setRedraw(true);
		}
	}

}

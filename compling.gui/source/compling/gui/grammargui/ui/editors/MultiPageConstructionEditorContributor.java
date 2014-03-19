package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

public class MultiPageConstructionEditorContributor extends MultiPageEditorActionBarContributor {

	private TextEditorActionContributor editorContributor;

//	private EditorActionBarContributor viewContributor;

	public MultiPageConstructionEditorContributor() {
		editorContributor = new TextEditorActionContributor();
//		viewContributor = new EditorActionBarContributor();
	}

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		editorContributor.init(bars);
		
//		viewContributor.init(bars, page);
		IStatusLineManager statusLineManager = bars.getStatusLineManager();
//		System.err.printf("items (init):\n%s\n", statusLineManager.getItems());
//		System.err.printf("overrides (init):\n%s\n", statusLineManager.getOverrides());
	}

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		IActionBars actionBars = getActionBars();
		IStatusLineManager statusLineManager = actionBars.getStatusLineManager();
//		System.err.printf("items:\n%s\n", statusLineManager.getItems());
//		System.err.printf("overrides:\n%s\n", statusLineManager.getOverrides());
		if (activeEditor instanceof ConstructionEditor) {
//			viewContributor.setActiveEditor(null);
			editorContributor.setActiveEditor(activeEditor);
		}
		else if (activeEditor instanceof PrettyView) {
//			reset(actionBars);
			editorContributor.setActiveEditor(activeEditor);
//			viewContributor.setActiveEditor(activeEditor);
		}
//		System.err.printf("items (after):\n%s\n", statusLineManager.getItems());
//		System.err.printf("overrides (after):\n%s\n", statusLineManager.getOverrides());
		//actionBars.updateActionBars();
	}

//	private void reset(IActionBars actionBars) {
//		actionBars.getStatusLineManager().remove(id);
//	}
}

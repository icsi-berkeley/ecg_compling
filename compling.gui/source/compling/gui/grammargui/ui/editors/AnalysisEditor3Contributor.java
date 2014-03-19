package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;

import compling.gui.grammargui.ui.editors.AnalysisEditor3.IniEditorPage;

public class AnalysisEditor3Contributor extends MultiPageEditorActionBarContributor {
	private IEditorPart activeEditorPart;
	private TextEditorActionContributor editorContributor;

	public AnalysisEditor3Contributor() {
		editorContributor = new TextEditorActionContributor();
	}

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		editorContributor.init(bars);
	}
	
    /**
     * Returns the action registed with the given text editor.
     * @return IAction or null if editor is null.
     */
    protected IAction getAction(ITextEditor editor, String actionID) {
        return (editor == null ? null : editor.getAction(actionID));
    }

    @Override
	public void setActiveEditor(IEditorPart targetEditor) {
//		if (targetEditor instanceof PDESourcePage) {
//			// Fixing the 'goto line' problem -
//			// the action is thinking that source page
//			// is a standalone editor and tries to activate it
//			// #19361
//			PDESourcePage page = (PDESourcePage) targetEditor;
//			PDEPlugin.getActivePage().activate(page.getEditor());
//			return;
//		}
		if (targetEditor instanceof FormEditor) {
			FormEditor editor = (FormEditor) targetEditor;
//		editor.updateUndo(getGlobalAction(ActionFactory.UNDO.getId()), getGlobalAction(ActionFactory.REDO.getId()));
			
//		updateSelectableActions(editor.getSelection());
			setActivePage(editor.getActiveEditor());
		}
	}

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		if (activeEditor instanceof ITextEditor) {
			editorContributor.setActiveEditor(activeEditor);
		}
		
//		if (activeEditorPart == activeEditor)
//			return;
//
//		activeEditorPart = activeEditor;
//
//		IActionBars actionBars = getActionBars();
//		if (actionBars != null) {
//
//			ITextEditor editor = (activeEditor instanceof ITextEditor) ? (ITextEditor) activeEditor : null;
//
//			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
//					getAction(editor, ActionFactory.DELETE.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
//					getAction(editor, ActionFactory.UNDO.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
//					getAction(editor, ActionFactory.REDO.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
//					getAction(editor, ActionFactory.CUT.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
//					getAction(editor, ActionFactory.COPY.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
//					getAction(editor, ActionFactory.PASTE.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
//					getAction(editor, ActionFactory.SELECT_ALL.getId()));
//			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
//					getAction(editor, ActionFactory.FIND.getId()));
//			actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
//					getAction(editor, IDEActionFactory.BOOKMARK.getId()));
//			actionBars.setGlobalActionHandler(IDEActionFactory.ADD_TASK.getId(),
//					getAction(editor, IDEActionFactory.ADD_TASK.getId()));
//			actionBars.updateActionBars();
//		}
	}

}

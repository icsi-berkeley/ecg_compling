/**
 * 
 */
package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;

import compling.gui.grammargui.ui.editors.AnalysisEditor;

public class PrintAction extends Action {

	private AnalysisEditor editor;

	public PrintAction(AnalysisEditor editor) {
		this.editor = editor;
	}

	@Override
	public void run() {
		editor.print();
	}

}
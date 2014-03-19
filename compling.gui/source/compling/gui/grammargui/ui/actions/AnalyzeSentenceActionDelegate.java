package compling.gui.grammargui.ui.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

import compling.gui.grammargui.model.AnalyzerEditorInput;
import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.ui.editors.AnalysisEditor;

public class AnalyzeSentenceActionDelegate implements IViewActionDelegate {

	private IViewPart view;
	private IStructuredSelection selection;

	public static final String ID = "compling.gui.grammargui.actions.analyze";

	public void init(IViewPart view) {
		this.view = view;
	}

	public void run(IAction action) {
		if (! selection.isEmpty()) {
			final AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
			Job parserJob = sentence.getParserJob();
			parserJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override public void run() { openEditor(new AnalyzerEditorInput(sentence)); }
						});
					}
					else {
						Display.getDefault().asyncExec(new Runnable() {
							@Override public void run() {
								MessageBox box = new MessageBox(view.getSite().getShell());
								box.setMessage(String.format("No parses found for \"%s\"", sentence.getText()));
							}
						});
					}
				}
			});
			parserJob.schedule();
		}
	}

	protected void openEditor(IEditorInput input) {
		try {
			view.getSite().getPage().openEditor(input, AnalysisEditor.ID);
		}
		catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = (IStructuredSelection) selection;
	}

}

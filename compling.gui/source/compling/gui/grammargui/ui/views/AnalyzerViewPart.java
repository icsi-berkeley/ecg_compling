package compling.gui.grammargui.ui.views;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.Application;
import compling.gui.grammargui.model.AnalyzerEditorInput;
import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.AnalyzerSentenceContentProvider;
import compling.gui.grammargui.model.AnalyzerSentenceLabelProvider;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.ui.editors.AnalysisEditor;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.ModelChangedEvent;

public class AnalyzerViewPart extends ViewPart {

	private class AddSentenceAction extends Action implements IModelChangedListener {

		public AddSentenceAction() {
			super();

			setText("&Add");
			setToolTipText("Add a new sentence.");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ADD_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ADD_SENTENCE_D));

			setEnabled(PrefsManager.getDefault().getGrammar() != null);
		}

		@Override
		public void run() {
			final String text = "<new sentence>";
			Combo combo = getViewer().getCombo();
			combo.setText(text);
			combo.setSelection(new Point(0, text.length()));
		}

		public void modelChanged(ModelChangedEvent event) {
			setEnabled(isEnabled());
		}

	}

	private class DeleteSentenceAction extends Action implements ISelectionListener {
		private IStructuredSelection selection;

		public DeleteSentenceAction() {
			super();

			setText("&Delete");
			setToolTipText("Delete the current sentence.");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.REMOVE_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.REMOVE_SENTENCE_D));
			getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(DeleteSentenceAction.this);
		}

		@Override
		public void run() {
			AnalyzerSentenceContentProvider provider = (AnalyzerSentenceContentProvider) getViewer().getContentProvider();
			AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
			IWorkbenchPage page = getSite().getPage();
			IEditorPart editor = page.findEditor(new AnalyzerEditorInput(sentence));
			if (editor != null)
				page.closeEditor(editor, false);
			// provider.removeSentence(sentence);
		}

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part == this) {
				this.selection = (IStructuredSelection) selection;
				setEnabled(this.selection.size() == 1);
			}
		}

	}

	private class AnalyzeSentenceAction extends Action implements IModelChangedListener {

		public AnalyzeSentenceAction() {
			super();

			setText("&Analyze");
			setToolTipText("Analyze the current sentence.");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ANALYZE_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin
					.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ANALYZE_SENTENCE_D));

			setEnabled(isAnalyzeActionEnabled());
		}

		@Override
		public void run() {
			final String text = getSentenceText();
			final AnalyzerSentence sentence = new AnalyzerSentence(text, PrefsManager.getDefault());
			Combo combo = comboViewer.getCombo();
			combo.setSelection(new Point(0, text.length()));
			PrefsManager.getDefault().addSentence(sentence);
			Job parserJob = sentence.getParserJob();
			parserJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								openEditor(new AnalyzerEditorInput(sentence));
							}
						});
					}
					else {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								MessageBox box = new MessageBox(getSite().getShell());
								box.setMessage(String.format("No parses found for \"%s\"", sentence.getText()));
							}
						});
					}
				}
			});
			parserJob.schedule();
		}

		protected void openEditor(IEditorInput input) {
			try {
				getSite().getPage().openEditor(input, AnalysisEditor.ID);
			}
			catch (PartInitException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void modelChanged(ModelChangedEvent event) {
			setEnabled(false);
		}

	}

	private ComboViewer comboViewer;
	// private Viewer tableViewer;
	// private Text transcript;

	public static final String ID = "compling.gui.grammargui.views.Analyzer";
	private AnalyzerSentenceContentProvider contentProvider;

	private IAction addSentence;
	private IAction analyzeSentence;
	private IAction deleteSentence;

	public AnalyzerViewPart() {
		// TODO Auto-generated constructor stub
	}

	public boolean isAnalyzeActionEnabled() {
		return getSentenceText().length() > 0 && PrefsManager.getDefault().getGrammar() != null;
	}

	public String getSentenceText() {
		return comboViewer.getCombo().getText();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 1;

		// TableLayout gridLayout = new TableLayout();
		// gridLayout.marginHeight = 2;
		// gridLayout.marginWidth = 2;

		parent.setLayout(gridLayout);

		comboViewer = createComboViewer(parent);
		contentProvider = new AnalyzerSentenceContentProvider(comboViewer);
		comboViewer.setContentProvider(contentProvider);
		comboViewer.setInput(PrefsManager.getDefault());

		getSite().setSelectionProvider(comboViewer);

		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
					// TODO: why this?
					IEditorInput input = new AnalyzerEditorInput(sentence);
					IWorkbenchPage page = getSite().getPage();
					IEditorPart editor = page.findEditor(input);
					if (editor != null)
						page.bringToTop(editor);
				}
			}
		});

		// tableViewer = createTableViewer(parent);

		updateActionBars();
	}

	private Label createLabel(Composite parent) {
		Label label = new Label(parent, SWT.WRAP);
		label.setText("&Sentence:");

		return label;
	}

	private ComboViewer createComboViewer(Composite parent) {
		Composite panel = new Composite(parent, SWT.WRAP);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		// data.grabExcessVerticalSpace = true;
		panel.setLayoutData(data);

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		panel.setLayout(layout);

		Label label = createLabel(panel);
		data = new GridData();
		data.horizontalAlignment = GridData.BEGINNING;
		label.setLayoutData(data);

		ComboViewer viewer = new ComboViewer(panel, SWT.DROP_DOWN);
		viewer.setLabelProvider(new AnalyzerSentenceLabelProvider());

		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		viewer.getCombo().setLayoutData(data);

		viewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object o1, Object o2) {
				final AnalyzerSentence s1 = (AnalyzerSentence) o1;
				final AnalyzerSentence s2 = (AnalyzerSentence) o2;
				return s1.getText().compareToIgnoreCase(s2.getText());
			}
		});

		// viewer.addDoubleClickListener(new IDoubleClickListener() {
		// public void doubleClick(DoubleClickEvent event) {
		// AnalyzerSentence newSentence = new
		// AnalyzerSentence(comboViewer.getCombo().getText(),
		// PrefsManager.getDefault());
		// contentProvider.addSentence(newSentence);
		// comboViewer.add(newSentence);
		// comboViewer.setSelection(new StructuredSelection(new Object[] {
		// newSentence }));
		// }
		// });

		return viewer;
	}

	// private Viewer createTableViewer(Composite parent) {
	// GridData d = new GridData();
	// d.horizontalAlignment = GridData.FILL;
	// d.verticalAlignment = GridData.FILL;
	// d.grabExcessVerticalSpace = true;
	// d.grabExcessHorizontalSpace = true;
	// // d.horizontalSpan = 2;
	//
	// Viewer table = new TableViewer(parent, SWT.VIRTUAL);
	// table.getControl().setLayoutData(d);
	//
	// return table;
	// }

	protected void updateActionBars() {
		analyzeSentence = new AnalyzeSentenceAction();
		addSentence = new AddSentenceAction();
		deleteSentence = new DeleteSentenceAction();

		// Register action as listener for all combo modification events
		Combo combo = comboViewer.getCombo();
		combo.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				analyzeSentence.setEnabled(isAnalyzeActionEnabled());
			}
		});

		// combo.addListener(SWT.DefaultSelection, new Listener() {
		// @Override
		// public void handleEvent(Event event) {
		// Log.logInfo("event: %s\n", event);
		//
		// // IStructuredSelection selection = (IStructuredSelection)
		// comboViewer.getSelection();
		// // assert selection != null;
		//
		// AnalyzerSentence sentence = new AnalyzerSentence(getSentenceText(),
		// PrefsManager.getDefault());
		// // combo.add(string, index);
		// PrefsManager.getDefault().addSentence(sentence);
		// }
		// });

		PrefsManager.getDefault().addModelChangeListener((IModelChangedListener) addSentence);

		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(analyzeSentence);
		toolBarManager.add(new Separator());
		toolBarManager.add(addSentence);
		toolBarManager.add(deleteSentence);
		toolBarManager.add(new Separator());
		toolBarManager.add(new GroupMarker("additions"));

		IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(analyzeSentence);
		menuManager.add(addSentence);
		menuManager.add(deleteSentence);
	}

	@Override
	public void dispose() {
		PrefsManager.getDefault().removeModelChangeListener((IModelChangedListener) addSentence);
		super.dispose();
	}

	/** @return the comboViewer */
	private ComboViewer getViewer() {
		return comboViewer;
	}

	@Override
	public void setFocus() {
		comboViewer.getControl().setFocus();
	}

}

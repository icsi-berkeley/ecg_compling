package compling.gui.grammargui.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.Application;
import compling.gui.grammargui.model.AnalyzerEditorInput;
import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.AnalyzerSentenceContentProvider;
import compling.gui.grammargui.model.AnalyzerSentenceLabelProvider;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.ModelChangedEvent;

public class AnalyzerViewPart extends ViewPart {

	private class AddSentenceAction extends Action implements IModelChangedListener {

		public AddSentenceAction() {
			super();

			setText("&Add");
			setToolTipText("Add a new sentence.");
			setImageDescriptor(AbstractUIPlugin
					.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ADD_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
					IImageKeys.ADD_SENTENCE_D));

			setEnabled(PrefsManager.instance().getGrammar() != null);
		}

		@Override
		public void run() {
			final String text = "<new sentence>";
			Combo combo = getViewer().getCombo();
			combo.setText(text);
			combo.setSelection(new Point(0, text.length()));
		}

		public void modelChanged(ModelChangedEvent event) {
			setEnabled(event.getGrammarProxy().get() != null);
		}

	}

	private class DeleteSentenceAction extends Action implements ISelectionListener {
		private IStructuredSelection selection;

		public DeleteSentenceAction() {
			super();

			setText("&Delete");
			setToolTipText("Delete the current sentence.");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
					IImageKeys.DELETE_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
					IImageKeys.DELETE_SENTENCE_D));
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
			provider.removeSentence(sentence);
		}

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part == this) {
				this.selection = (IStructuredSelection) selection;
				setEnabled(this.selection.size() == 1);
			}
		}

	}

//	private class DeleteSentenceActionDelegate implements IViewActionDelegate {
//
//		private IStructuredSelection selection;
//		
//		public void run(IAction action) {
//			AnalyzerSentenceContentProvider provider = 
//					(AnalyzerSentenceContentProvider) getViewer().getContentProvider();
//			provider.removeSentence(selection.getFirstElement());
//		}
//
//		public void selectionChanged(IAction action, ISelection selection) {
//			this.selection = (IStructuredSelection) selection;
//		}
//
//		@Override
//		public void init(IViewPart view) {
//			// TODO Auto-generated method stub
//			
//		}
//
//	}

	private ComboViewer comboViewer;
//	private Viewer tableViewer;

	// private Text transcript;

	public static final String ID = "compling.gui.grammargui.views.Analyzer";
	private AnalyzerSentenceContentProvider contentProvider;

	private IAction addSentence;

	public AnalyzerViewPart() {
		// TODO Auto-generated constructor stub
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
		comboViewer.setInput(PrefsManager.instance());
		getSite().setSelectionProvider(comboViewer);
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
					IEditorInput input = new AnalyzerEditorInput(sentence);
					IWorkbenchPage page = getSite().getPage();
					IEditorPart editor = page.findEditor(input);
					if (editor != null)
						page.bringToTop(editor);
				}
			}
		});
		
//		tableViewer = createTableViewer(parent);

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

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				AnalyzerSentence newSentence = new AnalyzerSentence(comboViewer.getCombo().getText(), PrefsManager.instance());
				contentProvider.addSentence(newSentence);
				comboViewer.add(newSentence);
				comboViewer.setSelection(new StructuredSelection(new Object[] { newSentence }));
			}
		});
		
		return viewer;
	}

	private Viewer createTableViewer(Composite parent) {
		GridData d = new GridData();
		d.horizontalAlignment = GridData.FILL;
		d.verticalAlignment = GridData.FILL;
		d.grabExcessVerticalSpace = true;
		d.grabExcessHorizontalSpace = true;
		// d.horizontalSpan = 2;
		
		Viewer table = new TableViewer(parent, SWT.VIRTUAL);
		table.getControl().setLayoutData(d);

		return table;
	}

	protected void updateActionBars() {
		addSentence = new AddSentenceAction();
		PrefsManager.instance().addModelChangeListener((IModelChangedListener) addSentence);
		IAction deleteSentence = new DeleteSentenceAction();

		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(addSentence);
		toolBarManager.add(deleteSentence);
		toolBarManager.add(new Separator());
		toolBarManager.add(new GroupMarker("additions"));

		IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(addSentence);
		menuManager.add(deleteSentence);
	}

	@Override
	public void dispose() {
		PrefsManager.instance().removeModelChangeListener((IModelChangedListener) addSentence);
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

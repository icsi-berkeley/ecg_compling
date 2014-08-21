package compling.gui.grammargui.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
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

public class AnalyzerViewPart2 extends ViewPart {

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
			Combo combo = getViewer().getCombo();
			final String text = "<new sentence>";
			combo.setText(text);
			combo.setSelection(new Point(0, text.length()));
		}

		public void modelChanged(ModelChangedEvent event) {
			setEnabled(event.getGrammarProxy().get() != null);
		}

	}

	private class DeleteSentenceAction extends Action implements ISelectionListener {
		private IStructuredSelection selection;

		/**
		 * @param view
		 */
		public DeleteSentenceAction() {
			super();

			setText("&Delete");
			setToolTipText("Delete the current sentence.");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
					IImageKeys.DELETE_SENTENCE_E));
			setDisabledImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
					IImageKeys.DELETE_SENTENCE_D));
			getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		}

		@Override
		public void run() {
			ContentViewer viewer = getViewer();
			AnalyzerSentenceContentProvider provider = (AnalyzerSentenceContentProvider) viewer.getContentProvider();
			AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
			IWorkbenchPage page = getSite().getPage();
			IEditorPart editor = page.findEditor(new AnalyzerEditorInput(sentence));
			if (editor != null)
				page.closeEditor(editor, false);
			provider.removeSentence(sentence);
		}

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part == AnalyzerViewPart2.this) {
				this.selection = (IStructuredSelection) selection;
				setEnabled(this.selection.size() == 1);
			}
		}

	}

	private ComboViewer comboViewer;
//	private CheckboxTableViewer tableViewer;

	// private Text transcript;

	public static final String ID = "compling.gui.grammargui.views.analyzer2";
	private AnalyzerSentenceContentProvider contentProvider;

	private IAction addSentence;
	private FormToolkit toolkit;
	private ScrolledForm form;

	private TreeViewer sentenceGroupViewer;
	
	// UI Stuff
//	private SashForm splitterForm;

	public AnalyzerViewPart2() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());

		form = toolkit.createScrolledForm(parent);

		// Layout
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 1;
		layout.leftMargin = 1;
		layout.rightMargin = 1;
		layout.topMargin = 1;
		
		form.getBody().setLayout(layout);

		// Label
//		toolkit.createLabel(form.getBody(), "&Sentence:");

		// Sections
		Section sentenceSection = createSection(form.getBody(), "Sentence", false);
		createSection(form.getBody(), "Analysis", true);
		Section groupSection = createSection(form.getBody(), "Groups", true);
		createSection(form.getBody(), "Test Results", true);

		// Combo
		comboViewer = createComboViewer(sentenceSection);
		CCombo combo = comboViewer.getCCombo();
		toolkit.adapt(combo, true, true);
//		combo.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		sentenceSection.setClient(combo);
		
		// The combo originates selections
		getSite().setSelectionProvider(comboViewer);

		// Content provider...
		contentProvider = new AnalyzerSentenceContentProvider(comboViewer);
		comboViewer.setContentProvider(contentProvider);
		comboViewer.setInput(PrefsManager.instance());
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

		// Sentence Group Viewer
		sentenceGroupViewer = createSentenceGroupViewer(groupSection);
		Control tree = sentenceGroupViewer.getControl();
		toolkit.adapt(tree, true, true);
		groupSection.setClient(tree);
		
		// Action bars
		updateActionBars();
	}

	private Section createSection(Composite parent, String title, boolean expandable) {
		int flags = Section.TITLE_BAR | Section.EXPANDED;
		if (expandable)
			flags |= Section.TWISTIE;
		Section section = toolkit.createSection(parent,  flags);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
//		td.colspan = 2;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		section.setText(title);
		
		return section;
	}

	private ComboViewer createComboViewer(Composite parent) {
		ComboViewer viewer = new ComboViewer(new CCombo(parent, SWT.DROP_DOWN + SWT.BORDER + SWT.FLAT));
		viewer.setLabelProvider(new AnalyzerSentenceLabelProvider());
		viewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object s1, Object s2) {
				return ((AnalyzerSentence) s1).getText().compareToIgnoreCase(((AnalyzerSentence) s2).getText());
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				AnalyzerSentence newSentence = new AnalyzerSentence(comboViewer.getCombo().getText(), PrefsManager.instance());
				contentProvider.addSentence(newSentence);
			}
		});

		return viewer;
	}

	private CheckboxTableViewer createTestViewer(Composite parent) {
		GridData d = new GridData();
		d.horizontalAlignment = GridData.FILL;
		d.verticalAlignment = GridData.FILL;
		d.grabExcessVerticalSpace = true;
		d.grabExcessHorizontalSpace = true;
		d.horizontalSpan = 2;
		Table table = new Table(parent, SWT.VIRTUAL);
		table.setLayoutData(d);

		return new CheckboxTableViewer(table);
	}

	private TreeViewer createSentenceGroupViewer(Composite parent) {
		return new ContainerCheckedTreeViewer(parent, SWT.BORDER);
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
		toolkit.dispose();
		super.dispose();
	}

	/** @return the comboViewer */
	public ComboViewer getViewer() {
		return comboViewer;
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

}

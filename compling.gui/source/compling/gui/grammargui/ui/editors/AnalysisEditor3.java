package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.CollapseAllHandler;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.AnalyzerSentenceContentProvider;
import compling.gui.grammargui.model.AnalyzerSentenceLabelProvider;
import compling.gui.grammargui.model.CompositeContentProvider;
import compling.gui.grammargui.model.IAnalyzerEditorInput;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TestSentenceModel;
import compling.gui.grammargui.model.TestSentenceModel.AbstractElement;
import compling.gui.grammargui.ui.actions.AbstractToggleLinkingAction;
import compling.gui.grammargui.ui.actions.CollapseAllAction;
import compling.gui.grammargui.ui.actions.LexicalSortingAction;
import compling.gui.grammargui.util.IComposite;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.OutlineElementType;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

public class AnalysisEditor3 extends FormEditor implements IGotoMarker, ISelectionListener {
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gotoMarker(IMarker marker) {
		setActivePage(Page.INI_EDITOR);
		IDE.gotoMarker(getEditorPage(), marker);
	}

	public static final String ID = "compling.gui.grammargui.editors.testSentence";

	protected TestSentenceModel model;

	/** Indices for pages. */
	protected interface Page {
		int ANALYZE_SENTENCE = 0;
		int TEST_SENTENCE = 1;
		int INI_EDITOR = 2;
	}

	protected OutlinePageHost outlineHost;
	
	// Pages ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected class TestSentencePage extends FormPage {

		public TestSentencePage(String id, String title) {
			super(AnalysisEditor3.this, id, title);
		}

		// TODO: More stuff here!
	}

	protected class IniEditorPage extends TextEditor implements IFormPage {
		protected Control control;
		private boolean active;
		private int index;
		private IContentOutlinePage outline;

		protected class LabelProvider extends BaseLabelProvider implements ILabelProvider {
			protected ImageRegistry registry;

			public LabelProvider() { this.registry = EcgEditorPlugin.getDefault().getImageRegistry(); }

			@Override public Image getImage(Object element) { 
				return registry.get(OutlineElementType.valueOf(element).getPath());
			}

			@Override public String getText(Object element) {
				final String content = ((AbstractElement) element).content();

				return content != null ? content : element.toString();
			}
		}

		protected class OutlinePage extends ContentOutlinePage {
			protected CollapseAllAction collapseAllAction;
			protected IAction toggleLinkingAction;
			public OpenAndLinkWithEditorHelper openAndLinkWithEditorHelper;

			protected void registerToolbarActions(IActionBars actionBars) {
				IToolBarManager toolBarManager = actionBars.getToolBarManager();

				collapseAllAction = new CollapseAllAction(getTreeViewer());
				collapseAllAction.setActionDefinitionId(CollapseAllHandler.COMMAND_ID);
				toolBarManager.add(collapseAllAction);

				toolBarManager.add(new LexicalSortingAction(getTreeViewer()));
			}

			/**
			 * This action toggles whether this Java Outline page links its selection to the active editor.
			 * 
			 * @since 3.0
			 */
			public class ToggleLinkingAction extends AbstractToggleLinkingAction {

				/**
				 * Constructs a new action.
				 */
				public ToggleLinkingAction() {
//					boolean isLinkingEnabled= EcgEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE);
					boolean isLinkingEnabled = true;
					setChecked(isLinkingEnabled);
					openAndLinkWithEditorHelper.setLinkWithEditor(isLinkingEnabled);
				}

				/**
				 * Runs the action.
				 */
				@Override
				public void run() {
					final boolean isChecked = isChecked();
//					PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, isChecked);
//					if (isChecked && fEditor != null)
//						fEditor.synchronizeOutlinePage(fEditor.computeHighlightRangeSourceReference(), false);

					openAndLinkWithEditorHelper.setLinkWithEditor(isChecked);
				}

			}

			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);

				TreeViewer viewer = getTreeViewer();
				viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
				viewer.setUseHashlookup(true);
				viewer.setLabelProvider(new LabelProvider());
				viewer.setContentProvider(new CompositeContentProvider<String>() {
					@Override public IComposite<String> getComposite(Object inputElement) {
						return getModel().getRootGroup();
					}
				});

				// Action bars
				IPageSite pageSite = getSite();
				IActionBars actionBars = pageSite.getActionBars();
				
				registerToolbarActions(actionBars);

				IHandlerService handlerService = (IHandlerService) pageSite.getService(IHandlerService.class);
//				handlerService.activateHandler(
//						IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR, new ActionHandler(toggleLinkingAction));

				// Collapse all handler
				handlerService.activateHandler(CollapseAllHandler.COMMAND_ID, new ActionHandler(collapseAllAction));

				if (getEditorInput() != null)
					viewer.setInput(getEditorInput());
			}

		}

		@Override
		public void createPartControl(Composite parent) {
			super.createPartControl(parent);
			
			Control[] children = parent.getChildren();
			control = children[children.length - 1];

			assert control != null;

//			PlatformUI.getWorkbench().getHelpSystem().setHelp(fControl, IHelpContextIds.MANIFEST_SOURCE_PAGE);
		}

		@Override
		public void initialize(FormEditor editor) {
			// nothing to do, for now...
		}
		
		@Override
		public FormEditor getEditor() {
			// XXX: check this!
			return AnalysisEditor3.this;
		}

		@Override
		public IManagedForm getManagedForm() {
			// not a form page
			return null;
		}

		@Override
		public void setActive(boolean active) {
			this.active = active;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public boolean canLeaveThePage() {
			return true;
		}

		@Override
		public Control getPartControl() {
			return control;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public boolean isEditor() {
			return true;
		}

		@Override
		public boolean selectReveal(Object object) {
			if (object instanceof IMarker) {
				IDE.gotoMarker(this, (IMarker) object);
				return true;
			}
			return false;
		}

		@Override
		public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
//			Log.consoleLog("IniEditorPage.getAdapter: %s", adapter);
			System.out.printf("IniEditorPage.getAdapter: %s\n", adapter);

			if (IContentOutlinePage.class.equals(adapter)) {
				if (outline == null)
					outline = new OutlinePage();
			
				return outline; 
			}
			return super.getAdapter(adapter);
		}

	}

	protected class AnalysisPage extends FormPage {
		
		@Override
		public void dispose() {
			tabFolder.dispose();
			tabFolder = null;
			
			button.dispose();
			button = null;
			
			outline.dispose();
			outline = null;
			
			comboViewer = null;
			
			super.dispose();
		}

		public static final String PAGE_ID = "analysis";

		private CTabFolder tabFolder;
//		private Text text;
		private ComboViewer comboViewer;
		private Button button;
		private AnalyzerSentenceContentProvider contentProvider;
		private AnalysisHtmlBuilder htmlBuilder;
		private AnalysisOutline outline;

		@Override
		public Object getAdapter(@SuppressWarnings("rawtypes") Class required) {
			Log.consoleLog("AnalysisPage.getAdapter: %s", required);
			
			if (IContentOutlinePage.class.equals(required)) {
				if (outline == null)
					outline = new AnalysisOutline();
				
				return outline;
			}
			return super.getAdapter(required);
		}

		public AnalysisPage(String title) {
			super(AnalysisEditor3.this, PAGE_ID, title);
			htmlBuilder = new AnalysisHtmlBuilder();
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

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			FormToolkit toolkit = managedForm.getToolkit();

			form.getBody().setLayout(new GridLayout());

			// Analysis section.
			Section analysisSection = createSection(form, toolkit);
			analysisSection.setText("Analysis");
			analysisSection.setDescription("Type a sentence and press the button to analyze.");
			analysisSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			Composite content = getAnalysisContent(analysisSection, form, toolkit);
			analysisSection.setClient(content);

			// Test Section here.
		}

		public Composite getAnalysisContent(Composite parent, ScrolledForm form, final FormToolkit toolkit) {
			Composite content = toolkit.createComposite(parent, SWT.NONE);

//			Composite client = form.getBody();

			form.setText("Analyzer (some feedback on A's state here)");

			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = layout.marginHeight = 0;
			layout.verticalSpacing = 1;

			content.setLayout(layout);

			// Combo
			comboViewer = createComboViewer(content);
			CCombo combo = comboViewer.getCCombo();
			combo.setVisibleItemCount(20);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			toolkit.adapt(combo, true, true);
			contentProvider = new AnalyzerSentenceContentProvider(comboViewer);
			comboViewer.setContentProvider(contentProvider);
			comboViewer.setInput(PrefsManager.instance());
			getSite().setSelectionProvider(comboViewer);
			comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					if (!selection.isEmpty()) {
						final AnalyzerSentence sentence = (AnalyzerSentence) selection.getFirstElement();
						Job parserJob = sentence.getParserJob();
						if (sentence.getParses() == null) {
							parserJob.addJobChangeListener(new JobChangeAdapter() {
								@Override
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										Display.getDefault().asyncExec(new Runnable() {
											public void run() {
												System.out.println("Parsing successful.");
												createAnalisysView(sentence.getEditorInput(), toolkit, tabFolder);
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
					}
				}
			});

			// Button
			button = toolkit.createButton(content, "Analyze", SWT.PUSH);
			button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					System.out.printf("pushed: %s\n", comboViewer.getSelection());
				}
			});

			tabFolder = new CTabFolder(content, SWT.NONE);
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
//			tabFolder.addSelectionListener(new SelectionAdapter() {
//				@Override public void widgetSelected(SelectionEvent e) {
//					contentProvider.modelChanged(null);
//				}
//			});
			toolkit.adapt(tabFolder, true, true);

			final Color start = toolkit.getColors().getColor(IFormColors.H_GRADIENT_START);
			final Color end = toolkit.getColors().getColor(IFormColors.H_GRADIENT_END);
			tabFolder.setSelectionBackground(new Color[] { end, start }, new int[] { 50 }, true);
//			tabFolder.setSimple(false);

			toolkit.paintBordersFor(tabFolder);

			tabFolder.setSelection(0);
//			updateSelection();

//			tabFolder.addSelectionListener(new SelectionAdapter() {
//				public void widgetSelected(SelectionEvent e) {
//						updateSelection();
//				}
//			});
			return content;
		}

		protected void disposeTabs() {
			for (int i = 0; i < tabFolder.getItemCount(); ++i)
				tabFolder.getItem(i).dispose();
		}

		// TODO: Life cycle management!
		private void createAnalisysView(IEditorInput input, FormToolkit toolkit, Composite parent) {
			disposeTabs();

			IAnalyzerEditorInput analyzerInput = (IAnalyzerEditorInput) input;
			assert analyzerInput != null;
			
			int page = 1;
			for (Pair<Analysis, Double> pair : analyzerInput.getParsesAsPairs()) {
				Browser b = new Browser(parent, SWT.NONE);
				b.setText(htmlBuilder.getHtmlText(analyzerInput.getSentence().getText(), pair.getFirst()));
				final CTabItem item = new CTabItem(tabFolder, SWT.NONE);
				item.setText(String.format("(%d) cost: %.4f", page, pair.getSecond()));
				item.setControl(b);
				++page;
			}
			
			// Update ouline--terrible!
			getOutlineHost().setPageActive(outline);
			outline.setInput(analyzerInput);
		}

//		private Text createSemspecViewer(final FormToolkit toolkit, Composite parent, String content) {
//			Composite tabContent = toolkit.createComposite(parent, SWT.NONE);
//
//			GridLayout layout = new GridLayout();
//			layout.marginWidth = 0;
//			tabContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//			tabContent.setLayout(layout);
//
//			Text text = toolkit.createText(parent, content, SWT.NONE); //$NON-NLS-1$
//			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

//			Button apply = toolkit.createButton(tabContent, "Add", SWT.PUSH);
//			apply.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
//			apply.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(SelectionEvent e) {
//					createTab(toolkit, "added!", "added content!");
//				}
//			});
//
//			Button reset = toolkit.createButton(tabContent, "Remove", SWT.PUSH); //$NON-NLS-1$
//			reset.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
//
//			return text;
//		}

		private Section createSection(final ScrolledForm form, FormToolkit toolkit) {
			int style = Section.TWISTIE | Section.TITLE_BAR | Section.DESCRIPTION | Section.EXPANDED;
			Section section = toolkit.createSection(form.getBody(), style);
			section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			section.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					form.reflow(false);
				}
			});

			return section;
		}

	}

	// FormEditor Methods //////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	protected FormToolkit createToolkit(Display display) {
		// Create a toolkit that shares colors between editors.
		return new FormToolkit(EcgEditorPlugin.getDefault().getFormColors(display));
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getEditorPage().doSave(monitor);
	}

	protected TextEditor getEditorPage() {
		return (TextEditor) pages.get(Page.INI_EDITOR);
	}
	
	@Override
	public void doSaveAs() {
		getEditorPage().doSaveAs();
	}

	@Override
	public boolean isDirty() {
		boolean dirty = getEditorPage().isDirty();
		System.out.printf("IsDirty: %s\n", dirty);
		
		return dirty;
//		return true;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return getEditorPage().isSaveAsAllowed();
	}

	@Override
	public void setFocus() {
		IFormPage active = getActivePageInstance();
		if (active != null)
			active.getPartControl().setFocus();
	}

	protected TestSentenceModel getModel() {
		return model;
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class required) {
//		Log.consoleLog("AnalyzerEditor3.getAdapter: %s", required);
		
		if (IContentOutlinePage.class.equals(required))
			return outlineHost;
		
		if (IGotoMarker.class.equals(required))
			return this;
		
		return super.getAdapter(required);
	}

	@Override
	protected void addPages() {
		try {
			List<IFormPage> pp = new ArrayList<IFormPage>();
			pp.add(Page.ANALYZE_SENTENCE, new AnalysisPage("Analysis"));
			pp.add(Page.TEST_SENTENCE, new TestSentencePage("compling.gui.page.test", "Test Sentence"));
			pp.add(Page.INI_EDITOR, new IniEditorPage());

			IFileEditorInput input = (IFileEditorInput) getEditorInput();
			IEditorSite site = getEditorSite();
			for (int i = 0; i < pp.size(); ++i) {
				IFormPage page = pp.get(i);
				page.init(site, input);
				addPage(i, page);
//				addPage(page, input);
				if (page.isEditor())
					setPageText(i, input.getName()); // TODO:
			}
		}
		catch (PartInitException e) {
			Log.logError(e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		this.model = new TestSentenceModel(((IFileEditorInput) input).getFile());
	}

	protected OutlinePageHost getOutlineHost() {
		if (outlineHost == null) 
			outlineHost = new OutlinePageHost();
		
		return outlineHost;
	}
	
//	@Override
//	protected void setActivePage(int pageIndex) {
//		Log.consoleLog("setting active page: %d", pageIndex);
//		
//		super.setActivePage(pageIndex);
//		getOutlineHost().setPageActive((IContentOutlinePage) getActivePageInstance().getAdapter(IContentOutlinePage.class));
//	}

	
	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		getOutlineHost().setPageActive((IContentOutlinePage) getActivePageInstance().getAdapter(IContentOutlinePage.class));
	}

}

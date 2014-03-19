package compling.gui.grammargui.ui.views;

import static java.lang.String.format;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.Application;
import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TypeSystemContentProvider;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.grammargui.model.TypeSystemLabelProvider;
import compling.gui.grammargui.ui.actions.ShowConstructionsAction;
import compling.gui.grammargui.ui.actions.ShowMapsAction;
import compling.gui.grammargui.ui.actions.ShowOntologyAction;
import compling.gui.grammargui.ui.actions.ShowSchemasAction;
import compling.gui.grammargui.ui.actions.ShowSituationsAction;
import compling.gui.grammargui.ui.editors.ConstructionEditor;
import compling.gui.grammargui.ui.editors.ConstructionEditorOutlinePage;
import compling.gui.grammargui.ui.editors.MultiPageConstructionEditor;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.gui.util.TypeSystemNodeType;
import compling.ontology.OWLTypeSystemNode;

public abstract class TypeSystemTreeView extends ViewPart {

	protected class OpenConstruction extends Action {

		public OpenConstruction(String text) {
			super(text);
		}

	}

	protected class OpenEditorAction extends Action implements ISelectionListener {

		private IStructuredSelection selection;

		public OpenEditorAction() {
			super();
			setEnabled(false);
			setText("Open &Editor...");
			setToolTipText("Open a multi-page editor on the selected Type System element.");
			setImageDescriptor(EcgEditorPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.OPEN_EDITOR_E));
		}

		@Override
		public void run() {
			try {
				TypeSystemEditorInput editorInput = new TypeSystemEditorInput((TypeSystemNode) selection.getFirstElement());
				getSite().getPage().openEditor(editorInput, MultiPageConstructionEditor.ID);
			}
			catch (PartInitException e) {
				Log.logError(e);
			}
		}

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part == TypeSystemTreeView.this) {
				this.selection = (IStructuredSelection) selection;
				setEnabled(this.selection.size() == 1 && !(this.selection.getFirstElement() instanceof OWLTypeSystemNode));
			}
		}

	}

	protected TreeViewer treeViewer;
	protected IAction openConstructionView;
	protected ISelectionListener pageSelectionListener;
	protected static int viewCount = 0;
	private ISelectionListener editorSelectionListener;
	private OpenEditorAction openEditorAction;

	public abstract String getId();

	protected abstract Object getTypeSystem();

	// protected abstract String getConnectedViewId();

	// protected abstract TreeViewer getTreeViewer();

	protected void createContextMenu() {
		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TypeSystemTreeView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuManager.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		IWorkbenchPartSite site = getSite();
		site.registerContextMenu(menuManager, treeViewer);
		site.setSelectionProvider(treeViewer);
	}

	protected void fillContextMenu(IMenuManager menuManager) {
		boolean isEmpty = treeViewer.getSelection().isEmpty();
		openConstructionView.setEnabled(!isEmpty);
		menuManager.add(openConstructionView);
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * To be reimplemented by subclasses!
	 */
	protected void createActions() {
		openConstructionView = new OpenConstruction("Open Construction View");
		openConstructionView.setToolTipText("Open or activate the Construction" + " in which this element is declared");
	}

	protected void createControls(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.VIRTUAL | SWT.MULTI);
		treeViewer.setUseHashlookup(true);
		treeViewer.setLabelProvider(new TypeSystemLabelProvider());
		treeViewer.setContentProvider(new TypeSystemContentProvider(treeViewer));
		treeViewer.setComparator(new TypeSystemTreeViewSorter());
		// treeViewer.setComparer(new IElementComparer() {
		//
		// public int hashCode(Object element) {
		// return Utils.toKey((TypeSystemNode) element).hashCode();
		// }
		//
		// public boolean equals(Object a, Object b) {
		// if (! (a instanceof TypeSystemNode) || ! (b instanceof TypeSystemNode))
		// return false;
		//
		// TypeSystemNode n1 = (TypeSystemNode) a;
		// TypeSystemNode n2 = (TypeSystemNode) b;
		// boolean e =
		// TypeSystemNodeType.fromNode(n1).equals(TypeSystemNodeType.fromNode(n2));
		// return e && n1.getType().equals(n2.getType());
		// }
		// });
		treeViewer.setInput(getTypeSystem());

		getSite().setSelectionProvider(treeViewer);

		PrefsManager.instance().addModelChangeListener(new IModelChangedListener() {
			public void modelChanged(ModelChangedEvent event) {
				if (event.getGrammarProxy() != null)
					SafeRunner.run(new SafeRunnable() {
						public void run() throws Exception {
							if (treeViewer == null)
								return;

							Control control = treeViewer.getControl();
							if (control != null && !control.isDisposed()) {
								control.setRedraw(false);
								treeViewer.setInput(getTypeSystem());
								control.setRedraw(true);
							}
						}
					});
			}
		});
	}

	protected void createActionBars() {
		openEditorAction = new OpenEditorAction();
		getSite().getPage().addSelectionListener(openEditorAction);
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(openEditorAction);
		menuManager.add(new Separator());
		menuManager.add(new GroupMarker("additions"));

		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(openEditorAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(new GroupMarker("additions"));
	}

	protected void createToolbarButtons() { // TODO
		IAction showConstructions = new ShowConstructionsAction() {
		};
		IAction showSchemas = new ShowSchemasAction() {
		};
		IAction showOntology = new ShowOntologyAction() {
		};
		IAction showMaps = new ShowMapsAction() {
		};
		IAction showSituations = new ShowSituationsAction() {
		};

		IToolBarManager manager = getViewSite().getActionBars().getToolBarManager();
		manager.add(showConstructions);
		manager.add(showSchemas);
		manager.add(showOntology);
		manager.add(showMaps);
		manager.add(showSituations);
	}

	public static String getViewIdFor(TypeSystemNode node) {
		switch (TypeSystemNodeType.fromNode(node)) {
		case CONSTRUCTION:
			return ConstructionTreeView.ID;
		case SCHEMA:
			return SchemaTreeView.ID;
		case MAP:
			return MapTreeView.ID;
		case SITUATION:
			return SituationTreeView.ID;
		case ONTOLOGY:
			return OntologyTreeView.ID;
		default:
			throw new IllegalArgumentException(format("Illegal argument: %s", node));
		}
	}

	public void setSelection(TypeSystemNode node) {
		treeViewer.setSelection(new TreeSelection(getParents(node)));
	}

	public TreePath[] getParents(TypeSystemNode node) {
		return ((ILazyTreePathContentProvider) treeViewer.getContentProvider()).getParents(node);
	}

	// protected void hookEditorSelection() {
	// editorSelectionListener = new ISelectionListener() {
	// public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	// if (part == TypeSystemTreeView.this)
	// pageSelectionChanged(part, selection);
	// else
	// editorSelectionChanged(part, selection);
	// }
	// };
	// getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(
	// editorSelectionListener);
	// }
	protected void hookPageSelection() {
		pageSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				pageSelectionChanged(part, selection);
			}
		};
		getSite().getPage().addSelectionListener(pageSelectionListener);
	}

	public static TypeSystemNode extractSystemNode(ITreeSelection selection) {
		Object lastSegment = Util.getLastSegment(selection);
		if (lastSegment instanceof TypeSystemNode)
			return (TypeSystemNode) lastSegment;

		if (lastSegment instanceof ConstructionEditorOutlinePage.ContentProvider.Segment)
			return ((ConstructionEditorOutlinePage.ContentProvider.Segment) lastSegment).getNode();

		return null;
	}

	public void pageSelectionChanged(IWorkbenchPart part, ISelection selection) {
		String sourceId = part.getSite().getId();

		// if (sourceId.equals(ConstructionEditor.ID)) {
		// ITextSelection textSel = (ITextSelection) selection;
		// System.err.printf("editor selection: [%d, %d]\n",
		// textSel.getStartLine(),
		// textSel.getEndLine());
		// }

		if (!sourceId.equals(getId()) && !sourceId.equals(TypeSystemNodeView.ID))
			return;

		if (selection.isEmpty())
			return;

		// System.out.printf("%s> sourceId: %s\n", getClass().getSimpleName(),
		// sourceId);
		// Thread.dumpStack();

		if (part == this || part instanceof TypeSystemNodeView) {
			try {
				TypeSystemNode node = extractSystemNode((ITreeSelection) selection);
				if (node != null) {
					openEditorFor(node);
					if (sourceId.equals(TypeSystemNodeView.ID) && getId().equals(getViewIdFor(node))) {
						treeViewer.setSelection(selection, true);
						getSite().getPage().bringToTop(this);
					}
				}
			}
			catch (PartInitException e) {
				// TODO: better error handling
				Log.logError(e, "pageSelectionChanged");
			}
		}
	}

	public void openEditorFor(Object node) throws PartInitException {
		openEditorFor((TypeSystemNode) node);
	}

	protected void openEditorFor(TypeSystemNode node) throws PartInitException {
		if (! getSite().getPage().isEditorAreaVisible())
			return;

		// final IEditorInput input = new TypeSystemEditorInput(node);
		final IEditorInput input = new FileEditorInput(PrefsManager.instance().getFileFor(node));
		final IWorkbenchPage page = getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null && activeEditor.equals(page.findEditor(input)))
			return;

		// Open editor without activating it
		IDE.openEditor(page, input, ConstructionEditor.ID, false);

		// Display.getDefault().asyncExec(new Runnable() {
		// public void run() {
		// try {
		// // Open editor without activating it
		// IDE.openEditor(page, input, ConstructionEditor.ID, false);
		// } catch (PartInitException e) {
		// Log.logError(e, "Impossible to run editor on input %s", input);
		// e.printStackTrace();
		// }
		// }
		// });
	}

	public TypeSystemTreeView() {
		super();
	}

	@Override
	public void dispose() {
		if (treeViewer != null) {
			treeViewer = null;
		}
		if (pageSelectionListener != null) {
			getSite().getPage().removePostSelectionListener(getId(), pageSelectionListener);
			pageSelectionListener = null;
		}
		if (editorSelectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(editorSelectionListener);
			editorSelectionListener = null;
		}
		if (openEditorAction != null) {
			getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(openEditorAction);
			openEditorAction = null;
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		createControls(parent);
		createActions();
		createContextMenu();
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				hookPageSelection();
			}
		});
		createActionBars();
		// createToolbarButtons();
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	// protected void editorSelectionChanged(IWorkbenchPart part, ISelection
	// selection) {
	// if (!(part instanceof MultiPageConstructionEditor))
	// return;
	// Object first = ((IStructuredSelection) selection).getFirstElement();
	// if (first instanceof SelectionEvent) {
	// SelectionEvent event = (SelectionEvent) first;
	// Grammar grammar = PrefsManager.instance().getGrammar();
	// TypeSystemNode node = Utils.fromDescriptor(grammar, event.text);
	// String nodeType = Utils.getNodeType(node);
	// // if (getId() == getViewIdFor(nodeType))
	// // treeViewer.setSelection(new TreeSelection(getParents(node)));
	// }
	// else if (first instanceof TypeSystemNode) {
	// }
	// else {
	// Log.logInfo(">>> %s: first: %s\n", getClass(), first);
	// }
	// }

}
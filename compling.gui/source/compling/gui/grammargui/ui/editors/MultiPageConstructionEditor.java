package compling.gui.grammargui.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.grammargui.util.Log;
import compling.gui.util.Utils;

public class MultiPageConstructionEditor extends FormEditor implements IGotoMarker, ISelectionListener {

	private static final int VIEW = 0;
	private static final int EDITOR = 1;

	private PrettyView viewEditor;
	private TextEditor editor;

	public static final String ID = "compling.gui.grammargui.editors.multiPageConstructionEditor";

	protected void createViewPage() {
		try {
			viewEditor = new PrettyView();
			int index = addPage(viewEditor, getEditorInput());
			setPageText(index, "Viewer");
			viewEditor.setListener(this);
		}
		catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void openEditorFor(TypeSystemNode node) throws PartInitException {
		final IEditorInput input = new TypeSystemEditorInput(node);
		final IWorkbenchPage page = getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null && activeEditor.equals(page.findEditor(input)))
			return;

		page.openEditor(input, MultiPageConstructionEditor.ID);
	}

	protected void createSourcePage() {
		try {
			editor = new ConstructionEditor(this);
			IFile file = (IFile) getEditorInput().getAdapter(IResource.class);
			Assert.isNotNull(file);
			int index = addPage(editor, new FileEditorInput(file));
			setPageText(index, "Source");
		}
		catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateTitle() {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	public void gotoMarker(IMarker marker) {
		setActivePage(EDITOR);
		IDE.gotoMarker(editor, marker);
	}

	@Override
	protected void createPages() {
		createViewPage();
		createSourcePage();
		updateTitle();
	}

	@Override
	public void setFocus() {
		switch (getActivePage()) {
		case VIEW:
			viewEditor.setFocus();
			break;
		case EDITOR:
			editor.setFocus();
			break;
		default:
			System.err.println("!!! no active page");
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		editor.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
		editor.doSaveAs();
		// setInput(editor.getEditorInput());
	}

	@Override
	public boolean isSaveAsAllowed() {
		return editor.isSaveAsAllowed();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		try {
			Object[] descriptors = ((IStructuredSelection) selection).toArray();
			openEditorFor(Utils.fromDescriptor(PrefsManager.instance().getGrammar(), descriptors));
		}
		catch (PartInitException e) {
			Log.logError(e, "selectionChanged");
		}
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		return editor.isSaveOnCloseNeeded();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class required) {
    Log.logInfo("MultiPageConstructionEditor.getAdapter: %s\n", required);

		Object adapter = editor.getAdapter(required);
		return adapter != null ? adapter : super.getAdapter(required);
	}

	@Override
	protected void addPages() {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	protected void addPages() {
//		// TODO Auto-generated method stub
//		
//	}




}

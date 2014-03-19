package compling.gui.grammargui.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import compling.gui.grammargui.util.Log;

public class ConstructionEditor extends TextEditor {

  public static final String ID = "compling.gui.grammargui.editors.constructionEditor";

  private IEditorPart parentPart;
  private ConstructionEditorOutlinePage outlinePage;

  public ConstructionEditor() {
    this(null);
  }

  public ConstructionEditor(IEditorPart parentPart) {
    super();

    this.parentPart = parentPart;
  }

  // @Override
  // protected void doSetInput(IEditorInput input) throws CoreException {
  // super.doSetInput(input);
  // updateTitle();
  // }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
    if (outlinePage != null)
      outlinePage.setInput(input);
  }

  @Override
  protected void initializeEditor() {
    super.initializeEditor();
    setSourceViewerConfiguration(new EcgSourceViewerConfiguration(this, getPreferenceStore()));
  }

  @Override
  protected void setPartName(String partName) {
    // TODO Auto-generated method stub
    super.setPartName(partName);
  }

  protected void updateTitle() {
    IEditorInput input = getEditorInput();
    setPartName(input.getName());
    setTitleToolTip(input.getToolTipText());
  }

  // @Override
  // protected ISourceViewer createSourceViewer(Composite parent,
  // IVerticalRuler ruler, int styles) {
  // ISourceViewer viewer = super.createSourceViewer(parent, ruler, styles);
  // listener = new ISelectionChangedListener() {
  // public void selectionChanged(SelectionChangedEvent event) {
  // System.err.printf("ConstructionEditor: event: %s\n", event);
  // }
  // };
  // viewer.getSelectionProvider().addSelectionChangedListener(listener);
  // return viewer;
  // }

  // @Override
  // public void dispose() {
  // super.dispose();
  // }

  @Override
  public void close(boolean save) {
    if (parentPart != null)
      getSite().getPage().closeEditor(parentPart, save);
    else
      super.close(save);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class required) {
//    final String name = getEditorInput() != null ? getEditorInput().getName() : "null";
//    Log.logInfo("ConstructionEditor.getAdapter: %s, input: %s\n", required, name);
    if (IContentOutlinePage.class.equals(required)) {
      if (outlinePage == null)
        outlinePage = new ConstructionEditorOutlinePage(getDocumentProvider(), this);

      if (getEditorInput() != null)
        outlinePage.setInput(getEditorInput());

      return outlinePage;
    }

    // if (fProjectionSupport != null) {
    // Object adapter = fProjectionSupport.getAdapter(getSourceViewer(),
    // required);
    // if (adapter != null)
    // return adapter;
    // }
    return super.getAdapter(required);
  }

  /**
   * @return the parent part of this editor
   */
  public IEditorPart getParentPart() {
    return parentPart;
  }

}

package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentRewriteSessionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentRewriteSessionListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.grammargui.model.TypeSystemLabelProvider;
import compling.gui.grammargui.ui.editors.ConstructionEditorOutlinePage.ContentProvider.Segment;
import compling.gui.grammargui.util.Log;
import compling.gui.util.TypeSystemNodeType;
import compling.gui.util.Utils;

public class ConstructionEditorOutlinePage extends ContentOutlinePage {

  protected ConstructionEditor textEditor;
  protected IDocumentProvider documentProvider;
  protected IEditorInput input;
  protected ISelectionListener listener;

  private static final Object[] NONE = new Object[0];

  public class SegmentLabelProvider extends TypeSystemLabelProvider {

    @Override
    public Image getImage(Object element) {
      return registry.get(nodeToKey(((Segment) element).getType()));
    }

    @Override
    public String getText(Object element) {
      return ((Segment) element).getName();
    }

  }

  public class ContentProvider implements ITreeContentProvider, IDocumentListener, IDocumentRewriteSessionListener {

    private final String IDENT = "([\\p{L}\\d_][\\p{L}\\d-_]*)";

    private final Pattern PRIMITIVE = Pattern.compile(String.format("\\W*(?:(?:abstract|general)\\s+)?"
            + "(schema|construction|map|situation)\\s+%s.*", IDENT), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            | Pattern.DOTALL);

    private final Pattern ONTOLOGY = Pattern.compile(String.format("\\W*(type|inst|fun|eq|sub|rel|rem|ind|fil"
            + "|setcurrentinterval)\\s+%s.*", IDENT), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);

    private static final String ECG_PRIMITIVE = "__ecg_primitive";

    private static final String ECG_ONTOLOGICAL_ITEM = "__ecg_ontological_item";

    private IPositionUpdater primitiveUpdater = new DefaultPositionUpdater(ECG_PRIMITIVE);

    private IPositionUpdater ontologyUpdater = new DefaultPositionUpdater(ECG_ONTOLOGICAL_ITEM);

    private List<Segment> content = new ArrayList<Segment>();

    private static final long DELAY = 1000L;

    private Job updaterJob = new Job("Content Updater Job") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            update();
          }
        });
        return Status.OK_STATUS;
      }
    };

    private ISelectionListener listener = new ISelectionListener() {
      public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        processSelection(part, selection);
      }
    };

    public ContentProvider() {
      updaterJob.setPriority(Job.SHORT);
      getSite().getPage().addPostSelectionListener(listener);
    }

    protected void processSelection(IWorkbenchPart part, ISelection selection) {
      if (part instanceof ContentOutline)
        return;

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection ss = (IStructuredSelection) selection;
        Object element = ss.getFirstElement();
        if (element instanceof TypeSystemNode) {
          TypeSystemNode n = (TypeSystemNode) element;
          Segment s = lookup(TypeSystemNodeType.fromNode(n).name(), n.getType());
          // Log.logInfo("CEOP: setSelection: %s\n", s);
          setSelection(s == null ? StructuredSelection.EMPTY : new StructuredSelection(s));
        }
      }
    }

    protected Segment lookup(String type, String name) {
      if (content != null) {
        for (Segment s : content)
          if (s.type.equalsIgnoreCase(type) && s.name.equals(name))
            return s;
      }
      return null;
    }

    public class Segment implements Comparable<Segment> {
      private String type;
      private String name;
      private Position position;

      public Segment(String type, String name, Position position) {
        this.type = Utils.toCapitalized(type);
        this.name = name;
        this.position = position;
      }

      /** @return the type */
      public String getType() {
        return type;
      }

      /** @return the name */
      public String getName() {
        return name;
      }

      /** @return the position */
      public Position getPosition() {
        return position;
      }

      public int compareTo(Segment other) {
        int i = this.type.compareTo(other.type);
        return i != 0 ? i : this.name.compareTo(other.name);
      }

      @Override
      public String toString() {
        return String.format("%s - %s", name, type);
      }

      /** @return the node */
      public TypeSystemNode getNode() {
        final Grammar g = PrefsManager.instance().getGrammar();
        return Utils.fromDescriptor(g, type, name);
      }
    }

    public Object[] getChildren(Object element) {
      return (element == input) ? content.toArray() : NONE;
    }

    public Object getParent(Object element) {
      return element instanceof Segment ? content : null;
    }

    public boolean hasChildren(Object element) {
      return element == input ? content.size() > 0 : false;
    }

    public Object[] getElements(Object inputElement) {
      return content.toArray();
    }

    public void dispose() {
      if (content != null) {
        content.clear();
        content = null;
      }
      if (input != null) {
        IDocument document = documentProvider.getDocument(input);
        if (document != null) {
          document.removeDocumentListener(this);
          input = null;
        }
      }
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if (oldInput != null) {
        IDocument document = documentProvider.getDocument(oldInput);
        if (document != null) {
          try {
            document.removePositionCategory(ECG_PRIMITIVE);
            document.removePositionCategory(ECG_ONTOLOGICAL_ITEM);
            document.removeDocumentListener(this);
          }
          catch (BadPositionCategoryException e) {
          }
          document.removePositionUpdater(primitiveUpdater);
          document.removePositionUpdater(ontologyUpdater);
        }
      }

      content.clear();

      if (newInput != null) {
        IDocument document = documentProvider.getDocument(newInput);
        if (document != null) {
          document.addPositionCategory(ECG_PRIMITIVE);
          document.addPositionUpdater(primitiveUpdater);

          document.addPositionCategory(ECG_ONTOLOGICAL_ITEM);
          document.addPositionUpdater(ontologyUpdater);

          parse(document);

          document.addDocumentListener(this);
          ((IDocumentExtension4) document).addDocumentRewriteSessionListener(this);
//          if (oldInput != newInput)
//            updateSelection();
        }
      }
    }

//    public void segmentChanged(String name) {
//      Segment toShow = lookup(name);
//      if (toShow != null) {
//        TreeSelection selection = new TreeSelection(new TreePath(new Object[] { toShow }));
//        setSelection(selection);
//        SelectionChangedEvent event = new SelectionChangedEvent(ConstructionEditorOutlinePage.this, selection);
//        selectionChanged(event);
//      }
//    }

    public void documentAboutToBeChanged(DocumentEvent event) {
      // Nothing here
    }

    public void documentChanged(DocumentEvent event) {
      switch (updaterJob.getState()) {
      case Job.NONE:
      case Job.RUNNING:
        updaterJob.schedule(DELAY);
        break;
      case Job.SLEEPING:
        updaterJob.wakeUp(DELAY);
        break;
      case Job.WAITING:
        updaterJob.sleep();
        updaterJob.wakeUp(DELAY);
        break;
      default:
        // Shouldn't get here
        Assert.isTrue(false, "Undefined job state");
      }
    }

    protected boolean isComment(IDocument document, int offset) throws BadLocationException, BadPartitioningException {
      String type = ((IDocumentExtension3) document)
              .getContentType(EcgPartitionScanner.ECG_PARTITIONING, offset, false);
      return type.equals(EcgPartitionScanner.ECG_COMMENT) || type.equals(EcgPartitionScanner.ECG_MULTILINE_COMMENT);
    }

    protected void parse(IDocument document) {
      System.out.println(">> parse: entering");
      long start = System.nanoTime();
      try {
        for (int l = 0; l < document.getNumberOfLines(); ++l) {
          int offset = document.getLineOffset(l);

          // Ignore comments
          if (isComment(document, offset))
            continue;

          int length = document.getLineLength(l);
          String line = document.get(offset, length);
          Matcher m = PRIMITIVE.matcher(line);
          if (m.matches()) {
            Position position = new Position(offset);
            document.addPosition(ECG_PRIMITIVE, position);
            content.add(new Segment(m.group(1), m.group(2), position));
          }
          else {
            m = ONTOLOGY.matcher(line);
            if (m.matches()) {
              Position position = new Position(offset);
              document.addPosition(ECG_ONTOLOGICAL_ITEM, position);
              content.add(new Segment(m.group(1), m.group(2), position));
            }
          }
        }
      }
      catch (BadLocationException e) {
        e.printStackTrace();
      }
      catch (BadPositionCategoryException e) {
        e.printStackTrace();
      }
      catch (BadPartitioningException e) {
        e.printStackTrace();
      }
      Collections.sort(content);
      System.out.printf(">> parse: exiting: %.3f ms\n", (System.nanoTime() - start) / 1e6);

    }

//    protected void updateSelection() {
//      IEditorPart parentPart = textEditor.getParentPart();
//      if (parentPart != null) {
//        IEditorInput parentInput = parentPart.getEditorInput();
//        if (parentInput != null && parentInput instanceof TypeSystemEditorInput)
//          segmentChanged(((TypeSystemEditorInput) parentInput).getName());
//      }
//    }

    @Override
    public void documentRewriteSessionChanged(DocumentRewriteSessionEvent event) {
      // TODO Auto-generated method stub
      Log.logInfo("documentRewriteSessionChanged: %s", event);
    }
  }

  public ConstructionEditorOutlinePage(IDocumentProvider documentProvider, ConstructionEditor textEditor) {
    super();
    this.documentProvider = documentProvider;
    this.textEditor = textEditor;
  }

  @Override
  public void init(IPageSite pageSite) {
    super.init(pageSite);
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    TreeViewer viewer = getTreeViewer();
    viewer.setContentProvider(new ContentProvider());
    viewer.setLabelProvider(new SegmentLabelProvider());
    viewer.addSelectionChangedListener(this);
    viewer.setInput(input);
  }

  // protected TypeSystemNode getSelectedNode(ISelection selection) {
  // if (selection instanceof IStructuredSelection) {
  // Object e = ((IStructuredSelection) selection).getFirstElement();
  // if (e instanceof TypeSystemNode)
  // return (TypeSystemNode) e;
  // }
  // return null;
  // }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    setRange(event.getSelection());
    super.selectionChanged(event);
  }

  /**
   * Sets highlight range in editor.
   * 
   * @param selection
   */
  protected void setRange(ISelection selection) {
    if (selection.isEmpty())
      textEditor.resetHighlightRange();
    else {
      Object element = ((IStructuredSelection) selection).getFirstElement();
      if (element instanceof Segment) {
        Segment segment = (Segment) element;
        int start = segment.position.getOffset();
        int length = segment.position.getLength();
        try {
          textEditor.setHighlightRange(start, length, true);
        }
        catch (IllegalArgumentException x) {
          // System.err.printf("setRange: %s\n", x);
          textEditor.resetHighlightRange();
        }
      }
    }
  }

  public void setInput(IEditorInput input) {
    this.input = input;
    update();
  }

  public void update() {
    update(true);
  }

  public void update(boolean resetInput) {
    TreeViewer viewer = getTreeViewer();
    if (viewer != null) {
      Control control = viewer.getControl();
      if (control != null && !control.isDisposed()) {
        control.setRedraw(false);
        if (resetInput)
          viewer.setInput(input);
        viewer.expandAll();
        control.setRedraw(true);
      }
    }
  }

  @Override
  public void dispose() {
    if (listener != null) {
      getSite().getPage().removeSelectionListener(listener);
      listener = null;
    }
    super.dispose();
  }

}

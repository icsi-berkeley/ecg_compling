package compling.gui.grammargui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import compling.context.MiniOntology.Type;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Prefs;
import compling.grammar.ecg.ECGGrammarUtilities.ECGGrammarFormatter;
import compling.grammar.ecg.ECGGrammarUtilities.SimpleGrammarPrinter;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.GUIConstants;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.parser.ecgparser.NoECGAnalysisFoundException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.AnalysisUtilities.*;
import compling.util.PriorityQueue;
import compling.utterance.Sentence;

/**
 * GUI for browsing an ECG Grammar. Displays constructions and schema sets in
 * HTML, with categories hyperlinked. Added new tabs to show Constructions and
 * Schemas. Now completely based on SWT
 * 
 * @author lucag, adapted from emok, adapted from nchang
 */
public class GrammarBrowser extends ApplicationWindow implements GUIConstants,
			IGrammarBrowserController {

   public static final String OUTPUT_SHELL_ID = "Output";
   protected GrammarBrowserModel model;

   /**
    * Delegate object used to manage events
    */
   protected ModelChangedEventManager eventManager 
         = new ModelChangedEventManager();

   /**
    * Maps strings to Widgets. Used for keeping track of the associations
    * between Schema/Construction names and the folder page they are shown.
    */
   private Map<String, Widget> idToView = new HashMap<String, Widget>();

   private ECGGrammarFormatter linkFormatter = new GrammarBrowserTextPrinter();
   private ECGGrammarFormatter textFormatter = new SimpleGrammarPrinter();

   private Shell shell;

   // Top part: left/rightCTabFolder; bottom part: output window
   private SashForm horizontalSplitPane;
   
   // Right/Left CTabFolders
   private SashForm verticalSplitPane;

   private CTabFolder leftTabFolder;
   private CTabFolder rightTabFolder;

   private CTabItem constructionTabItem;
   private CTabItem schemaTabItem;
   private CTabItem ontologyTabItem;

   /** Last sentence inputed */
   private String inputSentence;
   
   /** Text widget in the main window's bottom part */
   private Text outputText;

   /** Construction tree viewer in the left pane.*/
   private TreeViewer constructionTreeViewer;

   /** Schema tree viewer in the left pane.*/
   private TreeViewer schemaTreeViewer;

   /** Ontology tree viewer in the left pane.*/
   private TreeViewer ontologyTreeViewer;

   /**
    * Creates a new top-level window and displays a grammar in panels controlled
    * by tabs.
    * 
    * @param model - The model to display
    * @param parentShell - The parent Shell
    */
   public GrammarBrowser(GrammarBrowserModel model, Shell parentShell) {
      super(parentShell);

      this.model = model;

      addMenuBar();
      addStatusLine();
   }

   @Override
   protected Control createContents(Composite parent) {
      Composite contents = new Composite(parent, SWT.NONE);

      FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL);
      fillLayout.marginWidth = 3;
      fillLayout.marginHeight = 2;
      contents.setLayout(fillLayout);

      shell = getShell();
      shell.setText("Grammar Browser");

      Analysis.setFormatter(new DefaultAnalysisFormatter());

      verticalSplitPane = new SashForm(contents, SWT.VERTICAL | SWT.SMOOTH);
      horizontalSplitPane = new SashForm(verticalSplitPane, SWT.SMOOTH);
      horizontalSplitPane.setOrientation(SWT.HORIZONTAL);

      outputText = new Text(verticalSplitPane, SWT.BORDER | SWT.READ_ONLY
            | SWT.V_SCROLL);
      outputText.setBackground(new Color(shell.getDisplay(), 0xff, 0xff, 0xff));
      outputText.setText("<No Output>");

      // Relative heights of the two sash's parts (in percent)
      verticalSplitPane.setWeights(new int[] { 75, 25 });

      // Set up a popup menu for the output window
      Menu popupMenu = new Menu(outputText);
      outputText.setMenu(popupMenu);
      MenuItem mi = new MenuItem(popupMenu, SWT.POP_UP);
      mi.setText("Open in External Window");
      mi.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleOutputTextPopup();
         }
      });

      showOutputView(false);

      Display display = Display.getCurrent();

      // Set up colors for the tabs
      int colorCount = 3;
      Color[] colors = new Color[colorCount];
      colors[0] = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
      colors[1] = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
      colors[2] = colors[0];
      int[] percents = new int[] { 10, 50 };

      // Set up folder containing tree outlines for
      // Construnctions/Schemas/Ontologies
      leftTabFolder = new CTabFolder(horizontalSplitPane, SWT.BORDER);
      leftTabFolder.setSelection(0);
      leftTabFolder.setMRUVisible(true);
      leftTabFolder.setTabHeight(20);
      leftTabFolder.setSimple(false);
      leftTabFolder.setSelectionBackground(colors, percents, true);

      // Set up right folder
      rightTabFolder = new CTabFolder(horizontalSplitPane, SWT.BORDER);
      rightTabFolder.setSelection(0);
      rightTabFolder.setMRUVisible(true);
      rightTabFolder.setTabHeight(20);
      rightTabFolder.setSimple(false);
      rightTabFolder.setSelectionBackground(colors, percents, true);

      // Set up popup menus for all the right tabs
      Menu menu = new Menu(getShell(), SWT.POP_UP);
      MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
      menuItem.setText("Close");
      menuItem.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleTabClose();
         }
      });
      menuItem = new MenuItem(menu, SWT.CASCADE);
      menuItem.setText("Close Others");
      menuItem.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleTabCloseOthers();
         }
      });

      menuItem = new MenuItem(menu, SWT.CASCADE);
      menuItem.setText("Close All");
      menuItem.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleTabCloseAll();
         }
      });

      rightTabFolder.setMenu(menu);

      // set panel widths at 30%, 70%
      horizontalSplitPane.setWeights(new int[] { 30, 70 });

      constructionTabItem = new CTabItem(leftTabFolder, SWT.NONE);
      constructionTabItem.setText("Constructions");

      schemaTabItem = new CTabItem(leftTabFolder, SWT.NONE);
      schemaTabItem.setText("Schemas");

      ontologyTabItem = new CTabItem(leftTabFolder, SWT.NONE);
      ontologyTabItem.setText("Ontology");

      createViews();

      return contents;
   }

   /**
    * Fill in the contents of the three left tabs
    */
   protected void createViews() {

      // Construction view
      //
      constructionTreeViewer = new TreeViewer(leftTabFolder, SWT.VIRTUAL
            | SWT.BORDER);
      constructionTreeViewer.setUseHashlookup(true);
      constructionTreeViewer.setLabelProvider(new TypeSystemLabelProvider());

      Tree constructionTree = constructionTreeViewer.getTree();
      constructionTabItem.setControl(constructionTree);

      // We want to catch mouse clicks on the tree's elements
      constructionTree.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleActivationChange((TypeSystemNode) e.item.getData());
         }
      });

      // Schema view
      //
      schemaTreeViewer = new TreeViewer(leftTabFolder, SWT.VIRTUAL | SWT.BORDER);
      schemaTreeViewer.setUseHashlookup(true);
      schemaTreeViewer.setLabelProvider(new TypeSystemLabelProvider());

      Tree schemaTree = schemaTreeViewer.getTree();
      schemaTabItem.setControl(schemaTree);
      schemaTree.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleActivationChange((TypeSystemNode) e.item.getData());
         }
      });

      // Ontology view
      //
      ontologyTreeViewer = new TreeViewer(leftTabFolder, SWT.VIRTUAL | SWT.BORDER);
      ontologyTreeViewer.setUseHashlookup(true);
      ontologyTreeViewer.setLabelProvider(new TypeSystemLabelProvider());

      Tree ontologyTree = ontologyTreeViewer.getTree();
      ontologyTabItem.setControl(ontologyTree);
      ontologyTree.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            handleActivationChange((TypeSystemNode) e.item.getData());
         }
      });
   }

   @Override
   protected MenuManager createMenuManager() {
      GrammarBrowserAction action = new GrammarBrowserAction(this);

      MenuManager menuBar = new MenuManager("");

      MenuManager fileMenu = new MenuManager("&File");
      menuBar.add(fileMenu);
      fileMenu.add(action.new FileOpen());
      fileMenu.add(action.new FileClose());
      fileMenu.add(action.new FileExit());

      MenuManager viewMenu = new MenuManager("&View");
      menuBar.add(viewMenu);
      viewMenu.add(action.new ViewSelectFont());
      viewMenu.add(action.new ViewIncreaseFontSize());
      viewMenu.add(action.new ViewDecreaseFontSize());
      viewMenu.add(new Separator());
      viewMenu.add(action.new ViewShowHideOutput());

      MenuManager windowMenu = new MenuManager("&Window");
      menuBar.add(windowMenu);
      //windowMenu.add(action.new WindowShowWindowList());
      windowMenu.add(action.new WindowCloseAll());

      MenuManager analyzeMenu = new MenuManager("&Analyze");
      menuBar.add(analyzeMenu);
      analyzeMenu.add(action.new AnalyzeSentence());

      return menuBar;
   }

   /**
    * Resets the content of the two tabs on the left, and disposes of any viewers
    * in the right pane.
    */
   public void handleFileClose() {

      // Copy the values of the views to destroy.
      // This is necessary because all views unregister themselves
      // thus modifying idToView
      //
      destroyViews(new LinkedList<Widget>(idToView.values()));
      assert idToView.size() == 0;

      model.setGrammar(null);
      notifyModelChanged(model);
      resetViews();
      showOutputView(false);
   }

   /**
    * Empty the right and left parts
    */
   protected void resetViews() {

      if (model.getGrammar() == null) {
         // The TreeView.setIput method can only be called when a content provider
         // has been set
         if (constructionTreeViewer.getContentProvider() != null)
            constructionTreeViewer.setInput(null);
         if (schemaTreeViewer.getContentProvider() != null)
            schemaTreeViewer.setInput(null);
         if (ontologyTreeViewer.getContentProvider() != null)
            ontologyTreeViewer.setInput(null);
      } else {
         // Construction tab
         constructionTreeViewer
               .setContentProvider(new TypeSystemContentProvider<Construction>(
                     constructionTreeViewer, model.getGrammar().getCxnTypeSystem()));
         constructionTreeViewer.setInput(TreePath.EMPTY);

         // Schema tab
         schemaTreeViewer
               .setContentProvider(new TypeSystemContentProvider<Schema>(
                     schemaTreeViewer, model.getGrammar().getSchemaTypeSystem()));
         schemaTreeViewer.setInput(TreePath.EMPTY);

         // Ontology tab
         ontologyTreeViewer
               .setContentProvider(new TypeSystemContentProvider<Type>(
                     ontologyTreeViewer, model.getGrammar().getOntologyTypeSystem()));
         ontologyTreeViewer.setInput(TreePath.EMPTY);
      }
   }

   /** 
    * Handles File | Exit menu 
    */
   public void handleFileExit() {
      handleFileClose();
      shell.close();
   }

   /**
    * Open grammar/ontology
    */
   public void handleFileOpen() {
      handleFileClose();

      FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
      fileDialog.setFilterNames(new String[] 
       { "ECG Preference File", "All Files", });
      fileDialog.setFilterExtensions(new String[] {"*.prefs", "*.*" });

      String path = fileDialog.open();
      if (path != null) {

         //GrammarPrefs gp = new GrammarPrefs(path);
         
         try{
         // Read grammar...

         Grammar g = ECGGrammarUtilities.read(path);
         //setExampleSentences(gp.exampleSentences);
	 setExampleSentences(g.getPrefs().getList(AP.EXAMPLE_SENTENCES));
         setGrammar(g);
         
         // ... and update the UI.
         notifyModelChanged(model);
         } catch(GrammarException e){
             outputText.setText("");
             outputText.setText(e.getMessage());
             showOutputView(true);
         } catch(Exception e){
            outputText.setText("");
            outputText.setText(e.getMessage());
            showOutputView(true);
        }
      }
   }

   protected TreePath[] getParents(String nodeType, String nodeName) {
      TypeSystemType type = TypeSystemType.fromString(nodeType);
      switch (type) {
      case CONSTRUCTION:
         return ((ILazyTreePathContentProvider) constructionTreeViewer
               .getContentProvider()).getParents(model.getGrammar().getConstruction(
               nodeName));
      case SCHEMA:
         return ((ILazyTreePathContentProvider) schemaTreeViewer
               .getContentProvider()).getParents(model.getGrammar().getSchema(
               nodeName));
      case ONTOLOGY:
         return ((ILazyTreePathContentProvider) ontologyTreeViewer
               .getContentProvider()).getParents(model.getGrammar()
               .getOntologyTypeSystem().get(nodeName));
      default:
         assert false : String.format("unexpected TypeSystenType: %s", type);
         return null;
      }
   }

//   protected String getStructure(String nodeName) {
//      return new Analysis(model.getGrammar().getConstruction(nodeName))
//            .getFeatureStructure().toString();
//
//   }

   protected TreeViewer getTreeViewer(String nodeType) {
      TypeSystemType type = TypeSystemType.fromString(nodeType);
      switch (type) {
      case CONSTRUCTION:
         return constructionTreeViewer;
      case SCHEMA:
         return schemaTreeViewer;
      case ONTOLOGY:
         return ontologyTreeViewer;
      default:
         assert false : String.format("unexpected TypeSystenType: %s", type);
         return null;
      }
   }

   protected String getContentAsText(TypeSystemNode node) {
      Grammar.setFormatter(textFormatter);
      String text = node.toString();
      Grammar.setFormatter(linkFormatter);

      return text;
   }

   /**
    * Handles the event of an activation of a node, that is, a particular 
    * construction or schema, in one of the two left tree panels.
    * @param node - The node just selected by the user
    */
   public void handleActivationChange(TypeSystemNode node) {
      Widget view = lookupView(node);
      if (view == null) {
         CTabItem c = new CTabItem(rightTabFolder, SWT.CLOSE);
         c.setText(String.format("%s", node.getType()));
         String contentAsText = getContentAsText(node);

         // Set the tooltip to the full text of the construction/schema.
         // Notice that the contents are truncated
         c.setToolTipText(contentAsText);  

         c.setControl(new TypeSystemHtmlPanel(node, c, this, rightTabFolder,
               SWT.NONE));
         rightTabFolder.setSelection(c);
      } else {
         rightTabFolder.setSelection((CTabItem) view);
      }
   }

   public void handleAnalyzeSentence() throws IOException {
      List<String> es = getExampleSentences();
      InputDialog d = new InputDialog(getShell(), "Sentence Analysis",
            "Please enter a sentence to anlayze:",
            es.size() > 0 ? es.get(0) : "", null);

      if (d.open() == Window.OK) {
         inputSentence = d.getValue();
         parseSentence(d.getValue());
      }
   }

   protected Collection<Widget> getOutputViews() {
      Collection<Widget> views = new LinkedList<Widget>();
      for (Map.Entry<String, Widget> e : idToView.entrySet()) {
         if (e.getKey().contains(OUTPUT_SHELL_ID))
            views.add(e.getValue());
      }
      return views;
   }

   public void handleCloseAllWindows() {
      destroyViews(getOutputViews());
   }

   public void handleDecreaseFontSize() {
      FontData[] fdata = TypeSystemHtmlPanel.font.getFontData();
      for (FontData fd : fdata) {
         fd.setHeight(fd.getHeight() - 1);
      }
      TypeSystemHtmlPanel.font = new Font(shell.getDisplay(), fdata);
      for (CTabItem i : rightTabFolder.getItems()) {
         TypeSystemHtmlPanel panel = (TypeSystemHtmlPanel) i.getControl();
         panel.getFont().dispose();
         panel.setFont(TypeSystemHtmlPanel.font);
      }
   }

   public void handleIncreaseFontSize() {
      FontData[] fdata = TypeSystemHtmlPanel.font.getFontData();
      for (FontData fd : fdata) {
         fd.setHeight(fd.getHeight() + 1);
      }
      TypeSystemHtmlPanel.font = new Font(shell.getDisplay(), fdata);
      for (CTabItem i : rightTabFolder.getItems()) {
         TypeSystemHtmlPanel panel = (TypeSystemHtmlPanel) i.getControl();
         panel.getFont().dispose();
         panel.setFont(TypeSystemHtmlPanel.font);
      }
   }

   /**
    * Is the output text visible?
    * @return true if the bottom text in the lower right part is visible, false
    *         otherwise.
    */
   protected boolean isOutputVisible() {
      return verticalSplitPane.getMaximizedControl() == null;
   }

   /**
    * Shows/hides the bottom output view.
    * @param show - Whether to show or hide the output view.
    */
   protected void showOutputView(boolean show) {
      if (show)
         verticalSplitPane.setMaximizedControl(null);
      else
         verticalSplitPane.setMaximizedControl(horizontalSplitPane);
   }

   public void handleShowHideOutput() {
      showOutputView(!isOutputVisible());
   }

   // Unused
   public void handleShowWindowList() {
      Window windowList = new WindowList(getShell(), idToView.values().toArray());
      windowList.open();
      
//      PopupList list = new PopupList(shell);
//      List<String> items = new LinkedList<String>();
//      for (Map.Entry<String, Widget> e : idToView.entrySet()) {
//         if (e.getValue().getData(OUTPUT_SHELL_ID) != null)
//            items.add(e.getValue().getData(OUTPUT_SHELL_ID).toString());
//      }
//      String[] ia = new String[items.size()];
//      list.setItems(items.toArray(ia));
//      list.open(new Rectangle(120, 120, 100, 100));
   }

   /**
    * Create a new external output window.
    * @param text - The window title
    * @return - A new initialized Shell
    */
   protected Shell createView(String text) {
      Shell popupShell = new Shell(shell.getDisplay());
      popupShell.setData(OUTPUT_SHELL_ID, model.getSentences());
      popupShell.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
      popupShell.setText(String.format(
            "Grammar Browser - Analysis Output: \"%s\"", text));

      registerView(Util.toString(popupShell), popupShell);

      // Register a listener so we can catch a call to the Shell's dispose
      // method and unregister it
      //
      popupShell.addDisposeListener(new DisposeListener() {
         public void widgetDisposed(DisposeEvent e) {
            unregisterView(Util.toString(e.widget), e.widget);
         }
      });
      return popupShell;
   }

   protected void destroyViews(Collection<Widget> views) {
      for (Widget w : views)
         w.dispose();
   }

   /**
    * Handles the creation of a text window containing the current
    * output of the analyzer
    */
   public void handleOutputTextPopup() {
      Shell popupShell = createView(inputSentence);

      TextViewer textViewer = new SourceViewer(popupShell, new VerticalRuler(10),
            SWT.V_SCROLL | SWT.H_SCROLL);
      Document document = new Document(outputText.getText());
      textViewer.setDocument(document);
      textViewer.setEditable(false);

      popupShell.open();
   }

   public void handleSelectFont() {
      FontDialog fd = new FontDialog(getShell(), SWT.None);
      if (fd.open() == null)
         return;

      CTabItem selection = rightTabFolder.getSelection();
      if (selection != null) {
         TypeSystemHtmlPanel.font = new Font(shell.getDisplay(), fd.getFontList());
         for (CTabItem i : rightTabFolder.getItems()) {
            TypeSystemHtmlPanel panel = (TypeSystemHtmlPanel) i.getControl();
            panel.getFont().dispose();
            panel.setFont(TypeSystemHtmlPanel.font);
         }
      }
   }

   public void handleTabClose() {
      CTabItem selected = rightTabFolder.getSelection();
      if (selected != null)
         selected.dispose();
   }

   public void handleTabCloseOthers() {
      CTabItem selected = rightTabFolder.getSelection();
      if (selected != null)
         for (CTabItem c : rightTabFolder.getItems())
            if (c != selected) c.dispose();
   }

   public void handleTabCloseAll() {
      for (CTabItem c : rightTabFolder.getItems())
         c.dispose();
   }

   /**
    * Delegates this action to the eventManager
    * 
    * @param listener - The listener being registered
    * @see compling.gui.grammargui.ModelChangedEventManager#addModelChangeListener(compling.gui.grammargui.IModelChangedListener)
    */
   public void addModelChangeListener(IModelChangedListener listener) {
      eventManager.addModelChangeListener(listener);
   }

   /**
    * Delegates this action to the eventManager
    * 
    * @param listener - The listener being unregistered
    * @see compling.gui.grammargui.ModelChangedEventManager#removeModelChangeListener(compling.gui.grammargui.IModelChangedListener)
    */
   public void removeModelChangeListener(IModelChangedListener listener) {
      eventManager.removeModelChangeListener(listener);
   }

   /**
    * Delegates this action to the eventManager
    * 
    * @param model
    * @see compling.gui.grammargui.ModelChangedEventManager#notifyModelChanged(java.lang.Object)
    */
   public void notifyModelChanged(Object model) {
      eventManager.notifyModelChanged(model);
   }

   protected Widget lookupView(TypeSystemNode node) {
      return idToView.get(Util.toString(node));
   }

   /**
    * Notify the tree view that a node has been clicked on in one of the right
    * tabs.
    * 
    * @see compling.gui.grammargui.IGrammarBrowserController#notifyActivationChanged(java.lang.String,
    *      java.lang.String)
    */
   public void notifyActivationChanged(String nodeType, String nodeName) {
      TreePath[] parents = getParents(nodeType, nodeName);

      assert parents.length > 0;

      getTreeViewer(nodeType).setSelection(new TreeSelection(parents[0]), true);

      TypeSystemType type = TypeSystemType.fromString(nodeType);
      switch (type) {
      case CONSTRUCTION:
         leftTabFolder.setSelection(constructionTabItem);
         handleActivationChange(model.getGrammar().getCxnTypeSystem()
               .get(nodeName));
         // System.out.printf("Structure: %s\n", getStructure(nodeName));
         break;
      case SCHEMA:
         leftTabFolder.setSelection(schemaTabItem);
         handleActivationChange(model.getGrammar().getSchemaTypeSystem().get(
               nodeName));
         break;
      case ONTOLOGY:
         leftTabFolder.setSelection(ontologyTabItem);
         handleActivationChange(model.getGrammar().getOntologyTypeSystem().get(
               nodeName));
         break;
      default:
         assert false : "unexpected TypeSystenType: " + type;
      }
   }

   /*
    * @see compling.gui.grammargui.GrammarBrowserModel#getAnalyzer()
    */
   protected ECGAnalyzer getAnalyzer() throws IOException {
      return model.getAnalyzer();
   }

   /**
    * Parses a sentence
    * @param sentence - The sentence to be parsed
    * @throws IOException 
    */
   protected void parseSentence(String sentence) throws IOException {

      // Clear output window first
      outputText.setText("");

      ArrayList<String> words = Util.split(sentence);
      // output.append("\nAnalyzing sentence: " + source.getText() + "\n");

      ECGAnalyzer analyzer = getAnalyzer();

      try {
         if (analyzer.robust()) {
            PriorityQueue<List<Analysis>> parses = analyzer
                  .getBestPartialParses(new Sentence(words, null, 0));

            while (parses.size() > 0) {
               outputText
                     .append("RETURNED ANALYSIS\n____________________________\n");
               outputText.append(String.format("Cost: %s\n", parses.getPriority()));

               for (Analysis a : parses.next()) {
                  outputText.append(a.toString());
               }
            }
         } else {
            PriorityQueue<Analysis> pqa = analyzer.getBestParses(new Sentence(
                  words, null, 0));
            while (pqa.size() > 0) {
               outputText
                     .append("\n\nRETURNED ANALYSIS\n____________________________\n");
               outputText.append(String.format("Cost: %s\n", pqa.getPriority()));
               outputText.append(pqa.next().toString());
            }
         }
         if (analyzer.debug()) {
            outputText.append(analyzer.getParserLog());
         }
      } catch (ParserException e) {
         outputText.setText("Parser Exception: " + e.toString());
      } catch (NoECGAnalysisFoundException e) {
         outputText.setText("Parser Exception: " + e.toString());
      } catch (GrammarException e) {
         outputText.setText("Grammar Exception: " + e.toString());
      }
      verticalSplitPane.setMaximizedControl(null);

      setStatus(String.format("Output: \"%s\"", sentence));
   }

   /**
    * Registers a view with this class
    * @param id - The id to associate with the view
    * @param view - The widget implementing the view
    * @see IGrammarBrowserController#registerView(String, Widget)
    */
   public void registerView(String id, Widget view) {
      assert !idToView.containsKey(id);

      idToView.put(id, view);
   }

   /**
    * Set the grammar for this browser
    * @param grammar - The grammar to link to this browser
    */
   public void setGrammar(Grammar grammar) {
      if (grammar == null)
         return;

      model.setGrammar(grammar);
      Grammar.setFormatter(linkFormatter);

      // Reset all content
      resetViews();
   }

   /**
    * @return the sentence text
    */
   public List<String> getExampleSentences() {
      return model.getSentences();
   }

   /**
    * @param text - the sentence text to set
    */
   public void setExampleSentences(List<String> sentences) {
      if (sentences != null) {	
	      model.setSentences(sentences);
	}
   }

   /**
    * Unregisters a view with this class previously registered with 
    * registerView(String, Widget).
    * @param id - The id to associate with the view
    * @param view - The widget implementing the view
    * @see compling.gui.grammargui.IGrammarBrowserController#unregisterView(String,
    *      Widget)
    */
   public void unregisterView(String id, Widget view) {
      assert idToView.containsKey(id) && idToView.get(id).equals(view);

      idToView.remove(id);
   }

   /**
    * Create the GUI and show it.
    * 
    * For thread safety, this method should be invoked from the event-dispatching
    * thread.
    */
   public static void createAndShowGUI(Grammar grammar) {
//      Display display = Display.getCurrent();

      // Create and set up the main window.
      //
      GrammarBrowser grammarBrowser = new GrammarBrowser(new GrammarBrowserModel(
            grammar), null);
      grammarBrowser.setBlockOnOpen(true);
      grammarBrowser.open();

      Display.getCurrent().dispose();
   }

   public static void createAndShowGUI() {
      createAndShowGUI(null);
   }

   public static void main(String[] args) throws IOException {
      if (args.length > 1)
         usage();

      if (args.length == 0)
         createAndShowGUI();
      else
         createAndShowGUI(ECGGrammarUtilities.read(args[0]));
   }

   public static void usage() {
      System.out.printf("Usage: %s [<.params file>]\n", GrammarBrowser.class
            .getName());
   }
}

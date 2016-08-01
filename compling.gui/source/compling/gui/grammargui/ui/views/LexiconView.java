package compling.gui.grammargui.ui.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.grammargui.Application;
import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.grammargui.ui.editors.ConstructionEditor;
import compling.gui.grammargui.ui.editors.MultiPageConstructionEditor;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.ontology.OWLTypeSystemNode;
import compling.parser.ecgparser.ECGTokenReader;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.UtteranceGenerator;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;


public class LexiconView extends ViewPart {

	
	public static final String ID = "compling.gui.grammargui.views.lexicon";
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
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
			if (part == LexiconView.this) {
				this.selection = (IStructuredSelection) selection;
				setEnabled(this.selection.size() == 1 && !(this.selection.getFirstElement() instanceof OWLTypeSystemNode));
			}
		}

	}
	
	
	private TreeViewer treeViewer;
	
	private ArrayList<String> partsOfSpeech;
	private HashMap<String, ArrayList<String>> posToTypes;
	private Map<String, ArrayList<ECGToken>> tokens;
	
	private HashMap<String, List<Constraint>> tokensToConstraints;
	
	private HashMap<String, ArrayList<String>> typesToTokens;
	
	private LCPGrammarWrapper wrapper;
	
	private Tree tree;
	Button reloadButton;
	
	private TypeSystem ts;
	
	
	private void initializePartsOfSpeech() {
		partsOfSpeech = new ArrayList<String>(){{
			add("Adjective");
			add("Noun");
			add("Verb");
			add("Preposition");
			add("Determiner");
			add("Adverb");
			add("NP");
		}};
	}
	
	private void initializeTokens() throws IOException {
		//ECGTokenReader reader = new ECGTokenReader(getGrammar());
		tokens = getGrammar().getTokenReader().getTokens();
	}
	
	private void typesToTokens() {
		tokensToConstraints = new HashMap<String, List<Constraint>>();
		typesToTokens = new HashMap<String, ArrayList<String>>();
		for (ArrayList<ECGToken> tokenList : tokens.values()) {
			for (ECGToken token : tokenList) {
				
				tokensToConstraints.put(token.token_name, token.constraints);
				String typeName = token.parent.getName();
				if (!typesToTokens.containsKey(typeName)) {
					typesToTokens.put(typeName, new ArrayList<String>());
				}
				typesToTokens.get(typeName).add(token.token_name);
				Collections.sort(typesToTokens.get(typeName));
			}
		}
	}
	
	private void initializeData() {
		initializePartsOfSpeech();
		try {
			initializeTokens();
		} catch (IOException e) {
			// TODO: Do something here (error on reading token file)
			e.printStackTrace();
		}
		typesToTokens();
		initializePosToTypes();
	}
	
	private void initializePosToTypes() {
		posToTypes = new HashMap<String, ArrayList<String>>();
		posToTypes.put("Other", new ArrayList<String>());
		ts = getGrammar().getCxnTypeSystem();
		//HashMap<String, ArrayList<String>> typesToLexemes = new HashMap<String, ArrayList<String>>();
		for (String type : typesToTokens.keySet()) {
			boolean placed = false;
			for (String pos : partsOfSpeech) {
				try {
					if (ts.subtype(ts.getInternedString(type), ts.getInternedString(pos))) {
						if (!posToTypes.containsKey(pos)) {
							posToTypes.put(pos, new ArrayList<String>());
						}
						posToTypes.get(pos).add(type);
						placed = true;
						Collections.sort(posToTypes.get(pos));
					}
				} catch (TypeSystemException e) {
					// TODO DO something, error
					e.printStackTrace();
				}
			} if (!placed) {
				posToTypes.get("Other").add(type);
				placed = true;
				Collections.sort(posToTypes.get("Other"));
			}
		}
		for (Construction c : wrapper.getAllConcreteLexicalConstructions()) {
			String type = c.getName();
				try {
					if (!ts.subtype(ts.getInternedString(type), ts.getInternedString("GeneralTypeCxn"))) {
						boolean placed = false;
						for (String pos : partsOfSpeech) {
							try {
								if (ts.subtype(ts.getInternedString(type), ts.getInternedString(pos))) {
									if (!posToTypes.containsKey(pos)) {
										posToTypes.put(pos, new ArrayList<String>());
									}
									posToTypes.get(pos).add(type);
									placed = true;
									Collections.sort(posToTypes.get(pos));
								}
							} catch (TypeSystemException e) {
								// TODO DO something, error
								e.printStackTrace();
							}
						} if (!placed) {
							posToTypes.get("Other").add(type);
							placed = true;
							Collections.sort(posToTypes.get("Other"));
						}
					}
				} catch (TypeSystemException e) {
					// TODO Bad thing
					e.printStackTrace();
				}
		}
	}
	
	private void createTree() { //(Composite parent) {
//	    tree = new Tree(parent, SWT.BORDER | SWT.V_SCROLL
//		        | SWT.H_SCROLL);
		    //tree.setSize(290, 260);
		tree.removeAll();
		    
		for (Entry<String, ArrayList<String>> entry : posToTypes.entrySet()) {
	    	TreeItem pos = new TreeItem(tree, 0);
	    	pos.setText(entry.getKey());
	    	TreeItem lexicalCxns = new TreeItem(pos, 0);
	    	lexicalCxns.setText("Lexical-Cxns");
	    	for (String typeName : entry.getValue()) {
	    		if (typesToTokens.containsKey(typeName)) {
		    		TreeItem type = new TreeItem(pos, 0);
		    		type.setText(typeName);
	    			ArrayList<String> tokns = typesToTokens.get(typeName);
	    			for (String tok : tokns) {
	    				TreeItem token = new TreeItem(type, 0);
    					token.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.TOKEN).createImage());

	    				token.setText(tok);
	    				List<Constraint> constraints = tokensToConstraints.get(tok);
	    				for (Constraint c : constraints) {
	    					TreeItem constraint = new TreeItem(token, 0);
	    					String text = c.getArguments().get(0).toString() + " <-- " + c.getValue();
	    					constraint.setText(text);
	    				}
	    			}
	    		} else {
	    			TreeItem lex = new TreeItem(lexicalCxns, 0);
	    			lex.setText(typeName);
	    			lex.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.CONSTRUCTION).createImage());
	    			for (Constraint c : getGrammar().getConstruction(typeName).getMeaningBlock().getConstraints()) {
    					TreeItem constraint = new TreeItem(lex, 0);
    					//constraint.
    					String text = c.getArguments().get(0).toString() + " " + c.getOperator() + " " + c.getValue();
    					constraint.setText(text);
	    			}
	    		}
	    	}
	    }
		tree.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				TreeItem t = tree.getSelection()[0];
				if (tokens.keySet().contains(t.getText())) {
					String parent = t.getParentItem().getText();
					ArrayList<ECGToken> values = tokens.get(t.getText());
					for (ECGToken token : values) {
						if (token.parent.getName().equals(parent)) {
							try {
								openEditorForToken(token);
							} catch (PartInitException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				} else if (wrapper.getAllConcreteLexicalConstructions().contains(getGrammar().getConstruction(t.getText()))) {
					Construction l = getGrammar().getConstruction(t.getText());
					try {
						openEditorFor((TypeSystemNode) l);
					} catch (PartInitException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					//l.
				}
			}
		});
		
	}
	
	protected void openEditorForToken(ECGToken t) throws PartInitException {
		final IEditorInput input = new FileEditorInput(PrefsManager.getDefault().getFileFor(t));
		final IWorkbenchPage page = getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null && activeEditor.equals(page.findEditor(input)))
			return;

		IDE.openEditor(page, input, ConstructionEditor.ID, false);
	}

	protected void openEditorFor(TypeSystemNode node) throws PartInitException {
		if (! getSite().getPage().isEditorAreaVisible())
			return;

		// final IEditorInput input = new TypeSystemEditorInput(node);
		final IEditorInput input = new FileEditorInput(PrefsManager.getDefault().getFileFor(node));
		final IWorkbenchPage page = getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null && activeEditor.equals(page.findEditor(input)))
			return;

		IDE.openEditor(page, input, ConstructionEditor.ID, false);
	}

	

	@Override
	public void createPartControl(Composite parent) {
		wrapper = new LCPGrammarWrapper(getGrammar());		
		
		initializeData();
		
		//UtteranceGenerator generator = new UtteranceGenerator(wrapper);
		//generator.generateUtterances("VP");
		

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;

		form.getBody().setLayout(layout);
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		
		Button reloadButton = toolkit.createButton(form.getBody(), "Reload Lexicon", SWT.PUSH);
		reloadButton.setToolTipText("Press button to check grammar and reload lexicon, so new words appear in tree.");
		reloadButton.setLayoutData(gd);
		reloadButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				PrefsManager.getDefault().checkGrammar();
				initializeData();
				createTree();
			}
		});
		
		tree = toolkit.createTree(form.getBody(), SWT.BORDER | SWT.V_SCROLL
		        | SWT.H_SCROLL);
		GridData gdTree = new GridData(SWT.FILL, 1, true, true);
		gdTree.heightHint = 550;
		tree.setLayoutData(gdTree);
		createTree();
		


	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}


}

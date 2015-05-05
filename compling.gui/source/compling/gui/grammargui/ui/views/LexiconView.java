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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.grammargui.model.PrefsManager;
import compling.parser.ecgparser.ECGTokenReader;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;


public class LexiconView extends ViewPart {
	
	public static final String ID = "compling.gui.grammargui.views.lexicon";
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
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
		ECGTokenReader reader = new ECGTokenReader(wrapper);
		tokens = reader.getTokens();
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
	    		}
	    	}
	    }
		
	}

	

	@Override
	public void createPartControl(Composite parent) {
		wrapper = new LCPGrammarWrapper(getGrammar());
		initializeData();

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		GridLayout layout = new GridLayout();

		form.getBody().setLayout(layout);
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		
		Button reloadButton = toolkit.createButton(form.getBody(), "Reload Lexicon", SWT.PUSH);
		reloadButton.setToolTipText("Reload lexicon.");
		reloadButton.setLayoutData(gd);
		reloadButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				System.out.println("Reloading types");
				PrefsManager.getDefault().checkGrammar();
				initializeData();
				createTree();
			}
		});
		
		
		tree = toolkit.createTree(form.getBody(), SWT.BORDER | SWT.V_SCROLL
		        | SWT.H_SCROLL);
		GridData gdTree = new GridData(SWT.FILL, 1, true, true, 1, 2);
		gdTree.heightHint = 550;
		tree.setLayoutData(gdTree);
		createTree();
		


	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}


}

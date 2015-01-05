package compling.gui.grammargui.ui.views;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.grammargui.model.PrefsManager;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.util.fileutil.TextFileLineIterator;

public class TokenView extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.tokenView";
	private String token;
	private String parentCxn;
	private ArrayList<String> constraints;
	private File token_file;
	private File ontFile;
	private FileWriter fw;
	private BufferedWriter bw;
	private FileWriter ontWriter;
	private BufferedWriter bOntWriter;
	private AnalyzerPrefs prefs;
	private File base;
	private String[] typeCxns;
	
	private String parentValue;

	/** Opens up token file from TOKEN_PATH. */
	private void openFile() {
		try {
			token_file = new File(base, prefs.getSetting(AP.TOKEN_PATH));
			if (!token_file.exists()) {
				token_file.createNewFile();
			}
			fw = new FileWriter(token_file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/** Writes new token to Token File. */
	private void write(String tok, String par, ArrayList<String> cons) {
		try {
			openFile();
			String constraintString = "";
			for (int i = 0; i < cons.size(); i++) {
				constraintString += cons.get(i);
				if (i < cons.size() - 1) {
					constraintString += " :: ";
				}
			}
			bw.write(tok + " :: " + par + " :: " + constraintString);
			bw.newLine();
			bw.close();
			token = null;
			parentCxn = null;
			constraints.clear();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	
	/** Checks if VALUE exists in ontology. Uses method from Grammar object. */
	private boolean exists(String value) {
		return getGrammar().getOntologyTypeSystem().get(value.substring(1, value.length()).trim()) != null;
	}
	
	/** Returns a list of constructions using the grammar object's method for "getting constructions" based on orthography. */
	private String[] getTypes(String orth) {
		LCPGrammarWrapper lcp = new LCPGrammarWrapper(getGrammar());
		List<Construction> cxns = lcp.getLexicalConstruction(orth);
		String[] typeNames= new String[cxns.size()];
		for (int i=0; i < typeNames.length; i++) {
			typeNames[i] = cxns.get(i).getName();
		}
		return typeNames; 
	}
	
	
	// Adds VALUE to ontology as a subtype of PARENT. 
	private void addOntologyItem(String value, String parent) {
		//TypeSystem ts = getGrammar().getOntologyTypeSystem();
		value = value.substring(1, value.length()).trim();
		parent = parent.substring(1, parent.length()).trim();
		try {
			ontFile = new File(base, prefs.getSetting(AP.ONTOLOGY_PATHS));
			File tempFile = new File("tempFile.ont");
			ontWriter = new FileWriter(tempFile.getAbsoluteFile(), true);
			bOntWriter = new BufferedWriter(ontWriter);
			TextFileLineIterator tfli = new TextFileLineIterator(ontFile);
			while (tfli.hasNext()) {
				String line = tfli.next();
				if (line.contains("INSTS:")) {
					continue;
				} 
					
				bOntWriter.write(line + System.getProperty("line.separator") + System.getProperty("line.separator"));
			}
			String toWrite = "(type " + value + " sub " + parent + ")";
			tempFile.renameTo(ontFile);
			bOntWriter.write(toWrite);
			bOntWriter.newLine();
			bOntWriter.newLine();
			bOntWriter.write("INSTS:");
			bOntWriter.newLine();
			bOntWriter.close();
			PrefsManager.getDefault().checkGrammar();
		} catch (IOException problem) {
			System.out.println("Problem with ontology file.");
		}
	}
	
	/** Checks if CHILD is a subtype of PARENT. */
	private boolean isSubtype(String child, String parent) {
		try {
			TypeSystem ts = getGrammar().getOntologyTypeSystem();
			child = child.substring(1, child.length()).trim();
			String ancestor = parent.substring(1, parent.length()).trim();
			return ts.subtype(ts.getInternedString(child), ts.getInternedString(ancestor));
		} catch(TypeSystemException type) {
			System.out.println("Either " + child + " or " + parent + " doesn't exist.");
			return false;
		}
	}
	
	/** Returns type system. */
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getOntologyTypeSystem() : null;
	}
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}
	
	/** Returns Analyzer. */
	protected ECGAnalyzer getAnalyzer() {
		return PrefsManager.getDefault().getAnalyzer();
	}
	

	
	
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	
	public void createPartControl(Composite parent) {
		
		constraints = new ArrayList<String>();
		prefs =  (AnalyzerPrefs) getGrammar().getPrefs();
		base = prefs.getBaseDirectory();
		typeCxns = getTypes("\"*\"");
		


		
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		form.setText("Token Editor");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		layout.numColumns = 2;
		GridData gd = new GridData();
		gd.horizontalSpan = 4;

		final Label tokenLabel = toolkit.createLabel(form.getBody(), "Enter Token:");
		final Text tokenText = toolkit.createText(form.getBody(), "");
		tokenText.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));

		

		
		final Combo parentText = new Combo(parent, SWT.DROP_DOWN);
		parentText.setItems(typeCxns);
		toolkit.adapt(parentText);
		parentText.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		
		
		
		final Combo constraintBox = new Combo(parent, SWT.DROP_DOWN);
		constraintBox.setItems(new String[0]);
		toolkit.adapt(constraintBox);
		constraintBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		
		
		final Text constraintText = toolkit.createText(form.getBody(), "");
		constraintText.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));

		
		final HashMap<String, String> slotsValues = new HashMap<String, String>();
		final ArrayList<String> slots = new ArrayList<String>();
		
		final ArrayList<String> parents = new ArrayList<String>();
		

		constraintBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				slots.clear();
				slotsValues.clear();
				parents.clear();
				constraints.clear();
				parentCxn = parentText.getText();
				try {
					for (Constraint c : getGrammar().getConstruction(parentCxn).getMeaningBlock().getConstraints()) {
						slots.add(c.getArguments().get(0).toString());
						slotsValues.put(c.getArguments().get(0).toString(), c.getValue());
					}
					String[] slotArray = new String[slots.size()];
					for (int i=0; i < slotArray.length; i++) {
						slotArray[i] = slots.get(i);
					}
					constraintBox.setItems(slotArray);
				} catch(NullPointerException problem) {
					throw new GrammarException("That doesn't exist.");
				}
			}
		});
		
		constraintBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				constraintText.setText(slotsValues.get(constraintBox.getText()));
				parentValue = slotsValues.get(constraintBox.getText());
			}
		});
		

		Button addConstraintButton = toolkit.createButton(form.getBody(), "Add Constraint.", SWT.PUSH);
		addConstraintButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				String value = constraintText.getText();
				if (!exists(value)) {
					if (parentValue != null) {
						addOntologyItem(value, parentValue);
						constraints.add(constraintBox.getText() + " <-- " + value);
					} else {
						System.out.println("No parent assigned.");
					}
				} else {
					if (!isSubtype(value, parentValue)) {
						System.out.println("Not a proper subtype.");
						constraints.clear();
					} else {
						constraints.add(constraintBox.getText() + " <-- " + value);
					}
				}
			}
		});
		addConstraintButton.setLayoutData(gd);
		
		Button addTokenButton = toolkit.createButton(form.getBody(), "Add token.", SWT.PUSH);
		addTokenButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				token = tokenText.getText();
				parentCxn = parentText.getText();
				boolean write = true;
				write(token, parentCxn, constraints);
				
			}
		});
		addTokenButton.setLayoutData(gd);
		
		Button loadTokens = toolkit.createButton(form.getBody(), "Reload tokens.", SWT.PUSH);
		loadTokens.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				getAnalyzer().reloadTokens();
			}
		});
		loadTokens.setLayoutData(gd);
		


		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
	}
}

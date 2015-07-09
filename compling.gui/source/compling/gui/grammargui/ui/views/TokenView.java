package compling.gui.grammargui.ui.views;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.GrammarError;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.grammargui.Application;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.util.Utils;
import compling.parser.ParserException;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.util.fileutil.FileUtils;
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
	
	private String modifiedOnt = null;
	
	
	private IAction addToken;
	private IAction addConstraint;
	
	private String parentValue;

	/** Opens up token file from TOKEN_PATH. */
	private void openFile() {
		try {
			List<String> token_paths = prefs.getList(AP.TOKEN_PATH);
			token_file = new File(base, token_paths.get(0));  // writes to the first token path listed
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
			token = "";
			parentCxn = "";
			constraints.clear();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	
	/** Checks if VALUE exists in ontology. Uses method from Grammar object. Assumes value has "@ in it". */
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
		Arrays.sort(typeNames);
		return typeNames; 
	}
	
	
	private String[] getOntologyFiles() {
		List<String> ontPaths = prefs.getList(AP.ONTOLOGY_PATHS);
		String[] onts = new String[ontPaths.size()];
		for (int i=0; i < onts.length; i++) {
			onts[i] = ontPaths.get(i);
		}
		return onts;
	}
	
	
	// Adds VALUE to ontology as a subtype of PARENT. 
	private void addOntologyItem(String value, String parent, String ontologyFile) {
		value = value.substring(1, value.length()).trim();
		parent = parent.substring(1, parent.length()).trim();
		try {
			if (ontologyFile == null) {
				List<String> ontPaths = prefs.getList(AP.ONTOLOGY_PATHS);
				ontologyFile = ontPaths.get(0);
			}
			ontFile = new File(base, ontologyFile);
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
			bOntWriter.write(toWrite);
			bOntWriter.newLine();
			bOntWriter.newLine();
			bOntWriter.write("INSTS:");
			bOntWriter.newLine();
			bOntWriter.close();
			//File pointer = new File(ontFile.getAbsolutePath());
			String path = ontFile.getAbsolutePath();
			ontFile.delete();
			// Need to rename temp-file here.
			if (tempFile.renameTo(ontFile)) {
				System.out.println("Success");
			} else{
				throw new IOException();
			}
			System.out.println(tempFile);
			PrefsManager.getDefault().checkGrammar();
		} catch (IOException problem) {
			broadcastError("Something went wrong with modifying the ontology file " + ontologyFile + " with type " + value 
					+ "and parent " + parent + ".");
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
			//System.out.println("Either " + child + " or " + parent + " doesn't exist.");
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
	

	public void setTypes() {
		String t = parentText.getText();
		parentText.setItems(getTypes("\"*\""));
		parentText.setText(t);
	}
	
	static Combo parentText; // = new Combo(form.getBody(), SWT.DROP_DOWN);
	
	/** This is a method to map a language ontology value to the app ontology value. This is written to the mapping file. */
	public void writeMappingFile(String language, String application) {
		String mapping_path = prefs.getSetting(AP.MAPPING_PATH);
		File mapping_file = new File(base, mapping_path);
		try {
			FileWriter mw = new FileWriter(mapping_file.getAbsoluteFile(), true);
			BufferedWriter bmw = new BufferedWriter(mw);
			bmw.write(language + " :: " + application);
			bmw.close();
		} catch(IOException e) {
			broadcastError("There was a problem opening the mapping file. Check to make sure there is actually a file at " 
					+ mapping_path + ".");
		}
	}
	
	public void broadcastError(String message) {
		String newMessage = "WARNING: \n \n" + message;
		final IStatus status = new Status(IStatus.ERROR, Application.PLUGIN_ID, newMessage);
		ErrorDialog.openError(null, "Token Editor Error", null, status);
		throw new GrammarException(message);
	}
	
	
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
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		gd.horizontalSpan = 1;
		//gd.horizontalSpan = 2;

		final Label tokenLabel = toolkit.createLabel(form.getBody(), "Enter Token:");
		final Text tokenText = toolkit.createText(form.getBody(), "");
		tokenText.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		
	

		final Label parentLabel = toolkit.createLabel(form.getBody(), "Select Parent Type:");
		//final Combo parentText = new Combo(form.getBody(), SWT.DROP_DOWN);
		parentText = new Combo(form.getBody(), SWT.DROP_DOWN);
		//parentText.setItems(typeCxns);
		setTypes();
		//toolkit.adapt(parentText);
		parentText.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		parentText.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(parent);
		
		final Label constraintSelect = toolkit.createLabel(form.getBody(), "Select Role:");
		final Combo constraintBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		constraintBox.setItems(new String[0]);
		constraintBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		constraintBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(parent);
		
		final Label ontologyFileLabel = toolkit.createLabel(form.getBody(), "Select Ontology File:");
		final Combo ontologyFileBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		ontologyFileBox.setItems(getOntologyFiles());
		ontologyFileBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		ontologyFileBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(ontologyFileBox);
		
		final Label constraintSet = toolkit.createLabel(form.getBody(), "Set Role Item:");
		final Text constraintText = toolkit.createText(form.getBody(), "");
		constraintText.setLayoutData(gd);
		
		final Label constraintParentsLabel = toolkit.createLabel(form.getBody(), "Constraint parents (optional):");
		final Text constraintParents = toolkit.createText(form.getBody(), "");
		constraintParents.setToolTipText("Enter a comma-separated list of ontology super-types for the new constraint. E.g., \" entity, artifact, moveable \"");
		constraintParents.setLayoutData(gd);
		
		final Label appMappingLabel = toolkit.createLabel(form.getBody(), "Application mapping (optional):");
		final Text appMappingText = toolkit.createText(form.getBody(), "$");
		appMappingText.setToolTipText("Enter the value you want this constrain value to be translated to in the application domain. E.g., red-adj might become just red.");	
		appMappingText.setLayoutData(gd);
		
		
		Button addConstraintButton = toolkit.createButton(form.getBody(), "", SWT.PUSH);
		addConstraintButton.setToolTipText("Add constraint to list of constraints.");
		addConstraintButton.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.ADD_SENTENCE_E).createImage());
		addConstraintButton.setLayoutData(new GridData(SWT.FILL, 1, false, false));
		

		
		
		final HashMap<String, String> slotsValues = new HashMap<String, String>();
		final ArrayList<String> slots = new ArrayList<String>();
		
		final ArrayList<String> parents = new ArrayList<String>();
		

		parentText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				constraintText.setText("");
				slots.clear();
				slotsValues.clear();
				parents.clear();
				constraints.clear();
				parentCxn = parentText.getText();
				try {
					for (Constraint c : getGrammar().getConstruction(parentCxn).getMeaningBlock().getConstraints()) {
						if (c.isAssign()) {
							slots.add(c.getArguments().get(0).toString());
							slotsValues.put(c.getArguments().get(0).toString(), c.getValue());
						}
						/*
						if (labelText.size() > i && c.getOperator().equals("<--")) {
							labelText.get(i).setText(c.getArguments().get(0).toString());
							textList.get(i).setText(c.getValue());
							parents.add(textList.get(i).getText());
						}
						*/
					}
					for (Constraint c : getGrammar().getConstruction(parentCxn).getConstructionalBlock().getConstraints()) {
						if (c.isAssign()) {
							slots.add(c.getArguments().get(0).toString());
							slotsValues.put(c.getArguments().get(0).toString(), c.getValue());
						}
					}
					String[] slotArray = new String[slots.size()];
					for (int i=0; i < slotArray.length; i++) {
						slotArray[i] = slots.get(i);
					}
					constraintBox.setItems(slotArray);
				} catch(NullPointerException problem) {
					broadcastError("Problem: '" + parentCxn + "' doesn't exist in the grammar. You must add a type construction before you make a token of it.");
				}
			}
		});
		
		constraintBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				constraintText.setText(slotsValues.get(constraintBox.getText()));
				parentValue = slotsValues.get(constraintBox.getText());
				appMappingText.setText("$");
			}
		});		

		
		ontologyFileBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modifiedOnt = ontologyFileBox.getText();
			}
		});

		addConstraintButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				String value = constraintText.getText();
				String appValue = appMappingText.getText();
				String inputParents = "";
				if (!constraintParents.getText().equals("")) {
					inputParents = constraintParents.getText().replace(",", "");
					String[] inputParents2 = constraintParents.getText().split(",");
					for (String parent : inputParents2) {
						if (!exists("@" + parent)) {
							//System.out.println(parent + " does not exist in the ontology lattice. You should add it.");
							broadcastError("Value '@" + parent + "' does not exist in the ontology. You must add it before you create a subtype of it.");
						}
					}
				}
				if (value.equals("")) {
					String message = "There are no constraint values to add; you need to fill in the role item slot.";
					broadcastError(message);

				} else if (value.charAt(0) == ECGConstants.ONTOLOGYPREFIX &&
							parentValue.charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
					if (!exists(value)) {
						if (parentValue != null) {
							parentValue += " " + inputParents;
							addOntologyItem(value, parentValue, modifiedOnt);
							constraints.add(constraintBox.getText() + " <-- " + value);
							if (appValue.length() > 1) {
								writeMappingFile(value, appValue);
							}
						} else {
							String message = "No parent assigned; you must choose a parent type for the token.";
							broadcastError(message);
						}
					} else {
						if (!isSubtype(value, parentValue)) {
							constraints.clear();
							constraintText.setText("");
							String message = "@" + value + "' already exists in Ontology, and is not a subtype of @" + parentValue 
									+ " . This will cause errors and prevent proper unification of the token's constraints.";
							broadcastError(message);
							//System.out.println(value + " already exists in Ontology, and is not a subtype of " + parentValue + " .");
						} else {
							constraints.add(constraintBox.getText() + " <-- " + value);
							if (appValue.length() > 1) {
								writeMappingFile(value, appValue);
							}
						}
					}
				} else {
					constraints.add(constraintBox.getText() + " <-- " + value);
				}
				appMappingText.setText("$");
				constraintText.setText("");
				constraintParents.setText("");
			}
		});
		addConstraintButton.setLayoutData(gd);
		
		Button addTokenButton = toolkit.createButton(form.getBody(), "Add token.", SWT.PUSH);
		addTokenButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				token = tokenText.getText();
				parentCxn = parentText.getText();
				if (!token.equals("") && !parentCxn.equals("") && constraints.size() > 0) {
					write(token, parentCxn, constraints);
					tokenText.setText("");
					parentText.setText("");
					constraintText.setText("");
					constraintBox.setText("");
					constraintParents.setText("");
					// Check grammar instead of retrieving analyzer. Analyzer object too large for "base", etc.
					// TODO: Check that this is proper.
					PrefsManager.getDefault().checkGrammar();
				} else {
					//System.out.println("Definition not complete.");
					broadcastError("This token definition is not complete; you must make sure you select a parentCxn (currently set to '" + parentCxn + "'), "
							+ "fill in the box for the token's value (currently: '" + token
							+ "'), and add at least one constraint (currently " + constraints.size() + " constraints added).");
				}

				
			}
		});
		addTokenButton.setLayoutData(gd);

		
		Button reloadTypesButton = toolkit.createButton(form.getBody(), "Reload Type Constructions.", SWT.PUSH);
		reloadTypesButton.setToolTipText("Reload the list of type constructions from a newly checked grammar..");
		reloadTypesButton.setLayoutData(new GridData(SWT.FILL, 1, false, false));
		reloadTypesButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				//System.out.println("Reloading types");
				setTypes();
			}
		});

		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
	}
}

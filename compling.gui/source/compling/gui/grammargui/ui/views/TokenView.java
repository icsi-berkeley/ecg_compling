package compling.gui.grammargui.ui.views;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/* Ethan */
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;


import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.part.ViewPart;

import compling.context.ContextModel;
import compling.context.MiniOntology;
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
import compling.gui.grammargui.builder.GrammarBuilder;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.gui.grammargui.util.ResourceGatherer;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.util.Utils;
import compling.parser.ParserException;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.ECGTokenReader;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.ECGMorph.MorphEntry;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;

public class TokenView extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.tokenView";
	private String token;
	private String parentCxn;
	private ArrayList<String> constraints;
	
	private String modifiedTok = null;
	
	private File token_file;
	private File ontFile;
	private FileWriter fw;
	private BufferedWriter bw;
	private FileWriter ontWriter;
	private BufferedWriter bOntWriter;
	private AnalyzerPrefs prefs;
	private File base;
	private String[] typeCxns;
	private static final Charset DEFAULT_CHARSET = Charset.forName(ECGConstants.DEFAULT_ENCODING);
	
	private String modifiedOnt = null;
	
	private String modifiedMorph = null;
	
	
	private IAction addToken;
	private IAction addConstraint;
	
	private String parentValue;
	
	/* Ethan */
	private HashMap<String, HashSet<String>> inflections;
	private TableViewer constraintsTable;
	private TableViewer inflectionTable;

	/** Opens up token file from TOKEN_PATH. */
	private void openFile() {
		try {
			if (modifiedTok == null) {
				List<String> token_paths = prefs.getList(AP.TOKEN_PATH);
				token_file = new File(base, token_paths.get(0));  // writes to the first token path listed
				if (!token_file.exists()) {
					token_file.createNewFile();
				}
			} else {
				token_file = new File(base, modifiedTok);
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
			
			/* Ethan */
			constraintsTable.getTable().removeAll();
			
			try {
				getGrammar().buildTokenAndMorpher();
			} catch (ParserException e) {
				broadcastError(e.getMessage());
			}
			Utils.flushCaches();
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
	
	private String[] getTokenFiles() {
		List<String> token_paths = prefs.getList(AP.TOKEN_PATH);
		String[] toks = new String[token_paths.size()];
		for (int i=0; i < toks.length; i++) {
			toks[i] = token_paths.get(i);
		}
		return toks;
	}
	
	private String[] getMorphFiles() {
		List<String> morph_paths = prefs.getList(AP.MORPHOLOGY_PATH);
		String[] toks = new String[morph_paths.size()];
		for (int i=0; i < toks.length; i++) {
			toks[i] = morph_paths.get(i);
		}
		return toks;
	}
	
	
	private void rebuildOntology() {
		ResourceGatherer gatherer = new ResourceGatherer(prefs);
		ContextModel contextModel;
		try {
			contextModel = buildContextModel(gatherer);
			getGrammar().setContextModel(contextModel);
			//getGrammar().update();
			TypeSystem ts = getGrammar().getOntologyTypeSystem();
			
		} catch (CoreException e) {
			e.printStackTrace();
			broadcastError("Problem rebuilding ontology...");
		}
	}
	

	
	public ContextModel buildContextModel(ResourceGatherer gatherer) throws CoreException {
		//SampleResourceVisitor visitor = new SampleResourceVisitor();
//		for (IResource r : gatherer.getOntologyFiles(getProject())) {
//			r.accept(visitor);
//		}
		boolean onts = true;
		String[] exts = gatherer.getOntologyExtensions().split(" ");
		List<File> files = gatherer.getOntologyFiles();
		if (files.size() == 1) {
			return new ContextModel(files.get(0).getAbsolutePath(), DEFAULT_CHARSET);
		} else if (onts) {
			return new ContextModel(files, exts[2], DEFAULT_CHARSET);
		}
		else {
			return new ContextModel(files, exts[0], exts[1], DEFAULT_CHARSET);
		}
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
			//System.out.println(tempFile);
			//PrefsManager.getDefault().checkGrammar();
			rebuildOntology();
		} catch (IOException problem) {
			broadcastError("Something went wrong with modifying the ontology file " + ontologyFile + " with type " + value 
					+ "and parent " + parent + ".");
		}
	}
	
	/* Ethan */
	/** Adds a pair of values to the the constraint table. */
	private void addTableEntry(Table table, String constraint, String value) {
		TableItem newItem = new TableItem(table, SWT.NONE);
		newItem.setText(0, constraint);
		newItem.setText(1, value);	
	}
	/* */
	
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
		inflections = new HashMap<String, HashSet<String>>();
		
		prefs =  (AnalyzerPrefs) getGrammar().getPrefs();
		base = prefs.getBaseDirectory();
		typeCxns = getTypes("\"*\"");
		
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
//		form.setExpandVertical(true);
//		form.setMinHeight(10);
		
		form.setText("Token Editor");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		layout.numColumns = 2;
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		gd.horizontalSpan = 1;
		//gd.horizontalSpan = 2;

		final Label tokenLabel = toolkit.createLabel(form.getBody(), "Enter Lemma:");
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
		
		final Label ontologyFileLabel = toolkit.createLabel(form.getBody(), "Select Ontology File:");
		final Combo ontologyFileBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		ontologyFileBox.setItems(getOntologyFiles());
		ontologyFileBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		ontologyFileBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(ontologyFileBox);
		
		final Label tokenFileLabel = toolkit.createLabel(form.getBody(), "Select Token File:");
		final Combo tokenFileBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		tokenFileBox.setItems(getTokenFiles());
		tokenFileBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		tokenFileBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(tokenFileBox);
		
		final Label constraintSelect = toolkit.createLabel(form.getBody(), "Select Role to Modify:");
		final Combo constraintBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		constraintBox.setItems(new String[0]);
		constraintBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		constraintBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(parent);
		
		
		final Label constraintSet = toolkit.createLabel(form.getBody(), "Set Role Item:");
		final Text constraintText = toolkit.createText(form.getBody(), "");
		constraintText.setLayoutData(gd);
		
		final Label emptyEnterLabel = toolkit.createLabel(form.getBody(), "");
		String enteredConstraintsStr = "";
		Button addConstraintButton = toolkit.createButton(form.getBody(), "Enter constraint", SWT.PUSH);
		addConstraintButton.setToolTipText("Add constraint to list of constraints for this token.");
		addConstraintButton.setLayoutData(gd);
		
		
		/* ETHAN CHANGES */
		
		final Label emptyConstraintLabel = toolkit.createLabel(form.getBody(), "");
		
		final Composite tableComposite = new Composite(form.getBody(), SWT.NONE);
		
		constraintsTable = new TableViewer(tableComposite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		constraintsTable.getTable().setLinesVisible(true);
		constraintsTable.getTable().setHeaderVisible(true);
		TableViewerColumn roleColumn = new TableViewerColumn(constraintsTable, SWT.NONE);
		roleColumn.getColumn().setText("Role");
		roleColumn.getColumn().setResizable(false);
		TableViewerColumn itemColumn = new TableViewerColumn(constraintsTable, SWT.NONE);
		itemColumn.getColumn().setText("Item");
		itemColumn.getColumn().setResizable(false);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableComposite.setLayout(tableLayout);

	    tableLayout.setColumnData(roleColumn.getColumn(), new ColumnWeightData(50));
	    tableLayout.setColumnData(itemColumn.getColumn(), new ColumnWeightData(50));
	    GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, false);
	    tableGd.verticalSpan = 5;
	    tableComposite.setLayoutData(tableGd);
	    
	    for (int i = 0; i < tableGd.verticalSpan; i++) {
    			toolkit.createLabel(form.getBody(), "");
	    }
	    
//	    final Label emptyRemoveConstraintLabel = toolkit.createLabel(form.getBody(), "");
	    Button removeConstraintButton = toolkit.createButton(form.getBody(), "Remove constraint", SWT.PUSH);
	    removeConstraintButton.setToolTipText("Remove highlighted constraint from list of constraints for this token.");
	    removeConstraintButton.setLayoutData(gd);

		/* ETHAN - Done */
		
		final Label constraintParentsLabel = toolkit.createLabel(form.getBody(), "Additional ontology parents (optional):");
		final Text constraintParents = toolkit.createText(form.getBody(), "");
		constraintParents.setToolTipText("Enter a comma-separated list of ontology super-types for the new constraint. E.g., \" entity, artifact, moveable \"");
		constraintParents.setLayoutData(gd);
		
		final Label appMappingLabel = toolkit.createLabel(form.getBody(), "Application mapping (optional):");
		final Text appMappingText = toolkit.createText(form.getBody(), "$");
		appMappingText.setToolTipText("Enter the value you want this constrain value to be translated to in the application domain. E.g., red-adj might become just red.");	
		appMappingText.setLayoutData(gd);

		final HashMap<String, String> slotsValues = new HashMap<String, String>();
		final ArrayList<String> slots = new ArrayList<String>();
		
		final ArrayList<String> parents = new ArrayList<String>();
	
		Button addTokenButton = toolkit.createButton(form.getBody(), "Add token.", SWT.PUSH);
		Button reloadTypesButton = toolkit.createButton(form.getBody(), "Reload Type Constructions.", SWT.PUSH);
		
		
		/* Ethan Start */
		/* inflection -> 'past/present', wordform -> 'blocked' */
		
		/* Empty label for spacing */
		toolkit.createLabel(form.getBody(), "");
		toolkit.createLabel(form.getBody(), "");
		
		final Label morphFileLabel = toolkit.createLabel(form.getBody(), "Select Morphology File:");
		final Combo morphFileBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		morphFileBox.setItems(getMorphFiles());
		morphFileBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		morphFileBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(morphFileBox);
		
		/* NEEDS TO SHOW LIST OF INFLECTIONS */
		final Label inflectionSelect = toolkit.createLabel(form.getBody(), "Select Inflection to Modify:");
		final Combo inflectionBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		inflectionBox.setItems(new String[0]);
		inflectionBox.setLayoutData(gd); //new GridData(GridData.FILL_HORIZONTAL));
		inflectionBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(inflectionBox);
		
		final Label wordformSelect = toolkit.createLabel(form.getBody(), "Set Wordform:");
		final Text wordformText = toolkit.createText(form.getBody(), "");
		wordformText.setLayoutData(gd);
		
		/* Empty label for spacing */
		toolkit.createLabel(form.getBody(), "");
		
		Button addInflectionButton = toolkit.createButton(form.getBody(), "Enter inflection", SWT.PUSH);
		addInflectionButton.setToolTipText("Add constraint to list of constraints for this token.");
		addInflectionButton.setLayoutData(gd);
		
		/* Empty label for spacing */
		toolkit.createLabel(form.getBody(), "");
		
		final Composite inflectionTableComposite = new Composite(form.getBody(), SWT.NONE);
		inflectionTable = new TableViewer(inflectionTableComposite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		inflectionTable.getTable().setLinesVisible(true);
		inflectionTable.getTable().setHeaderVisible(true);
		TableViewerColumn inflectionColumn = new TableViewerColumn(inflectionTable, SWT.NONE);
		inflectionColumn.getColumn().setText("Inflection");
		inflectionColumn.getColumn().setResizable(false);
		TableViewerColumn wordformColumn = new TableViewerColumn(inflectionTable, SWT.NONE);
		wordformColumn.getColumn().setText("Wordform");
		wordformColumn.getColumn().setResizable(false);
		TableColumnLayout inflectionTableLayout = new TableColumnLayout();
		inflectionTableComposite.setLayout(inflectionTableLayout);

		inflectionTableLayout.setColumnData(inflectionColumn.getColumn(), new ColumnWeightData(50));
		inflectionTableLayout.setColumnData(wordformColumn.getColumn(), new ColumnWeightData(50));
	    inflectionTableComposite.setLayoutData(tableGd);
	    
	    for (int i = 0; i < tableGd.verticalSpan; i++) {
    			toolkit.createLabel(form.getBody(), "");
	    }

	    Button removeInflectionButton = toolkit.createButton(form.getBody(), "Remove inflection", SWT.PUSH);
	    removeInflectionButton.setToolTipText("Remove highlighted inflection from list of inflections for this token.");
	    removeInflectionButton.setLayoutData(gd);
		
	    toolkit.createLabel(form.getBody(), "");
	    Button addMorphButton = toolkit.createButton(form.getBody(), "Add morphology entry.", SWT.PUSH);
	    
		/* Ethan - Done */
		
		parentText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				constraintText.setText("");
				slots.clear();
				slotsValues.clear();
				parents.clear();
				constraints.clear();
				
				/* Ethan */
//				wordformText.setText("");
				inflections.clear();
				constraintsTable.getTable().removeAll();
				inflectionTable.getTable().removeAll();
				
				
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
		
		
		/* TODO */
//		inflectionBox.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				/* What does any of this do? */
////				inflectionText.setText(slotsValues.get(inflectionBox.getText()));
////				what does this do? - parentValue = slotsValues.get(inflectionBox.getText());
////				what does this do? - appMappingText.setText("$");
//			}
//		});	

		ontologyFileBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modifiedOnt = ontologyFileBox.getText();
			}
		});
		
		tokenFileBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modifiedTok = tokenFileBox.getText();
			}
		});
		
		morphFileBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modifiedMorph = morphFileBox.getText();
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
							parentValue += " " + inputParents + " shared";
							addOntologyItem(value, parentValue, modifiedOnt);
							constraints.add(constraintBox.getText() + " <-- " + value);
							
							/* Ethan */
							addTableEntry(constraintsTable.getTable(), constraintBox.getText(), value);
							
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
							
							/* Ethan */
							constraintsTable.getTable().removeAll();
							
							constraintText.setText("");
							String message = "@" + value + "' already exists in Ontology, and is not a subtype of @" + parentValue 
									+ " . This will cause errors and prevent proper unification of the token's constraints.";
							broadcastError(message);
							//System.out.println(value + " already exists in Ontology, and is not a subtype of " + parentValue + " .");
						} else {
							constraints.add(constraintBox.getText() + " <-- " + value);
							
							/* Ethan */
							addTableEntry(constraintsTable.getTable(), constraintBox.getText(), value);
							
							if (appValue.length() > 1) {
								writeMappingFile(value, appValue);
							}
						}
					}
				} else {
					constraints.add(constraintBox.getText() + " <-- " + value);
					
					/* Ethan */
					addTableEntry(constraintsTable.getTable(), constraintBox.getText(), value);
					
				}
				appMappingText.setText("$");
				constraintText.setText("");
				constraintParents.setText("");
			}
		});
		addConstraintButton.setLayoutData(gd);
		
		
		/* Ethan */
		removeConstraintButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				Table cTable = constraintsTable.getTable();
				int selected = cTable.getSelectionIndex();
				if (selected == -1) {
					
//					/* ETHAN - TODO TESTING Morphology*/
//					String message;
//					String tokText = tokenText.getText();
//					
//					if (entryInMorph(tokText)) {
//						message = tokText + " = True";
//					} else {
//						message = tokText + " = False";
//					}

					String message = "Please select a constraint to remove.";
					broadcastError(message);
				} else {
					cTable.deselectAll();
					cTable.remove(selected);
					constraints.remove(selected);
					
				}	
			}
		});
		removeConstraintButton.setLayoutData(gd);
		
		/* TODO - DONE?*/
		addInflectionButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				String wordform = wordformText.getText();

				if (wordform.equals("")) {
					String message = "There are no inflection values to add; you need to fill in the wordform value slot.";
					broadcastError(message);
				} 
				
				else {
					/* Maps a wordform to all inflections for ease of use */
					HashSet<String> inflectionVals;
					if (inflections.containsKey(wordform)) {
						inflectionVals = inflections.get(wordform);
						inflectionVals.add(inflectionBox.getText());
					} else {
						inflectionVals = new HashSet<String>();
						inflectionVals.add(inflectionBox.getText());
						inflections.put(wordform, inflectionVals);
					}
					
//					inflections.add(wordform + "\t" + tokenText.getText() + " " + inflectionBox.getText());
					addTableEntry(inflectionTable.getTable(), inflectionBox.getText(), wordform);
					inflectionBox.setText("");
				}
			}
		});
		addInflectionButton.setLayoutData(gd);
		
		
		/* TODO - DONE? */
		removeInflectionButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				Table iTable = inflectionTable.getTable();
				int selected = iTable.getSelectionIndex();
				if (selected == -1) {
					String message = "Please select a constraint to remove.";
					broadcastError(message);
				} else {
					TableItem row = iTable.getItem(selected);
					String inflection = row.getText(0);
					String wordform = row.getText(1);
					
					if (inflections.containsKey(wordform)) {
						HashSet<String> inflectionVals = inflections.get(wordform);
						if (inflectionVals.size() <= 1) {
							inflections.remove(wordform);
						} else {
							inflectionVals.remove(inflection);
						}
					}
					
					iTable.deselectAll();
					iTable.remove(selected);
				}	
			}
		});
		removeInflectionButton.setLayoutData(gd);
		
		
		addTokenButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				token = tokenText.getText();
				parentCxn = parentText.getText();
				
				if (!entryInMorph(token)) {
					broadcastError("This token definition does not contain a corresponding definition in the morphology. "
							+ "Please add a morphology entry for this token.");
				}
				else if (!token.equals("") && !parentCxn.equals("") && constraints.size() > 0) {
					write(token, parentCxn, constraints);
					tokenText.setText("");
					parentText.setText("");
					constraintText.setText("");
					constraintBox.setText("");
					constraintParents.setText("");
					// Check grammar instead of retrieving analyzer. Analyzer object too large for "base", etc.
					// TODO: Check that this is proper.
					//PrefsManager.getDefault().checkGrammar();
				} else {
					//System.out.println("Definition not complete.");
					broadcastError("This token definition is not complete; you must make sure you select a parentCxn (currently set to '" + parentCxn + "'), "
							+ "fill in the box for the token's value (currently: '" + token
							+ "'), and add at least one constraint (currently " + constraints.size() + " constraints added).");
				}

				
			}
		});
		addTokenButton.setLayoutData(gd);
		
		/* TODO */
		addMorphButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				token = tokenText.getText();
//				parentCxn = parentText.getText();
				
				if (entryInMorph(token)) {
					broadcastError("Token is already in the morphology.");
				}
				else if (!token.equals("") && !parentCxn.equals("") && inflections.size() > 0) {
					FileWriter morph_fw;
					BufferedWriter morph_bw;
					
					try {
						if (modifiedMorph == null) {
							broadcastError("Please select a morphology file.");
							return;
						}
						
						File morph_file = new File(base, modifiedMorph);
						morph_fw = new FileWriter(morph_file.getAbsoluteFile(), true);
						morph_bw = new BufferedWriter(morph_fw);
						
						String wordformString;
						boolean isFirst;
						for (String wordform : inflections.keySet()) {
							wordformString = wordform + "\t";
							isFirst = true;
							for (String inflection : inflections.get(wordform)) {
								if (!isFirst) {
									wordformString += " ";
								}
								wordformString += token + " " + inflection;
								isFirst = false;
							}	
							morph_bw.write(wordformString);
							morph_bw.newLine();
						}
						morph_bw.close();
	
						inflections.clear();
						inflectionTable.getTable().removeAll();
						wordformText.setText("");
						inflectionBox.setText("");
						
						try {
							getGrammar().buildTokenAndMorpher();
						} catch (ParserException pe) {
							broadcastError(pe.getMessage());
						}
						Utils.flushCaches();
					} catch(IOException ex) {
						ex.printStackTrace();
					}	
				} else {
					//System.out.println("Definition not complete.");
					broadcastError("This inflection definition is not complete; you must make sure you select a parentCxn (currently set to '" + parentCxn + "'), "
							+ "fill in the box for the token's value (currently: '" + token
							+ "'), and add at least one inflection (currently " + inflections.size() + " inflections added).");
				}

				
			}
		});
		addMorphButton.setLayoutData(gd);

		reloadTypesButton.setToolTipText("Reload the list of type constructions from a newly checked grammar..");
		reloadTypesButton.setLayoutData(new GridData(SWT.FILL, 1, false, false));
		reloadTypesButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				//System.out.println("Reloading types");
				setTypes();
			}
		});
	}
	
	/* Check if string exists in morphology */
	private boolean entryInMorph(String entry) {
		File base = prefs.getBaseDirectory();
		File ecgmorph_path;

		List<String> morph_paths = prefs.getList(AP.MORPHOLOGY_PATH);
		for (String path : morph_paths) {
			ecgmorph_path = new File(base, path);
			
			TextFileLineIterator tfli = new TextFileLineIterator(ecgmorph_path);
			
			int lineNum = 0;
									
			while (tfli.hasNext()) {
				lineNum++;
				String line = tfli.next();
				// Skip blank lines or lines with just a comment
				if (line.matches("\\s*#.*") || line.matches("\\s*")) {
					continue;
				}
				String splitline[] = line.split("\\s+");
				if (splitline.length < 3) {
					throw new ParserException("Improperly formatted entry in morph file " + ecgmorph_path + ", line " + lineNum);
				}
				
				if (entry.equals(splitline[0])) {
					return true;
				}
				
				for (int ii = 1; ii+1 < splitline.length; ii+=2) {
					if (entry.equals(splitline[ii])) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
	}
}

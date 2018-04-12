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

public class MorphView extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.morphView";
	private String token;
	private String parentCxn;
	private ArrayList<String> constraints;
	
	private String modifiedTok = null;
	
	private File token_file;
	private FileWriter fw;
	private BufferedWriter bw;
	private FileWriter ontWriter;
	private BufferedWriter bOntWriter;
	private AnalyzerPrefs prefs;
	private File base;
	private String[] typeCxns;
	private static final Charset DEFAULT_CHARSET = Charset.forName(ECGConstants.DEFAULT_ENCODING);
	
	
	private String modifiedMorph = null;
	
	private String parentValue;

	private HashMap<String, HashSet<String>> inflections;
	private TableViewer inflectionTable;

	
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
	
	/** Checks if CHILD is a subtype of PARENT.  - NOTE: not the same as version in TokenView */
	private boolean isSubtype2(String child, String parent) {
		TypeSystem ts = getGrammar().getConstructionTypeSystem();
		try {
			if (ts.subtype(ts.getInternedString(child), ts.getInternedString(parent))) {
				return true;
			}
		} catch (TypeSystemException e) {
			System.out.println("Either " + child + " or " + parent + " doesn't exist.");
			return false;
		}
		return false;
	}
	
	
	private String[] getMorphFiles() {
		List<String> morph_paths = prefs.getList(AP.MORPHOLOGY_PATH);
		String[] toks = new String[morph_paths.size()];
		for (int i=0; i < toks.length; i++) {
			toks[i] = morph_paths.get(i);
		}
		return toks;
	}
	
	private String[] getInflectionsForParentType(String parent) {
		HashSet<String> inflections = new HashSet<String>();
		
		File base = prefs.getBaseDirectory();
		File table_path = new File(base, prefs.getSetting(AP.TABLE_PATH));
		TextFileLineIterator tfli = new TextFileLineIterator(table_path);
		
		int lineNum = 1;
		while (tfli.hasNext()) {
			String l = tfli.next();
			if (l.equals("Constructional")) {
				continue;
			}
			if (l.startsWith("#")) {
				continue;
			}
			if (l.equals("Meaning")) {
				continue;
			}
			String[] s = l.split("\\s*::\\s*");
			if (s.length < 3) {
				throw new ParserException("Improperly formatted morph entry on line " + lineNum 
						+ " of table in file " + table_path + 
						". Entry must be of the form: \n 'FlectType :: constraints :: compatible_constructions'.");
			}
			String flectType = s[0];
			String[] compatible_cxns = s[2].split(",\\s");
			
			for (String cxn : compatible_cxns) {
				if (isSubtype2(parent, cxn)) {
					
					inflections.add(flectType.replace(',', '/'));
					break;
				}
			}
			
			lineNum += 1;
		}
		
		String[] inflectionsArray = inflections.toArray(new String[inflections.size()]);
		Arrays.sort(inflectionsArray);
		return inflectionsArray;
	}
	
	/** Adds a pair of values to the the constraint table. */
	private void addTableEntry(Table table, String constraint, String value) {
		TableItem newItem = new TableItem(table, SWT.NONE);
		newItem.setText(0, constraint);
		newItem.setText(1, value);	
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
	
	static Combo parentText;
	
	public void broadcastError(String message) {
		String newMessage = "WARNING: \n \n" + message;
		final IStatus status = new Status(IStatus.ERROR, Application.PLUGIN_ID, newMessage);
		ErrorDialog.openError(null, "Morphology Editor Error", null, status);
		throw new GrammarException(message);
	}
	
	
	public void createPartControl(Composite parent) {
		inflections = new HashMap<String, HashSet<String>>();
		
		prefs =  (AnalyzerPrefs) getGrammar().getPrefs();
		base = prefs.getBaseDirectory();
		typeCxns = getTypes("\"*\"");
		
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);

		form.setText("Morphology Editor");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		layout.numColumns = 2;
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		gd.horizontalSpan = 1;

		final Label tokenLabel = toolkit.createLabel(form.getBody(), "Enter Lemma:");
		final Text tokenText = toolkit.createText(form.getBody(), "");
		tokenText.setLayoutData(gd);
		
		
		final Label parentLabel = toolkit.createLabel(form.getBody(), "Select Parent Type:");
		parentText = new Combo(form.getBody(), SWT.DROP_DOWN);
		setTypes();
		parentText.setLayoutData(gd);
		parentText.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(parent);
		
		GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, false);
	    tableGd.verticalSpan = 5;
		
		/* inflection -> 'past/present', wordform -> 'blocked' */
		
		final Label morphFileLabel = toolkit.createLabel(form.getBody(), "Select Morphology File:");
		final Combo morphFileBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		morphFileBox.setItems(getMorphFiles());
		morphFileBox.setLayoutData(gd);
		morphFileBox.setData(toolkit.KEY_DRAW_BORDER, toolkit.TEXT_BORDER);
		toolkit.paintBordersFor(morphFileBox);
		
		final Label inflectionSelect = toolkit.createLabel(form.getBody(), "Select Inflection to Modify:");
		final Combo inflectionBox = new Combo(form.getBody(), SWT.DROP_DOWN);
		inflectionBox.setLayoutData(gd);
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
	    
		
		parentText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				inflections.clear();
				inflectionTable.getTable().removeAll();
				
				parentCxn = parentText.getText();
				inflectionBox.setItems(getInflectionsForParentType(parentCxn));
					
			}
		});
		
		
		morphFileBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modifiedMorph = morphFileBox.getText();
			}
		});
		
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
					
					addTableEntry(inflectionTable.getTable(), inflectionBox.getText(), wordform);
					inflectionBox.setText("");
				}
			}
		});
		addInflectionButton.setLayoutData(gd);
		
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

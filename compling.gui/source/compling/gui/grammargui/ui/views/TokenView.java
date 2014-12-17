package compling.gui.grammargui.ui.views;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.grammargui.model.PrefsManager;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;

public class TokenView extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.tokenView";

	private TableViewer viewer;
	
	private String token;
	private String parentCxn;
	private ArrayList<String> constraints;
	
	

	
	private File token_file;
	private FileWriter fw;
	private BufferedWriter bw;
	
	private AnalyzerPrefs prefs;
	

	private void openFile() {
		try {
			prefs =  (AnalyzerPrefs) getGrammar().getPrefs();
			File base = prefs.getBaseDirectory();
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
	
	private boolean exists(String value) {
		return getGrammar().getOntologyTypeSystem().get(value.substring(1, value.length()).trim()) != null;
	}
	
	private void addOntologyItem(String value, String parent) {
		TypeSystem ts = getGrammar().getOntologyTypeSystem();
	}
	
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
	
	
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getOntologyTypeSystem() : null;
	}
	
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}
	
	protected ECGAnalyzer getAnalyzer() {
		return PrefsManager.getDefault().getAnalyzer();
	}
	

	
	
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	
	public void createPartControl(Composite parent) {
		
		constraints = new ArrayList<String>();
		
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
		tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd = new GridData();
		gd.horizontalSpan = 4;
		
		final Label parentLabel = toolkit.createLabel(form.getBody(), "Enter Parent:");
		
		final Text parentText = toolkit.createText(form.getBody(), "");

		parentText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		
		final ArrayList<String> parents = new ArrayList<String>();
		

		final Label constraint1 = toolkit.createLabel(form.getBody(), "Constraint 1:");
		constraint1.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		final Text constraint1Text = toolkit.createText(form.getBody(), "");
		constraint1Text.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		final Label constraint2 = toolkit.createLabel(form.getBody(), "Constraint 2:");
		constraint2.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		final Text constraint2Text = toolkit.createText(form.getBody(), "");
		constraint2Text.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		final Label constraint3 = toolkit.createLabel(form.getBody(), "Constraint 3:");
		constraint3.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		final Text constraint3Text = toolkit.createText(form.getBody(), "");
		constraint3Text.setLayoutData(new GridData(SWT.FILL, 1, true, false));
		

		final ArrayList<Label> labelText = new ArrayList<Label>();
		labelText.add(constraint1);
		labelText.add(constraint2);
		labelText.add(constraint3);
		final ArrayList<Text> textList = new ArrayList<Text>();
		textList.add(constraint1Text);
		textList.add(constraint2Text);
		textList.add(constraint3Text);

		parentText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 13) {
					parents.clear();
					parentCxn = parentText.getText();
					int i = 0;
					try {
						for (Constraint c : getGrammar().getConstruction(parentCxn).getMeaningBlock().getConstraints()) {
							if (labelText.size() > i && c.getOperator().equals("<--")) {
								labelText.get(i).setText(c.getArguments().get(0).toString());
								textList.get(i).setText(c.getValue());
								parents.add(textList.get(i).getText());
							}
							i +=1 ;
						}
					} catch(NullPointerException problem) {
						throw new GrammarException("That doesn't exist.");
					}
				}
			}
		});
		

		Button addTokenButton = toolkit.createButton(form.getBody(), "Add token.", SWT.PUSH);
		addTokenButton.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				token = tokenText.getText();
				parentCxn = parentText.getText();
				boolean write = true;
				for (int i = 0; i < parents.size(); i++) {
					write = true;
					String p = parents.get(i);
					String value = textList.get(i).getText();
					if (value.charAt(0) == ECGConstants.ONTOLOGYPREFIX &&
							p.charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
						if (!exists(value)) {
							System.out.println("Better figure out how to add it.");
							write = false;
							addOntologyItem(value, p);  // TODO: Figure out how to add ontology items.
						} else {
							if (!isSubtype(value, p)) {
								System.out.println("Not a proper subtype.");
								write = false;
							}
						}
					}
					if (write) {
						constraints.add(labelText.get(i).getText() + " <-- " + textList.get(i).getText());
						
					}
					labelText.get(i).setText("Constraint " + (i+1));
					textList.get(i).setText("");
				}
				if (write) {
					write(token, parentCxn, constraints);
					tokenText.setText("");
					parentText.setText("");
				}
				parents.clear();

			}
		});
		addTokenButton.setLayoutData(gd);
		
		
		Button loadTokens = toolkit.createButton(form.getBody(), "Reload tokens.", SWT.PUSH);
		addTokenButton.addSelectionListener(new SelectionAdapter() { 
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

package compling.gui.grammargui.ui.views;

import java.util.ArrayList;
import java.util.HashMap;

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
import compling.gui.grammargui.model.PrefsManager;

public class PackageView extends ViewPart {
	
	
	public static final String ID = "compling.gui.grammargui.views.package";
	

	private Tree tree;
	ArrayList<String> declared;
	HashMap<String, ArrayList<String>> relations;
	
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}

	@Override
	public void createPartControl(Composite parent) {

		initializeData();
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;

		form.getBody().setLayout(layout);
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		
		Button reloadButton = toolkit.createButton(form.getBody(), "Reload Packages", SWT.PUSH);
		reloadButton.setToolTipText("Press button to check grammar and reload packages, so newly defined packages appear in tree.");
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
		// TODO Auto-generated method stub

	}
	
	private void initializeData() {
		declared = getGrammar().getDeclaredPackages();
		relations = getGrammar().getPackageRelations();
	}

	protected void createTree() {
		tree.removeAll();
		ArrayList<String> observed = new ArrayList<String>();
		System.out.println(relations);
		TreeItem active = new TreeItem(tree, 0);
		active.setText("Active Packages");
		for (String pkg : declared) {
			if (!observed.contains(pkg)) {
				
				ArrayList<String> observed2 = new ArrayList<String>();
				TreeItem dec = new TreeItem(active, 0);
				dec.setText(pkg);
				observed.add(pkg);
				System.out.println(pkg);
				System.out.println(observed);
				if (relations.containsKey(pkg)) {
					for (String imports : relations.get(pkg)) {
						if (!observed2.contains(imports)) {
							TreeItem imp = new TreeItem(dec, 0);
							if (declared.contains(imports)) {
								imports += " (active)";
							}
							imp.setText(imports);
							observed2.add(imports);
						}
					}
				}
			}
		}
		
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}

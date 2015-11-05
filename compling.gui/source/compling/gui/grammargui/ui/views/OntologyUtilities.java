package compling.gui.grammargui.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.parser.ParserException;

public class OntologyUtilities extends ViewPart {
	
	public static final String ID = "compling.gui.grammargui.views.ontologyUtilities";
	
	/*
	private class CheckSubtypeAction extends Action implements IModelChangedListener {
		
		public CheckSubtypeAction() {
			super();
			subtypeButton.setLayoutData(new GridData(SWT.FILL, 1, false, false));
		
			
			subtypeButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String type1 = type1Text.getText();
					String type2 = type2Text.getText();
					
				}
			});
		}
		
		public void run() {
			//return isSubtype()
		}

		@Override
		public void modelChanged(ModelChangedEvent event) {
			setEnabled(isEnabled());	
		}
		

	}
	*/
	
	/** Checks if CHILD is a subtype of PARENT. */
	private boolean isSubtype(String child, String parent) {
		try {
			TypeSystem ts = getGrammar().getOntologyTypeSystem();
			child = child.replace("@", "");
			//child = child.substring(1, child.length()).trim();
			//String ancestor = parent.substring(1, parent.length()).trim();
			parent = parent.replace("@", "");
			return ts.subtype(ts.getInternedString(child), ts.getInternedString(parent));
		} catch(TypeSystemException type) {
			System.out.println("Either " + child + " or " + parent + " doesn't exist.");
			//throw new ParserException("Problem");
			return false;
		}
	}

	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}	
	

	

	

	private Button subtypeButton;
	private Text relationText;
	private Text type1Text;
	private Text type2Text;
	
		
	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		form.setText("Ontology Utilities");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		GridData gd = new GridData(SWT.FILL, 1, true, false);
		gd.horizontalSpan = 1;
		//layout.
		
		
		final Label type1Label = toolkit.createLabel(form.getBody(), "Enter type 1:");
		type1Text = toolkit.createText(form.getBody(), "");
		
		final Label type2Label = toolkit.createLabel(form.getBody(), "Enter type 2:");
		type2Text = toolkit.createText(form.getBody(), "");
		
		subtypeButton = toolkit.createButton(form.getBody(), "Get relations.", SWT.PUSH);
		subtypeButton.setLayoutData(new GridData(SWT.FILL, 1, false, false));
		
		relationText = toolkit.createText(form.getBody(), "");
		relationText.setLayoutData(gd);
		
		subtypeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String type1 = type1Text.getText();
				String type2 = type2Text.getText();
				if (type1.equals(type2)) {
					relationText.setText(type1 + " and " + type2 + " are the same value.");
				}
				else if (isSubtype(type1, type2)) {
					relationText.setText(type1 + " is a subtype of " + type2);
				} 
				else if (isSubtype(type2, type1)) {
					relationText.setText(type2 + " is a subtype of " + type1);
				} else {
					relationText.setText(type1 + " is not related to " + type2);
				}
			}
		});
		


	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}

package compling.gui.grammargui.ui.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

public class OntologyLatticeView extends TypeSystemTreeView {
	
	public static final String ID = "compling.gui.grammargui.views.ontologylatticeview";

	
	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getOntologyTypeSystem() : null;
	}
	/*
	@Override
	public void createPartControl(Composite parent) {
		TreeViewer treeViewer = new TreeViewer(parent, SWT.VIRTUAL | SWT.MULTI);
		//treeViewer.

	}
	

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	*/
	
	@Override
	public String getId() {
		return ID;
	}

}

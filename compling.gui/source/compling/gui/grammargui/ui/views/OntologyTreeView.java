package compling.gui.grammargui.ui.views;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

public class OntologyTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.ontologyHierarchy";

	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getOntologyTypeSystem() : null;
	}

	@Override
	public String getId() {
		return ID;
	}

}

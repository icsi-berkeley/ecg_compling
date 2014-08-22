package compling.gui.grammargui.ui.views;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

public class SituationTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.SituationHierarchy";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getSituationTypeSystem() : null;
	}

}

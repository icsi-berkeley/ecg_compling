package compling.gui.grammargui.ui.views;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

public class MapTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.MapHierarchy";

	public MapTreeView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.instance().getGrammar();
		return grammar != null ? grammar.getMapTypeSystem() : null;
	}

}

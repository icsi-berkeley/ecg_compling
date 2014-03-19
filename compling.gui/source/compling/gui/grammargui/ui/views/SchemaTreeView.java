package compling.gui.grammargui.ui.views;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

public class SchemaTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.schemaHierarchy";

	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.instance().getGrammar();
		return grammar != null ? grammar.getSchemaTypeSystem() : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compling.gui.grammargui.views.TypeSystemTreeView#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}

}

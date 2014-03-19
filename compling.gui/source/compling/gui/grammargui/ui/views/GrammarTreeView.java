package compling.gui.grammargui.ui.views;

import java.util.Arrays;

import compling.grammar.ecg.Grammar;
import compling.gui.grammargui.model.PrefsManager;

public class GrammarTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.GrammarTreeView";

	public GrammarTreeView() {
		super();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected Object getTypeSystem() {
		Grammar g = PrefsManager.instance().getGrammar();
		return g == null ? null : Arrays.asList(g.getCxnTypeSystem(), g.getSchemaTypeSystem(), g.getMapTypeSystem(),
				g.getSituationTypeSystem(), g.getOntologyTypeSystem());
	}

}

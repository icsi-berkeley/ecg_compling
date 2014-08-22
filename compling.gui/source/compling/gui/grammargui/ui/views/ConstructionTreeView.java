/**
 * 
 */
package compling.gui.grammargui.ui.views;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;

/**
 * 
 * @author lucag
 */
public class ConstructionTreeView extends TypeSystemTreeView {

	public static final String ID = "compling.gui.grammargui.views.constructionHierarchy";

	/** Constructor. */
	public ConstructionTreeView() {
		super();
	}

	/**
	 * Subclasses must implement this method
	 * 
	 * @return The newly created content provider
	 */
	@Override
	protected TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar != null ? grammar.getCxnTypeSystem() : null;
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
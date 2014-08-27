package compling.gui.grammargui.util;

import java.util.EventObject;

import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.PrefsManager;

/**
 * A even describing change in the application's model
 * 
 * @author lucag
 */
public class ModelChangedEvent extends EventObject {

	private static final long serialVersionUID = -5459504104297303615L;

	private AnalyzerSentence[] added;
	private AnalyzerSentence[] removed;
	private IProxy grammarProxy;

	/**
	 * @param source
	 * @param added
	 * @param removed
	 */
	public ModelChangedEvent(Object source, AnalyzerSentence[] added, AnalyzerSentence[] removed) {
		super(source);
		this.added = added;
		this.removed = removed;
	}

	public ModelChangedEvent(Object source, IProxy grammarProxy) {
		super(source);
		this.grammarProxy = grammarProxy;
		this.added = ((PrefsManager) source).getSentences().toArray(new AnalyzerSentence[0]);
		this.removed = new AnalyzerSentence[0];
	}

	/**
	 * @return the added
	 */
	public AnalyzerSentence[] getAdded() {
		return added;
	}

	/**
	 * @return the removed
	 */
	public AnalyzerSentence[] getRemoved() {
		return removed;
	}

	/**
	 * @return the grammar
	 */
	public IProxy getGrammarProxy() {
		return grammarProxy;
	}
}

package compling.gui.grammargui.model;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.ModelChangedEvent;

public class AnalyzerSentenceContentProvider implements IStructuredContentProvider, IModelChangedListener {

	private StructuredViewer viewer;
	private Set<AnalyzerSentence> sentences;
	private PrefsManager model;

	/**
	 * @param viewer
	 */
	public AnalyzerSentenceContentProvider(StructuredViewer viewer) {
		this.viewer = viewer;
	}

	public Object[] getElements(Object parent) {
		if (sentences != null)
			return sentences.toArray();

		return new Object[0];
	}

	public void dispose() {
		if (model != null)
			model.removeModelChangeListener(this);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		init((PrefsManager) newInput);
		if (oldInput != null)
			((PrefsManager) oldInput).removeModelChangeListener(this);
	}

	private void addSentence(AnalyzerSentence newSentence) {
		if (! sentences.contains(newSentence)) {
			sentences.add(newSentence);
		}
	}

	private void removeSentence(AnalyzerSentence sentence) {
		sentences.remove(sentence);
	}

	protected void init(PrefsManager model) {
		this.model = model;
		if (model != null) {
			model.addModelChangeListener(this);
			sentences = model.getSentences();
		}
		else
			sentences = null;
		update();
	}

	protected void update() {
		viewer.getControl().setEnabled(sentences != null);
		viewer.refresh();
	}

	public void modelChanged(ModelChangedEvent event) {
//		Log.logInfo("modelChanged: %s\n", event);
		
		for (AnalyzerSentence s : event.getAdded())
			addSentence(s);

		for (AnalyzerSentence s : event.getRemoved())
			removeSentence(s);

		update();
	}

}

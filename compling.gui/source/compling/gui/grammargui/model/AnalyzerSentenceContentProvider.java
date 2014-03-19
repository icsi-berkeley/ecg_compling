package compling.gui.grammargui.model;

import java.util.Set;

import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import compling.gui.grammargui.util.ModelChangedEvent;

public class AnalyzerSentenceContentProvider implements IStructuredContentProvider, IModelChangedListener {

	private AbstractListViewer viewer;
	private Set<AnalyzerSentence> sentences;
	private PrefsManager model;

	/**
	 * @param viewer
	 */
	public AnalyzerSentenceContentProvider(AbstractListViewer viewer) {
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

	public void addSentence(String sentence) {
		AnalyzerSentence newSentence = new AnalyzerSentence(sentence, PrefsManager.instance());
		if (! sentences.contains(newSentence)) {
			sentences.add(newSentence);
			viewer.add(newSentence);
			viewer.setSelection(new StructuredSelection(new Object[] { newSentence }));
		}
	}

	public void removeSentence(Object sentence) {
		sentences.remove(sentence);
		update();
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
		init((PrefsManager) event.getSource());
	}

}

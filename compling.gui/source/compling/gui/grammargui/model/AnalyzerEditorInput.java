package compling.gui.grammargui.model;

import java.util.Collection;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;

import compling.gui.util.IParse;
import compling.gui.util.Utils;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

public class AnalyzerEditorInput implements IAnalyzerEditorInput {
	private static final int MAX_LEN = 9;
	private AnalyzerSentence sentence;

	/**
	 * Constructor.
	 * 
	 * @param sentence
	 */
	public AnalyzerEditorInput(AnalyzerSentence sentence) {
		this.sentence = sentence;
	}

	public String getText() {
		return sentence.getAnalysisText();
	}

	public String getGraphvizText() {
		StringBuilder b = new StringBuilder();
		for (String s : sentence.getAnalyses())
			b.append(s);

		return b.toString();
	}

	public Collection<IParse> getParses() {
		return sentence.getParses();
	}

	/**
	 * @return the sentence
	 */
	public AnalyzerSentence getSentence() {
		return sentence;
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		String name = sentence.getText();
		if (name.length() < MAX_LEN)
			return name;
		else
			return String.format("%s...", name.substring(0, MAX_LEN));
	}

	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getToolTipText() {
		return sentence.getText();
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof AnalyzerEditorInput) && this.sentence.equals(((AnalyzerEditorInput) other).sentence);
	}

	@Override
	public Collection<Pair<Analysis, Double>> getParsesAsPairs() {
		return Utils.getFlattened(getParses());
	}

}

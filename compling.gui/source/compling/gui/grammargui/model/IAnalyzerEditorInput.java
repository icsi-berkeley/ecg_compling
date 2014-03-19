package compling.gui.grammargui.model;

import java.util.Collection;

import org.eclipse.ui.IEditorInput;

import compling.gui.util.IParse;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

public interface IAnalyzerEditorInput extends IEditorInput {
	public String getText();

	public String getGraphvizText();

	public AnalyzerSentence getSentence();

	public Collection<IParse> getParses();

	public Collection<Pair<Analysis, Double>> getParsesAsPairs();
}

package compling.gui.grammargui.model;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;

import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.util.Log;
import compling.gui.util.IParse;
import compling.gui.util.Utils;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

/**
 * AnalyzerSentence bundles together a sentence to analyze (a string) and the preferences model.
 * 
 * @see PrefsManager
 * @author lucag
 */
public class AnalyzerSentence extends PlatformObject {

	private String sentence;
	private PrefsManager model;
	private Collection<IParse> parses;

	/**
	 * @return the model
	 */
	public PrefsManager getModel() {
		return model;
	}

	/**
	 * @param sentence
	 * @param model
	 */
	public AnalyzerSentence(String sentence, PrefsManager model) {
		super();
		
		if (sentence == null)
			throw new IllegalArgumentException("sentence must be non-null");

		this.sentence = sentence;
		this.model = model;
	}

	/**
	 * @return the sentence
	 */
	public String getText() {
		return sentence;
	}

	/**
	 * @return the analysis
	 * @throws IOException
	 */
	public String getAnalysisText() {
		return Utils.parse(sentence, model.getAnalyzer());
	}

	public List<String> getAnalyses() {
		return Utils.getTextParses(sentence, model.getAnalyzer());
	}

	public static class JobStatus extends Status implements IJobStatus {

		private Job job;

		public JobStatus(int severity, String pluginId, String message, Job job) {
			super(severity, pluginId, message);
			this.job = job;
		}

		public Job getJob() {
			return job;
		}

	}

	public IEditorInput getEditorInput() {
		return new AnalyzerEditorInput(this);
	}

	public Job getParserJob() {
		final String title = String.format("Analysis of \"%s\"", sentence);
		Job parserJob = new WorkspaceJob(title) {
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
//					parses = Collections.emptyList();
					parses = null;
					monitor.beginTask("Analysis", IProgressMonitor.UNKNOWN);
					parses = Utils.getParses(sentence, model.getAnalyzer());
					monitor.done();

					return JobStatus.OK_STATUS;
				}
				catch (RuntimeException x) {
					Log.logError(x, "Problem analyzing \"%s\"", sentence);
					return new JobStatus(IStatus.ERROR, EcgEditorPlugin.PLUGIN_ID, x.getMessage(), this);
				}
			}
		};
		parserJob.setUser(true);
		parserJob.setPriority(Job.LONG);

		return parserJob;
	}

	/**
	 * @return the parses
	 */
	public Collection<IParse> getParses() {
		return parses;
	}

	public Collection<Pair<Analysis, Double>> getAnalysisPairs() {
		return Utils.getFlattened(parses);
	}
	
	@Override
	public String toString() {
		return "AnalyzerSentence [sentence=" + sentence + ", model=" + model + "]";
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof AnalyzerSentence && this.sentence.equals(((AnalyzerSentence) other).sentence));
	}

	@Override
	public int hashCode() {
		return sentence.hashCode();
	}

}

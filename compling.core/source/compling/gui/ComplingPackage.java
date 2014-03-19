package compling.gui;

public enum ComplingPackage {

	// these must be ordered in terms of reverse precedence (so that more specific ones can override)
	GLOBAL(""),
	ANALYZER("compling.parser"),
	SIMULATOR("compling.simulator"),
	LEARNER("compling.learner"),
	CONTEXT_FITTER("compling.learner.contextfitting"),
	VERIFIER("compling.learner.AnalysisVerifier");

	private final String packageName;

	ComplingPackage(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}
}
// =============================================================================
// File        : NoECGAnalysisFoundException.java
// Author      : jbryant
// Change Log  : Created on Feb 25, 2007
//=============================================================================

package compling.parser.ecgparser;

import java.util.List;

//=============================================================================

public class NoECGAnalysisFoundException extends RuntimeException {

	private static final long serialVersionUID = -349091335193012819L;
	private List<? extends Analysis> analyses;

	public NoECGAnalysisFoundException() {
		super();
	}

	public NoECGAnalysisFoundException(List<Analysis> analyses) {
		super();
		this.analyses = analyses;
	}

	public NoECGAnalysisFoundException(String message, List<Analysis> analyses) {
		super(message);
		this.analyses = analyses;
	}

	public NoECGAnalysisFoundException(String message, Throwable cause, List<? extends Analysis> analyses) {
		super(message, cause);
		this.analyses = analyses;
	}

	public NoECGAnalysisFoundException(Throwable cause, List<? extends Analysis> analyses) {
		super(cause);
		this.analyses = analyses;
	}

	// Can return null
	public List<? extends Analysis> getAnalyses() {
		return analyses;
	}

}

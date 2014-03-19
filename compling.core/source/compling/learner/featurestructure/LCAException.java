// =============================================================================
// File        : LearnerException.java
// Author      : emok
// Change Log  : Created on Feb 27, 2006
//=============================================================================

package compling.learner.featurestructure;

//=============================================================================

public class LCAException extends RuntimeException {

	private static final long serialVersionUID = -349091335193012819L;

	public LCAException() {
		super();
	}

	public LCAException(String message) {
		super(message);
	}

	public LCAException(String message, Throwable cause) {
		super(message, cause);
	}

	public LCAException(Throwable cause) {
		super(cause);
	}

}

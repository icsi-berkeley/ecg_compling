// =============================================================================
// File        : LearnerException.java
// Author      : emok
// Change Log  : Created on Feb 27, 2006
//=============================================================================

package compling.learner;

//=============================================================================

public class LearnerException extends RuntimeException {

	private static final long serialVersionUID = -349091335193012819L;

	public LearnerException() {
		super();
	}

	public LearnerException(String message) {
		super(message);
	}

	public LearnerException(String message, Throwable cause) {
		super(message, cause);
	}

	public LearnerException(Throwable cause) {
		super(cause);
	}

	public static class InvalidGeneralizationException extends LearnerException {

		private static final long serialVersionUID = -9097193521114135586L;

		public InvalidGeneralizationException() {
			super();
		}

		public InvalidGeneralizationException(String message) {
			super(message);
		}

	}
}

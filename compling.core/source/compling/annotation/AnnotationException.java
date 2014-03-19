// =============================================================================
// File        : DataException.java
// Author      : emok
// Change Log  : Created on Jul 12, 2005
//=============================================================================

package compling.annotation;

//=============================================================================

public class AnnotationException extends RuntimeException {

	private static final long serialVersionUID = -5692352042369709631L;

	public AnnotationException() {
		super();
	}

	public AnnotationException(String message) {
		super(message);
	}

	public AnnotationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AnnotationException(Throwable cause) {
		super(cause);
	}

}

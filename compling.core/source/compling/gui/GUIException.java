// =============================================================================
// File        : GUIException.java
// Author      : emok
// Change Log  : Created on Nov 25, 2006
//=============================================================================

package compling.gui;

//=============================================================================

public class GUIException extends RuntimeException {

	private static final long serialVersionUID = -6248260197244321316L;

	public GUIException() {
		super();
	}

	public GUIException(String message, Throwable cause) {
		super(message, cause);
	}

	public GUIException(String message) {
		super(message);
	}

	public GUIException(Throwable cause) {
		super(cause);
	}

}

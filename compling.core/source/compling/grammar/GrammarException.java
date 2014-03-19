// =============================================================================
// File        : GrammarException.java
// Author      : emok
// Change Log  : Created on Nov 16, 2006
//=============================================================================

package compling.grammar;

import compling.grammar.ecg.Grammar.Primitive;

//=============================================================================

public class GrammarException extends RuntimeException {

	private static final long serialVersionUID = 5544834174034774755L;

	private Primitive primitive;

	public GrammarException() {
		super();
	}

	public GrammarException(String message, Primitive primitive) {
		super(message);
		this.primitive = primitive;
	}

	public GrammarException(String message) {
		super(message);
	}

	public GrammarException(Throwable cause) {
		super(cause);
	}

	public GrammarException(String message, Throwable cause) {
		super(message, cause);
	}

	public GrammarException(String message, Throwable cause, Primitive primitive) {
		super(message, cause);
		this.primitive = primitive;
	}

	/**
	 * @return the primitive
	 */
	public Primitive getPrimitive() {
		return primitive;
	}

}

// =============================================================================
// File        : TypeSystemException.java
// Author      : emok
// Change Log  : Created on Nov 16, 2006
//					: Updated (lucag) Ago 28, 2008
//=============================================================================

package compling.grammar.unificationgrammar;

import java.util.ArrayList;
import java.util.List;

import compling.grammar.ecg.GrammarError;
import compling.util.Arrays;

//=============================================================================

public class TypeSystemException extends Exception {

	/**
	 * TypeSystemException.java
	 */
	private static final long serialVersionUID = 3350582236503519076L;

	private List<GrammarError> errors;

	/**
	 * @return the errors
	 */
	public GrammarError[] getErrors() {
		return errors.toArray(new GrammarError[errors.size()]);
	}

	TypeSystemException() {
		super("Unknown TypeSystem Exception");
	}

	TypeSystemException(String error) {
		super(error);
	}
	


	public TypeSystemException(List<GrammarError> errors) {
		super(Arrays.join(errors.toArray(), "\n"));
		this.errors = errors;
	}

	public TypeSystemException(GrammarError error) {
		super(error.getMessage());
		this.errors = new ArrayList<GrammarError>();
		this.errors.add(error);
	}

}
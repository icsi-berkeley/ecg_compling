// =============================================================================
// File        : ChainingCandidate.java
// Author      : emok
// Change Log  : Created on Apr 26, 2008
//=============================================================================

package compling.learner.candidates;

import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.Pair;

//=============================================================================

public class ChainingCandidate extends Pair<TypeConstraint, TypeConstraint> {

	private static final long serialVersionUID = 1L;

	public ChainingCandidate(TypeConstraint a, TypeConstraint b) {
		super(a, b);
	}

}

// =============================================================================
// File        : Substitution.java
// Author      : emok
// Change Log  : Created on Jul 5, 2007
//=============================================================================

package compling.learner.grammartables;

import java.util.HashMap;

import compling.util.Pair;

//=============================================================================

public class Substitution<T> extends HashMap<T, Pair<T, Double>> {
	private static final long serialVersionUID = 1L;
	private T sourceDomain;

	public Substitution(T sourceDomain) {
		super();
		this.sourceDomain = sourceDomain;
	}

	public Substitution(Substitution<T> s) {
		super(s);
		this.sourceDomain = s.getSourceDomain();
	}

	public void substitutes(T from, T to, Double cost) {
		put(from, new Pair<T, Double>(to, cost));
	}

	public T getSourceDomain() {
		return sourceDomain;
	}

}

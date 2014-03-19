// =============================================================================
// File        : ConstituentLookupTable.java
// Author      : emok
// Change Log  : Created on Apr 26, 2007
//=============================================================================

package compling.learner.grammartables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.LookupTable;
import compling.util.MapSet;

//=============================================================================

public class ConstituentLookupTable extends LookupTable<TypeConstraint, TypeConstraint> {

	private static final long serialVersionUID = -5804073642031199317L;

	protected Map<TypeConstraint, MapSet<TypeConstraint, Role>> constituentByType = null;
	private Grammar grammar;

	public ConstituentLookupTable(Grammar grammar) {
		super();
		this.grammar = grammar;
		constituentByType = new HashMap<TypeConstraint, MapSet<TypeConstraint, Role>>();
		for (Construction cxn : this.grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {
		TypeConstraint type = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		Set<Role> cxnConstituents = construction.getConstructionalBlock().getElements();
		for (Role cxnConstituent : cxnConstituents) {
			incrementCount(type, cxnConstituent.getTypeConstraint(), 1);
			addRole(type, cxnConstituent.getTypeConstraint(), cxnConstituent);
		}
	}

	public Set<Role> getConstituentsOfType(TypeConstraint cxnType, TypeConstraint cstType) {
		return constituentByType.get(cxnType).get(cstType);
	}

	protected void addRole(TypeConstraint a, TypeConstraint b, Role role) {
		if (constituentByType.get(a) == null) {
			constituentByType.put(a, new MapSet<TypeConstraint, Role>());
		}
		constituentByType.get(a).put(b, role);
	}
}

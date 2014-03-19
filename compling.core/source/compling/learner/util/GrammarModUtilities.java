// =============================================================================
// File        : GrammarModificationUtilities.java
// Author      : emok
// Change Log  : Created on May 9, 2008
//=============================================================================

package compling.learner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerGrammar;
import compling.learner.grammartables.ConstituentCooccurenceTable;
import compling.learner.grammartables.Substitution;
import compling.util.Pair;

//=============================================================================

public class GrammarModUtilities {

	public static Construction modifyParents(Grammar grammar, Construction cxn, Set<String> newParents) {

		// shallow cloning is fine for this because the old constructions will be gotten rid of
		Block constructionalBlock = cxn.getConstructionalBlock().clone();
		Block formBlock = cxn.getFormBlock().clone();
		Block meaningBlock = cxn.getMeaningBlock().clone();

		Construction newConstruction = grammar.new Construction(cxn.getName(), cxn.isConcrete() ? ECGConstants.CONCRETE
				: ECGConstants.ABSTRACT, newParents, formBlock, meaningBlock, constructionalBlock);

		newConstruction.setComplements(new HashSet<Role>(cxn.getComplements()));
		newConstruction.setOptionals(new HashSet<Role>(cxn.getOptionals()));
		newConstruction.setExtraPosedRole(cxn.getExtraPosedRole());

		for (Role r : constructionalBlock.getElements()) {
			r.setContainer(newConstruction);
		}

		return newConstruction;
	}

	public static Construction modifyConstituents(Grammar grammar, Construction cxn, String newCxnName,
			Map<Role, Pair<String, TypeConstraint>> replacements, Block replacementMeaningBlock) {

		Map<Role, Role> roleReplacements = new LinkedHashMap<Role, Role>();
		for (Role oldConstituent : replacements.keySet()) {
			String newConstituentName = replacements.get(oldConstituent).getFirst();
			TypeConstraint newType = replacements.get(oldConstituent).getSecond();
			Role newConstituent = new Role(newConstituentName);
			newConstituent.setTypeConstraint(newType);
			newConstituent.setSource(newCxnName);
			roleReplacements.put(oldConstituent, newConstituent);
		}
		return modifyRoles(grammar, cxn, newCxnName, roleReplacements, replacementMeaningBlock, true);
	}

	public static Construction modifyRoles(Grammar grammar, Construction cxn, String newCxnName,
			Map<Role, Role> replacements, Block replacementMeaningBlock, boolean areConstructionalConstituents) {
		Block constructionalBlock = cxn.getConstructionalBlock().clone(false);
		Block formBlock = cxn.getFormBlock().clone(false);
		Block meaningBlock = replacementMeaningBlock != null ? replacementMeaningBlock : cxn.getMeaningBlock().clone(
				false);

		List<Role> constituents = new ArrayList<Role>(constructionalBlock.getElements());
		// this is done purely for aesthetics -- to maintain the printing ordering of the constituents in form order

		Set<Role> complements = new HashSet<Role>(cxn.getComplements());
		Set<Role> optionals = new HashSet<Role>(cxn.getOptionals());
		Role extraposed = cxn.getExtraPosedRole();

		for (Role oldConstituent : replacements.keySet()) {
			Role newConstituent = replacements.get(oldConstituent);
			if (areConstructionalConstituents)
				constituents.set(constituents.indexOf(oldConstituent), newConstituent);
			modifyConstituent(constructionalBlock, oldConstituent, newConstituent);
			modifyConstituent(formBlock, oldConstituent, newConstituent);
			modifyConstituent(meaningBlock, oldConstituent, newConstituent);

			if (complements.remove(oldConstituent)) {
				complements.add(newConstituent);
			}
			if (optionals.remove(oldConstituent)) {
				optionals.add(newConstituent);
			}
			if (extraposed == oldConstituent) {
				extraposed = newConstituent;
			}
		}
		reannotateBlock(constructionalBlock, newCxnName);
		reannotateBlock(formBlock, newCxnName);
		reannotateBlock(meaningBlock, newCxnName);

		constructionalBlock.setElements(new LinkedHashSet<Role>(constituents));

		Construction newConstruction = grammar.new Construction(newCxnName, cxn.isConcrete() ? ECGConstants.CONCRETE
				: ECGConstants.ABSTRACT, cxn.getParents(), formBlock, meaningBlock, constructionalBlock);

		newConstruction.setComplements(complements);
		newConstruction.setOptionals(optionals);
		newConstruction.setExtraPosedRole(extraposed);

		for (Role r : constructionalBlock.getElements()) {
			r.setContainer(newConstruction);
		}

		return newConstruction;
	}

	public static void modifyConstituent(Block block, Role oldConstituent, Role newConstituent) {
		for (Constraint c : block.getConstraints()) {
			for (SlotChain sc : c.getArguments()) {
				int index = sc.getChain().indexOf(oldConstituent);
				if (index >= 0) {
					List<Role> chain = sc.getChain();
					chain.set(index, newConstituent);
					sc.setChain(chain);
				}
			}
		}
	}

	public static void reannotateBlock(Block block, String newSource) {
		block.setTypeSource(newSource);
		for (Role r : block.getElements()) {
			r.setSource(newSource);
		}
		for (Role r : block.getEvokedElements()) {
			r.setSource(newSource);
		}
		for (Constraint c : block.getConstraints()) {
			c.setSource(newSource);
		}
	}

	public static Map<Construction, Construction> renameEvokedRole(Construction construction, Role oldEvoked,
			Role newEvoked, LearnerGrammar learnerGrammar) {

		Map<Construction, Construction> changes = new LinkedHashMap<Construction, Construction>();

		if (oldEvoked.getName().equals(newEvoked.getName()))
			return changes;

		modifyConstituent(construction.getMeaningBlock(), oldEvoked, newEvoked);
		Map<Role, Role> replacements = new HashMap<Role, Role>();
		replacements.put(oldEvoked, newEvoked);

		TypeConstraint cxnType = learnerGrammar.getGrammar().getCxnTypeSystem()
				.getCanonicalTypeConstraint(construction.getName());
		ConstituentCooccurenceTable cooccurenceTable = learnerGrammar.getGrammarTables().getConstituentCooccurenceTable();
		cooccurenceTable.setQueryExpander(null);
		Map<Substitution<TypeConstraint>, Set<TypeConstraint>> coveringTypes = cooccurenceTable
				.findCoveringTypes(cxnType);
		for (Set<TypeConstraint> users : coveringTypes.values()) {
			for (TypeConstraint userType : users) {
				Construction user = learnerGrammar.getGrammar().getConstruction(userType.getType());
				Construction modUser = GrammarModUtilities.modifyRoles(learnerGrammar.getGrammar(), user, user.getName(),
						replacements, null, false);
				changes.put(user, modUser);
			}
		}

		return changes;
	}
}

package compling.grammar.ecg.morph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.parser.ecgparser.morph.MAnalysis;
import compling.parser.ecgparser.morph.MGrammarWrapper;

public class MGrammarChecker {

	static class UtilizationGraphNode {
		public String cxnName;
		public Map<String, UtilizationGraphNode> utilizingCxns;
		public Map<String, UtilizationGraphNode> constituents;

		public UtilizationGraphNode(String cxnName) {
			this.cxnName = cxnName;
			this.utilizingCxns = new HashMap<String, UtilizationGraphNode>();
			this.constituents = new HashMap<String, UtilizationGraphNode>();
		}
	}

	static class UtilizationGraph {
		Map<String, UtilizationGraphNode> allCxns;

		public UtilizationGraph() {
			allCxns = new HashMap<String, UtilizationGraphNode>();
		}

		public void addCxn(Construction cxn) {
			Set<Role> constits = cxn.getComplements();
			Set<String> constitTypes = new HashSet<String>();
			for (Role constit : constits) {
				constitTypes.add(constit.getTypeConstraint().getType());
			}
			addCxn(cxn.getName(), constitTypes);
		}

		public boolean utilizes(String utilizer, String constituent) {
			assert (allCxns.containsKey(utilizer));
			UtilizationGraphNode utilizerNode = allCxns.get(utilizer);
			if (utilizerNode.constituents.containsKey(constituent))
				return true;
			assert (allCxns.containsKey(constituent));
			return false;
		}

		private void addCxn(String name, Set<String> constitTypes) {
			UtilizationGraphNode node = getOrCreateNode(name);

			for (String constit : constitTypes) {
				UtilizationGraphNode n = getOrCreateNode(constit);
				if (!node.constituents.containsKey(constit))
					node.constituents.put(constit, n);
				if (!n.utilizingCxns.containsKey(name))
					n.utilizingCxns.put(name, node);
			}
		}

		private UtilizationGraphNode getOrCreateNode(String cxnName) {
			if (allCxns.containsKey(cxnName))
				return allCxns.get(cxnName);
			else {
				UtilizationGraphNode newNode = new UtilizationGraphNode(cxnName);
				allCxns.put(cxnName, newNode);
				return newNode;
			}
		}
	}

	/**
	 * Check that the grammar is legal with respect to morphology, and store a canonical analysis for each construction.
	 * 
	 * @param grammar
	 * @param errorLog
	 * @return True iff no fatal errors occurred
	 */
	public static boolean checkGrammar(MGrammarWrapper grammar) {
		StringBuffer errorLog = grammar.errorLog;
		boolean fatalError = false;
		/*
		 * UtilizationGraph utilizationGraph = new UtilizationGraph();
		 * 
		 * //////////////////////// // NONLEXICAL CXNS ////////////////////////
		 * 
		 * Collection<Construction> nonlexCxns = grammar.getAllConcreteNonLexicalConstructions(); for (Construction cxn :
		 * nonlexCxns) { utilizationGraph.addCxn(cxn);
		 * 
		 * // Build the chain of 'meets' constraints for this construction
		 * 
		 * List<ECGSlotChain> meetsChain = new ArrayList<ECGSlotChain>(); Set<Constraint> constraints =
		 * MGrammarWrapper.getAllFormConstraints(cxn); //Set<Constraint> meetsConstraints = new HashSet<Constraint>();
		 * 
		 * Set<Constraint> beforeConstraints = new HashSet<Constraint>(); HashMap<ECGSlotChain,Constraint>
		 * meetsConstraintByLeftOperand = new HashMap<ECGSlotChain,Constraint>(); HashMap<ECGSlotChain,Constraint>
		 * meetsConstraintByRightOperand = new HashMap<ECGSlotChain,Constraint>();
		 * 
		 * for (Constraint constraint : constraints) { if (constraint.overridden()) continue;
		 * 
		 * ECGSlotChain left = (ECGSlotChain)constraint.getArguments().get(0); ECGSlotChain right =
		 * (constraint.getArguments().size()>1) ? (ECGSlotChain)constraint.getArguments().get(1) : null;
		 * 
		 * 
		 * if (constraint.getOperator()==ECGConstants.MEETS || constraint.getOperator()==ECGConstants.BEFORE) { if
		 * (MGrammarWrapper.isDeep(left) || MGrammarWrapper.isDeep(right)) {
		 * errorLog.append("Invalid form constraint in cxn " + cxn.getName() + ": " + constraint.toString() +
		 * " (contains a deep constituent reference, i.e. refers to a constituent of a constituent, which is illegal)\n");
		 * fatalError = true; }
		 * 
		 * if (constraint.getOperator()==ECGConstants.MEETS) { if (meetsConstraintByLeftOperand.containsKey(left) ||
		 * meetsConstraintByRightOperand.containsKey(right)) { errorLog.append("Superfluous constraint in cxn " +
		 * cxn.getName() + ": " + constraint.toString()); fatalError = true; }
		 * 
		 * else { meetsConstraintByLeftOperand.put(left, constraint); meetsConstraintByRightOperand.put(right,
		 * constraint); } } else if (constraint.getOperator()==ECGConstants.BEFORE) { beforeConstraints.add(constraint); }
		 * 
		 * 
		 * 
		 * } else if (constraint.getOperator()==ECGConstants.IDENTIFY) { if
		 * (left.getChain().get(left.getChain().size()-1).getName().equals("f") ||
		 * right.getChain().get(right.getChain().size()-1).getName().equals("f")) {
		 * errorLog.append("WARNING: Form pole reference in binding constraint in cxn " + cxn.getName() + ": " +
		 * constraint.toString()); } } }
		 * 
		 * Constraint constraint = null; if (meetsConstraintByLeftOperand.size()>0) // Pick an arbitrary starting
		 * constraint constraint = meetsConstraintByLeftOperand.values().iterator().next();
		 * //System.out.println(constraint.getArguments().get(0).getChain().toString());
		 * 
		 * // Work rightward in the chain from the starting constraint while (constraint != null) {
		 * 
		 * ECGSlotChain leftOperand = (ECGSlotChain)constraint.getArguments().get(0); ECGSlotChain rightOperand =
		 * (ECGSlotChain)constraint.getArguments().get(1);
		 * 
		 * if (!meetsChain.isEmpty() && meetsChain.get(0).equals(leftOperand)) {
		 * errorLog.append("Cycle of 'meets' constraints involving " + leftOperand + " in cxn " + cxn.getName() + "\n");
		 * fatalError = true; } meetsChain.add(leftOperand);
		 * 
		 * 
		 * if (meetsConstraintByLeftOperand.containsKey(rightOperand)) { constraint =
		 * meetsConstraintByLeftOperand.get(rightOperand); continue; }
		 * 
		 * // RHS of last constraint meetsChain.add(rightOperand); break; }
		 * 
		 * if (meetsChain.size()>0) { // Work leftward in the chain from the starting constraint ECGSlotChain r0 =
		 * meetsChain.get(0); constraint = null; if (meetsConstraintByRightOperand.containsKey(r0)) constraint =
		 * meetsConstraintByRightOperand.get(r0); }
		 * 
		 * while (constraint != null) {
		 * 
		 * ECGSlotChain leftOperand = (ECGSlotChain)constraint.getArguments().get(0);
		 * 
		 * meetsChain.add(0, leftOperand);
		 * 
		 * 
		 * if (meetsConstraintByRightOperand.containsKey(leftOperand)) { constraint =
		 * meetsConstraintByRightOperand.get(leftOperand); continue; }
		 * 
		 * break;
		 * 
		 * }
		 * 
		 * 
		 * 
		 * // Check that all 'meets' constraints are in the chain if (meetsChain.size()>0 && meetsChain.size() !=
		 * meetsConstraintByLeftOperand.size()+1) { errorLog.append("WARNING: Disjoint 'meets' constraint(s) in cxn " +
		 * cxn.getName() + "\n"); errorLog.append(meetsChain + "\n"); meetsChain.clear(); // Not a complete 'meets' chain
		 * }
		 * 
		 * 
		 * 
		 * 
		 * //if (meetsChain.size()==0) // isContiguous = false;
		 * 
		 * // Verify that all 'before' constraints are obeyed by the 'meets' chain for (Constraint bConstraint :
		 * beforeConstraints) { String leftOperand = bConstraint.getArguments().get(0).toString(); String rightOperand =
		 * bConstraint.getArguments().get(1).toString();
		 * 
		 * if (leftOperand.equals(rightOperand)) { errorLog.append("WARNING: Recursive 'before' constraint in cxn " +
		 * cxn.getName() + ": " + bConstraint + "\n"); }
		 * 
		 * int iLHS = meetsChain.indexOf(leftOperand); int iRHS = meetsChain.indexOf(rightOperand); if (iLHS == -1) { //
		 * Left-hand side of 'before' constraint never realized in a 'meets' constraint //isContiguous = false;
		 * meetsChain.clear(); } if (iRHS == -1) { // Right-hand side of 'before' constraint never realized in a 'meets'
		 * constraint //isContiguous = false; meetsChain.clear(); } if (iLHS > -1 && iRHS > -1 && iLHS > iRHS) {
		 * errorLog.append("WARNING: 'before' constraint violated by 'meets' constraints in cxn " + cxn.getName() + ": " +
		 * bConstraint + "\n"); }
		 * 
		 * // TODO: Verify there are no cycles of 'before' constraints }
		 * 
		 * /// DISPLAY MEETS CHAIN (WILL BE EMPTY IF NONCONTIGUOUS) /*System.out.println("meets chain..."); for (String r
		 * : meetsChain) { System.out.println(r); } System.out.println(".");* /
		 * 
		 * 
		 * 
		 * 
		 * // Store a canonical (just-initialized) MorphAnalysis object for a morphological cxn // Copy this object when
		 * building up a candidate analysis grammar.addMorphConstruction(cxn, meetsChain); }
		 * 
		 * ///////////////////// // LEXICAL CXNS /////////////////////
		 * 
		 * Collection<Construction> cxns = grammar.getAllConcreteLexicalConstructions();
		 * //System.out.println("------------- LEXICAL CXNS ------------"); for (Construction cxn : cxns) {
		 * utilizationGraph.addCxn(cxn);
		 * 
		 * if (!grammar.isAtomic(cxn)) { // may be a cxn with multiple form roles and no form constraints. // TODO: What
		 * if it's a lexical cxn with multiple form roles and meets constraints? grammar.addMorphConstruction(cxn, new
		 * ArrayList<ECGSlotChain>()); continue; }
		 * 
		 * String orthValue = null; orthValue = ECGGrammarUtilities.getLexemeFromLexicalConstruction(cxn);
		 * 
		 * /* try { if
		 * (!cxn.getCxnTypeSystem().getAllSuperTypes(cxn.getName().toString()).contains(MGrammarWrapper.MWORDCXN)) {
		 * System.err.println("WARNING: Lexical construction " + cxn.getName() +
		 * " is not a subtype of WLMorph, and thus will not be licensed in any word analysis"); } }
		 * catch(compling.grammar.unificationgrammar.TypeSystemException ex) { ex.printStackTrace(); } /
		 * 
		 * if (orthValue != null) { orthValue = MGrammarWrapper.unescapeECGStringLiteral(orthValue);
		 * 
		 * grammar.addLexicalConstruction(cxn, orthValue); } }
		 */

		// ///////////////////
		// MORE CHECKING
		// ///////////////////

		// Verify no literal assignment to *.f
		for (Construction c : grammar.getAllConstructions()) {
			for (Constraint cst : MGrammarWrapper.getAllFormConstraints(c)) {
				if (cst.getOperator().equals(ECGConstants.ASSIGN) && cst.getArguments().get(0).toString().endsWith(".f")) {
					errorLog.append("Literal assignment to a form pole is illegal: " + cst.toString() + " in construction "
							+ c.getName() + "\n");
					fatalError = true;
				}
			}
		}
		if (fatalError)
			return false;

		Set<Construction> noncontigCxns = grammar.getAllConcreteNoncontiguousConstructions();
		Set<String> noncontigCxnNames = new HashSet<String>();
		for (Construction c : noncontigCxns) {
			noncontigCxnNames.add(c.getName());
		}
		{ // Make a note of how many noncontiguous cxns are in the grammar
			int nn = noncontigCxns.size();
			if (nn > 0)
				errorLog.append("NOTE: The grammar has " + nn + " concrete noncontiguous morphological construction"
						+ ((nn == 1) ? "" : "s") + ": " + noncontigCxnNames + "\n");
		}
		// Verify that the Contiguity Assumption cannot be violated: i.e. x.f is never present in a
		// meets chain if 'x' can be a noncontiguous constituent
		for (String cxn : grammar.getMorphConstructionNames()) {
			if (!grammar.isContiguous(cxn)) {
				for (Constraint cst : MGrammarWrapper.getAllFormConstraints(grammar.getConstruction(cxn))) {
					if (!cst.overridden()
							&& (cst.getOperator() == ECGConstants.BEFORE || cst.getOperator() == ECGConstants.MEETS)
							&& (cst.getArguments().get(0).toString().equals("self.f") || cst.getArguments().get(1).toString()
									.equals("self.f"))) {
						errorLog.append("self.f not allowed as a form component in noncontiguous construction " + cxn + ": "
								+ cst.toString() + "\n");
						fatalError = true;
					}
				}
			}
			else {
				for (ECGSlotChain r : grammar.getMeetsChain(cxn)) {
					if ((r.startsWithSelf() && r.getChain().get(0).getName().equals(ECGConstants.FORM_POLE)) // self.f(.*)
							|| !r.getChain().get(r.getChain().size() - 1).getName().equals(ECGConstants.FORM_POLE)) // *.f
						continue;

					ECGSlotChain r2 = r;
					Construction cxn2 = grammar.getConstruction(cxn);

					// while ((idot = r2.toString().indexOf(".")) < r2.toString().length()-2) { // r2 ends with .f
					while (r2.getChain().size() > 1) { // r2 ends with .f
						cxn2 = grammar.getConstruction(new MAnalysis(cxn2, grammar).getFeatureStructure()
								.getSlot(r2.subChain(0, 1)).getTypeConstraint().getType());
						// r2 = r2.substring(idot+1);
						r2 = r2.subChain(1);
					}
					// String type = morphCxns.get(cxn).getAnalysis().getFeatureStructure().getSlot(new
					// ECGSlotChain(r.substring(0,r.length()-2))).getTypeConstraint().getType();
					String type = new MAnalysis(cxn2, grammar).getFeatureStructure()
							.getSlot(r2.subChain(0, r2.getChain().size() - 1)).getTypeConstraint().getType();
					List<String> subtypes = grammar.getConcreteSubtypes(type);
					for (String subtype : subtypes) {
						if (!grammar.isContiguous(subtype)) {
							errorLog.append("WARNING: A form component of construction " + cxn
									+ " may be the form pole of a noncontiguous construction (" + subtype + "): " + r
									+ " in a meets constraint has static type " + type + "\n");
						}
					}
				}
			}
		}

		// Verify that there are no literal assignments or 'meets' constraints involving form roles of a construction's
		// constituents
		// which have not been assigned locally by the constituent
		// TODO: This hasn't been tested for cases where the form role is filled by a schema, e.g. constit.f.x.y <-- "z"
		// (might produce too many or too few warnings for such constraints)
		for (String cxn : grammar.getMorphConstructionNames()) {
			Construction c = grammar.getConstruction(cxn);
			for (Constraint fcst : c.getFormBlock().getConstraints()) {
				if (!fcst.overridden() && (fcst.isAssign() || fcst.getOperator() == ECGConstants.MEETS)
						&& !fcst.getArguments().get(0).getChain().get(0).toString().equals(ECGConstants.FORM_POLE)) {
					// OK iff this serves a matching function, i.e. there is an assignment to that role in the constituent
					// filler
					for (SlotChain arg : fcst.getArguments()) {
						if (arg.getChain().size() > 2) { // refers to a form role
							Role constitRole = arg.getChain().get(0); // .f is at [1]
							Role formRole = arg.getChain().get(2);
							String tc = constitRole.getTypeConstraint().getType();
							List<String> constitFillers = grammar.getConcreteSubtypes(tc);
							for (String constitFiller : constitFillers) {
								Construction constit = grammar.getConstruction(constitFiller);
								boolean assignedRoleLocally = false;
								for (Constraint fcst2 : constit.getFormBlock().getConstraints()) {
									if (!fcst2.overridden()
											&& fcst2.isAssign()
											&& fcst2.getArguments().get(0).getChain().get(0).toString()
													.equals(ECGConstants.FORM_POLE)
											&& fcst2.getArguments().get(0).getChain().get(1).toString()
													.equals(formRole.toString())) {
										assignedRoleLocally = true;
										break;
									}
								}
								if (!assignedRoleLocally) {
									errorLog.append("WARNING: Form role referenced in " + cxn + " not assigned locally in "
											+ constitFiller + ": " + arg + " in '" + fcst.toString() + "'\n");
								}
							}
						}
					}
				}
			}
		}

		if (fatalError)
			return false;

		return true;
	}
}

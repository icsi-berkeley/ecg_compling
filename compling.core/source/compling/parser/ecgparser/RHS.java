package compling.parser.ecgparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.util.Counter;

/**
 * class tracks all the form constraints. It replaces the old POGraph class from the old analyzer
 * 
 * It is used by the LeftCornerParserTables class to preprocess the constituents needed.
 */
public final class RHS {

	public Counter<Role> inLinks;
	public HashMap<Role, List<Constraint>> formConstraints;
	private HashMap<Role, Role> roleCanonicalizer = new HashMap<Role, Role>();
	private Set<Role> leftOverConstituents = new HashSet<Role>();
	private Set<Role> leftOverComplements = new HashSet<Role>();
	private Set<Role> leftOverOptionals = new HashSet<Role>();
	private HashMap<Role, List<Constraint>> reverseFormConstraints;
	private Construction headCxn;
	private HashMap<Role, Role> meetsTable = new HashMap<Role, Role>();
	private Role lastSymbolMatched = null;

	public RHS(Construction head, Set<Role> complements, Set<Role> optionals, Set<Constraint> constraints) {
		this.headCxn = head;
		inLinks = new Counter<Role>();
		formConstraints = new HashMap<Role, List<Constraint>>();
		reverseFormConstraints = new HashMap<Role, List<Constraint>>();
		leftOverComplements.addAll(complements);
		leftOverOptionals.addAll(optionals);
		HashMap<Role, Role> reverseMeetsTable = new HashMap<Role, Role>();
		for (Role role : complements) {
			// System.out.println(role);
			inLinks.setCount(role, 0);
			formConstraints.put(role, new ArrayList<Constraint>());
			reverseFormConstraints.put(role, new ArrayList<Constraint>());
			roleCanonicalizer.put(role, role);
		}
		for (Role role : optionals) {
			inLinks.setCount(role, 0);
			formConstraints.put(role, new ArrayList<Constraint>());
			reverseFormConstraints.put(role, new ArrayList<Constraint>());
			roleCanonicalizer.put(role, role);
		}
		constraints = new HashSet<Constraint>(constraints);
		for (Constraint constraint : constraints) {
			if (constraint.getOperator().equals(ECGConstants.BEFORE) == false
					&& constraint.getOperator().equals(ECGConstants.MEETS) == false) {
				throw new GrammarException("Analysis.RHS does not support a " + constraint.getOperator() + " constraint: "
						+ constraint.toString());
			}
		}
		for (Constraint constraint : constraints) {
			if (constraint.getOperator().equals(ECGConstants.MEETS)) {
				Role lhs = getArg(0, constraint);
				Role rhs = getArg(1, constraint);
				if (meetsTable.containsKey(lhs) || reverseMeetsTable.containsKey(rhs)) {
					throw new GrammarException("Competing meets constraints involving constituent " + lhs + " or " + rhs
							+ " in construction " + head.getName());
				}
				meetsTable.put(lhs, rhs);
				reverseMeetsTable.put(rhs, lhs);
			}
		}
		List<Constraint> toRemove = new ArrayList<Constraint>();
		List<Constraint> toAdd = new ArrayList<Constraint>();
		for (Constraint constraint : constraints) {
			if (constraint.getOperator().equals(ECGConstants.BEFORE) && meetsTable.containsValue(getArg(1, constraint))) {
				Role lhs = reverseMeetsTable.get(getArg(1, constraint));
				while (reverseMeetsTable.containsKey(lhs)) {
					lhs = reverseMeetsTable.get(lhs);
				}
				toRemove.add(constraint);
				List<Role> chain = new ArrayList(constraint.getArguments().get(1).getChain());
				chain.set(0, lhs);
				SlotChain newSC = new SlotChain();
				newSC.setChain(chain);
				Constraint newC = new Constraint(constraint.getOperator(), constraint.getArguments().get(0), newSC);
				// constraint.getArguments().set(1, new SlotChain(lhs.getName()+".f"));
				// System.out.println("old constraint: "+constraint.toString());
				if (getArg(0, newC).equals(getArg(1, newC))) {
					// System.out.println("Not adding new constraint: "+newC.toString());
				}
				else {
					toAdd.add(newC);
					// System.out.println("new constraint: "+newC.toString());
				}
			}
		}
		int constraintSetSize = constraints.size();
		for (Constraint c : toRemove) {
			constraints.remove(c);
		}
		if (constraints.size() != constraintSetSize - toRemove.size()) {
			throw new GrammarException("Bad set size count");
		}
		for (Constraint c : toAdd) {
			constraints.add(c);
		}

		for (Constraint constraint : constraints) {
			Role lhs = getArg(0, constraint);
			Role rhs = getArg(1, constraint);
			boolean found = false;
			for (Constraint c : formConstraints.get(lhs)) {
				if (constraint.equals(c)) {
					found = true;
				}
			}
			if (!found) {
				formConstraints.get(lhs).add(constraint);
				reverseFormConstraints.get(rhs).add(constraint);

				if (constraint.getOperator().equals(ECGConstants.BEFORE)) {
					inLinks.incrementCount(rhs, 1);
				}
			}
		}

		leftOverConstituents = new HashSet<Role>();
		leftOverConstituents.addAll(leftOverComplements);
		leftOverConstituents.addAll(leftOverOptionals);
		for (Role role : reverseMeetsTable.keySet()) {
			inLinks.removeKey(role);
		}
	}

	public List<Constraint> getFormConstraintsUsingRightArgument(Role role) {
		return reverseFormConstraints.get(role);
	}

	public Set<Role> getLeftOverConstituents() {
		return leftOverConstituents;
	}

	// if the last matched constituent was the lhs of a meets constraint, this returns the
	// rhs of the meets constraint
	// otherwise, it returns a constituent with inLinks == 0
	// if more than one such constituent exists, the choice is nondeterministic
	// returns null if no such next constituent
	public Role getNextSymbol() {
		if (lastSymbolMatched == null || meetsTable.get(lastSymbolMatched) == null) {
			Set<Role> next = inLinks.getKeysWithCount(0);
			if (next.size() == 0) {
				if (inLinks.size() != 0) {
					throw new GrammarException("Fatal error. \n" + inLinks.toString() + "\n" + formConstraints.toString()
							+ "\n" + meetsTable.toString());
				}
				return null;
			}
			for (Role role : next) {
				return role;
			}
		}
		else { // lastSymbolMatched != null and meetsTable has it
			return meetsTable.get(lastSymbolMatched);
		}
		return null;
	}

	public void advance(Role role) {
		// should I put a check in here to make sure that no non-next roles are advanced?
		// System.out.println(inLinks);
		if (leftOverComplements.contains(role)) {
			leftOverComplements.remove(role);
		}
		else if (leftOverOptionals.contains(role)) {
			leftOverOptionals.remove(role);
		}
		else {
			throw new GrammarException("advancing non existent role:" + role.getName());
		}
		inLinks.removeKey(role);
		for (Constraint constraint : formConstraints.get(role)) {
			if (constraint.getOperator().equals(ECGConstants.BEFORE)) {
				inLinks.incrementCount(getArg(1, constraint), -1);
			}
		}
		leftOverConstituents.remove(role);
		// System.out.println(inLinks);
		lastSymbolMatched = role;
	}

	Set<Role> getLeftOverComplements() {
		return leftOverComplements;
	}

	boolean foundAllComplements() {
		return leftOverComplements.size() == 0;
	}

	boolean foundAllOptionals() {
		return leftOverOptionals.size() == 0;
	}

	public Role getArg(int arg, Constraint constraint) {
		return roleCanonicalizer.get(constraint.getArguments().get(arg).getChain().get(0));
	}
}

package compling.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import compling.context.ContextException.ItemNotDefinedException;
import compling.context.ContextUtilities.QueryResultPrinter;
import compling.context.MiniOntology.Individual;
import compling.context.MiniOntology.Interval;
import compling.context.MiniOntology.RelationFiller;
import compling.context.MiniOntology.Type;

public class MiniOntologyQueryAPI {

	/*
	 * Eva's latest edits: 1) interned strings obtained from TypeSystem automatically; 2) additional methods created to
	 * allow querying on current interval and all intervals 3) unsatisfied queries return null instead of generating
	 * exceptions
	 */

	static QueryResultPrinter printer = new QueryResultPrinter();

	static final char QUERYPREFIX = '?';

	protected static boolean isQueryVariable(String i) {
		return i.charAt(0) == QUERYPREFIX;
	}

	static Individual getIndividual(MiniOntology m, SimpleQuery s, boolean queryAllIntervals) {
		if (s.args.size() != 2) {
			throw new ContextException("Incorrect number of arguments supplied in SimpleQuery " + s);
		}
		return getIndividual(m, s.args.get(0), s.args.get(1), queryAllIntervals);
	}

	public static String retrieveIndividual(MiniOntology m, SimpleQuery s) {
		return retrieveIndividual(m, s, false);
	}

	public static String retrieveIndividual(MiniOntology m, SimpleQuery s, boolean queryAllIntervals) {
		if (s.args.size() != 2) {
			throw new ContextException("Incorrect number of arguments supplied in SimpleQuery " + s);
		}
		Individual ind = getIndividual(m, s.args.get(0), s.args.get(1), queryAllIntervals);
		if (ind == null) {
			return null;
		}
		else {
			return printer.format(ind);
		}
	}

	protected static Individual getIndividual(MiniOntology m, String individualArg, String typeArg,
			boolean queryAllIntervals) {
		String internedType = typeArg == null ? null : m.getTypeSystem().getInternedString(typeArg);
		List<Individual> candidates = new ArrayList<Individual>();
		List<Individual> individuals = new ArrayList<Individual>();

		if (individualArg.equals(MiniOntology.CURRENT_INTERVAL)) {
			candidates.add(m.getCurrentInterval());
		}
		else if (individualArg.equals(MiniOntology.PRIOR_INTERVAL)) {
			candidates.add(m.getCurrentInterval().getPrior());
		}
		else {
			if (queryAllIntervals) {
				for (Interval interval : m.getAllIntervals().values()) {
					candidates.add(interval.getIndividual(individualArg));
				}
			}
			else {
				candidates.add(m.getCurrentInterval().getIndividual(individualArg));
			}
		}

		for (Individual ind : candidates) {
			if (ind != null && (internedType == null || m.subtype(ind.getTypeName(), internedType))) {
				individuals.add(ind);
			}
		}

		if (individuals.size() == 0) {
			return null;
		}
		else {
			return individuals.get(0);
		}
	}

	public static List<HashMap<String, String>> individualQuery(MiniOntology m, String individualArg, String typeArg,
			List<HashMap<String, String>> inputBindings) {
		return individualQuery(m, individualArg, typeArg, inputBindings, false);
	}

	protected static List<HashMap<String, String>> individualQuery(MiniOntology m, String individualArg, String typeArg,
			List<HashMap<String, String>> inputBindings, boolean queryAllIntervals) {

		if (inputBindings == null) { // || m.getTypeSystem().get(typeArg) == null
			return null; // a previously unsatisfiable query messed us up
		}

		List<HashMap<String, String>> bindings = new ArrayList<HashMap<String, String>>();

		if (!isQueryVariable(individualArg) && !isQueryVariable(typeArg)) {
			if (getIndividual(m, individualArg, typeArg, queryAllIntervals) == null) {
				return null;
			}
			// otherwise since there are no variables, nothing needs to be done

		}
		else if (isQueryVariable(individualArg) && !isQueryVariable(typeArg)) {

			// NOTE: this won't return the current interval if an interval type is queried and queryAllIntervals is not
			// true
			// when the variable is the individual
			typeArg = m.getTypeSystem().getInternedString(typeArg);
			List<Individual> individuals = new ArrayList<Individual>();
			if (queryAllIntervals) {
				for (Interval interval : m.getAllIntervals().values()) {
					individuals.addAll(interval.getAllIndividualsOfType(typeArg));
				}
			}
			else {
				individuals.addAll(m.getCurrentInterval().getAllIndividualsOfType(typeArg));
			}

			if (individuals.size() == 0) {
				return null;
			}

			for (Individual i : individuals) {
				if (inputBindings.size() == 0) {
					HashMap<String, String> newBinding = new HashMap<String, String>();
					newBinding.put(individualArg, printer.format(i));
					bindings.add(newBinding);
				}
				else {
					for (HashMap<String, String> inputBinding : inputBindings) {
						if (inputBinding.get(individualArg) == null) {
							HashMap<String, String> newBinding = (HashMap<String, String>) inputBinding.clone();
							newBinding.put(individualArg, printer.format(i));
							bindings.add(newBinding);
						}
						else if (inputBinding.get(individualArg) != null
								&& inputBinding.get(individualArg).equals(printer.format(i))) {
							bindings.add(inputBinding);
						}
					}
				}
			}

		}
		else if (!isQueryVariable(individualArg) & isQueryVariable(typeArg)) {

			Set<Type> types = new HashSet<Type>();

			if (individualArg.equals(MiniOntology.CURRENT_INTERVAL)) {
				types.add(m.getCurrentInterval().getType());
			}
			else if (individualArg.equals(MiniOntology.PRIOR_INTERVAL) && m.getCurrentInterval().getPrior() != null) {
				types.add(m.getCurrentInterval().getPrior().getType());
			}
			else {
				if (queryAllIntervals) {
					for (Interval interval : m.getAllIntervals().values()) {
						Individual ind = interval.getIndividual(individualArg);
						if (ind != null) {
							types.add(ind.getType());
						}
					}
				}
				else {
					Individual ind = m.getCurrentInterval().getIndividual(individualArg);
					if (ind != null) {
						types.add(ind.getType());
					}
				}
			}

			if (types.size() == 0) {
				return null;
			}

			for (Type t : types) {
				if (inputBindings.size() == 0) {
					HashMap<String, String> newBinding = new HashMap<String, String>();
					newBinding.put(typeArg, printer.format(t));
					bindings.add(newBinding);
				}
				else {
					for (HashMap<String, String> inputBinding : inputBindings) {
						if (inputBinding.get(typeArg) == null) {
							HashMap<String, String> newBinding = (HashMap<String, String>) inputBinding.clone();
							newBinding.put(typeArg, printer.format(t));
							bindings.add(newBinding);
						}
						else if (inputBinding.get(typeArg) != null && inputBinding.get(typeArg).equals(printer.format(t))) {
							bindings.add(inputBinding);
						}
					}
				}
			}

		}
		else { // FIXME: free for all?

		}
		return bindings;
	}

	public static List<HashMap<String, String>> relationFillerQuery(MiniOntology m, String relationName,
			String holderArg, String valueArg, List<HashMap<String, String>> inputBindings) {
		return relationFillerQuery(m, relationName, holderArg, valueArg, inputBindings, false);
	}

	private static void addFillers(Interval interval, String holderArg, String valueArg, MiniOntology m,
			List<RelationFiller> relationFillers, Set<String> relationNames) {
		for (String key : relationNames) {
			for (RelationFiller rf : interval.getAllRelationFillers(key)) {
				if ((isQueryVariable(holderArg) || (!isQueryVariable(holderArg) && rf.getHolder().getName()
						.equals(holderArg)))
						&& (isQueryVariable(valueArg) || (!isQueryVariable(valueArg) && rf.getFiller().getName()
								.equals(valueArg)))) {
					relationFillers.add(rf);
				}
			}
		}
	}

	public static List<HashMap<String, String>> relationFillerQuery(MiniOntology m, String relationName,
			String holderArg, String valueArg, List<HashMap<String, String>> inputBindings, boolean queryAllIntervals) {

		if (holderArg.equals(MiniOntology.CURRENT_INTERVAL)) {
			holderArg = m.getCurrentInterval().getName();
		}
		else if (holderArg.equals(MiniOntology.PRIOR_INTERVAL)) {
			if (m.getCurrentInterval().getPrior() == null) {
				throw new ItemNotDefinedException(
						"The prior interval is found to be null while performing a relation filler query on PRIORINTERVAL");
			}
			holderArg = m.getCurrentInterval().getPrior().getName();
		}

		if (inputBindings == null) {
			return null;
		} // a previously unsatisfiable query messed us up

		if (!isQueryVariable(valueArg) && !isQueryVariable(holderArg) && !isQueryVariable(relationName)) {
			// if there are no variables in the query...
			if (holds(m, new SimpleQuery(relationName, holderArg, valueArg))) {
				return inputBindings;
			}
			else {
				return null; // unsatisfiable
			}
		}

		List<RelationFiller> relationFillers = new ArrayList<RelationFiller>();;
		if (!isQueryVariable(relationName)) {
			Set<String> tempSet = new HashSet<String>();
			tempSet.add(relationName);
			if (queryAllIntervals) {
				for (Interval interval : m.getAllIntervals().values()) {
					addFillers(interval, holderArg, valueArg, m, relationFillers, tempSet);
				}
			}
			else {
				addFillers(m.getCurrentInterval(), holderArg, valueArg, m, relationFillers, tempSet);
			}
		}
		else {
			if (queryAllIntervals) {
				for (Interval interval : m.getAllIntervals().values()) {
					addFillers(interval, holderArg, valueArg, m, relationFillers, m.relations.keySet());
				}
			}
			else {
				addFillers(m.getCurrentInterval(), holderArg, valueArg, m, relationFillers, m.relations.keySet());
			}
		}
		// System.out.println("got relationfillers: "+relationFillers);

		if (relationFillers.size() == 0) { // unsatisfiable
			// return inputBindings;
			return null;
			// Eva's note: not sure why sometimes null is returned when it's unsatisfiable, and sometimes an empty list.
			// This was changed from empty list to null in Johno's last edit on July 21, but it seems that the null
			// is important for enforcing satisfiability when a complex query is done.
			// So for consistency reason, I'm changing it back to null, and making the ask method deal with the null result
		}

		List<HashMap<String, String>> resultBindings = new ArrayList<HashMap<String, String>>();

		for (RelationFiller rf : relationFillers) {
			if (inputBindings.size() == 0) {
				resultBindings.add(incorporateVariables(new HashMap<String, String>(), rf, relationName, holderArg,
						valueArg));
			}
			for (HashMap<String, String> inputBinding : inputBindings) {
				if ((!inputBinding.containsKey(holderArg) || (inputBinding.containsKey(holderArg) && printer.format(
						rf.getHolder()).equals(inputBinding.get(holderArg))))
						&& (!inputBinding.containsKey(valueArg) || (inputBinding.containsKey(valueArg) && printer.format(
								rf.getFiller()).equals(inputBinding.get(valueArg))))
						&& (!inputBinding.containsKey(relationName) || (inputBinding.containsKey(relationName) && printer
								.format(rf.getRelation()).equals(inputBinding.get(relationName))))) {

					resultBindings.add(incorporateVariables((HashMap<String, String>) inputBinding.clone(), rf,
							relationName, holderArg, valueArg));
				}
			}
		}
		return resultBindings;
	}

	private static HashMap<String, String> incorporateVariables(HashMap<String, String> newBinding, RelationFiller rf,
			String relationName, String holderArg, String valueArg) {
		if (isQueryVariable(relationName) && !newBinding.containsKey(relationName)) {
			newBinding.put(relationName, printer.format(rf.getRelation()));
		}
		if (isQueryVariable(holderArg) && !newBinding.containsKey(holderArg)) {
			newBinding.put(holderArg, printer.format(rf.getHolder()));
		}
		if (isQueryVariable(valueArg) && !newBinding.containsKey(valueArg)) {
			newBinding.put(valueArg, printer.format(rf.getFiller()));
		}
		return newBinding;
	}

	public static boolean holds(MiniOntology m, SimpleQuery s) {
		return holds(m, s, m.getCurrentInterval());
	}

	public static boolean holds(MiniOntology m, SimpleQuery s, String interval) {
		Interval queryInterval = m.getInterval(interval);
		if (queryInterval == null) {
			throw new ContextException("Interval queried is not defined :" + interval);
		}
		return holds(m, s, queryInterval);
	}

	static boolean holds(MiniOntology m, SimpleQuery s, Interval interval) {
		if (s.args.size() == 2) {
			Individual ind = null;

			if (s.args.get(0).equals(MiniOntology.CURRENT_INTERVAL)) {
				ind = m.getCurrentInterval();
			}
			else if (s.args.get(0).equals(MiniOntology.PRIOR_INTERVAL)) {
				ind = m.getCurrentInterval().getPrior();
			}
			else {
				ind = interval.getIndividual(s.args.get(0));
			}
			if (ind == null) {
				throw new ItemNotDefinedException("Query attempted on a nonexistent individual :" + s.args.get(0),
						s.args.get(0));
			}
			return m.subtype(ind.getTypeName(), m.getTypeSystem().getInternedString(s.args.get(1)));
		}
		else {
			return interval.existingRelationFiller(s.args.get(0), s.args.get(1), s.args.get(2));
		}
	}

	public static class SimpleQuery {

		public static final String INDIVIDUALQUERY = "INDIVIDUALQUERY";
		public static final String RELATIONFILLERQUERY = "RELATIONFILLERQUERY";
		String kind;
		List<String> args;

		public SimpleQuery(List<String> args) {
			this.args = args;
			if (args.size() == 2) {
				kind = INDIVIDUALQUERY;
			}
			else {
				kind = RELATIONFILLERQUERY;
			}
		}

		public SimpleQuery(String indName, String type) {
			args = new ArrayList<String>();
			args.add(indName);
			args.add(type);
			kind = INDIVIDUALQUERY;
		}

		public SimpleQuery(String relnName, String holder, String value) {
			args = new ArrayList<String>();
			args.add(relnName);
			args.add(holder);
			args.add(value);
			kind = RELATIONFILLERQUERY;
		}

		public String toString() {
			return "(" + kind + " " + args.toString() + " )";
		}
	}

	public static List<HashMap<String, String>> ask(MiniOntology m, List<SimpleQuery> simpleQueries) {
		return ask(m, simpleQueries, false);
	}

	public static List<HashMap<String, String>> ask(MiniOntology m, List<SimpleQuery> simpleQueries,
			boolean queryAllIntervals) {

		List<HashMap<String, String>> bindings = new ArrayList<HashMap<String, String>>();
		for (SimpleQuery q : simpleQueries) {
			// System.out.println("bindings: "+bindingsToString(bindings));
			if (q.kind == SimpleQuery.INDIVIDUALQUERY) {
				bindings = individualQuery(m, q.args.get(0), q.args.get(1), bindings, queryAllIntervals);
			}
			else {
				bindings = relationFillerQuery(m, q.args.get(0), q.args.get(1), q.args.get(2), bindings, queryAllIntervals);
			}
		}
		return bindings != null ? bindings : new ArrayList<HashMap<String, String>>();
	}

	public static String complexQueryToString(List<SimpleQuery> q) {
		if (q == null || q != null && q.size() == 0) {
			return "Empty Query";
		}
		else if (q.size() == 1) {
			return q.get(0).toString();
		}
		else {
			String ret = "(";
			for (int i = 0; i < q.size(); i++) {
				if (i > 0) {
					ret = ret + " && ";
				}
				ret = ret + q.get(i).toString();
			}
			ret = ret + " )";
			return ret;
		}
	}

	public static String bindingsToString(List<HashMap<String, String>> bindingsList) {
		String ret = "";
		for (HashMap<String, String> bindings : bindingsList) {
			ret = ret + "\tBinding Set:";
			for (String key : bindings.keySet()) {
				ret = ret + ("\n\t\t" + key + ": " + bindings.get(key));
			}
			ret = ret + "\n";
		}
		return ret;
	}

	public static void printResult(MiniOntology m, List<SimpleQuery> simpleQueries) {
		System.out.println("Query: " + complexQueryToString(simpleQueries));
		System.out.println(bindingsToString(ask(m, simpleQueries)));
	}
}

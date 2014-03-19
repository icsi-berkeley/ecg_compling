// =============================================================================
// File        : Script.java
// Author      : emok
// Change Log  : Created on Aug 18, 2007
//=============================================================================

package compling.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import compling.annotation.childes.FeatureBasedEntity;
import compling.annotation.childes.FeatureBasedEntity.FillerType;
import compling.annotation.childes.FeatureBasedEntity.SimpleFeatureBasedEntity;
import compling.simulator.Simulator.ScriptLocalizer;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class SimulationParameters {

	public static class Value {
		String filler;
		boolean isValue;
		boolean isFunc;

		public Value(String filler, boolean isValue) {
			this.filler = filler;
			this.isValue = isValue;

			if (isValue) {
				isFunc = false;
			}
			else {
				isFunc = filler.contains("(");
			}
		}

		public boolean isValue() {
			return isValue;
		}

		public boolean isRef() {
			return !(isValue || isFunc);
		}

		public boolean isFunc() {
			return isFunc;
		}

		public String getFiller() {
			return filler;
		}

		public String toString() {
			String type = isValue ? "value" : "ref";
			return filler + "[" + type + "]";
		}
	}

	public static class BindingSet extends ArrayList<MapSet<String, Value>> {

		private static final long serialVersionUID = 6037036086501230189L;

		public BindingSet() {
			super();
		}

		public BindingSet(List<HashMap<String, Value>> values) {
			super();
			for (HashMap<String, Value> value : values) {
				this.add(new MapSet<String, Value>(value));
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (MapSet<String, Value> map : this) {
				sb.append("configuration ").append(this.indexOf(map)).append(": ").append("\n");
				for (Set<Value> values : map.values()) {
					for (Value value : values) {
						sb.append(value).append("\n");
					}
				}
			}
			return sb.toString();
		}
	}

	MapSet<String, Value> assignments;
	Map<String, BindingSet> assignmentConstraints;
	static int counter = 0;
	private ScriptLocalizer localizer;

	public SimulationParameters() {
		assignments = new MapSet<String, Value>();
		assignmentConstraints = new HashMap<String, BindingSet>();
	}

	public SimulationParameters(SimpleFeatureBasedEntity features) {
		this(features, null);
	}

	public SimulationParameters(SimpleFeatureBasedEntity features, ScriptLocalizer localizer) {
		this();
		this.localizer = localizer;
		setParameters(features);
	}

	protected void setParameters(FeatureBasedEntity<Pair<String, FillerType>> features) {
		Set<String> roles = features.getRoles();
		for (String role : roles) {
			Set<Pair<String, FillerType>> fillers = features.getBinding(role);
			for (Pair<String, FillerType> filler : fillers) {
				if (filler.getSecond().equals(FillerType.VALUE) && localizer != null) {
					assignments.put(role, new Value(localizer.getScriptLocalization(filler.getFirst()), true));
				}
				else {
					assignments.put(role, new Value(filler.getFirst(), filler.getSecond().equals(FillerType.VALUE)));
				}
			}
		}
	}

	public void addParameter(String variable, Value value) {
		removeInvalidBindingConstraints(variable);
		assignments.put(variable, value);
	}

	public Set<Value> getParameter(String feature) {
		return assignments.get(feature);
	}

	public Set<String> getAllFeatures() {
		return assignments.keySet();
	}

	public boolean hasAssignmentConstraint(String feature) {
		return assignmentConstraints.containsKey(feature);
	}

	public BindingSet getAssignmentConstraint(String feature) {
		return assignmentConstraints.get(feature);
	}

	public void addAssignmentConstraint(BindingSet bindingSet) {
		for (String variable : bindingSet.get(0).keySet()) {
			removeInvalidBindingConstraints(variable);
		}

		if (bindingSet.get(0).keySet().size() > 1) {
			// if the bindingSet only contains values for one variable,
			// then it isn't really an assignment constraint.
			for (String variable : bindingSet.get(0).keySet()) {
				assignmentConstraints.put(variable, bindingSet);
			}
		}

		for (MapSet<String, Value> constraint : bindingSet) {
			for (String variable : constraint.keySet()) {
				assignments.putAll(variable, constraint.get(variable));
			}
		}
	}

	protected void removeInvalidBindingConstraints(String variable) {
		BindingSet invalidBindingSet = assignmentConstraints.get(variable);
		if (invalidBindingSet != null) {
			for (String otherVar : invalidBindingSet.get(0).keySet()) {
				assignmentConstraints.remove(otherVar);
			}
		}
	}

	// /-------------------------------------------------------------------------
	/**
	 * 
	 * Generate allowable instantiations for the supplied variables, taking into account assignment constraints. This
	 * assumes that all the variables has been evaluated into ground literals (i.e. entity IDs).
	 * 
	 * @param variables
	 * @return
	 */
	public BindingSet generateInstantiations(List<String> variables) {

		Set<String> remainingVars = new HashSet<String>(variables);
		Stack<String> unconstrainedVars = new Stack<String>();
		Stack<String> constrainedVars = new Stack<String>();

		ListIterator<String> i = variables.listIterator();
		while (i.hasNext()) {
			String var = i.next();
			if (!hasAssignmentConstraint(var)) {
				remainingVars.remove(var);
				unconstrainedVars.add(var);
			}
			else if (remainingVars.contains(var)) {
				Set<String> g2 = new HashSet<String>(getAssignmentConstraint(var).get(0).keySet());
				g2.retainAll(remainingVars);
				if (g2.size() > 1) {
					constrainedVars.addAll(g2);
					remainingVars.removeAll(g2);
				}
				else {
					// there is only a single variable, which means it is unconstrained
					unconstrainedVars.addAll(g2);
					remainingVars.removeAll(g2);
				}
			}
		}

		// System.out.println("unconstrained :" + unconstrainedVars);
		// System.out.println("constrained :" + constrainedVars);
		BindingSet instantiations = new BindingSet();

		MapSet<String, Value> initial = new MapSet<String, Value>();
		for (String var : unconstrainedVars) {
			Set<Value> values = getParameter(var);
			if (values != null) {
				for (Value value : values) {
					initial.put(var, value);
				}
			}
		}
		instantiations.add(initial);

		while (!constrainedVars.isEmpty()) {
			String nextVar = constrainedVars.pop();
			BindingSet newInstantiations = new BindingSet();
			for (MapSet<String, Value> currentInst : instantiations) {
				for (MapSet<String, Value> assignment : getAssignmentConstraint(nextVar)) {
					MapSet<String, Value> newInst = new MapSet<String, Value>();
					newInst.putAll(currentInst);
					newInst.putAll(assignment);
					newInstantiations.add(newInst);
					constrainedVars.removeAll(assignment.keySet());
				}
			}
			instantiations = newInstantiations;
		}

		// System.out.println(instantiations);
		return instantiations;
	}

}

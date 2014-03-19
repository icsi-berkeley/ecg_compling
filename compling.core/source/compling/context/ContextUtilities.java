// =============================================================================
//File        : ContextUtilities.java
//Author      : emok
//Change Log  : Created on Nov 30, 2006
//=============================================================================

package compling.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.context.MiniOntology.Individual;
import compling.context.MiniOntology.Interval;
import compling.context.MiniOntology.Relation;
import compling.context.MiniOntology.StringValue;
import compling.context.MiniOntology.Type;
import compling.context.MiniOntology.Value;
import compling.learner.LearnerException;
import compling.util.MapSet;

//=============================================================================

public class ContextUtilities {

	public static class TopologicalSorter {

		MiniOntology miniOntology = null;

		MapSet<Interval, Interval> children = new MapSet<Interval, Interval>();
		MapSet<Interval, Interval> successors = new MapSet<Interval, Interval>();
		Set<Interval> intervals = null;
		ArrayList<Interval> sortedIntervals = null;

		public TopologicalSorter(MiniOntology miniOntology) {
			this.miniOntology = miniOntology;
			intervals = new HashSet<Interval>(miniOntology.getAllIntervals().values());

			for (Interval i : intervals) {
				if (i.getParent() != null) {
					children.put(i.getParent(), i);
				}
			}

			for (Interval i : intervals) {
				if (i.getPrior() != null) {
					successors.put(i.getPrior(), i);
				}
			}
		}

		private ArrayList<Interval> inheritanceSort() {
			ArrayList<Interval> toSort = new ArrayList<Interval>(intervals);
			if (toSort.size() == 1) {
				return toSort;
			}
			ArrayList<Interval> topologicalOrder = SortHelper(miniOntology.getBase(), new ArrayList<Interval>());
			Collections.reverse(topologicalOrder);
			toSort.removeAll(topologicalOrder);

			while (!toSort.isEmpty()) {
				toSort.removeAll(children.values());
				if (toSort.isEmpty()) {
					throw new ContextException("unrecoverable error occured while sorting intervals topologically");
				}
				ArrayList<Interval> moreSorted = SortHelper(toSort.get(0), new ArrayList<Interval>());
				Collections.reverse(moreSorted);
				topologicalOrder.addAll(moreSorted);
				toSort.removeAll(moreSorted);
			}

			return topologicalOrder;
		}

		private ArrayList<Interval> SortHelper(Interval interval, ArrayList<Interval> sorted) {
			if (sorted.contains(interval)) {
				return sorted;
			}

			if (children.get(interval) != null) {
				ArrayList<Interval> expansions = temporalSort(children.get(interval));
				Collections.reverse(expansions);
				for (Interval i : expansions) {
					SortHelper(i, sorted);
				}
			}
			sorted.add(interval);
			return sorted;
		}

		private ArrayList<Interval> temporalSort(Set<Interval> items) {
			ArrayList<Interval> toSort = new ArrayList<Interval>(items);
			if (toSort.size() == 1) {
				return toSort;
			}
			ArrayList<Interval> topologicalOrder = new ArrayList<Interval>();
			while (!toSort.isEmpty()) {
				toSort.removeAll(successors.values());
				if (toSort.isEmpty()) {
					throw new ContextException("unrecoverable error occured while sorting intervals topologically");
				}
				ArrayList<Interval> moreSorted = temporalSortHelper(toSort.get(0), new ArrayList<Interval>());
				Collections.reverse(moreSorted);
				topologicalOrder.addAll(moreSorted);
				toSort.removeAll(moreSorted);
			}

			return topologicalOrder;
		}

		private ArrayList<Interval> temporalSortHelper(Interval interval, ArrayList<Interval> sorted) {
			if (sorted.contains(interval)) {
				return sorted;
			}

			if (successors.get(interval) != null) {
				for (Interval i : successors.get(interval)) {
					SortHelper(i, sorted);
				}
			}
			sorted.add(interval);
			return sorted;
		}

		public ArrayList<Interval> getSortedIntervals() {
			if (sortedIntervals == null) {
				sortedIntervals = inheritanceSort();
			}
			return sortedIntervals;
		}

	}

	public static interface MiniOntologyFormatter {

		public String format(MiniOntology m);

		public String format(Type t);

		public String format(Interval i);

		public String format(Relation r);

		public String format(Individual i);

	}

	public static class QueryResultPrinter implements MiniOntologyFormatter {

		public String format(MiniOntology m) {
			return "";
		}

		public String format(Interval interval) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(interval.getName()).append(" (");
			sb.append(interval.getTypeName());
			sb.append("))");
			return sb.toString();
		}

		public String format(Type t) {
			return t.getType();
		}

		public String format(Relation r) {
			return r.getName();
		}

		public String format(Value v) {
			if (v instanceof Individual) {
				return format((Individual) v);
			}
			else if (v instanceof StringValue) {
				return ((StringValue) v).toString();
			}
			else {
				return v.getName();
			}
		}

		public String format(Individual i) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(i.getName()).append(" (");
			sb.append(i.getTypeName());
			sb.append("))");
			return sb.toString();
		}

		public static String formatIndividual(Individual i) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(i.getName()).append(" (");
			sb.append(i.getTypeName());
			sb.append("))");
			return sb.toString();
		}

		public static String getIndividualType(String i) {
			if (isTypedFiller(i)) {
				int parenIndex = i.indexOf("(", 1);
				return i.substring(parenIndex + 1, i.indexOf(")", parenIndex + 1)).trim();
			}
			else {
				return "";
			}
		}

		public static String getIndividualName(String i) {
			if (isTypedFiller(i)) {
				int parenIndex = i.indexOf("(", 1);
				return i.substring(1, parenIndex - 1).trim();
			}
			else {
				return i;
			}
		}

		public static boolean isTypedFiller(String i) {
			return i.charAt(0) == '(';
		}
	}

	public static class SimpleOntologyPrinter implements MiniOntologyFormatter {

		public String format(MiniOntology m) {
			StringBuilder sb = new StringBuilder();
			TopologicalSorter sorter = new TopologicalSorter(m);
			ArrayList<Interval> sortedIntervals = sorter.getSortedIntervals();

			sb.append("THIS MINI ONTOLOGY CONTAINS: ").append(sortedIntervals.size()).append(" INTERVALS\n\n");

			for (Interval i : sortedIntervals) {
				sb.append(format(i));
				sb.append("\n\n");
			}

			sb.append("THE CURRENT INTERVAL IS ").append(m.getCurrentInterval().getName()).append("\n");
			return sb.toString();
		}

		public String format(Interval interval) {
			StringBuilder sb = new StringBuilder();

			sb.append("INTERVAL: ").append(interval.getName());
			sb.append("  parent=");
			sb.append(interval.getParent() != null ? interval.getParent().getName() : "NULL");
			sb.append("  prior=");
			sb.append(interval.getPrior() != null ? interval.getPrior().getName() : "NULL");
			sb.append("\n");
			sb.append("INDIVIDUALS: \n");

			for (Individual i : interval.getAllIndividuals()) {
				sb.append(formatInstance(i));
				sb.append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}

		public String format(Type t) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(t.getType());

			if (t.getParents().size() > 0) {
				sb.append(" sub ");
				for (String parent : t.getParents()) {
					sb.append(parent).append(" ");
				}
			}

			for (Relation r : t.getRelations()) {
				sb.append("\n");
				sb.append(format(r));
			}
			sb.append(")");
			return sb.toString();
		}

		public String format(Relation r) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(r.getName()).append(" ");
			// sb.append(r.getDomain()).append(" ");
			sb.append(r.getRange()).append(" ");
			if (r.isPersistent()) {
				sb.append("persistent");
			}
			else {
				sb.append("transient");
			}
			sb.append(")");
			return sb.toString();
		}

		public String format(Individual i) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(i.getName()).append(" (");
			sb.append(i.getTypeName());
			sb.append("))");
			return sb.toString();
		}

		protected String formatInstance(Individual i) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(i.getName());
			// sb.append(" ");
			sb.append(")");
			return sb.toString();
		}

	}

	public static class OntologyGraphPrinter implements MiniOntologyFormatter {

		public String format(MiniOntology m) {
			StringBuilder sb = new StringBuilder();
			sb.append("digraph contextModel\n{\n");

			Map<String, Interval> intervals = m.getAllIntervals();
			for (Interval i : intervals.values()) {
				if (i.getParent() != null) {
					sb.append(i.getParent().getName()).append(" -> ").append(i.getName()).append(" [color=\"blue\"];\n");
				}
				if (i.getPrior() != null) {
					sb.append(i.getPrior().getName()).append(" -> ").append(i.getName())
							.append(" [color=\"red\" constraint=false];\n");
				}
			}
			sb.append("}\n");

			return sb.toString();
		}

		public String format(Interval interval) {
			return null;
		}

		public String format(Type t) {
			return null;
		}

		public String format(Relation r) {
			return null;
		}

		public String format(Individual i) {
			return null;
		}
	}

	public static Set<String> collapseResults(List<HashMap<String, String>> results, String importantVariable) {
		Set<String> entities = new HashSet<String>();
		if (results == null) {
			return entities;
		}
		for (HashMap<String, String> map : results) {
			entities.add(map.get(importantVariable));
		}
		return entities;
	}

	public static Set<String> collapseResults(List<HashMap<String, String>> results) {
		if (results == null || results.isEmpty()) {
			return new HashSet<String>();
		}
		if (results.get(0).keySet().size() > 1) {
			throw new LearnerException("Trying to collapse context query results across more than one query variable");
		}
		return collapseResults(results, results.get(0).keySet().iterator().next());
	}
}

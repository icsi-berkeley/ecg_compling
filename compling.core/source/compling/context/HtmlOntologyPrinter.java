// =============================================================================
// File : HtmlOntologyPrinter.java
// Author : emok
// Change Log : Created on Nov 30, 2006
// =============================================================================

package compling.context;

import java.util.ArrayList;

import compling.context.ContextUtilities.MiniOntologyFormatter;
import compling.context.ContextUtilities.TopologicalSorter;
import compling.context.MiniOntology.Individual;
import compling.context.MiniOntology.Interval;
import compling.context.MiniOntology.Relation;
import compling.context.MiniOntology.Type;
import compling.gui.GUIConstants;

// =============================================================================

public class HtmlOntologyPrinter implements MiniOntologyFormatter, GUIConstants {

	public String format(MiniOntology m) {
		StringBuffer sb = new StringBuffer();
		TopologicalSorter sorter = new TopologicalSorter(m);
		ArrayList<Interval> sortedIntervals = sorter.getSortedIntervals();

		sb.append("This mini ontology contains: ").append(sortedIntervals.size()).append(" intervals");
		sb.append(lineFeed).append(lineFeed);

		for (Interval i : sortedIntervals) {
			sb.append(format(i));
			sb.append(lineFeed);
		}
		return sb.toString();
	}

	public String format(Interval interval) {
		StringBuffer sb = new StringBuffer();

		sb.append("interval: ").append(interval.getName()).append(lineFeed);
		sb.append("individuals: ");
		sb.append(lineFeed).append(lineFeed);

		for (Individual i : interval.getAllIndividuals()) {
			sb.append(formatInstance(i));
			sb.append(lineFeed);
		}

		return sb.toString();
	}

	public String format(Type t) {
		StringBuffer sb = new StringBuffer();
		sb.append(open_paren);
		sb.append(t.getType());

		if (t.getParents().size() > 0) {
			sb.append(sub);
			for (String parent : t.getParents()) {
				sb.append(parent).append(space);
			}
		}
		if (t.getRelations().size() > 0) {
			sb.append(lineFeed);
			for (Relation r : t.getRelations()) {
				sb.append(tab);
				sb.append(format(r));
				sb.append(lineFeed);
			}
		}
		sb.append(close_paren);
		return sb.toString();
	}

	public String format(Relation r) {
		StringBuffer sb = new StringBuffer();
		sb.append(open_paren);
		sb.append(r.getName()).append(space);
		// sb.append(r.getDomain()).append(space);
		sb.append(r.getRange()).append(space);
		if (r.isPersistent()) {
			sb.append(persisentrel);
		}
		else {
			sb.append(transientrel);
		}
		sb.append(close_paren);
		return sb.toString();
	}

	public String format(Individual i) {
		StringBuffer sb = new StringBuffer();
		sb.append(open_paren);
		sb.append(i.getName()).append(space);
		sb.append(i.getType()).append(space);
		sb.append(close_paren);
		return sb.toString();
	}

	protected String formatInstance(Individual i) {
		StringBuffer sb = new StringBuffer();
		sb.append(open_paren);
		sb.append(i.getName());
		// sb.append(space);
		sb.append(close_paren);
		return sb.toString();
	}
}

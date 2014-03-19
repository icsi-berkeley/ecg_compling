package compling.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities.AnalysisFormatter;
import compling.parser.ecgparser.CxnalSpan;

public class GuiAnalysisFormatter implements AnalysisFormatter {

	public String format(Analysis a) {
		HashMap<Slot, List<String>> semanticConstraints = new HashMap<Slot, List<String>>();

		StringBuffer sb = new StringBuffer();
		sb.append("Analysis: ").append(a.getHeadCxn().getName());
		sb.append("(").append(a.getSpanLeftIndex()).append(", ").append(a.getSpanRightIndex()).append(")\n\n");
		sb.append("\tConstructions Used:\n\n");

		/*
		 * for (Slot s : a.getFeatureStructure().getSlots()){ TypeConstraint tc = s.getTypeConstraint(); if (tc != null
		 * &&tc.getTypeSystem() == a.getCxnTypeSystem()){ sb.append("\t\t").append(tc.getType()).append("["
		 * ).append(s.getSlotIndex()).append("]\n"); } }
		 */

		if (a.getSpans() != null) {
			for (CxnalSpan span : a.getSpans()) {
				int id = span.getSlotID();
				if (!span.omitted() && !span.gappedOut()) {
					sb.append("\t\t");
					Slot s = a.getFeatureStructure().getSlot(id);
					if (s != null && s.getTypeConstraint() != null) {
						sb.append(s.getTypeConstraint().getType()).append("[").append(s.getSlotIndex()).append("]");
						sb.append(" (").append(span.getLeft()).append(", ").append(span.getRight()).append(")\n");
					}
				}
			}
		}

		sb.append("\n\tSchemas Used:\n\n");
		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();
			if (tc != null && tc.getTypeSystem() != a.getCxnTypeSystem()) {
				// then this is a schema of some sort
				sb.append("\t\t");
				String type = tc.getType();
				if (tc.getTypeSystem() == a.getExternalTypeSystem()) {
					sb.append("@");
				}
				sb.append(type).append("[").append(s.getSlotIndex()).append("]").append("\n");
				semanticConstraints.put(s, new ArrayList<String>());
			}
		}

		// List roles/poles used in each set of semantic bindings
		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();
			if (tc == null) {
				continue;
			}
			String type = tc.getType();
			if (tc.getTypeSystem() != a.getCxnTypeSystem() && tc.getTypeSystem() != a.getSchemaTypeSystem()) {
				type = "@" + type;
			}
			int i = s.getSlotIndex();
			if (s.hasStructuredFiller()) {
				for (Role role : s.getFeatures().keySet()) {
					if (semanticConstraints.containsKey(s.getSlot(role))) {
						semanticConstraints.get(s.getSlot(role)).add(type + "[" + i + "]." + role.getName());
					}
				}
			}
		}

		// Display each group of bound roles/poles from above, along with the
		// group's filler
		sb.append("\n\tSemantic Constraints:\n\n");
		for (Slot s : semanticConstraints.keySet()) {
			int i = 0;
			List<String> constraints = semanticConstraints.get(s);
			for (String constraint : constraints) {
				i++;
				sb.append("\t\t").append(constraint);
				if (constraints.size() > 1 && i < constraints.size()) {
					sb.append(" <-->");
				}
				sb.append("\n");
			}
			String type = s.getTypeConstraint().getType();
			sb.append("\t\t\tFiller: ");
			if (s.getTypeConstraint().getTypeSystem() != a.getCxnTypeSystem()
					&& s.getTypeConstraint().getTypeSystem() != a.getSchemaTypeSystem()) {
				sb.append("@");
			}
			sb.append(type).append("[").append(s.getSlotIndex()).append("]").append("\n\n");
		}
//		sb.append(String.format("Global Focus: %s\n", a.getGlobalFocus()));

		return sb.toString();
	}

}

package compling.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import compling.grammar.ecg.Grammar.MapPrimitive;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities.AnalysisFormatter;
import compling.util.Arrays;

public class GraphvizAnalysisFormatter implements AnalysisFormatter {

	private Analysis analysys;
	private StringBuilder code;
	private Set<String> formatted;

	protected void emit(String s) {
		code.append(s);
	}

	protected void emitln(String s) {
		code.append(s + "\n");
	}

	protected String formatSlot(Slot slot) {
		TypeConstraint tc = slot.getTypeConstraint();
		return isExternal(tc) ? "@" + tc.getType() : tc.getType();
	}

	protected void emitSlot(Slot slot) {
		String name = formatSlot(slot);
		if (formatted.contains(name))
			return;

		formatted.add(name);

		emitln(String.format("\"%s\" [label=", name));
		emit(String.format("\"<%s>%s|{", name, name));

		if (slot.hasStructuredFiller()) {
			Iterator<Role> r = slot.getFeatures().keySet().iterator();
			while (r.hasNext()) {
				String roleName = r.next().toString();
				emit(String.format("<%s>%s", roleName, roleName));
				if (r.hasNext())
					emit("|");
			}
		}
		emitln("}\"];");
	}

	protected void formatMap(MapPrimitive schema) {
	}

	protected boolean isExternal(TypeConstraint typeConstraint) {
		return typeConstraint.getTypeSystem() == analysys.getExternalTypeSystem();
	}

	protected boolean isSchema(TypeConstraint typeConstraint) {
		return typeConstraint.getTypeSystem() != analysys.getCxnTypeSystem();
	}

	public String format(Analysis a) {
		this.analysys = a;
		this.code = new StringBuilder();
		this.formatted = new HashSet<String>();

		emitln("graph {");
		emitln("node [shape=Mrecord];");
		emitln("edge [arrowhead=dot, arrowtail=dot];");

		HashMap<Slot, List<String>> semanticConstraints = new HashMap<Slot, List<String>>();
		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();
			if (tc == null)
				continue;
			if (isSchema(tc)) {
				emitSlot(s);
				semanticConstraints.put(s, new ArrayList<String>());
			}
		}

		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();
			if (tc == null)
				continue;
			if (s.hasStructuredFiller()) {
				for (Role role : s.getFeatures().keySet()) {
					List<String> constraints = semanticConstraints.get(s.getSlot(role));
					if (constraints != null) {
						if (!isSchema(s.getTypeConstraint()))
							emitSlot(s);
						constraints.add(String.format("\"%s\":\"%s\"", formatSlot(s), role.getName()));
					}
				}
			}
		}

		for (Slot s : semanticConstraints.keySet()) {
			emit(Arrays.join(semanticConstraints.get(s).toArray(), " -- "));
			emitln(String.format(" -- \"%s\";", formatSlot(s)));
		}
		emitln("}");

		return code.toString();
	}

}

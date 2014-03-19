package compling.parser.ecgparser;

/**
 * This class keeps track of the helper (inner) classes for analyses. For example, classes that display the semspec or
 * extract bindings would be put here.
 * 
 * @author John Bryant
 * 
 **/

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import compling.context.MiniOntology.Individual;
import compling.context.Resolution;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotChainTables;
import compling.parser.ecgparser.PossibleSemSpecs.PartialSemSpec;
import compling.parser.ecgparser.morph.MAnalysis;

public class AnalysisUtilities {

	public static interface AnalysisFormatter {

		public String format(Analysis a);

	}

	public static class DefaultAnalysisFormatter implements AnalysisFormatter {

		public String format(Analysis a) {
			HashMap<Slot, List<String>> semanticConstraints = new HashMap<Slot, List<String>>();

			StringBuffer sb = new StringBuffer();
			sb.append("Analysis: ").append(a.getHeadCxn().getName());
			sb.append("(").append(a.getSpanLeftIndex()).append(", ").append(a.getSpanRightIndex()).append(")\n\n");
			sb.append("\tConstructions Used:\n\n");

			/*
			 * for (Slot s : a.getFeatureStructure().getSlots()){ TypeConstraint tc = s.getTypeConstraint(); if (tc != null
			 * &&tc.getTypeSystem() == a.getCxnTypeSystem()){
			 * sb.append("\t\t").append(tc.getType()).append("[").append(s.getSlotIndex()).append("]\n"); } }
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
				if (tc != null && tc.getTypeSystem() != a.getCxnTypeSystem()) { // then this is a schema of some sort
					sb.append("\t\t");
					String type = tc.getType();
					if (tc.getTypeSystem() != a.getSchemaTypeSystem()) {
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

			// Display each group of bound roles/poles from above, along with the group's filler
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
			return sb.toString();
		}
	}

	public static class CombinedAnalysisInContextFormatter implements AnalysisFormatter {

		ConstructionCentricFormatter ccf;
		DefaultAnalysisInContextFormatter daf;

		public CombinedAnalysisInContextFormatter(Grammar g, SlotChainTables table) {
			ccf = new ConstructionCentricFormatter(g, table, false);
			daf = new DefaultAnalysisInContextFormatter();
		}

		public String format(Analysis a) {
			StringBuilder sb = new StringBuilder();
			sb.append(ccf.format(a)).append("\n##############################\n");
			sb.append(daf.format(a));
			return sb.toString();
		}

	}

	public static class DefaultAnalysisInContextFormatter implements AnalysisFormatter {
		DefaultAnalysisFormatter daf = new DefaultAnalysisFormatter();

		public String format(Analysis a) {
			if (a instanceof AnalysisInContext) {
				StringBuilder sb = new StringBuilder();
				sb.append(daf.format(a)).append("\n\n\tResolutions:\n");
				List<Resolution> resolutions = ((AnalysisInContext) a).getResolutionsList();
				Set<Slot> printedSlots = new HashSet<Slot>();
				for (Resolution resolution : resolutions) {
					Slot source = ((AnalysisInContext) a).findSourceSlot(resolution);

					if (source != null && !printedSlots.contains(a.getFeatureStructure().getSlot(source, resolution.sc))) {
						Slot dest = a.getFeatureStructure().getSlot(source, resolution.sc);
						printedSlots.add(dest);
						sb.append("\t\t");
						if (dest != null && dest.getTypeConstraint() != null) {
							sb.append(dest.getTypeConstraint().getType()).append("[").append(dest.getSlotIndex())
									.append("] (").append(resolution.sc).append(")  ");
						}
						else if (dest != null && dest.getTypeConstraint() == null) { // an untyped slot
							sb.append("UNTYPED").append("[").append(dest.getSlotIndex()).append("] (").append(resolution.sc)
									.append(")  ");
						}
						else {
							throw new ParserException("????  When processing resolutions, slots were not found: "
									+ source.getSlotIndex() + "   " + resolution.sc + " dest == null");
						}

						sb.append("\t->\t");
						int j = 0;
						if (resolution.candidates != null && resolution.candidates.size() > 0) {
							for (Individual i : resolution.candidates) {
								String score = Double.toString(resolution.scores.get(j));
								// score = score.substring(0, score.indexOf(".")+3);
								sb.append(i.getName() + "[" + i.getTypeName() + "] " + score + " , ");
								j++;
							}
						}
						else {
							sb.append("No Referents");
						}
						sb.append("\n");
					}
				}
				sb.append("\n").append(a.getFeatureStructure().toString());
				return sb.toString();
			}
			else {
				return a.toString();
			}
		}
	}

	public static class ConstructionCentricFormatter implements AnalysisFormatter {
		SlotChainTables table;
		Grammar grammar;
		boolean printBindings;

		ConstructionCentricFormatter(Grammar g, SlotChainTables table) {
			this(g, table, true);
		}

		ConstructionCentricFormatter(Grammar g, SlotChainTables table, boolean printBindings) {
			this.table = table;
			grammar = g;
			this.printBindings = printBindings;
		}

		public String format(Analysis a) {
			HashMap<Integer, Map<SlotChain, String>> chainTracker = new HashMap<Integer, Map<SlotChain, String>>();
			HashMap<Integer, Map<SlotChain, Resolution>> resolutionsTracker = new HashMap<Integer, Map<SlotChain, Resolution>>();
			HashMap<Integer, String> intToStringID = new HashMap<Integer, String>();
			Map<Slot, Resolution> resolutionsTable = null;
			HashMap<String, List<String>> fillerToChains = new HashMap<String, List<String>>();

			FeatureStructureSet fss = a.getFeatureStructure();
			int rootID = fss.getMainRoot().getID();
			if (a instanceof AnalysisInContext) {
				resolutionsTable = ((AnalysisInContext) a).getResolutions();
			}

			for (Slot s : fss.getSlots()) {
				TypeConstraint tc = s.getTypeConstraint();
				if (tc != null && tc.getTypeSystem() == a.getCxnTypeSystem()) {
					String c = tc.getType() + "[" + s.getSlotIndex() + "]";
					Map<SlotChain, String> fillers = new HashMap<SlotChain, String>();
					Map<SlotChain, Resolution> resolutions = new HashMap<SlotChain, Resolution>();
					chainTracker.put(s.getID(), fillers);
					resolutionsTracker.put(s.getID(), resolutions);
					intToStringID.put(s.getID(), c);
					if (table.getCanonicalChains(grammar.getConstruction(tc.getType())) != null) {
						for (SlotChain chain : table.getCanonicalChains(grammar.getConstruction(tc.getType()))) {
							if (fss.hasSlot(s, chain)) {
								Slot fillerSlot = fss.getSlot(s, chain);
								TypeConstraint fillerTC = fillerSlot.getTypeConstraint();
								if (fillerTC != null) {
									String fillerString = fillerTC.getType() + "[" + fillerSlot.getSlotIndex() + "]";
									fillers.put(chain, fillerString);
									if (fillerToChains.get(fillerString) == null) {
										fillerToChains.put(fillerString, new ArrayList<String>());
									}
									fillerToChains.get(fillerString).add(c + "." + chain.toString());
								}
								else {
									fillers.put(chain, "--");
								}
								if (a instanceof AnalysisInContext && resolutionsTable.get(fillerSlot) != null) {
									resolutions.put(chain, resolutionsTable.get(fillerSlot));
								}

							}

						}
					}
				}
			}

			StringBuffer sb = new StringBuffer();
			sb.append("\nConstructions Used:\n\n");

			for (CxnalSpan span : a.getSpans()) {
				int id = span.getSlotID();

				if (!span.omitted() && !span.gappedOut()) {
					sb.append("  ");
					sb.append(intToStringID.get(id));
					sb.append(" (").append(span.getLeft()).append(", ").append(span.getRight()).append(")\n");
				}
			}

			Stack<CxnalSpan> stack = new Stack<CxnalSpan>();
			stack.push(new CxnalSpan(null, null, -1, 0, a.getSpanRightIndex()));
			int indentAmount = 0;
			int indentDelta = 4;

			sb.append("\nSemantics:\n\n");

			for (CxnalSpan span : a.getSpans()) {
				if (span.local()) {
					if (span.getLeft() >= stack.peek().getLeft() && span.getRight() <= stack.peek().getRight()) {
						indentAmount = indentAmount + indentDelta;
						stack.push(span);
					}
					else if (stack.peek().omitted() || stack.peek().gappedOut() || span.getRight() > stack.peek().getRight()) {
						while (stack.peek().omitted() || stack.peek().gappedOut()
								|| span.getRight() > stack.peek().getRight()) {
							indentAmount = indentAmount - indentDelta;
							stack.pop();
						}
						stack.push(span);
						indentAmount = indentAmount + indentDelta;
					}
				}
				else {
					boolean searching = true;
					while (searching) {
						Construction c = stack.peek().getType();
						boolean foundRole = false;
						// if (c == null){System.out.println("null c slotid="+stack.peek().getSlotID());}
						if (c != null) {
							for (Role role : c.getConstructionalBlock().getElements()) {
								if (role == span.getRole()) {
									foundRole = true;
									searching = false;
									break;
								}
							}
						}
						if (!foundRole) {
							indentAmount = indentAmount - indentDelta;
							stack.pop();
						}
					}
					stack.push(span);
					indentAmount = indentAmount + indentDelta;
				}
				int id = span.getSlotID();
				indent(sb, indentAmount);
				if (span.role != null) {
					sb.append(span.role.getName()).append(": ");
				}
				if (span.omitted()) {
					sb.append("omitted\n");
				}
				else if (span.gappedOut()) {
					sb.append("gapped out\n");
				}
				else {
					sb.append(intToStringID.get(id));
					sb.append(" (").append(span.getLeft()).append(", ").append(span.getRight()).append(") ");
					if (span.getSibs() != null) {
						indent(sb, 8);
						sb.append("Siblings: ");
						for (Analysis sib : span.getSibs()) {
							sb.append(sib.getHeadCxn().getName()).append("(").append(sib.getSpanLeftIndex()).append(",")
									.append(sib.getSpanRightIndex()).append("), ");
						}
					}
					sb.append("\n");
				}

				processChains(sb, indentAmount, id, a, chainTracker, resolutionsTracker, intToStringID, resolutionsTable,
						fillerToChains);
				sb.append("\n");

			}
			return sb.toString();
		}

		private void processChains(StringBuffer sb, int indentAmount, int id, Analysis a,
				HashMap<Integer, Map<SlotChain, String>> chainTracker,
				HashMap<Integer, Map<SlotChain, Resolution>> resolutionsTracker, HashMap<Integer, String> intToStringID,
				Map<Slot, Resolution> resolutionsTable, HashMap<String, List<String>> fillerToChains) {
			if (chainTracker.get(id) != null) {
				for (SlotChain sc : chainTracker.get(id).keySet()) {
					String filler = null;
					if (!chainTracker.get(id).get(sc).equals("--")) {
						filler = chainTracker.get(id).get(sc);
						indent(sb, indentAmount + 8);
						sb.append(sc).append(" : ").append(filler);
					}
					if (filler != null && a instanceof AnalysisInContext && resolutionsTracker.get(id).get(sc) != null) {
						int i = 0;
						indent(sb, 4);
						sb.append("--> Resolved to: ");
						Resolution resolution = resolutionsTracker.get(id).get(sc);
						if (resolution.candidates != null) {
							for (Individual individual : resolution.candidates) {
								sb.append(individual.getName()).append(" (").append(formatDouble(resolution.scores.get(i++)))
										.append("), ");
							}
						}
						else {
							System.out.println("an omitted slot has no candidates. ");
						}
					}

					if (filler != null) {
						sb.append("\n");
					}
					if (printBindings) {
						if (filler != null && fillerToChains.get(filler).size() > 1) {
							indent(sb, indentAmount + 12);
							sb.append("(");
							int i = 0;
							for (String otherChain : fillerToChains.get(filler)) {
								if (otherChain.indexOf(intToStringID.get(id)) == -1) {
									sb.append(otherChain).append(", ");
									i++;
								}
								if (i == 4) {
									sb.append(".....");
									break;
								}
							}
							sb.deleteCharAt(sb.length() - 1);
							sb.deleteCharAt(sb.length() - 1);
							sb.append(")\n");
						}
					}
				}
			}
			else {
				indent(sb, indentAmount + 8);
				sb.append("No chains found\n");
			}

		}

	}

	/**
	 * Used to generate a flat representation of the morphological parse of a word so it can be used in the original
	 * version of the syntactic parser. Constituent constructions are discarded, and the full word is assigned to
	 * self.f.orth. In order to be considered a lexical construction under the original parser's definition (thus
	 * permitting an assignment to self.f.orth), this must not inherit from any construction which defines constituents.
	 * 
	 * @author Nathan Schneider (nss)
	 * 
	 */
	public static class FlatAnalysisFormatter implements AnalysisFormatter {
		final private Grammar grammar;
		final private String input;

		public FlatAnalysisFormatter(Grammar g, String input) {
			grammar = g;
			this.input = input;
		}

		final static String SPECIAL_SUFFIX = "_"; // to indicate a generated construction

		public static String generateSchemaInstanceName(String typeName, int slotIndex) {
			return typeName.replaceAll("@", "ONTOLOGY_").replaceAll("[.]", "_dot_") + "_" + slotIndex + SPECIAL_SUFFIX;
		}

		/**
		 * Of c and c's ancestor constructions, get the nearest ones not defining any constituents. There may be some
		 * redundancy (e.g. A and B are found via different parents, where B is as subtype of A).
		 * 
		 * @param cxn
		 * @param grammar
		 * @return
		 */
		public Set<String> getSimpleAncestors(String cxn) {
			Set<String> simpleAncestors = new HashSet<String>();
			List<String> queue = new ArrayList<String>();
			Set<String> dead = new HashSet<String>();
			queue.add(cxn);
			while (!queue.isEmpty()) {
				String cn = queue.remove(0);
				Construction cx = grammar.getConstruction(cn);
				cn = cx.getName(); // make sure we have the canonicalized name (necessary for subtype checking)
				if (cx.getConstituents() != null && cx.getConstituents().size() > 0) { // need to move up at least one more
																												// level
					Set<String> pars = cx.getParents();
					for (String par : pars) {
						if (!simpleAncestors.contains(par) && !dead.contains(par) && !queue.contains(par))
							queue.add(par);
					}
					dead.add(cn);
				}
				else {
					boolean addCn = true;

					try {
						for (String anc : simpleAncestors) {
							if (cx.getCxnTypeSystem().subtype(anc, cn)) { // don't add cn--it is already covered by anc, one of
																							// its descendants
								addCn = false;
								break;
							}
							else if (cx.getCxnTypeSystem().subtype(cn, anc)) { // replace anc with cn, one if its descendants
																								// (and thus more specific)
								simpleAncestors.remove(anc);
								break;
							}
						}
					}
					catch (TypeSystemException ex) {
						ex.printStackTrace();
					}

					if (addCn)
						simpleAncestors.add(cn);
					else
						dead.add(cn);
				}
			}
			return simpleAncestors;
		}

		public String format(Analysis a) {

			HashMap<Slot, List<String>> cConstraints = new HashMap<Slot, List<String>>();
			HashMap<Slot, List<String>> mConstraints = new HashMap<Slot, List<String>>();

			StringBuffer sb = new StringBuffer();
			sb.append("construction ").append(input.replace(" ", "_").toUpperCase() + SPECIAL_SUFFIX).append("\n");

			String simpleAncestors = getSimpleAncestors(a.getHeadCxn().getName()).toString().replace("[", "")
					.replace("]", "").trim();
			if (simpleAncestors.length() > 0)
				sb.append("\tsubcase of ").append(simpleAncestors).append("\n");

			StringBuffer cblock = new StringBuffer(); // constructional block
			StringBuffer fblock = new StringBuffer(); // form block
			StringBuffer mblock = new StringBuffer(); // meaning block

			StringBuffer sbCConstraints = new StringBuffer();
			StringBuffer sbMConstraints = new StringBuffer();

			cblock.append("\tconstructional:" + a.getHeadCxn().getConstructionalBlock().getType()).append("\n");

			Set<Role> cxnalElts = a.getHeadCxn().getConstructionalFeatures();
			Map<String, Role> cSchemaRoles = new HashMap<String, Role>();
			for (Role r : cxnalElts) {
				// Mark the constructional feature as unified with the appropriate schema instance
				Slot slot = a.getFeatureStructure().getSlot(new ECGSlotChain(r));
				if (slot.getTypeConstraint() == null)
					continue;
				String fillerSchema = generateSchemaInstanceName(slot.getTypeConstraint().getType(), slot.getSlotIndex());
				sbCConstraints.append("\t\t\t").append(r.getName()).append(" <--> ").append(fillerSchema).append("\n");
				cSchemaRoles.put(fillerSchema, r);
			}

			fblock.append("\tform\n\t\tconstraints\n\t\t\tself.f.orth <-- \"").append(input).append("\"\n");

			mblock.append("\tmeaning\n");

			Slot mSlot = a.getFeatureStructure().getSlot(new SlotChain("m"));

			for (Slot s : a.getFeatureStructure().getSlots()) {
				TypeConstraint tc = s.getTypeConstraint();
				if (tc != null && tc.getTypeSystem() != a.getCxnTypeSystem()) { // then this is a schema of some sort
					String type = tc.getType();
					if (tc.getTypeSystem() != a.getSchemaTypeSystem())
						type = "@" + type;
					String schemaName = generateSchemaInstanceName(type, s.getSlotIndex());

					boolean isM = false; // constructional block
					if (!cSchemaRoles.containsKey(schemaName))
						isM = true; // meaning block

					// ALL evokes statements go in the meaning block--even if they're for constructional schemas
					mblock.append("\t\tevokes ").append(type).append(" as ").append(schemaName).append("\n");

					if (s.getFeatures() != null) {
						for (Role r : s.getFeatures().keySet()) {
							Slot childSlot = s.getFeatures().get(r);
							if (childSlot.hasAtomicFiller()) { // literal assignment constraint (some of these may be redundant
																			// due to unification or specified directly in the schema
																			// definition, but that's OK)
								if (schemaName.startsWith("VerbNotSg3_"))
									System.out.print("");

								if (!isM)
									sbCConstraints.append("\t\t\t").append(cSchemaRoles.get(schemaName) + "." + r.getName())
											.append(" <-- " + childSlot.getAtom() + "\n");
								else
									sbMConstraints.append("\t\t\t").append(schemaName + "." + r.getName())
											.append(" <-- " + childSlot.getAtom() + "\n");
							}
						}
					}

					if (cSchemaRoles.containsKey(schemaName))
						cConstraints.put(s, new ArrayList<String>());
					else
						mConstraints.put(s, new ArrayList<String>());
				}
			}

			// Print literal value assignments, and record roles/poles used in each set of semantic bindings
			Collection<Slot> slots = new ArrayList<Slot>();
			slots.addAll(a.getFeatureStructure().getSlots());
			for (Slot s : slots) {
				TypeConstraint tc = s.getTypeConstraint();
				if (tc == null) {
					continue;
				}
				String type = tc.getType();
				if (tc.getTypeSystem() != a.getCxnTypeSystem() && tc.getTypeSystem() != a.getSchemaTypeSystem())
					type = "@" + type;

				if (tc.getTypeSystem() == a.getCxnTypeSystem())
					continue;
				String schemaName = generateSchemaInstanceName(type, s.getSlotIndex());

				if (s == mSlot) { // the meaning pole
					sbMConstraints.append("\t\t\tself.m <--> " + schemaName).append("\n");
				}

				if (s.hasStructuredFiller()) {
					for (Role role : s.getFeatures().keySet()) {
						if (schemaName.startsWith("VerbNotSg3_"))
							System.out.print("");

						Slot s2 = s.getSlot(role);
						if (cConstraints.containsKey(s2) || cConstraints.containsKey(s)) {

							if (mConstraints.containsKey(s2)) {
								List<String> s2Constraints = mConstraints.remove(s2);
								cConstraints.put(s2, s2Constraints);
							}
							if (cConstraints.containsKey(s2))
								cConstraints.get(s2).add(cSchemaRoles.get(schemaName) + "." + role.getName());
						}
						else if (mConstraints.containsKey(s2)) {
							mConstraints.get(s2).add(schemaName + "." + role.getName());
						}
					}
				}

			}

			// Constructional constraints: bindings

			for (Slot s : cConstraints.keySet()) {
				String type = s.getTypeConstraint().getType();
				if (s.getTypeConstraint().getTypeSystem() != a.getCxnTypeSystem()
						&& s.getTypeConstraint().getTypeSystem() != a.getSchemaTypeSystem()) {
					type = "@" + type;
				}
				String filler = generateSchemaInstanceName(type, s.getSlotIndex());
				List<String> constraints = cConstraints.get(s);
				{
					for (String constraint : constraints) {
						sbCConstraints.append("\t\t\t").append(constraint + " <--> " + filler).append("\n");
					}
				}
			}

			// Meaning constraints: bindings

			for (Slot s : mConstraints.keySet()) {
				String type = s.getTypeConstraint().getType();
				if (s.getTypeConstraint().getTypeSystem() != a.getCxnTypeSystem()
						&& s.getTypeConstraint().getTypeSystem() != a.getSchemaTypeSystem()) {
					type = "@" + type;
				}
				String filler = generateSchemaInstanceName(type, s.getSlotIndex());
				List<String> constraints = mConstraints.get(s);
				{
					for (String constraint : constraints) {
						sbMConstraints.append("\t\t\t").append(constraint + " <--> " + filler).append("\n");
					}
				}
			}

			cblock.append("\t\tconstraints\n").append(sbCConstraints);
			mblock.append("\t\tconstraints\n").append(sbMConstraints);

			sb.append(cblock).append(fblock).append(mblock);

			return sb.toString();
		}
	}

	public static class MorphAnalysisFormatter implements AnalysisFormatter {
		public boolean suppressWordIndex = false; // If true, the word indices will only be printed in the span for the
																// head cxn
		public boolean showScore = true;

		public String format(Analysis a) {
			if (a instanceof MAnalysis) {
				return format((MAnalysis) a);
			}
			System.err.println("Error in MorphAnalysisFormatter: Not a morphological analysis.");
			return null;
		}

		public String format(MAnalysis a) {
			HashMap<Slot, List<String>> semanticConstraints = new HashMap<Slot, List<String>>();
			StringBuffer sb = new StringBuffer();
			if (showScore) {
				sb.append(a.getScore()).append(" ");
			}
			sb.append("Analysis: ").append(a.getHeadCxn().getName());
			sb.append("(").append(a.getSpanLeftIndex()).append(";").append(a.getSpanLeftCharIndex()).append(", ")
					.append(a.getSpanRightIndex()).append(";").append(a.getSpanRightCharIndex()).append(")\n\n");
			sb.append("\tConstructions Used:\n\n");

			/*
			 * for (Slot s : a.getFeatureStructure().getSlots()){ TypeConstraint tc = s.getTypeConstraint(); if (tc != null
			 * &&tc.getTypeSystem() == a.getCxnTypeSystem()){
			 * sb.append("\t\t").append(tc.getType()).append("[").append(s.getSlotIndex()).append("]\n"); } }
			 */

			if (a.getSpans() != null) {
				for (CxnalSpan span : a.getSpans()) {
					int id = span.getSlotID();
					if (!span.omitted() && !span.gappedOut()) {
						sb.append("\t\t");
						Slot s = a.getFeatureStructure().getSlot(id);
						if (s != null && s.getTypeConstraint() != null) {
							sb.append(s.getTypeConstraint().getType()).append("[").append(s.getSlotIndex()).append("]");

							if (!suppressWordIndex)
								sb.append(" (").append(span.getLeft()).append(";");
							else
								sb.append(";(");

							sb.append(span.getLeftChar()).append(", ");

							if (!suppressWordIndex)
								sb.append(span.getRight()).append(";");
							sb.append(span.getRightChar()).append(")\n");
						}
					}
				}
			}

			// Show omitted optionals from the head construction only
			sb.append("\n\tOmitted Optionals:\n\n");
			for (Role r : a.getOmittedOptionals()) {
				sb.append("\t\t" + r.getName() + "\n");
			}

			// Show disjoint (noncontiguous or nonadjacently used) constituents from the head construction only
			sb.append("\n\tDisjoint Constituents:\n\n");
			for (Role r : a.getDisjointConstits()) {
				sb.append("\t\t" + r.getName() + "\n");
			}

			Set<Slot> formSchemas = new HashSet<Slot>();

			sb.append("\n\tSemantic Schemas Used:\n\n");
			for (Slot s : a.getFeatureStructure().getSlots()) {
				TypeConstraint tc = s.getTypeConstraint();

				if (tc != null && tc.getTypeSystem() != a.getCxnTypeSystem()) { // then this is a schema of some sort
					if (s.getParentSlots().size() > 0
							&& compling.parser.ecgparser.morph.MGrammarWrapper.findRole(s.getParentSlots(), "f") != null) { // Some
																																							// .f
																																							// role
																																							// points
																																							// to
																																							// this;
																																							// it
																																							// must
																																							// be
																																							// a
																																							// form
																																							// schema
						formSchemas.add(s);
					}
					else { // Must be a semantic schema

						sb.append("\t\t");
						String type = tc.getType();
						if (tc.getTypeSystem() != a.getSchemaTypeSystem()) {
							sb.append("@");
						}
						sb.append(type).append("[").append(s.getSlotIndex()).append("]").append("\n");
						semanticConstraints.put(s, new ArrayList<String>());
					}
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

			// Display each group of bound roles/poles from above, along with the group's filler
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

			HashMap<Slot, List<String>> formConstraints = new HashMap<Slot, List<String>>();

			sb.append("\n\tForm Schemas Used:\n\n");
			for (Slot s : formSchemas) {
				TypeConstraint tc = s.getTypeConstraint();
				sb.append("\t\t");
				String type = tc.getType();
				if (tc.getTypeSystem() != a.getSchemaTypeSystem()) {
					sb.append("@");
				}
				sb.append(type).append("[").append(s.getSlotIndex()).append("]").append("\n");
				formConstraints.put(s, new ArrayList<String>());
			}

			// List roles/poles used in each set of form bindings
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
						if (formConstraints.containsKey(s.getSlot(role))) {
							formConstraints.get(s.getSlot(role)).add(type + "[" + i + "]." + role.getName());
						}
					}
				}
			}

			// Display each group of bound roles/poles from above, along with the group's filler
			sb.append("\n\tForm Constraints:\n\n");
			for (Slot s : formConstraints.keySet()) {
				int i = 0;
				List<String> constraints = formConstraints.get(s);
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

			return sb.toString();
		}
	}

	public static void indent(StringBuffer sb, int indentAmount) {
		if (indentAmount <= 0) {
			return;
		}
		for (int i = 0; i < indentAmount; i++) {
			sb.append(" ");
		}
	}

	public static void indent(StringBuilder sb, int indentAmount) {
		if (indentAmount <= 0) {
			return;
		}
		for (int i = 0; i < indentAmount; i++) {
			sb.append(" ");
		}
	}

	public static String formatDouble(double d) {
		return formatDouble(3, d);
	}

	public static String formatDouble(int decimalLength, double d) {
		String s = Double.toString(d);
		String epart = "";
		if (s.indexOf("E") > -1) {
			epart = s.substring(s.indexOf("E"), s.length());
		}
		int pointPosition = s.indexOf(".");
		int extra = Math.min(decimalLength, s.length() - pointPosition);
		return s.substring(0, pointPosition + extra) + epart;
	}

	public static class BuggyGuiFormatter implements AnalysisFormatter {
		public String format(Analysis a) {
			HashMap<Integer, String> intToStringID = new HashMap<Integer, String>();
			FeatureStructureSet fss = a.getFeatureStructure();
			for (Slot s : fss.getSlots()) {
				TypeConstraint tc = s.getTypeConstraint();
				if (tc != null && tc.getTypeSystem() == a.getCxnTypeSystem()) {
					String c = tc.getType() + "[" + s.getSlotIndex() + "]";
					intToStringID.put(s.getID(), c);
				}
			}

			StringBuffer sb = new StringBuffer();
			sb.append("      Constructions Used:\n");
			for (CxnalSpan span : a.getSpans()) {
				int id = span.getSlotID();

				if (!span.omitted() && !span.gappedOut()) {
					sb.append("\t");
					sb.append(intToStringID.get(id));
					sb.append(" (").append(span.getLeft()).append(", ").append(span.getRight()).append(")\n");
				}
			}
			sb.append("\n      Bindings:\n");
			PossibleSemSpecs psslist = a.getPossibleSemSpecs();
			for (PartialSemSpec pss : psslist.getSemSpecList()) {
				sb.append(pss.buggyBindingsView()).append("\n");
			}
			return sb.toString();
		}
	}

}

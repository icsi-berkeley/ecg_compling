// =============================================================================
//File        : LearnerGrammarPrinter.java
//Author      : emok
//Change Log  : Created on Feb 20, 2008
//=============================================================================

package compling.learner.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities.ECGGrammarFormatter;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.MapPrimitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Grammar.Situation;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;

//=============================================================================

public class LearnerGrammarPrinter implements ECGGrammarFormatter {

	public String format(MapPrimitive m) {
		// TODO Auto-generated method stub
		return null;
	}

	public String format(Situation s) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Logger logger = Logger.getLogger(LearnerGrammarPrinter.class.getName());

	public String format(Grammar g) {
		StringBuffer sb = new StringBuffer();
		for (Schema schema : g.getAllSchemas()) {
			sb.append(formatSchema(schema)).append("\n");
		}
		for (Construction cxn : g.getAllConstructions()) {
			sb.append(format(cxn)).append("\n");
		}
		return sb.toString();
	}

	public String format(Schema s) {
		return formatSchema(s).toString();
	}

	private StringBuffer formatSchema(Schema s) {
		StringBuffer sb = new StringBuffer("schema ");
		sb.append(s.getName()).append("\n");
		if (s.getParents().size() > 0) {
			sb.append("\t").append("subcase of ");
			for (String parent : s.getParents()) {
				sb.append(parent).append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
			sb.append("\n");
		}
		if (s.getContents() != null) {
			sb.append(formatBlock(s.getContents(), s.getName(), false));
		}
		return sb;
	}

	public String format(Construction c) {

		TypeSystem<Construction> cxnTS = c.getCxnTypeSystem();
		Set<Construction> parents = new HashSet<Construction>();
		for (String parent : c.getParents()) {
			parents.add(cxnTS.get(parent));
		}
		return formatCxn(c, parents).toString();
	}

	public String format(Construction c, Map<String, Construction> otherCxnsInGrammar) {

		Set<Construction> parents = new HashSet<Construction>();
		for (String parent : c.getParents()) {
			Construction parentCxn = otherCxnsInGrammar.get(parent);
			if (parentCxn == null) {
				throw new LearnerException("Parent construction " + parent + " of " + c.getName()
						+ " not defined in grammar");
			}
			parents.add(parentCxn);
		}
		return formatCxn(c, parents).toString();
	}

	private StringBuffer formatCxn(Construction c, Set<Construction> parents) {
		StringBuffer sb = new StringBuffer();
		if (c.getKind() == ECGConstants.ABSTRACT) {
			sb.append(toTitleCase(ECGConstants.ABSTRACT)).append(" ");
		}
		sb.append("Construction ");
		sb.append(c.getName()).append("\n");
		if (c.getParents().size() > 0) {
			sb.append("\t").append("subcase of ");
			for (String parent : c.getParents()) {
				sb.append(parent).append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
			sb.append("\n");
		}
		if (c.getConstructionalBlock() != null) {
			sb.append(formatBlockInConstruction(c.getConstructionalBlock(), ECGConstants.CONSTRUCTION, c, parents));
		}
		if (c.getFormBlock() != null) {
			sb.append(formatBlockInConstruction(c.getFormBlock(), ECGConstants.FORM, c, parents));
		}
		if (c.getMeaningBlock() != null) {
			sb.append(formatBlockInConstruction(c.getMeaningBlock(), ECGConstants.MEANING, c, parents));
		}
		return sb;
	}

	private StringBuffer formatBlockInConstruction(Block block, String blockKind, Construction construction,
			Set<Construction> parents) {
		boolean printBlockType = !hasInheritedBlockType(construction, blockKind, block, parents);
		return formatBlock(block, construction.getName(), printBlockType);
	}

	private StringBuffer formatBlock(Block b, String containerName, boolean printBlockType) {
		StringBuffer sb = new StringBuffer();

		if ((b.getType() == ECGConstants.UNTYPED || !printBlockType) && b.getEvokedElements().size() == 0
				&& b.getElements().size() == 0 && b.getConstraints().size() == 0) {
			return sb;
		}

		if (b.getKind() != ECGConstants.CONTENTS) {
			sb.append("\t").append(b.getKind().toLowerCase());
			if (b.getType() != null && b.getType() != ECGConstants.UNTYPED && printBlockType) {
				sb.append(": ");
				if (b.getBlockTypeTypeSystem() != null && b.getBlockTypeTypeSystem().getName() == ECGConstants.ONTOLOGY) {
					sb.append("@");
				}
				sb.append(b.getType());
			}
			sb.append("\n");
		}
		for (Role r : b.getEvokedElements()) {
			if (r.getSource().equals(containerName)) {
				sb.append("\t\tevokes ").append(formatTypeConstraint(r.getTypeConstraint()));
				sb.append(" as ").append(r.getName()).append("\n");
			}
		}
		if (b.getElements().size() > 0) {
			if (b.getKind() == ECGConstants.CONSTRUCTIONAL) {
				sb.append("\t\tconstituents\n");
			}
			if (b.getKind() == ECGConstants.MEANING || b.getKind() == ECGConstants.CONTENTS) {
				sb.append("\t\troles\n");
			}
			for (Role r : b.getElements()) {
				if (r.getSource().equals(containerName)) {
					sb.append("\t\t\t");
					if (r.getSpecialField().contains(ECGConstants.OPTIONAL)) {
						sb.append("optional ");
					}
					if (r.getSpecialField().contains(ECGConstants.EXTRAPOSED)) {
						sb.append("extraposed ");
					}
					sb.append(r.getName());
					if (r.getTypeConstraint() != null) {
						sb.append(" : ").append(formatTypeConstraint(r.getTypeConstraint()));
					}
					String remainder = r.getSpecialField().replaceAll(ECGConstants.OPTIONAL, "")
							.replaceAll(ECGConstants.EXTRAPOSED, "").trim();
					if (remainder.length() > 0) {

						remainder = remainder.replaceAll("\\[", "").replaceAll("\\]", "").trim();

						sb.append(" [");
						for (String param : remainder.split(" ")) {
							sb.append(param).append(", ");
						}
						sb.deleteCharAt(sb.length() - 1).deleteCharAt(sb.length() - 1);
						sb.append("]");
					}
					sb.append("\n");
				}
			}
		}
		if (b.getConstraints().size() > 0) {
			sb.append("\t\tconstraints\n");
			for (Constraint c : b.getConstraints()) {
				if (c.getSource().equals(containerName)) {
					sb.append("\t\t\t").append(c).append("\n");
				}
				else if (c.overridden()) {
					sb.append("\t\t\t").append("ignore ").append(c).append("\n");
				}
			}
		}
		return sb;
	}

	protected String formatTypeConstraint(TypeConstraint c) {
		if (c.getTypeSystem().getName() == ECGConstants.ONTOLOGY) {
			return "@" + c.getType();
		}
		return c.getType();
	}

	private String toTitleCase(String s) {
		if (s.length() == 0) {
			return s;
		}

		StringBuffer sb = new StringBuffer();

		String[] words = s.split(" ");
		for (String word : words) {
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.toLowerCase().substring(1)).append(" ");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	private boolean hasInheritedBlockType(Construction construction, String blockKind, Block block,
			Set<Construction> parents) {

		// this function in a sense reverse engineer the ComputeBlockType method in Grammar Checker.

		if (block.getType() == ECGConstants.UNTYPED) {
			return true;
		}
		else if (parents.contains(null)) {
			return false;
		}

		HashSet<String> types = new HashSet<String>();
		TypeSystem<?> ts = block.getBlockTypeTypeSystem();

		if (blockKind.equals(ECGConstants.FORM)) {
			for (Construction parent : parents) {
				if (parent.getFormBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getFormBlock().getType());
				}
			}
		}
		else if (blockKind.equals(ECGConstants.MEANING)) {
			for (Construction parent : parents) {
				if (parent.getMeaningBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getMeaningBlock().getType());
				}
			}
		}
		else if (blockKind.equals(ECGConstants.CONSTRUCTION)) {
			for (Construction parent : parents) {
				if (parent.getConstructionalBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getConstructionalBlock().getType());
				}
			}
		}
		else { // schema blocks
			return false;
		}

		String bestType = ECGConstants.UNTYPED;
		if (types.size() > 0) {
			try {
				bestType = ts.bestUnifyingType(types);
			}
			catch (TypeSystemException tse) {
				logger.warning("TypeSystemError while checking if a block type is inherited: " + tse.getLocalizedMessage());
			}
		}

		if (block.getType().equals(bestType)) {
			return true;
		}
		else {
			return false;
		}
	}
}

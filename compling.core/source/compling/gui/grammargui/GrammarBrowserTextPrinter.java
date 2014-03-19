/**
 *
 */
package compling.gui.grammargui;

import java.util.List;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.ECGGrammarUtilities.ECGGrammarFormatter;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.MapPrimitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Grammar.Situation;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;

/**
 * Implements a local Grammar Text Printer. The reason is that a different
 * format for an HTML anchor's href is needed for the SWT Link Composite.
 *
 * Copied from <a href="compling.grammar.ecg#HTMLGrammarPrinter">
 * 	compling.grammar.ecg.HTMLGrammarPrinter</a>
 *
 * @author emok, modified by lucag
 */
public class GrammarBrowserTextPrinter implements ECGGrammarFormatter,
		GrammarBrowserGUIConstants {

	private static final String ANCHOR_FORMAT = "<a href=\"%s/%s\">%s</a>";

	/**
	 * Generates and HTML link that doesn't make use of attributes for encoding
	 * the type (which can be either a COnstrucion, a Schema, a Block, and so on.
	 *
	 * @param linkText -
	 *          The link text that has to be shown
	 * @param target -
	 *          The target of the link
	 * @param type -
	 *          The type of the grammar element (Construction, Schema, and so on)
	 * @return A formatted String containing the HTML anchor.
	 */
	protected String generateHTMLLink(String linkText, String target, String type) {
		return String.format(ANCHOR_FORMAT, type, target, linkText);
	}

	public String format(Grammar g) {
		StringBuffer sb = new StringBuffer();
		for (Schema schema : g.getAllSchemas()) {
			sb.append(formatSchema(schema)).append(lineFeed);
		}
		for (Construction cxn : g.getAllConstructions()) {
			sb.append(formatCxn(cxn)).append(lineFeed);
		}
		return sb.toString();
	}

	public String format(Construction c) {
		return formatCxn(c).toString();
	}

	private StringBuffer formatCxn(Construction c) {
		StringBuffer sb = new StringBuffer();
      String kind = c.getKind().equals(ECGConstants.ABSTRACT) ? "GENERAL " : "";
		sb.append(kind).append(ECGConstants.CONSTRUCTION)
				.append(space);
		sb.append(c.getName()).append(lineFeed);
		if (c.getParents().size() > 0) {
			sb.append(tab).append(subcaseof);
			for (String parent : c.getParents()) {
				sb.append(generateHTMLLink(parent, parent, ECGConstants.CONSTRUCTION))
						.append(space);
			}
			sb.append(lineFeed);
		}
		if (c.getConstructionalBlock() != null) {
			sb.append(formatBlock(c.getConstructionalBlock(), c.getName()));
		}
		if (c.getFormBlock() != null) {
			sb.append(formatBlock(c.getFormBlock(), c.getName()));
		}
		if (c.getMeaningBlock() != null) {
			sb.append(formatBlock(c.getMeaningBlock(), c.getName()));
		}
		return sb;
	}

	public String format(Schema s) {
		return formatSchema(s).toString();
	}

	public String format(MapPrimitive m) {
		return formatMap(m).toString();
	}

	public String format(Situation m) {
		return formatSituation(m).toString();
	}
	
	private StringBuffer formatSchema(Schema s) {
		StringBuffer sb = new StringBuffer();
		sb.append(ECGConstants.SCHEMA).append(space);
		sb.append(s.getName()).append(lineFeed);
		if (s.getParents().size() > 0) {
			sb.append(tab).append(subcaseof);
			for (String parent : s.getParents()) {
				sb.append(generateHTMLLink(parent, parent, ECGConstants.SCHEMA))
						.append(space);
			}
			sb.append(lineFeed);
		}
		if (s.getContents() != null) {
			sb.append(formatBlock(s.getContents(), s.getName()));
		}
		return sb;
	}

	private StringBuffer formatMap(MapPrimitive m) {
		StringBuffer sb = new StringBuffer();
		sb.append(ECGConstants.MAP).append(space);
		sb.append(m.getName()).append(lineFeed);
		if (m.getParents().size() > 0) {
			sb.append(tab).append(subcaseof);
			for (String parent : m.getParents()) {
				sb.append(generateHTMLLink(parent, parent, ECGConstants.MAP))
				.append(space);
			}
			sb.append(lineFeed);
		}
		if (m.getContents() != null) {
			sb.append(formatBlock(m.getContents(), m.getName()));
		}
		return sb;
	}
	
	private StringBuffer formatSituation(Situation s) {
		StringBuffer sb = new StringBuffer();
		sb.append(ECGConstants.SITUATION).append(space);
		sb.append(s.getName()).append(lineFeed);
		if (s.getParents().size() > 0) {
			sb.append(tab).append(subcaseof);
			for (String parent : s.getParents()) {
				sb.append(generateHTMLLink(parent, parent, ECGConstants.SITUATION))
				.append(space);
			}
			sb.append(lineFeed);
		}
		if (s.getContents() != null) {
			sb.append(formatBlock(s.getContents(), s.getName()));
		}
		return sb;
	}
	
	private StringBuffer formatBlock(Block b, String currentSource) {
		StringBuffer sb = new StringBuffer();
		if (b.getKind().equals(ECGConstants.CONTENTS) == false) {
			sb.append(tab).append(b.getKind());
			if (b.getType() != null) {
				sb.append(colon).append(generateLinkForBlockConstraint(b));
			}
			sb.append(lineFeed);
		}
		for (Role r : b.getEvokedElements()) {
			sb.append(tab + tab + "evokes ").append(
					generateLinkForTypeConstraint(r.getTypeConstraint(), b));
			sb.append(" as ").append(r.getName());
			sb.append(formatInheritanceInfo(r.getSource(), currentSource));
			sb.append(lineFeed);
		}
		if (b.getElements().size() > 0) {
			if (b.getKind().equals(ECGConstants.CONSTRUCTIONAL)) {
				sb.append(tab + tab + ECGConstants.CONSTITUENTS + lineFeed);
			}
			if (b.getKind().equals(ECGConstants.MEANING)
					|| b.getKind().equals(ECGConstants.CONTENTS)) {
				sb.append(tab + tab + ECGConstants.ROLES + lineFeed);
			}
			for (Role r : b.getElements()) {
				sb.append(tab + tab + tab).append(r.getName());
				if (r.getTypeConstraint() != null) {
					sb.append(colon).append(
							generateLinkForTypeConstraint(r.getTypeConstraint(), b));
				}
				sb.append(formatInheritanceInfo(r.getSource(), currentSource));
				sb.append(lineFeed);
			}
		}
		if (b.getConstraints().size() > 0) {
			sb.append(tab + tab + "constraints" + lineFeed);
			for (Constraint c : b.getConstraints()) {
				sb.append(tab + tab + tab).append(formatConstraint(c));
				sb.append(formatInheritanceInfo(c.getSource(), currentSource));
				sb.append(lineFeed);
			}
		}
		return sb;
	}

	protected String formatConstraint(Constraint c) {
		StringBuffer sb = new StringBuffer();
		List<SlotChain> arguments = c.getArguments();
		String value = c.getValue();
		String operator = c.getOperator();
		if (operator.equals(ECGConstants.IDENTIFY)) {
			operator = html_double_headed_arrow;
		} else if (operator.equals(ECGConstants.ASSIGN)) {
			operator = html_left_arrow;
		}

		if (arguments.size() == 1 && value == null) {
			sb.append(operator).append(" ").append(arguments.get(0));
		} else if (arguments.size() == 1 && value != null) {
			sb.append(arguments.get(0)).append(" ").append(operator).append(" ");

			if (value.charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
				sb.append(generateHTMLLink(value, value.substring(1),
						ECGConstants.ONTOLOGY));
			} else {
				sb.append(value);
			}

		} else if (arguments.size() == 2) {
			sb.append(arguments.get(0)).append(" ").append(operator).append(" ")
					.append(arguments.get(1));

		} else if (arguments.size() > 2) {
			sb.append(operator);
			for (SlotChain argument : arguments) {
				sb.append(" ").append(argument);
			}
		}
		return sb.toString();
	}

	protected String formatInheritanceInfo(String info, String currentSource) {
		StringBuffer sb = new StringBuffer();
		if (currentSource != null && !info.equals(currentSource)) {
			sb.append(tab + "// inherited from ").append(info);
		}
		return sb.toString();
	}

	protected String generateLinkForBlockConstraint(Block b) {

		String text = b.getType();
		String target = b.getType();

		if (target.equals(ECGConstants.UNTYPED)) {
			return text;
		} else {

			String typeSystem = b.getBlockTypeTypeSystem().getName();

			if (typeSystem.equals(ECGConstants.ONTOLOGY)) {
				text = ECGConstants.ONTOLOGYPREFIX + target;
			}
			return generateHTMLLink(text, target, typeSystem);
		}
	}

	protected String generateLinkForTypeConstraint(TypeConstraint c, Block b) {

		String text = c.getType();
		String target = c.getType();
		String typeSystem = c.getTypeSystem().getName();

		if (typeSystem.equals(ECGConstants.ONTOLOGY)) {
			text = ECGConstants.ONTOLOGYPREFIX + target;
		}

		return generateHTMLLink(text, target, typeSystem);
	}

}

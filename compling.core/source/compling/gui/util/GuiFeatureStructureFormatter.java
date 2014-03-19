package compling.gui.util;

import java.util.HashSet;
import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.FeatureStructureFormatter;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;

public class GuiFeatureStructureFormatter implements FeatureStructureFormatter {

	public String format(FeatureStructureSet fss) {
		StringBuilder sb = new StringBuilder();
		Set<FeatureStructureSet.Slot> alreadyDone = new HashSet<FeatureStructureSet.Slot>();
		for (FeatureStructureSet.Slot root : fss.getRootSlots()) {
			if (root.getTypeConstraint() == null) {
				sb.append("Root\n");
			}
			else {
				sb.append(root.getTypeConstraint().getType());
				sb.append(" [" + root.getSlotIndex() + "]\n");
			}
			sb.append(formatHelper(root, alreadyDone, 0).append("\n"));
		}
		return sb.toString();
	}

	private StringBuilder formatHelper(FeatureStructureSet.Slot slot, Set<FeatureStructureSet.Slot> alreadyDone,
			int indent) {
		StringBuilder sb = new StringBuilder();
		if (!slot.hasFiller() || alreadyDone.contains(slot)) {
			return sb;
		}
		alreadyDone.add(slot);
		for (Role name : slot.getFeatures().keySet()) {
			sb.append(makeWhiteSpace(indent + 2)).append(name.toString()).append(": ");
			FeatureStructureSet.Slot childSlot = slot.getSlot(name);
			if (childSlot.getTypeConstraint() != null) {
				sb.append(childSlot.getTypeConstraint().getType());
			}
			sb.append(" [" + childSlot.getSlotIndex() + "]");
			if (childSlot.hasFiller() && !childSlot.hasStructuredFiller()) {
				if (childSlot.hasAtomicFiller()) {
					sb.append(childSlot.getAtom()).append("\n");
				}
				else if (childSlot.isListSlot()) {
					sb.append("  ");
					for (FeatureStructureSet.Slot s : childSlot.listValue) {
						if (s.getTypeConstraint() != null) {
							sb.append(s.getTypeConstraint().getType());
						}
						sb.append(" [").append(s.getSlotIndex()).append("] ,  ");
					}
				}
			}
			else if (childSlot.hasFiller() && childSlot.hasStructuredFiller()) {
				sb.append("\n").append(formatHelper(childSlot, alreadyDone, indent + 4 + name.getName().length()));
			}
			else {
				// it's null
				sb.append("\n");
			}
		}
		return sb;
	}

	public String format(Slot slot) {
		StringBuilder sb = new StringBuilder();
		if (slot.getTypeConstraint() != null) {
			sb.append(slot.getTypeConstraint().getType());
		}
		else {
			sb.append("UNTYPED");
		}
		sb.append(" [" + slot.getSlotIndex() + "]");
		return sb.toString();
	}

	public static StringBuilder makeWhiteSpace(int amount) {
		StringBuilder sb = new StringBuilder("    ");
		for (int i = 0; i < amount; i++) {
			sb.append(" ");
		}
		return sb;
	}

}

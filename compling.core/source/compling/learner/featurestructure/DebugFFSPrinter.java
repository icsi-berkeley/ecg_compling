// =============================================================================
//File        : DebugFFSPrinter.java
//Author      : emok
//Change Log  : Created on Jun 26, 2007
//=============================================================================

package compling.learner.featurestructure;

import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.DefaultStructureFormatter;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.FeatureStructureFormatter;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.util.Pair;

//=============================================================================

public class DebugFFSPrinter extends DefaultStructureFormatter implements FeatureStructureFormatter {

	LearnerCentricAnalysis lca = null;

	public DebugFFSPrinter(LearnerCentricAnalysis lca) {
		super();
		this.lca = lca;
	}

	public String format(Slot slot) {
		StringBuffer sb = new StringBuffer();
		if (slot.getTypeConstraint() != null) {
			sb.append(slot.getTypeConstraint().getType() + "[" + slot.getSlotIndex() + "]");
		}
		else {
			sb.append("UNTYPED [" + slot.getSlotIndex() + "]");
		}
		if (lca != null) {
			Set<Pair<Integer, SlotChain>> slotchains = lca.getTables().getSlotChainTable().get(slot);
			if (slotchains != null) {
				sb.append("\t<==>\t [");
				for (Pair<Integer, SlotChain> pair : slotchains) {
					sb.append(pair.getSecond());
				}
				sb.append("]\n");
			}
		}
		return sb.toString();
	}

}

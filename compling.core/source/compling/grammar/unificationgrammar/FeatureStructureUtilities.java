package compling.grammar.unificationgrammar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.util.Pair;

public class FeatureStructureUtilities {

	public static interface FeatureStructureFormatter {

		public String format(FeatureStructureSet fss);

		public String format(Slot slot);

	}

	public static class DefaultStructureFormatter implements FeatureStructureFormatter {

		public String format(FeatureStructureSet fss) {
			StringBuffer sb = new StringBuffer();
			HashSet<FeatureStructureSet.Slot> alreadyDone = new HashSet<FeatureStructureSet.Slot>();
			for (FeatureStructureSet.Slot root : fss.getRootSlots()) {
				if (root.getTypeConstraint() == null) {
					sb.append("Root\n");
				}
				else {
					sb.append(root.getTypeConstraint().getType());
					sb.append("[" + root.getSlotIndex() + "]\n");
				}
				sb.append(formatHelper(root, alreadyDone, 0).append("\n"));
			}
			return sb.toString();
		}

		private StringBuffer formatHelper(FeatureStructureSet.Slot slot, Set<FeatureStructureSet.Slot> alreadyDone,
				int indent) {
			StringBuffer sb = new StringBuffer();
			if (!slot.hasFiller() || alreadyDone.contains(slot)) {
				return sb;
			}
			alreadyDone.add(slot);
			for (Role name : slot.getFeatures().keySet()) {
				sb.append(makeWhiteSpace(indent + 2)).append(name.toString()).append(":");
				FeatureStructureSet.Slot childSlot = slot.getSlot(name);
				if (childSlot.getTypeConstraint() != null) {
					sb.append(childSlot.getTypeConstraint().getType());
				}
				sb.append("[" + childSlot.getSlotIndex() + "]");
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
					String type = "";
					sb.append("\n").append(formatHelper(childSlot, alreadyDone, indent + 4 + name.getName().length()));
				}
				else { // it's null
					sb.append("\n");
				}
			}
			return sb;
		}

		public String format(Slot slot) {
			StringBuffer sb = new StringBuffer();
			if (slot.getTypeConstraint() != null) {
				sb.append(slot.getTypeConstraint().getType());
			}
			else {
				sb.append("UNTYPED");
			}
			sb.append("[" + slot.getSlotIndex() + "]");
			return sb.toString();
		}

	}

	public static class TexFeatureStructureFormatter implements FeatureStructureFormatter {

		public String format(FeatureStructureSet fss) {
			StringBuffer sb = new StringBuffer();
			HashSet<FeatureStructureSet.Slot> alreadyDone = new HashSet<FeatureStructureSet.Slot>();
			for (FeatureStructureSet.Slot root : fss.getRootSlots()) {
				if (root.getTypeConstraint() == null) {
					sb.append("\\fstruct[Root]{\n");
				}
				else {
					sb.append("\\fstruct[");
					sb.append(root.getTypeConstraint().getType());
					sb.append("]{\n");
					// sb.append("\\ind{" + root.getSlotIndex() + "}\n");
				}
				sb.append(formatHelper(root, alreadyDone, 0, new HashSet<Integer>()).append("}\n"));
			}
			return sb.toString();
		}

		private StringBuffer formatHelper(FeatureStructureSet.Slot slot, HashSet<FeatureStructureSet.Slot> alreadyDone,
				int indent, HashSet<Integer> foundInd) {
			StringBuffer sb = new StringBuffer();
			if (!slot.hasFiller() || alreadyDone.contains(slot)) {
				return sb;
			}
			alreadyDone.add(slot);
			for (Role name : slot.getFeatures().keySet()) {
				if (name.getName().equals("features")) {
					continue;
				}
				sb.append(makeWhiteSpace(indent + 3)).append("\\featval{").append(name.toString()).append("}{");
				FeatureStructureSet.Slot childSlot = slot.getSlot(name);
				// if (childSlot.getTypeConstraint() != null) {
				// sb.append(childSlot.getTypeConstraint().getType());
				// }
				sb.append("\\ind{" + childSlot.getSlotIndex() + "} ");
				if (childSlot.hasFiller() && !childSlot.hasStructuredFiller()) {
					if (childSlot.hasAtomicFiller()) {
						sb.append(childSlot.getAtom()).append("}\n");
					}
					else if (childSlot.isListSlot()) {
						sb.append("  ");
						for (FeatureStructureSet.Slot s : childSlot.listValue) {
							// if (s.getTypeConstraint() != null){
							// sb.append(s.getTypeConstraint().getType());
							// }
							sb.append(" \\ind{").append(s.getSlotIndex()).append("} ,  ");
						}
						sb.append("}\n");
					}
				}
				else if (childSlot.hasFiller() && childSlot.hasStructuredFiller() && !alreadyDone.contains(childSlot)) {
					String type = "";
					if (childSlot.getTypeConstraint() != null) {
						type = childSlot.getTypeConstraint().getType();
					}
					sb.append("\n").append(makeWhiteSpace(indent + 5 + 3)).append("\\fstruct[").append(type).append("]")
							.append("{");
					sb.append("\n").append(formatHelper(childSlot, alreadyDone, indent + 7 + 3, foundInd));
					sb.append(makeWhiteSpace(indent + 5 + 3)).append("}\n");
					sb.append(makeWhiteSpace(indent + 3)).append("}\n");
				}
				else if (!childSlot.hasFiller() && childSlot.getTypeConstraint() != null
						&& !foundInd.contains(childSlot.getSlotIndex())) {
					sb.append(childSlot.getTypeConstraint().getType().toString().toUpperCase()).append("}\n");
					foundInd.add(childSlot.getSlotIndex());
				}
				else { // it's null
					sb.append("}\n");
				}
			}
			return sb;
		}

		public String format(Slot slot) {
			throw new RuntimeException("shouldn't be calling this.");
		}

	}

	public static StringBuffer makeWhiteSpace(int amount) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < amount; i++) {
			sb.append(" ");
		}
		return sb;
	}

	
	protected static class DfsBuilder {
		Set<Pair<SlotChain, Slot>> pairs;
		Set<Slot> done;
		
		public DfsBuilder(FeatureStructureSet featureStructure) {
			this.pairs = new HashSet<Pair<SlotChain, Slot>>();
			this.done = new HashSet<Slot>();
			build(featureStructure);
		}
		
		protected void build(FeatureStructureSet featureStructure) {
			for (Slot s : featureStructure.getRootSlots()) {
				build(new SlotChain(""), s);
			}
		}
		
		protected void build(SlotChain parent, Slot slot) {
//			pairs.add(Pair.make(parent, slot));

			if (! slot.hasFiller() || done.contains(slot))
				return;
			
			done.add(slot);

			for (Map.Entry<Role, Slot> e : slot.features.entrySet()) {
				List<Role> rr = new ArrayList<Role>();
				rr.addAll(parent.getChain());
				rr.add(e.getKey());
				SlotChain current = new SlotChain();
				current.setChain(rr);

				final Slot child = e.getValue();

				pairs.add(Pair.make(current, child));
				
				if (child.hasStructuredFiller())
					build(current, child);
			}
		}
		
		public Set<Pair<SlotChain, Slot>> get() {
			return pairs;
		}
	}
	
	public static Set<Pair<SlotChain, Slot>> getDfs(FeatureStructureSet featureStructure) {
		return new DfsBuilder(featureStructure).get();
	}
	
}

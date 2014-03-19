// =============================================================================
//File        : MultiRootedSlotChainTracker.java
//Author      : emok
//Change Log  : Created on Feb 15, 2008
//=============================================================================

package compling.learner.featurestructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.LearnerException;
import compling.util.MapSet;
import compling.util.Pair;
import compling.util.Triplet;

//=============================================================================

/**
 * NOTE: the coindexed chains returned by this clone table has an extra prefix for the root slot (unlike the slot chain
 * tracker used by the analyzer)
 */

public class MultiRootedSlotChainTracker {

	private MapSet<Integer, Pair<Slot, SlotChain>> allCoindexedPaths = new MapSet<Integer, Pair<Slot, SlotChain>>();
	private Map<SlotChain, Integer> slotChainToIndex = new HashMap<SlotChain, Integer>();
	private MapSet<Integer, Triplet<Slot, List<Role>, Integer>> descendentPaths = new MapSet<Integer, Triplet<Slot, List<Role>, Integer>>();
	private HashSet<Slot> structureTracker;

	public MultiRootedSlotChainTracker(FeatureStructureSet fss) {
		processFSS(fss, false);
	}

	public MultiRootedSlotChainTracker(LearnerCentricAnalysis lca) {
		this(lca, true);
	}

	public MultiRootedSlotChainTracker(LearnerCentricAnalysis lca, boolean includeSlotNumber) {
		for (FeatureStructureSet fss : lca.getFeatureStructureSets()) {
			processFSS(fss, includeSlotNumber);
		}
	}

	protected void processFSS(FeatureStructureSet fss, boolean includeSlotNumber) {
		Set<Slot> rootSlots = fss.getRootSlots();
		for (Slot rootSlot : rootSlots) {
			String startPath = rootSlot.getTypeConstraint().getType();
			if (includeSlotNumber) {
				startPath += "[" + String.valueOf(rootSlot.getSlotIndex()) + "]";
			}
			// System.out.println("Rec find all paths:"+path);
			structureTracker = new HashSet<Slot>();
			List<Role> path = new ArrayList<Role>();
			Role startRole = new Role(startPath);
			startRole.setTypeConstraint(rootSlot.getTypeConstraint());
			path.add(startRole);
			findAllCoindexedPaths(rootSlot, rootSlot, path, fss);
		}
	}

	// this method finds all non-recursive paths that are co-indexed
	private void findAllCoindexedPaths(Slot slot, Slot rootSlot, List<Role> path, FeatureStructureSet fss) {
		// System.out.println(path+"; ");
		SlotChain sc = new SlotChain().setChain(path);
		slotChainToIndex.put(sc, slot.getID());
		addAllDescendentPaths(rootSlot, path, slot.getID(), -1, fss);

		if (!structureTracker.contains(slot)) {
			allCoindexedPaths.put(slot.getID(), new Pair<Slot, SlotChain>(rootSlot, sc));
		}
		else if (structureTracker.contains(slot)) {
			allCoindexedPaths.put(slot.getID(), new Pair<Slot, SlotChain>(rootSlot, sc));

			if (descendentPaths.containsKey(slot.getID())) {
				for (Triplet<Slot, List<Role>, Integer> p : descendentPaths.get(slot.getID())) {
					// System.out.println("\t descendent path" + p.getSecond());
					List<Role> pathClone = new ArrayList<Role>(path);
					pathClone.addAll(p.getSecond());
					SlotChain scp = new SlotChain().setChain(pathClone);
					// System.out.println("\t"+scp);
					slotChainToIndex.put(scp, p.getThird());
					allCoindexedPaths.put(p.getThird(), new Pair<Slot, SlotChain>(rootSlot, scp));
					addAllDescendentPaths(rootSlot, pathClone, p.getThird(), slot.getID(), fss);
				}
			}

			return;
		}
		structureTracker.add(slot);

		if (slot.hasFiller() && slot.hasStructuredFiller()) {
			for (Role role : slot.getFeatures().keySet()) {
				Slot nextSlot = slot.getSlot(role);
				List<Role> newPath = new ArrayList<Role>(path);
				newPath.add(role);
				findAllCoindexedPaths(nextSlot, rootSlot, newPath, fss);
			}
		}
	}

	private void addAllDescendentPaths(Slot rootSlot, List<Role> path, int slotIndex, int sourceIndex,
			FeatureStructureSet fss) {
		for (int i = 1; i < path.size(); i++) {
			SlotChain parentPath = new SlotChain().setChain(path.subList(0, i));
			// if (sourceIndex != -1) {System.out.print("\tparentpath:"+parentPath);}
			if (slotChainToIndex.get(parentPath) == null) {
				SlotChain realPath = new SlotChain().setChain(parentPath.getChain()
						.subList(1, parentPath.getChain().size()));
				if (!fss.hasSlot(rootSlot, realPath)) {
					throw new LearnerException("Unable to locate path : " + realPath + " under the root ROOT["
							+ rootSlot.getSlotIndex() + "]\n" + fss.toString());
				}
				int newIndex = fss.getSlot(rootSlot, realPath).getID();
				slotChainToIndex.put(parentPath, newIndex);
				allCoindexedPaths.put(newIndex, new Pair<Slot, SlotChain>(rootSlot, parentPath));
				descendentPaths.put(newIndex, new HashSet<Triplet<Slot, List<Role>, Integer>>());
			}
			if (!descendentPaths.containsKey(slotChainToIndex.get(parentPath))) {
				descendentPaths.put(slotChainToIndex.get(parentPath), new HashSet<Triplet<Slot, List<Role>, Integer>>());
			}
			if (slotChainToIndex.get(parentPath) != sourceIndex) {
				descendentPaths.put(slotChainToIndex.get(parentPath),
						new Triplet<Slot, List<Role>, Integer>(rootSlot, path.subList(i, path.size()), slotIndex));

			}
			else if (slotChainToIndex.get(parentPath) == sourceIndex
					&& !descendentPaths.contains(slotChainToIndex.get(parentPath), new Triplet<Slot, List<Role>, Integer>(
							rootSlot, path.subList(i, path.size()), slotIndex))) {
				// System.out.print("  this is the weird recursive case where a repeat enters the fray and causes a concurrent mod exception.");
			}
			// if (sourceIndex != -1) {System.out.println("   ---> done  "+path.subList(i, path.size()));}
		}
	}

	public List<Set<Pair<Slot, SlotChain>>> getAllCoindexedSlotChains() {
		ArrayList<Set<Pair<Slot, SlotChain>>> al = new ArrayList<Set<Pair<Slot, SlotChain>>>();
		for (Integer key : allCoindexedPaths.keySet()) {
			al.add(allCoindexedPaths.get(key));
		}
		return al;
	}

	public Set<Pair<Slot, SlotChain>> getAllCoindexedSlotChainsBySlot(Slot slot) {
		return allCoindexedPaths.get(slot.getID());
	}

}

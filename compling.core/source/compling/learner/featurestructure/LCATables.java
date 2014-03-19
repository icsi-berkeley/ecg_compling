// =============================================================================
//File        : LCATables.java
//Author      : emok
//Change Log  : Created on Mar 15, 2007
//=============================================================================

package compling.learner.featurestructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.LearnerException;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class LCATables {

	LearnerCentricAnalysis lca;

	Map<Integer, FeatureStructureSet> allSlots = new HashMap<Integer, FeatureStructureSet>();
	List<Integer> allCxnSlots = new ArrayList<Integer>();
	List<Integer> allNonCxnSlots = new ArrayList<Integer>();
	Map<FeatureStructureSet, List<Integer>> leafSlots = new HashMap<FeatureStructureSet, List<Integer>>();
	Set<Integer> ignoreSlots = new HashSet<Integer>();
	Map<Integer, CxnalSpan> cxnalSpans = new HashMap<Integer, CxnalSpan>();

	MapSet<Integer, Integer> parentslotTable = new MapSet<Integer, Integer>(); // <childSlotID, parentSlotID>
	MapSet<Integer, Pair<Integer, SlotChain>> slotChainTable = new MapSet<Integer, Pair<Integer, SlotChain>>(); 
	// <slotID, Pair<rootID, slotchains>>
	
	Map<Integer, Integer> rdTable = new HashMap<Integer, Integer>(); // <resolvedRef, RD>
	MapSet<Integer, Integer> reverseRDTable = new MapSet<Integer, Integer>(); // <RD, resolvedRef>

	// MapSet<Integer, Integer> cxnRootTable = new MapSet<Integer, Integer>(); // <meaning slotID, slotID of construction
	// that dots into it>

	final static int FSS_MULTIPLIER = 0;
	static final Role RESOLVEDREFROLE = new Role(ECGConstants.RESOLVEDREFERENT);

	public LCATables(LearnerCentricAnalysis lca) {
		this.lca = lca;
		updateBasicTables();
	}

	public void clearAllTables() {
		allSlots.clear();
		allCxnSlots.clear();
		allNonCxnSlots.clear();
		ignoreSlots.clear();
		parentslotTable.clear();
		slotChainTable.clear();
		rdTable.clear();
		reverseRDTable.clear();
	}

	public void updateBasicTables() {
		clearAllTables();

		indexAllSlots();
		buildSlotChainTables();
		buildRDTable();
		addIgnoreSlots(lca.findDummyFillerSlots());
		addCxnalSpans(lca.getCxnalSpans());
	}

	public void addIgnoreSlot(Integer slotID) {
		ignoreSlots.add(slotID);
	}

	public void addIgnoreSlots(Set<Integer> slotIDs) {
		ignoreSlots.addAll(slotIDs);
	}

	public Set<Integer> getIgnoreSlots() {
		return ignoreSlots;
	}

	public void addCxnalSpan(Integer slotID, CxnalSpan span) {
		cxnalSpans.put(slotID, span);
	}

	public void addCxnalSpans(Map<Integer, CxnalSpan> cxnalSpans) {
		this.cxnalSpans.putAll(cxnalSpans);
	}

	public CxnalSpan getCxnalSpan(Integer slotID) {
		return cxnalSpans.get(slotID);
	}

	public Map<Integer, CxnalSpan> getAllCxnalSpans() {
		return cxnalSpans;
	}

	public boolean isInTheConstructionalTreeUnder(CxnalSpan child, CxnalSpan parent) {
		int childID = child.getSlotID();
		int parentID = parent.getSlotID();

		List<Integer> ancestors = new ArrayList<Integer>();
		int current = childID;
		while (getParentSlots(current) != null) {
			current = getParentSlots(current).iterator().next();
			ancestors.add(current);
		}
		return ancestors.contains(parentID);
	}

	public Integer getRootSlot(Integer slotID) {
		return allSlots.get(slotID).getMainRoot().getID();
	}

	public Set<Integer> getAllSlots() {
		return allSlots.keySet();
	}

	public List<Integer> getAllCxnSlots() {
		return allCxnSlots;
	}

	public List<Integer> getAllNonCxnSlots() {
		return allNonCxnSlots;
	}

	public FeatureStructureSet getContainingFSS(Integer slotID) {
		return allSlots.get(slotID);
	}

	/**
	 * This method guarantees that slots are indexed in breadth-first order.
	 */
	protected void indexAllSlots() {
		for (FeatureStructureSet fss : lca.getFeatureStructureSets()) {
			LinkedList<Slot> toProcess = new LinkedList<Slot>();
			Set<Slot> visited = new HashSet<Slot>();
			List<Integer> leaves = new ArrayList<Integer>();

			for (Slot rootSlot : fss.getRootSlots()) {
				toProcess.add(rootSlot);
			}

			while (!toProcess.isEmpty()) {
				Slot slot = toProcess.remove();
				if (!visited.contains(slot)) {
					visited.add(slot);
					Integer slotID = slot.getID();

					allSlots.put(slotID, fss);
					if (slot.getTypeConstraint() != null) {
						if (slot.getTypeConstraint().getTypeSystem().getName().equals(ECGConstants.CONSTRUCTION)) {
							allCxnSlots.add(slotID);
						}
						else {
							allNonCxnSlots.add(slotID);
						}
					}

					if (slot.hasStructuredFiller()) {
						for (Slot next : slot.getFeatures().values()) {
							toProcess.add(next);
							parentslotTable.put(next.getID(), slotID);
						}
					}
					else {
						leaves.add(slotID);
					}
				}
			}
			leafSlots.put(fss, leaves);
		}

	}

	public Set<Integer> getParentSlots(Integer slotID) {
		return parentslotTable.get(slotID);
	}

	protected List<Integer> findLeaveSlots(FeatureStructureSet fss) {
		return leafSlots.get(fss);
	}

	/**
	 * This uses the <code>SlotChainTracker</code> to build a table of slot chains leading to the leave nodes (i.e. the
	 * ones that do not have structured fillers).
	 */

	protected void buildSlotChainTables() {

		MultiRootedSlotChainTracker tracker = new MultiRootedSlotChainTracker(lca);

		for (FeatureStructureSet fss : lca.getFeatureStructureSets()) {
			for (Slot slot : fss.getSlots()) {
				Set<Pair<Slot, SlotChain>> chains = tracker.getAllCoindexedSlotChainsBySlot(slot);

				if (chains == null) {
					throw new LearnerException("Slot is not tracked for its slot chains!! " + slot + "\n" + lca);
				}
				else if (chains.isEmpty()) {
					throw new LearnerException("Slot has no coindexed slot chains!! " + slot + "\n" + lca);
				}

				for (Pair<Slot, SlotChain> chain : chains) {
					slotChainTable.put(slot.getID(),
							new Pair<Integer, SlotChain>(chain.getFirst().getID(), chain.getSecond()));
				}
			}
		}
	}

	protected void buildRDTable() {
		for (FeatureStructureSet fss : lca.getFeatureStructureSets()) {
			for (Slot slot : fss.getSlots()) {
				if (slot.getTypeConstraint() != null && slot.getTypeConstraint().getType().equals(ECGConstants.RD)) {
					Slot resolvedRefSlot = slot.getFeatures().get(RESOLVEDREFROLE);
					if (resolvedRefSlot != null) {
						rdTable.put(resolvedRefSlot.getID(), slot.getID());
						reverseRDTable.put(slot.getID(), resolvedRefSlot.getID());
					}
				}
			}
		}
	}

	public Integer getRelatedRD(Integer slotID) {
		return rdTable.get(slotID);
	}

	public Set<Integer> getSlotsFilledByRD(Integer slotID) {
		return reverseRDTable.get(slotID);
	}

	public MapSet<Integer, Pair<Integer, SlotChain>> getSlotChainTable() {
		return slotChainTable;
	}

	public MapSet<Integer, SlotChain> getSlotChainTableForLeaves(FeatureStructureSet fss) {
		List<Integer> leaves = findLeaveSlots(fss);
		MapSet<Integer, SlotChain> result = new MapSet<Integer, SlotChain>();
		for (Integer slotID : slotChainTable.keySet()) {
			if (leaves.contains(slotID)) {
				for (Pair<Integer, SlotChain> chain : slotChainTable.get(slotID)) {
					result.put(slotID, chain.getSecond());
				}
			}
		}

		return result;
	}
}

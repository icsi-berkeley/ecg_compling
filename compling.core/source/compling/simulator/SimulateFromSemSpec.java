// =============================================================================
// File        : SimulateFromSemSpec.java
// Author      : emok
// Change Log  : Created on Jun 1, 2008
//=============================================================================

package compling.simulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Logger;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.util.LearnerUtilities;
import compling.util.MapFactory.HashMapFactory;
import compling.util.MapSet;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public class SimulateFromSemSpec {
	Grammar currentGrammar = null;
	LearnerCentricAnalysis currentLCA = null;
	String currentDS = null;
	String currentSpeechAct = null;
	String currentSpeechActType = null;
	String currentSpeaker = null;
	String currentAddressee = null;
	Collection<String> jointAttention = null;
	LCATables tables = null;

	public enum SimulatableType {
		ED, COMPLEX_PROCESS, STRUCTURED_SIMPLE_PROCESS, UNSTRUCTURED_SIMPLE_PROCESS
	}

	MapSet<SimulatableType, Integer> simulatableByType = new MapSet<SimulatableType, Integer>(
			new HashMapFactory<SimulatableType, Set<Integer>>(), new LinkedHashSetFactory<Integer>());

	static Logger logger = Logger.getLogger(SimulateFromSemSpec.class.getName());

	public SimulateFromSemSpec(Grammar currentGrammar, LearnerCentricAnalysis lca) {
		this.currentGrammar = currentGrammar;
		this.currentLCA = lca;
		currentDS = lca.getCurrentDS();
		currentSpeechAct = lca.getCurrentSpeechAct();
		currentSpeechActType = lca.getCurrentSpeechActType();
		currentAddressee = lca.getCurrentAddressee();
		jointAttention = lca.getJointAttention();

		findSimulatable();
	}

	protected void findSimulatable() {
		logger.finest("slots to ignore: ");
		for (Integer slotID : tables.getIgnoreSlots()) {
			Slot slot = currentLCA.getSlot(slotID);
			logger.finest(slot.toString());
		}

		// again, this relies on lcaTable's guarantee that slots are returned in breadth-first order
		for (Integer slotID : tables.getAllNonCxnSlots()) {
			Slot slot = currentLCA.getSlot(slotID);
			TypeConstraint typeConstraint = currentLCA.getTypeConstraint(slotID);
			if (!tables.getIgnoreSlots().contains(slotID)) {
				if (LearnerUtilities.isEventDescriptor(currentGrammar, typeConstraint)) {
					simulatableByType.put(SimulatableType.ED, slotID);
				}
				else if (LearnerUtilities.isProcess(currentGrammar, typeConstraint)) {
					if (LearnerUtilities.isComplexProcess(currentGrammar, typeConstraint)) {
						simulatableByType.put(SimulatableType.COMPLEX_PROCESS, slotID);
					}
					else if (slot.hasStructuredFiller()) {
						simulatableByType.put(SimulatableType.STRUCTURED_SIMPLE_PROCESS, slotID);
					}
					else {
						simulatableByType.put(SimulatableType.UNSTRUCTURED_SIMPLE_PROCESS, slotID);
					}
				}
			}
		}
	}

	public LinkedHashMap<String, SimulationParameters> createSimulationInstructions() {
		Set<Integer> touched = new HashSet<Integer>();
		LinkedHashMap<String, SimulationParameters> simulations = new LinkedHashMap<String, SimulationParameters>();

		// How can the nesting be carried out with the current Script design?
		Set<Integer> eventDescriptors = simulatableByType.get(SimulatableType.ED);
		if (eventDescriptors != null) {

		}

		return simulations;
	}
}

// =============================================================================
//File        : CompositionFinder.java
//Author      : emok
//Change Log  : Created on Apr 5, 2007
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.context.ContextModel;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.AnalysisVerifier.Correctness;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.grammartables.SchemaReachabilityTable;
import compling.learner.grammartables.SimpleExpander;
import compling.learner.grammartables.SimpleExpander.ExpansionType;
import compling.learner.grammartables.Substitution;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.SemSpecScorer.BasicScorer;
import compling.util.LookupTable;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class CompositionFinder {

	LearnerCentricAnalysis lca;
	LCATables lcaTables;
	LearnerGrammar learnerGrammar;
	Grammar grammar;
	GrammarTables grammarTables;
	BasicScorer semanticModel;

	boolean useGoldStandard;
	static Role referentRole = null;
	static final boolean getMaximallyConnectedConstructions = true;

	static Logger logger = Logger.getLogger(CompositionFinder.class.getName());

	public CompositionFinder(LearnerGrammar learnerGrammar, LearnerCentricAnalysis lca, boolean useGoldStandard) {
		this.learnerGrammar = learnerGrammar;
		this.grammar = learnerGrammar.getGrammar();
		this.grammarTables = learnerGrammar.getGrammarTables();
		this.lca = lca;
		lcaTables = lca.getTables();
		this.useGoldStandard = useGoldStandard;
		referentRole = grammar.getSchema(ChildesLocalizer.eventDescriptorTypeName).getRole(
				ChildesLocalizer.referentRoleName);
	}

	int resultLimit = 10;

	public void setResultLimit(int n) {
		resultLimit = n;
	}

	public List<CompositionCandidate> findCandidates(ContextualFit fit, boolean includeDiscourse) {

		List<CompositionCandidate> candidates = new ArrayList<CompositionCandidate>();
		List<CompositionCandidate> singleUnificationCandidates = generateSingleUnificationCandidatesOp1(fit,
				includeDiscourse);

		if (getMaximallyConnectedConstructions) {
			LookupTable<CompositionCandidate, CompositionCandidate> graph = new LookupTable<CompositionCandidate, CompositionCandidate>();

			ListIterator<CompositionCandidate> i = singleUnificationCandidates.listIterator();
			while (i.hasNext()) {
				CompositionCandidate candidateI = i.next();
				graph.incrementCount(candidateI, candidateI, 1); // self loop -- mainly to avoid null pointers when
																					// traversing'm
				ListIterator<CompositionCandidate> j = singleUnificationCandidates.listIterator(i.nextIndex());
				while (j.hasNext()) {
					CompositionCandidate candidateJ = j.next();
					if (overlapsInConstituents(candidateI, candidateJ)) {
						graph.incrementCount(candidateI, candidateJ, 1);
						graph.incrementCount(candidateJ, candidateI, 1);
					}
				}
			}

			// at this point a number of different learning strategies can be deployed. Some options:
			// 1. learn the biggest cxn by picking the largest connected subgraph (more optimal?)
			// 2. learn cxn with less than n constituents (small processing window)
			// 3. learn cxn with a coherent meaning (i.e. either the meaning graph has a single root or there is a
			// non-compositional meaning)

			int graphLimit = graph.keySet().size(); // this is just to initialze the lastCxnSize;
			int loopCount = 0;

			while (!singleUnificationCandidates.isEmpty() && graphLimit > 0) {
				List<CompositionCandidate> connectedConstituents = findConnected(singleUnificationCandidates.get(0), graph,
						graphLimit);
				CompositionCandidate bigCxn = new CompositionCandidate(grammar);
				bigCxn.incorporate(connectedConstituents.get(0));
				for (CompositionCandidate candidate : connectedConstituents.subList(1, connectedConstituents.size())) {
					CompositionCandidate left, right;
					if (bigCxn.getSpanLeft() <= candidate.getSpanLeft()) {
						left = bigCxn;
						right = candidate;
					}
					else {
						left = candidate;
						right = bigCxn;
					}
					if (right.getSpanLeft() - left.getSpanRight() < 1) {
						bigCxn.incorporate(candidate);
					}
				}

				// even though the constituents may be connected, the combined meaning is not necessarily coherent
				// createMeaningfulCxn is guaranteed to either use up all the constituents or not at all
				List<CompositionCandidate> cxns = createMeaningfulCxn(bigCxn);
				if (cxns.isEmpty()) {
					// nothing meaningful came out of the last (too-big) cxn. To avoid looping infinitely, try finding a
					// smaller graph
					graphLimit = connectedConstituents.size() - 1;
				}
				else {
					candidates.addAll(cxns);
					singleUnificationCandidates.removeAll(connectedConstituents);
				}
				loopCount++;
			}
		}

		for (CompositionCandidate remainingCandidate : singleUnificationCandidates) {
			candidates.addAll(createMeaningfulCxn(remainingCandidate));
		}

		return candidates;
	}

	private boolean overlapsInConstituents(CompositionCandidate i, CompositionCandidate j) {
		List<CxnalSpan> iSpans = new ArrayList<CxnalSpan>(i);
		iSpans.retainAll(j);
		return !iSpans.isEmpty();
	}

	protected List<CompositionCandidate> createMeaningfulCxn(CompositionCandidate candidate) {

		List<CompositionCandidate> candidates = new ArrayList<CompositionCandidate>();

		if (candidate.isEmpty())
			return candidates;

		List<CxnalSpan> constituentsWithMeaningRoot = new ArrayList<CxnalSpan>(candidate);

		// go through the constituents and discard the ones whose meaning poles are involved in constraints
		// (i.e. these meaning schemas are role fillers of some other schema)
		List<List<Pair<CxnalSpan, ECGSlotChain>>> constraintSets = candidate.getUnificationConstraints();

		for (List<Pair<CxnalSpan, ECGSlotChain>> constraintSet : constraintSets) {
			for (Pair<CxnalSpan, ECGSlotChain> constraint : constraintSet) {
				if (constraint.getSecond().getChain().size() == 2
						&& constraint.getSecond().getChain().get(1).getName().equals(ECGConstants.MEANING_POLE)) {
					constituentsWithMeaningRoot.remove(constraint.getFirst());
				}
			}
		}

		// If there are more than one constituents that are "roots" of the meaning,
		// a non-compositional meaning is needed to cover these roots.

		if (constituentsWithMeaningRoot.size() == 0) {
			// If no constituents remain, somehow all the constituents.m are bound to something else.
			// The most likely case is when two reduplicated elements are bound together.
			// No clear "winner" here, so just pick one of the two.

			candidates.add(makePhrasalConstruction(candidate, candidate.get(0)));

		}
		else if (constituentsWithMeaningRoot.size() == 1) {
			// If there is only one constituent remaining, this is the "root" of the meaning,
			// If the root is a process, make a clausal cxn and use this as the event type.
			// Otherwise just make a phrasal cxn with it as the meaning pole.

			TypeConstraint rootMeaning = constituentsWithMeaningRoot.get(0).getType().getMeaningBlock()
					.getTypeConstraint();
			if (LearnerUtilities.isProcess(grammar, rootMeaning)) {
				candidates.add(makeClausalCxn(candidate, constituentsWithMeaningRoot.get(0), rootMeaning));
			}
			else {
				// FIXME: but this misses things like negation and benefaction, which will take the verb meaning in its
				// process role
				candidates.add(makePhrasalConstruction(candidate, constituentsWithMeaningRoot.get(0)));
			}

		}
		else {
			// more than one roots, but if only one of them has a process meaning, use it as the meaning root and proceed
			// as normal
			List<CxnalSpan> meaningRootsThatAreProcesses = new ArrayList<CxnalSpan>();
			for (CxnalSpan s : constituentsWithMeaningRoot) {
				TypeConstraint rootMeaning = s.getType().getMeaningBlock().getTypeConstraint();
				if (LearnerUtilities.isProcess(grammar, rootMeaning)) {
					meaningRootsThatAreProcesses.add(s);
				}
			}
			if (meaningRootsThatAreProcesses.size() == 1) {
				candidates.add(makeClausalCxn(candidate, meaningRootsThatAreProcesses.get(0), meaningRootsThatAreProcesses
						.get(0).getType().getMeaningBlock().getTypeConstraint()));
			}
			else {
				// multiple processes in the meaning. No choice but to find a non-compositional meaning
				logger.warning("non-compositional meaning required for compose");
				// candidates.addAll(makeNonCompositionalCxn(candidate, constituentsWithMeaningRoot));
			}
		}
		return candidates;
	}

	protected CompositionCandidate makePhrasalConstruction(CompositionCandidate candidate, CxnalSpan unifiedWithSelfM) {
		candidate.setMBlockType(unifiedWithSelfM.getType().getMeaningBlock().getTypeConstraint());

		List<Pair<CxnalSpan, ECGSlotChain>> constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(ECGConstants.SELF + "."
				+ ECGConstants.MEANING_POLE)));
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(unifiedWithSelfM, new ECGSlotChain("w."
				+ ECGConstants.MEANING_POLE)));
		// the first role of the slotchain doesn't matter: CompositionCandidate.createNewConstruction() will replace it
		// with the local name
		candidate.addUnificationConstraint(constraint);

		candidate.addParent(ChildesLocalizer.PHRASE);
		return candidate;
	}

	protected CompositionCandidate makeClausalCxn(CompositionCandidate candidate, CxnalSpan constituentWithMeaningRoot,
			TypeConstraint rootMeaning) {
		// this will have a EventDescriptor as its meaning pole by inheriting from the Clause construction
		// self.m.event_type should be constrained to the rootMeaning
		// self.m.profiled_process should be bound to this constituent.m

		// even though the meaning pole is inherited, it doesn't get updated until the grammar is re-instantiated,
		// and that causes problem in various checks that happen before the reinstantiation. So it's updated here.
		candidate.setMBlockType(getSchemaTypeConstraint(ChildesLocalizer.eventDescriptorTypeName));

		// the event type is both bound to the constituent's mpole and assigned a type constraint
		Pair<CxnalSpan, ECGSlotChain> slotChain = new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(
				ECGConstants.SELF + "." + ECGConstants.MEANING_POLE + "." + ChildesLocalizer.eventTypeRoleName));
		Pair<Pair<CxnalSpan, ECGSlotChain>, String> assignmentConstraint = new Pair<Pair<CxnalSpan, ECGSlotChain>, String>(
				slotChain, rootMeaning.getType());
		candidate.addAssignmentConstraint(assignmentConstraint);

		List<Pair<CxnalSpan, ECGSlotChain>> constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		constraint.add(slotChain);
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(constituentWithMeaningRoot, new ECGSlotChain("w."
				+ ECGConstants.MEANING_POLE)));
		candidate.addUnificationConstraint(constraint);

		// this is perhaps unnecessary, since this RD will be unified with the verb's RD, and the verb's
		// RD.profiled_process should be bound to self.m in the lexicon. But just in case.
		constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(ECGConstants.SELF + "."
				+ ECGConstants.MEANING_POLE + "." + ChildesLocalizer.profiledProcessRoleName)));
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(constituentWithMeaningRoot, new ECGSlotChain("w."
				+ ECGConstants.MEANING_POLE)));
		candidate.addUnificationConstraint(constraint);

		// additionally unify the event descriptor in the meaning pole with the verb's event descriptor
		constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(ECGConstants.SELF + "."
				+ ECGConstants.MEANING_POLE)));
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(constituentWithMeaningRoot, new ECGSlotChain("w."
				+ ChildesLocalizer.eventDescriptorRoleName)));
		candidate.addUnificationConstraint(constraint);

		// and unify the event structure role in the meaning pole with the verb's event structure
		constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(ECGConstants.SELF + "."
				+ ECGConstants.MEANING_POLE + "." + ChildesLocalizer.eventStructureRoleName)));
		constraint.add(new Pair<CxnalSpan, ECGSlotChain>(constituentWithMeaningRoot, new ECGSlotChain("w."
				+ ChildesLocalizer.eventStructureRoleName)));
		candidate.addUnificationConstraint(constraint);

		// add RD for each of the roles of the meaning pole schema (but not the evoked).
		// this assumes that all the roles in the schema are core, which is (mostly) true for the starter learner grammar
		TypeConstraint RD = getSchemaTypeConstraint(ECGConstants.RD);
		Schema process = grammar.getSchema(rootMeaning.getType());

		// heuristically selects core roles
		Set<String> coreRoleNames = grammarTables.getCoreRolesTable().get(process.getName());
		Set<Role> coreRoles = new HashSet<Role>();
		for (String name : coreRoleNames) {
			coreRoles.add(process.getRole(name));
		}
		MapSet<Role, Pair<CxnalSpan, Role>> rdMapping = findRDMapping(candidate, constituentWithMeaningRoot, coreRoles);

		int i = 0;
		Slot meaningRootSlot = lca.getSlot(constituentWithMeaningRoot.getSlotID()).getSlot(
				new Role(ECGConstants.MEANING_POLE));
		for (Role coreRole : coreRoles) {
			Slot roleSlot = meaningRootSlot.getSlot(coreRole);

			String rdName = String.format("rd%1$01d", i++);
			candidate.addEvokes(rdName, RD);

			// if there is already an RD evoked by the constituent whose meaning is bound to the role, unify the RDs.
			// otherwise bind the resolved ref of this RD to the role

			List<Pair<CxnalSpan, ECGSlotChain>> referent = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
			referent.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(rdName + "."
					+ ECGConstants.RESOLVEDREFERENT)));
			referent.add(new Pair<CxnalSpan, ECGSlotChain>(constituentWithMeaningRoot, new ECGSlotChain("w."
					+ ECGConstants.MEANING_POLE + "." + coreRole.getName())));
			candidate.addUnificationConstraint(referent);

			if (rdMapping.containsKey(coreRole)) {
				// directly unify the two RD's, and defer the rest of the constraints to the constituent's RD
				List<Pair<CxnalSpan, ECGSlotChain>> rdUnification = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
				rdUnification.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(rdName)));
				for (Pair<CxnalSpan, Role> rdPath : rdMapping.get(coreRole)) {
					rdUnification.add(new Pair<CxnalSpan, ECGSlotChain>(rdPath.getFirst(), new ECGSlotChain("w."
							+ rdPath.getSecond().getName())));
				}
				candidate.addUnificationConstraint(rdUnification);

			}
			else {
				// ontological category is the type of the context element found in context
				String fitCandidate = lca.getContextualFit().getCandidate(roleSlot.getID());
				if (fitCandidate != null) {
					String contextElementType = ContextModel.getIndividualType(fitCandidate);
					slotChain = new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(rdName + "."
							+ ECGConstants.ONTOLOGICALCATEGORY));
					Pair<Pair<CxnalSpan, ECGSlotChain>, String> ontologicalCategory = new Pair<Pair<CxnalSpan, ECGSlotChain>, String>(
							slotChain, ECGConstants.ONTOLOGYPREFIX + contextElementType);
					candidate.addAssignmentConstraint(ontologicalCategory);

					// discourse participant role is what's found in context, if present
					String discourseRole = findDiscourseRole(fitCandidate);
					if (discourseRole != null) {
						slotChain = new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(rdName + "."
								+ ECGConstants.discourseParticipantRoleRoleName));
						Pair<Pair<CxnalSpan, ECGSlotChain>, String> discourseParticipantRole = new Pair<Pair<CxnalSpan, ECGSlotChain>, String>(
								slotChain, ECGConstants.ONTOLOGYPREFIX + discourseRole);
						candidate.addAssignmentConstraint(discourseParticipantRole);
					}
				}
			}
		}

		candidate.addParent(ChildesLocalizer.CLAUSE);
		return candidate;

	}

	private MapSet<Role, Pair<CxnalSpan, Role>> findRDMapping(CompositionCandidate candidate,
			CxnalSpan constituentWithMeaningRoot, Set<Role> coreRoles) {
		Map<Construction, Role> cxnsWithRD = grammarTables.getSelfmRDLookupTable();
		MapSet<Role, Pair<CxnalSpan, Role>> rdMapping = new MapSet<Role, Pair<CxnalSpan, Role>>();

		for (CxnalSpan span : candidate) {
			Construction localized = grammar.getConstruction(span.getType().getName());
			if (cxnsWithRD.containsKey(localized)) {

				// This constituent has an RD in its meaning pole. Find which core role this constituent's meaning pole is
				// bound to.
				for (List<Pair<CxnalSpan, ECGSlotChain>> c : candidate.getUnificationConstraints()) {

					boolean isBound = false;
					for (Pair<CxnalSpan, ECGSlotChain> sc : c) {
						if (sc.getFirst() == span && sc.getSecond().getChain().size() == 2
								&& sc.getSecond().getChain().get(1).getName().equals(ECGConstants.MEANING_POLE)) {
							isBound = true;
							break;
						}
					}

					if (isBound) {
						for (Pair<CxnalSpan, ECGSlotChain> sc : c) {
							List<Role> chain = sc.getSecond().getChain();
							if (sc.getFirst() == constituentWithMeaningRoot) {
								for (Role coreRole : coreRoles) {
									if (chain.size() == 3 && chain.get(1).getName().equals(ECGConstants.MEANING_POLE)
											&& chain.get(2).getName().equals(coreRole.getName())) {
										rdMapping.put(coreRole, new Pair<CxnalSpan, Role>(span, cxnsWithRD.get(localized)));
									}
								}
							}
						}
					}
				}
			}
		}

		return rdMapping;
	}

	protected String findDiscourseRole(String fitCandidate) {
		if (fitCandidate.equals(lca.getCurrentAddressee())) {
			return ECGConstants.addresseeTypeName;
		}
		else if (fitCandidate.equals(lca.getCurrentSpeaker())) {
			return ECGConstants.speakerTypeName;
		}
		else if (lca.getJointAttention().contains(fitCandidate)) {
			return ECGConstants.attentionalFocusTypeName;
		}
		return null;
	}

	protected List<CompositionCandidate> makeNonCompositionalCxn(CompositionCandidate candidate,
			List<CxnalSpan> constituentsWithMeaningRoot) {

		List<CompositionCandidate> candidates = new ArrayList<CompositionCandidate>();

		MapSet<TypeConstraint, CxnalSpan> csts = new MapSet<TypeConstraint, CxnalSpan>();
		Map<TypeConstraint, Integer> types = new HashMap<TypeConstraint, Integer>();
		for (CxnalSpan span : constituentsWithMeaningRoot) {
			TypeConstraint mType = span.getType().getMeaningBlock().getTypeConstraint();
			csts.put(mType, span);
			if (types.containsKey(mType)) {
				types.put(mType, types.get(mType) + 1);
			}
			else {
				types.put(mType, 1);
			}
		}

		// consider making the temporal ordering of processes obey the form ordering of the words (sequential order of
		// narration),
		// though there are cases like "before you leave, lock the door" (zou3 zhi1 qian2 guan4 men2).

		// NOTE: this way of proposing non-compositional meaning is error-prone. Is there any use in checking against the
		// context model?
		SchemaReachabilityTable reachabilityTable = grammarTables.getSchemaReachabilityTable();
		reachabilityTable.setQueryExpander(new SimpleExpander(grammar, ExpansionType.Supertype));
		Map<Substitution<TypeConstraint>, Set<TypeConstraint>> coveringTypes = reachabilityTable.findCoveringTypes(types);

		// In a sense this is looking for the best schema to explain the relation between two things.
		// For the sake of simplicity, avoid speech act schemas and event descriptors, both of which are linguistic
		// notions.
		removeLinguisticSchemas(coveringTypes);
		if (coveringTypes.isEmpty()) {
			logger.fine("No suitable non-compositional meaning found for " + candidate.getCxnName());
			return candidates;
		}

		double bestScore = Double.NEGATIVE_INFINITY;
		Map<TypeConstraint, Map<CxnalSpan, List<SlotChain>>> bestNonCompMeaning = new HashMap<TypeConstraint, Map<CxnalSpan, List<SlotChain>>>();

		for (Substitution<TypeConstraint> substitution : coveringTypes.keySet()) {
			for (TypeConstraint potentialMPole : coveringTypes.get(substitution)) {
				Pair<Double, Map<CxnalSpan, List<SlotChain>>> semanticFit = tryMeaning(substitution, reachabilityTable,
						potentialMPole, csts);
				if (semanticFit.getFirst() == bestScore) {
					bestNonCompMeaning.put(potentialMPole, semanticFit.getSecond());
				}
				else if (semanticFit.getFirst() > bestScore) {
					bestScore = semanticFit.getFirst();
					bestNonCompMeaning.clear();
					bestNonCompMeaning.put(potentialMPole, semanticFit.getSecond());
				}
			}
		}

		Set<TypeConstraint> mostGeneral = new HashSet<TypeConstraint>(bestNonCompMeaning.keySet());
		if (bestNonCompMeaning.size() > 1) {
			Iterator<TypeConstraint> iter = mostGeneral.iterator();
			while (iter.hasNext()) {
				TypeConstraint mPole = iter.next();
				for (String parentType : grammar.getSchema(mPole.getType()).getParents()) {
					if (bestNonCompMeaning.containsKey(getSchemaTypeConstraint(parentType))) {
						iter.remove();
						break;
					}
				}
			}
		}

		if (mostGeneral.isEmpty()) {
			throw new LearnerException("Bizarre error: cannot find most general non compositional meaning among: "
					+ bestNonCompMeaning.keySet());

		}
		else if (mostGeneral.size() == 1) {
			TypeConstraint mPole = mostGeneral.iterator().next();
			logger.fine("non compositional meaning for " + candidate.getCxnName() + " is " + mPole.getType()
					+ " at semantic score of " + bestScore);
			candidates.add(fixupNonCompositionalMeaningBinding(candidate, mPole, bestNonCompMeaning.get(mPole)));

		}
		else {
			for (TypeConstraint mPole : mostGeneral) {
				logger.fine("non compositional meaning for " + candidate.getCxnName() + " is " + mPole.getType()
						+ " at semantic score of " + bestScore);
				CompositionCandidate candidateCopy = new CompositionCandidate(candidate);
				candidates.add(fixupNonCompositionalMeaningBinding(candidateCopy, mPole, bestNonCompMeaning.get(mPole)));
			}
		}

		return candidates;
	}

	private CompositionCandidate fixupNonCompositionalMeaningBinding(CompositionCandidate candidate,
			TypeConstraint bestMPole, Map<CxnalSpan, List<SlotChain>> bestMapping) {

		if (LearnerUtilities.isProcess(grammar, bestMPole)) {
			return makeCxnWithEventDescriptorMeaning(candidate, bestMPole, bestMapping);
		}

		candidate.setMBlockType(bestMPole);
		// do something crazy with the best Mapping
		for (CxnalSpan span : bestMapping.keySet()) {
			List<Pair<CxnalSpan, ECGSlotChain>> constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();

			List<Role> mappedTo = LearnerUtilities.findShortestChain(bestMapping.get(span)).getChain();
			SlotChain coindexedWith = new SlotChain().setChain(mappedTo.subList(1, mappedTo.size()));

			constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(ECGConstants.SELF + "."
					+ ECGConstants.MEANING_POLE + "." + coindexedWith.toString())));
			constraint.add(new Pair<CxnalSpan, ECGSlotChain>(span, new ECGSlotChain("w." + ECGConstants.MEANING_POLE)));
			candidate.addUnificationConstraint(constraint);
			candidate.addParent(ChildesLocalizer.PHRASE);
		}
		return candidate;
	}

	protected CompositionCandidate makeCxnWithEventDescriptorMeaning(CompositionCandidate candidate,
			TypeConstraint eventTypeConstraint, Map<CxnalSpan, List<SlotChain>> bestMapping) {
		// even though the meaning pole is inherited, it doesn't get updated until the grammar is re-instantiated,
		// and that causes problem in various checks that happen before the reinstantiation. So it's updated here.
		candidate.setMBlockType(getSchemaTypeConstraint(ChildesLocalizer.eventDescriptorTypeName));

		// the event type is both bound to the constituent's mpole and assigned a type constraint
		Pair<CxnalSpan, ECGSlotChain> slotChain = new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(
				ECGConstants.SELF + "." + ECGConstants.MEANING_POLE + "." + ChildesLocalizer.eventTypeRoleName));
		Pair<Pair<CxnalSpan, ECGSlotChain>, String> assignmentConstraint = new Pair<Pair<CxnalSpan, ECGSlotChain>, String>(
				slotChain, eventTypeConstraint.getType());
		candidate.addAssignmentConstraint(assignmentConstraint);

		String evokedProcessName = "ncProcess";
		candidate.addEvokes(evokedProcessName, eventTypeConstraint);

		List<Pair<CxnalSpan, ECGSlotChain>> evokeConstraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		evokeConstraint.add(slotChain);
		evokeConstraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(evokedProcessName)));
		candidate.addUnificationConstraint(evokeConstraint);

		for (CxnalSpan span : bestMapping.keySet()) {
			List<Pair<CxnalSpan, ECGSlotChain>> constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();

			List<Role> mappedTo = LearnerUtilities.findShortestChain(bestMapping.get(span)).getChain();
			SlotChain coindexedWith = new SlotChain().setChain(mappedTo.subList(1, mappedTo.size()));

			constraint.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(evokedProcessName + "."
					+ coindexedWith.toString())));
			constraint.add(new Pair<CxnalSpan, ECGSlotChain>(span, new ECGSlotChain("w." + ECGConstants.MEANING_POLE)));
			candidate.addUnificationConstraint(constraint);
			candidate.addParent(ChildesLocalizer.CLAUSE);
		}

		// add RD for each of the roles of the non compositional process schema.
		// this assumes that all the roles in the schema are core, which is (mostly) true for the starter learner grammar
		TypeConstraint RD = getSchemaTypeConstraint(ECGConstants.RD);
		Schema process = grammar.getSchema(eventTypeConstraint.getType());

		// heuristically selects core roles
		Set<String> coreRoleNames = grammarTables.getCoreRolesTable().get(process.getName());
		int i = 0;
		for (String coreRoleName : coreRoleNames) {
			String rdName = String.format("rd%1$01d", i++);
			candidate.addEvokes(rdName, RD);
			List<Pair<CxnalSpan, ECGSlotChain>> referent = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
			referent.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(rdName + "."
					+ ECGConstants.RESOLVEDREFERENT)));
			referent
					.add(new Pair<CxnalSpan, ECGSlotChain>(null, new ECGSlotChain(evokedProcessName + "." + coreRoleName)));
			candidate.addUnificationConstraint(referent);
		}

		return candidate;
	}

	protected void removeLinguisticSchemas(Map<Substitution<TypeConstraint>, Set<TypeConstraint>> coveringTypes) {
		try {
			Set<TypeConstraint> linguisticSchemas = new HashSet<TypeConstraint>();
			Set<String> speechActTypes = grammar.getSchemaTypeSystem().getAllSubtypes(ChildesLocalizer.SPEECH_ACTTYPE);
			for (String speechActType : speechActTypes) {
				linguisticSchemas.add(getSchemaTypeConstraint(speechActType));
			}
			linguisticSchemas.add(getSchemaTypeConstraint(ChildesLocalizer.eventDescriptorTypeName));
			linguisticSchemas.add(getSchemaTypeConstraint(ECGConstants.DISCOURSESEGMENTTYPE));

			Iterator<Substitution<TypeConstraint>> i = coveringTypes.keySet().iterator();
			while (i.hasNext()) {
				Substitution<TypeConstraint> substitution = i.next();
				Set<TypeConstraint> types = coveringTypes.get(substitution);
				Iterator<TypeConstraint> j = types.iterator();
				while (j.hasNext()) {
					TypeConstraint type = j.next();
					if (linguisticSchemas.contains(type))
						j.remove();
				}
				if (types.isEmpty()) {
					i.remove();
				}
			}

		}
		catch (TypeSystemException tse) {
			logger.warning("TypeSystemException encountered while trying to remove linguistic schemas from covering types");
		}
	}

	protected Pair<Double, Map<CxnalSpan, List<SlotChain>>> tryMeaning(Substitution<TypeConstraint> substitution,
			SchemaReachabilityTable reachabilityTable, TypeConstraint potentialMPole,
			MapSet<TypeConstraint, CxnalSpan> constituents) {

		// TODO: this doesn't work because there are 3 different cxnal type systems floating around.
		// semantic model is an identity hash map. It's based on the initial parsing grammar's type system
		// the analysis is in the experimental type system before any learning happened
		// the current grammar / tables are in the current experimental type sytem

		semanticModel = learnerGrammar.getSemanticModel(); // this triggers an on-demand building of the semantic model
																			// according to the current grammar

		List<Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint>> bindingsToScore = new ArrayList<Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint>>();

		Map<CxnalSpan, List<SlotChain>> unificationConstraints = new HashMap<CxnalSpan, List<SlotChain>>();

		for (TypeConstraint coveredType : constituents.keySet()) {

			TypeConstraint localized = coveredType.getTypeSystem().getName().equals(ECGConstants.SCHEMA) ? getSchemaTypeConstraint(coveredType
					.getType()) : grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(coveredType.getType());

			for (CxnalSpan cxnalSpan : constituents.get(coveredType)) {

				TypeConstraint roleType = substitution.get(coveredType).getFirst();
				List<List<SlotChain>> roles = reachabilityTable.getPathToRoleWithType(potentialMPole, roleType);
				roles.removeAll(unificationConstraints.values()); // remove all assigned / used-up roles

				if (roles == null || roles.isEmpty()) {
					throw new LearnerException(
							"Something is very wrong: cannot find a role in the potential meaning pole schema "
									+ potentialMPole.getType() + " that will accept something of type " + coveredType.getType());
				}

				if (roles.size() > 1 && semanticModel != null) {
					double bestSingleBindingScore = Double.NEGATIVE_INFINITY;
					List<SlotChain> bestPath = null;
					// multiple roles are suitable for this constituent. Find the best one? Can this assignment be done in a
					// greedy fashion?
					for (List<SlotChain> paths : roles) {
						double score = semanticModel.scoreSingleBinding(makeFrameRoleList(paths), localized);
						if (score > bestSingleBindingScore) {
							bestSingleBindingScore = score;
							bestPath = paths;
						}
					}
					unificationConstraints.put(cxnalSpan, bestPath);
					bindingsToScore.add(new Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint>(
							makeFrameRoleList(bestPath), localized));
				}
				else {
					unificationConstraints.put(cxnalSpan, roles.get(0));
					bindingsToScore.add(new Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint>(makeFrameRoleList(roles
							.get(0)), localized));
				}
			}
		}
		double score = semanticModel == null ? 0.0 : semanticModel.scoreAllBindings(bindingsToScore);
		return new Pair<Double, Map<CxnalSpan, List<SlotChain>>>(score, unificationConstraints);
	}

	protected Set<Pair<TypeConstraint, Role>> makeFrameRoleList(List<SlotChain> paths) {
		Set<Pair<TypeConstraint, Role>> frameRoles = new HashSet<Pair<TypeConstraint, Role>>();
		for (SlotChain path : paths) {
			List<Role> chain = path.getChain();
			Schema s = grammar.getSchema(chain.get(chain.size() - 2).getTypeConstraint().getType());
			frameRoles.add(new Pair<TypeConstraint, Role>(getSchemaTypeConstraint(s.getName()), s.getRole(chain.get(
					chain.size() - 1).getName())));
		}
		return frameRoles;
	}

	protected List<CompositionCandidate> findConnected(CompositionCandidate start,
			LookupTable<CompositionCandidate, CompositionCandidate> graph) {
		return findConnected(start, graph, Integer.MAX_VALUE);
	}

	protected List<CompositionCandidate> findConnected(CompositionCandidate start,
			LookupTable<CompositionCandidate, CompositionCandidate> graph, int max) {
		Stack<CompositionCandidate> toProcess = new Stack<CompositionCandidate>();
		List<CompositionCandidate> visited = new ArrayList<CompositionCandidate>();

		toProcess.push(start);

		while (!toProcess.isEmpty() && visited.size() < max) {
			CompositionCandidate next = toProcess.pop();
			visited.add(next);
			for (CompositionCandidate child : graph.get(next).keySet()) {
				if (!visited.contains(child)) {
					toProcess.push(child);
				}
			}
		}
		return visited;
	}

	public List<CompositionCandidate> generateSingleUnificationCandidatesOp1(ContextualFit fit, boolean includeDiscourse) {
		List<CompositionCandidate> candidates = new ArrayList<CompositionCandidate>();

		MapSet<String, Integer> sharedFillers = new MapSet<String, Integer>();
		Set<Integer> fittedSlots = fit.getSlots();
		for (Integer slotID : fittedSlots) {
			if (fit.getCandidate(slotID) != null
					&& (!useGoldStandard || fit.getVerificationResults(slotID) != Correctness.INCORRECT)) {
				sharedFillers.put(fit.getCandidate(slotID), slotID);
			}
		}

		MapSet<Integer, Pair<Integer, SlotChain>> slotChainTable = lcaTables.getSlotChainTable();

		for (String filler : sharedFillers.keySet()) {
			if (sharedFillers.get(filler).size() > 1) {

				// more than one construction share some filler in the meaning pole
				Set<Integer> usefulSlots = new HashSet<Integer>();
				for (Integer slotID : sharedFillers.get(filler)) {
					if (includeDiscourse || !LearnerUtilities.isOnlyDiscourse(slotChainTable.get(slotID))) {
						usefulSlots.add(slotID);
					}
				}

				if (usefulSlots.size() > 1) {

					// don't propose something with a big gap in the middle -- the analyzer can't use it for parsing
					TreeMap<CxnalSpan, ECGSlotChain> treeMap = LearnerUtilities.pullOutCxnalSpans(lca, grammar, usefulSlots,
							slotChainTable, includeDiscourse);
					int constructionNum = 0;
					MapSet<Integer, CxnalSpan> disjointConstructions = new MapSet<Integer, CxnalSpan>();

					Iterator<CxnalSpan> iter = treeMap.keySet().iterator();
					CxnalSpan first = iter.next();
					int lastSpanRight = first.getRight();
					disjointConstructions.put(constructionNum, first);
					while (iter.hasNext()) {
						CxnalSpan current = iter.next();
						if (current.omitted()) {
							iter.remove();
						}
						else if (current.getType().getType().equals(ChildesLocalizer.LEFTOVER_MORPHEME)) {
							throw new LearnerException("LEFTOVER_MORPHEME should not have been selected as a constituent");
						}
						else {
							if (current.getLeft() <= (lastSpanRight + 1)) {
								disjointConstructions.put(constructionNum, current);
							}
							else {
								constructionNum++;
								disjointConstructions.put(constructionNum, current);
							}
							lastSpanRight = current.getRight();
						}
					}

					for (Set<CxnalSpan> disjoint : disjointConstructions.values()) {
						if (disjoint.size() > 1) {
							CompositionCandidate candidate = new CompositionCandidate(grammar);
							List<Pair<CxnalSpan, ECGSlotChain>> constraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();

							for (CxnalSpan span : disjoint) {
								candidate.add(span);
								constraint.add(new Pair<CxnalSpan, ECGSlotChain>(span, treeMap.get(span)));
							}
							if (candidate.size() > 1) {
								boolean success = cleanupConstraint(constraint);
								if (success) {
									candidate.addUnificationConstraint(constraint);
									candidates.add(candidate);
								}
							}
						}
					}
				}
			}
		}

		return candidates;
	}

	// This is a hacky little method that attempts to resolve type system incompatibility conflicts.
	// If it can't be easily resolved, return false (i.e. ditch this proposed cxn)
	// There might still be meaning conflicts after this clean up. For example, multiple verbs (
	// with different meaning schemas) could be unified to the same meaning due to the context fitter
	// mistakenly fitting them to the same event in the lenient / expanded fit (e.g. pao3 qu4).
	// That's why there's a grammar compatibility check before any construction can be added to the grammar

	private boolean cleanupConstraint(List<Pair<CxnalSpan, ECGSlotChain>> constraint) {

		List<Pair<CxnalSpan, ECGSlotChain>> processRoles = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		List<Pair<CxnalSpan, ECGSlotChain>> ontologyProcessRoles = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();

		List<Pair<CxnalSpan, ECGSlotChain>> schemaRoles = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
		List<Pair<CxnalSpan, ECGSlotChain>> ontologyRoles = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();

		for (Pair<CxnalSpan, ECGSlotChain> unified : constraint) {
			List<Role> slotChain = unified.getSecond().getChain();
			TypeConstraint tc = slotChain.get(slotChain.size() - 1).getTypeConstraint();
			if (tc == null && slotChain.get(slotChain.size() - 1).getName().equals(ECGConstants.MEANING_POLE)
					&& slotChain.size() == 2) {
				tc = unified.getFirst().getType().getMeaningBlock().getTypeConstraint();
			}
			if (tc != null) {
				try {
					TypeSystem<?> ts = tc.getTypeSystem();
					if (ts.getName().equals(ECGConstants.SCHEMA)) {
						if (ts.subtype(ts.getInternedString(tc.getType()), ts.getInternedString(ChildesLocalizer.PROCESSTYPE))) {
							processRoles.add(unified);
						}
						schemaRoles.add(unified);
					}
					else if (ts.getName().equals(ECGConstants.ONTOLOGY)) {
						if (ts.subtype(ts.getInternedString(ChildesLocalizer.PROCESSTYPE), ts.getInternedString(tc.getType()))) {
							ontologyProcessRoles.add(unified);
						}
						ontologyRoles.add(unified);
					}
				}
				catch (TypeSystemException tse) {
					throw new LearnerException("Cannot lookup type " + tc.getType() + " in type system", tse);
				}
			}
		}

		// This is to check if cross-type-system constraints have been proposed (between type Process and type @Process)
		// This can happen if say, a percept is an event
		if (!processRoles.isEmpty() && !ontologyProcessRoles.isEmpty()) {
			// do some surgery to the processRoles
			for (Pair<CxnalSpan, ECGSlotChain> processChain : processRoles) {
				List<Role> slotChain = processChain.getSecond().getChain();
				Set<Role> evokedRoles = processChain.getFirst().getType().getMeaningBlock().getEvokedElements();
				Role ed = null;
				for (Role r : evokedRoles) {
					if (r.getTypeConstraint().getType().equals(ChildesLocalizer.eventDescriptorTypeName)
							&& r.getName().equals(ChildesLocalizer.eventDescriptorRoleName)) {
						ed = r;
					}
				}
				if (ed != null) {
					slotChain.add(ed);
					slotChain.add(referentRole);
					processChain.getSecond().setChain(slotChain);
				}
				else {
					return false;
				}
			}
		}
		else if (!schemaRoles.isEmpty() && !ontologyRoles.isEmpty()) {
			// if these aren't process roles (which can be resolved with a hack), then we have a problem
			// roles from different type systems are unified. Just get rid of the candidate
			return false;
		}
		return true;
	}

	protected TypeConstraint getSchemaTypeConstraint(String typeName) {
		return grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(typeName);
	}

}

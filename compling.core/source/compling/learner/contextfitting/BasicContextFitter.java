// =============================================================================
//File        : ContextFitter.java
//Author      : emok
//Change Log  : Created on Mar 18, 2007
//=============================================================================

package compling.learner.contextfitting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesLocalizer.DSRole;
import compling.context.ContextModel;
import compling.context.ContextUtilities;
import compling.context.MiniOntology;
import compling.context.MiniOntology.Type;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.contextfitting.ContextualFitScorer.ContextualFitScorerFactory;
import compling.learner.contextfitting.ContextualFitScorer.NestedScorerFactory;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.featurestructure.ResolutionResults.LCAResolution;
import compling.learner.util.LearnerUtilities;
import compling.util.MapFactory.HashMapFactory;
import compling.util.MapSet;
import compling.util.Pair;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public class BasicContextFitter implements ContextFitter {

	static final int MIN_NUM_RESULTS = 1;
	static final int AGGRESSIVE_PARAM = 3;
	boolean aggressiveFit = true;

	Grammar currentGrammar = null;
	ContextModel contextModel = null;
	ContextualFitScorerFactory scorerFactory = null;
	ContextualFitScorer scorer = null;
	ContextFittingLocalizer localizer;

	// OPTIMIZE: intern the current speech act type
	LearnerCentricAnalysis currentLCA = null;
	String currentDS = null;
	String currentSpeechAct = null;
	String currentSpeechActType = null;
	String currentSpeaker = null;
	String currentAddressee = null;
	Collection<String> jointAttention = null;
	List<String> expandedAttention = null;

	// TODO: use this in the scoring
	List<String> rdInvokedContextElements = null;

	TypeSystem<Schema> schemaTypeSystem = null;
	TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = null;
	TypeSystem<Construction> cxnalTypeSystem = null;

	TypeConstraint rootTC = null;
	TypeConstraint leftoverMorphemeTC = null;

	MapSet<FittableType, Integer> fittableByType = new MapSet<FittableType, Integer>(
			new HashMapFactory<FittableType, Set<Integer>>(), new LinkedHashSetFactory<Integer>());
	Map<Integer, FittableType> fittable = new LinkedHashMap<Integer, FittableType>();

	ContextualFit bestFit = null;
	LCATables tables = null;

	static Logger logger = Logger.getLogger(BasicContextFitter.class.getName());

	int slotCounter = -1;

	public BasicContextFitter(Grammar currentGrammar, boolean aggressiveFit, ContextFittingLocalizer localizer) {
		this(currentGrammar, new NestedScorerFactory(), aggressiveFit, localizer);
	}

	protected BasicContextFitter(Grammar currentGrammar, ContextualFitScorerFactory scorerFactory,
			boolean aggressiveFit, ContextFittingLocalizer localizer) {
		this.currentGrammar = currentGrammar;
		this.schemaTypeSystem = currentGrammar.getSchemaTypeSystem();
		this.ontologyTypeSystem = currentGrammar.getOntologyTypeSystem();
		this.cxnalTypeSystem = currentGrammar.getCxnTypeSystem();
		this.scorerFactory = scorerFactory;
		this.localizer = localizer;
		this.aggressiveFit = aggressiveFit;

		contextModel = currentGrammar.getContextModel();
		if (contextModel == null) {
			throw new LearnerException("Context Fitting cannot proceed without a valid context model");
		}

		rootTC = cxnalTypeSystem.getCanonicalTypeConstraint(ECGConstants.ROOT);
		leftoverMorphemeTC = cxnalTypeSystem.getCanonicalTypeConstraint(ChildesLocalizer.LEFTOVER_MORPHEME);
	}

	public ContextualFit getContextualFit(LearnerCentricAnalysis lca) {
		this.currentLCA = lca;

		currentDS = lca.getCurrentDS();
		currentSpeechAct = lca.getCurrentSpeechAct();
		currentSpeechActType = lca.getCurrentSpeechActType();
		currentSpeaker = lca.getCurrentSpeaker();
		currentAddressee = lca.getCurrentAddressee();
		jointAttention = lca.getJointAttention();

		bestFit = new ContextualFit(currentLCA, "bestFit", ontologyTypeSystem);
		tables = currentLCA.getTables();
		slotCounter = currentLCA.getLargestAssignedSlotID();
		slotCounter++;

		fittableByType.clear();
		fittableByType.initialize(Arrays.asList(FittableType.values()));
		fittable.clear();

		expandedAttention = new ArrayList<String>(jointAttention);
		expandedAttention.add(currentSpeaker);
		expandedAttention.add(currentAddressee);
		expandedAttention.add(currentDS);
		expandedAttention.add(currentSpeechAct);

		rdInvokedContextElements = currentLCA.getResolutionResults().getUniqueCandidates();

		scorer = scorerFactory.makeScorer(contextModel, this);
		fitToContext(aggressiveFit
				&& ((double) currentLCA.getUtteranceAnalyzed().size() / currentLCA.getNumSeparateAnalyses()) < AGGRESSIVE_PARAM);
		currentLCA.setContextualFit(bestFit);
		return bestFit;
	}

	protected void fitToContext(boolean aggressive) {
		if (aggressive) {
			Schema speechActSchema = currentSpeechActType == null ? null : schemaTypeSystem.get(currentSpeechActType);
			if (speechActSchema == null)
				throw new LearnerException("The speech act type " + currentSpeechActType
						+ " obtained from the current DS in context is not recognized");
			agressiveMerge(speechActSchema); // NOTE: this forces every fss to have a DS slot
			tables.updateBasicTables();
		}

		findFittable();

		for (FittableType type : FittableType.values()) {
			for (Integer slotID : fittableByType.get(type)) {
				Pair<Set<String>, Boolean> ret = findContextElements(type, slotID);
				Set<String> contextElements = ret.getFirst();
				boolean partialFit = ret.getSecond();
				if (!bestFit.isFitted(slotID)) {
					incorporateBest(bestFit, fitSlot(bestFit, slotID, contextElements, true, type.obeyRD, partialFit));
				}
				if (scorer != null) {
					scorer.score(bestFit, null);
				}
			}
		}
	}

	protected void incorporateBest(ContextualFit fit, Set<ContextualFit> fits) {
		if (fits == null || fits.isEmpty())
			return;

		List<ContextualFit> rankedFits = new ArrayList<ContextualFit>();

		for (ContextualFit toBeIncorporated : fits) {
			rankedFits.add(scorer.score(toBeIncorporated, fit));
		}
		Collections.sort(rankedFits, new ContextualFitComparator());
		Collections.reverse(rankedFits);
		ContextualFit bestCandidate = rankedFits.get(0);

		if (bestCandidate != null) {
			fit.incorporate(bestCandidate);
		}
	}

	protected Pair<Set<String>, Boolean> findContextElements(FittableType type, Integer slotID) {
		Set<String> contextElements = null;
		boolean partialFit = false;
		Slot slot = currentLCA.getSlot(slotID);
		TypeConstraint typeConstraint = slot.getTypeConstraint();

		if (typeConstraint == null) {
			return new Pair<Set<String>, Boolean>(new HashSet<String>(), false);
		}

		if (type == FittableType.DS) {
			contextElements = new HashSet<String>();
			contextElements.add(currentDS);
		}
		else if (type == FittableType.DISCOURSE_PARTTCIPANT) {
			contextElements = queryContext(typeConstraint, false);
		}
		else if (type == FittableType.STRUCTURED_ELEMENT || type == FittableType.UNSTRUCTURED_ELEMENT) {
			if (typeConstraint.getType().equals(ChildesLocalizer.ENTITYTYPE)
					|| typeConstraint.getType().equals(MiniOntology.INDIVIDUALNAME)) {
				// the type restriction is too loose. Let's try the attentional focus heuristic
				contextElements = new HashSet<String>(jointAttention);
			}
			else {
				contextElements = queryContext(typeConstraint, false);
			}
		}
		else if (type == FittableType.COMPLEX_PROCESS || type == FittableType.STRUCTURED_SIMPLE_PROCESS
				|| type == FittableType.UNSTRUCTURED_SIMPLE_PROCESS) {
			if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_CALLING)
					|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ACTION)
					|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ANSWER)
					|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_ADMONISHING)) {
				contextElements = findPartialContextElements(typeConstraint);
				partialFit = true;
			}
			else {
				contextElements = queryContext(typeConstraint, true);
			}
		}
		else {
			throw new LearnerException("Why are we looking for context elements for the slot " + slot.toString()
					+ " whose FittableType is " + type.toString());
		}
		return new Pair<Set<String>, Boolean>(contextElements, partialFit);
	}

	protected Pair<Set<String>, Boolean> findContextElements(Slot parentSlot, String contextElement, Role role,
			Integer roleSlotID) {
		Set<String> contextElements = null;
		boolean partialFit = false;
		FittableType type = fittable.get(roleSlotID);

		if (type != null && type == FittableType.COMPLEX_PROCESS || type == FittableType.STRUCTURED_SIMPLE_PROCESS
				|| type == FittableType.UNSTRUCTURED_SIMPLE_PROCESS) {
			contextElements = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(role.getName(),
					ContextModel.getIndividualName(contextElement), "?f"), true));
			if (contextElements.isEmpty()
					&& (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_CALLING)
							|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ACTION)
							|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ANSWER) || currentSpeechActType
								.equals(ChildesLocalizer.SPEECH_ACT_ADMONISHING))) {
				contextElements = findPartialContextElements(role.getTypeConstraint());
				partialFit = true;
			}
		}
		else {
			contextElements = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(role.getName(),
					ContextModel.getIndividualName(contextElement), "?f"), true));
		}
		return new Pair<Set<String>, Boolean>(contextElements, partialFit);
	}

	private Set<String> findPartialContextElements(TypeConstraint typeConstraint) {
		if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_CALLING)
				|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ACTION)
				|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ANSWER)) {
			// Since this is an action or answer request, the protagonist of the process must be the current addressee
			// On the other hand, a match is generally not expected, except when a similar action has been performed in the
			// past.
			List<SimpleQuery> queries = new ArrayList<SimpleQuery>();
			queries.add(new SimpleQuery("?x", typeConstraint.getType()));
			queries.add(new SimpleQuery(ChildesLocalizer.PROTAGONIST, "?x", ContextModel
					.getIndividualName(currentAddressee)));
			Set<String> contextElements = ContextUtilities.collapseResults(contextModel.query(queries, true), "?x");
			if (contextElements.isEmpty()) {
				// if this indeed comes back with no matches, take a last-ditch effort to find a recent similar action
				// (e.g. in the case of group activities or a parent demonstration), but the protagonist should be clamped
				// to the addressee
				contextElements = queryContext(typeConstraint, true);
			}
			return contextElements;
		}
		else if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_ADMONISHING)) {
			// In the case of an admonishing, the addressee is presumed to have done something bad.
			// A match is expected in this case, unless the speaker is threatening with her own repercussive actions.
			List<SimpleQuery> queries = new ArrayList<SimpleQuery>();
			queries.add(new SimpleQuery("?x", typeConstraint.getType()));
			queries.add(new SimpleQuery(ChildesLocalizer.PROTAGONIST, "?x", ContextModel
					.getIndividualName(currentAddressee)));
			Set<String> contextElements = ContextUtilities.collapseResults(contextModel.query(queries, true), "?x");
			return contextElements;
		}
		else {
			// it should never reach here
			return null;
		}
	}

	protected Set<ContextualFit> fitSlot(ContextualFit currentFit, Integer slotID, Set<String> contextElements,
			boolean isRoot, boolean obeyRD, boolean partialFit) {

		Slot slot = currentLCA.getSlot(slotID);
		Set<ContextualFit> allFits = new HashSet<ContextualFit>();
		Integer rdSlotID = tables.getRelatedRD(slotID);
		LCAResolution resolution = rdSlotID == null ? null : currentLCA.getResolution(rdSlotID);

		// begin partial-fitting-specific code
		Slot protagonistSlot = slot.getFeatures() == null ? null : slot.getSlot(new Role(ChildesLocalizer.PROTAGONIST));
		Integer protagonistSlotID = protagonistSlot == null ? null : protagonistSlot.getID();
		Integer protagonistRDSlotID = protagonistSlotID == null ? null : tables.getRelatedRD(protagonistSlotID);
		boolean hasOmittedProtagonist = protagonistRDSlotID != null
				&& currentLCA.getResolution(protagonistRDSlotID) != null
				&& currentLCA.getResolution(protagonistRDSlotID).isOmitted();

		String guessedProtagonist = null;
		if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_CALLING)
				|| currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ACTION)) {
			guessedProtagonist = currentAddressee;
		}
		else if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_REQUESTING_ANSWER)) {
			if (protagonistSlot != null && protagonistSlot.getTypeConstraint() != null
					&& LearnerUtilities.isAnimate(currentGrammar, protagonistSlot.getTypeConstraint())) {
				guessedProtagonist = currentAddressee;
			}
		}
		else if (currentSpeechActType.equals(ChildesLocalizer.SPEECH_ACT_ADMONISHING)) {
			guessedProtagonist = currentSpeaker;
		}

		// end partial-fitting-specific code

		if (contextElements.isEmpty()) {
			boolean isEventDescriptor = false;
			isEventDescriptor = LearnerUtilities.isEventDescriptor(currentGrammar, slot.getTypeConstraint());
			if (isEventDescriptor && slot.getFeatures() != null) {
				ContextualFit f = fitStructure(currentFit, slotID, null, isRoot, obeyRD, true, 0);
				allFits.add(f);
				return allFits;
			}
			else if (partialFit && slot.getFeatures() != null) {
				ContextualFit nullFit = new ContextualFit(currentLCA, "fit" + slot.getSlotIndex(), ontologyTypeSystem);
				nullFit.addCandidate(slotID, null, false);

				if (protagonistRDSlotID == null || hasOmittedProtagonist) {
					if (!hasBeenFitted(currentFit, nullFit, protagonistSlotID)) {
						nullFit.addCandidate(protagonistSlotID, guessedProtagonist, isRoot);
					}
					else if (currentFit.getCandidate(protagonistSlotID) != null
							&& !currentFit.getCandidate(protagonistSlotID).equals(guessedProtagonist)) {
						currentFit.setCatastrophicallyImcompatable();
					}
					else if (nullFit.getCandidate(protagonistSlotID) != null
							&& !nullFit.getCandidate(protagonistSlotID).equals(guessedProtagonist)) {
						nullFit.setCatastrophicallyImcompatable();
					}
				}

				// FUTURE: if simulation is used to test future actions, this is one place where it goes
				allFits.add(nullFit);
			}
			else {
				ContextualFit nullFit = new ContextualFit(currentLCA, "fit" + slot.getSlotIndex(), ontologyTypeSystem);
				nullFit.addCandidate(slotID, null, false);
				allFits.add(nullFit);
				return allFits;
			}
		}

		if (slot.getFeatures() == null && !hasBeenFitted(currentFit, null, slotID)) {
			int i = 0;
			for (String contextElement : contextElements) {
				if (!obeyRD || resolution == null
						|| (resolution != null && resolution.isCandidate(ContextModel.getIndividualName(contextElement)))) {
					ContextualFit f = new ContextualFit(currentLCA, "fit" + slot.getSlotIndex() + "-" + i,
							ontologyTypeSystem);
					i++;
					f.addCandidate(slotID, contextElement, isRoot);
					allFits.add(f);
					// FIXME: even if the slot doesn't have features, context may still supply additional role fillers.
					// one possible reason for there to not be features is the shortcut taken by the analyzer at start-up
					// time.
				}
			}
			return allFits;
		}

		if (partialFit && protagonistRDSlotID != null && currentLCA.getResolution(protagonistRDSlotID) != null
				&& currentLCA.getResolution(protagonistRDSlotID).isOmitted()
				&& currentFit.getCandidate(protagonistSlotID) != null) {
			currentFit.addCandidate(protagonistSlotID, guessedProtagonist, isRoot);
		}

		int i = 0;
		for (String contextElement : contextElements) {
			if (!obeyRD || resolution == null
					|| (resolution != null && resolution.isCandidate(ContextModel.getIndividualName(contextElement)))) {
				ContextualFit f = fitStructure(currentFit, slotID, contextElement, isRoot, obeyRD, !partialFit, i);
				// FUTURE: if simulation is used to test future actions, this is one place where it goes
				allFits.add(f);
				i++;
			}
		}
		return allFits;

	}

	private ContextualFit fitStructure(ContextualFit currentFit, Integer slotID, String contextElement, boolean isRoot,
			boolean obeyRD, boolean checkCompatibility, int counter) {

		// FIXME: why is checkCompatibility not used? this is for partial fitting, I thought.

		Slot slot = currentLCA.getSlot(slotID);
		ContextualFit f = new ContextualFit(currentLCA, "fit" + slot.getSlotIndex() + "-" + counter, ontologyTypeSystem);
		Schema matchedSchema = null;

		if (slot.getTypeConstraint().getTypeSystem() == schemaTypeSystem) {
			if (contextElement != null) {
				f.addCandidate(slotID, contextElement, isRoot);

				// note: the fitting has to go by the lowest common supertype between the type of the slot and the type of
				// the context element.
				Set<String> types = new HashSet<String>();
				types.add(slot.getTypeConstraint().getType());
				types.add(ContextModel.getIndividualType(contextElement));

				try {
					String commonSupertype = schemaTypeSystem.bestCommonSupertype(types, false);
					matchedSchema = currentGrammar.getSchema(commonSupertype);
				}
				catch (TypeSystemException tse) {
					logger.warning("encountered error while trying to look up the common supertype of " + types
							+ " in the type system.");
				}
			}
			else {
				matchedSchema = currentGrammar.getSchema(slot.getTypeConstraint().getType());
			}
		}

		if (matchedSchema != null) {
			for (Role role : prioritizeRoles(matchedSchema)) {
				fitRoleSlot(currentFit, f, slot, role, contextElement, obeyRD);
			}
		}

		return f;
	}

	protected List<Role> prioritizeRoles(Schema schema) {
		if (schema.getName().equals(ECGConstants.DISCOURSESEGMENTTYPE)) {
			List<Role> roles = new ArrayList<Role>();
			for (DSRole importantRole : DSRole.values()) {
				Role role = findRole(schema, localizer.getDSRoleLocalization(importantRole));
				if (role != null)
					roles.add(role);
			}
			Set<Role> remainingRoles = new HashSet<Role>(schema.getAllRoles());
			remainingRoles.removeAll(roles);
			roles.addAll(remainingRoles);
			return roles;
		}
		else {
			return new ArrayList<Role>(schema.getAllRoles());
		}
	}

	private Role findRole(Schema schema, String roleName) {
		for (Role role : schema.getAllRoles()) {
			if (role.getName().equals(localizer.getDSRoleLocalization(DSRole.Speaker))) {
				return role;
			}
		}
		return null;
	}

	private void fitRoleSlot(ContextualFit currentFit, ContextualFit f, Slot parentSlot, Role role,
			String contextElement, boolean obeyRD) {
		Slot roleSlot = parentSlot.getFeatures().get(role);
		if (roleSlot == null) {
			Set<Pair<Integer, SlotChain>> chainsToParent = tables.getSlotChainTable().get(parentSlot.getID());
			Pair<Integer, SlotChain> chainToParent = chainsToParent.iterator().next();
			List<Role> chainToSlot = new ArrayList<Role>(chainToParent.getSecond().getChain());
			chainToSlot.add(role);
			Pair<Integer, SlotChain> additionalSlot = new Pair<Integer, SlotChain>(chainToParent.getFirst(),
					new SlotChain().setChain(chainToSlot));

			if (contextElement != null) {
				Pair<Set<String>, Boolean> ret = findContextElements(parentSlot, contextElement, role, null);
				Set<String> contextElements = ret.getFirst();
				if (contextElements != null && !contextElements.isEmpty() && !hasBeenFitted(currentFit, f, additionalSlot)) {

					logger.finest("Found a context element for the non-existent slot " + chainToParent.getSecond() + "."
							+ role.getName() + ": " + ret.getFirst());
					Set<ContextualFit> fits = new HashSet<ContextualFit>();
					for (String candidate : contextElements) {
						ContextualFit roleFit = new ContextualFit(currentLCA, "addnFit", ontologyTypeSystem);
						roleFit.addCandidate(additionalSlot, candidate);
						fits.add(roleFit);
					}
					incorporateBest(f, fits);
				}
			}
			return;
		}

		Integer roleSlotID = roleSlot.getID();
		if (!fittable.containsKey(roleSlotID))
			return;

		if (!hasBeenFitted(currentFit, f, roleSlotID)) {
			if (contextElement != null) {
				Pair<Set<String>, Boolean> ret = findContextElements(parentSlot, contextElement, role, roleSlot.getID());
				incorporateBest(f, fitSlot(f, roleSlotID, ret.getFirst(), false, obeyRD, ret.getSecond()));
			}
			else {
				Pair<Set<String>, Boolean> ret = findContextElements(fittable.get(roleSlotID), roleSlotID);
				incorporateBest(f, fitSlot(f, roleSlotID, ret.getFirst(), false, obeyRD, ret.getSecond()));
			}
		}
		else {
			if (contextElement != null) {
				Pair<Set<String>, Boolean> ret = findContextElements(parentSlot, contextElement, role, roleSlot.getID());
				Set<String> fillers = ret.getFirst();
				if (!(fillers.contains(currentFit.getCandidate(roleSlot.getID())) || fillers.contains(f
						.getCandidate(roleSlot.getID())))) {
					// flag the fit as incompatible if the current candidate isn't amongst the query results
					currentFit.setCatastrophicallyImcompatable();
				}
			}
		}
	}

	private boolean hasBeenFitted(ContextualFit currentFit, ContextualFit f, Integer slotID) {
		return f == null ? bestFit.isFitted(slotID) || currentFit.isFitted(slotID) : bestFit.isFitted(slotID)
				|| currentFit.isFitted(slotID) || f.isFitted(slotID);
	}

	private boolean hasBeenFitted(ContextualFit currentFit, ContextualFit f, Pair<Integer, SlotChain> additionalSlot) {
		return f == null ? bestFit.isFitted(additionalSlot) || currentFit.isFitted(additionalSlot) : bestFit
				.isFitted(additionalSlot) || currentFit.isFitted(additionalSlot) || f.isFitted(additionalSlot);
	}

	protected void findFittable() {
		logger.finest("slots to ignore: ");
		for (Integer slotID : tables.getIgnoreSlots()) {
			Slot slot = currentLCA.getSlot(slotID);
			logger.finest(slot.toString());
		}

		for (Integer slotID : tables.getAllNonCxnSlots()) {
			Slot slot = currentLCA.getSlot(slotID);
			TypeConstraint typeConstraint = currentLCA.getTypeConstraint(slotID);
			if (!tables.getIgnoreSlots().contains(slotID)) {
				if (LearnerUtilities.isDS(currentGrammar, typeConstraint)) {
					fittableByType.put(FittableType.DS, slotID);
					fittable.put(slotID, FittableType.DS);
				}
				else if (LearnerUtilities.isDSRelatedRD(currentGrammar, slot, typeConstraint)) {
					Slot referentSlot = slot.getSlot(new Role(ECGConstants.RESOLVEDREFERENT));
					if (referentSlot != null && isUsefulElementToMatch(slot)) {
						fittableByType.put(FittableType.DISCOURSE_PARTTCIPANT, referentSlot.getID());
						fittable.put(slotID, FittableType.DISCOURSE_PARTTCIPANT);
					}
				}
				else if (LearnerUtilities.isProcess(currentGrammar, typeConstraint) && isUsefulProcessToMatch(slot)) {
					if (LearnerUtilities.isComplexProcess(currentGrammar, typeConstraint)) {
						fittableByType.put(FittableType.COMPLEX_PROCESS, slotID);
						fittable.put(slotID, FittableType.COMPLEX_PROCESS);
					}
					else if (slot.hasStructuredFiller()) {
						fittableByType.put(FittableType.STRUCTURED_SIMPLE_PROCESS, slotID);
						fittable.put(slotID, FittableType.STRUCTURED_SIMPLE_PROCESS);
					}
					else {
						fittableByType.put(FittableType.UNSTRUCTURED_SIMPLE_PROCESS, slotID);
						fittable.put(slotID, FittableType.UNSTRUCTURED_SIMPLE_PROCESS);
					}
				}
				else if (LearnerUtilities.isNotProcessOrHighlevelSchemas(currentGrammar, typeConstraint)
						&& isUsefulElementToMatch(slot)) {
					if (slot.hasStructuredFiller()) {
						fittableByType.put(FittableType.STRUCTURED_ELEMENT, slotID);
						fittable.put(slotID, FittableType.STRUCTURED_ELEMENT);
					}
					else {
						fittableByType.put(FittableType.UNSTRUCTURED_ELEMENT, slotID);
						fittable.put(slotID, FittableType.UNSTRUCTURED_ELEMENT);
					}
				}
			}
		}
	}

	private boolean isUsefulProcessToMatch(Slot slot) {
		boolean useful = true;
		useful &= !tables.getIgnoreSlots().contains(slot.getID());

		List<Role> roles = slot.getParentSlots();
		if (roles.size() == 1) {
			useful &= !roles.get(0).getName().equals(ChildesLocalizer.contentRoleName);
		}
		else {
			// if there are more than one roles, at least one isn't the content role
			boolean pointedToByUsefulRoles = false;
			for (Role role : roles) {
				pointedToByUsefulRoles |= !role.getName().equals(ChildesLocalizer.contentRoleName);
			}
			useful &= pointedToByUsefulRoles;
		}
		if (useful) {
			useful &= isDirectMeaning(slot);
		}
		return useful;
	}

	private boolean isUsefulElementToMatch(Slot slot) {
		boolean useful = true;
		TypeConstraint slotType = slot.getTypeConstraint();
		useful &= !tables.getIgnoreSlots().contains(slot.getID());
		useful &= !LearnerUtilities.isSetSize(currentGrammar, slotType);
		List<Role> roles = slot.getParentSlots();
		if (roles.size() == 1) {
			useful &= !(roles.get(0).getName().equals(ChildesLocalizer.instrumentRoleName) && roles.get(0)
					.getTypeConstraint().equals(slotType));
		}
		else {
			boolean pointedToByUsefulRoles = false;
			for (Role role : roles) {
				// if there are more than one roles, at least one isn't an unfilled instrument
				pointedToByUsefulRoles |= !(role.getName().equals(ChildesLocalizer.instrumentRoleName) && role
						.getTypeConstraint().equals(slotType));
			}
			useful &= pointedToByUsefulRoles;
		}
		if (useful) {
			useful &= isDirectMeaning(slot);
		}
		return useful;
	}

	private boolean isDirectMeaning(Slot slot) {
		// if all the slot chains go through more than one constructional types, then it's no good.
		boolean directMeaning = false;
		for (Pair<Integer, SlotChain> sc : tables.getSlotChainTable().get(slot.getID())) {
			int numCxnalType = 0;
			for (Role role : sc.getSecond().getChain()) {
				TypeConstraint tc = role.getTypeConstraint();
				if (tc != null && tc.getTypeSystem() == cxnalTypeSystem && tc != rootTC && tc != leftoverMorphemeTC) {
					numCxnalType++;
				}
			}
			if (numCxnalType <= 1) { // want at least one slotchain that is a direct meaning pole
				directMeaning = true;
				break;
			}
		}
		return directMeaning;
	}

	protected Set<String> queryContext(TypeConstraint type, boolean expand) {
		Set<String> contextElements = new HashSet<String>();
		if (ontologyTypeSystem.get(type.getType()) == null) {
			// the corresponding type is not defined in context
			return contextElements;
		}
		contextElements = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery("?x", type.getType()),
				false));

		if (contextElements.size() < MIN_NUM_RESULTS && expand) {
			if (type.getTypeSystem() == schemaTypeSystem) {
				for (Schema supertype : schemaTypeSystem.getParents(schemaTypeSystem.get(type.getType()))) {
					contextElements.addAll(ContextUtilities.collapseResults(contextModel.query(new SimpleQuery("?x",
							supertype.getType()), false)));
				}
			}
			else if (type.getTypeSystem() == ontologyTypeSystem) {
				for (TypeSystemNode supertype : ontologyTypeSystem.getParents(ontologyTypeSystem.get(type.getType()))) {
					contextElements.addAll(ContextUtilities.collapseResults(contextModel.query(new SimpleQuery("?x",
							supertype.getType()), false)));
				}
			}
		}
		return contextElements;
	}

	protected void agressiveMerge(Schema speechActSchema) {
		SlotChain sc = new SlotChain(ECGConstants.DS);

		Iterator<FeatureStructureSet> i = currentLCA.getFeatureStructureSets().iterator();
		FeatureStructureSet fss1 = i.next();

		if (fss1.getSlot(sc).getID() == -1) {
			fss1.getSlot(sc).setID(slotCounter++);
			fss1.getSlot(sc).setTypeConstraint(
					schemaTypeSystem.getCanonicalTypeConstraint(ECGConstants.DISCOURSESEGMENTTYPE));
		}
		while (i.hasNext()) {
			FeatureStructureSet fss = i.next();
			if (fss.getSlot(sc).getID() == -1) {
				fss.getSlot(sc).setID(slotCounter++);
			}
			currentLCA.coindexAcrossFeatureStructureSets(fss1, sc, fss, sc);
		}

		SlotChain saSC = new SlotChain(ECGConstants.slotChainToSpeechAct);
		if (fss1.getSlot(saSC).getID() == -1) {
			fss1.getSlot(saSC).setID(slotCounter++);
		}

		fss1.getSlot(saSC).setTypeConstraint(schemaTypeSystem.getCanonicalTypeConstraint(currentSpeechActType));
		for (Role r : speechActSchema.getContents().getEvokedElements()) {
			fss1.getSlot(new ECGSlotChain(ECGConstants.slotChainToSpeechAct, r)).setID(slotCounter++);
		}

		for (Role r : speechActSchema.getContents().getElements()) {
			fss1.getSlot(new ECGSlotChain(ECGConstants.slotChainToSpeechAct, r)).setID(slotCounter++);
		}

		for (Constraint c : speechActSchema.getContents().getConstraints()) {
			if (!c.overridden()) {
				addConstraint(fss1, c, ECGConstants.slotChainToSpeechAct);
			}
		}

		// there should now only be one fss, but there might still be multiple DS
		// (because various evoked ones won't be unified automatically by parser)

		List<Integer> allDS = new ArrayList<Integer>();
		for (Integer slotID : tables.getAllNonCxnSlots()) {
			TypeConstraint typeConstraint = currentLCA.getTypeConstraint(slotID);
			if (LearnerUtilities.isDS(currentGrammar, typeConstraint)) {
				allDS.add(slotID);
			}
		}
		if (allDS.size() > 1) {
			Iterator<Integer> ds = allDS.listIterator();
			Slot refDS = currentLCA.getSlot(ds.next());
			while (ds.hasNext()) {
				boolean unified = fss1.coindex(refDS, currentLCA.getSlot(ds.next()));
				// it is possible that this unification may fail (e.g. nested verb clauses may have a different speech act
				// than the overall)
				if (!unified) {
					logger.info("some DS's within the semspec fail to unify under agressive merge");
				}
			}
		}

	}

	private boolean addConstraint(FeatureStructureSet featureStructure, Constraint constraint, String prefix) {
		if (constraint.getOperator().equals(ECGConstants.ASSIGN)) {
			if (constraint.getValue().charAt(0) == '\"') {
				return featureStructure.fill(new ECGSlotChain(prefix, constraint.getArguments().get(0)),
						constraint.getValue());
			}
			else if (constraint.getValue().charAt(0) != '\"') {// then this is a type
				TypeConstraint typeConstraint = null;
				if (constraint.getValue().charAt(0) == '@') {
					typeConstraint = ontologyTypeSystem.getCanonicalTypeConstraint(constraint.getValue().substring(1));
				}
				else {
					typeConstraint = ontologyTypeSystem.getCanonicalTypeConstraint(constraint.getValue());
				}
				if (typeConstraint == null) {
					throw new GrammarException("Type " + constraint.getValue() + " in constraint " + constraint
							+ " is undefined");
				}
				featureStructure.getSlot(new ECGSlotChain(prefix, constraint.getArguments().get(0))).setTypeConstraint(
						typeConstraint);
			}
			return true;
		}
		else if (constraint.getOperator().equals(ECGConstants.IDENTIFY)) {
			SlotChain sc0 = constraint.getArguments().get(0);
			SlotChain sc1 = constraint.getArguments().get(1);
			if (sc0.getChain().get(0) != ECGConstants.DSROLE) {
				sc0 = new ECGSlotChain(prefix, sc0);
			}
			if (sc1.getChain().get(0) != ECGConstants.DSROLE) {
				sc1 = new ECGSlotChain(prefix, sc1);
			}
			return featureStructure.coindex(sc0, sc1);
		}
		else {
			throw new LearnerException("Error while trying to add constraints posthoc to the analysis: The "
					+ constraint.getOperator() + " operator is not supported.");
		}
	}

	public List<String> getExpandedJointAttention() {
		return expandedAttention;
	}

	public List<String> getRdInvokedContextElements() {
		return rdInvokedContextElements;
	}
}

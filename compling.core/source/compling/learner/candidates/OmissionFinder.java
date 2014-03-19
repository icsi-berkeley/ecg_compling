// =============================================================================
// File        : OmissionFinder.java
// Author      : emok
// Change Log  : Created on May 12, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerGrammar;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.learnertables.NGram;
import compling.learner.util.GrammarModUtilities;
import compling.learner.util.LearnerUtilities;
import compling.learner.util.LearnerUtilities.EqualsMappingFunction;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;

//=============================================================================

public class OmissionFinder {
	LearnerCentricAnalysis lca;
	LCATables lcaTables;

	LearnerGrammar learnerGrammar;
	Grammar grammar;
	GrammarTables grammarTables;
	NGram ngram;
	TypeSystem<Construction> cxnTypeSystem;

	ChildesClause utterance;

	static Logger logger = Logger.getLogger(OmissionFinder.class.getName());
	Map<Construction, Construction> substitutionToGrammar = new LinkedHashMap<Construction, Construction>();

	public OmissionFinder(LearnerGrammar learnerGrammar, LearnerCentricAnalysis lca) {
		setGrammar(learnerGrammar);
		this.lca = lca;
		utterance = lca.getUtteranceAnalyzed(); // do I need this?
		lcaTables = lca.getTables();
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		grammarTables = learnerGrammar.getGrammarTables();
		ngram = learnerGrammar.getNGram();
		cxnTypeSystem = grammar.getCxnTypeSystem();
	}

	// if there really is an omissible constituent, all that really needs to happen is
	// changing the unexpressed probabilyt in the locality table -- based on the unigram of the two items.

	// the way this is right now, it can also do optional constituents. But optional constituents have a keyword for
	// them.

	public boolean findOmissible(String shorterCxnUsed, Set<String> differsByOne) {
		boolean found = false;
		for (String contrast : differsByOne) {
			Construction shorter = grammar.getConstruction(shorterCxnUsed);
			Construction longer = grammar.getConstruction(contrast);
			Set<Role> sConstituents = shorter.getConstructionalBlock().getElements();
			Set<Role> lConstituents = longer.getConstructionalBlock().getElements();

			List<Map<Role, Role>> constituentMap = LearnerUtilities.mapConstituents(sConstituents, lConstituents,
					new EqualsMappingFunction());
			for (Map<Role, Role> map : constituentMap) { // I highly doubt that there should be more than one "equals"
																		// mapping

				// are the differences found in the LCA? If not, then those constituents can be omissified or optionalized
				// (depending on coreness).
				List<Role> difference = new ArrayList<Role>(lConstituents);
				difference.removeAll(map.values());
				List<Role> presentInInput = foundInLCA(difference);
				// honestly the difference should only be one constituent given how the NearlyIdentical list is created

				// at least some of the differences are not found in the input.
				// But also make sure that the longer one has actually been used (i.e. is a valid proposal)
				difference.removeAll(presentInInput);
				if (!difference.isEmpty() && learnerGrammar.getNGram().getUnigram(longer.getName()) > 0) {
					createNewConstructions(difference, shorter, longer);
					found = true;
				}
			}

		}
		return found;
	}

	public void createNewConstructions(List<Role> missingConstituents, Construction shorter, Construction longer) {

		logger.info("Changing the omission / optional status of constituents in " + longer.getName());
		// make a version of the longer one that allow omission / optionals
		Set<String> optionalRoles = new LinkedHashSet<String>();
		for (Role r : missingConstituents) {
			// is r bound to a core role of longer's meaning?
			boolean isCore = isTiedToCore(r, longer);

			// if core, omit; if not core, make optional
			if (isCore) {
				int omissionCount = learnerGrammar.getNGram().getUnigram(shorter.getName());
				int localCount = learnerGrammar.getNGram().getUnigram(longer.getName());
				learnerGrammar.getConstructionalSubtypeTable().addOmissionInformation(longer.getName(), r, omissionCount,
						localCount);
			}
			else {
				optionalRoles.add(r.getName());
			}
		}

		if (!optionalRoles.isEmpty()) {
			// I'm using modifyRoles here to essentially get a clone of the construction.
			Construction newLonger = GrammarModUtilities.modifyRoles(grammar, longer, longer.getName(),
					new HashMap<Role, Role>(), null, false);
			Set<Role> newOptionals = new LinkedHashSet<Role>();
			for (String optional : optionalRoles) {
				newOptionals.add(newLonger.getConstructionalBlock().getRole(optional));
			}
			newLonger.setOptionals(newOptionals);
			substitutionToGrammar.put(longer, newLonger);
		}
	}

	public GrammarChanges getChanges() {
		return new GrammarChanges(null, substitutionToGrammar, null);
	}

	protected boolean isTiedToCore(Role constituent, Construction cxn) {

		boolean isEventDescriptor = false;
		TypeConstraint schemaType = cxn.getMeaningBlock().getTypeConstraint();
		if (schemaType != null && schemaType.getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
			isEventDescriptor = true;
			schemaType = LearnerUtilities.findEventTypeRestriction(cxn, learnerGrammar.getGrammarTables());
		}

		if (!LearnerUtilities.isProcess(grammar, schemaType)) {
			// NOTE: core roles are assumed to be only defined for process schemas. if it isn't even a schema meaning,
			// assume non-core.
			return false;
		}

		Analysis semSpec = grammarTables.getConstructionCloneTable().getInstance(cxn);
		FeatureStructureSet fss = semSpec.getFeatureStructure();
		ECGSlotChain chain = new ECGSlotChain(constituent.getName() + "." + ECGConstants.MEANING_POLE);
		Slot mSlot = fss.hasSlot(fss.getMainRoot(), chain) ? fss.getSlot(fss.getMainRoot(), chain) : null;
		if (mSlot == null)
			return false;

		Set<String> coreRoleNames = grammarTables.getCoreRolesTable().get(schemaType.getType());
		for (String coreRoleName : coreRoleNames) {
			ECGSlotChain coreChain = isEventDescriptor ? new ECGSlotChain(ECGConstants.SELF + "."
					+ ECGConstants.MEANING_POLE + "." + ChildesLocalizer.eventTypeRoleName + "." + coreRoleName)
					: new ECGSlotChain(ECGConstants.SELF + "." + ECGConstants.MEANING_POLE + "." + coreRoleName);
			Slot coreSlot = fss.hasSlot(fss.getMainRoot(), coreChain) ? fss.getSlot(fss.getMainRoot(), coreChain) : null;
			if (mSlot == coreSlot)
				return true;
		}
		return false;
	}

	// NOTE: is it better to use the analysis or to use the textual string?
	// word senses may be wrong in the analysis. However, the missing constituent can be a phrasal one.
	protected List<Role> foundInLCA(List<Role> differingConstituents) {

		Map<String, Role> missingConstituentType = new LinkedHashMap<String, Role>();
		for (Role r : differingConstituents) {
			missingConstituentType.put(r.getTypeConstraint().getType(), r);
		}

		List<Role> found = new ArrayList<Role>();
		for (CxnalSpan span : lca.getCxnalSpans().values()) {
			if (!span.omitted()) {
				String cxnType = span.getType().getName();
				if (missingConstituentType.containsKey(cxnType)) {
					// at least one is found
					found.add(missingConstituentType.get(cxnType));
				}
			}
		}

		return found;
	}
}

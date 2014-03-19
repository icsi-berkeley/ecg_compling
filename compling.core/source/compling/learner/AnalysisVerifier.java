// =============================================================================
//File        : AnalysisScorer.java
//Author      : emok
//Change Log  : Created on Nov 30, 2007
//=============================================================================

package compling.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesConstants;
import compling.annotation.childes.ChildesConstants.GSPrimitive;
import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesTranscript;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.FeatureBasedEntity;
import compling.annotation.childes.FeatureBasedEntity.Binding;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.context.ContextModel;
import compling.context.ContextUtilities;
import compling.context.MiniOntology.Type;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.featurestructure.ResolutionResults.LCAResolution;
import compling.learner.grammartables.GrammarTables;
import compling.learner.grammartables.SchemaCloneTable;
import compling.learner.util.AnnotationUtilities;
import compling.learner.util.AnnotationUtilities.GoldStandardAnnotationLocalizer;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.LookupTable;
import compling.util.MapMap;
import compling.util.Pair;
import compling.util.Triplet;

//=============================================================================

public class AnalysisVerifier {

	private Grammar currentGrammar = null;
	private GrammarTables currentGrammarTables = null;
	private SchemaCloneTable schemaCloneTable = null;
	private ContextModel contextModel = null;
	private ChildesTranscript currentTranscript = null;
	private LearnerCentricAnalysis currentLCA = null;
	private GoldStandardAnnotation currentGoldStandardAnnotation = null;
	private TypeSystem<Construction> cxnTypeSystem = null;
	private TypeSystem<Schema> schemaTypeSystem = null;
	private TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = null;
	private LCATables tables = null;
	private Map<Integer, CxnalSpan> spans = null;
	private ChildesClause currentClause = null;

	private GoldStandardAnnotationLocalizer localizer = new ChildesLocalizer();

	private static Logger logger = Logger.getLogger(AnalysisVerifier.class.getName());

	private static final Role ROLE_MPOLE = new Role(ECGConstants.MEANING_POLE);
	private static final Role ROLE_EVENTTYPE = new Role(ChildesLocalizer.eventTypeRoleName);
	private static final Role ROLE_MODALITY = new Role(ChildesLocalizer.modalityRoleName);
	private static final Role ROLE_MODIFIERS = new Role(ChildesLocalizer.modifiersRoleName);
	private static final Role ROLE_MODIFIER_CATEGORY = new Role(ChildesLocalizer.modifierCategoryRoleName);
	private static final Role ROLE_MODALITY_CATEGORY = new Role(ChildesLocalizer.modalityCategoryRoleName);
	private static final Role ROLE_EVENT_DESCRIPTOR = new Role(ChildesLocalizer.eventDescriptorRoleName);
	private static final Role ROLE_MODIFIER_ASPECT = new Role(ChildesLocalizer.modifierAspectRoleName);
	private static final Role ROLE_EVENT_STRUCTURE = new Role(ChildesLocalizer.eventStructureRoleName);

	public static enum Cat {
		CORE("CoreArg"), ADJ("Adjunct"), RES("Resol'n"), CF("Fitting"), BRACKET("Bracket");
		public String description;

		Cat(String des) {
			description = des;
		}

		public String toString() {
			return description;
		}
	};

	public static enum Correctness {
		CORRECT, INCORRECT, NOMATCH
	};

	public static class Scorecard {
		protected LookupTable<Cat, Correctness> scores = new LookupTable<Cat, Correctness>();
		protected MapMap<Cat, Slot, List<Pair<Correctness, String>>> log = new MapMap<Cat, Slot, List<Pair<Correctness, String>>>();
		protected Map<Cat, StringBuffer> addnLog = new HashMap<Cat, StringBuffer>();

		public Scorecard() {
			for (Cat cat : Cat.values()) {
				scores.put(cat, new HashMap<Correctness, Integer>());
				for (Correctness c : Correctness.values()) {
					scores.put(cat, c, 0);
				}
				log.put(cat, new HashMap<Slot, List<Pair<Correctness, String>>>());
				addnLog.put(cat, new StringBuffer());
			}
		}

		public Scorecard sum(Scorecard that) {
			return sum(that, false);
		}

		public Scorecard sum(Scorecard that, boolean ignoreDuplicates) {
			if (that != null) {
				for (Cat cat : Cat.values()) {
					for (Correctness c : Correctness.values()) {
						scores.incrementCount(cat, c, that.scores.getCount(cat, c));
					}
				}
				for (Cat cat : Cat.values()) {
					for (Slot s : that.log.get(cat).keySet()) {
						if (log.get(cat, s) == null) {
							log.put(cat, s, new ArrayList<Pair<Correctness, String>>());
						}
						log.get(cat, s).addAll(that.log.get(cat, s));
					}
					addnLog.get(cat).append(that.addnLog.get(cat));
				}
			}
			return this;
		}

		public static Scorecard max(List<Scorecard> scores) {
			if (scores.isEmpty()) {
				return null;
			}
			List<Scorecard> sortedScores = new ArrayList<Scorecard>(scores);
			Collections.sort(sortedScores, Collections.reverseOrder(new ScorecardComparator()));
			return sortedScores.get(0);
		}

		public int getAbsoluteCount(Cat category, Correctness correctness) {
			return scores.get(category, correctness);
		}

		public double getFScore(Cat cat) {
			return fScore(scores.getCount(cat, Correctness.CORRECT), scores.getCount(cat, Correctness.INCORRECT),
					scores.getCount(cat, Correctness.NOMATCH));
		}

		protected double fScore(int numCorrect, int numIncorrect, int numNoMatch) {
			if (numCorrect == 0) {
				return 0.0;
			}
			double precision = (double) numCorrect / (numCorrect + numIncorrect);
			double recall = (double) numCorrect / (numCorrect + numIncorrect + numNoMatch);
			return (2 * precision * recall) / (precision + recall);
		}

		public void log(Cat category, Correctness correctness, String slotInfo, Integer count) {
			scores.incrementCount(category, correctness, count);
			addnLog.get(category).append(slotInfo).append(" is ").append(correctness).append("\n");
		}

//      public void log(Cat category, Correctness correctness, Slot slot) {
//         log(category, correctness, slot, "");
//      }

		public void log(Cat category, Correctness correctness, Slot slot, String fillerOf) {
			scores.incrementCount(category, correctness, 1);
			if (log.get(category, slot) == null) {
				log.put(category, slot, new ArrayList<Pair<Correctness, String>>());
			}
			log.get(category, slot).add(new Pair<Correctness, String>(correctness, fillerOf));
		}

//      public void alterScore(Cat category, Correctness newCorrectness, Slot slot) {
//         alterScore(category, newCorrectness, slot, "");
//      }
//
//      public void alterScore(Cat category, Correctness newCorrectness, Slot slot, String fillerOf) {
//         Correctness oldCorrectness = log.get(category, slot).getFirst();
//         scores.decrementCount(category, oldCorrectness, 1);
//         log(category, newCorrectness, slot, fillerOf);
//      }

		public String getLog() {
			StringBuffer sb = new StringBuffer();
			for (Cat cat : Cat.values()) {
				sb.append(cat.toString()).append("\n");
				for (Slot slot : log.get(cat).keySet()) {
					for (Pair<Correctness, String> logItem : log.get(cat, slot)) {
						sb.append(slot).append(" is ").append(logItem.getFirst());
						if (!logItem.getSecond().equals("")) {
							sb.append("(filling ").append(logItem.getSecond()).append(" )");
						}
						sb.append("\n");
					}
				}
				if (addnLog.get(cat).length() > 0) {
					sb.append(addnLog.get(cat));
				}
				sb.append("\n");
			}
			return sb.toString();
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();

			for (Cat cat : Cat.values()) {
				sb.append(cat.toString()).append(": \t[");
				for (Correctness c : Correctness.values()) {
					sb.append(scores.getCount(cat, c)).append("\t");
				}
				sb.deleteCharAt(sb.length() - 1).append("]\n");
			}

			sb.append("\n");

			for (Cat cat : Cat.values()) {
				sb.append(cat.toString()).append(" FScore: \t");
				sb.append(getFScore(cat));
				sb.append("\n");
			}

			return sb.toString();
		}

		public static class ScorecardComparator implements Comparator<Scorecard> {

			public int compare(Scorecard s1, Scorecard s2) {
				int core = compare(s1.getFScore(Cat.CORE), s1.getFScore(Cat.CORE));
				int bracket = compare(s1.getFScore(Cat.BRACKET), s2.getFScore(Cat.BRACKET));
				int res = compare(s1.getFScore(Cat.RES), s2.getFScore(Cat.RES));
				int cf = compare(s1.getFScore(Cat.CF), s2.getFScore(Cat.CF));

				return core != 0 ? core : bracket != 0 ? bracket : res != 0 ? res : cf;
			}

			protected int compare(double fScore1, double fScore2) {
				if (fScore1 == fScore2) {
					return 0;
				}
				else if (fScore1 > fScore2) {
					return 1;
				}
				else {
					return -1;
				}
			}
		}
	}

	public AnalysisVerifier(Grammar currentGrammar, ChildesTranscript currentTranscript,
			GrammarTables currentGrammarTables) {
		this.currentGrammar = currentGrammar;
		this.currentGrammarTables = currentGrammarTables;
		this.schemaCloneTable = this.currentGrammarTables.getSchemaCloneTable();
		this.currentTranscript = currentTranscript;
		this.contextModel = this.currentGrammar.getContextModel();
		this.cxnTypeSystem = currentGrammar.getCxnTypeSystem();
		this.schemaTypeSystem = currentGrammar.getSchemaTypeSystem();
		this.ontologyTypeSystem = currentGrammar.getOntologyTypeSystem();
	}

	public void verify(LearnerCentricAnalysis lca, GoldStandardAnnotation ga) {
		currentLCA = lca;
		currentGoldStandardAnnotation = ga;
		currentClause = currentLCA.getUtteranceAnalyzed();

		tables = currentLCA.getTables();
		spans = tables.getAllCxnalSpans();

		Scorecard TEscore = new Scorecard();
		Scorecard TSscore = new Scorecard();

		List<ExtendedFeatureBasedEntity> argStructs = currentGoldStandardAnnotation.getArgumentStructureAnnotations();
		Collection<ExtendedFeatureBasedEntity> reduplications = currentGoldStandardAnnotation
				.getAnnotationsOfType(GSPrimitive.REDUP);

		for (ExtendedFeatureBasedEntity annotation : argStructs) {
			if (annotation.getType().equals(GSPrimitive.TE.getType())) {
				Scorecard teScore = verifyTemporalElement(annotation, reduplications);
				TEscore.sum(teScore);
			}
			else if (annotation.getType().equals(GSPrimitive.TS.getType())) {
				Scorecard tsScore = verifyTemporalStructure(annotation, reduplications);
				TSscore.sum(tsScore);
			}
		}

		currentLCA.setVerifierScore(TEscore, TSscore);
	}

	protected Scorecard verifyTemporalElement(ExtendedFeatureBasedEntity temporalElement,
			Collection<ExtendedFeatureBasedEntity> reduplications) {
		Scorecard score = new Scorecard();

		if (temporalElement.getSpanLeft() == null || temporalElement.getSpanRight() == null) {
			// NOTE: the entire temporal element (main verb) is omitted.
			// This happens but very rarely (maybe once) in the data, so it's not dealt with here.
			return score;
		}

		Triplet<CxnalSpan, Set<String>, Set<String>> result = scoreMeaning(temporalElement, score, false, reduplications);
		CxnalSpan span = result.getFirst();
		Set<String> coreRolesToScore = result.getSecond();
		Set<String> adjRolesToScore = result.getThird();

		if (span == null)
			return score;

		// a match is found - should affect precision
		Slot cxnSlot = currentLCA.getSlot(span.getSlotID());
		Slot mSlot = cxnSlot.getSlot(ROLE_MPOLE);

		if (mSlot.getTypeConstraint() != null && mSlot.getTypeConstraint().getTypeSystem() == schemaTypeSystem) {
			verifyFeatures(temporalElement, score, coreRolesToScore, adjRolesToScore, cxnSlot, mSlot);
		}
		else {
			// no meaning type constraint found, probably due to an incorrect sense, lowers precision
			int penalty = coreRolesToScore.size(); // penalize for missing verb arguments
			score.log(Cat.CORE, Correctness.INCORRECT, mSlot,
					temporalElement.getType() + " " + temporalElement.getCategory());
			score.log(Cat.CORE, Correctness.NOMATCH, penalty + " roles in " + mSlot.toString(), penalty);
		}

		return score;
	}

	protected Scorecard verifyTemporalStructure(ExtendedFeatureBasedEntity temporalStructure,
			Collection<ExtendedFeatureBasedEntity> reduplications) {
		Scorecard score = new Scorecard();

		if (temporalStructure.getCategory().equalsIgnoreCase(AnnotationUtilities.GoldStandardAnnotationLocalizer.NONE)) {
			return score;
		}

		if (temporalStructure.getSpanLeft() == null || temporalStructure.getSpanRight() == null) {
			// this should not happen if the temporal structure category is not "none"
			throw new LearnerException("bizarre error occured: a temporal structure annotation without span encountered. "
					+ (temporalStructure.getID() != null ? "ID is " + temporalStructure.getID() : ""));
		}

		Triplet<CxnalSpan, Set<String>, Set<String>> result = scoreMeaning(temporalStructure, score, true, reduplications);
		CxnalSpan span = result.getFirst();
		Set<String> coreRolesToScore = result.getSecond();
		Set<String> adjRolesToScore = result.getThird();

		if (span == null)
			return score;

		// a match is found - should affect precision
		Slot cxnSlot = currentLCA.getSlot(span.getSlotID());
		Slot mSlot = cxnSlot.getSlot(ROLE_MPOLE);
		if (mSlot.getTypeConstraint() != null && mSlot.getTypeConstraint().getTypeSystem() == schemaTypeSystem) {
			if (mSlot.getTypeConstraint().getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				Slot eventTypeSlot = mSlot.getSlot(ROLE_EVENTTYPE);
				if (eventTypeSlot == null) {
					throw new LearnerException("No slot found for " + ChildesLocalizer.eventTypeRoleName + " in "
							+ mSlot.toString());
				}
				mSlot = eventTypeSlot;
			}
			verifyFeatures(temporalStructure, score, coreRolesToScore, adjRolesToScore, cxnSlot, mSlot);
		}
		else {
			// no meaning type constraint found, probably due to an incorrect sense, lowers precision
			int penalty = coreRolesToScore.size();
			score.log(Cat.CORE, Correctness.INCORRECT, mSlot,
					temporalStructure.getType() + " " + temporalStructure.getCategory());
			score.log(Cat.CORE, Correctness.NOMATCH, penalty + " roles in " + mSlot.toString(), penalty);
		}

		return score;
	}

	protected Scorecard verifyRecursiveStructures(ExtendedFeatureBasedEntity annotatedStructure, Slot mSlot) {
		Scorecard score = new Scorecard();
		Set<String> rolesToScore = new HashSet<String>(annotatedStructure.getRoles());
		if (mSlot.getTypeConstraint() != null) {
			verifyFeatures(annotatedStructure, score, rolesToScore, null, null, mSlot);
		}
		return score;
	}

	protected Triplet<CxnalSpan, Set<String>, Set<String>> scoreMeaning(ExtendedFeatureBasedEntity fStructure,
			Scorecard score, boolean isClausal, Collection<ExtendedFeatureBasedEntity> reduplications) {

		CxnalSpan span = getCxnalSpanWithSpan(spans.values(), fStructure.getSpanLeft(), fStructure.getSpanRight(),
				isClausal, reduplications);
		Set<String> rolesToScore = new HashSet<String>(extractRolesToScore(fStructure));

		Set<String> coreRolesToScore = new HashSet<String>(rolesToScore);
		coreRolesToScore.removeAll(localizer.getGoldStandardAdjunctRoles());

		Set<String> adjRolesToScore = new HashSet<String>(localizer.getGoldStandardAdjunctRoles());
		adjRolesToScore.retainAll(rolesToScore);

		if (span == null) {
			// no matches are found - should affect recall
			int penalty = coreRolesToScore.size() + 1; // penalize for missing verb and missing verb arguments
			score.log(Cat.CORE, Correctness.NOMATCH, penalty + " roles in and including " + fStructure.getType(), penalty);
			score.log(Cat.BRACKET, Correctness.NOMATCH, penalty + " roles in and including " + fStructure.getType() + " "
					+ fStructure.getCategory(), penalty);
		}
		else {
			score.log(Cat.BRACKET, Correctness.CORRECT, fStructure.getType() + " " + fStructure.getCategory(), 1);
		}

		return new Triplet<CxnalSpan, Set<String>, Set<String>>(span, coreRolesToScore, adjRolesToScore);
	}

	protected Set<String> extractRolesToScore(ExtendedFeatureBasedEntity fStructure) {
		if (fStructure.getRoles().size() == 0) {
			return new HashSet<String>();
		}

		if (fStructure.getType().equals(GSPrimitive.TE.getType())) {

			Set<String> rolesToScore = new HashSet<String>(fStructure.getRoles());
			return rolesToScore;

		}
		else if (fStructure.getType().equals(GSPrimitive.TS.getType())) {

			String teID = fStructure.getAttributeValue(ChildesConstants.PROFILED);
			ExtendedFeatureBasedEntity profiledTE = null;
			if (teID != null) {
				profiledTE = getAnnotationWithID(teID);
			}

			Set<String> rolesToScore = new HashSet<String>(fStructure.getRoles());

			// remove all argument structure arguments that are also arguments of the verb to avoid double-scoring
			if (profiledTE != null) {
				for (Binding tsBinding : fStructure.getAllBindings()) {
					for (Binding teBinding : profiledTE.getAllBindings()) {
						if (tsBinding.getSpanLeft() != null && teBinding.getSpanLeft() != null
								&& tsBinding.getSpanRight() != null && teBinding.getSpanRight() != null
								&& tsBinding.getSpanLeft() == teBinding.getSpanLeft()
								&& tsBinding.getSpanRight() == teBinding.getSpanRight()) {
							// same bracketing, therefore assume to be idential arguments
							rolesToScore.remove(tsBinding.getField());
						}
						else if (tsBinding.getAttributeValue(ChildesConstants.REFERENCE) != null
								&& teBinding.getAttributeValue(ChildesConstants.REFERENCE) != null
								&& tsBinding.getAttributeValue(ChildesConstants.REFERENCE).equals(
										teBinding.getAttributeValue(ChildesConstants.REFERENCE))) {
							// omitted arguments but same reference
							rolesToScore.remove(tsBinding.getField());
						}
						else if (tsBinding.getAttributeValue(ChildesConstants.VALUE) != null
								&& teBinding.getAttributeValue(ChildesConstants.VALUE) != null
								&& tsBinding.getAttributeValue(ChildesConstants.VALUE).equals(
										teBinding.getAttributeValue(ChildesConstants.VALUE))) {
							// omitted arguments but same value
							rolesToScore.remove(tsBinding.getField());
						}
					}
				}
			}

			rolesToScore.removeAll(localizer.getGoldStandardRolesToIgnore());
			return rolesToScore;

		}
		else {

			Set<String> rolesToScore = new HashSet<String>(fStructure.getRoles());
			return rolesToScore;

		}
	}

	protected void verifyFeatures(ExtendedFeatureBasedEntity fStructure, Scorecard score, Set<String> coreRolesToScore,
			Set<String> adjRolesToScore, Slot cxnSlot, Slot mSlot) {

		String annotatedType = fStructure.getCategory();
		String mType = mSlot.getTypeConstraint().getType();
		Set<String> aTypes = localizer.getGoldStandardLocalization(annotatedType);

		Set<String> subtypeOf = new HashSet<String>();

		for (String aType : aTypes) {
			try {
				if (schemaTypeSystem.subtype(schemaTypeSystem.getInternedString(mType),
						schemaTypeSystem.getInternedString(aType))) {
					subtypeOf.add(aType);
				}
			}
			catch (TypeSystemException tse) {
				logger.warning("encountered error while trying to look up " + mType + " or " + aType
						+ " in the type system.");
			}
		}

		if (!subtypeOf.isEmpty()) {
			score.log(Cat.CORE, Correctness.CORRECT, mSlot, fStructure.getType() + " " + fStructure.getCategory());
			try {
				String strictestSupertype = schemaTypeSystem.bestCommonSubtype(subtypeOf, false);
				score.sum(verifyFeatureHelper(fStructure, strictestSupertype, coreRolesToScore, adjRolesToScore, cxnSlot,
						mSlot));
			}
			catch (TypeSystemException tse) {
				logger.warning("encountered error while trying to find the best common subtype of " + subtypeOf
						+ " in the type system.");
			}
		}
		else {
			score.log(Cat.CORE, Correctness.INCORRECT, mSlot, fStructure.getType() + " " + fStructure.getCategory());
			try {
				String loosestSupertype = schemaTypeSystem.bestCommonSupertype(aTypes, false);
				score.sum(verifyFeatureHelper(fStructure, loosestSupertype, coreRolesToScore, adjRolesToScore, cxnSlot,
						mSlot));
			}
			catch (TypeSystemException tse) {
				logger.warning("encountered error while trying to find the best common supertype of " + aTypes
						+ " in the type system.");
			}
		}

	}

	protected Scorecard verifyFeatureHelper(ExtendedFeatureBasedEntity fStructure, String fStructureType,
			Set<String> coreRolesToScore, Set<String> adjRolesToScore, Slot cxnSlot, Slot mSlot) {
		Scorecard score = new Scorecard();

		if (fStructure.getRoles().size() == 0) {
			return score;
		}

		if (coreRolesToScore != null) {
			if (coreRolesToScore.size() != 0 && !mSlot.hasStructuredFiller()) {
				int penalty = coreRolesToScore.size();
				score.log(Cat.CORE, Correctness.NOMATCH, penalty + " roles in " + mSlot.toString(), penalty);
				score.log(Cat.BRACKET, Correctness.NOMATCH, penalty + " roles in " + mSlot.toString(), penalty);
				return score;
			}

			Type ontType = (Type) ontologyTypeSystem.get(fStructureType);
			for (String role : coreRolesToScore) {
				Set<Binding> bindings = fStructure.getBinding(role);
				String localRoleName = localizer.getGoldStandardRoleNameLocalization(fStructureType, role);
				List<String> coindexedRoles = AnnotationUtilities.getCoindexedRoleNames(currentGrammar, schemaCloneTable,
						ontType, localRoleName);
				if (coindexedRoles == null) {
					logger.warning("Role " + role + " is not defined for ontology type " + fStructureType);
				}
				score.sum(Scorecard.max(verifyCoreFeatureBinding(bindings, mSlot, coindexedRoles, localizer
						.getGoldStandardRolesWithImpreciseBracketing().contains(role))));
			}
		}

		if (adjRolesToScore != null) {
			if (adjRolesToScore.size() != 0 && !mSlot.hasStructuredFiller()) {
				int penalty = coreRolesToScore.size(); // penalize for missing verb and missing verb arguments
				score.log(Cat.ADJ, Correctness.NOMATCH, penalty + " roles in " + mSlot.toString(), penalty);
				return score;
			}

			for (String role : adjRolesToScore) {
				Set<Binding> bindings = fStructure.getBinding(role);
				score.sum(verifyAdjFeatureBinding(bindings, cxnSlot, mSlot, role, localizer
						.getGoldStandardRolesWithImpreciseBracketing().contains(role)));
			}
		}

		return score;
	}

	private Pair<String, Slot> getCorrespondingSlot(Slot parentSlot, List<String> coindexedRoles) {
		Slot roleSlot = null;
		for (String coindexedRole : coindexedRoles) {

			List<String> chain = Arrays.asList(coindexedRole.split("\\."));
			if (chain.size() == 1) {
				if ((roleSlot = parentSlot.getSlot(new Role(chain.get(0)))) != null) {
					// just need to find one out of these to be found in the semspec.
					return new Pair<String, Slot>(chain.get(0), roleSlot);
				}
			}
			else {
				Slot tmp = parentSlot;
				for (String role : chain) {
					if (tmp != null) {
						tmp = tmp.getSlot(new Role(role));
					}
				}
				if (tmp != null) {
					return new Pair<String, Slot>(coindexedRole, tmp);
				}
			}
		}
		return null;
	}

	private List<String> queryTypes(List<String> contextElements) {
		List<String> types = new ArrayList<String>();
		for (String contextElement : contextElements) {
			List<SimpleQuery> simpleQueries = new ArrayList<SimpleQuery>();
			simpleQueries.add(new SimpleQuery(contextElement, "?x"));
			Set<String> results = ContextUtilities.collapseResults(contextModel.query(simpleQueries, true));
			if (results.size() == 0) {
				throw new LearnerException("Bizarre error: the named context element " + contextElement
						+ " does not exist in context");
			}
			else if (results.size() > 1) {
				throw new LearnerException("Bizarre error: more than one type retrieved for the context element "
						+ contextElement);
			}
			types.addAll(results);
		}
		return types;
	}

	protected List<Scorecard> verifyCoreFeatureBinding(Set<Binding> bindings, Slot mSlot, List<String> coindexedRoles,
			boolean impreciseBracketing) {

		List<Scorecard> scores = new ArrayList<Scorecard>();

		Pair<String, Slot> correspondingSlot = getCorrespondingSlot(mSlot, coindexedRoles);
		if (correspondingSlot == null) {
			Scorecard score = new Scorecard();
			score.log(Cat.CORE, Correctness.NOMATCH, mSlot.toString() + "." + coindexedRoles, 1);
			score.log(Cat.BRACKET, Correctness.NOMATCH, mSlot.toString() + "." + coindexedRoles, 1);
			scores.add(score);
			return scores;
		}
		String correspondingRole = correspondingSlot.getFirst();
		Slot roleSlot = correspondingSlot.getSecond();

		for (Binding binding : bindings) {
			Scorecard score = verifySingleCoreBinding(binding, mSlot, coindexedRoles, impreciseBracketing,
					correspondingRole, roleSlot);
			scores.add(score);
		}

		return scores;
	}

	// parameter object
	private class Filler {
		public List<String> contextElements = new ArrayList<String>();
		public List<String> bindingFillerTypes = new ArrayList<String>();
		public boolean DNIorINI = false;
		public String ref = null;
		boolean recursive = false;

		public Filler(List<String> contextElements, List<String> bindingFillerTypes, boolean DNIorINI, String ref,
				boolean recursive) {
			this.contextElements = contextElements;
			this.bindingFillerTypes = bindingFillerTypes;
			this.DNIorINI = DNIorINI;
			this.ref = ref;
			this.recursive = recursive;
		}
	}

	private Filler getFiller(Binding binding) {

		List<String> contextElements = new ArrayList<String>();
		List<String> bindingFillerTypes = new ArrayList<String>();
		boolean DNIorINI = false;
		String ref = null;
		boolean recursive = false;

		if (binding.getAttributeValue(ChildesConstants.REFERENCE) != null) {
			ref = binding.getAttributeValue(ChildesConstants.REFERENCE);
			if (AnnotationUtilities.isFunction(ref)) {
				List<String> resolved = AnnotationUtilities.resolveFunction(contextModel, ref);
				for (String contextElement : resolved) {
					contextElements.add(ContextModel.getIndividualName(contextElement));
					bindingFillerTypes.add(ContextModel.getIndividualType(contextElement));
				}
			}
			else if (ref.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.DNI)
					|| ref.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.INI)) {
				// annotation uses DNI when a specific entity is referred to but its identity is not established by previous
				// transcript annotation
				// in either case, resort to scoring resolution and context fitting based on just the type
				DNIorINI = true;
			}
			else if (contextModel.retrieveIndividual(new SimpleQuery(ref, null), true) == null) {
				// this could be an annotation-internal reference
				ExtendedFeatureBasedEntity anno = getAnnotationWithID(ref);
				if (anno == null) {
					FeatureBasedEntity<?> entity = getTranscriptElementWithID(ref);
					if (entity == null) {
						throw new LearnerException("Error in annotation: an annotation-internal reference " + ref
								+ " is not previously established in the annotation");
					}
					if (entity.getCategory() != null) {
						bindingFillerTypes.addAll(localizer.getGoldStandardLocalization(entity.getCategory()));
					}
				}
				else {
					if (anno.getType().equals(GSPrimitive.TE.getType()) || anno.getType().equals(GSPrimitive.TS.getType())) {
						if (anno.getCategory() != null) {
							bindingFillerTypes.addAll(localizer.getGoldStandardLocalization(anno.getCategory()));
						}
					}
					else {
						recursive = true;
					}
				}
			}
			else {
				contextElements.add(ref);
				bindingFillerTypes = queryTypes(contextElements);
			}
		}

		return new Filler(contextElements, bindingFillerTypes, DNIorINI, ref, recursive);

	}

	protected Correctness checkBracketing(Binding binding, Slot roleSlot, boolean DNIorINI, boolean impreciseBracketing) {

		boolean omitted = false;
		if (!DNIorINI && binding.getSpanLeft() == null && binding.getSpanRight() == null) {
			omitted = true;
		}

		Set<Integer> parents = new HashSet<Integer>(tables.getParentSlots(roleSlot.getID()));
		parents.retainAll(tables.getAllCxnSlots());

		if (DNIorINI && parents.isEmpty()) {
			return Correctness.CORRECT;
		}
		else if (DNIorINI && !parents.isEmpty()) {
			return Correctness.INCORRECT;
		}
		else if (!DNIorINI && parents.isEmpty()) {
			return Correctness.NOMATCH;
		}
		else {

			boolean foundCorrectBracketing = false;

			for (Iterator<Integer> i = parents.iterator(); i.hasNext();) {
				Integer slotID = i.next();
				Slot parentSlot = currentLCA.getSlot(slotID);
				Set<Role> roleThatPointToRoleSlot = new HashSet<Role>();
				for (Role r : parentSlot.getFeatures().keySet()) {
					if (parentSlot.getSlot(r) == roleSlot) {
						roleThatPointToRoleSlot.add(r);
					}
				}
				if (!roleThatPointToRoleSlot.contains(ROLE_MPOLE)) {
					i.remove();
				}
			}
			if (parents.isEmpty()) {
				if (omitted) {
					foundCorrectBracketing = true;
				}
			}
			else {
				for (Integer slotID : parents) {
					CxnalSpan roleSpan = spans.get(slotID);
					if (!omitted
							&& roleSpan != null
							&& isOverlapping(binding.getSpanLeft(), binding.getSpanRight(), roleSpan.getLeft(),
									roleSpan.getRight())) {
						foundCorrectBracketing = true;
					}
					else if (omitted
							&& (roleSpan == null || (roleSpan != null && roleSpan.getLeft() == roleSpan.getRight()))) {
						foundCorrectBracketing = true;
					}
				}
			}
			return foundCorrectBracketing ? Correctness.CORRECT : Correctness.INCORRECT;
		}

	}

	protected Correctness checkContextFitting(Binding binding, Slot roleSlot, List<String> contextElements) {

		ContextualFit fit = currentLCA.getContextualFit();
		if (fit == null)
			return Correctness.NOMATCH;

		String candidate = fit.getCandidate(roleSlot.getID());
		if (candidate == null) {
			fit.setVerificationResults(roleSlot.getID(), Correctness.NOMATCH);
			return Correctness.NOMATCH;
		}
		else {
			boolean hasCorrectCFResults = false;
			for (String contextElement : contextElements) {
				hasCorrectCFResults |= ContextModel.getIndividualName(candidate).equals(contextElement);
			}
			fit.setVerificationResults(roleSlot.getID(), hasCorrectCFResults ? Correctness.CORRECT : Correctness.INCORRECT);
			return hasCorrectCFResults ? Correctness.CORRECT : Correctness.INCORRECT;
		}
	}

	protected Correctness checkResolution(Binding binding, Slot roleSlot, List<String> contextElements) {
		LCAResolution res = null;
		Integer rdSlotID = tables.getRelatedRD(roleSlot.getID());
		if (rdSlotID == null && binding.getSpanLeft() == null && binding.getSpanRight() == null) { // this is an omitted
																																	// item
			rdSlotID = roleSlot.getID();
		}
		if (rdSlotID != null) {
			res = currentLCA.getResolution(rdSlotID);
		}
		if (res == null) {
			return Correctness.NOMATCH;
		}
		else {
			boolean hasCorrectResolutionResults = false;
			for (String contextElement : contextElements) {
				hasCorrectResolutionResults |= res.isCandidate(contextElement);
			}
			return hasCorrectResolutionResults ? Correctness.CORRECT : Correctness.INCORRECT;
		}
	}

	protected Correctness checkCoreType(Binding binding, Slot mSlot, List<String> coindexedRoles,
			String correspondingRole, Slot roleSlot, Filler filler) {

		// FIXME: wh-values should be handled by checking givenness

		if (binding.getAttributeValue(ChildesConstants.VALUE) != null) {
			String localizedFiller = localizer.getGoldStandardRoleFillerLocalization(mSlot.getTypeConstraint().getType(),
					correspondingRole, binding.getAttributeValue(ChildesConstants.VALUE));
			filler.bindingFillerTypes.add(localizedFiller);
		}

		// NOTE: the directionality of the subtype has to be reversed because the actual filler type in context tends to
		// be more specific
		TypeSystem<?> typeSystem = roleSlot.getTypeConstraint().getTypeSystem();
		String mType = roleSlot.getTypeConstraint().getType();

		boolean hasCorrectTypeConstraint = LearnerUtilities.isProcess(currentGrammar, roleSlot.getTypeConstraint()) ? hasCorrectTypeConstraint(
				mType, filler.bindingFillerTypes, typeSystem, false) : hasCorrectTypeConstraint(mType,
				filler.bindingFillerTypes, typeSystem, true);
		return hasCorrectTypeConstraint ? Correctness.CORRECT : Correctness.INCORRECT;

	}

	private Scorecard verifySingleCoreBinding(Binding binding, Slot mSlot, List<String> coindexedRoles,
			boolean impreciseBracketing, String correspondingRole, Slot roleSlot) {

		Filler filler = getFiller(binding);
		if (filler.recursive) {
			return verifyRecursiveStructures(getAnnotationWithID(filler.ref), roleSlot);
		}

		Scorecard score = new Scorecard();
		Correctness bracketing = checkBracketing(binding, roleSlot, filler.DNIorINI, impreciseBracketing);
		if (bracketing == Correctness.NOMATCH || bracketing == Correctness.INCORRECT) {
			score.log(Cat.BRACKET, bracketing, roleSlot, mSlot.toString() + "." + coindexedRoles);
		}
		else {
			score.log(Cat.BRACKET, Correctness.CORRECT, roleSlot, mSlot.toString() + "." + coindexedRoles);
		}

		if (bracketing == Correctness.CORRECT || impreciseBracketing) {
			// Score core argument type for only those that have correct bracketing.
			Correctness core = checkCoreType(binding, mSlot, coindexedRoles, correspondingRole, roleSlot, filler);
			if (core == Correctness.CORRECT) {
				score.log(Cat.CORE, Correctness.CORRECT, roleSlot, mSlot.toString() + "." + coindexedRoles);
			}
			else {
				score.log(Cat.CORE, core, roleSlot, mSlot.toString() + "." + coindexedRoles);
			}
		}
		else {
			score.log(Cat.CORE, bracketing, roleSlot, mSlot.toString() + "." + coindexedRoles);
		}

		if (filler.DNIorINI) {
			return score;
		}

		// Score both res and cf for those that are not INI or DNI, even if bracketing is incorrect.
		if (filler.ref != null) {
			Correctness cf = checkContextFitting(binding, roleSlot, filler.contextElements);
			score.log(Cat.CF, cf, roleSlot, mSlot.toString() + "." + coindexedRoles);

			Correctness res = checkResolution(binding, roleSlot, filler.contextElements);
			score.log(Cat.RES, res, roleSlot, mSlot.toString() + "." + coindexedRoles);
		}

		return score;
	}

	private boolean hasCorrectTypeConstraint(String scoredType, Collection<String> goldStandardTypes,
			TypeSystem<?> typeSystem, boolean reverseDirection) {
		boolean hasCorrectTypeConstraint = false;
		for (String goldStandardType : goldStandardTypes) {
			try {
				if (reverseDirection) {
					hasCorrectTypeConstraint |= typeSystem.subtype(typeSystem.getInternedString(goldStandardType),
							typeSystem.getInternedString(scoredType));
				}
				else {
					hasCorrectTypeConstraint |= typeSystem.subtype(typeSystem.getInternedString(scoredType),
							typeSystem.getInternedString(goldStandardType));
				}
			}
			catch (TypeSystemException tse) {
				logger.warning("encountered error while trying to look up " + scoredType + " or " + goldStandardType
						+ " in the type system.");
			}
		}
		return hasCorrectTypeConstraint;
	}

	protected ExtendedFeatureBasedEntity getAnnotationWithID(String ID) {
		List<ExtendedFeatureBasedEntity> annotations = currentGoldStandardAnnotation.getAllAnnotations();

		for (ExtendedFeatureBasedEntity annotation : annotations) {
			if (annotation.getID() != null) {
				if (annotation.getID().equals(ID)) {
					return annotation;
				}
			}
		}
		return null;
	}

	protected FeatureBasedEntity<?> getTranscriptElementWithID(String ID) {
		return currentTranscript.getPrecedingAnnotationWithID(ID, currentGoldStandardAnnotation);
	}

	protected Scorecard verifyAdjFeatureBinding(Set<Binding> bindings, Slot cxnSlot, Slot mSlot, String role,
			boolean impreciseBracketing) {

		if (role.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.ASPECT)) {

			Slot modifierAspectSlot = null;
			// go into event descriptor's (which should be the type constraint of mSlot) event structure schema
			if (mSlot.getTypeConstraint() != null
					&& mSlot.getTypeConstraint().getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				modifierAspectSlot = mSlot.getSlot(ROLE_EVENT_STRUCTURE).getSlot(ROLE_MODIFIER_ASPECT);
			}
			else {
				try {
					modifierAspectSlot = cxnSlot.getSlot(ROLE_EVENT_DESCRIPTOR).getSlot(ROLE_EVENT_STRUCTURE)
							.getSlot(ROLE_MODIFIER_ASPECT);
				}
				catch (NullPointerException npe) {
				}
			}
			return scoreSubCat(bindings, modifierAspectSlot, cxnSlot, ChildesLocalizer.modifierAspectRoleName);

		}
		else if (role.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.MODALITY)) {

			Slot modalityCategorySlot = null;
			if (mSlot.getTypeConstraint() != null
					&& mSlot.getTypeConstraint().getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				modalityCategorySlot = mSlot.getSlot(ROLE_MODALITY).getSlot(ROLE_MODALITY_CATEGORY);
			}
			else {
				try {
					modalityCategorySlot = cxnSlot.getSlot(ROLE_EVENT_DESCRIPTOR).getSlot(ROLE_MODALITY)
							.getSlot(ROLE_MODALITY_CATEGORY);
				}
				catch (NullPointerException npe) {
				}
			}
			return scoreSubCat(bindings, modalityCategorySlot, cxnSlot, ChildesLocalizer.modalityRoleName);

		}
		else if (role.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.MODIFIER)) {

			Slot modifierCategorySlot = null;
			if (mSlot.getTypeConstraint() != null
					&& mSlot.getTypeConstraint().getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				modifierCategorySlot = mSlot.getSlot(ROLE_MODIFIERS).getSlot(ROLE_MODIFIER_CATEGORY);
			}
			else {
				try {
					modifierCategorySlot = cxnSlot.getSlot(ROLE_EVENT_DESCRIPTOR).getSlot(ROLE_MODIFIERS)
							.getSlot(ROLE_MODIFIER_CATEGORY);
				}
				catch (NullPointerException npe) {
				}
			}
			return scoreSubCat(bindings, modifierCategorySlot, cxnSlot, ChildesLocalizer.modifiersRoleName);

		}
		else if (role.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.RESULTATIVE)) {
			return new Scorecard();
		}
		else {
			return new Scorecard();
		}
	}

	private Scorecard scoreSubCat(Set<Binding> goldStandardBindings, Slot scoredSlot, Slot cxnSlot, String scoredRole) {
		Scorecard score = new Scorecard();

		if (scoredSlot == null) {
			score.log(Cat.ADJ, Correctness.NOMATCH, scoredRole + " in " + cxnSlot.toString(), 1);
			return score;
		}

		String aspect = scoredSlot.getTypeConstraint().getType();

		Set<String> goldStandardAspects = new HashSet<String>();
		for (Binding binding : goldStandardBindings) {
			goldStandardAspects.addAll(localizer.getGoldStandardLocalization(binding
					.getAttributeValue(ChildesConstants.SUBCAT)));
		}
		boolean hasCorrectTypeConstraint = hasCorrectTypeConstraint(aspect, goldStandardAspects, ontologyTypeSystem,
				false);
		if (hasCorrectTypeConstraint) {
			score.log(Cat.ADJ, Correctness.CORRECT, scoredSlot, scoredRole + " in " + cxnSlot.toString());
		}
		else {
			score.log(Cat.ADJ, Correctness.INCORRECT, scoredSlot, scoredRole + " in " + cxnSlot.toString());
		}
		return score;
	}

	/**
	 * This method is a slew of crappy heuristics because the annotation doesn't always bracket the utterance the way the
	 * constructions would, and could be inconsistent when it comes to clausal brackets (e.g. the subject is usually
	 * included in the temporal structure annotation except in say, serial verb clauses where subjects are shared). On
	 * the other hand, negative imperatives, etc are not usually in the bracket even though the clausal constructions
	 * would pick them up.
	 */

	protected CxnalSpan getCxnalSpanWithSpan(Collection<CxnalSpan> spans, Integer left, Integer right,
			boolean preferClausal, Collection<ExtendedFeatureBasedEntity> reduplications) {

		int annotatedLeft = left;
		int annotatedRight = right;

		// first get rid of the reduplicated span
		if (reduplications != null && !reduplications.isEmpty()) {
			for (ExtendedFeatureBasedEntity redup : reduplications) {
				if (redup.getSpanLeft() == annotatedLeft) { // redup occurs in the beginning of the labelled span
					// make the realLeft the spanRight of the reduplication
					annotatedLeft = redup.getSpanRight();
				}
				else if (redup.getSpanRight() == annotatedRight) {// redup occurs at the end of the labelled span
					// make the realRight the spanLeft of the reduplication
					annotatedRight = redup.getSpanLeft();
				}
			}
		}

		Pair<Integer, Integer> realSpan = getRealSpan(annotatedLeft, annotatedRight);
		int realLeft = realSpan.getFirst();
		int realRight = realSpan.getSecond();

		List<CxnalSpan> matchingSpans = new ArrayList<CxnalSpan>();
		for (CxnalSpan span : spans) {
			if (span.getLeft() == realLeft && span.getRight() == realRight) {
				matchingSpans.add(span);
			}
		}

		// if it's not lexical, then it's easy. Return the only one we've got or the first one that is a morpheme / word
		if (!preferClausal) {
			if (matchingSpans.size() == 0) {
				return null;
			}
			else if (matchingSpans.size() == 1) {
				return matchingSpans.get(0);
			}
			else {
				String morpheme = cxnTypeSystem.getInternedString(ChildesLocalizer.MORPHEME);
				String word = cxnTypeSystem.getInternedString(ChildesLocalizer.WORD);

				for (CxnalSpan span : matchingSpans) {
					String type = currentLCA.getSlot(span.getSlotID()).getTypeConstraint() != null ? currentLCA
							.getSlot(span.getSlotID()).getTypeConstraint().getType() : null;
					try {
						if (type != null && cxnTypeSystem.subtype(type, morpheme) || cxnTypeSystem.subtype(type, word)) {
							return span;
						}
					}
					catch (TypeSystemException tse) {
						logger.warning("encountered error while trying to look up " + type + " in the type system.");
					}
				}
			}
		}
		else {

			List<CxnalSpan> clausalSpans = retainClausalSpan(matchingSpans);

			// Repeat this once. Either because no matching spans were found to start with or none of them were clausal,
			// expand the bracket outwards to see if anything else useful can be found.
			if (clausalSpans.size() == 0) {
				// temporal_structures aren't always annotated with the "correct" spans that corresond to a construction
				// try extending the brackets outwards and finding the tightest-fitting span
				for (CxnalSpan span : spans) {
					if (isCovering(span.getLeft(), span.getRight(), realLeft, realRight)) {
						matchingSpans.add(span);
					}
				}
				clausalSpans = retainClausalSpan(matchingSpans);
			}

			if (clausalSpans.size() == 0) {
				return null;
			}
			else if (clausalSpans.size() == 1) {
				return clausalSpans.get(0);
			}
			else { // find the span with the tightest bracket
				int spanSize = Integer.MAX_VALUE;
				CxnalSpan smallestSpan = null;
				for (CxnalSpan span : clausalSpans) {
					if ((span.getRight() - span.getLeft()) < spanSize) {
						spanSize = span.getRight() - span.getLeft();
						smallestSpan = span;
					}
				}
				return smallestSpan;
			}
		}
		return null;
	}

	private List<CxnalSpan> retainClausalSpan(List<CxnalSpan> matchingSpans) {
		List<CxnalSpan> clausalSpans = new ArrayList<CxnalSpan>();

		String clause = cxnTypeSystem.get(ChildesLocalizer.CLAUSE) == null ? null : cxnTypeSystem
				.getInternedString(ChildesLocalizer.CLAUSE);
		String vc = cxnTypeSystem.get(ChildesLocalizer.VC) == null ? null : cxnTypeSystem
				.getInternedString(ChildesLocalizer.VC);
		String voicing = cxnTypeSystem.get(ChildesLocalizer.VOICING) == null ? null : cxnTypeSystem
				.getInternedString(ChildesLocalizer.VOICING);
		String finiteClause = cxnTypeSystem.get(ChildesLocalizer.FINITE_CLAUSE) == null ? null : cxnTypeSystem
				.getInternedString(ChildesLocalizer.FINITE_CLAUSE);

		for (CxnalSpan span : matchingSpans) {
			String type = currentLCA.getSlot(span.getSlotID()).getTypeConstraint() != null ? currentLCA
					.getSlot(span.getSlotID()).getTypeConstraint().getType() : null;
			if (type == null)
				continue;
			type = cxnTypeSystem.getInternedString(type);
			try {
				if ((clause != null && cxnTypeSystem.subtype(type, clause))
						|| (vc != null && cxnTypeSystem.subtype(type, vc))
						|| (voicing != null && cxnTypeSystem.subtype(type, voicing))
						|| (finiteClause != null && cxnTypeSystem.subtype(type, finiteClause))) {
					clausalSpans.add(span);
				}
			}
			catch (TypeSystemException tse) {
				logger.warning("encountered error while trying to look up " + type + " in the type system.");
			}
		}
		return clausalSpans;
	}

	protected boolean isOverlapping(int bindingLeft, int bindingRight, int cxnSpanLeft, int cxnSpanRight) {
		Pair<Integer, Integer> realSpan = getRealSpan(bindingLeft, bindingRight);
		int realLeft = realSpan.getFirst();
		int realRight = realSpan.getSecond();

		if (realLeft == cxnSpanLeft)
			return true;

		if (realLeft < cxnSpanLeft && realRight > cxnSpanLeft)
			return true;

		if (realLeft > cxnSpanLeft && cxnSpanRight > realLeft)
			return true;

		return false;
	}

	protected boolean isCovering(int cxnSpanLeft, int cxnSpanRight, int realLeft, int realRight) {
		return (cxnSpanLeft <= realLeft && cxnSpanRight >= realRight);
	}

	protected Pair<Integer, Integer> getRealSpan(int bindingLeft, int bindingRight) {
		// This takes into account whether the transcript / grammar uses compound words.
		// the role binding counts are expressed in terms of character index.

		Integer realLeft, realRight;
		if (ChildesTranscript.useCompoundWords()) {
			realLeft = currentClause.getLexToCompoundSpanMap().get(bindingLeft);
			if (realLeft == null)
				logger.warning("Gold Standard Annotation messed up bracketing information on " + bindingLeft);
			while (realLeft == null && bindingLeft > 0) {// this may happen if the annotation is messed up and gives a
																		// within-compound-word index
				bindingLeft--; // try to fix it by heuristically moving further to the left (this is more generous)
				realLeft = currentClause.getLexToCompoundSpanMap().get(bindingLeft);
			}

			realRight = currentClause.getLexToCompoundSpanMap().get(bindingRight);
			if (realRight == null)
				logger.warning("Gold Standard Annotation messed up bracketing information on " + bindingRight);
			while (realRight == null && bindingRight < currentClause.getLexSize()) {// this may happen if the annotation is
																											// messed up and gives a
																											// within-compound-word index
				bindingRight++; // try to fix it by heuristically moving further to the left (this is more generous)
				realRight = currentClause.getLexToCompoundSpanMap().get(bindingRight);
			}
		}
		else {
			realLeft = bindingLeft;
			realRight = bindingRight;
		}
		return new Pair<Integer, Integer>(realLeft, realRight);
	}
}

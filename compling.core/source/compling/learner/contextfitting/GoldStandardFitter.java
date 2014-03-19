// =============================================================================
//File        : ContextFitter.java
//Author      : emok
//Change Log  : Created on Mar 18, 2007
//=============================================================================

package compling.learner.contextfitting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesConstants;
import compling.annotation.childes.ChildesConstants.GSPrimitive;
import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.FeatureBasedEntity.Binding;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.context.ContextUtilities;
import compling.context.MiniOntology.Type;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.contextfitting.ContextualFitScorer.NestedScorerFactory;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.util.AnnotationUtilities;
import compling.learner.util.AnnotationUtilities.GoldStandardAnnotationLocalizer;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Pair;

//=============================================================================

public class GoldStandardFitter extends BasicContextFitter {

	GoldStandardAnnotation goldStandardAnnotation = null;
	GoldStandardAnnotationLocalizer goldStandardLocalizer = null;
	GrammarTables grammarTables = null;

	public GoldStandardFitter(Grammar currentGrammar, GrammarTables grammarTables, boolean aggressiveFit,
			ContextFittingLocalizer localizer, GoldStandardAnnotation goldStandardAnnotation,
			GoldStandardAnnotationLocalizer goldStandardLocalizer) {
		super(currentGrammar, new NestedScorerFactory(), aggressiveFit, localizer);
		this.grammarTables = grammarTables;
		this.goldStandardAnnotation = goldStandardAnnotation;
		this.goldStandardLocalizer = goldStandardLocalizer;
	}

	@Override
	public ContextualFit getContextualFit(LearnerCentricAnalysis lca) {
		this.currentLCA = lca;

		currentDS = lca.getCurrentDS();
		currentSpeechAct = lca.getCurrentSpeechAct();
		currentSpeechActType = lca.getCurrentSpeechActType();
		currentAddressee = lca.getCurrentAddressee();
		jointAttention = lca.getJointAttention();

		bestFit = new ContextualFit(currentLCA, "bestFit", ontologyTypeSystem);
		tables = currentLCA.getTables();
		slotCounter = currentLCA.getLargestAssignedSlotID();
		slotCounter++;

		fittableByType.clear();
		fittableByType.initialize(Arrays.asList(FittableType.values()));
		fittable.clear();

		fitToContext(aggressiveFit && currentLCA.getNumSeparateAnalyses() > AGGRESSIVE_PARAM);
		currentLCA.setContextualFit(bestFit);
		return bestFit;
	}

	@Override
	protected void incorporateBest(ContextualFit fit, Set<ContextualFit> fits) {
		if (fits == null)
			return;
		ContextualFit bestCandidate = null;
		for (ContextualFit f : fits) {
			if (!f.isCatastrophicallyImcompatible()) {
				bestCandidate = f;
				break;
			}
		}

		if (bestCandidate != null) {
			fit.incorporate(bestCandidate);
		}
	}

	@Override
	protected Pair<Set<String>, Boolean> findContextElements(FittableType type, Integer slotID) {
		Set<String> contextElementNames = null;
		boolean partialFit = false;
		Slot slot = currentLCA.getSlot(slotID);
		TypeConstraint typeConstraint = slot.getTypeConstraint();

		if (typeConstraint == null) {
			return new Pair<Set<String>, Boolean>(new HashSet<String>(), false);
		}

		if (type == FittableType.DS) {
			contextElementNames = new HashSet<String>();
			contextElementNames.add(currentDS);
		}
		else if (type == FittableType.DISCOURSE_PARTTCIPANT) {
			contextElementNames = queryContext(typeConstraint, false);
		}
		else if (type == FittableType.STRUCTURED_ELEMENT || type == FittableType.UNSTRUCTURED_ELEMENT) {

			// find the corresponding cxnalSpan and use it to look up gold standard
			Map<Integer, CxnalSpan> spans = currentLCA.getCxnalSpans();
			Set<Integer> parentSlots = new HashSet<Integer>(tables.getParentSlots(slotID));
			parentSlots.retainAll(tables.getAllCxnSlots());
			if (parentSlots.isEmpty()) {
				contextElementNames = new HashSet<String>();
				for (Integer parentSlotID : tables.getParentSlots(slotID)) {
					Slot parentSlot = currentLCA.getSlot(parentSlotID);
					Role role = null;
					for (Role r : parentSlot.getFeatures().keySet()) {
						if (parentSlot.getSlot(r) == slot) {
							role = r;
						}
					}
					Pair<Set<String>, Boolean> ret = findContextElements(parentSlot, null, role, slotID);
					contextElementNames.addAll(ret.getFirst());
				}
			}
			else {
				contextElementNames = new HashSet<String>();
				for (Integer parentID : parentSlots) {
					CxnalSpan parentSpan = spans.get(parentID);
					if (parentSpan != null) { // it could be null if the analysis is a catastrophically bad one.
						Binding binding = goldStandardAnnotation
								.getBindingBySpan(parentSpan.getLeft(), parentSpan.getRight());
						if (binding != null) {
							contextElementNames.addAll(getFillers(binding));
						}
					}
				}
			}

		}
		else if (type == FittableType.COMPLEX_PROCESS || type == FittableType.STRUCTURED_SIMPLE_PROCESS
				|| type == FittableType.UNSTRUCTURED_SIMPLE_PROCESS) {
			// In the gold standard case, it really doesn't matter what the events get fitted to as long as we get the
			// arguments correct.
			contextElementNames = queryContext(typeConstraint, true); // HACK for now
		}
		else {
			throw new LearnerException("Why are we looking for context elements for the slot " + slot.toString()
					+ " whose FittableType is " + type.toString());
		}
		return new Pair<Set<String>, Boolean>(contextElementNames, partialFit);
	}

	@Override
	protected Pair<Set<String>, Boolean> findContextElements(Slot parentSlot, String contextElementName, Role role,
			Integer roleSlotID) {
		Set<String> contextElementNames = new HashSet<String>();
		boolean partialFit = false;
		FittableType parentType = fittable.get(parentSlot.getID());
		TypeConstraint parentMType = parentSlot.getTypeConstraint();

		if (parentType == FittableType.DS && contextElementName != null) {
			contextElementNames = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(role.getName(),
					contextModel.getIndividualName(contextElementName), "?f"), true));
		}
		else if (parentType == FittableType.COMPLEX_PROCESS || parentType == FittableType.STRUCTURED_SIMPLE_PROCESS
				|| parentType == FittableType.UNSTRUCTURED_SIMPLE_PROCESS) {
			// clamp these to the annotation values regardless of what context element was chosen for the process
			List<ExtendedFeatureBasedEntity> argStructs = goldStandardAnnotation.getArgumentStructureAnnotations();
			for (ExtendedFeatureBasedEntity argStruct : argStructs) {
				contextElementNames.addAll(getFillers(argStruct, parentMType, role));
			}
		}
		else {
			if (LearnerUtilities.isSPG(currentGrammar, parentMType)) {
				// get the image schema gold standard annotation
				Collection<ExtendedFeatureBasedEntity> annotations = goldStandardAnnotation
						.getAnnotationsOfType(GSPrimitive.IMG);
				if (annotations != null) {
					for (ExtendedFeatureBasedEntity annotation : annotations) {
						contextElementNames.addAll(getFillers(annotation, parentMType, role));
					}
				}
			}
			else if (contextElementName != null) {
				contextElementNames = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(role.getName(),
						contextModel.getIndividualName(contextElementName), "?f"), true));
			}
		}
		return new Pair<Set<String>, Boolean>(contextElementNames, partialFit);
	}

	private Set<String> getFillers(ExtendedFeatureBasedEntity annotation, TypeConstraint parentMType, Role role) {
		Set<String> contextElementNames = new HashSet<String>();

		if (annotation.getCategory() == null || annotation.getCategory().equalsIgnoreCase(ChildesLocalizer.NONE)) {
			return contextElementNames;
		}

		Set<String> cats = goldStandardLocalizer.getGoldStandardLocalization(annotation.getCategory());
		for (String cat : cats) {
			try {
				if (schemaTypeSystem.subtype(parentMType.getType(), schemaTypeSystem.getInternedString(cat))) {
					Set<Binding> bindings = annotation.getAllBindings();
					for (Binding binding : bindings) {
						String localRoleName = goldStandardLocalizer.getGoldStandardRoleNameLocalization(cat,
								binding.getField());
						List<String> coindexedRoles = AnnotationUtilities.getCoindexedRoleNames(currentGrammar,
								grammarTables.getSchemaCloneTable(), (Type) currentGrammar.getOntologyTypeSystem().get(cat),
								localRoleName);
						if (coindexedRoles.contains(role.getName())) {
							contextElementNames.addAll(getFillers(binding));
						}
					}
				}
			}
			catch (TypeSystemException tse) {
				logger.warning("Unexpected type system error while trying to find context elements for slot "
						+ parentMType.getType() + "." + role.getName() + ". Message given: " + tse.getLocalizedMessage());
			}
		}
		return contextElementNames;
	}

	private List<String> getFillers(Binding binding) {

		List<String> contextElements = new ArrayList<String>();

		if (binding.getAttributeValue(ChildesConstants.REFERENCE) != null) {
			String ref = binding.getAttributeValue(ChildesConstants.REFERENCE);
			if (AnnotationUtilities.isFunction(ref)) {
				contextElements = AnnotationUtilities.resolveFunction(contextModel, ref);

			}
			else if (ref.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.DNI)
					|| ref.equals(AnnotationUtilities.GoldStandardAnnotationLocalizer.INI)) {
				// annotation uses DNI when a specific entity is referred to but its identity is not established by previous
				// transcript annotation
				// in either case, resort to scoring resolution and context fitting based on just the type

			}
			else {
				String individual = contextModel.retrieveIndividual(new SimpleQuery(ref, null), true);
				if (individual == null) {
					// this could be an annotation-internal reference
				}
				else {
					contextElements.add(individual);
				}
			}
		}
		return contextElements;
	}

}

// =============================================================================
//File        : LearnerUtilities.java
//Author      : emok
//Change Log  : Created on Mar 7, 2008
//=============================================================================

package compling.learner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.context.MiniOntology;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.candidates.CompositionCandidate;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

/****
 * This class makes certain assumptions about the ECG syntax, e.g. constraints on constructional constituents are always
 * binary
 */

public class LearnerUtilities {

	private static final int BEFORE = 1;
	private static final int MEETS = 2;

	static Logger logger = Logger.getLogger(LearnerUtilities.class.getName());

	static interface ConstituentMappingFunction {
		public boolean accepts(Role a, Role b);
	}

	public static class EqualsMappingFunction implements ConstituentMappingFunction {
		public boolean accepts(Role a, Role b) {
			if (a.getTypeConstraint().getType().equals(b.getTypeConstraint().getType())) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	public static class SubtypeMappingFunction implements ConstituentMappingFunction {

		// accepts if a subsumes b (is more relaxed)
		public boolean accepts(Role a, Role b) {
			TypeSystem<?> ts = a.getTypeConstraint().getTypeSystem(); // grammar.getCxnTypeSystem();
			String aType = ts.getInternedString(a.getTypeConstraint().getType());
			String bType = ts.getInternedString(b.getTypeConstraint().getType());

			try {
				if (ts.subtype(bType, aType)) {
					return true;
				}
			}
			catch (TypeSystemException tse) {
				// these should both be constructional types, so a TSE is bad
				throw new LearnerException("TypeSystem Exception encountered while comparing "
						+ a.getTypeConstraint().getType() + " to " + b.getTypeConstraint().getType());
			}
			return false;
		}
	}

	public static class SemanticallyCompatibleMappingFunction implements ConstituentMappingFunction {
		public boolean accepts(Role a, Role b) {
			Construction aCxn = (Construction) a.getTypeConstraint().getTypeSystem().get(a.getTypeConstraint().getType());
			Construction bCxn = (Construction) b.getTypeConstraint().getTypeSystem().get(b.getTypeConstraint().getType());

			TypeSystem<?> aTS = aCxn.getMeaningBlock().getTypeConstraint() == null ? null : aCxn.getMeaningBlock()
					.getTypeConstraint().getTypeSystem();
			TypeSystem<?> bTS = bCxn.getMeaningBlock().getTypeConstraint() == null ? null : bCxn.getMeaningBlock()
					.getTypeConstraint().getTypeSystem();

			if (aTS != bTS) {
				return false;
			}

			if (aCxn.getMeaningBlock().getTypeConstraint() == null || bCxn.getMeaningBlock().getTypeConstraint() == null) {
				return false;
			}

			String aMPole = aCxn.getMeaningBlock().getTypeConstraint().getType();
			String bMPole = bCxn.getMeaningBlock().getTypeConstraint().getType();

			Set<String> types = new HashSet<String>();
			types.add(aMPole);
			types.add(bMPole);

			try {
				List<String> superTypes = aTS.allBestCommonSupertypes(types);

				if (aMPole.equals(ChildesLocalizer.ENTITYTYPE) || aMPole.equals(ChildesLocalizer.PROCESSTYPE)
						|| bMPole.equals(ChildesLocalizer.ENTITYTYPE) || bMPole.equals(ChildesLocalizer.PROCESSTYPE)
						|| containsNonGenericTypes(superTypes)) {
					// as long as the generalization doesn't go all the way up to Entity or Process
					// (except when one of them is Entity or Process), it's okay.
					return true;
				}
			}
			catch (TypeSystemException tse) {
				// because we know the types are defined, a TSE at this point just means that there aren't any common
				// supertypes
				return false;
			}
			return false;
		}
	}

	protected static boolean containsNonGenericTypes(Collection<String> types) {
		for (String type : types) {
			if (!(type.equals(ChildesLocalizer.ENTITYTYPE) || type.equals(ChildesLocalizer.PROCESSTYPE) || type
					.equals(ChildesLocalizer.ELEMENTTYPE))) {
				return true;
			}
		}
		return false;
	}

	public static boolean equals(Construction a, Construction b, LearnerGrammar learnerGrammar) {

		// equals(a, b) is essentially subsumedBy(a, b) && subsumedBy(b, a), but that implementation will be inefficient

		List<Role> aConstituents = new ArrayList<Role>(a.getConstructionalBlock().getElements());
		List<Role> bConstituents = new ArrayList<Role>(b.getConstructionalBlock().getElements());

		if (aConstituents.isEmpty() || bConstituents.isEmpty()) {
			// this is a lexical construction or an abstract one?
			return false; // this is probably not correct, but let's handle this later.
		}
		if (aConstituents.size() != bConstituents.size())
			return false;

		if (a.getMeaningBlock().getTypeConstraint() != b.getMeaningBlock().getTypeConstraint())
			return false;

		List<Map<Role, Role>> mappings = mapConstituents(aConstituents, bConstituents, new EqualsMappingFunction());
		if (mappings.isEmpty())
			return false;

		mappings = retainSyntacticallySubsuming(a, b, mappings);
		if (mappings.isEmpty())
			return false;

		mappings = retainSemanticallySubsuming(a, b, mappings, learnerGrammar);
		if (mappings.isEmpty())
			return false;

		List<Map<Role, Role>> reverseMappings = reverseMappings(mappings);

		reverseMappings = retainSyntacticallySubsuming(b, a, reverseMappings);
		if (reverseMappings.isEmpty())
			return false;

		reverseMappings = retainSemanticallySubsuming(b, a, reverseMappings, learnerGrammar);
		if (reverseMappings.isEmpty())
			return false;

		return true;
	}

	public static List<Map<Role, Role>> reverseMappings(List<Map<Role, Role>> mappings) {
		List<Map<Role, Role>> reverseMappings = new ArrayList<Map<Role, Role>>();
		for (Map<Role, Role> map : mappings) {
			reverseMappings.add(reverseMappings(map));
		}
		return reverseMappings;
	}

	public static Map<Role, Role> reverseMappings(Map<Role, Role> map) {
		Map<Role, Role> reverseMap = new HashMap<Role, Role>();
		for (Role aRole : map.keySet()) {
			reverseMap.put(map.get(aRole), aRole);
		}
		return reverseMap;
	}

	public static boolean subsumes(Construction a, Construction b, LearnerGrammar learnerGrammar) {
		List<Role> aConstituents = new ArrayList<Role>(a.getConstructionalBlock().getElements());
		List<Role> bConstituents = new ArrayList<Role>(b.getConstructionalBlock().getElements());

		if (aConstituents.isEmpty() || bConstituents.isEmpty()) {
			// this is a lexical construction or an abstract one?
			return false; // this is probably not correct, but let's handle this later.
		}
		if (aConstituents.size() > bConstituents.size())
			return false;

		TypeConstraint aMPole = a.getMeaningBlock().getTypeConstraint();
		TypeConstraint bMPole = b.getMeaningBlock().getTypeConstraint();
		if (aMPole == null) {
			// then bMPole can be whatever
		}
		else if (bMPole == null) {
			return false;
		}
		else {
			try {
				if (!aMPole.getTypeSystem().subtype(bMPole.getType(), aMPole.getType()))
					return false;
			}
			catch (TypeSystemException tse) {
				return false;
			}
		}

		List<Map<Role, Role>> mappings = mapConstituents(aConstituents, bConstituents, new SubtypeMappingFunction());
		if (mappings.isEmpty())
			return false;

		mappings = retainSyntacticallySubsuming(a, b, mappings);
		if (mappings.isEmpty())
			return false;

		mappings = retainSemanticallySubsuming(a, b, mappings, learnerGrammar);
		if (mappings.isEmpty())
			return false;

		return true;
	}

	public static List<Map<Role, Role>> constrainedMapConstituents(Collection<Role> aConstituents,
			Collection<Role> bConstituents, Role constrainedAConstituent) {
		List<Role> roles = new ArrayList<Role>();
		roles.add(constrainedAConstituent);
		return constrainedMapConstituents(aConstituents, bConstituents, roles,
				new SemanticallyCompatibleMappingFunction());
	}

	public static List<Map<Role, Role>> constrainedMapConstituents(Collection<Role> aConstituents,
			Collection<Role> bConstituents, List<Role> constrainedAConstituents) {
		return constrainedMapConstituents(aConstituents, bConstituents, constrainedAConstituents,
				new SemanticallyCompatibleMappingFunction());
	}

	public static List<Map<Role, Role>> constrainedMapConstituents(Collection<Role> aConstituents,
			Collection<Role> bConstituents, List<Role> constrainedAConstituents, ConstituentMappingFunction mappingFunction) {

		List<Map<Role, Role>> mappings = new ArrayList<Map<Role, Role>>();

		List<Role> unconstrainedAConstituents = new ArrayList<Role>(aConstituents);

		// initialize the base case
		if (constrainedAConstituents.isEmpty()) {
			List<Role> viable = mapSingle(unconstrainedAConstituents.get(0), bConstituents, mappingFunction);
			for (Role v : viable) {
				Map<Role, Role> newMap = new HashMap<Role, Role>();
				newMap.put(unconstrainedAConstituents.get(0), v);
				mappings.add(newMap);
			}
			unconstrainedAConstituents.remove(0);

		}
		else {
			unconstrainedAConstituents.removeAll(constrainedAConstituents);

			List<Role> viable = mapSingle(constrainedAConstituents.get(0), bConstituents, new EqualsMappingFunction());
			for (Role v : viable) {
				Map<Role, Role> newMap = new HashMap<Role, Role>();
				newMap.put(constrainedAConstituents.get(0), v);
				mappings.add(newMap);
			}
			for (Role aRole : constrainedAConstituents.subList(1, constrainedAConstituents.size())) {
				mappings = mapHelper(aRole, bConstituents, mappings, new EqualsMappingFunction());
			}
		}

		for (Role aRole : unconstrainedAConstituents) {
			mappings = mapHelper(aRole, bConstituents, mappings, mappingFunction);
		}

		return mappings;
	}

	/***
	 * This seems to work when the two sets of constituents are unequal in number, as long as the smaller one comes
	 * first.
	 */

	public static List<Map<Role, Role>> mapConstituents(Collection<Role> aConstituents, Collection<Role> bConstituents,
			ConstituentMappingFunction mappingFunction) {
		return constrainedMapConstituents(aConstituents, bConstituents, new ArrayList<Role>(), mappingFunction);
	}

	protected static List<Map<Role, Role>> mapHelper(Role aRole, Collection<Role> bConstituents,
			List<Map<Role, Role>> mappings, ConstituentMappingFunction mappingFunction) {

		List<Map<Role, Role>> newMappings = new ArrayList<Map<Role, Role>>();

		for (Map<Role, Role> map : mappings) {
			List<Role> candidates = new ArrayList<Role>(bConstituents);
			candidates.removeAll(map.values());
			List<Role> viable = mapSingle(aRole, candidates, mappingFunction);
			for (Role v : viable) {
				Map<Role, Role> newMap = new HashMap<Role, Role>();
				newMap.putAll(map);
				newMap.put(aRole, v);
				newMappings.add(newMap);
			}
		}
		return newMappings;
	}

	private static List<Role> mapSingle(Role aRole, Collection<Role> candidates, ConstituentMappingFunction function) {
		List<Role> viable = new ArrayList<Role>();

		for (Role candidate : candidates) {
			if (function.accepts(aRole, candidate)) {
				viable.add(candidate);
			}
		}
		return viable;
	}

	public static List<Map<Role, Role>> retainSyntacticallySubsuming(Construction a, Construction b,
			List<Map<Role, Role>> aToBMaps) {
		List<Map<Role, Role>> viableMappings = new ArrayList<Map<Role, Role>>();
		List<Boolean> syntacticallySubsuming = syntacticallySubsumes(a, b, aToBMaps);
		for (int i = 0; i < syntacticallySubsuming.size(); i++) {
			if (syntacticallySubsuming.get(i)) {
				viableMappings.add(aToBMaps.get(i));
			}
		}
		return viableMappings;
	}

	/***
	 * A syntactically subsumes B if A has FEWER or more RELAXED constraints than B.
	 */
	public static List<Boolean> syntacticallySubsumes(Construction a, Construction b, List<Map<Role, Role>> aToBMaps) {
		List<Boolean> syntacticallySubsuming = new ArrayList<Boolean>();

		for (Map<Role, Role> mapping : aToBMaps) {
			// each mapping should satisfy both cxnA and cxnB's form constraints
			MapMap<Role, Role, Integer> bConstraintGraph = makeFormConstraintGraph(b);

			boolean satisfied = true;
			Set<Constraint> aFormConstraints = a.getFormBlock().getConstraints();
			// see if all of these are satisfiable given the mapped form constraint graph
			for (Constraint constraint : aFormConstraints) {
				List<SlotChain> arguments = constraint.getArguments();
				String operator = constraint.getOperator();
				if (arguments.size() == 2) {
					Role lhs = mapping.get(arguments.get(0).getChain().get(0));
					Role rhs = mapping.get(arguments.get(1).getChain().get(0));
					if (lhs == null || rhs == null) {
						throw new LearnerException("null countered when matching "
								+ (lhs == null ? arguments.get(0).getChain().get(0).toString() : arguments.get(1).getChain()
										.get(0).toString()));
					}

					if (operator.equals(ECGConstants.BEFORE)) {
						// is rhs reachable in the graph given lhs
						if (!reachable(lhs, rhs, bConstraintGraph))
							satisfied = false;
					}
					else if (operator.equals(ECGConstants.MEETS)) {
						// is rhs directly connected to lhs
						if (bConstraintGraph.get(lhs, rhs) == null || bConstraintGraph.get(lhs, rhs) != MEETS)
							satisfied = false;
					}
				}
				else {
					// ignore them all for now
				}
			}
			if (satisfied) {
				syntacticallySubsuming.add(true);
			}
			else {
				syntacticallySubsuming.add(false);
			}
		}
		return syntacticallySubsuming;
	}

	public static MapMap<Role, Role, Integer> makeFormConstraintGraph(Construction cxn) {
		MapMap<Role, Role, Integer> constraintGraph = new MapMap<Role, Role, Integer>();

		Set<Constraint> formConstraints = cxn.getFormBlock().getConstraints();
		for (Constraint constraint : formConstraints) {
			List<SlotChain> arguments = constraint.getArguments();
			String operator = constraint.getOperator();
			if (arguments.size() == 2) {
				Role lhs = arguments.get(0).getChain().get(0);
				Role rhs = arguments.get(1).getChain().get(0);
				if (operator.equals(ECGConstants.BEFORE)) {
					constraintGraph.put(lhs, rhs, BEFORE);
				}
				else if (operator.equals(ECGConstants.MEETS)) {
					constraintGraph.put(lhs, rhs, MEETS);
				}
			}
			else {
				// ignore them all for now
			}
		}

		return constraintGraph;
	}

	protected static boolean reachable(Role from, Role to, MapMap<Role, Role, Integer> constraintGraph) {
		Stack<Role> toProcess = new Stack<Role>();
		Set<Role> visited = new HashSet<Role>();

		toProcess.push(from);

		while (!toProcess.isEmpty()) {
			Role next = toProcess.pop();
			if (next.equals(to))
				return true;
			visited.add(next);
			if (constraintGraph.get(next) != null) {
				for (Role child : constraintGraph.get(next).keySet()) { // only non-zero entries in graph
					if (!visited.contains(child)) {
						toProcess.push(child);
					}
				}
			}
		}

		return false;
	}

	public static List<Map<Role, Role>> retainSemanticallySubsuming(Construction a, Construction b,
			List<Map<Role, Role>> aToBMaps, LearnerGrammar learnerGrammar) {
		List<Map<Role, Role>> viableMappings = new ArrayList<Map<Role, Role>>();
		List<Pair<Boolean, List<Constraint>>> semanticallySubsuming = semanticallySubsumes(a, b, aToBMaps, learnerGrammar);
		for (int i = 0; i < semanticallySubsuming.size(); i++) {
			if (semanticallySubsuming.get(i).getFirst()) {
				viableMappings.add(aToBMaps.get(i));
			}
		}
		return viableMappings;
	}

	public static List<Pair<Boolean, List<Constraint>>> semanticallySubsumes(Construction a, Construction b,
			List<Map<Role, Role>> aToBMaps, LearnerGrammar learnerGrammar) {

		List<Pair<Boolean, List<Constraint>>> semanticallySubsumed = new ArrayList<Pair<Boolean, List<Constraint>>>();

		for (Map<Role, Role> mapping : aToBMaps) {
			Pair<Boolean, List<Constraint>> subsumed = semanticallySubsumesGivenMapping(a, b, mapping, learnerGrammar);
			semanticallySubsumed.add(subsumed);
		}

		return semanticallySubsumed;
	}

	/***
	 * A semantically subsumes B if A has FEWER or more RELAXED constraints than B.
	 */
	public static Pair<Boolean, List<Constraint>> semanticallySubsumesGivenMapping(Construction a, Construction b,
			Map<Role, Role> aToBMaps, LearnerGrammar learnerGrammar) {

		List<Constraint> failingConstraints = new ArrayList<Constraint>();

		Analysis bAnalysis = learnerGrammar.getGrammarTables().getConstructionCloneTable().getInstance(b).clone();
		for (Constraint aConstraint : a.getMeaningBlock().getConstraints()) {
			Constraint mappedConstraint = mapConstraint(aConstraint, aToBMaps);
			if (!isInAnalysis(mappedConstraint, bAnalysis, learnerGrammar.getGrammar(), false)) {
				failingConstraints.add(aConstraint);
			}
		}

		return new Pair<Boolean, List<Constraint>>(failingConstraints.isEmpty(), failingConstraints);
	}

	public static boolean isInAnalysis(Constraint constraint, Analysis analysis, Grammar grammar,
			boolean requiresExactMatch) {
		FeatureStructureSet absFSS = analysis.getFeatureStructure();

		// remove constraints that have been refactored to the abstract construction
		if (constraint.getOperator().equals(ECGConstants.ASSIGN)) {
			if (absFSS.hasSlot(absFSS.getMainRoot(), constraint.getArguments().get(0))) {
				Slot slot = absFSS.getSlot(absFSS.getMainRoot(), constraint.getArguments().get(0));
				if (constraint.getValue().charAt(0) == ECGConstants.CONSTANTFILLERPREFIX) {
					if (slot.getAtom() != null && slot.getAtom().equals(constraint.getValue())) {
						return true;
					}
				}
				else {
					TypeConstraint filler = slot.getTypeConstraint();
					TypeConstraint suppliedConstraint = constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX ? grammar
							.getOntologyTypeSystem().getCanonicalTypeConstraint(constraint.getValue().substring(1)) : grammar
							.getSchemaTypeSystem().getCanonicalTypeConstraint(constraint.getValue());
					if (filler != null
							&& (filler == suppliedConstraint || (!requiresExactMatch && isTypeSubsumedBy(suppliedConstraint,
									filler)))) {
						return true;
					}
				}
			}
		}
		else if (constraint.getOperator().equals(ECGConstants.IDENTIFY)) {
			if (absFSS.hasSlot(absFSS.getMainRoot(), constraint.getArguments().get(0))
					&& absFSS.hasSlot(absFSS.getMainRoot(), constraint.getArguments().get(1))) {
				// if these slot chains already point to the same slot, they must have been unified by the abstract cxn
				if (absFSS.getSlot(absFSS.getMainRoot(), constraint.getArguments().get(0)) == absFSS.getSlot(
						absFSS.getMainRoot(), constraint.getArguments().get(1))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isTypeSubsumedBy(TypeConstraint thisType, TypeConstraint thatType) {
		if (thisType == null && thatType == null) {
			return true;
		}
		else if (thisType == null && thatType != null) {
			return false;
		}
		else if (thisType != null && thatType == null) {
			return true;
		}
		else if (thisType.typeSystem != thatType.typeSystem) {
			return false;
		}
		else { // now that both slots have compatible type systems
			try {
				if (thisType.typeSystem.subtype(thisType.type, thatType.type)) {
					// this type is more specific
					return true;
				}
				else if (thisType.typeSystem.subtype(thatType.type, thisType.type)) {
					// that type is more specific
					return false;
				}
				else {
					return false;
				}
			}
			catch (TypeSystemException tse) {
				throw new LearnerException("TypeSystemException while checking type constraint subsumption: " + tse);
			}
		}
	}

	public static Constraint mapConstraint(Constraint aConstraint, Map<Role, Role> mapping) {
		List<SlotChain> mappedArguments = new ArrayList<SlotChain>();
		for (SlotChain aSlotChain : aConstraint.getArguments()) {
			List<Role> chain = new ArrayList<Role>(aSlotChain.getChain());
			ListIterator<Role> i = chain.listIterator();
			while (i.hasNext()) {
				Role aRole = i.next();
				if (mapping.get(aRole) != null) {
					chain.set(i.previousIndex(), mapping.get(aRole));
				}
			}
			mappedArguments.add(new SlotChain().setChain(chain));
		}
		if (aConstraint.isAssign()) {
			return new Constraint(aConstraint.getOperator(), mappedArguments.get(0), aConstraint.getValue());
		}
		else {
			return new Constraint(aConstraint.getOperator(), mappedArguments);
		}
	}

	public static TypeConstraint findEventTypeRestriction(Construction construction, GrammarTables tables) {
		assert (construction.getMeaningBlock().getTypeConstraint().getType()
				.equals(ChildesLocalizer.eventDescriptorTypeName));
		ECGSlotChain sc = new ECGSlotChain(ECGConstants.SELF + "." + ECGConstants.MEANING_POLE + "."
				+ ChildesLocalizer.eventTypeRoleName);
		Analysis a = tables.getConstructionCloneTable().getInstance(construction);
		try {
			TypeConstraint constraint = a.getFeatureStructure().getSlot(sc).getTypeConstraint();
			return constraint;
		}
		catch (NullPointerException npe) {
			return null;
		}
	}

	public static boolean isProcess(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (grammar.getSchemaTypeSystem().subtype(
					grammar.getSchemaTypeSystem().getInternedString(typeConstraint.getType()),
					grammar.getSchemaTypeSystem().getInternedString(ChildesLocalizer.PROCESSTYPE))) {
				return true;
			}
		}
		catch (TypeSystemException tse) {
			return false;
		}
		return false;
	}

	public static boolean isDS(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& grammar.getSchemaTypeSystem().subtype(typeConstraint.getType(),
							grammar.getSchemaTypeSystem().getInternedString(ECGConstants.DISCOURSESEGMENTTYPE))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isRD(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& grammar.getSchemaTypeSystem().subtype(typeConstraint.getType(),
							grammar.getSchemaTypeSystem().getInternedString(ECGConstants.RD))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isDSRelatedRD(Grammar grammar, Slot slot, TypeConstraint typeConstraint) {
		if (!isRD(grammar, typeConstraint)) {
			return false;
		}
		Slot discourseParticipantSlot = slot.getSlot(new Role(ECGConstants.discourseParticipantRoleRoleName));
		if (discourseParticipantSlot != null) {
			TypeConstraint discourseParticipant = discourseParticipantSlot.getTypeConstraint();
			TypeConstraint speakerTypeConstraint = grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(
					ECGConstants.speakerTypeName);
			TypeConstraint addresseeTypeConstraint = grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(
					ECGConstants.addresseeTypeName);
			TypeConstraint attentionalFocusTypeConstraint = grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(
					ECGConstants.attentionalFocusTypeName);

			if (discourseParticipant == speakerTypeConstraint || discourseParticipant == addresseeTypeConstraint
					|| discourseParticipant == attentionalFocusTypeConstraint) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEventDescriptor(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& grammar.getSchemaTypeSystem().subtype(typeConstraint.getType(),
							grammar.getSchemaTypeSystem().getInternedString(ChildesLocalizer.eventDescriptorTypeName))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isComplexProcess(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& (grammar.getSchemaTypeSystem().subtype(typeConstraint.getType(), grammar.getSchemaTypeSystem()
							.getInternedString(ChildesLocalizer.COMPLEXPROCESSTYPE)))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isElement(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getOntologyTypeSystem()
					&& grammar.getOntologyTypeSystem().subtype(typeConstraint.getType(),
							grammar.getOntologyTypeSystem().getInternedString(ChildesLocalizer.ELEMENTTYPE))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isNotProcessOrHighlevelSchemas(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getOntologyTypeSystem()
					&& grammar.getOntologyTypeSystem().subtype(typeConstraint.getType(),
							grammar.getOntologyTypeSystem().getInternedString(ChildesLocalizer.ELEMENTTYPE))) {
				return true;
			}
			else if (typeConstraint != null && typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& !isProcess(grammar, typeConstraint) && !isDS(grammar, typeConstraint)
					&& !isRD(grammar, typeConstraint) && !isEventDescriptor(grammar, typeConstraint)) {
				// this has to be here for schemas like SPG
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isAnimate(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getOntologyTypeSystem()
					&& grammar.getOntologyTypeSystem().subtype(typeConstraint.getType(),
							grammar.getOntologyTypeSystem().getInternedString(ChildesLocalizer.ANIMATE))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isSetSize(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getOntologyTypeSystem()
					&& grammar.getOntologyTypeSystem().subtype(typeConstraint.getType(),
							grammar.getOntologyTypeSystem().getInternedString(MiniOntology.SETNAME))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isSPG(Grammar grammar, TypeConstraint typeConstraint) {
		try {
			if (typeConstraint != null
					&& typeConstraint.getTypeSystem() == grammar.getSchemaTypeSystem()
					&& (grammar.getSchemaTypeSystem().subtype(typeConstraint.getType(), grammar.getSchemaTypeSystem()
							.getInternedString(ChildesLocalizer.SPGTYPE)))) {
				return true;
			}
			return false;
		}
		catch (TypeSystemException tse) {
			return false;
		}
	}

	public static boolean isOnlyDiscourse(Set<Pair<Integer, SlotChain>> slotchains) {
		boolean areAllDS = true;
		for (Pair<Integer, SlotChain> chain : slotchains) {
			boolean containsDS = false;
			for (Role role : chain.getSecond().getChain()) {
				if (role.getName().equals(ECGConstants.DS) || role.getName().equals(ECGConstants.speechActRoleName)) {
					containsDS = true;
				}
			}
			areAllDS &= containsDS;
		}
		return areAllDS;
	}

	public static SlotChain findShortestChain(Collection<SlotChain> chains) {
		int length = Integer.MAX_VALUE;
		SlotChain shortest = null;
		for (SlotChain sc : chains) {
			if (sc.getChain().size() < length) {
				length = sc.getChain().size();
				shortest = sc;
			}
		}
		return shortest;
	}

	public static Pair<ECGSlotChain, ECGSlotChain> findShortestChain(Grammar grammar, Collection<SlotChain> chains,
			boolean includeDiscourse) {

		List<Pair<ECGSlotChain, ECGSlotChain>> shortest = new ArrayList<Pair<ECGSlotChain, ECGSlotChain>>();
		// ECGSlotChain shortest = null;
		// ECGSlotChain prefixOfShortest = null;

		int minLength = Integer.MAX_VALUE;
		String ds = "." + ECGConstants.DS + ".";

		for (SlotChain sc : chains) {
			if (includeDiscourse || !sc.toString().contains(ds)) {
				List<Role> prefix = new ArrayList<Role>();
				List<Role> chain = new ArrayList<Role>(sc.getChain());

				if (chain.get(0).getTypeConstraint() != null
						&& chain.get(0).getTypeConstraint().getType().equals(ECGConstants.ROOT)) {
					prefix.add(chain.remove(0));
				}
				if (chain.get(0).getTypeConstraint() != null
						&& chain.get(0).getTypeConstraint().getType().equals(ChildesLocalizer.IMPORTANT_TYPE)
						&& chain.get(1).getTypeConstraint() != null
						&& chain.get(1).getTypeConstraint().getType().equals(ChildesLocalizer.LEFTOVER_MORPHEME)) {
					prefix.add(chain.remove(0));
					prefix.add(chain.remove(0));
				}

				// if (!chain.isEmpty() && chain.size() <= minLength && chain.get(0).getTypeConstraint() != null &&
				// (chain.get(0).getTypeConstraint().getTypeSystem().getName().equals(ECGConstants.CONSTRUCTION) ||
				// (includeDiscourse && chain.get(0).equals(ECGConstants.DSROLE)))) {

				if (!chain.isEmpty() && chain.size() <= minLength && chain.get(0).getTypeConstraint() != null) {
					if (chain.size() == minLength) {
						shortest.add(new Pair<ECGSlotChain, ECGSlotChain>(new ECGSlotChain().setChain(prefix),
								new ECGSlotChain().setChain(chain)));
					}
					else {
						shortest.clear();
						shortest.add(new Pair<ECGSlotChain, ECGSlotChain>(new ECGSlotChain().setChain(prefix),
								new ECGSlotChain().setChain(chain)));
						minLength = chain.size();
					}
				}
			}
		}

		if (shortest.isEmpty()) {
			return new Pair<ECGSlotChain, ECGSlotChain>(null, null);
		}
		else if (shortest.size() == 1) {
			return shortest.get(0);
		}
		else {
			// if there are multiple slot chain of the same (shoretest) length, choose a role that is local to the meaning
			// pole of the construction

			try {
				Map<String, Pair<ECGSlotChain, ECGSlotChain>> pairSource = new HashMap<String, Pair<ECGSlotChain, ECGSlotChain>>();
				for (Pair<ECGSlotChain, ECGSlotChain> pair : shortest) {
					ECGSlotChain slotChain = pair.getSecond();
					String source = slotChain.getChain().get(minLength - 1).getSource();
					if (source == null && slotChain.getChain().get(minLength - 2).getTypeConstraint() != null) {
						Schema parent = grammar.getSchema(slotChain.getChain().get(minLength - 2).getTypeConstraint()
								.getType());
						Role r = parent.getRole(slotChain.getChain().get(minLength - 1).getName());
						source = r.getSource();
					}
					if (source != null) {
						pairSource.put(source, pair);
					}
				}

				if (pairSource.keySet().size() > 1) {
					try {
						String bestSubtype = grammar.getSchemaTypeSystem().bestCommonSubtype(pairSource.keySet(), false);
						if (pairSource.containsKey(bestSubtype)) {
							return pairSource.get(bestSubtype);
						}
					}
					catch (TypeSystemException tse) {
						// no common subtype found. Oh well.
					}
				}
			}
			catch (NullPointerException npe) {
				// heuristics failed. Just give up and return the first one.
			}
			return shortest.get(0);
		}
	}

	public static TreeMap<CxnalSpan, ECGSlotChain> pullOutCxnalSpans(LearnerCentricAnalysis lca, Grammar grammar,
			Set<Integer> usefulSlots, MapSet<Integer, Pair<Integer, SlotChain>> slotChainTable, boolean includeDiscourse) {

		TreeMap<CxnalSpan, ECGSlotChain> treeMap = new TreeMap<CxnalSpan, ECGSlotChain>(
				new CompositionCandidate.CxnalSpanComparator());
		Map<Integer, CxnalSpan> cxnalSpans = lca.getCxnalSpans();

		MapSet<Integer, SlotChain> slotchainByRoot = new MapSet<Integer, SlotChain>();
		for (int slotID : usefulSlots) {
			for (Pair<Integer, SlotChain> chain : slotChainTable.get(slotID)) {
				slotchainByRoot.put(chain.getFirst(), chain.getSecond());
			}
		}

		for (int root : slotchainByRoot.keySet()) {
			// by learning from constructions at the root, the learner should be composing out of the biggest "chunks"
			Pair<ECGSlotChain, ECGSlotChain> chainToUseForCoindexation = LearnerUtilities.findShortestChain(grammar,
					slotchainByRoot.get(root), includeDiscourse);
			if (chainToUseForCoindexation.getSecond() != null) {
				Role constituent = chainToUseForCoindexation.getSecond().getChain().get(0);

				CxnalSpan span = null;
				if (!chainToUseForCoindexation.getFirst().getChain().isEmpty()) {
					List<Role> chain = new ArrayList<Role>(chainToUseForCoindexation.getFirst().getChain());
					chain.remove(0);
					chain.add(new Role(constituent.getName()));
					ECGSlotChain slotChain = new ECGSlotChain().setChain(chain);
					span = cxnalSpans.get(lca.getSlot(root, slotChain).getID());
				}
				else {
					span = cxnalSpans.get(root);
				}

				treeMap.put(span, chainToUseForCoindexation.getSecond());
			}
		}
		return treeMap;
	}
}

// =============================================================================
//File        : CompositionCandidate.java
//Author      : emok
//Change Log  : Created on Dec 28, 2007
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.ECGLearnerEngine;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Pair;

//=============================================================================

public class CompositionCandidate extends ArrayList<CxnalSpan> implements Cloneable {

	private static final long serialVersionUID = 1L;

	public static class CxnalSpanComparator implements Comparator<CxnalSpan> {
		// this should be sufficient since there should be no overlapping constituents
		public int compare(CxnalSpan c1, CxnalSpan c2) {
			return c1.getLeft() < c2.getLeft() ? -1 : c1.getLeft() > c2.getLeft() ? 1 : 0;
		}
	}

	Grammar grammar;
	String cxnName = null;
	Set<String> parents = new HashSet<String>();
	Map<String, TypeConstraint> evokes = new LinkedHashMap<String, TypeConstraint>();
	Set<Role> evokedRoles;
	List<List<Pair<CxnalSpan, ECGSlotChain>>> unificationConstraints = new ArrayList<List<Pair<CxnalSpan, ECGSlotChain>>>();
	List<Pair<Pair<CxnalSpan, ECGSlotChain>, String>> assignmentConstraints = new ArrayList<Pair<Pair<CxnalSpan, ECGSlotChain>, String>>();
	TypeConstraint mBlockType = null;
	Map<CxnalSpan, String> localNames = new HashMap<CxnalSpan, String>();

	int spanLeft = Integer.MAX_VALUE, spanRight = -1;

	static Logger logger = Logger.getLogger(CompositionCandidate.class.getName());

	public CompositionCandidate(Grammar grammar) {
		super();
		setGrammar(grammar);
	}

	public CompositionCandidate(CompositionCandidate that) {
		// this is somewhat shallow copy.
		super(that);
		setGrammar(that.grammar);
		cxnName = generateCxnName();
		parents.addAll(that.parents);
		evokes.putAll(that.evokes);
		unificationConstraints.addAll(that.unificationConstraints);
		assignmentConstraints.addAll(that.assignmentConstraints);
		mBlockType = that.mBlockType;
		localNames.putAll(that.localNames);
	}

	public void setGrammar(Grammar grammar) {
		this.grammar = grammar;
	}

	public boolean add(CxnalSpan constituent) {
		boolean val = super.add(constituent);
		Collections.sort(this, new CxnalSpanComparator());
		if (constituent.getLeft() < spanLeft) {
			spanLeft = constituent.getLeft();
		}
		if (constituent.getRight() > spanRight) {
			spanRight = constituent.getRight();
		}
		return val;
	}

	public void setUnificationConstraints(List<List<Pair<CxnalSpan, ECGSlotChain>>> feature) {
		unificationConstraints = feature;
	}

	public int getSpanLeft() {
		return spanLeft;
	}

	public int getSpanRight() {
		return spanRight;
	}

	public List<List<Pair<CxnalSpan, ECGSlotChain>>> getUnificationConstraints() {
		return unificationConstraints;
	}

	public void addUnificationConstraint(List<Pair<CxnalSpan, ECGSlotChain>> constraint) {
		unificationConstraints.add(constraint);
	}

	public List<Pair<Pair<CxnalSpan, ECGSlotChain>, String>> getAssignmentConstraint() {
		return assignmentConstraints;
	}

	public void addAssignmentConstraint(Pair<Pair<CxnalSpan, ECGSlotChain>, String> constraint) {
		assignmentConstraints.add(constraint);
	}

	public boolean addEvokes(Role r) {
		if (evokedRoles == null) {
			evokedRoles = new LinkedHashSet<Role>();
		}
		return evokedRoles.add(r);
	}

	public boolean addEvokes(String localname, TypeConstraint evokedType) {
		if (evokes.containsKey(localname)) {
			return false;
		}
		evokes.put(localname, evokedType);
		return true;
	}

	public void setParent(Set<String> parentNames) {
		parents.addAll(parentNames);
	}

	public void addParent(String parentName) {
		parents.add(parentName);
	}

	protected void setMBlockType(TypeConstraint mBlockType) {
		this.mBlockType = mBlockType;
	}

	public void incorporate(CompositionCandidate that) {
		List<CxnalSpan> additional = new ArrayList<CxnalSpan>(that);
		additional.removeAll(this);
		this.addAll(additional);
		unificationConstraints.addAll(that.unificationConstraints);
		assignmentConstraints.addAll(that.assignmentConstraints);
		Collections.sort(this, new CxnalSpanComparator());
		if (that.spanLeft < spanLeft) {
			spanLeft = that.spanLeft;
		}
		if (that.spanRight > spanRight) {
			spanRight = that.spanRight;
		}
	}

	public Construction createNewConstruction() {

		if (cxnName == null) {
			cxnName = generateCxnName();
		}

		Block constructionalBlock = grammar.new Block(ECGConstants.CONSTRUCTIONAL, ECGConstants.UNTYPED);
		constructionalBlock.setTypeSource(cxnName);
		constructionalBlock.setElements(generateConstituents(cxnName));

		Block formBlock = grammar.new Block(ECGConstants.FORM, ECGConstants.UNTYPED);
		formBlock.setTypeSource(cxnName);
		formBlock.setConstraints(generateFormConstraints(cxnName));

		if (mBlockType != null)
			mBlockType = getCurrentTypeConstraint(mBlockType);
		Block meaningBlock = grammar.new Block(ECGConstants.MEANING, mBlockType == null ? ECGConstants.UNTYPED
				: mBlockType.getType().toString());
		meaningBlock.setTypeSource(cxnName);
		if (mBlockType != null) {
			meaningBlock.setBlockTypeTypeSystem(mBlockType.getTypeSystem());
		}
		if (evokedRoles == null) {
			evokedRoles = generateEvokedRoles(cxnName);
		}
		meaningBlock.setEvokedElements(evokedRoles);
		meaningBlock.setConstraints(generateMeaningConstraints(cxnName));

		Construction newConstruction = grammar.new Construction(cxnName, ECGConstants.CONCRETE, parents, formBlock,
				meaningBlock, constructionalBlock);

		newConstruction.setComplements(new HashSet<Role>());
		newConstruction.setOptionals(new HashSet<Role>());

		return newConstruction;
	}

	private Set<Constraint> generateFormConstraints(String cxnName) {

		Set<Constraint> formConstraints = new LinkedHashSet<Constraint>();

		for (int i = 0; i < this.size() - 1; i++) {
			CxnalSpan a = get(i);
			CxnalSpan b = get(i + 1);
			int diff = b.getLeft() - a.getRight();
			if (diff == 0) {
				Constraint c = new Constraint(ECGConstants.MEETS, new SlotChain(localNames.get(a) + "."
						+ ECGConstants.FORM_POLE), new SlotChain(localNames.get(b) + "." + ECGConstants.FORM_POLE));
				c.setSource(cxnName);
				formConstraints.add(c);
			}
			else if (diff > 0) {
				Constraint c = new Constraint(ECGConstants.BEFORE, new ECGSlotChain(localNames.get(a) + "."
						+ ECGConstants.FORM_POLE), new ECGSlotChain(localNames.get(b) + "." + ECGConstants.FORM_POLE));
				c.setSource(cxnName);
				formConstraints.add(c);
			}
		}

//      ListIterator<CxnalSpan> iter1 = this.listIterator();
//      while (iter1.hasNext()) {
//         CxnalSpan a = iter1.next();
//         ListIterator<CxnalSpan> iter2 = this.listIterator(iter1.nextIndex());
//         while (iter2.hasNext()) {
//            CxnalSpan b = iter2.next();
//            int diff = b.getLeft() - a.getRight();
//
//            if (diff == 0) {
//               Constraint c = new Constraint(ECGConstants.MEETS,
//                  new SlotChain(localNames.get(a) + "." + ECGConstants.FORM_POLE),
//                  new SlotChain(localNames.get(b) + "." + ECGConstants.FORM_POLE));
//               c.setSource(cxnName);
//               formConstraints.add(c);
//            } else if (diff > 0) {
//               Constraint c = new Constraint(ECGConstants.BEFORE,
//                  new ECGSlotChain(localNames.get(a) + "." + ECGConstants.FORM_POLE),
//                  new ECGSlotChain(localNames.get(b) + "." + ECGConstants.FORM_POLE));
//               c.setSource(cxnName);
//               formConstraints.add(c);
//            }
//         }
//      }
		return formConstraints;
	}

	/**
	 * requires that the CxnalSpans be sorted in form order
	 * 
	 * @return
	 */
	private Set<Role> generateConstituents(String cxnName) {

		Set<Role> constituents = new LinkedHashSet<Role>();
		for (CxnalSpan construct : this) {
			Role r = new Role(generateLocalName(construct));
			String typeConstraint = construct.getType().getType();
			for (String constructParent : construct.getType().getParents()) {
				if (constructParent.contains(ChildesLocalizer.VARIANT_SUFFIX)) {
					// this construct is a pronounciation variant. Use the abstract type.
					typeConstraint = constructParent;
					break;
				}
			}
			r.setTypeConstraint(grammar.getCxnTypeSystem().getCanonicalTypeConstraint(typeConstraint));
			r.setSource(cxnName);
			constituents.add(r);
			localNames.put(construct, r.getName());
		}
		return constituents;
	}

	private String generateLocalName(CxnalSpan construct) {
		return String.valueOf(construct.getType().getName().charAt(0)) + indexOf(construct);
	}

	private Set<Role> generateEvokedRoles(String cxnName) {
		Set<Role> evokedMeaningBlockElements = new LinkedHashSet<Role>();
		for (String roleName : evokes.keySet()) {
			Role r = new Role(roleName);
			TypeConstraint typeConstraint = evokes.get(roleName).getTypeSystem().getName().equals(ECGConstants.SCHEMA) ? grammar
					.getSchemaTypeSystem().getCanonicalTypeConstraint(evokes.get(roleName).getType()) : grammar
					.getOntologyTypeSystem().getCanonicalTypeConstraint(evokes.get(roleName).getType());
			r.setTypeConstraint(typeConstraint);
			r.setSource(cxnName);
			evokedMeaningBlockElements.add(r);
		}
		return evokedMeaningBlockElements;
	}

	private ECGSlotChain expressInCurrentGrammarTerms(ECGSlotChain slotChain) {
		List<Role> newRoles = new ArrayList<Role>();
		for (Role r : slotChain.getChain()) {
			newRoles.add(getCurrentRole(r));
		}
		// have to clone because of the startsWithSelf thing
		ECGSlotChain newSlotChain = slotChain.clone().setChain(newRoles);
		return newSlotChain;
	}

	private Role getCurrentRole(Role oldRole) {
		TypeConstraint type = oldRole.getTypeConstraint();
		String roleName = oldRole.getName();
		if (type == null)
			return oldRole;
		Role newRole = new Role(roleName);
		newRole.setTypeConstraint(getCurrentTypeConstraint(type));
		return newRole;
	}

	private TypeConstraint getCurrentTypeConstraint(TypeConstraint type) {
		if (type.getTypeSystem().getName().equals(ECGConstants.CONSTRUCTION)) {
			return grammar.getCxnTypeSystem().getCanonicalTypeConstraint(type.getType());
		}
		else if (type.getTypeSystem().getName().equals(ECGConstants.SCHEMA)) {
			return grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(type.getType());
		}
		else if (type.getTypeSystem().getName().equals(ECGConstants.ONTOLOGY)) {
			return grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(type.getType());
		}
		else {
			logger.warning("Unknown type system encountered " + type);
			return null;
		}
	}

	private Set<Constraint> generateMeaningConstraints(String cxnName) {

		Set<Constraint> meaningConstraints = new LinkedHashSet<Constraint>();

		for (List<Pair<CxnalSpan, ECGSlotChain>> constraint : unificationConstraints) {
			List<ECGSlotChain> coindexed = new ArrayList<ECGSlotChain>();

			for (Pair<CxnalSpan, ECGSlotChain> chain : constraint) {
				List<Role> roles = chain.getSecond().getChain();
				ECGSlotChain slotChain;

				// FIXME: the problem is that these slot chains are expressed in terms of the old type system (used in
				// parsing)

				if (chain.getFirst() != null) {
					Role constituent = new Role(localNames.get(chain.getFirst()));
					constituent.setTypeConstraint(grammar.getCxnTypeSystem().getCanonicalTypeConstraint(
							chain.getFirst().getType().getType()));
					roles.set(0, constituent);
					slotChain = new ECGSlotChain().setChain(roles);
				}
				else {
					slotChain = chain.getSecond();
				}
				coindexed.add(expressInCurrentGrammarTerms(slotChain));
			}

			ListIterator<ECGSlotChain> iter1 = coindexed.listIterator();
			while (iter1.hasNext()) {
				ECGSlotChain a = iter1.next();
				ListIterator<ECGSlotChain> iter2 = coindexed.listIterator(iter1.nextIndex());
				while (iter2.hasNext()) {
					ECGSlotChain b = iter2.next();

					Constraint c = new Constraint(ECGConstants.IDENTIFY, expressInCurrentGrammarTerms(a),
							expressInCurrentGrammarTerms(b));
					c.setSource(cxnName);
					meaningConstraints.add(c);
				}
			}
		}

		for (Pair<Pair<CxnalSpan, ECGSlotChain>, String> constraint : assignmentConstraints) {
			ECGSlotChain slotChain;
			Pair<CxnalSpan, ECGSlotChain> chain = constraint.getFirst();
			List<Role> roles = chain.getSecond().getChain();
			if (chain.getFirst() != null) {
				Role constituent = new Role(localNames.get(chain.getFirst()));
				constituent.setTypeConstraint(grammar.getCxnTypeSystem().getCanonicalTypeConstraint(
						chain.getFirst().getType().getType()));
				roles.set(0, constituent);
				slotChain = new ECGSlotChain().setChain(roles);
			}
			else {
				slotChain = chain.getSecond();
			}

			Constraint c = new Constraint(ECGConstants.ASSIGN, expressInCurrentGrammarTerms(slotChain),
					constraint.getSecond());
			c.setSource(cxnName);
			meaningConstraints.add(c);
		}

		return meaningConstraints;
	}

	private String generateCxnName() {
		StringBuilder sb = new StringBuilder();
		for (CxnalSpan span : this) {
			String cst = span.getType().getName();
			cst = cst.replaceAll("-\\w\\d*\\b", "");
			sb.append(cst).append("_");
		}
		return String.format("%s%s%3$03d", sb.substring(0, sb.length() - 1).toString(), ECGLearnerEngine.LEX_CXN_SUFFIX,
				ECGLearnerEngine.getCounter());
	}

	public String getCxnName() {
		if (cxnName == null) {
			cxnName = generateCxnName();
		}
		return cxnName;
	}

	public void setCxnName(String cxnName) {
		this.cxnName = cxnName;

	}

	public List<Construction> getConstituentTypes() {
		List<Construction> types = new ArrayList<Construction>();
		for (CxnalSpan span : this) {
			types.add(span.getType());
		}
		return types;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("constituent types:\n");
		for (CxnalSpan span : this) {
			sb.append("\t").append(span.getType().getName());
			sb.append(" (").append(span.getLeft()).append(", ").append(span.getRight()).append(")\n");
		}

		sb.append("meaning pole: ").append(mBlockType).append("\n\n");

		if (!evokes.isEmpty()) {
			sb.append("evoked elements:\n");
			for (String localName : evokes.keySet()) {
				sb.append(localName).append(": ").append(evokes.get(localName)).append("\n");
			}
			sb.append("\n");
		}

		sb.append("unification constraints: \n");
		for (List<Pair<CxnalSpan, ECGSlotChain>> constraint : unificationConstraints) {
			sb.append("\t");
			for (Pair<CxnalSpan, ECGSlotChain> sc : constraint) {
				if (sc.getFirst() == null) {
					sb.append(sc.getSecond().toString());
				}
				else {
					sb.append(sc.getFirst().getType().getName()).append(".").append(sc.getSecond().subChain(1));
				}
				sb.append(" <--> ");
			}
			sb.delete(sb.lastIndexOf(" <--> "), sb.length());
			sb.append("\n");
		}

		for (Pair<Pair<CxnalSpan, ECGSlotChain>, String> constraint : assignmentConstraints) {
			sb.append("\t");
			if (constraint.getFirst().getFirst() == null) {
				sb.append(constraint.getFirst().getSecond().toString());
			}
			else {
				sb.append(constraint.getFirst().getFirst().getType().getName()).append(".")
						.append(constraint.getFirst().getSecond().subChain(1));
			}
			sb.append(" <-- ").append(constraint.getSecond()).append("\n");
		}

		return sb.toString();
	}

}
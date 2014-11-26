package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Primitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.parser.ParserException;
import compling.util.StringUtilities;

/**
 * 
 * @author Nathan Schneider
 * @see compling.parser.ecgparser.LCPGrammarWrapper
 * 
 */

public class MGrammarWrapper implements GrammarWrapper {

	Grammar grammar;
	public StringBuffer errorLog;
	private HashMap<String, List<String>> subtypeList = new HashMap<String, List<String>>();
	// private HashMap<String, List<Construction>> lexemeToLexicalConstructions = new HashMap<String,
	// List<Construction>>();

	private Set<Construction> lexCxns = new HashSet<Construction>(); // All lexical constructions

	private HashMap<String, LexicalConstructions> cxnHash = new HashMap<String, LexicalConstructions>(); // Maps from
																																			// strings to
																																			// compatible
																																			// lexical
																																			// analyses

	private Map<String, StaticMorphAnalysis> morphCxns = new HashMap<String, StaticMorphAnalysis>(); // Maps head cxn
																																		// types to
																																		// MorphAnalysis
																																		// objects

	private Set<Construction> noncontigs = new HashSet<Construction>(); // All concrete noncontiguous morphological
																								// constructions
	
	public Grammar getGrammar() {
		return grammar;
	}
	
	
	
	

	public MGrammarWrapper(Grammar ecgGrammar, StringBuffer errLog) {
		grammar = ecgGrammar;
		errorLog = errLog;
		try {
			Set<String> morphs1 = new HashSet<String>();
			morphs1.addAll(grammar.getCxnTypeSystem().getAllSubtypes(ECGConstants.MCXNTYPE));
			morphs1.addAll(grammar.getCxnTypeSystem().getAllSubtypes(ECGConstants.MROOTTYPE));
			for (String pname : morphs1) {
				Construction parent = grammar.getConstruction(pname);
				subtypeList.put(parent.getName(), new ArrayList<String>());
				for (Construction child : grammar.getAllConstructions()) {
					try {
						if (grammar.getCxnTypeSystem().subtype(child.getName(), parent.getName()) && isConcrete(child)) {
							subtypeList.get(parent.getName()).add(child.getName());
						}
					}
					catch (Exception e) {
						throw new GrammarException(e.toString());
					}
				}
				if (isConcrete(parent)) {
					if (isLexical(parent)) {
						addLexicalConstruction(parent,
								StringUtilities.removeQuotes(ECGGrammarUtilities.getLexemeFromLexicalConstruction(parent)));
					}
					else if (!addMorphConstruction(parent)) {
						System.err.println(errorLog.toString());
						System.err.println("WARNING: Unable to add morphological construction " + parent.getName());
					}
				}
			}
		}
		catch (TypeSystemException ex) {
			ex.printStackTrace(); // TODO
		}

		ParserException wlException = new ParserException("Must define morphological word construction "
				+ ECGConstants.MWORDTYPE + " as a subtype of " + ECGConstants.MCXNTYPE);
		try {
			// TypeSystem.subtype() does object-equality tests on construction names.
			String wlType = grammar.getConstruction(ECGConstants.MWORDTYPE).getName();
			String mType = grammar.getConstruction(ECGConstants.MCXNTYPE).getName();
			if (!subtypeList.containsKey(ECGConstants.MWORDTYPE) || !grammar.getCxnTypeSystem().subtype(wlType, mType)) {
				throw wlException;
			}
		}
		catch (Exception ex) {
			throw wlException;
		}
	}

	private boolean addMorphConstruction(Construction cxn) {
		boolean fatalError = false;
		List<ECGSlotChain> meetsChain = new ArrayList<ECGSlotChain>();
		Set<Constraint> constraints = MGrammarWrapper.getAllFormConstraints(cxn);

		Set<Constraint> beforeConstraints = new HashSet<Constraint>();
		HashMap<ECGSlotChain, Constraint> meetsConstraintByLeftOperand = new HashMap<ECGSlotChain, Constraint>();
		HashMap<ECGSlotChain, Constraint> meetsConstraintByRightOperand = new HashMap<ECGSlotChain, Constraint>();

		for (Constraint constraint : constraints) {
			if (constraint.overridden())
				continue;

			ECGSlotChain left = (ECGSlotChain) constraint.getArguments().get(0);
			ECGSlotChain right = (constraint.getArguments().size() > 1) ? (ECGSlotChain) constraint.getArguments().get(1)
					: null;

			if (constraint.getOperator() == ECGConstants.MEETS || constraint.getOperator() == ECGConstants.BEFORE) {
				if (MGrammarWrapper.isDeep(left) || MGrammarWrapper.isDeep(right)) {
					errorLog
							.append("Invalid form constraint in cxn "
									+ cxn.getName()
									+ ": "
									+ constraint.toString()
									+ " (contains a deep constituent reference, i.e. refers to a constituent of a constituent, which is illegal)\n");
					fatalError = true;
				}

				if (constraint.getOperator() == ECGConstants.MEETS) {
					if (meetsConstraintByLeftOperand.containsKey(left) || meetsConstraintByRightOperand.containsKey(right)) {
						errorLog.append("Superfluous constraint in cxn " + cxn.getName() + ": " + constraint.toString());
						fatalError = true;
					}

					else {
						meetsConstraintByLeftOperand.put(left, constraint);
						meetsConstraintByRightOperand.put(right, constraint);
					}
				}
				else if (constraint.getOperator() == ECGConstants.BEFORE) {
					beforeConstraints.add(constraint);
				}

			}
			else if (constraint.getOperator() == ECGConstants.IDENTIFY) {
				if (left.getChain().get(left.getChain().size() - 1).getName().equals("f")
						|| right.getChain().get(right.getChain().size() - 1).getName().equals("f")) {
					errorLog.append("WARNING: Form pole reference in binding constraint in cxn " + cxn.getName() + ": "
							+ constraint.toString());
				}
			}
		}

		Constraint constraint = null;
		if (meetsConstraintByLeftOperand.size() > 0) // Pick an arbitrary starting constraint
			constraint = meetsConstraintByLeftOperand.values().iterator().next();
		// System.out.println(constraint.getArguments().get(0).getChain().toString());

		// Work rightward in the chain from the starting constraint
		while (constraint != null) {

			ECGSlotChain leftOperand = (ECGSlotChain) constraint.getArguments().get(0);
			ECGSlotChain rightOperand = (ECGSlotChain) constraint.getArguments().get(1);

			if (!meetsChain.isEmpty() && meetsChain.get(0).equals(leftOperand)) {
				errorLog
						.append("Cycle of 'meets' constraints involving " + leftOperand + " in cxn " + cxn.getName() + "\n");
				fatalError = true;
			}
			meetsChain.add(leftOperand);

			if (meetsConstraintByLeftOperand.containsKey(rightOperand)) {
				constraint = meetsConstraintByLeftOperand.get(rightOperand);
				continue;
			}

			// RHS of last constraint
			meetsChain.add(rightOperand);
			break;
		}

		if (meetsChain.size() > 0) { // Work leftward in the chain from the starting constraint
			ECGSlotChain r0 = meetsChain.get(0);
			constraint = null;
			if (meetsConstraintByRightOperand.containsKey(r0))
				constraint = meetsConstraintByRightOperand.get(r0);
		}

		while (constraint != null) {

			ECGSlotChain leftOperand = (ECGSlotChain) constraint.getArguments().get(0);

			meetsChain.add(0, leftOperand);

			if (meetsConstraintByRightOperand.containsKey(leftOperand)) {
				constraint = meetsConstraintByRightOperand.get(leftOperand);
				continue;
			}

			break;

		}

		// Check that all 'meets' constraints are in the chain
		if (meetsChain.size() > 0 && meetsChain.size() != meetsConstraintByLeftOperand.size() + 1) {
			errorLog.append("WARNING: Disjoint 'meets' constraint(s) in cxn " + cxn.getName() + "\n");
			errorLog.append(meetsChain + "\n");
			meetsChain.clear(); // Not a complete 'meets' chain
		}

		// if (meetsChain.size()==0)
		// isContiguous = false;

		// Verify that all 'before' constraints are obeyed by the 'meets' chain
		for (Constraint bConstraint : beforeConstraints) {
			ECGSlotChain leftOperand = (ECGSlotChain) bConstraint.getArguments().get(0);
			ECGSlotChain rightOperand = (ECGSlotChain) bConstraint.getArguments().get(1);

			if (leftOperand.equals(rightOperand)) {
				errorLog.append("WARNING: Recursive 'before' constraint in cxn " + cxn.getName() + ": " + bConstraint
						+ "\n");
			}

			int iLHS = meetsChain.indexOf(leftOperand);
			int iRHS = meetsChain.indexOf(rightOperand);
			if (iLHS == -1) {
				// Left-hand side of 'before' constraint never realized in a 'meets' constraint
				// isContiguous = false;
				meetsChain.clear();
			}
			if (iRHS == -1) {
				// Right-hand side of 'before' constraint never realized in a 'meets' constraint
				// isContiguous = false;
				meetsChain.clear();
			}
			if (iLHS > -1 && iRHS > -1 && iLHS > iRHS) {
				errorLog.append("WARNING: 'before' constraint violated by 'meets' constraints in cxn " + cxn.getName()
						+ ": " + bConstraint + "\n");
			}

			// TODO: Verify there are no cycles of 'before' constraints
		}

		// / DISPLAY MEETS CHAIN (WILL BE EMPTY IF NONCONTIGUOUS)
		/*
		 * System.out.println("meets chain..."); for (String r : meetsChain) { System.out.println(r); }
		 * System.out.println(".");
		 */

		// Store a canonical (just-initialized) MorphAnalysis object for a morphological cxn
		// Copy this object when building up a candidate analysis
		if (!fatalError)
			addMorphConstruction(cxn, meetsChain);

		return (!fatalError);
	}

	/**
	 * Store a canonical (just-initialized) MorphAnalysis object for a morphological cxn. Copy this object when building
	 * up a candidate analysis.
	 * 
	 * @param cxn
	 * @param meetsChain
	 */
	private void addMorphConstruction(Construction cxn, List<ECGSlotChain> meetsChain) {
		this.morphCxns.put(cxn.getName(), new StaticMorphAnalysis(new MAnalysis(cxn, this), meetsChain));
		if (!this.isContiguous(cxn))
			noncontigs.add(cxn);
	}

	private void addLexicalConstruction(Construction c, String orthValue) {
		// TODO: Decide whether there might be a morphologically complex analysis with the same form

		if (this.cxnHash.containsKey(orthValue))
			this.cxnHash.get(orthValue).addConstruction(c);
		else {
			this.cxnHash.put(orthValue, new LexicalConstructions(c, true)); // Assume possibly complex
		}

		lexCxns.add(c);
	}

	public Set<String> getMorphConstructionNames() {
		return this.morphCxns.keySet();
	}

	/** Returns the concrete rules that are subtypes of construction */
	public List<Construction> getRules(String construction) {
		List<Construction> cxns = new ArrayList<Construction>();
		// System.out.println(symbol+" "+getConcreteSubtypes(symbol));
		for (String type : getConcreteSubtypes(construction)) {
			cxns.add(getConstruction(type));
		}
		return cxns;
	}

	public List<String> getConcreteSubtypes(String cxn) {
		if (getConstruction(cxn) == null)
			return null;
		return subtypeList.get(cxn);
	}

	public boolean isLexicalConstruction(Construction c) {
		return c.getKind() == ECGConstants.CONCRETE && lexCxns.contains(c);
	}

	public List<Construction> getLexicalConstruction(String lexeme) {
		LexicalConstructions lex = cxnHash.get(lexeme);
		if (lex == null) {
			// System.out.println(lexemeToLexicalConstruction.keySet());
			throw new GrammarException("undefined lexeme: " + lexeme + " in Grammar.getLexicalConstruction");
		}
		List<Construction> lexes = new ArrayList<Construction>(lex.getConstructions());
		return lexes;
	}

	public Set<Construction> getAllConcreteConstructions() {
		Set<Construction> cxns = new LinkedHashSet<Construction>();
		for (Construction c : grammar.getAllConstructions()) {
			if (c.getKind() == ECGConstants.CONCRETE) {
				cxns.add(c);
			}
		}
		return cxns;
	}

	public Set<Construction> getAllConcretePhrasalConstructions() {
		Set<Construction> cxns = new LinkedHashSet<Construction>();
		for (Construction c : grammar.getAllConstructions()) {
			if (isPhrasalConstruction(c) && c.getKind() == ECGConstants.CONCRETE) {
				cxns.add(c);
			}
		}
		return cxns;
	}

	public Set<Construction> getAllConcreteLexicalConstructions() {
		return lexCxns;
	}

	public Set<Construction> getAllConcreteNonLexicalConstructions() {
		Set<Construction> cxns = new HashSet<Construction>();
		for (Construction c : getAllConstructions()) {
			if (!isLexicalConstruction(c) && c.getKind() == ECGConstants.CONCRETE) {
				cxns.add(c);
			}
		}
		return cxns;
	}

	public Construction getRootConstruction() {
		return grammar.getConstruction(ECGConstants.ROOT);
	}

	public TypeSystem<Construction> getCxnTypeSystem() {
		return grammar.getCxnTypeSystem();
	}

	public TypeSystem<Schema> getSchemaTypeSystem() {
		return grammar.getSchemaTypeSystem();
	}

	public ContextModel getContextModel() {
		return grammar.getContextModel();
	}

	public <T extends Collection<Construction>> T morphFilter(T cxns) {
		for (Iterator<Construction> it = cxns.iterator(); it.hasNext();)
			if (isPhrasalConstruction(it.next()))
				it.remove();
		return cxns;
	}

	public abstract static class Constructions {
		protected Set<Construction> cxns;

		public Constructions() {
			this.cxns = new HashSet<Construction>();
		}

		public void addConstruction(Construction cxn) {
			this.cxns.add(cxn);
		}

		public Set<Construction> getConstructions() {
			return cxns;
		}
	}

	public static class LexicalConstructions extends Constructions {
		private boolean morphAmbiguous; // Might be a morphologically complex analysis with the same form

		public LexicalConstructions(Construction cxn, boolean morphAmbiguous) {
			addConstruction(cxn);
			this.morphAmbiguous = morphAmbiguous;
		}

		public boolean getMorphAmbiguous() {
			return morphAmbiguous;
		}
	}

	public static class StaticMorphAnalysis {
		protected MAnalysis analysis; // Initialized Analysis object for copying
		private boolean contiguous; // Is this a contiguous cxn?
		private List<ECGSlotChain> meetsChain;

		public StaticMorphAnalysis(MAnalysis analysis, List<ECGSlotChain> meetsChain) {
			this.analysis = analysis;
			this.contiguous = true;
			this.meetsChain = meetsChain;
		}

		public StaticMorphAnalysis(MAnalysis analysis) {
			this.analysis = analysis;
			this.contiguous = false;
			this.meetsChain = null;
		}

		public List<ECGSlotChain> getMeetsChain() {
			return meetsChain;
		}

		public MAnalysis getAnalysis() {
			return analysis;
		}

		public boolean isContiguous() {
			return contiguous;
		}
	}

	/**
	 * Is there an lexical (i.e. nondecomposable) analysis for the word via a single construction?
	 */
	public boolean hasLexicalConstruction(String inputWord) {
		return cxnHash.containsKey(inputWord);
	}
	
	// TODO: Not used in anything, but should make sure it works.
	public boolean hasLemmaConstruction(String inputWord) {
		return cxnHash.containsKey(inputWord);
	}

	public LexicalConstructions getLexicalConstructions(String inputWord) {
		return cxnHash.get(inputWord);
	}

	/**
	 * Checks whether a construction is phrasal. A construction is morphological iff it is a descendant of the Morph
	 * construction; it is phrasal otherwise and will be ignored by this class.
	 * 
	 * @param c
	 *           The construction
	 * @return
	 */
	public boolean isPhrasalConstruction(String cxn) {
		return (!subtypeList.keySet().contains(cxn));
	}

	public boolean isPhrasalConstruction(Construction c) {
		return isPhrasalConstruction(c.getName());
	}

	public boolean isLexicalConstruction(String cxnName) {
		return isLexicalConstruction(getConstruction(cxnName));
	}

	/**
	 * Checks whether the given construction is <i>atomic</i>. A construction is defined to be atomic iff it: - has no
	 * constituents (is <i>lexical</i>); - has no 'before' or 'meets' constraints; and - has exactly one form role,
	 * 'orth'.
	 * 
	 * @param c
	 * @return
	 */
	private boolean isLexical(Construction c) {
		if (!assertMorphological(c.getName())) {
			return false;
		}

		if (!isConcrete(c))
			return false;

		if (hasConstituents(c))
			return false; // Has a constituent
		Set<Constraint> allConstrs = getAllFormConstraints(c);
		for (Constraint constr : allConstrs) {
			if (constr.getOperator() == ECGConstants.BEFORE || constr.getOperator() == ECGConstants.MEETS) {
				return false; // Has an ordering constraint
			}
		}

		// Verify that the cxn contains exactly one form role, 'orth'
		Set<Role> allFormRoles = getAllFormRoles(c);
		if (allFormRoles.size() == 1 && allFormRoles.iterator().next().getName().equals("orth"))
			return true;

		return false;
	}

	private boolean isConcrete(Construction c) {
		return (c.getKind() == ECGConstants.CONCRETE);
	}

	private boolean hasConstituents(Construction c) {
		return (c.getConstructionalBlock().getElements().size() > 0);
	}

	public static Set<Role> getAllFormRoles(Construction c) {
		Set<Role> allFormRoles = new HashSet<Role>();
		if (c.getFormBlock().getType() != ECGConstants.UNTYPED)
			allFormRoles.addAll(c.getSchemaTypeSystem().get(c.getFormBlock().getType()).getContents().getElements()); // Form
																																							// roles
																																							// defined
																																							// in
																																							// a
																																							// schema
		allFormRoles.addAll(c.getFormBlock().getElements()); // Locally defined or inherited form roles (if allowed)
		return allFormRoles;
	}

	public static Set<Constraint> getAllFormConstraints(Construction c) {
		Set<Constraint> allConstrs = new HashSet<Constraint>();
		if (c.getFormBlock().getType() != ECGConstants.UNTYPED)
			allConstrs.addAll(c.getSchemaTypeSystem().get(c.getFormBlock().getType()).getContents().getConstraints()); // Form
																																							// constraints
																																							// defined
																																							// in
																																							// a
																																							// schema
		allConstrs.addAll(c.getFormBlock().getConstraints()); // Form constraints defined locally in a cxn or inherited
																				// from an ancestor cxn
		return allConstrs;
	}

	public List<ECGSlotChain> getMeetsChain(String cxnName) {
		if (!morphCxns.containsKey(cxnName) && !assertMorphological(cxnName))
			return null;

		return morphCxns.get(cxnName).getMeetsChain();
	}

	public boolean isContiguous(String cxnName) {
		return isContiguous(getConstruction(cxnName));
	}

	public boolean isContiguous(Construction c) {
		if (isLexicalConstruction(c))
			return true;
		return (morphCxns.containsKey(c.getName()) && getMeetsChain(c.getName()).size() > 0);
		// TODO: Figure out if lexical cxns can be noncontiguous, and remove hashtable check
	}

	public static boolean isDeep(SlotChain sc) {
		List<Role> roles = sc.getChain();
		if (roles.size() < 2)
			return false;
		Role fRole = findRole(roles, "f");
		if (fRole == null) {
			System.err.println(".f role missing from slot chain"); // TODO: What if we have evoked a (form) schema, and
																						// used the local variable for the schema instead of
																						// self.f?
			return true;
		}
		if (roles.get(0) == fRole || roles.get(1) == fRole) // if it is self.f.*, the slot chain object will start with
																				// 'f'
			return false;
		return true;
	}

	// Note: the method below is similar to GrammarChecker.getCorrespondingRole()
	public static Role findRole(Iterable<Role> roles, String roleName) {
		for (Role r : roles) {
			if (r.getName().equals(roleName)) {
				return r;
			}
		}
		return null;
	}

	public static Role findRole(Iterable<Role> roles, Role role) {
		for (Role r : roles) {
			if (r.equals(role)) {
				return r;
			}
		}
		return null;
	}

	public static String getConstitName(ECGSlotChain fc) {
		return getConstitRole(fc).getName();
	}

	public static Role getConstitRole(ECGSlotChain fc) {
		if (fc.startsWithSelf() || fc.getChain().size() == 0)
			return null;
		return fc.getChain().get(0);
	}

	public Set<Construction> getAllConcreteNoncontiguousConstructions() {
		return noncontigs;
	}

	public Collection<Construction> getAllConstructions() {
		return morphFilter(new ArrayList<Construction>(grammar.getAllConstructions()));
	}

	public Collection<Schema> getAllSchemas() {
		return grammar.getAllSchemas();
	}

	public Construction getConstruction(String name) {
		Construction c = grammar.getConstruction(name);
		if (c == null)
			System.err.println("Construction not found: " + name);
		if (!assertMorphological(name))
			return null;
		return c;
	}

	private boolean assertMorphological(String cxn) {
		if (isPhrasalConstruction(cxn) /* && !cxn.equals(MROOTTYPE) */) {
			System.err.println("Attempted to perform a morphological operation on a phrasal construction: " + cxn
					+ ". Is " + cxn + " supposed to be a subtype of " + ECGConstants.MCXNTYPE + "?");
			return false;
		}
		return true;
	}

	/**
	 * @param <T>
	 * @param items
	 * @param includeAllParents
	 * @param typeSystem
	 * @param lookupMap
	 * @param concreteItems
	 * @param color
	 * @return
	 * @see #graphDescendants(Construction[], boolean)
	 * @see #graphDescendants(Schema[], boolean)
	 */
	private <T extends Primitive> String graphDescendants(T[] items, boolean includeAllParents,
			boolean includeRoleEdges, TypeSystem<T> typeSystem, Map<String, T> lookupMap, Collection<T> concreteItems,
			String color) {
		try {
			Set<String> generalItems = new HashSet<String>();

			StringBuffer sb = new StringBuffer();

			for (T item : items) {
				if (item == null) {
					System.err.println("Item is null--skipping");
					continue;
				}

				Set<String> subtypes = typeSystem.getAllSubtypes(item.getName());
				for (String child : subtypes) {
					T ch = lookupMap.get(child);
					if (!concreteItems.contains(ch))
						generalItems.add(child);

					for (String parent : ch.getParents()) {
						if (!includeAllParents && !subtypes.contains(parent))
							continue;
						T pa = lookupMap.get(parent);
						if (!concreteItems.contains(pa))
							generalItems.add(parent);

						// Parent -> Child
						sb.append("\t").append(parent).append(" -> ").append(child).append("\n");
					}

					for (Role r : ch.getAllRoles()) {
						if (r.getTypeConstraint() != null && r.getTypeConstraint().getTypeSystem() == typeSystem) {
							String typeConstraint = r.getTypeConstraint().getType();

							if (includeRoleEdges) {
								// DefiningType -> TypeConstraint
								sb.append("\t").append(child).append(" -> ").append(typeConstraint)
										.append(" [label=\"" + r.getName() + "\", style=\"dotted\"]\n");
							}
						}
					}
				}
			}
			sb.append("}\n");

			StringBuffer sb0 = new StringBuffer();
			sb0.append("digraph types {\n\tsize=\"11,8.5\";\n");
			sb0.append("\tnode [fontname=\"Arial\", fontcolor=black, color=\"" + color + "\", fillcolor=\"" + color
					+ "\"];\n\tnode [style=\"solid\"];");
			for (String cxn : generalItems)
				sb0.append(" " + cxn);
			if (generalItems.size() > 0)
				sb0.append(";");
			sb0.append("\n\tnode [style=\"filled\"];\n\n");

			return sb0.toString() + sb.toString();
		}
		catch (TypeSystemException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Using the DOT language, constructs an inheritance graph with nodes encompassing all subtypes of each item in
	 * <code>cxns</code>, plus optionally all parents of these subtypes. Command to generate a PDF of the graph: dot
	 * -Tpdf -o$PDFFILENAME $DOTFILENAME
	 * 
	 * @param cxns
	 * @param includeAllParents
	 * @return
	 */
	public String graphDescendants(Construction[] cxns, boolean includeAllParents, boolean includeConstituentEdges) {
		return graphDescendants(cxns, includeAllParents, includeConstituentEdges, cxns[0].getCxnTypeSystem(),
				grammar.getAllConstructionsByName(), this.getAllConcreteConstructions(), "#aaff88");
	}

	/**
	 * Using the DOT language, constructs an inheritance graph with nodes encompassing all subtypes of each item in
	 * <code>schemas</code>, plus optionally all parents of these subtypes. Command to generate a PDF of the graph: dot
	 * -Tpdf -o$PDFFILENAME $DOTFILENAME
	 * 
	 * @param cxns
	 * @param includeAllParents
	 * @return
	 */
	public String graphDescendants(Schema[] schemas, boolean includeAllParents, boolean includeRoleEdges) {
		return graphDescendants(schemas, includeAllParents, includeRoleEdges, grammar.getSchemaTypeSystem(),
				grammar.getAllSchemasByName(), grammar.getAllSchemas(), "#ffaaaa");
	}
}

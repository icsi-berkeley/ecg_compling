package compling.grammar.ecg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.MapPrimitive;
import compling.grammar.ecg.Grammar.Primitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Grammar.Situation;
import compling.grammar.ecg.ecgreader.BasicGrammarErrorListener;
import compling.grammar.ecg.ecgreader.IErrorListener;
import compling.grammar.ecg.ecgreader.IErrorListener.Severity;
import compling.grammar.ecg.ecgreader.ILoggingErrorListener;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.Arrays;
import compling.util.Interner;

public class GrammarChecker {

	/**
	 * @return the errorListener
	 */
	public static ILoggingErrorListener getErrorListener() {
		return errorListener;
	}

	public static final BasicGrammarErrorListener DEFAULT_ERROR_LISTENER = new BasicGrammarErrorListener();

	private static ILoggingErrorListener errorListener = DEFAULT_ERROR_LISTENER;

	public static StringBuffer checkGrammar(Grammar g) {
		try {
			// TODO: I think this has to be changed
			checkSituations(g, errorListener);
			checkSchemas(g, errorListener);
			checkMaps(g, errorListener);
			checkConstructions(g, errorListener);
		} catch (TypeSystemException x) {
			for (GrammarError e : x.getErrors())
				errorListener.notify(e.getMessage(), e.getLocation(), e.getSeverity());
		}
		return errorListener.asStringBuffer();
	}

	public static void setErrorListener(ILoggingErrorListener errorListener) {
		GrammarChecker.errorListener = errorListener;
	}

	private static void checkSchemas(Grammar g, IErrorListener errorListener)
			throws TypeSystemException {

		// build the type system
		HashSet<Schema> schemaSet = new HashSet<Schema>();
		schemaSet.addAll(g.getAllSchemas());
		g.getSchemaTypeSystem().addTypes(schemaSet);

		// this function automatically calls the build function
		List<Schema> topoSortedSchemas = g.getSchemaTypeSystem().topologicalSort();

		// Set up sources for roles/constraints and type systems
		for (Schema s : g.getAllSchemas()) {
			annotateBlock(s.getContents(), s, g.getSchemaTypeSystem(), g, errorListener);
		}

		// add inherited stuff
		for (Schema schema : topoSortedSchemas) {
			for (Schema parent : g.getSchemaTypeSystem().getParents(schema)) {
				addParentInfo(schema.getContents(), parent.getContents(), schema, errorListener);
			}
		}

		for (Schema s : g.getAllSchemas()) {
			checkConstraints(s.getContents(), s, errorListener, g);
		}
	}

	/**
	 * Checks a MapPrimitive. Very similar (if not perfectly identical) to
	 * checkSchemas.
	 * 
	 * @param g
	 *            - The Grammar object
	 * @param errorListener
	 *            - IErrorListener object to report error
	 * @throws TypeSystemException
	 */
	private static void checkMaps(Grammar g, IErrorListener errorListener)
			throws TypeSystemException {

		// build the type system
		Set<MapPrimitive> mapSet = new HashSet<MapPrimitive>();
		mapSet.addAll(g.getAllMaps());
		g.getMapTypeSystem().addTypes(mapSet);

		// this function automatically calls the build function
		List<MapPrimitive> topoSortedMaps = g.getMapTypeSystem().topologicalSort();

		// Set up sources for roles/constraints and type systems
		for (MapPrimitive m : g.getAllMaps()) {
			// annotateBlock(m.getContents(), m, g.getSchemaTypeSystem(), g,
			// errorListener);
			annotateBlock(m.getContents(), m, g.getMapTypeSystem(), g, errorListener);
		}

		// add inherited stuff
		for (MapPrimitive map : topoSortedMaps) {
			for (MapPrimitive parent : g.getMapTypeSystem().getParents(map)) {
				addParentInfo(map.getContents(), parent.getContents(), map, errorListener);
			}
		}

		for (MapPrimitive m : g.getAllMaps()) {
			checkConstraints(m.getContents(), m, errorListener, g);
		}
	}

	/**
	 * Checks a MapPrimitive. Very similar (if not perfectly identical) to
	 * checkSchemas.
	 * 
	 * @param g
	 *            - The Grammar object
	 * @param errorListener
	 *            - IErrorListener object to report error
	 * @throws TypeSystemException
	 */

	private static void checkSituations(Grammar g, IErrorListener errorListener)
			throws TypeSystemException {

		// build the type system
		Set<Situation> situationSet = new HashSet<Situation>();
		situationSet.addAll(g.getAllSituations());
		g.getSituationTypeSystem().addTypes(situationSet);

		// this function automatically calls the build function
		List<Situation> topoSortedSituations = g.getSituationTypeSystem().topologicalSort();

		// Set up sources for roles/constraints and type systems
		for (Situation s : g.getAllSituations()) {
			annotateBlock(s.getContents(), s, g.getSituationTypeSystem(), g, errorListener);
		}

		// add inherited stuff
		for (Situation s : topoSortedSituations) {
			for (Situation parent : g.getSituationTypeSystem().getParents(s)) {
				addParentInfo(s.getContents(), parent.getContents(), s, errorListener);
			}
		}

		for (Situation s : g.getAllSituations()) {
			checkConstraints(s.getContents(), s, errorListener, g);
		}
	}

	private static void checkConstructions(Grammar g, IErrorListener errorListener)
			throws TypeSystemException {
		// System.out.println("check constructions is being called");
		// Build the type system
		HashSet<Construction> cxnSet = new HashSet<Construction>();
		cxnSet.addAll(g.getAllConstructions());
		g.getCxnTypeSystem().addTypes(cxnSet);
		// this function automatically calls the build function

		// get the topo sort
		List<Construction> topoSortedCxns = g.getCxnTypeSystem().topologicalSort();

		// Set up sources for roles/constraints and type systems
		for (Construction c : g.getAllConstructions()) {
			annotateBlock(c.getConstructionalBlock(), c, g.getCxnTypeSystem(), g, errorListener);
			annotateBlock(c.getFormBlock(), c, g.getSchemaTypeSystem(), g, errorListener);
			annotateBlock(c.getMeaningBlock(), c, g.getSchemaTypeSystem(), g, errorListener);
		}

		// add inherited stuff
		for (Construction construction : topoSortedCxns) {
			for (Construction parent : g.getCxnTypeSystem().getParents(construction)) {
				addParentInfo(construction.getFormBlock(), parent.getFormBlock(), construction,
						errorListener);
				addParentInfo(construction.getMeaningBlock(), parent.getMeaningBlock(),
						construction, errorListener);
				addParentInfo(construction.getConstructionalBlock(),
						parent.getConstructionalBlock(), construction, errorListener);
			}
			computeBlockType(construction, ECGConstants.FORM, construction.getFormBlock(), g,
					errorListener);
			computeBlockType(construction, ECGConstants.MEANING, construction.getMeaningBlock(), g,
					errorListener);
			computeBlockType(construction, ECGConstants.CONSTRUCTIONAL,
					construction.getConstructionalBlock(), g, errorListener);
		}

		for (Construction c : g.getAllConstructions()) {
			HashSet<Role> complements = new LinkedHashSet<Role>();
			HashSet<Role> optionals = new LinkedHashSet<Role>();
			for (Role r : c.getConstructionalBlock().getElements()) {
				Role clonedRole = r.clone();
				clonedRole.setContainer(c);
				if (r.getSpecialField().indexOf(ECGConstants.OPTIONAL) > -1) {
					optionals.add(clonedRole);
				} else {
					complements.add(clonedRole);
				}
			}
			c.setOptionals(optionals);
			c.setComplements(complements);
			LinkedHashSet<Role> clonedElements = new LinkedHashSet<Role>();
			clonedElements.addAll(optionals);
			clonedElements.addAll(complements);
			c.getConstructionalBlock().setElements(clonedElements);
			checkConstraints(c.getConstructionalBlock(), c, errorListener, g);
			checkConstraints(c.getMeaningBlock(), c, errorListener, g);
			checkConstraints(c.getFormBlock(), c, errorListener, g);
			c.setExtraPosedRole(discoverExtraPosedConstituent(c, errorListener));

		}

		// Check for a root cxn definition
		if (g.getConstruction(ECGConstants.ROOT) == null) {
			String message = "Grammar missing a root construction definition of type "
					+ ECGConstants.ROOT;
			errorListener.notify(message, Location.UNKNOWN, Severity.EXCEPTION);
		}
	}

	private static Role discoverExtraPosedConstituent(Construction c, IErrorListener errorListener) {
		Role extraPosedRole = null;
		for (Role role : c.getConstructionalBlock().getElements()) {
			if (role.getSpecialField().indexOf(ECGConstants.EXTRAPOSED) > -1) {
				if (extraPosedRole == null) {
					extraPosedRole = role;
				} else {
					String message = "Both " + extraPosedRole.getName() + " and " + role.getName()
							+ " are extra posed in cxn " + c.getName()
							+ " cannot have more than one extra posed role!";
					errorListener.notify(message, c.getLocation(), Severity.EXCEPTION);
				}
			}
		}
		return extraPosedRole;
	}

	private static List<TypeSystemNode> getPossibleTypes(String value, Grammar g) {
		List<TypeSystemNode> possible = new ArrayList<TypeSystemNode>();
		possible.add(g.getOntologyTypeSystem().get(value.substring(1)));
		possible.add(g.getSchemaTypeSystem().get(value));
		possible.add(g.getMapTypeSystem().get(value));
		possible.add(g.getSituationTypeSystem().get(value));

		return Arrays.dropNull(possible);
	}

	private static boolean isConstant(String value) {
		return value.charAt(0) == '\"';
	}

	private static void checkConstraints(Block block, Primitive primitive,
			IErrorListener errorListener, Grammar g) {
		Primitive originalType = primitive;
		for (Constraint constraint : block.getConstraints()) {

			// ADDED (lucag): begin this is a hack!!!
			String value = constraint.getValue();
			if (constraint.isAssign() && !isConstant(value)
					&& getPossibleTypes(value, g).size() == 0) {
				constraint.getArguments().add(new ECGSlotChain(value));
				constraint.setValue(null);
			}
			// ADDED (lucag): end this is a hack!!!

			List<TypeConstraint> slotChainTypes = new ArrayList<TypeConstraint>();
			Role lastRole = null;
			for (SlotChain argument : constraint.getArguments()) {
				Primitive parentPrimitive = null;
				primitive = originalType;
				for (Role role : argument.getChain()) {
					Role correspondingRole = primitiveHasRole(primitive, role, parentPrimitive);
					if (correspondingRole == null) {
						if (constraint.getSource().equals(originalType.getName())) {
							String message = "In " + originalType.getName() + ", constraint "
									+ constraint + " has undefined role: " + role
									+ " in slotchain " + argument + "\n";
							errorListener.notify(message, originalType.getLocation(),
									Severity.ERROR);
						}
						break;
					} else {
						parentPrimitive = primitive;
						if (correspondingRole.getTypeConstraint() != null
								&& (correspondingRole.getTypeConstraint().getTypeSystem() == g
										.getSchemaTypeSystem()
										|| correspondingRole.getTypeConstraint().getTypeSystem() == g
												.getCxnTypeSystem()
										// ADDED: the following 2 lines (lucag)
										|| correspondingRole.getTypeConstraint().getTypeSystem() == g
												.getMapTypeSystem() || correspondingRole
										.getTypeConstraint().getTypeSystem() == g
										.getSituationTypeSystem())) {
							primitive = (Grammar.Primitive) correspondingRole.getTypeConstraint()
									.getTypeSystem()
									.get(correspondingRole.getTypeConstraint().getType());
						} else {
							primitive = null;
						}
						// if (correspondingRole.getTypeConstraint() != null &&
						// (
						// correspondingRole.getTypeConstraint().getType().equals("ControlVerb")
						// ||
						// correspondingRole.getTypeConstraint().getType().equals("ControlVerbProcess"))){
						// System.out.println("correspondingRole: "+correspondingRole+" "+correspondingRole.getTypeConstraint()+" \n"+originalType.toString());
						// }
						role.setTypeConstraint(correspondingRole.getTypeConstraint());
						// if (correspondingRole.getTypeConstraint() != null){
						// System.out.println(/*primitive.getName()+"."+*/role.getName()+"
						// has its type set
						// to:"+correspondingRole.getTypeConstraint().getType()+"@"+correspondingRole.getTypeConstraint().getTypeSystem().getName());
						// }
					}
					lastRole = role;
				}

				if (lastRole == null && block.getTypeConstraint() != null) {
					slotChainTypes.add(block.getTypeConstraint());
				} else if (lastRole != null && lastRole.getTypeConstraint() != null) {
					slotChainTypes.add(lastRole.getTypeConstraint());

				}
			}
			TypeSystem ts = null;
			boolean badTS = false;
			for (TypeConstraint tc : slotChainTypes) {
				if (ts == null) {
					ts = tc.getTypeSystem();
				} else if (ts != tc.getTypeSystem()) {
					StringBuffer errorLog = new StringBuffer();
					errorLog.append("Constraint: " + constraint + " in " + originalType.getName()
							+ " connects types from different type systems\n");
					errorListener.notify(errorLog.toString(), originalType.getLocation(),
							Severity.ERROR);
					badTS = true;
					break;
				}

			}

			if (constraint.getOperator() != ECGConstants.MEETS
					&& constraint.getOperator() != ECGConstants.BEFORE) {
				String bestType = null;
				if (!badTS && ts != null) {
					for (TypeConstraint tc : slotChainTypes) {
						try {
							if (bestType == null) {
								bestType = tc.getType();
							} else if (ts.subtype(tc.getType(), bestType)) {
								bestType = tc.getType();
							} else if (ts.subtype(bestType, tc.getType())) {
								bestType = bestType;
							} else {
								String message = "Constraint: " + constraint + " in "
										+ originalType.getName() + " connects incompatible types\n";
								errorListener.notify(message, originalType.getLocation(),
										Severity.ERROR);
								break;
							}
						} catch (TypeSystemException tse) {
							String message = "Constraint: " + constraint + " in "
									+ originalType.getName() + " connects undefined types\n";
							errorListener.notify(message, originalType.getLocation(),
									Severity.ERROR);
							break;
						}
					}
				}
			}
			Interner<String> interner = buildTemporaryECGInterner();
			if (constraint.isAssign()) {
				// String value = value;
				if (value != null && value.charAt(0) != '\"') {
					if (value.charAt(0) == '@') {
						if (g.getOntologyTypeSystem().get(value.substring(1)) == null) {
							String message = "Constraint: " + constraint
									+ " has an unknown type in its rhs\n";
							errorListener.notify(message, originalType.getLocation(),
									Severity.ERROR);
						}
					} else {
						// ADDED: (lucag) begin
						if (!value.equals(ECGConstants.FOCUS)) { // "focus" is
																	// fine...
							Collection<TypeSystemNode> notNull = getPossibleTypes(value, g);
							if (notNull.size() == 0) {
								String message = "Constraint: " + constraint
										+ " has an unknown type in its rhs\n";
								errorListener.notify(message, originalType.getLocation(),
										Severity.ERROR);
							} else if (notNull.size() > 1) {
								String message = "Impossible to determine type of constraint "
										+ constraint + "; multiple types have the same name: "
										+ Arrays.join(notNull.toArray());
								errorListener.notify(message, originalType.getLocation(),
										Severity.WARNING);
							}
						}
						// ADDED: (lucag) end
					}
				} else {
					constraint.setValue(interner.intern(value));
				}
			}
		}
	}

	private static Interner<String> buildTemporaryECGInterner() {
		Interner<String> interner = new Interner<String>();
		interner.intern(ECGConstants.GIVEN);
		interner.intern(ECGConstants.IDENTIFIABLE);
		interner.intern(ECGConstants.CONTEXTNEW);
		return interner;
	}

	private static Role primitiveHasRole(Primitive primitive, Role role, Primitive parentPrimitive) {
		if (primitive == null
				&& (parentPrimitive == null || parentPrimitive instanceof Construction == false)) {
			return null;
		} else if (primitive == null && parentPrimitive instanceof Construction == true) {
			if (role.getName().equals("m")) {
				return ECGConstants.UNTYPEDMROLE;
			}
			if (role.getName().equals("f")) {
				return ECGConstants.UNTYPEDFROLE;
			}
			// } else if (role.getName().equals(ECGConstants.FOCUS)) {
			// return ECGConstants.FOCUSROLE;
		} else if (primitive.getAllRoles().contains(role)) {
			return getCorrespondingRole(primitive.getAllRoles(), role);
		}
		return null;
	}

	private static Role getCorrespondingRole(Set<Role> roleSet, Role that) {
		for (Role r : roleSet) {
			if (r.equals(that)) {
				// System.out.println("getCorrespondingRole: "+that.getName()+"->
				// "+r.getName()+":"+
				return r;
			}
		}
		throw new GrammarException("this code point should never be reached");
	}

	private static void externalTypeCheck(HashSet<String> types, HashSet<TypeSystem> typeSystems,
			Block b, Grammar g) {
		String t = b.getType();
		if (t.charAt(0) == '@') {
			types.add(t.substring(1));
			typeSystems.add(g.getOntologyTypeSystem());
		} else if (b.getType() != ECGConstants.UNTYPED) {
			types.add(t);
			typeSystems.add(g.getSchemaTypeSystem());
		}
	}

	private static void computeBlockType(Construction construction, String blockKind, Block block,
			Grammar g, IErrorListener errorListener) /*
													 * throws
													 * TypeSystemException
													 */{
		if (block == null) {
			return;
		}

		HashSet<String> types = new HashSet<String>();
		HashSet<TypeSystem> typeSystems = new HashSet<TypeSystem>();
		externalTypeCheck(types, typeSystems, block, g);
		if (blockKind.equals(ECGConstants.FORM)) {
			for (Construction parent : g.getCxnTypeSystem().getParents(construction)) {
				if (parent.getFormBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getFormBlock().getType());
					typeSystems.add(parent.getFormBlock().getBlockTypeTypeSystem());
				}
			}
		} else if (blockKind.equals(ECGConstants.MEANING)) {
			for (Construction parent : g.getCxnTypeSystem().getParents(construction)) {
				if (parent.getMeaningBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getMeaningBlock().getType());
					typeSystems.add(parent.getMeaningBlock().getBlockTypeTypeSystem());
				}
			}
		} else {
			for (Construction parent : g.getCxnTypeSystem().getParents(construction)) {
				if (parent.getConstructionalBlock().getType() != ECGConstants.UNTYPED) {
					types.add(parent.getConstructionalBlock().getType());
					typeSystems.add(parent.getConstructionalBlock().getBlockTypeTypeSystem());
				}
			}
		}
		String bestType = ECGConstants.UNTYPED;

		if (typeSystems.size() > 1) {
			String message = "Construction " + construction.getName()
					+ " has inherited mixed ecg and external types for its " + blockKind
					+ " pole out of the inherited types: " + types + "\n";
			errorListener.notify(message, construction.getLocation(), Severity.ERROR);

		}
		if (types.size() > 0) {
			try {
				TypeSystem ts = typeSystems.iterator().next();
				bestType = ts.bestUnifyingType(types);
				block.setBlockTypeTypeSystem(ts);
			} catch (TypeSystemException tse) {
				String message = "Construction " + construction.getName()
						+ " does not have a consistent inherited type for its " + blockKind
						+ " pole out of the inherited types: " + types + "\n";
				errorListener.notify(message, construction.getLocation(), Severity.ERROR);
			}
		}
		block.setType(bestType);
	}

	private static void addInheritedRoles(Set<Role> childRoles, Set<Role> parentRoles,
			Primitive childPrimitive, IErrorListener errorListener) {
		String childName = childPrimitive.getName();
		for (Role prole : parentRoles) {
			if (childRoles.contains(prole) && prole.getTypeConstraint() != null) {
				TypeConstraint ptc = prole.getTypeConstraint();
				Role crole = null;
				for (Role cr : childRoles) {
					if (cr.equals(prole)) {
						crole = cr;
					}
				}
				TypeConstraint ctc = crole.getTypeConstraint();
				try {
					if (ctc == null) {
						childRoles.remove(crole);
						childRoles.add(prole);
					} else if (ptc.getTypeSystem() != ctc.getTypeSystem()) {
						StringBuffer errorLog = new StringBuffer();
						errorLog.append("Structure ")
								.append(childName)
								.append(" has inherited (or locally defined) roles with the same name but different type systems. The roles are ");
						errorLog.append(prole.toString()).append(":").append(ptc.toString())
								.append(" from ").append(prole.getSource());
						errorLog.append(" and ").append(crole.toString()).append(":")
								.append(ctc.toString()).append(" from ").append(crole.getSource())
								.append("\n");
						errorListener.notify(errorLog.toString(), childPrimitive.getLocation(),
								Severity.ERROR);
					} else if (ptc.getTypeSystem().subtype(ptc.getType(), ctc.getType())
							&& !crole.getSource().equals(childName)) {
						// the parent's type constraint is more specific than
						// the child's constraint
						// this happens if one ancestor got processed first and
						// the type on its role
						// was more general than the other ancestors. (only do
						// this though when the child type isn't locally
						// defined)
						childRoles.remove(crole);
						childRoles.add(prole);
					} else if (ptc.getTypeSystem().subtype(ctc.getType(), ptc.getType())) {
						// in this case we do nothing cuz the child role is a
						// subtype of the parent role
					} else { // incompatible types
						StringBuffer errorLog = new StringBuffer();
						errorLog.append("Structure ")
								.append(childName)
								.append(" has inherited (or locally defined) roles with the same name but incompatible types. The roles are ");
						errorLog.append(prole.toString()).append(":").append(ptc.toString())
								.append(" from ").append(prole.getSource());
						errorLog.append(" and ").append(crole.toString()).append(":")
								.append(ctc.toString()).append(" from ").append(crole.getSource())
								.append("\n");
						errorListener.notify(errorLog.toString(), childPrimitive.getLocation(),
								Severity.ERROR);
					}
				} catch (TypeSystemException tse) {
					errorListener.notify(tse.toString(), childPrimitive.getLocation(),
							Severity.FATAL);
				}
			} else {
				childRoles.add(prole);
			}
		}
	}

	// <<<<<<< GrammarChecker.java
	private static void addParentInfo(Block child, Block parent, Primitive childPrimitive,
			IErrorListener errorListener) {
		for (Constraint c : parent.getConstraints()) {
			List<SlotChain> newArgs = new ArrayList<SlotChain>();
			for (SlotChain sc : c.getArguments()) {
				newArgs.add(new ECGSlotChain(sc.toString()));
			}
			child.getConstraints().add(
					new Constraint(c.getOperator(), c.getSource(), c.getValue(), c.overridden(),
							newArgs));
		}
		// =======
		// private static void addParentInfo(Block child, Block parent, String
		// childName, StringBuffer errorLog) {
		// for (Constraint c : parent.getConstraints()){
		// List<SlotChain> newArgs = new ArrayList<SlotChain>();
		// for (SlotChain sc : c.getArguments()){
		// newArgs.add(new ECGSlotChain(sc.toString()));
		// }
		// child.getConstraints().add(new Constraint(c.getOperator(),
		// c.getSource(), c.getValue(), c.overridden(), newArgs));
		// }
		// >>>>>>> 1.29
		child.getConstraints().addAll(parent.getConstraints());
		addInheritedRoles(child.getElements(), parent.getElements(), childPrimitive, errorListener);
		addInheritedRoles(child.getEvokedElements(), parent.getEvokedElements(), childPrimitive,
				errorListener);
	}

	private static void annotateBlock(Block block, Primitive primitive, TypeSystem typeSystem,
			Grammar g, IErrorListener errorListener) {
		if (block == null)
			return;

		String source = primitive.getName();
		block.setTypeSource(source);
		for (Constraint c : block.getConstraints()) {
			c.setSource(source);
		}
		updateRoles(primitive, block.getEvokedElements(), typeSystem, g, errorListener);
		updateRoles(primitive, block.getElements(), typeSystem, g, errorListener);
	}

	private static void updateRoles(Primitive primitive, Set<Role> roles, TypeSystem typeSystem,
			Grammar g, IErrorListener errorListener) {
		String source = primitive.getName();
		for (Role r : roles) {
			r.setSource(source);
			if (r.getName().equals(ECGConstants.FORM_POLE)
					|| r.getName().equals(ECGConstants.MEANING_POLE)) {
				String message = "Role " + r.getName() + " from " + source
						+ ": roles/constituents/evokes variables cannot be named '"
						+ ECGConstants.FORM_POLE + "' or '" + ECGConstants.MEANING_POLE + "'\n";
				errorListener.notify(message, primitive.getLocation(), Severity.ERROR);
			}
			if (r.getTypeConstraint() != null /*
											 * &&
											 * r.getTypeConstraint().getTypeSystem
											 * () == null
											 */) {
				String type = r.getTypeConstraint().getType();
				if (type.charAt(0) == '@'
						|| (g.getOntologyTypeSystem() != null && r.getTypeConstraint()
								.getTypeSystem() == g.getOntologyTypeSystem())) {
					if (type.charAt(0) == '@') {
						type = r.getTypeConstraint().type.substring(1);
					}

					if (g.getOntologyTypeSystem().get(type) == null) {
						String message = "Role " + r.getName() + " from " + source
								+ " has an undefined type constraint: "
								+ r.getTypeConstraint().getType() + "\n";
						errorListener.notify(message, primitive.getLocation(), Severity.ERROR);
					} else {
						r.setTypeConstraint(g.getOntologyTypeSystem()
								.getCanonicalTypeConstraint(type));
					}
				} else {
					// MODIFIED: lucag
					// Let's quickly take care of the common case
					if (typeSystem.get(type) != null) {
						r.setTypeConstraint(typeSystem.getCanonicalTypeConstraint(type));
						continue;
					}

					// FIXME: this is a hack
					List<TypeConstraint> possible = new ArrayList<TypeConstraint>();
					possible.add(g.getMapTypeSystem().getCanonicalTypeConstraint(type));
					possible.add(g.getSituationTypeSystem().getCanonicalTypeConstraint(type));
					possible.add(g.getSchemaTypeSystem().getCanonicalTypeConstraint(type));
					List<TypeConstraint> nonNull = Arrays.dropNull(possible);
					if (nonNull.size() > 1) {
						String message = String.format(
								"Role %s from %s has conflicting type constraints: %s",
								r.getName(), source, Arrays.join(nonNull.toArray()));
						errorListener.notify(message, primitive.getLocation(), Severity.ERROR);
						r.setTypeConstraint(typeSystem.getCanonicalTypeConstraint(type));
					} else if (nonNull.size() == 1) {
						r.setTypeConstraint(nonNull.get(0));
					} else {
						String message = "Role " + r.getName() + " from " + source
								+ " has an undefined type constraint: "
								+ r.getTypeConstraint().getType() + "\n";
						errorListener.notify(message, primitive.getLocation(), Severity.ERROR);
					}
					// MODIFIED: end
				}
			}
		}
	}

}

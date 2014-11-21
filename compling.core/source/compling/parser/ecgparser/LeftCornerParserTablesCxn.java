package compling.parser.ecgparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import compling.context.ContextModelCache;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Prefs;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotChainTables;
import compling.util.Counter;
import compling.util.IdentityHashSet;
import compling.util.Pair;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;
import compling.util.math.SloppyMath;

public class LeftCornerParserTablesCxn {

	public static class ConstituentsToSatisfyTable {
		Map<Role, LinkedHashSet<Role>> table;
		LCPGrammarWrapper grammar;

		public ConstituentsToSatisfyTable(LCPGrammarWrapper g) {
			table = new IdentityHashMap<Role, LinkedHashSet<Role>>();
			grammar = g;
			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				RHS rhs = new RHS(cxn, cxn.getComplements(), cxn.getOptionals(), cxn.getFormBlock().getConstraints());
				HashMap<Role, LinkedHashSet<Role>> processed = new HashMap<Role, LinkedHashSet<Role>>();

				Role role = rhs.getNextSymbol();
				while (role != null) {
					LinkedHashSet<Role> union = new LinkedHashSet<Role>();
					for (Constraint constraint : rhs.getFormConstraintsUsingRightArgument(role)) {
						union.add(rhs.getArg(0, constraint));
						if (processed.get(rhs.getArg(0, constraint)) == null) {
							System.out.println(rhs.inLinks);
							System.out.println(rhs.formConstraints);
							System.out.println(role);
							System.out.println(constraint);
							RHS rhs2 = new RHS(cxn, cxn.getComplements(), cxn.getOptionals(), cxn.getFormBlock()
									.getConstraints());
							System.out.println(rhs2.inLinks);
							System.out.println(rhs2.formConstraints);
						}
						union.addAll(processed.get(rhs.getArg(0, constraint)));
					}
					table.put(role, union);
					rhs.advance(role);
					processed.put(role, union);
					role = rhs.getNextSymbol();
				}
			}
		}

		public Set<Role> get(Role r) {
			return table.get(r);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("Constituents To Satisfy Table\n--------------------------------\n");
			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				sb.append("\t").append(cxn.getName()).append("\n");
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					sb.append("\t\t").append(role.getName()).append(" (");
					if (table.get(role) == null) {
						System.out.println(role.getName() + " is null in the table");
					}
					for (Role prior : table.get(role)) {
						sb.append(prior.getName()).append(", ");
					}
					sb.append(" )\n");
				}
			}
			return sb.toString();
		}

	}

	public static class TypeToConstituentsTable {

		public static class AttachPoint {
			public Construction cxn;
			public Role constituent;

			AttachPoint(Construction cxn, Role constituent) {
				this.cxn = cxn;
				this.constituent = constituent;
			}
		}

		Map<Construction, List<AttachPoint>> table;

		public TypeToConstituentsTable(LCPGrammarWrapper g, UnifyTable ut) {
			table = new HashMap<Construction, List<AttachPoint>>();
			for (Construction cxn : g.getAllConcretePhrasalConstructions()) {
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					for (Construction sub : g.getRules(role.getTypeConstraint().getType())) {
						if (ut.unifies(role, sub)) {
							if (table.containsKey(sub) == false) {
								table.put(sub, new ArrayList<AttachPoint>());
							}
							table.get(sub).add(new AttachPoint(cxn, role));
						}
					}
				}
			}
		}

		List<AttachPoint> get(Construction c) {
			return table.get(c);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("\nType To Constituent Table:\n");
			for (Construction c : table.keySet()) {
				sb.append(c.getName()).append(": ");
				for (AttachPoint ap : table.get(c)) {
					sb.append(ap.cxn.getName()).append(".").append(ap.constituent.getName()).append(", ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public static class UnifyTable { // checks pairwise unifications
		private Map<Role, Set<Construction>> table = new IdentityHashMap<Role, Set<Construction>>();
		LCPGrammarWrapper grammar;

		public UnifyTable(LCPGrammarWrapper g, CloneTable ct) {
			// System.out.println("UnifyTable constructor");
			this.grammar = g;
			for (Construction cxn : g.getAllConcretePhrasalConstructions()) {
				// System.out.println("Construction: "+cxn.getName());
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					// System.out.println("\tRole:"+role.getName()+" type:"+role.getTypeConstraint().getType()+" system:"+role.getTypeConstraint().getTypeSystem().getName());
					table.put(role, new HashSet<Construction>());
					for (Construction sub : g.getRules(role.getTypeConstraint().getType())) {
						Analysis cxnA = ct.get(cxn);
						// System.out.println("\t\ttrying: "+cxn.getName()+" "+role.getName()+" "+sub.getName());
						if (cxnA.advance(role, ct.get(sub))) {
							table.get(role).add(sub);
							// System.out.print("  true");
						}
						// System.out.print("\n");
					}
				}
			}
		}

		public boolean unifies(Role constituent, Construction type) {
			return table.get(constituent).contains(type);
		}

	}

	public static class ReachabilityTable {
		Map<Construction, Map<Construction, PathRecord>> table = new HashMap<Construction, Map<Construction, PathRecord>>();
		Map<Role, Map<Construction, Double>> reachabilityCost = new IdentityHashMap<Role, Map<Construction, Double>>();
		Map<Role, Double> lexNormalizer = new IdentityHashMap<Role, Double>();
		IdentityHashSet<Construction> lexicalConstructions = new IdentityHashSet<Construction>();
		int MAXDISTANCE = 15;

		public class PathRecord {
			public double cost[] = new double[MAXDISTANCE + 1];
			public Construction source;
			public Construction dest;

			PathRecord(Construction source, Construction dest) {
				this.source = source;
				this.dest = dest;
				for (int i = 0; i < cost.length; i++) {
					cost[i] = Double.NEGATIVE_INFINITY;
				}
			}
		}

		public double reachable(Role r, Construction c) {
			Double cost = reachabilityCost.get(r).get(c);
			if (cost != null) {
				return cost;
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		public double normReachable(Role r, Construction c) {
			if (!lexicalConstructions.contains(c)) {
				return Double.NEGATIVE_INFINITY;
			}
			Double cost = reachabilityCost.get(r).get(c);
			if (cost != null) {
				return cost - lexNormalizer.get(r);
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		private void setUpReachabilityBaseCase(LCPGrammarWrapper g, ConstituentsToSatisfyCostTable ctsct,
				ConstituentExpansionCostTable cect, UnifyTable unifyTable, ConstituentLocalityCostTable clct) {
			for (Construction source : g.getAllConcretePhrasalConstructions()) {
				table.put(source, new HashMap<Construction, PathRecord>());
				for (Construction dest : g.getAllConstructions()) {
					if (dest.isConcrete()) {
						table.get(source).put(dest, new PathRecord(source, dest));
						// System.out.println(source.getName()+" "+dest.getName());
					}
				}
			}
			for (Construction source : g.getAllConcretePhrasalConstructions()) {
				for (Role role : source.getConstructionalBlock().getElements()) {
					reachabilityCost.put(role, new HashMap<Construction, Double>());
					for (Construction sub : g.getRules(role.getTypeConstraint().getType())) {
						if (unifyTable.unifies(role, sub)) {
							double ctsctc = ctsct.getConstituentsToSatisfyCost(role);
							// System.out.println("init: "+role.getName()+" "+sub.getName());
							if (cect == null || clct == null) {
								System.out.println("what?");
							}
							double cectc = cect.getConstituentExpansionCost(role, sub) + clct.getLocalCost(role);

							// System.out.println("init: "+source.getName()+" "+sub.getName()+" ->  cts:"+ctsctc+" +  ce:"+cectc+" + oldCost:"+
							// table.get(source).get(sub).cost[0]+" = "+SloppyMath.logAdd(table.get(source).get(sub).cost[0],
							// ctsctc + cectc));
							table.get(source).get(sub).cost[0] = SloppyMath.logAdd(table.get(source).get(sub).cost[0], ctsctc
									+ cectc);
							reachabilityCost.get(role).put(sub, cectc);
						}

					}
				}
			}
		}

		public ReachabilityTable(LCPGrammarWrapper g, ConstituentsToSatisfyCostTable ctsct,
				ConstituentExpansionCostTable cect, UnifyTable unifyTable, ConstituentLocalityCostTable clct) {
			setUpReachabilityBaseCase(g, ctsct, cect, unifyTable, clct);

			for (int distance = 1; distance < MAXDISTANCE; distance++) {
				for (Construction source : g.getAllConcretePhrasalConstructions()) {
					for (Role role : source.getConstructionalBlock().getElements()) {
						double ctsctc = ctsct.getConstituentsToSatisfyCost(role);

						for (Construction sub : g.getRules(role.getTypeConstraint().getType())) {
							if (unifyTable.unifies(role, sub)) {
								double cectc = cect.getConstituentExpansionCost(role, sub) + clct.getLocalCost(role);
								if (table.get(sub) != null) {
									for (Construction dest : table.get(sub).keySet()) {
										PathRecord sourcePR = table.get(source).get(dest);
										PathRecord subPR = table.get(sub).get(dest);

										// System.out.println("iter"+distance+": "+source.getName()+" "+sub.getName()+" "+dest.getName()+" ->  cts:"
										// +ctsctc+" ce:"+cectc+" oldSourcePR:"+
										// sourcePR.cost[distance]+" sub-1:"+subPR.cost[distance-1]+
										// " newSource: "+SloppyMath.logAdd(sourcePR.cost[distance], ctsctc + cectc+
										// subPR.cost[distance-1]));

										sourcePR.cost[distance] = SloppyMath.logAdd(sourcePR.cost[distance], ctsctc + cectc
												+ subPR.cost[distance - 1]);
									}
								}
							}
						}
					}
				}
			}
			for (Construction c : g.getAllConcretePhrasalConstructions()) {
				for (Role role : c.getConstructionalBlock().getElements()) {
					for (Construction source : g.getRules(role.getTypeConstraint().getType())) {
						double cectc = cect.getConstituentExpansionCost(role, source) + clct.getLocalCost(role);
						if (table.get(source) != null && unifyTable.unifies(role, source)) {
							for (Construction dest : table.get(source).keySet()) {
								double oldCost = Double.NEGATIVE_INFINITY;
								if (reachabilityCost.get(role).get(dest) != null) {
									oldCost = reachabilityCost.get(role).get(dest);
								}
								// if (source == dest){oldCost = SloppyMath.logAdd(oldCost, 0);}
								// System.out.println(role.getName()+" s:"+source.getName()+" d:"+dest.getName());
								// System.out.println("\toc:"+oldCost+" ce:"+cectc+" sum distance:"+SloppyMath.logAdd(table.get(source).get(dest).cost)+" newCost:"+
								// SloppyMath.logAdd(oldCost, cectc+SloppyMath.logAdd(table.get(source).get(dest).cost)));
								reachabilityCost.get(role).put(dest,
										SloppyMath.logAdd(oldCost, cectc + SloppyMath.logAdd(table.get(source).get(dest).cost)));
							}
						}
					}
				}
			}
			for (Role r : reachabilityCost.keySet()) {
				double total = Double.NEGATIVE_INFINITY;
				for (Construction c : g.getAllConcreteLexicalConstructions()) {
					lexicalConstructions.add(c);
					if (reachabilityCost.get(r).get(c) != null) {
						total = SloppyMath.logAdd(total, reachabilityCost.get(r).get(c));
					}
				}
				lexNormalizer.put(r, total);
			}
			for (Role r : lexNormalizer.keySet()) {
				lexNormalizer.put(r, lexNormalizer.get(r) - clct.getLocalCost(r));
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(
					"\nREACHABILITY TABLE\n--------------------------------------------------------------\n");
			for (Role role : reachabilityCost.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName()).append("\n");
				for (Construction cxn : reachabilityCost.get(role).keySet()) {
					sb.append("\t-> ").append(cxn.getName()).append("\t").append(reachabilityCost.get(role).get(cxn))
							.append("\n");
				}
			}
			return sb.toString();
		}

		public String toString(Construction from) {
			StringBuilder sb = new StringBuilder("\nREACHABILITY TABLE ENTRIES FOR " + from.getName()
					+ "\n--------------------------------------------------------------\n");
			for (Role role : reachabilityCost.keySet()) {
				if (role.getContainer() == from) {
					sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName())
							.append("\n");
					for (Construction cxn : reachabilityCost.get(role).keySet()) {
						sb.append("\t-> ").append(cxn.getName()).append("\t").append(reachabilityCost.get(role).get(cxn))
								.append("\n");
					}
				}
			}
			return sb.toString();
		}
	}

	public static interface AnalysisFactory<T extends Analysis> {
		public T get(Construction cxn);

		public void update(T t);

		public void cleanUp(T t);

		public void staticSetUp();
	}

	public static class BasicAnalysisFactory implements AnalysisFactory<Analysis> {
		SlotChainTables sct;
		LCPGrammarWrapper g;
		Boolean expanded = false;

		// this constructor is just here for inheritance purposes
		public BasicAnalysisFactory() {
		}

		public BasicAnalysisFactory(LCPGrammarWrapper g) {
			this(g, false);
		}

		public BasicAnalysisFactory(LCPGrammarWrapper g, Boolean expanded) {
			this.sct = new SlotChainTables(g, new CloneTable(g, new BasicAnalysisFactory()));
			this.g = g;
			this.expanded = expanded;
		}

		public Analysis get(Construction cxn) {
			Analysis a = new Analysis(cxn, g);
			return a;
		}

		public void update(Analysis a) {
			if (sct != null) {
				List<List<Pair<TypeConstraint, Role>>> frameRoles = new ArrayList<List<Pair<TypeConstraint, Role>>>();
				for (SlotChain sc : sct.getCanonicalChains(a.getHeadCxn())) {
					frameRoles.add(sct.getFrameRoles(a.getHeadCxn(), sc));
				}
				a.setSemanticChains(sct.getCanonicalChains(a.getHeadCxn()), frameRoles);
			}
			return;
		}

		public void cleanUp(Analysis a) {
		}

		public void staticSetUp() {
			return;
		}
	}

	public static class AnalysisInContextFactory implements AnalysisFactory<AnalysisInContext> {
		SlotChainTables sct;
		LCPGrammarWrapper g;
		ContextModelCache cmc;
		boolean expanded = false;

		public AnalysisInContextFactory(LCPGrammarWrapper g, ContextModelCache cmc) {
			this(g, cmc, false);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public AnalysisInContextFactory(LCPGrammarWrapper g, ContextModelCache cmc, boolean expanded) {
			this.sct = new SlotChainTables(g, new CloneTable(g, new BasicAnalysisFactory()));
			this.g = g;
			this.cmc = cmc;
			this.expanded = expanded;
		}

		public AnalysisInContext get(Construction cxn) {
			return new AnalysisInContext(cxn, g);
		}

		@Override
		public void update(AnalysisInContext aic) {
			List<List<Pair<TypeConstraint, Role>>> frameRoles = new ArrayList<List<Pair<TypeConstraint, Role>>>();
			for (SlotChain sc : sct.getCanonicalChains(aic.getHeadCxn())) {
				frameRoles.add(sct.getFrameRoles(aic.getHeadCxn(), sc));
			}
			aic.setSemanticChains(sct.getCanonicalChains(aic.getHeadCxn()), frameRoles);
			aic.setRDChains(sct.getRDChains(aic.getHeadCxn()));
		}

		public void cleanUp(AnalysisInContext aic) {
			aic.resolve();
		}

		public void staticSetUp() {
			AnalysisInContext.setRDTypeConstraint(g.getSchemaTypeSystem().getCanonicalTypeConstraint("RD"));
			AnalysisInContext.setContextModelCache(cmc);
		}

		public SlotChainTables getSlotChainTables() {
			return sct;
		}
	}

	public static class CloneTable<T extends Analysis> {
		HashMap<Construction, T> canonicalInstances = new HashMap<Construction, T>();
		int slotCounter = 0;
		AnalysisFactory<T> factory;

		public CloneTable(LCPGrammarWrapper g, AnalysisFactory<T> factory) {
			this.factory = factory;
			for (Construction cxn : g.getAllConstructions()) {
				// if (cxn.isConcrete()){
				canonicalInstances.put(cxn, factory.get(cxn));
				// }
			}
		}

		public void update() {
			factory.staticSetUp();
			for (Construction cxn : canonicalInstances.keySet()) {
				factory.update(canonicalInstances.get(cxn));

			}
		}

		T get(Construction cxn, int startIndex) {
			T a = get(cxn);
			a.setStartIndex(startIndex);
			a.getPossibleSemSpecs().setID();
			return a;
		}
		
		// adds a new Construction to canonical Instances HashMap. @seantrott. 
		public void put(Construction cxn) {
			T a = factory.get(cxn);
			canonicalInstances.put(cxn, a); //   factory.get(cxn));
			//System.out.println(factory.get(cxn));
			
		}

		@SuppressWarnings("unchecked")
		public T get(Construction cxn) {
			T a = (T) canonicalInstances.get(cxn).clone();
			for (Slot s : a.getFeatureStructure().getSlots()) {
				s.setID(slotCounter++);
			}
			return a;
		}

	}

	public static class ConstituentLocalityCostTable {
		IdentityHashMap<Role, Double> localCostTable = new IdentityHashMap<Role, Double>();
		IdentityHashMap<Role, Double> omissionCostTable = new IdentityHashMap<Role, Double>();
		IdentityHashMap<Role, Double> nonLocalCostTable = new IdentityHashMap<Role, Double>();

		private void tablePutter(IdentityHashMap<Role, Double> table, Role role, double prob) {
			table.put(role, Math.log(prob));
		}

		ConstituentLocalityCostTable(LCPGrammarWrapper grammar) {
			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				// System.out.println(cxn.getName());
				for (Role role : cxn.getComplements()) {
					// System.out.print("\t"+role.getName());
					String special = role.getSpecialField();
					if (special.indexOf("[") >= 0) {
						String[] probs = special.substring(special.indexOf("[") + 1, special.indexOf("]")).split("\\s");

						// for (int i = 0; i < probs.length; i++){
						// System.out.print("probs |");
						// System.out.print(probs[i]);
						// System.out.print("| ");
						// }
						double expressedProbability = Double.parseDouble(probs[1]);
						if (probs.length == 2) {
							tablePutter(localCostTable, role, expressedProbability);
							tablePutter(omissionCostTable, role, (1 - expressedProbability));
							tablePutter(nonLocalCostTable, role, 0);
						}
						else if (probs.length == 3) {
							double localProbability = Double.parseDouble(probs[2]);
							tablePutter(localCostTable, role, expressedProbability * localProbability);
							tablePutter(omissionCostTable, role, (1 - expressedProbability));
							tablePutter(nonLocalCostTable, role, (expressedProbability * (1 - localProbability)));
						}
						else {
							throw new ParserException("Bad probs specified in grammar file for " + cxn.getName() + "."
									+ role.getName());
						}

					}
					else if (special.indexOf(ECGConstants.EXTRAPOSED) > -1) {
						tablePutter(localCostTable, role, 1);
						tablePutter(omissionCostTable, role, 0);
						tablePutter(nonLocalCostTable, role, 0);

					}
					else {
						tablePutter(localCostTable, role, ECGConstants.DEFAULTLOCALPROBABILITY);
						tablePutter(omissionCostTable, role, ECGConstants.DEFAULTOMISSIONPROBABILITY);
						tablePutter(nonLocalCostTable, role, ECGConstants.DEFAULTNONLOCALPROBABILITY);
					}
					// System.out.println(" ("+localCostTable.get(role)+", "+omissionCostTable.get(role)+", "+nonLocalCostTable.get(role)+" )");
				}
				for (Role role : cxn.getOptionals()) {
					// System.out.print("\t"+role.getName());
					String special = role.getSpecialField();
					if (special.indexOf("[") >= 0) {
						String probs[] = special.substring(special.indexOf("[") + 1, special.indexOf("]")).split(" |,");
						double probability = Double.parseDouble(probs[1]);
						tablePutter(localCostTable, role, probability);
						tablePutter(omissionCostTable, role, 1 - probability);
					}
					else {
						tablePutter(localCostTable, role, ECGConstants.DEFAULTOPTIONALPROBABILITY);
						tablePutter(omissionCostTable, role, 1 - ECGConstants.DEFAULTOPTIONALPROBABILITY);
					}
					tablePutter(nonLocalCostTable, role, 0.0);
					// System.out.println(" ("+localCostTable.get(role)+", "+omissionCostTable.get(role)+", "+nonLocalCostTable.get(role)+" )");
				}
			}
		}

		public double getLocalCost(Role r) {
			return localCostTable.get(r);
		}

		public double getOmissionCost(Role r) {
			return omissionCostTable.get(r);
		}

		public double getNonLocalCost(Role r) {
			return nonLocalCostTable.get(r);
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"Constituent Locality Cost Table\n-------------------------------------------\n");
			for (Role role : localCostTable.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName())
						.append(" : l=").append(localCostTable.get(role)).append(", o=");
				sb.append(omissionCostTable.get(role)).append(", nl=").append(nonLocalCostTable.get(role)).append("\n");
			}
			return sb.toString();
		}

	}

	public static interface ConstituentExpansionCostTable {
		public double getConstituentExpansionCost(Role role, Construction cxn);
	}

	public static class DumbConstituentExpansionCostTable implements ConstituentExpansionCostTable {
		IdentityHashMap<Role, Counter<Construction>> roleToConstructionCounter = new IdentityHashMap<Role, Counter<Construction>>();

		public DumbConstituentExpansionCostTable(LCPGrammarWrapper grammar) {
			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				// System.out.println(cxn.getName());
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					// System.out.print("\t"+role.getName()+": ");
					roleToConstructionCounter.put(role, new Counter<Construction>());
					double prob = ((double) 1)
							/ ((double) grammar.getConcreteSubtypes(role.getTypeConstraint().getType()).size());
					for (String cxnName : grammar.getConcreteSubtypes(role.getTypeConstraint().getType())) {
						// System.out.print("  "+cxnName+":");
						// System.out.print(Math.abs(Math.log(prob)));
						roleToConstructionCounter.get(role).setCount(grammar.getConstruction(cxnName), Math.log(prob));
						// System.out.print(roleToConstructionCounter.get(role).getCount(grammar.getConstruction(cxnName)));
					}
					// System.out.println(";");
				}
			}
		}

		public double getConstituentExpansionCost(Role role, Construction cxn) {
			if (roleToConstructionCounter.get(role).containsKey(cxn)) {
				return roleToConstructionCounter.get(role).getCount(cxn);
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"Dumb Constituent Expansion Table\n----------------------------------------------------\n");
			for (Role role : roleToConstructionCounter.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName()).append(": ");
				for (Construction cxn : roleToConstructionCounter.get(role).keySet()) {
					sb.append(cxn.getName()).append("[").append(getConstituentExpansionCost(role, cxn)).append("]  ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public static class ConstituentsToSatisfyCostTable {

		IdentityHashMap<Role, Double> costTable = new IdentityHashMap<Role, Double>();

		public ConstituentsToSatisfyCostTable(LCPGrammarWrapper grammar, ConstituentsToSatisfyTable ctst,
				ConstituentLocalityCostTable clct) {
			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				// System.out.println(cxn.getName());
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					// System.out.print("\t"+role+"  -> \t");
					double cost = 0;
					for (Role toSatisfy : ctst.get(role)) {
						// System.out.print("+ "+toSatisfy.getName()+": "+clct.getOmissionCost(toSatisfy));
						cost = cost + clct.getOmissionCost(toSatisfy);
					}
					costTable.put(role, cost);
					// System.out.println(" = "+cost);
				}
			}
		}

		public double getConstituentsToSatisfyCost(Role role) {
			return costTable.get(role);
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"Constituents To Satisfy Cost Table\n-----------------------------------------\n");
			for (Role role : costTable.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName()).append(" : ")
						.append(costTable.get(role)).append("\n");
			}
			return sb.toString();
		}
	}

	public static class ParamLineParser {

		public static class ParamContainer {
			public String structureName;
			public String role = null;
			public List<Pair<String, Double>> params;
		}

		public static ParamContainer parseLine(String paramLine) {
			StringTokenizer st = new StringTokenizer(paramLine);
			ParamContainer pc = new ParamContainer();
			String firstElement = st.nextToken();
			if (firstElement.indexOf(".") > -1) {
				String[] structureAndRole = firstElement.split("\\.");
				pc.structureName = structureAndRole[0];
				pc.role = structureAndRole[1];
			}
			else {
				pc.structureName = firstElement;
			}
			pc.params = new ArrayList<Pair<String, Double>>();
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.indexOf(":") > -1) {
					String[] fillerAndParam = token.split(":");
					pc.params.add(new Pair<String, Double>(fillerAndParam[0], Double.parseDouble(fillerAndParam[1])));
				}
			}
			return pc;
		}
	}

	public static class ParamFileConstituentExpansionCostTableCFG implements ConstituentExpansionCostTable {
		Map<String, Counter<Construction>> table = new IdentityHashMap<String, Counter<Construction>>();

		public ParamFileConstituentExpansionCostTableCFG(LCPGrammarWrapper grammar, String paramFile) throws IOException {
			this(grammar, new TextFileLineIterator(paramFile));
		}

		public ParamFileConstituentExpansionCostTableCFG(LCPGrammarWrapper grammar, TextFileLineIterator lineIterator) {
			while (lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == "") {
					continue;
				}
				ParamLineParser.ParamContainer pc = ParamLineParser.parseLine(line);
				Counter<Construction> counter = new Counter<Construction>();
				Construction key = grammar.getConstruction(pc.structureName);
				if (key == null) {
					// throw new ParserException("Unknown cxntype: "+key.getName()+ " in params file");
				}
				else {
					table.put(key.getName(), counter);
					for (Pair<String, Double> pair : pc.params) {
						Construction fillerType = grammar.getConstruction(pair.getFirst());
						if (fillerType == null) {
							// throw new ParserException("Unknown cxn: "+pair.getFirst()+ " in params file");
						}
						else {
							counter.setCount(fillerType, Math.log(pair.getSecond()));
						}
					}
				}
			}
		}

		public double getConstituentExpansionCost(Role role, Construction cxn) {
			if (role.getTypeConstraint() == null) {
				throw new ParserException("Role " + role.getName() + " does not have a typeconstraint!");
			}
			if (table.get(role.getTypeConstraint().getType()) != null
					&& table.get(role.getTypeConstraint().getType()).containsKey(cxn)) {
				return table.get(role.getTypeConstraint().getType()).getCount(cxn);
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"ParamFile Constituent Expansion Table CFG\n----------------------------------------------------\n");
			for (String cxn : table.keySet()) {
				sb.append(cxn).append(": ");
				for (Construction sub : table.get(cxn).keySet()) {
					sb.append(sub.getName()).append("[").append(table.get(cxn).getCount(sub)).append("]  ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}

	}

	public static class ParamFileConstituentExpansionCostTable implements ConstituentExpansionCostTable {
		IdentityHashMap<Role, Counter<Construction>> roleToConstructionCounter = new IdentityHashMap<Role, Counter<Construction>>();

		public ParamFileConstituentExpansionCostTable(LCPGrammarWrapper grammar, String paramFile) throws IOException {
			this(grammar, new TextFileLineIterator(paramFile));
		}

		public ParamFileConstituentExpansionCostTable(LCPGrammarWrapper grammar, TextFileLineIterator lineIterator) {
			while (lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == "") {
					continue;
				}
				ParamLineParser.ParamContainer pc = ParamLineParser.parseLine(line);
				Role constituent = getRole(grammar, pc.structureName, pc.role);
				if (constituent == null) {
					// throw new ParserException("Locality parameter references unknown constituent " + pc.role + " in " +
					// pc.structureName);
				}
				else {
					Counter<Construction> counter = new Counter<Construction>();
					roleToConstructionCounter.put(constituent, counter);
					for (Pair<String, Double> pair : pc.params) {
						Construction fillerType = grammar.getConstruction(pair.getFirst());
						if (fillerType == null) {
							// throw new ParserException("Unknown cxn: " + fillerType.getName() + " in params file");
						}
						else {
							counter.setCount(fillerType, Math.log(pair.getSecond()));
						}
					}
				}
			}
		}

		static Role getRole(LCPGrammarWrapper grammar, String constructionName, String roleName) {
			Construction cxn = grammar.getConstruction(constructionName);
			if (cxn == null) {
				return null;
			}
			else {
				// for (Role role : cxn.getAllRoles()){ //somehow this doesn't work -- a role with same name but different
				// memory address is retrieved
				for (Role role : cxn.getConstructionalBlock().getElements()) {
					if (role.getName().equals(roleName)) {
						return role;
					}
				}
			}
			return null;
		}

		public double getConstituentExpansionCost(Role role, Construction cxn) {
			if (roleToConstructionCounter.get(role) != null && roleToConstructionCounter.get(role).containsKey(cxn)) {
				return roleToConstructionCounter.get(role).getCount(cxn);
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"ParamFile Constituent Expansion Table\n----------------------------------------------------\n");
			for (Role role : roleToConstructionCounter.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName()).append(": ");
				for (Construction cxn : roleToConstructionCounter.get(role).keySet()) {
					sb.append(cxn.getName()).append("[").append(getConstituentExpansionCost(role, cxn)).append("]  ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public static class ParamFileConstituentExpansionCostTableFromCounts implements ConstituentExpansionCostTable {

		public static final double COUNT_AS_ZERO_THRESHOLD = 1.0;

		Map<Role, Counter<Construction>> expansionCostTable = new IdentityHashMap<Role, Counter<Construction>>();
		LCPGrammarWrapper grammar = null;
		UnifyTable unifyTable = null;
		boolean useCFGBackoff = true;

		public ParamFileConstituentExpansionCostTableFromCounts(LCPGrammarWrapper grammar, String paramFile,
				boolean useCFGbackoff, UnifyTable unifyTable) throws IOException {
			this(grammar, new TextFileLineIterator(paramFile), useCFGbackoff, unifyTable);
		}

		public ParamFileConstituentExpansionCostTableFromCounts(LCPGrammarWrapper grammar,
				TextFileLineIterator lineIterator, boolean useCFGbackoff, UnifyTable unifyTable) {

			this.grammar = grammar;
			this.unifyTable = unifyTable;
			this.useCFGBackoff = useCFGbackoff;

			Map<Role, Counter<Construction>> counts = new IdentityHashMap<Role, Counter<Construction>>();
			Map<String, Counter<Construction>> backoffCounts = new IdentityHashMap<String, Counter<Construction>>();

			while (lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == "") {
					continue;
				}
				ParamLineParser.ParamContainer pc = ParamLineParser.parseLine(line);
				Role constituent = ParamFileConstituentExpansionCostTable.getRole(grammar, pc.structureName, pc.role);
				if (constituent != null) {

					// update counts
					Counter<Construction> counter = new Counter<Construction>();
					counts.put(constituent, counter);

					Counter<Construction> cfgCounter = null;
					if (useCFGbackoff) {
						String type = constituent.getTypeConstraint().getType();
						if (!backoffCounts.containsKey(type)) {
							backoffCounts.put(type, new Counter<Construction>());
						}
						cfgCounter = backoffCounts.get(type);
					}

					for (Pair<String, Double> pair : pc.params) {
						Construction fillerType = grammar.getConstruction(pair.getFirst());
						if (fillerType != null) {
							counter.setCount(fillerType, pair.getSecond());
							if (useCFGbackoff) {
								cfgCounter.incrementCount(fillerType, pair.getSecond());
							}
						}
					}

				}
			}

			tabulate(counts, backoffCounts);
		}

		protected List<Construction> getUnifiableSubtypes(Role role) {
			List<Construction> unifiableSubtypes = new ArrayList<Construction>(grammar.getRules(role.getTypeConstraint()
					.getType()));

			Iterator<Construction> subtypeIter = unifiableSubtypes.listIterator();
			while (subtypeIter.hasNext()) {
				Construction fillerType = subtypeIter.next();
				if (!unifyTable.unifies(role, fillerType)) {
					subtypeIter.remove();
				}
			}
			return unifiableSubtypes;
		}

		protected Map<String, Counter<Construction>> smoothCFGProb(Map<String, Counter<Construction>> backoffCounts,
				String roleType, List<Construction> unifiableSubtypes) {

			Map<String, Counter<Construction>> cfgProb = new IdentityHashMap<String, Counter<Construction>>();

			Counter<Construction> counter = backoffCounts.get(roleType);
			cfgProb.put(roleType, new Counter<Construction>());

			if (backoffCounts.containsKey(roleType)) {

				// smooth the CFG backoff table (particular to the unifiable type constraints)
				double N = 0; // N = # observed tokens
				double T = 0; // T = # observed types

				for (Construction c : counter.keySet()) {
					T++;
					N += counter.getCount(c);
				}
				double V = unifiableSubtypes.size();
				double alpha = N == 0 ? 0.0 : N / (N + T);

				for (Construction subtype : unifiableSubtypes) {
					double smoothedProb = N == 0 ? 1 / V : alpha * counter.getCount(subtype) / N + (1 - alpha) * 1 / V;
					cfgProb.get(roleType).setCount(subtype, smoothedProb);
				}
			}
			else {
				for (Construction subtype : unifiableSubtypes) {
					cfgProb.get(roleType).setCount(subtype, 1.0 / unifiableSubtypes.size());
				}
			}

			return cfgProb;
		}

		protected void tabulate(Map<Role, Counter<Construction>> counts, Map<String, Counter<Construction>> backoffCounts) {

			for (Construction cxn : grammar.getAllConcretePhrasalConstructions()) {
				for (Role role : cxn.getConstructionalBlock().getElements()) {

					expansionCostTable.put(role, new Counter<Construction>());
					List<Construction> unifiableSubtypes = getUnifiableSubtypes(role);
					String roleType = role.getTypeConstraint().getType();

					Map<String, Counter<Construction>> cfgProb = smoothCFGProb(backoffCounts, roleType, unifiableSubtypes);

					if (counts.containsKey(role)) {
						// backoff to uniform or cfg table if a particular filler type has not been observed
						double N = 0;
						double T = 0;
						Counter<Construction> counter = counts.get(role);

						for (Construction c : counter.keySet()) {
							if (unifiableSubtypes.contains(c)) {
								T++;
								N += counter.getCount(c);
							}
						}
						double V = unifiableSubtypes.size();
						double alpha = N == 0 ? 0.0 : N / (N + T);

						for (Construction subtype : unifiableSubtypes) {

							double smoothedProb;
							if (!useCFGBackoff) {
								smoothedProb = N == 0 ? 1 / V : alpha * counter.getCount(subtype) / N + (1 - alpha) * 1 / V;
							}
							else {
								smoothedProb = N == 0 ? cfgProb.get(roleType).getCount(subtype) : alpha
										* counter.getCount(subtype) / N + (1 - alpha) * cfgProb.get(roleType).getCount(subtype);
							}
							expansionCostTable.get(role).setCount(subtype, Math.log(smoothedProb));
						}

					}
					else {
						// role has not been observed (special case where N = T = 0), so just use CFG table
						for (Construction subtype : unifiableSubtypes) {
							if (!useCFGBackoff) {
								expansionCostTable.get(role).setCount(subtype, Math.log(1.0 / unifiableSubtypes.size()));
							}
							else {
								expansionCostTable.get(role).setCount(subtype,
										Math.log(cfgProb.get(roleType).getCount(subtype)));
							}
						}
					}
				}
			}

		}

		public double getConstituentExpansionCost(Role role, Construction cxn) {
			if (expansionCostTable.get(role) != null && expansionCostTable.get(role).containsKey(cxn)) {
				return expansionCostTable.get(role).getCount(cxn);
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(
					"ParamFile Constituent Expansion Table With Backoff\n----------------------------------------------------\n");
			for (Role role : expansionCostTable.keySet()) {
				sb.append(((Construction) role.getContainer()).getName()).append(".").append(role.getName()).append(": ");
				for (Construction cxn : expansionCostTable.get(role).keySet()) {
					sb.append(cxn.getName()).append("[").append(expansionCostTable.get(role).getCount(cxn)).append("]  ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public static void main(String[] args) throws IOException, TypeSystemException {
		Grammar ecgGrammar = ECGGrammarUtilities.read(args[0]);
		Prefs p = ecgGrammar.getPrefs();
		if (!(p instanceof AnalyzerPrefs)) {
			throw new ParserException("AnalyzerPrefs object expected");
		}
		AnalyzerPrefs prefs = (AnalyzerPrefs) p;

		LCPGrammarWrapper grammar = new LCPGrammarWrapper(ecgGrammar);
		System.out.println("Begin table building");
		// System.out.println(grammar);
		CloneTable ct = new CloneTable(grammar, new BasicAnalysisFactory());
		System.out.println("done with clone table");
		UnifyTable ut = new UnifyTable(grammar, ct);
		// CanonicalSemanticSlotChainFinder csscf = new CanonicalSemanticSlotChainFinder(grammar, ct);
		// System.out.println(csscf);
		ConstituentsToSatisfyTable ctst = new ConstituentsToSatisfyTable(grammar);
		System.out.println(ctst);
		TypeToConstituentsTable ttct = new TypeToConstituentsTable(grammar, ut);
		System.out.println(ttct.toString());

		ConstituentExpansionCostTable cect = new DumbConstituentExpansionCostTable(grammar);
		if (prefs.getList(AP.GRAMMAR_PARAMS_PATHS).size() > 0) {

			String grammarParamsCxnExt = prefs.getSetting(AP.GRAMMAR_PARAMS_CXN_EXTENSION) == null ? "cxn" : prefs
					.getSetting(AP.GRAMMAR_PARAMS_CXN_EXTENSION);

			boolean useBackoff = prefs.getSetting(AP.GRAMMAR_PARAMS_USE_CFGBACKOFF) == null ? true : Boolean.valueOf(prefs
					.getSetting(AP.GRAMMAR_PARAMS_USE_CFGBACKOFF));

			List<File> paramFiles = FileUtils.getFilesUnder(prefs.getBaseDirectory(),
					prefs.getList(AP.GRAMMAR_PARAMS_PATHS), new ExtensionFileFilter(grammarParamsCxnExt));

			if (paramFiles.isEmpty()) {
				cect = new DumbConstituentExpansionCostTable(grammar);
			}
			else {
				String paramsType = prefs.getSetting(AP.GRAMMAR_PARAMS_TYPE) == null ? "cfg" : prefs
						.getSetting(AP.GRAMMAR_PARAMS_TYPE);
				if (paramsType.equalsIgnoreCase("normal")) {
					cect = new ParamFileConstituentExpansionCostTable(grammar, paramFiles.get(0).getAbsoluteFile()
							.getAbsolutePath());
				}
				else if (paramsType.equalsIgnoreCase("cfg")) {
					cect = new ParamFileConstituentExpansionCostTableCFG(grammar, paramFiles.get(0).getAbsoluteFile()
							.getAbsolutePath());
				}
				else if (paramsType.equalsIgnoreCase("counts")) {
					cect = new ParamFileConstituentExpansionCostTableFromCounts(grammar, paramFiles.get(0).getAbsoluteFile()
							.getAbsolutePath(), useBackoff, ut);
				}
				else { // default
					cect = new ParamFileConstituentExpansionCostTableCFG(grammar, paramFiles.get(0).getAbsoluteFile()
							.getAbsolutePath());
				}
			}
		}
		System.out.println(cect);
		ConstituentLocalityCostTable clct = new ConstituentLocalityCostTable(grammar);
		ConstituentsToSatisfyCostTable ctsct = new ConstituentsToSatisfyCostTable(grammar, ctst, clct);
		ReachabilityTable rt = new ReachabilityTable(grammar, ctsct, cect, ut, clct);
		System.out.println(rt);

	}

}

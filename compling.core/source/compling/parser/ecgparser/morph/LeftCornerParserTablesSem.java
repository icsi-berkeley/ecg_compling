package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement.Binding;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.BasicAnalysisFactory;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.CloneTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentExpansionCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentLocalityCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentsToSatisfyCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentsToSatisfyTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.DumbConstituentExpansionCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ReachabilityTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.UnifyTable;
import compling.util.Pair;

public class LeftCornerParserTablesSem {

	public static class CanonicalSemanticSlotChainFinder {
		Map<Construction, Set<String>> canonicalRoles;
		Map<Construction, Map<SlotChain, List<Pair<TypeConstraint, Role>>>> canonicalRoleTypes = new HashMap<Construction, Map<SlotChain, List<Pair<TypeConstraint, Role>>>>();

		public boolean isCanonicalRole(Construction cxn, String chain) {
			return canonicalRoles.containsKey(cxn) && canonicalRoles.get(cxn).contains(chain);
		}

		public CanonicalSemanticSlotChainFinder(LCPGrammarWrapper grammar, CloneTable ct) {
			canonicalRoles = new HashMap<Construction, Set<String>>();
			for (Construction cxn : grammar.getAllConstructions()) {
				canonicalRoles.put(cxn, new HashSet<String>());
				// System.out.println(cxn.getName());
				List<String> chains = new ArrayList<String>();
				Analysis a = ct.get(cxn);
				for (HashSet<SlotChain> scs : new SlotChainTracker(a).getAllCoindexedSlotChains()) {
					List<Role> winnerChain = null;
					int winnerChainLength = 1000;
					int winnerChainStringLength = 10000;
					// System.out.print("\t");
					for (SlotChain chain : scs) {
						String ch = chain.toString();
						// System.out.println("<-->"+ch);
						if (chain.getChain().size() > 1) { /*
																		 * ||
																		 * chain.getChain().size()
																		 * ==2 &&
																		 * chain.getChain().
																		 * get(1).
																		 * getName().equals("m")
																		 */
							if (!constructionalRole(cxn, chain.getChain().get(1).getName()) && chain.getChain().size() >= 2) {
								if (winnerChain == null) {
									winnerChain = chain.getChain();
									winnerChainLength = chain.getChain().size();
									winnerChainStringLength = ch.length();
								}
								else {
									if (chain.getChain().get(1).getName().equals("m")) {
										if (chain.getChain().size() < winnerChainLength
													|| !winnerChain.get(1).getName().equals("m")
													|| chain.getChain().size() == winnerChainLength
													&& winnerChain.get(1).getName().equals("m")
													&& winnerChainStringLength > ch.length()) {
											winnerChain = chain.getChain();
											winnerChainLength = chain.getChain().size();
											winnerChainStringLength = ch.length();
										}
									}
									else { // !chain.getChain().get(1).getName().equals("m")
										if (!winnerChain.get(1).getName().equals("m")) {
											if (chain.getChain().size() < winnerChainLength
														|| chain.getChain().size() == winnerChainLength
														&& winnerChainStringLength > ch.length()) {
												winnerChain = chain.getChain();
												winnerChainLength = chain.getChain().size();
												winnerChainStringLength = ch.length();
											}
										}
										else { // winnerChain.get(1).getName().equals("m")
													// &&
													// !chain.getChain().get(1).getName().equals("m")
											// do nothing
										}
									}
								}
							}
						}

					}
					if (winnerChain != null) {
						SlotChain sc = new SlotChain(makeStringList(winnerChain.subList(1, winnerChain.size())));
						canonicalRoles.get(cxn).add(sc.toString());
						FeatureStructureSet fss = a.getFeatureStructure();
						List<Pair<TypeConstraint, Role>> rolesThatPointToSC = new LinkedList<Pair<TypeConstraint, Role>>();
						Slot goal = fss.getSlot(sc);
						for (Slot parent : fss.getSlots()) {
							if (parent.hasStructuredFiller() && parent.getTypeConstraint() != null
										&& parent.getTypeConstraint().getTypeSystem() != grammar.getCxnTypeSystem()) {
								for (Role role : parent.getFeatures().keySet()) {
									if (goal == parent.getSlot(role)
												&& role.getName().equals(sc.getChain().get(sc.getChain().size() - 1).getName())) {
										rolesThatPointToSC.add(new Pair<TypeConstraint, Role>(parent.getTypeConstraint(), role));
										// System.out.println("In cxn: "+cxn.getName()+" for slotchain "+sc+" we have a parent role type of "+parent.getTypeConstraint()+"."+role);
									}
								}
							}
						}
						if (rolesThatPointToSC.size() > 0) {
							if (canonicalRoleTypes.get(cxn) == null) {
								canonicalRoleTypes.put(cxn, new HashMap<SlotChain, List<Pair<TypeConstraint, Role>>>());
							}
							canonicalRoleTypes.get(cxn).put(sc, rolesThatPointToSC);
						}
					}

				}

			}
		}

		private List<String> makeStringList(List<Role> chain) {
			List<String> ret = new ArrayList<String>();
			for (Role r : chain) {
				ret.add(r.getName());
			}
			return ret;
		}

		private boolean constructionalRole(Construction c, String roleName) {
			for (Role r : c.getConstructionalBlock().getElements()) {
				if (r.getName().equals(roleName)) {
					return true;
				}
			}
			return false;
		}

		public Set<Construction> getKeys() {
			return canonicalRoles.keySet();
		}

		public Set<String> getStringChains(Construction cxn) {
			return canonicalRoles.get(cxn);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("Canonical Roles Tables:\n");
			for (Construction c : canonicalRoles.keySet()) {
				sb.append("\t").append(c.getName()).append(": ");
				for (String s : canonicalRoles.get(c)) {
					sb.append(s).append(" ");
				}
				sb.append("\n");
			}
			return sb.toString();
		}

	}

	public static class SlotChainTracker {
		private HashMap<Integer, HashSet<SlotChain>> allCoindexedPaths = new HashMap<Integer, HashSet<SlotChain>>();
		private Map<SlotChain, Integer> slotChainToIndex = new HashMap<SlotChain, Integer>();
		private HashMap<Integer, HashSet<Pair<List<String>, Integer>>> descendentPaths = new HashMap<Integer, HashSet<Pair<List<String>, Integer>>>();
		private HashSet<Slot> structureTracker;

		public SlotChainTracker(Analysis a) {
			this(a.getFeatureStructure(), false);
		}

		public SlotChainTracker(FeatureStructureSet fss) {
			this(fss, false);
		}

		public SlotChainTracker(FeatureStructureSet fss, boolean includeSlotNumber) {
			Set<Slot> rootSlots = fss.getRootSlots();
			for (Slot rootSlot : rootSlots) {
				String startPath = rootSlot.getTypeConstraint().getType();
				if (includeSlotNumber) {
					startPath += "[" + String.valueOf(rootSlot.getSlotIndex()) + "]";
				}
				// System.out.println("Rec find all paths:"+path);
				structureTracker = new HashSet<Slot>();
				List<String> path = new ArrayList<String>();
				path.add(startPath);
				findAllCoindexedPaths(rootSlot, path, fss);
			}
		}

		// this method finds all non-recursive paths that are co-indexed
		private void findAllCoindexedPaths(Slot slot, List<String> path, FeatureStructureSet fss) {
			// System.out.println(path+"; ");
			SlotChain sc = new SlotChain(path);
			slotChainToIndex.put(sc, slot.getSlotIndex());
			addAllDescendentPaths(path, slot.getSlotIndex(), -1, fss);

			if (!structureTracker.contains(slot)) {
				if (!allCoindexedPaths.containsKey(slot.getSlotIndex())) {
					allCoindexedPaths.put(slot.getSlotIndex(), new HashSet<SlotChain>());
				}
				allCoindexedPaths.get(slot.getSlotIndex()).add(sc);
			}
			else if (structureTracker.contains(slot)) {
				// System.out.println("\tfound already as: "+allCoindexedPaths.get(slot.getSlotIndex()));
				allCoindexedPaths.get(slot.getSlotIndex()).add(sc);

				if (descendentPaths.containsKey(slot.getSlotIndex())) {
					// System.out.println("\tfound in descendentPaths");
					Set<Pair<List<String>, Integer>> copyOfDesc = (Set<Pair<List<String>, Integer>>) descendentPaths.get(
								slot.getSlotIndex()).clone();
					for (Pair<List<String>, Integer> p : descendentPaths.get(slot.getSlotIndex())) {
						// System.out.println("\t descendent path"+p.getFirst());
						List<String> pathClone = (List<String>) ((ArrayList<String>) path).clone();
						pathClone.addAll(p.getFirst());
						SlotChain scp = new SlotChain(pathClone);
						// System.out.println("\t"+scp);
						slotChainToIndex.put(scp, p.getSecond());
						allCoindexedPaths.get(p.getSecond()).add(scp);
						addAllDescendentPaths(pathClone, p.getSecond(), slot.getSlotIndex(), fss);
					}
				}

				return;
			}
			structureTracker.add(slot);

			if (slot.hasFiller() && slot.hasStructuredFiller()) {
				Set<Role> slotNames = slot.getFeatures().keySet();
				for (Role slotName : slotNames) {
					Slot nextSlot = slot.getSlot(slotName);
					List<String> newPath = (List<String>) ((ArrayList<String>) path).clone();
					newPath.add(slotName.toString());
					findAllCoindexedPaths(nextSlot, newPath, fss);
				}
			}
		}

		private void addAllDescendentPaths(List<String> path, int slotIndex, int sourceIndex, FeatureStructureSet fss) {
			for (int i = 1; i < path.size(); i++) {
				SlotChain parentPath = new SlotChain(path.subList(0, i));
				// if (sourceIndex != -1)
				// {System.out.print("\tparentpath:"+parentPath);}
				if (slotChainToIndex.get(parentPath) == null) {
					SlotChain realPath = new SlotChain();
					realPath.setChain(parentPath.getChain().subList(1, parentPath.getChain().size()));
					if (!fss.hasSlot(fss.getMainRoot(), realPath)) {
						throw new ParserException("\nthis is very bad.\n" + realPath + "\n" + fss.toString());
					}
					int newIndex = fss.getSlot(realPath).getSlotIndex();
					slotChainToIndex.put(parentPath, newIndex);
					allCoindexedPaths.get(newIndex).add(parentPath);
					descendentPaths.put(newIndex, new HashSet<Pair<List<String>, Integer>>());
				}
				if (!descendentPaths.containsKey(slotChainToIndex.get(parentPath))) {
					descendentPaths.put(slotChainToIndex.get(parentPath), new HashSet<Pair<List<String>, Integer>>());
				}
				if (slotChainToIndex.get(parentPath) != sourceIndex) {
					descendentPaths.get(slotChainToIndex.get(parentPath)).add(
								new Pair<List<String>, Integer>(path.subList(i, path.size()), slotIndex));

				}
				else if (slotChainToIndex.get(parentPath) == sourceIndex
							&& !descendentPaths.get(slotChainToIndex.get(parentPath)).contains(
										new Pair<List<String>, Integer>(path.subList(i, path.size()), slotIndex))) {
					// System.out.print("  this is the weird recursive case where a repeat enters the fray and causes a concurrent mod exception.");
				}
				// if (sourceIndex != -1)
				// {System.out.println("   ---> done  "+path.subList(i,
				// path.size()));}
			}
		}

		public List<HashSet<SlotChain>> getAllCoindexedSlotChains() {
			ArrayList<HashSet<SlotChain>> al = new ArrayList<HashSet<SlotChain>>();
			for (Integer key : allCoindexedPaths.keySet()) {
				al.add(allCoindexedPaths.get(key));
			}
			return al;
		}

		public HashSet<SlotChain> getAllCoindexedSlotChainsBySlot(Slot slot) {
			return allCoindexedPaths.get(slot.getSlotIndex());
		}

	}

	public static class SlotChainTables {
		Map<Construction, List<SlotChain>> canonicalChains = new IdentityHashMap<Construction, List<SlotChain>>();
		Map<Construction, List<SlotChain>> RDChains = new IdentityHashMap<Construction, List<SlotChain>>();
		Map<Role, SlotChain> meaningPoleChain;
		Map<Construction, List<List<TypeConstraint>>> canonicalChainTypes = new IdentityHashMap<Construction, List<List<TypeConstraint>>>();
		Map<Construction, Map<SlotChain, List<Pair<TypeConstraint, Role>>>> canonicalRoleTypes;

		public List<SlotChain> getRDChains(Construction cxn) {
			return RDChains.get(cxn);
		}

		public List<SlotChain> getCanonicalChains(Construction cxn) {
			return canonicalChains.get(cxn);
		}

		public List<Pair<TypeConstraint, Role>> getFrameRoles(Construction cxn, SlotChain sc) {
			if (canonicalRoleTypes.get(cxn) != null) {
				return canonicalRoleTypes.get(cxn).get(sc);
			}
			return null;
		}

		public SlotChainTables(LCPGrammarWrapper g, CloneTable ct) {
			CanonicalSemanticSlotChainFinder csscf = new CanonicalSemanticSlotChainFinder(g, ct);
			canonicalRoleTypes = csscf.canonicalRoleTypes;
			for (Construction cxn : csscf.getKeys()) {
				canonicalChains.put(cxn, new ArrayList<SlotChain>());
				RDChains.put(cxn, new ArrayList<SlotChain>());
				for (String chain : csscf.getStringChains(cxn)) {
					// System.out.println(cxn.getName()+" ->"+chain);
					canonicalChains.get(cxn).add(new SlotChain(chain));
				}
				Analysis a = ct.get(cxn);
				// System.out.println(a);
				SlotChainTracker sct = new SlotChainTracker(a);
				for (Slot slot : a.getFeatureStructure().getSlots()) {
					TypeConstraint tc = slot.getTypeConstraint();
					if (tc != null && tc.getType().equals("RD")) {
						// System.out.println(tc.getType());
						List<SlotChain> canonicals = canonicalChains.get(cxn);
						// System.out.println(canonicals);
						Set<SlotChain> rdc = sct.getAllCoindexedSlotChainsBySlot(slot);
						Set<SlotChain> alteredRDC = new HashSet<SlotChain>();
						for (SlotChain sc : rdc) {
							SlotChain temp = new SlotChain();
							temp.setChain(sc.getChain().subList(1, sc.getChain().size()));
							alteredRDC.add(temp);
						}

						// System.out.println(alteredRDC);

						alteredRDC.retainAll(canonicals);
						if (alteredRDC.size() > 1) {
							throw new RuntimeException("What the? rdc.size() = " + rdc.size());
						}
						else if (alteredRDC.size() == 1) {
							for (SlotChain theSingleChain : alteredRDC) {
								RDChains.get(cxn).add(theSingleChain);
							}
						}
					}
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("SlotChainTables:\n");
			for (Construction cxn : canonicalChains.keySet()) {
				sb.append("\t").append(cxn.getName()).append("\n\t\tCanonical Chains:\n");
				for (SlotChain sc : canonicalChains.get(cxn)) {
					sb.append("\t\t\t").append(sc.toString()).append("\n");
				}
				sb.append("\t\tRD Chains:\n");
				for (SlotChain sc : RDChains.get(cxn)) {
					sb.append("\t\t\t").append(sc.toString()).append("\n");
				}
				sb.append("\n");
			}
			sb.append("\n");
			return sb.toString();
		}

	}

	public static class SlotConnectionTracker {
		Map<Role, Map<Construction, BindingArrangementRecord>> tempTable;
		Map<Role, Map<Construction, List<BindingArrangement>>> table;
		Map<Role, Map<Construction, BindingArrangement>> directConnectTable;

		class BindingArrangementRecord {

			List<List<BindingArrangement>> l = new ArrayList<List<BindingArrangement>>();

			List<BindingArrangement> get(int dist) {
				if (l.size() <= dist) {
					return new LinkedList<BindingArrangement>();
				}
				return l.get(dist);
			}

			void addBindingArrangement(int dist, BindingArrangement ba) {
				if (l.size() <= dist) {
					while (l.size() <= dist) {
						l.add(new ArrayList<BindingArrangement>());
					}
				}
				l.get(dist).add(ba);
			}

			int getMaxDist() {
				return l.size();
			}
		}

		BindingArrangementRecord tempTableGet(Role role, Construction dest) {
			if (tempTable.get(role).get(dest) == null) {
				tempTable.get(role).put(dest, new BindingArrangementRecord());
			}
			return tempTable.get(role).get(dest);
		}

		List<BindingArrangement> tableGet(Role role, Construction dest) {
			if (table.get(role).get(dest) == null) {
				table.get(role).put(dest, new ArrayList<BindingArrangement>());
			}
			return table.get(role).get(dest);
		}

		private void setup(LCPGrammarWrapper g, CloneTable ct, UnifyTable ut, SlotChainTables sct,
					ConstituentsToSatisfyCostTable ctsct, ConstituentExpansionCostTable cect,
					ConstituentLocalityCostTable clct) {
			tempTable = new IdentityHashMap<Role, Map<Construction, BindingArrangementRecord>>();
			table = new IdentityHashMap<Role, Map<Construction, List<BindingArrangement>>>();
			directConnectTable = new IdentityHashMap<Role, Map<Construction, BindingArrangement>>();

			for (Construction source : g.getAllConcretePhrasalConstructions()) {
				for (Role role : source.getConstructionalBlock().getElements()) {
					tempTable.put(role, new HashMap<Construction, BindingArrangementRecord>());
					table.put(role, new HashMap<Construction, List<BindingArrangement>>());
					directConnectTable.put(role, new HashMap<Construction, BindingArrangement>());
					// for (Construction dest : g.getAllConstructions()){
					// if (dest.isConcrete() /*&& ut.unifies(role, dest)*/ ){
					// tempTable.get(role).put(dest, new BindingArrangementRecord());
					// table.get(role).put(dest, new
					// ArrayList<BindingArrangement>());
					// }
					// }
					// for (Construction sub :
					// g.getRules(role.getTypeConstraint().getType())){
					for (String subName : g.getAllSubtypes(role.getTypeConstraint().getType())) {
						Construction sub = g.getConstruction(subName);
						// if (ut.unifies(role, sub)){
						Analysis a = ct.get(source);
						double cec = cect.getConstituentExpansionCost(role, sub);
						if (cec > Double.NEGATIVE_INFINITY && a.advance(role, ct.get(sub))) {
							Map<SlotChain, Slot> slotToChain = new HashMap<SlotChain, Slot>();
							FeatureStructureSet fs = a.getFeatureStructure();
							for (SlotChain sc : sct.getCanonicalChains(source)) {
								slotToChain.put(sc, fs.getSlot(sc));
							}
							Set<Binding> matchingBindings = new HashSet<Binding>();
							for (SlotChain csc : sct.getCanonicalChains(sub)) {
								SlotChain modSC = new ECGSlotChain(role.getName() + ".", csc);
								Slot slot = fs.getSlot(modSC);
								for (SlotChain psc : slotToChain.keySet()) {
									if (slotToChain.get(psc) == slot) {
										// System.out.println("MATCH!!!    "+source.getName()+"."+role.getName()+"\t\t"+sub.getName()+"\t\t"+
										// source.getName()+"."+psc+" <--> "+source.getName()+"."+modSC+"   "+sub.getName());
										matchingBindings.add(new Binding(psc, csc));
									}

								}
							}
							BindingArrangement ba = new BindingArrangement(0, source, role, sub, matchingBindings);
							ba.logAddLikelihood(clct.getLocalCost(role) + cect.getConstituentExpansionCost(role, sub));
							ba.addDescendentParentRole(role);
							// tempTable.get(role).get(sub).addBindingArrangement(0,
							// ba);
							tempTableGet(role, sub).addBindingArrangement(0, ba);
							directConnectTable.get(role).put(sub, ba);
						}
					}
				}
			}
		}

		public SlotConnectionTracker(LCPGrammarWrapper g, CloneTable ct, UnifyTable ut, SlotChainTables sct,
					ConstituentsToSatisfyCostTable ctsct, ConstituentExpansionCostTable cect,
					ConstituentLocalityCostTable clct) {
			int MAXDIST = 10;
			setup(g, ct, ut, sct, ctsct, cect, clct);

			for (int i = 1; i < MAXDIST; i++) {
				// System.out.println("-----------------------------------------------------------------------------------------------------\n"+"i="+i+"\n----");
				for (Construction source : g.getAllConcretePhrasalConstructions()) {

					for (Role role : source.getConstructionalBlock().getElements()) {

						// System.out.println(source.getName()+"."+role.getName());

						// for (Construction sub :
						// g.getRules(role.getTypeConstraint().getType())){
						// for (String subName :
						// g.getAllSubtypes(role.getTypeConstraint().getType())){
						for (Construction sub : tempTable.get(role).keySet()) {
							// Construction sub = g.getConstruction(subName);
							// double subCEC = cect.getConstituentExpansionCost(role,
							// sub);
							// if (subCEC == Double.NEGATIVE_INFINITY){continue;}
							for (Role subrole : sub.getConstructionalBlock().getElements()) {

								// System.out.println("\t"+sub.getName()+"."+subrole.getName());

								// for (Construction desc :
								// g.getRules(subrole.getTypeConstraint().getType())){
								// for (String descName :
								// g.getAllSubtypes(subrole.getTypeConstraint().getType())){
								// System.out.println(subrole);
								if (tempTable.get(subrole) == null) {
									continue;
								}
								for (Construction desc : tempTable.get(subrole).keySet()) {
									// Construction desc = g.getConstruction(descName);
									// double descCEC =
									// cect.getConstituentExpansionCost(subrole, desc);

									// if (descCEC ==
									// Double.NEGATIVE_INFINITY){continue;}
									// System.out.println("\t"+sub.getName()+"."+subrole.getName()+"    proposed filler:"+desc.getName());
									// BindingArrangementRecord ancBindingRecord =
									// tempTable.get(role).get(sub);
									BindingArrangementRecord descBindingRecord = tempTableGet(subrole, desc);
									BindingArrangementRecord ancBindingRecord = tempTableGet(role, sub);
									List<BindingArrangement> newBAs = new ArrayList<BindingArrangement>();
									for (BindingArrangement ancBA : ancBindingRecord.get(0)) {
										for (BindingArrangement descBA : descBindingRecord.get(i - 1)) {
											BindingArrangement newBA = ancBA.makeConnectedArrangement(descBA);
											newBA.logAddLikelihood(ancBA.getLogLikelihood() + descBA.getLogLikelihood() +
											// cect.getConstituentExpansionCost(role, sub)
											// + don't need this cuz it's in the ancBA
														ctsct.getConstituentsToSatisfyCost(subrole));
											// cect.getConstituentExpansionCost(subrole,
											// desc)); //I don't need this cuz it's
											// already in the descBA
											newBAs.add(newBA);
										}
									}
									for (BindingArrangement ba : newBAs) {
										// System.out.println(ba);
										boolean foundAndMerged = false;
										for (BindingArrangement possibleMatch : ancBindingRecord.get(i)) {
											if (possibleMatch.equals(ba)) {
												possibleMatch.logAddLikelihood(ba.getLogLikelihood());
												possibleMatch.addAllDescendentParentRoles(ba.getDescendentParentRoles());
												foundAndMerged = true;
												break;
											}
										}

										if (!foundAndMerged) {
											ancBindingRecord.addBindingArrangement(i, ba);
										}

									}
								}
							}
						}
					}
				}
			}
			for (Role roleKey : tempTable.keySet()) {
				for (Construction cxnKey : tempTable.get(roleKey).keySet()) {
					BindingArrangementRecord bar = tempTable.get(roleKey).get(cxnKey);
					for (int i = 0; i < bar.getMaxDist(); i++) {
						for (BindingArrangement ba : bar.get(i)) {
							boolean foundAndMerged = false;
							List<BindingArrangement> baList = tableGet(roleKey, ba.getDescendent());

							for (BindingArrangement possibleMatch : baList) {
								if (possibleMatch.equals(ba)) {
									possibleMatch.logAddLikelihood(ba.getLogLikelihood());
									possibleMatch.addAllDescendentParentRoles(ba.getDescendentParentRoles());
									foundAndMerged = true;
									break;
								}
							}

							if (!foundAndMerged && ba.getLogLikelihood() > Double.NEGATIVE_INFINITY) {
								baList.add(ba);
							}
						}
					}
				}
			}
			tempTable = null;
		}

		public List<BindingArrangement> getBindingArrangement(Role role, Construction dest) {
			return tableGet(role, dest);
		}

		public BindingArrangement getDirectConnectBindingArrangement(Role role, Construction dest) {
			return directConnectTable.get(role).get(dest);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("SlotConnectionTracker\n-----------------------------\n");
			for (Role roleKey : table.keySet()) {

				for (Construction cxnKey : table.get(roleKey).keySet()) {
					if (table.get(roleKey).get(cxnKey).size() > 0) {
						BindingArrangement fba = table.get(roleKey).get(cxnKey).get(0);
						sb.append("\t");
						sb.append(fba.getAncestor().getName()).append(".").append(fba.getRole().getName()).append("  -->  ")
									.append(fba.getDescendent().getName()).append("\n");
						int i = 0;
						BindingArrangement lastba = null;
						for (BindingArrangement ba : table.get(roleKey).get(cxnKey)) {
							sb.append("\t\tBinding Arrangement #").append(i++).append("  (").append(ba.getLogLikelihood())
										.append(")").append("  -->  ");
							sb.append("Possible descendent parent Roles: (");
							for (Role c : ba.getDescendentParentRoles().keySet()) {
								sb.append(c.getName()).append(",");
							}
							sb.append(")  ");
							for (Binding binding : ba.getBindings()) {
								sb.append(binding.ancChain).append(" <--> ").append(binding.descChain).append(" ;  ");
							}
							if (ba.getBindings().size() == 0) {
								sb.append("No bindings between roles");
							}
							sb.append("\n");
							lastba = ba;
						}
						sb.append("\n");
					}
				}
				sb.append("\n");
			}
			return sb.toString();
		}

	}

	public static void main(String[] args) {
		String ontFile = null;
		if (args.length > 1) {
			ontFile = args[1];
		}
		Grammar ecgGrammar = ECGGrammarUtilities.read(args[0], "ecg cxn sch grm", ontFile);
		LCPGrammarWrapper grammar = new LCPGrammarWrapper(ecgGrammar);
		System.out.println("Begin table building");
		// System.out.println(grammar);
		CloneTable ct = new CloneTable(grammar, new BasicAnalysisFactory());
		SlotChainTables sct = new SlotChainTables(grammar, ct);
		// System.out.println(sct);
		UnifyTable ut = new UnifyTable(grammar, ct);
		DumbConstituentExpansionCostTable dcept = new DumbConstituentExpansionCostTable(grammar);
		ConstituentsToSatisfyTable ctst = new ConstituentsToSatisfyTable(grammar);
		ConstituentLocalityCostTable clct = new ConstituentLocalityCostTable(grammar);
		ConstituentsToSatisfyCostTable ctsct = new ConstituentsToSatisfyCostTable(grammar, ctst, clct);
		SlotConnectionTracker st = new SlotConnectionTracker(grammar, ct, ut, sct, ctsct, dcept, clct);
		System.out.println(st);
		ReachabilityTable rt = new ReachabilityTable(grammar, ctsct, dcept, ut, clct);
		// System.out.println(rt);
	}

}

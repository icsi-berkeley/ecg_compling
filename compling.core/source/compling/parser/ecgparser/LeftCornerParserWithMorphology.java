package compling.parser.ecgparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import compling.context.ContextModel;
import compling.context.ContextModelCache;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.RobustParser;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement;
import compling.parser.ecgparser.PossibleSemSpecs.PartialSemSpec;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.AnalysisFactory;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.CloneTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentExpansionCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentLocalityCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentsToSatisfyCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ConstituentsToSatisfyTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.DumbConstituentExpansionCostTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.MorphologyTypeInterface;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ParamFileConstituentExpansionCostTableCFG;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.ReachabilityTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.TypeToConstituentsTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.TypeToConstituentsTable.AttachPoint;
import compling.parser.ecgparser.morph.LeftCornerParserTablesCxn.UnifyTable;
import compling.parser.ecgparser.morph.LeftCornerParserTablesSem.SlotChainTables;
import compling.parser.ecgparser.morph.LeftCornerParserTablesSem.SlotConnectionTracker;
import compling.parser.ecgparser.morph.MAnalysis;
import compling.parser.ecgparser.morph.MGrammarWrapper;
import compling.parser.ecgparser.morph.MorphAnalyzer;
import compling.util.IdentityHashSet;
import compling.util.Pair;
import compling.util.PriorityQueue;
import compling.util.math.SloppyMath;
import compling.utterance.Utterance;
import compling.utterance.Word;

public class LeftCornerParserWithMorphology<T extends Analysis> implements RobustParser<T> {

	/** CONSTANTS THAT GET SET BY THE setParameters method */
	boolean DEBUG = true;
	private int MAXBEAMWIDTH = 3;
	private int PARSESTORETURN = 3;
	private boolean ROBUST = true;
	private double EXTRAROOTPENALTY = -3;
	private double FULLBEAMTHRESHOLD = 12;
	private double NOTFULLBEAMTHRESHOLD = 30;
	boolean PSYCHOOUTPUT = false;
	private double CUTOFF = 12;

	private PriorityQueue<RobustParserState> workingQ;
	private PriorityQueue<RobustParserState> nextIterationQ;
	private Utterance<Word, String> lastUtteranceProcessed = null;
	private PriorityQueue<List<T>> completeAnalyses;
	private TypeSystem<Construction> cxnTypeSystem;

	private Grammar ecgGrammar;
	private LCPGrammarWrapper grammar;
	private ContextModel contextModel;
	private ContextModelCache cmc;
	private CloneTable<T> cloneTable;
	// private CanonicalSemanticSlotChainFinder csscf;
	private ConstituentsToSatisfyTable constituentsToSatisfyTable;
	private TypeToConstituentsTable typeToConstituentsTable;
	private ReachabilityTable reachabilityTable;
	private UnifyTable unifyTable;
	private SlotChainTables slotChainTables;
	private ConstituentLocalityCostTable constituentLocalityCostTable;
	private ConstituentExpansionCostTable constituentExpansionCostTable;
	private ConstituentsToSatisfyCostTable constituentsToSatisfyCostTable;
	private AnalysisFactory<T> analysisFactory;
	private SlotConnectionTracker slotConnectionTracker;
	private int statecounter = 0;
	private int processedStates = 0;
	private MorphAnalyzer morphAnalyzer;
	private MAnalysis[][] inputMorphAnalyses;
	private Construction RootCxn;
	private Role RootCxnConstituent;
	private StringBuilder parserLog;
	private MorphologyTypeInterface morphInterfaceTable;

	private long constructorTime;
	private double currentEntropy = 0;
	private double lnTwo = Math.log(2);
	private double lastNormalizer = 0;

	public void setParameters(boolean robust, boolean debug, int maxBeamWidth, int parsesToReturn,
			double extraRootPenalty) {
		this.ROBUST = robust;
		this.DEBUG = debug;
		this.MAXBEAMWIDTH = maxBeamWidth;
		this.PARSESTORETURN = parsesToReturn;
		if (extraRootPenalty > 0) {
			extraRootPenalty = 0 - extraRootPenalty;
		}
		this.EXTRAROOTPENALTY = extraRootPenalty;
		setPsychoMode();
	}

	public void setPsychoMode() {
		PSYCHOOUTPUT = true;
	}

	public LeftCornerParserWithMorphology(compling.grammar.ecg.Grammar grammar, AnalysisFactory<T> analysisFactory) {
		this(grammar, analysisFactory, new DumbConstituentExpansionCostTable(new LCPGrammarWrapper(grammar)));
	}

	public LeftCornerParserWithMorphology(compling.grammar.ecg.Grammar grammar, AnalysisFactory<T> analysisFactory,
			String paramFile) throws IOException {
		this(grammar, analysisFactory, new ParamFileConstituentExpansionCostTableCFG(new LCPGrammarWrapper(grammar),
				paramFile));
	}

	public LeftCornerParserWithMorphology(compling.grammar.ecg.Grammar ecgGrammar, AnalysisFactory<T> analysisFactory,
			ConstituentExpansionCostTable cect) {
		constructorTime = System.currentTimeMillis();

		this.ecgGrammar = ecgGrammar;
		this.grammar = new LCPGrammarWrapper(ecgGrammar);
		MGrammarWrapper morphWrapper = new MGrammarWrapper(ecgGrammar, new StringBuffer()); // TODO: How to pass error
																														// logger?
		this.morphAnalyzer = new MorphAnalyzer(morphWrapper);
		this.analysisFactory = analysisFactory;
		this.cxnTypeSystem = grammar.getCxnTypeSystem();
		this.contextModel = grammar.getContextModel();
		this.cloneTable = new CloneTable<T>(grammar, analysisFactory);
		this.unifyTable = new UnifyTable(grammar, cloneTable);
		// this.csscf = new CanonicalSemanticSlotChainFinder(grammar, this.cloneTable);
		this.constituentsToSatisfyTable = new ConstituentsToSatisfyTable(grammar);
		this.typeToConstituentsTable = new TypeToConstituentsTable(grammar, cect);
		this.constituentLocalityCostTable = new ConstituentLocalityCostTable(grammar);
		this.constituentExpansionCostTable = cect;
		// debugPrint("----------------------------------------------------------------------------------");
		this.constituentsToSatisfyCostTable = new ConstituentsToSatisfyCostTable(grammar,
				this.constituentsToSatisfyTable, this.constituentLocalityCostTable);
		this.reachabilityTable = new ReachabilityTable(grammar, constituentsToSatisfyCostTable,
				constituentExpansionCostTable, this.unifyTable, this.constituentLocalityCostTable);
		slotChainTables = new SlotChainTables(grammar, cloneTable);
		this.slotConnectionTracker = new SlotConnectionTracker(grammar, cloneTable, unifyTable, slotChainTables,
				constituentsToSatisfyCostTable, constituentExpansionCostTable, constituentLocalityCostTable);
		// debugPrint(reachabilityTable.toString());
		cloneTable.update();
		// Analysis.setFormatter(new CombinedAnalysisInContextFormatter(grammar, slotChainTables));
		// debugPrint("--------------- next compute processing threshold ---------------");
		// debugPrint(String.valueOf(System.currentTimeMillis()));
		// this.PROCESSINGTHRESHOLD = computeProcessingThreshold();
		this.morphInterfaceTable = new MorphologyTypeInterface(this.grammar, morphWrapper,
				this.constituentExpansionCostTable);
		constructorTime = System.currentTimeMillis() - constructorTime;
		RootCxn = grammar.getConstruction(ECGConstants.ROOT);
		RootCxnConstituent = (Role) RootCxn.getConstructionalBlock().getElements().toArray()[0];
	}

	public long getConstructorTime() {
		return constructorTime;
	}

	public int getNumberOfStatesCreatedForLastUtterance() {
		return statecounter;
	}

	public int getNumberOfStatesProcessedForLastUtterance() {
		return processedStates;
	}

	public PriorityQueue<List<T>> getBestPartialParses(Utterance<Word, String> utterance) {
		lastNormalizer = 0;
		currentEntropy = 0;
		parserLog = new StringBuilder();
		lastUtteranceProcessed = utterance;
		// debugPrint("----------------------------------------------------------------------------------");
		// debugPrint(String.valueOf(System.currentTimeMillis()));

		statecounter = 0;
		processedStates = 0;
		if (contextModel != null) {
			cmc = contextModel.getContextModelCache();
		}
		this.completeAnalyses = new PriorityQueue<List<T>>();

		inputMorphAnalyses = new MAnalysis[utterance.size() + 1][];
		for (int i = 0; i < utterance.size(); i++) {
			try {
				String inputWord = utterance.getElement(i).getOrthography();
				Set<MAnalysis> morphAnalyses = morphAnalyzer.analyze(inputWord);

				inputMorphAnalyses[i] = new MAnalysis[morphAnalyses.size()];
				for (int j = 0; j < morphAnalyses.size(); j++) {
					// System.out.println("i:"+i+", j:"+j+"  "+lexicalCxns.get(j).getName());
					inputMorphAnalyses[i][j] = morphAnalyses.iterator().next();
				}
			}
			catch (GrammarException g) {
				System.out.println("Unknown input lexeme: " + utterance.getElement(i).getOrthography());
				inputMorphAnalyses[i] = new MAnalysis[1];
				inputMorphAnalyses[i][0] = morphAnalyzer.unknownAnalysis;
			}

		}
		inputMorphAnalyses[utterance.size()] = new MAnalysis[1];
		inputMorphAnalyses[utterance.size()][0] = null;

		T root = cloneTable.get(RootCxn, 0);
		RobustParserState rootState = new RobustParserState(root, null, 0);

		if (ROBUST) {
			workingQ = new PriorityQueue<RobustParserState>();
			addStatesToQ(pushLexicalState(rootState, 0), false);
			leftCornerAgendaParse(utterance);
			debugPrint("\nNumber of states created: " + statecounter + "; Number of states processed: " + processedStates);
			if (completeAnalyses.size() == 0) {
				// throw new ParserException("No complete analysis found \n\n " + parserLog);
				throw new ParserException("No complete analysis found for: " + utterance.toString());
			}
			else {
				return completeAnalyses;
			}
		}
		else {

			nextIterationQ = new PriorityQueue<RobustParserState>();
			addStatesToQ(pushLexicalState(rootState, 0), true);

			workingQ = nextIterationQ;
			workingQ = prune(workingQ, MAXBEAMWIDTH);
			for (int i = 1; i <= utterance.size(); i++) {
				workingQ = prune(makeNewCandidates(utterance, i), MAXBEAMWIDTH);
			}
			debugPrint("\nNumber of states created: " + statecounter + "; Number of states processed: " + processedStates);
			if (completeAnalyses.size() == 0) {
				// throw new ParserException("No complete analysis found \n\n " + parserLog);
				throw new ParserException("No complete analysis found for: " + utterance.toString());
			}
			else {
				return prune(completeAnalyses, PARSESTORETURN);
			}
		}
	}

	public List<T> getBestPartialParse(Utterance<Word, String> utterance) {
		return getBestPartialParses(utterance).next();
	}

	public T getBestParse(Utterance<Word, String> utterance) {
		List<T> partial = getBestPartialParse(utterance);
		if (partial.size() > 1) {
			throw new ParserException("No single rooted parse found for: " + utterance.toString());
		}
		return partial.get(0);
	}

	public int getLargestAssignedSlotID() {
		return cloneTable.slotCounter;
	}

	private boolean stoppingCriteria(int index, int utteranceSize, double currentLogLikelihood) {
		if (index == utteranceSize
				&& ((completeAnalyses.size() >= PARSESTORETURN && completeAnalyses.getPriority() >= currentLogLikelihood
						+ FULLBEAMTHRESHOLD) || (completeAnalyses.size() < PARSESTORETURN && completeAnalyses.size() > 0 && completeAnalyses
						.getWorstPriority() >= currentLogLikelihood + NOTFULLBEAMTHRESHOLD))) {
			return true;
		}
		else if (index < utteranceSize
				&& ((nextIterationQ.size() >= MAXBEAMWIDTH && nextIterationQ.getPriority() >= currentLogLikelihood
						+ FULLBEAMTHRESHOLD) || (nextIterationQ.size() < MAXBEAMWIDTH && nextIterationQ.size() > 0 && nextIterationQ
						.getWorstPriority() >= currentLogLikelihood + NOTFULLBEAMTHRESHOLD))) {
			return true;
		}
		return false;
	}

	private RobustParserState getRoot(RobustParserState rps) {
		while (rps.ancestor != null) {
			rps = rps.ancestor;
		}
		return rps;
	}

	private PriorityQueue<RobustParserState> makeNewCandidates(Utterance<Word, String> utterance, int index) {
		// if (true){System.out.println("\nmake next candidates with index="+index+" Num states created="+statecounter);}
		nextIterationQ = new PriorityQueue<RobustParserState>();

		if (workingQ.size() == 0) {
			throw new ParserException("No parse found for: " + utterance.toString());
		}
		RobustParserState bestSoFar = workingQ.peek();

		PriorityQueue<RobustParserState> normalizerQ = workingQ.clone();
		List<RobustParserState> rpsList = new ArrayList<RobustParserState>();
		double normalizer = Double.NEGATIVE_INFINITY;
		double entropy = 0;
		while (normalizerQ.size() > 0) {
			RobustParserState rps = normalizerQ.next();
			rpsList.add(rps);
			normalizer = SloppyMath.logAdd(normalizer, rps.getLogLikelihood());
			// System.out.println("rps.getLL "+rps.getLogLikelihood());
		}
		if (PSYCHOOUTPUT || DEBUG) {
			// System.out.println("\nSurviving parser states incorporating up through the word \""+utterance.getElement(index
			// -1).getOrthography()+"\"\n\n");
			parserLog.append("\nSurviving parser states incorporating up through the word \""
					+ utterance.getElement(index - 1).getOrthography() + "\"\n\n");
			parserLog
					.append("-------------------------------------------------------------------------------------------\n");
			for (RobustParserState rps : rpsList) {
				parserLog.append(rps.stackToString(normalizer)).append("\n");
				entropy = entropy + Math.exp(rps.getLogLikelihood() - normalizer) * -1
						* (rps.getLogLikelihood() - normalizer) / lnTwo;
			}
			parserLog.append("\t\tP(w_i|...): ").append(
					AnalysisUtilities.formatDouble(16, Math.exp(normalizer - lastNormalizer)));
			// parserLog.append("  "+normalizer+"   "+lastNormalizer);
			parserLog.append("\tCurrent Entropy: ").append(AnalysisUtilities.formatDouble(4, entropy));
			parserLog.append("    Difference in Entropy: ")
					.append(AnalysisUtilities.formatDouble(4, entropy - currentEntropy)).append("\n");
			currentEntropy = entropy;
			parserLog
					.append("-------------------------------------------------------------------------------------------\n");
			lastNormalizer = normalizer;
		}
		// PriorityQueue<RobustParserState> safetyQ = workingQ.clone(); //only used when we out of stuff on the workingQ
		// and there is nothing on the nextIterationQ

		while (workingQ.size() > 0) {

			RobustParserState current = workingQ.next();

			if (stoppingCriteria(index, utterance.size(), current.getLogLikelihood())) {
				break;
			}
			debugPrint("\nPROCESSING: " + current.toString());
			// debugPrint(current.primaryAnalysis.getPossibleSemSpecs().toString());
			processedStates++;
			if (current.primaryAnalysis.completed()) {

				if (current.getSpanLeftIndex() == 0 && index == utterance.size() && current.ancestor == null) {
					List<T> completedAnalysis = new ArrayList<T>();
					T a = (T) current.primaryAnalysis.clone();
					CxnalSpan completeSpan = new CxnalSpan(null, a.getHeadCxn(), a.getFeatureStructure().getMainRoot()
							.getID(), a.getSpanLeftIndex(), a.getSpanRightIndex());
					if (a.spans == null) {
						a.spans = new LinkedList<CxnalSpan>();
					}
					a.spans.add(0, completeSpan);
					completedAnalysis.add(a);
					if (ROBUST && current.rightSiblings != null) {
						completedAnalysis.addAll(current.rightSiblings);
					}
					addToCompletedQ(completedAnalysis, current.getLogLikelihood()); // it's an analysis that covers and has
																											// no ancestors
					// System.out.println("\tFound Complete Analysis "+current.getLogLikelihood());
					debugPrint("\tFound Complete Analysis");

				}
				else {

					addStatesToQ(attachCompleteStateToAncestorState(current), false);
					addStatesToQ(proposeNewStates(current), false);

				}

			}
			else {
				if (index < utterance.size()) {
					addStatesToQ(pushLexicalState(current, index), true);
				}
				addStatesToQ(finishIncompleteState(current), false);
			}
		}

		return nextIterationQ;
	}

	private void leftCornerAgendaParse(Utterance<Word, String> utterance) {
		while (true) {
			if (workingQ.size() == 0 && completeAnalyses.size() == 0) {
				throw new ParserException("No parse found for: " + utterance.toString());
			}
			if (workingQ.size() == 0 && completeAnalyses.size() > 0 || completeAnalyses.size() == PARSESTORETURN
					|| completeAnalyses.size() > 0 && completeAnalyses.getPriority() > workingQ.getPriority() + CUTOFF) {
				return;
			}
			RobustParserState current = workingQ.next();
			debugPrint("\nPROCESSING: " + current.toString());
			processedStates++;
			if (current.primaryAnalysis.completed()) {

				if (current.getSpanLeftIndex() == 0 && current.nextWord == utterance.size() && current.ancestor == null) {
					List<T> completedAnalysis = new ArrayList<T>();
					T a = (T) current.primaryAnalysis.clone();
					CxnalSpan completeSpan = new CxnalSpan(null, a.getHeadCxn(), a.getFeatureStructure().getMainRoot()
							.getID(), a.getSpanLeftIndex(), a.getSpanRightIndex());
					if (a.spans == null) {
						a.spans = new LinkedList<CxnalSpan>();
					}
					a.spans.add(0, completeSpan);
					completedAnalysis.add(a);
					if (ROBUST && current.rightSiblings != null) {
						completedAnalysis.addAll(current.rightSiblings);
					}
					addToCompletedQ(completedAnalysis, current.getLogLikelihood()); // it's an analysis that covers and has
																											// no ancestors
					// System.out.println("\tFound Complete Analysis "+current.getLogLikelihood());
					debugPrint("\tFound Complete Analysis");

				}
				else {
					if (ROBUST && current.ancestor == null && current.leftSibling != null) { // gotta switch back to the
																														// leftsib stack
						RobustParserState ls = current.leftSibling;
						RobustParserState newCurrent = new RobustParserState((T) ls.primaryAnalysis.clone(), ls.ancestor,
								current.getConstructionalLogLikelihood(), ls.leftSibling, ls.rightSiblings, ls.prevRootSemCost
										+ ls.completedRightSiblingSemanticCost, current.completedRightSiblingSemanticCost
										+ current.primaryAnalysis.computeSemanticCost());
						newCurrent.addRightSibling(current.primaryAnalysis, current.rightSiblings, current.nextWord);
						current = newCurrent;

						debugPrint("\tFINISHED ROOT. NOW PROCESSING the state that was STATE[" + ls.stateID + "]\n"
								+ current.toString());
					}

					addStatesToQ(attachCompleteStateToAncestorState(current), false);
					addStatesToQ(proposeNewStates(current), false);
					if (ROBUST && current.nextWord < utterance.size() && current.ancestor != null
							&& current.rightIndex - current.leftIndex == 1
							&& (current.rightSiblings == null || current.rightSiblings.size() == 0)) {
						RobustParserState robustRoot = new RobustParserState(cloneTable.get(RootCxn, current.nextWord), null,
								current.getConstructionalLogLikelihood() + EXTRAROOTPENALTY, current, null,
								current.prevRootSemCost + current.primaryAnalysis.computeSemanticCost()
										+ current.completedRightSiblingSemanticCost, 0);
						robustRoot.leftIndex = current.nextWord;
						robustRoot.rightIndex = current.nextWord;
						debugPrint("\tMaking New Robust States");
						addStatesToQ(pushLexicalState(robustRoot, current.nextWord), false);
					}

				}

			}
			else {
				if (current.nextWord < utterance.size()) {
					addStatesToQ(pushLexicalState(current, current.nextWord), false);
				}
				addStatesToQ(finishIncompleteState(current), false);
			}
		}

	}

	private List<RobustParserState> attachCompleteStateToAncestorState(RobustParserState current) {
		// System.out.println("attach");
		List<RobustParserState> results = new ArrayList<RobustParserState>();
		if (current.ancestor != null) {
			// debugPrint("\t\tAttempting to attach "+current.primaryAnalysis.getHeadCxn().getName()+" to "+current.ancestor.primaryAnalysis.getHeadCxn().getName());
			RobustParserState possibleParent = current.ancestor;
			T ppa = possibleParent.primaryAnalysis;
			for (Role role : ppa.getLeftOverConstituents()) {
				boolean goForward = false;
				if (constituentExpansionCostTable.getConstituentExpansionCost(role, current.primaryAnalysis.getHeadCxn()) > Double.NEGATIVE_INFINITY) {
					goForward = true;
				}

				if (goForward == false && !grammar.isPhrasalConstruction(current.primaryAnalysis.getHeadCxn())
						&& morphInterfaceTable != null) {// try the interface type instead
					// System.out.println("here in attach1");
					for (Construction t : morphInterfaceTable
							.getAllMorphInterfaceTypes(current.primaryAnalysis.getHeadCxn())) {
						// System.out.println("here in attach2 "+role.getTypeConstraint().getType()+" "+t.getName());
						if (constituentExpansionCostTable.getConstituentExpansionCost(role, t) > Double.NEGATIVE_INFINITY) {
							// System.out.println("here in attach3");
							goForward = true;
							break;
						}
					}
				}
				if (goForward) {
					// System.out.println("here in attach");
					List<T> sibs = null;
					if (possibleParent.rightSiblings != null || current.rightSiblings != null) {
						sibs = new LinkedList<T>();
						if (possibleParent.rightSiblings != null) {
							sibs.addAll(possibleParent.rightSiblings);
						}
						if (current.rightSiblings != null) {
							sibs.addAll(current.rightSiblings);
						}
					}
					results.addAll(advancer(
							ppa,
							role,
							current.primaryAnalysis,
							possibleParent.ancestor,
							current.getConstructionalLogLikelihood()
									+ 0
									- computeReachability(ppa, current.primaryAnalysis.getHeadCxn(),
											possibleParent.hasGapFiller(), possibleParent.getGapFillerType()),
							possibleParent.leftSibling, sibs));
				}
			}
		}
		return results;
	}

	private List<RobustParserState> proposeNewStates(RobustParserState current) {
		// System.out.println("propose");
		List<RobustParserState> results = new ArrayList<RobustParserState>();
		if (current.ancestor != null && typeToConstituentsTable.get(current.primaryAnalysis.getHeadCxn()) != null) {
			// System.out.println("here in propose1");
			for (AttachPoint ap : typeToConstituentsTable.get(current.primaryAnalysis.getHeadCxn())) {
				// System.out.println("here in propose 2 "+ap.cxn.getName());
				double proposeCost = computeReachability(current.ancestor.primaryAnalysis, ap.cxn,
						current.ancestor.hasGapFiller() && !current.ancestor.primaryAnalysis.alreadyUsedGapFiller()
								&& !current.primaryAnalysis.alreadyUsedGapFiller(), current.ancestor.getGapFillerType());

				if (proposeCost > Double.NEGATIVE_INFINITY) { // && !(index == utteranceSize && ap.cxn ==
																				// current.primaryAnalysis.getHeadCxn())){
					T proposedAnalysis = cloneTable.get(ap.cxn, current.primaryAnalysis.getSpanLeftIndex());
					if (incorporateAncSem(current.ancestor, ap.cxn, proposedAnalysis)) {
						results.addAll(advancer(
								proposedAnalysis,
								ap.constituent,
								current.primaryAnalysis,
								current.ancestor,
								current.getConstructionalLogLikelihood()
										+ proposeCost
										- computeReachability(current.ancestor.primaryAnalysis,
												current.primaryAnalysis.getHeadCxn(), current.ancestor.hasGapFiller()
														&& !current.ancestor.primaryAnalysis.alreadyUsedGapFiller()
														&& !current.primaryAnalysis.alreadyUsedGapFiller(),
												current.ancestor.getGapFillerType()), null, current.rightSiblings));
					}
				}
			}
		}
		return results;
	}

	private List<RobustParserState> pushLexicalState(RobustParserState ancestor, int index) {
		List<RobustParserState> results = new LinkedList<RobustParserState>();
		for (int i = 0; i < inputMorphAnalyses[index].length; i++) {
			MAnalysis morphAnalysis = inputMorphAnalyses[index][i];
			Construction mCxn = morphAnalysis.getHeadCxn();
			double reachabilityCost = computeNormalizedReachability(ancestor.primaryAnalysis, mCxn,
					ancestor.hasGapFiller() && !ancestor.primaryAnalysis.alreadyUsedGapFiller(), ancestor.getGapFillerType());

			if (reachabilityCost > Double.NEGATIVE_INFINITY) {
				MAnalysis lexical = morphAnalysis.clone();
				lexical.setSpanLeftIndex(index);
				// lexical.setSpanRightIndex(index+1);
				lexical.advance();
				lexical.setCompleted(true);
				// System.out.println("left: "+lexical.getSpanLeftIndex()+" right: "+lexical.getSpanRightIndex()+
				// " complete "+lexical.completed()+" super.completed "+((Analysis) lexical).completed());
				List<List<Pair<TypeConstraint, Role>>> frameRoles = new ArrayList<List<Pair<TypeConstraint, Role>>>();
				for (SlotChain sc : slotChainTables.getCanonicalChains(lexical.getHeadCxn())) {
					frameRoles.add(slotChainTables.getFrameRoles(lexical.getHeadCxn(), sc));
				}
				lexical.setSemanticChains(slotChainTables.getCanonicalChains(lexical.getHeadCxn()), frameRoles);
				// System.out.println(lexical.getPossibleSemSpecs());

				if (incorporateAncSem(ancestor, mCxn, lexical)) {
					// FIXME the type coercion (T) lexical is probably not right
					RobustParserState rps = new RobustParserState((T) lexical, ancestor, reachabilityCost
							+ ancestor.getConstructionalLogLikelihood());
					results.add(rps);
				}
			}
			else {
				debugPrint("\t\t" + ancestor.primaryAnalysis.getHeadCxn().getName() + " cannot generate " + mCxn.getName());
			}
		}
		return results;
	}

	private List<RobustParserState> finishIncompleteState(RobustParserState p) {
		// System.out.println("finish");
		List<Pair<T, Double>> results;
		T toFinish = p.primaryAnalysis;
		if (p.ancestor != null) {
			results = processRoles(toFinish, toFinish.getLeftOverConstituents(),
					p.ancestor.hasGapFiller() && !toFinish.alreadyUsedGapFiller(), p.ancestor.getGapFillerType(),
					p.ancestor.getGapFiller());
		}
		else {
			results = processRoles(toFinish, toFinish.getLeftOverConstituents(), false, null, null);
		}
		List<RobustParserState> rpsStates = new ArrayList<RobustParserState>();
		for (Pair<T, Double> pair : results) {
			double finishingCost = pair.getSecond();
			T a = pair.getFirst();
			if (a.completed()) { // only need to worry about this because of extraposedconstituents
				RobustParserState rps = new RobustParserState(a, p.ancestor, p.getConstructionalLogLikelihood()
						+ finishingCost, p.leftSibling, p.rightSiblings, p.prevRootSemCost,
						p.completedRightSiblingSemanticCost);
				rpsStates.add(rps);
			}
		}
		return rpsStates;
	}

	private double pNextInputGivenStack(int index, RobustParserState stackTop) {
		// return pNextInputGivenStack(index, stackTop, true);
		double robustTerm = Double.NEGATIVE_INFINITY;

		if (ROBUST && stackTop.ancestor != null && stackTop.rightIndex - stackTop.leftIndex == 1
				&& (stackTop.rightSiblings == null || stackTop.rightSiblings.size() == 0)) {
			for (int i = 0; i < inputMorphAnalyses[index].length; i++) {
				Construction interfaceCxn = morphInterfaceTable.getMorphInterfaceType(RootCxnConstituent,
						inputMorphAnalyses[index][i].getHeadCxn());
				robustTerm = SloppyMath.logAdd(robustTerm, reachabilityTable.reachable(RootCxnConstituent, interfaceCxn));
			}
			robustTerm = robustTerm + EXTRAROOTPENALTY;
			// System.out.println(robustTerm);

		}
		double pnext = pNextInputGivenStack(index, stackTop, true);
		// System.out.println("pnext: "+pnext);
		return SloppyMath.logAdd(pnext, robustTerm);
		// return 0;
	}

	private double pNextInputGivenStack(int index, RobustParserState stackTop, boolean tryLeftSib) {
		double total = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < inputMorphAnalyses[index].length; i++) {

			Construction lexicalCxn = null;
			if (inputMorphAnalyses[index][i] != null) {
				lexicalCxn = inputMorphAnalyses[index][i].getHeadCxn();
			}

			total = SloppyMath.logAdd(
					total,
					computeNormalizedReachability(stackTop.primaryAnalysis, lexicalCxn, stackTop.hasGapFiller(),
							stackTop.getGapFillerType()));
			double completionCostSoFar = getRemainingConstituentCosts(stackTop.primaryAnalysis.getLeftOverConstituents(),
					stackTop.hasGapFiller(), stackTop.getGapFillerType());

			Construction childType = stackTop.primaryAnalysis.getHeadCxn();
			RobustParserState currentState = stackTop.ancestor;

			RobustParserState leftSib = getRoot(stackTop).leftSibling;

			while (currentState != null) {

				double currentStateFinishingCost = Double.NEGATIVE_INFINITY;
				double currentStateTotal = Double.NEGATIVE_INFINITY;
				Set<Role> leftOvers = currentState.primaryAnalysis.getLeftOverConstituents();
				// Set<Role> leftOverComps = currentState.primaryAnalysis.getLeftOverComplements();
				double normalizingReachability = computeReachability(currentState.primaryAnalysis, childType,
						currentState.hasGapFiller(), currentState.getGapFillerType());

				for (Role rSubDesc : leftOvers) { // this is the role that must cover the state above it on the stack
					// double normalizingReachability = reachabilityTable.reachable(rSubDesc, childType);
					if (normalizingReachability == Double.NEGATIVE_INFINITY) {
						continue;
					}
					Set<Role> newLeftOvers = new IdentityHashSet(leftOvers);
					newLeftOvers.remove(rSubDesc);
					newLeftOvers.removeAll(constituentsToSatisfyTable.get(rSubDesc));
					double rSubDescCost = getRemainingConstituentCosts(rSubDesc, leftOvers, currentState.hasGapFiller(),
							currentState.getGapFillerType()) + constituentLocalityCostTable.getLocalCost(rSubDesc);
					double rSubDescFinishCost = getRemainingConstituentCosts(newLeftOvers, currentState.hasGapFiller(),
							currentState.getGapFillerType());

					double expansionCost = constituentExpansionCostTable.getConstituentExpansionCost(rSubDesc, childType);
					if (expansionCost == Double.NEGATIVE_INFINITY && !grammar.isPhrasalConstruction(childType)
							&& morphInterfaceTable != null) {
						// System.out.println("here in pnext1");
						for (Construction t : morphInterfaceTable.getAllMorphInterfaceTypes(childType)) {
							// System.out.println("here in pnext2 "+rSubDesc.getTypeConstraint().getType()+" "+t.getName());
							expansionCost = constituentExpansionCostTable.getConstituentExpansionCost(rSubDesc, t);
							if (expansionCost > Double.NEGATIVE_INFINITY) {
								// System.out.println("here in pnext");
								break;
							}
						}
					}

					expansionCost = expansionCost + rSubDescCost;
					if (expansionCost > Double.NEGATIVE_INFINITY) {
						// this is the case where we attach directly and then generate the next input from the remaining roles
						currentStateTotal = SloppyMath.logAdd(
								currentStateTotal,
								computeNormalizedReachability(newLeftOvers, lexicalCxn, completionCostSoFar + expansionCost
										- normalizingReachability, currentState.hasGapFiller(), currentState.getGapFillerType()));
						currentStateFinishingCost = SloppyMath.logAdd(currentStateFinishingCost, expansionCost
								+ rSubDescFinishCost - normalizingReachability);
					}

					for (Construction hidden : grammar.getAllConcretePhrasalConstructions()) {

						double currentToHiddenReachability = computeReachability(currentState.primaryAnalysis, hidden,
								currentState.hasGapFiller(), currentState.getGapFillerType());
						if (currentToHiddenReachability > Double.NEGATIVE_INFINITY) {
							Set<Role> hiddenLeftOvers = new IdentityHashSet<Role>();
							hiddenLeftOvers.addAll(hidden.getComplements());
							hiddenLeftOvers.addAll(hidden.getOptionals());
							for (Role rSubHiddenToChild : hiddenLeftOvers) {

								double hiddenRoleToChildReachability = reachabilityTable
										.reachable(rSubHiddenToChild, childType);
								if (hiddenRoleToChildReachability > Double.NEGATIVE_INFINITY) {

									double costToDealWithChildType = getRemainingConstituentCosts(rSubHiddenToChild,
											hiddenLeftOvers, currentState.hasGapFiller(), currentState.getGapFillerType())
											+ hiddenRoleToChildReachability;
									Set<Role> newHiddenLeftOvers = new IdentityHashSet<Role>(hiddenLeftOvers);
									newHiddenLeftOvers.remove(rSubHiddenToChild);
									newHiddenLeftOvers.removeAll(constituentsToSatisfyTable.get(rSubHiddenToChild));
									currentStateTotal = SloppyMath.logAdd(
											currentStateTotal,
											computeNormalizedReachability(newHiddenLeftOvers, lexicalCxn, completionCostSoFar
													+ rSubDescCost + costToDealWithChildType + currentToHiddenReachability
													- normalizingReachability, currentState.hasGapFiller(),
													currentState.getGapFillerType()));

									double hiddenFinishingCost = getRemainingConstituentCosts(newHiddenLeftOvers,
											currentState.hasGapFiller(), currentState.getGapFillerType());
									currentStateTotal = SloppyMath.logAdd(
											currentStateTotal,
											computeNormalizedReachability(newLeftOvers, lexicalCxn, completionCostSoFar
													+ rSubDescCost + hiddenFinishingCost + currentToHiddenReachability
													+ costToDealWithChildType - normalizingReachability,
													currentState.hasGapFiller(), currentState.getGapFillerType()));
									currentStateFinishingCost = SloppyMath.logAdd(currentStateFinishingCost, rSubDescCost
											+ rSubDescFinishCost + hiddenFinishingCost + currentToHiddenReachability
											+ costToDealWithChildType - normalizingReachability);
								}
							}
						}
					}
				}
				total = SloppyMath.logAdd(total, currentStateTotal);
				completionCostSoFar = currentStateFinishingCost + completionCostSoFar;
				childType = currentState.primaryAnalysis.getHeadCxn();
				currentState = currentState.ancestor;
				if (currentState == null && lexicalCxn == null) {
					return completionCostSoFar;
				}
			}
			if (ROBUST && tryLeftSib && leftSib != null && total < 0 && index < inputMorphAnalyses.length - 1) {
				total = SloppyMath.logAdd(total, completionCostSoFar + pNextInputGivenStack(index, leftSib, false));
				if (total > 0) {
					total = 0;
				}
			}
			else if (ROBUST && tryLeftSib && leftSib != null && total < 0 && index == inputMorphAnalyses.length - 1) {
				while (leftSib != null) {
					total = total + pNextInputGivenStack(index, leftSib, false);
					leftSib = leftSib.leftSibling;
				}
			}
		}
		return total;
	}

	private double computeReachability(Analysis a, Construction cxn, boolean availableGapFiller,
			Construction gapFillerType) {
		if (a.getHeadCxn().getName().equals("ROOT") && cxn == null) {
			return 0;
		}
		return computeReachability(a.getLeftOverConstituents(), cxn, 0, availableGapFiller, gapFillerType);
	}

	private double computeReachability(Set<Role> leftOverConstituents, Construction cxn, double additionalCost,
			boolean availableGapFiller, Construction gapFillerType) {
		if (cxn == null) {
			return Double.NEGATIVE_INFINITY;
		}
		double cost = Double.NEGATIVE_INFINITY;
		if (leftOverConstituents != null) {
			for (Role r : leftOverConstituents) {
				double reachabilityCost = reachabilityTable.reachable(r, cxn);
				if (reachabilityCost == Double.NEGATIVE_INFINITY && !grammar.isPhrasalConstruction(cxn)
						&& morphInterfaceTable != null) {// try the interface type instead
					for (Construction t : morphInterfaceTable.getAllMorphInterfaceTypes(cxn)) {
						reachabilityCost = SloppyMath.logAdd(reachabilityTable.reachable(r, t), reachabilityCost);
					}
				}
				if (reachabilityCost > Double.NEGATIVE_INFINITY) {
					cost = SloppyMath.logAdd(cost,
							getRemainingConstituentCosts(r, leftOverConstituents, availableGapFiller, gapFillerType)
									+ reachabilityCost);
				}
			}
		}
		return cost + additionalCost;
	}

	private double computeNormalizedReachability(Analysis a, Construction cxn, boolean availableGapFiller,
			Construction gapFillerType) {
		if (a.getHeadCxn().getName().equals("ROOT") && cxn == null) {
			return 0;
		}
		return computeNormalizedReachability(a.getLeftOverConstituents(), cxn, 0, availableGapFiller, gapFillerType);
	}

	private double computeNormalizedReachability(Set<Role> leftOverConstituents, Construction cxn,
			double additionalCost, boolean availableGapFiller, Construction gapFillerType) {
		if (cxn == null) {
			return Double.NEGATIVE_INFINITY;
		}
		double cost = Double.NEGATIVE_INFINITY;
		if (leftOverConstituents != null) {
			for (Role r : leftOverConstituents) {
				double reachabilityCost = reachabilityTable.normReachable(r, cxn);
				if (reachabilityCost == Double.NEGATIVE_INFINITY && !grammar.isPhrasalConstruction(cxn)
						&& morphInterfaceTable != null) {// try the interface type instead
					// do not need to re-initialize reachabilityCost
					for (Construction t : morphInterfaceTable.getAllMorphInterfaceTypes(cxn)) {
						// use logAdd here since we're summing logs (and not intending it to be multiplication)
						reachabilityCost = SloppyMath.logAdd(reachabilityTable.normReachable(r, t), reachabilityCost);
					}
				}
				if (reachabilityCost > Double.NEGATIVE_INFINITY) {
					cost = SloppyMath.logAdd(cost,
							getRemainingConstituentCosts(r, leftOverConstituents, availableGapFiller, gapFillerType)
									+ reachabilityCost);
				}
			}
		}
		return cost + additionalCost;
	}

	private double getRemainingConstituentCosts(Role r, Set<Role> leftOver, boolean availableGapFiller,
			Construction gapFillerType) {
		return getRemainingConstituentCosts(leftOver, constituentsToSatisfyTable.get(r), availableGapFiller,
				gapFillerType);
	}

	private double getRemainingConstituentCosts(Set<Role> leftOver, boolean availableGapFiller,
			Construction gapFillerType) {
		return getRemainingConstituentCosts(leftOver, leftOver, availableGapFiller, gapFillerType);
	}

	private double getRemainingConstituentCosts(Set<Role> leftOver, Set<Role> complements, boolean availableGapFiller,
			Construction gapFillerType) {
		if (leftOver == null) {
			return 0;
		}
		double omissionCost = 0;
		for (Role leftOverRole : leftOver) {
			if (complements.contains(leftOverRole)) {
				omissionCost = omissionCost + constituentLocalityCostTable.getOmissionCost(leftOverRole);
			}
		}
		if (availableGapFiller) {
			omissionCost = SloppyMath.logAdd(omissionCost,
					dealWithGapping(leftOver, complements, availableGapFiller, gapFillerType));
		}
		return omissionCost;
	}

	private double dealWithGapping(Set<Role> leftOver, Set<Role> rolesToProcess, boolean availableGapFiller,
			Construction gapFillerType) {
		// System.out.println("here in deal with gapping -> leftover:"+leftOver+"  rtp:"+rolesToProcess+"   avail:"+availableGapFiller+"  cxn:"+gapFillerType.getName());
		double gappingCost = Double.NEGATIVE_INFINITY;
		if (availableGapFiller) {
			for (Role gapRole : rolesToProcess) {
				double cost = 0;
				try {
					if (leftOver.contains(gapRole)
							&& cxnTypeSystem.subtype(gapFillerType.getName(), gapRole.getTypeConstraint().getType())) {
						for (Role otherRole : rolesToProcess) {
							if (otherRole == gapRole) {
								cost = cost + constituentLocalityCostTable.getNonLocalCost(otherRole);
							}
							else if (leftOver.contains(otherRole)) {
								cost = cost + constituentLocalityCostTable.getOmissionCost(otherRole);
							}
						}
						gappingCost = SloppyMath.logAdd(gappingCost, cost);
					}
				}
				catch (TypeSystemException tse) {
					throw new ParserException("Type Exception: " + tse.toString());
				}
			}
		}
		return gappingCost;
	}

	private boolean incorporateAncSem(RobustParserState anc, Construction cxn, Analysis newAnalysis) {
		if (anc == null) {
			return true;
		}
		List<BindingArrangement> bas = new ArrayList<BindingArrangement>();
		List<Double> penalties = new ArrayList<Double>();
		Analysis ancPrimAnalysis = anc.primaryAnalysis;
		for (Role r : anc.primaryAnalysis.getLeftOverConstituents()) {
			double penalty = getRemainingConstituentCosts(r, anc.primaryAnalysis.getLeftOverConstituents(),
					anc.hasGapFiller() && !anc.primaryAnalysis.alreadyUsedGapFiller(), anc.getGapFillerType());
			if (penalty > Double.NEGATIVE_INFINITY) {
				Set<Construction> iCxns;
				if (morphInterfaceTable != null && !grammar.isPhrasalConstruction(cxn)) {
					iCxns = morphInterfaceTable.getAllMorphInterfaceTypes(cxn);
				}
				else {
					iCxns = new HashSet();
					iCxns.add(cxn);
				}
				for (Construction iCxn : iCxns) {
					// It's possible that the following line might throw a null pointer exception,
					// but I think that's ok if iCxn is a morph type...
					for (BindingArrangement ba : slotConnectionTracker.getBindingArrangement(r, iCxn)) {
						// bas.addAll(slotConnectionTracker.getBindingArrangement(r, iCxn));
						bas.add(ba);
						penalties.add(penalty);
						// System.out.println(penalty+" "+ba);
					}
				}
			}

		}
		return newAnalysis.incorporateAncestorSemanticInfo(ancPrimAnalysis.getPossibleSemSpecs(), bas, penalties);
	}

	private List<RobustParserState> advancer(T newAnalysis, Role filledRole, T filler, RobustParserState ancestor,
			double costSoFar, RobustParserState leftSibling, List<T> rightSiblings) {
		List<RobustParserState> rpsResults = new ArrayList<RobustParserState>();
		List<Pair<T, Double>> results = null;
		if (ancestor != null) {
			results = processRoles(newAnalysis, constituentsToSatisfyTable.get(filledRole), ancestor.hasGapFiller()
					&& !newAnalysis.alreadyUsedGapFiller() && !filler.alreadyUsedGapFiller(), ancestor.getGapFillerType(),
					ancestor.getGapFiller());
		}
		else {
			results = processRoles(newAnalysis, constituentsToSatisfyTable.get(filledRole), false, null, null);
		}
		for (Pair<T, Double> pair : results) {
			T a = pair.getFirst();
			double omissionCost = pair.getSecond();
			Construction interfaceCxn = filler.getHeadCxn();
			if (!grammar.isPhrasalConstruction(interfaceCxn)) {
				interfaceCxn = morphInterfaceTable.getMorphInterfaceType(filledRole, filler.getHeadCxn());
			}
			// System.out.println("In advancer: "+filledRole.toString()+" "+filler.getHeadCxn().getName());
			// System.out.println("In advancer: "+filledRole.toString()+" "+interfaceCxn.getName());
			// System.out.println("In advancer\n"+slotConnectionTracker.getDirectConnectBindingArrangement(filledRole,
			// interfaceCxn).toString());
			// System.out.println("In advancer\n"+slotConnectionTracker.getDirectConnectBindingArrangement(filledRole,
			// filler.getHeadCxn()).toString());
			if (a.advance(filledRole, filler.clone(),
					slotConnectionTracker.getDirectConnectBindingArrangement(filledRole, interfaceCxn))) {
				double attachCost = constituentExpansionCostTable.getConstituentExpansionCost(filledRole,
						filler.getHeadCxn());
				if (attachCost == Double.NEGATIVE_INFINITY && !grammar.isPhrasalConstruction(filler.getHeadCxn())
						&& morphInterfaceTable != null) {
					attachCost = constituentExpansionCostTable.getConstituentExpansionCost(filledRole,
							morphInterfaceTable.getMorphInterfaceType(filledRole, filler.getHeadCxn()));
				}
				attachCost = attachCost + constituentLocalityCostTable.getLocalCost(filledRole);
				T ancestorPrimary = null;
				if (ancestor != null) {
					ancestorPrimary = ancestor.primaryAnalysis;
				}
				double cxnalLL = attachCost + omissionCost + costSoFar;
				if (cxnalLL > Double.NEGATIVE_INFINITY) {
					RobustParserState rps = new RobustParserState(a, ancestor, cxnalLL, leftSibling, rightSiblings,
							ancestor == null ? 0.0 : ancestor.prevRootSemCost, ancestor == null ? 0.0
									: ancestor.completedRightSiblingSemanticCost);
					rpsResults.add(rps);
				}
			}
		}
		return rpsResults;
	}

	private List<Pair<T, Double>> processRoles(T unclonedStarter, Set<Role> rolesToProcess, boolean availableGapFiller,
			Construction gapFillerType, PossibleSemSpecs gapFiller) {
		if (rolesToProcess == null) {
			throw new RuntimeException("Empty rolesToProcess list in ECGLeftCornerParser.processRoles");
		}
		T starter = (T) unclonedStarter.clone();
		double cost = 0;
		List<Pair<T, Double>> retList = new ArrayList<Pair<T, Double>>();
		for (Role role : rolesToProcess) {
			// if (starter.getLeftOverComplements().contains(role)){
			if (starter.getLeftOverConstituents().contains(role)) {
				if (starter.getHeadCxn().isExtraPosedRole(role)) {
					return retList;
				} /* empty: you can't omit an extraposed constituent */
				if (starter.getLeftOverComplements().contains(role)) {
					starter.omit(role);
				}
				else {
					starter.omitOptional(role);
				}
				cost = cost + constituentLocalityCostTable.getOmissionCost(role);
			}
		}
		retList.add(new Pair<T, Double>(starter, cost));

		// now on the return list we've got everything the case where everything is omitted

		if (availableGapFiller) {
			// now for each role, we need to omit up to the role, gap out the role, then omit the rest

			// System.out.println("we have an available gap filler!!!!!!!! "+rolesToProcess+" type: "+unclonedStarter.getHeadCxn().getName());
			// System.out.println("rolestoprocess: "+rolesToProcess);

			for (Role gapRole : rolesToProcess) {
				// System.out.println("here: "+gapRole.getName());
				try {
					if (unclonedStarter.getLeftOverComplements().contains(gapRole)
							&& cxnTypeSystem.subtype(gapFillerType.getName(), gapRole.getTypeConstraint().getType())) {
						// System.out.println("matching types!");
						cost = 0;
						starter = (T) unclonedStarter.clone();
						boolean add = true;
						for (Role otherRole : rolesToProcess) {
							// System.out.println("processing role : "+otherRole.getName());
							if (otherRole == gapRole) {
								if (!starter.gapOut(otherRole, gapFillerType, gapFiller,
										slotConnectionTracker.getDirectConnectBindingArrangement(otherRole, gapFillerType))) {
									add = false;
									break;
								}
								cost = cost + constituentLocalityCostTable.getNonLocalCost(otherRole);
							}
							else if (starter.getLeftOverConstituents().contains(otherRole)) {
								if (starter.getLeftOverComplements().contains(otherRole)) {
									starter.omit(otherRole);
								}
								else {
									starter.omitOptional(otherRole);
								}
								cost = cost + constituentLocalityCostTable.getOmissionCost(otherRole);
								// System.out.println(cost);
							}
						}
						if (add) {
							retList.add(new Pair<T, Double>(starter, cost));
						}
					}
				}
				catch (TypeSystemException tse) {
					throw new ParserException("Type Exception: " + tse.toString());
				}
			}
		}
		return retList;

	}

	class RobustParserState {
		RobustParserState aSubi = null;
		T primaryAnalysis;
		RobustParserState ancestor;
		int leftIndex;
		int rightIndex;
		int stateID = -1;
		RobustParserState leftSibling;
		List<T> rightSiblings;
		double prevRootSemCost = 0;
		double completedRightSiblingSemanticCost = 0;
		double cxnalLL;
		int nextWord = -1;

		RobustParserState(T analysis, RobustParserState ancestor, double cxnalLL) {
			setUp(analysis, ancestor, cxnalLL, null, null, ancestor == null ? 0.0 : ancestor.prevRootSemCost,
					ancestor == null ? 0.0 : ancestor.completedRightSiblingSemanticCost);
		}

		RobustParserState(T analysis, RobustParserState ancestor, double cxnalLL, RobustParserState leftSibling,
				List<T> rightSiblings, double prevSemCost, double rightSibSemCost) {
			if (rightSiblings != null) {
				rightSiblings = new LinkedList(rightSiblings);
			}
			setUp(analysis, ancestor, cxnalLL, leftSibling, rightSiblings, prevSemCost, rightSibSemCost);
		}

		private void setUp(T analysis, RobustParserState ancestor, double cxnalLL, RobustParserState leftSibling,
				List<T> rightSiblings, double prevRootSemCost, double completedRightSemanticCost) {
			this.primaryAnalysis = analysis;
			this.ancestor = ancestor;
			this.leftSibling = leftSibling;
			this.leftIndex = primaryAnalysis.getSpanLeftIndex();
			this.rightIndex = primaryAnalysis.getSpanRightIndex();
			stateID = statecounter++;
			this.cxnalLL = cxnalLL;
			this.rightSiblings = rightSiblings;
			this.prevRootSemCost = prevRootSemCost;
			this.completedRightSiblingSemanticCost = completedRightSemanticCost;
			nextWord = rightIndex;
			if (rightSiblings != null) {
				for (Analysis a : rightSiblings) {
					if (a.getSpanRightIndex() > nextWord) {
						nextWord = a.getSpanRightIndex();
					}
				}
			}

		}

		void addRightSibling(T sib, List<T> rightSiblingSiblings, int nw) {
			if (rightSiblings == null) {
				rightSiblings = new LinkedList<T>();
			}
			T a = (T) sib.clone();
			CxnalSpan completeSpan = new CxnalSpan(null, a.getHeadCxn(), a.getFeatureStructure().getMainRoot().getID(),
					a.getSpanLeftIndex(), a.getSpanRightIndex());
			if (a.spans == null) {
				a.spans = new LinkedList<CxnalSpan>();
			}
			a.spans.add(0, completeSpan);
			rightSiblings.add(a);
			if (rightSiblingSiblings != null) {
				rightSiblings.addAll(rightSiblingSiblings);
			}
			this.nextWord = nw;
		}

		int getSpanLeftIndex() {
			return leftIndex;
		}

		double getLogLikelihood() {
			analysisFactory.cleanUp(primaryAnalysis);
			return getConstructionalLogLikelihood() + primaryAnalysis.computeSemanticCost() + prevRootSemCost
					+ completedRightSiblingSemanticCost;
		}

		double getConstructionalLogLikelihood() {
			return cxnalLL;
		}

		boolean hasGapFiller() {
			return primaryAnalysis.hasGapFiller() || (ancestor != null && ancestor.hasGapFiller());
		}

		Construction getGapFillerType() {
			if (primaryAnalysis.hasGapFiller()) {
				return primaryAnalysis.getGapFillerType();
			}
			else if (ancestor != null) {
				return ancestor.getGapFillerType();
			}
			return null;
		}

		PossibleSemSpecs getGapFiller() {
			if (primaryAnalysis.hasGapFiller()) {
				return primaryAnalysis.getGapFillerPSS();
			}
			else if (ancestor != null) {
				return ancestor.getGapFiller();
			}
			return null;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder("\t\tSTATE[");
			sb.append(stateID).append("] (").append(getSpanLeftIndex()).append(", ").append(this.rightIndex).append(")");
			sb.append(" LL:").append(AnalysisUtilities.formatDouble(getLogLikelihood())).append(" ");
			sb.append(" CLL:").append(AnalysisUtilities.formatDouble(getConstructionalLogLikelihood()));
			sb.append(" ANC:");
			if (ancestor == null) {
				sb.append("null");
			}
			else {
				sb.append(ancestor.stateID).append(" AncLL:")
						.append(AnalysisUtilities.formatDouble(ancestor.getLogLikelihood()));
			}
			sb.append(" HC: ").append(primaryAnalysis.getHeadCxn().getName());
			sb.append(" FIN:").append(primaryAnalysis.completed());
			if (!primaryAnalysis.completed()) {
				sb.append("LeftOvers: ").append(primaryAnalysis.getLeftOverComplements());
			}
			if (ROBUST) {
				sb.append(" LS:");
				if (leftSibling == null) {
					sb.append("null");
				}
				else {
					sb.append(leftSibling.stateID);
				}
			}
			sb.append("\n");
			return sb.toString();
		}

		public String stackToString() {
			return stackToString(0);
		}

		public String stackToString(double normalizer) {
			StringBuilder sb = new StringBuilder("Stack:\n");
			List<RobustParserState> stack = new ArrayList<RobustParserState>();
			RobustParserState current = this;
			while (current != null) {
				stack.add(current);
				current = current.ancestor;
			}
			int indent = 2;
			for (int i = stack.size() - 1; i >= 0; i--) {
				AnalysisUtilities.indent(sb, indent);
				sb.append(stack.get(i).primaryAnalysis.getHeadCxn().getName()).append("[").append(stack.get(i).stateID)
						.append("] ");
				sb.append("(").append(stack.get(i).getSpanLeftIndex()).append(", ").append(stack.get(i).rightIndex)
						.append(")\t");

				if (stack.get(i).primaryAnalysis.getLeftOverConstituents() != null) {
					sb.append("Open constituents [ ");
					for (Role r : stack.get(i).primaryAnalysis.getLeftOverConstituents()) {
						sb.append(r.getName()).append(":").append(r.getTypeConstraint().getType()).append(", ");
					}
					sb.delete(sb.length() - 2, sb.length()).append(" ]\n");
				}
				if (stack.get(i).primaryAnalysis.localRoleSpans != null) {
					for (CxnalSpan cspan : stack.get(i).primaryAnalysis.localRoleSpans) {
						AnalysisUtilities.indent(sb, indent + 2);
						sb.append(cspan.getRole() != null ? cspan.getRole().getName() : "UNNAMED").append(": ")
								.append(cspan.getType() != null ? cspan.getType().getName() : "UNTYPED").append(" (")
								.append(cspan.left).append(", ").append(cspan.right).append(") ");

						sb.append(" -> \" ");
						for (int word = cspan.left; word < cspan.right; word++) {
							sb.append(lastUtteranceProcessed.getElement(word).getOrthography()).append(" ");
						}
						sb.append("\"\n");
					}
				}
				sb.append("\n");
				if (i > 0) {
					AnalysisUtilities.indent(sb, indent + 2);
					sb.append("...\n\n");
				}
				indent = indent + 4;
			}

			sb.append("\n  Bindings:\n");
			PossibleSemSpecs psslist = primaryAnalysis.getPossibleSemSpecs();
			if (psslist.getSemSpecList().size() == 0 || psslist.getSemSpecList().size() == 1
					&& psslist.getSemSpecList().get(0).buggyBindingsView().length() == 0) {
				sb.append("    No notable bindings yet\n");
			}
			else {
				for (PartialSemSpec pss : psslist.getSemSpecList()) {
					sb.append(pss.buggyBindingsView()).append("\n");
				}
			}
			sb.append("\n Prob:").append(AnalysisUtilities.formatDouble(4, Math.exp(getLogLikelihood() - normalizer)));
			sb.append("  ULL:").append(AnalysisUtilities.formatDouble(4, getLogLikelihood()));
			sb.append("  UCLL:").append(AnalysisUtilities.formatDouble(4, getConstructionalLogLikelihood()));
			sb.append("\n");
			return sb.toString();
		}

	}

	private void debugPrint(String message) {
		if (DEBUG) {
			System.out.println(message);
		}
	}

	private <T> PriorityQueue<T> prune(PriorityQueue<T> queue, int maxBeamWidth) {
		PriorityQueue<T> prunedQueue = new PriorityQueue<T>();
		int i = 0;
		while (queue.size() > 0) {
			double score = queue.getPriority();
			T p = queue.next();
			prunedQueue.add(p, score);
			i++;
			if (i >= maxBeamWidth) {
				break;
			}
		}

		return prunedQueue;
	}

	private void addToCompletedQ(List<T> completeAnalysis, double logLikelihood) {
		completeAnalyses.add(completeAnalysis, logLikelihood);
	}

	private void addStatesToQ(List<RobustParserState> states, boolean nextIterQ) {
		if (states == null) {
			return;
		}
		for (RobustParserState state : states) {
			if (nextIterQ) {
				debugPrint("\tAdding state to next iter Q \n" + state.toString());
				nextIterationQ.add(state, state.getLogLikelihood());
			}
			else {
				double pNext = pNextInputGivenStack(state.nextWord, state);
				if (pNext > Double.NEGATIVE_INFINITY) {
					debugPrint("\tAdding state to working Q (pnext=" + pNext + ")\n" + state.toString());
					workingQ.add(state, state.getLogLikelihood() + pNext);
				}
				else {
					debugPrint("\tIgnoring (pnext=" + pNext + ")\n" + state.toString());
				}
			}
		}
	}

	public String getParserLog() {
		if (parserLog == null) {
			return "";
		}
		else {
			return parserLog.toString();
		}
	}

}

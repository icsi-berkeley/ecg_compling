package compling.parser.ecgparser;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import compling.context.ContextModel;
import compling.context.ContextModelCache;
import compling.grammar.ComplexCacheException;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.RobustParser;
import compling.parser.ecgparser.ECGMorph.MorphEntry;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.AnalysisFactory;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.AnalysisInContextFactory;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.CloneTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentExpansionCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentLocalityCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentsToSatisfyCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentsToSatisfyTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.DumbConstituentExpansionCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ReachabilityTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.TypeToConstituentsTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.TypeToConstituentsTable.AttachPoint;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.UnifyTable;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotChainTables;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotConnectionTracker;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement;
import compling.parser.ecgparser.PossibleSemSpecs.PartialSemSpec;
import compling.util.IdentityHashSet;
import compling.util.Pair;
import compling.util.PriorityQueue;
import compling.util.StringUtilities;
import compling.util.math.SloppyMath;
import compling.utterance.Sentence;
import compling.utterance.Utterance;
import compling.utterance.Word;
import compling.parser.ecgparser.ECGMorph;
import compling.parser.ecgparser.ECGTokenReader;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;

public class LeftCornerParser<T extends Analysis> implements RobustParser<T> {

  /** CONSTANTS THAT GET SET BY THE setParameters method */
  boolean DEBUG = false;
  private int MAXBEAMWIDTH = 3;
  private int BEAMSIZE = 3;
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
  
  private ECGMorph morpher;
  private ECGTokenReader tokenReader;
  private ECGMorphTableReader morphTable;

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
  private ConstituentLocalityCostTable constituentLocalityCostTable;
  private ConstituentExpansionCostTable constituentExpansionCostTable;
  private ConstituentsToSatisfyCostTable constituentsToSatisfyCostTable;
  private AnalysisFactory<T> analysisFactory;
  private SlotConnectionTracker slotConnectionTracker;
  private int statecounter = 0;
  private int processedStates = 0;
  

  
  private ArrayList<ArrayList<Construction>> constructionInput; 
  private ArrayList<ArrayList<MorphTokenPair>> morphToken;
  
  /**This is intended to be a cache of type Cxns; when type-identical utterances appear, just retrieve from cache.
  Will still need to integrate MorphToken information into SemSpec effectively. */
  private HashMap<TypeCacheEntry, PriorityQueue<List<T>>> typeCache;

  
  

  
  private Construction[][] input;
  private Construction RootCxn;
  private Role RootCxnConstituent;
  private StringBuilder parserLog;
  
 private HashMap<String, ArrayList<String[]>> constructional_morphTable;
  private HashMap<String, ArrayList<String[]>> meaning_morphTable;
  
 
 

  private long constructorTime;
  private double currentEntropy = 0;
  private double lnTwo = Math.log(2);
  private double lastNormalizer = 0;
  
  


  public void setParameters(boolean robust, boolean debug, int maxBeamWidth, int parsesToReturn, double extraRootPenalty, int beamSize) {
    this.ROBUST = robust;
    this.DEBUG = debug;
    this.MAXBEAMWIDTH = maxBeamWidth;
    this.BEAMSIZE = beamSize;
    this.PARSESTORETURN = parsesToReturn;
    if (extraRootPenalty > 0) {
      extraRootPenalty = 0 - extraRootPenalty;
    }
    this.EXTRAROOTPENALTY = extraRootPenalty;
    // setPsychoMode();
  }

  public void setPsychoMode() {
    PSYCHOOUTPUT = true;
  }

  public LeftCornerParser(compling.grammar.ecg.Grammar grammar, AnalysisFactory<T> analysisFactory) throws IOException {
    this(grammar, analysisFactory, new DumbConstituentExpansionCostTable(new LCPGrammarWrapper(grammar)));
  }

  // public LeftCornerParser(compling.grammar.ecg.Grammar grammar,
  // AnalysisFactory<? extends Analysis> factory, ConstituentExpansionCostTable
  // cect)
  // throws IOException {
  // this(grammar, factory, new ParamFileConstituentExpansionCostTableCFG(new
  // LCPGrammarWrapper(grammar),
  // cect));
  // }

  public LeftCornerParser(compling.grammar.ecg.Grammar ecgGrammar, AnalysisFactory analysisFactory,
          ConstituentExpansionCostTable cect) throws IOException {
    constructorTime = System.currentTimeMillis();
    
    this.typeCache = new HashMap<TypeCacheEntry, PriorityQueue<List<T>>>();

    this.ecgGrammar = ecgGrammar;
    this.grammar = new LCPGrammarWrapper(ecgGrammar);
    
    this.tokenReader = this.ecgGrammar.getTokenReader();
	this.morpher = this.ecgGrammar.getMorpher();
    
    this.morphTable = new ECGMorphTableReader(this.grammar);
    

    
    this.analysisFactory = analysisFactory;
    this.cxnTypeSystem = grammar.getCxnTypeSystem();
    this.contextModel = grammar.getContextModel();
    this.cloneTable = new CloneTable<T>(grammar, analysisFactory);
    this.unifyTable = new UnifyTable(grammar, cloneTable);
    // this.csscf = new CanonicalSemanticSlotChainFinder(grammar,
    // this.cloneTable);
    this.constituentsToSatisfyTable = new ConstituentsToSatisfyTable(grammar);
    this.typeToConstituentsTable = new TypeToConstituentsTable(grammar, this.unifyTable);
    this.constituentLocalityCostTable = new ConstituentLocalityCostTable(grammar);
    this.constituentExpansionCostTable = cect;
    // debugPrint("----------------------------------------------------------------------------------");
    this.constituentsToSatisfyCostTable = new ConstituentsToSatisfyCostTable(grammar, this.constituentsToSatisfyTable,
            this.constituentLocalityCostTable);
    this.reachabilityTable = new ReachabilityTable(grammar, constituentsToSatisfyCostTable,
            constituentExpansionCostTable, this.unifyTable, this.constituentLocalityCostTable);
    SlotChainTables slotChainTables = new SlotChainTables(grammar, cloneTable);
    this.slotConnectionTracker = new SlotConnectionTracker(grammar, cloneTable, unifyTable, slotChainTables,
            constituentsToSatisfyCostTable, constituentExpansionCostTable, constituentLocalityCostTable);
    // debugPrint(reachabilityTable.toString());
    cloneTable.update();
    // Analysis.setFormatter(new CombinedAnalysisInContextFormatter(grammar,
    // slotChainTables));
    // debugPrint("--------------- next compute processing threshold ---------------");
    // debugPrint(String.valueOf(System.currentTimeMillis()));
    // this.PROCESSINGTHRESHOLD = computeProcessingThreshold();
    constructorTime = System.currentTimeMillis() - constructorTime;
    RootCxn = grammar.getConstruction(ECGConstants.ROOT);
    RootCxnConstituent = (Role) RootCxn.getConstructionalBlock().getElements().toArray()[0];

			
			
	this.constructional_morphTable = this.morphTable.getConstructionalTable();
	this.meaning_morphTable = this.morphTable.getMeaningTable();
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
  
  
  // TODO: Returns a boolean if "cxn" is compatible with type. Noun-Block is compatible with "noun", etc.
  private boolean isCompatible(Construction cxn, String type) {
	  Set<String> parents = cxn.getParents();
	  if (cxn.getName().equals(type)) {
		  return true;
	  }
	  if (parents.contains(type)) {
		  return true;
	  } 
	  else if (parents.contains("RootType")) {
		  return false;
	  } else {
		  for (String p : parents) {
			  Construction c = this.grammar.getConstruction(p);
			  if (isCompatible(c, type)) {
				  return true;
			  }
		  }
		  return false;
	  }
  }
  
  // TODO: Returns a boolean if "cxn" is compatible with type. Noun-Block is compatible with "noun", etc.
  private boolean isCompatible2(Construction cxn, String[] type) {
	  //Set<String> parents = cxn.getParents();
	  //List<String> types = Arrays.asList(type);
	  
	  TypeSystem ts = this.ecgGrammar.getConstructionTypeSystem();
	  for (String t : type) {
		  try {
			if (ts.subtype(ts.getInternedString(cxn.getName()), ts.getInternedString(t))) {
				  return true;
			  }
		} catch (TypeSystemException e) {
			throw new ParserException("Either " + cxn.getName() + " or " + t 
					+ " not found in the construction lattice. Check .morph file for inconsistencies.");
		}
	  } return false;
	  /*
	  
	  if (types.contains(cxn.getName())) {
		  return true;
	  }
	  if (!Collections.disjoint(parents, types)) {
		  return true;
	  }

	  else if (parents.contains("RootType")) {
		  return false;
	  } else {
		  for (String p : parents) {
			  Construction c = this.grammar.getConstruction(p);
			  if (isCompatible2(c, type)) {
				  return true;
			  }
		  }
		  return false;
	  }
	  */
	  
  }
  
  /** Reloads tokens and morphology instances. 
 * @throws IOException */
  public void reloadTokens() throws IOException {
	  this.tokenReader = this.ecgGrammar.getTokenReader();
	  this.morpher = new ECGMorph(this.ecgGrammar, this.tokenReader);
  }
  
  public Map<String, ArrayList<ECGToken>> getTokens() {
	  return tokenReader.getTokens();
  }
  
  public HashMap<String, List<MorphEntry>> getMorphInflections() {
	  return morpher.morphs;
  }
  
  //TODO: Finish this method. Should take values from morphTokens and insert them into respective analyses.
  public PriorityQueue<List<T>> cacheIntoAnalyses(PriorityQueue<List<T>> analyses, ArrayList<ArrayList<MorphTokenPair>> morphTokens, TypeCacheEntry tce) {
	  PriorityQueue<List<T>> test = analyses.clone();
	  ArrayList<String> seen = new ArrayList<String>();
	  ArrayList<ArrayList<Construction>> cxnList = tce.getCxnList();
	  for (ArrayList<Construction> cxns : cxnList) {
		  for (Construction cxn : cxns) {
			  if (cxn != null) {
				  if (seen.contains(cxn.getName())) {
					  throw new ComplexCacheException("This was too complex to cache into.");
				  } else {
					  seen.add(cxn.getName());
				  }
			  }
		  }
	  }
	  while (test.hasNext()) {
		  List<T> t = test.next();
		  for (T analysis : t) {
			  for (int spanIndex=0; spanIndex < analysis.getSpans().size(); spanIndex++) {
				  // TODO: this method doesn't correctly assign to the right cxns.
				  // Need a way to determine WHICH Nountype, etc.
				  CxnalSpan cxns = analysis.getSpans().get(spanIndex);
				//			  }
//			  for (CxnalSpan cxns : analysis.getSpans()) {
				  int id = cxns.getSlotID();
				  Slot comparator = analysis.getFeatureStructure().getSlot(id);
				  if (comparator != null) {
					  String replaced = comparator.getTypeConstraint().toString().replace("@CONSTRUCTION", "");
					  for (int index1=0; index1 < cxnList.size(); index1++) {
						  for (int index2=0; index2 < cxnList.get(index1).size(); index2 ++) {
							  Construction inputCxn = cxnList.get(index1).get(index2);
							  ECGToken token = morphTokens.get(index1).get(index2).token;
							  if (inputCxn != null && token != null && replaced.equals(inputCxn.getName())) {
								  for (Entry<Role, Slot> slots : comparator.getFeatures().entrySet()) {
									  if (slots.getKey().getName().equals("m")) {
										  //System.out.println(slots.getValue().getTypeConstraint().);
										  for (Entry<Role, Slot> featureSlots : slots.getValue().getFeatures().entrySet()) {
											  for (Constraint c : token.constraints) {
												  String[] args = c.getArguments().get(0).toString().split("\\.");
												  if (args.length > 0) {
													  if (args[args.length-1].equals(featureSlots.getKey().toString())) {
														  TypeSystem ts = this.ecgGrammar.getOntologyTypeSystem();
														  if (featureSlots.getValue().hasAtomicFiller()) {
															  featureSlots.getValue().setAtom(c.getValue());
														  } else {
															  String child = c.getValue().substring(1, c.getValue().length()).trim();
															  String tsItem = ts.getInternedString(child);
															  featureSlots.getValue().getTypeConstraint().setType(tsItem);
														  }
													  }
												  }
											  }
										  }
									  }
								  }
							  }
						  }
					  }
				  }
			  }
		  }
	  }
	  return analyses.clone();
  }
  
  public void setBeamWidth(int width) {
	  this.MAXBEAMWIDTH = width;
  }
  

  public PriorityQueue<List<T>> getBestPartialParses(Utterance<Word, String> utterance) {
	  
	
	//this.ecgGrammar.readTokens();
	this.tokenReader = this.ecgGrammar.getTokenReader();
	    //this.tokenReader = new ECGTokenReader(new LCPGrammarWrapper(this.ecgGrammar));
	this.morpher = this.ecgGrammar.getMorpher();
	
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

    //input = new Construction[utterance.size() + 1][];

    constructionInput = new ArrayList<ArrayList<Construction>>();
    morphToken = new ArrayList<ArrayList<MorphTokenPair>>();
    
    ArrayList<String> unknowns = new ArrayList<String>();

    for (int i = 0; i < utterance.size(); i++) {
      try {
    	String wordform = utterance.getElement(i).getOrthography();

    	Set<String> lems = this.morpher.getLemmas(wordform);
    	constructionInput.add(new ArrayList<Construction>());
    	morphToken.add(new ArrayList<MorphTokenPair>());

    	for (String lemma : lems) {
	    	try {
		        List<ECGToken> tokens = this.tokenReader.getToken(lemma);
		        for (ECGToken token : tokens) {
		        	Construction parent = token.parent;
		        	
		        	String[] inflections = morpher.getInflections(lemma, wordform);
		        	for (String inf : inflections) {
		        		//int what = this.meaning_morphTable.get(inf).length - 1;
		        		String[] morphType = new String[]{};
		        		try {
		        			morphType = this.meaning_morphTable.get(inf).get(1);
		        		} catch (Exception e) {
		        			throw new ParserException("Morphology table does not contain " + inf);
		        		}
		        		if (isCompatible2(parent, morphType)) { //this.meaning_morphTable.get(inf).get(1))) {
		        			
		        			constructionInput.get(i).add(parent);
		        			morphToken.get(i).add(new MorphTokenPair(inf, token));
		        		}
		        		/*
		        		if (isCompatible(parent, this.meaning_morphTable.get(inf)[what])) {

		        		} */
		        	}
		        }		        
	    	} catch (GrammarException g) {
	    		debugPrint("Unknown input lemma: " + lemma);
	    	}	    	
    	} 	
      } catch (GrammarException g) {
    	  debugPrint("Unknown input lemma: " + utterance.getElement(i).getOrthography());  	  
      }
      // This block will handle numbers
      try {
    	  String potentialNumber = utterance.getElement(i).getOrthography();
    	  try {
    		  double value = Double.parseDouble(potentialNumber);
    		  Construction cxn = grammar.getConstruction("NumberType");
              if (i >= constructionInput.size()) {
            	  constructionInput.add(new ArrayList<Construction>());  
              }
              if (i >= morphToken.size()) {
            	  morphToken.add(new ArrayList<MorphTokenPair>());
              }
              constructionInput.get(i).add(cxn);
              ECGTokenReader.ECGToken tok = this.tokenReader.new ECGToken(); //new ECGTokenReader.ECGToken();
              tok.constraints = new ArrayList<Constraint>();
              tok.constraints.add(new Constraint("<--", new SlotChain("self.m.value"), StringUtilities.addQuotes(potentialNumber)));
              String morph = "Singular";
              if (value != 1) {
            	  morph = "Plural";
              }
              
              MorphTokenPair mtp = new MorphTokenPair(morph, tok);
              morphToken.get(i).add(mtp);
    	  } catch (NumberFormatException e) {
    		  throw new GrammarException("Number not found.");
    	  }
    	  //long n = potentialNumber
      } catch (GrammarException g) {
    	  debugPrint("Could not identify number in " + utterance.getElement(i).getOrthography() + 
    			  " or construction NumberType not found in grammar.");
      }
      // This block will handle lexical constructions
      try { 
          if (i >= constructionInput.size()) {
        	  constructionInput.add(new ArrayList<Construction>());  
          }
          if (i >= morphToken.size()) {
        	  morphToken.add(new ArrayList<MorphTokenPair>());
          }
          List<Construction> lexicalCxns = grammar.getLexicalConstruction(StringUtilities.addQuotes(utterance.getElement(
                  i).getOrthography()));
          constructionInput.get(i).addAll(lexicalCxns);
          for (int k = 0; k < lexicalCxns.size(); k++) {
        	  //morphToken.get(i).add(mt);
        	  morphToken.get(i).add(new MorphTokenPair(null, null));
          }          
        } catch (GrammarException g) {
        	debugPrint("Unknown input lexeme: " + utterance.getElement(i).getOrthography());
        	
        	List<Construction> lexicalCxns = grammar.getLexicalConstruction(StringUtilities
	               .addQuotes(ECGConstants.UNKNOWN_ITEM));
        	//Construction lexicalCxns = grammar.getConstruction("NounType");
        	if (constructionInput.get(i).isEmpty()) {
        		//constructionInput.get(i).add(lexicalCxns);
        		unknowns.add(utterance.getElement(i).getOrthography());
        		constructionInput.get(i).add(lexicalCxns.get(0));
        		morphToken.get(i).add(new MorphTokenPair(null, null));
        	}
        }
    }   
    
    if ((unknowns.size() > 0) && (!ROBUST)) {
    	 throw new ParserException("Analysis not possible, unknown words identified: " + unknowns.toString() + ".");
    }
    
    
    morphToken.add(new ArrayList<MorphTokenPair>());
    morphToken.get(utterance.size()).add(new MorphTokenPair(null, null));
   
    
    constructionInput.add(new ArrayList<Construction>());
    constructionInput.get(utterance.size()).add(null);
    
    TypeCacheEntry tcEntry = new TypeCacheEntry(constructionInput, morphToken);


    /*
    for (Entry<TypeCacheEntry, PriorityQueue<List<T>>> item : typeCache.entrySet()) {
    	if (tcEntry.compareEntry(item.getKey())) {
    		try {
    			PriorityQueue<List<T>> analysesReturn = cacheIntoAnalyses(item.getValue().clone(), morphToken, tcEntry);
    			System.out.println("-------Retrieving from type cache---------");
    			System.out.println("Retrieved from cache.");
    			return analysesReturn;
    		} catch (ComplexCacheException e) {
    			System.out.println(e.getMessage());
    			break;
    		}
    	}
    } */

    T root = cloneTable.get(RootCxn, 0);
    RobustParserState rootState = new RobustParserState(root, null, 0);

    if (ROBUST) {
      workingQ = new PriorityQueue<RobustParserState>();
      addStatesToQ(pushLexicalState(rootState, 0), false);
      leftCornerAgendaParse(utterance);
      debugPrint("\nNumber of states created: " + statecounter + "; Number of states processed: " + processedStates);
      if (completeAnalyses.size() == 0) {
        // throw new ParserException("No complete analysis found \n\n " +
        // parserLog);
        throw new ParserException("No complete analysis found for: " + utterance.toString());
      }
      else {
    	typeCache.put(tcEntry, this.completeAnalyses.clone());  // typeCache
        return completeAnalyses;
      }
    }
    else {

      nextIterationQ = new PriorityQueue<RobustParserState>();
      addStatesToQ(pushLexicalState(rootState, 0), true);

      workingQ = nextIterationQ;
      workingQ = prune(workingQ, MAXBEAMWIDTH, BEAMSIZE);
      for (int i = 1; i <= utterance.size(); i++) {
        workingQ = prune(makeNewCandidates(utterance, i), MAXBEAMWIDTH, BEAMSIZE);
      }
      debugPrint("\nNumber of states created: " + statecounter + "; Number of states processed: " + processedStates);
      if (completeAnalyses.size() == 0) {
        // throw new ParserException("No complete analysis found \n\n " +
        // parserLog);
        throw new ParserException("No complete analysis found for: " + utterance.toString());
      }
      else {
    	PriorityQueue<List<T>> toReturn = prune(completeAnalyses, PARSESTORETURN, BEAMSIZE);
//    	System.out.println("------- Inserting into TYPE CACHE (2). ---------");
    	typeCache.put(tcEntry, toReturn.clone());
        return toReturn; //prune(completeAnalyses, PARSESTORETURN);
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

  /**
   * @param utterance
   * @param index
   * @return
   */
  private PriorityQueue<RobustParserState> makeNewCandidates(Utterance<Word, String> utterance, int index) {
    // if
    // (true){System.out.println("\nmake next candidates with index="+index+" Num states created="+statecounter);}
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
    }
    if (PSYCHOOUTPUT || DEBUG) {
      // System.out.println("\nSurviving parser states incorporating up through the word \""+utterance.getElement(index
      // -1).getOrthography()+"\"\n\n");
      parserLog.append("\nSurviving parser states incorporating up through the word \""
              + utterance.getElement(index - 1).getOrthography() + "\"\n\n");
      parserLog.append("-------------------------------------------------------------------------------------------\n");
      for (RobustParserState rps : rpsList) {
        parserLog.append(rps.stackToString(normalizer)).append("\n");
        entropy = entropy + Math.exp(rps.getLogLikelihood() - normalizer) * -1 * (rps.getLogLikelihood() - normalizer)
                / lnTwo;
      }
      parserLog.append("\t\tP(w_i|...): ").append(
              AnalysisUtilities.formatDouble(6, Math.exp(normalizer - lastNormalizer)));
      parserLog.append("\tCurrent Entropy: ").append(AnalysisUtilities.formatDouble(4, entropy));
      parserLog.append("    Difference in Entropy: ")
              .append(AnalysisUtilities.formatDouble(4, entropy - currentEntropy)).append("\n");
      currentEntropy = entropy;
      parserLog.append("-------------------------------------------------------------------------------------------\n");
      lastNormalizer = normalizer;
    }

    // PriorityQueue<RobustParserState> safetyQ = workingQ.clone();
    
    // only used when we out of stuff on the workingQ and there is nothing on the nextIterationQ

    while (workingQ.size() > 0) {

      RobustParserState current = workingQ.next();

      if (stoppingCriteria(index, utterance.size(), current.getLogLikelihood())) {
        break;
      }
      debugPrint("\nPROCESSING: " + current.toString());
      processedStates++;
      if (current.primaryAnalysis.completed()) {

        if (current.getSpanLeftIndex() == 0 && index == utterance.size() && current.ancestor == null) {
          List<T> completedAnalysis = new ArrayList<T>();
          T a = (T) current.primaryAnalysis.clone();
          CxnalSpan completeSpan = new CxnalSpan(null, a.getHeadCxn(), a.getFeatureStructure().getMainRoot().getID(),
                  a.getSpanLeftIndex(), a.getSpanRightIndex());
          if (a.spans == null) {
            a.spans = new LinkedList<CxnalSpan>();
          }
          a.spans.add(0, completeSpan);
          completedAnalysis.add(a);
          if (ROBUST && current.rightSiblings != null) {
            completedAnalysis.addAll(current.rightSiblings);
          }
          // it's an analysis that covers and has no ancestors
          addToCompletedQ(completedAnalysis, current.getLogLikelihood()); 
          
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
          CxnalSpan completeSpan = new CxnalSpan(null, a.getHeadCxn(), a.getFeatureStructure().getMainRoot().getID(),
                  a.getSpanLeftIndex(), a.getSpanRightIndex());
          if (a.spans == null) {
            a.spans = new LinkedList<CxnalSpan>();
          }
          a.spans.add(0, completeSpan);
          completedAnalysis.add(a);
          if (ROBUST && current.rightSiblings != null) {
            completedAnalysis.addAll(current.rightSiblings);
          }

          // it's an analysis that covers and has no ancestors
          addToCompletedQ(completedAnalysis, current.getLogLikelihood());

          // System.out.println("\tFound Complete Analysis "+current.getLogLikelihood());
          debugPrint("\tFound Complete Analysis");

        }
        else {
          if (ROBUST && current.ancestor == null && current.leftSibling != null) { 
            // gotta switch back to the leftsib stack
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
        if (constituentExpansionCostTable.getConstituentExpansionCost(role, current.primaryAnalysis.getHeadCxn()) > Double.NEGATIVE_INFINITY) {
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
      for (AttachPoint ap : typeToConstituentsTable.get(current.primaryAnalysis.getHeadCxn())) {

        double proposeCost = computeReachability(current.ancestor.primaryAnalysis, ap.cxn,
                current.ancestor.hasGapFiller() && !current.ancestor.primaryAnalysis.alreadyUsedGapFiller()
                        && !current.primaryAnalysis.alreadyUsedGapFiller(), current.ancestor.getGapFillerType());

        if (proposeCost > Double.NEGATIVE_INFINITY) { // && !(index ==
          // utteranceSize &&
          // ap.cxn ==
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
    int iter = index;
    //for (int iter=0; iter < constructionInput.size(); iter++) {
    	//for (int second=0; second < constructionInput.get(iter).size(); second ++) {
    for (int second=0; second < constructionInput.get(index).size(); second++) {
		Construction cxn = constructionInput.get(iter).get(second);
		double reachabilityCost = computeNormalizedReachability(ancestor.primaryAnalysis, cxn,
	              ancestor.hasGapFiller() && !ancestor.primaryAnalysis.alreadyUsedGapFiller(), ancestor.getGapFillerType());
  		MorphTokenPair extra_info = morphToken.get(iter).get(second);
	    if (reachabilityCost > Double.NEGATIVE_INFINITY && cxn != null) {
	    	T lex_analysis = cloneTable.get(cxn, index);
	    	//System.out.println(lex_analysis);
	    	T ultimate = (T) lex_analysis.clone(); 	    	
	    	if (extra_info.morph != null) {
	    		String morph = extra_info.morph;
	    		String[] mConstraint = this.meaning_morphTable.get(morph).get(0);
	    		for (int k = 0; k < mConstraint.length - 1; k += 2) {
	    			ultimate.addConstraint(UnificationGrammar.generateConstraint(mConstraint[k+1]), mConstraint[k]);
	    		}
	    		String[] con_constraint = new String[]{};
	    		try {
	    			con_constraint = this.constructional_morphTable.get(morph).get(0);
	    		} catch (Exception e) {
	    			throw new ParserException("Constructional morphology table does not contain morph: " + morph + ".");
	    		}
	        	for (int k = 0; k < con_constraint.length - 1; k += 2) {
	        		ultimate.addConstraint(UnificationGrammar.generateConstraint(con_constraint[k+1]), con_constraint[k]);
	        	}
	    	}
	    	if (extra_info.token != null) {
	    		ECGTokenReader.ECGToken token = extra_info.token;
	    		for (Constraint c : token.constraints) {
	    			ultimate.addConstraint(UnificationGrammar.generateConstraint(c.getValue()), c.getArguments().get(0).toString());
	    		}
	    	}
	    	ultimate.advance();
	        if (incorporateAncSem(ancestor, cxn, ultimate)) {
	            RobustParserState rps = new RobustParserState(ultimate, ancestor, reachabilityCost
	                    + ancestor.getConstructionalLogLikelihood());
	            // System.out.println("And finally here.");
	            results.add(rps);
	        }
	    } else {
     		debugPrint("\t\t" + ancestor.primaryAnalysis.getHeadCxn().getName() + " cannot generate "
               + cxn.getName());
	    }
    }
    if (results.size() == 0) {
    	debugPrint("no results");
    	//System.out.println("no results");
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
      if (a.completed()) { // only need to worry about this because of
                           // extraposed constituents
        RobustParserState rps = new RobustParserState(a, p.ancestor,
                p.getConstructionalLogLikelihood() + finishingCost, p.leftSibling, p.rightSiblings, p.prevRootSemCost,
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
      // TODO: *** for (int i = 0; i < input[index].length; i++) {
    	for (int i = 0; i < constructionInput.get(index).size(); i++) {
    		robustTerm = SloppyMath.logAdd(robustTerm, reachabilityTable.reachable(RootCxnConstituent, constructionInput.get(index).get(i)));//input[index][i]));
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


    for (int i = 0; i < constructionInput.get(index).size(); i++) {
      Construction lexicalCxn = constructionInput.get(index).get(i);

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
        // Set<Role> leftOverComps =
        // currentState.primaryAnalysis.getLeftOverComplements();
        double normalizingReachability = computeReachability(currentState.primaryAnalysis, childType,
                currentState.hasGapFiller(), currentState.getGapFillerType());

        for (Role rSubDesc : leftOvers) { // this is the role that must
          // cover the state above it on
          // the stack
          // double normalizingReachability =
          // reachabilityTable.reachable(rSubDesc, childType);
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

          double expansionCost = rSubDescCost
                  + constituentExpansionCostTable.getConstituentExpansionCost(rSubDesc, childType);
          if (expansionCost > Double.NEGATIVE_INFINITY) {
            // this is the case where we attach directly and then generate
            // the next input from the remaining roles
            currentStateTotal = SloppyMath.logAdd(
                    currentStateTotal,
                    computeNormalizedReachability(newLeftOvers, lexicalCxn, completionCostSoFar + expansionCost
                            - normalizingReachability, currentState.hasGapFiller(), currentState.getGapFillerType()));
            currentStateFinishingCost = SloppyMath.logAdd(currentStateFinishingCost, expansionCost + rSubDescFinishCost
                    - normalizingReachability);
          }

          for (Construction hidden : grammar.getAllConcretePhrasalConstructions()) {

            double currentToHiddenReachability = computeReachability(currentState.primaryAnalysis, hidden,
                    currentState.hasGapFiller(), currentState.getGapFillerType());
            if (currentToHiddenReachability > Double.NEGATIVE_INFINITY) {
              Set<Role> hiddenLeftOvers = new IdentityHashSet<Role>();
              hiddenLeftOvers.addAll(hidden.getComplements());
              hiddenLeftOvers.addAll(hidden.getOptionals());
              for (Role rSubHiddenToChild : hiddenLeftOvers) {

                double hiddenRoleToChildReachability = reachabilityTable.reachable(rSubHiddenToChild, childType);
                if (hiddenRoleToChildReachability > Double.NEGATIVE_INFINITY) {

                  double costToDealWithChildType = getRemainingConstituentCosts(rSubHiddenToChild, hiddenLeftOvers,
                          currentState.hasGapFiller(), currentState.getGapFillerType()) + hiddenRoleToChildReachability;
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
                          computeNormalizedReachability(newLeftOvers, lexicalCxn, completionCostSoFar + rSubDescCost
                                  + hiddenFinishingCost + currentToHiddenReachability + costToDealWithChildType
                                  - normalizingReachability, currentState.hasGapFiller(),
                                  currentState.getGapFillerType()));
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
      if (ROBUST && tryLeftSib && leftSib != null && total < 0 && index < constructionInput.size() - 1) {       /////input.length - 1) {
        total = SloppyMath.logAdd(total, completionCostSoFar + pNextInputGivenStack(index, leftSib, false));
        if (total > 0) {
          total = 0;
        }
      }
      else if (ROBUST && tryLeftSib && leftSib != null && total < 0 && index == constructionInput.size() - 1) {		//input.length - 1) {
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

  private double computeNormalizedReachability(Set<Role> leftOverConstituents, Construction cxn, double additionalCost,
          boolean availableGapFiller, Construction gapFillerType) {
    if (cxn == null) {
      return Double.NEGATIVE_INFINITY;
    }
    double cost = Double.NEGATIVE_INFINITY;
    if (leftOverConstituents != null) {
      for (Role r : leftOverConstituents) {
        double reachabilityCost = reachabilityTable.normReachable(r, cxn);
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
    return getRemainingConstituentCosts(leftOver, constituentsToSatisfyTable.get(r), availableGapFiller, gapFillerType);
  }

  private double getRemainingConstituentCosts(Set<Role> leftOver, boolean availableGapFiller, Construction gapFillerType) {
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
        for (BindingArrangement ba : slotConnectionTracker.getBindingArrangement(r, cxn)) {
          // bas.addAll(slotConnectionTracker.getBindingArrangement(r,
          // cxn));
          bas.add(ba);
          penalties.add(penalty);
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
      if (a.advance(filledRole, filler.clone(),
              slotConnectionTracker.getDirectConnectBindingArrangement(filledRole, filler.getHeadCxn()))) {
        double attachCost = constituentExpansionCostTable.getConstituentExpansionCost(filledRole, filler.getHeadCxn())
                + constituentLocalityCostTable.getLocalCost(filledRole);
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

    // now on the return list we've got everything the case where everything
    // is omitted

    if (availableGapFiller) {
      // now for each role, we need to omit up to the role, gap out the role,
      // then omit the rest

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

  private <T> PriorityQueue<T> prune(PriorityQueue<T> queue, int maxBeamWidth, int beamSize) {
    PriorityQueue<T> prunedQueue = new PriorityQueue<T>();
    if (queue.size() == 0) {
    	return prunedQueue;
    }
    double bestScore = queue.getPriority();
    // if i >= maxBeamWidth or score > bestScore + beamSize, break
    int i = 0;
    while (queue.size() > 0) {
      double score = queue.getPriority();
      T p = queue.next();
      prunedQueue.add(p, score);
      i++;
      if (i >= maxBeamWidth || score > (bestScore + beamSize)) {
        break;
      }
    }
    return prunedQueue;
  }
  
  private <T> PriorityQueue<T> prune(PriorityQueue<T> queue, int maxBeamWidth) {
	    PriorityQueue<T> prunedQueue = new PriorityQueue<T>();
	    if (queue.size() == 0) {
	    	return prunedQueue;
	    }
	    double bestScore = queue.getPriority();
	    // if i >= maxBeamWidth or score > bestScore + beamSize, break
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

  public static void main(String[] args) throws IOException {
    String ontFile = null;
    ontFile = args[1];
    Grammar grammar = ECGGrammarUtilities.read(args[0], "ecg cxn sch grm", ontFile);

    // debugPrint(grammar);

    LeftCornerParser<AnalysisInContext> parser = new LeftCornerParser<AnalysisInContext>(grammar,
            new AnalysisInContextFactory(new LCPGrammarWrapper(grammar), grammar.getContextModel()
                    .getContextModelCache()));
    

    
    
    List<String> words = new ArrayList<String>();
    for (int i = 2; i < args.length; i++) {
      words.add(args[i]);
    }
    PriorityQueue<List<AnalysisInContext>> pqa = parser.getBestPartialParses(new Sentence(words, null, 0));
    int i = 0;
    while (pqa.size() > 0 && i < 5) {
      System.out.println("\n\nRETURNED ANALYSIS\n____________________________\n");
      System.out.println("Cost: " + pqa.getPriority());
      for (AnalysisInContext a : pqa.next()) {
        System.out.println(a);
      }
      i++;
    }
  }

}

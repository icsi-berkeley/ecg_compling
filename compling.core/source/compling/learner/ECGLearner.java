// =============================================================================
//File        : ECGLearner.java
//Author      : emok
//Change Log  : Created on Dec 7, 2006
//=============================================================================

package compling.learner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesIterator;
import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesLocalizer.DSRole;
import compling.annotation.childes.ChildesLocalizer.Participant;
import compling.annotation.childes.ChildesTranscript;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesTranscript.ChildesEvent;
import compling.annotation.childes.ChildesTranscript.ChildesItem;
import compling.annotation.childes.FeatureBasedEntity.FillerType;
import compling.annotation.childes.FeatureBasedEntity.SimpleFeatureBasedEntity;
import compling.context.ContextModel;
import compling.context.ContextUtilities;
import compling.context.MiniOntologyQueryAPI;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.gui.LearnerPrefs;
import compling.gui.LearnerPrefs.LP;
import compling.learner.AnalysisVerifier.Cat;
import compling.learner.AnalysisVerifier.Scorecard;
import compling.learner.contextfitting.BasicContextFitter;
import compling.learner.contextfitting.ContextFitter;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.contextfitting.GoldStandardFitter;
import compling.learner.featurestructure.LCAException;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.parser.ParserException;
import compling.parser.ecgparser.AnalysisInContext;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.NoECGAnalysisFoundException;
import compling.parser.ecgparser.SemSpecScorer.BasicScorer;
import compling.simulator.Simulator;
import compling.util.Pair;
import compling.util.PriorityQueue;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;

//=============================================================================

public class ECGLearner {

	public static enum RerankMethod {
		NONE, CONTEXTUAL_FIT, VERIFIER
	}

	ECGLearnerEngine engine = null;
	ECGAnalyzer analyzer = null;
	ContextModel contextModel = null;
	Simulator simulator = null;
	String xmlOutputPath = null;
	LearnerGrammar parsingGrammar = null;
	ContextFitter contextFitter = null;
	BasicScorer semanticModel = null;
	ChildesLocalizer localizer = new ChildesLocalizer();

	LearnerCentricAnalysis currentLCA = null;
	LearnerPrefs preferences = null;

	File baseDirectory = null;

	List<File> transcriptFiles = null;
	Iterator<File> fileIterator = null;
	ChildesTranscript currentTranscript = null;
	ChildesIterator childesIterator = null;

	ChildesClause nextClauseToAnalyze = null;
	boolean isAdultUtterance = false;
	GoldStandardAnnotation goldStandardAnnotation = null;
	SimpleFeatureBasedEntity speechActAnnotation = null;

	List<String> exceptionallyBad = new ArrayList<String>();
	List<String> unparsable = new ArrayList<String>();

	Pair<Scorecard, Scorecard> runningScore = new Pair<Scorecard, Scorecard>(new Scorecard(), new Scorecard());
	Pair<Scorecard, Scorecard> priorToRerankRunningScore = new Pair<Scorecard, Scorecard>(new Scorecard(),
			new Scorecard());
	double postRerankAnalyzerScore = 0.0;
	double preRerankAnalyzerScore = 0.0;

	int numAnalysis = 0;

	Set<String> jointAttention = new HashSet<String>();

	private static Logger logger = Logger.getLogger(ECGLearner.class.getName());
	private static Logger parserLogger = Logger.getLogger(ECGAnalyzer.class.getName());
	private static Logger verifierLogger = Logger.getLogger(AnalysisVerifier.class.getName());
	private static Logger contextualFitLogger = Logger.getLogger(ContextFitter.class.getName());

	boolean analyze, simulate, learn, aggressiveFit = false, batchUpdate = false, useMDL = true;
	boolean rerank = false, useGoldStandard = false;
	RerankMethod rerankMethod = RerankMethod.NONE;
	boolean newGrammarLoaded = false;

	int iteration = 0;
	int maxNumIterations = 1;

	int totalAnalyzedLengthOfUtterances = 0;
	int totalNumOfAnalysisRoots = 0;

	public ECGLearner(String preferenceFilePath) throws Exception {
		preferences = new LearnerPrefs(preferenceFilePath);
		baseDirectory = preferences.getBaseDirectory();

		analyze = Boolean.valueOf(preferences.getSetting(LP.ANALYZE));
		simulate = Boolean.valueOf(preferences.getSetting(LP.SIMULATE));
		learn = Boolean.valueOf(preferences.getSetting(LP.LEARN));

		if (preferences.getSetting(LP.AGRESSIVE_CONTEXT_FITTING) != null) {
			aggressiveFit = Boolean.valueOf(preferences.getSetting(LP.AGRESSIVE_CONTEXT_FITTING));
		}
		if (preferences.getSetting(LP.USE_GOLD_STANDARD) != null) {
			useGoldStandard = Boolean.valueOf(preferences.getSetting(LP.USE_GOLD_STANDARD));
		}
		if (preferences.getSetting(LP.BATCH_UPDATE) != null) {
			batchUpdate = Boolean.valueOf(preferences.getSetting(LP.BATCH_UPDATE));
		}
		if (preferences.getSetting(LP.ANALYSIS_RERANK_METHOD) != null) {
			rerank = Integer.valueOf(preferences.getSetting(LP.ANALYSIS_RERANK_METHOD)) != 0;
			rerankMethod = RerankMethod.values()[Integer.valueOf(preferences.getSetting(LP.ANALYSIS_RERANK_METHOD))];
		}
		if (preferences.getSetting(LP.USE_MDL) != null) {
			useMDL = Boolean.valueOf(preferences.getSetting(LP.USE_MDL));
		}

		if (preferences.getSetting(LP.ITERATIONS) != null) {
			maxNumIterations = Integer.valueOf(preferences.getSetting(LP.ITERATIONS));
		}

		loadModules();

		List<String> paths = preferences.getList(LP.DATA_PATHS);
		String ext = preferences.getSetting(LP.DATA_EXTENSIONS);
		if (ext == null) {
			ext = "xml";
		}
		transcriptFiles = FileUtils.getFilesUnder(baseDirectory, paths, new ExtensionFileFilter(ext));
	}

	protected void loadModules() throws Exception {

		logger.info("Loading learner modules...");

		parsingGrammar = LearnerGrammar.instantiateGrammar(preferences);
		Grammar grammar = parsingGrammar.getGrammar();
		contextModel = grammar.getContextModel();

		if (simulate) {
			List<String> paths = preferences.getList(LP.SCRIPT_PATHS);
			String ext = preferences.getSetting(LP.SCRIPT_EXTENSIONS);
			simulator = new Simulator(contextModel, FileUtils.getFilesUnder(baseDirectory, paths, new ExtensionFileFilter(
					ext)));
		}

		if (analyze) {
			analyzer = new ECGAnalyzer(grammar);
		}

		if (preferences.getSetting(LP.OUTPUT_XML) != null && Boolean.valueOf(preferences.getSetting(LP.OUTPUT_XML))) {
			xmlOutputPath = preferences.getSetting(LP.OUTPUT_XML_PATH);
		}

		if (learn) {
			engine = new ECGLearnerEngine(parsingGrammar.makeCopy(), useMDL);
		}
	}

	public boolean step() throws LCAException, IOException {
		// returns a flag indicating whether the grammar has been modified
		prepareToGetNextLCA();

		PriorityQueue<LearnerCentricAnalysis> lcaQueue = getNextLCA();

		if (lcaQueue == null || lcaQueue.isEmpty()) {
			return false;
		}
		else {
			LearnerCentricAnalysis bestAnalysis = chooseBestAnalysis(lcaQueue);

			if (!bestAnalysis.isRecovered() && isAdultUtterance) {
				parsingGrammar.updateTablesAfterAnalysis(bestAnalysis, learn);
				if (learn) {
					engine.learn(bestAnalysis, useGoldStandard);
					if (!batchUpdate) {
						loadNewGrammar();
					}
				}
				else {
					parserLogger.finer(bestAnalysis.toString());
					parserLogger.finest(analyzer.getParserLog());
				}
				updateContextModel(bestAnalysis);
			}

			return true;
		}
	}

	public void run() throws IOException {

		parsingGrammar.clearTables();

		for (iteration = 0; iteration < maxNumIterations; iteration++) {
			resetNextTranscript();
			while (moreToLearn()) {
				step();
			}
			if (learn && batchUpdate) {
				loadNewGrammar();
			}
		}
	}

	private void loadNewGrammar() {
		if (engine.isGrammarModified()) {
			LearnerGrammar newGrammar = engine.getExperimentalGrammar().makeCopy();
			if (newGrammar != null) {
				parsingGrammar = newGrammar;
				StringBuffer grammarParamsCxn = newGrammar.getConstructionalSubtypeTable()
						.outputConstituentExpansionCountTable();
				StringBuffer grammarParamsSem = newGrammar.getSemanticSubtypeTable().outputSemanticFillerCostTable();
				StringBuffer grammarParamsLoc = newGrammar.getConstructionalSubtypeTable()
						.outputConstituentLocalityCostTable();
				ECGGrammarUtilities.updateLocality(newGrammar.getGrammar(), new TextFileLineIterator(grammarParamsLoc));
				analyzer.loadNewGrammar(newGrammar.getGrammar(), grammarParamsCxn, grammarParamsSem);
				engine.resetGrammarModified();
				newGrammarLoaded = true;

				logger.info("New grammar loaded from the learner engine into the learner");

			}
			else {
				throw new LearnerException(
						"new grammar cannot be created from the experimental grammar in the learner engine");
			}
		}
	}

	protected void resetNextTranscript() {
		fileIterator = transcriptFiles.iterator();
		setupNextTranscript();
	}

	protected void setupNextTranscript() {
		if (fileIterator.hasNext()) {
			File nextFile = fileIterator.next();
			currentTranscript = new ChildesTranscript(nextFile);
			childesIterator = currentTranscript.iterator();

			// do not exclude child utterances because we want to keep the discourse model up to date. Instead ignore these
			// for learning purposes
			// ChildesFilter excludeChild = new ChildesFilter();
			// excludeChild.setSpeakerExclusion("CHI");
			// childesIterator.setFilter(excludeChild);
			getGrammar().getContextModel().reset();
			jointAttention.clear();

			logger.info("Processing... " + nextFile.getName() + " ..... ");
		}
		if (simulator != null) {
			simulator.initializeParticipants(currentTranscript.getParticipantIDs());
			simulator.initializeSetting(currentTranscript.getSettingEntitiesAndBindings(),
					currentTranscript.getSetupEntitiesAndBindings());
		}
	}

	protected void prepareToGetNextLCA() {
		if (!childesIterator.hasNext() && fileIterator.hasNext()) {
			setupNextTranscript();
		}
		simulateTillNextClause();
	}

	public void simulateTillNextClause() {

		boolean success = true;

		while (nextClauseToAnalyze == null && childesIterator.hasNext()) {
			ChildesItem item = childesIterator.next();

			if (item instanceof ChildesClause) {
				ChildesClause clause = (ChildesClause) item;
				if (clause.size() > 0) {
					if (simulator != null) {
						Set<String> attentionalFocus = new LinkedHashSet<String>();
						for (String entity : jointAttention) {
							if (MiniOntologyQueryAPI.retrieveIndividual(contextModel.getMiniOntology(), new SimpleQuery(
									ContextModel.getIndividualName(entity), ContextModel.getIndividualType(entity))) != null) {
								attentionalFocus.add(ContextModel.getIndividualName(entity));
							}
						}
						success &= simulator.registerUtterance(clause, attentionalFocus);
					}
					nextClauseToAnalyze = clause;
				}
			}
			else if (item instanceof ChildesEvent) {
				if (simulator != null) {
					success &= simulator.simulateEvent((ChildesEvent) item);
					updateJointAttention(simulator.getRecentlyAccessedIndividuals());
				}
				nextClauseToAnalyze = null;
			}
		}
	}

	protected void updateLCAWithDiscourseInfo(LearnerCentricAnalysis lca, String currentDS, String currentSpeechAct,
			String currentSpeechActType, String currentSpeaker, String currentAddressee, Set<String> jointAttention) {
		lca.setCurrentDS(currentDS);
		lca.setCurrentSpeechAct(currentSpeechAct);
		lca.setCurrentSpeechActType(currentSpeechActType);
		lca.setCurrentSpeaker(currentSpeaker);
		lca.setCurrentAddressee(currentAddressee);
		lca.setJointAttention(jointAttention);
	}

	protected PriorityQueue<LearnerCentricAnalysis> getNextLCA() throws LCAException {

		String vernacular = "";
		PriorityQueue<LearnerCentricAnalysis> lcaQueue = new PriorityQueue<LearnerCentricAnalysis>();

		if (nextClauseToAnalyze == null) {
			return lcaQueue;
		}

		SimpleQuery s = new SimpleQuery(contextModel.getMiniOntology().getCurrentIntervalName(),
				ECGConstants.DISCOURSESEGMENTTYPE);
		String currentDS = contextModel.retrieveIndividual(s);
		if (currentDS == null) {
			throw new LearnerException("Error retreiving the current " + ECGConstants.DISCOURSESEGMENTTYPE);
		}
		Set<String> speechActQuery = ContextUtilities.collapseResults(contextModel.query(
				new SimpleQuery(localizer.getDSRoleLocalization(DSRole.Speech_Act), ContextModel
						.getIndividualName(currentDS), "?s"), true));
		String currentSpeechAct = speechActQuery.isEmpty() ? null : speechActQuery.iterator().next();
		String currentSpeechActType = currentSpeechAct == null ? null : ContextModel.getIndividualType(currentSpeechAct);
		Set<String> speakerQuery = ContextUtilities.collapseResults(contextModel.query(
				new SimpleQuery(localizer.getDSRoleLocalization(DSRole.Speaker), ContextModel.getIndividualName(currentDS),
						"?s"), true));
		String currentSpeaker = speakerQuery.isEmpty() ? null : speakerQuery.iterator().next();
		Set<String> addresseeQuery = ContextUtilities.collapseResults(contextModel.query(
				new SimpleQuery(localizer.getDSRoleLocalization(DSRole.Addressee), ContextModel
						.getIndividualName(currentDS), "?a"), true));
		String currentAddressee = addresseeQuery.isEmpty() ? null : addresseeQuery.iterator().next();

		try {
			vernacular = nextClauseToAnalyze.getChildesAnnotation().getVernacularTier().getContent();
			isAdultUtterance = !nextClauseToAnalyze.getSpeaker().equalsIgnoreCase(
					localizer.getParticipantLocalization(Participant.Child));
			logger.info("Parsing... " + vernacular);

			PriorityQueue<?> bestParses;
			if (analyzer.robust()) {
				bestParses = analyzer.getBestPartialParses(nextClauseToAnalyze);
				while (bestParses.hasNext()) {
					double priority = bestParses.getPriority();
					// HACK: this is forcing a type cast to AnalysisInContext that isn't even checked.
					LearnerCentricAnalysis lca = new LearnerCentricAnalysis(nextClauseToAnalyze,
							(List<AnalysisInContext>) bestParses.next(), analyzer.getLargestAssignedSlotID(), false);
					updateLCAWithDiscourseInfo(lca, currentDS, currentSpeechAct, currentSpeechActType, currentSpeaker,
							currentAddressee, jointAttention);
					lcaQueue.add(lca, priority);
				}
			}
			else {
				bestParses = analyzer.getBestParses(nextClauseToAnalyze);
				while (bestParses.hasNext()) {
					double priority = bestParses.getPriority();
					// HACK: this is forcing a type cast to AnalysisInContext that isn't even checked.
					LearnerCentricAnalysis lca = new LearnerCentricAnalysis(nextClauseToAnalyze,
							(AnalysisInContext) bestParses.next(), analyzer.getLargestAssignedSlotID(), false);
					updateLCAWithDiscourseInfo(lca, currentDS, currentSpeechAct, currentSpeechActType, currentSpeaker,
							currentAddressee, jointAttention);
					lcaQueue.add(lca, priority);
				}
			}

			goldStandardAnnotation = nextClauseToAnalyze.getChildesAnnotation().getGoldStandardTier().getContent();
			speechActAnnotation = nextClauseToAnalyze.getChildesAnnotation().getSpeechActTier().getContent();
			nextClauseToAnalyze = null;
			return lcaQueue;
		}
		catch (NoECGAnalysisFoundException neafe) {
			exceptionallyBad.add(vernacular);

			logger.warning("EXCEPTIONALLY BAD ANALYSIS:");

			List<AnalysisInContext> analyses = (List<AnalysisInContext>) neafe.getAnalyses();
			for (AnalysisInContext analysis : analyses) {

				logger.finer(analysis.toString());

				List<CxnalSpan> cxnalSpans = analysis.getSpans();
				if (cxnalSpans == null) {
					CxnalSpan completeSpan = new CxnalSpan(null, analysis.getHeadCxn(), analysis.getFeatureStructure()
							.getMainRoot().getID(), analysis.getSpanLeftIndex(), analysis.getSpanRightIndex());
					analysis.addSpan(completeSpan);
				}
			}

			LearnerCentricAnalysis lca = new LearnerCentricAnalysis(nextClauseToAnalyze, analyses,
					analyzer.getLargestAssignedSlotID(), true);
			updateLCAWithDiscourseInfo(lca, currentDS, currentSpeechAct, currentSpeechActType, currentSpeaker,
					currentAddressee, jointAttention);
			lcaQueue.add(lca, 0);
			goldStandardAnnotation = nextClauseToAnalyze.getChildesAnnotation().getGoldStandardTier().getContent();
			speechActAnnotation = nextClauseToAnalyze.getChildesAnnotation().getSpeechActTier().getContent();
			nextClauseToAnalyze = null;
			return lcaQueue;

		}
		catch (ParserException pe) {
			unparsable.add(vernacular);
			logger.warning(pe.getLocalizedMessage());
			parserLogger.finest(analyzer.getParserLog());
			nextClauseToAnalyze = null;
			return null;
		}
	}

	public static class ContextualFitBasedComparator implements Comparator<LearnerCentricAnalysis> {
		public int compare(LearnerCentricAnalysis lca1, LearnerCentricAnalysis lca2) {
			double score1 = lca1.getAnalyzerScore() + Math.log(lca1.getContextualFit().getScore());
			double score2 = lca2.getAnalyzerScore() + Math.log(lca2.getContextualFit().getScore());
			return score1 == score2 ? 0 : score1 < score2 ? -1 : +1;
		}
	}

	public static class LCAVerifierBasedComparator implements Comparator<LearnerCentricAnalysis> {

		public int compare(LearnerCentricAnalysis lca1, LearnerCentricAnalysis lca2) {
			Scorecard s1 = new Scorecard().sum(lca1.getVerifierScore().getFirst(), true).sum(
					lca1.getVerifierScore().getSecond(), true);
			Scorecard s2 = new Scorecard().sum(lca2.getVerifierScore().getFirst(), true).sum(
					lca2.getVerifierScore().getSecond(), true);
			double avg1 = (s1.getFScore(Cat.CORE) + s1.getFScore(Cat.BRACKET) + s1.getFScore(Cat.RES)) / 3;
			double avg2 = (s2.getFScore(Cat.CORE) + s2.getFScore(Cat.BRACKET) + s2.getFScore(Cat.RES)) / 3;
			return avg1 == avg2 ? 0 : avg1 < avg2 ? -1 : +1;
		}
	}

	protected LearnerCentricAnalysis chooseBestAnalysis(PriorityQueue<LearnerCentricAnalysis> lcaQueue) {

		Grammar currentGrammar = getGrammar();
		GrammarTables currentGrammarTables = getGrammarTables();
		AnalysisVerifier verifier = new AnalysisVerifier(currentGrammar, currentTranscript, currentGrammarTables);

		LearnerCentricAnalysis originalTop = lcaQueue.peek();
		LearnerCentricAnalysis best = null;

		totalAnalyzedLengthOfUtterances += originalTop.getUtteranceAnalyzed().size();
		totalNumOfAnalysisRoots += originalTop.getAnalyses().size();

		if (rerank) {
			List<LearnerCentricAnalysis> LCAs = new ArrayList<LearnerCentricAnalysis>();

			if (useGoldStandard && goldStandardAnnotation != null) {
				contextFitter = new GoldStandardFitter(currentGrammar, currentGrammarTables, aggressiveFit, localizer,
						goldStandardAnnotation, localizer);
			}
			else {
				contextFitter = new BasicContextFitter(currentGrammar, aggressiveFit, localizer);
			}

			while (lcaQueue.hasNext()) {
				double analyzerScore = lcaQueue.getPriority();
				LearnerCentricAnalysis lca = lcaQueue.next();
				lca.setAnalyzerScore(analyzerScore);

				contextFitter.getContextualFit(lca);

				LCAs.add(lca);

				if (goldStandardAnnotation != null) {
					verifier.verify(lca, goldStandardAnnotation);
				}
			}
			if (rerankMethod == RerankMethod.CONTEXTUAL_FIT) {
				Collections.sort(LCAs, Collections.reverseOrder(new ContextualFitBasedComparator()));
			}
			else if (rerankMethod == RerankMethod.VERIFIER) {
				Collections.sort(LCAs, Collections.reverseOrder(new LCAVerifierBasedComparator()));
			}

			best = LCAs.get(0);
			if (best != originalTop) {
				logger.fine("best analysis changed");
			}

		}
		else {
			best = originalTop;
			contextFitter = new BasicContextFitter(currentGrammar, aggressiveFit, localizer);
			double analyzerScore = lcaQueue.getPriority();
			best.setAnalyzerScore(analyzerScore);
			contextFitter.getContextualFit(best);
			if (goldStandardAnnotation != null) {
				verifier.verify(best, goldStandardAnnotation);
			}
		}

		if (originalTop.getAnalyzerScore() != Double.NEGATIVE_INFINITY) {
			preRerankAnalyzerScore += originalTop.getAnalyzerScore();
		}
		Pair<Scorecard, Scorecard> originalScore = originalTop.getVerifierScore();
		if (originalScore != null) {
			priorToRerankRunningScore.getFirst().sum(originalScore.getFirst());
			priorToRerankRunningScore.getSecond().sum(originalScore.getSecond());
		}

		if (best.getAnalyzerScore() != Double.NEGATIVE_INFINITY) {
			postRerankAnalyzerScore += best.getAnalyzerScore();
		}
		Pair<Scorecard, Scorecard> bestScore = best.getVerifierScore();
		if (bestScore != null) {
			runningScore.getFirst().sum(bestScore.getFirst());
			runningScore.getSecond().sum(bestScore.getSecond());
		}
		numAnalysis++;

		logger.fine("pre-rerank best analysis score = " + originalTop.getAnalyzerScore());
		logger.fine("post-rerank best analysis score = " + best.getAnalyzerScore());
		parserLogger.finer("best analysis:\n");
		parserLogger.finer(best.toString());
		contextualFitLogger.finer("contextual fit:\n");
		contextualFitLogger.finer(best.getContextualFit().toString());
		if (bestScore != null) {
			verifierLogger.finer("verifier score:\n");
			verifierLogger.finer("verb argument score:\n");
			verifierLogger.finer(bestScore.getFirst().toString());
			verifierLogger.finer(bestScore.getFirst().getLog());
			verifierLogger.finer("argument structure argument score:\n");
			verifierLogger.finer(bestScore.getSecond().toString());
			verifierLogger.finer(bestScore.getSecond().getLog());
		}
		parserLogger.finest(analyzer.getParserLog());

		return best;
	}

	protected void updateContextModel(LearnerCentricAnalysis lca) {
		Set<String> recentIndividuals = new LinkedHashSet<String>();

		ContextualFit fit = lca.getContextualFit();
		for (Integer slotID : fit.getSlots()) {
			String candidate = fit.getCandidate(slotID);
			if (candidate != null) {
				recentIndividuals.add(candidate);
			}
		}
		List<String> individuals = new ArrayList<String>(recentIndividuals);
		contextModel.updateRecentDiscourseEntities(individuals);
		updateJointAttention(individuals);
	}

	protected void updateJointAttention(List<String> unfilteredCandidates) {
		Set<String> possibleJointAttention = new HashSet<String>();
		Set<String> speakersAndAddressees = new HashSet<String>();

		if (speechActAnnotation != null) {
			// if the individual is neither the speaker nor the addressee, then it's possibly a jointly attended object
			for (Pair<String, FillerType> speaker : speechActAnnotation.getBinding(localizer
					.getDSRoleLocalization(DSRole.Speaker))) {
				speakersAndAddressees.add(speaker.getFirst());
			}
			for (Pair<String, FillerType> addressee : speechActAnnotation.getBinding(localizer
					.getDSRoleLocalization(DSRole.Addressee))) {
				speakersAndAddressees.add(addressee.getFirst());
			}
		}

		for (String individual : unfilteredCandidates) {
			String name = ContextModel.getIndividualName(individual);
			if (name.matches("u\\d*sa\\d*"))
				continue;

			if (!speakersAndAddressees.isEmpty() && !speakersAndAddressees.contains(name)) {
				possibleJointAttention.add(individual);
			}
			else {
				if (!name.equals(localizer.getParticipantLocalization(Participant.Child))
						&& !name.equals(localizer.getParticipantLocalization(Participant.Mother))
						&& !name.equals(localizer.getParticipantLocalization(Participant.Father))) {
					possibleJointAttention.add(individual);
				}
			}
		}

		if (!possibleJointAttention.isEmpty()) {
			jointAttention.clear();
			jointAttention.addAll(possibleJointAttention);
		}
	}

	protected boolean moreToLearn() {
		return childesIterator.hasNext() || fileIterator.hasNext();
	}

	public LearnerCentricAnalysis getLCA() {
		return currentLCA;
	}

	public Grammar getGrammar() {
		return parsingGrammar.getGrammar();
	}

	public GrammarTables getGrammarTables() {
		return parsingGrammar.getGrammarTables();
	}

	public ContextModel getContextModel() {
		return contextModel;
	}

	public Simulator getSimulator() {
		return simulator;
	}

	public long getAnalyzerConstructorTime() {
		return analyzer.getConstructorTime();
	}

	public void outputResults() throws IOException {
		String grammarOutpath = preferences.getSetting(LP.OUTPUT_GRAMMAR_PATH);
		String tableOutpath = preferences.getSetting(LP.OUTPUT_GRAMMAR_PARAMS_PATH);
		parsingGrammar.outputToFile(makeAbsoluteDir(grammarOutpath, baseDirectory),
				makeAbsoluteDir(tableOutpath, baseDirectory));
	}

	public static File makeAbsoluteDir(String path, File baseDirectory) {
		File dir = null;
		if (path != null) {
			dir = new File(path);
			if (!dir.isAbsolute()) {
				dir = new File(baseDirectory, path);
			}
			if (dir.isDirectory()) {
				dir.mkdirs();
			}
			else {
				dir = dir.getParentFile();
			}
		}
		return dir;
	}

	public String getFinalOutput() {
		StringBuffer sb = new StringBuffer("\n");

		sb.append("=================================================\n\n");
		sb.append("number of analyses produced = \t").append(numAnalysis).append("\n");
		sb.append("number of words analyzed = \t").append(totalAnalyzedLengthOfUtterances).append("\n");
		sb.append("number of roots generated = \t").append(totalNumOfAnalysisRoots).append("\n\n");
		sb.append("pre-reranking analyzer score = \t").append(preRerankAnalyzerScore).append("\n");
		sb.append("post-reranking analyzer score = \t").append(postRerankAnalyzerScore).append("\n\n");
		sb.append("=================================================\n\n");

		sb.append("total score = \n").append(runningScore.toString()).append("\n\n");
		if (rerank) {
			sb.append("total score before reranking = \n").append(priorToRerankRunningScore.toString()).append("\n\n");
		}

		if (parsingGrammar.getConstructionalSubtypeTable() != null) {
			sb.append(parsingGrammar.getConstructionalSubtypeTable().toString()).append("\n\n");;
		}

		sb.append("Number of exceptionally bad analyses = ").append(exceptionallyBad.size()).append("\n");
		sb.append("Exceptionally bad sentences: \n");
		for (String s : exceptionallyBad) {
			sb.append(s).append("\n");
		}

		sb.append("Number of unparsable sentences = ").append(unparsable.size()).append("\n");
		sb.append("Unparsable sentences: \n");
		for (String s : unparsable) {
			sb.append(s).append("\n");
		}

		if (engine != null) {
			sb.append("\n\n");
			sb.append(engine.getFinalOutput());
			sb.append(newGrammarLoaded ? "Grammar updated." : "Grammar unchanged.");
		}

		return sb.toString();
	}

}

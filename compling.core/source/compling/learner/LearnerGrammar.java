// =============================================================================
// File        : LearnerGrammar.java
// Author      : emok
// Change Log  : Created on Apr 14, 2008
//=============================================================================

package compling.learner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Prefs;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.LearnerPrefs;
import compling.gui.LearnerPrefs.LP;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.learnertables.ConstructionalSubtypeTable;
import compling.learner.learnertables.ConstructionalSubtypeTable.Locality;
import compling.learner.learnertables.GeneralizationHistory;
import compling.learner.learnertables.NGram;
import compling.learner.learnertables.SemanticSubtypeTable;
import compling.learner.util.LearnerGrammarPrinter;
import compling.parser.ParserException;
import compling.parser.ecgparser.SemSpecScorer.BasicScorer;
import compling.parser.ecgparser.SemSpecScorer.ParamFileScorerFromCounts;
import compling.util.Counter;
import compling.util.LookupTable;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.Pair;
import compling.util.RecencyCache;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileReadingUtils;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;
import compling.utterance.Sentence;
import compling.utterance.Utterance;
import compling.utterance.Word;

//=============================================================================

/***
 * This class is mainly a variable object that contains a grammar and all the tables that need to be swaped in and out
 * with it.
 */

public class LearnerGrammar {

	public static enum MetaData {
		SENTENCE_LIST("### Cached Sentence ###"),
		GENERALIZATION_LIST("### Generalizations Made ###"),
		REVISION_WATCHLIST("### Revision Watchlist ###"),
		OMISSION_WATCHLIST("### Omission Watchlist ###"),
		CHAINABLE_WATCHLIST("### Chainable Watchlist ###");

		private String blockTitle;

		private MetaData(String blockTitle) {
			this.blockTitle = blockTitle;
		}

		public String getTitle() {
			return blockTitle;
		}
	}

	// data object for capturing grammar changes.
	public static class GrammarChanges {

		public Collection<Construction> cxnsToAdd = null;
		public Map<Construction, Construction> cxnsToReplace = null;
		public Collection<Construction> cxnsToPurge = null;

		public GrammarChanges() {
			cxnsToAdd = new LinkedHashSet<Construction>();
			cxnsToReplace = new LinkedHashMap<Construction, Construction>();
			cxnsToPurge = new LinkedHashSet<Construction>();
		}

		public GrammarChanges(Collection<Construction> cxnsToAdd, Map<Construction, Construction> cxnsToReplace,
				Collection<Construction> cxnsToPurge) {
			this.cxnsToAdd = cxnsToAdd;
			this.cxnsToReplace = cxnsToReplace;
			this.cxnsToPurge = cxnsToPurge;
		}

		public GrammarChanges(Construction cxnToAdd) {
			cxnsToAdd = new ArrayList<Construction>();
			cxnsToAdd.add(cxnToAdd);
		}

		public void aggregateChanges(GrammarChanges that) {
			if (that.cxnsToAdd != null)
				cxnsToAdd.addAll(that.cxnsToAdd);
			if (that.cxnsToReplace != null)
				cxnsToReplace.putAll(that.cxnsToReplace);
			if (that.cxnsToPurge != null)
				cxnsToPurge.addAll(that.cxnsToPurge);
		}

		public boolean isEmpty() {
			return (cxnsToAdd == null || cxnsToAdd.isEmpty()) && (cxnsToReplace == null || cxnsToReplace.isEmpty())
					&& (cxnsToPurge == null || cxnsToPurge.isEmpty());
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("================================\nGrammar modifications:\n");
			sb.append("Added : \n");
			if (cxnsToAdd != null) {
				sb.append(cxnsToAdd.toString());
			}
			sb.append("Replaced : \n");
			if (cxnsToReplace != null) {
				sb.append(cxnsToReplace.toString());
			}
			sb.append("Purged : \n");
			if (cxnsToPurge != null) {
				sb.append(cxnsToPurge.toString());
			}
			sb.append("================================\n");
			return sb.toString();
		}
	}

	static final int MAX_UTTERANCE_CACHE = 6;

	Grammar grammar = null;
	TypeSystem<Construction> cxnTypeSystem = null;
	ContextModel contextModel = null;
	Prefs grammarPrefs = null;
	GrammarTables grammarTables = null;
	StringBuffer grammarBuffer = null;

	boolean useBackoff;
	BasicScorer semanticModel = null;
	StringBuffer semanticModelBuffer = null;
	boolean semanticModelUpToDate = false;

	NGram ngram;
	ConstructionalSubtypeTable constructionalSubtypeTable;
	SemanticSubtypeTable semanticSubtypeTable;

	RecencyCache<Utterance<Word, String>> cachedUtterances = new RecencyCache<Utterance<Word, String>>(
			MAX_UTTERANCE_CACHE);
	GeneralizationHistory generalizationHistory = new GeneralizationHistory();

	MDLCost mdlCost = null;

	static LearnerGrammarPrinter printer = new LearnerGrammarPrinter();
	static Logger logger = Logger.getLogger(LearnerGrammar.class.getName());

	MapSet<String, String> watchList = new MapSet<String, String>();
	MapSet<String, String> nearlyIdentical = new MapSet<String, String>();
	List<List<String>> chainable = new ArrayList<List<String>>();

	MapSet<String, String> removedWatchList = new MapSet<String, String>();
	MapSet<String, String> removedNearlyIdentical = new MapSet<String, String>();
	List<List<String>> removedChainable = new ArrayList<List<String>>();

	public LearnerGrammar(Grammar grammar) throws IOException {
		this(grammar, new GrammarTables(grammar), new NGram(grammar), new ConstructionalSubtypeTable(grammar),
				new SemanticSubtypeTable(grammar), printer.format(grammar));
	}

	public LearnerGrammar(Grammar grammar, TextFileLineIterator nGramTableIterator,
			TextFileLineIterator subtypeTableIterator, TextFileLineIterator localityTableIterator) throws IOException {

		this(grammar, new GrammarTables(grammar), new NGram(grammar, nGramTableIterator), new ConstructionalSubtypeTable(
				grammar, subtypeTableIterator, localityTableIterator), new SemanticSubtypeTable(grammar), printer
				.format(grammar));
	}

	protected LearnerGrammar(Grammar grammar, GrammarTables tables, NGram nGram, ConstructionalSubtypeTable cst,
			SemanticSubtypeTable sst, String grammarBuffer) throws IOException {

		this.grammar = grammar;
		this.cxnTypeSystem = grammar.getCxnTypeSystem();
		contextModel = grammar.getContextModel();
		grammarPrefs = grammar.getPrefs();

		grammarTables = tables;
		ngram = nGram;
		constructionalSubtypeTable = cst;
		semanticSubtypeTable = sst;
		this.grammarBuffer = new StringBuffer(grammarBuffer);
		mdlCost = new MDLCost(this);
	}

	public LearnerGrammar makeCopy() throws IOException {
		try {
			Grammar newGrammar = ECGGrammarUtilities.read(grammarBuffer, contextModel);
			newGrammar.update();
			newGrammar.setPrefs(grammarPrefs);
			GrammarTables newGrammarTables = new GrammarTables(newGrammar);

			// alternatively the following tables could have been built by spitting the output to stringBuffers and reading
			// them back in
			NGram ng = new NGram(newGrammar, ngram);
			ConstructionalSubtypeTable cst = new ConstructionalSubtypeTable(newGrammar, constructionalSubtypeTable);
			SemanticSubtypeTable sst = new SemanticSubtypeTable(newGrammar, semanticSubtypeTable);

			LearnerGrammar copy = new LearnerGrammar(newGrammar, newGrammarTables, ng, cst, sst, grammarBuffer.toString());
			copy.cachedUtterances.addAll(cachedUtterances.entries());
			copy.generalizationHistory = new GeneralizationHistory(generalizationHistory);
			copy.watchList = new MapSet<String, String>(watchList);
			copy.nearlyIdentical = new MapSet<String, String>(nearlyIdentical);

			copy.useBackoff = useBackoff;
			copy.semanticModelBuffer = semanticModelBuffer; // assuming this doesn't ever get updated
			copy.semanticModelUpToDate = false;
			// don't instantiate the actual model until it's asked for to save time -- it doesn't always get used.

			return copy;
		}
		catch (GrammarException ge) {
			logger.warning(ge.getLocalizedMessage());
			return null;
		}
	}

	public boolean modifyGrammar(GrammarChanges changes) {

		Pair<Boolean, StringBuffer> modifications = makeNewGrammarBuffer(changes.cxnsToAdd, changes.cxnsToReplace,
				changes.cxnsToPurge);

		if (modifications.getFirst()) {
			try {
				Grammar newGrammar = ECGGrammarUtilities.read(modifications.getSecond(), contextModel);
				newGrammar.update();
				newGrammar.setPrefs(grammarPrefs);
				GrammarTables newGrammarTables = new GrammarTables(newGrammar);
				grammar = newGrammar;
				cxnTypeSystem = grammar.getCxnTypeSystem();
				grammarTables = newGrammarTables;
				ngram = new NGram(grammar, ngram);
				constructionalSubtypeTable = new ConstructionalSubtypeTable(grammar, constructionalSubtypeTable);
				semanticSubtypeTable = new SemanticSubtypeTable(grammar, semanticSubtypeTable);
				grammarBuffer = modifications.getSecond();
				semanticModelUpToDate = false;
				// meta info isn't affected by the type system and doesn't need to be recreated. The semantic model will
				// have to be reinstantiated.

				if (changes.cxnsToPurge != null) { // if the purges includes a bad generalization, delete it from the table
					for (Construction c : changes.cxnsToPurge) {
						generalizationHistory.removeType(c.getName());
					}
				}
				mdlCost.noLongerUpToDate();
				return true;
			}
			catch (GrammarException ge) {
				logger.warning("Grammar modification failed");
				logger.warning(ge.getLocalizedMessage());
				return false;
			}
			catch (ParserException pe) {
				logger.warning("Grammar modification failed");
				logger.warning(pe.getLocalizedMessage());
				return false;
			}
		}
		return false;
	}

	protected Pair<Boolean, StringBuffer> makeNewGrammarBuffer(Collection<Construction> cxnsToAdd,
			Map<Construction, Construction> cxnsToReplace, Collection<Construction> cxnsToPurge) {

		StringBuffer buffer = new StringBuffer();
		for (Schema schema : grammar.getAllSchemas()) {
			buffer.append(printer.format(schema)).append("\n");
		}

		LinkedHashMap<String, Construction> cxns = new LinkedHashMap<String, Construction>(
				grammar.getAllConstructionsByName());

		boolean modified = false;

		if (cxnsToAdd != null && !cxnsToAdd.isEmpty()) {
			for (Construction cxn : cxnsToAdd) {
				cxns.put(cxn.getName(), cxn);
			}
			modified = true;
		}

		if (cxnsToReplace != null && !cxnsToReplace.isEmpty()) {
			for (Construction toRemove : cxnsToReplace.keySet()) {
				cxns.remove(toRemove.getName());
			}
			for (Construction cxn : cxnsToReplace.values()) {
				cxns.put(cxn.getName(), cxn);
			}
			modified = true;
		}

		if (cxnsToPurge != null && !cxnsToPurge.isEmpty()) {
			for (Construction toRemove : cxnsToPurge) {
				cxns.remove(toRemove.getName());
			}
			modified = true;
		}

		for (Construction cxn : cxns.values()) {
			buffer.append(printer.format(cxn, cxns)).append("\n");
		}
		return new Pair<Boolean, StringBuffer>(modified, buffer);
	}

	public void clearTables() {
		ngram.clear();
		constructionalSubtypeTable.clear();
		constructionalSubtypeTable.clear();
	}

	public void updateTablesAfterAnalysis(LearnerCentricAnalysis lca, boolean learningMode) {
		ngram.updateTable(lca);
		constructionalSubtypeTable.updateTable(lca, learningMode);
		semanticSubtypeTable.updateTable(lca);
	}

	public void updateTablesAfterGeneralization(String newGeneralCxnName, Collection<TypeConstraint> lastGeneralized,
			MapMap<Role, TypeConstraint, Role> roleMap) {

		LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> expansionTable = constructionalSubtypeTable
				.getExpansionTable();
		LookupTable<Pair<TypeConstraint, Role>, Locality> localityTable = constructionalSubtypeTable.getLocalityTable();

		// localize the type constraints in lastGeneralized
		List<TypeConstraint> generalizedOver = new ArrayList<TypeConstraint>();
		List<String> namesGeneralizedOver = new ArrayList<String>();

		// Store all the generalizations made. Notice that this is transitive.
		for (TypeConstraint subcase : lastGeneralized) {
			generalizedOver.add(getCurrentTypeConstraint(subcase));
			namesGeneralizedOver.add(subcase.getType());
		}
		generalizationHistory.addType(newGeneralCxnName, namesGeneralizedOver);

		// deal with other constructions that used the generalizedOver as constituents
		TypeConstraint newCxnTC = getCurrentTypeConstraint(newGeneralCxnName);
		for (Pair<TypeConstraint, Role> role : expansionTable.keySet()) {
			for (TypeConstraint subcase : generalizedOver) {
				if (expansionTable.get(role).containsKey(subcase)) {
					expansionTable.incrementCount(role, newCxnTC, expansionTable.getCount(role, subcase));
				}
			}
		}

		// deal with the constituents of the new general construction
		for (Role genRole : roleMap.keySet()) {
			Pair<TypeConstraint, Role> role = new Pair<TypeConstraint, Role>(newCxnTC, genRole);
			for (TypeConstraint subcase : roleMap.get(genRole).keySet()) {
				Role subcaseRole = roleMap.get(genRole, subcase);
				TypeConstraint fillerType = getCurrentTypeConstraint(subcaseRole.getTypeConstraint());

				// update the expansion of the constituents of the new general construction
				expansionTable.incrementCount(role, fillerType, ngram.getUnigram(subcase.getType())); // this should be pure
																																	// frequency of the
																																	// cxn)

				// merge the locality counts for the roles
				for (Locality loc : Locality.values()) {
					Integer count = localityTable.get(new Pair<TypeConstraint, Role>(subcase, subcaseRole), loc);
					if (count != null) {
						localityTable.incrementCount(role, loc, count);
					}
				}
			}
		}

		// merge ngram counts
		Counter<TypeConstraint> frequencies = ngram.getFrequencies();
		LookupTable<TypeConstraint, TypeConstraint> bigram = ngram.getBigram();

		for (TypeConstraint subcase : generalizedOver) {
			frequencies.incrementCount(newCxnTC, frequencies.getCount(subcase));
		}

		// FIXME: there is a simple fix-up to get the unigrams of the new abstract categories, but what about bigrams?
		// can't be faked, huh?
		for (Role genRole : roleMap.keySet()) {
			TypeConstraint genRoleType = genRole.getTypeConstraint();
			if (!frequencies.containsKey(getCurrentTypeConstraint(genRoleType))) {
				frequencies.incrementCount(genRoleType, frequencies.getCount(newCxnTC));
			}
		}

		Set<TypeConstraint> A = new HashSet<TypeConstraint>(bigram.keySet());
		for (TypeConstraint a : A) {
			if (generalizedOver.contains(a)) {
				Set<TypeConstraint> B = new HashSet<TypeConstraint>(bigram.get(a).keySet());
				for (TypeConstraint b : B) {
					if (generalizedOver.contains(b)) {
						bigram.incrementCount(newCxnTC, newCxnTC, bigram.getCount(a, b));
					}
					else {
						bigram.incrementCount(newCxnTC, b, bigram.getCount(a, b));
					}
				}
			}
			else {
				Set<TypeConstraint> B = new HashSet<TypeConstraint>(bigram.get(a).keySet());
				for (TypeConstraint b : B) {
					if (generalizedOver.contains(b)) {
						bigram.incrementCount(a, newCxnTC, bigram.getCount(a, b));
					}
				}
			}
		}

	}

	public void updateTablesAfterCategoryMerge(String mergedInto, Collection<String> merged,
			ConstructionalSubtypeTable oldCxnTable, NGram oldNGram, boolean brandNewCategory) {

		TypeConstraint mergedTC = getCurrentTypeConstraint(mergedInto);

		TypeSystem<Construction> oldCxnTypeSystem = oldCxnTable.getCxnTypeSystem();
		LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> oldExpansionTable = oldCxnTable.getExpansionTable();
		LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> expansionTable = constructionalSubtypeTable
				.getExpansionTable();

		List<TypeConstraint> localizedMergedTypes = new ArrayList<TypeConstraint>();
		for (String subcase : merged) {
			localizedMergedTypes.add(oldCxnTypeSystem.getCanonicalTypeConstraint(subcase));
		}

		// the merged category gets the sum of the counts of the original ones
		for (Pair<TypeConstraint, Role> oldSlot : oldExpansionTable.keySet()) {
			Set<TypeConstraint> overlap = new HashSet<TypeConstraint>(oldExpansionTable.get(oldSlot).keySet());
			overlap.retainAll(localizedMergedTypes);
			if (!overlap.isEmpty()) {
				TypeConstraint newFrameTC = getCurrentTypeConstraint(oldSlot.getFirst().getType());
				Role newRole = newFrameTC == null ? null
						: getCurrentRole(oldSlot.getFirst(), oldSlot.getSecond().getName());
				if (newFrameTC != null && newRole != null) {
					Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(newFrameTC, newRole);
					for (TypeConstraint mergedType : overlap) {
						if (oldExpansionTable.get(oldSlot).containsKey(mergedType)) {
							expansionTable.incrementCount(newSlot, mergedTC, oldExpansionTable.getCount(oldSlot, mergedType));
						}
					}
				}
			}
		}

		// FUTURE: For now, merge only happens with abstract construction without constituents. If in the future there are
		// constituents,
		// merge expansion in the other direction (constituents of the merged types and their locality)

		// merge ngram counts
		Counter<TypeConstraint> oldFrequencies = oldNGram.getFrequencies();
		LookupTable<TypeConstraint, TypeConstraint> oldBigram = oldNGram.getBigram();
		Counter<TypeConstraint> frequencies = ngram.getFrequencies();
		LookupTable<TypeConstraint, TypeConstraint> bigram = ngram.getBigram();

		for (TypeConstraint subcase : localizedMergedTypes) {
			try {
				if (!oldCxnTypeSystem.subtype(oldCxnTypeSystem.getInternedString(subcase.getType()),
						oldCxnTypeSystem.getInternedString(mergedInto))) {
					frequencies.incrementCount(mergedTC, oldFrequencies.getCount(subcase));
				}
			}
			catch (TypeSystemException tse) {
				logger.warning(tse.getLocalizedMessage());
			}
		}

		for (TypeConstraint a : oldBigram.keySet()) {
			if (localizedMergedTypes.contains(a)) {
				for (TypeConstraint b : oldBigram.get(a).keySet()) {
					if (localizedMergedTypes.contains(b)) {
						bigram.incrementCount(mergedTC, mergedTC, oldBigram.getCount(a, b));
					}
					else {
						TypeConstraint newB = getCurrentTypeConstraint(b.getType());
						bigram.incrementCount(mergedTC, newB, oldBigram.getCount(a, b));
					}
				}
			}
			else {
				for (TypeConstraint b : oldBigram.get(a).keySet()) {
					if (localizedMergedTypes.contains(b)) {
						TypeConstraint newA = getCurrentTypeConstraint(a.getType());
						bigram.incrementCount(newA, mergedTC, oldBigram.getCount(a, b));
					}
				}
			}
		}
	}

	private TypeConstraint getCurrentTypeConstraint(TypeConstraint oldType) {
		return cxnTypeSystem.getCanonicalTypeConstraint(oldType.getType());
	}

	private TypeConstraint getCurrentTypeConstraint(String oldType) {
		return cxnTypeSystem.getCanonicalTypeConstraint(oldType);
	}

	private Role getCurrentRole(TypeConstraint cxnType, String constituentName) {
		return cxnType == null ? null : grammar.getConstruction(cxnType.getType()).getConstructionalBlock()
				.getRole(constituentName);
	}

	public void cacheUtterance(Utterance<Word, String> utterance) {
		cachedUtterances.add(utterance);
		mdlCost.noLongerUpToDate();
	}

	public void cacheUtterance(String utterance) {
		List<String> words = Arrays.asList(utterance.trim().split(" "));
		cachedUtterances.add(new Sentence(words, null, 0));
	}

	public Collection<Utterance<Word, String>> getCacheUtterances() {
		return cachedUtterances.entries();
	}

	public boolean hasChainableList() {
		return !chainable.isEmpty();
	}

	public void addToChainable(List<String> chain) {
		if (!removedChainable.contains(chain)) {
			chainable.add(chain);
		}
	}

	public void addToChainable(String cxn1, String cxn2) {
		List<String> toChain = new ArrayList<String>();
		toChain.add(cxn1);
		toChain.add(cxn2);
		addToChainable(toChain);
	}

	public void removeFromChainableList(List<String> chain) {
		chainable.remove(chain);
		removedChainable.add(chain);
	}

	public boolean hasWatchlist() {
		return !watchList.isEmpty();
	}

	public MapSet<String, String> getWatchlist() {
		return watchList;
	}

	public void addToWatchList(String cxn1, String cxn2) {
		if (!watchList.contains(cxn1, cxn2) && !removedWatchList.contains(cxn1, cxn2)) {
			watchList.put(cxn1, cxn2);
			watchList.put(cxn2, cxn1);
		}
	}

	public boolean isOnWatchList(String cxnName) {
		return watchList.containsKey(cxnName);
	}

	public void removeFromWatchList(String cxnName) {
		for (String pairedCxn : watchList.get(cxnName)) {
			if (watchList.get(pairedCxn).size() == 1) {
				removedWatchList.put(pairedCxn, watchList.remove(pairedCxn));
			}
		}
		removedWatchList.put(cxnName, watchList.remove(cxnName));
	}

	public boolean hasNearlyIdenticalConstructions() {
		return !nearlyIdentical.isEmpty();
	}

	public MapSet<String, String> getNearlyIdenticalList() {
		return nearlyIdentical;
	}

	// Make sure the shorter construction comes first. It's what triggers the omission operation.
	public void addToNearlyIdenticalList(String shorterCxn, String longerCxn) {
		if (!nearlyIdentical.contains(shorterCxn, longerCxn) && !removedNearlyIdentical.contains(shorterCxn, longerCxn)) {
			nearlyIdentical.put(shorterCxn, longerCxn);
		}
	}

	public boolean isOnNearlyIdenticalList(String cxnName) {
		return nearlyIdentical.containsKey(cxnName);
	}

	public void removeFromNearlyIdenticalList(String cxnName) {
		removedNearlyIdentical.put(cxnName, nearlyIdentical.remove(cxnName));
	}

	public Grammar getGrammar() {
		return grammar;
	}

	public GrammarTables getGrammarTables() {
		return grammarTables;
	}

	public ConstructionalSubtypeTable getConstructionalSubtypeTable() {
		return constructionalSubtypeTable;
	}

	public SemanticSubtypeTable getSemanticSubtypeTable() {
		return semanticSubtypeTable;
	}

	public BasicScorer getSemanticModel() {
		if ((!semanticModelUpToDate || semanticModel == null) && semanticModelBuffer != null
				&& semanticModelBuffer.length() > 0) {
			semanticModel = new ParamFileScorerFromCounts(grammar, new TextFileLineIterator(semanticModelBuffer),
					useBackoff);
			semanticModelUpToDate = true;
		}
		return semanticModel;
	}

	public NGram getNGram() {
		return ngram;
	}

	public GeneralizationHistory getGeneralizationHistory() {
		return generalizationHistory;
	}

	public double getDescriptionLength() throws IOException {
		return mdlCost.getDescriptionLength();
	}

	public void readMetadata(TextFileLineIterator lineIterator) {
		MetaData currentBlock = MetaData.SENTENCE_LIST;
		while (lineIterator.hasNext()) {

			String line = lineIterator.next();
			if (line.trim().equals(""))
				continue;

			if (line.contains(MetaData.SENTENCE_LIST.getTitle())) {
				currentBlock = MetaData.SENTENCE_LIST;
			}
			else if (line.contains(MetaData.GENERALIZATION_LIST.getTitle())) {
				currentBlock = MetaData.GENERALIZATION_LIST;
			}
			else if (line.contains(MetaData.REVISION_WATCHLIST.getTitle())) {
				currentBlock = MetaData.REVISION_WATCHLIST;
			}
			else if (line.contains(MetaData.OMISSION_WATCHLIST.getTitle())) {
				currentBlock = MetaData.OMISSION_WATCHLIST;
			}
			else if (line.contains(MetaData.CHAINABLE_WATCHLIST.getTitle())) {
				currentBlock = MetaData.CHAINABLE_WATCHLIST;
			}
			else {

				if (currentBlock == MetaData.SENTENCE_LIST) {
					cacheUtterance(line);
				}
				else if (currentBlock == MetaData.GENERALIZATION_LIST) {
					if (line.contains(":")) {
						// generalization information
						ArrayList<String> cxnNames = new ArrayList<String>(Arrays.asList(line.split("[\\s:]")));
						while (cxnNames.contains(""))
							cxnNames.remove("");
						String supertype = cxnNames.remove(0).trim();
						generalizationHistory.addType(supertype, cxnNames);
					}
				}
				else if (currentBlock == MetaData.REVISION_WATCHLIST) {
					// this should be a list of only two items
					if (line.contains(":")) {
						// watchlist items
						ArrayList<String> cxnNames = new ArrayList<String>(Arrays.asList(line.split("[\\s:]")));
						while (cxnNames.contains(""))
							cxnNames.remove("");
						String key = cxnNames.remove(0).trim();
						for (String value : cxnNames.subList(1, cxnNames.size())) {
							addToWatchList(key, value.trim());
						}
					}
				}
				else if (currentBlock == MetaData.OMISSION_WATCHLIST) {
					if (line.contains(":")) {
						// generalization information
						ArrayList<String> cxnNames = new ArrayList<String>(Arrays.asList(line.split("[\\s:]")));
						while (cxnNames.contains(""))
							cxnNames.remove("");
						String key = cxnNames.remove(0).trim();
						for (String value : cxnNames.subList(1, cxnNames.size())) {
							addToNearlyIdenticalList(key, value.trim());
						}
					}
				}
				else if (currentBlock == MetaData.CHAINABLE_WATCHLIST) {
					if (line.contains(",")) {
						// watchlist items
						ArrayList<String> cxnNames = new ArrayList<String>(Arrays.asList(line.split("[\\s]")));
						addToChainable(cxnNames);
					}
				}
			}
		}
	}

	public void outputToFile(File grammarDirectory, File tableDirectory) throws IOException {
		if (grammarDirectory != null) {
			File outputPath = new File(grammarDirectory, "newGrammar.grm");
			PrintStream ps = new PrintStream(outputPath);
			ps.print(printer.format(grammar));
			ps.close();

			outputPath = new File(grammarDirectory, "newGrammar.info");
			ps = new PrintStream(outputPath);

			ps.println("\n" + MetaData.SENTENCE_LIST.getTitle() + "\n");
			for (Utterance<Word, String> utterance : cachedUtterances.entries()) {
				StringBuffer sentence = new StringBuffer();
				for (Word w : utterance.getElements()) {
					sentence.append(" ").append(w.getOrthography());
				}
				sentence.deleteCharAt(0);
				ps.println(sentence.toString());
			}

			ps.println("\n" + MetaData.GENERALIZATION_LIST.getTitle() + "\n");
			ps.println(generalizationHistory.toString());

			ps.println("\n" + MetaData.REVISION_WATCHLIST.getTitle() + "\n");
			for (String watched : watchList.keySet()) {
				ps.print(watched + ":");
				for (String paired : watchList.get(watched)) {
					ps.print(" " + paired);
				}
				ps.println("");
			}

			ps.println("\n" + MetaData.OMISSION_WATCHLIST.getTitle() + "\n");
			for (String watched : nearlyIdentical.keySet()) {
				ps.print(watched + ":");
				for (String paired : nearlyIdentical.get(watched)) {
					ps.print(" " + paired);
				}
				ps.println("");
			}

			ps.println("\n" + MetaData.CHAINABLE_WATCHLIST.getTitle() + "\n");
			for (List<String> watched : chainable) {
				ps.print(chainable.get(0));
				for (String item : watched.subList(1, watched.size() - 1)) {
					ps.print(" " + item);
				}
				ps.println("");
			}
		}

		if (tableDirectory != null) {
			constructionalSubtypeTable.outputConstituentExpansionCountTable(new File(tableDirectory,
					"constituentTable.cxn"));
			constructionalSubtypeTable.outputConstituentLocalityCostTable(new File(tableDirectory, "localityTable.loc"));
			semanticSubtypeTable.outputSemanticFillerCostTable(new File(tableDirectory, "semanticTable.sem"));
			ngram.outputNGram(new File(tableDirectory, "ngram.ngm"));
		}
	}

	public static LearnerGrammar instantiateGrammar(LearnerPrefs preferences) throws TypeSystemException, IOException {
		LearnerGrammar learnerGrammar;
		Grammar grammar = ECGGrammarUtilities.read(preferences);
		grammar.update();

		File cxnParamFile = null, locParamFile = null, ngramParamFile = null;

		File baseDirectory = preferences.getBaseDirectory();
		if (preferences.getList(AP.GRAMMAR_PARAMS_PATHS).size() > 0) {
			String grammarParamsCxnExt = preferences.getSetting(AP.GRAMMAR_PARAMS_CXN_EXTENSION) == null ? "cxn"
					: preferences.getSetting(AP.GRAMMAR_PARAMS_CXN_EXTENSION);
			List<File> cxnParamFiles = FileUtils.getFilesUnder(baseDirectory,
					preferences.getList(AP.GRAMMAR_PARAMS_PATHS), new ExtensionFileFilter(grammarParamsCxnExt));
			String grammarParamsLocExt = preferences.getSetting(AP.GRAMMAR_PARAMS_LOCALITY_EXTENSION) == null ? "cxn"
					: preferences.getSetting(AP.GRAMMAR_PARAMS_LOCALITY_EXTENSION);
			List<File> locParamFiles = FileUtils.getFilesUnder(baseDirectory,
					preferences.getList(AP.GRAMMAR_PARAMS_PATHS), new ExtensionFileFilter(grammarParamsLocExt));
			String ngramParamsExt = preferences.getSetting(AP.GRAMMAR_PARAMS_NGRAM_EXTENSION) == null ? "ngm"
					: preferences.getSetting(AP.GRAMMAR_PARAMS_NGRAM_EXTENSION);
			List<File> ngramParamFiles = FileUtils.getFilesUnder(baseDirectory,
					preferences.getList(AP.GRAMMAR_PARAMS_PATHS), new ExtensionFileFilter(ngramParamsExt));

			if (!cxnParamFiles.isEmpty())
				cxnParamFile = cxnParamFiles.get(0);
			if (!locParamFiles.isEmpty())
				locParamFile = locParamFiles.get(0);
			if (!ngramParamFiles.isEmpty())
				ngramParamFile = ngramParamFiles.get(0);
		}

		if (cxnParamFile != null && locParamFile != null && ngramParamFile != null) {
			learnerGrammar = new LearnerGrammar(grammar, new TextFileLineIterator(ngramParamFile),
					new TextFileLineIterator(cxnParamFile), new TextFileLineIterator(locParamFile));
		}
		else {
			learnerGrammar = new LearnerGrammar(grammar);
		}

		String metaExt = preferences.getSetting(LP.GRAMMAR_METADATA_EXTENSIONS);
		if (metaExt != null) {
			List<String> grammarPaths = preferences.getList(AP.GRAMMAR_PATHS);
			List<File> files = FileUtils.getFilesUnder(baseDirectory, grammarPaths, new ExtensionFileFilter(metaExt));
			if (!files.isEmpty()) {
				learnerGrammar.readMetadata(new TextFileLineIterator(files.get(0)));
			}
		}

		learnerGrammar.useBackoff = Boolean.valueOf(preferences.getSetting(AP.GRAMMAR_PARAMS_USE_CFGBACKOFF));
		List<File> semParamFiles = new ArrayList<File>();
		String grammarParamsSemExt = preferences.getSetting(AP.GRAMMAR_PARAMS_SEM_EXTENSION);
		if (grammarParamsSemExt == null) {
			grammarParamsSemExt = "sem";
		}
		semParamFiles = FileUtils.getFilesUnder(baseDirectory, preferences.getList(AP.GRAMMAR_PARAMS_PATHS),
				new ExtensionFileFilter(grammarParamsSemExt));
		if (!semParamFiles.isEmpty()) {
			try {
				learnerGrammar.semanticModelBuffer = FileReadingUtils.ReadFileIntoStringBuffer(semParamFiles.get(0)
						.getAbsoluteFile());
				learnerGrammar.semanticModelUpToDate = false;
			}
			catch (IOException ioe) {
				throw new IllegalArgumentException(ioe);
			}
		}

		return learnerGrammar;
	}

}

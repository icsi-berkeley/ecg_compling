// =============================================================================
// File        : Tester.java
// Author      : emok
// Change Log  : Created on Mar 25, 2008
//=============================================================================

package compling.learner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGGrammarUtilities.SimpleGrammarPrinter;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.LearnerPrefs;
import compling.gui.LearnerPrefs.LP;
import compling.gui.LoggingHandler;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.candidates.CategoryExpander;
import compling.learner.candidates.CategoryMerger;
import compling.learner.candidates.GeneralizationCandidate;
import compling.learner.candidates.GeneralizationFinder;
import compling.learner.grammartables.CoreRolesTable;
import compling.learner.learnertables.ConstructionalSubtypeTable;
import compling.learner.learnertables.NGram;
import compling.learner.util.LearnerGrammarPrinter;
import compling.learner.util.LearnerUtilities;
import compling.learner.util.LearnerUtilities.SubtypeMappingFunction;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.util.MapMap;
import compling.util.Pair;

//=============================================================================

public class Tester {

	static LearnerGrammarPrinter printer = new LearnerGrammarPrinter();

	public static void testCategoryExpansion(String[] args) throws Exception {

		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);
		Grammar grammar = learnerGrammar.getGrammar();

		Set<String> toExpand = new HashSet<String>();
		toExpand.add("ACxn");
		toExpand.add("CCxn");

		CategoryExpander expander = new CategoryExpander(learnerGrammar, toExpand);
		boolean changed = expander.expandCategories(1);

		if (changed) {
			System.out.println(learnerGrammar.getGrammar());
		}
	}

	public static void testMeaningGeneralization(String[] args) throws Exception {
		String cxnA = "AB";
		String cxnB = "AA";

		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);
		Grammar grammar = learnerGrammar.getGrammar();

		GeneralizationCandidate candidate = GeneralizationFinder.generatePairwiseCandidate(learnerGrammar, grammar
				.getCxnTypeSystem().getCanonicalTypeConstraint(cxnA), grammar.getCxnTypeSystem()
				.getCanonicalTypeConstraint(cxnB), new ArrayList<Role>(), true);

		List<Construction> tested = new ArrayList<Construction>();
		tested.add(grammar.getConstruction(cxnA));
		tested.add(grammar.getConstruction(cxnB));

		candidate.createNewConstructions();
		if (candidate.isViable()) {
			GrammarChanges changes = candidate.getChanges();
			Grammar.setFormatter(new SimpleGrammarPrinter());
			System.out.println(changes);
		}
	}

	public static void testGeneralization(String[] args) throws Exception {
		String cxnA = "hao2yu3_chi1-Cxn254";
		String cxnB = "ni3_gei3_yi2-Cxn002";
		String cxnC = "ni3_gei3_wo3-Cxn003";

		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		GeneralizationFinder finder = new GeneralizationFinder(learnerGrammar);

		finder.addSeed(cxnA);
		while (finder.hasNext()) {
			GeneralizationCandidate candidate = finder.next();
			candidate.createNewConstructions();
			if (candidate.isViable()) {
				GrammarChanges changes = candidate.getChanges();
				System.out.println("viable: " + candidate);
				System.out.println(changes);
				learnerGrammar.modifyGrammar(changes);
				finder.setGrammar(learnerGrammar);
			}
		}

		System.out.println("Omission? \n" + learnerGrammar.getNearlyIdenticalList());
		System.out.println("Watch? \n" + learnerGrammar.getWatchlist());
		// System.out.println(new LearnerGrammarPrinter().format(learnerGrammar.getGrammar()));
	}

	public static void testSingleGeneralization(String[] args) throws Exception {
		String cxn1 = "wo3gei3";
		String cxn2 = "ni3gei3";

		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		TypeConstraint ngTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint(cxn1);
		TypeConstraint wgTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint(cxn2);
		GeneralizationCandidate candidate = GeneralizationFinder.generatePairwiseCandidate(learnerGrammar, ngTC, wgTC,
				new ArrayList<Role>(), true);

		candidate.createNewConstructions();
		if (candidate.isViable()) {
			GrammarChanges changes = candidate.getChanges();
			boolean successfullyAdded = learnerGrammar.modifyGrammar(changes);
			if (successfullyAdded) {
				System.out.println(learnerGrammar.getGrammar());
			}
			else {
				System.out.println("nothing changed");
			}
		}

	}

	public static void testTables(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		TypeConstraint ngTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint("ni3gei3");
		TypeConstraint wgTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint("wo3gei3");

		GeneralizationCandidate candidate = GeneralizationFinder.generatePairwiseCandidate(learnerGrammar, ngTC, wgTC,
				new ArrayList<Role>(), true);

		candidate.createNewConstructions();
		if (candidate.isViable()) {
			GrammarChanges changes = candidate.getChanges();
			boolean successfullyAdded = learnerGrammar.modifyGrammar(changes);
			if (successfullyAdded) {
				Map<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>> finalRoleMapping = candidate
						.getFinalRoleMapping();
				for (GeneralizationCandidate c : finalRoleMapping.keySet()) {

					learnerGrammar.updateTablesAfterGeneralization(c.getNewCxnName(), c.getGeneralizedOver(),
							finalRoleMapping.get(c));
				}
			}
		}

		TypeConstraint cgTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint("genCxn001");
		Block cg = learnerGrammar.getGrammar().getConstruction("genCxn001").getConstructionalBlock();
		TypeConstraint n = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint("ni3");
		if (learnerGrammar.getConstructionalSubtypeTable().getExpansionTable()
				.getCount(new Pair<TypeConstraint, Role>(cgTC, cg.getRole("c0")), n) != 2) {
			System.err.println("Something's wrong");
		}

		TypeConstraint ygTC = learnerGrammar.getGrammar().getCxnTypeSystem().getCanonicalTypeConstraint("yi2gei3");
		GeneralizationCandidate candidate2 = GeneralizationFinder.generatePairwiseCandidate(learnerGrammar, cgTC, ygTC,
				new ArrayList<Role>(), true);

		candidate.createNewConstructions();
		if (candidate.isViable()) {
			GrammarChanges changes2 = candidate.getChanges();
			boolean successfullyAdded2 = learnerGrammar.modifyGrammar(changes2);
			if (successfullyAdded2) {
				Map<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>> finalRoleMapping2 = candidate2
						.getFinalRoleMapping();
				for (GeneralizationCandidate c : finalRoleMapping2.keySet()) {
					learnerGrammar.updateTablesAfterGeneralization(c.getNewCxnName(), c.getGeneralizedOver(),
							finalRoleMapping2.get(c));
				}
			}
		}

		Set<String> toMerge = new HashSet<String>();
		toMerge.add("Cat000");
		CategoryMerger merger = new CategoryMerger(learnerGrammar, "Cat002", toMerge);
		GrammarChanges changes3 = merger.mergeCategories();
		ConstructionalSubtypeTable oldCxnTable = learnerGrammar.getConstructionalSubtypeTable();
		NGram oldNGram = learnerGrammar.getNGram();
		boolean successfullyAdded3 = learnerGrammar.modifyGrammar(changes3);
		if (successfullyAdded3) {
			learnerGrammar.updateTablesAfterCategoryMerge("Cat002", toMerge, oldCxnTable, oldNGram, false);
		}

		outputGrammar(learnerGrammar, prefs);
	}

	public static void testCategoryMerger(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		Set<String> toMerge = new HashSet<String>();
		toMerge.add("Cat002");
		CategoryMerger merger = new CategoryMerger(learnerGrammar, "Cat001", toMerge);
		GrammarChanges changes = merger.mergeCategories();
		learnerGrammar.modifyGrammar(changes);
		System.out.println(changes);
		outputGrammar(learnerGrammar, prefs);
	}

	public static void testGeneralizationFinder(String[] args) throws Exception {

		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);
		Grammar grammar = learnerGrammar.getGrammar();
		Set<String> seeds = new HashSet<String>();
		try {
			seeds.addAll(grammar.getCxnTypeSystem().getAllSubtypes(
					grammar.getCxnTypeSystem().getInternedString(ChildesLocalizer.CLAUSE)));
			seeds.addAll(grammar.getCxnTypeSystem().getAllSubtypes(
					grammar.getCxnTypeSystem().getInternedString(ChildesLocalizer.PHRASE)));
			seeds.remove(ChildesLocalizer.CLAUSE);
			seeds.remove(ChildesLocalizer.PHRASE);
		}
		catch (TypeSystemException tse) {
			System.err.println(tse.getLocalizedMessage());
		}

		ECGLearnerEngine engine = new ECGLearnerEngine(learnerGrammar, false);
		engine.reorganizeConstructions(seeds);
		outputGrammar(engine.getExperimentalGrammar(), prefs);
	}

	public static void testSchemaCloneTable(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);
		Grammar g = learnerGrammar.getGrammar();

		Construction a = g.getConstruction("ni3_gei3_wo3-Cxn003");
		Construction b = g.getConstruction("ni3_ping2zi_gei3_wo3-Cxn001");

		List<Map<Role, Role>> rMappings = LearnerUtilities.mapConstituents(a.getConstructionalBlock().getElements(), b
				.getConstructionalBlock().getElements(), new SubtypeMappingFunction());
		System.out.println(LearnerUtilities.syntacticallySubsumes(a, b, rMappings));
		for (Map<Role, Role> mapping : rMappings) {
			System.out.println(mapping);
			System.out.println(LearnerUtilities.semanticallySubsumesGivenMapping(a, b, mapping, learnerGrammar));
		}
		System.out.println(LearnerUtilities.subsumes(a, b, learnerGrammar));
	}

	public static void testMDCost(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		learnerGrammar.cacheUtterance("ni3 kan4 dao4 dian4 deng1 le ba");
		learnerGrammar.cacheUtterance("bie2 gei3 ma1 ma wang3 lian3 shang4 mo3 e");
		learnerGrammar.cacheUtterance("ba3 zhei4 ge gei3 a1 yi2 ao");

		MDLCost cost = new MDLCost(learnerGrammar);
		System.out.println(cost.getDescriptionLength());

		try {
			learnerGrammar.outputToFile(new File("C:\\Documents and Settings\\emok\\Desktop\\"), null);
		}
		catch (IOException ioe) {
			System.err.println(ioe.getLocalizedMessage());
		}
	}

	private static void outputGrammar(LearnerGrammar learnerGrammar, LearnerPrefs prefs) {

		try {
			String grammarOutpath = prefs.getSetting(LP.OUTPUT_GRAMMAR_PATH);
			String tableOutpath = prefs.getSetting(LP.OUTPUT_GRAMMAR_PARAMS_PATH);
			learnerGrammar.outputToFile(ECGLearner.makeAbsoluteDir(grammarOutpath, prefs.getBaseDirectory()),
					ECGLearner.makeAbsoluteDir(tableOutpath, prefs.getBaseDirectory()));

		}
		catch (IOException ioe) {
			System.err.println(ioe.getLocalizedMessage());
		}
	}

	public static void testConstituentMap(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		Schema you = learnerGrammar.getGrammar().getSchema("You");
		Schema i = learnerGrammar.getGrammar().getSchema("I");

		List<Map<Role, Role>> mappings = LearnerUtilities.mapConstituents(you.getAllRoles(), i.getAllRoles(),
				new LearnerUtilities.EqualsMappingFunction());
		System.out.println(mappings);
	}

	public static void testConstructionalTables(String[] args) throws Exception {
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);

		System.out.println(learnerGrammar.getConstructionalSubtypeTable().outputConstituentExpansionCountTable());

		ECGAnalyzer analyzer = new ECGAnalyzer(learnerGrammar.getGrammar());
		System.out.println(analyzer.getConstituentExpansionCostTable());
	}

	public static void testCoreRoleExtraction(String[] args) throws Exception {
		Grammar.setFormatter(new SimpleGrammarPrinter());
		LearnerPrefs prefs = new LearnerPrefs(args[0]);
		LearnerGrammar learnerGrammar = LearnerGrammar.instantiateGrammar(prefs);
		CoreRolesTable crt = new CoreRolesTable(learnerGrammar.getGrammar(), learnerGrammar.getGrammarTables()
				.getSchemaCloneTable());
		System.out.println(crt);
	}

	public static void setLoggingLevel(Level level) {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		for (Handler existing : rootLogger.getHandlers()) {
			rootLogger.removeHandler(existing);
		}
		rootLogger.setLevel(level);
		rootLogger.addHandler(new LoggingHandler());
	}

	public static void main(String[] args) throws Exception {

		setLoggingLevel(Level.INFO);

		Grammar.setFormatter(printer);
		// testMeaningGeneralization(args);
		// testGeneralization(args);
		// testSingleGeneralization(args);
		// testConstituentMap(args);
		// testSchemaCloneTable(args);
		// testGeneralizationFinder(args);
		// testCategoryMerger(args);
		// testCategoryExpansion(args);
		// testConstructionalTables(args);
		testCoreRoleExtraction(args);

	}

}

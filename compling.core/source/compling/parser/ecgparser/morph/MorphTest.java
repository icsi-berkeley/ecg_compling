package compling.parser.ecgparser.morph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureUtilities;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ecgparser.AnalysisUtilities.MorphAnalysisFormatter;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.morph.MAnalysisComparison.CSet;
import compling.util.Pair;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;

/**
 * Conduct unit tests of the morphological analyzer.
 * 
 * @author Nathan Schneider
 * @see compling.parser.ecgparser.morph.MorphAnalyzer
 */
public class MorphTest {

	static int stackCounter = 0;

	public MorphTest(Grammar gmr) {

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * try { java.io.PrintWriter out = new java.io.PrintWriter(new java.io.BufferedWriter(new
		 * java.io.FileWriter("garbage.grm"))); for (int i=0; i<10000; i++) { out.append("construction X" + i + "\n");
		 * out.append("subcase of WLMorph\n"); out.append("  form constraints\n"); out.append("    self.f.orth <-- \"" +
		 * Character.toString((char)((i%26)+65)) + Character.toString((char)(((i/10)%26)+65)) +
		 * Character.toString((char)(((i/100)%26)+65)) + Character.toString((char)(((i/1000)%26)+65)) +
		 * Character.toString((char)(((i/10000)%26)+65)) + "\"\n\n"); } } catch (Exception ex) { ex.printStackTrace(); }
		 * 
		 * System.exit(0);
		 */

		// args:
		// 1) path to grammar prefs file
		// 2) word to analyze
		// -- OR --
		// 1) directory containing unit test cases

		if (args.length > 1) { // Analyze a single word given a grammar prefs file
			String inputWord = args[1];
			parseInput(args[0], inputWord);
		}
		else if (args[0].endsWith(".prefs")) { // Interactive parser
			System.out
					.println("Morphological parser running in interactive mode. Enter a word to parse, or leave blank to exit.");
			String inputWord;
			Scanner in = new Scanner(System.in);
			System.out.print("> ");
			while (!(inputWord = in.nextLine()).equals("")) {
				parseInput(args[0], inputWord);
				System.out.print("> ");
			}
		}
		else { // Do unit tests in the given path, treating each grammar file independently
			String path = args[0];
			String extensions = "grm";
			List<File> files = FileUtils.getFilesUnder(path, new ExtensionFileFilter(extensions));

			for (int i = files.size() - 1; i >= 0; i--) { // Iterate through grammar files, testing each independently. Go
																			// in reverse so as to start with the newest test set (which
																			// presumably is last alphabetically).
				File f = files.get(i);
				List<File> thisFile = new ArrayList<File>();
				thisFile.add(f);

				System.out.println("Grammar: " + f.getAbsolutePath());

				MGrammarWrapper grammar = null;
				try {
					StringBuffer errorLog = new StringBuffer();
					grammar = new MGrammarWrapper(ECGGrammarUtilities.read(thisFile, new ContextModel(new ArrayList<File>(),
							"inst", "defs")), errorLog);

					/*
					 * // Read POS probabilities // TODO: sample Map<String,Double> posTable = new HashMap<String,Double>();
					 * Construction noun = grammar.getConstruction("Noun"); Construction verb =
					 * grammar.getConstruction("Verb"); if (noun!=null) posTable.put(noun.getName(), Math.log(0.75)); if
					 * (verb!=null) posTable.put(noun.getName(), Math.log(0.25)); MAnalysis.scorer = new POSScorer(posTable);
					 */
				}
				catch (GrammarException ex) {
					System.err.println("Error reading grammar from " + f);
					ex.printStackTrace();
					System.exit(1);
				}

				// Corresponding file with test cases
				File tf = new File(path + "/" + f.getName().substring(0, f.getName().lastIndexOf(".")) + ".test");
				if (!tf.exists())
					continue;

				if (!unitTests(grammar, tf))
					break;
			}
		}

	}

	public static void parseInput(String prefsFilePath, String inputWord) throws Exception {
		boolean useProbabilities = false;

		StringBuffer errorLog = new StringBuffer();
		MGrammarWrapper grammar = new MGrammarWrapper(ECGGrammarUtilities.read(prefsFilePath), errorLog);
		MorphAnalyzer ma = null;

		// Construction[] cxntypes = {grammar.getConstruction("InflectedVerb") /*,
		// grammar.getConstruction("VerbInflection")*/ };
		// System.out.println(grammar.graphDescendants(cxntypes,false,true));
		// Schema[] schematypes = {/*grammar.grammar.getSchema("MorphForm") ,
		// grammar.grammar.getSchema("AgreementFeatures"), grammar.grammar.getSchema("AgreementFeatureSet")*/
		// grammar.grammar.getSchema("FiniteOrNonFinite") };
		// System.out.println(grammar.graphDescendants(schematypes,false,false));
		// ECGGrammarUtilities.TexGrammarPrinter tgp = new ECGGrammarUtilities.TexGrammarPrinter();
		// System.out.println(tgp.format(grammar.grammar.getConstruction("SuffixATION")));
		// System.out.println(tgp.format(grammar.grammar.getSchema("Institutionalization")));

		if (useProbabilities) // Read POS probabilities
			MAnalysis.scorer = new ParamFileWordGivenPOS(grammar, "/Users/nathan/research/ecg/dogsrun1/posparams.cxn");
		else
			MAnalysis.scorer = new UniformScorer();

		System.out.println("########################################");
		System.out.println("analyzing " + inputWord);

		StringBuffer warningLog = new StringBuffer();
		ma = new MorphAnalyzer(grammar, warningLog);
		if (warningLog.length() > 0)
			System.err.println(warningLog.toString());

		Set<MAnalysis> analyses = ma.analyze(inputWord);

		MorphAnalysisFormatter maf = new MorphAnalysisFormatter();
		maf.suppressWordIndex = true;

		// AnalysisUtilities.FlatAnalysisFormatter faf = new AnalysisUtilities.FlatAnalysisFormatter(grammar.grammar,
		// inputWord);
		FeatureStructureUtilities.FeatureStructureFormatter tfsf = new FeatureStructureUtilities.TexFeatureStructureFormatter();
		FeatureStructureUtilities.FeatureStructureFormatter dfsf = new FeatureStructureUtilities.DefaultStructureFormatter();

		ma.showAnalyses(analyses, inputWord, maf, dfsf /* tfsf */);
	}

	static final String ANALYSIS_SEP = "-------------------------------------";

	/**
	 * Load a set of test exercises from a file, and check whether the morphological analyzer produces the correct output
	 * under the corresponding grammar. Display results in the standard output console and any discrepancies or grammar
	 * errors/warnings in the error console.
	 * 
	 * @param grammar
	 * @param testCases
	 * @return
	 */
	public static boolean unitTests(MGrammarWrapper grammar, File testCases) {
		MorphAnalyzer ma = null;
		try {
			StringBuffer warningLog = new StringBuffer();

			ma = new MorphAnalyzer(grammar, warningLog);

			if (warningLog.length() > 0)
				System.err.print(warningLog);
		}
		catch (GrammarException ex2) {
			ex2.printStackTrace();
			System.exit(1);
		}

		String input = null;
		boolean passedAll = true;
		boolean inComment = false; // In a block comment
		StringBuffer output = new StringBuffer();

		try {
			BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(testCases), "UTF-8"));
			String ln = "";
			List<Pair<List<CxnalSpan>, List<CSet>>> desiredResults = new ArrayList<Pair<List<CxnalSpan>, List<CSet>>>();
			List<CxnalSpan> spanList = new ArrayList<CxnalSpan>();
			List<CSet> csetList = new ArrayList<CSet>();
			CSet cset = new CSet();
			String section = ""; // "c" => cxn list; "s" => semantic schema list; "b" => semantic constraints (bindings)
			String newSection = ""; // if the current line is a section label

			while ((ln = fin.readLine()) != null && (ln = ln.trim()) != null) {
				if (ln.equals(""))
					continue;

				int commentStart = ln.indexOf("//");
				if (commentStart > -1)
					ln = ln.substring(0, commentStart);
				while ((inComment && (commentStart = 0) == 0) || ((commentStart = ln.indexOf("/*")) > -1)) {
					int commentEnd = ln.indexOf("*/", ((inComment) ? commentStart : commentStart + 2));
					if (commentEnd == -1) { // In a block comment that extends beyond this line
						inComment = true;
						ln = ln.substring(0, commentStart);
						break;
					}
					else {
						inComment = false;
						ln = ln.substring(0, commentStart) + ln.substring(commentEnd + 2);
					}
				}

				if (ln.equals(""))
					continue;

				if (ln.startsWith("> ")) {
					if (input != null) {
						passedAll = passedAll && passJudgment(input, desiredResults, ma, output);
						desiredResults.clear();
					}

					input = ln.trim().substring(2);
				}
				else if (ln.startsWith("Constructions Used:")) {
					newSection = "c";
				}
				if (ln.startsWith("Schemas Used:") || ln.startsWith("Semantic Schemas Used:")
						|| ln.startsWith("Form Schemas Used:")) {
					newSection = "s";
				}
				else if (ln.startsWith("Semantic Constraints:")) {
					newSection = "sb";
				}
				else if (ln.startsWith("Form Constraints:")) {
					newSection = "fb";
				}
				else if (ln.startsWith(ANALYSIS_SEP)) {
					newSection = "";
				}

				if (section.equals("c")) {
					if (!newSection.equals("c")) { // We just entered a new section
						if (newSection.equals("")) {
							desiredResults.add(new Pair<List<CxnalSpan>, List<CSet>>(spanList, csetList));
							spanList = new ArrayList<CxnalSpan>();
							csetList = new ArrayList<CSet>();
						}
					}
					else {
						java.util.regex.Pattern pat = java.util.regex.Pattern
								.compile("([A-Za-z0-9_]+)\\[([0-9]+)\\]\\s\\(([0-9]+)[,]\\s([0-9]+)\\)");
						java.util.regex.Matcher mat = pat.matcher(ln);
						if (!mat.matches()) {
							System.err.println("Error in test case: unable to parse " + ln);
						}
						Construction c = grammar.getConstruction(mat.group(1));
						if (c == null)
							System.err.println("Construction " + mat.group(1) + " in test case not found in grammar.");
						CxnalSpan newspan = new CxnalSpan(null, c, Integer.parseInt(mat.group(2)), 0, 0, Integer.parseInt(mat
								.group(3)), Integer.parseInt(mat.group(4)));
						spanList.add(newspan);
					}
				}
				else if (section.equals("sb")) {
					if (!newSection.equals("sb")) { // We just entered a new section
						if (newSection.equals("")) {
							if ((csetList.size() == 0 || csetList.get(csetList.size() - 1) != cset)
									&& cset.schemaRoles.size() > 0) {
								csetList.add(cset);
								cset = new CSet();
							}
							desiredResults.add(new Pair<List<CxnalSpan>, List<CSet>>(spanList, csetList));
							spanList = new ArrayList<CxnalSpan>();
							csetList = new ArrayList<CSet>();
						}
					}
					else {
						ln.replace("<-->", "");

						if (ln.startsWith("Filler:")) { // e.g. "Filler: Container[2]"
							String fillerType = ln.substring(8, ln.indexOf("["));
							int slotIndex = Integer.parseInt(ln.substring(ln.indexOf("[") + 1, ln.indexOf("]")));
							cset.fillerType = fillerType;
							cset.slotIndex = slotIndex;
							csetList.add(cset);
							cset = new CSet();
						}
						else { // e.g. "X1[6].cont"
							String schemaType = ln.substring(0, ln.indexOf("["));
							String roleName = ln.substring(ln.indexOf(".") + 1);
							cset.addRole(schemaType, new Role(roleName));
						}
					}
				}
				else if (ln.startsWith("!")) { // A grammar error is expected
					// TODO
				}
				else if (newSection.equals("") && spanList.size() > 0) {
					desiredResults.add(new Pair<List<CxnalSpan>, List<CSet>>(spanList, csetList));
					spanList = new ArrayList<CxnalSpan>();
					csetList = new ArrayList<CSet>();
				}

				section = newSection;
			}

			if (input != null) {
				passedAll = passedAll && passJudgment(input, desiredResults, ma, output);
				desiredResults.clear();
			}

		}
		catch (java.io.IOException ex) {
			System.err.println("Error reading test case");
			ex.printStackTrace();
			return false;
		}

		System.out.println("Test results for " + testCases.getName() + ": " + ((passedAll) ? "PASS" : "FAIL"));

		if (!passedAll) {
			System.out.print(output);
//			System.out.println(MorphAnalyzer.glCharts);
		}
		return passedAll;
	}

	static boolean passJudgment(String input, List<Pair<List<CxnalSpan>, List<CSet>>> desiredResults, MorphAnalyzer ma,
			StringBuffer output) {
		Set<MAnalysis> analyses = ma.analyze(input);

		boolean m = MAnalysisComparison.matchesAnalyses(desiredResults, analyses);
		output.append("<" + input + "> " + ((m) ? "PASS" : "FAIL") + "\n");
		if (!m) {
			output.append("RETURNED ANALYSES:\n" + analyses.toString());
			output.append(ma.glCharts);
		}

		return m;
	}

}

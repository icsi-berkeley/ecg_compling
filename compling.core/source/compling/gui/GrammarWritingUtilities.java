// =============================================================================
//File        : GrammarWritingUtilities.java
//Author      : emok
//Change Log  : Created on Jul 28, 2007
//=============================================================================

package compling.gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesIterator;
import compling.annotation.childes.ChildesTranscript;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesTranscript.ChildesEvent;
import compling.annotation.childes.ChildesTranscript.ChildesItem;
import compling.annotation.childes.FeatureBasedEntity.Binding;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.context.ContextException.ItemNotDefinedException;
import compling.context.ContextModel;
import compling.context.ContextUtilities.MiniOntologyFormatter;
import compling.context.ContextUtilities.OntologyGraphPrinter;
import compling.context.ContextUtilities.SimpleOntologyPrinter;
import compling.context.MiniOntology;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.ECGGrammarUtilities.SimpleGrammarPrinter;
import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.gui.LearnerPrefs.LP;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.NoECGAnalysisFoundException;
import compling.simulator.Simulator;
import compling.simulator.SimulatorException;
import compling.simulator.SimulatorException.ScriptNotFoundException;
import compling.util.Pair;
import compling.util.PriorityQueue;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;
import compling.utterance.Word;

//=============================================================================

public class GrammarWritingUtilities {

	static PrintStream printStream = System.out;

	private Set<Pair<String, String>> undefinedLex = new LinkedHashSet<Pair<String, String>>();
	private Set<String> undefinedScripts = new LinkedHashSet<String>();
	private Set<String> undefinedTypes = new LinkedHashSet<String>();

	private boolean analyze = false;
	private boolean simulate = false;
	private boolean makeSnapShots = false;
	private int fileCounter = 1;

	File output = null;
	List<File> dataFiles = null;
	String ontFileName = null;
	StringBuffer defs = null;

	Grammar grammar = null;
	Simulator simulator = null;
	ContextModel contextModel = null;
	ECGAnalyzer analyzer = null;

	LearnerPrefs preferences = null;

	public GrammarWritingUtilities(LearnerPrefs prefs) {
		try {
			preferences = prefs;

			makeSnapShots = preferences.getSetting(LP.OUTPUT_SNAPSHOTS) == null ? false : Boolean.valueOf(preferences
					.getSetting(LP.OUTPUT_SNAPSHOTS));
			output = preferences.getSetting(LP.OUTPUT_SNAPSHOTS_PATH) == null ? null : new File(
					preferences.getSetting(LP.OUTPUT_SNAPSHOTS_PATH));

			if (output != null && output.isFile() && !makeSnapShots) {
				setPrintStream(new PrintStream(output));
			}
			File baseDirectory = preferences.getBaseDirectory();

			List<String> dpaths = preferences.getList(LP.DATA_PATHS);
			String dext = preferences.getSetting(LP.DATA_EXTENSIONS);
			dataFiles = FileUtils.getFilesUnder(baseDirectory, dpaths, new ExtensionFileFilter(dext));

			grammar = ECGGrammarUtilities.read(preferences);
			grammar.update();
			contextModel = grammar.getContextModel();

			List<String> spaths = preferences.getList(LP.SCRIPT_PATHS);
			String sext = preferences.getSetting(LP.SCRIPT_EXTENSIONS);
			simulator = new Simulator(contextModel, FileUtils.getFilesUnder(baseDirectory, spaths,
					new ExtensionFileFilter(sext)));

			analyze = Boolean.valueOf(preferences.getSetting(LP.ANALYZE));
			simulate = Boolean.valueOf(preferences.getSetting(LP.SIMULATE));
		}
		catch (IOException ioe) {
			outputErrorMessage(ioe);
		}
		catch (TypeSystemException e) {
			outputErrorMessage(e);
		}

	}

	protected static void setPrintStream(PrintStream printStream) {
		GrammarWritingUtilities.printStream = printStream;
	}

	protected void outputErrorMessage(Exception e) {
		System.out.println(e.getMessage());
		if (e.getCause() != null) {
			System.err.println(e.getMessage());
			System.err.println(e.getCause().getMessage());
		}
		if (!(e instanceof ScriptNotFoundException || e instanceof ItemNotDefinedException || e.getMessage().contains(
				"No speech act annotation found"))) {
			e.printStackTrace(System.err);
		}
	}

	public Grammar getGrammar() {
		return grammar;
	}

	protected void checkGoldStandardAnnotation(ChildesClause clause) {
		GoldStandardAnnotation annotation = clause.getChildesAnnotation().getGoldStandardTier().getContent();
		String vern = clause.getChildesAnnotation().getVernacularTier().getContent();

		for (ExtendedFeatureBasedEntity tag : annotation.getArgumentStructureAnnotations()) {
			if (tag.getSpanLeft() != null && tag.getSpanRight() != null) {
				int left = tag.getSpanLeft();
				int right = tag.getSpanRight();
				printStream.print(tag.getJDOMElement().getName() + "\t");
				printStream.print(vern.substring(left, right) + "\t");
				if (tag.getAttributeValue("ref") != null) {
					printStream.print(tag.getAttributeValue("ref"));
				}
				else {
					printStream.print(tag.getCategory());
				}
				printStream.print("\t");
				if (tag.getCategory() != null && tag.getCategory().toLowerCase().contains("state")) {
					try {
						printStream.print(tag.getBinding("property").iterator().next().getAttributeValue("value"));
					}
					catch (NullPointerException e) {
					}
				}

				printStream.println("\t" + clause.getSource());
				for (String role : tag.getRoles()) {
					for (Binding binding : tag.getBinding(role)) {
						if (binding.getSpanLeft() != null && binding.getSpanRight() != null) {
							int bleft = binding.getSpanLeft();
							int bright = binding.getSpanRight();

							printStream.print("\t");
							printStream.print(vern.substring(bleft, bright) + "\t");
							if (binding.getAttributeValue("ref") != null) {
								printStream.print(binding.getAttributeValue("ref"));
							}
							else if (binding.getAttributeValue("value") != null) {
								printStream.print(binding.getAttributeValue("value"));
							}
							else {
								printStream.print(binding.getField());
							}
							printStream.print("\t");
							if (binding.getAttributeValue("subcat") != null) {
								printStream.print(binding.getAttributeValue("subcat"));
							}
							printStream.println("\t" + clause.getSource());
						}
					}
				}

			}
			// System.out.println(tag);
		}
		// System.out.println(annotation);
	}

	protected void checkLexicon(ChildesClause clause, Grammar grammar) {
		List<Word> undefinedWords = new ArrayList<Word>();
		List<Word> words = clause.getElements();
		for (Word word : words) {
			try {
//            grammar.getLexicalConstruction("\"" + word.getOrthography() + "\"");
			}
			catch (GrammarException ge) {
				undefinedWords.add(word);

				int index = words.indexOf(word);
				String chinese = clause.getChildesAnnotation().getVernacularTier().getWordsAt(index, index + 1);
				boolean added = undefinedLex.add(new Pair<String, String>(word.getOrthography(), String.valueOf(chinese)));
				if (added) {
					System.out.println(word.getOrthography() + " : " + chinese);
				}
			}
		}
	}

	protected void printUtterance(ChildesClause clause) {
		List<Word> words = clause.getElements();
		StringBuilder sb = new StringBuilder();
		for (Word word : words) {
			sb.append(word.getOrthography()).append(' ');
		}
		System.out.println(sb);
	}

	protected void crazyCounting(ChildesClause clause) {
		Word targetWord = new Word("gei3");

		List<Word> words = clause.getElements();
		int index = words.indexOf(targetWord);
		if (index != -1) {
			String vern = clause.getChildesAnnotation().getVernacularTier().getContent();
			GoldStandardAnnotation annotation = clause.getChildesAnnotation().getGoldStandardTier().getContent();
			for (ExtendedFeatureBasedEntity tag : annotation.getAllAnnotations()) {
				if (tag.getSpanLeft() != null && tag.getSpanLeft() == index && tag.getSpanRight() == index + 1) {
					System.out.println(vern + "\t" + tag.getCategory());
					break;
				}
				else if (tag.getType() != null && tag.getType().equals("benefaction")
						|| tag.getType().equals("malefaction")) {
					System.out.println(vern + "\t" + tag.getType());
					break;
				}
			}

		}
	}

	protected void analyzeUtterance(ChildesClause clause) {

		System.out.println("Analyzing.... ");
		printUtterance(clause);

		PriorityQueue<?> pqa;
		try {
			if (analyzer.robust()) {
				pqa = analyzer.getBestPartialParses(clause);

			}
			else {
				pqa = analyzer.getBestParses(clause);
			}
			while (pqa.size() > 0) {
				System.out.println("\n\nRETURNED ANALYSIS\n____________________________\n");
				System.out.println("Cost: " + pqa.getPriority());
				System.out.println(pqa.next());
			}
		}
		catch (NoECGAnalysisFoundException neafe) {
			System.out.println("\n\nEXCEPTIONALLY BAD ANALYSIS\n____________________________\n");
			for (Analysis a : neafe.getAnalyses()) {
				System.out.println(a);
			}
		}
		catch (ParserException pe) {
			System.err.println(pe.getLocalizedMessage());
			pe.printStackTrace();
			System.out.println(analyzer.getParserLog());
		}
	}

	protected void processTranscript(File datafile) throws IOException {

		ChildesTranscript transcript = new ChildesTranscript(datafile);

		if (simulate) {
			simulator.initializeParticipants(transcript.getParticipantIDs());
			simulator.initializeSetting(transcript.getSettingEntitiesAndBindings(),
					transcript.getSetupEntitiesAndBindings());
		}

		ChildesIterator transcriptIter = transcript.iterator();

		while (transcriptIter.hasNext()) {
			try {
				ChildesItem item = transcriptIter.next();
				if (item instanceof ChildesClause) {
					ChildesClause clause = (ChildesClause) item;
					if (clause.size() > 0) {
						// System.out.println("clause id = " + clause.getID());
						if (simulate) {
							boolean success = simulator.registerUtterance(clause, new HashSet<String>());
						}
						checkLexicon(clause, grammar);
						// checkGoldStandardAnnotation(clause);
						// printUtterance(clause);
						// crazyCounting(clause);
						// printStatus(success, verbose);

						if (analyze) {
							analyzeUtterance(clause);
						}

					}
				}
				else if (item instanceof ChildesEvent && simulate) {
					boolean success = simulator.simulateEvent((ChildesEvent) item);
					if (!success)
						System.out.println(item.getID() + " " + success);
					// printStatus(success, verbose);
				}
				if (makeSnapShots) {
					makeSnapShot();
				}

			}
			catch (ScriptNotFoundException snfe) {
				// outputErrorMessage(snfe);
				undefinedScripts.add(snfe.getScriptName());
			}
			catch (ItemNotDefinedException infe) {
				// outputErrorMessage(infe);
				undefinedTypes.add(infe.getUnknownItem());
			}
			catch (SimulatorException se) {
				outputErrorMessage(se);
			}
		}
		if (!makeSnapShots && output != null) {
			outputContextModelGraph();
		}
	}

	protected void processTranscripts() {
		try {
			for (File dataFile : dataFiles) {

				System.out.println("Processing... " + dataFile.getName() + " ..... ");
				System.out.flush();
				grammar.getContextModel().reset();

				if (analyze) {
					analyzer = new ECGAnalyzer(grammar);
				}

				processTranscript(dataFile);
			}

		}
		catch (IOException ioe) {
			outputErrorMessage(ioe);
		}
		catch (AnnotationException ae) {
			outputErrorMessage(ae);
		}
		catch (SimulatorException se) {
			outputErrorMessage(se);
		}
		catch (GrammarException ge) {
			outputErrorMessage(ge);
		}
		finally {
			outputToFile();
		}
	}

	public void outputToFile() {
		printStream.println("============================");
		printStream.println("Undefined Lexical Items");
		printStream.println("============================");
		for (Pair<String, String> lex : undefinedLex) {
			printStream.println(lex.getFirst() + ":" + lex.getSecond());
		}
		printStream.println("\n\n");
		printStream.println("============================");
		printStream.println("Undefined Types");
		printStream.println("============================");
		for (String type : undefinedTypes) {
			printStream.println(type);
		}
		printStream.println("\n\n");
		printStream.println("============================");
		printStream.println("Undefined Scripts");
		printStream.println("============================");
		for (String script : undefinedScripts) {
			printStream.println(script);
		}
	}

	public void outputContextModelGraph() {
		MiniOntology.setFormatter(new OntologyGraphPrinter());
		printStream.println(contextModel.getMiniOntology());
	}

	public void makeSnapShot() throws IOException {
		int zeros = 3 - String.valueOf(fileCounter).length();

		String count = "";
		for (int i = 0; i < zeros; i++) {
			count += "0";
		}
		count += String.valueOf(fileCounter);

		String filename = "contextModel" + count + ".dt";

		File snapshot = new File(output, filename);
		setPrintStream(new PrintStream(snapshot));
		outputContextModelGraph();

		String[] cmd = { "cmd", "/c", "dot", "-Nshape=polygon", "-Nsides=6", "-o",
				output.getPath() + File.separator + "cm" + count + ".png", "-Tpng", snapshot.getPath() };
		Runtime.getRuntime().exec(cmd);

		fileCounter++;
	}

	public void printStatus(boolean success, boolean verbose) {
		System.out.println(success);
		if (verbose) {
			System.out.println("*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
			MiniOntologyFormatter old = MiniOntology.getFormatter();
			MiniOntology.setFormatter(new SimpleOntologyPrinter());
			System.out.println(contextModel.getMiniOntology());
			MiniOntology.setFormatter(old);
			System.out.println("~~~~~~~~~~~~~ cache ~~~~~~~~~~~~~~");
			System.out.println(contextModel.getContextModelCache());
			System.out.println("*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
		}
	}

	public static void setLoggingLevel(Level level) {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		for (Handler existing : rootLogger.getHandlers()) {
			rootLogger.removeHandler(existing);
		}
		rootLogger.setLevel(level);
		rootLogger.addHandler(new LoggingHandler());
	}

	public static void main(String[] argv) throws IOException {

		if (argv.length < 1) {
			String errormsg = "usage: <learner preference file>";
			System.err.println(errormsg);
			System.exit(1);
		}

		LearnerPrefs prefs = new LearnerPrefs(argv[0]);

		String globalLoggingLevel = prefs.getLoggingLevels().get(ComplingPackage.GLOBAL);
		Level loggingLevel = globalLoggingLevel != null ? Level.parse(globalLoggingLevel) : Level.INFO;
		setLoggingLevel(loggingLevel);

		GrammarWritingUtilities util = new GrammarWritingUtilities(prefs);
		Grammar.setFormatter(new SimpleGrammarPrinter());
		util.processTranscripts();

	}

}

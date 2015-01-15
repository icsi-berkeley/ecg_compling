/**
 * Some utility functions. Used in the compling.gui.grammargui package.
 * 
 * TODO: move this somewhere else.
 */

package compling.gui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities;
import compling.parser.ecgparser.AnalysisUtilities.DefaultAnalysisFormatter;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.NoECGAnalysisFoundException;
import compling.util.Arrays;
import compling.util.Pair;
import compling.util.PriorityQueue;
import compling.utterance.Sentence;


/**
 * Utility class. Probably to be merged to some other utility class somewhere else.
 * 
 * @author lucag
 */
public final class Utils {
	
	private static HashMap<String, String> parseCache = new HashMap<String, String>();
	private static HashMap<String, Collection<IParse>> alterParseCache = new HashMap<String, Collection<IParse>>();
	
	public static void flushCaches(ECGAnalyzer analyzer) {
		String[] keySet = parseCache.keySet().toArray(new String[parseCache.keySet().size()]);
		for (String key : keySet) {
			System.out.println("flushed " + key + " parse");
			alterParseCache.remove(key);
			alterParseCache.put(key, getParses(key, analyzer));
			parseCache.remove(key);
			parseCache.put(key, parse(key, analyzer));
			System.out.println("replaced " + key + " parse");
		}
		/*
		for (String key : parseCache.keySet()) {
			System.out.println("flushed " + key + " parse");
			alterParseCache.remove(key);
			alterParseCache.put(key, getParses(key, analyzer));
			System.out.println("replaced " + key + " parse");
		}
		for (String key : alterParseCache.keySet()) {
			System.out.println("flushed " + key + " parse");
			parseCache.remove(key);
			parseCache.put(key, parse(key, analyzer));
			System.out.println("replaced " + key + " parse");
		}
		*/
		
	}

	public static class Parse implements IParse {
		public static final IParse NO_PARSE = new Parse((Analysis) null, 0);
		
		protected double cost;
		protected Collection<Analysis> analyses;

		public Parse(Analysis analysis, double cost) {
			this.analyses = new ArrayList<Analysis>();
			this.analyses.add(analysis);
			this.cost = cost;
		}

		public Parse(Collection<Analysis> analyses, double cost) {
			this.analyses = analyses;
			this.cost = cost;
		}

		@Override
		public Collection<Analysis> getAnalyses() {
			return analyses;
		}

		@Override
		public double getCost() {
			return cost;
		}

	}

	private static final DefaultAnalysisFormatter DEFAULT_FORMATTER = new AnalysisUtilities.DefaultAnalysisFormatter();

	private static final GraphvizAnalysisFormatter GRAPHVIZ_FORMATTER = new GraphvizAnalysisFormatter();

	/**
	 * Emits a Graphviz representation for the analysis
	 * 
	 * @param analays
	 * @return A Graphviz representation
	 */
	public static String emit(Analysis analysis) {
		Analysis.setFormatter(GRAPHVIZ_FORMATTER);
		String code = analysis.toString();
		Analysis.setFormatter(DEFAULT_FORMATTER);
		return code;
	}

	public static TypeSystemNode fromDescriptor(Grammar grammar, Object[] descriptors) {
		return fromDescriptor(grammar, descriptors[0].toString(), descriptors[1].toString());
	}

	/**
	 * Retrieves a node in the grammar from a descriptor in the form "CONSTRUCTION/Name" or "CONSTRUCTION:Name".
	 * 
	 * @param grammar
	 * @param xdescriptor
	 * @return the corresponding {@link TypeSystemNode}
	 */
	public static TypeSystemNode fromDescriptor(Grammar grammar, String descriptor) {
		return fromDescriptor(grammar, descriptor.split("[:/]"));
	}

	public static TypeSystemNode fromDescriptor(Grammar grammar, String type, String name) {
		switch (TypeSystemNodeType.fromString(type)) {
		case CONSTRUCTION:
			return grammar.getConstruction(name);
		case SCHEMA:
			return grammar.getSchema(name);
		case MAP:
			return grammar.getMap(name);
		case SITUATION:
			return grammar.getSituation(name);
		case ONTOLOGY:
			return grammar.getOntologyTypeSystem().get(name);
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @param node
	 *           - A {@link compling.grammar.unificationgrammar.TypeSystemNode}
	 * @return the Node's simple name
	 * 
	 * @see TypeSystemNodeType
	 */
	public static String getNodeType(TypeSystemNode node) {
		return node.getClass().getSimpleName();
	}

	// TODO: make this synchronized?
	public static Collection<IParse> getParses(String text, ECGAnalyzer analyzer) {
		if (alterParseCache.containsKey(text)) {
			System.out.println("Retrieving from cache...");
			return alterParseCache.get(text);
		} 
		assert text != null;
		Collection<IParse> parses = new ArrayList<IParse>();
		Sentence sentence = new Sentence(Arrays.split(text));
		if (analyzer.robust()) {

			PriorityQueue<List<Analysis>> pp = analyzer.getBestPartialParses(sentence);
			while (pp.size() > 0) {
				double priority = pp.getPriority();
				List<Analysis> analyses = pp.next();
				parses.add(new Parse(analyses, priority));
			}
		}
		else {
			PriorityQueue<Analysis> pp = analyzer.getBestParses(sentence);
			while (pp.size() > 0) {
				double priority = pp.getPriority();
				Analysis analysis = pp.next();
				parses.add(new Parse(analysis, priority));
			}
		}
		System.out.println("Inserting into cache...");
		alterParseCache.put(text, parses);
		return parses;
	}

//	public static Collection<Pair<Analysis, Double>> getFlattenedParses(String text, ECGAnalyzer analyzer) {
//		assert text != null;
//		
//		Collection<Pair<Analysis, Double>> pairs = new ArrayList<Pair<Analysis, Double>>(); 
//		Sentence sentence = new Sentence(Arrays.split(text));
//		if (analyzer.robust()) {
//			PriorityQueue<List<Analysis>> pp = analyzer.getBestPartialParses(sentence);
//			while (pp.size() > 0) {
//				double priority = pp.getPriority();
//				for (Analysis a : pp.next())
//					pairs.add(Pair.make(a, priority));
//			}
//		}
//		else {
//			PriorityQueue<Analysis> pp = analyzer.getBestParses(sentence);
//			while (pp.size() > 0) {
//				double priority = pp.getPriority();
//				pairs.add(Pair.make(pp.next(), priority));
//			}
//		}
//		return pairs;
//	}

	public static Collection<Pair<Analysis, Double>> getFlattened(Collection<IParse> parses) {
		Collection<Pair<Analysis, Double>> pairs = new ArrayList<Pair<Analysis, Double>>();
		for (IParse p : parses) 
			for (Analysis a : p.getAnalyses())
				pairs.add(Pair.make(a, p.getCost()));
		
		return pairs;
	}
	
	/**
	 * 
	 * @param file
	 *           the file whose path needs to be "relativized"
	 * @param base
	 *           the base
	 * @return a File relative to base
	 */
	public static File getRelativeTo(File file, File base) {
		if (base != null)
			return new File(file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1));
		else
			return file;
	}

	/**
	 * Returns the Graphviz representation of all the parses for the sentence
	 * 
	 * @param sentence
	 * @param analyzer
	 * @return
	 */
	public static List<String> getTextParses(String sentence, ECGAnalyzer analyzer) {
		List<String> words = Arrays.split(sentence);
		List<String> buffers = new ArrayList<String>();
		if (analyzer.robust()) {
			PriorityQueue<List<Analysis>> parses = analyzer.getBestPartialParses(new Sentence(words, null, 0));
			while (parses.hasNext())
				for (Analysis a : parses.next())
					buffers.add(emit(a));
		}
		else {
			PriorityQueue<Analysis> parses = analyzer.getBestParses(new Sentence(words, null, 0));
			while (parses.hasNext())
				buffers.add(emit(parses.next()));
		}
		return buffers;
	}

	public static String toCapitalized(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	public static String toKey(TypeSystemNode node) {
		return String.format("%s/%s", node.getClass().getSimpleName(), node.getType());
	}

	public static String parse(String sentence, ECGAnalyzer analyzer) {
		if (parseCache.containsKey(sentence)) {
			System.out.println("Retrieving from cache...");
			return parseCache.get(sentence);
		} 
		List<String> words = Arrays.split(sentence);
		StringBuilder result = new StringBuilder();
		Analysis.setFormatter(new GuiAnalysisFormatter());
		FeatureStructureSet.setFormatter(new GuiFeatureStructureFormatter());
		// output.append("\nAnalyzing sentence: " + source.getText() + "\n");
		try {
			if (analyzer.robust()) {
				PriorityQueue<List<Analysis>> parses = analyzer.getBestPartialParses(new Sentence(words, null, 0));
				while (parses.size() > 0) {
					result.append(String.format("Sentence: \"%s\"\n\n", sentence));
					result.append("Returned analysis:\n\n");
					result.append(String.format("Cost: %s\n", parses.getPriority()));
	
					for (Analysis a : parses.next()) {
						result.append(a.toString());
						result.append("\n\nSemantic Specification:\n");
						FeatureStructureSet fs = a.getFeatureStructure();
						result.append(fs.toString());
					}
				}
			}
			else {
				result.append(String.format("Sentence: \"%s\"\n\n", sentence));
	
				PriorityQueue<Analysis> parses = analyzer.getBestParses(new Sentence(words, null, 0));
				while (parses.size() > 0) {
					result.append("\n\nReturned analysis:\n\n");
					result.append(String.format("Cost: %s\n", parses.getPriority()));
					Analysis analysis = parses.next();
					result.append(analysis.toString());
					result.append("\n\nSemantic Specification:\n\n");
					FeatureStructureSet fs = analysis.getFeatureStructure();
					result.append(fs.toString());
				}
			}
			result.append(analyzer.getParserLog());
			if (analyzer.debug()) {
			}
		}
		catch (NoECGAnalysisFoundException e) {
			result.append("\n\nEXCEPTIONALLY BAD ANALYSIS:\n\n");
			for (Analysis a : e.getAnalyses()) {
				result.append(a);
			}
		}
		catch (ParserException e) {
			System.err.printf("Grammar exception: %s\n", e);
//			Log.logError(e, "Parser Exception");
		}
		catch (GrammarException e) {
			System.err.printf("Grammar exception: %s\n", e);
//			Log.logError(e, "Grammar Exception");
		}
		System.out.println("Inserting into cache...");
		parseCache.put(sentence, result.toString());
		return result.toString();
	}

	public static void main(String[] args) throws Exception {
		getParses("he moved", new ECGAnalyzer(args[0]));
	}
//	public static String toString(Viewer viewer) {
//		return String.format("Output::%s", viewer.getData(GrammarBrowser.OUTPUT_SHELL_ID));
//	}
//
//	public static String toString(Widget widget) {
//		return String.format("Output::%s", widget.getData(GrammarBrowser.OUTPUT_SHELL_ID));
//	}
}

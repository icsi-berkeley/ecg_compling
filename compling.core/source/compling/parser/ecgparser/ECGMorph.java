package compling.parser.ecgparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.hp.hpl.jena.sparql.util.StringUtils;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.GrammarWrapper;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.util.StringUtilities;
import compling.util.fileutil.TextFileLineIterator;

/**
 * Simple morphology lookup using a dictionary. This version is based on the 
 * Celex database.
 * 
 * Perhaps we should define an abstract interface for this?
 * 
 * The Analyzer must already have been created and the grammar read so that
 * we can store only words actually referenced.
 *
 * The format of the .ecgmorph file is one entry per line. Each entry consists 
 * of a wordform followed by whitespace followed by morphs. Each morph is the
 * lemma followed by whitespace followed by the analysis. The analysis consists
 * of inflections separated by a slash (/).
 * 
 * Example:
 * 
 * blocking  block PresentTense/Participle
 * blocks    block Plural 		block Singular/PresentTense/ThirdPerson
 *
 * Typical usage:
 * 
 * 		ECGMorph morph = new ECGMorph(analyzer);
 *      for (String lemma : morph.getLemmas(wordform)) {
 *        [...]
 *        if (morph.match(wordform, lemma, inflectionspec) { 
 *           [...]
 *           
 * @author Adam Janin
 */

public class ECGMorph {
	ECGAnalyzer analyzer;
	Grammar grammar;
	GrammarWrapper grammarWrapper;
	AnalyzerPrefs prefs;
	File ecgmorph_path;
	HashMap<String, List<MorphEntry> > morphs;
	ECGTokenReader tokenReader;
	
	/**
	 * Store a single morph entry, including the lemma and the inflections.
	 */
	public class MorphEntry {
		String lemma;
		TreeSet<String> inflections;
		
		/**
		 * @param lemma_str         The lemma (e.g. block)
		 * @param inflections_str   The string representing the inflection (e.g. Singular/PastTense/FirstPerson)
		 */
		public MorphEntry(String lemma_str, String inflections_str) {
			lemma = lemma_str;
			inflections = new TreeSet<String>(); //new HashSet<String>();
			for (String inflect : inflections_str.split("\\s*[,/]\\s*")) {
				inflections.add(inflect);			
			}
		} // MorphEntry()
	} // class MorphEntry
	
	/**
	 * Given an already loaded Analyzer, read a morph dictionary. Only
	 * words that actually exist in the grammar within the Analyzer are
	 * loaded.
	 * 
	 * TODO: Account for for "lemma=" in addtion to "orth=" in the grammar.
	 * 
	 * @param analyzer_arg
	 * @throws IOException
	 */
	public ECGMorph(GrammarWrapper wrapper, ECGTokenReader tokener) throws IOException { //ECGAnalyzer analyzer_arg) throws IOException {
		//analyzer = analyzer_arg;
		//grammar = analyzer.getGrammar();
		grammarWrapper = wrapper;
		//grammarWrapper = analyzer.getGrammarWrapper();
		// prefs = (AnalyzerPrefs) grammar.getPrefs();
		tokenReader = tokener;
		
		morphs = new HashMap<String, List<MorphEntry>>();
		
		prefs = (AnalyzerPrefs) grammarWrapper.getGrammar().getPrefs();
		
		File base = prefs.getBaseDirectory();	

		List<String> morph_paths = prefs.getList(AP.MORPHOLOGY_PATH);
		for (String path : morph_paths) {
			ecgmorph_path = new File(base, path);
	
			TextFileLineIterator tfli = new TextFileLineIterator(ecgmorph_path);
			
			int lineNum = 0;
									
			while (tfli.hasNext()) {
				lineNum++;
				String line = tfli.next();
				// Skip blank lines or lines with just a comment
				if (line.matches("\\s*#.*") || line.matches("\\s*")) {
					continue;
				}
				String splitline[] = line.split("\\s+");
				if (splitline.length < 3) {
					// TODO: Create a MorphException class and throw that instead
					throw new ParserException("Improperly formatted entry in morph file " + ecgmorph_path + ", line " + lineNum);
				}
				if (entryInGrammar(splitline)) {
					// System.out.println("Found morph for \"" + entrystr[0] + "\"");
					List<MorphEntry> morphlist;
					// Create data structure. Since every wordform should only occur once,
					// this could probably be hoisted, but better safe...
					if (!morphs.containsKey(splitline[0])) {
						morphlist = new ArrayList<MorphEntry>();
						morphs.put(splitline[0], morphlist);
					} else {
						morphlist = morphs.get(splitline[0]);
					}
					for (int ii = 1; ii < splitline.length; ii+=2) {
						morphlist.add(new MorphEntry(splitline[ii], splitline[ii+1]));
					}
				}
			}
		}
	} // ECGMorph()
	
	/**
	 * Given a wordform, a lemma, and a inflection specification, return
	 * true if the wordform matches the lemma and inflection.
	 * 
	 * Example:
	 * 
	 * match("block", "blocked", "PastTense/!Participle") 
	 * true
	 */
	public boolean match(String lemma, String wordform, String spec) {
		if (!morphs.containsKey(wordform)) {
			// TODO: Should this be an error? A warning? Or is just return false okay?
			return false;
		}
		for (MorphEntry morph : morphs.get(wordform)) {
			if (morph.lemma.equals(lemma) && spec_match(spec, morph.inflections)) {
				return true;
			}
		}
		return false;
	} // match()
	
	
	public String[] getInflections(String lemma, String wordform) {
		List<String> inflections = new ArrayList<String>();
		for (MorphEntry morph : morphs.get(wordform)) {
			if (morph.lemma.equals(lemma)) {
				inflections.add(concatenateSet(morph.inflections));
			}
		}
		return inflections.toArray(new String[inflections.size()]);
	}
	
	private String concatenateSet(Set<String> input) {
		String[] array = input.toArray(new String[input.size()]);
		String output = "";
		for (int i = 0; i < array.length;  i++) {
			output += array[i];
			if (i < (array.length - 1)) {
				output += ",";
			}
			
		}
		return output;
	}
	
	
	/**
	 * Looks up a wordform and returns the lemmas.
	 * 
	 * @returns A Set of Strings containing the lemmas
	 */
	
	public Set<String> getLemmas(String wordform) {
		Set<String> lemmaStrs = new HashSet<String>();
		if (morphs.keySet().contains(wordform)) {
			for (MorphEntry morph : morphs.get(wordform)) {
				lemmaStrs.add(morph.lemma);
			}
			return lemmaStrs;
		} else {
			throw new ParserException("Cannot find wordform in lemma base");
		}

	}  // getLemmas()
	
	/**
	 * Return true if the inflection spec string matches the inflections specified
	 * in the Set flects.
	 */
	
	private boolean spec_match(String spec, Set<String> flects) {
		String[] specs = spec.split("\\s*[,/]\\s*");
		for (int ii = 0; ii < specs.length; ii++) {
			if (specs[ii].charAt(0) == '!' && flects.contains(specs[ii].substring(1))) {
				return false;
			} else if (specs[ii].charAt(0) != '!' && !flects.contains(specs[ii])) {
				return false;
			}
		}
		return true;
	} // spec_match()
	
	/**
	 * Return true if the lemma or any of the wordforms actually occur in the grammar.
	 * TODO: Add handling of lemma constructions
	 * @param splitline
	 */
	private boolean entryInGrammar(String[] splitline) {
		// First check if the wordform occurs
		if (grammarWrapper.hasLexicalConstruction(StringUtilities.addQuotes(splitline[0]))) {
			return true;
		}
		if (grammarWrapper.hasLemmaConstruction(StringUtilities.addQuotes(splitline[0]))) {
			return true;
		} 
		if (tokenReader.tokens.keySet().contains(splitline[0])) {
			return true;
		}
		if (tokenReader.hasToken(StringUtilities.addQuotes(splitline[0]))) {
			return true;
		}
		// Now check for all the possible lemmas.
		for (int ii = 1; ii < splitline.length; ii+=2) {
			if (grammarWrapper.hasLexicalConstruction(StringUtilities.addQuotes(splitline[ii]))) {
				return true;
			}
			if (grammarWrapper.hasLemmaConstruction(StringUtilities.addQuotes(splitline[ii]))) {
				return true;
			}
			if (tokenReader.hasToken(StringUtilities.addQuotes(splitline[ii]))) {
				return true;
			}
			if (tokenReader.tokens.keySet().contains(splitline[ii])) {
				return true;
			}
		}
		return false;
	} // entryInGrammar()
	
} // class ECGMorph


package compling.parser.spanishMorph;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.hunspell.HunspellAffix;
import org.apache.lucene.analysis.hunspell.HunspellDictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemmer;
import org.apache.lucene.analysis.hunspell.HunspellStemmer.Stem;

import compling.grammar.unificationgrammar.FeatureStructureUtilities.DefaultStructureFormatter;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.util.PriorityQueue;
import compling.utterance.Sentence;

/**
 * This class implements an Spanish analyzer adapted to use a Spanish 
 * inflectional grammar (see the /data/Spanish directory for a good example).
 * Basically, it lets the user introduce the sentences to be analyzed. The 
 * suffixes of each word are stripped and then it uses the standard ECGAnalyzer 
 * (with the Spanish inflectional grammar).
 * An important point is that the suffix stripper is not very accurate and
 * should be substituted in the future.
 * @author joliva
 *
 */
public class Analyzer {

	private static final String DICTIONARIES_PATH="./data/Spanish/dictionaries";
	private static final String affixFile = "/es_ES.aff";
	private static final String dicFile = "/es_ES.dic";
	private static final String grammarFile = 
		"./data/Spanish/grammar/Inf_SpECG.prefs";
	
	private static HunspellStemmer stemmer;
	
	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		String line;
		StringBuffer result = new StringBuffer();
		Scanner scanner = new Scanner(System.in);
		ECGAnalyzer analyzer = new ECGAnalyzer(grammarFile);

		buildStemmer();
		
		System.out.print("> ");
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if ("exit".equals(line)) {
				break;
			}
			
			/*
			 * Break each word into stem and suffixes using the stemmer
			 */
			for (String word : line.split("\\s")){
				result.append(getStemResults(word, stemmer.stem(
						word.toCharArray(), word.length())));
			}
			
			System.out.println(result);
			StringTokenizer st = new StringTokenizer(result.toString());
			List<String> words = new ArrayList<String>();
			while (st.hasMoreTokens()){
				words.add(st.nextToken());
			}
			PriorityQueue<Analysis> pqa = analyzer.getBestParses(new Sentence(
					words, null, 0));
			while (pqa.size() > 0){
				System.out.println("\n\nRETURNED ANALYSIS\n________________\n");
				System.out.println("Cost: "+pqa.getPriority());
			    Analysis a = pqa.next();
			    DefaultStructureFormatter df = new DefaultStructureFormatter();
			    System.out.println(df.format(a.getFeatureStructure()));
			}
			result = new StringBuffer();
			System.out.print("> ");
	    }
	}
	
	/**
	 * Build the stemmer using the dictionaries under /data/Spanish.
	 * For the moment we used the lucene-hunspell project but the suffix
	 * stripper should be replaced in the future for a more accurate one.
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void buildStemmer() throws IOException, ParseException{
		InputStream affixInputStream = new FileInputStream(DICTIONARIES_PATH + 
				affixFile);
		InputStream dicInputStream = new FileInputStream(DICTIONARIES_PATH + 
				dicFile);

		HunspellDictionary dictionary = new HunspellDictionary(affixInputStream,
				dicInputStream);

		affixInputStream.close();
		dicInputStream.close();

		stemmer = new HunspellStemmer(dictionary);
	}
	
	
	
	/**
	   * Prints the results of the stemming of a word
	   *
	   * @param originalWord Word that has been stemmed
	   * @param stems Stems of the word
	   */
	  private static String getStemResults(String originalWord, 
			  List<Stem> stems) {
	    StringBuilder builder = new StringBuilder();
	    boolean variousSuffixes = false;
	    Stem initialStem; 
	    String stemString;

	    if (stems.size() == 1){
	    	initialStem = stems.get(0);
	    	if (initialStem.getSuffixes().size() == 0){
	    		if ((initialStem.getStemString().endsWith("o")) && 
	    			(!initialStem.getStemString().equals("no")))
	    			builder.append(initialStem.getStemString().
    					substring(0, initialStem.getStemString().length()-1)).
    					append(" o ");
	    		else
	    			builder.append(initialStem.getStemString()+" ");
	    	}
	    	else{
	    		for (HunspellAffix suffix : initialStem.getSuffixes()) {
			        if (hasText(suffix.getStrip())) {
			        	stemString = initialStem.getStemString().substring(0, 
			        			initialStem.getStemString().length() - 
			        			suffix.getStrip().length());
			        	if (stemString.endsWith("o"))
			    			builder.append(stemString.
		    					substring(0, stemString.length()-1)).
		    					append(" o ");
			    		else
			    			builder.append(stemString+" ");
			        }
			        else if (!variousSuffixes){
			        	if ((initialStem.getStemString().endsWith("o")) && 
			    			(!initialStem.getStemString().equals("no")))
			    			builder.append(initialStem.getStemString().
		    					substring(0, initialStem.getStemString().
		    							length()-1)).append(" o ");
			    		else
			    			builder.append(initialStem.getStemString()+" ");
			        }
			        builder.append(suffix.getAppend()).append(" ");
			        variousSuffixes = true;
			      }
	    	}
	    }
	    
	    else{
		    for (Stem stem : stems) {
		    	variousSuffixes = false;
		    	if (stem.getSuffixes().size() == 0){
		    		continue;
		    	}
		    	else{
	
		      // Do not care about prefixes for the moment
		      /*for (HunspellAffix prefix : stem.getPrefixes()) {
		        builder.append(prefix.getAppend()).append("+");
	
		        if (hasText(prefix.getStrip())) {
		          builder.append(prefix.getStrip()).append("-");
		        }
		      }*/
	
			      for (HunspellAffix suffix : stem.getSuffixes()) {
			        if (hasText(suffix.getStrip())) {
			        	stemString = stem.getStemString().substring(0, 
			        			stem.getStemString().length() - 
			        			suffix.getStrip().length());
			        	if (stemString.endsWith("o"))
			    			builder.append(stemString.
		    					substring(0, stemString.length()-1)).
		    					append(" o ");
			    		else
			    			builder.append(stemString+" ");
			        }
			        else if (!variousSuffixes){
			        	if ((stem.getStemString().endsWith("o")) && 
			    			(!stem.getStemString().equals("no")))
			    			builder.append(stem.getStemString().
		    					substring(0, stem.getStemString().length()-1)).
		    					append(" o ");
			    		else
			    			builder.append(stem.getStemString()+" ");
			        }
			        builder.append(suffix.getAppend()).append(" ");
			        variousSuffixes = true;
			      }
		    	}
		    }
	    }
	    return builder.toString();
	  }

	  /**
	   * Simple utility to check if the given String has any text
	   *
	   * @param str String to check if it has any text
	   * @return {@code true} if the String has text, {@code false} otherwise
	   */
	  private static boolean hasText(String str) {
	    return str != null && str.length() > 0;
	  }

}

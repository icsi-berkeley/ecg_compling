package compling.parser.ecgparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import compling.grammar.ecg.GrammarWrapper;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.util.fileutil.TextFileLineIterator;


/**
 * This class reads in a text file containing morphological specifications.
 * Specifically, the text file represents a matching between certain "FlectTypes" (Celex output)
 * and the ECG constraints.
 * 
 * Example:
 * "Plural :: self.m.number <-- @plural, self.m.bounding <-- @indeterminate".
 *                       
 * The text file must be formatted with the "constructional block" of constraints at the top,
 * and the "meaning block" of constraints following it - with each section clearly marked "constructional"
 * or "meaning". 
 * 
 * Once the class reads in the constraints, it builds a HashMap between each FlectType and the constraints, which
 * is referenced by the Parser during runtime. 
 * 
 * @author Sean Trott
 *
 */
public class ECGMorphTableReader {
	GrammarWrapper grammarWrapper;
	AnalyzerPrefs prefs;
	File table_path;
	public HashMap<String, ArrayList<String[]>> constructional_morphTable;
	public HashMap<String, ArrayList<String[]>> meaning_morphTable;
	
	public ECGMorphTableReader(GrammarWrapper wrapper) throws IOException {
		
		constructional_morphTable = new HashMap<String, ArrayList<String[]>>();
		meaning_morphTable = new HashMap<String, ArrayList<String[]>>();
		
		grammarWrapper = wrapper;
		prefs = (AnalyzerPrefs) grammarWrapper.getGrammar().getPrefs();
		//BufferedReader in = new BufferedReader(new FileReader("compling/first/first.morph"));
		File base = prefs.getBaseDirectory();	
		table_path = new File(base, prefs.getSetting(AP.TABLE_PATH));
		TextFileLineIterator tfli = new TextFileLineIterator(table_path);
		
		int lineNum = 1;
		// This loop fills in constructional_morphTable
		while (tfli.hasNext()) {
			String l = tfli.next();
			if (l.equals("Constructional")) {
				continue;
			}
			if (l.startsWith("#")) {
				continue;
			}
			if (l.equals("Meaning")) {
				break;
			}
			String[] s = l.split("\\s*::\\s*");
			if (s.length < 3) {
				throw new ParserException("Improperly formatted morph entry on line " + lineNum 
						+ " of table in file " + table_path + 
						". Entry must be of the form: \n 'FlectType :: constraints :: compatible_constructions'.");
			}
			String key = s[0];
			String[] constraints = s[1].split(",\\s");
			String[] parents = s[2].split(",\\s");
			ArrayList<String[]> values = new ArrayList<String[]>();
			values.add(constraints);
			values.add(parents);
			constructional_morphTable.put(key, values);
			lineNum += 1;
		}
		
		// This loop fills in meaning_morphTable.
		while (tfli.hasNext()) {
			String l = tfli.next();
			if (l.startsWith("#")) {
				continue;
			}
			String[] array = l.split("\\s*::\\s*");
			if (array.length < 3) {
				throw new ParserException("Improperly formatted morph entry on line " + lineNum 
						+ " of table in file " + table_path + 
						". Entry must be of the form: \n 'FlectType :: constraints :: compatible_constructions'.");
			}
			String key = array[0];
			String[] constraints = array[1].split(",\\s");
			String[] parents = array[2].split(",\\s");
			ArrayList<String[]> values = new ArrayList<String[]>();
			values.add(constraints);
			values.add(parents);
			meaning_morphTable.put(key, values);
			lineNum += 1;
		}

	}
	
	public HashMap<String, ArrayList<String[]>> getConstructionalTable() {
		return constructional_morphTable;
	}
	
	public HashMap<String, ArrayList<String[]>> getMeaningTable() {
		return meaning_morphTable;
	}


}

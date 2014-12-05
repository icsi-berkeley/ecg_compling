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
import compling.util.fileutil.TextFileLineIterator;

public class ECGMorphTableReader {
	GrammarWrapper grammarWrapper;
	AnalyzerPrefs prefs;
	File table_path;
	public HashMap<String, String[]> constructional_morphTable;
	public HashMap<String, String[]> meaning_morphTable;
	
	public ECGMorphTableReader(GrammarWrapper wrapper) throws IOException {
		
		constructional_morphTable = new HashMap<String, String[]>();
		meaning_morphTable = new HashMap<String, String[]>();
		
		grammarWrapper = wrapper;
		prefs = (AnalyzerPrefs) grammarWrapper.getGrammar().getPrefs();
		//BufferedReader in = new BufferedReader(new FileReader("compling/first/first.morph"));
		File base = prefs.getBaseDirectory();	
		table_path = new File(base, prefs.getSetting(AP.TABLE_PATH));
		TextFileLineIterator tfli = new TextFileLineIterator(table_path);
		
		// This loop fills in constructional_morphTable
		while (tfli.hasNext()) {
			String l = tfli.next();
			if (l.equals("Constructional")) {
				continue;
			}
			if (l.equals("Meaning")) {
				break;
			}
			String[] s = l.split("\\s*::\\s*");
			String key = s[0];
			String[] value = s[1].split(",\\s");
			constructional_morphTable.put(key, value);
		}
		
		// This loop fills in meaning_morphTable.
		while (tfli.hasNext()) {
			String l = tfli.next();
			String[] array = l.split("\\s*::\\s*");
			String key = array[0];
			String[] value = array[1].split(",\\s");
			meaning_morphTable.put(key, value);
		}

	}
	
	public HashMap<String, String[]> getConstructionalTable() {
		return constructional_morphTable;
	}
	
	public HashMap<String, String[]> getMeaningTable() {
		return meaning_morphTable;
	}


}

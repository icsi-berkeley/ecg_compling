package compling.parser.ecgparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import compling.grammar.ecg.GrammarWrapper;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.util.fileutil.TextFileLineIterator;

public class MappingReader {
	
	AnalyzerPrefs prefs;
	File mappingPath;
	public HashMap<String, String> mappings;
	TextFileLineIterator tfli;
	
	public MappingReader(GrammarWrapper wrapper) throws IOException {
		mappings = new HashMap<String, String>();
		prefs = (AnalyzerPrefs) wrapper.getGrammar().getPrefs();
		File base = prefs.getBaseDirectory();
		if (!(prefs.getSetting(AP.MAPPING_PATH) == null)) {
			mappingPath = new File(base, prefs.getSetting(AP.MAPPING_PATH));
			tfli = new TextFileLineIterator(mappingPath);
			readMappings();
		}
	}

	private void readMappings() {
		int lineNum = 1;
		while (tfli.hasNext()) {
			String l = tfli.next();
			String[] contents = l.split(" :: ");
			try {
				String lingValue = contents[0];
				String appValue = contents[1];
				if (lingValue.contains("@")) {
					lingValue = lingValue.replace("@", "");
				}
				if (appValue.contains("$")) {
					appValue = appValue.replace("$", "");
				}
				mappings.put(lingValue, appValue);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ParserException("Mapping file " + mappingPath.getAbsolutePath() + " has an error on line " + lineNum + ".");
			}
			lineNum += 1;
		}
		
	}
	
	public HashMap<String, String> getMappings() {
		return mappings;
	}

}

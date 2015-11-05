// =============================================================================
//File        : LearnerPrefs.java
//Author      : emok
//Change Log  : Created on Dec 10, 2007
//=============================================================================

package compling.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Prefs;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.util.fileutil.TextFileLineIterator;

//=============================================================================

public class LearnerPrefs extends AnalyzerPrefs implements Prefs {

	private static Logger logger = Logger.getLogger(AnalyzerPrefs.class.getName());
	private Map<ComplingPackage, String> loggingLevels = new HashMap<ComplingPackage, String>();

	public static enum Datasource {
		TRANSCRIPT, TEXT, XML;
	}

	public static enum LP implements Property {
		USE_GUI(Datatype.BOOL),

		LOGGING(Datatype.STRING),

		ANALYZE(Datatype.BOOL), SIMULATE(Datatype.BOOL), LEARN(Datatype.BOOL),

		DATA_SOURCE(Datatype.STRING), DATA_PATHS(Datatype.LISTSTRING), DATA_EXTENSIONS(Datatype.STRING),

		GRAMMAR_METADATA_EXTENSIONS(Datatype.STRING),

		SCRIPT_PATHS(Datatype.LISTSTRING), SCRIPT_EXTENSIONS(Datatype.STRING),

		OUTPUT_XML(Datatype.BOOL), OUTPUT_XML_PATH(Datatype.STRING),

		OUTPUT_SNAPSHOTS(Datatype.BOOL), OUTPUT_SNAPSHOTS_PATH(Datatype.STRING),

		OUTPUT_GRAMMAR_PATH(Datatype.STRING), OUTPUT_GRAMMAR_PARAMS_PATH(Datatype.STRING),

		LEARNER_SETTINGS(Datatype.STRING),

		AGRESSIVE_CONTEXT_FITTING(Datatype.BOOL), ANALYSIS_RERANK_METHOD(Datatype.INTEGER), USE_GOLD_STANDARD(
				Datatype.BOOL), BATCH_UPDATE(Datatype.BOOL), USE_MDL(Datatype.BOOL), ITERATIONS(Datatype.INTEGER),

		SKIP(Datatype.BOOL);

		private Datatype type = null;

		private LP(Datatype type) {
			this.type = type;
		}

		public Datatype getDataType() {
			return type;
		}
	}

	public LearnerPrefs(String prefsFilesPath) throws IOException {
		super();
		listsTable.put(LP.DATA_PATHS, new ArrayList<String>());
		listsTable.put(LP.SCRIPT_PATHS, new ArrayList<String>());
		processFile(prefsFilesPath);
		if (settingsTable.get(AP.BASE) != null) {
			base = new File(settingsTable.get(AP.BASE));
		}
		else {
			base = new File(prefsFilesPath).getParentFile();
		}
		if (!hasSufficientParameters()) {
			throw new GUIException("Insufficient parameters are supplied");
		}
	}

	protected void processFile(String prefsFilesPath) throws IOException {
		super.processFile(prefsFilesPath, false, null);
		TextFileLineIterator tfli = new TextFileLineIterator(prefsFilesPath);

		int lineNum = 0;
		LP currentBlock = null;

		while (tfli.hasNext()) {
			lineNum++;
			String line = tfli.next();
			if (line.indexOf("::==") >= 0) {
				if (currentBlock != null) {
					throw new GUIException("Block " + line.split("::==")[0] + " is beginning in the middle of the block "
							+ currentBlock);
				}
				try {
					currentBlock = LP.valueOf(line.split("::==")[0].trim());
				}
				catch (IllegalArgumentException iae) {
					currentBlock = LP.SKIP;
				}
			}
			else if (line.indexOf(";") >= 0) {
				if (currentBlock == null) {
					throw new GUIException("Unexpected semi-colon encountered in preference file around line " + lineNum);
				}
				currentBlock = null;
			}
			else {
				line = line.replaceAll("\"", "");
				if (line.indexOf("=") >= 0) {
					String[] pair = line.split("=");
					if (currentBlock == LP.LOGGING) {
						String[] loggingPackage = pair[0].trim().split("\\.");
						ComplingPackage packageID = null;
						try {
							if (loggingPackage.length == 1) {
								packageID = ComplingPackage.valueOf(loggingPackage[0]);
							}
							else if (loggingPackage.length == 2) {
								packageID = ComplingPackage.valueOf(loggingPackage[1]);
							}
							else {
								throw new GUIException("Unexpected format for package logging levels around line " + lineNum);
							}
						}
						catch (IllegalArgumentException iae) {
							throw new GUIException("Unknown package name supplied around line " + lineNum);
						}
						loggingLevels.put(packageID, pair[1].trim().toUpperCase());
					}
					else {
						LP setting = null;
						try {
							setting = LP.valueOf(pair[0].trim());
						}
						catch (IllegalArgumentException iae) {
						}
						if (setting != null) {
							settingsTable.put(setting, pair[1].trim());
						}
					}
				}
				else {
					if (currentBlock == LP.SCRIPT_PATHS || currentBlock == LP.DATA_PATHS) {
						listsTable.get(currentBlock).add(line);
					}
				}
			}
		}

	}

	public boolean hasSufficientParameters() {
		boolean sufficient = true;

		Datasource source = null;
		try {
			source = settingsTable.get(LP.DATA_SOURCE) != null ? Datasource.valueOf(settingsTable.get(LP.DATA_SOURCE))
					: null;
		}
		catch (IllegalArgumentException iae) {
			logger.warning(settingsTable.get(LP.DATA_SOURCE) + " is an invalid data source");
		}

		// if analyzer is used
		if (Boolean.valueOf(settingsTable.get(LP.ANALYZE))) {
			sufficient &= settingsTable.get(LP.USE_GUI) != null;
			sufficient &= !listsTable.get(AP.GRAMMAR_PATHS).isEmpty();
			sufficient &= !listsTable.get(LP.DATA_PATHS).isEmpty();
			sufficient &= (source == Datasource.TEXT || source == Datasource.TRANSCRIPT);
		}

		// if simulator is used
		if (Boolean.valueOf(settingsTable.get(LP.SIMULATE))) {
			sufficient &= !listsTable.get(AP.ONTOLOGY_PATHS).isEmpty();
			sufficient &= !listsTable.get(LP.SCRIPT_PATHS).isEmpty();
			sufficient &= source == Datasource.TRANSCRIPT;
		}

		// if learner is used
		if (Boolean.valueOf(settingsTable.get(LP.LEARN))) {
			sufficient &= !listsTable.get(AP.GRAMMAR_PATHS).isEmpty();
			sufficient &= !listsTable.get(LP.DATA_PATHS).isEmpty();
			sufficient &= (source == Datasource.XML || Boolean.valueOf(settingsTable.get(LP.ANALYZE)));
			sufficient &= Boolean.valueOf(settingsTable.get(AP.ANALYZE_IN_CONTEXT));
		}

		return sufficient;
	}

	public Map<ComplingPackage, String> getLoggingLevels() {
		return loggingLevels;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("Learner Prefs:\n");
		for (Property setting : listsTable.keySet()) {
			sb.append(setting).append(" = ").append(listsTable.get(setting)).append("\n");
		}

		for (Property setting : settingsTable.keySet()) {
			sb.append(setting).append(" = ").append(settingsTable.get(setting)).append("\n");
		}
		return sb.toString();
	}

	public static void main(String[] args) throws IOException, TypeSystemException {

		if (args.length != 1) {
			throw new GUIException("Must be called with the path of the prefs file");
		}
		LearnerPrefs preferences = new LearnerPrefs(args[0]);
		System.out.println(preferences);
		Grammar grammar = ECGGrammarUtilities.read(preferences);
		System.out.println(grammar);

	}
}

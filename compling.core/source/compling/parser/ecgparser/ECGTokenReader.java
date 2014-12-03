package compling.parser.ecgparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.util.fileutil.TextFileLineIterator;

public class ECGTokenReader {

	public class ECGToken {
		String token_name;  // E.g. red, walk
		Construction parent;
		List<Constraint> constraints;
	}

	GrammarWrapper grammarWrapper;
	AnalyzerPrefs prefs;
	File token_path;
	
	Map<String, ArrayList<ECGToken>> tokens;

	public ECGTokenReader(GrammarWrapper wrapper) throws IOException {
		grammarWrapper = wrapper;

		prefs = (AnalyzerPrefs) grammarWrapper.getGrammar().getPrefs();
		File base = prefs.getBaseDirectory();
		token_path = new File(base, prefs.getSetting(AP.TOKEN_PATH));
		tokens = new HashMap<String, ArrayList<ECGToken>>();		
		
		TextFileLineIterator tfli = new TextFileLineIterator(token_path);
		int lineNum = 0;
		while (tfli.hasNext()) {
			lineNum++;
			String line = tfli.next();
			// Skip blank lines or lines with just a comment
			if (line.matches("^\\s*#") || line.matches("^\\s*$")) {
				continue;
			}
			String splitline[] = line.split("\\s*\\t\\s*");
			if (splitline.length < 3) {
				// TODO: Create a TokenException class and throw that instead
				throw new IOException("Improperly formatted entry in token file " + token_path + ", line " + lineNum);
			}
			String token_name = splitline[0];
			String parent_name = splitline[1];
			// TODO: Make sure parent exists, is non-general, and has orth="*".
			ECGToken token = new ECGToken();
			token.token_name = token_name;
			token.parent = grammarWrapper.getGrammar().getConstruction(parent_name);
			token.constraints = new ArrayList<Constraint>();
			for (int ii = 2; ii < splitline.length; ii++) {
				String constraint_str = splitline[ii];
				String split_constraint[] = constraint_str.split("\\s*<--\\s*");
				if (split_constraint.length != 2) {
					// TODO: Create a TokenException class and throw that instead
					throw new IOException("Improperly formatted constraint in token file " + token_path + ", line " + lineNum + ", constraint " + constraint_str);
				}
				String slotchain_str = split_constraint[0];
				String value_str = split_constraint[1];
				// TODO: Make sure constraint is consistent with parent
				token.constraints.add(new Constraint("<--", new SlotChain(slotchain_str), value_str));
			}
			// TODO: Make sure there's an appropriate ontology constraint

			// Add token to token list associated with the name
			if (!tokens.containsKey(token_name)) {
				tokens.put(token_name, new ArrayList<ECGToken>());
			}
			tokens.get(token_name).add(token);
		}
	} // ECGTokenReader()
	
	public List<ECGToken> getToken(String token) {
		return tokens.get(token);
	} // getToken()
	
	public boolean hasToken(String token) {
		return tokens.containsKey(token);
	} // hasToken()
	
} // class ECGTokenReader


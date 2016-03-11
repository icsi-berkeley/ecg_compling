package compling.parser.ecgparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.util.fileutil.TextFileLineIterator;

/**
 * Reads in the Token file and builds "ECGToken" instances, which contains a parent Cxn and additional constraints.
 * It then constructs a HashMap from each token string to a list of possible ECGToken instances.
 *
 */
public class ECGTokenReader {

	public class ECGToken {
		public String token_name;  // E.g. red, walk
		public Construction parent;
		public List<Constraint> constraints;
		public Location location;
		
		public void setLocation(Location l) {
			location = l;
		}
		
		public Location getLocation() {
			return location;
		}
		
		public String toString() {
			return token_name + " :: " + parent.getName() + " :: " + constraints.toString();
		}
	}
	
	Grammar grammar; 
	//GrammarWrapper grammarWrapper;
	AnalyzerPrefs prefs;
	File token_path;
	
	Map<String, ArrayList<ECGToken>> tokens;
	
	public Map<String, ArrayList<ECGToken>> getTokens() {
		return tokens;
	}

	public ECGTokenReader(Grammar inputGrammar) {
		grammar = inputGrammar;
		prefs = (AnalyzerPrefs) grammar.getPrefs();
		File base = prefs.getBaseDirectory();
		//token_path = new File(base, prefs.getSetting(AP.TOKEN_PATHS));
		List<String> token_paths = prefs.getList(AP.TOKEN_PATH);
		tokens = new HashMap<String, ArrayList<ECGToken>>();	
		for (String path : token_paths) {
			token_path = new File(base, path);
	
			
			TextFileLineIterator tfli = new TextFileLineIterator(token_path);
			int lineNum = 0;
			while (tfli.hasNext()) {
				lineNum++;
				String line = tfli.next();
				// Skip blank lines or lines with just a comment
				if (line.matches("\\s*#.*") || line.matches("\\s*")) {
					continue;
				}
				String splitline[] = line.split("\\s*::\\s*");
				if (splitline.length < 3) {
					// TODO: Create a TokenException class and throw that instead
					throw new ParserException("Improperly formatted entry in token file " + token_path + ", line " + lineNum);
				}
				String token_name = splitline[0].trim();
				String parent_name = splitline[1].trim();
				ECGToken token = new ECGToken();
				token.setLocation(new Location("test", path, 0, 0));
				
				token.token_name = token_name;
				token.parent = grammar.getConstruction(parent_name);
				
				if (token.parent == null) {
					// TODO: Create a TokenException class and throw that instead
					throw new ParserException("Parent construction \"" + parent_name + "\" is not defined (token \"" + token_name + "\" in token file \"" + token_path + "\" line " + lineNum + ")");
				}
				// TODO: Other checks of parent (non-general, has orth="*", etc)		
				token.constraints = new ArrayList<Constraint>();
				for (int ii = 2; ii < splitline.length; ii++) {
					String constraint_str = splitline[ii].trim();
					String split_constraint[] = constraint_str.split("\\s*<--\\s*");
					if (split_constraint.length != 2) {
						// TODO: Create a TokenException class and throw that instead
						throw new ParserException("Improperly formatted constraint in token file " + token_path + ", line " + lineNum + ", constraint " + constraint_str);
					}
					String slotchain_str = split_constraint[0];
					String value_str = split_constraint[1];
					// TODO: Make sure constraint is consistent with parent
					
					
					// This iterates through parent's constraints; if it doesn't find the Slotchain, no Exception is thrown.
					if (!slotMatch(token.parent, slotchain_str, value_str)) {
						throw new ParserException("Error with token " + token.token_name + " on line " + lineNum + ". Either slot chain " + slotchain_str + " not found in parent " + token.parent.getName() 
								+ " or value " + value_str + " not a proper subcase.");
					}
	
					token.constraints.add(new Constraint("<--", new SlotChain(slotchain_str), value_str));
				}
				// TODO: Make sure there's an appropriate ontology constraint that's consistent with parent
	
				// Add token to token list associated with the name
				if (!tokens.containsKey(token_name)) {
					tokens.put(token_name, new ArrayList<ECGToken>());
				}
				boolean add = true;
				boolean parentFound = false;
				// TODO: only parentFound=True if they're ALL the same...
				for (ECGToken t : tokens.get(token_name)) {
					if (t.parent.equals(token.parent)) {
						parentFound = true;
						//t.constraints.
						Collections.sort(t.constraints);
						Collections.sort(token.constraints);
						if (t.constraints.equals(token.constraints)) {
							add = false;
						}
					}
				}
				if (parentFound) {
					//TODO: Issue warning if they're the same, but with different constraints
					if (!add) {
						throw new ParserException("Two tokens with lemma " + token_name + " and type " + parent_name + ", as well as shared constraints.");
					}
					//throw new GrammarException("Two tokens with lemma " + token_name + " and type " + parent_name + ". This is allowed, but unusual.");
				}
				if (add) { 
					tokens.get(token_name).add(token); 
				}
			}
		}
	} // ECGTokenReader()
	
	
	// Currently assumes VALUE is either an Ontology item ("@walk") or a FillerString (""8""). 
	public boolean slotMatch(Construction parentCxn, String slotchain, String value) {
		boolean found = false;
		for (Constraint constraint : parentCxn.getMeaningBlock().getConstraints()) {
			if (constraint.getArguments().get(0).toString().equals(slotchain)) {
				if (constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX &&
						value.charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
					try {
						TypeSystem ts = grammar.getOntologyTypeSystem();
						String child = value.substring(1, value.length()).trim();
						String ancestor = constraint.getValue().substring(1, constraint.getValue().length()).trim();
						found = ts.subtype(ts.getInternedString(child), ts.getInternedString(ancestor));
					} catch (TypeSystemException tse) {
						System.out.println(tse.getMessage());
						return false;
					}
				} else {
					found = true;
				}
			}
		}
		for (Constraint constraint : parentCxn.getConstructionalBlock().getConstraints()) {
			if (constraint.getArguments().get(0).toString().equals(slotchain)) {
				if (constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX &&
						value.charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
					try {
						TypeSystem ts = grammar.getOntologyTypeSystem();
						String child = value.substring(1, value.length()).trim();
						String ancestor = constraint.getValue().substring(1, constraint.getValue().length()).trim();
						found = ts.subtype(ts.getInternedString(child), ts.getInternedString(ancestor));
					} catch (TypeSystemException tse) {
						System.out.println(tse.getMessage());
						return false;
					}
				} else {
					found = true;
				}
			}
		}
		return found;
	}
	
	public List<ECGToken> getToken(String token) {
		if (tokens.keySet().contains(token)) {
			return tokens.get(token);
		} else {
			throw new GrammarException("Token not in database.");
		}
	} // getToken()
	
	public boolean hasToken(String token) {
		return tokens.containsKey(token);
	} // hasToken()
	
} // class ECGTokenReader


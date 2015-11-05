package compling.gui.grammargui.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.gui.grammargui.model.PrefsManager;
import compling.parser.ecgparser.ECGTokenReader;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;

public class LexiconUtils {
	
	private ArrayList<String> partsOfSpeech;
	private HashMap<String, ArrayList<String>> posToTypes;
	private Map<String, ArrayList<ECGToken>> tokens;
	
	private HashMap<String, List<Constraint>> tokensToConstraints;
	
	private HashMap<String, ArrayList<String>> typesToTokens;
	
	private TypeSystem ts;
	private static LCPGrammarWrapper wrapper;
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}
	
	public static ArrayList<String> initializePartsOfSpeech() {
		ArrayList<String> partsOfSpeech = new ArrayList<String>(){{
			add("Adjective");
			add("Noun");
			add("Verb");
			add("Preposition");
			add("Determiner");
			add("Adverb");
			add("NP");
		}};
		return partsOfSpeech;
	}
	
	public static Map<String, ArrayList<ECGToken>> initializeTokens() throws IOException {
		//LCPGrammarWrapper wrapper = new LCPGrammarWrapper(getGrammar());
		ECGTokenReader reader =  new ECGTokenReader(wrapper.getGrammar());
		Map<String, ArrayList<ECGToken>> tokens = reader.getTokens();
		return tokens;
	}
	
	public HashMap<String, ArrayList<String>> typesToTokens() {
		tokensToConstraints = new HashMap<String, List<Constraint>>();
		typesToTokens = new HashMap<String, ArrayList<String>>();
		for (ArrayList<ECGToken> tokenList : tokens.values()) {
			for (ECGToken token : tokenList) {
				
				tokensToConstraints.put(token.token_name, token.constraints);
				String typeName = token.parent.getName();
				if (!typesToTokens.containsKey(typeName)) {
					typesToTokens.put(typeName, new ArrayList<String>());
				}
				typesToTokens.get(typeName).add(token.token_name);
				Collections.sort(typesToTokens.get(typeName));
			}
		}
		return typesToTokens;
	}
	
	public HashMap<String, List<Constraint>> getTokensToConstraints() {
		return tokensToConstraints;
	}
	
	public HashMap<String, ArrayList<String>> initializePosToTypes() {
		posToTypes = new HashMap<String, ArrayList<String>>();
		posToTypes.put("Other", new ArrayList<String>());
		ts = getGrammar().getCxnTypeSystem();
		//HashMap<String, ArrayList<String>> typesToLexemes = new HashMap<String, ArrayList<String>>();
		for (String type : typesToTokens.keySet()) {
			boolean placed = false;
			for (String pos : partsOfSpeech) {
				try {
					if (ts.subtype(ts.getInternedString(type), ts.getInternedString(pos))) {
						if (!posToTypes.containsKey(pos)) {
							posToTypes.put(pos, new ArrayList<String>());
						}
						posToTypes.get(pos).add(type);
						placed = true;
						Collections.sort(posToTypes.get(pos));
					}
				} catch (TypeSystemException e) {
					// TODO DO something, error
					e.printStackTrace();
				}
			} if (!placed) {
				posToTypes.get("Other").add(type);
				placed = true;
				Collections.sort(posToTypes.get("Other"));
			}
		}
		for (Construction c : wrapper.getAllConcreteLexicalConstructions()) {
			String type = c.getName();
				try {
					if (!ts.subtype(ts.getInternedString(type), ts.getInternedString("GeneralTypeCxn"))) {
						boolean placed = false;
						for (String pos : partsOfSpeech) {
							try {
								if (ts.subtype(ts.getInternedString(type), ts.getInternedString(pos))) {
									if (!posToTypes.containsKey(pos)) {
										posToTypes.put(pos, new ArrayList<String>());
									}
									posToTypes.get(pos).add(type);
									placed = true;
									Collections.sort(posToTypes.get(pos));
								}
							} catch (TypeSystemException e) {
								// TODO DO something, error
								e.printStackTrace();
							}
						} if (!placed) {
							posToTypes.get("Other").add(type);
							placed = true;
							Collections.sort(posToTypes.get("Other"));
						}
					}
				} catch (TypeSystemException e) {
					// TODO Bad thing
					e.printStackTrace();
				}
		}
		return posToTypes;
	}

}

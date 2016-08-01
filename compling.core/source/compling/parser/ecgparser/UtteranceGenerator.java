package compling.parser.ecgparser;
import java.util.ArrayList;
import java.util.Map.Entry;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ecgparser.ECGTokenReader.ECGToken;

public class UtteranceGenerator {
	
	public static GrammarWrapper grammarWrapper = null;
	public static TypeSystem ts = null;
	public static ECGTokenReader tk = null;// new ECGTokenReader
	
	public UtteranceGenerator(GrammarWrapper g) {
		grammarWrapper = g;
		ts = grammarWrapper.getGrammar().getConstructionTypeSystem();
		tk = new ECGTokenReader(grammarWrapper.getGrammar());
	}
	
	/* Enumerates all possible utterances. Currently have to be of type "Utterance",
	 *  but could theoretically be extended to all "RootType" cxns, which would include NPs and PPs.
	 *  Ultimately idea is to generate all possible cxns with all possible fillers.
	 */
	public ArrayList<String> generateUtterances(String type) {
		ArrayList<String> utterances = new ArrayList<String>();
		ArrayList<String> observed = new ArrayList<String>();
		//g.
		for (Construction cxn : grammarWrapper.getAllConcretePhrasalConstructions()) {
			
			try {
				if (ts.subtype(ts.getInternedString(cxn.getName()), ts.getInternedString(type))) {
					observed.add(cxn.getName());
					//System.out.println(cxn.getName());
					ArrayList<ArrayList<String>> fillers = new ArrayList<ArrayList<String>>();
					for (Role r: cxn.getComplements()) {
						fillers.add(fillConstituent(grammarWrapper.getGrammar().getConstruction(r.getTypeConstraint().getType()), observed));
						//r.
						System.out.println(fillers);
					}
					utterances.add(fillers.toString());
				}
			} catch (TypeSystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println(utterances);
		return utterances;
	}
	
	
	public static ArrayList<String> fillWord(Construction cxn) throws TypeSystemException {
		ArrayList<String> words = new ArrayList<String>();
		for (Construction word : grammarWrapper.getAllConcreteLexicalConstructions()) {
			if (ts.subtype(ts.getInternedString(word.getName()), ts.getInternedString(cxn.getName()))) {
				if (ts.subtype(ts.getInternedString(word.getName()), ts.getInternedString("GeneralTypeCxn"))) {
					for (Entry<String, ArrayList<ECGToken>> entry : tk.getTokens().entrySet()) {
						for (ECGToken tok : entry.getValue()) {
							if (tok.parent.equals(word)) {
								words.add(tok.token_name);
							}
						}
					}
				} else {
					words.add(ECGGrammarUtilities.getLexemeFromLexicalConstruction(word));
				}
				
			}
		}
		return words;
	}
	
	
	public static ArrayList<String> fillConstituent(Construction cxn, ArrayList<String> observed) throws TypeSystemException {
		ArrayList<String> filler = new ArrayList<String>();
		if (ts.subtype(ts.getInternedString(cxn.getName()), ts.getInternedString("Word"))) {
			ArrayList<String> words = fillWord(cxn);
			System.out.println("-------------");
			System.out.println(cxn.getName());
			System.out.println(words);
			System.out.println("------------");
			return filler;
			
		} else if (cxn.isConcrete()) {// && !observed.contains(cxn.getName())) {
			//observed.add(cxn.getName());
			for (Role r: cxn.getComplements()) {
				 fillConstituent(grammarWrapper.getGrammar().getConstruction(r.getTypeConstraint().getType()), observed);
			}
		} else {
			//cxn.
			for (Construction child : grammarWrapper.getAllConcretePhrasalConstructions()) {
					if (ts.subtype(ts.getInternedString(child.getName()), ts.getInternedString(cxn.getName()))
							&& !observed.contains(child.getName())) {
						observed.add(child.getName());
						fillConstituent(child, observed);
					}
			}
			for (Construction child : grammarWrapper.getAllConcreteLexicalConstructions()) {
				if (ts.subtype(ts.getInternedString(child.getName()), ts.getInternedString(cxn.getName()))
						&& !observed.contains(child.getName())) {
					observed.add(child.getName());
					fillConstituent(child, observed);
				}
			}
		}
		return filler;
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

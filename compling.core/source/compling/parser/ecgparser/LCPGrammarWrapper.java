package compling.parser.ecgparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.parser.ParserException;

public class LCPGrammarWrapper implements GrammarWrapper {

	Grammar grammar;
	private HashMap<String, List<String>> subtypeList = new HashMap<String, List<String>>();
	private HashMap<String, List<Construction>> lexemeToLexicalConstructions = new HashMap<String, List<Construction>>();

	// for lemmas and constructions
	private HashMap<String, List<Construction>> lemmaToLexicalConstructions = new HashMap<String, List<Construction>>

	Construction morph;
	Construction word;

	public LCPGrammarWrapper(Grammar ecgGrammar) {
		grammar = ecgGrammar;
		morph = grammar.getConstruction(ECGConstants.MCXNTYPE);
		word = grammar.getConstruction(ECGConstants.MWORDTYPE);

		for (Construction parent : grammar.getAllConstructions()) {
			subtypeList.put(parent.getName(), new ArrayList<String>());
			for (Construction child : grammar.getAllConstructions()) {
				try {
					if (grammar.getCxnTypeSystem().subtype(child.getName(), parent.getName())
							&& child.getKind() == ECGConstants.CONCRETE) {
						subtypeList.get(parent.getName()).add(child.getName());
					}
				}
				catch (Exception e) {
					throw new GrammarException(e.toString());
				}
			}
			if (isLexicalConstruction(parent)) {
				if (lexemeToLexicalConstructions.get(ECGGrammarUtilities.getLexemeFromLexicalConstruction(parent)) == null) {
					lexemeToLexicalConstructions.put(ECGGrammarUtilities.getLexemeFromLexicalConstruction(parent),
							new ArrayList<Construction>());
				} 
				// @ author seantrott
				// instantiate new entry for lemma if it's not already in hashmap
				if (lemmaToLexicalConstructions.get(ECGGrammarUtilities.getLemmaFromLexicalConstruction(parent)) == null) {     // so far just putting in lexeme hashmap
					lemmaToLexicalConstructions.put(ECGGrammarUtilities.getLemmaFromLexicalConstruction(parent), new ArrayList<Construction>());
				}

				// add parent to lemma hashmap
				lemmaToLexicalConstructions.get(ECGGrammarUtilities.getLemmaFromLexicalConstruction(parent)).add(parent);

				lexemeToLexicalConstructions.get(ECGGrammarUtilities.getLexemeFromLexicalConstruction(parent)).add(parent);
			} // could be code checking if it's a LemmaConstruction?, then put in Lemma hashmap (if necessary) (@seantrott, 11/12/14)
			  // alternatively, lemma constructions could just also be lexical constructions
			// elif (isLemmaConstruction(parent))
		}
	}

	/** Returns the concrete rules that are subtypes of construction */
	public List<Construction> getRules(String construction) {
		List<Construction> cxns = new ArrayList<Construction>();
		// System.out.println(symbol+" "+getConcreteSubtypes(symbol));
		for (String type : getConcreteSubtypes(construction)) {
			cxns.add(getConstruction(type));
		}
		return cxns;
	}

	public List<String> getConcreteSubtypes(String c) {
		return subtypeList.get(c);
	}

	public boolean isSubcaseOfMorph(Construction c) {
		if (morph == null) {
			return false;
		}
		try {
			return getCxnTypeSystem().subtype(c.getName(), morph.getName());
		}
		catch (Exception e) {
			throw new RuntimeException("What the heck?\n" + e.toString());
		}
	}

	public boolean isSubcaseOfWord(Construction c) {
		if (word == null) {
			return false;
		}
		try {
			return getCxnTypeSystem().subtype(c.getName(), word.getName());
		}
		catch (Exception e) {
			throw new RuntimeException("What the heck?\n" + e.toString());
		}
	}

	public boolean isLexicalConstruction(Construction c) {
		return c.getKind() == ECGConstants.CONCRETE && c.getConstructionalBlock().getElements().size() == 0
				&& !isSubcaseOfMorph(c);
	}

	// public boolean isLemmaConstruction(Construction c) {
		// ** check if it's a lemma construction
	// }

//	public boolean isLexicalConstruction(Construction c) {
//// return c.getKind() == ECGConstants.CONCRETE && isSubcaseOfWord(c);
//		return c.getKind() == ECGConstants.CONCRETE && isSubcaseOfMorph(c);
//	}

	public boolean isPhrasalConstruction(Construction c) {
		// System.out.println("in phr cxn "+c.getName()+" "+morph.getName()+" "+getCxnTypeSystem().subtype(c.getName(),
		// morph.getName()));
		return c.getKind() == ECGConstants.CONCRETE && c.getConstructionalBlock().getElements().size() > 0
				&& !isSubcaseOfMorph(c);
	}

	public boolean hasLexicalConstruction(String lexeme) {
		return lexemeToLexicalConstructions.containsKey(lexeme);
	}

	public List<Construction> getLexicalConstruction(String lexeme) {
		List<Construction> lex = lexemeToLexicalConstructions.get(lexeme);
		if (lex == null) {
			// System.out.println(lexemeToLexicalConstruction.keySet());
			throw new GrammarException("undefined lexeme: " + lexeme + " in Grammar.getLexicalConstruction");
		}
		return lex;
	}

	// @author seantrott 
	public List<Construction> getLemmaConstruction(String lemma) {
		List<Construction> lem = lemmaToLexicalConstructions.get(lemma);
		if (lem == null) {
			// System.out.println(lexemeToLexicalConstruction.keySet());
			throw new GrammarException("undefined lemma: " + lemma + " in Grammar.getLexicalConstruction");
		}
		return lem;
	}

	public Set<Construction> getAllConcretePhrasalConstructions() {
		Set<Construction> cxns = new LinkedHashSet<Construction>();
		for (Construction c : grammar.getAllConstructions()) {
			if (isPhrasalConstruction(c) && c.getKind() == ECGConstants.CONCRETE) {
				cxns.add(c);
			}
		}
		return cxns;
	}

	public Set<Construction> getAllConcreteLexicalConstructions() {
		Set<Construction> cxns = new HashSet<Construction>();
		for (Construction c : getAllConstructions()) {
			if (isLexicalConstruction(c) && c.getKind() == ECGConstants.CONCRETE) {
				cxns.add(c);
			}
		}
		return cxns;
	}

	public Collection<Construction> getAllConstructions() {
		return grammar.getAllConstructions();
	}

	public Construction getRootConstruction() {
		return grammar.getConstruction(ECGConstants.ROOT);
	}

	public TypeSystem<Construction> getCxnTypeSystem() {
		return grammar.getCxnTypeSystem();
	}

	public TypeSystem<Schema> getSchemaTypeSystem() {
		return grammar.getSchemaTypeSystem();
	}

	public Construction getConstruction(String c) {
		return grammar.getConstruction(c);
	}

	public ContextModel getContextModel() {
		return grammar.getContextModel();
	}

	public Set<String> getAllSubtypes(String c) {
		try {
			return getCxnTypeSystem().getAllSubtypes(c);
		}
		catch (TypeSystemException t) {
			throw new ParserException("TypeSystemException in LCPGrammarWrapper.getAllSubtypes " + c);
		}
	}

}

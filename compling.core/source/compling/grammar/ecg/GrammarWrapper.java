package compling.grammar.ecg;

/**
 * interface GrammarWrapper allows the writer of an application to use a well-defined interface for adding application
 * specific functionality to a grammar.
 * 
 * Different applications define lexical constructions differently, for example. And if you don't want to do redefine
 * these functions, make a grammar wrapper class that has another grammar wrapper class as an instance, and just define
 * these functions as wrappers around the instance wrapper's functions.
 * 
 * The idea of this interface is that the grammar itself remains static during the life of the wrapper.
 * 
 * @author John Bryant
 */

import java.util.Collection;
import java.util.List;
import java.util.Set;

import compling.grammar.ecg.Grammar.Construction;

public interface GrammarWrapper {

	public boolean isLexicalConstruction(Construction c);

	public boolean isPhrasalConstruction(Construction c);

	public boolean hasLexicalConstruction(String lexeme);

	public List<Construction> getLexicalConstruction(String lexeme);

	public Set<Construction> getAllConcretePhrasalConstructions();

	public Set<Construction> getAllConcreteLexicalConstructions();

	public Collection<Construction> getAllConstructions();

	public List<String> getConcreteSubtypes(String c);

	public Construction getRootConstruction();

	public List<Construction> getRules(String construction);

}

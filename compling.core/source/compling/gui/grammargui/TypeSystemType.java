/**
 * 
 */

package compling.gui.grammargui;

/**
 * enum TypeSystemType identifies a specific TypeSystem instantiation. 
 * It's  used by GrammarBrowser to encapsulate the map from concrete 
 * TypeSysem names, such as Schema, Construction, and Ontology, to the 
 * present enum type.  
 * <p> <strong>Rationale:</strong> mainly to make code cleaner.
 * @author   lucag
 */
public enum TypeSystemType {
	
	/**
	 * A Schema description. 
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	SCHEMA, 
	
	/**
	 * A Construction description. 
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	CONSTRUCTION, 
	
	/**
	 * An TypeSystemType or Ontology description. 
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	ONTOLOGY;
	
	/**
	 * Maps a TypeSystem name to this enum type. 
	 * 
	 * @see compling.gui.grammargui.TypeSystemType 
	 * @see	compling.grammar.unificationgrammar.TypeSystem
	 * 
	 * @param typeSystemName -
	 * 			A TypeSystem name
	 * @return
	 * 			The corresponding TypeSystemType identifier	
	 */
	public static TypeSystemType fromString(String typeSystemName) {
		if (typeSystemName.compareToIgnoreCase("schema") == 0)
			return TypeSystemType.SCHEMA;
		else if (typeSystemName.compareToIgnoreCase("construction") == 0)
			return TypeSystemType.CONSTRUCTION;
		else if (typeSystemName.compareToIgnoreCase("ontology") == 0)
			return TypeSystemType.ONTOLOGY;
		else
			throw new IllegalArgumentException(
					String.format("%s: unknown TypeSystem name", typeSystemName));
	}

}

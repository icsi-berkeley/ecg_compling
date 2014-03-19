/**
 * 
 */

package compling.gui.util;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;

/**
 * enum TypeSystemNodeType identifies a specific TypeSystem instantiation. It's used by GrammarBrowser to encapsulate
 * the map from concrete TypeSysem names, such as Schema, Construction, and Ontology, to the present enum type.
 * 
 * <p>
 * <strong>Rationale:</strong> mainly to make code cleaner.
 * 
 * @author lucag
 */
public enum TypeSystemNodeType {

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
	 * A Map description.
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	MAP,

	/**
	 * An TypeSystemNodeType or Ontology description.
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	ONTOLOGY,

	/**
	 * An TypeSystemNodeType or Situation description.
	 * 
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 */
	SITUATION;

	/**
	 * Maps a TypeSystemNode name (as return by TypeSystemNode.getType()) to this enum type.
	 * 
	 * @see compling.gui.util.TypeSystemNodeType
	 * @see compling.grammar.unificationgrammar.TypeSystem
	 * @param typeSystemName
	 *           - A TypeSystem name
	 * @return The corresponding TypeSystemNodeType identifier
	 */
	public static TypeSystemNodeType fromString(String typeSystemName) {
		if (typeSystemName.compareToIgnoreCase(ECGConstants.SCHEMA) == 0)
			return TypeSystemNodeType.SCHEMA;
		else if (typeSystemName.compareToIgnoreCase(ECGConstants.CONSTRUCTION) == 0)
			return TypeSystemNodeType.CONSTRUCTION;
		else if (typeSystemName.compareToIgnoreCase(ECGConstants.MAP) == 0)
			return TypeSystemNodeType.MAP;
		else if (typeSystemName.compareToIgnoreCase("type") == 0
				|| typeSystemName.compareToIgnoreCase(ECGConstants.ONTOLOGY) == 0)
			return TypeSystemNodeType.ONTOLOGY;
		else if (typeSystemName.compareToIgnoreCase(ECGConstants.SITUATION) == 0)
			return TypeSystemNodeType.SITUATION;
		else
			throw new IllegalArgumentException(String.format("%s: unknown TypeSystem name", typeSystemName));
	}

	public static TypeSystemNodeType fromTypeSystem(TypeSystem<? extends TypeSystemNode> typeSystem) {
		return fromString(typeSystem.getName());
	}

	public static TypeSystemNodeType fromNode(TypeSystemNode node) {
		if (node instanceof Grammar.Schema)
			return TypeSystemNodeType.SCHEMA;
		else if (node instanceof Grammar.Construction)
			return TypeSystemNodeType.CONSTRUCTION;
		else if (node instanceof Grammar.MapPrimitive)
			return TypeSystemNodeType.MAP;
		else if (node instanceof TypeSystemNode)
			return TypeSystemNodeType.ONTOLOGY;
		else if (node instanceof Grammar.Situation)
			return TypeSystemNodeType.SITUATION;
		else
			throw new IllegalArgumentException(String.format("%s: unknown TypeSystem name", node));
	}
	
}

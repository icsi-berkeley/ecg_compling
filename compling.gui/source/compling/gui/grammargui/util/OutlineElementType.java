package compling.gui.grammargui.util;

import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.grammargui.model.TestSentenceModel.Group;
import compling.gui.grammargui.model.TestSentenceModel.Sentence;
import compling.gui.util.TypeSystemNodeType;

/**
 * The type of nodes in outlines throughout the ECG Workbench.
 * 
 * @author lucag
 */
public enum OutlineElementType {

	SENTENCE_VALID("icons/e/public_co.gif"),
	SENTENCE_INVALID("icons/e/private_co.gif"),
	DELETE_SENTENCE_E("icons/e/remove_exc.gif"),
	DELETE_SENTENCE_D("icons/d/remove_exc.gif"),
	ADD_SENTENCE_E("icons/e/add_exc.gif"),
	ADD_SENTENCE_D("icons/d/add_exc.gif"),
	OPEN_EDITOR_E("icons/text.gif"),
	PROBLEM_OVERLAY("icons/error_co.gif"),
	CONSTRUCTION("icons/constr16-1.png"),
	SCHEMA("icons/schema16-1.png"),
	ONTOLOGY("icons/ont16-1.png"),
	MAP("icons/map16-1.png"),
	SITUATION("icons/sit16-3.png"),
	PARSE("icons/16-file-archive_16x16.png"),
	UNTYPED("icons/shape_square_16x16.png"),
	ATOMIC("icons/edit-quotation_16x16.png"),
//	SENTENCE("icons/field_public_obj_16x16.gif"),
	SENTENCE("icons/decrypt.gif"),
	SENTENCE_GROUP("icons/queries_closed_highlighted.gif"),
	UNKNOWN("icons/unknown.png");

	private final String path;

	private OutlineElementType(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public static final OutlineElementType valueOf(TypeSystemNodeType nodeType) {
		switch (nodeType) {
		case CONSTRUCTION:
			return CONSTRUCTION;
		case SCHEMA:
			return SCHEMA;
		case ONTOLOGY:
			return ONTOLOGY;
		case MAP:
			return MAP;
		case SITUATION:
			return SITUATION;
		default:
			// TODO: is this ok?
			return UNTYPED;
		}
	}

	public static final OutlineElementType valueOf(TypeSystem<?> typeSystem) {
		return valueOf(TypeSystemNodeType.fromTypeSystem(typeSystem));
	}

	public static final OutlineElementType valueOf(TypeConstraint typeConstraint) {
		return typeConstraint != null ? 
				valueOf(TypeSystemNodeType.fromTypeSystem(typeConstraint.getTypeSystem())) 
				: UNTYPED;
	}

	public static final OutlineElementType valueOf(Slot slot) {
		if (slot.hasAtomicFiller())
			return ATOMIC;
		else
			return valueOf(slot.getTypeConstraint());
	}
	
	public static final OutlineElementType valueOf(Object object) {
		if (object instanceof Sentence)
			return SENTENCE;
		else if (object instanceof Group) 
			return SENTENCE_GROUP;
		else
			return UNKNOWN;
	}	
}

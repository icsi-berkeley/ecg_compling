package compling.gui.grammargui.util;

import compling.gui.util.TypeSystemNodeType;

public final class Constants {
	public interface IImageKeys {
		public static final String SENTENCE = "icons/edit-quotation_16x16.png";
		public static final String SENTENCE_VALID = "icons/e/public_co.gif";
		public static final String SENTENCE_INVALID = "icons/d/private_co.gif";
		public static final String REMOVE_SENTENCE_E = "icons/e/remove_co.png";
		public static final String REMOVE_SENTENCE_D = "icons/d/remove_co.png";
		public static final String REMOVE_ALL_SENTENCES_E = "icons/e/removeall_co.png";
		public static final String REMOVE_ALL_SENTENCES_D = "icons/d/removeall_co.png";
		public static final String ADD_SENTENCE_E = "icons/e/add_exc.gif";
		public static final String ADD_SENTENCE_D = "icons/d/add_exc.gif";
		public static final String ANALYZE_SENTENCE_E = "icons/e/run_exc.gif";
		public static final String ANALYZE_SENTENCE_D = "icons/d/run_exc.gif";
		public static final String OPEN_EDITOR_E = "icons/text.gif";
		public static final String PROBLEM_OVERLAY = "icons/error_co.gif";
		public static final String CONSTRUCTION = "icons/constr16-1.png";
		public static final String SCHEMA = "icons/schema16-1.png";
		public static final String ONTOLOGY = "icons/ont16-1.png";
		public static final String MAP = "icons/map16-1.png";
		public static final String SITUATION = "icons/sit16-3.png";
		public static final String ANALYSIS = "icons/ADD_Attribute_16x16.gif";
		public static final String PARSE = "icons/16-file-archive_16x16.png";
		public static final String UNTYPED = "icons/brackets_16x16.png";
		public static final String ATOMIC = "icons/attr_val_16x16.gif";
		public static final String START_E = "icons/e/run_exc.gif";
		public static final String START_D = "icons/d/run_exc.gif";
		public static final String ERROR = "icons/error_co.gif";
		public static final String TOKEN = "icons/tokenTest.ico";
		
	}
	
	public static final String nodeToKey(TypeSystemNodeType nodeType) {
		switch (nodeType) {
		case CONSTRUCTION:
			return IImageKeys.CONSTRUCTION;
		case SCHEMA:
			return IImageKeys.SCHEMA;
		case ONTOLOGY:
			return IImageKeys.ONTOLOGY;
		case MAP:
			return IImageKeys.MAP;
		case SITUATION:
			return IImageKeys.SITUATION;
		default:
			throw new IllegalArgumentException("shouldn't have gotten here");
		}
	}
}


package compling.annotation.childes;

public interface ChildesConstants {

	/* XML tags and attributes. */

	public static final String PARTICIPANTS = "Participants";
	public static final String PARTICIPANT = "participant";
	public static final String SETTING = "Setting";
	public static final String SETUP = "Setup";
	public static final String ENTITY = "entity";
	public static final String BINDING = "binding";
	public static final String EVENT = "event";
	public static final String UTTERANCE = "u";
	public static final String SPEAKER = "who";
	public static final String CLAUSE = "clause";
	public static final String WORD = "w";
	public static final String WORDNET = "wn";
	public static final String PAUSE = "pause";

	public static final String CATEGORY = "cat";
	public static final String ID = "id";
	public static final String FIELD = "field";
	public static final String SOURCE_REF = "source_ref";
	public static final String REFERENCE = "ref";
	public static final String VALUE = "value";

	public static final String ANNOTATION = "a";
	public static final String TYPE = "type";
	public static final String SPEECH_ACT_ANNOTATION = "sa";
	public static final String SEMANTIC_ANNOTATION = "semantic";

	public static final String SPEECH_ACT = "speech act";
	public static final String VERNACULAR = "vernacular";
	public static final String TRANSLATION = "english translation";
	public static final String GOLD_STANDARD = "gold standard";

	public static final String SPAN_LEFT = "left";
	public static final String SPAN_RIGHT = "right";
	public static final String PROFILED = "profiled";
	public static final String SUBCAT = "subcat";

	/* XPath expressions */
	public static final String NS = "cl";
	public static final String NSPREFIX = "cl:";
	public static final String NAMESPACE = "http://www.talkbank.org/ns/talkbank";

	public static final String XPATH_CHAT = "//" + NSPREFIX + "CHAT";

	public static final String XPATH_UTTERANCE_TYPE = NSPREFIX + UTTERANCE;
	public static final String XPATH_UTTERANCE = XPATH_CHAT + "/" + XPATH_UTTERANCE_TYPE;

	public static final String XPATH_CLAUSE_TYPE = NSPREFIX + CLAUSE;
	public static final String XPATH_CLAUSE = XPATH_UTTERANCE + "/" + XPATH_CLAUSE_TYPE;

	public static final String XPATH_EVENT_TYPE = NSPREFIX + EVENT;
	public static final String XPATH_EVENT = XPATH_CHAT + "/" + XPATH_EVENT_TYPE;

	public static final String XPATH_PARTICIPANT = XPATH_CHAT + "/" + NSPREFIX + PARTICIPANTS + "/" + NSPREFIX
			+ PARTICIPANT;
	public static final String XPATH_SETTING = XPATH_CHAT + "/" + NSPREFIX + SETTING + "/" + NSPREFIX + "*";
	public static final String XPATH_SETUP = XPATH_CHAT + "/" + NSPREFIX + SETUP + "/" + NSPREFIX + "*";

	public static final String XPATH_PAUSE_TYPE = NSPREFIX + PAUSE;
	public static final String XPATH_PAUSE = "descendant::" + XPATH_PAUSE_TYPE;

	public static final String XPATH_WORD_TYPE = NSPREFIX + WORD;
	public static final String XPATH_WORD_DESCENDANT = "descendant::" + XPATH_WORD_TYPE;

	public static final String XPATH_WORDNET_TYPE = NSPREFIX + WORDNET;
	public static final String XPATH_WORDNET_PARENT = "parent::" + XPATH_WORDNET_TYPE;

	public static final String XPATH_ANNOTATION_TYPE = NSPREFIX + ANNOTATION;
	public static final String XPATH_ANNOTATION_CHILD = "child::" + XPATH_ANNOTATION_TYPE;

	public static final String XPATH_OPEN_TYPE_RESTRICTION = "[@" + TYPE + "=\"";
	public static final String XPATH_CLOSE_RESTRICTION = "\"]";

	public static final String XPATH_RETRACE_TYPE = NSPREFIX + "k";
	public static final String XPATH_RETRACE_CHILD = "child::" + XPATH_RETRACE_TYPE + "[@type=\"retracing\"]";

	public static final String XPATH_REPLACEMENT_CHILD = "child::" + XPATH_ANNOTATION_TYPE + "[@type=\"replacement\"]";

	public static final String XPATH_BINDING_TYPE = NSPREFIX + BINDING;
	public static final String XPATH_BINDING = "descendant::" + XPATH_BINDING_TYPE;

	public static final String XPATH_ANNOTATION_BY_ID = "preceding::*[@" + ID + "=\"";

	/* output strings */
	static final String COMPOUND_SEPERATOR = "+";
	static final String DEFAULT_SEPERATOR = " ";

	/* TODO: setting column width based on max value. */
	public static final String SPEAKER_MAX_PATTERN = "XXXX";

	public enum GSPrimitive {
		TE("temporal_element"), TS("temporal_structure"), TOPIC("topic"), COMT("comment"), COND("conditional"), TMP(
				"temporal_ordering"), BEN("benefaction"), MAL("malefaction"), REDUP("reduplication"), IMG("image_schema");

		private final String type;

		GSPrimitive(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public static GSPrimitive getType(String name) {
			for (GSPrimitive p : values()) {
				if (p.type.equals(name)) {
					return p;
				}
			}
			return null;
		}
	}
}

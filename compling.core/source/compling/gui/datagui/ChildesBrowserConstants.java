package compling.gui.datagui;

public interface ChildesBrowserConstants {

	// //////////////////////
	// ChildesBrowser2
	// //////////////////////

	/* Messages and menu items */
	public static final String BROWSER_TITLE = "Childes Browser";
	public static final String MENU_FILE = "File";
	public static final String MENU_OPEN_FILE = "Open File...";
	public static final String MENU_PREPROCESS = "Preprocess File...";
	public static final String FC_SAVE_PREPROCESS = "Save Preprocessed File...";
	public static final String ERROR = "Error";
	public static final String ERROR_BAD_XML = "Not a valid XML file.";

	public static final String MENU_TOOLS = "Tools";
	public static final String MENU_STATS = "Corpus statistics";

	public static final String FILE_PATTERN = ".xml$";
	public static final String FILE_SUFFIX = ".anno.xml";

	public static final int EMPTY_CHOICE = 2;

	// //////////////////
	// ChildesPanel and AnnotationPanel
	// /////////////////

	public static final String ITEM_TAB = "Clause";
	public static final String ITEM_TAB_TOOLTIP = "Annotate item.";

	public static final String VERN_LABEL = "Vernacular: ";

	/** Number of visible columns in the JTextField; does not limit user character input. */
	public static final int VERN_FIELD_SIZE = 30;

	/** Text for annotation toolbar. */
	public static final String TOOLBAR_TITLE = "Annotation";
	public static final String NEXT = "Next";
	public static final String PREV = "Prev";
}

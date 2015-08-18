package compling.grammar.ecg;

import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;

/**
 * This class is a holder for all static constants in the system including grammar keywords.
 * 
 * Having this around will clean up other classes and set up a common holder for static stuff.
 * 
 * @Author John Bryant
 * 
 */

public class ECGConstants {

	public static final String SELF = "SELF";

	public static final String ROOT = "ROOT";

	public static final String UNKNOWN_ITEM = "UNKNOWN-ITEM";

	/** Construction Keywords */
	public static final String CONCRETE = "CONCRETE";

	public static final String ABSTRACT = "ABSTRACT";

	public static final String FORM = "FORM";

	public static final String MEANING = "MEANING";

	public static final String CONSTRUCTIONAL = "CONSTRUCTIONAL";

	public static final String OPTIONAL = "OPTIONAL";

	/** For extraposition */
	public static final String EXTRAPOSED = "EXTRAPOSED";

	/** Schema Keywords */
	public static final String CONTENTS = "CONTENTS";

	public static final String SEMANTIC = "SEMANTIC";

	public static final String FEATURE = "FEATURE";

	/** Typing constants */
	public static final String UNTYPED = "UNTYPED";

	/** Constraint constants */

	/**
	 * The constant for coindexation that should be passed into the operator role of the constraint constructor. IDENT =
	 * "<-->"
	 */
	public static final String IDENTIFY = "<-->";

	/**
	 * The constant for assignment that should be passed into the operator role of the constraint constructor. ASSIGN =
	 * "<--"
	 */
	public static final String ASSIGN = "<--";
	
	/**
	 * 	 * The constant for assignment that should be passed into the operator role of the constraint constructor. ASSIGN =
	 * "<--"
	 */
	public static final String NEGATE = "#";

/**
    * The constant for form constraint 'before' that should be passed into the
    * operator role of the constraint constructor. BEFORE = "<"
    */
	public static final String BEFORE = "before";

	/**
	 * The constant for form constraint 'meets' that should be passed into the operator role of the constraint
	 * constructor. MEETS ="<="
	 */
	public static final String MEETS = "meets";

	/**
	 * A static "self" ECGSlotChain
	 */

	public static final ECGSlotChain SELFSLOTCHAIN = new ECGSlotChain("self");
	public static final ECGSlotChain EMPTYSLOTCHAIN = new ECGSlotChain("");
	public static final ECGSlotChain ANCESTORSLOTCHAIN = new ECGSlotChain("ancestor");
	public static final ECGSlotChain GAPFILLERSLOTCHAIN = new ECGSlotChain("gapfiller");

	public static final String FORM_POLE = "f";
	public static final String MEANING_POLE = "m";

	/** Untyped roles "m" and "f" for use when checking constraints on untyped constructions */

	public static final Role UNTYPEDMROLE = new Role(MEANING_POLE);
	public static final Role UNTYPEDFROLE = new Role(FORM_POLE);

	public static final char ONTOLOGYPREFIX = '@';
	public static final char CONSTANTFILLERPREFIX = '\"';

	/** DS stuff */
	public static final String DS = "DS";
	public static final Role DSROLE = new Role(DS);
	public static final String DISCOURSESEGMENTTYPE = "Discourse_Segment";

	public static final String speakerTypeName = "Speaker";
	public static final String speakerRoleName = "speaker";
	public static final String addresseeTypeName = "Addressee";
	public static final String addresseeRoleName = "addressee";
	public static final String attentionalFocusTypeName = "Attentional_Focus";
	public static final String attentionalFocusRoleName = "attentional_focus";
	public static final String discourseParticipantRoleTypeName = "Discourse_Participant_Role";
	public static final String discourseParticipantRoleRoleName = "discourse_participant_role";
	public static final String speechActTypeName = "Speech_Act";
	public static final String speechActRoleName = "speech_act";

	public static final String slotChainToSpeechAct = DS + "." + speechActRoleName;

	// ADDED (nschneid)
	public final static String MCXNTYPE = "Morph";
	public final static String MWORDTYPE = "WLMorph";
	public final static String MROOTTYPE = "WROOT";

	// ADDED (lucag): begin

	//
	// Situations
	//

	/** Global role name */
	public static final String FOCUS = "focus";

	/** focus role, added when <code>SITUATIONTYPE</code> is present */
	public static final Role FOCUSROLE = new Role(FOCUS);

	/**
	 * Name for the situation type whose presence triggers the addition of the focus role.
	 */
	public static final String SITUATIONTYPE = "SituationRoot";

	public static final String BASESITUATIONTYPE = "BaseSpace";
	// ADDED (lucag): end

	/** Random strings that should be treated as constants */

	/** The NOFILLER String is used in Analysis utilities */
	public static final String NOFILLER = "NO FILLER";

	/** RD CONSTANTS */
	public static final String RD = "RD";
	public static final String RESOLVEDREFERENT = "referent";
	public static final String ONTOLOGICALCATEGORY = "ontological_category";
	public static final String REFERENCEKIND = "givenness";

	public static final String GIVEN = "\"GIVEN\"";
	public static final String IDENTIFIABLE = "\"IDENTIFIABLE\"";
	public static final String CONTEXTNEW = "\"CONTEXTNEW\"";

	/** Context Element CONSTANTS */
	public static final String CONTEXTELEMENT = "Context_Element";
	public static final String PROPERTY_FILLER = "Property_Filler";

	public static final String SCHEMA = "SCHEMA";
	public static final String CONSTRUCTION = "CONSTRUCTION";
	public static final String MAP = "MAP";
	public static final String SITUATION = "SITUATION";
	public static final String ONTOLOGY = "ONTOLOGY";
	public static final String CONSTITUENTS = "constituents";
	public static final String ROLES = "roles";

	/** Encoding for input files (such as grammar files) */
	public static final String DEFAULT_ENCODING = "UTF-8";

	/**
	 * This is the current home of the default probabilities for local, omitted and nonlocal constituents THESE THREE
	 * NUMBERS SHOULD ADD TO 1!!!
	 */
	public static final double DEFAULTLOCALPROBABILITY = 1;
	public static final double DEFAULTNONLOCALPROBABILITY = 0;
	public static final double DEFAULTOMISSIONPROBABILITY = 0;

	/** This is the current home for the default probability that an optional constituent appears */
	public static final double DEFAULTOPTIONALPROBABILITY = .5;
	
	/** Package / import constants. (seantrott). */
	public static final String PACKAGE = "PACKAGE";
	public static final String IMPORT = "IMPORT";

}

// =============================================================================
//File        : ChildesLocalizer.java
//Author      : emok
//Change Log  : Created on Jan 26, 2008
//=============================================================================

package compling.annotation.childes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import compling.learner.contextfitting.ContextFitter.ContextFittingLocalizer;
import compling.learner.util.AnnotationUtilities.GoldStandardAnnotationLocalizer;
import compling.simulator.Simulator.ScriptLocalizer;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class ChildesLocalizer implements GoldStandardAnnotationLocalizer, ScriptLocalizer, ContextFittingLocalizer {

	public static enum Participant {
		Child,
		Mother,
		Father,
		Mother_Child,
		Father_Child,
		Parents,
		Parents_and_Child,
		Investigator,
		Inv_Child,
		Mot_Inv,
		Fat_Inv,
		Mot_Chi_Inv,
		Fat_Chi_Inv,
		Parents_Inv,
		Parents_Chi_Inv,
		Granny,
		Uncle;
	}

	public static enum DSRole {
		Speaker, Addressee, Attentional_Focus, Speech_Act;
	}

	/** CONSTANTS important to simulator */

	/** CONSTANTS important to context fitting */
	public static final String ELEMENTTYPE = "Element";
	public static final String PROCESSTYPE = "Process";
	public static final String COMPLEXPROCESSTYPE = "Complex_Process";
	public static final String ENTITYTYPE = "Entity";
	public static final String ANIMATE = "Animate";
	public static final String INANIMATE = "Inanimate";
	public static final String SPEECH_ACTTYPE = "Speech_Act";
	public static final String TIMESTAMP = "timestamp";
	public static final String PROTAGONIST = "protagonist";
	public static final String SPGTYPE = "Source_Path_Goal";

	public static final String contentRoleName = "content";
	public static final String instrumentRoleName = "instrument";
	public static final String eventStructureRoleName = "event_structure";
	public static final String modifierAspectRoleName = "modifier_aspect";
	public static final String eventDescriptorTypeName = "Event_Descriptor";
	public static final String eventDescriptorRoleName = "ed";
	public static final String referentRoleName = "referent";
	public static final String eventTypeRoleName = "event_type";
	public static final String profiledProcessRoleName = "profiled_process";
	public static final String profiledParticipantRoleName = "profiled_participant";
	public static final String modifiersRoleName = "modifiers";
	public static final String modifierCategoryRoleName = "category";
	public static final String modalityRoleName = "modifiers";
	public static final String modalityCategoryRoleName = "category";

	public static final String SETTING = "Setting";
	public static final String START = "Start";

	public static final String SPEECH_ACT_ADMONISHING = "Admonishing";
	public static final String SPEECH_ACT_REQUESTING_ACTION = "Requesting_Action";
	public static final String SPEECH_ACT_REQUESTING_ANSWER = "Requesting_Answer";
	public static final String SPEECH_ACT_CALLING = "Calling";

	/** CONSTANTS important to verifier */
	public static final String NONE = "none";
	public static final String DNI = "DNI";
	public static final String INI = "INI";

	public final String ASPECT = "aspect";
	public final String MODALITY = "modality";
	public final String MODIFIER = "modifier";
	public final String RESULTATIVE = "resultative";

	public static final String VC = "Verb_Clause";
	public static final String VOICING = "Voicing";
	public static final String FINITE_CLAUSE = "Finite_Clause";

	/** CONSTANTS important to learner */
	public static final String IMPORTANT_TYPE = "Important_Type";
	public static final String MORPHEME = "Morpheme";
	public static final String WORD = "Word";
	public static final String CLAUSE = "Clause";
	public static final String PHRASE = "Phrase";
	public static final String INTERJECTION = "Interjection";
	public static final String LEFTOVER_MORPHEME = "Leftover_Morpheme";
	public static final String VARIANT_SUFFIX = "_variant";
	public static Set<String> GIVEN_CATEGORIES = new HashSet<String>();

	/** MAPS important to simulator */
	private HashMap<String, String> scriptMapings = new HashMap<String, String>();
	private HashMap<Participant, String> participants = new HashMap<Participant, String>();
	private HashMap<DSRole, String> dsroles = new HashMap<DSRole, String>();

	/** MAPS important to context fitting */
	public static HashMap<String, String> speechActCxns = new HashMap<String, String>();

	/** MAPS important to verifier */
	MapSet<String, String> goldStandardMappings = new MapSet<String, String>();
	Map<String, String> reverseGoldStandardMappings = new HashMap<String, String>();
	private Set<String> ignoreRoles = new HashSet<String>();
	private Set<String> adjRoles = new HashSet<String>();
	private Set<String> impreciseBracketingRoles = new HashSet<String>();
	private MapMap<String, String, String> roleNames = new MapMap<String, String, String>();
	private MapMap<Pair<String, String>, String, String> roleFillers = new MapMap<Pair<String, String>, String, String>();

	public ChildesLocalizer() {
		GIVEN_CATEGORIES.add(IMPORTANT_TYPE);
		GIVEN_CATEGORIES.add(MORPHEME);
		GIVEN_CATEGORIES.add(WORD);
		GIVEN_CATEGORIES.add(CLAUSE);
		GIVEN_CATEGORIES.add(PHRASE);

		populate();
	}

	public void populate() {
		/** MAPS important to simulator */
		scriptMapings.put("explaining", "Explaining");
		scriptMapings.put("answering", "Answering");
		scriptMapings.put("approving", "Approving");
		scriptMapings.put("admonishing", "Admonishing");
		scriptMapings.put("requesting-action", "Requesting_Action");
		scriptMapings.put("requesting-answer", "Requesting_Answer");
		scriptMapings.put("calling", "Calling");
		scriptMapings.put("exclaiming", "Exclaiming");
		scriptMapings.put("practicing", "Practicing");

		scriptMapings.put("reduced", "Reduced");
		scriptMapings.put("normal", "Normal");

		dsroles.put(DSRole.Speaker, "speaker");
		dsroles.put(DSRole.Addressee, "addressee");
		dsroles.put(DSRole.Attentional_Focus, "attentional_focus");
		dsroles.put(DSRole.Speech_Act, "speech_act");

		participants.put(Participant.Child, "CHI");
		participants.put(Participant.Mother, "MOT");
		participants.put(Participant.Father, "FAT");
		participants.put(Participant.Mother_Child, "MOTandCHI");
		participants.put(Participant.Father_Child, "FATandCHI");
		participants.put(Participant.Parents, "MOTandFAT");
		participants.put(Participant.Parents_and_Child, "MOTandFATandCHI");
		participants.put(Participant.Investigator, "INV");
		participants.put(Participant.Inv_Child, "INVandCHI");
		participants.put(Participant.Mot_Inv, "MOTandINV");
		participants.put(Participant.Mot_Chi_Inv, "MOTandCHIandINV");
		participants.put(Participant.Fat_Inv, "FATandINV");
		participants.put(Participant.Fat_Chi_Inv, "FATandCHIandINV");
		participants.put(Participant.Parents_Inv, "MOTandFATandINV");
		participants.put(Participant.Parents_Chi_Inv, "MOTandFATandCHIandINV");
		participants.put(Participant.Granny, "ONN");
		participants.put(Participant.Uncle, "UNC");

		/** MAPS important to context fitting */
		speechActCxns.put("Labeling", "Labeling_Clause");
		speechActCxns.put("Explaining", "Explaining_Clause");
		speechActCxns.put("Repeating", "Repeating_Clause");
		speechActCxns.put("Answering", "Answering_Clause");
		speechActCxns.put("Requesting_Action", "Requesting_Action_Clause");
		speechActCxns.put("Requesting_Answer", "Requesting_Answer_Clause");
		speechActCxns.put("Calling", "Calling_Clause");
		speechActCxns.put("Admonishing", "Admonishing_Clause");
		speechActCxns.put("Approving", "Approving_Clause");
		speechActCxns.put("Exclaiming", "Exclaiming_Clause");
		speechActCxns.put("Conversing", "Conversing_Clause");

		/** MAPS important to verifier */
		goldStandardMappings.put("Action", "Action");
		goldStandardMappings.put("Caused_Motion", "Caused_Motion");
		goldStandardMappings.put("Concurrent_Processes", "Concurrent_Processes");
		goldStandardMappings.put("Copula", "Two_Participant_State");
		goldStandardMappings.put("Ditransitive_Action", "Transfer");
		goldStandardMappings.put("Ditransitive_Action", "Communication");
		goldStandardMappings.put("Intransitive_Action", "Intransitive_Action");
		goldStandardMappings.put("Intransitive_State", "Intransitive_State");
		goldStandardMappings.put("Joint_Motion", "Joint_Motion");
		goldStandardMappings.put("Other_Transitive_Action", "Uncategorized_Transitive_Action");
		goldStandardMappings.put("Self_Motion", "Translational_Self_Motion");
		goldStandardMappings.put("Serial_Processes", "Complex_Process");
		goldStandardMappings.put("Concurrent_Processes", "Complex_Process");
		goldStandardMappings.put("Transitive_Action", "Cause_Effect");
		goldStandardMappings.put("Transitive_Action", "Cause_Change");
		goldStandardMappings.put("Transitive_Action", "Obtainment");
		goldStandardMappings.put("Transitive_Action", "Ingestion");
		goldStandardMappings.put("Transitive_Action", "Perception");
		goldStandardMappings.put("Two_Participant_State", "Two_Participant_State");

		goldStandardMappings.put("SPG", SPGTYPE);

		goldStandardMappings.put("Perfective", "Modifier_Perfective");
		goldStandardMappings.put("Imperfective", "Modifier_Imperfective");
		goldStandardMappings.put("Experiential", "Modifier_Experiential");

		adjRoles.add(ASPECT);
		adjRoles.add(MODALITY);
		adjRoles.add(MODIFIER);
		adjRoles.add(RESULTATIVE);

		impreciseBracketingRoles.add("spg");
		impreciseBracketingRoles.add("deixis");
		impreciseBracketingRoles.add("path");
		impreciseBracketingRoles.add("resultative");

		roleNames.put("Source_Path_Goal", "direction", "goal");

		roleNames.put("Process", "agent", "protagonist");
		roleNames.put("Action", "agent", "protagonist");
		roleNames.put("Complex_Process", "agent", "protagonist");

		roleNames.put("Two_Participant_State", "equal", "percept");
		roleNames.put("Cause_Change", "patient", "undergoer");
		roleNames.put("Obtainment", "patient", "obtained");
		roleNames.put("Ingestion", "patient", "ingested");
		roleNames.put("Perception", "patient", "percept");

		roleFillers.put(new Pair<String, String>("Source_Path_Goal", "deixis"), "Towards", "Deitic_Towards");
		roleFillers.put(new Pair<String, String>("Source_Path_Goal", "deixis"), "Away", "Deitic_Away");

	}

	public Set<String> getGoldStandardLocalization(String key) {
		if (goldStandardMappings.get(key) != null) {
			return goldStandardMappings.get(key);
		}
		else {
			Set<String> values = new HashSet<String>();
			values.add(key);
			return values;
		}
	}

	public String getGoldStandardRoleNameLocalization(String type, String roleName) {
		if (roleNames.get(type, roleName) != null) {
			return roleNames.get(type, roleName);
		}
		else {
			return roleName;
		}
	}

	public String getGoldStandardRoleFillerLocalization(String type, String roleName, String fillerName) {
		Pair<String, String> role = new Pair<String, String>(type, roleName);
		if (roleFillers.get(role, fillerName) != null) {
			return roleFillers.get(role, fillerName);
		}
		else {
			return fillerName;
		}
	}

	public Set<String> getGoldStandardRolesToIgnore() {
		return ignoreRoles;
	}

	public Set<String> getGoldStandardAdjunctRoles() {
		return adjRoles;
	}

	public Set<String> getGoldStandardRolesWithImpreciseBracketing() {
		return impreciseBracketingRoles;
	}

	public boolean hasLocalization(String annotation) {
		return scriptMapings.containsKey(annotation.toLowerCase().replace('_', '-'));
	}

	public String getScriptLocalization(String annotation) {
		String localized = scriptMapings.get(annotation.toLowerCase().replace('_', '-'));
		return localized != null ? localized : annotation;
	}

	public String getParticipantLocalization(Participant participant) {
		return participants.get(participant);
	}

	public String getDSRoleLocalization(DSRole role) {
		return dsroles.get(role);
	}
}

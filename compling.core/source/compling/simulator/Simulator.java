// =============================================================================
//File        : Simulator.java
//Author      : emok
//Change Log  : Created on Aug 17, 2007
//=============================================================================

package compling.simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compling.annotation.childes.ChildesAnnotation.SpeechActTier;
import compling.annotation.childes.ChildesConstants;
import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesLocalizer.DSRole;
import compling.annotation.childes.ChildesLocalizer.Participant;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesTranscript.ChildesEvent;
import compling.annotation.childes.FeatureBasedEntity.SimpleFeatureBasedEntity;
import compling.context.ContextException;
import compling.context.ContextModel;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.ECGConstants;
import compling.learner.LearnerException;
import compling.simulator.SimulationParameters.Value;
import compling.simulator.SimulatorException.ScriptNotFoundException;

//=============================================================================

public class Simulator {

	public enum SimulatorMode {
		CONTEXTMODEL_UPDATE, SIMULATION, TEST_PRECONDITION;
	}

	public static interface ScriptLocalizer {
		public boolean hasLocalization(String annotation);

		public String getScriptLocalization(String annotation);

		public String getParticipantLocalization(Participant participant);

		public String getDSRoleLocalization(DSRole role);
	}

	private TreeMap<String, String> scripts = new TreeMap<String, String>();;
	private ScriptLocalizer localizer = new ChildesLocalizer();
	private ContextModel contextModel;
	private String lastExecutedScriptName;

	private final String INSTANCE_SCRIPT_NAME = "Instance";

	private String settingScript = "{exec (inst " + ChildesLocalizer.SETTING
			+ " Interval BASE NONE); exec (setcurrentinterval " + ChildesLocalizer.SETTING + ");}";
	private String setupScript = "{exec (inst " + ChildesLocalizer.START
			+ " Interval Setting NONE); exec (setcurrentinterval " + ChildesLocalizer.START + ");}";
	private String instanceScriptPrefix = "{exec (inst _id ";
	private String instanceScriptSuffix = " );}";
	private String bindingScript = "{exec (fil _" + ChildesConstants.FIELD + " _" + ChildesConstants.SOURCE_REF + " _"
			+ ChildesConstants.VALUE + ");}";

	private List<String> recentlyAccessedIndividuals = new ArrayList<String>();

	protected static Logger logger = Logger.getLogger(Simulator.class.getName());

	public Simulator(ContextModel contextModel, File scriptFile) throws IOException {
		this.contextModel = contextModel;
		loadScripts(scriptFile);
	}

	public Simulator(ContextModel contextModel, List<File> scriptFiles) throws IOException {
		this.contextModel = contextModel;
		for (File scriptFile : scriptFiles) {
			loadScripts(scriptFile);
		}
	}

	public void loadScripts(File file) throws IOException {
		try {
			ScriptSplitterLexer scanner;
			scanner = new ScriptSplitterLexer(new BufferedReader(new FileReader(file)));
			ScriptSplitter splitter = new ScriptSplitter(scanner);
			splitter.setSimulator(this);
			try {
				splitter.parse();
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new SimulatorException("Terminal Error: Cannot parse script file.");
			}
		}
		catch (FileNotFoundException e1) {
			throw new ContextException(e1.toString());
		}
	}

	public boolean simulateEvent(ChildesEvent event) {
		String scriptName = event.getCategory();
		SimulationParameters parameters = new SimulationParameters(event, localizer);
		parameters.addParameter("id", new Value(event.getID(), false));
		boolean success = updateContextModel(scriptName, parameters);

		List<String> recentIndividualIDs = new ArrayList<String>();
		for (String feature : parameters.getAllFeatures()) {
			for (Value simulatedValue : parameters.getParameter(feature)) {
				if (simulatedValue.isRef()) {
					recentIndividualIDs.add(simulatedValue.getFiller());
				}
			}
		}
		if (success) {
			recentlyAccessedIndividuals = contextModel.updateRecentSituationEntities(recentIndividualIDs);
		}

		return success;
	}

	/**
	 * Given a <code>ChildesClause</code>, it adds the speech act to the <code>ContextModel</code> according to the
	 * special Speech_Act script, preferably by instantiating a new interval.
	 * 
	 * @param clause
	 * @return
	 * @throws SimulatorException
	 */
	public boolean registerUtterance(ChildesClause clause, Set<String> jointAttention) throws SimulatorException {
		if (clause == null) {
			throw new SimulatorException("A null clause is encountered");
		}

		SpeechActTier speechActTier = clause.getChildesAnnotation().getSpeechActTier();
		if (speechActTier == null) {
			throw new SimulatorException("No speech act annotation found for clause " + clause.getID());
		}

		SimpleFeatureBasedEntity annotation = speechActTier.getContent();
		SimulationParameters params = new SimulationParameters(annotation);
		params.addParameter("id", new Value(annotation.getID(), false));
		for (String attentionalFocus : jointAttention) {
			params.addParameter(ECGConstants.attentionalFocusRoleName, new Value(attentionalFocus, false));
		}
		boolean success = updateContextModel(localizer.getScriptLocalization(annotation.getCategory()), params);

		if (success) {
			List<String> recentIndividualIDs = new ArrayList<String>();
			for (String feature : params.getAllFeatures()) {
				for (Value simulatedValue : params.getParameter(feature)) {
					if (simulatedValue.isRef()) {
						recentIndividualIDs.add(simulatedValue.getFiller());
					}
				}
			}
			SimpleQuery s = new SimpleQuery(contextModel.getMiniOntology().getCurrentIntervalName(),
					ECGConstants.DISCOURSESEGMENTTYPE);
			String currentDS = contextModel.retrieveIndividual(s);
			if (currentDS == null) {
				throw new LearnerException("Error retreiving the current " + ECGConstants.DISCOURSESEGMENTTYPE);
			}
			recentIndividualIDs.add(currentDS);
			recentlyAccessedIndividuals = contextModel.updateRecentSituationEntities(recentIndividualIDs);
		}

		return success;
	}

	boolean updateContextModel(String scriptName, SimulationParameters parameters) {
		if (!scripts.containsKey(scriptName)) {
			throw new ScriptNotFoundException("script not defined for " + scriptName, scriptName);
		}
		String script = scripts.get(scriptName);
		return run(scriptName, script, parameters, SimulatorMode.CONTEXTMODEL_UPDATE, false);
	}

	boolean updateContextModel(String scriptName, String scriptContent, SimulationParameters parameters) {
		return run(scriptName, scriptContent, parameters, SimulatorMode.CONTEXTMODEL_UPDATE, false);
	}

	protected boolean run(String scriptName, String scriptContent, SimulationParameters parameters, SimulatorMode mode,
			boolean updateRecencyModel) {

		checkParameters(scriptName, scriptContent, parameters);

		ScriptReaderLexer scanner;
		scanner = new ScriptReaderLexer(new StringReader(scriptContent));
		ScriptReader reader = new ScriptReader(scanner);
		reader.setParameters(parameters);
		Set<String> initialParameterizedFeatures = new HashSet<String>();
		if (parameters != null) {
			initialParameterizedFeatures.addAll(parameters.getAllFeatures());
		}

		reader.setMiniOntology(contextModel.getMiniOntology());
		reader.setSimulator(this);
		contextModel.getMiniOntology().initRecentlyAccessedIndividuals();
		lastExecutedScriptName = scriptName;

		try {
			reader.run(scriptName, mode);
		}
		catch (SimulatorException se) {
			throw new SimulatorException("Caught a SimulatorException while running " + scriptName, se); // FUTURE: bad
																																		// form to
																																		// re-throw an
																																		// exception, but
																																		// what can I do.
		}
		catch (Exception e) {
			throw new SimulatorException("Terminal Error: Cannot execute script " + scriptName, e);
		}

		logger.finer(scriptName + " --> " + (reader.isSuccessful() ? "success!" : "failed"));
		return reader.isSuccessful();
	}

	protected void checkParameters(String scriptName, String scriptContent, SimulationParameters parameters) {
		if (parameters == null || scriptName.equals(INSTANCE_SCRIPT_NAME))
			return;

		Pattern p = Pattern.compile("\\b\\_([0-9A-Za-z][0-9a-zA-Z\\_\\-]*)");
		Matcher m = p.matcher(scriptContent);
		Set<String> variables = new HashSet<String>();
		while (m.find()) {
			variables.add(m.group(1));
		}
		for (String suppliedParam : parameters.getAllFeatures()) {
			if (!variables.contains(suppliedParam)) {
				throw new SimulatorException("There must be a typo: " + suppliedParam + " is not a variable in the script "
						+ scriptName);
			}
		}
	}

	/**
	 * Given a list of the particpants (currently specified by the 3-letter abbreviation such as MOT, CHI), this methods
	 * runs the corresponding <code>Script</code> to add the participants to the <code>ContextModel</code>.
	 * 
	 * @param participants
	 * @return
	 */
	public boolean initializeParticipants(List<String> participants) {
		boolean success = true;

		List<String> instantiatedParticipants = new ArrayList<String>();

		String child = localizer.getParticipantLocalization(Participant.Child);
		String mother = localizer.getParticipantLocalization(Participant.Mother);
		String father = localizer.getParticipantLocalization(Participant.Father);
		String investigator = localizer.getParticipantLocalization(Participant.Investigator);
		String granny = localizer.getParticipantLocalization(Participant.Granny);
		String uncle = localizer.getParticipantLocalization(Participant.Uncle);

		if (participants.contains(child)) {
			instantiatedParticipants.add(child);
			success &= updateContextModel(child, null);

			if (participants.contains(mother)) {
				instantiatedParticipants.add(mother);
				success &= updateContextModel(mother, null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Mother_Child), null);
			}
			if (participants.contains(father)) {
				instantiatedParticipants.add(father);
				success &= updateContextModel(father, null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Father_Child), null);
			}
			if (participants.contains(mother) && participants.contains(father)) {
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Parents), null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Parents_and_Child), null);
			}

			if (participants.contains(investigator)) {
				instantiatedParticipants.add(investigator);
				updateContextModel(investigator, null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Inv_Child), null);
			}

			if (participants.contains(investigator) && participants.contains(mother)) {
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Mot_Inv), null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Mot_Chi_Inv), null);
			}

			if (participants.contains(investigator) && participants.contains(father)) {
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Fat_Inv), null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Fat_Chi_Inv), null);
			}

			if (participants.contains(investigator) && participants.contains(mother) && participants.contains(father)) {
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Parents_Inv), null);
				success &= updateContextModel(localizer.getParticipantLocalization(Participant.Parents_Chi_Inv), null);
			}
		}

		if (participants.contains(granny)) {
			instantiatedParticipants.add(granny);
			success &= updateContextModel(granny, null);
		}

		if (participants.contains(uncle)) {
			instantiatedParticipants.add(uncle);
			success &= updateContextModel(uncle, null);
		}

		contextModel.updateRecentSituationEntities(instantiatedParticipants);

		return success;
	}

	public boolean initializeSetting(List<SimpleFeatureBasedEntity> settingFeatures,
			List<SimpleFeatureBasedEntity> setupFeatures) {
		boolean success = true;
		updateContextModel("Setting", settingScript, null);
		success &= populate(settingFeatures);
		updateContextModel("Setup", setupScript, null);
		success &= populate(setupFeatures);
		return true;
	}

	protected boolean populate(List<SimpleFeatureBasedEntity> entities) {

		boolean success = true;

		for (SimpleFeatureBasedEntity entity : entities) {

			if (entity.getType().equals(ChildesConstants.ENTITY)) {
				if (entity.getID() == null || entity.getCategory() == null) {
					throw new SimulatorException("Trying to add an entity that has either an undefined type or undefined ID");
				}
				if (scripts.containsKey(entity.getCategory())) {
					SimulationParameters params = new SimulationParameters();
					params.addParameter("id", new Value(entity.getID(), false));
					success &= updateContextModel(entity.getCategory(), params);
				}
				else {
					SimulationParameters params = new SimulationParameters();
					params.addParameter("id", new Value(entity.getID(), false));
					params.addParameter("cat", new Value(entity.getCategory(), false));
					String script = instanceScriptPrefix + entity.getCategory() + instanceScriptSuffix;
					success &= updateContextModel(INSTANCE_SCRIPT_NAME, script, params);
				}
			}
			else if (entity.getType().equals(ChildesConstants.BINDING)) {
				success &= updateContextModel("Binding", bindingScript, new SimulationParameters(entity));
			}
			else {
				throw new SimulatorException("Encountered a setup instruction that is neither an entity nor a binding");
			}
		}
		return success;
	}

	public ContextModel getContextModel() {
		return contextModel;
	}

	public void setContextModel(ContextModel contextModel) {
		this.contextModel = contextModel;
	}

	public void addScript(String name, String script) {
		scripts.put(name, script);
	}

	public String getNameOfLastExecutedScript() {
		return lastExecutedScriptName;
	}

	public List<String> getRecentlyAccessedIndividuals() {
		return recentlyAccessedIndividuals;
	}

	public static void main(String[] argv) {
		try {
			Simulator simulator = new Simulator(new ContextModel(argv[0]), new File(argv[1]));
			// SimulationParameters params = new SimulationParameters();
			// params.addParameter("id", new Value("throw01", false));
			// params.addParameter("thrower", new Value("CHI", false));
			// params.addParameter("throwee", new Value("ball", false));

			simulator.updateContextModel("CHI", null);
			System.out.println(simulator.getContextModel().getMiniOntology());
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new SimulatorException("Terminal Error");
		}
	}

}

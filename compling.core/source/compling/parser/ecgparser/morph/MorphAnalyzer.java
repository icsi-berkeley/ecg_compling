package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.morph.MGrammarChecker;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.FeatureStructureUtilities;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.morph.MGrammarWrapper.LexicalConstructions;
import compling.util.StringUtilities;

public class MorphAnalyzer {
	private MGrammarWrapper grammar;

	public final MAnalysis unknownAnalysis; // TODO: Do we need to clone this? I think not.

	/**
	 * Stores the parse charts from the last run of the morphological analyzer so they can be displayed for debugging
	 * purposes.
	 */
	public static List<MParseChart> glCharts = null;

	/**
	 * Runs the morphological grammar checker and initializes the analyzer object with the provided grammar.
	 * 
	 * @param grammar
	 * @throws GrammarException
	 *            If an error occurred in the morphological grammar checker. Warnings will be suppressed&mdash;use the
	 *            other constructor to capture warnings.
	 */
	public MorphAnalyzer(MGrammarWrapper gmr) {
		this(gmr, new StringBuffer()); // Suppress warnings
	}

	/**
	 * Runs the morphological grammar checker and initializes the analyzer object with the provided grammar.
	 * 
	 * @param grammar
	 * @param warningLog
	 *           Holds any warnings issued by the morphological grammar checker.
	 * @throws GrammarException
	 *            If an error occurred in the morphological grammar checker.
	 */
	public MorphAnalyzer(MGrammarWrapper grammar, StringBuffer warningLog) {
		this.grammar = grammar;
		StringBuffer errorLog = grammar.errorLog;
		errorLog.append("\n");
		boolean success = MGrammarChecker.checkGrammar(grammar);
		if (!success) {
			throw new GrammarException(
					"The constructions and schemas in your grammar had these morphological errors:"
							+ "\n-----------------------------------------------------------------------------------------------------\n"
							+ errorLog.toString());
		}
		else if (errorLog.length() > 1) {
			warningLog.append(errorLog.toString());
		}

		// TODO: handle unknown words
		unknownAnalysis = null;
		// unknownAnalysis = new
		// MAnalysis(grammar.getLexicalConstruction(StringUtilities.addQuotes(ECGConstants.UNKNOWN_ITEM)).get(0),
		// grammar);
	}

	/**
	 * Given a grammar, return all possible morphological ECG analyses spanning the entire input word.
	 * 
	 * @param inputWord
	 *           Word to be analyzed
	 */
	public Set<MAnalysis> analyze(final String inputWord) {
		Set<MAnalysis> analyses = new HashSet<MAnalysis>();

		boolean complexMorph = true; // Whether or not to look for complex morphological analyses

		// Look for lexical cxns with this form
		if (grammar.hasLexicalConstruction(inputWord)) {
			LexicalConstructions lexConstructions = grammar.getLexicalConstructions(inputWord);
			Set<MAnalysis> lexicalAnalyses = new HashSet<MAnalysis>();
			for (Construction c : lexConstructions.getConstructions()) {
				try {
					if (c.getCxnTypeSystem().subtype(c.getName(), grammar.getConstruction(ECGConstants.MWORDTYPE).getName())) { // Only
																																									// return
																																									// analyses
																																									// that
																																									// can
																																									// be
																																									// words
						MAnalysis lexicalAnalysis = new MAnalysis(c, grammar);
						lexicalAnalysis.setSpanLeftCharIndex(0);
						lexicalAnalysis.setSpanRightCharIndex(inputWord.length());
						setScore(lexicalAnalysis, null, null, inputWord);
						lexicalAnalyses.add(lexicalAnalysis);
					}
				}
				catch (TypeSystemException ex) {
					throw new ParserException("TypeSystemException encountered for construction " + c.getName() + ": "
							+ ex.getMessage());
				}
			}
			analyses.addAll(lexicalAnalyses);
			complexMorph = lexConstructions.getMorphAmbiguous();
		}

		if (complexMorph) {
			Set<MAnalysis> completeAnalyses = analyze(ECGConstants.MROOTTYPE, inputWord);
			analyses.addAll(completeAnalyses);
		}

		return analyses;
	}

	/**
	 * Return all possible morphological analyses spanning the entire input and rooted with <code>rootCxn</code>.
	 * 
	 * @param rootCxn
	 *           Construction type for the analysis root. All concrete subtypes of this type will be considered as heads
	 *           of the resulting analysis.
	 * @param inputWord
	 *           Word to be analyzed.
	 * @return
	 */
	public Set<MAnalysis> analyze(final String rootCxn, final String inputWord) {
		/*
		 * Analysis an = new Analysis(grammar.getConstruction("Cxn2"), grammar); Analysis an2 = new
		 * Analysis(grammar.getConstruction("Cxn1"), grammar); Role r =
		 * grammar.findRole(grammar.getConstruction("Cxn2").getAllRoles(), "c"); an.omitOptional(r);
		 * System.out.println("omitted optional"); System.out.println(an.getFeatureStructure()); //System.out.println(an);
		 * an.advance(r, an2); System.out.println(an.getFeatureStructure()); //System.out.println(an); if (true) return
		 * null;
		 */

		/*
		 * Analysis suban = new Analysis(grammar.getConstruction("XX")); Analysis x1a = new
		 * Analysis(grammar.getConstruction("X1")); x1a.setSpanLeftCharIndex(0); x1a.setSpanRightCharIndex(1); Analysis
		 * x1b = new Analysis(grammar.getConstruction("X1")); x1b.setSpanLeftCharIndex(2); x1b.setSpanRightCharIndex(3);
		 * //Analysis x2 = new Analysis(grammar.getConstruction("X2")); //x2.setSpanLeftCharIndex(1);
		 * //x2.setSpanRightCharIndex(2);
		 * 
		 * Role rl = suban.getLeftOverConstituents().iterator().next(); System.out.println(rl.toString() + ") " +
		 * suban.advance(rl, x1a)); //rl = suban.getLeftOverConstituents().iterator().next();
		 * System.out.println(rl.toString() + ") " + suban.advance(rl, x1b)); System.out.println(suban);
		 * System.out.println(suban.getFeatureStructure()); if (true) return null;
		 * 
		 * Analysis an = new Analysis(grammar.getConstruction("XX")); rl = an.getLeftOverConstituents().iterator().next();
		 * System.out.println(rl.toString() + ") " + an.advance(rl, suban)); rl =
		 * an.getLeftOverConstituents().iterator().next(); System.out.println(rl.toString() + ") " + an.advance(rl, x1b));
		 * System.out.println(an); System.out.println(an.getFeatureStructure());
		 * System.out.println(an.getFeatureStructure().getSlot(new ECGSlotChain("a.b.f.orth")).getAtom()); if (true)
		 * return null;
		 */

		Set<MAnalysis> analyses = new HashSet<MAnalysis>();
		List<MParseChart> charts = new ArrayList<MParseChart>(inputWord.length());
		for (int i = 0; i <= inputWord.length(); i++)
			charts.add(new MParseChart());

		// Initialize charts[0] to contain start rule(s)
		addRulesForType(null, null, rootCxn, charts.get(0), false, null, inputWord);

		analyze(rootCxn, inputWord, charts);

		// Candidates in chart--those which are headed by a subtype of rootCxn
		Set<MParseState> candidates = new HashSet<MParseState>();
		MParseChart ch = charts.get(inputWord.length());
		for (String rootType : grammar.getConcreteSubtypes(rootCxn)) {
			if (ch.hasLHS(rootType))
				candidates.addAll(ch.getByLHS(rootType));
		}
		if (candidates != null) {
			for (MParseState candidate : candidates) {
				if (candidate.getSpanLeftCharIndex() == 0 && candidate.getSpanRightCharIndex() == inputWord.length()) {
					Set<MAnalysis> ases = buildAnalysis(candidate, inputWord);
					for (MAnalysis a : ases)
						analyses.add(a.getLastAdvanced());
//					showAnalyses(a);
				}
			}
		}
		glCharts = charts;

		for (MAnalysis a : analyses) {
			a.completed();
			a.setLexicalRHSFound(); // TODO: Temporary
		}

		return analyses;
	}

	/**
	 * Parse the input, starting with <code>rootCxn</code>, and storing parser states in <code>charts</code>.
	 * 
	 * @param rootCxn
	 *           Construction type for the analysis root.
	 * @param inputWord
	 *           Word to be analyzed.
	 * @param charts
	 *           List of morphological parse charts. The <i>i</i>th chart in the list will hold parser states ending
	 *           after the <i>i</i>th character of the input.
	 */
	public void analyze(final String rootCxn, final String inputWord, final List<MParseChart> charts) {
		final int L = inputWord.length();
		for (int i = 0; i <= L; i++) {
			int h = -1; // Rule offset in the chart
			while (++h < charts.get(i).numStates()) {
				MParseState state = charts.get(i).getByOffset(h);
				if (!state.isComplete()) {
					predictorScanner(state, inputWord, charts);
				}
				else {
					completer(state, inputWord, charts);
				}
			}
		}
	}

	/**
	 * Create a copy of the current parser state, and add to the appropriate chart all possible expansions of that form
	 * component. Equivalently: If the form component is a known string literal, check that it is matched in the input
	 * and advance to the next component. If, however, the form component refers to a constituent, prepare to consider
	 * all possible fillers of that constituent.
	 * 
	 * @param predictFrom
	 *           The current parser state.
	 * @param input
	 *           Input string under analysis.
	 * @param charts
	 *           All parse charts.
	 */
	private void predictorScanner(final MParseState predictFrom, final String input, final List<MParseChart> charts) {

		// Given: X -> alpha * Y beta (i,j) -- predictFrom
		// & grammar contains Y -> gamma1, Y -> gamma2
		// Add to chart[j] { Y -> * gamma1 (j,j), Y -> * gamma2 (j,j) }

		ECGSlotChain nextSymbol = predictFrom.getNextRHSSymbol();

		FormComponent valueOrType = getValueOrType(predictFrom, nextSymbol, input, true);
		Role constitRole = valueOrType.getConstit();

		if (!valueOrType.hasValue) { // Type of an unfilled constituent
			String constitType = valueOrType.getConstitType();

			// boolean emptyRHS = predictFrom.hasFormRoleAppearance(constitRole);

			// Is this a form role appearance?
			boolean emptyRHS = nextSymbol.getChain().size() > 2 /* && (!predictFrom.nonadjacentUsage / TODO: ?? /) */;

			addRulesForType(predictFrom, constitRole, constitType, charts.get(predictFrom.getSpanRightCharIndex()),
					emptyRHS, nextSymbol, input);

			// for each potential noncontiguous constit of the form Z[z1="s1", z2="s2", ...]
			// if this cxn is Y -> z1 gamma1 ...
			// add Y -> * Z.z1 gamma1
		}
		else { // A literal value
			String literalValue = valueOrType.value;
			// Check that it matches input
			if (input.substring(predictFrom.getSpanRightCharIndex()).startsWith(literalValue)) {
				// Copy state and add it to a subsequent chart with position advanced
				MParseState newState = predictFrom.clone();
				newState.position++;
				newState.scanees = new ArrayList<MParseState>();
				newState.setSpanRightCharIndex(predictFrom.getSpanRightCharIndex() + literalValue.length());
				newState = charts.get(predictFrom.getSpanRightCharIndex() + literalValue.length()).addState(newState);
				predictFrom.scanees.add(newState);
			}
		}
	}

	/**
	 * Adds parser states (productions) for a constituent of the current state.
	 * 
	 * @param predictFrom
	 *           The current parser state.
	 * @param role
	 *           Role corresponding to the constituent to expand within in the head construction of the current state.
	 * @param type
	 *           Construction type for the new state. The function will recursively add states for all concrete subtypes
	 *           as well.
	 * @param chart
	 *           Parse chart in which to add the new states.
	 * @param emptyRHS
	 *           If true, new states will not contain form components on the right-hand side--they will be epsilon
	 *           productions (to facilitate parsing of noncontiguous constructions and form role appearances).
	 * @return Whether new states have been added (a state won't be added if an equivalent one is already in the chart)
	 */
	public boolean addRulesForType(final MParseState predictFrom, final Role role, final String type,
			final MParseChart chart, final boolean emptyRHS, ECGSlotChain fc, String input) {
		// If constituent is optional, add an epsilon production.
		//
		if (predictFrom != null && predictFrom.leftOverOptionals()
				&& predictFrom.getHeadCxn().getOptionals().contains(role)
		// && (predictFrom.isFirstAppearance(role, fc) || predictFrom.isFirstAppearanceOmitted(role))
		) {
			MParseState newEpsilonRule = new MParseState(grammar.getConstruction(type), true, grammar);
			newEpsilonRule.position = 0;

			if (predictFrom != null) { // Will be null on the root cxn
				newEpsilonRule.setSpanLeftCharIndex(predictFrom.getSpanRightCharIndex());
				newEpsilonRule.setSpanRightCharIndex(predictFrom.getSpanRightCharIndex());
			}

			newEpsilonRule.noncontig = false;
			newEpsilonRule.omitted = true;

			chart.addState(newEpsilonRule);

			// predictFrom.omitOptional(role); // TODO: ?????
			// if (chart.addState(newEpsilonRule)!=newEpsilonRule)
			// return false; // Equivalent state already exists
			// but we still may need to add the production below in case the epsilon rule already exists from another
			// utilizer but not the non-epsilon rule
			// the other utilizer could have been a non-adjacent utilizer, in which case the regular non-epsilon rule
			// wouldn't have been added
		}

		if (grammar.getConstruction(type).isConcrete()) {
			MParseState newRule = new MParseState(grammar.getConstruction(type), emptyRHS, grammar);
			newRule.position = 0;

			if (predictFrom != null) { // Will be null on the root cxn
				newRule.setSpanLeftCharIndex(predictFrom.getSpanRightCharIndex());
				newRule.setSpanRightCharIndex(predictFrom.getSpanRightCharIndex());
			}

			if (!grammar.isContiguous(type)) {
				newRule.noncontig = true;
			}

			boolean valid = true;

			if (role != null && predictFrom.hasMultipleNonadjacentAppearances(role)) { // the current utilizer refers to
																												// form roles of c in a nonadjacent
																												// fashion
				newRule.nonadjacentUsage = true;
				if (fc != null && fc.getChain().size() > 2 /* exclude form pole appearances */) {

					ECGSlotChain rfc = new ECGSlotChain("self." + fc.subChain(1).toString());
					FormComponent valueOrType = getValueOrType(newRule, rfc, input, true);
					if (valueOrType.hasValue) {
						String s = input.substring(newRule.getSpanLeftCharIndex());
						String fcv = valueOrType.value;
						if (s.length() >= fcv.length() && s.startsWith(fcv)) {
							newRule.setSpanRightCharIndex(newRule.getSpanLeftCharIndex() + fcv.length());
						}
						else
							valid = false;
					}
				}
			}

			if (valid && chart.addState(newRule) != newRule)
				return false; // Equivalent state already exists
		}
		List<String> subtypes = grammar.getConcreteSubtypes(type);
		for (String subtype : subtypes) {
			if (!subtype.equals(type))
				addRulesForType(predictFrom, role, subtype, chart, emptyRHS, fc, input); // Ignore noncontiguous or form
																													// role appearance descendants
		}
		return true;
	}

	/**
	 * Having completed a parser state, identify other states who can incorporate this one, and add an advanced copies of
	 * those states in the appropriate chart. Equivalently: Having matched a construction instance, identify potential
	 * utilizing constructs, and prepare to look for the next constituent filler in each of those constructs.
	 * 
	 * @param completeFrom
	 *           A complete parser state.
	 * @param input
	 *           Input string under analysis.
	 * @param charts
	 *           All parse charts.
	 */
	private void completer(final MParseState completeFrom, final String input, final List<MParseChart> charts) {

		// Given: Y -> gamma * (j,k)
		// For each X -> alpha * Y beta (i,j) in charts[j]
		// Add X -> alpha Y * beta (i,k) to charts[k]

		int h = -1; // Rule offset within chart
		while (++h < charts.get(completeFrom.getSpanLeftCharIndex()).numStates()) {
			MParseState state = charts.get(completeFrom.getSpanLeftCharIndex()).getByOffset(h);
			if (state.isComplete())
				continue;

			ECGSlotChain nextSymbol = state.getNextRHSSymbol();
			FormComponent valueOrType = getValueOrType(state, nextSymbol, input, true);

			if (!valueOrType.hasValue) { // Type of an unfilled constituent
				String constitType = valueOrType.getConstitType();
				Role constitRole = valueOrType.getConstit();
				MParseState cfCopy = completeFrom;

				/*
				 * // Clone noncontiguous states. In general, the economy of a chart parser comes from states that refer to
				 * others in the chart). // However, after-the-fact modifications to the right character index of a
				 * noncontiguous constituent would be performed excessively // if the parse state reference was shared by
				 * two utilizing constructs. See note in getValueOrType(). // Note that noncontiguous states cannot have
				 * parent states (constituents), so this shouldn't mess anything up. if (completeFrom.noncontig) cfCopy =
				 * completeFrom.clone(); else cfCopy = completeFrom;
				 */

				if (constitType.equals(cfCopy.getHeadType())
						|| grammar.getConcreteSubtypes(constitType).contains(cfCopy.getHeadType())) {
					// Copy state and add it to a subsequent chart with position advanced
					MParseState newState = state.clone();
					newState.position++;
					newState.scanees = new ArrayList<MParseState>();

					if (!cfCopy.omitted && (cfCopy.noncontig || newState.hasFormRoleAppearance(constitRole))) {
						newState.filledConstits.put(constitRole, cfCopy);
					}
					if (!cfCopy.omitted
							&& (cfCopy.noncontig /* || (cfCopy.nonadjacentUsage /TODO ??/) */|| nextSymbol.getChain().size() > 2 /*
																																									 * WAS
																																									 * :
																																									 * newState
																																									 * .
																																									 * hasFormRoleAppearance
																																									 * (
																																									 * constitRole
																																									 * )
																																									 */)) {

						FormComponent valueOrType2 = getValueOrType(newState, nextSymbol, input, true);
						String fcv = valueOrType2.value;

						if (fcv == null) {
							System.out.println(cfCopy.getFeatureStructure());
							valueOrType2 = getValueOrType(newState, nextSymbol, input, true); // for debugging TODO
						}

						if (input.length() < (newState.getSpanRightCharIndex() + fcv.length())
								|| !input.substring(newState.getSpanRightCharIndex(),
										newState.getSpanRightCharIndex() + fcv.length()).equals(fcv))
							continue;

						// v noncontig,optional,nonadjacUsage
						// v form role reference where cfCopy is a form pole state
						if (cfCopy.isEmptyRHS() || nextSymbol.getChain().size() > 2) { // TODO: emptyRHS test + else clause
							newState.setSpanRightCharIndex(newState.getSpanRightCharIndex() + fcv.length());
							if (!cfCopy.isEmptyRHS()) {
//								System.out.println(" ~ Wouldn't have been here for: " + cfCopy + " new right index=" + newState.getSpanRightCharIndex());
								continue;
							}
						}
						else
							newState.setSpanRightCharIndex(cfCopy.getSpanRightCharIndex());

						int oldlocation = cfCopy.getSpanRightCharIndex();
						if (newState.getSpanRightCharIndex() != oldlocation) {
							charts.get(oldlocation).removeState(cfCopy);
						}
						cfCopy.setSpanRightCharIndex(newState.getSpanRightCharIndex()); // If there are multiple appearances,
																												// this will be the right index of
																												// the FIRST appearance
						// TODO: I think the above line creates an incongruity with the right index and the chart location.
						// We try moving the state to the proper chart location as a remedy.
//						if (newState.getSpanRightCharIndex()!=oldlocation)
//							System.err.println(cfCopy + " right index changed from " + oldlocation);
						cfCopy = charts.get(cfCopy.getSpanRightCharIndex()).addState(cfCopy);
					}
					else
						newState.setSpanRightCharIndex(cfCopy.getSpanRightCharIndex());

					MParseState newOrExistingState = charts.get(newState.getSpanRightCharIndex()).addState(newState, true);

					newOrExistingState.addParentState(cfCopy);
					if (newOrExistingState != newState) { // An equivalent state already existed in the chart
						Map<Integer, Set<MParseState>> nps = new HashMap<Integer, Set<MParseState>>();
						for (Map.Entry<Integer, Set<MParseState>> e : newOrExistingState.parentStates.entrySet()) {
							nps.put(e.getKey(), new HashSet<MParseState>(e.getValue()));
						}

						newOrExistingState.addParentStates(newState.parentStates);
						// update all scanees, i.e. states that were produced by scanning on 'newOrExistingState'
						for (MParseState scanee : newOrExistingState.scanees) {
							scanee.addParentStates(newOrExistingState.parentStates);
							charts.get(scanee.getSpanRightCharIndex()).addState(scanee, true);
						}

						newState.addParentStates(nps);
					}

					state.scanees.add(newOrExistingState);
				}
			}
		}
	}

	/**
	 * Given a form component slot chain and a morphological parser state, determine the string value for the form
	 * component if possible; otherwise, return the type of the constituent that the form component refers to (so the
	 * parsing algorithm knows to augment the analysis with a filler for that constituent).
	 * 
	 * Assumes no deep appearances--i.e. no dotting into subconstituents.
	 * 
	 * @param predictFrom
	 *           The current parser state.
	 * @param fc
	 *           The slot chain for the form component.
	 * @param input
	 *           The input string being analyzed.
	 * @return A FormComponent instance containing: the ECG string literal value if hasValue is true; or the constituent
	 *         role and type if hasValue is false.
	 */
	private FormComponent getValueOrType(final MParseState predictFrom, final ECGSlotChain fc, final String input,
			final boolean inAnalysis) {

		if (fc.startsWithSelf()) {
			// Get value of fc
			Slot slot = predictFrom.getFeatureStructure().getSlot(fc);
			String fcv = null;

			if (slot.hasAtomicFiller()) {
				fcv = StringUtilities.removeQuotes(slot.getAtom());
			}
			else {
				// Refers to some constituent.f
				// Find the span for the constituent and get its form from the input
				// A bit of a hack, since we can't go directly to the feature structure for complex

				fcv = input.substring(predictFrom.getSpanLeftCharIndex(), predictFrom.getSpanRightCharIndex());
			}

			final Role constitRole = null; // A self-reference
			String bestKnownType = ((slot.getTypeConstraint() != null) ? slot.getTypeConstraint().getType() : null);
			return new FormComponent(bestKnownType, constitRole, fcv);
		}
		else { // constit.f or constit.f.r
			Role constitRoleInFC = fc.getChain().get(0);
			// The constituent Role instance is NOT the same as the Role instance in the slot chain!
			Role constitRole = MGrammarWrapper.findRole(predictFrom.getHeadCxn().getConstituents(), constitRoleInFC);

			String constitType = constitRole.getTypeConstraint().getType();

			// Verify that there is not a deep appearance (constituent reference)
			if (!fc.getChain().get(1).getName().equals(ECGConstants.FORM_POLE)) {
				// A deep constituent reference! This should never happen.
				throw new ParserException("Morphological parsing error: Deep constituent reference " + fc
						+ " in construction " + predictFrom.getHeadCxn());
			}

			if (predictFrom.filledConstits.containsKey(constitRole)) { // e.g. b.f, b.f.x where b is known
				MParseState constitState = predictFrom.filledConstits.get(constitRole);

				constitState.getFeatureStructure();

				if (constitState.getFeatureStructure().hasSlot(constitState.getFeatureStructure().getMainRoot(),
						fc.subChain(1))) {
					Slot slot = constitState.getFeatureStructure().getSlot(fc.subChain(1));

					if (slot.hasAtomicFiller()) {
						String fcv = StringUtilities.removeQuotes(slot.getAtom());
						/*
						 * if (inAnalysis && constitState.noncontig && predictFrom.filledConstits.containsKey(constitRole) &&
						 * predictFrom.hasMultipleAppearances(constitRole) &&
						 * !predictFrom.hasMultipleNonadjacentAppearances(constitRole)) { // If a noncontiguous constituent
						 * has one or more appearances which are all adjacent, we want the right character index of the span
						 * to reflect that of the last appearance. // In the case of a single appearance, the span was
						 * determinable as soon as that form component was processed. // Here there are multiple adjacent
						 * appearances, and so we update the right character index of the span to reflect the last observed
						 * one (so far). // The parse state for the noncontiguous constituent is unique, not a shared
						 * reference--otherwise this could be applied excessively to an object. See note in completer().
						 * constitState.setSpanRightCharIndex(constitState.getSpanRightCharIndex() + fcv.length());
						 * System.out.print(""); }
						 */

						return new FormComponent(constitType, constitRole, fcv); // VALUE
					}
				}

				return new FormComponent(constitType, constitRole); // TYPE
			}
			else { // Constituent isn't instantiated, so return its type instead.
				Slot slot = predictFrom.getFeatureStructure().getSlot(fc.subChain(0, 1));

				// Get the slot type if possible. If a form role reference, the slot will be untyped, so return the type of
				// the constituent.
				String slotType = slot.getTypeConstraint().getType();
				String bestKnownType = ((slotType != null) ? slotType : constitRole.getTypeConstraint().getType());
				return new FormComponent(bestKnownType, constitRole); // TYPE
			}
		}
	}

	/**
	 * Given a parser state corresponding to a complete parse of the input string, build all possible morphological
	 * analyses (<code>MAnalysis</code> instances) bottom-up.
	 * 
	 * @param candidate
	 *           A parser state representing a complete parse of the input.
	 * @param input
	 *           Input string under analysis.
	 * @return All complete morphological analyses spanning the entire input.
	 */
	public Set<MAnalysis> buildAnalysis(MParseState candidate, final String input) {
		if (candidate.omitted) // Ignore parse states for omitted constituents
			return new HashSet<MAnalysis>();

		Map<Integer, Set<MAnalysis>> analyses = new HashMap<Integer, Set<MAnalysis>>();

		MAnalysis thisan = new MAnalysis(candidate.getHeadCxn(), grammar);
		setScore(thisan, null, null, input);

		List<Role> constitQueue;
		if (grammar.isLexicalConstruction(thisan.getHeadCxn().getName())) {
			constitQueue = new ArrayList<Role>();
		}
		else
			constitQueue = MParseState.buildConstitQueue(grammar.getMeetsChain(thisan.getHeadCxn().getName()),
					thisan.getHeadCxn());
		thisan.initConstitQueue(constitQueue);

		if (candidate.filledConstits.size() > 0) { // Has nonconcatenative constituents // TODO: May be others as well...
			for (Map.Entry<Role, MParseState> entry : candidate.filledConstits.entrySet()) {
				Role constitRole = entry.getKey();
				MParseState specialState = entry.getValue();
				MAnalysis specialAn = specialState.clone(); // Constituent cxn is either noncontiguous or has a form role
																			// appearance
				/*
				 * if (candidate.hasMultipleNonadjacentAppearances(entry.getKey())) { // Constituent cxn is noncontiguous,
				 * so give it the same span as its utilizing cxn
				 * specialAn.setSpanLeftCharIndex(candidate.getSpanLeftCharIndex());
				 * specialAn.setSpanRightCharIndex(candidate.getSpanRightCharIndex()); }
				 */
				if (!thisan.getLeftOverConstituents().contains(constitRole)) {
					System.err.println("not good");
				}
				if (specialState.omitted && !thisan.getLeftOverOptionals().contains(constitRole)) {
					System.err.println("bad news");
				}

				if (specialState.nonadjacentUsage || specialState.noncontig) {
					thisan.recordDisjointConstit(constitRole);
				}

				// TODO: added check because APPLEES was parsing (though the constituent failed to unify). verify this works
				// with test suite.
				if (!thisan.advance(constitRole, specialAn))
					return new HashSet<MAnalysis>(); // a constituent failed to unify
				setScore(thisan, constitRole, specialAn, input);

//				System.out.println("ADVANCED " + constitRole + " = " + specialState);
			}
		}
		thisan.setSpanLeftCharIndex(candidate.getSpanLeftCharIndex());

		analyses.put(thisan.getSpanLeftCharIndex(), new HashSet<MAnalysis>());
		analyses.get(thisan.getSpanLeftCharIndex()).add(thisan);

		List<ECGSlotChain> rhsSymbolic = candidate.rhsSymbolic;

		int iFC = -1;
		for (ECGSlotChain symbol : rhsSymbolic) {
			iFC++;
			FormComponent valueOrType = getValueOrType(candidate, symbol, input, false);
			if (!valueOrType.hasValue) { // Type of an unfilled constituent

				// Duplicate 'analyses' as 'analyses2'
				Map<Integer, Set<MAnalysis>> analyses2 = new HashMap<Integer, Set<MAnalysis>>(); // Right offset of last
																															// constituent filled so
																															// far => Analyses
				for (Map.Entry<Integer, Set<MAnalysis>> entry : analyses.entrySet()) {
					analyses2.put(entry.getKey(), new HashSet<MAnalysis>(entry.getValue()));
				}

				String constitType = valueOrType.getConstitType(); // e.g. 'Morph'
				Role constitRole = valueOrType.getConstit();

				// Decode lattice of parentStates

				Set<MParseState> parentStates = candidate.parentStates.get(iFC);

				for (MParseState pstate : parentStates) {
					if (grammar.getConcreteSubtypes(constitType).contains(pstate.getHeadType())) { // Try binding the parent
																																// analysis to this
																																// constituent

						// Somewhat confusingly, the child state is at a higher level in the analysis than its parents
						Set<MAnalysis> pans = buildAnalysis(pstate.clone(), input); // Go down to a constituent

						if (pstate.omitted) {
							if (analyses.containsKey(pstate.getSpanLeftCharIndex())) { // 'pans' is empty

								// TODO: BAH!
								Set<MAnalysis> analysesToRemove = new HashSet<MAnalysis>();
								for (MAnalysis a : analyses.get(pstate.getSpanLeftCharIndex())) {
									if (a.getNextConstit() != constitRole) {
										// continue;
									}

									MAnalysis aa = a.clone();
									boolean constitUnbound = aa.getLeftOverOptionals().contains(constitRole);

									if (!constitUnbound) {
										if (!aa.isOmittedOptional(constitRole)) {
//											System.err.println("hmm " + pstate);
											// The analysis requires contradictory fillers for a constituent (e.g. one omitted, one
											// not).
											// This is allowed only if both fillers are omissions of the same type.

											// 'continue' (ignoring the second of the two fillers) is not sufficient here--we need
											// to remove the existing candidate from consideration.
											analysesToRemove.add(a); // TODO a change in buildAnalysis() has hopefully removed
																				// (heh) the need for this
										}
										else
											aa.setSpanRightCharIndex(pstate.getSpanRightCharIndex()); // TODO ??? doesn't seem to
																															// make a difference
										continue;
									}

									if (!candidate.isFirstAppearance(constitRole, symbol)) {
										// We've already added an MAnalysis in which this constituent is marked as optional.
										continue;
									}

									if (aa.omitOptional(constitRole)) {
										if (!analyses2.containsKey(pstate.getSpanRightCharIndex()))
											analyses2.put(pstate.getSpanRightCharIndex(), new HashSet<MAnalysis>());

										aa.setSpanRightCharIndex(pstate.getSpanRightCharIndex());
										analyses2.get(pstate.getSpanRightCharIndex()).add(aa);
									}

									analysesToRemove.add(a); // TODO: ??
								}
								if (analysesToRemove.size() > 0)
//									System.err.println("removing " + analysesToRemove.size() + " analyses");
									for (MAnalysis a : analysesToRemove) {
										analyses2.get(pstate.getSpanLeftCharIndex()).remove(a);
									}
							}

						}
						else {
							for (MAnalysis pan : pans) {
								if (!analyses.containsKey(pan.getSpanLeftCharIndex()))
									continue;

								for (MAnalysis a : analyses.get(pan.getSpanLeftCharIndex())) {
									if (a.getNextConstit() != constitRole) {
										// continue;
									}

									MAnalysis aa = a.clone();
									boolean constitUnbound = aa.getLeftOverConstituents().contains(constitRole);

									// Continue if this is not the first appearance of 'constitRole' in aa's head cxn.
									// (If the constituent is optional, two analyses with the same right offset
									// will be present: one with the constituent marked as omitted, and one without.
									// Since an appearance must evaluate to non-empty unless the constituent is omitted,
									// and all omitted constituents' appearances must be empty, we ignore the case where
									// no constituent filler or omission has been registered for a previous corresponding
									// appearance.
									if (!constitUnbound && !candidate.isFirstAppearance(constitRole, symbol)
											&& !aa.isDisjointConstit(constitRole)) {
										continue;
									}

									if (!constitUnbound && !candidate.isFirstAppearance(constitRole, symbol)
											&& input.equals("xac")) {
										continue;
									}

									if (pstate.noncontig || pstate.nonadjacentUsage) {
										aa.recordDisjointConstit(constitRole);
									}

									if (!constitUnbound
											|| (aa.advance(constitRole, pan) && setScore(aa, constitRole, pan, input))) {
//										System.out.println("advanced " + constitRole + " = " + pstate);

										if (!analyses2.containsKey(pan.getSpanRightCharIndex()))
											analyses2.put(pan.getSpanRightCharIndex(), new HashSet<MAnalysis>());

										aa.setSpanRightCharIndex(pan.getSpanRightCharIndex());
										analyses2.get(pan.getSpanRightCharIndex()).add(aa);
									}
									else if (constitUnbound) {
//										System.err.println("Failed Advance " + constitRole + " = " + pstate);
									}
								}
							}
						}
					}
				}
				analyses = analyses2;
			}
			else { // A string literal--advance ending right offset of all previous analyses by its length
				String fcValue = valueOrType.value;
				Map<Integer, Set<MAnalysis>> analyses2 = new HashMap<Integer, Set<MAnalysis>>();
				for (Map.Entry<Integer, Set<MAnalysis>> entry : analyses.entrySet()) {
					for (MAnalysis an : entry.getValue()) {
						int oldRightChar = an.getSpanRightCharIndex();
						an.setSpanRightCharIndex(entry.getKey() + fcValue.length());
						int newRightChar = an.getSpanRightCharIndex();
						List<CxnalSpan> spans = an.getSpans();
						if (spans == null)
							spans = new ArrayList<CxnalSpan>();
						for (CxnalSpan sp : spans) {
							if (sp.getLeftChar() < newRightChar && sp.getRightChar() > newRightChar) {
								continue;
							}
						}

//						if (constitRole!=null && an.getLeftOverConstituents().contains(constitRole)) {
						// We know the value through magical means (e.g. a constraint), but we still need to advance the
						// constituent
						// TODO: Is this right???? Do we need to consider all subtypes, or is this the dynamic type and thus
						// definitive?
//							String t = an.getFeatureStructure().getSlot(new ECGSlotChain(constitRole)).getTypeConstraint().getType();
//							an.advance(constitRole, new Analysis(grammar.getConstruction(t)));

//						}
					}
					analyses2.put(entry.getKey() + fcValue.length(), entry.getValue());
				}
				analyses = analyses2;
			}
		}

		if (!analyses.containsKey(candidate.getSpanRightCharIndex()))
			return new HashSet<MAnalysis>();

		Set<MAnalysis> allAnalyses = new HashSet<MAnalysis>();
		// Only return analyses that have all complements (non-optional constituents) filled.
		// (Epsilon constructions can lead the algorithm to produce analyses with unfilled complements.)
		for (MAnalysis a : analyses.get(candidate.getSpanRightCharIndex())) {
			if (a.leftOverOptionals()) {
				continue;
			}

			if ((a.getLeftOverComplements() == null || a.getLeftOverComplements().size() == 0)) {
				a.setCompleted(true);
				allAnalyses.add(a);
			}
		}
		return allAnalyses;

		// If a constituent is optional, there will be at least two states in the chart:
		// - the one prior to processing of the constituent, and
		// - the one generated upon marking the constituent as omitted
		// These will both share the same right index. We want to discard the first,
		// so every constituent that has been processed is always either filled or
		// explicitly marked as omitted.
		/*
		 * if (a.leftOverOptionals()) continue;
		 */
	}

	/**
	 * Display a set of morphological analyses in the console. If <code>anFormatter</code> and <code>fsFormatter</code>
	 * are both <code>null</code>, simply displays the number of analyses.
	 * 
	 * @param analyses
	 * @param inputWord
	 * @param anFormatter
	 *           An analyses formatter, or <code>null</code> if the analysis should not be rendered
	 * @param fsFormatter
	 *           A feature structure formatter, or <code>null</code> if the feature structure should not be displayed
	 */
	public void showAnalyses(Set<MAnalysis> analyses, String inputWord, AnalysisUtilities.AnalysisFormatter anFormatter,
			FeatureStructureUtilities.FeatureStructureFormatter fsFormatter) {
		if (anFormatter != null)
			MAnalysis.setFormatter(anFormatter);

		if (analyses.size() == 0)
			System.out.println("No analysis found.");
		else if (anFormatter != null || fsFormatter != null) {
			for (Analysis analysis : analyses) {
				if (anFormatter != null)
					System.out.println(analysis);
				if (fsFormatter != null)
					System.out.println(fsFormatter.format(analysis.getFeatureStructure()));

				System.out.println("-------------------------------------");
			}
		}
		else
			System.out.println("Number of analyses: " + analyses.size());
	}

	/**
	 * Assigns a score to a specified composite analysis upon advancing a constituent, or to a leaf construction (where
	 * 'constitRole' and 'filler' are null). Always returns true. Right now the MAnalysisScorer is invoked; if it returns
	 * -Infinity, the filler analysis's score is used. This can be modified to a more sophisticated scoring system.
	 */
	private boolean setScore(MAnalysis complex, Role constitRole, MAnalysis filler, String inputWord) {
		double newScore = MAnalysis.scorer.score(complex, inputWord);
		if (filler != null && (Double.isInfinite(newScore) || Double.isNaN(newScore))) {
			double fillerScore = filler.getScore();
			if (!Double.isInfinite(fillerScore) && !Double.isNaN(fillerScore)) { // Recursively use the constituent's
																										// score, if it has one
				complex.score = fillerScore;
				return true;
			}
		}
		complex.score = newScore;
		return true;
	}
}

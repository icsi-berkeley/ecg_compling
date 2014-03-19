package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;

/***
 * Extension of the Analysis class to accommodate analysis at the morphological level. Introduces the notion of left and
 * right character indices within the word, and adds form features to the feature structure which are left out at the
 * phrasal level.
 * 
 * @author Nathan Schneider
 * 
 */
public class MAnalysis extends Analysis {

	public void setLexicalRHSFound() {
		lexicalRHS = FOUND;
	}

	public void setCompleted(boolean v) {
		completed = v;
	}

	public boolean completed() {
		return completed;
	}

	private boolean completed = false;

	/**
	 * The last MAnalysis to have been advanced as a constituent of this analysis
	 */
	private MAnalysis lastAdvanced = null;

	int leftCharIndex = 0;
	int rightCharIndex = 0;

	protected double score = Double.NEGATIVE_INFINITY;
	static MAnalysisScorer scorer;

	public static int globalUniqueSlotId = 0;

	Set<Role> omittedOptionals = new HashSet<Role>();
	private List<Role> constituentQueue = null;
	private int curConstitOffset;
	private List<Role> disjointConstits = new ArrayList<Role>(); // TODO: convert to HashSet?

	public MAnalysis(Construction headCxn, MGrammarWrapper grammar) {
		super(headCxn, grammar);
		// MAnalysis.formatter = new MorphAnalysisFormatter();
	}

	public void initConstitQueue(List<Role> constitQueue) {
		constituentQueue = constitQueue;
		curConstitOffset = 0; // Points to the next constituent (in the queue) that is to be advanced/omitted
	}

	protected boolean addLocalStructure(GrammarWrapper grammar) {
		super.addLocalStructure(grammar);

		// Add form-related features in the feature structure

		// .f
		Role f = new Role("f");
		if (headCxn.getFormBlock().getType() != ECGConstants.UNTYPED) {
			// System.out.println(headCxn.getMeaningBlock().getType());
			f.setTypeConstraint(headCxn.getFormBlock().getBlockTypeTypeSystem()
					.getCanonicalTypeConstraint(headCxn.getFormBlock().getType()));
		}

		if (featureStructure.addSlot(new ECGSlotChain(f)) == false) {
			return false;
		}
		featureStructure.getSlot(new ECGSlotChain(f)).setRealFiller(true);

		// form block constraints
		for (Constraint constraint : headCxn.getFormBlock().getConstraints()) {
			if (!constraint.overridden()) {
				// addConstraint() doesn't handle 'before' or 'meets' constraints
				if (constraint.getOperator() != ECGConstants.MEETS && constraint.getOperator() != ECGConstants.BEFORE) {
					if (addConstraint(constraint, "") == false) {
						logger.warning("problem with " + constraint);
						return false;
					}
				}
			}
		}

		// form schema roles & constraints
		if (headCxn.getFormBlock().getType() != ECGConstants.UNTYPED
				&& headCxn.getFormBlock().getBlockTypeTypeSystem() == headCxn.getSchemaTypeSystem()) {

			// form schema roles
			for (Role r : headCxn.getSchemaTypeSystem().get(headCxn.getFormBlock().getType()).getContents().getElements()) {
				featureStructure.addSlot(new ECGSlotChain("" + ".f", r));
			}

			// form schema constraints
			for (Constraint constraint : headCxn.getSchemaTypeSystem().get(headCxn.getFormBlock().getType()).getContents()
					.getConstraints()) {
				if (!constraint.overridden()) {
					// addConstraint() doesn't handle 'before' or 'meets' constraints
					if (constraint.getOperator() != ECGConstants.MEETS && constraint.getOperator() != ECGConstants.BEFORE) {
						if (addConstraint(constraint, f, "") == false) {
							logger.warning("problem with " + constraint);
							return false;
						}
					}
				}
			}
		}

		for (Slot s : featureStructure.getSlots()) {
			if (s.getID() == -1)
				s.setID(globalUniqueSlotId++);
		}

		return true;
	}

	public int getSpanLeftCharIndex() {
		return leftCharIndex;
	}

	public int getSpanRightCharIndex() {
		return rightCharIndex;
	}

	public void setSpanLeftCharIndex(int index) {
		leftCharIndex = index;
	}

	public void setSpanRightCharIndex(int index) {
		rightCharIndex = index;
	}

	public Role getNextConstit() {
		if (constituentQueue == null) {
			throw new ParserException("Constituent queue not initialized for MAnalysis instance");
		}
		if (curConstitOffset > (constituentQueue.size() - 1))
			return null;
		return constituentQueue.get(curConstitOffset);
	}

	/**
	 * @return The last analysis to have been advanced as a child of this analysis
	 */
	public MAnalysis getLastAdvanced() {
		return lastAdvanced;
	}

	public boolean advance(Role role, MAnalysis that) {
		if (role != getNextConstit() && !isDisjointConstit(role)) {
			System.err.println("WARNING: Not ready to advance this constituent"); // TODO: a real error?
		}
		constituentQueue.remove(role);

		// simplified from Analysis.advance() because no gapping allowed in morphology
		if (lexicalRHS == null) {
			processConstituentSets(role);
			ECGSlotChain ecgsc = new ECGSlotChain(role);
			if (!featureStructure.coindexAcrossFeatureStructureSets(ecgsc, ECGConstants.EMPTYSLOTCHAIN,
					that.getFeatureStructure())) {
				return false;
			}

			CxnalSpan csp = new CxnalSpan(role, that.getHeadCxn(), getFeatureStructure().getSlot(new ECGSlotChain(role))
					.getID(), that.getSpanLeftIndex(), that.getSpanRightIndex(), that.getSpanLeftCharIndex(),
					that.getSpanRightCharIndex());

			spans.add(csp);
			if (that.getSpans() != null) {
				spans.addAll(that.getSpans());
			}

			setSpanRightCharIndex(that.getSpanRightCharIndex());

			lastAdvanced = that;

			return true;
		}
		else {
			throw new ParserException("advance called on an analysis with a null rhs");
		}
	}

	private void processConstituentSets(Role role) {
		leftOverComplements.remove(role);
		leftOverConstituents.remove(role);
		leftOverOptionals.remove(role);
	}

	public boolean omitOptional(Role role) {
		if (isDisjointConstit(role)) {
			System.err.println("WARNING: Omitting a disjoint constituent"); // TODO is this an error?
		}
		if (role != getNextConstit()) {
			System.err.println("WARNING: Not ready to omit this optional constituent");
			return false;
		}
		constituentQueue.remove(role);
		boolean success = super.omitOptional(role);
		omittedOptionals.add(role);
		return success;
	}

	public boolean isOmittedOptional(Role role) {
		return omittedOptionals.contains(role);
	}

	public Set<Role> getOmittedOptionals() {
		return omittedOptionals;
	}

	/**
	 * A disjoint constituent is one which is noncontiguous or has a nonadjacent usage
	 * 
	 * @param constitRole
	 */
	public void recordDisjointConstit(Role constitRole) {
		if (!disjointConstits.contains(constitRole)) {
			if (isOmittedOptional(constitRole)) {
				System.err.println("WARNING: A disjoint optional constituent?"); // TODO: an error?
			}
			disjointConstits.add(constitRole);
			constituentQueue.remove(constitRole);
		}
	}

	public boolean isDisjointConstit(Role constitRole) {
		return disjointConstits.contains(constitRole);
	}

	public List<Role> getDisjointConstits() {
		return disjointConstits;
	}

	public double getScore() {
		return score;
	}

	public MAnalysis clone() {
		try {
			MAnalysis a = (MAnalysis) super.clone();
			a.omittedOptionals = new HashSet<Role>(this.omittedOptionals);
			if (this.constituentQueue != null)
				a.constituentQueue = new ArrayList<Role>(this.constituentQueue);
			a.curConstitOffset = this.curConstitOffset;
			a.disjointConstits = new ArrayList<Role>(this.disjointConstits);
			a.setSpanLeftCharIndex(this.leftCharIndex);
			a.setSpanRightCharIndex(this.rightCharIndex);
			a.completed = this.completed;
			a.lexicalRHS = this.lexicalRHS; // TODO: temporary
			a.score = this.score;
			return a;
		}
		catch (Exception e) {
			logger.warning("Inexplicable meltdown in MAnalysis.clone: " + e);
			throw new ParserException("Inexplicable meltdown in Analysis.clone: " + e);
		}
	}
}

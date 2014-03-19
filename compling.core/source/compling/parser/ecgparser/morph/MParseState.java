package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ecgparser.Analysis;
import compling.util.StringUtilities;

class MParseState extends MAnalysis {
	private MGrammarWrapper grammar;
	public int position = 0;
	public boolean noncontig = false; // Is the head construction noncontiguous?
	public boolean nonadjacentUsage = false; // Are all appearances of the head constituent adjacent in its utilizer's
															// formation?
	public boolean omitted = false;
	public List<ECGSlotChain> rhsSymbolic;
	public List<String> rhsLiteral = new ArrayList<String>();
	public Map<Role, MParseState> filledConstits = new HashMap<Role, MParseState>();
	public Map<Integer, Set<MParseState>> parentStates = new HashMap<Integer, Set<MParseState>>(); // maps fc position to
																																	// parent states at
																																	// that position

	public List<MParseState> scanees = new ArrayList<MParseState>(); // States produced by scanning on this state, i.e.
																							// advancing the position to the next form
																							// component

	private Map<Role, ECGSlotChain> firstAppearances = new HashMap<Role, ECGSlotChain>();

	public MParseState(Construction headCxn, boolean emptyRHS, MGrammarWrapper gmr) {
		super(headCxn, gmr);
		this.grammar = gmr;
		if (emptyRHS)
			rhsSymbolic = new ArrayList<ECGSlotChain>();
		else if (grammar.isLexicalConstruction(getHeadType())) {
			String lexeme = StringUtilities.removeQuotes(ECGGrammarUtilities.getLexemeFromLexicalConstruction(headCxn));
			rhsLiteral.add(lexeme);
			rhsSymbolic = new ArrayList<ECGSlotChain>();
			rhsSymbolic.add(new ECGSlotChain("self.f.orth"));
		}
		else {
			rhsSymbolic = grammar.getMeetsChain(getHeadType());
			this.initConstitQueue(buildConstitQueue(rhsSymbolic, grammar.getConstruction(getHeadType())));
		}
	}

	public String getRuleName() {
		return getHeadType();
	}

	public String getRuleRange() {
		return "(" + this.getSpanLeftIndex() + ";" + this.getSpanLeftCharIndex() + ", " + this.getSpanRightIndex() + ";"
				+ this.getSpanRightCharIndex() + ")";
	}

	public String getHeadType() {
		return this.getHeadCxn().getName();
	}

	public ECGSlotChain getNextRHSSymbol() {
		return rhsSymbolic.get(position);
	}

	public boolean isComplete() {
		return (position == rhsSymbolic.size());
	}

	public boolean isEmptyRHS() {
		return rhsSymbolic.size() == 0;
	}

	public void addParentState(MParseState newState) {
		if (!parentStates.containsKey(this.position - 1)) // We already advanced the position
			parentStates.put(this.position - 1, new HashSet<MParseState>());
		parentStates.get(this.position - 1).add(newState);
	}

	public void addParentStates(Map<Integer, Set<MParseState>> newStates) {
		for (Map.Entry<Integer, Set<MParseState>> entry : newStates.entrySet()) {
			int i = entry.getKey();
			if (!parentStates.containsKey(i)) // We already advanced the position
				parentStates.put(i, new HashSet<MParseState>());
			// parentStates.get(i).addAll(entry.getValue());
			List<MParseState> slist = new ArrayList<MParseState>(entry.getValue());
			for (MParseState s : slist) {
				if (!parentStates.get(i).contains(s))
					parentStates.get(i).add(s);
			}
		}
	}

	/**
	 * Looks at the form components of the parse state's head construction, returning true if the given constituent
	 * appears in a form component which references one of its form roles (a <i>form role appearance</i>). Note that the
	 * parser assumes there will be no <i>deep</i> appearances, in which a form component refers to a subconstituent of
	 * one of its host construction's constituents.
	 * 
	 * @param constitRole
	 *           The constituent whose appearances are being inspected
	 * @return
	 */
	public boolean hasFormRoleAppearance(Role constitRole) {
		// Check for multiple appearances. (At most one appearance may be a non-form role appearance.)
		if (grammar.isLexicalConstruction(this.getHeadCxn()))
			return false;
		List<ECGSlotChain> mc = grammar.getMeetsChain(this.getHeadCxn().getName());
		List<Integer> refs = references(mc, 0, constitRole, 2); // Up to 2 meets chain references to the constituent
		if (refs.size() == 0)
			return false;
		if (refs.size() > 1)
			return true;

		// refs.size()==1--single appearance
		// Check if it is simply constitRole.f, or constitRole.f.*
		String ref = mc.get(refs.get(0)).toString();
		return (!ref.equals(constitRole.toString() + ".f"));
	}

	public boolean hasMultipleNonadjacentAppearances(Role constitRole) {
		// Check for multiple appearances, and see whether they're all adjacent
		List<ECGSlotChain> mc = grammar.getMeetsChain(this.getHeadCxn().getName());
		List<Integer> refs = references(mc, 0, constitRole, -1); // All chain references to the constituent
		if (refs.size() < 2)
			return false; // Doesn't have multiple appearances

		// Check if all appearances are adjacent
		int prev = -1;
		for (int i : refs) {
			if (prev > -1 && i != (prev + 1))
				return true;
			prev = i;
		}
		return false;
	}

	public boolean hasMultipleAppearances(Role constitRole) {
		// Check for multiple appearances
		List<ECGSlotChain> mc = grammar.getMeetsChain(this.getHeadCxn().getName());
		List<Integer> refs = references(mc, 0, constitRole, 2); // All chain references to the constituent
		return (refs.size() > 1); // Doesn't have multiple appearances
	}

	private static List<Integer> references(List<ECGSlotChain> meetsChain, int i, Role constitRole, int maxTimes) {
		if (maxTimes == 0)
			return new ArrayList<Integer>();
		if (meetsChain.size() == 0 || i == meetsChain.size())
			return new ArrayList<Integer>();
		String r = meetsChain.get(i).toString();
		if (r.startsWith("self.") || !r.startsWith(constitRole.getName() + ".")) {
			return references(meetsChain, i + 1, constitRole, maxTimes);
		}
		else {
			List<Integer> refs = references(meetsChain, i + 1, constitRole, maxTimes - 1);
			refs.add(0, i);
			return refs;
		}

		// TODO: make this iterative
	}

	/**
	 * Extracts the constituent roles of appearances in a meets chain, returning them in order (by first appearance).
	 * 
	 * @param meetsChain
	 * @return
	 */
	public static List<Role> buildConstitQueue(List<ECGSlotChain> meetsChain, Construction c) {
		List<Role> queue = new ArrayList<Role>();
		for (ECGSlotChain sc : meetsChain) {
			String s = sc.toString();
			if (s.startsWith("self."))
				continue;
			String roleName = s.substring(0, s.indexOf("."));
			Role constitRole = MGrammarWrapper.findRole(c.getConstituents(), roleName);
			if (!queue.contains(constitRole))
				queue.add(constitRole);
		}
		return queue;
	}

	public boolean isFirstAppearance(Role role, ECGSlotChain fc) {
		List<ECGSlotChain> mc = grammar.getMeetsChain(this.getHeadCxn().getName());
		List<Integer> refs = references(mc, 0, role, 1);
		return (refs.size() == 1 && mc.get(refs.get(0)).equals(fc));
	}

	/*
	 * public boolean isFirstAppearanceOmitted(Role role) { if (!isFirstAppearanceRecorded(role)) return false;
	 * ECGSlotChain firstAppearanceFC = firstAppearances.get(role); for (int i=0; i < this.rhsSymbolic.size(); i++) {
	 * ECGSlotChain fc = this.rhsSymbolic.get(i); if (fc.equals(firstAppearanceFC)) { this.parentStates.get(i); } }
	 * 
	 * }
	 */

	public MParseState clone() {
		Analysis aCopy = super.clone();
		MParseState that = (MParseState) aCopy;
		that.omitted = this.omitted;
		that.noncontig = this.noncontig;
		that.position = this.position;
		that.leftCharIndex = this.leftCharIndex;
		that.rightCharIndex = this.rightCharIndex;
		that.nonadjacentUsage = this.nonadjacentUsage;
		that.rhsSymbolic = new ArrayList<ECGSlotChain>(this.rhsSymbolic);
		that.rhsLiteral = new ArrayList<String>(this.rhsLiteral);
		that.filledConstits = new HashMap<Role, MParseState>(this.filledConstits);
		that.parentStates = new HashMap<Integer, Set<MParseState>>();
		for (Map.Entry<Integer, Set<MParseState>> entry : this.parentStates.entrySet()) {
			that.parentStates.put(entry.getKey(), new HashSet<MParseState>(entry.getValue()));
		}
		that.firstAppearances = new HashMap<Role, ECGSlotChain>(this.firstAppearances);
		that.scanees = new ArrayList<MParseState>(this.scanees);
		return that;
	}

	public boolean equivalentTo(MParseState that) {
		return (that.position == this.position && that.getHeadType().equals(this.getHeadType())
				&& that.getSpanLeftIndex() == this.getSpanLeftIndex()
				&& that.getSpanRightIndex() == this.getSpanRightIndex()
				&& that.getSpanLeftCharIndex() == this.getSpanLeftCharIndex()
				&& that.getSpanRightCharIndex() == this.getSpanRightCharIndex()
				&& that.rhsSymbolic.equals(this.rhsSymbolic) && that.filledConstits.equals(this.filledConstits)
				&& that.noncontig == this.noncontig && that.omitted == this.omitted /*
																											 * && that.nonadjacentUsage==this.
																											 * nonadjacentUsage
																											 */);
	}

	public String toString() {
		return toPlainString(true);
	}

	public String toHTMLString(boolean showParents) {
		String s = " ";
		if (noncontig)
			s += "&";
		if (nonadjacentUsage)
			s += "#";
		if (omitted)
			s += "%";
		s += "<b>" + getRuleName() + "</b>" + getRuleRange() + " -> ";
		for (int i = 0; i < position; i++) {
			s += rhsSymbolic.get(i);
			if (i < rhsLiteral.size()) {
				s += rhsLiteral.toString();
			}
			s += " ";
		}
		s += "* ";
		for (int i = position; i < rhsSymbolic.size(); i++) {
			s += rhsSymbolic.get(i) + " ";
		}

		if (showParents && parentStates.size() > 0) {
			s += "<ul class=\"par\">";
			for (Map.Entry<Integer, Set<MParseState>> ps : parentStates.entrySet()) {
				s += ps.getKey() + " = ";
				Set<MParseState> set = ps.getValue();
				s += "<ul class=\"stateset\">";
				for (MParseState mps : set) {
					s += "<li>" + mps + "</li>";
				}
				s += "</ul>";
			}
			s += "</li></ul>";
		}
		return s;
	}

	public String toPlainString(boolean showParents) {
		String s = " ";
		if (noncontig)
			s += "&";
		if (nonadjacentUsage)
			s += "#";
		if (omitted)
			s += "%";
		s += getRuleName() + getRuleRange() + " -> ";
		for (int i = 0; i < position; i++) {
			s += rhsSymbolic.get(i);
			if (i < rhsLiteral.size()) {
				s += "[" + rhsLiteral + "]";
			}
			s += " ";
		}
		s += "* ";
		for (int i = position; i < rhsSymbolic.size(); i++) {
			s += rhsSymbolic.get(i) + " ";
		}

		if (showParents && parentStates.size() > 0) {
			String ps = parentStates.toString();
			s += "^{" + ps.substring(1, ps.length() - 1) + "}";
		}
		return s;
	}
}

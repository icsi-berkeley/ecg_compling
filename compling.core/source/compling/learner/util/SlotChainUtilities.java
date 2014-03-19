// =============================================================================
// File        : SlotChainUtilities.java
// Author      : emok
// Change Log  : Created on Jun 28, 2006
//=============================================================================

package compling.learner.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Primitive;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.LearnerException;

//=============================================================================

public class SlotChainUtilities {

	public static String extractFromSlotChain(String slotchain, int index) {
		ECGSlotChain sc = new ECGSlotChain(slotchain);
		return extractFromSlotChain(sc, index, index);
	}

	public static String extractFromSlotChain(SlotChain slotchain, int index) {
		return extractFromSlotChain(slotchain, index, index);
	}

	public static String extractFromSlotChain(String slotchain, int fromIndex, int toIndex) {
		ECGSlotChain sc = new ECGSlotChain(slotchain);
		return extractFromSlotChain(sc, fromIndex, toIndex);
	}

	public static String extractFromSlotChain(SlotChain slotchain, int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return slotchain.getChain().get(fromIndex).getName();
		}
		else {
			List<String> chain = new ArrayList<String>();
			for (Role role : slotchain.getChain().subList(fromIndex, toIndex)) {
				chain.add(role.getName());
			}
			return joinStrings(chain, ".");
		}
	}

	public static int extractIDfromSlotChain(SlotChain slotchain) {
		return extractIDfromName(extractFromSlotChain(slotchain, 0));
	}

	public static int extractIDfromName(String name) {
		int firstLeftBracket = name.indexOf("[");
		int firstRightBracket = name.indexOf("]");
		if (firstLeftBracket > firstRightBracket) {
			// throw exception
			return -1;
		}
		else {
			return Integer.valueOf(name.substring(firstLeftBracket + 1, firstRightBracket));
		}
	}

	public static SlotChain replaceID(SlotChain slotchain, int newID) {
		String firstDot = extractFromSlotChain(slotchain, 0);
		int firstLeftBracket = firstDot.indexOf("[");
		int firstRightBracket = firstDot.indexOf("]");
		if (firstLeftBracket > firstRightBracket) {
			// nothing to replace, throw exception
			return slotchain;
		}
		else {
			firstDot = firstDot.substring(0, firstLeftBracket) + "[" + String.valueOf(newID) + "]";
			return spliceSlotChain(slotchain, firstDot, 0);
		}

	}

	public static <T extends Primitive> T instanceToType(TypeSystem<T> typeSystem, String name) {
		int lastLeftBracket = name.lastIndexOf("[");
		if (lastLeftBracket != -1) {
			return typeSystem.get(name.substring(0, lastLeftBracket));
		}
		else {
			return typeSystem.get(name);
		}
	}

	public static String instanceToTypeString(String name) {
		int lastLeftBracket = name.lastIndexOf("[");
		return lastLeftBracket != -1 ? name.substring(0, lastLeftBracket) : name;
	}

	// /-------------------------------------------------------------------------
	/**
	 * @param originalString
	 * @param replacementString
	 * @param index
	 * @return SlotChain
	 * @throws LearnerException
	 * 
	 *            it takes the original slot chain, replaces the slot at the index with the replacement string, and
	 *            returns a SlotChain. If the replacement string is null or the empty string, the section of the slot
	 *            chain is removed.
	 */
	public static SlotChain spliceSlotChain(String originalString, String replacementString, int index)
			throws LearnerException {
		return new SlotChain(slotChainReplacementHelper(originalString, replacementString, index));
	}

	public static SlotChain spliceSlotChain(SlotChain originalSlotChain, String replacementString, int index)
			throws LearnerException {
		return new SlotChain(slotChainReplacementHelper(originalSlotChain.toString(), replacementString, index));
	}

// /-------------------------------------------------------------------------
	/**
	 * @param originalString
	 * @param replacementString
	 * @param index
	 * @return ECGSlotChain
	 * @throws LearnerException
	 * 
	 *            it takes the original slot chain, replaces the slot at the index with the replacement string, and
	 *            returns an ECGSlotChain. If the replacement string is null or the empty string, the section of the slot
	 *            chain is removed.
	 */
	public static ECGSlotChain spliceECGSlotChain(String originalString, String replacementString, int index)
			throws LearnerException {
		return new ECGSlotChain(joinStrings(slotChainReplacementHelper(originalString, replacementString, index), "."));
	}

	public static ECGSlotChain spliceECGSlotChain(ECGSlotChain originalSlotChain, String replacementString, int index)
			throws LearnerException {
		return new ECGSlotChain(joinStrings(
				slotChainReplacementHelper(originalSlotChain.toString(), replacementString, index), "."));
	}

	private static List<String> slotChainReplacementHelper(String originalString, String replacementString, int index)
			throws LearnerException {
		ArrayList<String> slotChain = new ArrayList<String>(Arrays.asList(originalString.split("\\.")));
		if (index > slotChain.size()) {
			throw new LearnerException("Error manipulating slot chains: slot index out of bound.");
		}
		if (replacementString == null || replacementString.equals("")) {
			slotChain.remove(index);
		}
		else {
			slotChain.set(index, replacementString);
		}
		return slotChain;
	}

	public static String joinStrings(List<String> strings, String separator) {
		String joinedString = strings.get(0);
		for (String string : strings.subList(1, strings.size())) {
			joinedString = joinedString + separator + string;
		}
		return joinedString;
	}
}

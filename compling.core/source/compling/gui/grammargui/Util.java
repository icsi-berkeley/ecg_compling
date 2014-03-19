/**
 * Some utility functions. Used in the compling.gui.grammargui package.
 *
 * TODO: move this somewhere else.
 */

package compling.gui.grammargui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Widget;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.parser.ParserException;
import compling.parser.ecgparser.*;
import compling.util.PriorityQueue;
import compling.utterance.Utterance;
import compling.utterance.Word;

/**
 * Utility class. Probably to be merged with some other utility class somewhere
 * else.
 *
 * @author lucag
 */
public class Util {

	public static String toString(Viewer viewer) {
		return String.format("Output::%s", viewer
				.getData(GrammarBrowser.OUTPUT_SHELL_ID));
	}

	public static String toString(Widget widget) {
		return String.format("Output::%s", widget
				.getData(GrammarBrowser.OUTPUT_SHELL_ID));
	}

	public static String toString(TypeSystemNode node) {
		return String.format("%s::%s", node.getClass().getName(), node.getType());
	}

	/**
	 * A comma-separated representation of an array.
	 *
	 * @param array -
	 *          The array to make a string representation of
	 * @return A comma-separated representation of an array, or the empty string
	 *         if the array doesn't contain any element.
	 */
	public static String join(Object[] array) {
		return join(array, ", ");
	}

	/**
	 * A string representation of an array.
	 *
	 * @param array
	 *          The array to make a string representation of
	 * @param c
	 *          The separator of the array elements
	 * @return A string representation of an array, or the empty string if the
	 *         array doesn't contain any element.
	 */
	public static String join(Object[] array, String c) {
		if (array == null)
			return "<null>";

		StringBuilder sb = new StringBuilder();
		Object last = array[array.length - 1];
		for (Object e : array) {
			sb.append(e.toString());
			if (!e.equals(last))
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * A string representation of a set. The elements are enclosed in curly
	 * braces.
	 *
	 * @param set -
	 *          The set to make a representation of
	 * @return A string representing the set
	 */
	public static String setToString(Set<?> set) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append(join(set.toArray()));
		sb.append('}');

		return sb.toString();
	}

	/**
	 * @param node -
	 *          A {@link TypeSystemNode}
	 * @return the Node's simple name
	 *
	 * @see TypeSystemType
	 */
	public static String getNodeType(TypeSystemNode node) {
		return node.getClass().getSimpleName();
	}

	/**
	 * Split a string on white space as defined by {@link StringTokenizer}
	 *
	 * @param text -
	 *          The text to split
	 * @return an array containing the element of text that were separated by
	 *         whitespace
	 */
	public static ArrayList<String> split(String text) {
		ArrayList<String> words = new ArrayList<String>();
		StringTokenizer t = new StringTokenizer(text);
		while (t.hasMoreTokens()) {
			words.add(t.nextToken());
		}
		return words;
	}

	public static Analysis getBestParse(
	      LeftCornerParser<Analysis> analyzer,
			Utterance<Word, String> utterance) {
		return analyzer.getBestParse(utterance);
	}

	public static PriorityQueue<Analysis> getBestParses(
	      LeftCornerParser<Analysis> analyzer,
			Utterance<Word, String> utterance) {
		PriorityQueue<java.util.List<Analysis>> pqa = analyzer
				.getBestPartialParses(utterance);
		PriorityQueue<Analysis> parses = new PriorityQueue<Analysis>();
		while (pqa.size() > 0) {
			double priority = pqa.getPriority();
			java.util.List<Analysis> al = pqa.next();
			if (al.size() > 1) {
				throw new ParserException("shouldn't have more than one root.");
			}
			Analysis a = al.get(0);
			parses.add(a, priority);
		}
		return parses;
	}

	/**
	 * Drops the elements of an array that are not of the given type
	 *
	 * @param array - the array to be processed
	 * @param klass - the class of the elements to keep
	 * @return the filtered array
	 */
	@SuppressWarnings("unchecked")
	public static Object[] dropNotType(Object[] array, Class klass) {
		List ret = new ArrayList();
		for (Object e : array) {
			if (klass.isInstance(e))
				ret.add(e);
		}
		return ret.toArray();
	}
}

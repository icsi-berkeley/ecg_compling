package compling.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Arrays {

	/**
	 * A comma-separated representation of an array.
	 * 
	 * @param array
	 *           - The array to make a string representation of
	 * @return A comma-separated representation of an array, or the empty string if the array doesn't contain any
	 *         element.
	 */
	public static String join(Object[] array) {
		return join(array, ", ");
	}

	/**
	 * A string representation of an array.
	 * 
	 * @param array
	 *           The array to make a string representation of
	 * @param c
	 *           The separator of the array elements
	 * @return A string representation of an array, or the empty string if the array doesn't contain any element.
	 */
	public static String join(Object[] array, String c) {
		if (array == null)
			return "<null>";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; ++i) {
			sb.append(array[i].toString());
			if (i != array.length - 1)
				sb.append(c);
		}
		return sb.toString();
	}

	public static Object last(Object[] array) {
		int len = array.length;
		return len > 0 ? array[len - 1] : null;
	}

	/**
	 * Drops null elements.
	 * 
	 * @param <T>
	 *           the element type
	 * @param collection
	 * @return a list of non-null element in <code>collection</code>
	 */
	public static <T> List<T> dropNull(List<T> collection) {
		List<T> nonNull = new ArrayList<T>();
		for (T e : collection)
			if (e != null)
				nonNull.add(e);

		return nonNull;
	}

	/**
	 * Drops the elements of an array that are not of the given type
	 * 
	 * @param array
	 *           - the array to be processed
	 * @param klass
	 *           - the class of the elements to keep
	 * @return the filtered array
	 */
	public static Object[] dropNotType(Object[] array, Class<?> klass) {
		List<Object> ret = new ArrayList<Object>();
		for (Object e : array) {
			if (klass.isInstance(e))
				ret.add(e);
		}
		return ret.toArray();
	}

	public static <T> T[] copyOfRange(T[] original, int from, int to) {
		return java.util.Arrays.copyOfRange(original, from, to);
	}

	/**
	 * Split a string on white space as defined by {@link StringTokenizer}
	 * 
	 * @param text
	 *           - The text to split
	 * @return an array containing the element of text that were separated by whitespace
	 */
	public static List<String> split(String text) {
		ArrayList<String> words = new ArrayList<String>();
		StringTokenizer t = new StringTokenizer(text, "!?., \n\r", true);
		while (t.hasMoreTokens()) {
			String token = t.nextToken();
			if (!Character.isWhitespace(token.charAt(0)))
				words.add(token);
		}
		return words;
	}

}

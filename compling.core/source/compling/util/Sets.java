package compling.util;

import java.util.Set;

public class Sets {

	/**
	 * A string representation of a set. The elements are enclosed in curly braces.
	 * 
	 * @param set
	 *           - The set to make a representation of
	 * @return A string representing the set
	 */
	public static String setToString(Set<?> set) {
		return String.format("{%s}", Arrays.join(set.toArray()));
	}

}

package compling.util;

public class StringUtilities {

	public static String removeQuotes(String quotedString) {
		if (quotedString.startsWith("\"")) {
			return quotedString.substring(1, quotedString.length() - 1);
		}
		else {
			throw new RuntimeException("removeQuotes can only be called on a string with quotes. It was called on: "
					+ quotedString);
		}
	}

	public static String addQuotes(String string) {
		return (new StringBuffer("\"").append(string).append("\"")).toString();
	}

	public static String toCapitalized(String s) {
		return s != null ? s.substring(0, 1).toUpperCase() + s.substring(1) : null;
	}
}

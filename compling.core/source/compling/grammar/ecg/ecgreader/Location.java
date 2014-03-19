package compling.grammar.ecg.ecgreader;

/**
 * A Location of a symbol definition, including the file, line number, and the character count from the beginning of
 * file at with the definition occurs.
 * 
 * @author lucag
 */
public class Location {

	String symbol;
	String file;
	int line;
	int start;

	public static final Location UNKNOWN = new Location("<unknown symbol>", "<unknown file>", -1, -1);

	public Location(String symbol, String file, int line, int start) {
		this.symbol = symbol;
		this.file = file;
		this.line = line;
		this.start = start;
	}

	/** @return the file */
	public String getFile() {
		return file;
	}

	public int getEnd() {
		return start == -1 ? -1 : start + symbol.length();
	}

	/** @return the row */
	public int getLineNumber() {
		return line;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/** @return the column at which the symbol starts */
	public int getStart() {
		return start;
	}

	@Override
	public String toString() {
		return String.format("%s@%s:%d:%d", symbol, file, line, start);
	}

}

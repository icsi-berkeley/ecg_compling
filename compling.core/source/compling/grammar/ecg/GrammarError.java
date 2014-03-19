package compling.grammar.ecg;

import compling.grammar.ecg.ecgreader.IErrorListener.Severity;
import compling.grammar.ecg.ecgreader.Location;

/**
 * A simple record structure to hold errors.
 * 
 * @author lucag
 */
public class GrammarError {

	String message;
	Location location;
	Severity severity;

	public GrammarError(String message, Location location, Severity severity) {
		this.location = location;
		this.message = message;
		this.severity = severity;
	}

	public GrammarError(String message, Location location) {
		this(message, location, Severity.ERROR);
	}

	/** @return the message */
	public String getMessage() {
		return message;
	}

	/** @return the location */
	public Location getLocation() {
		return location;
	}

	/** @return the severity */
	public Severity getSeverity() {
		return severity;
	}

	@Override
	public String toString() {
		return String.format("GrammarError (%s): %s: %s", severity, message, location);
	}
}
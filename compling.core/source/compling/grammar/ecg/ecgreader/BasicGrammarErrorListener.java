package compling.grammar.ecg.ecgreader;

import java.util.LinkedList;
import java.util.List;

import compling.grammar.GrammarException;
import compling.grammar.ecg.GrammarError;

public class BasicGrammarErrorListener implements ILoggingErrorListener {

	protected List<GrammarError> errors = new LinkedList<GrammarError>();

	public void notify(String message, Location location, Severity severity) {
		errors.add(new GrammarError(message, location, severity));
		if (severity == Severity.EXCEPTION)
			throw new GrammarException(message);
	}
	
	public void notify(String message, Severity severity) {
		if (severity == Severity.EXCEPTION)
			throw new GrammarException(message);
	}

	public StringBuffer asStringBuffer() {
		StringBuffer b = new StringBuffer();
		for (GrammarError e : errors)
			b.append(e.getMessage() + '\n');
		return b;
	}

	public String[] asStringArray() {
		String[] strings = new String[errors.size()];
		int i = 0;
		for (GrammarError e : errors)
			strings[i++] = e.getMessage();
		return strings;
	}

}

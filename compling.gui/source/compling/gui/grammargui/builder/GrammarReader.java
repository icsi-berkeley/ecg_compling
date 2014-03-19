package compling.gui.grammargui.builder;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

import java_cup.runtime.Symbol;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.ecgreader.ECGReader;
import compling.grammar.ecg.ecgreader.IErrorListener;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.ecg.ecgreader.Yylex;
import compling.grammar.ecg.ecgreader.IErrorListener.Severity;
import compling.gui.grammargui.util.ISpecificationReader;

/**
 * <code>GrammarReader</code> is a parser for ECG grammars.
 * 
 * @author lucag
 * 
 */
public class GrammarReader extends ECGReader implements ISpecificationReader {

	private IErrorListener errorListener;

	/**
	 * @param grammar
	 */
	public GrammarReader(Grammar grammar) {
		super();
		setGrammar(grammar);
	}

	public GrammarReader() {
		this(new Grammar());
	}

	@Override
	public Object read(IFile from, IErrorListener listener) throws Exception {
		this.errorListener = listener;
		Yylex scanner;
		Charset encoding = Charset.forName(from.getCharset());
		Symbol parsed = null;
		try {
			scanner = new Yylex(new InputStreamReader(from.getContents(), encoding));
		}
		catch (CoreException e) {
			// Retry once...
			from.refreshLocal(IResource.DEPTH_ZERO, null);
			scanner = new Yylex(new InputStreamReader(from.getContents(), encoding));
		}
		setScanner(scanner);
		scanner.file = file = from.getProjectRelativePath().toPortableString();
		try {
			parsed = parse();
		}
		catch (GrammarException e) {
			report_error(e.getMessage(), e);
		}
		catch (Exception e) {
			report_error(getErrorLog() + ": " + scanner.getScannerErrors(), null);
		}
		if (getErrorLog().length() > 0) {
			// This is the case where the errors were structurally recoverable,
			// but the grammar is still broken.
			report_error(getErrorLog() + ": " + scanner.getScannerErrors(), null);
		}
		if (scanner.getScannerErrors().length() > 0) {
			// This is the case where the only errors were scanner issues
			report_error(scanner.getScannerErrors(), null);
		}
		return parsed;
	}

	@Override
	public void report_error(String message, Object info) {
		if (errorListener != null) {
			String errorMessage = message.length() == 0 ? "Syntax error" : message;
			// Let's assume getPrimitve() never returns null...
			// But it does return null!
			Location location = (info != null && ((GrammarException) info).getPrimitive() != null) ? ((GrammarException) info)
					.getPrimitive().getLocation() : ((Yylex) getScanner()).getLocation();
			errorListener.notify(errorMessage, location, Severity.ERROR);
		}
	}

	@Override
	public void syntax_error(Symbol cur_token) {
		super.syntax_error(cur_token);
		if (errorListener != null) {
			String errorLog = getErrorLog();
			errorListener.notify(errorLog.length() == 0 ? "Syntax Error" : errorLog, ((Yylex) getScanner()).getLocation(),
					Severity.ERROR);
		}
	}

}

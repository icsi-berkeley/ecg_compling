/**
 * 
 */
package compling.grammar.ecg.ecgreader;

/**
 * An <code>IErrorListener</code> listens to error, usually produced by <code>GrammarReader</code>s or
 * <code>GrammarChecker</code>s, and reports them through the <code>notify</code> method.
 * 
 * @see compling.gui.grammargui.builder.GrammarReader
 * @see compling.grammar.ecg.GrammarChecker
 * @author lucag
 */
public interface IErrorListener {

	/**
	 * Severity of the error. If the severity is <code>Severity.EXCEPTION</code>, the <code>report</code> method should
	 * throw a <code>RuntimeException</code>-derived exception to stop the checking process. This is to maintain
	 * compatibility with the previous error reporting in <code>GrammarChecker</code>.
	 */
	public enum Severity {
		WARNING, ERROR, FATAL, EXCEPTION,
	};

	void notify(String errorMessage, Location location, Severity severity);
	
	void notify(String errorMessage, Severity severity);

}

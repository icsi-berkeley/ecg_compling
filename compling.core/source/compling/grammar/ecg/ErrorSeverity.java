/**
 * 
 */
package compling.grammar.ecg;

import compling.grammar.ecg.ecgreader.IErrorListener;

/**
 * Error severity for IErrorListener.
 * 
 * @see IErrorListener#notify(String, Location, compling.compling.grammar.ecg.ecgreader.IErrorListener.Severity)
 * @author lucag
 * 
 */
public enum ErrorSeverity {
	WARNING, ERROR, FATAL, EXCEPTION
}

/**
 * 
 */
package compling.grammar.ecg.ecgreader;

/**
 * A logging error listener. Extends <code>IErrorListner</code> so that it remembers all error it received and returns
 * them.
 * 
 * @author lucag
 */
public interface ILoggingErrorListener extends IErrorListener {
	public StringBuffer asStringBuffer();

	public String[] asStringArray();
}

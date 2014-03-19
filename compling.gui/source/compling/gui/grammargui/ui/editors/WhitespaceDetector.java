/*
 * 
 */
package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A java aware white space detector.
 */
public class WhitespaceDetector implements IWhitespaceDetector {

	/*
	 * (non-Javadoc) Method declared on IWhitespaceDetector
	 */
	public boolean isWhitespace(char character) {
		return Character.isWhitespace(character);
	}
}

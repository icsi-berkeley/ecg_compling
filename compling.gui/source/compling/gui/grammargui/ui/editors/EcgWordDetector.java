package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A Java aware word detector.
 */
public class EcgWordDetector implements IWordDetector {

	/*
	 * (non-Javadoc) Method declared on IWordDetector.
	 */
	public boolean isWordPart(char character) {
		// TODO: Correct this!!!
		return Character.isLetter(character) || character == '-';
	}

	/*
	 * (non-Javadoc) Method declared on IWordDetector.
	 */
	public boolean isWordStart(char character) {
		return Character.isLetter(character);
	}
}

package compling.utterance;

/**
 * A simple abstract class representing a discreet chunk of (spoken/written) language on the size a
 * word/morpheme/punctuation.
 * 
 * @author John Bryant
 */

abstract public class FormElement<S> {

	abstract public S getOrthography();
}

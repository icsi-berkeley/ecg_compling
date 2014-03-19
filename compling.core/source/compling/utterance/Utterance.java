package compling.utterance;

import java.util.List;

/**
 * This interface represents a single unit of form on the level of a sentence. It provides methods for accessing the
 * form elements of the utterance, getting the source file and index into that file.
 * 
 * It also has the basic functions set up for adding and getting annotations on the Utterance.
 * 
 * Notice that it is parameterized by another interface representing the form elements that come together to form the
 * utterance. Any class that implements FormElement is ok.
 */

public interface Utterance<FORMTYPE extends FormElement, SOURCETYPE> {

	public void setElements(List<FORMTYPE> newElements);

	public List<FORMTYPE> getElements();

	public FORMTYPE getElement(int i);

	public SOURCETYPE getSource();

	public void setSource(SOURCETYPE source);

	public int getSourceOffset();

	public void setSourceOffset(int offset);

	public int size();

	public void addAnnotation(UtteranceAnnotation sa);

	public boolean hasAnnotation(String annotationType);

	public UtteranceAnnotation getAnnotation(String annotationType);

}

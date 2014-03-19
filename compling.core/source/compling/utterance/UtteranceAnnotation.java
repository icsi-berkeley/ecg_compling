package compling.utterance;

/**
 * This interface is something that should be implemented by all the ways that a sentence can be annotated. Add new
 * annotation types if the annotation that is desired isn't here.
 * 
 * @author John Bryant
 * 
 */

public interface UtteranceAnnotation {

	public static final String NAMEDENTITY = "NAMEDENTITY";
	public static final String COREFERENCE = "COREFERENCE";
	public static final String PENNTREEBANK = "PENNTREEBANK";
	public static final String PROPBANK = "PROPBANK";
	public static final String FRAMENET = "FRAMENET";
	public static final String METAPHOR = "METAPHOR";
	public static final String POS = "POS";
	public static final String CHILDES = "CHILDES";

	public String getAnnotationType();
}

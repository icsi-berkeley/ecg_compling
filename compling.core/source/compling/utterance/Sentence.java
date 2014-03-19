package compling.utterance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Sentence implements Utterance<Word, String> {

	List<Word> words;
	private static final Word offEdgeWord = new Word("*E*");
	private HashMap<String, UtteranceAnnotation> annotations = new HashMap<String, UtteranceAnnotation>();
	String sourceFile;
	int fileOffset;

	public Sentence(List<String> words, String sourceFile, int offset) {
		List<Word> wl = new ArrayList<Word>();
		for (String word : words) {
			wl.add(new Word(word));
		}
		setElements(wl);
		setSource(sourceFile);
		setSourceOffset(offset);
	}

	public Sentence(List<String> words) {
		this(words, null, 0);
	}

	public Sentence(String... words) {
		this(Arrays.asList(words), null, 0);
	}

	public List<Word> getElements() {
		return words;
	}

	public void setElements(List<Word> words) {
		this.words = words;
	}

	public int size() {
		return words.size();
	}

	public Word getElement(int i) {
		if (i >= size() || i < 0) {
			return offEdgeWord;
		}
		return words.get(i);
	}

	public int getSourceOffset() {
		return fileOffset;
	}

	public void setSourceOffset(int offset) {
		this.fileOffset = offset;
	}

	public String getSource() {
		return sourceFile;
	}

	public void setSource(String source) {
		this.sourceFile = source;
	}

	public void addAnnotation(UtteranceAnnotation sa) {
		annotations.put(sa.getAnnotationType(), sa);
	}

	public UtteranceAnnotation getAnnotation(String annotationType) {
		return annotations.get(annotationType);
	}

	public boolean hasAnnotation(String annotationType) {
		return annotations.containsKey(annotationType);
	}

	public List<String> getOrths() {
		List<String> orths = new ArrayList<String>();
		for (Word w : words) {
			orths.add(w.getOrthography());
		}
		return orths;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (String s : getOrths()) {
			sb.append(s).append(" ");
		}
		return sb.toString();
	}

}

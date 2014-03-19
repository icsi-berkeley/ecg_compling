package compling.utterance;

public class Word extends FormElement<String> implements Comparable<Word> {

	String orthography;

	public Word(String orthography) {
		this.orthography = orthography;
	}

	public String getOrthography() {
		return orthography;
	}

	public int compareTo(Word w) {
		return this.getOrthography().compareTo(w.getOrthography());
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Word))
			return false;

		return orthography.equals(((Word) o).getOrthography());
	}

	public int hashCode() {
		return orthography.hashCode();
	}

	public String toString() {
		return orthography;
	}

}

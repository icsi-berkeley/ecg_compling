package compling.parser.ecgparser;

public class MorphTokenPair {
	public String morph;
	public ECGTokenReader.ECGToken token;
	public MorphTokenPair(String morph, ECGTokenReader.ECGToken token) {
		this.morph = morph;
		this.token = token;
	}
}

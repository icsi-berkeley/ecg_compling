package compling.parser;

public class UnknownWordException extends ParserException {
	
	
	
	
	public boolean isUnknown() {
		return true;
	}
	
	private static final long serialVersionUID = -349091335193012818L; // is this okay? (ST)
	
	public UnknownWordException() {
		super();
	}

	public UnknownWordException(String message) {
		super(message);
	}

}

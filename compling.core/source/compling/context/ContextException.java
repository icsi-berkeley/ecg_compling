// =============================================================================
// File        : ContextException.java
// Author      : emok
// Change Log  : Created on Oct 18, 2006
//=============================================================================

package compling.context;

//=============================================================================

public class ContextException extends RuntimeException {

	private static final long serialVersionUID = -3298978613520105731L;

	public ContextException() {
		super();
	}

	public ContextException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContextException(String message) {
		super(message);
	}

	public ContextException(Throwable cause) {
		super(cause);
	}

	public static class ItemNotDefinedException extends ContextException {

		private static final long serialVersionUID = -7508052626292226018L;
		String unknownItem = "";

		public ItemNotDefinedException() {
			super();
		}

		public ItemNotDefinedException(String message, Throwable cause) {
			super(message, cause);
		}

		public ItemNotDefinedException(String message) {
			super(message);
		}

		public ItemNotDefinedException(Throwable cause) {
			super(cause);
		}

		public ItemNotDefinedException(String message, String unknownItem, Throwable cause) {
			super(message, cause);
			this.unknownItem = unknownItem;
		}

		public ItemNotDefinedException(String message, String unknownItem) {
			super(message);
			this.unknownItem = unknownItem;
		}

		public String getUnknownItem() {
			return unknownItem;
		}

	}

	public static class NoInstanceFoundException extends ContextException {

		private static final long serialVersionUID = 7965221786501186504L;

		public NoInstanceFoundException() {
			super();
		}

		public NoInstanceFoundException(String message, Throwable cause) {
			super(message, cause);
		}

		public NoInstanceFoundException(String message) {
			super(message);
		}

		public NoInstanceFoundException(Throwable cause) {
			super(cause);
		}
	}
}

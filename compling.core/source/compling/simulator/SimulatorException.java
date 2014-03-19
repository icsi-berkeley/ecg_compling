// =============================================================================
// File        : SimulatorException.java
// Author      : emok
// Change Log  : Created on Oct 9, 2006
//=============================================================================

package compling.simulator;

//=============================================================================

public class SimulatorException extends RuntimeException {

	private static final long serialVersionUID = -1356215689944280349L;

	public SimulatorException() {
		super();
	}

	public SimulatorException(String message) {
		super(message);
	}

	public SimulatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public SimulatorException(Throwable cause) {
		super(cause);
	}

	public static class ScriptNotFoundException extends SimulatorException {

		private static final long serialVersionUID = -7508052626292226018L;
		String scriptName = "";

		public ScriptNotFoundException() {
			super();
		}

		public ScriptNotFoundException(String message, Throwable cause) {
			super(message, cause);
		}

		public ScriptNotFoundException(String message) {
			super(message);
		}

		public ScriptNotFoundException(Throwable cause) {
			super(cause);
		}

		public ScriptNotFoundException(String message, String unknownScript) {
			super(message);
			this.scriptName = unknownScript;
		}

		public String getScriptName() {
			return scriptName;
		}

	}
}

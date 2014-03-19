// =============================================================================
//File        : LoggingFormatter.java
//Author      : emok
//Change Log  : Created on Sep 3, 2007
//=============================================================================

package compling.gui;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

//=============================================================================

public class LoggingHandler extends Handler {

	public LoggingHandler() {
		super();
		setFormatter(new LoggingFormatter());
	}

	public void close() throws SecurityException {
		flush();
	}

	public void flush() {
		System.out.flush();
	}

	public void publish(LogRecord arg0) {
		if (!(arg0.getLoggerName().startsWith("sun.rmi") || arg0.getLoggerName().startsWith("javax.management"))) {
			System.out.println(this.getFormatter().format(arg0));
		}
	}

	public class LoggingFormatter extends Formatter {

		public LoggingFormatter() {
			super();
		}

		@Override
		public String format(LogRecord arg0) {
			return arg0.getMessage();
		}

	}
}

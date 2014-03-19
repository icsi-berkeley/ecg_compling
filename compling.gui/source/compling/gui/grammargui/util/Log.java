package compling.gui.grammargui.util;

import java.util.Formatter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import compling.gui.grammargui.EcgEditorPlugin;

public class Log {

	private static Formatter formatter = new Formatter();

	public static void logInfo(String format, Object... args) {
		log(createStatus(IStatus.INFO, IStatus.OK, formatter.format(format, args).toString(), null));
	}

	public static void logError(Throwable t) {
		log(createStatus(IStatus.ERROR, IStatus.OK, "Unexpected exception", t));
	}

	public static void logError(Throwable t, String format, Object... args) {
		log(createStatus(IStatus.ERROR, IStatus.OK, formatter.format(format, args).toString(), t));
	}

	public static IStatus createStatus(int severity, int code, String message, Throwable t) {
		return new Status(severity, EcgEditorPlugin.PLUGIN_ID, code, message, t);
	}

	public static void log(IStatus status) {
		EcgEditorPlugin.getDefault().getLog().log(status);
	}

	public static void consoleLog(String format, Object... args) {
		System.out.println(formatter.format(format, args));
	}
}

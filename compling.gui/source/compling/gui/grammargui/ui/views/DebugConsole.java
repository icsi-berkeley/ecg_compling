package compling.gui.grammargui.ui.views;

import java.io.PrintStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class DebugConsole extends IOConsole {

	public DebugConsole() {
		this("Debug Console", null);
	}

	public DebugConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
	}

	public DebugConsole(String name, String consoleType, ImageDescriptor imageDescriptor) {
		super(name, consoleType, imageDescriptor);
	}

	public DebugConsole(String name, String consoleType, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, autoLifecycle);
	}

	public DebugConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding,
			boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
	}

	@Override
	protected void init() {
		super.init();

		IOConsoleOutputStream outputStream = newOutputStream();
		outputStream.setColor(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		PrintStream out = new PrintStream(outputStream, true);
		System.setOut(out);

		IOConsoleOutputStream errorStream = newOutputStream();
		errorStream.setColor(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		PrintStream err = new PrintStream(errorStream, true);
		System.setErr(err);
	}

}

package compling.gui.grammargui;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.MessageConsole;

public class ConsoleFactory implements IConsoleFactory {

	public void openConsole() {
		ConsolePlugin.getDefault().getConsoleManager()
					.addConsoles(new IConsole[] { new MessageConsole("Debug Console", null) });
	}
}

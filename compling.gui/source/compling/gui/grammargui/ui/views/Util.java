package compling.gui.grammargui.ui.views;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

public final class Util {
	
	public static Object getLastSegment(ITreeSelection selection) {
		if (selection == TreeSelection.EMPTY)
			return null;

		TreePath[] paths = selection.getPaths();
		if (paths.length < 1)
			return null;

		return paths[0].getLastSegment();
	}

}

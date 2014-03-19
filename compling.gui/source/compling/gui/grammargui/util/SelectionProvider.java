package compling.gui.grammargui.util;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class SelectionProvider extends EventManager {
	/**
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		addListenerObject(listener);
	}

	/**
	 * @param listener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		removeListenerObject(listener);
	}

	/**
	 * @param event
	 *           the event
	 */
	public void fireSelectionChanged(final SelectionChangedEvent event) {
		// pass on the notification to listeners
		for (final Object l : getListeners()) {
			SafeRunner.run(new SafeRunnable() {
				public void run() {
//					((ISelectionChangedListener) l).selectionChanged(event);
				}
			});
		}
	}

}

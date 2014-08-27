/**
 * A specialized event manager.
 */
package compling.gui.grammargui.util;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.swt.widgets.Display;

import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;

/**
 * Used in GrammarBrowser to keep track of objects (manus) listening for changes in the underlying data.
 * 
 * TODO: This should be merged with SelectionProvider
 * 
 * @author lucag
 */
public class ModelChangedEventManager extends EventManager {

	public void addModelChangeListener(IModelChangedListener listener) {
		addListenerObject(listener);
	}

	public void removeModelChangeListener(IModelChangedListener listener) {
		removeListenerObject(listener);
	}

	public void fireModelChanged(Object model, IProxy grammarProxy) {
		final ModelChangedEvent event = new ModelChangedEvent(model, grammarProxy);
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				for (Object l : getListeners()) {
					((IModelChangedListener) l).modelChanged(event);
				}
			}
		});
	}

	public void fireModelChanged(Object model, AnalyzerSentence[] added, AnalyzerSentence[] removed) {
		ModelChangedEvent event = new ModelChangedEvent(model, added, removed);
		for (Object l : getListeners()) {
			((IModelChangedListener) l).modelChanged(event);
		}
	}
}

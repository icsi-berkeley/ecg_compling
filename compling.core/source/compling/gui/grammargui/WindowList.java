/*
 * 
 */
package compling.gui.grammargui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/** The Output Window List: not used */
public class WindowList extends Window {

	private ListViewer listViewer;
	private Object[] widgets;

	public WindowList(Shell parentShell, Object[] widgets) {
		super(parentShell);
		this.widgets = widgets;
	}

	@Override
	protected Control getContents() {
		listViewer = new ListViewer(getShell());
		listViewer.setContentProvider(new ArrayContentProvider());
		listViewer.setLabelProvider(new LabelProvider() {
			/** @return The window title */
			@Override
			public String getText(Object element) {
				return ((Window) element).getShell().getText();
			}
		});
		listViewer.addFilter(new ViewerFilter() {
			/** @return True if the element to be shown is a Shell */
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof Shell; // Floating windows are all Shells
			}
		});
		listViewer.setInput(widgets);
		getShell().setText("Output Window List");
		listViewer.refresh();
		
		return listViewer.getControl();
	}
}

package compling.gui.grammargui.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import compling.gui.grammargui.model.TypeSystemEditorInput;

public class PrettyView extends EditorPart {

	private Color color;
	private Link link;
	private ISelectionListener listener;

	public static final String ID = "compling.gui.grammargui.editors.prettyView";

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof TypeSystemEditorInput))
			throw new PartInitException("input must be a TypeSystemEditorInput instance");
		setSite(site);
		setInput(input);
	}

	public void addSelectionListener(SelectionListener listener) {
		link.addSelectionListener(listener);
	}

	/**
	 * @return the listener
	 */
	public ISelectionListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 *           the listener to set
	 */
	public void setListener(ISelectionListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		top.setLayout(layout);
		link = new Link(top, SWT.NONE);
		link.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		color = new Color(Display.getDefault(), 255, 255, 255);
		link.setBackground(color);

		link.setText(((TypeSystemEditorInput) getEditorInput()).getTypeSystemNode().toString());
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] elements = e.text.split("[:/]");
				if (listener != null)
					listener.selectionChanged(PrettyView.this, new StructuredSelection(elements));
			}
		});
	}

	@Override
	public void setFocus() {
		if (link != null)
			link.setFocus();
	}

	@Override
	public void dispose() {
		if (color != null && !color.isDisposed()) {
			color.dispose();
			color = null;
		}
		if (link != null && !link.isDisposed()) {
			link.dispose();
			link = null;
		}
		super.dispose();
	}

}

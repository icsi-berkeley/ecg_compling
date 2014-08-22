package compling.gui.grammargui.ui.views;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.ui.editors.ConstructionEditorOutlinePage;
import compling.gui.grammargui.util.SelectionProvider;
import compling.gui.util.Utils;

public class TypeSystemNodeView extends ViewPart implements ISelectionProvider {

	public static final String ID = "compling.gui.grammargui.views.contentView";

	protected Link link;
	protected IViewSite site;
	protected ISelectionListener linkListener;
	protected SelectionProvider selectionProvider;
	protected ISelection selection;

	// TODO: REMOVE THIS!!!!!
	protected String getConnectedViewId() {
		return null;
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		this.site = site;
		linkListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				updateLink(part, selection);
			}
		};
		site.getPage().addSelectionListener(linkListener);
		selectionProvider = new SelectionProvider();
		site.setSelectionProvider(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		top.setLayout(layout);
		link = new Link(top, SWT.NONE);
		link.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		link.setBackground(new Color(site.getShell().getDisplay(), 255, 255, 255));
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				linkSelected(e.text);
			}
		});
	}

	/**
	 * Used to separate the elements in the anchors' href attributes.
	 * 
	 * @see compling.gui.grammargui.GrammarBrowserTextPrinter
	 */
	protected static final Pattern splitter = Pattern.compile("[:/]");

	protected void linkSelected(String text) {
		Assert.isTrue(text.indexOf('@') == -1, "linkListener: " + text);

		String[] elements = splitter.split(text);

		Assert.isTrue(elements.length > 1);

		TypeSystemNode node = Utils.fromDescriptor(PrefsManager.getDefault().getGrammar(), elements);
		link.setText(node.toString());
		setSelection(new TreeSelection(new TreePath(new Object[] { node })));
		selectionProvider.fireSelectionChanged(new SelectionChangedEvent(this, selection));
	}

	public static TypeSystemNode extractSystemNode(ITreeSelection selection) {
		Object lastSegment = Util.getLastSegment(selection);
		if (lastSegment instanceof TypeSystemNode)
			return (TypeSystemNode) lastSegment;

		if (lastSegment instanceof ConstructionEditorOutlinePage.ContentProvider.Segment)
			return ((ConstructionEditorOutlinePage.ContentProvider.Segment) lastSegment).getNode();

		return null;
	}

	protected void updateLink(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof ITreeSelection && part != this) {
			TypeSystemNode node = extractSystemNode((ITreeSelection) selection);
			if (node != null) {
				link.setEnabled(true);
				link.setText(node.toString());
			}
			else {
				link.setEnabled(false);
				link.setText("No type selected.");
			}
		}
	}

	@Override
	public void setFocus() {
		link.setFocus();
	}

	@Override
	public void dispose() {
		site.getPage().removeSelectionListener(linkListener);
		linkListener = null;
		selectionProvider = null;
		site.setSelectionProvider(null);
		super.dispose();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}

	public ISelection getSelection() {
		return selection;
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

}
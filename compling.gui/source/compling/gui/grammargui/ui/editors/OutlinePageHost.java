package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * <p>It hosts other IContentOutlinePage objects, i.e. the ones that show up in the <b>Outline</b> View.</p> 
 * 
 * <p>Needed with multipage editors.</p>
 * 
 * @author lucag
 */
public class OutlinePageHost extends Page implements IContentOutlinePage, ISelectionProvider, ISelectionChangedListener {
	private final static String DEFAULT_TEXT = "An outline is not available."; 
	
	private PageBook pageBook;
	private IContentOutlinePage currentPage;
	private IContentOutlinePage emptyPage;
	private ISelection selection;
	private ArrayList<ISelectionChangedListener> listeners;
	private IActionBars actionBars;
	
	public static class EmptyOutlinePage extends MessagePage implements IContentOutlinePage {
		public EmptyOutlinePage(String message) { setMessage(message); }
		@Override public void addSelectionChangedListener(ISelectionChangedListener listener) { }
		@Override public ISelection getSelection() { return EMPTY; }
		@Override public void removeSelectionChangedListener(ISelectionChangedListener listener) { }
		@Override public void setSelection(ISelection selection) { }
	}

	private static final ISelection EMPTY = new ISelection() {
		@Override public boolean isEmpty() { return true; }
	};

	public OutlinePageHost() {
		this.selection = EMPTY;
		this.listeners = new ArrayList<ISelectionChangedListener>();
	}

    protected IContentOutlinePage createDefaultPage(PageBook book) {
        if (emptyPage == null) {
        	emptyPage = new EmptyOutlinePage(DEFAULT_TEXT);
        	emptyPage.createControl(book);
        }
        return emptyPage;
    }

	public void setPageActive(IContentOutlinePage page) {
		if (page == null) {
			page = createDefaultPage(pageBook);
		}
		
		if (currentPage != null) {
			currentPage.removeSelectionChangedListener(this);
		}
		
		IPageSite site = getSite();
		
		System.out.printf("site: %s\n", site);
		
		if (site != null && page != emptyPage)
			((ContentOutlinePage) page).init(site);
		
		// TODO:  enable sorting?
		//		page.sort(sortingOn);

		page.addSelectionChangedListener(this);
		
		if (pageBook == null) {
			return;
		}
		
		Control control = page.getControl();
		if (control == null || control.isDisposed()) {
			// first time
			page.createControl(pageBook);
			page.setActionBars(getActionBars());
			control = page.getControl();
		}
		pageBook.showPage(control);
		this.currentPage = page;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
		this.actionBars = actionBars;
		
		registerToolbarActions(actionBars);
		
		if (currentPage != null)
			setPageActive(currentPage);
	}

	private void registerToolbarActions(IActionBars actionBars) {
		// TODO:
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
//		if (toolBarManager != null) {
//			toolBarManager.add(new AbstractToggleLinkingAction(editor));
//			toolBarManager.add(new SortingAction());
//		}
	}


	public IActionBars getActionBars() {
		return actionBars;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub
		
		System.out.printf("OutlinePageHost.selectionChanged: %s\n", event);
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}

	@Override
	public void init(IPageSite pageSite) {
		// TODO Auto-generated method stub
		super.init(pageSite);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;

		if (listeners == null)
			return;
		
		SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).selectionChanged(e);
		}
	}

	@Override
	public void createControl(Composite parent) {
		pageBook = new PageBook(parent, SWT.None);
//		setPageActive(createDefaultPage(pageBook));
	}

	@Override
	public Control getControl() {
		return pageBook;
	}

	@Override
	public void dispose() {
		if (pageBook != null && !pageBook.isDisposed())
			pageBook.dispose();

		if (emptyPage != null) {
			emptyPage.dispose();
			emptyPage = null;
		}
		pageBook = null;
		listeners = null;
	}

	@Override
	public void setFocus() {
		if (currentPage != null)
			currentPage.setFocus();
	}

}

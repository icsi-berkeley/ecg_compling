package compling.gui.grammargui.model;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import compling.gui.grammargui.util.IComposite;
import compling.gui.grammargui.util.IElement;

@SuppressWarnings("unchecked")
public abstract class CompositeContentProvider<T> implements ITreeContentProvider {

	static final Object[] NONE = { null };
	
	protected IComposite<T> rootComposite;
	
	@Override
	public void dispose() {
		// Nothing to do, I guess
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		System.out.printf("viewer: %s\n", viewer);
		System.out.printf("oldInput: %s\n", oldInput);
		System.out.printf("newInput: %s\n", newInput);
		
		rootComposite = null;
	}

	public abstract IComposite<T> getComposite(Object inputElement);
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (rootComposite == null)
			rootComposite = getComposite(inputElement);
		
		return rootComposite.children().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Assert.isTrue(parentElement instanceof IComposite);
		
		return parentElement instanceof IComposite ? ((IComposite<T>) parentElement).children().toArray() : NONE;
	}

	@Override
	public Object getParent(Object element) {
		Assert.isTrue(element instanceof IComposite);
		
		System.err.printf("getParent called: %s", element);
		
		return element;
	}

	@Override
	public boolean hasChildren(Object element) {
		Assert.isTrue(element instanceof IComposite || element instanceof IElement);

		if (element instanceof IComposite)

			return ((IComposite<T>) element).children().size() > 0;
		return false;
	}
	
}
package compling.gui.grammargui.ui.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.util.TypeSystemNodeType;

public class TypeSystemTreeViewSorter extends ViewerComparator {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		TypeSystemNode n1 = (TypeSystemNode) e1;
		TypeSystemNode n2 = (TypeSystemNode) e2;
		int i = TypeSystemNodeType.fromNode(n1).compareTo(TypeSystemNodeType.fromNode(n2));
		return i != 0 ? i : n1.getType().compareTo(n2.getType());
	}

	@Override
	public boolean isSorterProperty(Object element, String property) {
		return true;
	}

	@Override
	public int category(Object element) {
		// TODO Auto-generated method stub
		return super.category(element);
	}

	@Override
	public void sort(Viewer viewer, Object[] elements) {
		// TODO Auto-generated method stub
		super.sort(viewer, elements);
	}

}

/**
 * 
 */
package compling.gui.grammargui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.GUIException;

// TODO: update this!

/**
 * Implements a content provider for the TreeView. Elements are lists that represent the whole path of the element itself. The reason is keeping track of "parents": each TypeSystemNode may represent a Construction that is a subpart of more than one Constructions.
 * @author  lucag
 */
public class TypeSystemContentProvider<N extends TypeSystemNode> implements
		ILazyTreePathContentProvider {

	public TypeSystemContentProvider(TreeViewer treeViewer,
			TypeSystem<N> typeSystem) {
		this.treeViewer = treeViewer;
		this.typeSystem = typeSystem;
		this.roots = findRoots(typeSystem);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// Nothing to do I guess...
	}

	private Set<N> findRoots(TypeSystem<N> typeSystem) {
		TreeSet<N> roots = new TreeSet<N>();
		for (N node : typeSystem.getAllTypes()) {
			if (typeSystem.getParents(node).isEmpty()) {
				roots.add(node);
			}
		}
		return roots;
	}

	// @SuppressWarnings("unchecked")
	private List<List<N>> getAllParentContinuations(List<List<N>> paths) {

		List<List<N>> parentPaths = new ArrayList<List<N>>();

		for (List<N> path : paths) {
			N top = path.get(path.size() - 1);
			Set<N> parents = typeSystem.getParents(top);
			for (N parent : parents) {
				List<N> parentPath = new ArrayList<N>();
				parentPath.addAll(path);
				parentPath.add(parent);
				parentPaths.add(parentPath);
			}
		}
		if (parentPaths.size() > 0) {
			return getAllParentContinuations(parentPaths);
		} else {
			return paths;
		}

	}

	/**
	 * Returns the children of the element at path.
	 * 
	 * @param path -
	 *          The path of the element being queried
	 * @return
	 * 				A set of nodes containing the children
	 */
	@SuppressWarnings("unchecked")
	protected Set<N> getChildren(TreePath path) {
		if (path == TreePath.EMPTY)
			return roots;
		else
			return typeSystem.getChildren(((N) path.getLastSegment()));
	}

	/**
	 * Returns the parent for the given element, or the empty path indicating that
	 * the parent can't be computed.
	 * 
	 * @see org.eclipse.jface.viewers.ILazyTreePathContentProvider#getParents(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public TreePath[] getParents(Object element) {
//		System.out.printf("getParents: %s\n", element);

		List<List<N>> starterPath = new ArrayList<List<N>>();
		List<N> starter = new ArrayList<N>();
		starter.add((N) element);
		starterPath.add(starter);

		List<List<N>> allPaths = getAllParentContinuations(starterPath);

		if (allPaths == null || allPaths.size() == 0) {
			throw new GUIException("Can't find path to selected node");
		}

		TreePath[] paths = new TreePath[allPaths.size()];
		for (int i = 0; i < paths.length; ++i) {
			List<N> p = allPaths.get(i);
			// TODO: p.add(root);
			Collections.reverse(p);
			paths[i] = new TreePath(p.toArray());
		}
		return paths;
	}

	/**
	 * Called when SetInput is called
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Nothing to do.
		//System.out.printf("Old input: %s, new input: %s\n", oldInput, newInput);
	}

	/**
	 * Called when the TreeViewer needs an up-to-date child count for the given
	 * path. If the content provider knows the given element, it responds by
	 * calling TreeViewer.setChildCount(Object, int). If the given current child
	 * count is already correct, no action has to be taken.
	 * 
	 * @param path -
	 *          The path to element for which an up-to-date child count is needed,
	 *          or the viewer's input if the number of root elements is requested
	 * @param currentCount -
	 *          The current child count for the element that needs updating
	 */
	@SuppressWarnings("unchecked")
	public void updateChildCount(TreePath path, int currentCount) {
//		System.out.printf("updateChildCount: %s, %d\n", path, currentCount);

		int actualCount = getChildren(path).size();
		if (actualCount != currentCount) {
			treeViewer.setChildCount(path, actualCount);
		}
	}

	/**
	 * Called when a previously-blank item becomes visible in the TreeViewer.
	 * 
	 * @see org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateElement(org.eclipse.jface.viewers.TreePath,
	 *      int)
	 */
	@SuppressWarnings("unchecked")
	public void updateElement(TreePath parentPath, int index) {
//		System.out.printf("updateElement: %s, %d\n", parentPath, index);

		Set<N> children = getChildren(parentPath);
		Object[] elements = children.toArray();
		treeViewer.replace(parentPath, index, elements[index]);

		TreePath p = parentPath.createChildPath(elements[index]);
		updateChildCount(p, -1);
	}

	/**
	 * Called when the TreeViewer needs up-to-date information whether the node at
	 * the given tree path can be expanded.
	 * 
	 * @param path
	 *          The path of the item being queried
	 * 
	 * @see org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateHasChildren(org.eclipse.jface.viewers.TreePath)
	 */
	public void updateHasChildren(TreePath path) {
//		System.out.printf("updateHasChildren: %s\n", path);

		Set<N> set = getChildren(path);
		treeViewer.setChildCount(path, set.size());
	}

	private Set<N> roots;

	private TreeViewer treeViewer;

	/**
	 * @uml.property  name="typeSystem"
	 * @uml.associationEnd  
	 */
	private TypeSystem<N> typeSystem;

	public static final String ROOT = "<root>";

}

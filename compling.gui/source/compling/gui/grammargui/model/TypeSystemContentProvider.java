/**
 *
 */
package compling.gui.grammargui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.GUIException;
import compling.gui.grammargui.util.Log;
import compling.gui.util.Utils;

// TODO: update this!

/**
 * Implements a content provider for the TreeView. Elements are lists that represent the whole path of the element
 * itself. The reason is keeping track of "parents": each TypeSystemNode may represent a Construction that is a subpart
 * of more than one Constructions.
 * 
 * @author lucag
 */
public class TypeSystemContentProvider implements ILazyTreePathContentProvider {

	private Set<TypeSystemNode> roots;
	private TreeViewer treeViewer;
	private Map<String, TypeSystem<TypeSystemNode>> nodeToTypeSystem;

	public TypeSystemContentProvider(TreeViewer treeViewer) {
		this.treeViewer = treeViewer;
	}

	protected void initialize(Collection<TypeSystem<TypeSystemNode>> typeSystems) {
		nodeToTypeSystem = new HashMap<String, TypeSystem<TypeSystemNode>>();
		roots = new HashSet<TypeSystemNode>();
		for (TypeSystem<TypeSystemNode> ts : typeSystems)
			for (TypeSystemNode node : ts.getAllTypes()) {
				nodeToTypeSystem.put(Utils.toKey(node), ts);
				if (ts.getParents(node).isEmpty())
					roots.add(node);
			}
	}

	public void dispose() {
		treeViewer = null;
		reset();
	}

	private List<List<TypeSystemNode>> getAllParentContinuations(List<List<TypeSystemNode>> paths) {
		if (roots != null) {
			// ...therefore we've been initialized
			List<List<TypeSystemNode>> parentPaths = new ArrayList<List<TypeSystemNode>>();
			for (List<TypeSystemNode> path : paths) {
				TypeSystemNode last = path.get(path.size() - 1);
				TypeSystem<TypeSystemNode> ts = getTypeSystem(last);
				if (ts == null) {
					Log.logInfo("getAllParentContinuations: %s has no TS\n", last);
					continue;
				}
				for (TypeSystemNode parent : ts.getParents(last)) {
					List<TypeSystemNode> parentPath = new ArrayList<TypeSystemNode>();
					parentPath.addAll(path);
					parentPath.add(parent);
					parentPaths.add(parentPath);
				}
			}
			if (parentPaths.size() > 0)
				return getAllParentContinuations(parentPaths);
		}
		return paths;
	}

	/**
	 * Returns the children of the element at path.
	 * 
	 * @param path
	 *           - The path of the element being queried
	 * @return A set of nodes containing the children
	 */
	protected Set<TypeSystemNode> getChildren(TreePath path) {
		if (path == TreePath.EMPTY && roots != null) {
			return roots;
		}
		else {
			final TypeSystemNode node = (TypeSystemNode) path.getLastSegment();
			return getTypeSystem(node).getChildren(node);
		}
	}

	protected TypeSystem<TypeSystemNode> getTypeSystem(TypeSystemNode node) {
//		Assert.isTrue(nodeToTypeSystem.containsKey(node));
		return nodeToTypeSystem.get(Utils.toKey(node));
	}

	/**
	 * Returns the parent for the given element, or the empty path indicating that the parent can't be computed.
	 */
	public TreePath[] getParents(Object element) {
		List<List<TypeSystemNode>> starterPath = new ArrayList<List<TypeSystemNode>>();
		List<TypeSystemNode> starter = new ArrayList<TypeSystemNode>();
		starter.add((TypeSystemNode) element);
		starterPath.add(starter);

		List<List<TypeSystemNode>> allPaths = getAllParentContinuations(starterPath);

		if (allPaths == null || allPaths.size() == 0)
			throw new GUIException("Can't find path to selected node");

		TreePath[] paths = new TreePath[allPaths.size()];
		for (int i = 0; i < paths.length; ++i) {
			List<TypeSystemNode> p = allPaths.get(i);
			// TODO: p.add(root);
			Collections.reverse(p);
			paths[i] = new TreePath(p.toArray());
		}
		return paths;
	}

	/**
	 * Called by the framework when setInput is called
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
	 *      java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO: Add event notification
		if (newInput != null) {
			Assert.isTrue(newInput instanceof TypeSystem || newInput instanceof Collection,
					String.format("newInput %s is of the wong type", newInput));

			if (newInput instanceof TypeSystem)
				initialize(Arrays.asList((TypeSystem<TypeSystemNode>) newInput));
			else
				initialize((Collection<TypeSystem<TypeSystemNode>>) newInput);
		}
		else
			reset();
	}

	private void reset() {
		roots = null;
		nodeToTypeSystem = null;
	}

	/**
	 * Called when the TreeViewer needs an up-to-date child count for the given path. If the content provider knows the
	 * given element, it responds by calling TreeViewer.setChildCount(Object, int). If the given current child count is
	 * already correct, no action has to be taken.
	 * 
	 * @param path
	 *           - The path to element for which an up-to-date child count is needed, or the viewer's input if the number
	 *           of root elements is requested
	 * @param currentCount
	 *           - The current child count for the element that needs updating
	 */
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
	 * @see org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateElement(org.eclipse.jface.viewers.TreePath, int)
	 */
	public void updateElement(TreePath parentPath, int index) {
//		System.out.printf("updateElement: %s, %d\n", parentPath, index);

		Iterator<TypeSystemNode> e = getChildren(parentPath).iterator();
		for (int i = 0; i < index; ++i)
			e.next();

		TypeSystemNode n = e.next();

		treeViewer.replace(parentPath, index, n);
		TreePath p = parentPath.createChildPath(n);

		updateChildCount(p, -1);
	}

	/**
	 * Called when the TreeViewer needs up-to-date information whether the node at the given tree path can be expanded.
	 * 
	 * @param path
	 *           The path of the item being queried
	 * 
	 * @see org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateHasChildren(org.eclipse.jface.viewers.TreePath)
	 */
	public void updateHasChildren(TreePath path) {
//		System.out.printf("updateHasChildren: %s\n", path);

		treeViewer.setChildCount(path, getChildren(path).size());
	}

}

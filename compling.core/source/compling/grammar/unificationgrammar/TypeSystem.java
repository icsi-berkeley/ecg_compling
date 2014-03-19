package compling.grammar.unificationgrammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import compling.grammar.ecg.GrammarError;
import compling.grammar.ecg.ecgreader.IErrorListener.Severity;
import compling.grammar.ecg.ecgreader.ILocatable;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.Indexer;
import compling.util.Interner;

public class TypeSystem<N extends TypeSystemNode> {

	private HashMap<String, N> types = new HashMap<String, N>();
	private boolean[][] subtypeMatrix;
	private Indexer<String> indexer = new Indexer<String>(Indexer.INTERNEDKEYS);
	private LinkedList<N> topologicalSort;
	private HashMap<String, Set<N>> children = new HashMap<String, Set<N>>();
	private String name;
	private static Interner<String> interner = new Interner<String>();
	private HashMap<String, TypeConstraint> typeConstraintTable = new HashMap<String, TypeConstraint>();

	public TypeSystem(String name) {
		// super();
		setName(name);
	}

	private void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int size() {
		return types.keySet().size();
	}

	public String getInternedString(String s) {
		return interner.intern(s);
	}

	public void addTypes(Set<N> typeSet) throws TypeSystemException {
		for (N tn : typeSet) {
			addType(tn);
		}
		build();
	}

	public void addType(N typeNode) {
		String internedName = interner.intern(typeNode.getType());
		typeNode.setType(internedName);
		types.put(internedName, typeNode);
	}

	public void build() throws TypeSystemException {
		subtypeMatrix = new boolean[size()][size()];
		for (N tn : types.values()) {
			indexer.add(tn.getType());
		}
		List<N> roots = new ArrayList<N>();

		// MODIFIED (lucag)
		List<GrammarError> typeErrors = new ArrayList<GrammarError>();

		// build up children lists and check to make sure all types are defined
		for (N tn : types.values()) {
			for (String parent : tn.getParents()) {
				if (types.containsKey(parent) == false) {
					Location location = tn instanceof ILocatable ? ((ILocatable) tn).getLocation() : null;
					typeErrors.add(new GrammarError("Parent type " + parent + " of type " + tn.getType()
								+ " is undefined.\n", location, Severity.ERROR));
				}
				if (children.containsKey(parent) == false) {
					children.put(parent, new HashSet<N>());
				}
				Set<N> myChildren = children.get(parent);
				myChildren.add(tn);
			}

		}
		if (typeErrors.size() > 0) {
			throw new TypeSystemException(typeErrors);
		}

		for (N tn : types.values()) {
			HashSet<String> allSuperTypes = new HashSet<String>();
			try {
				getAllSuperTypes(tn.getType(), allSuperTypes);
			}
			catch (TypeSystemException e) {
				Location location = tn instanceof ILocatable ? ((ILocatable) tn).getLocation() : null;
				throw new TypeSystemException(new GrammarError(e.getMessage(), location));
			}
			int typeIndex = indexer.indexOf(tn.getType());
			if (tn.getParents().size() == 0) {
				roots.add(tn);
			}
			for (String parent : allSuperTypes) {
				int parentIndex = indexer.indexOf(interner.intern(parent));
				subtypeMatrix[typeIndex][parentIndex] = true;
			}
			typeConstraintTable.put(tn.getType(), new TypeConstraint(tn.getType(), this));
		}

		topologicalSort = new LinkedList<N>();
		HashSet<N> started = new HashSet<N>();
		HashSet<N> finished = new HashSet<N>();
		for (N root : roots) {
			topologicalSortHelper(root, started, finished);
		}
	}

	private void topologicalSortHelper(N node, HashSet<N> started, HashSet<N> finished) throws TypeSystemException {
		if (finished.contains(node)) {
			return;
		}
		if (started.contains(node)) {
			throw new TypeSystemException(new GrammarError(
						"There is a circularity in the inheritance hierarchy including type: " + node.getType(),
						(node instanceof ILocatable) ? ((ILocatable) node).getLocation() : null, Severity.ERROR));
		}
		started.add(node);
		for (N child : getChildren(node)) {
			topologicalSortHelper(child, started, finished);
		}
		finished.add(node);
		topologicalSort.addFirst(node);
	}

	public TypeConstraint getCanonicalTypeConstraint(String typeName) {
		return typeConstraintTable.get(typeName);
	}

	public N get(String name) {
		return types.get(name);
	}

	public Set<N> getChildren(TypeSystemNode parent) {
		String type = parent.getType();
		return children.containsKey(type) ? children.get(type) : new HashSet<N>();
	}

	public Set<N> getParents(TypeSystemNode typeSystemNode) {
		HashSet<N> parents = new HashSet<N>();
		for (String parent : typeSystemNode.getParents()) {
			parents.add(get(parent));
		}
		return parents;
	}

	public boolean subtype(String child, String parent) throws TypeSystemException {
		try {
			return subtypeMatrix[indexer.indexOf(child)][indexer.indexOf(parent)];
		}
		catch (Exception e) {
			throw new TypeSystemException("Either type: " + child + " or type: " + parent + " is not defined");
		}
	}

	public List<N> topologicalSort() {
		return topologicalSort;
	}

	public String bestUnifyingType(Set<String> types) throws TypeSystemException {
		for (String type : types) {
			boolean subtypeOfAll = true;
			for (String parent : types) {
				if (!subtype(interner.intern(type), interner.intern(parent))) {
					subtypeOfAll = false;
					break;
				}
			}
			if (subtypeOfAll) {
				return interner.intern(type);
			}
		}
		throw new TypeSystemException("No common subtype");
	}

	public String bestCommonSubtype(Set<String> types, boolean requiresUnique) throws TypeSystemException {
		List<String> all = allBestCommonSubtypes(types);
		if (all.size() == 0)
			throw new TypeSystemException("No common subtype found for " + types);
		if (all.size() == 1 || !requiresUnique)
			return all.get(0);
		else
			throw new TypeSystemException("No unique common subtype for " + types);
	}

	public List<String> allBestCommonSubtypes(Set<String> parents) throws TypeSystemException {
		Set<String> typeSet = new HashSet<String>();
		List<String> commonSubTypes = new ArrayList<String>();
		typeSet.addAll(types.keySet());

		for (String type : parents) {
			typeSet.retainAll(getAllSubtypes(type));
		}

		for (String type : typeSet) {
			boolean isChild = false;
			for (String type2 : typeSet) {
				if (type != type2 && subtype(type, type2)) {
					isChild = true;
				}
			}
			if (!isChild) {
				commonSubTypes.add(type);
			}
		}

		return commonSubTypes;
	}

	public String bestCommonSupertype(Set<String> children, boolean requiresUnique) throws TypeSystemException {
		List<String> all = allBestCommonSupertypes(children);
		if (all.size() == 0)
			throw new TypeSystemException("No common supertype found for " + children);
		else if (all.size() == 1 || !requiresUnique)
			return all.get(0);
		else
			throw new TypeSystemException("No unique common supertype for " + children);
	}

	public List<String> allBestCommonSupertypes(Set<String> children) throws TypeSystemException {
		Set<String> typeSet = new HashSet<String>();
		List<String> superTypes = new ArrayList<String>();
		typeSet.addAll(types.keySet());

		for (String type : children) {
			typeSet.retainAll(getAllSuperTypes(type));
		}

		for (String type : typeSet) {
			boolean isParent = false;
			for (String type2 : typeSet) {
				if (type != type2 && subtype(type2, type)) {
					isParent = true;
				}
			}
			if (!isParent) {
				superTypes.add(type);
			}
		}

		return superTypes;
	}

	public Set<String> getAllSuperTypes(String type) throws TypeSystemException {
		Set<String> supers = new LinkedHashSet<String>();
		getAllSuperTypes(type, supers);
		return supers;
	}

	private void getAllSuperTypes(String type, Set<String> superTypes) throws TypeSystemException {
		try {
			if (!superTypes.contains(type)) {
				superTypes.add(type);
				for (String parent : types.get(type).getParents()) {
					getAllSuperTypes(parent, superTypes);
				}
			}
			// else
			// Log.info("Possible cycle in type %s", type);
		}
		catch (NullPointerException e) {
			throw new TypeSystemException("Type " + type + " is not defined");
		}
	}

	public Set<String> getAllSubtypes(String type) throws TypeSystemException {
		Set<String> subtypes = new LinkedHashSet<String>();
		getAllSubtypes(type, subtypes);
		return subtypes;
	}

	private void getAllSubtypes(String type, Set<String> subtypes) throws TypeSystemException {
		try {
			subtypes.add(type);
			for (N child : getChildren(types.get(type))) {
				getAllSubtypes(child.getType(), subtypes);
			}
		}
		catch (NullPointerException e) {
			throw new TypeSystemException("Type: " + type + " is not defined");
		}
	}

	public Collection<N> getAllTypes() {
		return types.values();
	}

	public Collection<String> getAllTypeNames() {
		return types.keySet();
	}

	static class TypeSystemTesterNode implements TypeSystemNode {

		String type;
		Set<String> parents = new HashSet<String>();

		TypeSystemTesterNode(String type, String parents) {
			this.type = type;
			StringTokenizer st = new StringTokenizer(parents);
			while (st.hasMoreTokens())
				this.parents.add(st.nextToken());
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Set<String> getParents() {
			return parents;
		}

		public String toString() {
			return type;
		}

		@Override
		public int compareTo(TypeSystemNode testerNode) {
			if (!(testerNode instanceof TypeSystemTesterNode)) {
				throw new ClassCastException("Error in casting " + testerNode.toString() + "to a TypeSystemTesterNode");
			}
			return this.type.compareTo(((TypeSystemTesterNode) testerNode).type);
		}
	}

	public static void main(String[] args) throws TypeSystemException {

		Set<TypeSystemTesterNode> types = new HashSet<TypeSystemTesterNode>();
		types.add(new TypeSystemTesterNode("Z", ""));
		types.add(new TypeSystemTesterNode("Y", ""));
		types.add(new TypeSystemTesterNode("A", "Z"));
		types.add(new TypeSystemTesterNode("B", "Y"));
		types.add(new TypeSystemTesterNode("C", "A B"));
		types.add(new TypeSystemTesterNode("D", "A B"));
		types.add(new TypeSystemTesterNode("E", "C D"));
		types.add(new TypeSystemTesterNode("F", "C D A"));

		TypeSystem<TypeSystemTesterNode> ts = new TypeSystem<TypeSystemTesterNode>("tester");

		ts.addTypes(types);
		// System.out.println(ts.subtype("B", "A"));
		// System.out.println(ts.subtype("D", "A"));
		// System.out.println(ts.subtype("A", "A"));
		// System.out.println(ts.subtype("A", "D"));
		// System.out.println(ts.getParents(ts.get("D")));
		// System.out.println(ts.getChildren(ts.get("B")));
		// System.out.println(ts.topologicalSort());
		Set<String> t = new HashSet<String>();
		t.add(ts.getInternedString("C"));
		t.add(ts.getInternedString("D"));
		System.out.println(ts.bestCommonSupertype(t, false));
		System.out.println(ts.bestCommonSubtype(t, false));
		System.out.println(ts.bestUnifyingType(t));
	}

	@Override
	public String toString() {
		return "TypeSystem [name=" + name + "\n            types=" + types + "\n]";
	}

}

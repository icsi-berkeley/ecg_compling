package compling.grammar.ecg;

/**
 * This class represents an ECG grammar. It has public inner classes for Constructions, Schemas and (form, meaning,
 * constructional) blocks. It also has private HashMaps to link cxn/schema names to the corresponding cxn/schema, and
 * TypeSystems for cxns and schemas.
 * 
 * This class also does all the static grammar analysis by pushing the inherited roles/constraints to their inherited
 * types, checking to make sure the constraints are valid and checking the inheritance hierarchy.
 * 
 * @author John Bryant
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import compling.context.ContextModel;
import compling.grammar.GrammarException;
import compling.grammar.Rule;
import compling.grammar.ecg.ECGGrammarUtilities.ECGGrammarFormatter;
import compling.grammar.ecg.ECGGrammarUtilities.SimpleGrammarPrinter;
import compling.grammar.ecg.ecgreader.ILocatable;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;

public class Grammar {
	private HashMap<String, Construction> cxns = new LinkedHashMap<String, Construction>();
	private HashMap<String, Schema> schemas = new LinkedHashMap<String, Schema>();
	private HashMap<String, MapPrimitive> maps = new LinkedHashMap<String, MapPrimitive>();
	private HashMap<String, Situation> situations = new LinkedHashMap<String, Situation>();
	
	private TypeSystem<Construction> cxnTypeSystem = new TypeSystem<Construction>(ECGConstants.CONSTRUCTION);
	private TypeSystem<Schema> schemaTypeSystem = new TypeSystem<Schema>(ECGConstants.SCHEMA);
	private TypeSystem<MapPrimitive> mapTypeSystem = new TypeSystem<MapPrimitive>(ECGConstants.MAP);
	private TypeSystem<Situation> situationTypeSystem = new TypeSystem<Situation>(ECGConstants.SITUATION);
	private TypeSystem<? extends TypeSystemNode> ontologyTypeSystem;
	
	private boolean consistent = false;
	
	private static ECGGrammarFormatter formatter = new SimpleGrammarPrinter();
	private ContextModel contextModel = null;

	
	private String pkg = "global";
	private ArrayList<String> pkgs = new ArrayList<String>(){{
		add("global");
	}};
	private String importRequest = "global";
	private ArrayList<String> importRequests = new ArrayList<String>(){{
		add("global");
	}};

	/** Testing: seantrott. Setting field for package. */
	public String setPackage(String packageName) {
		pkg = packageName;
		return pkg;
	}
	
	public String getPackage() {
		return pkg;
	}
	
	public List<String> getPackages() {
		return pkgs;
	}
	
	public String addPackage(String pkgName) {
		if (!pkgs.contains(pkgName)) {
			pkgs.add(pkgName);
		}
		return pkgName;
	}
	
	/** Adds import to import list, returns import name. */
	public String addImport(String importName) {
		if (!importRequests.contains(importName)) {
			importRequests.add(importName);
		}
		//importRequest.add(importName);
		importRequest = importName;
		return importRequest;
	}
	
	public ArrayList<String> getImport() {
		return importRequests;
	}
	
	
	public void setContextModel(ContextModel cm) {
		this.contextModel = cm;
	}

	public ContextModel getContextModel() {
		return contextModel;
	}

	protected Prefs prefs = null;

	public Prefs getPrefs() {
		return prefs;
	}

	public void setPrefs(Prefs prefs) {
		this.prefs = prefs;
	}

	public void addPrimitives(HashSet<Primitive> primitives) {
		for (Primitive primitive : primitives) {
			addPrimitive(primitive);
		}
		consistent = false;
	}

	public void addPrimitive(Primitive primitive) {
		if (primitive instanceof Construction) {
			addConstruction((Construction) primitive);
		}
		else if (primitive instanceof Schema) {
			addSchema((Schema) primitive);
		}
		else {
			throw new GrammarException("Unanticipated primitive in Grammar.constructor", primitive);
		}
	}

	//@seantrott
	public Construction copyConstruction(Construction cxn) {
		Construction returned = new Construction(cxn.getName(), cxn.getKind(), cxn.getParents(),
        		  								cxn.getFormBlock(), cxn.getMeaningBlock(), cxn.getConstructionalBlock());
		return returned;
		
	}
	
	
	public static void setFormatter(ECGGrammarFormatter f) {
		formatter = f;
	}

	public void addConstruction(Construction cxn) {
		if (cxns.containsKey(cxn.getName())) {
			throw new GrammarException("There are at least two definitions of the construction: " + cxn.getName(), cxn);
		}
		cxns.put(cxn.getName(), cxn);
		consistent = false;
	}

	public void removeConstruction(Construction cxn) {
		cxns.remove(cxn.getName());
		consistent = false;
	}

	public Construction getConstruction(String name) {
		update();
		return cxns.get(name);
	}

	public Map<String, Construction> getAllConstructionsByName() {
		update();
		return cxns;
	}
	
	public Collection<Construction> getCxnsNoUpdate() {
		return cxns.values();
	}

	public Collection<Construction> getAllConstructions() {
		update();
		return cxns.values();
	}

	public int getNumConstructions() {
		return cxns.size();
	}

	public void addSchema(Schema schema) {
		if (schemas.containsKey(schema.getName())) {
			throw new GrammarException("There are at least two definitions of the schema: " + schema.getName());
		}
		schemas.put(schema.getName(), schema);
		consistent = false;
	}

	public void addMap(MapPrimitive map) {
		if (maps.containsKey(map.getName())) {
			throw new GrammarException("There are at least two definitions of the map: " + map.getName(), map);
		}
		maps.put(map.getName(), map);
		consistent = false;
	}

	public void addSituation(Situation situation) {
		if (situations.containsKey(situation.getName())) {
			throw new GrammarException("There are at least two definitions of the situation: " + situation.getName(),
					situation);
		}
		situations.put(situation.getName(), situation);
		consistent = false;
	}

	public Schema getSchema(String name) {
		update();
		return schemas.get(name);
	}

	public MapPrimitive getMap(String name) {
		update();
		return maps.get(name);
	}

	public Situation getSituation(String name) {
		update();
		return situations.get(name);
	}
	
	/** Added @seantrott for testing - building incremental grammars, don't want to update. */
	public Collection<Schema> getSchemasNoUpdate() {
		return schemas.values();
	}

	public Collection<Schema> getAllSchemas() {
		update();
		return schemas.values();
	}

	public Map<String, Schema> getAllSchemasByName() {
		update();
		return schemas;
	}

	public Collection<MapPrimitive> getAllMaps() {
		update();
		return maps.values();
	}

	public Collection<Situation> getAllSituations() {
		update();
		return situations.values();
	}

	public String toString() {
		update();
		return formatter.format(this);
	}

	public TypeSystem<Schema> getSchemaTypeSystem() {
		update();
		return schemaTypeSystem;
	}

	public TypeSystem<Situation> getSituationTypeSystem() {
		update();
		return situationTypeSystem;
	}

	public TypeSystem<MapPrimitive> getMapTypeSystem() {
		update();
		return mapTypeSystem;
	}

	public TypeSystem<Construction> getCxnTypeSystem() {
		update();
		return cxnTypeSystem;
	}

	public TypeSystem<? extends TypeSystemNode> getOntologyTypeSystem() {
		if (contextModel == null) {
			return ontologyTypeSystem;
		}
		return contextModel.getTypeSystem();
	}

	public void setOntologyTypeSystem(TypeSystem<? extends TypeSystemNode> ontologyTypeSystem) {
		if (contextModel != null)
			throw new IllegalArgumentException("ontologyTypeSystem cannot be set is contextModel is non-null");
		
		this.ontologyTypeSystem = ontologyTypeSystem;
	}

	public void update() {
		if (consistent) {
			return;
		}
		consistent = true;
		StringBuffer errors = GrammarChecker.checkGrammar(this);
		if (errors.length() > 1 && GrammarChecker.getErrorListener() == GrammarChecker.DEFAULT_ERROR_LISTENER) {
			// If the errorListener is not the default one all these errors have
			// already been caught (hopefully).
			// System.out.println(errors.toString());
			throw new GrammarException("The constructions and schemas in your grammar had these errors:\n" +
			// "-----------------------------------------------------------------------------------------------------\n"+
					errors.toString());
		}
	}

	public abstract class Primitive implements TypeSystemNode, ILocatable {
		
		/* New field for package name. */
		protected String pkg;
		
		/** Returns the package type for construction. Constructions are defined as part of a package.
		 * (Default is "global" in grammar). *
		 * @return package name that construction is a part of. ("Global" is default").
		 */
		public String getPackage() {
			return pkg;
		}
		
		/** Sets package name.
		 * 
		 * @param 
		 * @return Returns the string representation of the package name.
		 */
		public String setPackage(String packageName) {
			pkg = packageName;
			return pkg;
		}

		protected String name;
		protected Set<String> parents;

		/**
		 * The file this <code>Primitive</code> was defined in.
		 */
		protected Location location;

		/**
		 * It returns the file this <code>Primitive</code> was defined in.
		 * 
		 * @return the file name containing this <code>Primitive</code>'s definition.
		 */
		@Override
		public Location getLocation() {
			return location;
		}

		@Override
		public void setLocation(Location location) {
			this.location = location;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override 
		public String getType() {
			return getName();
		}

		@Override 
		public void setType(String type) {
			setName(type);
		}

		public void setParents(Set<String> parents) {
			this.parents = parents;
		}

		@Override 
		public Set<String> getParents() {
			return parents;
		}

		abstract public Set<Role> getAllRoles();

		@Override 
		public int compareTo(TypeSystemNode other) {
			if (! (other instanceof Primitive)) {
				throw new ClassCastException("Error in casting " + other.toString() + "to a Primitive");
			}
			return this.name.compareTo(((Primitive) other).name);
		}

	}

	public final class Block {

		private Set<Role> evokedElements = new LinkedHashSet<Role>();
		private Set<Constraint> constraints = new LinkedHashSet<Constraint>();
		private Set<Role> elements = new LinkedHashSet<Role>(); // roles,
		// features,
		// constituents
		// go here
		private String kind;
		private String type = ECGConstants.UNTYPED;
		private TypeSystem blockTypeTypeSystem = null;
		private String typeSource = null;

		public Block(String kind, String type) {
			this.kind = kind;
			this.type = type;
		}

		public Block clone() {
			return clone(true);
		}

		public Block clone(boolean shallow) {
			Block clone = new Block(kind, type);
			clone.blockTypeTypeSystem = blockTypeTypeSystem;
			clone.typeSource = typeSource;

			Map<Role, Role> roleMap = new HashMap<Role, Role>();

			if (shallow) {
				clone.evokedElements = new LinkedHashSet<Role>(evokedElements);
				clone.elements = new LinkedHashSet<Role>(elements);
				clone.constraints = new LinkedHashSet<Constraint>(constraints);
			}
			else {
				clone.evokedElements = new LinkedHashSet<Role>();
				for (Role r : evokedElements) {
					Role rClone = r.clone();
					clone.evokedElements.add(rClone);
					roleMap.put(r, rClone);
				}
				clone.elements = new LinkedHashSet<Role>();
				for (Role r : elements) {
					Role rClone = r.clone();
					clone.elements.add(rClone);
					roleMap.put(r, rClone);
				}
				clone.constraints = new LinkedHashSet<Constraint>();
				for (Constraint c : constraints) {
					clone.constraints.add(c.clone());
				}
			}
			return clone;
		}

		public void setTypeSource(String typeSource) {
			this.typeSource = typeSource;
		}

		public String getTypeSource() {
			return typeSource;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public TypeConstraint getTypeConstraint() {
			return blockTypeTypeSystem == null ? null : blockTypeTypeSystem.getCanonicalTypeConstraint(type);
		}

		public TypeSystem getBlockTypeTypeSystem() {
			return blockTypeTypeSystem;
		}

		public void setBlockTypeTypeSystem(TypeSystem t) {
			blockTypeTypeSystem = t;
		}

		public String getKind() {
			return kind;
		}

		public Set<Role> getEvokedElements() {
			return evokedElements;
		}

		public void setEvokedElements(Set<Role> evoked) {
			this.evokedElements = evoked;
		}

		public Set<Constraint> getConstraints() {
			return constraints;
		}

		public void setConstraints(Set<Constraint> constraints) {
			this.constraints = constraints;
		}

		public Set<Role> getElements() {
			return elements;
		}

		public void setElements(Set<Role> elements) {
			this.elements = elements;
		}

		public Role getRole(String name) {
			for (Role r : elements) {
				if (r.getName().equals(name)) {
					return r;
				}
			}
			for (Role r : evokedElements) {
				if (r.getName().equals(name)) {
					return r;
				}
			}
			return null;
		}
	}

	public final class Construction extends Primitive implements Rule<String> {

		private String kind; // filled by either abstract or concrete in
		// ECGConstants
		private Block formBlock = null;
		private Block meaningBlock = null;
		private Block constructionalBlock = null;
		private Set<Role> optionals = null;
		private Set<Role> complements = null;
		private Role extraPosedRole = null;
		private Set<Role> allRoles = null;

		
		
		public Construction(String name, String kind, Set<String> parents, Block formBlock, Block meaningBlock,
				Block constructionalBlock) {
			this.name = name;
			this.parents = parents;
			this.formBlock = formBlock;
			this.meaningBlock = meaningBlock;
			this.constructionalBlock = constructionalBlock;
			this.kind = kind;
		}
		

		public void setMeaningBlock(Block meaningBlock) {
			this.meaningBlock = meaningBlock;
		}

		public Block getMeaningBlock() {
			return meaningBlock;
		}

		public Block getFormBlock() {
			return formBlock;
		}

		public Block getConstructionalBlock() {
			return constructionalBlock;
		}

		public String getLHS() {
			return getName();
		}

		public String getKind() {
			return kind;
		}

		/**
		 * @return Constituents marked as 'optional'
		 */
		public Set<Role> getOptionals() {
			return optionals;
		}

		public void setOptionals(Set<Role> optionals) {
			this.optionals = optionals;
		}

		/**
		 * @return Required constituents (i.e. those not marked 'optional')
		 */
		public Set<Role> getComplements() {
			return complements;
		}

		/**
		 * @return Union of complements and optionals
		 */
		public Set<Role> getConstituents() {
			Set<Role> constituents = new HashSet<Role>(complements);
			constituents.addAll(optionals);
			return constituents;
		}

		public Set<Role> getConstructionalFeatures() {
			Set<Role> roles = new HashSet<Role>();
			if (constructionalBlock.getType().equals(ECGConstants.UNTYPED) == false) {
				roles.addAll(schemaTypeSystem.get(constructionalBlock.getType()).getAllRoles());
			}
			return roles;
		}

		public void setComplements(Set<Role> complements) {
			this.complements = complements;
		}

		public Set<Role> getAllRoles() {
			if (allRoles == null) {
				HashSet<Role> roles = new LinkedHashSet<Role>();
				roles.addAll(constructionalBlock.getElements());
				roles.addAll(getConstructionalFeatures());
				// roles.addAll(optionals);

				Role f = ECGConstants.UNTYPEDFROLE;
				if (formBlock.getType().equals(ECGConstants.UNTYPED) == false) {
					f = new Role("f");
					f.setTypeConstraint(formBlock.getBlockTypeTypeSystem().getCanonicalTypeConstraint(formBlock.getType()));
				}

				Role m = ECGConstants.UNTYPEDMROLE;
				if (meaningBlock.getType().equals(ECGConstants.UNTYPED) == false) {
					m = new Role("m");
					m.setTypeConstraint(meaningBlock.getBlockTypeTypeSystem().getCanonicalTypeConstraint(
							meaningBlock.getType()));
				}
				roles.add(f);
				roles.add(m);
				if (schemaTypeSystem.get(ECGConstants.DISCOURSESEGMENTTYPE) != null) {
					Role ds = ECGConstants.DSROLE;
					ds.setTypeConstraint(schemaTypeSystem.getCanonicalTypeConstraint(ECGConstants.DISCOURSESEGMENTTYPE));
					roles.add(ds);
				}
				// ADDED (lucag): begin
				// Add a focus role only in the situation type system defines a
				// certain type (see ECGConstants)
				if (situationTypeSystem.get(ECGConstants.SITUATIONTYPE) != null) {
					Role focus = ECGConstants.FOCUSROLE;
					focus.setTypeConstraint(situationTypeSystem.getCanonicalTypeConstraint(ECGConstants.SITUATIONTYPE));
					roles.add(focus);
				}
				// ADDED (lucag): end
				roles.addAll(meaningBlock.getEvokedElements());
				allRoles = roles;
			}
			return allRoles;

		}

		public TypeSystem<Schema> getSchemaTypeSystem() {
			return schemaTypeSystem;
		}

		public TypeSystem<Construction> getCxnTypeSystem() {
			return cxnTypeSystem;
		}

		// ADDED: (lucag) begin

		public TypeSystem<MapPrimitive> getMapTypeSystem() {
			return mapTypeSystem;
		}

		public TypeSystem<Situation> getSituationTypeSystem() {
			return situationTypeSystem;
		}

		// ADDED: (lucag) end

		// Changed: the return type used to be TypeSystem<Miniontology.Type>
		// (lucag)
		public TypeSystem<? extends TypeSystemNode> getExternalTypeSystem() {
			if (contextModel == null)
				return getOntologyTypeSystem();
			else
				return contextModel.getTypeSystem();
		}

		public boolean isExtraPosedRole(Role role) {
			return role == extraPosedRole;
		}

		public Role getExtraPosedRole() {
			return extraPosedRole;
		}

		public void setExtraPosedRole(Role role) {
			extraPosedRole = role;
		}

		public boolean isConcrete() {
			return kind == ECGConstants.CONCRETE;
		}

		public String toString() {
			return formatter.format(this);
		}

	}

	public final class Schema extends Primitive {

		/** Filled by either form, meaning, constructional in ECGConstants */
		String kind;

		Block contents;

		public Schema(String name, String kind, Set<String> parents, Block contents) {
			this.name = name;
			this.kind = kind;
			this.parents = parents;
			this.contents = contents;
			contents.setType(name);
			contents.setBlockTypeTypeSystem(schemaTypeSystem);
		}

		public Block getContents() {
			return contents;
		}
		
		public String getKind() {
			return this.kind;
		}

		public Set<Role> getAllRoles() {
			Set<Role> roles = new LinkedHashSet<Role>();
			roles.addAll(getContents().getElements());
			roles.addAll(getContents().getEvokedElements());
			return roles;
		}

		public Role getRole(String name) {
			for (Role r : getAllRoles()) {
				if (r.getName().equals(name)) {
					return r;
				}
			}
			return null;
		}

		public String toString() {
			return formatter.format(this);
		}

	}

	public static final class ECGSlotChain extends SlotChain {

		boolean startedWithSelf = false;

		public ECGSlotChain() {
		}

		public ECGSlotChain(String slotChain) {
			StringTokenizer st = new StringTokenizer(slotChain, ".");
			chain = new ArrayList<Role>();
			while (st.hasMoreTokens())
				chain.add(new Role(st.nextToken()));
			removeSelf();
			internChain();
		}

		public ECGSlotChain(Role r) {
			setChain(new ArrayList<Role>());
			chain.add(r);
			removeSelf();
			internChain();
		}

		public ECGSlotChain(String slotPrefix, Role r) {
			this(slotPrefix);
			chain.add(r);
			internChain();
		}

		public ECGSlotChain(String slotPrefix, SlotChain sc) {
			this(slotPrefix);
			chain.addAll(sc.getChain());
			internChain();
		}

		public ECGSlotChain(Role r1, Role r2) {
			setChain(new ArrayList<Role>());
			chain.add(r1);
			chain.add(r2);
			internChain();
		}

		public ECGSlotChain(Role r1, SlotChain r2) {
			setChain(new ArrayList<Role>());
			chain.add(r1);
			chain.addAll(r2.getChain());
			internChain();
		}

		public ECGSlotChain setChain(List<Role> chain) {
			this.chain = chain;
			builtString = false;
			removeSelf();
			internChain();
			return this;
		}

		public ECGSlotChain subChain(int fromIndex) {
			return subChain(fromIndex, chain.size());
		}

		public ECGSlotChain subChain(int fromIndex, int toIndex) {
			List<Role> newChain = new ArrayList<Role>(chain.subList(fromIndex, toIndex));
			ECGSlotChain newSlotChain = new ECGSlotChain();
			newSlotChain.setChain(newChain);
			return newSlotChain;
		}

		private void removeSelf() {
			if (chain.size() > 0 && chain.get(0).getName().equalsIgnoreCase(ECGConstants.SELF)) {
				chain = chain.subList(1, chain.size());
				startedWithSelf = true;
			}
		}

		public boolean startsWithSelf() {
			return startedWithSelf;
		}

		public void internChain() {
			cachedToString = interner.intern(super.toString());
			cachedHashCode = cachedToString.hashCode();
		}

		public ECGSlotChain clone() {
			ECGSlotChain clone = new ECGSlotChain().setChain(new ArrayList<Role>(getChain()));
			clone.startedWithSelf = startedWithSelf;
			return clone;
		}

		public String toString() {
			if (chain.size() > 0 && startedWithSelf) {
				return "self." + super.toString();
			}
			else if (chain.size() > 0 && !startedWithSelf) {
				return super.toString();
			}
			else {
				return "self";
			}
		}

	}

	public final class MapPrimitive extends Primitive {

		/** Filled by either form, meaning, constructional in ECGConstants */
		String kind;

		Block contents;

		public MapPrimitive(String name, String kind, Set<String> parents, Block contents) {
			this.name = name;
			this.kind = kind;
			this.parents = parents;
			this.contents = contents;
			contents.setType(name);
			contents.setBlockTypeTypeSystem(mapTypeSystem);
		}

		@Override
		public Set<Role> getAllRoles() {
			Set<Role> roles = new LinkedHashSet<Role>();
			roles.addAll(contents.getElements());
			roles.addAll(contents.getEvokedElements());
			return roles;
		}

		/**
		 * @return the contents
		 */
		public Block getContents() {
			return contents;
		}

		@Override
		public String toString() {
			return formatter.format(this);
		}
	}

	public final class Situation extends Primitive {

		/** Filled by either form, meaning, constructional in ECGConstants */
		String kind;

		Block contents;

		public Situation(String name, String kind, Set<String> parents, Block contents) {
			this.name = name;
			this.kind = kind;
			this.parents = parents;
			this.contents = contents;
			contents.setType(name);
			contents.setBlockTypeTypeSystem(situationTypeSystem);
		}

		@Override
		public Set<Role> getAllRoles() {
			Set<Role> roles = new LinkedHashSet<Role>();
			roles.addAll(contents.getElements());
			return roles;
		}

		/**
		 * @return the contents
		 */
		public Block getContents() {
			return contents;
		}

		@Override
		public String toString() {
			return formatter.format(this);
		}

	}
}

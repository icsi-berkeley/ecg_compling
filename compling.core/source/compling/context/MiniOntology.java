/**
 * MiniOntology is a class representing a lightweight ontology system with situations. In other words, a database for
 * representing entities and relations between them.
 * 
 * By design in this simple system, types and relation(types) hold over all situations. However the individuals that
 * exist and the bindings between them might vary depending on the situation. Additionally, the situations are ordered
 * in terms of containment and temporal precedence.
 * 
 * This class was designed for simplicity and not for speed.
 */

package compling.context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.context.ContextException.ItemNotDefinedException;
import compling.context.ContextException.NoInstanceFoundException;
import compling.context.ContextUtilities.MiniOntologyFormatter;
import compling.context.ContextUtilities.SimpleOntologyPrinter;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ecgreader.ILocatable;
import compling.grammar.ecg.ecgreader.Location;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.util.PackageHandler;
import compling.util.Pair;

public class MiniOntology extends PackageHandler {

	// FIXME: I don't think the current MiniOntology implementation actually
	// supports blocking.
	// The parameter is passed into addRelation but then is not processed.

	static final String STRING = "STRING";

	static final String BASE = "BASE";
	static final String PRIOR_INTERVAL = "PRIORINTERVAL";
	static final String CURRENT_INTERVAL = "CURRENTINTERVAL";
	static final String NONE = "NONE";

	public static final String INDIVIDUAL = "INDIVIDUAL";
	public static final String INDIVIDUALNAME = "Individual";
	public static final String INTERVAL = "INTERVAL";
	public static final String INTERVALNAME = "Interval";
	public static final String STRINGVALUE = "STRINGVALUE";

	public static final String SETNAME = "Set";
	public static final String SETSIZE = "set_size";
	public static final String MEMBER = "member";
	public static final String SINGLETONNAME = "Singleton";
	public static final String PAIRNAME = "Pair";
	public static final String MULTITUDENAME = "Multitude";
	public static final String GROUPNAME = "Group";
	

	public static final String TIMESTAMP = "timestamp"; // eva
	private int timestampCounter = 0;
	
	

	TypeSystem<Type> typeSystem = new TypeSystem<Type>(ECGConstants.ONTOLOGY);

	HashMap<String, List<Relation>> relations = new HashMap<String, List<Relation>>();
	HashMap<String, Interval> intervals = new HashMap<String, Interval>();

	Interval currentInterval;

	boolean consistentTypeSystem = false;
	boolean verbose = false;

	List<Individual> recentlyAccessedIndividuals;

	private static MiniOntologyFormatter formatter = new SimpleOntologyPrinter();

	protected static Logger logger = Logger.getLogger(MiniOntology.class.getName());

	MiniOntology(boolean verbose) {
		typeSystem.addType(new Type(STRING, new ArrayList<String>()));
		typeSystem.addType(new Type(INDIVIDUALNAME, new ArrayList<String>()));

		List<String> parents = new ArrayList<String>();
		parents.add(INDIVIDUALNAME);
		typeSystem.addType(new Type(INTERVALNAME, parents));
		typeSystem.addType(new Type(SETNAME, parents));

		parents = new ArrayList<String>();
		parents.add(SETNAME);
		typeSystem.addType(new Type(SINGLETONNAME, parents));
		typeSystem.addType(new Type(GROUPNAME, parents));

		parents = new ArrayList<String>();
		parents.add(GROUPNAME);
		typeSystem.addType(new Type(PAIRNAME, parents));
		typeSystem.addType(new Type(MULTITUDENAME, parents));

		addRelation(SETSIZE, INDIVIDUALNAME, SETNAME);
		addRelation(MEMBER, SETNAME, INDIVIDUAL);
		addRelation(TIMESTAMP, INDIVIDUALNAME, STRING); // eva

		currentInterval = new Interval(BASE, INTERVALNAME, null, null); // used to be "Interval"
		intervals.put(BASE, currentInterval);
		this.verbose = verbose;
	}

	MiniOntology() {
		this(false);
	}

	abstract class Value {
		abstract String getKindOfValue();

		abstract String getName();

		abstract public String toString();
	}

	public class Individual extends Value {
		String name;
		String type;
		
		private String packageName;
		
		public String getPackage() {
			return packageName;
		}
		
		public void setPackage(String pkg) {
			packageName = pkg;
		}

		Individual(String name, String type) {
			this.name = name;
			this.type = typeSystem.getInternedString(type);
		}

		String getKindOfValue() {
			return INDIVIDUAL;
		}

		public Type getType() {
			return typeSystem.get(type);
		}

		public String getTypeName() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			return formatter.format(this);
		}

	}

	public class Interval extends Individual {

		Interval parent = null; // containing interval
		Interval prior = null; // temporally prior interval
		HashMap<String, Individual> individuals;
		HashMap<Relation, List<RelationFiller>> relnToRelationFillers;
		HashMap<Individual, List<RelationFiller>> holderToRelationFillers;
		HashMap<Value, List<RelationFiller>> fillerToRelationFillers;
		List<RelationFiller> persistentRelationFillers;

		Interval(String name, String type, Interval parent, Interval prior) {
			super(name, type);
			this.parent = parent;
			this.prior = prior;
			this.individuals = new HashMap<String, Individual>();
			this.relnToRelationFillers = new HashMap<Relation, List<RelationFiller>>();
			this.holderToRelationFillers = new HashMap<Individual, List<RelationFiller>>();
			this.fillerToRelationFillers = new HashMap<Value, List<RelationFiller>>();
			persistentRelationFillers = new ArrayList<RelationFiller>();
			if (prior != null) {
				setPrecedingInterval(prior);
			}
		}

		Interval getParent() {
			return parent;
		}

		Interval getPrior() {
			return prior;
		}

		void setPrecedingInterval(Interval prior) {
			for (RelationFiller rf : prior.persistentRelationFillers) {
				RelationFiller newCopy = rf; // new
				// RelationFiller(rf.getRelation(),
				// rf.getHolder(), rf.getFiller());
				addRelationFiller(newCopy);
				Individual localHolder = null;
				localHolder = getIndividual(rf.getHolder().getName());
				if (localHolder == null) {
					addIndividual(rf.getHolder());
				}
				else if (rf.getHolder() != localHolder) {
					throw new ContextException("Funny name conflict in persistent link between "
							+ rf.getHolder().getKindOfValue() + " and " + localHolder.getName());
				}
				else { /*
						 * do nothing because the name is already there, but it's the same element
						 */
				}

				if (rf.getFiller().getKindOfValue() != STRINGVALUE) {
					Individual i = (Individual) rf.getFiller();
					Individual localFiller = null;
					try {
						localFiller = getIndividual(i.getName());
					}
					catch (ContextException ce) {
					}
					if (localFiller == null) {
						addIndividual(i);
					}
					else if (i != localFiller) {
						throw new ContextException("Funny name conflict in persistent link between "
								+ rf.getHolder().getKindOfValue() + " and " + localHolder.getName());
					}
					else { /* no issue, same as before */
					}
				}
				// persistentRelationFillers.add(newCopy); this functionality
				// got
				// moved to addrelfiller
			}
		}

		String getKindOfValue() {
			return INTERVAL;
		}

		void addIndividual(Individual individual) {
			individuals.put(individual.getName(), individual);
		}

		Individual getIndividual(String name) {
			Interval i = this;
			while (i != null) {
				if (i.individuals.get(name) != null) {
					return i.individuals.get(name);
				}
				i = i.parent;
			}
			// throw new ContextException("Unknown individual: " + name);
			return null;
		}

		List<Individual> getAllIndividualsOfType(String type) {
			List<Individual> individuals = new ArrayList<Individual>();
			Interval interval = this;
			try {
				while (interval != null) {
					for (Individual ind : interval.individuals.values()) {
						if (typeSystem.subtype(ind.type, type)) {
							individuals.add(ind);
						}
					}
					interval = interval.parent;
				}
			}
			catch (TypeSystemException t) {
				throw new ContextException("Error while retrieving individuals of type " + type
						+ ". Error message given was : " + t.getMessage(), t);
			}
			return individuals;
		}

		Set<Individual> getAllIndividuals() {
			return new HashSet<Individual>(individuals.values());
		}

		boolean existingRelationFiller(String relnName, String holderName, String valueName) {
			Individual holder = null;
			if (holderName.equals(CURRENT_INTERVAL)) {
				holder = getCurrentInterval();
			}
			else if (holderName.equals(PRIOR_INTERVAL)) {
				holder = getCurrentInterval().getPrior();
			}
			else {
				holder = getCurrentInterval().getIndividual(holderName);
			}
			if (holder == null) {
				throw new ItemNotDefinedException("Error while removing a relation filler from the instance" + holderName
						+ ". It is not defined in the ontology.", holderName);
			}
			if (holderToRelationFillers.get(holder) == null) {
				return false;
			}
			for (RelationFiller rf : holderToRelationFillers.get(holder)) {
				if (rf.getRelation().getName().equals(relnName)) {
					// System.out.println(rf);
					if (valueName.charAt(0) == '\"') {
						if (rf.getFiller() instanceof StringValue
								&& ((StringValue) rf.getFiller()).getValue().equals(valueName)) {
							return true;
						}
					}
					else {
						Individual v = null;
						if (valueName.equals(CURRENT_INTERVAL)) {
							v = getCurrentInterval();
						}
						else if (valueName.equals(PRIOR_INTERVAL)) {
							v = getCurrentInterval().getPrior();
						}
						else {
							v = getCurrentInterval().getIndividual(valueName);
						}
						if (v != null && rf.getFiller() == v) {
							return true;
						}
					}
				}
			}
			return false;
		}

		List<RelationFiller> getAllRelationFillers(String relationName) {
			List<RelationFiller> relationFillers = new ArrayList<RelationFiller>();
			Interval interval = this;
			while (interval != null) {
				if (relations.get(relationName) != null) {
					for (Relation reln : relations.get(relationName)) {
						if (interval.relnToRelationFillers.get(reln) != null) {
							// System.out.println("adding filler");
							// System.out.println(interval.relnToRelationFillers.get(reln));
							relationFillers.addAll(interval.relnToRelationFillers.get(reln));
						}
					}
				}
				interval = interval.parent;
			}
			return relationFillers;
		}

		void removeRelationFiller(RelationFiller toRemove) {
			if (relnToRelationFillers.get(toRemove.getRelation()) == null) {
				throw new NoInstanceFoundException("No instances of the relation " + toRemove.getRelation().toString()
						+ " is found.");
			}
			List<RelationFiller> toRemoveList = new ArrayList<RelationFiller>();
			for (RelationFiller rf : relnToRelationFillers.get(toRemove.getRelation())) {
				if (rf.equals(toRemove)) {
					toRemoveList.add(rf);
				}
			}
			for (RelationFiller rf : toRemoveList) {
				relnToRelationFillers.get(toRemove.getRelation()).remove(rf);
				holderToRelationFillers.get(toRemove.getHolder()).remove(rf);
				fillerToRelationFillers.get(toRemove.getFiller()).remove(rf);
				persistentRelationFillers.remove(rf);
			}
		}

		void removeRelationFillersAbout(Relation reln, Individual holder) {
			if (relnToRelationFillers.get(reln) == null) {
				throw new NoInstanceFoundException("No instances of the relation " + reln.getName()
						+ " is found on the holder " + holder.getName());
			}
			List<RelationFiller> rfs = new ArrayList<RelationFiller>();
			for (RelationFiller rf : relnToRelationFillers.get(reln)) {
				if (rf.getRelation() == reln && rf.getHolder() == holder) {
					rfs.add(rf);
				}
			}

			for (RelationFiller rf : rfs) {
				relnToRelationFillers.get(rf.getRelation()).remove(rf);
				holderToRelationFillers.get(rf.getHolder()).remove(rf);
				fillerToRelationFillers.get(rf.getFiller()).remove(rf);
				persistentRelationFillers.remove(rf);
			}
		}

		void addRelationFiller(RelationFiller rf) {
			if (verbose) {
				System.out.println("Adding relation filler: " + rf.toString());
			}
			if (rf.getRelation().isPersistent()) {
				persistentRelationFillers.add(rf);
			}
			if (relnToRelationFillers.get(rf.getRelation()) == null) {
				relnToRelationFillers.put(rf.getRelation(), new ArrayList<RelationFiller>());
			}
			relnToRelationFillers.get(rf.getRelation()).add(rf);
			if (holderToRelationFillers.get(rf.getHolder()) == null) {
				holderToRelationFillers.put(rf.getHolder(), new ArrayList<RelationFiller>());
			}
			holderToRelationFillers.get(rf.getHolder()).add(rf);
			if (fillerToRelationFillers.get(rf.getFiller()) == null) {
				fillerToRelationFillers.put(rf.getFiller(), new ArrayList<RelationFiller>());
			}
			fillerToRelationFillers.get(rf.getFiller()).add(rf);
		}

		public String toString() {
			return formatter.format(this);
		}
	}

	class StringValue extends Value {

		String value;

		StringValue(String value) {
			this.value = value;
		}

		String getKindOfValue() {
			return STRINGVALUE;
		}

		String getValue() {
			return value;
		}

		String getName() {
			return getValue();
		}

		public String toString() {
			return value;
		}

		public boolean equals(Object o) {
			if (o instanceof StringValue == false) {
				return false;
			}
			return value.equals(((StringValue) o).getValue());
		}

		public int hashCode() {
			return value.hashCode();
		}
	}

	public class Type implements TypeSystemNode, ILocatable {
		String typeName;
		Set<String> parentNames = new HashSet<String>();
		HashMap<String, Relation> relations;
		Set<Pair<String, String>> coindexation;
		HashMap<Relation, Set<Relation>> relnToCoindexationSet;
		Location location;

		Type(String typeName, List<String> parentNames) {
			this.typeName = typeName;
			if (parentNames.size() > 0) {
				this.parentNames.addAll(parentNames);
			}
			else if (typeName.equals(INDIVIDUALNAME) == false) {
				this.parentNames.add(INDIVIDUALNAME);
			}
			this.relations = new HashMap<String, Relation>();
			this.relnToCoindexationSet = new HashMap<Relation, Set<Relation>>();
			this.coindexation = new HashSet<Pair<String, String>>();
		}

		public String getType() {
			return typeName;
		}

		public void setType(String type) {
			this.typeName = type;
		}

		public Set<String> getParents() {
			return parentNames;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		void addRelation(Relation reln) {
			if (verbose) {
				System.out.println("Adding relation " + reln.toString() + " to type: " + typeName);
			}
			relations.put(reln.getName(), reln);
			Set<Relation> temp = new HashSet<Relation>();
			temp.add(reln);
			relnToCoindexationSet.put(reln, temp);
		}

		Collection<Relation> getRelations() {
			return relations.values();
		}

		public Relation getRelation(String name) {
			return relations.get(name);
		}

		void addCoindexation(String reln1, String reln2) {
			coindexation.add(new Pair<String, String>(reln1, reln2));
		}

		void coindex(Relation reln1, Relation reln2) {
			if (verbose) {
				System.out.println("Storing the coindexation relation between: " + reln1.toString() + " and "
						+ reln2.toString() + " in type: " + typeName);
			}
			Set<Relation> coindexedRelations = relnToCoindexationSet.get(reln1);
			coindexedRelations.addAll(relnToCoindexationSet.get(reln2));
			for (Relation r : coindexedRelations) {
				relnToCoindexationSet.put(r, coindexedRelations);
			}
		}

		void setupCoindexation() {
			for (Pair<String, String> coindexed : coindexation) {
				Relation reln1 = relations.get(coindexed.getFirst());
				Relation reln2 = relations.get(coindexed.getSecond());
				if (reln1 == null || reln2 == null) {
					// System.out.println(relations);
					throw new ContextException("Relation " + (reln1 == null ? coindexed.getFirst() : coindexed.getSecond())
							+ " is undefined for coindexation in type: " + typeName);
				}
				coindex(reln1, reln2);
			}
		}

		public Set<Relation> getCoindexedSet(String reln) {
			if (relnToCoindexationSet.get(relations.get(reln)) == null) {
				// System.out.println(relnToCoindexationSet);
				throw new ContextException("Relation: " + reln + " is not in relnToCoindexationSet in type " + typeName);
			}
			return relnToCoindexationSet.get(relations.get(reln));
		}

		@Override
		public int compareTo(TypeSystemNode other) {
			if (!(other instanceof MiniOntology.Type)) {
				throw new ClassCastException("Error in casting " + other.toString() + "to a MiniOntology.Type");
			}
			return this.typeName.compareTo(((MiniOntology.Type) other).getType());
		}

		@Override
		public String toString() {
			return formatter.format(this);
		}

	}

	public class Relation {

		String name;
		String domain;
		String range;
		boolean persistent = false; // if true, the fillers will carry over into

		// the successor interval

		Relation(String name, String domainType, String rangeType, boolean persistent) {
			this.name = name;
			this.domain = domainType;
			this.range = rangeType;
			this.persistent = persistent;
		}

		public String getName() {
			return name;
		}

		public String getDomain() {
			return domain;
		}

		public String getRange() {
			return range;
		}

		public String toString() {
			return formatter.format(this);
		}

		public boolean isPersistent() {
			return persistent;
		}
	}

	class RelationFiller {

		Relation reln;
		Individual holder;
		Value filler;

		RelationFiller(Relation reln, Individual holder, Value filler) {
			this.reln = reln;
			this.holder = holder;
			this.filler = filler;
		}

		Relation getRelation() {
			return reln;
		}

		Individual getHolder() {
			return holder;
		}

		void updateHolder(Individual i) {
			holder = i;
		}

		Value getFiller() {
			return filler;
		}

		boolean isPersistent() {
			return reln.persistent;
		}

		void setFiller(Value val) {
			filler = val;
		}

		public String toString() {
			return "( " + reln.getName() + " " + holder.getName() + " " + filler.getName() + " )";
		}

		public boolean equals(Object o) {
			if (o instanceof RelationFiller == false) {
				return false;
			}
			RelationFiller other = (RelationFiller) o;
			return reln.equals(other.reln) && holder.equals(other.holder) && filler.equals(other.filler);
		}

		public int hashCode() {
			throw new UnsupportedOperationException("RelationFiller.hashCode() is not implemented!");
		}
	}

	public void addType(String name, List<String> parents) {
		Type t = new Type(name, parents);
		typeSystem.addType(t);
		if (verbose) {
			System.out.println("New Type: " + name + " with parents: " + parents);
		}
		consistentTypeSystem = false;
	}

	void addRelation(String name, String domain, String range) {
		addRelation(name, domain, range, false, false);
	}

	void addFunction(String name, String domain, String range, boolean persistent, boolean blocking) {
		addRelation(name, domain, range, persistent, blocking);
	}

	void addRelation(String name, String domain, String range, boolean persistent, boolean blocking) {
		Relation reln = new Relation(name, domain, range, persistent);
		Type type = typeSystem.get(domain);
		type.addRelation(reln);
		if (type == null) {
			throw new ItemNotDefinedException("Error while adding relation to type " + domain
					+ ". The type is not defined in the ontology.", domain);
		}
		consistentTypeSystem = false;
		if (relations.get(name) == null) {
			relations.put(name, new ArrayList<Relation>());
		}
		relations.get(name).add(reln);
		if (verbose) {
			System.out.println("New relation: " + reln.toString());
		}
	}

	void addCoindexation(String typeName, String reln1, String reln2) {
		Type type = typeSystem.get(typeName);
		if (type == null) {
			throw new ItemNotDefinedException("Error while adding coindexation constraints to type " + typeName
					+ ". The type is not defined in the ontology.", typeName);
		}
		type.addCoindexation(reln1, reln2);
		consistentTypeSystem = false;
		if (verbose) {
			System.out.println("In type: " + type + " coindexing relations: " + reln1 + " and " + reln2);
		}
	}
	
	public void addIndividual(String name, String type) {
		Type ontType = typeSystem.get(type);
		if (ontType == null) {
			throw new ItemNotDefinedException("Error while adding an individual of type " + type
					+ ". The type is not defined in the ontology.", type);
		}

		Individual i = new Individual(name, type);
		recentlyAccessedIndividuals.add(i);
		getCurrentInterval().addIndividual(i);
		String setname = SINGLETONNAME + name;
		getCurrentInterval().addIndividual(new Individual(setname, SINGLETONNAME));
		addRelationFiller(MEMBER, setname, name);
		addRelationFiller(SETSIZE, name, setname);
		addRelationFiller(TIMESTAMP, name, "\"" + String.valueOf(timestampCounter) + "\""); // eva
		timestampCounter++; // eva
	}

	public void addIndividual(String name, String type, String pkg) {
		Type ontType = typeSystem.get(type);
		if (ontType == null) {
			throw new ItemNotDefinedException("Error while adding an individual of type " + type
					+ ". The type is not defined in the ontology.", type);
		}

		Individual i = new Individual(name, type);
		i.setPackage(pkg);
		recentlyAccessedIndividuals.add(i);
		getCurrentInterval().addIndividual(i);
		String setname = SINGLETONNAME + name;
		getCurrentInterval().addIndividual(new Individual(setname, SINGLETONNAME));
		addRelationFiller(MEMBER, setname, name);
		addRelationFiller(SETSIZE, name, setname);
		addRelationFiller(TIMESTAMP, name, "\"" + String.valueOf(timestampCounter) + "\""); // eva
		timestampCounter++; // eva
	}

	public void addRelationFiller(String reln, String holderName, String value) {
		Individual holder = null;
		if (holderName.equals(CURRENT_INTERVAL)) {
			holder = getCurrentInterval();
		}
		else if (holderName.equals(PRIOR_INTERVAL)) {
			holder = getCurrentInterval().getPrior();
		}
		else {
			holder = getCurrentInterval().getIndividual(holderName);
		}
		if (holder == null) {
			throw new ItemNotDefinedException("Error while adding a relation to the instance" + holderName
					+ ". It is not defined in the ontology.", holderName);
		}

		recentlyAccessedIndividuals.add(holder);
		Type type = holder.getType();
		Value v = null;
		if (value.charAt(0) == '\"') {
			v = new StringValue(value); // value.substring(1,
			// value.length()-1));
		}
		else {
			if (value.equals(CURRENT_INTERVAL)) {
				v = getCurrentInterval();
			}
			else if (value.equals(PRIOR_INTERVAL)) {
				v = getCurrentInterval().getPrior();
			}
			else {
				v = getCurrentInterval().getIndividual(value);
			}
			recentlyAccessedIndividuals.add((Individual) v);
		}
		if (v == null) {
			throw new ItemNotDefinedException("Null value while trying to add relation filler " + value + " to " + reln
					+ " of " + holderName);
		}
		Interval interval = getCurrentInterval();
		try {
			if (typeSystem.subtype(holder.type, INTERVALNAME)) {
				interval = (Interval) holder;
			}
		}
		catch (TypeSystemException t) {
			throw new ContextException("Error while accessing type " + type + ". Error message given was : "
					+ t.getMessage(), t);
		}
		if (reln.equals(SETSIZE) && interval.existingRelationFiller(reln, holderName, SINGLETONNAME + holderName)) {
			removeAllRelationFillers(SETSIZE, holderName);
		}
		for (Relation relation : type.getCoindexedSet(reln)) {
			if (!interval.existingRelationFiller(relation.getName(), holderName, value)) { // eva's edit 12/9/07. (see if
																														// this
				// way of avoiding
				// duplicates works)
				interval.addRelationFiller(new RelationFiller(relation, holder, v));
			}
		}
	}

	public void removeRelationFiller(String relnName, String holderName, String value) {
		Individual holder = null;
		if (holderName.equals(CURRENT_INTERVAL)) {
			holder = getCurrentInterval();
		}
		else if (holderName.equals(PRIOR_INTERVAL)) {
			holder = getCurrentInterval().getPrior();
		}
		else {
			holder = getCurrentInterval().getIndividual(holderName);
		}
		if (holder == null) {
			throw new ItemNotDefinedException("Error while removing a relation filler from the instance" + holderName
					+ ". It is not defined in the ontology.", holderName);
		}

		Type type = holder.getType();
		// Relation reln = type.getRelation(relnName);
		Value v = null;
		if (value.charAt(0) == '\"') {
			v = new StringValue(value); // value.substring(1,
			// value.length()-1));
		}
		else {
			if (value.equals(CURRENT_INTERVAL)) {
				v = getCurrentInterval();
			}
			else if (value.equals(PRIOR_INTERVAL)) {
				v = getCurrentInterval().getPrior();
			}
			else {
				v = getCurrentInterval().getIndividual(value);
			}
		}
		for (Relation relation : type.getCoindexedSet(relnName)) {
			// modified by Eva on Sept 10, 2007
			// the variable relation was not used -- it seems like the loop
			// should
			// be over all the coindexed relations.
			// getCurrentInterval().removeRelationFiller(new
			// RelationFiller(reln,
			// holder, v));
			getCurrentInterval().removeRelationFiller(new RelationFiller(relation, holder, v));
		}
	}

	public void removeAllRelationFillers(String relnName, String holderName) {
		Individual holder = null;
		if (holderName.equals(CURRENT_INTERVAL)) {
			holder = getCurrentInterval();
		}
		else if (holderName.equals(PRIOR_INTERVAL)) {
			holder = getCurrentInterval().getPrior();
		}
		else {
			holder = getCurrentInterval().getIndividual(holderName);
		}
		if (holder == null) {
			throw new ItemNotDefinedException("Error while removing all relation fillers from the instance" + holderName
					+ ". It is not defined in the ontology.", holderName);
		}
		Relation reln = holder.getType().getRelation(relnName);
		getCurrentInterval().removeRelationFillersAbout(reln, holder);
	}

	Interval getCurrentInterval() {
		return currentInterval;
	}

	Interval getBase() {
		return intervals.get(BASE);
	}

	Map<String, Interval> getAllIntervals() {
		return intervals;
	}

	Interval getInterval(String name) {
		return intervals.get(name);
	}

	public String getCurrentIntervalName() {
		return currentInterval.getName();
	}

	void setCurrentInterval(Interval interval) {
		this.currentInterval = interval;
		recentlyAccessedIndividuals.add(interval);
	}

	public void setCurrentInterval(String name) {
		if (name.equals(CURRENT_INTERVAL)) {
			this.setCurrentInterval(getCurrentInterval());
		}
		else if (name.equals(PRIOR_INTERVAL)) {
			this.setCurrentInterval(getCurrentInterval().getPrior());
		}
		else {
			this.setCurrentInterval(intervals.get(name));
		}
	}

	public void defineNewInterval(String name, String type, String parentName, String predecessorName) {
		if (name.equals(NONE)) {
			throw new ContextException("Cannot define an interval named \"NONE\"");
		}
		Interval parent = null;
		if (parentName.equals(CURRENT_INTERVAL)) {
			parent = getCurrentInterval();
		}
		else if (parentName.equals(PRIOR_INTERVAL)) {
			parent = getCurrentInterval().getPrior();
		}
		else {
			parent = intervals.get(parentName);
		}
		Interval predecessor = null;
		if (predecessorName.equals(CURRENT_INTERVAL)) {
			predecessor = getCurrentInterval();
		}
		else if (predecessorName.equals(PRIOR_INTERVAL)) {
			predecessor = getCurrentInterval().getPrior();
		}
		else {
			predecessor = intervals.get(predecessorName);
		}
		if (parent == null) {
			throw new ItemNotDefinedException("Undefined parent " + parentName + " while trying to add new interval "
					+ name, parentName);
		}
		if (predecessor == null && !predecessorName.equals(NONE)) {
			throw new ItemNotDefinedException("Undefined predecessor " + predecessorName
					+ " while trying to add new interval " + name, predecessorName);
		}
		Interval s = new Interval(name, type, parent, predecessor);
		parent.addIndividual(s);
		intervals.put(name, s);
		recentlyAccessedIndividuals.add(s);
		addRelationFiller(TIMESTAMP, name, "\"" + String.valueOf(timestampCounter) + "\""); // eva
		timestampCounter++; // eva

		if (verbose) {
			System.out.println("adding new interval of type:" + type);
		}
	}

	public void resetMiniOntologyInstances() {
		intervals.clear();
		recentlyAccessedIndividuals.clear();
		currentInterval = new MiniOntology.Interval(BASE, INTERVALNAME, null, null); // used
		// to
		// be
		// "Interval"
		intervals.put(BASE, currentInterval);
	}

	boolean subtype(String childType, String parentType) {
		try {
			return typeSystem.subtype(childType, parentType);
		}
		catch (TypeSystemException t) {
			return false;
		}
	}

	public TypeSystem<Type> getTypeSystem() {
		return typeSystem;
	}

	public List<Individual> getRecentlyAccessedIndividuals() {
		return recentlyAccessedIndividuals;
	}

	public void initRecentlyAccessedIndividuals() {
		recentlyAccessedIndividuals = new ArrayList<Individual>();
	}

	public static void setFormatter(MiniOntologyFormatter f) {
		formatter = f;
	}

	public static MiniOntologyFormatter getFormatter() {
		return formatter;
	}

	public String toString() {
		return formatter.format(this);
	}

	void build() {
		if (consistentTypeSystem == true) {
			if (verbose) {
				System.out.println("No need to rebuild, system still consistent.");
			}
			return;
		}
		if (verbose) {
			System.out.println("Building the type system");
		}
		try {
			typeSystem.build();
		}
		catch (TypeSystemException t) {
			throw new ContextException("Type System error: " + t.getLocalizedMessage());
		}
		List<Type> topoSortedTypes = typeSystem.topologicalSort();
		for (Type type : topoSortedTypes) {
			for (Type parent : typeSystem.getParents(type)) {
				for (Relation r : parent.getRelations()) {
					type.addRelation(r);
					type.coindexation.addAll(parent.coindexation);
				}
			}
			type.setupCoindexation();
		}
		consistentTypeSystem = true;
	}

	static MiniOntology makeMiniOntology(String ontologySpecFileName) {
		try {
			Yylex scanner;
			scanner = new Yylex(new BufferedReader(new FileReader(ontologySpecFileName)));
			scanner.file = ontologySpecFileName;
			MiniOntologyReader mor = new MiniOntologyReader(scanner);
			mor.file = ontologySpecFileName;
			try {
				mor.parse();
				return mor.getMiniOntology();
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new ContextException("Terminal Error: Cannot read grammar. ");
			}
		}
		catch (FileNotFoundException e1) {
			throw new ContextException(e1.toString());
		}

	}
}

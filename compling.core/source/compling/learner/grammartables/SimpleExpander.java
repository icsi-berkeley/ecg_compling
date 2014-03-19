// =============================================================================
// File        : TypeConstraintQueryExpansion.java
// Author      : emok
// Change Log  : Created on Apr 27, 2007
//=============================================================================

package compling.learner.grammartables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import compling.context.MiniOntology.Type;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.grammartables.CooccurrenceTable.QueryExpander;

//=============================================================================

public class SimpleExpander implements QueryExpander<TypeConstraint> {

	public static enum ExpansionType {
		Supertype, Sibling
	}

	protected Grammar grammar;
	protected ExpansionType expansion;
	private TypeSystem<Construction> cxnTypeSystem = null;
	private TypeSystem<Schema> schemaTypeSystem = null;
	private TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = null;

	public SimpleExpander(Grammar grammar, ExpansionType expansion) {
		this.grammar = grammar;
		this.expansion = expansion;
		this.cxnTypeSystem = grammar.getCxnTypeSystem();
		this.schemaTypeSystem = grammar.getSchemaTypeSystem();
		this.ontologyTypeSystem = grammar.getOntologyTypeSystem();
	}

	public Map<TypeConstraint, Double> expandQuery(TypeConstraint a) {

		Map<TypeConstraint, Double> superTypes = new HashMap<TypeConstraint, Double>();

		if (a.getTypeSystem().getName().equals(ECGConstants.CONSTRUCTION)) {
			TypedExpander<Construction> expander = new TypedExpander<Construction>();
			Map<TypeSystemNode, Double> constructions = expander.expand(cxnTypeSystem, cxnTypeSystem.get(a.getType()),
					expansion);
			for (TypeSystemNode cxn : constructions.keySet()) {
				superTypes.put(cxnTypeSystem.getCanonicalTypeConstraint(cxn.getType()), constructions.get(cxn));
			}
		}
		else if (a.getTypeSystem().getName().equals(ECGConstants.SCHEMA)) {
			TypedExpander<Schema> expander = new TypedExpander<Schema>();
			Map<TypeSystemNode, Double> schemas = expander.expand(schemaTypeSystem, schemaTypeSystem.get(a.getType()), expansion);
			for (TypeSystemNode schema : schemas.keySet()) {
				superTypes.put(schemaTypeSystem.getCanonicalTypeConstraint(schema.getType()), schemas.get(schema));
			}
		}
		else if (a.getTypeSystem().getName().equals(ECGConstants.ONTOLOGY)) {
			TypedExpander<? extends TypeSystemNode> expander = new TypedExpander<Type>();
			Map<TypeSystemNode, Double> types = expander.expand(ontologyTypeSystem, ontologyTypeSystem.get(a.getType()), expansion);
			for (TypeSystemNode type : types.keySet()) {
				superTypes.put(ontologyTypeSystem.getCanonicalTypeConstraint(type.getType()), types.get(type));
			}
		}
		else {
			throw new LearnerException("Undefined type system encountered during query expansion");
		}

		return superTypes;
	}

	protected class TypedExpander<N extends TypeSystemNode> {

		protected Map<TypeSystemNode, Double> expand(TypeSystem<? extends TypeSystemNode> ontologyTypeSystem,
				TypeSystemNode typeSystemNode, ExpansionType expansion) {
			if (expansion == ExpansionType.Supertype) {
				return expandSuperType(ontologyTypeSystem, typeSystemNode, 0);
			}
			else if (expansion == ExpansionType.Sibling) {
				return expandSibling(ontologyTypeSystem, typeSystemNode, 0);
			}
			// TODO: log warning
			return null;
		}

		protected Map<TypeSystemNode, Double> expandSuperType(TypeSystem<? extends TypeSystemNode> ontologyTypeSystem,
				TypeSystemNode typeSystemNode, double distance) {
			distance += 1;

			Set<? extends TypeSystemNode> parents = ontologyTypeSystem.getParents(typeSystemNode);
			Map<TypeSystemNode, Double> superTypes = new HashMap<TypeSystemNode, Double>();
			for (TypeSystemNode parent : parents) {
				superTypes.put(parent, distance);
			}

			for (TypeSystemNode parent : parents) {
				Map<TypeSystemNode, Double> grandparents = expandSuperType(ontologyTypeSystem, parent, distance);
				for (TypeSystemNode grandparent : grandparents.keySet()) {
					if (!superTypes.containsKey(grandparent) || grandparents.get(grandparent) < superTypes.get(grandparent)) {
						superTypes.put(grandparent, grandparents.get(grandparent));
					}
				}
			}
			return superTypes;
		}

		protected Map<TypeSystemNode, Double> expandSibling(TypeSystem<? extends TypeSystemNode> ontologyTypeSystem,
				TypeSystemNode typeSystemNode, double distance) {
			distance += 2;

			Set<? extends TypeSystemNode> parents = ontologyTypeSystem.getParents(typeSystemNode);
			Map<TypeSystemNode, Double> siblings = new HashMap<TypeSystemNode, Double>();
			for (TypeSystemNode parent : parents) {
				for (TypeSystemNode sibling : ontologyTypeSystem.getChildren(parent)) {
					siblings.put(sibling, distance);
				}
			}
			siblings.remove(typeSystemNode);
			return siblings;
		}
	}
}
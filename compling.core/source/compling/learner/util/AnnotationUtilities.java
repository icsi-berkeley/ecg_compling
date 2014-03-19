// =============================================================================
//File        : AnnotationUtilities.java
//Author      : emok
//Change Log  : Created on Mar 19, 2008
//=============================================================================

package compling.learner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import compling.context.ContextModel;
import compling.context.MiniOntology.Relation;
import compling.context.MiniOntology.Type;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.grammartables.SchemaCloneTable;
import compling.learner.grammartables.SchemaCloneTable.SchemaInstance;

//=============================================================================

public class AnnotationUtilities {

	public static interface GoldStandardAnnotationLocalizer {
		public String NONE = "none";
		public String DNI = "DNI";
		public String INI = "INI";

		public String ASPECT = "aspect";
		public String MODALITY = "modality";
		public String MODIFIER = "modifier";
		public String RESULTATIVE = "resultative";

		public Set<String> getGoldStandardLocalization(String key);

		public String getGoldStandardRoleNameLocalization(String type, String roleName);

		public String getGoldStandardRoleFillerLocalization(String type, String roleName, String fillerName);

		public Set<String> getGoldStandardRolesToIgnore();

		public Set<String> getGoldStandardAdjunctRoles();

		public Set<String> getGoldStandardRolesWithImpreciseBracketing();
	}

	public static boolean isFunction(String filler) {
		return filler.contains("(");
	}

	// REFACTOR: This is essentially a straight copy of ScriptReader's resolveValue.
	public static List<String> resolveFunction(ContextModel contextModel, String function) {

		if (!isFunction(function)) {
			List<String> ret = new ArrayList<String>();
			ret.add(function);
			return ret;
		}
		else {
			Stack<String> funcs = new Stack<String>();
			int nextP = function.indexOf('(');
			int lastP = 0;
			while (nextP != -1) {
				funcs.push(function.substring(lastP, nextP));
				lastP = nextP + 1;
				nextP = function.indexOf('(', lastP);
			}

			java.util.PriorityQueue<String> oldDomains;
			java.util.PriorityQueue<String> newDomains = new java.util.PriorityQueue<String>();
			newDomains.add(function.substring(lastP, function.indexOf(')', lastP)));

			String func, domain;

			while (!funcs.isEmpty()) {
				oldDomains = newDomains;
				newDomains = new java.util.PriorityQueue<String>();
				func = funcs.pop();
				while ((domain = oldDomains.poll()) != null) {
					List<SimpleQuery> simpleQueries = new ArrayList<SimpleQuery>();
					simpleQueries.add(new SimpleQuery(func, domain, "?x"));
					List<HashMap<String, String>> queryResults = contextModel.query(simpleQueries);

					for (HashMap<String, String> map : queryResults) {
						for (String val : map.values()) {
							newDomains.add(val);
						}
					}
				}
			}
			return new ArrayList<String>(newDomains);
		}
	}

	public static List<String> getCoindexedRoleNames(Grammar grammar, SchemaCloneTable schemaCloneTable,
			Type ontologyType, String role) {
		Set<String> ontologyRoles = new HashSet<String>();

		if (ontologyType.getRelation(role) != null) {
			for (Relation r : ontologyType.getCoindexedSet(role)) {
				ontologyRoles.add(r.getName());
			}
		}

		Schema schema = grammar.getSchema(ontologyType.getType());
		if (schema == null && ontologyRoles.isEmpty()) {
			return null;
		}
		else if (schema == null && !ontologyRoles.isEmpty()) {
			return new ArrayList<String>(ontologyRoles);
		}
		else if (schema != null && ontologyRoles.isEmpty()) {
			ontologyRoles.add(role);
		}

		List<String> roles = new ArrayList<String>();
		// In addition, go through the schema definition if available, since the coindexation is more complete there
		SchemaInstance instance = schemaCloneTable.getInstance(schema);

		for (String ontologyRole : ontologyRoles) {
			if (schema.getAllRoles().contains(new Role(ontologyRole))) {
				List<SlotChain> slotchains = instance.getCoindexedRoles(ontologyRole);
				for (SlotChain sc : slotchains) {
					SlotChain tmpSC = new SlotChain().setChain(sc.getChain().subList(1, sc.getChain().size()));
					roles.add(tmpSC.toString());
				}
			}
		}
		roles.addAll(ontologyRoles);
		return roles;
	}
}

// =============================================================================
// File        : EvokesLookupTable.java
// Author      : emok
// Change Log  : Created on Apr 26, 2007
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.grammartables.SchemaCloneTable.SchemaInstance;
import compling.learner.util.LearnerUtilities;
import compling.util.MapFactory.LinkedHashMapFactory;
import compling.util.MapSet;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public class CoreRolesTable extends MapSet<String, String> {

	private static final long serialVersionUID = 2465360058698582441L;
	private Grammar grammar;
	private SchemaCloneTable schemaCloneTable;
	private String processType;
	private TypeSystem<Schema> ts;
	List<String> dontCare = Arrays.asList("instrument", "effector", "inherent_aspect", "category");

	public CoreRolesTable(Grammar grammar, SchemaCloneTable schemaCloneTable) {
		super(new LinkedHashMapFactory<String, Set<String>>(), new LinkedHashSetFactory<String>());
		this.grammar = grammar;
		this.schemaCloneTable = schemaCloneTable;
		ts = grammar.getSchemaTypeSystem();
		processType = ts.getInternedString(ChildesLocalizer.PROCESSTYPE);
		for (Schema schema : ts.topologicalSort()) {
			try {
				if (ts.subtype(ts.getInternedString(schema.getName()), processType)) {
					processSchema(schema);
				}
			}
			catch (TypeSystemException tse) {

			}
		}

	}

	public void processSchema(Schema schema) {
		SchemaInstance instance = schemaCloneTable.getInstance(schema);
		Set<String> coreRoles = new HashSet<String>();

		Set<String> parents = schema.getParents();
		Set<String> inheritedCores = new HashSet<String>();
		for (String parent : parents) {
			inheritedCores.addAll(get(parent));
		}

		for (String inheritedCore : inheritedCores) {
			List<SlotChain> allPaths = instance.getCoindexedRoles(inheritedCore);
			if (allPaths.size() == 1) {
				coreRoles.add(inheritedCore);
			}
			else {
				List<String> localNames = new ArrayList<String>();
				List<String> inheritedRoles = new ArrayList<String>();
				for (SlotChain chain : allPaths) {
					if (chain.getChain().size() == 2) {
						if (chain.getChain().get(1).getSource().equals(schema.getName())) {
							localNames.add(chain.getChain().get(1).getName());
						}
						else {
							inheritedRoles.add(chain.getChain().get(1).getName());
						}
					}
				}
				if (!localNames.isEmpty()) {
					coreRoles.add(localNames.get(0));
				}
				else if (inheritedRoles.size() == 1) {
					coreRoles.add(inheritedCore);
				}
				else {
					Collections.sort(inheritedRoles);
					coreRoles.add(inheritedRoles.get(0));
				}
			}
		}

		for (Role r : schema.getContents().getElements()) {
			if (r.getSource().equals(schema.getName()) && LearnerUtilities.isElement(grammar, r.getTypeConstraint())
					&& !dontCare.contains(r.getName())) {
				coreRoles.add(r.getName());
			}
		}

		this.putAll(schema.getName(), coreRoles);
	}
}

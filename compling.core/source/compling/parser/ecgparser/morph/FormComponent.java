package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.List;

import compling.grammar.unificationgrammar.UnificationGrammar.Role;

// TODO: Make this work with ECGSlotChain objects. Requires rewrite of getValueOrType() in morph analyzer.

// If a construction 'cxn' has a form component which is a slot chain of the form a.b.c.f(.*)
// then 'a' is the immediate constituent of the slot chain, i.e. the constituent of 'cxn'; and 
// 'c' is the target constituent of the slot chain
public class FormComponent {
	public final List<String> typeChain;
	public final List<Role> slotChain;

	public boolean isDeep() {
		return slotChain.size() > 1;
	}

	public Role getConstit() {
		return slotChain.get(0);
	}

	public String getConstitType() {
		return typeChain.get(0);
	}

	public final boolean hasValue;
	public final String value;

	public FormComponent(String type, Role role) {
		typeChain = new ArrayList<String>();
		typeChain.add(type);
		slotChain = new ArrayList<Role>();
		slotChain.add(role);
		hasValue = false;
		value = null;
	}

	// Would be necessary if deep appearances were allowed, but they're not.
	private FormComponent(List<String> types, List<Role> roles) {
		typeChain = types;
		slotChain = roles;
		hasValue = false;
		value = null;
	}

	public FormComponent(String type, Role role, String value) {
		typeChain = new ArrayList<String>();
		typeChain.add(type);
		slotChain = new ArrayList<Role>();
		slotChain.add(role);
		this.value = value;
		this.hasValue = true;
	}

	public String toString() {
		return getConstit() + ":" + getConstitType() + ((hasValue) ? " = " + value : "");
	}
}

package compling.grammar.unificationgrammar;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import compling.grammar.GrammarException;
import compling.util.Interner;

public class UnificationGrammar {

	public static class Role implements Cloneable {

		private String name;
		private TypeConstraint typeConstraint = null;
		private String source;
		private Object container = null;
		private String special = "";
		private static Interner<String> interner = new Interner<String>();
		private int cachedHashCode;

		public Role(String name) {
			this.name = interner.intern(name);
			cachedHashCode = this.name.hashCode();
		}

		public void setName(String name) {
			this.name = interner.intern(name);
			cachedHashCode = this.name.hashCode();
		}

		public String getName() {
			return name;
		}

		public void setTypeConstraint(TypeConstraint typeConstraint) {
			this.typeConstraint = typeConstraint;
		}

		public TypeConstraint getTypeConstraint() {
			return typeConstraint;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getSpecialField() {
			return special;
		}

		public void setSpecialField(String special) {
			this.special = special;
		}

		public Object getContainer() {
			return container;
		}

		public void setContainer(Object container) {
			this.container = container;
		}

		public boolean equals(Object o) {
			if (o instanceof Role == false) {
				return false;
			}
			return name == ((Role) o).getName();
			// return name.equals(((Role) o).getName());
		}

		public int hashCode() {
			return cachedHashCode;
		}

		public String toString() {
			return name;
		}

		public Role clone() {
			try {
				return (Role) super.clone();
			}
			catch (Exception e) {
				throw new GrammarException("Bad clone");
			}
		}

	}

	public static class TypeConstraint implements Cloneable {

		public String type;
		public TypeSystem<? extends TypeSystemNode> typeSystem;

		public TypeConstraint(String type, TypeSystem<? extends TypeSystemNode> typeSystem) {
			this.type = type;
			this.typeSystem = typeSystem;
		}

		public void setTypeSystem(TypeSystem<? extends TypeSystemNode> typeSystem) {
			this.typeSystem = typeSystem;
		}

		public TypeConstraint clone() {
			try {
				return (TypeConstraint) super.clone();
			}
			catch (Exception e) {
				throw new GrammarException("Bad clone");
			}
		}

		public String getType() {
			return type;
		}

		public TypeSystem<?> getTypeSystem() {
			return typeSystem;
		}

		public String toString() {
			String ret = type + "@";
			if (typeSystem != null) {
				ret = ret + typeSystem.getName();
			}
			else {
				ret = ret + "null";
			}
			return ret;
		}

	}

	public static class SlotChain {
		static protected Interner<String> interner = new Interner<String>();
		protected List<Role> chain;
		protected boolean builtString = false;
		protected String cachedToString = "";
		protected int cachedHashCode;

		public SlotChain() {
			// Empty
		}

		public void internChain() {
			cachedToString = interner.intern(toString());
			cachedHashCode = cachedToString.hashCode();
		}

		public SlotChain(String slotChain) {
			StringTokenizer st = new StringTokenizer(slotChain, ".");
			chain = new ArrayList<Role>();
			while (st.hasMoreTokens())
				chain.add(new Role(st.nextToken()));
			internChain();
		}

		public SlotChain(List<String> slotChain) {
			chain = new ArrayList<Role>();
			for (String s : slotChain) {
				chain.add(new Role(s));
			}
			internChain();
		}

		
//		public SlotChain(List<Role> roles) {
//			setChain(roles);
//		}
		
		public SlotChain setChain(List<Role> chain) {
			this.chain = chain;
			builtString = false;
			internChain();
			return this;
		}

		public List<Role> getChain() {
			return chain;
		}

		public SlotChain subChain(int fromIndex) {
			return subChain(fromIndex, chain.size());
		}

		public SlotChain subChain(int fromIndex, int toIndex) {
			List<Role> newChain = new ArrayList<Role>(chain.subList(fromIndex, toIndex));
			SlotChain newSlotChain = new SlotChain();
			newSlotChain.setChain(newChain);
			return newSlotChain;
		}

		public int hashCode() {
			return cachedHashCode;
		}

		public boolean equals(Object o) {
			if (o instanceof SlotChain) {
				SlotChain that = (SlotChain) o;
				return this.cachedToString == that.cachedToString;
			}
			return false;
		}

		public SlotChain clone() {
			return new SlotChain().setChain(new ArrayList<Role>(this.getChain()));
		}

		public String toString() {
			if (!builtString) {
				StringBuffer sb = new StringBuffer();
				for (Role role : chain) {
					sb.append(role).append(".");
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
				}
				cachedToString = sb.toString();
				builtString = true;
				// builtString = false;
			}
			return cachedToString;
		}
	}

	public static class Constraint {

		private String operator;
		private List<SlotChain> arguments;
		private String source;
		private String value = null;
		private boolean overridden = false; // a boolean for grammars that allow constraint overriding

		public Constraint(String operator, List<SlotChain> arguments) {
			this.operator = operator;
			this.arguments = arguments;
		}

		public Constraint(String operator, SlotChain arg1, SlotChain arg2) {
			arguments = new ArrayList<SlotChain>();
			arguments.add(arg1);
			arguments.add(arg2);
			this.operator = operator;
		}

		public Constraint(String operator, String source, String value, boolean overridden, List<SlotChain> arguments) {
			this.operator = operator;
			this.source = source;
			this.value = value;
			this.overridden = overridden;
			this.arguments = arguments;
		}

		public Constraint(String operator, SlotChain arg1, String value) {
			arguments = new ArrayList<SlotChain>();
			arguments.add(arg1);
			this.operator = operator;
			this.value = value;
		}

		public void setOverridden(boolean val) {
			overridden = val;
		}

		public boolean overridden() {
			return overridden;
		}

		public String getOperator() {
			return operator;
		}

		public List<SlotChain> getArguments() {
			return arguments;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getValue() {
			// if (value != null){
			return value;
			// } else {
			// return "value is null. You shouldn't be asking for it.";
			// }
		}

		public void setValue(String value) {
			this.value = value;
		}

		public boolean isAssign() {
			return value != null;
		}

		public boolean equals(Object o) {
			if (o instanceof Constraint == false)
				return false;

			Constraint that = (Constraint) o;

			if (operator.equals(that.getOperator()) == false)
				return false;

			if (arguments.equals(that.arguments) == false)
				return false;

			if (value == null && value != that.value)
				return false;

			if (value != null && value.equals(that.value) == false)
				return false;

			return true;
		}

		public int hashCode() {
			return operator.hashCode() + arguments.hashCode();
		}

		public Constraint clone() {
			List<SlotChain> clonedArguments = new ArrayList<SlotChain>();
			for (SlotChain sc : arguments) {
				clonedArguments.add(sc.clone());
			}

			Constraint clone = new Constraint(operator, clonedArguments);
			clone.source = source;
			clone.value = value;
			clone.overridden = overridden;
			return clone;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			if (arguments.size() == 1 && value == null) {
				sb.append(getOperator()).append(" ").append(arguments.get(0));
			}
			else if (arguments.size() == 1 && value != null) {
				sb.append(arguments.get(0)).append(" ").append(getOperator()).append(" ").append(value);
			}
			else if (arguments.size() == 2) {
				sb.append(arguments.get(0)).append(" ").append(getOperator()).append(" ").append(arguments.get(1));
			}
			else if (arguments.size() > 2) {
				sb.append(getOperator());
				for (SlotChain argument : getArguments()) {
					sb.append(" ").append(argument);
				}
			}
			return sb.toString();
		}
	}

}

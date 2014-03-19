package compling.parser.ecgparser;

import java.util.List;
import java.util.Set;

import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;

public class CxnalSpan {
	public int left = -1;
	public int right = -1;
	public int leftChar = -1;
	public int rightChar = -1;
	int slotID;
	boolean omitted = false;
	boolean local = false;
	boolean gappedOut = false;
	Role role = null;
	Construction type = null;
	Set<Role> unusedOptionals = null;
	List<Analysis> sibs;

	public CxnalSpan(Role role, Construction type, int slotID, int left, int right) {
		this.role = role;
		this.slotID = slotID;
		this.left = left;
		this.right = right;
		local = true;
		this.type = type;
	}

	public CxnalSpan(Role role, Construction type, int slotID, int left, int right, int leftChar, int rightChar) {
		this(role, type, slotID, left, right);
		this.leftChar = leftChar;
		this.rightChar = rightChar;
	}

	public CxnalSpan(Role role, int slotID, int lrindex, boolean omitted) {
		this.role = role;
		this.slotID = slotID;
		if (omitted) {
			this.omitted = true;
		}
		else {
			this.gappedOut = true;
		}
		left = lrindex;
		right = lrindex;
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	public int getLeftChar() {
		return leftChar;
	}

	public int getRightChar() {
		return rightChar;
	}

	public int getSlotID() {
		return slotID;
	}

	public boolean omitted() {
		return omitted;
	}

	public boolean local() {
		return local;
	}

	public boolean gappedOut() {
		return gappedOut;
	}

	public Role getRole() {
		return role;
	}

	public Construction getType() {
		return type;
	}

	public Set<Role> getUnusedOptionals() {
		return unusedOptionals;
	}

	public void setUnusedOptionals(Set<Role> s) {
		unusedOptionals = s;
	}

	public void setSibs(List<Analysis> sibs) {
		this.sibs = sibs;
	}

	public List<Analysis> getSibs() {
		return sibs;
	};

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(").append(left);
		if (leftChar > -1)
			sb.append(";").append(leftChar);
		sb.append(", ").append(right);
		if (rightChar > -1)
			sb.append(";").append(rightChar);
		sb.append(") ");

		if (role != null) {
			sb.append(role.getName()).append(" filled by ");
		}
		if (type != null) {
			sb.append(type.getName()).append(" ");
		}
		if (omitted) {
			sb.append("[omitted]");
		}
		else if (gappedOut) {
			sb.append("[gapped out]");
		}
		return sb.toString();
	}
}

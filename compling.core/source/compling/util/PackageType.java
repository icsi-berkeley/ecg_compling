package compling.util;

import java.util.Set;

import compling.grammar.unificationgrammar.TypeSystemNode;

public class PackageType implements TypeSystemNode{
	
	String packageType;
	
	public PackageType(String type) {
		packageType = type;
	}
	
	public String toString() {
		return packageType;
	}

	@Override
	public int compareTo(TypeSystemNode arg0) {
		if (arg0.getType().equals(packageType)) {
			return 1;
		}
		return 0;
	}

	@Override
	public Set<String> getParents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		return packageType;
	}

	@Override
	public void setType(String type) {
		packageType = type;
		
	}

}

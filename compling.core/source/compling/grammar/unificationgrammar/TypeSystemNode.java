package compling.grammar.unificationgrammar;

import java.util.Set;

public interface TypeSystemNode extends Comparable<TypeSystemNode> {
	Set<String> getParents();
	String getType();
	void setType(String type);
}

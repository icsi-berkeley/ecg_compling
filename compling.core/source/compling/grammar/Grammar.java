package compling.grammar;

import java.util.List;

public interface Grammar<SYM, RULE extends Rule<SYM>> {

	public boolean isNonTerminal(SYM symbol);

	public List<RULE> getRules(SYM LHSsymbol);

	public SYM getStartSymbol();

	public boolean isStartSymbol(SYM sym);

}

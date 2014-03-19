
package compling.parser;

import compling.utterance.*;

public interface ChartParser<PARSEKIND, STATEKIND extends State> extends Parser<PARSEKIND>{

    public Chart<STATEKIND> getChart(Utterance<Word, String> utterance);

}

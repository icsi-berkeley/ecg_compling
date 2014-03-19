
package compling.parser.treebank;

import compling.utterance.*;
import compling.grammar.cfg.*;


public interface TreeBankParser {

   public Tree getBestParse(Sentence s);
}

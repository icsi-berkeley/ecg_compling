
package compling.grammar.cfg;

import java.util.*;

import compling.grammar.cfg.HeadedTree;


/**
 * This interface is used to make HeadedTree independent from any particular
 * label/tree encoding scheme.
 * 
 * The only method that needs to be implemented is findHead
 * 
 * @author John Bryant
 * 
 */

public interface HeadFinder {

   HeadedTree findHead(String nodeLable, List<HeadedTree> headedChildren);
}

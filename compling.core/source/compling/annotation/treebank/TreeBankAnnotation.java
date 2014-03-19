
package compling.annotation.treebank;

import compling.utterance.UtteranceAnnotation;
import compling.grammar.cfg.*;
import java.util.List;

/**
 * This class represents a penn treebank annotation of a sentence, implementing
 * the SentenceAnnotation interface
 * 
 * @author John Bryant
 * 
 */

public class TreeBankAnnotation implements UtteranceAnnotation {

   private HeadedTree originalTree;
   private HeadedTree normalizedTree;


   public String getAnnotationType() {
      return UtteranceAnnotation.PENNTREEBANK;
   }


   public TreeBankAnnotation(Tree original, TreeUtilities.TreeTransformer normalizer) {
      originalTree = HeadedTree.makeHeadedTree(original, new HeadedTreePrefs());
      normalizedTree = HeadedTree.makeHeadedTree(normalizer
            .transformTree(originalTree), new HeadedTreePrefs());
   }


   public Tree getOriginalTree() {
      return originalTree;
   }


   public HeadedTree getNormalizedTree() {
      return normalizedTree;
   }

    public int getNormalizedIndex(int index){
	List<String> original = originalTree.getYield();
	List<String> normalized = normalizedTree.getYield();
	int j = 0;
	int traces = 0;
	for (int i = 0; i <= index; i++) {
	    String oword = (String) original.get(i);
	    String nword = (String) normalized.get(j);
	    if (!oword.equals(nword)) {
		traces++;
	    } else {
		j++;
	    }
	}
	return index - traces;
    }

}

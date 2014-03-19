
package compling.grammar.cfg;

import java.util.*;


/**
 * A simple headed tree class extends Tree and methods for heads.
 * 
 * @author John Bryant
 */

public class HeadedTree extends Tree {

   String head;
   String headTag;


   public void setHead(String h) {
      head = h;
   }


   public String getHead() {
      return head;
   }


   public void setHeadTag(String t) {
      headTag = t;
   }


   public String getHeadTag() {
      return headTag;
   }

    
   public HeadedTree(String label, String head, String headTag, List<HeadedTree> children) {
      super(label, children);
      setHead(head);
      setHeadTag(headTag);
   }
    

   public HeadedTree(String label) {
      super(label);
      setHead(label);
   }


   public static HeadedTree makeHeadedTree(Tree input, HeadFinder hf) {
      String label = input.getLabel();
      if (input.isLeaf()) {
         return new HeadedTree(label);
      }
      ArrayList<HeadedTree> headedChildren = new ArrayList<HeadedTree>();
      for (Tree child : input.getChildren()) {
         HeadedTree headedChild = makeHeadedTree(child, hf);
         headedChildren.add(headedChild);
      }
      String l = label;
      if (l.indexOf("-") != -1) {
         l = l.substring(0, l.indexOf("-"));
      }
      if (l.indexOf("=") != -1) {
         l = l.substring(0, l.indexOf("="));
      }
      HeadedTree ht = hf.findHead(l, headedChildren);
      return new HeadedTree(label, ht.getHead(), ht.getHeadTag(), headedChildren);
   }
}

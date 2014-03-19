
package compling.grammar.cfg;

import java.util.*;


/**
 * A simple n-ary tree class
 * 
 * The label is the symbol at this node. the parent is the node's parent in the
 * tree and the children list is a list of Tree instances.
 * 
 * Adapted from code originally written by Dan Klein.
 * 
 * @author John Bryant
 */

public class Tree {

   protected String label;
   protected List<? extends Tree> children;
   protected Tree parent;

   public Tree(String label, List<? extends Tree> children) {
      this.label = label;
      this.children = children;
      for (Tree child : children) {
         child.parent = this;
      }
   }


   public Tree(String label) {
      this.label = label;
      this.children = Collections.EMPTY_LIST;
   }

   public String getLabel() {
      return label;
   }


   public List<? extends Tree> getChildren() {
      return children;
   }


   public Tree getParent() {
      return parent;
   }


   public boolean allChildrenPreTerminals() {
      if (isLeaf() || isPreTerminal()) {
         return false;
      }
      for (int i = 0; i < children.size(); i++) {
         if (((Tree) children.get(i)).isPreTerminal() == false) {
            return false;
         }
      }
      return true;
   }


   public boolean isLeaf() {
      return getChildren().isEmpty();
   }


   public boolean isPreTerminal() {
      return getChildren().size() == 1 && ((Tree) getChildren().get(0)).isLeaf();
   }


   public List<String> getYield() {
      List<String> yield = new ArrayList<String>();
      appendYield(this, yield);
      return yield;
   }


   private static void appendYield(Tree tree, List<String> yield) {
      if (tree.isLeaf()) {
         yield.add(tree.getLabel());
         return;
      }
      for (Tree child : tree.getChildren()) {
         appendYield(child, yield);
      }
   }


   public List<String> getPreTerminalYield() {
      List<String> yield = new ArrayList<String>();
      appendPreTerminalYield(this, yield);
      return yield;
   }


   private static void appendPreTerminalYield(Tree tree, List<String> yield) {
      if (tree.isPreTerminal()) {
         yield.add(tree.getLabel());
         return;
      }
      for (Tree child : tree.getChildren()) {
         appendPreTerminalYield(child, yield);
      }
   }


   public List<Tree> getLeaves() {
      List<Tree> yield = new ArrayList<Tree>();
      appendLeaves(this, yield);
      return yield;
   }


   private static void appendLeaves(Tree tree, List<Tree> yield) {
      if (tree.isLeaf()) {
         yield.add(tree);
         return;
      }
      for (Tree child : tree.getChildren()) {
         appendLeaves(child, yield);
      }
   }


   public Tree getNode(int terminal, int height) {
      List<Tree> leaves = getLeaves();
      Tree node = (Tree) leaves.get(terminal);
      while (height >= 0) {
         height--;
         node = node.getParent();
      }
      return node;
   }


   public Tree mirror() {
      if (!isLeaf()) {
         List<Tree> newchildren = new ArrayList<Tree>();
         for (int i = children.size() - 1; i >= 0; i--) {
            newchildren.add(((Tree) children.get(i)).mirror());
         }
         children = newchildren;
      }
      return this;
   }


   public List<CFGRule> getCFGRules() {
      ArrayList<CFGRule> al = new ArrayList<CFGRule>();
      appendRules(this, al);
      return al;
   }


   private static void appendRules(Tree tree, List<CFGRule> rules) {
      if (tree.isLeaf()) {
         return;
      }
      for (Tree child : tree.getChildren()) {
         StringBuffer rhs = new StringBuffer();
         rhs.append(child.getLabel()).append(" ");
         appendRules(child, rules);
         rules.add(new CFGRule(tree.getLabel(), rhs.toString()));
      }
   }
}

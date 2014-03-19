
package compling.annotation.treebank;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import compling.grammar.cfg.*;


/** The code in this file is taken from the Dan Klein's 294 class code */

public class TreeUtilities {

   public static interface TreeTransformer {

      Tree transformTree(Tree tree);
   }

   public static class EqualsNodeStripper implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         String transformedLabel = tree.getLabel();
         int cutIndex = transformedLabel.indexOf('=');
         if (cutIndex > 0 && !tree.isLeaf()) {
            transformedLabel = new String(transformedLabel.substring(0, cutIndex));
         }
         if (tree.isLeaf()) {
            return new Tree(transformedLabel);
         }
         List<Tree>transformedChildren = new ArrayList<Tree>();
         for (Iterator i = tree.getChildren().iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            transformedChildren.add(transformTree(child));
         }
         return new Tree(transformedLabel, transformedChildren);
      }
   }

   public static class TraceReplacer implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         String transformedLabel = tree.getLabel();
         int starIndex = transformedLabel.indexOf('*');
         if (starIndex > 0) {
            transformedLabel = TreeBankSymbols.TRACE;
         }
         if (tree.isLeaf()) {
            return new Tree(transformedLabel);
         }
         List<Tree>transformedChildren = new ArrayList<Tree>();
         for (Iterator i = tree.getChildren().iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            transformedChildren.add(transformTree(child));
         }
         return new Tree(transformedLabel, transformedChildren);
      }
   }

   public static class FunctionNodeStripper implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         String transformedLabel = tree.getLabel();
         int cutIndex = transformedLabel.indexOf('-');
         if (cutIndex > 0 && !tree.isLeaf() && !transformedLabel.equals("NP-TMP")) {
            transformedLabel = new String(transformedLabel.substring(0, cutIndex));
         }
         if (tree.isLeaf()) {
            return new Tree(transformedLabel);
         }
         List<Tree>transformedChildren = new ArrayList<Tree>();
         boolean allPreTerms = true;
         for (Iterator i = tree.getChildren().iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            if (child.isPreTerminal() == false) {
               allPreTerms = false;
            }
            transformedChildren.add(transformTree(child));
         }
         Tree newTree = new Tree(transformedLabel, transformedChildren);
         if (allPreTerms) {
         }
         return newTree;
      }
   }

   public static class NonRecMarker implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         if (tree.isLeaf()) {
            return new Tree(tree.getLabel());
         }
         List<Tree>transformedChildren = new ArrayList<Tree>();
         boolean allPreTerms = true;
         for (Iterator i = tree.getChildren().iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            if (child.isPreTerminal() == false) {
               allPreTerms = false;
            }
            transformedChildren.add(transformTree(child));
         }
         String transformedLabel = tree.getLabel();
         if (tree.allChildrenPreTerminals()) {
            transformedLabel = transformedLabel + "-NR";
         }
         return new Tree(transformedLabel, transformedChildren);
      }
   }

   public static class EmptyNodeStripper implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         String label = tree.getLabel();
         if (label.equals("-NONE-")) {
            return null;
         }
         if (tree.isLeaf()) {
            return new Tree(label);
         }
         List<? extends Tree>children = tree.getChildren();
         List<Tree>transformedChildren = new ArrayList<Tree>();
         for (Iterator i = children.iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            Tree transformedChild = transformTree(child);
            if (transformedChild != null)
               transformedChildren.add(transformedChild);
         }
         if (transformedChildren.size() == 0)
            return null;
         return new Tree(label, transformedChildren);
      }
   }

   public static class XOverXRemover implements TreeTransformer {

      public Tree transformTree(Tree tree) {
         String label = tree.getLabel();
         List<? extends Tree>children = tree.getChildren();
         while (children.size() == 1 && !((Tree) children.get(0)).isLeaf()
               && label.equals(((Tree) children.get(0)).getLabel())) {
            children = ((Tree) children.get(0)).getChildren();
         }
         List<Tree>transformedChildren = new ArrayList<Tree>();
         for (Iterator i = children.iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            transformedChildren.add(transformTree(child));
         }
         return new Tree(label, transformedChildren);
      }
   }

   public static class StandardTreeNormalizer implements TreeTransformer {

      EmptyNodeStripper emptyNodeStripper = new EmptyNodeStripper();
      XOverXRemover xOverXRemover = new XOverXRemover();
      FunctionNodeStripper functionNodeStripper = new FunctionNodeStripper();
      EqualsNodeStripper equalsNodeStripper = new EqualsNodeStripper();
      NonRecMarker nonRecMarker = new NonRecMarker();


      public Tree transformTree(Tree tree) {
         tree = equalsNodeStripper.transformTree(tree);
         tree = functionNodeStripper.transformTree(tree);
         tree = emptyNodeStripper.transformTree(tree);
         tree = xOverXRemover.transformTree(tree);
         tree = nonRecMarker.transformTree(tree);
         return tree;
      }

   }

   public static class MetaphorTreeNormalizer implements TreeTransformer {

      FunctionNodeStripper functionNodeStripper = new FunctionNodeStripper();
      EqualsNodeStripper equalsNodeStripper = new EqualsNodeStripper();
      TraceReplacer traceReplacer = new TraceReplacer();


      public Tree transformTree(Tree tree) {
         tree = functionNodeStripper.transformTree(tree);
         tree = equalsNodeStripper.transformTree(tree);
         tree = traceReplacer.transformTree(tree);
         return tree;
      }
   }

   public static class PennTreeReader implements Iterator {

      public static String ROOT_LABEL = "ROOT";

      PushbackReader in;
      Tree nextTree;


      public boolean hasNext() {
         return (nextTree != null);
      }


      public Object next() {
         if (!hasNext())
            throw new NoSuchElementException();
         Tree tree = nextTree;
         nextTree = readRootTree();
         try {
            if (nextTree == null) {
               in.close();
            }
         } catch (IOException e) {
            throw new RuntimeException("Problem closing tree file");
         }
         return tree;
      }


      private Tree readRootTree() {
         try {
            readWhiteSpace();
            if (!isLeftParen(peek()))
               return null;
            return readTree(true);
         } catch (IOException e) {
            throw new RuntimeException("Error reading tree.");
         }
      }


      private Tree readTree(boolean isRoot) throws IOException {
         readLeftParen();
         String label = readLabel();
         if (label.length() == 0 && isRoot)
            label = ROOT_LABEL;
         List<Tree>children = readChildren();
         readRightParen();
         return new Tree(label, children);
      }


      private String readLabel() throws IOException {
         readWhiteSpace();
         return readText();
      }


      private String readText() throws IOException {
         StringBuffer sb = new StringBuffer();
         int ch = in.read();
         while (!isWhiteSpace(ch) && !isLeftParen(ch) && !isRightParen(ch)) {
            sb.append((char) ch);
            ch = in.read();
         }
         in.unread(ch);
         // System.out.println("Read text: ["+sb+"]");
         return sb.toString().intern();
      }


      private List<Tree>readChildren() throws IOException {
         readWhiteSpace();
         if (!isLeftParen(peek()))
            return Collections.singletonList(readLeaf());
         return readChildList();
      }


      private int peek() throws IOException {
         int ch = in.read();
         in.unread(ch);
         return ch;
      }


      private Tree readLeaf() throws IOException {
         String label = readText();
         return new Tree(label);
      }


      private List<Tree>readChildList() throws IOException {
         List<Tree>children = new ArrayList<Tree>();
         readWhiteSpace();
         while (!isRightParen(peek())) {
            children.add(readTree(false));
            readWhiteSpace();
         }
         return children;
      }


      private void readLeftParen() throws IOException {
         // System.out.println("Read left.");
         readWhiteSpace();
         int ch = in.read();
         if (!isLeftParen(ch))
            throw new RuntimeException("Format error reading tree.");
      }


      private void readRightParen() throws IOException {
         // System.out.println("Read right.");
         readWhiteSpace();
         int ch = in.read();
         if (!isRightParen(ch))
            throw new RuntimeException("Format error reading tree.");
      }


      private void readWhiteSpace() throws IOException {
         int ch = in.read();
         while (isWhiteSpace(ch)) {
            ch = in.read();
         }
         in.unread(ch);
      }


      private boolean isWhiteSpace(int ch) {
         return (ch == ' ' || ch == '\t' || ch == '\f' || ch == '\r' || ch == '\n');
      }


      private boolean isLeftParen(int ch) {
         return ch == '(';
      }


      private boolean isRightParen(int ch) {
         return ch == ')';
      }


      public void remove() {
         throw new UnsupportedOperationException();
      }


      public PennTreeReader(Reader in) {
         this.in = new PushbackReader(in);
         nextTree = readRootTree();
      }
   }

   /**
    * Renderer for pretty-printing trees according to the Penn Treebank
    * indenting guidelines (mutliline). Adapted from code originally written by
    * Dan Klein and modified by Chris Manning.
    */
   public static class PennTreeRenderer {

      /**
       * Print the tree as done in Penn Treebank merged files. The formatting
       * should be exactly the same, but we don't print the trailing whitespace
       * found in Penn Treebank trees. The basic deviation from a bracketed
       * indented tree is to in general collapse the printing of adjacent
       * preterminals onto one line of tags and words. Additional complexities
       * are that conjunctions (tag CC) are not collapsed in this way, and that
       * the unlabeled outer brackets are collapsed onto the same line as the
       * next bracket down.
       */
      public static String render(Tree tree) {
         StringBuffer sb = new StringBuffer();
         renderTree(tree, 0, false, false, false, true, sb);
         sb.append('\n');
         return sb.toString();
      }


      /**
       * Display a node, implementing Penn Treebank style layout
       */
      private static void renderTree(Tree tree, int indent, boolean parentLabelNull,
            boolean firstSibling, boolean leftSiblingPreTerminal, boolean topLevel,
            StringBuffer sb) {
         // the condition for staying on the same line in Penn Treebank
         boolean suppressIndent = (parentLabelNull
               || (firstSibling && tree.isPreTerminal()) || (leftSiblingPreTerminal
               && tree.isPreTerminal() && (tree.getLabel() == null || !tree
               .getLabel().toString().startsWith("CC"))));
         if (suppressIndent) {
            sb.append(' ');
         } else {
            if (!topLevel) {
               sb.append('\n');
            }
            for (int i = 0; i < indent; i++) {
               sb.append("  ");
            }
         }
         if (tree.isLeaf() || tree.isPreTerminal()) {
            renderFlat(tree, sb);
            return;
         }
         sb.append('(');
         sb.append(tree.getLabel());
         renderChildren(tree.getChildren(), indent + 1, tree.getLabel() == null
               || tree.getLabel().toString() == null, sb);
         sb.append(')');
      }


      private static void renderFlat(Tree tree, StringBuffer sb) {
         if (tree.isLeaf()) {
            sb.append(tree.getLabel().toString());
            return;
         }
         sb.append('(');
         sb.append(tree.getLabel().toString());
         sb.append(' ');
         sb.append(((Tree) tree.getChildren().get(0)).getLabel().toString());
         sb.append(')');
      }


      private static void renderChildren(List<? extends Tree>children, int indent,
            boolean parentLabelNull, StringBuffer sb) {
         boolean firstSibling = true;
         boolean leftSibIsPreTerm = true; // counts as true at beginning
         for (Iterator i = children.iterator(); i.hasNext();) {
            Tree child = (Tree) i.next();
            renderTree(child, indent, parentLabelNull, firstSibling,
                  leftSibIsPreTerm, false, sb);
            leftSibIsPreTerm = child.isPreTerminal();
            // CC is a special case
            if (child.getLabel() != null
                  && child.getLabel().toString().startsWith("CC")) {
               leftSibIsPreTerm = false;
            }
            firstSibling = false;
         }
      }
   }


   public static void main(String[] args) {
      PennTreeReader reader = new PennTreeReader(
            new StringReader(
                  "((S (NP (DT the) (JJ quick) (JJ brown) (NN fox)) (VP (VBD jumped) (PP (IN over) (NP (DT the) (JJ lazy) (NN dog)))) (. .)))"));
      Tree tree = (Tree) reader.next();
      System.out.println(PennTreeRenderer.render(tree));
      System.out.println(tree);
   }

}

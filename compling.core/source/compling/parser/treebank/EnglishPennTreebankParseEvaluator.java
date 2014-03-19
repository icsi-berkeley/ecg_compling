
package compling.parser.treebank;

import java.util.*;
import java.io.PrintWriter;
import java.io.StringReader;
//import compling.annotation.treebank.TreeUtilities.*;
import compling.annotation.treebank.*;
import compling.grammar.cfg.*;


/**
 * Evaluates precision and recall for English Penn Treebank parse trees. NOTE:
 * Unlike the standard evaluation, multiplicity over each span is ignored. Also,
 * punction is NOT currently deleted properly (approximate hack), and other
 * normalizations (like AVDP ~ PRT) are NOT done.
 * 
 * @author Dan Klein
 */
public class EnglishPennTreebankParseEvaluator {

   abstract static class AbstractEval {

      public boolean printEachEval = false;
      protected String str = "";

      private int exact = 0;
      private int total = 0;

      private int correctEvents = 0;
      private int guessedEvents = 0;
      private int goldEvents = 0;


      abstract Set<LabeledConstituent> makeObjects(Tree tree);


      public void evaluate(Tree guess, Tree gold) {
         evaluate(guess, gold, new PrintWriter(System.out, true));
      }


      /*
       * evaluates precision and recall by calling makeObjects() to make a set
       * of structures for guess Tree and gold Tree, and compares them with each
       * other.
       */
      public void evaluate(Tree guess, Tree gold, PrintWriter pw) {
         Set<LabeledConstituent> guessedSet = makeObjects(guess);
         Set<LabeledConstituent> goldSet = makeObjects(gold);
         Set<LabeledConstituent> correctSet = new HashSet<LabeledConstituent>();
         correctSet.addAll(goldSet);
         correctSet.retainAll(guessedSet);

         correctEvents += correctSet.size();
         guessedEvents += guessedSet.size();
         goldEvents += goldSet.size();

         int currentExact = 0;
         if (correctSet.size() == guessedSet.size()
               && correctSet.size() == goldSet.size()) {
            exact++;
            currentExact = 1;
         }
         total++;

         // guess.pennPrint(pw);
         // gold.pennPrint(pw);
         displayPRF(str + " [Current] ", correctSet.size(), guessedSet.size(),
               goldSet.size(), currentExact, 1, pw);

      }


      private void displayPRF(String prefixStr, int correct, int guessed, int gold,
            int exact, int total, PrintWriter pw) {
         double precision = (guessed > 0 ? correct / (double) guessed : 1.0);
         double recall = (gold > 0 ? correct / (double) gold : 1.0);
         double f1 = (precision > 0.0 && recall > 0.0 ? 2.0 / (1.0 / precision + 1.0 / recall)
               : 0.0);

         double exactMatch = exact / (double) total;

         String displayStr = " P: " + ((int) (precision * 10000)) / 100.0 + " R: "
               + ((int) (recall * 10000)) / 100.0 + " F1: " + ((int) (f1 * 10000))
               / 100.0 + " EX: " + ((int) (exactMatch * 10000)) / 100.0;

         if (printEachEval)
            pw.println(prefixStr + displayStr);
      }


      public void display(boolean verbose) {
         display(verbose, new PrintWriter(System.out, true));
      }


      public void display(boolean verbose, PrintWriter pw) {
         displayPRF(str + " [Average] ", correctEvents, guessedEvents, goldEvents,
               exact, total, pw);
      }
   }

   static class LabeledConstituent {

      String label;
      int start;
      int end;


      public String getLabel() {
         return label;
      }


      public int getStart() {
         return start;
      }


      public int getEnd() {
         return end;
      }


      public boolean equals(Object o) {
         if (this == o)
            return true;
         if (!(o instanceof LabeledConstituent))
            return false;

         final LabeledConstituent labeledConstituent = (LabeledConstituent) o;

         if (end != labeledConstituent.end)
            return false;
         if (start != labeledConstituent.start)
            return false;
         if (label != null ? !label.equals(labeledConstituent.label)
               : labeledConstituent.label != null)
            return false;

         return true;
      }


      public int hashCode() {
         int result;
         result = (label != null ? label.hashCode() : 0);
         result = 29 * result + start;
         result = 29 * result + end;
         return result;
      }


      public String toString() {
         return label + "[" + start + "," + end + "]";
      }


      public LabeledConstituent(String label, int start, int end) {
         this.label = label;
         this.start = start;
         this.end = end;
      }
   }

   public static class LabeledConstituentEval extends AbstractEval {

      Set<String> labelsToIgnore;
      Set<String> punctuationTags;


      static Tree stripLeaves(Tree tree) {
         if (tree.isLeaf())
            return null;
         if (tree.isPreTerminal())
            return new Tree(tree.getLabel());
         List<Tree> children = new ArrayList<Tree>();
         for (Tree child: tree.getChildren()){
            children.add(stripLeaves(child));
         }
         return new Tree(tree.getLabel(), children);
      }


      Set<LabeledConstituent> makeObjects(Tree tree) {
         // System.out.println("leaftree "+tree);
         Tree noLeafTree = stripLeaves(tree);
         // System.out.println("noleaftree "+noLeafTree);
         Set<LabeledConstituent> set = new HashSet<LabeledConstituent>();
         addConstituents(noLeafTree, set, 0);
         // System.out.println("set: "+set);
         return set;
      }


      private int addConstituents(Tree tree, Set<LabeledConstituent> set, int start) {
         if (tree.isLeaf()) {
            if (punctuationTags.contains(tree.getLabel()))
               return 0;
            else
               return 1;
         }
         int end = start;
	 for (Tree child : tree.getChildren()){
            int childSpan = addConstituents(child, set, end);
            end += childSpan;
         }
         String label = tree.getLabel();
         if (!labelsToIgnore.contains(label)) {
            set.add(new LabeledConstituent(label, start, end));
         }
         return end - start;
      }


      public LabeledConstituentEval(Set<String> labelsToIgnore, Set<String> punctuationTags) {
         this.labelsToIgnore = labelsToIgnore;
         this.punctuationTags = punctuationTags;
      }

   }


   public static void main(String[] args) throws Throwable {
      Tree goldTree = (Tree) (new TreeUtilities.PennTreeReader(new StringReader(
            "(ROOT (S (NP (DT the) (NN can)) (VP (VBD fell))))"))).next();
      Tree guessedTree = (Tree) (new TreeUtilities.PennTreeReader(new StringReader(
            "(ROOT (S (NP (DT the)) (VP (MB can) (VP (VBD fell)))))"))).next();
      LabeledConstituentEval eval = new LabeledConstituentEval(Collections
            .singleton("ROOT"), new HashSet<String>());
      eval.evaluate(guessedTree, goldTree);
      eval.display(true);
   }
}

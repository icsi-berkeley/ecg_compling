
package compling.annotation.metaphor;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import compling.annotation.namedentity.NamedEntityAnnotation;
import compling.annotation.namedentity.NamedEntityAnnotationIterator;
import compling.annotation.propbank.PropBankAnnotation;
import compling.annotation.propbank.PropBankFileIterator;
import compling.annotation.propbank.PropBankIterator;
import compling.annotation.propbank.TargetAnnotation;
import compling.annotation.treebank.TreeBankAnnotation;
import compling.annotation.treebank.TreeBankFileIterator;
import compling.annotation.treebank.TreeBankSymbols;
import compling.annotation.treebank.TreeUtilities.MetaphorTreeNormalizer;
import compling.classifier.BasicLabeledDatum;
import compling.classifier.ClassifierTrainer;
import compling.classifier.LabeledDatum;
import compling.classifier.ProbabilisticClassifier;
import compling.grammar.cfg.HeadedTree;
import compling.grammar.cfg.Tree;
import compling.util.fileutil.NumberRangeFileFilter;
import compling.util.fileutil.NumberRangeIterator;
import compling.util.fileutil.TextFileLineIterator;
import compling.util.wordnet.SimpleWordNetInterface;
import compling.utterance.Sentence;
import compling.utterance.UtteranceAnnotation;
import compling.utterance.Word;


/**
 * Extracts features from the sentences that belong to the specified frame.
 * 
 * @author Branimir Ciric with modifications by John Bryant
 */
public class FeatureExtractor {

    public static final String PERSON = "person";
    public static final String LOCATION = "location";
    public static final String DATE = "date";
    public static final String COMPANY = "company";
    public static final String PERCENT = "percent";
    public static final String ORGANIZATION = "organization";
    public static final String MONEY = "money";

   public static final String UNKNOWN = "unknown";
   public static final String NOT_ANNOTATED = "not-annotated";
   public static final String NOTAG = "NOTAG";
   public static final String VERB = "VERB";

   public static final String[][] pronounsArray = { { "i", PERSON },
         { "me", PERSON }, { "myself", PERSON }, { "you", PERSON },
         { "yourself", PERSON }, { "he", PERSON }, { "him", PERSON },
         { "himself", PERSON }, { "she", PERSON }, { "her", PERSON },
         { "herself", PERSON }, { "it", UNKNOWN }, { "itself", UNKNOWN },
         { "we", PERSON }, { "us", PERSON }, { "ourselves", PERSON },
         { "yourselves", PERSON }, { "they", UNKNOWN }, { "them", UNKNOWN },
         { "themselves", UNKNOWN }, { "self", PERSON }, { "oneself", PERSON },
         { "ownself", PERSON }, { "who", PERSON }, { "whoever", PERSON },
         { "whomever", PERSON } };

   public static final String[][] namedEntitiesArray = { { "PERSON", PERSON },
         { "LOCATION", LOCATION }, { "DATE", DATE },
         { "COMPANY", COMPANY }, { "ORGANIZATION", ORGANIZATION },
         { "MONEY", MONEY }, { "PERCENT", PERCENT } };

   public static final String[] argumentsArray = { FeatureExtractor.VERB,
         TargetAnnotation.ARG0, TargetAnnotation.ARG1, TargetAnnotation.ARG2 };

   private static boolean debug = false;

   private HashMap<String, String> pronouns;
   private HashMap<String, String> namedEntities;
   private HashMap<String, String> targets;
   private HashMap<String, Sentence> namedEntityAnnotations;
   private List<BasicLabeledDatum> sentenceFeatures;


   public FeatureExtractor(String frame) throws IOException {
      setupPronouns();
      setupNamedEntities();
      setupTargets(frame);
      setupNamedEntityAnnotations(frame);
      sentenceFeatures = new ArrayList<BasicLabeledDatum>();
   }


   private void setupPronouns() {
      pronouns = new HashMap<String, String>();
      for (int i = 0; i < pronounsArray.length; i++) {
         String pronoun = pronounsArray[i][0];
         String type = pronounsArray[i][1];
         if (!pronouns.containsKey(pronoun)) {
            pronouns.put(pronoun, type);
         }
      }
   }


   private void setupNamedEntities() {
      namedEntities = new HashMap<String, String>();
      for (int i = 0; i < namedEntitiesArray.length; i++) {
         String namedEntity = namedEntitiesArray[i][0];
         String type = namedEntitiesArray[i][1];
         if (!namedEntities.containsKey(namedEntity)) {
            namedEntities.put(namedEntity, type);
         }
      }
   }


   private void setupTargets(String frame) throws IOException {
      String file = frame + ".verbs";
      targets = new HashMap<String, String>();
      TextFileLineIterator tfli = new TextFileLineIterator(file);
      while (tfli.hasNext()) {
         String target = ( tfli.next()).trim().toLowerCase();
         if (!targets.containsKey(target)) {
            targets.put(target, target);
         }
      }
   }


   private void setupNamedEntityAnnotations(String frame) throws IOException {
      String file = frame + ".netagged";
      namedEntityAnnotations = new HashMap<String, Sentence>();
      NamedEntityAnnotationIterator neai = new NamedEntityAnnotationIterator(file);
      while (neai.hasNext()) {
         Sentence s = (Sentence) neai.next();
         String key = getText(s);
         if (!namedEntityAnnotations.containsKey(key)) {
            namedEntityAnnotations.put(key, s);
         }
      }
   }


   private String getText(Sentence s) {
      return s.toString().substring(s.toString().indexOf(":") + 1).trim();
   }


   private List<BasicLabeledDatum> getSentenceFeatures() {
      return sentenceFeatures;
   }


   private void addSentenceFeatures(BasicLabeledDatum sf) {
      this.sentenceFeatures.add(sf);
   }


   private void addFeatures(TargetAnnotation ta, Sentence s, ArrayList<String> features) {
      String argLabel = argumentsArray[0];
      String vtype = ta.getTarget();
      String vf = makeFeature(argLabel, vtype);
      features.add(vf);
      // int maxArg = 4;

      int startArg = 1;
      int maxArg = argumentsArray.length;
      if (WNMODE == 4) {
         return;
      }
      for (int i = startArg; i < maxArg; i++) {
         argLabel = argumentsArray[i];
         String proplabel = ta.getArgAddresses(argLabel);
         String type = getType(proplabel, s);
         if (!(type.equals(NOT_ANNOTATED) || type.equals(UNKNOWN))) {
            if (WNMODE == 5) {
               String h = getHead(proplabel, s);
               String ah = makeFeature(argLabel, h);
               features.add(ah);
            } else {
               String af = makeFeature(argLabel, type);
               features.add(af);
            }
            // features.add(vf+"&"+af);
            // features.add(vf+"&"+h);
         }
      }

      features.add("def");

   }


   private String getHead(String proplabel, Sentence s) {
      String type = null;
      TreeBankAnnotation tba = (TreeBankAnnotation) s
            .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
      HeadedTree tree = tba.getNormalizedTree();
      int headIndex = getHeadIndex(proplabel, tree);
      if (headIndex >= 0) {
         int index = getAdjustedIndex(headIndex, s);
         type = s.getElement(index).toString();
      } else {
         type = NOT_ANNOTATED;
      }
      return type;
   }


   private String getType(String proplabel, Sentence s) {
      String type = null;
      TreeBankAnnotation tba = (TreeBankAnnotation) s
            .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
      HeadedTree tree = tba.getNormalizedTree();
      int headIndex = getHeadIndex(proplabel, tree);
      if (headIndex >= 0) {
         int index = getAdjustedIndex(headIndex, s);
         String head = s.getElement(index).toString();
         if ((type = checkNamedEntity(s, index)) == null) {
            if ((type = checkPronouns(head)) == null) {
               if ((type = checkWordNet(head)) == null) {
                  type = UNKNOWN;
               }
            }
         }
         if (debug)
            type += "<" + head + ">";
      } else {
         type = NOT_ANNOTATED;
      }
      return type;
   }


   private String checkNamedEntity(Sentence s, int headIndex) {
      String type = null;
      String text = getText(s);
      if (namedEntityAnnotations.containsKey(text)) {
         Sentence nameTagged = (Sentence) namedEntityAnnotations.get(text);
         NamedEntityAnnotation nea = (NamedEntityAnnotation) nameTagged
               .getAnnotation(UtteranceAnnotation.NAMEDENTITY);
         String tag = nea.getWordTag(headIndex);
         if (!tag.equalsIgnoreCase(NOTAG)) {
            if (namedEntities.containsKey(tag)) {
               type = checkWordNet(tag);
               // type =  namedEntities.get(tag) + (debug ? "<" + tag +
               // ">" : "");
            } else {
               System.err.println("Unknown tag: " + tag);
            }
         } else {

         }
      }
      return type;
   }


   private String checkPronouns(String word) {
      String type = null;
      if (pronouns.containsKey(word.toLowerCase())) {
         type =  pronouns.get(word.toLowerCase());
         if (!type.equals(UNKNOWN)) {
            type =  pronouns.get(word.toLowerCase());
         }
      }
      return type;
   }


   private String checkWordNet(String word) {
      /*
       * String type = null; if (SimpleWordNetInterface.inWordNet(word)) { if
       * (SimpleWordNetInterface.isAnimate(word)) { type = PERSON; } else if
       * (SimpleWordNetInterface.isPhysicalObject(word) ||
       * SimpleWordNetInterface.isSubstance(word)) { type = PHYSICAL_OBJECT; }
       * else { type = NOT_PHYSICAL_OBJECT; } } if (type == null){ if
       * (SimpleWordNetInterface.inWordNet(word.substring(0, word.length()-1))) {
       * word = word.substring(0, word.length()-1); if
       * (SimpleWordNetInterface.isAnimate(word)) { type = PERSON; } else if
       * (SimpleWordNetInterface.isPhysicalObject(word) ||
       * SimpleWordNetInterface.isSubstance(word)) { type = PHYSICAL_OBJECT; }
       * else { type = NOT_PHYSICAL_OBJECT; } } } return null;
       */

      if (SimpleWordNetInterface.inWordNet(word)) {
         return SimpleWordNetInterface.getNounAncestorAtDistance(word, WNMODE);
      } else {
         word = word.substring(0, word.length() - 1);
         if (SimpleWordNetInterface.inWordNet(word)) {
            return SimpleWordNetInterface.getNounAncestorAtDistance(word, WNMODE);
         }
         return null;
      }

   }


   private int getHeadIndex(String proplabel, HeadedTree tree) {
      int headIndex = -1;
      if (proplabel != null) {
         Vector<String> syntacticRelations = getSyntacticRelations(proplabel);
         for (int i = 0; i < syntacticRelations.size(); i++) {
            String syntacticRelation =  syntacticRelations.elementAt(i);
            int terminal = getTerminal(syntacticRelation);
            int height = getHeight(syntacticRelation);
            headIndex = getIndex(terminal, height, tree);
            if (headIndex >= 0)
               break;
         }
      }
      return headIndex;
   }


   private Vector<String> getSyntacticRelations(String proplabel) {
      Vector<String> syntacticRelations = new Vector<String>();
      StringTokenizer nodes = new StringTokenizer(proplabel, "*");
      while (nodes.hasMoreTokens()) {
         StringTokenizer splitArguments = new StringTokenizer(nodes.nextToken(), ",");
         while (splitArguments.hasMoreTokens()) {
            syntacticRelations.add(splitArguments.nextToken());
         }
      }
      return syntacticRelations;
   }


   private int getIndex(int terminal, int height, HeadedTree tree) {
      int headIndex = -1;
      HeadedTree node = (HeadedTree) tree.getNode(terminal, height);
      HeadedTree nounPhrase = node;
      if (node.getLabel().equalsIgnoreCase(TreeBankSymbols.PP)) {
         ArrayList children = (ArrayList) node.getChildren();
         for (int i = 0; i < children.size(); i++) {
            HeadedTree child = (HeadedTree) children.get(i);
            if (child.getLabel().equalsIgnoreCase(TreeBankSymbols.NP)) {
               nounPhrase = child;
               break;
            }
         }
      }
      String head = nounPhrase.getHead();
      if (!isTrace(head)) {
         String nodeYield = ArrayListToString(node.getYield());
         int headStart = nodeYield.indexOf(head);
         StringTokenizer st = new StringTokenizer(nodeYield.substring(0, headStart));
         int nodeIndex = st.countTokens();
         headIndex = terminal + nodeIndex;
      }
      return headIndex;
   }


   private int getTerminal(String syntacticRelation) {
      return Integer.parseInt(syntacticRelation.substring(0, syntacticRelation
            .indexOf(":")));
   }


   private int getHeight(String syntacticRelation) {
      return Integer.parseInt(syntacticRelation.substring(syntacticRelation
            .indexOf(":") + 1, syntacticRelation.length()));
   }


   private String makeFeature(String arg, String type) {
      return arg + "=" + type;
   }


   private boolean isTrace(String head) {
      return head.indexOf("*") >= 0;
   }


   private String ArrayListToString(List<String> list) {
      String s = "";
      for (int i = 0; i < list.size(); i++) {
         String word =  list.get(i);
         s += word + " ";
      }
      return s.trim();
   }


   public static int getAdjustedIndex(int index, Sentence s) {
      TreeBankAnnotation tba = (TreeBankAnnotation) s
            .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
      List<String> original = tba.getOriginalTree().getYield();
      List<Word> normalized = s.getElements();
      int j = 0;
      int traces = 0;
      for (int i = 0; i <= index; i++) {
         String originalWord = original.get(i);
         String normalizedWord = normalized.get(j).getOrthography();
         if (!originalWord.equals(normalizedWord)) {
            traces++;
         } else {
            j++;
         }
      }
      return index - traces;
   }


   public static List extractFeatures(String frame, int start, int end) throws IOException {
      FeatureExtractor fe = new FeatureExtractor(frame);
      MetaphorTreeNormalizer trans = new MetaphorTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", 0, 5000, true);
      TreeBankFileIterator tbfi = new TreeBankFileIterator("wsj", ff, trans);
      PropBankFileIterator pbfi = new PropBankFileIterator("prop.txt", 0, 5000);
      PropBankIterator pbi = new PropBankIterator(tbfi, pbfi);
      MetaphorFilter mf = new MetaphorFilter(pbi, frame + ".verbs");
      NumberRangeIterator nri = new NumberRangeIterator(start, end, mf);
      MetaphorTaggedFileIterator mtfi = new MetaphorTaggedFileIterator(frame
            + ".metalabel");
      HashMap<String, MetaphoricCandidates> mah = new HashMap<String, MetaphoricCandidates>();
      while (mtfi.hasNext()) {
         MetaphoricCandidates ma = mtfi.next();
         // System.out.println(ma.getSource()+ma.getIndex());
         mah.put(ma.getSource() + ma.getIndex(), ma);
      }
      while (nri.hasNext()) {
         Sentence s = (Sentence) nri.next();
         PropBankAnnotation pba = (PropBankAnnotation) s
               .getAnnotation(UtteranceAnnotation.PROPBANK);
         Tree original = ((TreeBankAnnotation) s
               .getAnnotation(UtteranceAnnotation.PENNTREEBANK)).getNormalizedTree();
         List yield = original.getYield();
         Iterator iter = pba.getTargetAnnotations().iterator();
         while (iter.hasNext()) {
            TargetAnnotation ta = (TargetAnnotation) iter.next();
            String target = ta.getTarget();
            if (fe.targets.containsKey(target.toLowerCase())) {
               ArrayList<String> features = new ArrayList<String>();
               fe.addFeatures(ta, s, features);
               int taIndex = getAdjustedIndex(ta.getTargetIndex(), s);

               if (mah.get(ta.getSource() + ta.getIndex()) != null) {
                  MetaphoricCandidates ma = (MetaphoricCandidates) mah.get(ta.getSource()
                        + ta.getIndex());

                  if (ma.getWordTag(taIndex) != null) {
                     String label = ma.getWordTag(taIndex).getMetaphor();
                     BasicLabeledDatum bld = new BasicLabeledDatum(label, features);
                     fe.addSentenceFeatures(bld);
                     if (debug && fe.getSentenceFeatures().size() < 10) {
                        System.out.println(s);
                        System.out.println(bld + "\n");
                     }
                  }
               } else {
                  // System.out.println(ta.getSource()+"-"+ta.getIndex()+"-"+taIndex);
               }
               /*
                * if (ma.getSource().equals(ta.getSource()) && ma.getIndex() ==
                * ta.getIndex() && ma.getWordTag(taIndex) != null){ label =
                * ma.getWordTag(taIndex).getMetaphor(); ma = mtfi.next();
                * 
                *  } else { //System.out.println("Alignment issue");
                * //System.out.println(ma);
                * 
                * //System.out.println(ta);
                * 
                * //System.out.println(yield); //System.exit(0); }
                */

               // SentenceFeatures sf = new SentenceFeatures(s, ta, bld);
            }
         }
      }
      return fe.getSentenceFeatures();
   }

   private static final int THENODE = 0;
   private static final int PARENT = 1;
   private static final int GRANDPARENT = 2;
   private static final int MOSTDISTANTANCESTOR = -1;
   private static final int SECONDMOSTDISTANT = -2;
   private static final int THIRDMOSTDISTANT = -3;

   private static int WNMODE = THENODE;


   public static void main(String[] args) throws IOException {
      String frame = "";
      int metaTrainSize = 0;
      int metaTestSize = 0;
      int litTrainSize = 0;
      int litTestSize = 0;

      try {
         frame = args[0];
         metaTrainSize = Integer.parseInt(args[1]);
         metaTestSize = Integer.parseInt(args[2]);
         litTrainSize = Integer.parseInt(args[3]);
         litTestSize = Integer.parseInt(args[4]);
      } catch (Exception e) {
         System.out
               .println("The arguments for this FeatureExtractor.main are:\n\t1) Verb File Root\n\t2) Metaphor Training Set Size\n"
                     + "\t3) Metaphor Testing Set Size\n\t4) Literal Training Set Size\n\t5)Literal Testing Set Size");
         System.out.println("Exception: " + e);
         System.exit(0);
      }

      boolean test = true;
      int[] modes = { 4, 5, 0, 1, 2, -1, -2, -3 };
      FeatureExtractor.debug = false;

      for (int mode = 0; mode < modes.length; mode++) {
         WNMODE = modes[mode];

         System.out.println("WordNet Method: " + WNMODE);
         List sentenceFeatures = FeatureExtractor.extractFeatures(frame, 0, 5000);
         List<BasicLabeledDatum> positiveExamples = new ArrayList<BasicLabeledDatum>();
         List<BasicLabeledDatum> negativeExamples = new ArrayList<BasicLabeledDatum>();

         for (Iterator i = sentenceFeatures.iterator(); i.hasNext();) {
            // System.out.println(sentenceFeatures.get(i));
            BasicLabeledDatum bld = (BasicLabeledDatum) i.next();
            if (bld.getLabel().equals("+")) {
               positiveExamples.add(bld);
            } else if (bld.getLabel().equals("-")) {
               negativeExamples.add(bld);
            }
         }
         System.out.println("Total Datum: " + sentenceFeatures.size());
         System.out.println("NumPos:" + positiveExamples.size() + "   numNeg:"
               + negativeExamples.size());

         List<LabeledDatum> trainingData = new ArrayList<LabeledDatum>();
         List<BasicLabeledDatum> testData = new ArrayList<BasicLabeledDatum>();

         trainingData.addAll(positiveExamples.subList(0, metaTrainSize));
         trainingData.addAll(negativeExamples.subList(0, litTrainSize));
         testData.addAll(positiveExamples.subList(metaTrainSize, metaTrainSize
               + metaTestSize));
         testData.addAll(negativeExamples.subList(litTrainSize, litTrainSize
               + litTestSize));

         // Training of the classifier

         ClassifierTrainer ct = new ClassifierTrainer();
         ct.incrementCounts(trainingData);
         ct.addLabeledData(trainingData);
         ProbabilisticClassifier cl = ct.trainMaxEnt(40);

         // Testing

         int numRight = 0;
         int total = 0;
         int numPos = 0;
         int numNeg = 0;
         int numPosRight = 0;
         int numPosWrong = 0;
         int numNegRight = 0;
         int numNegWrong = 0;
         List<BasicLabeledDatum> wrongOnes = new ArrayList<BasicLabeledDatum>();
         List<BasicLabeledDatum> qdata = testData;
         System.out.println("RESULTS");
         for (Iterator i = qdata.iterator(); i.hasNext();) {
            total++;
            BasicLabeledDatum bld = (BasicLabeledDatum) i.next();
            String guess = cl.getLabel(bld);

            if (guess.equals(bld.getLabel())) {
               numRight++;
               if (bld.getLabel().equals("+"))
                  numPosRight++;
               if (bld.getLabel().equals("-"))
                  numNegRight++;
            } else {
               wrongOnes.add(bld);
               if (bld.getLabel().equals("+"))
                  numPosWrong++;
               if (bld.getLabel().equals("-"))
                  numNegWrong++;
            }
            if (bld.getLabel().equals("+"))
               numPos++;
            if (bld.getLabel().equals("-"))
               numNeg++;

         }
         System.out.println("NR:" + numRight + "    Total:" + total);
         System.out.println("N+R:" + numPosRight + "   N+W:" + numPosWrong);
         System.out.println("N-R:" + numNegRight + "   N-W:" + numNegWrong);
         System.out.println(wrongOnes);

      }
   }
}


package compling.classifier;

import compling.util.*;
import java.io.*;
import java.util.*;
import compling.classifier.MaximumEntropyClassifierTester.*;


public class ClassifierTrainer {

   Counter<String> featureCounter = new Counter<String>();
   Interner<String> featureInterner = new Interner<String>();
   List<LabeledDatum> allData = new ArrayList<LabeledDatum>();


   public void removeLowCountFeaturesFromCounter(int cutoff) {
      featureCounter.removeLowCountKeys(cutoff);
   }


   public ProbabilisticClassifier trainMaxEnt(int numIterations) {
      return (ProbabilisticClassifier) train(new MaximumEntropyClassifier.Factory(1.0, numIterations));
   }


   public Classifier train(ClassifierFactory factory) {
      featureInterner = null;
      featureCounter = null;
      System.out.println("Beginning training");
      return factory.trainClassifier(allData);
   }


   public void incrementCounts(List<LabeledDatum> labeledData) {
       for (LabeledDatum datum : labeledData) incrementCounts(datum);   
   }


   public void incrementCounts(LabeledDatum bld) {
       for (String feature : bld.getFeatures()) featureCounter.incrementCount(feature, 1);
   }


   private BasicLabeledDatum removeLowCountFeatures(LabeledDatum bld) {
      Collection<String> featureList = bld.getFeatures();
      List<String> newFeatureList = new ArrayList<String>();
      for (String feature: featureList){
	  if (featureCounter.containsKey(feature)){
	      newFeatureList.add(featureInterner.intern(feature));
	  }
      }
      return new BasicLabeledDatum(bld.getLabel(), newFeatureList);
   }


   public void addLabeledDatum(LabeledDatum bld) {
      allData.add(removeLowCountFeatures(bld));
   }


   public void addLabeledData(List<LabeledDatum> data) {
      for (LabeledDatum dataPoint: data) {
         addLabeledDatum(dataPoint);
      }
   }


   public static void saveClassifier(String name, Classifier classifier) {
      try {
         System.out.print("Saving classifier... ");
         // Serialize to a file
         ObjectOutput out = new ObjectOutputStream(new FileOutputStream(name));
         out.writeObject(classifier);
         out.close();
         System.out.println("done");
      } catch (IOException e) {
         System.out.println("\nCould not write model file" + e);
      }
   }

}

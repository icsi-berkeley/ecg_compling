
package compling.classifier;

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
import java.io.Serializable;
//import java.util.ArrayList;
import java.util.Iterator;
//import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
//import java.lang.Runtime;
import compling.util.*;
import compling.util.math.*;


/**
 * Harness for building and testing a maximum-entropy classifier. Contains a toy
 * problem and a real proper-noun classification problem.
 * 
 * @author Dan Klein
 */
public class MaximumEntropyClassifierTester implements Serializable {

   public static class MaximumEntropyClassifier implements ProbabilisticClassifier,
         Serializable {

      /**
       * Factory for training MaximumEntropyClassifiers.
       */
      public static class Factory implements ClassifierFactory, Serializable {

         double sigma;
         int iterations;


         public Classifier trainClassifier(List<LabeledDatum> trainingData) {
            // build data encodings so the inner loops can be efficient
            Encoding encoding = buildEncoding(trainingData);
            System.out.println("Num unique features is:  "
                  + encoding.getNumFeatures());
            IndexLinearizer indexLinearizer = buildIndexLinearizer(encoding);
            encoding.labelTracker = new LabelTracker();
            double[] initialWeights = buildInitialWeights(indexLinearizer);
            EncodedDatum[] data = encodeData(trainingData, encoding);
            // build a minimizer object
            GradientMinimizer minimizer = new LBFGSMinimizer(iterations);
            // build the objective function for this data
            DifferentiableFunction objective = new ObjectiveFunction(encoding, data,
                  indexLinearizer, sigma);
            // learn our voting weights
            double[] weights = minimizer.minimize(objective, initialWeights, 1e-4);
            // build a classifer using these weights (and the data encodings)
            return new MaximumEntropyClassifier(weights, encoding, indexLinearizer);
         }


         private double[] buildInitialWeights(IndexLinearizer indexLinearizer) {
            return DoubleArrays.constantArray(0.0, indexLinearizer
                  .getNumLinearIndexes());
         }


         private IndexLinearizer buildIndexLinearizer(Encoding encoding) {
            return new SparseIndexLinearizer(encoding.getNumFeatures(), encoding
                  .getNumLabels(), encoding.getLabelTracker());
         }


         /**
          * Not only does this build an encoding, but it also builds a data
          * structure that tracks the labels associated with each feature
          */
         private Encoding buildEncoding(List<LabeledDatum> data) {
            Indexer<String> featureIndexer = new Indexer<String>();
            Indexer<String> labelIndexer = new Indexer<String>();
            LabelTracker labelTracker = new LabelTracker();
	    //            for (Iterator i = data.iterator(); i.hasNext();) {
            for (LabeledDatum labeledDatum : data){
		//               LabeledDatum labeledDatum = (LabeledDatum) i.next();
               labelIndexer.add(labeledDatum.getLabel());
               for (Iterator s = labeledDatum.getFeatures().iterator(); s.hasNext();) {
                  String feature = (String) s.next();
                  featureIndexer.add(feature);
                  labelTracker.addLabel(featureIndexer.indexOf(feature), labelIndexer
                        .indexOf((String) labeledDatum.getLabel()));
               }
            }
            System.out.println(labelIndexer.getObjects());
            return new Encoding(featureIndexer, labelIndexer, labelTracker);
         }


         private EncodedDatum[] encodeData(List<LabeledDatum> data, Encoding encoding) {
            EncodedDatum[] encodedData = new EncodedDatum[data.size()];
            for (int i = 0; i < data.size(); i++) {
               LabeledDatum labeledDatum = (LabeledDatum) data.get(i);
               encodedData[i] = EncodedDatum.encodeLabeledDatum(labeledDatum,
                     encoding);
            }
            return encodedData;
         }


         /**
          * Sigma controls the variance on the prior / penalty term. 1.0 is a
          * reasonable value for large problems, bigger sigma means LESS
          * smoothing. Zero sigma is a special indicator that no smoothing is to
          * be done. <p/> Iterations determines the maximum number of iterations
          * the optimization code can take before stopping.
          */
         public Factory(double sigma, int iterations) {
            this.sigma = sigma;
            this.iterations = iterations;
         }
      }

      /**
       * This is the MaximumEntropy objective function: the (negative) log
       * conditional likelihood of the training data, possibly with a penalty
       * for large weights. Note that this objective get MINIMIZED so it's the
       * negative of the objective we normally think of.
       */
      public static class ObjectiveFunction implements DifferentiableFunction,
            Serializable {

         IndexLinearizer indexLinearizer;
         Encoding encoding;
         EncodedDatum[] data;

         double sigma;

         double lastValue;
         double[] lastDerivative;
         double[] lastX;


         public int dimension() {
            return indexLinearizer.getNumLinearIndexes();
         }


         public double valueAt(double[] x) {
            ensureCache(x);
            return lastValue;
         }


         public double[] derivativeAt(double[] x) {
            ensureCache(x);
            return lastDerivative;
         }


         private void ensureCache(double[] x) {
            if (requiresUpdate(lastX, x)) {
               Pair currentValueAndDerivative = calculate(x);
               lastValue = ((Double) currentValueAndDerivative.getFirst())
                     .doubleValue();
               lastDerivative = (double[]) currentValueAndDerivative.getSecond();
               lastX = x;
            }
         }


         private boolean requiresUpdate(double[] lastX, double[] x) {
            if (lastX == null)
               return true;
            for (int i = 0; i < x.length; i++) {
               if (lastX[i] != x[i])
                  return true;
            }
            return false;
         }


         /**
          * The important part of the classifier learning process! This method
          * determines, for the given weight vector x, what the (negative) log
          * conditional likelihood of the data is, as well as the derivatives of
          * that likelihood wrt each weight parameter.
          */
         private Pair calculate(double[] x) {
            double objective = 0.0;
            double[] derivatives = DoubleArrays.constantArray(0.0, dimension());
            // TODO: compute the objective and its derivatives
            // TODO

            // logProb
            for (int d = 0; d < data.length; d++) {
               EncodedDatum datum = data[d];
               double[] logProbabilities = MaximumEntropyClassifier
                     .getLogProbabilities(datum, x, encoding, indexLinearizer);
               int c = datum.getLabelIndex();
               objective -= logProbabilities[c];
               for (int labelIndex = 0; labelIndex < encoding.getNumLabels(); labelIndex++) {
                  double expectation = Math.exp(logProbabilities[labelIndex]);
                  for (int i = 0; i < datum.getNumActiveFeatures(); i++) {
                     int featureIndex = datum.getFeatureIndex(i);
                     double featureCount = datum.getFeatureCount(i);
                     int index = indexLinearizer.getLinearIndex(featureIndex,
                           labelIndex);
                     if (index == -1) {
                        continue;
                     } // labels not represented are ignored
                     derivatives[index] += expectation * featureCount;
                     // System.out.println("Found feature
                     // "+encoding.getFeature(featureIndex)+" with label
                     // "+encoding.getLabel(labelIndex)+" with exp
                     // "+expectation+" vs "+featureCount*(labelIndex == c ? 1 :
                     // 0));
                     if (labelIndex == c)
                        derivatives[index] -= featureCount;
                  }
               }
            }
            // penalties
            if (sigma != 0.0) {
               for (int i = 0; i < x.length; i++) {
                  double val = x[i];
                  objective += val * val / 2.0 / sigma / sigma;
                  derivatives[i] += val / sigma / sigma;
               }
            }
            return new Pair(new Double(objective), derivatives);
         }


         public ObjectiveFunction(Encoding encoding, EncodedDatum[] data,
               IndexLinearizer indexLinearizer, double sigma) {
            this.indexLinearizer = indexLinearizer;
            this.encoding = encoding;
            this.data = data;
            this.sigma = sigma;
         }
      }

      /**
       * EncodedDatums are sparse representations of (labeled) feature count
       * vectors for a given data point. Use getNumActiveFeatures() to see how
       * many features have non-zero count in a datum. Then, use
       * getFeatureIndex() and getFeatureCount() to retreive the number and
       * count of each non-zero feature. Use getLabelIndex() to get the label's
       * number.
       */
      public static class EncodedDatum implements Serializable {

         public static EncodedDatum encodeDatum(Datum datum, Encoding encoding) {
            Collection<String> features = datum.getFeatures();
            Counter<String> featureCounter = new Counter<String>();
            //for (Iterator s = features.iterator(); s.hasNext();) {
            //   String feature = (String) s.next();
	    for (String feature : features){
               if (encoding.getFeatureIndex(feature) < 0)
                  continue;
               featureCounter.incrementCount(feature, 1.0);
            }
            int numActiveFeatures = featureCounter.keySet().size();
            int[] featureIndexes = new int[numActiveFeatures];
            double[] featureCounts = new double[featureCounter.keySet().size()];
            int i = 0;
            for (Iterator s = featureCounter.keySet().iterator(); s.hasNext();) {
               String feature = (String) s.next();
               int index = encoding.getFeatureIndex(feature);
               double count = featureCounter.getCount(feature);
               featureIndexes[i] = index;
               featureCounts[i] = count;
               i++;
            }
            EncodedDatum encodedDatum = new EncodedDatum(-1, featureIndexes,
                  featureCounts);
            return encodedDatum;
         }


         public static EncodedDatum encodeLabeledDatum(LabeledDatum labeledDatum,
               Encoding encoding) {
            EncodedDatum encodedDatum = encodeDatum(labeledDatum, encoding);
            encodedDatum.labelIndex = encoding.getLabelIndex(labeledDatum.getLabel());
            return encodedDatum;
         }

         int labelIndex;
         int[] featureIndexes;
         double[] featureCounts;


         public int getLabelIndex() {
            return labelIndex;
         }


         public int getNumActiveFeatures() {
            return featureCounts.length;
         }


         public int getFeatureIndex(int num) {
            return featureIndexes[num];
         }


         public double getFeatureCount(int num) {
            return featureCounts[num];
         }


         public EncodedDatum(int labelIndex, int[] featureIndexes,
               double[] featureCounts) {
            this.labelIndex = labelIndex;
            this.featureIndexes = featureIndexes;
            this.featureCounts = featureCounts;
         }
      }

      /**
       * The Encoding maintains correspondences between the various
       * representions of the data, labels, and features. The external
       * representations of labels and features are object-based. The functions
       * getLabelIndex() and getFeatureIndex() can be used to translate those
       * objects to integer representatiosn: numbers between 0 and
       * getNumLabels() or getNumFeatures() (exclusive). The inverses of this
       * map are the getLabel() and getFeature() functions.
       */
      public static class Encoding implements Serializable {

         Indexer featureIndexer;
         Indexer labelIndexer;
         LabelTracker labelTracker;


         public int getNumFeatures() {
            return featureIndexer.size();
         }


         public int getFeatureIndex(String feature) {
            return featureIndexer.indexOf(feature);
         }


         public String getFeature(int featureIndex) {
            return (String) featureIndexer.get(featureIndex);
         }


         public int getNumLabels() {
            return labelIndexer.size();
         }


         public int getLabelIndex(String label) {
            return labelIndexer.indexOf(label);
         }


         public String getLabel(int labelIndex) {
            return (String) labelIndexer.get(labelIndex);
         }


         public LabelTracker getLabelTracker() {
            return labelTracker;
         }


         public Encoding(Indexer featureIndexer, Indexer labelIndexer) {
            this.featureIndexer = featureIndexer;
            this.labelIndexer = labelIndexer;
         }


         public Encoding(Indexer featureIndexer, Indexer labelIndexer,
               LabelTracker labelTracker) {
            this(featureIndexer, labelIndexer);
            this.labelTracker = labelTracker;
         }

      }

      /**
       * The IndexLinearizer maintains the linearization of the two-dimensional
       * features-by-labels pair space. This is because, while we might think
       * about lambdas and derivatives as being indexed by a feature-label pair,
       * the optimization code expects one long vector for lambdas and
       * derivatives. To go from a pair featureIndex, labelIndex to a single
       * pairIndex, use getLinearIndex().
       */
      public static class IndexLinearizer implements Serializable {

         int numFeatures;
         int numLabels;


         public int getNumLinearIndexes() {
            return numFeatures * numLabels;
         }


         public int getLinearIndex(int featureIndex, int labelIndex) {
            return labelIndex + featureIndex * numLabels;
         }


         public int getFeatureIndex(int linearIndex) {
            return linearIndex / numLabels;
         }


         public int getLabelIndex(int linearIndex) {
            return linearIndex % numLabels;
         }


         public IndexLinearizer(int numFeatures, int numLabels) {
            this.numFeatures = numFeatures;
            this.numLabels = numLabels;
            System.out.println("NumFeats: " + numFeatures + " ;  NumLabels:  "
                  + numLabels);
         }
      }

      public static class SparseIndexLinearizer extends IndexLinearizer implements
            Serializable {

         int numFeatures;
         int numLabels;
         int numLinearIndexes;
         int[] featureIndexToLinearIndexOffset;
         int[][] raggedIndex;


         public int getNumLinearIndexes() {
            return numLinearIndexes;
         }


         // This returns -1 if the label isn't there
         private int getLabelOffset(int featureIndex, int labelIndex) {
            int maxLabels = raggedIndex[featureIndex].length;
            for (int i = 0; i < maxLabels; i++) {
               if (raggedIndex[featureIndex][i] == labelIndex) {
                  return i;
               }
            }
            return -1;
         }


         public int getLinearIndex(int featureIndex, int labelIndex) {
            int labelOffset = getLabelOffset(featureIndex, labelIndex);
            if (labelOffset == -1) {
               return -1;
            }
            return featureIndexToLinearIndexOffset[featureIndex] + labelOffset;
         }


         public int getFeatureIndex(int linearIndex) {
            for (int i = 0; i < numFeatures - 1; i++) {
               if (linearIndex >= featureIndexToLinearIndexOffset[i]
                     && linearIndex < featureIndexToLinearIndexOffset[i + 1]) {
                  return i;
               }
            }
            if (linearIndex < getNumLinearIndexes()) {
               return featureIndexToLinearIndexOffset.length - 1;
            }
            return -1;
         }


         public int getLabelIndex(int linearIndex) {
            int featureIndex = getFeatureIndex(linearIndex);
            int labelOffset = linearIndex
                  - featureIndexToLinearIndexOffset[featureIndex];
            return raggedIndex[featureIndex][labelOffset];
         }


         public SparseIndexLinearizer(int numFeatures, int numLabels,
               LabelTracker labelTracker) {
            super(numFeatures, numLabels);

            // build up the raggedIndex and the featureIndexToLinearIndexOffset
            featureIndexToLinearIndexOffset = new int[numFeatures];
            featureIndexToLinearIndexOffset[0] = 0;
            raggedIndex = new int[numFeatures][];

            for (int feature = 0; feature < numFeatures; feature++) {
               HashSet labels = labelTracker.getLabels(feature);
               int numLabelsForThisFeature = labels.size();
               raggedIndex[feature] = new int[numLabelsForThisFeature];
               int labelIndex = 0;
               for (Iterator iter = labels.iterator(); iter.hasNext();) {
                  int label = ((Integer) iter.next()).intValue();
                  raggedIndex[feature][labelIndex] = label;
                  labelIndex++;
               }
               if (feature < numFeatures - 1) {
                  featureIndexToLinearIndexOffset[feature + 1] = featureIndexToLinearIndexOffset[feature]
                        + numLabelsForThisFeature;
               }
            }

            numLinearIndexes = featureIndexToLinearIndexOffset[numFeatures - 1]
                  + raggedIndex[numFeatures - 1].length;
            System.out.println("NumLinearIndexes: " + numLinearIndexes
                  + "   Not Sparse rep would be: " + (numFeatures * numLabels));
         }
      }

      private double[] weights;
      private Encoding encoding;
      private IndexLinearizer indexLinearizer;


      /**
       * Calculate the log probabilities of each class, for the given datum
       * (feature bundle). Note that the weighted votes (refered to as
       * activations) are *almost* log probabilities, but need to be normalized.
       */
      private static double[] getLogProbabilities(EncodedDatum datum,
            double[] weights, Encoding encoding, IndexLinearizer indexLinearizer) {
         double[] activations = DoubleArrays.constantArray(0.0, encoding
               .getNumLabels());
         for (int i = 0; i < datum.getNumActiveFeatures(); i++) {
            int featureIndex = datum.getFeatureIndex(i);
            double featureCount = datum.getFeatureCount(i);
            for (int labelIndex = 0; labelIndex < encoding.getNumLabels(); labelIndex++) {
               int weightIndex = indexLinearizer.getLinearIndex(featureIndex,
                     labelIndex);
               if (weightIndex == -1) {
                  continue;
               }
               double weight = weights[weightIndex];
               activations[labelIndex] += weight * featureCount;
            }
         }
         double logTotal = SloppyMath.logAdd(activations);
         for (int i = 0; i < activations.length; i++) {
            activations[i] -= logTotal;
         }
         return activations;
      }


      public Counter<String> getProbabilities(Datum datum) {
         EncodedDatum encodedDatum = EncodedDatum.encodeDatum(datum, encoding);
         double[] logProbabilities = getLogProbabilities(encodedDatum, weights,
               encoding, indexLinearizer);
         return logProbabilityArrayToProbabilityCounter(logProbabilities);
      }


      private Counter logProbabilityArrayToProbabilityCounter(
            double[] logProbabilities) {
         Counter<String> probabiltyCounter = new Counter<String>();
         for (int labelIndex = 0; labelIndex < logProbabilities.length; labelIndex++) {
            double logProbability = logProbabilities[labelIndex];
            double probability = Math.exp(logProbability);
            String label = (String) encoding.getLabel(labelIndex);
            probabiltyCounter.setCount(label, probability);
         }
         return probabiltyCounter;
      }


      public String getLabel(Datum datum) {
         return (String) getProbabilities(datum).argMax();
      }


      public MaximumEntropyClassifier(double[] weights, Encoding encoding,
            IndexLinearizer indexLinearizer) {
         this.weights = weights;
         this.encoding = encoding;
         this.indexLinearizer = indexLinearizer;
      }
   }
}

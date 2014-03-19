
package compling.classifier;

import compling.util.Counter;


/**
 * @author Dan Klein
 */
public interface ProbabilisticClassifier extends Classifier {

   Counter<String> getProbabilities(Datum datum);
}

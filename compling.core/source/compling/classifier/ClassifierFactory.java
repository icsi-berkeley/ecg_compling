
package compling.classifier;

import java.util.List;


/**
 */
public interface ClassifierFactory {

   Classifier trainClassifier(List<LabeledDatum> trainingData);
}

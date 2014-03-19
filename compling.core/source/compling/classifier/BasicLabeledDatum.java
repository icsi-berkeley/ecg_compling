
package compling.classifier;

import java.util.List;
import java.util.Collection;


/**
 * A minimal implementation of a labeled datum, wrapping a list of features and
 * a label.
 * 
 * @author Dan Klein
 */
public class BasicLabeledDatum implements LabeledDatum {

   String label;
   List<String> features;


   public String getLabel() {
      return label;
   }


   public Collection<String> getFeatures() {
      return features;
   }


   public String toString() {
      return "<" + getLabel() + " : " + getFeatures().toString() + ">";
   }


   public BasicLabeledDatum(String label, List<String> features) {
      this.label = label;
      this.features = features;
   }
}

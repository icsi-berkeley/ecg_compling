
package compling.classifier;

import java.util.*;
import java.io.Serializable;


/**
 * 
 * Tracks the set of labels that a particular feature appears with. Used to
 * build up the sparse representation of the linearized weight array;
 * 
 */

public class LabelTracker implements Serializable {

   Map<Integer, HashSet<Integer>> featureMap;


   /**
    * Return the list of labels associated with the given feature index
    * 
    * @param index
    */
   public HashSet<Integer> getLabels(int index) {
      return featureMap.get(index);
   }


   /**
    * Returns the number of objects indexed.
    */
   public int size() {
      return featureMap.size();
   }


   /**
    * Adds the label to the given feature's label list
    */
   public void addLabel(int featureIndex, int labelIndex) {
      HashSet<Integer> labels = getLabels(featureIndex);
      if (labels == null) {
         labels = new HashSet<Integer>();
         labels.add(labelIndex);
         featureMap.put(featureIndex, labels);
      } else {
         labels.add(labelIndex);
      }
   }


   public LabelTracker() {
      featureMap = new HashMap<Integer, HashSet<Integer>>();
   }
}

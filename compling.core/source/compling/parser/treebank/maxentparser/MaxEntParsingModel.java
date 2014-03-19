
package compling.parser.treebank.maxentparser;

import compling.classifier.*;
import compling.util.*;
import java.util.Iterator;
import java.io.*;


class MaxEntParsingModel {

   ProbabilisticClassifier SRClassifier;
   ProbabilisticClassifier BCClassifier;
   ProbabilisticClassifier SRChunkClassifier;
   ProbabilisticClassifier BCChunkClassifier;


   MaxEntParsingModel(String srFile, String bcFile, String srChFile, String bcChFile) {
      try {
         System.out.println("Loading model/grammar");
         // Deserialize from a file
         File file = new File(srFile);
         ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
         // Deserialize the object
         SRClassifier = (ProbabilisticClassifier) in.readObject();
         in.close();
         file = new File(bcFile);
         in = new ObjectInputStream(new FileInputStream(file));
         // Deserialize the object
         BCClassifier = (ProbabilisticClassifier) in.readObject();
         in.close();
         file = new File(srChFile);
         in = new ObjectInputStream(new FileInputStream(file));
         // Deserialize the object
         SRChunkClassifier = (ProbabilisticClassifier) in.readObject();
         in.close();
         file = new File(bcChFile);
         in = new ObjectInputStream(new FileInputStream(file));
         // Deserialize the object
         BCChunkClassifier = (ProbabilisticClassifier) in.readObject();
         in.close();
      } catch (Exception e) {
         System.out.println("Problem reading in model file:" + e);
         // FIXME: System.exit()!!
         System.exit(0);
      }
   }


   PriorityQueue getBCScores(MaxEntDerivation med) {
      Counter c = BCClassifier.getProbabilities(new BasicLabeledDatum(null,
            BCFeatureExtractor.extractBCFeatures(med)));
      PriorityQueue pq = new PriorityQueue();
      for (Iterator n = c.keySet().iterator(); n.hasNext();) {
         String op = (String) n.next();
         pq.add(op, c.getCount(op));
      }
      return pq;
   }


   PriorityQueue getSRScores(MaxEntDerivation med) {
      Counter c = SRClassifier.getProbabilities(new BasicLabeledDatum(null,
            SRFeatureExtractor.extractSRFeatures(med)));
      PriorityQueue pq = new PriorityQueue();
      for (Iterator n = c.keySet().iterator(); n.hasNext();) {
         String op = (String) n.next();
         pq.add(op, c.getCount(op));
      }
      return pq;
   }


   PriorityQueue getBCChunkScores(MaxEntDerivation med) {
      Counter c = BCChunkClassifier.getProbabilities(new BasicLabeledDatum(null,
            BCFeatureExtractor.extractBCFeatures(med)));
      PriorityQueue pq = new PriorityQueue();
      for (Iterator n = c.keySet().iterator(); n.hasNext();) {
         String op = (String) n.next();
         pq.add(op, c.getCount(op));
      }
      return pq;
   }


   PriorityQueue getSRChunkScores(MaxEntDerivation med) {
      Counter c = SRChunkClassifier.getProbabilities(new BasicLabeledDatum(null,
            SRFeatureExtractor.extractSRFeatures(med)));
      PriorityQueue pq = new PriorityQueue();
      for (Iterator n = c.keySet().iterator(); n.hasNext();) {
         String op = (String) n.next();
         pq.add(op, c.getCount(op));
      }
      return pq;
   }
}

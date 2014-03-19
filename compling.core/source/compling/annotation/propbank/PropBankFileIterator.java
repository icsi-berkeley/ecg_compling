
package compling.annotation.propbank;

//import java.io.*;
import java.io.IOException;
import java.util.*;
import compling.util.fileutil.*;


/**
 * This class iterates through a text file of TargetAnnotations and returns them
 * grouped as PropBankAnnotations.
 * 
 * The startFile and endFile parameters on the constructor are both inclusive.
 * 
 * Each call to next() and nextPBA() returns a PropBankAnnotation
 * 
 * @author John Bryant
 */

public class PropBankFileIterator implements Iterator<PropBankAnnotation> {

   TextFileLineIterator tfli;
   PropBankAnnotation nextPBA = null;
   TargetAnnotation nextTA = null;
   int endFile = 0;


   /**
    * The startFile and endFile parameters refer to the WSJ file numbers within
    * the propbank file. Further, the startFile and endFile params are inclusive
    * on both arguments. So if you enter 200 as the start number, only
    * annotations from 200 onward (inclusive) will be returned upto and
    * including the endFile index.
    * @throws IOException 
    */

   public PropBankFileIterator(String path, int startFile, int endFile) throws IOException {
      tfli = new TextFileLineIterator(path);
      if (startFile > endFile) {
         return;
      }
      moveTFLItoStart(startFile);
      this.endFile = endFile;
      setupNextPBA();
   }


   private void moveTFLItoStart(int start) {
      while (tfli.hasNext()) {
         nextTA = new TargetAnnotation(tfli.next());
         if (nextTA.getSourceFileNum() >= start) {
            return;
         }
      }
      nextTA = null;
   }


   private void setupNextPBA() {
      if (nextTA == null && tfli.hasNext()) {
         nextPBA = new PropBankAnnotation(new TargetAnnotation(tfli.next()));
      } else if (nextTA == null && tfli.hasNext() == false) {
         nextTA = null;
         nextPBA = null;
      } else if (nextTA != null && tfli.hasNext()) {
         nextPBA = new PropBankAnnotation(nextTA);
      } else if (nextTA != null && tfli.hasNext() == false) {
         nextPBA = new PropBankAnnotation(nextTA);
         nextTA = null;
      }
      while (tfli.hasNext()) {
         nextTA = new TargetAnnotation(tfli.next());
         if (nextTA.getSource().equals(nextPBA.getSource())
               && nextTA.getIndex() == nextPBA.getIndex()) {
            nextPBA.addTargetAnnotation(nextTA);
         } else {
            return;
         }
      }
      nextTA = null;
   }


   public boolean hasNext() {
      if (nextPBA == null || nextPBA.getSourceFileNum() > endFile) {
         return false;
      }
      return true;
   }


   public void remove() {
      throw new UnsupportedOperationException(
            "PropBankFileIterator does not support the remove method");
   }


   public PropBankAnnotation next() {
      if (hasNext() == false) {
         throw new NoSuchElementException();
      }
      PropBankAnnotation p = nextPBA;
      setupNextPBA();
      return p;
   }


   public static void main(String[] args) throws IOException {
      PropBankFileIterator pbai = new PropBankFileIterator(args[0], 118, 519);
      while (pbai.hasNext()) {
         System.out.println(pbai.next());
      }
   }
}

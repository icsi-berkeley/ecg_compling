
package compling.annotation.metaphor;

import java.io.*;
import java.util.*;
import compling.util.fileutil.*;
import compling.annotation.metaphor.MetaphoricCandidates.MetaphorTag;

/**
 * This class iterates through a text file of MetaphoricCandidatess and returns
 * them grouped as MetaphoricCandidatess.
 * 
 * Each call to next() and nextMA() returns a MetaphoricCandidates
 * 
 * @author John Bryant
 */

public class MetaphorTaggedFileIterator implements Iterator<MetaphoricCandidates> {

   TextFileLineIterator tfli;
   MetaphoricCandidates nextMC = null;
    String[] nextLine = null;

   public MetaphorTaggedFileIterator(String path) throws IOException {
      tfli = new TextFileLineIterator(path);
      setupNext();
   }


   private String[] processLine(String line) {
      StringTokenizer st = new StringTokenizer(line);
      String[] res = new String[4];
      res[0] = st.nextToken(); //annotation tag
      String sfi = st.nextToken(); 
      res[1] = sfi.substring(0, sfi.indexOf("[")); //source file
      res[2] = sfi.substring(sfi.indexOf("[") + 1, sfi.indexOf("]")); //index therein
      int i = 0;
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         if (token.indexOf("{") != -1) {
            break;
         }
         i++;
      }
      res[4] = ""+i; //annotated word index
      return res;

   }


   private void setupNext() {
       if (tfli.hasNext()){
	   if (nextLine == null){nextLine = processLine(tfli.next());} //haven't processed anything yet
	   nextMC = new MetaphoricCandidates(nextLine[1], Integer.parseInt(nextLine[2]));
	   nextMC.addTag(Integer.parseInt(nextLine[3]), nextLine[0]);

	   while (tfli.hasNext()){
	       nextLine = processLine(tfli.next());
	       if (nextMC.getSource().equals(nextLine[1]) && nextMC.getIndex() == Integer.parseInt(nextLine[2])){
		   nextMC.addTag(Integer.parseInt(nextLine[3]), nextLine[0]);
	       } else { return;}
	   }
	   nextLine = null; //if we reach here, we ran out of file

       } else if (nextLine != null){ //no more file left, but still a next line
	   nextMC = new MetaphoricCandidates(nextLine[1], Integer.parseInt(nextLine[2]));
	   nextMC.addTag(Integer.parseInt(nextLine[3]), nextLine[0]);
	   nextLine = null;
       } else { //no more text file and no more nextLine
	   nextMC = null;
       }
   }


   public boolean hasNext() {
      if (nextMC == null) {
         return false;
      }
      return true;
   }

   public void remove() {
      throw new UnsupportedOperationException(
            "MetaphorTaggedFileIterator does not support the remove method");
   }


   public MetaphoricCandidates next() {
      if (hasNext() == false) {
         throw new NoSuchElementException();
      }
      MetaphoricCandidates p = nextMC;
      setupNext();
      return p;
   }


   public static void main(String[] args) throws IOException {
      MetaphorTaggedFileIterator pbai = new MetaphorTaggedFileIterator(args[0]);
      while (pbai.hasNext()) {
         System.out.println(pbai.next());
      }
   }
}

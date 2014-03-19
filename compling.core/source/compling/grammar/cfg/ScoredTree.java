
package compling.grammar.cfg;

import java.util.*;
import compling.parser.Parse;

//An extension of tree to add a score

public class ScoredTree extends Tree implements Parse{

    public ScoredTree(String label, List<? extends Tree> children) {
	super(label, children);
   }


   public ScoredTree(String label) {
       super(label);
   }

    private double score;
    
    public void setScore(double score){this.score = score;}

    public double score(){return score;}
    

}

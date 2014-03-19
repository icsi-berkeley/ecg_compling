
package compling.parser;

import java.util.*;
import compling.grammar.Rule;

public interface State<SYM, KEY, RULE extends Rule<SYM>, PARSEKIND extends Parse, STATEKIND extends State<SYM, KEY, RULE, PARSEKIND, STATEKIND>>  {

   public   SYM getLHS();


   public   List<STATEKIND> advance(STATEKIND state);


   public   List<STATEKIND> advance(String input);


   public   Set<SYM> getNextSymbols();

    /*Completed is true when we can no longer add children to a state*/
   public   boolean completed();

    /*isGoalState is true when this state can act as a goal state (i.e. should be taken out of the queue*/
    public boolean isGoalState(int utteranceSize);

    public   int getStart();


    public   int getEnd();


   public   KEY getKey();

    public  void incorporate(STATEKIND state);

    public double forwardScore();

    public double viterbiScore();

    public  List<STATEKIND> generateNextStates(SYM sym);

    public PARSEKIND makeBestParse();

    //    public List<PARSEKIND> makeAllParses();
}

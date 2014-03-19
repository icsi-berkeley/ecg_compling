
package compling.parser;

import compling.util.PriorityQueue;
import java.util.List;

public interface Chart<STATE extends State> {

    public List<STATE> getStatesAt(int position);
    
    
    public int getTotalStates();
    
    
    public int getLength();


    public PriorityQueue<STATE> getSpanningStates();


}

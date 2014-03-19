package compling.parser;

import java.util.*;
import compling.util.PriorityQueue;
import compling.parser.*;

public class PartialParseExtractor <PARSEKIND extends ScoredParse, STATE extends State<?, ?, ?, PARSEKIND, STATE>> {
    Chart<STATE> chart;
    List<List<List<STATE>>> table;
    int length = -1;

    public PartialParseExtractor(Chart<STATE> chart){
	this.chart = chart;
	//System.out.println(chart);
	this.length = chart.getLength();
	table = new ArrayList<List<List<STATE>>>();
	//System.out.println("Total chart states:"+chart.getTotalStates());
	for (int i = 0; i <= length; i++){
	    table.add(new ArrayList<List<STATE>>());
	    for ( int j = 0; j <= length; j++){
		table.get(i).add(new ArrayList<STATE>());
	    }
	}
    }

    public List<PARSEKIND> getBestPartialParse(){
	//this is only called when there is no single parse from 0 to length
	for (int numRoots = 1; numRoots <= length; numRoots++){
	    getBestPartialParseHelper(numRoots, length);
	    if (table.get(numRoots).get(length).size() > 0){
		List<List<ScoredPartialParse>> viterbiTable = new ArrayList<List<ScoredPartialParse>>();
		for (int i = 0; i <=length; i++){
		    viterbiTable.add(new ArrayList<ScoredPartialParse>());
		    for (int j = 0; j <= length; j++){
			viterbiTable.get(i).add(new ScoredPartialParse());
		    }
		}
		return buildBestPartialParse(numRoots, length, viterbiTable).partialParse;
	    }
	}
	throw new ParserException("NO PARTIAL PARSE IN getBestPartialParse() is BAD!!!");
    }

    private void getBestPartialParseHelper(int numRoots, int chartPos){
	if (chartPos <= 0 || numRoots <= 0){throw new RuntimeException("bad stuff in getBestPartialParseHelper");}

	if (table.get(numRoots).get(chartPos).size() > 0){ return; } //if we already did this, return
	
	//if we don't possibly have enough utterance left given the number of roots, return
	if (chartPos < numRoots){return;} 

	List<STATE> statesAtChartPos = chart.getStatesAt(chartPos);
	//System.out.println("queue size:"+statesAtChartPos.size());

	for (STATE state : statesAtChartPos){
	    //System.out.println("State:"+state.getKey());
	    if (state.completed()){ //only look at completed states
		//System.out.println("State:"+state.getLHS()+" ("+state.getStart()+","+state.getEnd()+")");
		if (numRoots == 1){ //if we can only use one hop
		    if (state.getStart() == 0){//can only look at elements that start at 0
			table.get(numRoots).get(chartPos).add(state); //add to table
		    }
		} else {//need to do the recursive call
		    getBestPartialParseHelper(numRoots - 1, state.getStart());
		    if (table.get(numRoots - 1).get(state.getStart()).size() > 0){
			table.get(numRoots).get(chartPos).add(state); //add to table
		    }
		}
	    }
	}
    }
	

    class ScoredPartialParse {
	double score = Double.NEGATIVE_INFINITY;
	List<PARSEKIND> partialParse;    
    }

    //this method is pretty close to the one above, but we only look for the best
    private ScoredPartialParse buildBestPartialParse(int numRoots, int pos, List<List<ScoredPartialParse>> viterbiTable){
	if (pos <= 0 || numRoots <= 0){throw new RuntimeException("bad stuff in getBestPartialParseHelper");}
	if (pos < numRoots){throw new RuntimeException("Unanticipated condition in buildBestPartialParse");} 

	ScoredPartialParse myspp = viterbiTable.get(numRoots).get(pos);
	if (viterbiTable.get(numRoots).get(pos).partialParse != null){ //if we already did this, return
	    return myspp; 
	} 
	//for all the states that can reach the beginning with this number
	//of roots and at this position
	for (STATE state : table.get(numRoots).get(pos)){	    
	    if (numRoots == 1){
		PARSEKIND parse = state.makeBestParse();
		if (parse.score() > myspp.score){
		    myspp.score = parse.score();
		    myspp.partialParse = new ArrayList<PARSEKIND>();
		    myspp.partialParse.add(parse);
		}
	    } else {
		ScoredPartialParse spprest = buildBestPartialParse(numRoots-1, state.getStart(), viterbiTable);
		if (spprest.partialParse == null){throw new RuntimeException("null partial parse!");}
		PARSEKIND parse = state.makeBestParse();
		double score = spprest.score + parse.score();
		if (score > myspp.score){
		    myspp.score = score;
		    myspp.partialParse = new ArrayList<PARSEKIND>();
		    myspp.partialParse.addAll(spprest.partialParse);
		    myspp.partialParse.add(parse);
		}
	    }
	}
	return myspp;
    }





}

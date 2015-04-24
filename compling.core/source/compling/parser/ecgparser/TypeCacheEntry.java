package compling.parser.ecgparser;

import java.util.ArrayList;

import compling.grammar.ecg.Grammar.Construction;

public class TypeCacheEntry {
	public ArrayList<ArrayList<Construction>> cxnList;
	public ArrayList<ArrayList<String>> morphList; // just the "morph" half of the corresponding morph-token pairs
	
	public TypeCacheEntry(ArrayList<ArrayList<Construction>> cxns, ArrayList<ArrayList<MorphTokenPair>> morphs) {
		cxnList = cxns;
		morphList = convertMTPair(morphs);
	}
	
	public boolean compareEntry(TypeCacheEntry tce) {
		return (compareCxnList(tce.getCxnList()) && compareMorphList(tce.getMorphList()));
	}
	
	public ArrayList<ArrayList<Construction>> getCxnList() {
		return cxnList;
	}
	
	public ArrayList<ArrayList<String>> getMorphList() {
		return morphList;
	}
	
	public ArrayList<ArrayList<String>> convertMTPair(ArrayList<ArrayList<MorphTokenPair>> morphs) {
		ArrayList<ArrayList<String>> returned = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < morphs.size(); i++) { //ArrayList<MorphTokenPair> slot : morphs) {
			returned.add(new ArrayList<String>());
			for (MorphTokenPair mt : morphs.get(i)) {
				returned.get(i).add(mt.morph);
			}
		}
		return returned;
	}
	
	public boolean compareCxnList(ArrayList<ArrayList<Construction>> cxns) {
		if (cxns.size() != cxnList.size()) {
			return false;
		}
		for (int i = 0; i < cxns.size(); i++) {
			if (cxns.get(i).size() != cxnList.get(i).size()) {
				return false;
			}
			for (int j = 0; j < cxns.get(i).size(); j++) {
				String cxnName = null;
				String cxnListName = null;
				if (!(cxns.get(i).get(j) == null)) {
					cxnName = cxns.get(i).get(j).getName();
				} 
				if (!(cxnList.get(i).get(j) == null)) {
					cxnListName = cxnList.get(i).get(j).getName();
				}
				if (!(cxnName == cxnListName)) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean compareMorphList(ArrayList<ArrayList<String>> morphs) {
		if (morphs.size() != morphList.size()) {
			return false;
		}
		boolean same = true;
		for (int i = 0; i < morphs.size(); i++) {
			if (morphs.get(i).size() != morphList.get(i).size()) {
				return false;
			}
			for (int j = 0; j < morphs.get(i).size(); j++) {
				String morphName = morphs.get(i).get(j);
				String morphListName = morphList.get(i).get(j);
				if (morphName == null || morphListName == null) {
					if (morphName != morphListName) {
						return false;
					}
				} else if (!(morphName.equals(morphListName))) {
					return false;
				}
			}
		}
		return same;
	}
}

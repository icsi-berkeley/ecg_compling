package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.List;

import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;

public class AnnotatedAnalysis {
	
	List<Integer> observed;
	
	public String getAnnotatedText(String sentence, Analysis a) {
		StringBuilder builder = new StringBuilder();
		
		observed = new ArrayList<Integer>();
		
		builder.append("Sentence: " + sentence);
		//builder.append("\n " + a.toString());
		String annotated = annotate(a.getSpans(), sentence, a);
		builder.append("\n \n" + annotated);
		
		for (FeatureStructureSet.Slot root : a.getFeatureStructure().getRootSlots()) {
			formatSlots(root);
		}
		
		return builder.toString();
	}
	
	public String annotate(List<CxnalSpan> spans, String sentence, Analysis a) {
		FeatureStructureSet fss = a.getFeatureStructure();
		String annotated = "";
		String[] split = sentence.split(" ");
		for (int i=0; i<split.length; i++) {
			String spanText = "";
			for (CxnalSpan span : spans) {
				if (span.left == i && span.right == i+1) {
					//spanText += span.toString();
					spanText = span.getType().getName();
					Role r = span.getRole();
					
					if (r != null) {
						System.out.println(r.getName());
						System.out.println(r.getTypeConstraint());
						TypeConstraint tc = r.getTypeConstraint();
						if (tc != null) {
							System.out.println(tc.getType());
						}
					}
					//System.out.println(span.getSlotID());
					int id = span.getSlotID();
					Slot s = fss.getSlot(id);
					spanText += "\n " + formatSlots(s);
					//System.out.println(fss.getSlot(id));
					//System.out.println(span.)
					//for (Analysis a : span.getSibs()) {
					//	System.out.println(a);
					//}
				}
			}
			annotated += split[i] + ": " + spanText + " \n";
		}
//		for (CxnalSpan span : spans) {
//			System.out.println(span);
//			System.out.println(span.getType());
//		}
		return annotated;
	}
	
	public String formatSlots(FeatureStructureSet.Slot slot) {
		String formatted = "";
		for (Role role : slot.getFeatures().keySet()) {
			if (role.getName().equals("features"))
				continue;
			
			Slot s = slot.getFeatures().get(role);
			if (!observed.contains(s.getID())) {
				formatted += s.getTypeConstraint();
				if (s.hasFiller()) {
					if (s.hasStructuredFiller()) {
						formatted += "\n " + formatSlots(s);
					}
					else if (s.hasAtomicFiller() && s.getAtom() != null) {
						formatted += "\n   " + s.getAtom();
					}
				}
				observed.add(s.getID());
			}
		}
		
		
		return formatted;
	}

}

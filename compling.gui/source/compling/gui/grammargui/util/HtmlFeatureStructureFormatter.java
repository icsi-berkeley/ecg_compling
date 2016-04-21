package compling.gui.grammargui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.util.TypeSystemNodeType;

public class HtmlFeatureStructureFormatter {

  protected final TextEmitter emitter;
  
  
  //TODO:
  private ArrayList<Role> inherited = new ArrayList<Role>();

  public HtmlFeatureStructureFormatter() {
    this(new TextEmitter(1));
  }
  
  public HtmlFeatureStructureFormatter(TextEmitter emitter) {
    this.emitter = emitter;
  }

  public void format(FeatureStructureSet fss, HashMap<String, String> spansToText) {
    Set<FeatureStructureSet.Slot> alreadyDone = new HashSet<FeatureStructureSet.Slot>();
    /*
    for (FeatureStructureSet.Slot root : fss.getRootSlots()) {
        emitter.sayln(0, "<div class='root-wrapper'>");
        testIt(root, alreadyDone, new HashSet<Integer>(), 1);
        emitter.sayln(0, "</div>");
      } */
    
    for (FeatureStructureSet.Slot root : fss.getRootSlots()) {
      emitter.sayln(0, "<div class='root-wrapper'>");
      formatHelper(root, alreadyDone, new HashSet<Integer>(), 1, spansToText);
      emitter.sayln(0, "</div>");
    } 
    emitter.getOutput();
  }

  private final String slotType(Slot slot) {
    final TypeConstraint t = slot.getTypeConstraint();
    return t != null ? t.getType() : "<untyped>";
  }
  
  
  private void nonLocalTable(int level) {
	  emitter.sayln(level, "<tr><td class='collapsible-content'><div class='%s'>", "test");
  }
  
  private void testIt(FeatureStructureSet.Slot slot, Set<FeatureStructureSet.Slot> alreadyDone,
          Set<Integer> foundInd, int level) {
	  emitter.sayln(level++, "<table>");
	  emitter.sayln(level, "<tr><td class='type-name %s'>", "Test");
	  emitter.sayln(level + 1, "<img src='%s' height=16 width=16 style='float: left;'/>", 15);
	  emitter.sayln(level, "%s</td></tr>", "test2");
  }

  private void formatHelper(FeatureStructureSet.Slot slot, Set<FeatureStructureSet.Slot> alreadyDone,
          Set<Integer> foundInd, int level, HashMap<String, String> spansToText) {

    if (!slot.hasFiller() || alreadyDone.contains(slot))
      return;

    alreadyDone.add(slot);

    emitter.sayln(level++, "<table>");

    String type = "###UNK###";
    String typeName;
    TypeConstraint typeConstraint = slot.getTypeConstraint();
    if (typeConstraint != null) {
      type = typeConstraint.getType();
      typeName = typeConstraint.getTypeSystem().getName();
      
      //seantrott: testing, adding in span/text information to constructions..
      if (typeName.equals("CONSTRUCTION")) {
    	  String text = spansToText.get(type + "[" + slot.getID() + "]");
//    	  System.out.println(text);
    	  if (text != null) {
    		  type += "   ('" + text + "')";
    	  }
      }
      
      
      String key = Constants.nodeToKey(TypeSystemNodeType.fromString(typeName));
      emitter.sayln(level, "<tr><td class='type-name %s'>", typeName);
      emitter.sayln(level + 1, "<img src='%s' height=16 width=16 style='float: left;'/>", key);
      emitter.sayln(level, "%s</td></tr>", type);
    }
    emitter.sayln(level++, "<tr><td class='collapsible-content'><div class='%s'>", type);
    emitter.sayln(level++, "<table class='content'>");
    for (Role role : slot.getFeatures().keySet()) {
      if (role.getName().equals("features"))
        continue;
      
      if (!role.isLocal()) {
    	  inherited.add(role);
    	  continue;
      }
      
      String roleString = role.toString();
      
      if (role.getSource() != null && !role.getSource().equals(slot.toString())) {
    	  //roleString += " \n (inherited from " + role.getSource() + ") ";
      }
      
      emitter.sayln(level++, "<tr>");
      emitter.sayln(level, "<td>%s:</td>", roleString);

      FeatureStructureSet.Slot childSlot = slot.getSlot(role);
      emitter.say(level, "<td><span class='index' title='%s'>%d</span></td>", slotType(childSlot), childSlot.getSlotIndex());

      if (childSlot.hasFiller() && !childSlot.hasStructuredFiller()) {
        if (childSlot.hasAtomicFiller())
          emitter.sayln(level, "<td>%s</td>", childSlot.getAtom());
        else if (childSlot.isListSlot()) {
          emitter.say(level, "<td>");
          for (FeatureStructureSet.Slot s : childSlot.listValue)
            emitter.say("<span class='index' title='%s'>%d</span>&nbsp;", slotType(s), s.getSlotIndex());
          emitter.sayln(level, "</td>");
        }
      }
      else if (childSlot.hasFiller() && childSlot.hasStructuredFiller() && !alreadyDone.contains(childSlot)) {
        emitter.sayln(level, "<td><div class='collapsible-content'>");
        formatHelper(childSlot, alreadyDone, foundInd, level + 1, spansToText);
        emitter.sayln(level, "</div></td>");
      }
      else
        emitter.sayln(level, "<td></td>");
      emitter.sayln(--level, "</tr>");
    }
    emitter.sayln(--level, "</table>");
    emitter.sayln(--level, "</div></td></tr>");

    //fillInInherited(level);
    
    emitter.sayln(--level, "</table>");
  }

  private void fillInInherited(int level) {
	emitter.sayln(level++, "</table>");
	emitter.sayln(level++, "<tr><td class='collapsible-content'><div class='%s'>", "INHERITED");
	emitter.sayln(level, "<tr><td class='type-name %s'>", "INHERITED");
	emitter.sayln(level++, "<tr>");
	emitter.sayln(level++, "<table class='content'>");
	for (Role r : inherited) {
		emitter.sayln(level, "<td>%s:</td>", r);
	}
	
}

public void format(Slot slot) {
    throw new RuntimeException("shouldn't be calling this.");
  }

}

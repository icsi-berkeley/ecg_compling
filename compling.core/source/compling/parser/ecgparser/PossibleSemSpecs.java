package compling.parser.ecgparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.BasicAnalysisFactory;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.CloneTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentLocalityCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentsToSatisfyCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ConstituentsToSatisfyTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.DumbConstituentExpansionCostTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.UnifyTable;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotChainTables;
import compling.parser.ecgparser.LeftCornerParserTablesSem.SlotConnectionTracker;
import compling.parser.ecgparser.PossibleSemSpecs.PartialSemSpec.Slot;
import compling.util.IdentityHashSet;
import compling.util.Pair;
import compling.util.math.SloppyMath;

public class PossibleSemSpecs implements Cloneable {

  List<PartialSemSpec> semSpecList = new ArrayList<PartialSemSpec>();
  static int idcounter = 0;
  int id;
  Set<Integer> ancestorIDs = new HashSet<Integer>();

  List<PartialSemSpec> getSemSpecList() {
    return semSpecList;
  }

  public PossibleSemSpecs(List<SlotChain> slotChains, List<TypeConstraint> types,
          List<List<Pair<TypeConstraint, Role>>> frameRoles) {
    PartialSemSpec pss = new PartialSemSpec(slotChains, frameRoles);
    if (slotChains.size() != types.size()) {
      throw new ParserException("Number of slot chains does not equal number of types in PossibleSemSpecs constructor");
    }
    for (int i = 0; i < slotChains.size(); i++) {
      pss.setTypeConstraint(new LWSlotChain(slotChains.get(i)), types.get(i));
    }
    semSpecList.add(pss);
  }

  public void setID() {
    id = idcounter++;
  }

  public PossibleSemSpecs clone() {
    try {
      PossibleSemSpecs c = (PossibleSemSpecs) super.clone();
      c.semSpecList = new ArrayList<PartialSemSpec>();
      for (PartialSemSpec pss : semSpecList) {
        c.semSpecList.add(pss.clone());
      }
      c.ancestorIDs = new HashSet<Integer>(ancestorIDs);
      c.id = id;
      return c;
    }
    catch (Exception c) {
      throw new ParserException("Clone meltdown in PartialSemSpec.clone(): " + c.toString());
    }
  }

  public class PartialSemSpec implements Cloneable {
    Map<LWSlotChain, Slot> slots;
    IdentityHashMap<Role, Role> possibleParentRoleSet = new IdentityHashMap<Role, Role>();
    double externalProb = 0;

    PartialSemSpec(List<SlotChain> slotChains) {
      slots = new HashMap<LWSlotChain, Slot>();
      for (int i = 0; i < slotChains.size(); i++) {
        this.slots.put(new LWSlotChain(slotChains.get(i)), new Slot());
      }
    }

    PartialSemSpec(List<SlotChain> slotChains, List<List<Pair<TypeConstraint, Role>>> frameRoles) {
      slots = new HashMap<LWSlotChain, Slot>();
      for (int i = 0; i < slotChains.size(); i++) {
        LWSlotChain lwsc = new LWSlotChain(slotChains.get(i));
        this.slots.put(lwsc, new Slot(frameRoles.get(i)));
      }
    }

    public class Slot implements Cloneable {
      TypeConstraint fillerTypeConstraint;
      Set<Pair<TypeConstraint, Role>> frameRoles;

      Slot() {
      }

      Slot(List<Pair<TypeConstraint, Role>> frameRoles) {
        setFrameRoles(frameRoles);
      }

      void setFrameRoles(List<Pair<TypeConstraint, Role>> frameRoles) {
        if (frameRoles != null) {
          if (this.frameRoles == null) {
            this.frameRoles = new IdentityHashSet<Pair<TypeConstraint, Role>>();
          }
          this.frameRoles.addAll(frameRoles);
        }
      }

      double computeLogLikelihood() {
        return 0;
      }

      boolean incorporateInfo(Slot ancestorSlot) {
        TypeConstraint ancestorTypeConstraint = ancestorSlot.fillerTypeConstraint;
        // System.out.println("this.fillerTypeConstraint: "+fillerTypeConstraint+"  ancestorSlot.typeconstraint: "+ancestorSlot.fillerTypeConstraint);
        if (ancestorTypeConstraint != null) {
          if (fillerTypeConstraint == null) {
            fillerTypeConstraint = ancestorTypeConstraint;
          }
          else if (fillerTypeConstraint.typeSystem != ancestorTypeConstraint.typeSystem) {
            // System.out.println("Here?");
            return false;
          }
          else {
            try {
              if (fillerTypeConstraint.getTypeSystem().subtype(ancestorTypeConstraint.getType(),
                      fillerTypeConstraint.getType())) {
                // System.out.println("\tthis.fillerTypeConstraint: "+fillerTypeConstraint+"  super of ancestorSlot.typeconstraint: "+ancestorSlot.fillerTypeConstraint);
                fillerTypeConstraint = ancestorTypeConstraint;
              }
              else if (fillerTypeConstraint.getTypeSystem().subtype(fillerTypeConstraint.getType(),
                      ancestorTypeConstraint.getType())) {
                // do nothing
                // System.out.println("\tthis.fillerTypeConstraint: "+fillerTypeConstraint+"  is a sub of ancestorSlot.typeconstraint: "+ancestorSlot.fillerTypeConstraint);
              }
              else {
                // System.out.println("\tthis.fillerTypeConstraint: "+fillerTypeConstraint+" unrel ancestorSlot.typeconstraint: "+ancestorSlot.fillerTypeConstraint);
                return false; // this case is where they are not
                // compatible (neither one is a subtype
                // of the
                // other.)
              }
            }
            catch (TypeSystemException t) {
              throw new ParserException(
                      "Catastrophic type failure in PossibleSemSpecs.PartialSemSpec.Slot.incorporateInfo");
            }

          }
        }
        if (frameRoles == null && ancestorSlot.frameRoles != null) {
          frameRoles = ancestorSlot.frameRoles;
        }
        else if (frameRoles != null && ancestorSlot.frameRoles != null) {
          frameRoles.addAll(ancestorSlot.frameRoles);
        }
        return true;
      }

      public boolean equals(Object o) {
        if (!(o instanceof Slot)) {
          return false;
        }
        Slot that = (Slot) o;
        return fillerTypeConstraint == that.fillerTypeConstraint;
      }

      public Slot clone() {
        try {
          Slot c = (Slot) super.clone();
          if (frameRoles != null) {
            c.frameRoles = new IdentityHashSet<Pair<TypeConstraint, Role>>(frameRoles);
          }
          return c;
        }
        catch (Exception c) {
          throw new ParserException("Clone meltdown in PartialSemSpec.Slot.clone()");
        }
      }
    }

    public double computeLogLikelihood() {
      return 0;
    }

    public void setTypeConstraint(LWSlotChain ch, TypeConstraint tc) {
      if (slots.get(ch) != null) {
        if (tc == null && slots.get(ch).fillerTypeConstraint != null) {
          throw new RuntimeException("wha? " + slots.get(ch).fillerTypeConstraint.toString() + " " + ch);
        }
        slots.get(ch).fillerTypeConstraint = tc;
      }
      else {
        Slot slot = new Slot();
        slot.fillerTypeConstraint = tc;
        slots.put(ch, slot);
      }
    }

    public boolean equals(Object o) {
      if (!(o instanceof PartialSemSpec)) {
        return false;
      }
      Map<LWSlotChain, Slot> thatSlots = ((PartialSemSpec) o).slots;
      if (slots.size() != thatSlots.size()) {
        return false;
      }
      for (LWSlotChain sc : slots.keySet()) {
        Slot thatSlot = thatSlots.get(sc);
        if (thatSlot == null) {
          return false;
        }
        if (!slots.get(sc).equals(thatSlot)) {
          return false;
        }
      }
      return true;
    }

    public PartialSemSpec clone() {
      try {
        PartialSemSpec pss = (PartialSemSpec) super.clone();
        pss.slots = new HashMap<LWSlotChain, Slot>();
        for (LWSlotChain sc : slots.keySet()) {
          pss.slots.put(sc, slots.get(sc).clone());
        }
        pss.possibleParentRoleSet = new IdentityHashMap<Role, Role>();
        addAll(pss.possibleParentRoleSet, possibleParentRoleSet);
        return pss;
      }
      catch (CloneNotSupportedException c) {
        throw new ParserException("Clone meltdown in PartialSemSpec.clone()");
      }
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("\t\t{\n\t\t\tAttRoles: ");
      for (Role c : possibleParentRoleSet.keySet()) {
        sb.append(c.getName()).append(", ");
      }
      sb.append("\n\n");
      for (LWSlotChain sc : slots.keySet()) {
        sb.append("\t\t\t").append(sc).append(": ").append(slots.get(sc).fillerTypeConstraint).append("\n");
      }
      sb.append("\t\t}\n");
      return sb.toString();
    }

    public String buggyBindingsView() {
      StringBuilder sb = new StringBuilder();
      for (LWSlotChain sc : slots.keySet()) {
        if (slots.get(sc).fillerTypeConstraint != null && slots.get(sc).frameRoles != null
                && slots.get(sc).frameRoles.size() > 0
                && !slots.get(sc).fillerTypeConstraint.getType().equals("complexxnet")) {
          HashMap<Role, List<TypeConstraint>> simplifier = new HashMap<Role, List<TypeConstraint>>();

          for (Pair<TypeConstraint, Role> pair : slots.get(sc).frameRoles) {
            if (pair.getFirst().getType().equals("RD") || pair.getFirst().getType().equals("EventDescriptor")) {
              continue;
            }
            if (simplifier.get(pair.getSecond()) != null) {
              List<TypeConstraint> alreadyFoundTypes = simplifier.get(pair.getSecond());
              TypeConstraint tc = pair.getFirst();
              boolean alreadyFound = false;
              TypeConstraint toRemove = null;
              for (TypeConstraint alreadyFoundType : alreadyFoundTypes) {
                try {
                  if (tc.getTypeSystem().subtype(tc.getType(), alreadyFoundType.getType())) {
                    alreadyFound = true;
                    toRemove = alreadyFoundType;
                  }
                  else if (tc.getTypeSystem().subtype(alreadyFoundType.getType(), tc.getType())) {
                    alreadyFound = true;
                    break;
                  }
                }
                catch (TypeSystemException tse) {
                }
              }
              if (!alreadyFound) {
                alreadyFoundTypes.add(tc);
              }
              if (toRemove != null) {
                alreadyFoundTypes.remove(toRemove);
                alreadyFoundTypes.add(tc);
              }
            }
            else {
              List<TypeConstraint> ll = new LinkedList<TypeConstraint>();
              ll.add(pair.getFirst());
              simplifier.put(pair.getSecond(), ll);
            }
          }
          if (simplifier.keySet().size() > 0) {
            sb.append("\t   ");
            for (Role role : simplifier.keySet()) {
              for (TypeConstraint tc : simplifier.get(role)) {
                sb.append(tc.getType()).append(".").append(role.getName()).append(" <--> ");
              }
            }
            sb.delete(sb.length() - 5, sb.length());
            sb.append(": ");
            sb.append(slots.get(sc).fillerTypeConstraint.getType());
            sb.append("\n");
          }

        }
      }
      return sb.toString();
    }

    public String oneLine() {
      StringBuilder sb = new StringBuilder();// ("\t\t\t\t ATTROLES: ");
      // for (Role c : possibleParentRoleSet.keySet()){
      // sb.append(c.getName()).append(", ");
      // }
      // sb.append(";   ");
      sb.append("\t\t\t{  ");
      int i = 0;
      for (LWSlotChain sc : slots.keySet()) {
        // if (i > 0 && i % 3 == 0){sb.append("\n\t\t\t\t\t   "); }
        sb.append(sc);
        if (slots.get(sc).frameRoles != null && slots.get(sc).frameRoles.size() > 0) {
          sb.append(" (");
          for (Pair<TypeConstraint, Role> pair : slots.get(sc).frameRoles) {
            sb.append(pair.getFirst().getType()).append(".").append(pair.getSecond().getName()).append(" ");
          }
          sb.append(" ) ");
        }
        sb.append(": ");
        if (slots.get(sc).fillerTypeConstraint == null) {
          sb.append("none");
        }
        else {
          sb.append(slots.get(sc).fillerTypeConstraint.getType());
        }
        sb.append(";\n\t\t\t   ");
        i++;
      }
      sb.append("}");
      return sb.toString();
    }

  }

  public boolean incorporateChildPossibleSemSpecs(PossibleSemSpecs child, BindingArrangement ba, boolean gappingScenario) {
    Role parentRole = ba.getRole();
    List<PartialSemSpec> newPSSList = new ArrayList<PartialSemSpec>();
    for (PartialSemSpec childss : child.semSpecList) {
      if (childss.possibleParentRoleSet.get(parentRole) != null || childss.possibleParentRoleSet.size() == 0
              || gappingScenario) {
        Map<LWSlotChain, PartialSemSpec.Slot> childSlots = childss.slots;
        for (PartialSemSpec parentss : semSpecList) {
          PartialSemSpec parentClone = parentss.clone();
          Set<LWSlotChain> remainingChildSlots = new HashSet<LWSlotChain>(childSlots.keySet());
          boolean badUni = false;
          // System.out.println(remainingChildSlots);
          for (BindingArrangement.Binding binding : ba.bindings) {
            LWSlotChain ancChain = binding.ancChain;
            LWSlotChain descChain = binding.descChain;

            // FIXME: emok's hack since this is making null pointers
            // sometimes -- ancChain can't be found among
            // the parentClone's slots
            try {
              if (!parentClone.slots.get(ancChain).incorporateInfo(childSlots.get(descChain))) {
                badUni = true;
                break;
              }
            }
            catch (NullPointerException npe) {
              badUni = true;
              break;
            }
             //System.out.println("in incorp child "+ancChain.toString()+"  "+descChain.toString()+" pid:"+id+" cid:"+child.id);
            remainingChildSlots.remove(descChain);
          }
          // System.out.println(remainingChildSlots);
          if (!badUni) {
            if (!gappingScenario) {
              for (LWSlotChain r : remainingChildSlots) {
                PartialSemSpec.Slot childSlot = childSlots.get(r);

                if (r.local() && (childSlot.fillerTypeConstraint != null || childSlot.frameRoles != null)) {
                  LWSlotChain lsc = new LWSlotChain(child.id, r.chain);
                  parentClone.setTypeConstraint(lsc, childSlot.fillerTypeConstraint);
                  parentClone.slots.get(lsc).frameRoles = childSlot.frameRoles;
                }
                else if (!r.local() && !ancestorIDs.contains(r.source) && r.source != id) {
                  LWSlotChain lsc = new LWSlotChain(r.source, r.chain);
                  parentClone.setTypeConstraint(lsc, childSlot.fillerTypeConstraint);
                  parentClone.slots.get(lsc).frameRoles = childSlot.frameRoles;
                }
              }
            }

            boolean foundMatch = false;
            for (int i = 0; i < newPSSList.size(); i++) {
              if (newPSSList.get(i).equals(parentClone)) {
                newPSSList.get(i).externalProb = SloppyMath.logAdd(newPSSList.get(i).externalProb,
                        parentClone.externalProb);
                addAll(newPSSList.get(i).possibleParentRoleSet, parentClone.possibleParentRoleSet);
                foundMatch = true;
                break;
              }
            }
            if (!foundMatch) {
              newPSSList.add(parentClone);
            }
          }
        }
      }
    }
    semSpecList = newPSSList;
    if (semSpecList.size() == 0) {
      return false;
    }
    return true;
  }

  public boolean incorporateAncestorPossibleSemSpecs(PossibleSemSpecs ancestor, List<BindingArrangement> bas,
          List<Double> penalties) {
    List<PartialSemSpec> newSemSpecList = new ArrayList<PartialSemSpec>();
    ancestorIDs.addAll(ancestor.ancestorIDs);
    ancestorIDs.add(ancestor.id);
    int isubpenalty = 0;
    for (BindingArrangement ba : bas) {
      double penalty = penalties.get(isubpenalty);
      isubpenalty++;
      for (int pc = 0; pc < ancestor.semSpecList.size(); pc++) {
        Map<LWSlotChain, PartialSemSpec.Slot> ancestorSlots = ancestor.semSpecList.get(pc).slots;
        for (int cc = 0; cc < this.semSpecList.size(); cc++) {
          PartialSemSpec newChild = this.semSpecList.get(cc).clone();
          newChild.externalProb = ancestor.semSpecList.get(pc).externalProb + penalty + ba.logLikelihood;
          boolean badUni = false;
          Set<LWSlotChain> remainingAncestorSlots = new HashSet<LWSlotChain>(ancestorSlots.keySet());
          for (BindingArrangement.Binding binding : ba.bindings) {
            LWSlotChain ancChain = binding.ancChain;
            LWSlotChain descChain = binding.descChain;
            if (ancestorSlots.get(ancChain) == null) {
              System.out.println("ancChain: " + ancChain.toString() + "\n" + ba.toString() + "\n"
                      + ancestor.semSpecList.get(pc).toString());
            }
           try {
              if (!newChild.slots.get(descChain).incorporateInfo(ancestorSlots.get(ancChain))) {
                badUni = true;
                break;
              }
           }
            catch (NullPointerException e) {
            	//e.printStackTrace();
              System.out.printf("descChain: %s, ba: %s, slots: %s\n", descChain, ba, newChild.slots);
//              badUni = true;
//              break;
            }
            remainingAncestorSlots.remove(ancChain);
          }
          if (! badUni) {
            for (LWSlotChain r : remainingAncestorSlots) {
              int source = r.source;
              if (r.local()) {
                source = ancestor.id;
              }
              LWSlotChain lsc = new LWSlotChain(source, r.chain);
              newChild.setTypeConstraint(lsc, ancestorSlots.get(r).fillerTypeConstraint);
              newChild.slots.get(lsc).frameRoles = ancestorSlots.get(r).frameRoles;
            }
            boolean foundMatch = false;
            for (int i = 0; i < newSemSpecList.size(); i++) {
              if (newSemSpecList.get(i).equals(newChild)) {
                newSemSpecList.get(i).externalProb = SloppyMath.logAdd(newSemSpecList.get(i).externalProb,
                        newChild.externalProb);
                addAll(newSemSpecList.get(i).possibleParentRoleSet, ba.getDescendentParentRoles());
                foundMatch = true;
                break;
              }
            }
            if (!foundMatch) {
              addAll(newChild.possibleParentRoleSet, ba.getDescendentParentRoles());
              newSemSpecList.add(newChild);
            }
          }

        }
      }
    }
    semSpecList = newSemSpecList;
    if (semSpecList.size() == 0) {
      return false;
    }
    return true;
  }

  public static class LWSlotChain {
    int source = -1;
    String chain = null;
    int hashcode = -1;

    public LWSlotChain(SlotChain sc) {
      chain = sc.toString();
      hashcode = chain.hashCode() - 1;
    }

    public LWSlotChain(int s, String chain) {
      source = s;
      this.chain = chain;
      hashcode = source + this.chain.hashCode();
    }

    public int hashCode() {
      return hashcode;
    }

    public boolean equals(Object o) {
      if (o instanceof LWSlotChain) {
        LWSlotChain that = (LWSlotChain) o;
        return this.source == that.source && this.chain == that.chain;
      }
      return false;
    }

    boolean local() {
      return source == -1;
    }

    public String toString() {
      if (local()) {
        return chain;
      }
      else {
        return new StringBuilder().append(source).append(".").append(chain).toString();
      }
    }
  }

  public static class BindingArrangement {

    public static class Binding {
      public LWSlotChain ancChain;
      public LWSlotChain descChain;

      public Binding(SlotChain ancChain, SlotChain descChain) {
        this.ancChain = new LWSlotChain(ancChain);
        this.descChain = new LWSlotChain(descChain);
      }

      public Binding(LWSlotChain ancChain, LWSlotChain descChain) {
        this.ancChain = ancChain;
        this.descChain = descChain;
      }

      public boolean equals(Object o) {
        if (o instanceof Binding) {
          Binding that = (Binding) o;
          return this.ancChain.equals(that.ancChain) && this.descChain.equals(that.descChain);
        }
        return false;
      }

      public int hashCode() {
        return ancChain.hashCode() + descChain.hashCode();
      }
    }

    static int arrangementIDCounter = 0;

    int id;
    double logLikelihood = Double.NEGATIVE_INFINITY;
    Construction ancestor;
    Role role;
    Construction descendent;
    Set<Binding> bindings;
    int bindingsSize = 0;
    IdentityHashMap<Role, Role> possibleDescendentParentRoles = new IdentityHashMap<Role, Role>();

    public BindingArrangement(double weight, Construction ancestor, Role role, Construction descendent,
            Set<Binding> bindings) {
      if (weight != 0) {
        throw new RuntimeException("The weight argument is broken and should never be used with anything but 0");
      }
      this.ancestor = ancestor;
      this.role = role;
      this.descendent = descendent;
      // incLogLikelihood(weight);
      this.bindings = bindings;
      id = arrangementIDCounter++;
      bindingsSize = bindings.size();
    }

    public BindingArrangement(double weight, Construction ancestor, Role role, Construction descendent,
            Set<Binding> bindings, Role descendentParentRole) {
      this(weight, ancestor, role, descendent, bindings);
      possibleDescendentParentRoles.put(descendentParentRole, descendentParentRole);
    }

    /*
     * broken public void incLogLikelihood(double weight){ logLikelihood =
     * logLikelihood+ weight; }
     */
    public void logAddLikelihood(double weight) {
      logLikelihood = SloppyMath.logAdd(logLikelihood, weight);
    }

    public double getLogLikelihood() {
      return logLikelihood;
    }

    public Construction getDescendent() {
      return descendent;
    }

    public Construction getAncestor() {
      return ancestor;
    }

    public Set<Binding> getBindings() {
      return bindings;
    }

    public Role getRole() {
      return role;
    }

    public IdentityHashMap<Role, Role> getDescendentParentRoles() {
      return possibleDescendentParentRoles;
    }

    public void addAllDescendentParentRoles(IdentityHashMap<Role, Role> p) {
      addAll(possibleDescendentParentRoles, p);
    }

    public void addDescendentParentRole(Role c) {
      possibleDescendentParentRoles.put(c, c);
    }

    public boolean equals(Object o) {
      if (o instanceof BindingArrangement) {
        BindingArrangement that = (BindingArrangement) o;
        return this.ancestor == that.ancestor && this.role == that.role && this.descendent == that.descendent
                && this.bindings.equals(that.bindings);
      }
      return false;
    }

    public BindingArrangement makeConnectedArrangement(BindingArrangement that) {
      Set<Binding> newBindings = new HashSet<Binding>();
      for (Binding ancBinding : this.bindings) {
        for (Binding descBinding : that.bindings) {
          if (ancBinding.descChain.equals(descBinding.ancChain)) {
            newBindings.add(new Binding(ancBinding.ancChain, descBinding.descChain));
          }
        }
      }
      BindingArrangement ba = new BindingArrangement(0, this.ancestor, this.role, that.descendent, newBindings);
      ba.addAllDescendentParentRoles(that.getDescendentParentRoles());
      return ba;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(ancestor.getName()).append(".").append(role.getName()).append(" --> ").append(descendent.getName())
              .append("\n\t\t");
      sb.append("Possible descendent parents: ");
      for (Role c : possibleDescendentParentRoles.keySet()) {
        sb.append(c.getName()).append(",");
      }
      sb.append("\n\t\t");
      for (Binding binding : bindings) {
        sb.append(binding.ancChain).append(" <--> ").append(binding.descChain).append(" ;  ");
      }
      if (bindings.size() == 0) {
        sb.append("--> No common bindings;");
      }
      return sb.toString();
    }

  }

  public String toString() {
    StringBuilder sb = new StringBuilder("\tPossibleSemSpecs:\n");
    for (PartialSemSpec pss : semSpecList) {
      sb.append(pss.toString()).append("\n");
    }
    sb.append("\n");
    return sb.toString();
  }

  public String oneLine() {
    StringBuilder sb = new StringBuilder();
    // sb.append("\t\t\tid = ").append(id).append(" ancestors are ").append(ancestorIDs.toString()).append("\n");
    for (PartialSemSpec pss : semSpecList) {
      sb.append(pss.oneLine()).append("\n");
    }
    return sb.toString();
  }

  static PossibleSemSpecs makePSS(LCPGrammarWrapper g, String cxnName, CloneTable ct, SlotChainTables sct) {
    return makePSS(g.getConstruction(cxnName), ct, sct);
  }

  static PossibleSemSpecs makePSS(Construction cxn, CloneTable ct, SlotChainTables sct) {
    List<SlotChain> chains = sct.getCanonicalChains(cxn);
    List<TypeConstraint> types = new ArrayList<TypeConstraint>();
    List<List<Pair<TypeConstraint, Role>>> frameRoles = new ArrayList<List<Pair<TypeConstraint, Role>>>();
    Analysis a = ct.get(cxn);
    for (SlotChain chain : chains) {
      types.add(a.getFeatureStructure().getSlot(chain).getTypeConstraint());
      frameRoles.add(sct.getFrameRoles(cxn, chain));
    }
    return new PossibleSemSpecs(chains, types, frameRoles);
  }

  static Role roleGetter(LCPGrammarWrapper g, String cxnName, String roleName) {
    for (Role role : g.getConstruction(cxnName).getAllRoles()) {
      if (role.getName().equals(roleName)) {
        return role;
      }
    }
    return null;
  }

  static void addAll(IdentityHashMap<Role, Role> addedToRoleMap, IdentityHashMap<Role, Role> addedFromHashMap) {
    for (Role newRole : addedFromHashMap.keySet()) {
      if (!addedToRoleMap.containsKey(newRole)) {
        addedToRoleMap.put(newRole, newRole);
      }
    }
  }

  public static void main(String[] args) {
    String ontFile = null;
    if (args.length > 1) {
      ontFile = args[1];
    }
    Grammar ecgGrammar = ECGGrammarUtilities.read(args[0], "ecg cxn sch grm", ontFile);
    LCPGrammarWrapper grammar = new LCPGrammarWrapper(ecgGrammar);
    System.out.println("Begin table building");
    // System.out.println(grammar);
    CloneTable ct = new CloneTable(grammar, new BasicAnalysisFactory());
    SlotChainTables sct = new SlotChainTables(grammar, ct);
    System.out.println(sct);
    UnifyTable ut = new UnifyTable(grammar, ct);
    DumbConstituentExpansionCostTable dcept = new DumbConstituentExpansionCostTable(grammar);
    ConstituentsToSatisfyTable ctst = new ConstituentsToSatisfyTable(grammar);
    ConstituentLocalityCostTable clct = new ConstituentLocalityCostTable(grammar);
    ConstituentsToSatisfyCostTable ctsct = new ConstituentsToSatisfyCostTable(grammar, ctst, clct);
    SlotConnectionTracker st = new SlotConnectionTracker(grammar, ct, ut, sct, ctsct, dcept, clct);
    System.out.println(st);

    PossibleSemSpecs Evepss = makePSS(grammar, "Eve", ct, sct);
    PossibleSemSpecs NPVPpss = makePSS(grammar, "NPVP", ct, sct);
    PossibleSemSpecs SpatialPPpss = makePSS(grammar, "SpatialPP", ct, sct);
    PossibleSemSpecs WalkedVerbpss = makePSS(grammar, "WalkedVerb", ct, sct);
    PossibleSemSpecs Intopss = makePSS(grammar, "Into", ct, sct);
    PossibleSemSpecs ActiveControlMotionPath2pss = makePSS(grammar, "ActiveControlMotionPath2", ct, sct);
    PossibleSemSpecs ActiveSelfMotionPathpss = makePSS(grammar, "ActiveSelfMotionPath", ct, sct);

    System.out.println("Eve:\n" + Evepss);
    System.out.println("NPVP:\n" + NPVPpss);
    System.out.println("SpatialPP:\n" + SpatialPPpss);
    System.out.println("WalkedVerb:\n" + WalkedVerbpss);
    System.out.println("Into:\n" + Intopss);

    /*
     * PossibleSemSpecs Into2pss = Intopss.clone();
     * Into2pss.incorporateAncestorPossibleSemSpecs(NPVPpss,
     * st.getBindingArrangement(roleGetter(grammar, "NPVP", "bigvp"),
     * grammar.getConstruction("Into")));
     * System.out.println("Into after clone with NPVP ancestor\n"+Into2pss);
     * 
     * 
     * System.out.println("Eve with no ancestor:\n"+Evepss);
     * 
     * System.out.println("NPVP with no ancestor:\n"+NPVPpss);
     * 
     * NPVPpss.incorporateChildPossibleSemSpecs(Evepss,
     * st.getDirectConnectBindingArrangement(roleGetter(grammar, "NPVP",
     * "subj"), grammar.getConstruction("Eve")), false);
     * System.out.println("NPVP incorporating Eve\n"+NPVPpss);
     * 
     * 
     * WalkedVerbpss.incorporateAncestorPossibleSemSpecs(NPVPpss,
     * st.getBindingArrangement(roleGetter(grammar, "NPVP", "bigvp"),
     * grammar.getConstruction("WalkedVerb")));
     * System.out.println("WalkedVerb with NPVP ancestor\n"+WalkedVerbpss);
     * 
     * ActiveSelfMotionPathpss.incorporateAncestorPossibleSemSpecs(NPVPpss,
     * st.getBindingArrangement(roleGetter(grammar, "NPVP", "bigvp"),
     * grammar.getConstruction("ActiveSelfMotionPath")));
     * System.out.println("ActiveSelfMotionPath with NPVP ancestor\n"
     * +ActiveSelfMotionPathpss);
     * 
     * ActiveSelfMotionPathpss.incorporateChildPossibleSemSpecs(WalkedVerbpss,
     * st.getDirectConnectBindingArrangement(roleGetter(grammar,
     * "ActiveSelfMotionPath", "v"), grammar.getConstruction("WalkedVerb")),
     * false);
     * System.out.println("ActiveSelfMotionPath incorporating WalkedVerb\n"
     * +ActiveSelfMotionPathpss);
     * 
     * Intopss.incorporateAncestorPossibleSemSpecs(ActiveSelfMotionPathpss,
     * st.getBindingArrangement(roleGetter(grammar, "ActiveSelfMotionPath",
     * "pp"), grammar.getConstruction("Into")));
     * System.out.println("Into with ActiveSelfMotionPath ancestor\n" +Intopss);
     * 
     * SpatialPPpss.incorporateAncestorPossibleSemSpecs(ActiveSelfMotionPathpss
     * , st.getBindingArrangement(roleGetter(grammar, "ActiveSelfMotionPath",
     * "pp"), grammar.getConstruction("SpatialPP")));
     * System.out.println("SpatialPP with ActiveSelfMotionPath ancestor\n"
     * +SpatialPPpss);
     * 
     * SpatialPPpss.incorporateChildPossibleSemSpecs(Intopss,
     * st.getDirectConnectBindingArrangement(roleGetter(grammar, "SpatialPP",
     * "prep"), grammar.getConstruction("Into")), false);
     * System.out.println("SpatialPP incorporating IntoChild\n"+SpatialPPpss);
     * 
     * ActiveSelfMotionPathpss.incorporateChildPossibleSemSpecs(SpatialPPpss,
     * st.getDirectConnectBindingArrangement(roleGetter(grammar,
     * "ActiveSelfMotionPath", "pp"), grammar.getConstruction("SpatialPP")),
     * false);
     * System.out.println("ActiveSelfMotionPath incorporating SpatialPP\n"
     * +ActiveSelfMotionPathpss);
     * 
     * NPVPpss.incorporateChildPossibleSemSpecs(ActiveSelfMotionPathpss,
     * st.getDirectConnectBindingArrangement(roleGetter(grammar, "NPVP",
     * "bigvp"), grammar.getConstruction("ActiveSelfMotionPath")), false);
     * System
     * .out.println("NPVP after incorporating ActiveSelfMotionpath:\n"+NPVPpss
     * );
     */
  }
}

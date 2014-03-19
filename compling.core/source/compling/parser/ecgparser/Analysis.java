package compling.parser.ecgparser;

/**
 * This class represents a complete ECG analysis and an in progress analysis that spans some subset of the input. It
 * uses a FeatureStructureSet to track the semantic bindings and constructional constraints.
 * 
 * @author John Bryant
 * 
 **/

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ParserException;
import compling.parser.ecgparser.AnalysisUtilities.AnalysisFormatter;
import compling.parser.ecgparser.AnalysisUtilities.DefaultAnalysisFormatter;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement;
import compling.parser.ecgparser.SemSpecScorer.BasicScorer;
import compling.util.IdentityHashSet;
import compling.util.Pair;
import compling.util.math.SloppyMath;

public class Analysis implements Cloneable {

  protected static BasicScorer semSpecScorer = null;
  protected static AnalysisFormatter formatter = new DefaultAnalysisFormatter();
  protected FeatureStructureSet featureStructure;
  protected Construction headCxn;
  protected String lexicalRHS = null; // for when it is a single form rhs
  protected static final String FOUND = "***###FOUND###***";
  private static final String CXNAL_ORTHOGRAPHY = "***###ORTH###***";
  private int leftIndex;
  private int rightIndex;
  protected IdentityHashSet<Role> leftOverConstituents;
  protected IdentityHashSet<Role> leftOverComplements;
  protected IdentityHashSet<Role> leftOverOptionals;
  // private double innerCost = 0;
  private PossibleSemSpecs myMiniSemSpecs;
  protected List<CxnalSpan> spans;
  protected List<CxnalSpan> localRoleSpans;

  private PossibleSemSpecs gapFillerPSS;
  private Construction gapFillerType;
  private boolean alreadyUsedGapFiller = false;
  private SlotChain gapFillerChain = null;
  private List<CxnalSpan> gappedSpans = new LinkedList<CxnalSpan>();
  private boolean foundGap = false;

  protected static Logger logger = Logger.getLogger(Analysis.class.getName());

  public static void setFormatter(AnalysisFormatter f) {
    formatter = f;
  }

  public static void setSemSpecScorer(BasicScorer sss) {
    semSpecScorer = sss;
  }

  public static BasicScorer getSemSpecScorer() {
    return semSpecScorer;
  }

  public Analysis(Construction headCxn) {
    this(headCxn, null);
  }

  public Analysis(Construction headCxn, GrammarWrapper grammar) {
    this.headCxn = headCxn;
    featureStructure = new FeatureStructureSet(headCxn.getCxnTypeSystem().getCanonicalTypeConstraint(headCxn.getName()));
    featureStructure.getMainRoot().setRealFiller(true);
    if (!addLocalStructure(grammar)) {
      throw new ParserException("Error instantiating construction " + headCxn.getName());
    }
    // setStartIndex(startIndex);
  }

  protected boolean addLocalStructure(GrammarWrapper grammar) {
    // if (!headCxn.isConcrete() ||
    // headCxn.getConstructionalBlock().getElements().size() > 0){
    // if (grammar != null && grammar.isPhrasalConstruction(headCxn) ){
    if (grammar != null && headCxn.getConstructionalBlock().getElements().size() > 0) {
      leftOverComplements = new IdentityHashSet<Role>(headCxn.getComplements());
      leftOverOptionals = new IdentityHashSet<Role>(headCxn.getOptionals());
      leftOverConstituents = new IdentityHashSet<Role>();
      leftOverConstituents.addAll(leftOverComplements);
      leftOverConstituents.addAll(leftOverOptionals);
      spans = new ArrayList<CxnalSpan>();
      localRoleSpans = new ArrayList<CxnalSpan>();
      // } else if (headCxn.isConcrete() &&
      // headCxn.getConstructionalBlock().getElements().size() == 0){
    }
    else if (grammar != null && grammar.isLexicalConstruction(headCxn)) {
      lexicalRHS = ECGGrammarUtilities.getLexemeFromLexicalConstruction(headCxn);
    }
    else {
      // not sure what to do in this case.
      lexicalRHS = null;
      leftOverConstituents = null;
      // throw new RuntimeException("hah");
    }

    // .m
    Role m = new Role("m");
    if (headCxn.getMeaningBlock().getType() != ECGConstants.UNTYPED) {
      // System.out.println(headCxn.getMeaningBlock().getType());
      m.setTypeConstraint(headCxn.getMeaningBlock().getBlockTypeTypeSystem()
              .getCanonicalTypeConstraint(headCxn.getMeaningBlock().getType()));
    }

    if (featureStructure.addSlot(new ECGSlotChain(m)) == false) {
      return false;
    }
    featureStructure.getSlot(new ECGSlotChain(m)).setRealFiller(true);

    // constructional block constituents
    for (Role r : headCxn.getConstructionalBlock().getElements()) {
      featureStructure.addSlot(new ECGSlotChain(r));
    }

    // meaning block evoked elements
    for (Role r : headCxn.getMeaningBlock().getEvokedElements()) {
      ECGSlotChain rc = new ECGSlotChain(r);
      featureStructure.addSlot(rc); // these have to have type constraints
      featureStructure.getSlot(rc).setRealFiller(true);
      if (r.getTypeConstraint().getTypeSystem() == headCxn.getSchemaTypeSystem()) {
        for (Constraint constraint : headCxn.getSchemaTypeSystem().get(r.getTypeConstraint().getType()).getContents()
                .getConstraints()) {
          if (!constraint.overridden()) {
            if (addConstraint(constraint, r, "") == false) {
              logger.warning("problem with " + constraint);
              return false;
            }
          }
        }
      }
    }

    // meaning block constraints
    for (Constraint constraint : headCxn.getMeaningBlock().getConstraints()) {
      if (!constraint.overridden()) {
        if (addConstraint(constraint, "") == false) {
          logger.warning("problem with " + constraint);
          return false;
        }
      }
    }

    // constructional block constraints
    for (Constraint constraint : headCxn.getConstructionalBlock().getConstraints()) {
      if (!constraint.overridden()) {
        if (addConstraint(constraint, "") == false) {
          logger.warning("problem with " + constraint);
          return false;
        }
      }
    }

    // meaning schema roles & constraints
    if (headCxn.getMeaningBlock().getType() != ECGConstants.UNTYPED
            && headCxn.getMeaningBlock().getBlockTypeTypeSystem() == headCxn.getSchemaTypeSystem()) {

      // meaning schema evoked elements
      for (Role r : headCxn.getSchemaTypeSystem().get(headCxn.getMeaningBlock().getType()).getContents()
              .getEvokedElements()) {
        featureStructure.addSlot(new ECGSlotChain("" + ".m", r)); // these have
                                                                  // to have
                                                                  // type
                                                                  // constraints

        if (r.getTypeConstraint().getTypeSystem() == headCxn.getSchemaTypeSystem()) {
          for (Constraint constraint : headCxn.getSchemaTypeSystem().get(r.getTypeConstraint().getType()).getContents()
                  .getConstraints()) {
            if (!constraint.overridden()) {
              if (addConstraint(constraint, r, ".m") == false) {
                logger.warning("problem with " + constraint);
                return false;
              }
            }
          }
        }

      }

      // meaning schema roles
      for (Role r : headCxn.getSchemaTypeSystem().get(headCxn.getMeaningBlock().getType()).getContents().getElements()) {
        // System.out.println("ROLEROLEROLE ROLE: "+r.getName()+" : "+r.getTypeConstraint().getType()+"@"+r.getTypeConstraint().getTypeSystem().getName());
        featureStructure.addSlot(new ECGSlotChain("" + ".m", r));
      }

      // meaning schema constraints
      for (Constraint constraint : headCxn.getSchemaTypeSystem().get(headCxn.getMeaningBlock().getType()).getContents()
              .getConstraints()) {
        // addConstraint(constraint, m, "");
        if (!constraint.overridden()) {
          if (addConstraint(constraint, m, "") == false) {
            logger.warning("problem with " + constraint);
            return false;
          }
        }
      }
    }

    return addAdditionalConstraints();

    // return true;
  }

  public boolean addAdditionalConstraints() {
    Set<Slot> originalSlots = new HashSet<Slot>(featureStructure.getSlots());
    for (Slot s : originalSlots) {
      if (s.getTypeConstraint() != null && s.getTypeConstraint().getTypeSystem() == getSchemaTypeSystem()
              && s.getFeatures() != null && s.getFeatures().size() > 0) {
        Set<Constraint> constraints = ((Schema) s.getTypeConstraint().getTypeSystem()
                .get(s.getTypeConstraint().getType())).getContents().getConstraints();
        List<Role> sources = new ArrayList<Role>(s.getFeatures().keySet());
        for (Role source : sources) {
          for (Constraint c : constraints) {
            if (c.getOperator() == ECGConstants.IDENTIFY) {
              if (c.getArguments().get(0).getChain().size() == 1 && c.getArguments().get(1).getChain().size() == 1
                      && c.getArguments().get(0).getChain().get(0).equals(source)
                      || c.getArguments().get(0).getChain().size() == 1
                      && c.getArguments().get(1).getChain().size() == 1
                      && c.getArguments().get(1).getChain().get(0).equals(source)) {
                if (!featureStructure.coindex(s, c.getArguments().get(0), s, c.getArguments().get(1))) {
                  return false;
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  public TypeSystem getExternalTypeSystem() {
    return headCxn.getExternalTypeSystem();
  }

  public TypeSystem getSchemaTypeSystem() {
    return headCxn.getSchemaTypeSystem();
  }

  public TypeSystem getCxnTypeSystem() {
    return headCxn.getCxnTypeSystem();
  }

  public void setStartIndex(int index) {
    this.leftIndex = index;
    gapFillerPSS = null;
    gapFillerType = null;
    alreadyUsedGapFiller = false;
    gapFillerChain = null;
    gappedSpans = new LinkedList<CxnalSpan>();
    foundGap = false;
  }

  public void setSemanticChains(List<SlotChain> chains, List<List<Pair<TypeConstraint, Role>>> frameRoles) {
    List<TypeConstraint> types = new ArrayList<TypeConstraint>();
    for (SlotChain chain : chains) {
      types.add(getFeatureStructure().getSlot(chain).getTypeConstraint());
    }
    myMiniSemSpecs = new PossibleSemSpecs(chains, types, frameRoles);
  }

  public boolean addConstraint(Constraint constraint, Role prepend, String prefix) {
    if (constraint.isAssign()) {
      return addConstraint(new Constraint(constraint.getOperator(), new ECGSlotChain(prepend, constraint.getArguments()
              .get(0)), constraint.getValue()), prefix);
    }
    else {
      SlotChain sc0 = constraint.getArguments().get(0);
      SlotChain sc1 = constraint.getArguments().get(1);
      if (sc0.toString().equals(ECGConstants.SELF) == false) {
        sc0 = new ECGSlotChain(prepend, sc0);
      }
      if (sc1.toString().equals(ECGConstants.SELF) == false) {
        sc1 = new ECGSlotChain(prepend, sc1);
      }
      return addConstraint(new Constraint(constraint.getOperator(), sc0, sc1), prefix);
    }
  }

  public boolean addConstraint(Constraint constraint, String prefix) {
    if (constraint.getOperator().equals(ECGConstants.ASSIGN)) {
      if (constraint.getValue().charAt(0) == ECGConstants.CONSTANTFILLERPREFIX) {
        return featureStructure.fill(new ECGSlotChain(prefix, constraint.getArguments().get(0)), constraint.getValue());
      }
      else if (constraint.getValue().charAt(0) != ECGConstants.CONSTANTFILLERPREFIX) { // then
                                                                                       // this
                                                                                       // is
                                                                                       // a
                                                                                       // type
        TypeConstraint typeConstraint = null;
        if (constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX) {
          typeConstraint = getExternalTypeSystem().getCanonicalTypeConstraint(constraint.getValue().substring(1));
        }
        else {
          // XXX: Only Schemas can be literal fillers! (lucag)
          typeConstraint = getSchemaTypeSystem().getCanonicalTypeConstraint(constraint.getValue());
        }
        if (typeConstraint == null) {
          throw new ParserException("Type " + constraint.getValue() + " in constraint " + constraint + " is undefined");
        }
        featureStructure.getSlot(new ECGSlotChain(prefix, constraint.getArguments().get(0))).setTypeConstraint(
                typeConstraint);
      }
      return true;
    }
    else if (constraint.getOperator().equals(ECGConstants.IDENTIFY)) {
      SlotChain sc0 = constraint.getArguments().get(0);
      SlotChain sc1 = constraint.getArguments().get(1);
      if (sc0.getChain().get(0) != ECGConstants.DSROLE) {
        sc0 = new ECGSlotChain(prefix, sc0);
      }

      if (sc1.getChain().get(0) != ECGConstants.DSROLE) {
        sc1 = new ECGSlotChain(prefix, sc1);
      }

      // XXX: I (lucag) think the following was here just for debugging
      // purposes.
      // TypeConstraint tc00 = sc0.getChain().get(sc0.getChain().size() -
      // 1).getTypeConstraint();
      // TypeConstraint tc11 = sc1.getChain().get(sc1.getChain().size() -
      // 1).getTypeConstraint();
      //
      // boolean hasSlotTc0 =
      // featureStructure.hasSlot(featureStructure.getMainRoot(), sc0);
      // boolean hasSlotTc1 =
      // featureStructure.hasSlot(featureStructure.getMainRoot(), sc1);
      //
      // String fstructString = featureStructure.toString();
      // TypeConstraint tc0 = featureStructure.getSlot(sc0).getTypeConstraint();
      // TypeConstraint tc1 = featureStructure.getSlot(sc1).getTypeConstraint();

      boolean success = featureStructure.coindex(sc0, sc1);

      return success;
    }
    else {
      throw new ParserException("Analysis does not support a " + constraint.getOperator() + " constraint");
    }
  }

  /**
   * returns the construction that acts as the head of this analysis
   */
  public Construction getHeadCxn() {
    return headCxn;
  }

  public int getSpanLeftIndex() {
    return leftIndex;
  }

  public int getSpanRightIndex() {
    return rightIndex;
  }

  public void setSpanLeftIndex(int index) {
    leftIndex = index;
  }

  public void setSpanRightIndex(int index) {
    rightIndex = index;
  }

  private void processConstituentSets(Role role) {
    if (leftOverComplements == null) {
      System.out.println("what?");
    }
    leftOverComplements.remove(role);
    leftOverConstituents.remove(role);
    leftOverOptionals.remove(role);
  }

  public PossibleSemSpecs getPossibleSemSpecs() {
    return myMiniSemSpecs;
  }

  public boolean incorporateAncestorSemanticInfo(PossibleSemSpecs pss, List<BindingArrangement> bas,
          List<Double> penalties) {
    return myMiniSemSpecs.incorporateAncestorPossibleSemSpecs(pss, bas, penalties);
  }

  public boolean advance(Role role, Analysis that) {
    return advance(role, that, null);
  }

  public boolean advance(Role role, Analysis that, BindingArrangement bindingArrangement) {
    if (lexicalRHS == null) {
      processConstituentSets(role);
      ECGSlotChain ecgsc = new ECGSlotChain(role);
      if (!featureStructure.coindexAcrossFeatureStructureSets(ecgsc, ECGConstants.EMPTYSLOTCHAIN,
              that.getFeatureStructure())) {
        return false;
      }
      if (bindingArrangement != null) {
        if (!myMiniSemSpecs.incorporateChildPossibleSemSpecs(that.getPossibleSemSpecs(), bindingArrangement, false)) {
          return false;
          // throw new
          // ParserException("incorporateChildPossibleSemSpecs failed. "+this.getHeadCxn().getName()+"["+featureStructure.getMainRoot().getID()+"]"+
          // " "
          // +that.getHeadCxn().getName()+"["+that.featureStructure.getMainRoot().getID()+"]"+"\n"+this.myMiniSemSpecs.toString()+"\n\n-------------\n"
          // +bindingArrangement.toString()+"\n\n---------------\n"+that.myMiniSemSpecs.toString());
          // System.out.println("here's where the exception would be");
        }
      }
      CxnalSpan csp = new CxnalSpan(role, that.getHeadCxn(), getFeatureStructure().getSlot(new ECGSlotChain(role))
              .getID(), that.getSpanLeftIndex(), that.getSpanRightIndex());
      localRoleSpans.add(csp);
      if (that.leftOverOptionals != null) {
        csp.setUnusedOptionals((Set<Role>) that.leftOverOptionals.clone());
      }
      spans.add(csp);
      if (that.getSpans() != null) {
        spans.addAll(that.getSpans());
      }
      if (headCxn.isExtraPosedRole(role)) {
        if (that.gappedSpans.size() > 0) {
          return false;
        } // can't have gaps in the gap filler
        gapFillerPSS = that.getPossibleSemSpecs();
        gapFillerType = that.getHeadCxn();
        gapFillerChain = ecgsc;
        System.out.println("binding to extraposed role. role name is: " + role.getName() + " rootslotid:"
                + getFeatureStructure().getMainRoot().getID());
      }
      else if (this.hasGapFiller()) {
        for (CxnalSpan span : that.gappedSpans) {
          Slot gapFillerSlot = featureStructure.getSlot(gapFillerChain);
          if (!featureStructure.coindex(gapFillerSlot, featureStructure.getSlot(span.getSlotID()))) {
            return false;
          }
          foundGap = true;
          for (int i = 0; i < spans.size(); i++) {
            if (spans.get(i).getSlotID() == span.getSlotID()) {
              CxnalSpan newSpan = new CxnalSpan(span.role, gapFillerSlot.getID(), span.getRight(), false);
              spans.set(i, newSpan);
              break;
            }
          }
        }
        // if (foundGap){
        // System.out.println("found gap "+" rootslotid:"+getFeatureStructure().getMainRoot().getID());}
      }
      else {
        this.gappedSpans.addAll(that.gappedSpans);
      }
      setSpanRightIndex(that.getSpanRightIndex());
      return true;
    }
    else {
      throw new ParserException("advance called on an analysis with a null rhs");
    }
  }

  public boolean hasGapFiller() {
    return gapFillerPSS != null;
  }

  public boolean alreadyUsedGapFiller() {
    return alreadyUsedGapFiller;
  }

  public PossibleSemSpecs getGapFillerPSS() {
    return gapFillerPSS;
  }

  public Construction getGapFillerType() {
    return gapFillerType;
  }

  public boolean gapOut(Role role, Construction gapFillerType, PossibleSemSpecs gapFiller,
          BindingArrangement bindingArrangement) {
    if (lexicalRHS == null) {
      // System.out.println("begin gap out");
      processConstituentSets(role);
      if (bindingArrangement != null) {
        if (!myMiniSemSpecs.incorporateChildPossibleSemSpecs(gapFiller, bindingArrangement, true)) {
          // System.out.println("here's where the exception would be between"+this.getHeadCxn().getName()+"["+featureStructure.getMainRoot().getID()+"]"
          // +"  "+role.getName()+": \n"+gapFiller.toString());
          return false;

        }
      }
      CxnalSpan span = new CxnalSpan(role, getFeatureStructure().getSlot(new ECGSlotChain(role)).getID(),
              this.getSpanRightIndex(), false);
      span.type = gapFillerType;
      localRoleSpans.add(span);
      spans.add(span);
      gappedSpans.add(span);
      alreadyUsedGapFiller = true;
      // System.out.println("completed gap out");
      return true;
    }
    else {
      throw new ParserException("advance called on an analysis with a null rhs");
    }
  }

  public boolean omit(Role role) {
    if (lexicalRHS == null && leftOverComplements.contains(role)) {
      processConstituentSets(role);
      CxnalSpan csp = new CxnalSpan(role, getFeatureStructure().getSlot(new ECGSlotChain(role)).getID(),
              this.getSpanRightIndex(), true);
      localRoleSpans.add(csp);
      spans.add(csp);
      return true;
    }
    else {
      // System.out.println("head = " + headCxn.getName() + " role name = " +
      // role.getName());
      // System.out.println("leftover complements = " + leftOverComplements);
      // System.out.println("leftover optionals = " + leftOverOptionals);
      throw new ParserException("cannot omit from a construction without constituents");
    }
  }

  public boolean omitOptional(Role role) {
    if (lexicalRHS == null && leftOverOptionals.contains(role)) {
      processConstituentSets(role);
      // CxnalSpan csp = new CxnalSpan(role, getFeatureStructure().getSlot(new
      // ECGSlotChain(role)).getID(),
      // this.getSpanRightIndex(), true);
      // localRoleSpans.add(csp);
      // spans.add(csp);
      return true;
    }
    else {
      throw new ParserException("cannot optionalize from a construction without constituents");
    }
  }

  public boolean advance() {
    if (lexicalRHS != null) {
      rightIndex = leftIndex + 1;
      lexicalRHS = FOUND;
      return true;
    }
    else {
      return false;
    }
  }

  /** Returns true is this analysis is complete */
  public boolean completed() {
    if (lexicalRHS == null) {
      return foundAllComplements() && !leftOverOptionals()
              && (gapFillerPSS == null || gapFillerPSS != null && foundGap);
    }
    else {
      return lexicalRHS == FOUND;
    }
  }

  private boolean foundAllComplements() {
    return leftOverComplements.size() == 0;
  }

  public boolean leftOverOptionals() {
    return lexicalRHS == null && leftOverOptionals != null && leftOverOptionals.size() > 0;
  }

  public Set<Role> getLeftOverConstituents() {
    return leftOverConstituents;
  }

  public Set<Role> getLeftOverComplements() {
    return leftOverComplements;
  }

  public Set<Role> getLeftOverOptionals() {
    return leftOverOptionals;
  }

  double computeSemanticCost() {
    if (semSpecScorer == null) {
      return 0;
    }
    double normalizer = Double.NEGATIVE_INFINITY;
    List<PossibleSemSpecs.PartialSemSpec> pss = myMiniSemSpecs.getSemSpecList();
    int numSSs = pss.size();
    double[] innerCosts = new double[numSSs];
    for (int i = 0; i < numSSs; i++) {
      PossibleSemSpecs.PartialSemSpec ps = pss.get(i);
      normalizer = SloppyMath.logAdd(normalizer, ps.externalProb);
      for (PossibleSemSpecs.PartialSemSpec.Slot slot : ps.slots.values()) {
        innerCosts[i] = innerCosts[i] + semSpecScorer.scoreSingleBinding(slot.frameRoles, slot.fillerTypeConstraint);
      }
    }
    double score = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < numSSs; i++) {
      PossibleSemSpecs.PartialSemSpec ps = pss.get(i);
      score = SloppyMath.logAdd(score, ps.externalProb - normalizer + innerCosts[i]);
    }
    return score;
  }

  /**
   * returns the FeatureStructureSet tracking the bindings/constraints
   * associated with this analysis
   */
  public FeatureStructureSet getFeatureStructure() {
    return featureStructure;
  }

  public String toString() {
    return formatter.format(this);
  }

  public List<CxnalSpan> getSpans() {
    return spans;
  }

  public List<CxnalSpan> getGappedSpans() {
    return gappedSpans;
  }

  public void addSpan(CxnalSpan span) {
    if (spans == null) {
      spans = new ArrayList<CxnalSpan>();
    }
    spans.add(span);
  }

  /** Clone */
  public Analysis clone() {
    try {
      Analysis a = (Analysis) super.clone();
      if (this.getFeatureStructure() == null) {
        throw new ParserException("Analysis.clone(): null fs");
      }
      a.featureStructure = this.getFeatureStructure().clone();
      if (myMiniSemSpecs != null) {
        a.myMiniSemSpecs = myMiniSemSpecs.clone();
      }
      if (lexicalRHS == null && this.leftOverConstituents != null) {
        a.leftOverComplements = new IdentityHashSet<Role>(this.leftOverComplements);
        a.leftOverOptionals = new IdentityHashSet<Role>(this.leftOverOptionals);
        a.leftOverConstituents = new IdentityHashSet<Role>(this.leftOverConstituents);
      }
      if (spans != null) {
        a.spans = new ArrayList<CxnalSpan>();
        a.spans.addAll(spans);
      }

      if (localRoleSpans != null) {
        a.localRoleSpans = new ArrayList<CxnalSpan>();
        a.localRoleSpans.addAll(localRoleSpans);
      }
      if (gappedSpans != null) {
        a.gappedSpans = new LinkedList<CxnalSpan>();
        a.gappedSpans.addAll(gappedSpans);
      }
      return a;
    }
    catch (Exception e) {
      logger.warning("Inexplicable meltdown in Analysis.clone: " + e);
      throw new ParserException("Inexplicable meltdown in Analysis.clone: " + e);
    }
  }

  public static void main(String[] args) throws IOException, TypeSystemException {
    String ontFile = null;
    Grammar grammar = null;
    if (args.length > 1) {
      ontFile = args[1];
      grammar = ECGGrammarUtilities.read(args[0], "ecg cxn sch grm", ontFile);
    }
    else if (args.length == 1) {
      grammar = ECGGrammarUtilities.read(args[0]);
    }

    System.out.println(grammar);
    for (Construction c : grammar.getAllConstructions()) {
      Analysis a = new Analysis(c);
      System.out.println(a.getFeatureStructure());
    }

  }

}

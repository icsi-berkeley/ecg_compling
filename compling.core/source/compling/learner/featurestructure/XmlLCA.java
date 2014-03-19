//=============================================================================
// File        : XMLLearnerFeatureStructure.java
// Author      : emok
// Change Log  : Created on Feb 16, 2007
//=============================================================================

package compling.learner.featurestructure;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.*;

//import compling.context.Resolution;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.*;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.unificationgrammar.UnificationGrammar.*;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.*;
import compling.learner.util.SlotChainUtilities;
import compling.parser.ecgparser.CxnalSpan;

import compling.util.Pair;
import compling.util.Triplet;
import compling.util.MapSet;

//=============================================================================

public class XmlLCA extends LearnerCentricAnalysis {

   // TODO: need span of the root level constructs

   public static final String FS = "FS";
   public static final String SLOT = "slot";
   public static final String ID = "id";
   public static final String ROLE = "role";
   public static final String NAME = "name";
   public static final String UNIFICATION_CAUSE = "cause"; //obsolete
   public static final String FILLER = "filler";
   public static final String FILLER_TYPE = "type";
   public static final String FILLER_ROLE = "localRole";
   public static final String RESOLUTION = "resolution";
   public static final String CANDIDATE = "candidate";
   public static final String SCORE = "score";
   public static final String CONSTRUCT = "construct";
   public static final String SCHEMA = "schema";
   public static final String SPAN_START = "start";
   public static final String SPAN_END = "end";
   public static final String NOFILLER = "no filler";
   public static final String ATOMIC = "atomic";

   private static int slotCounter = 0;

   HashMap<String, Pair<FeatureStructureSet, Slot>> featureStructureSets = null;  //HashMap<rootName, Pair<fss, rootSlot>>

   XmlLCA (Grammar grammar, File file) throws LCAException{

      super(grammar);

      featureStructureSets = new HashMap<String, Pair<FeatureStructureSet, Slot>>();
      resolutions = new ResolutionResults();

      Document document = null;
      SAXBuilder builder;

      builder = new SAXBuilder();
      try {
         document = builder.build(file);
      } catch (IOException ioe) {
         System.err.println("IO Exception");
         throw new LCAException(ioe);
      } catch (JDOMException jde) {
         System.err.println("JDOM Exception");
         throw new LCAException(jde);
      }

      Element root = document.getRootElement();
      extractXMLSlots(root);
   }


   public Collection<FeatureStructureSet> getFeatureStructureSets() {
      Set<FeatureStructureSet> allFSS = new HashSet<FeatureStructureSet>();
      for (Pair<FeatureStructureSet, Slot> loc : featureStructureSets.values()) {
         allFSS.add(loc.getFirst());
      }
      return allFSS;
   }

   public int getNumSeparateAnalyses() {
      return featureStructureSets.size();
   }


   public int getFeatureStructureSetIndex(FeatureStructureSet fss) {
      int i = 0;
      for (FeatureStructureSet f : getFeatureStructureSets()) {
         if (f == fss) {
            return i;
         }
         i++;
      }
      return -1;
   }

   public int getLargestAssignedSlotID() {
      return slotCounter;
   }

   private Pair<FeatureStructureSet, Slot> findLocation(String rootName) {
      if (featureStructureSets.containsKey(rootName)) {
         return featureStructureSets.get(rootName);
      } else {
         return null;
      }
   }

   private void updateLocation(String rootName, FeatureStructureSet newFSS, Slot newRootSlot) {
      featureStructureSets.put(rootName, new Pair<FeatureStructureSet, Slot>(newFSS, newRootSlot));
   }


   private void extractXMLSlots(Element root) throws LCAException{

      List<Element> xmlSlots = (List<Element>) root.getChildren(SLOT);

      for (Element xmlSlot : xmlSlots) {

         List<Element> xmlRoles = (List<Element>) xmlSlot.getChildren(ROLE);

         SlotChainInfo first = processRoleElement(xmlRoles.get(0));
         Pair<FeatureStructureSet, Slot> firstLoc = findLocation(first.getRootName());

         for (Element xmlRole : xmlRoles.subList(1, xmlRoles.size())) {

            SlotChainInfo current = processRoleElement(xmlRole);
            Pair<FeatureStructureSet, Slot> currentLoc = findLocation(current.getRootName());

            if (firstLoc.getFirst() == currentLoc.getFirst()) {
               // both slot chains are in the same FSS - coindex given the root slots
               boolean unified = firstLoc.getFirst().coindex(firstLoc.getSecond(), first.getSlotChain(), currentLoc.getSecond(), current.getSlotChain());
               if (!unified) {
                  throw new LCAException("unification between these two slot chains failed: \n" + first.getRootName() + "." + first.getSlotChain() + "\n" + current.getRootName() + "." + current.getSlotChain());
               }
            } else {
               // the slot chains are in different FSS - merge first (so we know where the new root slot is), then coindex
               Slot newRoot = firstLoc.getFirst().mergeFeatureStructureSets(currentLoc.getFirst());
               boolean unified = firstLoc.getFirst().coindex(firstLoc.getSecond(), first.getSlotChain(), newRoot, current.getSlotChain());
               if (!unified) {
                  throw new LCAException("unification between these two slot chains failed: \n" + first.getRootName() + "." + first.getSlotChain() + "\n" + current.getRootName() + "." + current.getSlotChain());
               }
               updateLocation(current.getRootName(), firstLoc.getFirst(), newRoot);
            }

         }


         Element xmlFiller = xmlSlot.getChild(FILLER);
         if (xmlFiller != null) {
            String filler = xmlFiller.getAttributeValue(NAME);
            if (filler != null && !filler.equals(NOFILLER) && xmlFiller.getAttribute(FILLER_TYPE) != null && xmlFiller.getAttribute(FILLER_TYPE).getValue().equals(ATOMIC)) {
               firstLoc.getFirst().fill(firstLoc.getSecond(), first.getSlotChain(), "\"" + filler + "\"");
            }
         }

         // is Slot stable at this point?
         Element xmlResolution = xmlSlot.getChild(RESOLUTION);
         processResolutionElement(firstLoc.getSecond(), xmlResolution);
      }

   }

   public boolean coindexAcrossFeatureStructureSets(FeatureStructureSet fss1, SlotChain sc1, FeatureStructureSet fss2, SlotChain sc2) {
      return fss1.coindexAcrossFeatureStructureSets(sc1, sc2, fss2);
   }


   private SlotChainInfo processRoleElement(Element xmlRole) {

      if (xmlRole.getAttributeValue(NAME) == null) {
         throw new LCAException("null role name in XML file");
      }

      SlotChainInfo current = createTypedSlotChain(xmlRole.getAttributeValue(NAME));
      addSlotChain(current);
      return current;
   }

   private Slot addSlotChain(SlotChainInfo info) {
      // This SlotChain is new, so we have to look up which FSS it should go in
      // If the root is already defined in some FSS, stick the slotchain under there
      // If the root isn't found anywhere, a new FSS is needed
      if (info == null) {
         return null;
      }

      Pair<FeatureStructureSet, Slot> location = findLocation(info.getRootName());
      if (location != null) {
         Slot slot = location.getFirst().getSlot(location.getSecond(), info.getSlotChain());
         slot.setID(slotCounter++);
         return slot;
      } else {
         FeatureStructureSet newFSS = new FeatureStructureSet();
         newFSS.getMainRoot().setTypeConstraint(info.getRootType());
         Slot slot = newFSS.getSlot(info.getSlotChain());
         slot.setID(slotCounter++);
         updateLocation(info.getRootName(), newFSS, newFSS.getMainRoot());
         return slot;
      }
   }


   private SlotChainInfo createTypedSlotChain(String slotchain) throws LCAException {

      String rootName = SlotChainUtilities.extractFromSlotChain(slotchain, 0);
      String rootTypeName = SlotChainUtilities.instanceToTypeString(rootName);
      Construction rootCxn = grammar.getConstruction(rootTypeName);
      if ( rootCxn == null) {
         throw new LCAException("Error in parsing slot chain: " + rootTypeName + " is not a valid construct");
      }
      TypeConstraint rootType = grammar.getCxnTypeSystem().getCanonicalTypeConstraint(rootTypeName);


      SlotChain resultChain = SlotChainUtilities.spliceSlotChain(slotchain, "", 0);

      List<TypeConstraint> typeChain = new ArrayList<TypeConstraint>();
      typeChain = findTypeConstraint(rootType, resultChain.getChain(), typeChain, 0);

      for (Role role : resultChain.getChain()) {
         role.setTypeConstraint(typeChain.get(resultChain.getChain().indexOf(role)));
      }
      return new SlotChainInfo(resultChain, rootName, rootType);

   }

   private List<TypeConstraint> findTypeConstraint(TypeConstraint rootType, List<Role> slotChain, List<TypeConstraint> typeChain, int index) {

      if (index == slotChain.size()) return typeChain;

      TypeConstraint typeConstraint = null;
      Role current = slotChain.get(index);

      if (rootType.getTypeSystem().equals(grammar.getCxnTypeSystem())) {

         Construction cxn = grammar.getConstruction(rootType.getType());

         if (current.getName().equals(ECGConstants.MEANING_POLE)) {
            // check to see if this is the meaning block
            String mpole = cxn.getMeaningBlock().getType();
            if (mpole.equals(ECGConstants.UNTYPED)) {
               // meaning pole is untyped. There might still be evoked items coming up.
               // do nothing (and add null)
            } else {
               if (cxn.getMeaningBlock().getBlockTypeTypeSystem().equals(grammar.getSchemaTypeSystem())) {
                  typeConstraint = grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(mpole);
               } else if (cxn.getMeaningBlock().getBlockTypeTypeSystem().equals(grammar.getOntologyTypeSystem())) {
                  typeConstraint = grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(mpole);
               } else {
                  // something terribly wrong here
                  unknownSlotInSlotChain(current.getName(), slotChain);
               }
            }
         } else if (cxn.getConstructionalBlock().getElements().contains(current)) {
            // constituent?
            for (Role role : cxn.getConstructionalBlock().getElements()) {
               if (current.equals(role)) {
                  typeConstraint = role.getTypeConstraint();
                  break;
               }
            }
         } else if (cxn.getMeaningBlock().getEvokedElements().contains(current)) {
            // evoked
            for (Role role : cxn.getMeaningBlock().getEvokedElements()) {
               if (current.equals(role)) {
                  typeConstraint = role.getTypeConstraint();
                  break;
               }
            }
         } else {
            // something terribly wrong here
            unknownSlotInSlotChain(current.getName(), slotChain);
         }

      } else if (rootType.getTypeSystem().equals(grammar.getSchemaTypeSystem())) {
         // elements

         Schema schema = grammar.getSchema(rootType.getType());

         if (schema.getAllRoles().contains(current)) {
            for (Role role : schema.getAllRoles()) {
               if (current.equals(role)) {
                  typeConstraint = role.getTypeConstraint();
                  break;
               }
            }
         } else {
            // something terribly wrong here
            unknownSlotInSlotChain(current.getName(), slotChain);
         }

      } else if (rootType.getTypeSystem().equals(grammar.getOntologyTypeSystem())) {
         // this should not be necessary until ECG supports dotting into ontology items
      }
      typeChain.add(typeConstraint);
      return findTypeConstraint(typeConstraint, slotChain, typeChain, index + 1);
   }

   private void unknownSlotInSlotChain(String slotName, List<Role> slotChain) {
      SlotChain sc = new SlotChain().setChain(slotChain);
      throw new LCAException("Encountered an undetermined slot " + slotName + " while attempting to decipher the following slot chain: " + sc.toString());
   }


   public void processResolutionElement(Slot slot, Element xmlResolution) {
      //TODO: make XML based LFSS read in resolutions too
      List<Element> xmlCandidates = (List<Element>) xmlResolution.getChildren(CANDIDATE);
      double avgScore = 1.0 / xmlCandidates.size();
      for (Element xmlCandidate : xmlCandidates) {
         String individualName = xmlCandidate.getAttributeValue(NAME);
         String score = xmlCandidate.getAttributeValue(SCORE);
         if (individualName == null) {
            throw new LCAException("null resolution candidate name in XML file");
         }
         if (score == null) {
            resolutions.addResult(slot, individualName, avgScore);
         } else {
            resolutions.addResult(slot, individualName, Double.valueOf(score));
         }
      }

   }

   public Slot getSlot(int slotID) {
      // FIXME: get slot
      return null;
   }

   public Slot getSlot(int rootID, String slotChain) {
      // FIXME: get slot
      return null;
   }

   public Slot getSlot(int rootID, SlotChain slotChain) {
      // FIXME: get slot
      return null;
   }


   public TypeConstraint getTypeConstraint(int slotID) {
      return getSlot(slotID) == null ? null : getSlot(slotID).getTypeConstraint();
   }

   public Set<Integer> findDummyFillerSlots() {
      // FIXME: dummy function
      return null;
   }

   public Map<Integer, CxnalSpan> getCxnalSpans() {
      // FIXME: dummy function
      return null;
   }

   public Set<Construction> getCxnsUsed() {
      // FIXME: dummy function
      return null;
   }

   public static void generateXML(LearnerCentricAnalysis lfss, File file) throws IOException{

      //TODO: output resolution results

      Element fs = new Element(FS);
      Document outputDoc = new Document(fs);

      for (FeatureStructureSet fss : lfss.getFeatureStructureSets()) {

         MapSet<Integer, SlotChain> table = lfss.getTables().getSlotChainTableForLeaves(fss);

         for (Integer slotID : table.keySet()) {
            Slot slot = fss.getSlot(slotID);
            Element xSlot = new Element(SLOT);
            Set<SlotChain> slotChains = table.get(slot);
            for (SlotChain slotChain : slotChains) {
               Element xRole = new Element(ROLE);
               xRole.setAttribute(NAME, slotChain.toString());
               xSlot.addContent(xRole);
            }
            fs.addContent(xSlot);

            if (slot.hasAtomicFiller()) {
               Element filler = new Element(FILLER);
               filler.setAttribute(FILLER_TYPE, ATOMIC);
               filler.setAttribute(NAME, slot.getAtom());
               xSlot.addContent(filler);
            }
         }
      }


      FileOutputStream fos = new FileOutputStream(file);
      XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
      outputter.output(outputDoc, fos);
      fos.close();

   }



   // This class exists merely to make the code easier to read.

   protected class SlotChainInfo extends Triplet<SlotChain, String, TypeConstraint> {

      private static final long serialVersionUID = 1986601466060983578L;


      protected SlotChainInfo(SlotChain slotChain, String rootName, TypeConstraint rootType) {
         super(slotChain, rootName, rootType);
      }


      protected String getRootName() {
         return getSecond();
      }


      protected TypeConstraint getRootType() {
         return getThird();
      }


      protected SlotChain getSlotChain() {
         return getFirst();
      }

   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      for (FeatureStructureSet fss : getFeatureStructureSets()) {
         sb.append(fss.toString());
         sb.append("-----\n");
      }

      sb.append("\n").append(resolutions.toString()).append("\n");
      return sb.toString();

   }
}

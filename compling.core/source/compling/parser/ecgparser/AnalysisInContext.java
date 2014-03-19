package compling.parser.ecgparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compling.context.ContextModelCache;
import compling.context.MiniOntology.Individual;
import compling.context.Resolution;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.GrammarWrapper;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ecgparser.PossibleSemSpecs.BindingArrangement;

public class AnalysisInContext extends Analysis implements Cloneable {
	private static TypeConstraint RDTYPECONSTRAINT;
	List<Resolution> resolutions;
	private static ContextModelCache cmc;

	static final Role mRole = new Role(ECGConstants.MEANING_POLE);
	static final Role RESOLVEDREFROLE = new Role(ECGConstants.RESOLVEDREFERENT);
	static final Role REFERENCEKINDROLE = new Role(ECGConstants.REFERENCEKIND);
	static final Role CATEGORYROLE = new Role(ECGConstants.ONTOLOGICALCATEGORY);

	public static void setRDTypeConstraint(TypeConstraint rd) {
		RDTYPECONSTRAINT = rd;
	}

	public static void setContextModelCache(ContextModelCache cache) {
		cmc = cache;
	}

	public AnalysisInContext(Construction headCxn, GrammarWrapper g) {
		super(headCxn, g);
	}

	public void setRDChains(List<SlotChain> refs) {
		resolutions = new ArrayList<Resolution>();
		for (SlotChain sc : refs) {
			Resolution r = new Resolution(sc);
			r.sourceID = -1;
			resolutions.add(r);
		}

	}

	public boolean advance(Role role, Analysis that, BindingArrangement bindingArrangement) {
		boolean successfulFill = super.advance(role, that, bindingArrangement);
		AnalysisInContext aic = (AnalysisInContext) that;
		for (Resolution tr : aic.resolutions) {
			int slotID = tr.sourceID;
			if (slotID == -1) {
				slotID = getFeatureStructure().getSlot(new ECGSlotChain(role)).getID();
			}
			resolutions
					.add(new Resolution(slotID, tr.sc, tr.candidates, tr.scores, tr.bestIndex, tr.resolved, tr.omitted));
		}

		return successfulFill;
	}

	public boolean omit(Role role) {
		if (super.omit(role)) {
			SlotChain sc = new ECGSlotChain(role, mRole);
			Resolution resolution = new Resolution(sc);
			resolution.omitted = true;
			resolution.sourceID = -1;
			resolutions.add(resolution);

			return true;
		}
		return false;
	}

	public void resolve() {
		// System.out.println(cmc);
		for (Resolution res : resolutions) {
			Slot slot = null;
			if (res.sourceID == -1) {
				slot = getFeatureStructure().getSlot(res.sc);
			}
			else {
				try {
					slot = getFeatureStructure().getSlot(getFeatureStructure().getSlot(res.sourceID), res.sc);
				}
				catch (NullPointerException npe) {
					npe.printStackTrace();
					System.out.println("head cxn = " + headCxn);
				}
			}
			// System.out.println("in resolve");
			// if (!res.resolved){
			// System.out.println("\tIn Resolve: source:"+res.sourceID+"  "+res.sc+" resolved?:"+res.resolved);

			TypeConstraint tc = slot.getTypeConstraint();

			if (RDTYPECONSTRAINT == null || (RDTYPECONSTRAINT != null && tc != RDTYPECONSTRAINT)) {
				if (tc != null) {
					cmc.resolve(ECGConstants.GIVEN, null, tc.getType(), null, res);
				}
				else {
					// throw new
					// ParserException("TypeConstraint == null in AnalysisInContext.resolve: "+getHeadCxn().getName()+"  "+res.sc);
					// I think this now indicates the omission of something that didn't have semantic significance, so I'll
					// just
					// mark it as resolved and move on
					res.resolved = true;
				}
			}
			else {
				Map<Role, Slot> features = slot.getFeatures();
				Map<Role, TypeConstraint> properties = new HashMap<Role, TypeConstraint>();
				TypeConstraint resRefTC = null;
				TypeConstraint category = null;
				String resolutionType = ECGConstants.GIVEN;
				if (features != null) {
					for (Role role : features.keySet()) {
						if (role.equals(RESOLVEDREFROLE)) {
							resRefTC = features.get(role).getTypeConstraint();
						}
						else if (role.equals(REFERENCEKINDROLE)) {
							resolutionType = features.get(role).getAtom();
						}
						else if (role.equals(CATEGORYROLE)) {
							category = features.get(role).getTypeConstraint();
						}
						else if (features.get(role).getTypeConstraint() != null) {
							properties.put(role, features.get(role).getTypeConstraint());
						}
					}
				}
				String resRefTCType = null;
				if (resRefTC != null) {
					resRefTCType = resRefTC.getType();
				}
				String categoryType = null;
				if (category != null) {
					categoryType = category.getType();
				}
				// System.out.println("In Resolve: "+res.sc+" catType:"+categoryType+"  resRefTCType:"+resRefTCType);
				if (resolutionType == null) {
					resolutionType = ECGConstants.GIVEN;
				}
				cmc.resolve(resolutionType, properties, resRefTCType, categoryType, res);
				StringBuilder sb = new StringBuilder();
				int j = 0;
				if (res.candidates != null && res.candidates.size() > 0) {
					for (Individual i : res.candidates) {
						String score = Double.toString(res.scores.get(j));
						// score = score.substring(0, score.indexOf(".")+3);
						sb.append(i.getName() + "[" + i.getTypeName() + "] " + score + " , ");
						j++;
					}
				}
				else {
					sb.append("No Referents");
				}
				// System.out.println("\t"+sb.toString());
				sb.append("\n");

			}
			res.resolved = true;
		}
	}

	public AnalysisInContext clone() {
		try {
			AnalysisInContext aic = (AnalysisInContext) super.clone();
			aic.resolutions = new ArrayList<Resolution>();
			if (resolutions != null) {
				for (Resolution res : resolutions) {
					aic.resolutions.add(new Resolution(res.sourceID, res.sc, res.candidates, res.scores, res.bestIndex,
							res.resolved, res.omitted));
				}
			}
			return aic;
		}
		catch (Exception e) {
			throw new GrammarException("Inexplicable meltdown in AnalysisInContext.clone(): " + e);
		}

	}

	public List<Resolution> getResolutionsList() {
		return resolutions;
	}

	public Map<Slot, Resolution> getResolutions() {
		Map<Slot, Resolution> res = new HashMap<Slot, Resolution>();
		for (Resolution r : resolutions) {
			Slot source = findSourceSlot(r);
			if (source != null && res.get(getFeatureStructure().getSlot(source, r.sc)) == null) {
				res.put(getFeatureStructure().getSlot(source, r.sc), r);
			}
		}
		return res;
	}

	Slot findSourceSlot(Resolution resolution) {
		Slot source = null;
		if (resolution.sourceID == -1) {
			source = getFeatureStructure().getMainRoot();
		}
		else {
			for (Slot slot : getFeatureStructure().getSlots()) {
				if (slot.getID() == resolution.sourceID) {
					source = slot;
					break;
				}
			}
		}
		return source;
	}

}

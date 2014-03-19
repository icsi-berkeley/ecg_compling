package compling.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import compling.context.MiniOntology.Individual;
import compling.context.MiniOntology.Interval;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.context.RDCEScorer.BasicScorer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.RecencyCache;

public class SimpleContextModelCache extends ContextModelCache {

	static final double MATCH_THRESHOLD = 0.5;

	// for the situation
	RecencyCache<MiniOntology.Interval> situationIntervalCache;
	RecencyCache<MiniOntology.Individual> situationIndividualCache;

	// for the discourse
	RecencyCache<MiniOntology.Interval> discourseIntervalCache;
	RecencyCache<MiniOntology.Individual> discourseIndividualCache;
	Map<MiniOntology.Individual, Map<Role, TypeConstraint>> properties; // caches relational stuff
	Role DISCOURSEPARTICIPANTROLE = new Role(ECGConstants.discourseParticipantRoleRoleName);
	List<Individual> speaker = new LinkedList<Individual>();
	List<Individual> addressee = new LinkedList<Individual>();
	List<Individual> attentionalFocus = new LinkedList<Individual>();
	TypeConstraint speakerTypeConstraint;
	TypeConstraint addresseeTypeConstraint;
	TypeConstraint attentionalFocusTypeConstraint;
	TypeConstraint discourseParticipantRoleTypeConstraint;
	String propertyFillerTypeName;

	RDCEScorer.Scorer scorer = new BasicScorer();

	SimpleContextModelCache(ContextModel contextModel, int situationIntervalCacheSize, int situationIndividualCacheSize,
			int discourseIntervalCacheSize, int discourseIndividualCacheSize) {
		super(contextModel);
		situationIntervalCache = new RecencyCache<MiniOntology.Interval>(situationIntervalCacheSize);
		situationIndividualCache = new RecencyCache<MiniOntology.Individual>(situationIndividualCacheSize);
		discourseIntervalCache = new RecencyCache<MiniOntology.Interval>(discourseIntervalCacheSize);
		discourseIndividualCache = new RecencyCache<MiniOntology.Individual>(discourseIndividualCacheSize);
		properties = new HashMap<MiniOntology.Individual, Map<Role, TypeConstraint>>();
		speaker.add(null); // this is just a silly ( but necessary) init to get the set method to work
		addressee.add(null);
		attentionalFocus.add(null);
		speakerTypeConstraint = contextModel.getTypeSystem().getCanonicalTypeConstraint(ECGConstants.speakerTypeName);
		addresseeTypeConstraint = contextModel.getTypeSystem().getCanonicalTypeConstraint(ECGConstants.addresseeTypeName);
		attentionalFocusTypeConstraint = contextModel.getTypeSystem().getCanonicalTypeConstraint(
				ECGConstants.attentionalFocusTypeName);
		discourseParticipantRoleTypeConstraint = contextModel.getTypeSystem().getCanonicalTypeConstraint(
				ECGConstants.discourseParticipantRoleTypeName);

		propertyFillerTypeName = miniOntology.getTypeSystem().getInternedString(ECGConstants.PROPERTY_FILLER);
	}

	private void update(List<MiniOntology.Individual> values, RecencyCache<Individual> individualCache,
			RecencyCache<Interval> intervalCache) {
		for (MiniOntology.Individual i : values) {
			if (i.getKindOfValue() == MiniOntology.INDIVIDUAL) {
				try {
					if (!i.getTypeName().equals(MiniOntology.SINGLETONNAME)
							&& !miniOntology.getTypeSystem().subtype(i.getTypeName(), propertyFillerTypeName)) {
						individualCache.add(i);
					}
				}
				catch (TypeSystemException tse) {
					throw new ContextException("Error while adding an individual to the context model cache", tse);
				}
			}
			else {
				intervalCache.add((MiniOntology.Interval) i);
			}
			properties.put(i, new HashMap<Role, TypeConstraint>());
			SimpleQuery q = new SimpleQuery("?r", i.getName(), "?v");
			List<SimpleQuery> ql = new LinkedList<SimpleQuery>();
			ql.add(q);
			List<HashMap<String, String>> results = contextModel.query(ql);
			// System.out.println("For individual: "+i.getName()+" results for query are:\n"+MiniOntologyQueryAPI.bindingsToString(results));
			for (HashMap<String, String> result : results) {
				String roleName = result.get("?r");
				String fillerType = result.get("?v");
				// System.out.println("filler type: "+fillerType);
				if (contextModel.isTypedFiller(fillerType)) {
					String fillerName = contextModel.getIndividualName(fillerType);
					// System.out.println("filler name = "+fillerName);
					fillerType = contextModel.getIndividualType(fillerType);

					properties.get(i).put(new Role(roleName),
							miniOntology.getTypeSystem().getCanonicalTypeConstraint(fillerType));
					if (i.getKindOfValue() == MiniOntology.INTERVAL) {
						if (i.getType().getType().equals(ECGConstants.DISCOURSESEGMENTTYPE)) {
							// System.out.println("we're in here now. role name is "+roleName);
							if (roleName.equals(ECGConstants.speakerRoleName)) {
								Individual speakerInd = contextModel.getMiniOntology().getCurrentInterval()
										.getIndividual(fillerName);
								if (speakerInd == null) {
									throw new ContextException("Bizarre error while updating cache: retrieved speaker "
											+ fillerName + " does not exist");
								}
								speaker.set(0, speakerInd);
							}
							else if (roleName.equals(ECGConstants.addresseeRoleName)) {
								Individual addresseeInd = contextModel.getMiniOntology().getCurrentInterval()
										.getIndividual(fillerName);
								if (addresseeInd == null) {
									throw new ContextException("Bizarre error while updating cache: retrieved addressee "
											+ fillerName + " does not exist");
								}
								addressee.set(0, addresseeInd);
							}
							else if (roleName.equals(ECGConstants.attentionalFocusRoleName)) {
								// Eva's edit: the attentional focus can be an event (which will be a preceeding interval)
								// Individual atfInd =
								// contextModel.getMiniOntology().getCurrentInterval().getIndividual(fillerName);
								Individual atfInd = MiniOntologyQueryAPI.getIndividual(miniOntology, new SimpleQuery(
										fillerName, null), true);
								if (atfInd == null) {
									throw new ContextException(
											"Bizarre error while updating cache: retrieved attentional focus " + fillerName
													+ " does not exist");
								}
								attentionalFocus.set(0, atfInd);
							}
						}
					}
				}
			}
		}

	}

	public void situationUpdate(List<MiniOntology.Individual> participants) {
		update(participants, situationIndividualCache, situationIntervalCache);
		// the properties need to be updated here, maybe.
	}

	public void discourseUpdate(List<MiniOntology.Individual> referenced) {
		update(referenced, discourseIndividualCache, discourseIntervalCache);
		// the properties need to be updated here, maybe.
	}

	public void resolve(String resolutionType, Map<Role, TypeConstraint> RDproperties, String slotType, String category,
			Resolution resolution) {
		List<Double> scores = new ArrayList<Double>();

		if (RDproperties != null) {
			TypeConstraint discourseParticipant = RDproperties.remove(DISCOURSEPARTICIPANTROLE);
			if (discourseParticipant != null) {
				resolution.scores = scores;
				scores.add(1.0);
				if (discourseParticipant == speakerTypeConstraint) {
					if (speaker.get(0) != null) {
						resolution.candidates = speaker;
						resolution.bestIndex = 0;
						return;
					}
				}
				else if (discourseParticipant == addresseeTypeConstraint) {
					if (addressee.get(0) != null) {
						resolution.candidates = addressee;
						resolution.bestIndex = 0;
						return;
					}
				}
				else if (discourseParticipant == attentionalFocusTypeConstraint) {
					if (attentionalFocus.get(0) != null) {
						resolution.candidates = attentionalFocus;
						resolution.bestIndex = 0;
						return;
					}
				}
				else if (discourseParticipant != discourseParticipantRoleTypeConstraint) {
					throw new ContextException("Unrecognized type given in discourse_participant_role :"
							+ discourseParticipant.getType());
				}
			}
		}
		if (resolutionType == ECGConstants.GIVEN || resolutionType == ECGConstants.IDENTIFIABLE) {
			// System.out.println("\tGIVEN or IDENTIFIABLE: slotType "+slotType+"   category  "+category+" features:"+RDproperties);
			List<MiniOntology.Individual> candidates = new ArrayList<MiniOntology.Individual>();
			if (category == null) {
				processCaches(slotType, candidates, scores, RDproperties);
			}
			else {
				processCaches(category, candidates, scores, RDproperties);
			}
			double sum = 0;
			for (int i = 0; i < scores.size(); i++) {
				sum = sum + scores.get(i);
			}
			int best = -1;
			double bestScore = 0;
			for (int i = 0; i < scores.size(); i++) {
				double score = scores.get(i) / sum;
				scores.set(i, score);
				if (score > bestScore) {
					best = i;
					bestScore = score;
				}
			}
			resolution.candidates = candidates;
			resolution.scores = scores;
			resolution.bestIndex = best;
			// System.out.println(scores);
		}
		else {
			throw new ContextException("Unknown resolutionType: " + resolutionType);
		}
	}

	private void processCaches(String type, List<MiniOntology.Individual> candidates, List<Double> scores,
			Map<Role, TypeConstraint> RDproperties) {
		if (miniOntology.subtype(type, MiniOntology.INTERVALNAME)) {
			processCache(type, situationIntervalCache, candidates, scores, RDproperties);
			processCache(type, discourseIntervalCache, candidates, scores, RDproperties);
		}
		else {
			processCache(type, situationIndividualCache, candidates, scores, RDproperties);
			processCache(type, discourseIndividualCache, candidates, scores, RDproperties);
		}
	}

	private <T extends MiniOntology.Individual> void processCache(String type, RecencyCache<T> rc,
			List<MiniOntology.Individual> candidates, List<Double> scores, Map<Role, TypeConstraint> RDproperties) {
		for (T i : rc.entries()) {
			if (miniOntology.subtype(i.getTypeName(), type) || type == null) {
				double score = 1;
				if (RDproperties != null) {
					score = scorer.computeSimilarityScore(RDproperties, properties.get(i));
				}
				if (score >= MATCH_THRESHOLD) {
					candidates.add(i);
					scores.add(score);
				}
			}
		}
	}

	public List<MiniOntology.Interval> matchInterval(TypeConstraint tc) {
		List<MiniOntology.Interval> matches = new ArrayList<MiniOntology.Interval>();
		for (MiniOntology.Interval interval : situationIntervalCache.entries()) {
			if (interval.getTypeName() == tc.getType()) {
				matches.add(interval);
			}
		}
		return matches;
	}

	public void clear() {
		situationIntervalCache.clear();
		situationIndividualCache.clear();
		discourseIntervalCache.clear();
		discourseIndividualCache.clear();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("ContextModelCache:\n\tSituation\n\t\t");
		sb.append(situationIndividualCache.toString()).append("\n\t\t").append(situationIntervalCache.toString());
		sb.append("\n\tDiscourse\n\t\t").append(discourseIndividualCache).append("\n\t\t").append(discourseIntervalCache);
		return sb.toString();
	}

}

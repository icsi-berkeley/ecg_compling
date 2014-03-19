package compling.context;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;

public abstract class ContextModelCache {
	ContextModel contextModel;
	MiniOntology miniOntology;
	private static Logger logger = Logger.getLogger(ContextModelCache.class.getName());

	ContextModelCache(ContextModel contextModel) {
		this.contextModel = contextModel;
		miniOntology = contextModel.getMiniOntology();
		if (miniOntology == null) {
			logger.warning("Null miniOntology in contextmodelcache");
		}
	}

	public abstract void situationUpdate(List<MiniOntology.Individual> participants);

	public abstract void discourseUpdate(List<MiniOntology.Individual> referenced);

	public abstract void resolve(String resolutionType, Map<Role, TypeConstraint> properties, String slotType,
			String category, Resolution resolution);

	public abstract List<MiniOntology.Interval> matchInterval(TypeConstraint tc);

	public abstract String toString();

	public abstract void clear();

}

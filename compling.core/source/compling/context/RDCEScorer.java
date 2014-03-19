package compling.context;

import java.util.Map;

import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;

public class RDCEScorer {

	public static interface Scorer {
		public double computeSimilarityScore(Map<Role, TypeConstraint> RDprops, Map<Role, TypeConstraint> CEProps);
	}

	public static class SillyScorer implements Scorer {
		public double computeSimilarityScore(Map<Role, TypeConstraint> RDprops, Map<Role, TypeConstraint> CEProps) {
			return 1.0;
		}
	}

	public static class BasicScorer implements Scorer {
		public double computeSimilarityScore(Map<Role, TypeConstraint> RDProps, Map<Role, TypeConstraint> CEProps) {
			try {
				int pDem = RDProps.size();
				int rDem = min(pDem, CEProps.size());
				if (rDem == 0 && pDem == 0) {
					return 1;
				}
				int numerator = 0;
				for (Role role : RDProps.keySet()) {
					TypeConstraint rdtc = RDProps.get(role);
					TypeConstraint cetc = CEProps.get(role);
					// System.out.println(role);
					// if (rdtc != null && cetc != null){System.out.println(rdtc.getType() + " "+cetc.getType());}
					// if (rdtc != null)
					if (rdtc != null && cetc != null && rdtc.getTypeSystem() == cetc.getTypeSystem()
							&& rdtc.getTypeSystem().subtype(cetc.getType(), rdtc.getType())) {
						numerator++;
					}
				}
				if (numerator == 0) {
					return 0;
				}

				double P = numerator / pDem;
				double R = numerator / rDem;
				return 2 * P * R / (P + R);
			}
			catch (TypeSystemException ce) {
				throw new ContextException(ce.toString());
			}
		}

		int min(int x, int y) {
			if (x < y) {
				return x;
			}
			return y;
		}

	}

}

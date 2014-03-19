package compling.parser.ecgparser;

/**
 * This is an container class for defining classes that can score a semspec
 * 
 * @author John Bryant
 * 
 **/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import compling.grammar.ecg.ECGGrammarUtilities;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.ecg.Prefs;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ParserException;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser;
import compling.util.Counter;
import compling.util.Pair;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;
import compling.util.fileutil.TextFileLineIterator;

public class SemSpecScorer {

	public static abstract class BasicScorer {

		abstract public double scoreSingleBinding(Set<Pair<TypeConstraint, Role>> frameRoles, TypeConstraint fillerType);

		public double scoreAllBindings(List<Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint>> bindings) {
			double cost = 0;
			for (Pair<Set<Pair<TypeConstraint, Role>>, TypeConstraint> binding : bindings) {
				cost = cost + scoreSingleBinding(binding.getFirst(), binding.getSecond());
			}
			return cost;
		}
	}

	/**
	 * This uses the harmonic mean to compute the average over the bindings. If this becomes the real scorer, it can be
	 * optimized.
	 */
	public static class BasicTableScorer extends BasicScorer {

		IdentityHashMap<TypeConstraint, Map<TypeConstraint, Counter<Role>>> params = new IdentityHashMap<TypeConstraint, Map<TypeConstraint, Counter<Role>>>();

		public double scoreSingleBinding(Set<Pair<TypeConstraint, Role>> frameRoles, TypeConstraint fillerType) {
			if (frameRoles == null) {
				return 0;
			}
			int numParams = 0;
			double denom = 0;
			for (Pair<TypeConstraint, Role> pair : frameRoles) {
				numParams++;
				try {

					denom = denom + 1 / params.get(pair.getFirst()).get(fillerType).getCount(pair.getSecond());
				}
				catch (NullPointerException npe) {
					numParams--;
					// System.out.println("Not bad, if intentional: Unfound param in BasicTableScorer: "+pair.toString()
					// + "  "+fillerType);
				}
			}
			if (numParams == 0) {
				return 0;
			}
			return Math.log(((double) numParams) / denom);
		}

		public BasicTableScorer(String fileName, TypeSystem<Schema> schemaTypeSystem, TypeSystem<?> externalTypeSystem)
				throws IOException {
			TextFileLineIterator lines = new TextFileLineIterator(fileName);
			int lineno = 0;
			while (lines.hasNext()) {
				String line = lines.next();
				StringTokenizer st = new StringTokenizer(line);
				if (st.countTokens() == 4) {
					String frameName = st.nextToken();
					TypeConstraint frame = schemaTypeSystem.getCanonicalTypeConstraint(frameName);
					String fillerType = st.nextToken();
					TypeConstraint filler = null;
					if (fillerType.charAt(0) == '@') {
						filler = externalTypeSystem.getCanonicalTypeConstraint(fillerType.substring(1));
					}
					else {
						filler = schemaTypeSystem.getCanonicalTypeConstraint(fillerType);
					}
					if (frame == null || filler == null) {
						throw new ParserException("Either unknown frame or filler type (" + frameName + ", " + fillerType
								+ " ) in param file: " + fileName + "at line num: " + lineno);
					}
					String roleName = st.nextToken();
					Role role = null;
					for (Role r : schemaTypeSystem.get(frameName).getAllRoles()) {
						if (roleName.equals(r.getName())) {
							role = r;
						}
					}
					if (role == null) {
						throw new ParserException("Unknown role name " + roleName + " in param file: " + fileName
								+ "at line num: " + lineno);
					}
					String doubleString = st.nextToken();
					try {
						Double param = new Double(doubleString);

						if (!params.containsKey(frame)) {
							params.put(frame, new IdentityHashMap<TypeConstraint, Counter<Role>>());
						}
						Map<TypeConstraint, Counter<Role>> frameTable = params.get(frame);
						if (!frameTable.containsKey(filler)) {
							frameTable.put(filler, new Counter<Role>());
						}
						Counter<Role> fillerTable = frameTable.get(filler);
						if (fillerTable.containsKey(role)) {
							throw new ParserException("Duplicate table entry in file: " + fileName + " at line number: "
									+ lineno + " line=\"" + line + "\"");
						}
						fillerTable.setCount(role, param);

					}
					catch (NumberFormatException n) {
						throw new ParserException("Bad number: " + doubleString + " in file: " + fileName
								+ " on line number " + lineno);
					}

				}
				else if (st.countTokens() > 0 && (st.countTokens() < 4 || st.countTokens() > 4)) {
					throw new ParserException("In file: " + fileName + " there is a bad param line: " + line);
				}
				lineno++;
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(
					"Semantic Param Table build by SemSpecScorer.BasicTableScorer\n-----------------------------------------------------\n");
			for (TypeConstraint frame : params.keySet()) {
				for (TypeConstraint filler : params.get(frame).keySet()) {
					for (Role role : params.get(frame).get(filler).keySet()) {
						sb.append("\t").append(frame).append(" ").append(filler).append(" ").append(role.getName())
								.append(" -> ").append(params.get(frame).get(filler).getCount(role)).append("\n");
					}
				}
			}
			return sb.toString();
		}
	}

	public static class ParamFileScorerFromCounts extends BasicScorer {

		public static final double COUNT_AS_ZERO_THRESHOLD = 1.0;

		IdentityHashMap<TypeConstraint, Map<Role, Counter<TypeConstraint>>> params = new IdentityHashMap<TypeConstraint, Map<Role, Counter<TypeConstraint>>>();
		Grammar grammar = null;
		boolean useCFGBackoff = true;

		public ParamFileScorerFromCounts(Grammar grammar, String paramFile, boolean useCFGbackoff) throws IOException {
			this(grammar, new TextFileLineIterator(paramFile), useCFGbackoff);
		}

		public ParamFileScorerFromCounts(Grammar grammar, TextFileLineIterator lineIterator, boolean useCFGbackoff) {

			this.grammar = grammar;
			this.useCFGBackoff = useCFGbackoff;

			Map<TypeConstraint, Map<Role, Counter<TypeConstraint>>> counts = new IdentityHashMap<TypeConstraint, Map<Role, Counter<TypeConstraint>>>();
			Map<TypeConstraint, Counter<TypeConstraint>> backoffCounts = new IdentityHashMap<TypeConstraint, Counter<TypeConstraint>>();

			while (lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == "") {
					continue;
				}
				ParamLineParser.ParamContainer pc = ParamLineParser.parseLine(line);
				Role role = getRole(grammar, pc.structureName, pc.role);
				TypeConstraint frame = grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(pc.structureName);
				if (role != null) {
					if (counts.get(frame) == null) {
						counts.put(frame, new IdentityHashMap<Role, Counter<TypeConstraint>>());
					}
					// update counts
					Counter<TypeConstraint> counter = new Counter<TypeConstraint>();
					counts.get(frame).put(role, counter);

					Counter<TypeConstraint> cfgCounter = null;
					if (useCFGbackoff) {
						TypeConstraint type = role.getTypeConstraint();
						if (!backoffCounts.containsKey(type)) {
							backoffCounts.put(type, new Counter<TypeConstraint>());
						}
						cfgCounter = backoffCounts.get(type);
					}

					for (Pair<String, Double> pair : pc.params) {
						String typeName = pair.getFirst();
						TypeConstraint fillerType = typeName.indexOf("@") == -1 ? grammar.getSchemaTypeSystem()
								.getCanonicalTypeConstraint(typeName) : grammar.getOntologyTypeSystem()
								.getCanonicalTypeConstraint(typeName.substring(1));
						if (fillerType != null) {
							counter.setCount(fillerType, pair.getSecond());
							if (useCFGbackoff) {
								cfgCounter.incrementCount(fillerType, pair.getSecond());
							}
						}
					}
				}
			}

			tabulate(counts, backoffCounts);
		}

		static Role getRole(Grammar grammar, String frame, String roleName) {
			Schema schema = grammar.getSchema(frame);
			if (schema == null)
				return null;
			for (Role role : schema.getAllRoles()) {
				if (role.getName().equals(roleName)) {
					return role;
				}
			}
			return null;
		}

		protected Map<TypeConstraint, Counter<TypeConstraint>> smoothCFGProb(
				Map<TypeConstraint, Counter<TypeConstraint>> backoffCounts, TypeConstraint roleType,
				List<TypeConstraint> unifiableSubtypes) {

			Map<TypeConstraint, Counter<TypeConstraint>> cfgProb = new IdentityHashMap<TypeConstraint, Counter<TypeConstraint>>();

			Counter<TypeConstraint> counter = backoffCounts.get(roleType);
			cfgProb.put(roleType, new Counter<TypeConstraint>());

			if (backoffCounts.containsKey(roleType)) {

				// smooth the CFG backoff table (particular to the unifiable type
				// constraints)
				double N = 0; // N = # observed tokens
				int T = 0; // T = # observed types

				for (TypeConstraint c : counter.keySet()) {
					T++;
					N += counter.getCount(c);
				}
				double V = unifiableSubtypes.size();
				double alpha = N == 0 ? 0.0 : N / (N + T);

				for (TypeConstraint subtype : unifiableSubtypes) {
					double smoothedProb = N == 0 ? 1 / V : alpha * counter.getCount(subtype) / N + (1 - alpha) * 1 / V;
					cfgProb.get(roleType).setCount(subtype, smoothedProb);
				}
			}
			else {
				for (TypeConstraint subtype : unifiableSubtypes) {
					cfgProb.get(roleType).setCount(subtype, 1.0 / unifiableSubtypes.size());
				}
			}
			return cfgProb;
		}

		protected List<TypeConstraint> getSubtypes(TypeConstraint type) {
			List<TypeConstraint> subtypes = new ArrayList<TypeConstraint>();
			try {
				for (String subtype : type.getTypeSystem().getAllSubtypes(type.getType())) {
					subtypes.add(type.getTypeSystem().getCanonicalTypeConstraint(subtype));
				}
			}
			catch (TypeSystemException tse) {
			}
			return subtypes;
		}

		protected void tabulate(Map<TypeConstraint, Map<Role, Counter<TypeConstraint>>> counts,
				Map<TypeConstraint, Counter<TypeConstraint>> backoffCounts) {

			for (Schema schema : grammar.getAllSchemas()) {

				TypeConstraint schemaType = grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(schema.getName());
				Map<Role, Counter<TypeConstraint>> map = new HashMap<Role, Counter<TypeConstraint>>();
				params.put(schemaType, map);

				for (Role role : schema.getAllRoles()) {

					if (role.getTypeConstraint() == null)
						continue;

					map.put(role, new Counter<TypeConstraint>());
					List<TypeConstraint> subtypes = getSubtypes(role.getTypeConstraint());
					TypeConstraint roleType = role.getTypeConstraint();

					Map<TypeConstraint, Counter<TypeConstraint>> cfgProb = smoothCFGProb(backoffCounts, roleType, subtypes);

					if (counts.containsKey(role)) {
						// backoff to uniform or cfg table if a particular filler type
						// has not been observed
						double N = 0;
						int T = 0;
						Counter<TypeConstraint> counter = counts.get(schemaType).get(role);

						for (TypeConstraint c : counter.keySet()) {
							if (subtypes.contains(c) && counter.getCount(c) > COUNT_AS_ZERO_THRESHOLD) {
								T++;
								N += counter.getCount(c);
							}
						}
						double V = subtypes.size();
						double alpha = N == 0 ? 0.0 : N / (N + T);

						for (TypeConstraint subtype : subtypes) {
							double smoothedProb;
							if (!useCFGBackoff) {
								smoothedProb = N == 0 ? 1 / V : alpha * counter.getCount(subtype) / N + (1 - alpha) * 1 / V;
							}
							else {
								smoothedProb = N == 0 ? cfgProb.get(roleType).getCount(subtype) : alpha
										* counter.getCount(subtype) / N + (1 - alpha) * cfgProb.get(roleType).getCount(subtype);
							}
							map.get(role).setCount(subtype, smoothedProb); // double
																							// check
																							// whether
																							// this
																							// should
																							// be
																							// logged
																							// or not
						}
					}
					else {
						// role has not been observed (special case where N = T = 0),
						// so just use CFG table
						for (TypeConstraint subtype : subtypes) {
							if (!useCFGBackoff) {
								map.get(role).setCount(subtype, 1.0 / subtypes.size());
							}
							else {
								map.get(role).setCount(subtype, cfgProb.get(roleType).getCount(subtype));
							}
						}
					}
				}

			}
		}

		public double scoreSingleBinding(Set<Pair<TypeConstraint, Role>> frameRoles, TypeConstraint fillerType) {
			if (frameRoles == null) {
				return 0;
			}
			int numParams = 0;
			double denom = 0;
			for (Pair<TypeConstraint, Role> pair : frameRoles) {
				try {
					denom = denom + 1 / params.get(pair.getFirst()).get(pair.getSecond()).getCount(fillerType);
					numParams++;
				}
				catch (NullPointerException npe) {
					// System.out.println("Not bad, if intentional: Unfound param in BasicTableScorer: "+pair.toString()
					// + "  "+fillerType);
				}
			}
			if (numParams == 0) {
				return 0;
			}
			return Math.log(((double) numParams) / denom);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(
					"Semantic Param Table build by ParamFileScorerFromCounts.\n-----------------------------------------------------\n");
			for (TypeConstraint frame : params.keySet()) {
				for (Role role : params.get(frame).keySet()) {
					sb.append(frame).append(".").append(role.getName()).append("\t");
					for (TypeConstraint filler : params.get(frame).get(role).keySet()) {
						sb.append(filler).append(":").append(params.get(frame).get(role).getCount(filler)).append("\t");
					}
					sb.append("\n");
				}
			}
			return sb.toString();
		}

	}

	public static void main(String[] args) throws IOException, TypeSystemException {
		Grammar grammar = ECGGrammarUtilities.read(args[0]);
		Prefs p = grammar.getPrefs();
		if (!(p instanceof AnalyzerPrefs)) {
			throw new ParserException("AnalyzerPrefs object expected");
		}
		AnalyzerPrefs prefs = (AnalyzerPrefs) p;

		if (prefs.getList(AP.GRAMMAR_PARAMS_PATHS).size() > 0) {
			String grammarParamsSemExt = prefs.getSetting(AP.GRAMMAR_PARAMS_SEM_EXTENSION) == null ? "sem" : prefs
					.getSetting(AP.GRAMMAR_PARAMS_SEM_EXTENSION);

			List<File> semParamFiles = FileUtils.getFilesUnder(prefs.getBaseDirectory(),
					prefs.getList(AP.GRAMMAR_PARAMS_PATHS), new ExtensionFileFilter(grammarParamsSemExt));

			boolean useBackoff = prefs.getSetting(AP.GRAMMAR_PARAMS_USE_CFGBACKOFF) == null ? true : Boolean.valueOf(prefs
					.getSetting(AP.GRAMMAR_PARAMS_USE_CFGBACKOFF));

			if (!semParamFiles.isEmpty()) {
				ParamFileScorerFromCounts scorer = new ParamFileScorerFromCounts(grammar, semParamFiles.get(0)
						.getAbsoluteFile().getAbsolutePath(), useBackoff);
				System.out.println(scorer);
			}
		}
	}

}

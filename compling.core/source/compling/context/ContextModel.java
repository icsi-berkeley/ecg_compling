package compling.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import compling.context.ContextUtilities.QueryResultPrinter;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileReadingUtils;
import compling.util.fileutil.FileUtils;

@SuppressWarnings("deprecation")
public class ContextModel {

	private MiniOntology miniOntology;
	private MiniOntologyReader mor;
	private Yylex scanner;
	private ContextModelCache cache;

	public ContextModel(File ontologyDirectory, String defFileExtension, String instFileExtension) {
		if (!ontologyDirectory.isDirectory()) {
			return;
		}
		try {
			StringBuffer ontologySpec = new StringBuffer();
			ontologySpec.insert(0, "DEFS: ");
			for (File ontFile : FileUtils.getFilesUnder(ontologyDirectory, new ExtensionFileFilter(defFileExtension))) {
				ontologySpec.append("\n");
				ontologySpec.append(FileReadingUtils.ReadFileIntoStringBuffer(ontFile));
			}
			ontologySpec.append(" INSTS: ");
			for (File ontFile : FileUtils.getFilesUnder(ontologyDirectory, new ExtensionFileFilter(instFileExtension))) {
				ontologySpec.append("\n");
				ontologySpec.append(FileReadingUtils.ReadFileIntoStringBuffer(ontFile));
			}

			instantiate(new BufferedReader(new StringReader(ontologySpec.toString())), "ontology definitions");

		}
		catch (IOException ioe) {
			throw new ContextException("Terminal Error: Cannot read ontology spec. ", ioe);
		}
	}

	public ContextModel(List<File> ontologySpecFiles, String defFileExtension, String instFileExtension) {
		this(ontologySpecFiles, defFileExtension, instFileExtension, Charset.defaultCharset());
	}

	public ContextModel(List<File> ontologySpecFiles, String defFileExtension, String instFileExtension, Charset charSet) {
		try {
			StringBuffer defs = new StringBuffer();
			StringBuffer insts = new StringBuffer();
			StringBuffer ontologySpec = new StringBuffer();

			defs.insert(0, "DEFS: ");
			insts.append(" INSTS: ");

			for (File ontFile : ontologySpecFiles) {
				if (ontFile.getName().endsWith("." + defFileExtension)) {
					defs.append("\n");
					defs.append(FileReadingUtils.ReadFileIntoStringBuffer(ontFile, charSet));
				}
				else if (ontFile.getName().endsWith("." + instFileExtension)) {
					insts.append("\n");
					insts.append(FileReadingUtils.ReadFileIntoStringBuffer(ontFile, charSet));
				}
			}
			ontologySpec.append(defs).append(insts);
			instantiate(new BufferedReader(new InputStreamReader(new StringBufferInputStream(ontologySpec.toString()),
					charSet)), "ontology definitions");

		}
		catch (IOException ioe) {
			throw new ContextException("Terminal Error: Cannot read ontology spec. ", ioe);
		}
	}

	public ContextModel(StringBuffer ontologySpec) {
		instantiate(new BufferedReader(new StringReader(ontologySpec.toString())), "ontology definitions");
	}

	public ContextModel(String ontologySpecFileName) {
		this(ontologySpecFileName, Charset.defaultCharset());
	}

	public ContextModel(String ontologySpecFileName, Charset charSet) {
		try {
			instantiate(new BufferedReader(new InputStreamReader(new FileInputStream(ontologySpecFileName), charSet)),
					ontologySpecFileName);
		}
		catch (IOException ioe) {
			throw new ContextException("Terminal Error: Cannot read ontology spec at " + ontologySpecFileName, ioe);
		}
	}

	private void instantiate(BufferedReader specFileReader, String fileName) {
		try {
			scanner = new Yylex(specFileReader);
			scanner.file = fileName;
			mor = new MiniOntologyReader(scanner);
			mor.file = fileName;
			mor.parse();
			miniOntology = mor.getMiniOntology();
			cache = new SimpleContextModelCache(this, 5, 12, 5, 12);
			cache.situationUpdate(miniOntology.getRecentlyAccessedIndividuals());
		}
		catch (Exception e) {
			// e.printStackTrace();
			throw new ContextException("Terminal Error: Cannot read ontology spec. ", e);
		}
	}

	public void updateOntology(StringBuffer commands) {
		commands.insert(0, "DEFS: INSTS: ");
		try {
			scanner.yyclose();
			scanner.yyreset(new BufferedReader(new StringReader(commands.toString())));
			mor.file = "update";
			mor.parse();
			cache.situationUpdate(miniOntology.getRecentlyAccessedIndividuals());
		}
		catch (Exception e) {
			throw new ContextException("Terminal Error: Cannot read ontology spec. ", e);
		}
	}

	public ContextModelCache getContextModelCache() {
		return cache;
	}

	public String retrieveIndividual(SimpleQuery simpleQuery) {
		return MiniOntologyQueryAPI.retrieveIndividual(miniOntology, simpleQuery);
	}

	public String retrieveIndividual(SimpleQuery simpleQuery, boolean queryAllIntervals) {
		return MiniOntologyQueryAPI.retrieveIndividual(miniOntology, simpleQuery, queryAllIntervals);
	}

	public List<HashMap<String, String>> query(SimpleQuery simpleQuery, boolean queryAllIntervals) {
		List<SimpleQuery> simpleQueries = new ArrayList<SimpleQuery>();
		simpleQueries.add(simpleQuery);
		return MiniOntologyQueryAPI.ask(miniOntology, simpleQueries, queryAllIntervals);
	}

	public List<HashMap<String, String>> query(List<SimpleQuery> simpleQueries) {
		return MiniOntologyQueryAPI.ask(miniOntology, simpleQueries);
	}

	public List<HashMap<String, String>> query(List<SimpleQuery> simpleQueries, boolean queryAllIntervals) {
		return MiniOntologyQueryAPI.ask(miniOntology, simpleQueries, queryAllIntervals);
	}

	public boolean test(SimpleQuery s) {
		return MiniOntologyQueryAPI.holds(miniOntology, s);
	}

	public boolean test(SimpleQuery s, String interval) {
		return MiniOntologyQueryAPI.holds(miniOntology, s, interval);
	}

	public void removeRelationFiller(String relnName, String holderName, String value) {
		miniOntology.removeRelationFiller(relnName, holderName, value);
	}

	public void removeAllRelationFillers(String relnName, String holderName) {
		miniOntology.removeAllRelationFillers(relnName, holderName);
	}

	public List<String> updateRecentDiscourseEntities(List<String> individuals) {
		List<MiniOntology.Individual> inds = new ArrayList<MiniOntology.Individual>();
		List<String> contextElements = new ArrayList<String>();

		for (String individual : individuals) {
			MiniOntology.Individual ind = MiniOntologyQueryAPI.getIndividual(miniOntology, new SimpleQuery(
					getIndividualName(individual), null), true);
			if (ind == null) {
				throw new ContextException("Error while updating recent discourse entities. " + individual
						+ " is not defined in the context model.");
			}
			inds.add(ind);
			contextElements.add(QueryResultPrinter.formatIndividual(ind));
		}
		cache.discourseUpdate(inds);
		return contextElements;
	}

	public List<String> updateRecentSituationEntities(List<String> individuals) {
		List<MiniOntology.Individual> inds = new ArrayList<MiniOntology.Individual>();
		List<String> contextElements = new ArrayList<String>();

		for (String individual : individuals) {
			MiniOntology.Individual ind = MiniOntologyQueryAPI.getIndividual(miniOntology, new SimpleQuery(
					getIndividualName(individual), null), true);
			if (ind == null) {
				ind = MiniOntologyQueryAPI.getIndividual(miniOntology,
						new SimpleQuery(getIndividualName(individual), null), true);
				throw new ContextException("Error while updating recent discourse entities. " + individual
						+ " is not defined in the context model.");
			}
			inds.add(ind);
			contextElements.add(QueryResultPrinter.formatIndividual(ind));
		}
		cache.situationUpdate(inds);
		return contextElements;
	}

	/***
	 * removes all existing intervals and individuals from the miniOntology and reinitialize with one BASE interval. Also
	 * clears the context model cache.
	 */
	public void reset() {
		miniOntology.resetMiniOntologyInstances();
		cache.clear();
	}

	public static boolean isTypedFiller(String i) {
		return QueryResultPrinter.isTypedFiller(i);
	}

	public static String getIndividualType(String i) {
		return QueryResultPrinter.getIndividualType(i);
	}

	public static String getIndividualName(String i) {
		return QueryResultPrinter.getIndividualName(i);
	}

	public boolean isInterval(String i) {
		return miniOntology.getInterval(i) != null;
	}

	public MiniOntology getMiniOntology() {
		return miniOntology;
	}

	public TypeSystem<? extends TypeSystemNode> getTypeSystem() {
		return miniOntology.getTypeSystem();
	}

	public static void main(String[] args) {
		System.out.println("ContextModel.main");
		ContextModel c = new ContextModel("evaontspec.txt");
		StringBuffer sb1 = new StringBuffer(
				"(inst Episode1 Episode BASE NONE fil (participant xixi) (participant mother)(object lotion1) (object rice1))");
		sb1.append("\n(setcurrentinterval Episode1)");
		sb1.append("\n(inst piggyback1 PiggyBackAction CURRENTINTERVAL NONE fil (piggybacker mother) (piggybackee xixi))");
		sb1.append("\n(setcurrentinterval piggyback1)");
		sb1.append("\n(fil location xixi \"sofa\")");
		c.updateOntology(sb1);
		System.out.println(c.getContextModelCache().toString());
		// c.removeRelationFiller("location", "xixi", "\"sofa\"");
		StringBuffer sb2 = new StringBuffer(
				"(inst speechact1 SpeechAct Episode1 CURRENTINTERVAL \n\t fil (speaker mother) (addressee xixi) (content \"eve walked\") (satype (inst RequestSpeechActType)))");
		sb2.append("\n(setcurrentinterval speechact1)");
		c.updateOntology(sb2);
		System.out.println(c.getContextModelCache().toString());
		List<SimpleQuery> qs = new ArrayList<SimpleQuery>();
		qs.add(new SimpleQuery("?x", "xixi", "?y"));
		MiniOntologyQueryAPI.printResult(c.miniOntology, qs);
		SimpleQuery typetrue = new SimpleQuery("xixi", c.getTypeSystem().getInternedString("Person"));
		SimpleQuery typefalse = new SimpleQuery("xixi", c.getTypeSystem().getInternedString("Episode"));
		System.out.println("Does my true simple typequery hold: " + typetrue + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, typetrue));
		System.out.println("Does my false simple typequery hold: " + typefalse + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, typefalse));
		SimpleQuery reltrue = new SimpleQuery("speaker", "speechact1", "mother");
		SimpleQuery reltruestring = new SimpleQuery("content", "speechact1", "\"eve walked\"");
		SimpleQuery relfalse = new SimpleQuery("speaker", "speechact1", "xixi");
		System.out.println("Does my true simple relqauery hold: " + reltrue + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, reltrue));
		System.out.println("Does my true simple relqauery hold: " + reltruestring + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, reltruestring));
		System.out.println("Does my false simple relquery hold: " + relfalse + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, relfalse));
		System.out.println("removing the speaker relation");
		c.removeAllRelationFillers("speaker", "speechact1");
		System.out.println("Retesting the now false query: " + reltrue + " is "
				+ MiniOntologyQueryAPI.holds(c.miniOntology, reltrue));
		List<SimpleQuery> qs2 = new ArrayList<SimpleQuery>();
		qs2.add(new SimpleQuery("?x", c.getTypeSystem().getInternedString("Person")));
		MiniOntologyQueryAPI.printResult(c.getMiniOntology(), qs2);

	}

}

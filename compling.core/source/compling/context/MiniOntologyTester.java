package compling.context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.grammar.ecg.ECGConstants;

class MiniOntologyTester {

	static List<String> stringToList(String parentListString) {
		StringTokenizer st = new StringTokenizer(parentListString);
		List<String> list = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

//	static void testFunction() {
//		MiniOntology m = new MiniOntology(true);
//		m.addType("Entity", stringToList(""));
//		m.addType("Agent", stringToList(""));
//		m.addType("PhysicalEntity", stringToList("Entity"));
//		m.addType("Animate", stringToList("PhysicalEntity Agent"));
//		m.addType("Person", stringToList("Animate"));
//		m.addType("Adult", stringToList("Person"));
//		m.addType("Name", stringToList(""));
//		m.addType("Situation", stringToList("Interval"));
//		m.addType("Event", stringToList("Interval"));
//		m.addRelation("Protagonist", "Event", "Entity");
//		m.addType("Action", stringToList("Event"));
//		m.addRelation("Actor", "Action", "Agent");
//		m.addCoindexation("Action", "Actor", "Protagonist");
//		m.addType("TransitiveAction", stringToList("Action"));
//		m.addRelation("Patient", "TransitiveAction", "Entity");
//		m.addType("SpeechAct", stringToList("TransitiveAction"));
//		m.addRelation("Utterance", "Situation", "SpeechAct", false);
//		m.addRelation("Participant", "Situation", "Entity", false);
//		m.addRelation("Speaker", "SpeechAct", "Person", true);
//		m.addRelation("Addressee", "SpeechAct", "Person", true);
//		m.addCoindexation("SpeechAct", "Speaker", "Actor");
//		m.addCoindexation("SpeechAct", "Patient", "Addressee");
//		m.addRelation("Location", "PhysicalEntity", "String", true);
//		m.addRelation("ContainingArea", "Interval", "String", true);
//		m.addRelation("First", "Name", "String", true);
//		m.addRelation("Last", "Name", "String", true);
//		m.addRelation("Name", "Person", "Name");
//		m.build();
//		m.addIndividual("mother", "Adult");
//		m.addIndividual("nomi", "Person");
//		m.addIndividual("father", "Adult");
//		m.addIndividual("investigator", "Adult");
//		m.addIndividual("speechAct1", "SpeechAct");
//		m.addIndividual("speechAct2", "SpeechAct");
//		m.addRelationFiller("Speaker", "speechAct1", "nomi");
//		m.addRelationFiller("Speaker", "speechAct2", "mother");
//		m.addRelationFiller("Addressee", "speechAct1", "mother");
//	}

	public static void main(String[] args) {
		// testFunction();
		MiniOntologyReader mor;
		String filename = args[0];
		MiniOntology m = null;
		Yylex scanner;
		try {
			scanner = new Yylex(new BufferedReader(new InputStreamReader(new FileInputStream(filename),
					ECGConstants.DEFAULT_ENCODING)));
			scanner.file = filename;
			mor = new MiniOntologyReader(scanner);
			mor.file = filename;
			mor.parse();
			m = mor.getMiniOntology();
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ContextException("Terminal Error: Cannot read grammar. ");
		}

		if (m != null) {
			List<SimpleQuery> queries = new ArrayList<SimpleQuery>();

			SimpleQuery s = new SimpleQuery("?x", "Entity");
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

			s = new SimpleQuery("?x", "Interval");
			queries = new ArrayList<SimpleQuery>();
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

			queries = new ArrayList<SimpleQuery>();
			s = new SimpleQuery("?z", "Human");
			queries.add(s);
			s = new SimpleQuery("?p", "?z", "?q");
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

			queries = new ArrayList<SimpleQuery>();
			s = new SimpleQuery("?z", "?y", "ktground");
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

			s = new SimpleQuery("?y", "Human");
			queries.add(s);

			MiniOntologyQueryAPI.printResult(m, queries);

			s = new SimpleQuery("?z", "?y", "?a");
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

			queries = new ArrayList<SimpleQuery>();
			s = new SimpleQuery("set_size", "?y", "?x");
			queries.add(s);
			MiniOntologyQueryAPI.printResult(m, queries);

		}
		else {
			System.out.println("null miniontology!");
		}

	}

}

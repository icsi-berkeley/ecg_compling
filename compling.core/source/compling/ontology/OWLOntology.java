package compling.ontology;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;

public class OWLOntology {

	protected OntModel model;
	protected String namespace;
	
	protected OWLOntology(OntModel model, String namespace) {
		this.model = model;
		this.namespace = namespace;
	}

	public TypeSystem<OWLTypeSystemNode> getTypeSystem() throws TypeSystemException {
		TypeSystem<OWLTypeSystemNode> ts = new TypeSystem<OWLTypeSystemNode>(ECGConstants.ONTOLOGY);
		for (Iterator<OntClass> i = model.listClasses(); i.hasNext(); ) {
			OntClass c = i.next();
			if (c.getLocalName() != null)
				ts.addType(new OWLTypeSystemNode(c));
		}
		ts.build();
		
		return ts;
	}
	
	public static OWLOntology fromPreferences(AnalyzerPrefs preferenceFile) throws IOException {
		String type = preferenceFile.getSetting(AP.ONTOLOGY_TYPE);
		if (! type.equalsIgnoreCase(AnalyzerPrefs.OWL_TYPE))
			throw new IllegalArgumentException(String.format("Invalid type %s in preference file", type));
		
		List<String> paths = preferenceFile.getList(AP.ONTOLOGY_PATHS);
		if (paths.size() > 1)
			throw new IllegalArgumentException(String.format("Preference file specifies more than one file: %s", paths));

		String ext = preferenceFile.getSetting(AP.ONTOLOGY_EXTENSIONS);
		if (ext == null)
			ext = "def inst ont owl";
		
		List<File> ontFiles = FileUtils.getFilesUnder(preferenceFile.getBaseDirectory(), paths, new ExtensionFileFilter(ext));
		assert ontFiles.size() == 1;
		
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		String path = ontFiles.get(0).getCanonicalPath();
		m.read("file:" + path);
		
		String ns = preferenceFile.getSetting(AP.ONTOLOGY_NAMESPACE);
		
		return new OWLOntology(m, ns);
	}
	
	public static void main(String[] args) throws IOException, TypeSystemException {
		OWLOntology o = OWLOntology.fromPreferences(new AnalyzerPrefs(args[0]));
//		System.out.println(o.getTypeSystem());
	}
}

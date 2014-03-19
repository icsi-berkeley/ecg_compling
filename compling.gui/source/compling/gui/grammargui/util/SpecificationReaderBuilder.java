/**
 * 
 */
package compling.gui.grammargui.util;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import compling.gui.AnalyzerPrefs;
import compling.gui.grammargui.builder.GrammarReader;
import compling.gui.grammargui.builder.OntologyReader;

/**
 * SpecificationReaderBuilder builds ISpecifactionReader objects. It tries to guess the right object by looking at the
 * extension.
 * 
 * @author lucag
 */
public class SpecificationReaderBuilder {
	private ResourceGatherer gatherer;
	private IProject project;

	/**
	 * @param gatherer
	 * @param project
	 */
	public SpecificationReaderBuilder(AnalyzerPrefs prefs, IProject project) {
		super();
		this.gatherer = new ResourceGatherer(prefs);
		this.project = project;
	}

	protected boolean isOntologyFile(IFile file) {
		for (IResource f : gatherer.getOntologyFiles(project))
			if (f.equals(file))
				return true;

		return false;
	}

	protected boolean isGrammarFile(IFile file) {
		for (File f : gatherer.getGrammarFiles())
			if (project.findMember(f.getPath()).equals(file))
				return true;
		return false;
	}

	public ISpecificationReader buildFrom(IFile file) {
		if (isGrammarFile(file))
			return new GrammarReader();
		else if (isOntologyFile(file))
			return new OntologyReader();
		else
			return null;
	}

//	public ISpecificationReader buildFrom(IFile file) {
//		String ontext = "inst def ont";
//		if (prefs.getSetting(AP.ONTOLOGY_EXTENSIONS) != null) {
//			ontext = prefs.getSetting(AP.ONTOLOGY_EXTENSIONS);
//		}
//		String[] exts = ontext.split(" ");
//		
//		String fileExt = file.getFileExtension();
//
//		if (fileExt != null) {
//			for (String ext : exts)
//				if (fileExt.equalsIgnoreCase(ext))
//					return new OntologyReader();
//		} 
//		else if (file.getName().startsWith("ont"))
//			return new OntologyReader();
//		
//		String grammarExts = "ecg cxn sch grm";
//		if (prefs.getSetting(AP.GRAMMAR_EXTENSIONS) != null)
//			grammarExts = prefs.getSetting(AP.GRAMMAR_EXTENSIONS);
//		
//		for(String ext : grammarExts.split(" "))
//			if (fileExt.equalsIgnoreCase(ext))
//				return new GrammarReader();
//		
//		return null;		
//	}
}

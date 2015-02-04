/**
 * 
 */
package compling.gui.grammargui.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.util.Utils;
import compling.util.Arrays;
import compling.util.fileutil.ExtensionFileFilter;
import compling.util.fileutil.FileUtils;

/**
 * <code>ResourceGatherer</code> uses a <code>Prefs</code> object to gather all and only the resources that are
 * necessary for reading and instantiating a <code>Grammar</code>.
 * 
 * It encapsulates the same behavior in {@link compling.grammar.ecg.ECGGrammarUtilities#read(AnalyzerPrefs preferences)}
 * 
 * @author lucag
 */
public class ResourceGatherer {

	public static final String DEFAULT_ONTOLOGY_EXTENSIONS = "def inst ont";
	public static final String DEFAULT_GRAMMAR_EXTENSIONS = "ecg cxn sch grm";

	private AnalyzerPrefs preferences;
	private File base;

	/**
	 * @param preferences
	 */
	public ResourceGatherer(AnalyzerPrefs preferences) {
		this.preferences = preferences;
		this.base = preferences.getBaseDirectory();
	}

	public String getOntologyExtensions() {
		List<String> exts = preferences.getList(AP.ONTOLOGY_EXTENSIONS);
		if (exts == null || exts.isEmpty()) {
			return DEFAULT_ONTOLOGY_EXTENSIONS;
		}
		return Arrays.join(exts.toArray(), " ");
	}

	public String getGrammarExtensions() {
		String exts = preferences.getSetting(AP.GRAMMAR_EXTENSIONS);
		if (exts == null) {
			return DEFAULT_GRAMMAR_EXTENSIONS;
		}
		return exts;
	}

	/**
	 * @return A List of File objects whose path is absolute
	 */
	public List<File> getOntologyFiles() {
		List<String> paths = preferences.getList(AP.ONTOLOGY_PATHS);
		return FileUtils.getFilesUnder(base, paths, new ExtensionFileFilter(getOntologyExtensions()));
	}

	public List<IResource> getOntologyFiles(IProject project) {
		List<IResource> newList = new ArrayList<IResource>();
		for (File f : getOntologyFiles())
			newList.add(project.findMember(Utils.getRelativeTo(f, preferences.getBaseDirectory()).getPath()));

		return newList;
	}
	
	/** Returns a list of File objects whose path is specified in this prefs' base. These are files whose contents
	 * will be imported by the grammar using the package/import system.
	 */
	public List<File> getImportFiles() {
		List<String> importPaths = preferences.getList(AP.IMPORT_PATHS);
		ArrayList<File> files = new ArrayList<File>();
		List<File> filesUnder = FileUtils.getFilesUnder(base, importPaths, new ExtensionFileFilter(
				getGrammarExtensions()));
		for (File f : filesUnder)
			files.add(Utils.getRelativeTo(f, base));
		return files;
	}
	
	public List<List<File>> getImportFilesDir() {
		List<String> importPaths = preferences.getList(AP.IMPORT_PATHS);
		List<List<File>> fileList = new ArrayList<List<File>>();
		for (String path : importPaths) {
			List<File> newFiles = new ArrayList<File>();
			List<String> test = new ArrayList<String>();
			test.add(path);
			List<File> filesUnder = FileUtils.getFilesUnder(base, test, new ExtensionFileFilter(
					getGrammarExtensions()));
			for (File f : filesUnder)
				newFiles.add(Utils.getRelativeTo(f, base));
			fileList.add(newFiles);
		}
		return fileList;
	}

	/**
	 * @return A List of File objects whose path is relative to this prefs' base
	 */
	public List<File> getGrammarFiles() {
		List<String> grammarPaths = preferences.getList(AP.GRAMMAR_PATHS);
		ArrayList<File> files = new ArrayList<File>();
		List<File> filesUnder = FileUtils.getFilesUnder(base, grammarPaths, new ExtensionFileFilter(
				getGrammarExtensions()));
		for (File f : filesUnder)
			files.add(Utils.getRelativeTo(f, base));
		return files;
	}

	public static void main(String[] args) throws IOException {
		ResourceGatherer resourceGatherer = new ResourceGatherer(new AnalyzerPrefs(args[0]));
		System.out.println(resourceGatherer.getOntologyFiles());
		System.out.println(resourceGatherer.getGrammarFiles());
	}
}

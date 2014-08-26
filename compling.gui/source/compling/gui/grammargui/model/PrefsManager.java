package compling.gui.grammargui.model;

import static java.lang.String.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.widgets.Display;

import compling.context.MiniOntology;
import compling.grammar.GrammarException;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ECGGrammarUtilities.ECGGrammarFormatter;
import compling.grammar.ecg.ECGGrammarUtilities.SimpleGrammarPrinter;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Primitive;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.gui.grammargui.Application;
import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.builder.GrammarNature;
import compling.gui.grammargui.util.GrammarBrowserTextPrinter;
import compling.gui.grammargui.util.IProxy;
import compling.gui.grammargui.util.ISpecificationReader;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.ModelChangedEventManager;
import compling.gui.grammargui.util.SpecificationReaderBuilder;
import compling.parser.ecgparser.ECGAnalyzer;

/**
 * PrefsManager insulates the application from the analayzer's details.
 * 
 * @see AnalyzerPrefs
 * @see Grammar
 * @author lucag
 */
public class PrefsManager implements IResourceChangeListener, ISaveParticipant {

	public static QualifiedName PREFS_PROPKEY = new QualifiedName(Application.PLUGIN_ID, "prefsFile");

	private static PrefsManager instance;

	private Grammar grammar;
	private AnalyzerPrefs prefs;
	private ECGAnalyzer analyzer = null;
	private ECGGrammarFormatter linkFormatter = new GrammarBrowserTextPrinter();
	private ECGGrammarFormatter textFormatter = new SimpleGrammarPrinter();
	private Set<AnalyzerSentence> sentences;
	private ModelChangedEventManager eventManager = new ModelChangedEventManager();
	private IProject project;
	private IWorkspace workspace;
	private NullProgressMonitor nullProgressMonitor;
	private IPath prefsPath;

	private Set<String> symbols;
	private HashMap<String, IFile> nodeToFileMap;
	private Map<String, TypeSystemNode> nameToNode;

	private static final int BUFFER_LEN = 1024;

	private PrefsManager() {
		project = null;
		workspace = ResourcesPlugin.getWorkspace();
		workspace.getDescription().setAutoBuilding(false);
		workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		nullProgressMonitor = new NullProgressMonitor();

		Grammar.setFormatter(linkFormatter);
	}

	/**
	 * @return the nullProgressMonitor
	 */
	public NullProgressMonitor getNullProgressMonitor() {
		return nullProgressMonitor;
	}

	@Override
	public String toString() {
		return "PrefsManager [prefsPath=" + prefsPath + "]";
	}

	/**
	 * @return the project
	 */
	public IProject getProject() {
		return project;
	}

	public void checkGrammar() {
		resetTables();
		SafeRunner.run(new SafeRunnable() {
			public void run() throws Exception {
				project.build(IncrementalProjectBuilder.FULL_BUILD, nullProgressMonitor);
			}
		});
	}

	private void resetTables() {
		nodeToFileMap = null;
		symbols = null;
		nameToNode = null;
	}

	/**
	 * @param nullProgressMonitor
	 *           the nullProgressMonitor to set
	 */
	public void setNullProgressMonitor(NullProgressMonitor nullProgressMonitor) {
		this.nullProgressMonitor = nullProgressMonitor;
	}

	public static PrefsManager getDefault() {
		if (instance == null)
			instance = new PrefsManager();

		return instance;
	}

	public static void shutdown() {
		if (instance != null && instance.workspace != null)
			instance.workspace.removeResourceChangeListener(instance);

		// TODO: save prefs file here.
	}

	public synchronized void updateGrammar() {
		grammar = null;
		eventManager.fireModelChanged(this, new IProxy() {
			public Object get() {
				return getGrammar();
			}
		});
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (prefsPath == null)
			return;

		final IFile prefsFile = project.getFile(prefsPath.lastSegment());
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					StringBuilder b = new StringBuilder();
					switch (delta.getKind()) {
					case IResourceDelta.ADDED:
						b.append("Added");
						break;
					case IResourceDelta.REMOVED:
						b.append("Removed");
						if (delta.getResource().equals(prefsFile)) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									try {
										setPreferences(null);
									} catch (CoreException e) {
										Log.logError(e, "should not be here");
									}
								}
							});
							return false;
						}
						break;
					case IResourceDelta.CHANGED:
						b.append("Changed");
						break;
					default:
						b.append(String.format("[%d]", delta.getKind()));
						break;
					}
					b.append(String.format(": %s", delta.getResource()));
					// System.out.println(b.toString());
					return true;
				}
			});
		} catch (CoreException e) {
			Log.logError(e, "resourceChanged");
		}
	}

	/**
	 * @param listener
	 * @see ModelChangedEventManager#addModelChangeListener(IModelChangedListener)
	 */
	public void addModelChangeListener(IModelChangedListener listener) {
		eventManager.addModelChangeListener(listener);
	}

	/**
	 * Lazy method.
	 * 
	 * @return the analyzer
	 * @throws IOException
	 */
	public ECGAnalyzer getAnalyzer() {
		if (prefs == null)
			throw new IllegalStateException();

		if (analyzer == null) {
			try {
				analyzer = new ECGAnalyzer(getGrammar(), prefs);
			} catch (IOException e) {
				e.printStackTrace();
				Log.logError(e, "impossible to get analyzer from %s", prefs);
				analyzer = null;
			}
		}
		return analyzer;
	}

	public void updateNodeMap(Collection<? extends TypeSystemNode> nodes, IFile file) {
		nodeToFileMap = new HashMap<String, IFile>();
		for (TypeSystemNode n : nodes)
			nodeToFileMap.put(n.getType(), file);
	}

	private IFile mapFileNameToResource(String fileName) {
		return (IFile) project.findMember(fileName);
	}

	public IFile getFileFor(TypeSystemNode node) {
		if (node instanceof Primitive) {
			String fileName = ((Primitive) node).getLocation().getFile();
			Assert.isTrue(fileName != null, format("fileName for %s is null!", node));
			return mapFileNameToResource(fileName);
		} else if (node instanceof MiniOntology.Type) {
			if (nodeToFileMap == null) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, nullProgressMonitor);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
				Assert.isNotNull(nodeToFileMap);
			}
			IFile file = nodeToFileMap.get(node.getType());
			if (file != null)
				return file;
		} else {
			// FIXME: This is a terrible hack...
			String fileName = prefs.getList(AP.ONTOLOGY_PATHS).get(0);
			if (fileName != null)
				return mapFileNameToResource(fileName);
		}
		return null;
	}

	/**
	 * @return the grammar associated with the current GrammarPrefs
	 */
	public synchronized final Grammar getGrammar() {
		if (grammar == null && prefs != null) {
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE, nullProgressMonitor);
				project.build(IncrementalProjectBuilder.FULL_BUILD, null);
			} catch (GrammarException e) {
				grammar = null;
				Log.logError(e, "Problem reading grammar %s", prefs);
			} catch (CoreException e) {
				grammar = null;
				Log.logError(e, "Problem building grammar %s", prefs);
			}
		}
		return grammar;
	}

	/**
	 * @param grammar
	 *           the grammar to set
	 */
	public void setGrammar(Grammar grammar) {
		this.grammar = grammar;
		this.analyzer = null;
		eventManager.fireModelChanged(this, new IProxy() {
			public Object get() {
				return getGrammar();
			}
		});
	}

	public ISpecificationReader getParserFor(IFile file) {
		if (prefs == null)
			throw new IllegalStateException();

		return new SpecificationReaderBuilder(prefs, project).buildFrom(file);
	}

	/**
	 * @return the sentences defined in the local GrammarPrefs object.
	 */
	public Set<AnalyzerSentence> getSentences() {
		if (prefs == null)
			return null;

		if (sentences == null) {
			sentences = new HashSet<AnalyzerSentence>();
			for (String s : prefs.getList(AP.EXAMPLE_SENTENCES))
				sentences.add(new AnalyzerSentence(s, this));
		}
		return sentences;
	}

	public void addSentence(AnalyzerSentence sentence) {
		if (prefs == null)
			throw new IllegalStateException("preference not set");

		if (sentence == null)
			throw new IllegalArgumentException("sentence cannot be null");

		if (sentences == null)
			sentences = new HashSet<AnalyzerSentence>();

		Log.logInfo("addSentence: sentence: %s\n", sentence);
		Log.logInfo("addSentence: sentences: %s\n", sentences);

		if (! sentences.contains(sentence)) {
			sentences.add(sentence);
			prefs.getList(AP.EXAMPLE_SENTENCES).add(sentence.getText());

			eventManager.fireModelChanged(this, new AnalyzerSentence[] { sentence }, new AnalyzerSentence[] { /* none */});
		}
	}

	public void removeSentence(AnalyzerSentence sentence) {
		if (prefs == null)
			throw new IllegalStateException("preference not set");

		if (sentences == null)
			throw new IllegalStateException("no sentences present");

		if (sentence == null)
			throw new IllegalArgumentException("sentence cannot be null");

		if (sentences.contains(sentence)) {
			sentences.remove(sentence);
			prefs.getList(AP.EXAMPLE_SENTENCES).remove(sentence.getText());

			eventManager.fireModelChanged(this, new AnalyzerSentence[0], new AnalyzerSentence[] { sentence });
		} else
			Log.logInfo("PrefsManager.removeSentence: sentence \"%s\" not present", sentence.getText());
	}

	public void removeAllSentences() {
		if (prefs == null)
			throw new IllegalStateException("preference not set");

		if (sentences == null)
			throw new IllegalStateException("no sentences present");

		AnalyzerSentence[] removed = new AnalyzerSentence[sentences.size()];
		sentences.toArray(removed);
		
		eventManager.fireModelChanged(this, new AnalyzerSentence[0], removed);
	}
	
	public String getContentAsText(String nodeName) {
		if (grammar == null)
			return null;

		if (nameToNode == null) {
			nameToNode = new HashMap<String, TypeSystemNode>();
			nameToNode.putAll(getGrammar().getAllConstructionsByName());
			for (TypeSystemNode n : getGrammar().getAllSchemas())
				nameToNode.put(n.getType(), n);
			TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = getGrammar().getOntologyTypeSystem();
			if (ontologyTypeSystem != null)
				for (TypeSystemNode n : ontologyTypeSystem.getAllTypes())
					nameToNode.put(n.getType(), n);
		}
		TypeSystemNode node = nameToNode.get(nodeName);
		return node != null ? getContentAsText(node) : null;
	}

	public String getContentAsText(TypeSystemNode node) {
		Grammar.setFormatter(textFormatter);
		String text;
		try {
			text = node.toString();
		} catch (NullPointerException e) {
			return null;
		} finally {
			Grammar.setFormatter(linkFormatter);
		}
		return text;
	}

	public Set<String> getSymbols() {
		if (prefs == null)
			throw new IllegalStateException();

		if (symbols == null) {
			symbols = new HashSet<String>();
			for (Construction c : getGrammar().getAllConstructions())
				symbols.add(c.getName());
			for (Schema s : getGrammar().getAllSchemas())
				symbols.add(s.getName());
			TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = getGrammar().getOntologyTypeSystem();
			if (ontologyTypeSystem != null)
				for (TypeSystemNode t : ontologyTypeSystem.getAllTypes())
					symbols.add(t.getType());
		}
		return symbols;
	}

	/**
	 * @param symbols
	 *           the symbols to set
	 */
	public void setSymbols(Set<String> symbols) {
		this.symbols = symbols;
	}

	/**
	 * @param listener
	 * @see compling.gui.grammargui.util.ModelChangedEventManager#removeModelChangeListener(compling.gui.grammargui.model.IModelChangedListener)
	 */
	public void removeModelChangeListener(IModelChangedListener listener) {
		eventManager.removeModelChangeListener(listener);
	}

	public void setPreferences(String prefsFileName) throws CoreException {
		if (prefsFileName != null) {
			try {
				prefsPath = new Path(prefsFileName);
				if (!prefsPath.isAbsolute())
					new IllegalArgumentException(String.format("Argument %s must be absolute", prefsFileName));
				this.prefs = new AnalyzerPrefs(prefsPath.toOSString(), Charset.forName(ECGConstants.DEFAULT_ENCODING));
			} catch (IOException e) {
				IStatus s = new Status(IStatus.ERROR, EcgEditorPlugin.PLUGIN_ID, "unable to read preferences", e);
				Log.log(s);
				throw new CoreException(s);
			}
		} else {
			prefs = null;
			prefsPath = null;
			if (project != null) {
				project.close(nullProgressMonitor);
				project = null;
			}
		}

		grammar = null;
		analyzer = null;
		sentences = null;
		nodeToFileMap = null;
		nameToNode = null;

		if (prefs != null)
			setupProject();

		eventManager.fireModelChanged(this, new IProxy() {
			public Object get() {
				return getGrammar();
			}
		});
	}

	/**
	 * @return the grammarPrefs
	 * @throws IOException
	 */
	public AnalyzerPrefs getPreferences() throws CoreException {
		if (project != null) {
			String prefsName = String.format("%s.prefs", project.getName());
			IFile prefsFile = project.getFile(prefsName);
			try {
				prefs = new AnalyzerPrefs(prefsFile.getRawLocation().toOSString(), Charset.forName(ECGConstants.DEFAULT_ENCODING));
			} catch (IOException e) {
				IStatus s = new Status(IStatus.ERROR, EcgEditorPlugin.PLUGIN_ID, "problem reading preferences");
				Log.log(s);
				throw new CoreException(s);
			}
		}
		return prefs;
	}

	protected void setupProject() {
		try {
			String prefsName = prefsPath.lastSegment();
			String projectName = prefsName.substring(0, prefsName.indexOf('.'));

			project = workspace.getRoot().getProject(projectName);
			if (!project.exists())
				project.create(nullProgressMonitor);

			if (!project.isOpen())
				project.open(nullProgressMonitor);

			project.getFile(".project").setHidden(true);

			IProjectDescription description = project.getDescription();
			List<String> ids = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
			if (ids.indexOf(GrammarNature.NATURE_ID) == -1) {
				ids.add(GrammarNature.NATURE_ID);
				description.setNatureIds(ids.toArray(new String[ids.size()]));
				project.setDescription(description, null);
			}
			IFile prefsFile = project.getFile(prefsName);
			if (!prefsFile.exists())
				prefsFile.createLink(prefsPath, IResource.FILE, nullProgressMonitor);
			IPath prefsBase = prefsPath.removeLastSegments(1);
			for (String p : prefs.getList(AP.GRAMMAR_PATHS)) {
				IFolder folder = project.getFolder(p);
				if (!folder.exists())
					folder.createLink(prefsBase.append(p).makeAbsolute(), IResource.NONE, nullProgressMonitor);
			}
		} catch (CoreException e) {
			Log.logError(e, "Problem setting up project");
		}
	}

	public void doneSaving(ISaveContext context) {
		EcgEditorPlugin plugin = EcgEditorPlugin.getDefault();

		// Delete the old saved state since it is not necessary anymore
		int previous = context.getPreviousSaveNumber();
		String oldFileName = String.format("save-%d", previous);
		File f = plugin.getStateLocation().append(oldFileName).toFile();
		f.delete();
	}

	public void prepareToSave(ISaveContext context) throws CoreException {
		// Nothing to do
	}

	public void rollback(ISaveContext context) {
		EcgEditorPlugin plugin = EcgEditorPlugin.getDefault();

		// Delete the old saved state since it is not necessary anymore
		int current = context.getSaveNumber();
		String oldFileName = String.format("save-%d", current);
		File f = plugin.getStateLocation().append(oldFileName).toFile();
		f.delete();
	}

	public void saving(ISaveContext context) throws CoreException {
		switch (context.getKind()) {
		case ISaveContext.FULL_SAVE:
			EcgEditorPlugin plugin = EcgEditorPlugin.getDefault();

			// save the plug-in state
			int current = context.getSaveNumber();
			String fileName = String.format("save-%d", current);

			// if we fail to write, an exception is thrown and we do not update the
			// path
			try {
				Writer w = new FileWriter(plugin.getStateLocation().append(fileName).toFile());
				writeStateTo(w);
				w.close();
			} catch (IOException e) {
				throw new CoreException(new Status(Status.WARNING, "Status save failed", EcgEditorPlugin.PLUGIN_ID, e));
			}
			context.map(new Path("save"), new Path(fileName));
			context.needSaveNumber();
			break;
		case ISaveContext.PROJECT_SAVE:
			// // get the project related to this save operation
			// IProject project = context.getProject();
			// // save its information, if necessary
			break;
		case ISaveContext.SNAPSHOT:
			// This operation needs to be really fast because
			// snapshots can be requested frequently by the
			// workspace.
			break;
		}
	}

	private void writeStateTo(Writer writer) throws IOException {
		if (prefsPath != null)
			writer.write(prefsPath.toOSString());
	}

	public void readStateFrom(Reader reader) throws Exception {
		char[] buffer = new char[BUFFER_LEN];
		int read = reader.read(buffer);
		if (read > 0)
			setPreferences(String.valueOf(buffer, 0, read));
	}

}

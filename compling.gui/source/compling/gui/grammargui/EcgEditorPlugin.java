package compling.gui.grammargui;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.OutlineElementType;

/**
 * The activator class controls the plug-in life cycle
 */
public class EcgEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "compling.gui";

	// The shared instance
	private static EcgEditorPlugin plugin;

	/**
	 * The constructor
	 */
	public EcgEditorPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		//Log.consoleLog("Plug-in %s starting: %s", PLUGIN_ID, context.toString());
		
//		Display.getDefault().asyncExec(new Runnable() {
//			public void run() {
//				initImageRegistry();
//			}
//		});

		PrefsManager manager = PrefsManager.getDefault();
		ISavedState lastState = ResourcesPlugin.getWorkspace().addSaveParticipant(PLUGIN_ID, manager);
		if (lastState == null)
			return;
		IPath location = lastState.lookup(new Path("save"));
		if (location == null)
			return;

		File file = getStateLocation().append(location).toFile();
		try {
			Reader r = new FileReader(file);
			manager.readStateFrom(r);
			r.close();
		}
		catch (Exception e) {
			Log.logError(e, "Most likely error: file %s not found", file);
			manager.setPreferences(null);
		}
	}

	final void initImageRegistry() {
		ImageRegistry r = getImageRegistry();
		for (OutlineElementType t : OutlineElementType.values()) {
			final String path = t.getPath();
			r.put(path, imageFor(path));
		}
	}

	private final Image imageFor(String key) {
		return imageDescriptorFromPlugin(PLUGIN_ID, key).createImage();
	}

	protected FormColors formColors;

	public FormColors getFormColors(Display display) {
		if (formColors == null) {
			formColors = new FormColors(display);
			formColors.markShared();
		}
		return formColors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
	 */
	public void stop(BundleContext context) throws Exception {
		PrefsManager.shutdown();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static EcgEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *           the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

}

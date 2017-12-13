package compling.gui.grammargui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import compling.gui.grammargui.ui.views.DebugConsole;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.Log;

@SuppressWarnings({ "restriction", "deprecation" })
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID =
//		"compling.gui.grammargui.perspectives.default";
	"compling.gui.grammargui.perspectives.Analysis";

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	@Override
	public boolean preShutdown() {
		final MultiStatus status = new MultiStatus(Application.PLUGIN_ID, 0, "Saving Workspace", null);
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					IWorkspace ws = ResourcesPlugin.getWorkspace();
					status.merge(ws.save(true, monitor));
				}
				catch (CoreException e) {
					status.merge(e.getStatus());
				}
			}
		};
		try {
			new ProgressMonitorDialog(null).run(false, false, runnable);
		}
		catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!status.isOK())
			Log.log(status);

		return super.preShutdown();
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	private void declareLocalImages(IWorkbenchConfigurer configurer) {
		configurer.declareImage(IImageKeys.SENTENCE_VALID,
				AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.SENTENCE_VALID), true);
		configurer.declareImage(IImageKeys.SENTENCE_INVALID,
				AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.SENTENCE_INVALID), true);
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		configurer.setSaveAndRestore(true);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.EDITOR_MINIMUM_CHARACTERS, 13);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.VIEW_MINIMUM_CHARACTERS, 13);

		IDE.registerAdapters();

		// Display.getDefault().asyncExec(new Runnable() {
		// public void run() {
		EcgEditorPlugin.getDefault().initImageRegistry();
		// }
		// });

		declareLocalImages(configurer);

		final String ICONS_PATH = "icons/full/";
		final String PATH_OBJECT = ICONS_PATH + "obj16/";
		Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);
		declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT, PATH_OBJECT + "prj_obj.gif", true);
		declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, PATH_OBJECT
				+ "cprj_obj.gif", true);

		// Problems view
		declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEM_CATEGORY,
				"icons/img1.gif", true);
		declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, "icons/img2.gif",
				true);
		declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WARNING_PATH, "icons/img3.gif",
				true);
		declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INFO_PATH, "icons/img4.gif",
				true);

		final String banner = "/icons/full/wizban/saveas_wiz.png";
		URL url = ideBundle.getEntry(banner);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		configurer.declareImage(IDEInternalWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG, desc, true);

		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { new DebugConsole() });
	}

	private void declareWorkbenchImage(IWorkbenchConfigurer configurer, Bundle ideBundle, String symbolicName,
			String path, boolean shared) {
		URL url = ideBundle.getEntry(path);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		configurer.declareImage(symbolicName, desc, shared);
	}

	@Override
	public IAdaptable getDefaultPageInput() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}

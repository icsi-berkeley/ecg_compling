package compling.gui.grammargui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.grammargui.ui.editors.MultiPageConstructionEditor;
import compling.gui.grammargui.ui.views.TypeSystemTreeView;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	@Override
	public void postWindowCreate() {
		super.postWindowCreate();
		for (IWorkbenchPage page : getWindowConfigurer().getWindow().getPages()) {
			page.hideActionSet("org.eclipse.ui.edit.text.actionSet.openExternalFile");
			page.hideActionSet("org.eclipse.ui.WorkingSetActionSet");
			page.hideActionSet("org.eclipse.update.ui.softwareUpdates");
		}
	}

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	private IWorkbenchPage getPage() {
		IWorkbenchPage[] pages = getWindowConfigurer().getWindow().getPages();
		Assert.isTrue(pages.length == 1);
		return pages[0];
	}

	private static TypeSystemEditorInput getInput(IWorkbenchPart part) {
		if (part instanceof MultiPageConstructionEditor)
			return (TypeSystemEditorInput) ((IEditorPart) part).getEditorInput();
		return null;
	}

	private IPartListener partListener = new IPartListener() {

		public void partActivated(IWorkbenchPart part) {
			TypeSystemEditorInput input = getInput(part);
			if (input != null) {
				final TypeSystemNode node = input.getTypeSystemNode();
				final String viewId = TypeSystemTreeView.getViewIdFor(node);
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						TypeSystemTreeView viewPart = (TypeSystemTreeView) getPage().findView(viewId);
						if (viewPart != null) {
							viewPart.setSelection(node);
							getPage().bringToTop(viewPart);
						}
					}
				});
			}
		}

		public void partBroughtToTop(IWorkbenchPart part) {
			partActivated(part);
		}

		public void partOpened(IWorkbenchPart part) {
			partActivated(part);
		}

		public void partClosed(IWorkbenchPart part) {
			// Nothing to do
		}

		public void partDeactivated(IWorkbenchPart part) {
			// Nothing to do
		}

	};

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 600));
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("ECG Workbench");

		// FIXME: Take care of this one!
//		configurer.getWindow().getPartService().addPartListener(partListener);

//		URL entry = Platform.getBundle(Application.PLUGIN_ID).getEntry("icons");
//		URL fileURL = null;
//		try {
//			fileURL = FileLocator.toFileURL(entry);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.printf("/: %s\n", fileURL);
	}

	@Override
	public void dispose() {
		getWindowConfigurer().getWindow().getPartService().removePartListener(partListener);
		partListener = null;
		super.dispose();
	}

}

package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;


/**
 * This is an action template for actions that toggle whether
 * it links its selection to the active editor.
 *
 * @since 3.0
 */
public abstract class AbstractToggleLinkingAction extends Action {

	/**
	 * Constructs a new action.
	 */
	public AbstractToggleLinkingAction() {
		super("Link with Editor");
		
		setDescription("Link with editor");
		setToolTipText("Link this outline with the selection change in the current editor.");
		
//		JavaPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$
		
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);
		setChecked(false);
	}

	/**
	 * Runs the action.
	 */
	@Override
	public abstract void run();
}

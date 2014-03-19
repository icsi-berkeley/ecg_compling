package compling.gui.grammargui;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import compling.gui.grammargui.ui.actions.CheckGrammarAction;
import compling.gui.grammargui.ui.actions.CloseFileAction;
import compling.gui.grammargui.ui.actions.UpdateAction;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the actions added to a workbench window.
 * Each window will be populated with new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	/*
	 * Actions - important to allocate these only in makeActions, and then use them in the fill methods. This ensures
	 * that the actions aren't recreated when fillActionBars is called with FILL_PROXY.
	 */
	private IWorkbenchAction aboutAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	/**
	 * Registers the action as global action and registers it for disposal.
	 * 
	 * @param action
	 *           the action to register
	 */
	private void registerAsGlobal(IAction action) {
		getActionBarConfigurer().registerGlobalAction(action);
		register(action);
	}

	/**
	 * Creates the actions and registers them. Registering is needed to ensure that key bindings work. The corresponding
	 * commands keybindings are defined in the plugin.xml file. Registering also provides automatic disposal of the
	 * actions when the window is closed.
	 */
	@Override
	protected void makeActions(final IWorkbenchWindow window) {
		// File
		registerAsGlobal(ActionFactory.SAVE.create(window));
		registerAsGlobal(ActionFactory.SAVE_AS.create(window));
		registerAsGlobal(ActionFactory.ABOUT.create(window));
		registerAsGlobal(ActionFactory.SAVE_ALL.create(window));
		registerAsGlobal(ActionFactory.CLOSE.create(window));
		registerAsGlobal(ActionFactory.CLOSE_ALL.create(window));
		registerAsGlobal(ActionFactory.CLOSE_ALL_SAVED.create(window));
		registerAsGlobal(ActionFactory.REVERT.create(window));
		registerAsGlobal(ActionFactory.PRINT.create(window));
		registerAsGlobal(ActionFactory.QUIT.create(window));

		// Edit
		registerAsGlobal(ActionFactory.SELECT_ALL.create(window));
		registerAsGlobal(ActionFactory.FIND.create(window));
		registerAsGlobal(ActionFactory.UNDO.create(window));
		registerAsGlobal(ActionFactory.REDO.create(window));
		registerAsGlobal(ActionFactory.CUT.create(window));
		registerAsGlobal(ActionFactory.COPY.create(window));
		registerAsGlobal(ActionFactory.PASTE.create(window));

		// Grammar
		registerAsGlobal(new CheckGrammarAction());
		registerAsGlobal(new CloseFileAction());

		// Window
		registerAsGlobal(ActionFactory.SHOW_VIEW_MENU.create(window));
		registerAsGlobal(ActionFactory.EDIT_ACTION_SETS.create(window));
		registerAsGlobal(ActionFactory.OPEN_PERSPECTIVE_DIALOG.create(window));
		registerAsGlobal(ActionFactory.PREFERENCES.create(window));

		// About
		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);

		// Update
		registerAsGlobal(new UpdateAction());
	}

	private IMenuManager createFileMenu() {
		MenuManager menu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE); //$NON-NLS-1$
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
		menu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
		menu.add(getAction(ActionFactory.CLOSE.getId()));
		menu.add(getAction(ActionFactory.CLOSE_ALL.getId()));
		// menu.add(closeAllSavedAction);
		menu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.SAVE.getId()));
		menu.add(getAction(ActionFactory.SAVE_AS.getId()));
		menu.add(getAction(ActionFactory.SAVE_ALL.getId()));
		menu.add(getAction(ActionFactory.REVERT.getId()));
		menu.add(ContributionItemFactory.REOPEN_EDITORS
				.create(getActionBarConfigurer().getWindowConfigurer().getWindow()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.PRINT.getId()));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.QUIT.getId()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		return menu;
	}

	private IMenuManager createHelpMenu() {
		MenuManager menu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator());
		menu.add(getAction(UpdateAction.ID));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.ABOUT.getId()));
		return menu;
	}

	private IMenuManager createGrammarMenu() {
		MenuManager menu = new MenuManager("&Grammar", IWorkbenchActionConstants.M_PROJECT);
		menu.add(new GroupMarker(IWorkbenchActionConstants.WB_START));
		menu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
		menu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
		menu.add(getAction(CloseFileAction.ID));
		menu.add(new Separator());
		menu.add(getAction(CheckGrammarAction.ID));
		menu.add(new GroupMarker(IWorkbenchActionConstants.BUILD_EXT));
		menu.add(new GroupMarker(IWorkbenchActionConstants.WB_END));
//		menu.add(getAction(ActionFactory.???.getId()));
		return menu;
	}

	private IMenuManager createWindowMenu() {
		MenuManager menu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		MenuManager subMenu = new MenuManager("Open &View", IWorkbenchActionConstants.VIEW_EXT);
		subMenu.add(ContributionItemFactory.VIEWS_SHORTLIST.create(getActionBarConfigurer().getWindowConfigurer()
				.getWindow()));
		menu.add(subMenu);
		menu.add(getAction(ActionFactory.SHOW_VIEW_MENU.getId()));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.PREFERENCES.getId()));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
		menu.add(getAction(ActionFactory.OPEN_PERSPECTIVE_DIALOG.getId()));
		menu.add(new Separator());
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		return menu;
	}

	private IMenuManager createEditMenu() {
		MenuManager menu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT); //$NON-NLS-1$
		menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
		menu.add(getAction(ActionFactory.UNDO.getId()));
		menu.add(getAction(ActionFactory.REDO.getId()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
		menu.add(getAction(ActionFactory.CUT.getId()));
		menu.add(getAction(ActionFactory.COPY.getId()));
		menu.add(getAction(ActionFactory.PASTE.getId()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
		menu.add(getAction(ActionFactory.SELECT_ALL.getId()));
		menu.add(new Separator());
		menu.add(getAction(ActionFactory.FIND.getId()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
		menu.add(new GroupMarker(IWorkbenchActionConstants.ADD_EXT));
		menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		return menu;
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createGrammarMenu());
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(createWindowMenu());
		menuBar.add(createHelpMenu());
	}

}

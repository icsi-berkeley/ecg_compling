package compling.gui.grammargui.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import compling.gui.grammargui.ui.views.AnalyzerViewPart;
import compling.gui.grammargui.ui.views.ConstructionTreeView;
import compling.gui.grammargui.ui.views.GrammarTreeView;
import compling.gui.grammargui.ui.views.MapTreeView;
import compling.gui.grammargui.ui.views.OntologyTreeView;
import compling.gui.grammargui.ui.views.SchemaTreeView;
import compling.gui.grammargui.ui.views.SituationTreeView;
import compling.gui.grammargui.ui.views.TypeSystemNodeView;

public class AnalysisPerspective implements IPerspectiveFactory {

	private static final String HIERARCHY_FOLDER = "hierarchyFolder";
	private static final String RESOURCE_FOLDER = "resourceFolder";
	private static final String TOP_FOLDER = "topfolder";
	private static final String BOTTOM_FOLDER = "bottomfolder";

	private static final String CONTENT_OUTLINE_ID = "org.eclipse.ui.views.ContentOutline";
	private static final String COMMON_EXPLORER_ID = "compling.gui.grammargui.views.commonExplorer";
	private static final String LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		createLeftFolder(layout, editorArea);
		createRightFolder(layout, editorArea);
		createTopFolder(layout, editorArea);
		createBottomFolder(layout, editorArea);

		// Add shortcuts to menu
		layout.addShowViewShortcut(GrammarTreeView.ID);
		layout.addShowViewShortcut(TypeSystemNodeView.ID);
		layout.addShowViewShortcut(ConstructionTreeView.ID);
		layout.addShowViewShortcut(SchemaTreeView.ID);
		layout.addShowViewShortcut(MapTreeView.ID);
		layout.addShowViewShortcut(SituationTreeView.ID);
		layout.addShowViewShortcut(OntologyTreeView.ID);
		layout.addShowViewShortcut(AnalyzerViewPart.ID);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
//		layout.addShowViewShortcut(LOG_VIEW_ID);
		layout.addShowViewShortcut(COMMON_EXPLORER_ID);
		layout.addShowViewShortcut(CONTENT_OUTLINE_ID);
	}

	private void createBottomFolder(IPageLayout layout, String editorArea) {
		IFolderLayout folder = layout.createFolder(BOTTOM_FOLDER, IPageLayout.BOTTOM, 0.70f, editorArea);
		folder.addView(IPageLayout.ID_PROBLEM_VIEW);
		folder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
	}

	private void createTopFolder(IPageLayout layout, String editorArea) {
		IFolderLayout folder = layout.createFolder(TOP_FOLDER, IPageLayout.TOP, 0.15f, editorArea);
		folder.addView(AnalyzerViewPart.ID);
	}

	private void createRightFolder(IPageLayout layout, String editorArea) {
		IFolderLayout folder = layout.createFolder(RESOURCE_FOLDER, IPageLayout.RIGHT, 0.60f, editorArea);
		folder.addView(COMMON_EXPLORER_ID);
		folder.addView(CONTENT_OUTLINE_ID);
	}

	private void createLeftFolder(IPageLayout layout, String editorArea) {
		IFolderLayout folder = layout.createFolder(HIERARCHY_FOLDER, IPageLayout.LEFT, 0.30f, editorArea);
		folder.addView(GrammarTreeView.ID);
//		folder.addView(SchemaTreeView.ID);
//		folder.addView(MapTreeView.ID);
//		folder.addView(SituationTreeView.ID);
//		folder.addView(OntologyTreeView.ID);
	}

}

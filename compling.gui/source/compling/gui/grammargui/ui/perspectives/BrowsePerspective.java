package compling.gui.grammargui.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import compling.gui.grammargui.ui.views.ConstructionTreeView;
import compling.gui.grammargui.ui.views.GrammarTreeView;
import compling.gui.grammargui.ui.views.MapTreeView;
import compling.gui.grammargui.ui.views.OntologyTreeView;
import compling.gui.grammargui.ui.views.SchemaTreeView;
import compling.gui.grammargui.ui.views.SituationTreeView;
import compling.gui.grammargui.ui.views.TypeSystemNodeView;

public class BrowsePerspective implements IPerspectiveFactory {

	private static final String HIERARCHY_FOLDER = "hierarchyFolder";
	private static final String RESOURCE_FOLDER = "resourceFolder";

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		// Add shortcuts to menu
		layout.addShowViewShortcut(GrammarTreeView.ID);
		layout.addShowViewShortcut(TypeSystemNodeView.ID);
		layout.addShowViewShortcut(ConstructionTreeView.ID);
		layout.addShowViewShortcut(SchemaTreeView.ID);
		layout.addShowViewShortcut(OntologyTreeView.ID);
		layout.addShowViewShortcut(MapTreeView.ID);
		layout.addShowViewShortcut(SituationTreeView.ID);

		// Create lateral folders
		createLeftFolder(layout, editorArea);
		createRightFolder(layout, editorArea);
	}

	private void createRightFolder(IPageLayout layout, String editorArea) {
		IFolderLayout folder = layout.createFolder(RESOURCE_FOLDER, IPageLayout.RIGHT, 0.60f, editorArea);
		folder.addView(TypeSystemNodeView.ID);
	}

	private void createLeftFolder(IPageLayout layout, String editorArea) {
		IFolderLayout leftFolder = layout.createFolder(HIERARCHY_FOLDER, IPageLayout.LEFT, 0.4f, editorArea);
		leftFolder.addView(GrammarTreeView.ID);
//		leftFolder.addView(SchemaTreeView.ID);
//		leftFolder.addView(MapTreeView.ID);
//		leftFolder.addView(SituationTreeView.ID);
//		leftFolder.addView(OntologyTreeView.ID);
	}

}

package compling.gui.grammargui.ui.views;

public class ConstructionView extends TypeSystemNodeView {

	public static final String ID = "compling.gui.grammargui.views.construction";

	@Override
	protected String getConnectedViewId() {
		return ConstructionTreeView.ID;
	}

	public ConstructionView() {
		super();
	}

}

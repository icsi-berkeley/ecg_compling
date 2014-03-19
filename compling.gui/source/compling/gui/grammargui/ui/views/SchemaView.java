package compling.gui.grammargui.ui.views;

public class SchemaView extends TypeSystemNodeView {

	public static final String ID = "compling.gui.grammargui.views.schema";

	@Override
	protected String getConnectedViewId() {
		return SchemaTreeView.ID;
	}

}

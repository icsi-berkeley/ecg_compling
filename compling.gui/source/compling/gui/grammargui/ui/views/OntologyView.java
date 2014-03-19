package compling.gui.grammargui.ui.views;

public class OntologyView extends TypeSystemNodeView {

	public final static String ID = "compling.gui.grammargui.views.ontology";

	@Override
	protected String getConnectedViewId() {
		return OntologyTreeView.ID;
	}

}

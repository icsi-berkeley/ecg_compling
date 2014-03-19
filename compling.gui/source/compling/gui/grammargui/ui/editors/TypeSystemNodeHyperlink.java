/**
 * 
 */
package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import compling.grammar.ecg.Grammar;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.model.TypeSystemEditorInput;
import compling.gui.util.TypeSystemNodeType;
import compling.gui.util.Utils;

/**
 * @author lucag
 * 
 */
public class TypeSystemNodeHyperlink implements IHyperlink {

	private IRegion region;
	private String text;
	private IWorkbenchPart part;

	/**
	 * @param region
	 * @param sourceViewer
	 * @param text
	 */
	public TypeSystemNodeHyperlink(IRegion region, String text, IWorkbenchPart part) {
		this.region = region;
		this.text = text;
		this.part = part;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getHyperlinkText() {
		return text;
	}

	public String getTypeLabel() {
		// TODO Auto-generated method stub
		return "Hyperlink Type Label";
	}

	public void open() {
		try {
			Grammar grammar = PrefsManager.instance().getGrammar();
			for (TypeSystemNodeType t : TypeSystemNodeType.values()) {
				TypeSystemNode node = Utils.fromDescriptor(grammar, t.toString() + '/' + text);
				if (node != null)
					part.getSite().getPage().openEditor(new TypeSystemEditorInput(node), ConstructionEditor.ID);
			}
		}
		catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

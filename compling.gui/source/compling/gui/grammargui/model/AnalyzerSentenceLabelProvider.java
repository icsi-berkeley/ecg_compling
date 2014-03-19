/**
 * 
 */
package compling.gui.grammargui.model;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import compling.gui.grammargui.util.Constants.IImageKeys;

/**
 * @author lucag
 * 
 */
public class AnalyzerSentenceLabelProvider extends LabelProvider {

	private static final ISharedImages PLATFORM_IMAGES = PlatformUI.getWorkbench().getSharedImages();

	@Override
	public Image getImage(Object element) {
		return PLATFORM_IMAGES.getImage(IImageKeys.SENTENCE_VALID);
	}

	@Override
	public String getText(Object element) {
		return ((AnalyzerSentence) element).getText();
	}

}

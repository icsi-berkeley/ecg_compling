/**
 * 
 */
package compling.gui.grammargui.model;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.util.Constants.IImageKeys;

/**
 * @author lucag
 * 
 */
public class AnalyzerSentenceLabelProvider extends ColumnLabelProvider {
	private Color failureColor;

	public Color getFailureColor() {
		if (failureColor == null)
			failureColor = new Color(Display.getCurrent(), 220, 20, 60);

		return failureColor;
	}

	@Override
	public Image getImage(Object element) {
		return EcgEditorPlugin.getDefault().imageFor(IImageKeys.SENTENCE);
	}

	@Override
	public String getText(Object element) {
		return ((AnalyzerSentence) element).getText();
	}

	@Override
	public Color getBackground(Object element) {
		final Object elementData = ((AnalyzerSentence) element).getData();
		final Boolean parseSucceded = elementData == null || (Boolean) elementData;
		
		return parseSucceded ? Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND) : getFailureColor();
	}

	@Override
	public void dispose() {
		if (failureColor != null)
			failureColor.dispose();
		
		super.dispose();
	}
	
	
}

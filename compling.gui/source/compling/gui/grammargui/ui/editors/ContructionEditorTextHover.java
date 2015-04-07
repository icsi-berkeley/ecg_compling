package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import compling.gui.grammargui.model.PrefsManager;

public class ContructionEditorTextHover extends DefaultTextHover {

	public ContructionEditorTextHover(ISourceViewer sourceViewer) {
		super(sourceViewer);
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// Let's see if there's an annotation at the hover region
		@SuppressWarnings("deprecation")
		String info = super.getHoverInfo(textViewer, hoverRegion);
		if (info != null)
			return info;

		try {
			String symbol = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
			if (PrefsManager.getDefault().getGrammar().getDeclaredPackages().contains(symbol)) {
				return PrefsManager.getDefault().getPackageAsText(symbol);
			} else {
				return PrefsManager.getDefault().getContentAsText(symbol);
			}
		}
		catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return findWord(textViewer.getDocument(), offset);
	}

	private IRegion findWord(IDocument document, int offset) {
		int start = -2;
		int end = -1;

		try {
			int pos = offset;
			char c;
			for (; pos >= 0; --pos) {
				c = document.getChar(pos);
				if (!Character.isUnicodeIdentifierPart(c) && c != '-')
					break;
			}
			start = pos;

			pos = offset;
			int length = document.getLength();
			for (; pos < length; ++pos) {
				c = document.getChar(pos);
				if (!Character.isUnicodeIdentifierPart(c) && c != '-')
					break;
			}
			end = pos;

		}
		catch (BadLocationException x) {
		}

		if (start >= -1 && end > -1) {
			if (start == offset && end == offset)
				return new Region(offset, 0);
			else if (start == offset)
				return new Region(start, end - start);
			else
				return new Region(start + 1, end - start - 1);
		}
		return null;
	}

}

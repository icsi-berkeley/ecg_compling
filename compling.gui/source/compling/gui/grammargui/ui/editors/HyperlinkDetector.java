package compling.gui.grammargui.ui.editors;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPart;

import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Log;

public class HyperlinkDetector extends AbstractHyperlinkDetector {

	private static final String ID = "[a-zA-Z][0-9a-zA-Z_-]*";
	private static final Pattern ID_PATTERN = Pattern.compile(ID);

	private IWorkbenchPart part;

	public HyperlinkDetector(IWorkbenchPart part) {
		this.part = part;
	}

	protected IRegion findWord(IDocument document, int offset) {
		IRegion lineInfo;
		String line;
		try {
			lineInfo = document.getLineInformationOfOffset(offset);
			line = document.get(lineInfo.getOffset(), lineInfo.getLength());
		}
		catch (BadLocationException ex) {
			return null;
		}
		Matcher matcher = ID_PATTERN.matcher(line);
		int i = offset - lineInfo.getOffset();
		while (matcher.find()) {
			if (matcher.start() <= i && i <= matcher.end())
				return new Region(lineInfo.getOffset() + matcher.start(), matcher.end() - matcher.start());
		}
		return null;
	}

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		IDocument document = textViewer.getDocument();
		IRegion wordRegion = findWord(document, region.getOffset());
		if (wordRegion != null) {
			try {
				Set<String> symbols = PrefsManager.instance().getSymbols();
				String s = document.get(wordRegion.getOffset(), wordRegion.getLength());
				if (symbols.contains(s))
					return new IHyperlink[] { new TypeSystemNodeHyperlink(wordRegion, s, part) };
			}
			catch (BadLocationException e) {
				Log.logError(e, "detectHyperlinks");
			}
		}
		return null;
	}

}

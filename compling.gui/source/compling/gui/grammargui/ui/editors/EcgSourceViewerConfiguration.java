package compling.gui.grammargui.ui.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import compling.gui.grammargui.model.ColorProvider;

public class EcgSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private IWorkbenchPart part;

	static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	/**
	 * @param part
	 * @param preferenceStore
	 *           TODO
	 */
	public EcgSourceViewerConfiguration(IWorkbenchPart part, IPreferenceStore preferenceStore) {
		super(preferenceStore);
		this.part = part;
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return EcgPartitionScanner.ECG_PARTITIONING;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, EcgPartitionScanner.ECG_COMMENT };
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(EcgCodeScanner.instance());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		Color commentColor = ColorProvider.instance().getColor(ColorProvider.COMMENT);
		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(commentColor)));
		reconciler.setDamager(dr, EcgPartitionScanner.ECG_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, EcgPartitionScanner.ECG_MULTILINE_COMMENT);

		return reconciler;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] { new HyperlinkDetector(part) };
	}

	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new DefaultAnnotationHover() {
			@Override
			protected boolean isIncluded(Annotation annotation) {
				return true;
			}
		};
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new ContructionEditorTextHover(sourceViewer) {
			@Override
			protected boolean isIncluded(Annotation annotation) {
				return isShownInText(annotation);
			}
		};
	}

}

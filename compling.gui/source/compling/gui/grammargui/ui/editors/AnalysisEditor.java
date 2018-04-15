package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import compling.gui.grammargui.model.IAnalyzerEditorInput;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.ui.actions.PrintAction;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.gui.util.IParse;
import compling.parser.ecgparser.Analysis;

public class AnalysisEditor extends MultiPageEditorPart  implements IModelChangedListener {

	public static final String ID = "compling.gui.grammargui.editors.analyzer";

	private Text transcript;
	private List<Browser> browsers;
	private AnalysisOutline outline;
	private AnalysisHtmlBuilder htmlBuilder;
	
	private Text annotationText;
	
	private AnnotatedAnalysis annotationTool;
	
	private interface IViewType {
		public int TRANSCRIPT = 0;
		public int HTML = 1;
		public int ANNOTATION = 2;
		// public int ALTERNATE = 1;
		// public int GRAPHICAL = 2;
	}
	

	
	public AnalysisEditor() {
		this.htmlBuilder = new AnalysisHtmlBuilder();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);

		PrintAction printAction = new PrintAction(this);
		site.getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), printAction);
	}

	public void print() {
		// TODO: Print!
		int b = getActivePage() - IViewType.HTML;
		if (0 <= b && b < browsers.size())
			browsers.get(b).execute("javascript:print()");
	}
	
	protected void createAnnotationPage() {
		
		annotationTool = new AnnotatedAnalysis();
		annotationText = new Text(getContainer(), SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();
		for (IParse p : input.getParses()) {
			for (Analysis a : p.getAnalyses()) {
				Browser b = new Browser(getContainer(), SWT.NONE);
				String text = annotationTool.getAnnotatedText(input.getSentence().getText(), a);
				b.setText(text);
				annotationText.setText(text);
				browsers.add(b);
				addPage(IViewType.ANNOTATION, b);
				
				//addPage(IViewType.ANNOTATION, b);
				setPageText(IViewType.ANNOTATION, "Annotation: " + p.getCost());
			}
		}
		//Analysis first = input.getParses().iterator().next().getAnalyses().iterator().next();

	} 

	protected void createRawTextViewPage() {
		transcript = new Text(getContainer(), SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL);
		transcript.setLayoutData(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
		transcript.setBackground(transcript.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		transcript.setForeground(transcript.getDisplay().getSystemColor(SWT.COLOR_BLACK));

		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();
		long startTime = System.nanoTime();
		String parseText = input.getText();
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000000;
		String timeString = String.format("Time: %s second(s)", duration);
		String finalTranscript = timeString + "\n" + parseText;
		//transcript.setText(input.getText());
		transcript.setText(finalTranscript);
		
		addPage(IViewType.TRANSCRIPT, transcript);
		setPageText(IViewType.TRANSCRIPT, "Text Output");
	}

	protected void crateBrowserPages() {
		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();
		
		browsers = new ArrayList<Browser>();

		int page = IViewType.HTML;
		for (IParse p : input.getParses()) {
			for (Analysis a : p.getAnalyses()) {
				
				Browser b = new Browser(getContainer(), SWT.NONE);
				b.setText(htmlBuilder.getHtmlText(a.getText(), a)); // Use the actual text used in the analysis to construct the view
				browsers.add(b);
				addPage(page, b);
				setPageText(page, String.format("SemSpec %d, cost %f", page, p.getCost()));
				++page;
			}
		}
	}

	



	@Override
	public void setFocus() {
		int page = getActivePage();
		if (page == IViewType.TRANSCRIPT)
			transcript.setFocus();
		else if (page <= browsers.size())
			browsers.get(page - 1).setFocus();
		else if (page == IViewType.ANNOTATION)
			annotationText.setFocus();
		else
			Log.logInfo("!!! no active page");
	}

	public void close() {
		getSite().getPage().closeEditor(this, false);
	}

	@Override
	public void modelChanged(ModelChangedEvent event) {
		if (event.getGrammarProxy() != null)
			close();
	}

	@Override
	public void dispose() {
		unregister();
		super.dispose();
	}

	protected void updateTitle() {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	protected void register() {
		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();
		input.getSentence().getModel().addModelChangeListener(this);
	}

	protected void unregister() {
		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();
		input.getSentence().getModel().removeModelChangeListener(this);
	}

	@Override
	protected void createPages() {
		register();
		createRawTextViewPage();
		crateBrowserPages();
		//createAnnotationPage();
		updateTitle();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (outline == null)
				outline = new AnalysisOutline(getEditorInput());

			return outline;
		}
		return super.getAdapter(required);
	}

}

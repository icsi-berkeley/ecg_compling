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
	
	private interface IViewType {
		public int TRANSCRIPT = 0;
		public int HTML = 1;
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

	protected void createRawTextViewPage() {
		transcript = new Text(getContainer(), SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL);
		transcript.setLayoutData(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
		transcript.setBackground(transcript.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		transcript.setForeground(transcript.getDisplay().getSystemColor(SWT.COLOR_BLACK));

		IAnalyzerEditorInput input = (IAnalyzerEditorInput) getEditorInput();

		transcript.setText(input.getText());
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
				b.setText(htmlBuilder.getHtmlText(input.getSentence().getText(), a));
				browsers.add(b);
				addPage(page, b);
				setPageText(page, String.format("SemSpec %d, cost %f", page, p.getCost()));
				++page;
			}
		}
	}

	

	// protected void createHtmlViewPage() {
	// alternate = new Text(getContainer(), SWT.BORDER | SWT.READ_ONLY |
	// SWT.MULTI
	// | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
	// alternate.setLayoutData(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
	// alternate.setBackground(alternate.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	// alternate.setForeground(alternate.getDisplay().getSystemColor(SWT.COLOR_BLACK));
	// alternate.setText(getHtmlText());
	// addPage(ViewType.ALTERNATE, alternate);
	// setPageText(ViewType.ALTERNATE, "HTML Output");
	// }

//	protected void createHtmlBrowserPages(Analysis analysis, IAnalyzerEditorInput input) {
//		Browser browser = new Browser(getContainer(), SWT.NONE);
//		browser.setLayoutData(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
//		FeatureStructureFormatter formatter = new HtmlFeatureStructureFormatter(emitter);
//		
//		browser.setText(getHtmlText(formatter, analysis, input));
//		browsers.add(browser);
//		// browser.setText("<p>cacca</p>");
//		addPage(IViewType.HTML, browser);
//		setPageText(IViewType.HTML, "SemSpec");
//	}

	// protected void createGraphicalViewPage() {
	// graph = new Graph(getContainer(), SWT.NONE);
	//
	// GraphContainer c = new GraphContainer(graph, SWT.NONE, "What is this?");
	//
	// GraphNode n1 = new GraphNode(c, SWT.NONE, "Paper");
	// GraphNode n2 = new GraphNode(c, SWT.NONE, "Rock");
	// GraphNode n3 = new GraphNode(graph, SWT.NONE, "Scissors");
	//
	// // c.setLayoutAlgorithm(new SpringLayoutAlgorithm(), true);
	//
	// new GraphConnection(graph, SWT.NONE, n1, n2);
	// new GraphConnection(graph, SWT.NONE, n2, n3);
	// new GraphConnection(graph, SWT.NONE, n3, n1);
	//
	// graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(
	// LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
	//
	// addPage(ViewType.GRAPHICAL, graph);
	// setPageText(ViewType.GRAPHICAL, "Graphical Output");
	// }

	@Override
	public void setFocus() {
		int page = getActivePage();
		if (page == IViewType.TRANSCRIPT)
			transcript.setFocus();
		else if (page <= browsers.size())
			browsers.get(page - 1).setFocus();
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

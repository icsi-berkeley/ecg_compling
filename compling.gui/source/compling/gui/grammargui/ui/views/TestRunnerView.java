package compling.gui.grammargui.ui.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import compling.gui.grammargui.EcgEditorPlugin;
import compling.gui.grammargui.model.AnalyzerSentence;
import compling.gui.grammargui.model.AnalyzerSentenceContentProvider;
import compling.gui.grammargui.model.AnalyzerSentenceLabelProvider;
import compling.gui.grammargui.model.IModelChangedListener;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.ModelChangedEvent;
import compling.gui.util.Utils;
import compling.parser.ParserException;
import compling.parser.ecgparser.ECGAnalyzer;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

@SuppressWarnings("restriction")
public class TestRunnerView extends ViewPart implements IModelChangedListener /*, ISelectionProvider */ {

	public class TestRunnerJob extends Job {

		private AnalyzerSentence[] sentences;

		public TestRunnerJob(String name) {
			super(name);
			this.sentences = getSelectedSentences();
		}

		protected AnalyzerSentence[] getSelectedSentences() {
			Object[] es = viewer.getCheckedElements();
			AnalyzerSentence[] sentences = new AnalyzerSentence[es.length];
			for (int i = 0; i < es.length; ++i)
				sentences[i] = (AnalyzerSentence) es[i];

			return sentences;
		}

		protected Object[] getElements() {
			return ((AnalyzerSentenceContentProvider) viewer.getContentProvider()).getElements(null);
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			int step = 0;
			int failures = 0;
			long startTime = System.nanoTime();
			long analyzerTime = 0;
			try {
				long startupTimeOne = System.nanoTime();
				ECGAnalyzer analyzer = new ECGAnalyzer(PrefsManager.getDefault().getGrammar());
				long startupTimeTwo = System.nanoTime();
				analyzerTime = (startupTimeTwo - startupTimeOne) / 1000000000;
				
				List<Boolean> results = new ArrayList<Boolean>();
				beginProgress(monitor, sentences.length);
				for (AnalyzerSentence s : sentences) {
					try {
						results.add(Utils.getParses(s.getText(), analyzer).size() > 0);
						s.setData(true);
						Log.logInfo("Test success on sentence <%s>\n", s.getText());
					} 
					catch (ParserException e) {
						Log.logError(e, "Test error on sentence <%s>\n", s.getText());
						s.setData(false);
						results.add(false);
						++failures;
					}
					reportProgress(monitor, ++step, failures);
				}
			} 
			catch (IOException e) {
				Log.logError(e, "Major problem while instantiating Analyzer");
				return new JobStatus(IStatus.ERROR, this, "Problem initializing Analyzer object.");
			}
			long endTime = System.nanoTime();
			
			long duration = (endTime - startTime) / 1000000000;
			//duration

			reportFinal(monitor, getElements().length, step, failures, duration, analyzerTime);
			
			return JobStatus.OK_STATUS;
		}
		
		protected void reportProgress(final IProgressMonitor monitor, final int step, final int failures) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (monitor != null)
						monitor.worked(step);
					progressBar.step(failures);
				}
			});
		}
		
		protected void beginProgress(final IProgressMonitor monitor, final int totalWork) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (monitor != null)
						monitor.beginTask("Parsing test set", totalWork);
					progressBar.reset();
					progressBar.setMaximum(totalWork);
				}
			});
		}
		
		protected void reportFinal(final IProgressMonitor monitor, final int total, final int done, final int failures, final long time, final long analyzerTime) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (monitor != null)
						monitor.done();
					counterPanel.setLabels(total, done, failures, time, analyzerTime);
					viewer.refresh();
				}
			});
		}
	}

	/**
	 * A progress bar with a red/green indication for success or failure.
	 */
	public class ProgressBar extends Canvas {
		private static final int DEFAULT_WIDTH = 160;
		private static final int DEFAULT_HEIGHT = 18;

		private int fCurrentTickCount = 0;
		private int fMaxTickCount = 0;
		private int fColorBarWidth = 0;
		private Color fOKColor;
		private Color fFailureColor;
		private Color fStoppedColor;
		private boolean fError;
		private boolean fStopped = false;

		public ProgressBar(Composite parent) {
			super(parent, SWT.NONE);

			addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					fColorBarWidth = scale(fCurrentTickCount);
					redraw();
				}
			});
			addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					paint(e);
				}
			});
			addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					fFailureColor.dispose();
					fOKColor.dispose();
					fStoppedColor.dispose();
				}
			});
			Display display = parent.getDisplay();
			fFailureColor = new Color(display, 178, 34, 34);
			fOKColor = new Color(display, 95, 191, 95);
			fStoppedColor = new Color(display, 120, 120, 120);
		}

		public void setMaximum(int max) {
			fMaxTickCount = max;
		}

		public void reset() {
			fError = false;
			fStopped = false;
			fCurrentTickCount = 0;
			fMaxTickCount = 0;
			fColorBarWidth = 0;
			redraw();
		}

		public void reset(boolean hasErrors, boolean stopped, int ticksDone, int maximum) {
			boolean noChange = fError == hasErrors && fStopped == stopped && fCurrentTickCount == ticksDone
					&& fMaxTickCount == maximum;
			fError = hasErrors;
			fStopped = stopped;
			fCurrentTickCount = ticksDone;
			fMaxTickCount = maximum;
			fColorBarWidth = scale(ticksDone);
			if (!noChange)
				redraw();
		}

		private void paintStep(int startX, int endX) {
			GC gc = new GC(this);
			setStatusColor(gc);
			Rectangle rect = getClientArea();
			startX = Math.max(1, startX);
			gc.fillRectangle(startX, 1, endX - startX, rect.height - 2);
			gc.dispose();
		}

		private void setStatusColor(GC gc) {
			if (fStopped)
				gc.setBackground(fStoppedColor);
			else if (fError)
				gc.setBackground(fFailureColor);
			else
				gc.setBackground(fOKColor);
		}

		public void stopped() {
			fStopped = true;
			redraw();
		}

		private int scale(int value) {
			if (fMaxTickCount > 0) {
				Rectangle r = getClientArea();
				if (r.width != 0)
					return Math.max(0, value * (r.width - 2) / fMaxTickCount);
			}
			return value;
		}

		private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
			gc.setForeground(topleft);
			gc.drawLine(x, y, x + w - 1, y);
			gc.drawLine(x, y, x, y + h - 1);

			gc.setForeground(bottomright);
			gc.drawLine(x + w, y, x + w, y + h);
			gc.drawLine(x, y + h, x + w, y + h);
		}

		private void paint(PaintEvent event) {
			GC gc = event.gc;
			Display disp = getDisplay();

			Rectangle rect = getClientArea();
			gc.fillRectangle(rect);
			drawBevelRect(gc, rect.x, rect.y, rect.width - 1, rect.height - 1, disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
					disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

			setStatusColor(gc);
			fColorBarWidth = Math.min(rect.width - 2, fColorBarWidth);
			gc.fillRectangle(1, 1, fColorBarWidth, rect.height - 2);
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {
			checkWidget();
			Point size = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			if (wHint != SWT.DEFAULT)
				size.x = wHint;
			if (hHint != SWT.DEFAULT)
				size.y = hHint;
			return size;
		}

		public void step(int failures) {
			fCurrentTickCount++;
			int x = fColorBarWidth;

			fColorBarWidth = scale(fCurrentTickCount);

			if (!fError && failures > 0) {
				fError = true;
				x = 1;
			}
			if (fCurrentTickCount == fMaxTickCount)
				fColorBarWidth = getClientArea().width - 1;
			paintStep(x, fColorBarWidth);
		}

		public void refresh(boolean hasErrors) {
			fError = hasErrors;
			redraw();
		}

	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		// TODO Auto-generated method stub
		super.init(site, memento);
	}

	@Override
	public void saveState(IMemento memento) {
		// TODO Auto-generated method stub
		super.saveState(memento);
	}

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "compling.gui.views.TestRunnerView";

	private CheckboxTableViewer viewer;
	private Action start;
	private Action removeSelected;
	private Action removeAll;
	private Action doubleClickAction;
	private CounterPanel counterPanel;
	private Composite counterComposite;
	private ProgressBar progressBar;
	private IStructuredSelection selection = StructuredSelection.EMPTY;
	private PrefsManager model;
	private IModelChangedListener modelListener; 
	
	public class CounterPanel extends Composite {
		protected Text numberOfErrors;
		protected Text numberFailed;
		protected Text numberTested;
		protected Text totalTime;
		protected Text startupTime;
		protected int total;
		protected float time;
		protected int ignoredCount;
		protected int assumptionFailedCount;

//		private final Image errorIcon = EcgEditorPlugin.getImageDescriptor(IImageKeys.ERROR).createImage();
		private final Image failureIcon = EcgEditorPlugin.getImageDescriptor(IImageKeys.ERROR).createImage();

		public CounterPanel(Composite parent) {
			super(parent, SWT.WRAP);

			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 6;
			gridLayout.makeColumnsEqualWidth = false;
			gridLayout.marginWidth = 0;

			setLayout(gridLayout);

			setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

			numberTested = createLabel("Tested:", null, " 0/0  ");
			numberFailed = createLabel("Failures:", failureIcon, " 0 ");
			totalTime = createLabel("Total time:", null, " ");
			startupTime = createLabel("Startup Time:", null, " ");
			

			addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					disposeIcons();
				}
			});
		}

		private void disposeIcons() {
//			errorIcon.dispose();
			failureIcon.dispose();
		}

		private Text createLabel(String name, Image image, String init) {
			Label label = new Label(this, SWT.NONE);
			if (image != null) {
				image.setBackground(label.getBackground());
				label.setImage(image);
			}
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			label = new Label(this, SWT.NONE);
			label.setText(name);
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			// label.setFont(JFaceResources.getBannerFont());

			Text value = new Text(this, SWT.READ_ONLY);
			value.setText(init);
			// bug: 39661 Junit test counters do not repaint correctly [JUnit]
			// SWTUtil.fixReadonlyTextBackground(value);
			value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
			return value;
		}

		public void reset() {
			setLabels(0, 0, 0, 0, 0);

			total = 0;
		}

		public void setLabels(int total, int tested, int failures, long time, long analyzerTime) {
			String runString = String.format(" %s/%d ", tested, total);
			String runStringTooltip = "Number of sentences tested / total number of sentences.";
			
			String timeString = String.format(" %s seconds", time);
			String analyzerString = String.format(" %s seconds", analyzerTime);
			
			numberTested.setText(runString);
			numberTested.setToolTipText(runStringTooltip);

			numberFailed.setText(String.format(" %d ", failures));
			numberFailed.setToolTipText("Number of failed parses.");
			
			totalTime.setText(timeString);
			
			startupTime.setText(analyzerString);
			
			numberTested.redraw();
			numberFailed.redraw();
			
			totalTime.redraw();
			

			redraw();
		}

	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public TestRunnerView() {
		this.model = PrefsManager.getDefault();
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);
		
		Button selectAll = new Button(parent, SWT.PUSH);
		selectAll.setText("Select/Deselect All Sentences");
		
		selectAll.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				//System.out.println("Reloading types");
				//viewer.setAllGrayed(true);
				if (viewer.getCheckedElements().length > 0) {
					viewer.setAllChecked(false);
				} else {
					viewer.setAllChecked(true);
				}
			}
		});


		counterComposite = createProgressCountPanel(parent);
		counterComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Table table = new Table(parent, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new AnalyzerSentenceContentProvider(viewer));
		viewer.setLabelProvider(new AnalyzerSentenceLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(PrefsManager.getDefault());

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					TestRunnerView.this.selection = (IStructuredSelection) selection; 
					removeSelected.setEnabled(((IStructuredSelection) selection).size() > 0);
				}
			}
		});
		
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TestRunnerView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(start);
		manager.add(new Separator());
		manager.add(removeSelected);
		manager.add(removeAll);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(start);
		manager.add(removeSelected);
		manager.add(removeAll);

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(start);
		manager.add(new Separator());
		manager.add(removeSelected);
		manager.add(removeAll);
	}

	private void makeActions() {
		start = new Action() {
			@Override
			public void run() {
				Job test = new TestRunnerJob("Sentence Test Run");
				test.setPriority(Job.LONG);
				test.schedule();
			}

		};
		start.setText("Start");
		start.setToolTipText("Begin testing the current grammar on the sentences.");
		start.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.START_E));
		start.setDisabledImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.START_D));
		
		modelListener = new IModelChangedListener() {
			public void modelChanged(ModelChangedEvent event) {
				start.setEnabled(((PrefsManager) event.getSource()).getSentences().size() > 0);
			}
		};

		model.addModelChangeListener(modelListener);
		
		removeSelected = new Action() {
			public void run() {
				for (Object e : selection.toArray())
					model.removeSentence((AnalyzerSentence) e);
			}
		};
		removeSelected.setText("Remove seleted sentence(s)");
		removeSelected.setToolTipText("Remove currently selected sentence(s).");
		removeSelected.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_SENTENCE_E));
		removeSelected.setDisabledImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_SENTENCE_D));

		removeAll = new Action() {
			public void run() {
				model.removeAllSentences();
			}
		};
		removeAll.setText("Remove all sentences");
		removeAll.setToolTipText("Remove all test sentences.");
		removeAll.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_ALL_SENTENCES_E));

		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	@Override
	public void dispose() {
		model.removeModelChangeListener(modelListener);
		
		super.dispose();
	}

	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite = new Composite(parent, SWT.NORMAL);
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.marginBottom = 0;

		composite.setLayout(layout);
		setCounterColumns(layout);

		counterPanel = new CounterPanel(composite);
		counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		progressBar = new ProgressBar(composite);
		progressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		return composite;
	}

	private void setCounterColumns(GridLayout layout) {
		// if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL)
		// layout.numColumns= 2;
		// else
		layout.numColumns = 1;
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Sentence Test Runner View", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void modelChanged(ModelChangedEvent event) {
		// TODO Auto-generated method stub
		
	}
}
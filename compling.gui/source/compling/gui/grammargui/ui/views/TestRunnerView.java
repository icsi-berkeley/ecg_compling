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
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Constants.IImageKeys;
import compling.gui.grammargui.util.Log;
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
public class TestRunnerView extends ViewPart {

	public class TestRunnerJob extends Job {

		private AnalyzerSentence[] sentences;

		public TestRunnerJob(String name, AnalyzerSentence[] sentences) {
			super(name);
			this.sentences = sentences;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				ECGAnalyzer analyzer = new ECGAnalyzer(PrefsManager.getDefault().getGrammar());
				List<Boolean> results = new ArrayList<Boolean>();
				for (AnalyzerSentence s : sentences) {
					try {
						results.add(Utils.getParses(s.getText(), analyzer).size() > 0);
						Log.logInfo("Test success on sentence <%s>\n", s.getText());
					}
					catch (ParserException e) {
						Log.logError(e, "Test error on sentence <%s>\n", s.getText());
						results.add(false);
					}
				}
			} 
			catch (IOException e) {
				Log.logError(e, "Major problem while instantiating Analyzer");
				return new JobStatus(IStatus.ERROR, this, "Problem initializing Analyzer object.");
			}
			return JobStatus.OK_STATUS;
		}
	}

	protected AnalyzerSentence[] getSelectedSentences() {
		Object[] es = viewer.getCheckedElements();
		AnalyzerSentence[] sentences = new AnalyzerSentence[es.length];
		for (int i = 0; i < es.length; ++i)
			sentences[i] = (AnalyzerSentence) es[i];
		
		return sentences;  
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
	private Action removeSentence;
	private Action removeAllSentences;
	private Action doubleClickAction;

	private CounterPanel counterPanel;

	private Composite counterComposite;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view, or
	 * ignore it and always show the same content (like Task List, for example).
	 */

	// class ViewContentProvider implements IStructuredContentProvider {
	// public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	// }
	//
	// public void dispose() {
	// }
	//
	// public Object[] getElements(Object parent) {
	// return new String[] { "One", "Two", "Three" };
	// }
	// }

	// class ViewLabelProvider extends LabelProvider implements
	// ITableLabelProvider {
	// public String getColumnText(Object obj, int index) {
	// return getText(obj);
	// }
	//
	// public Image getColumnImage(Object obj, int index) {
	// return getImage(obj);
	// }
	//
	// public Image getImage(Object obj) {
	// return
	// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
	// }
	// }

	public class CounterPanel extends Composite {
		protected Text numberOfErrors;
		protected Text numberFailed;
		protected Text numberTested;
		protected int total;
		protected int ignoredCount;
		protected int assumptionFailedCount;

		private final Image errorIcon = EcgEditorPlugin.getImageDescriptor(IImageKeys.ERROR).createImage();
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

			addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					disposeIcons();
				}
			});
		}

		private void disposeIcons() {
			errorIcon.dispose();
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
			setErrorValue(0);
			setFailureValue(0);
			setLabels(0, 0);
			
			total = 0;
		}

		public void setLabels(int total, int failures) {
			String runString = String.format(" %d/%d ", total, failures);
			String runStringTooltip = "runStringTooltip";
			
			numberTested.setText(runString);
			numberTested.setToolTipText(runStringTooltip);

//			if (ignoredCount == 0 && ignoredCount > 0 || ignoredCount != 0 && ignoredCount == 0) {
//				layout();
//			} else if (assumptionFailedCount == 0 && assumptionFailureCount > 0 || assumptionFailedCount != 0
//					&& assumptionFailureCount == 0) {
//				layout();
//			} else {
				numberTested.redraw();
				redraw();
//			}
		}

		public void setErrorValue(int value) {
			numberOfErrors.setText(Integer.toString(value));
			redraw();
		}

		public void setFailureValue(int value) {
			numberFailed.setText(Integer.toString(value));
			redraw();
		}
	}

	
	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public TestRunnerView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		parent.setLayout(gridLayout);

		counterComposite = createProgressCountPanel(parent);
		counterComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		Table table = new Table(parent, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new AnalyzerSentenceContentProvider(viewer));
		viewer.setLabelProvider(new AnalyzerSentenceLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(PrefsManager.getDefault());
	
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		hookListener();
	}

	private void hookListener() {
		viewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				// TODO Auto-generated method stub
				Log.consoleLog("CheckStateChange: %s", event);
			}
		});
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
		manager.add(removeSentence);
		manager.add(removeAllSentences);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(start);
		manager.add(removeSentence);
		manager.add(removeAllSentences);

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(start);
		manager.add(new Separator());
		manager.add(removeSentence);
		manager.add(removeAllSentences);
	}

	private void makeActions() {
		start = new Action() {
			public void run() {
				Job test = new TestRunnerJob("Sentence Test", getSelectedSentences());
				test.setPriority(Job.LONG);
//				test.setUser(true);
				
				test.schedule();
			}
		};
		start.setText("Start");
		start.setToolTipText("Begin testing the current grammar on the sentences.");
		start.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.START));

		removeSentence = new Action() {
			public void run() {
				showMessage("Sentence removed");
			}
		};
		removeSentence.setText("Remove sentence");
		removeSentence.setToolTipText("Remove currently selected sentence.");
		removeSentence.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_SENTENCE_E));
		removeSentence.setDisabledImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_SENTENCE_D));

		removeAllSentences = new Action() {
			public void run() {
				showMessage("All sentences removed");
			}
		};
		removeAllSentences.setText("Remove all sentences");
		removeAllSentences.setToolTipText("Remove all test sentences.");
		removeAllSentences.setImageDescriptor(EcgEditorPlugin.getImageDescriptor(IImageKeys.REMOVE_ALL_SENTENCES_E));

		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite = new Composite(parent, SWT.NORMAL);
		composite.setBackgroundMode(SWT.INHERIT_NONE);
		
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
		// fProgressBar = new JUnitProgressBar(composite);
		// fProgressBar.setLayoutData(
		// new GridData(GridData.GRAB_HORIZONTAL |
		// GridData.HORIZONTAL_ALIGN_FILL));
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
}
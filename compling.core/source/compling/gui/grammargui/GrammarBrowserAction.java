package compling.gui.grammargui;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;

/**
 * GrammarBrowserAction defines menus for the main window as required by JFace.
 * 
 * @see GrammarBrowser#createMenuManager()
 * 
 * @author lucag
 */
public class GrammarBrowserAction {

	GrammarBrowser grammarBrowser;

	GrammarBrowserAction(GrammarBrowser grammarBrowser) {
		this.grammarBrowser = grammarBrowser;
	}

	/**
	 * Open a file.
	 */
	public class FileOpen extends Action {

		FileOpen() {
			setText("&Open\tCtrl+O");
		}

		@Override
		public void run() {
			grammarBrowser.handleFileOpen();
		}
	}

	/**
	 * A Menu Action to close a file. It grays itself out if the application's
	 * model is null.
	 */
	public class FileClose extends Action {

		FileClose() {
			setText("&Close\tCtrl+W");
			setEnabled(false);
			grammarBrowser.addModelChangeListener(new IModelChangedListener() {
				public void modelChanged(ModelChangedEvent event) {
					update((GrammarBrowserModel) event.getSource());
				}
			});
		}

		@Override
		public void run() {
			grammarBrowser.handleFileClose();
		}

		void update(GrammarBrowserModel model) {
			boolean enabled = model.getGrammar() != null;
			setEnabled(enabled);
		}

	}

	/**
	 * A Menu Action to quit the application.
	 */
	public class FileExit extends Action {

		FileExit() {
			setText("E&xit\tESC");
			setAccelerator(SWT.ESC);
		}

		@Override
		public void run() {
			grammarBrowser.handleFileExit();
		}
	}

	/**
	 * A Menu Action to change the main view's font.
	 */
	public class ViewSelectFont extends Action {

		ViewSelectFont() {
			setText("&Change Font");
		}

		@Override
		public void run() {
			grammarBrowser.handleSelectFont();
		}
	}

	/**
	 * A Menu Action to increase the view's font size.
	 */
	public class ViewIncreaseFontSize extends Action {

		ViewIncreaseFontSize() {
			setText("&Increase Font Size\tCtrl++");
			setAccelerator(SWT.CONTROL | '+');
		}

		@Override
		public void run() {
			grammarBrowser.handleIncreaseFontSize();
		}
	}

	/**
	 * A Menu Action to decrease the view's font size. 
	 */
	public class ViewDecreaseFontSize extends Action {

		ViewDecreaseFontSize() {
			setText("&Decrease Font Size\tCtrl+-");
			setAccelerator(SWT.CONTROL | '-');
		}

		@Override
		public void run() {
			grammarBrowser.handleDecreaseFontSize();
		}
	}

	/**
	 * A Menu Action to show/hide the analyzer's output view.
	 */
	public class ViewShowHideOutput extends Action {

		ViewShowHideOutput() {
			// setEnabled(false);
			setText("Show/Hide O&uptup\tAlt+0");
		}

		void update(GrammarBrowserModel model) {
			setEnabled(model.getSentences() != null);
		}

		@Override
		public void run() {
			grammarBrowser.handleShowHideOutput();
		}
	}

	/**
	 * An Action for Analyze | Sentence
	 */
	public class AnalyzeSentence extends Action {

		AnalyzeSentence() {
			setEnabled(false);
			setText("&Sentence...");
			grammarBrowser.addModelChangeListener(new IModelChangedListener() {
				public void modelChanged(ModelChangedEvent event) {
					update((GrammarBrowserModel) event.getSource());
				}
			});
		}

		void update(GrammarBrowserModel model) {
			boolean enabled = model.getGrammar() != null;
//			System.out.printf("AnalyzeSentenceActionDelegate.update: %s\n", enabled);
			setEnabled(enabled);
		}

		@Override
		public void run() {
			SafeRunner.run(new SafeRunnable() {
				public void run() throws Exception {
					grammarBrowser.handleAnalyzeSentence();
				}
			});
		}
	}

	/**
	 * An Action for Window | Show Window List
	 */
	public class WindowShowWindowList extends Action {

		WindowShowWindowList() {
			setEnabled(false);
			setText("Show &Windows...");
			grammarBrowser.addModelChangeListener(new IModelChangedListener() {
				public void modelChanged(ModelChangedEvent event) {
					update((GrammarBrowserModel) event.getSource());
				}
			});
		}

		void update(GrammarBrowserModel model) {
			boolean enabled = model.getGrammar() != null;
			setEnabled(enabled);
		}

		@Override
		public void run() {
			grammarBrowser.handleShowWindowList();
		}
	}

	/**
	 * Window | Close All
	 */
	public class WindowCloseAll extends Action {

		WindowCloseAll() {
			setEnabled(false);
			setText("Close All");
			grammarBrowser.addModelChangeListener(new IModelChangedListener() {
				public void modelChanged(ModelChangedEvent event) {
					update((GrammarBrowserModel) event.getSource());
				}
			});
		}

		void update(GrammarBrowserModel model) {
			boolean enabled = model.getGrammar() != null;
			setEnabled(enabled);
		}

		@Override
		public void run() {
			grammarBrowser.handleCloseAllWindows();
		}
	}

}

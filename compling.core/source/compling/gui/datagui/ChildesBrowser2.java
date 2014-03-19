package compling.gui.datagui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesTranscript;

public class ChildesBrowser2 extends JFrame implements ChildesBrowserConstants {

	private static final long serialVersionUID = 20050901L;

	private Action openTranscript = null;
	private ChildesPanel2 currentPanel = null;
	private ChildesTranscript transcript = null;
	private File currentTranscriptFile = null; // TODO ask eva: autosave to same file?
	private JFileChooser generalChooser = null;
	protected static final int HEIGHT = 600;
	protected static final int WIDTH = 800;
	protected static final int INSET = 250;

	private ChildesBrowser2() {
		super(BROWSER_TITLE);
		// TODO 1) base directory 2) remember working directories between uses?
		generalChooser = new JFileChooser(".");
		openTranscript = new OpenTranscriptAction(MENU_OPEN_FILE);
		setJMenuBar(createMenuBar());
	}

	private static void createAndShowGUI() {
		ChildesBrowser2 browserFrame = new ChildesBrowser2();
		browserFrame.addWindowListener(browserFrame.new SaveOnExitListener()); // window closing

		// Display the window.
		browserFrame.pack();
		// TODO JFrame set to previous size on load
		browserFrame.setBounds(new Rectangle(INSET, INSET, WIDTH, HEIGHT)); // restore down size
		browserFrame.setExtendedState(browserFrame.getExtendedState() | Frame.MAXIMIZED_BOTH); // Maximize (AFTER pack)
		browserFrame.setVisible(true);
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenuItem menuItem;

		JMenu fileMenu = new JMenu(MENU_FILE);
		// TODO Alt-F for file (mac?), etc.
		menuBar.add(fileMenu);

		menuItem = new JMenuItem(openTranscript);
		fileMenu.add(menuItem);

		menuItem = new JMenuItem(MENU_PREPROCESS);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				preprocessTranscriptLoop();
			}
		});
		fileMenu.add(menuItem);

		/*
		 * JMenu toolsMenu = new JMenu(MENU_TOOLS); menuBar.add(toolsMenu); menuItem = new JMenuItem(MENU_STATS);
		 */

		return menuBar;
	}

	/** ...file must not be null. */
	private void openTranscript(File file) {
		String xmlFile = file.getAbsolutePath();
		/*
		 * TODO transcript check that xmlFile is good? Show error if not. ask eva: a static method says whether xmlFile is
		 * childes format, another static method says has been preprocessed
		 */
		try {
			ChildesTranscript possibleTranscript = new ChildesTranscript(xmlFile);
			while (!possibleTranscript.hasBeenPreprocessed()) {
				Object[] options = { "Preprocess", "Cancel" };
				int choice = JOptionPane.showOptionDialog(this,
						"This transcript must be preprocessed first. Preprocess it now?", "Preprocess Transcript",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (choice == 0) {
					File destination = preprocessTranscript(file);
					if (destination != null) { // user must preprocess to continue
						possibleTranscript = new ChildesTranscript(destination);
					}
				}
				else { // else do nothing
					return;
				}
			}
			/* possibleTranscript is good; display it. */
			transcript = possibleTranscript;
			currentTranscriptFile = file; // open was successful, so note file (for autosaving)
			Rectangle bounds = getContentPane().getBounds();
			currentPanel = new ChildesPanel2(transcript, new Dimension(bounds.width, bounds.height));
			setContentPane(currentPanel);
			validate(); // repaints the frame
		}
		catch (IOException e) {
			displayErrorMessage(e.getMessage());
		}
		catch (AnnotationException e) { // TODO exception model. ChildesTranscript
			// TODO if is childes format, do something...
			displayErrorMessage(e.getMessage());
		}
	}

	private void closeTranscript() {
		if (currentPanel != null) {
			currentPanel.finish();
			saveTranscript(currentTranscriptFile);
		}
		currentPanel = null;
		currentTranscriptFile = null;
		JPanel blank = new JPanel();
		// blank.setPreferredSize(new Dimension(bounds.width, bounds.height));
		setContentPane(blank); // blank panel
		validate(); // repaints the frame
	}

	/** ... transcript must not be null. destination must be a valid file. */
	private void saveTranscript(File destination) {
		try {
			if (currentPanel != null) {
				currentPanel.saveAnnotation(); // call save directly to ensure saving
			}
			transcript.outputXML(destination);
		}
		catch (IOException ioe) {
			displayErrorMessage("TODO ERROR IOE: \n" + ioe.getMessage()); // TODO errmsg save i/o
		}
	}

	private File preprocessTranscript(File filein) {
		try {
			ChildesTranscript t = new ChildesTranscript(filein);
			if (!t.hasBeenPreprocessed()) { // do not preprocess twice
				t.preprocessTranscript();
			}
			String newFile = filein.getAbsolutePath().replaceAll(FILE_PATTERN, FILE_SUFFIX);
			File destination = chooseFile(newFile, FC_SAVE_PREPROCESS, false);
			if (destination != null) {
				t.outputXML(destination);
				return destination;
			}
		}
		catch (AnnotationException e) {
			displayErrorMessage(e.getMessage());
		}
		catch (IOException e) {
			displayErrorMessage(e.getMessage());
		}
		return null;
	}

	/**
	 * TODO discuss eva, automatic name, etc. "med2.anno.xml" TODO constants
	 */
	private void preprocessTranscriptLoop() {
		boolean openAnother = true;
		while (openAnother) { // TODO constant
			File file = chooseFile(MENU_PREPROCESS, true);
			if (file == null) { // abort
				return;
			}
			File destination = preprocessTranscript(file);
			if (destination == null) { // abort
				return;
			}
			Object[] options = { "Open transcript", "Preprocess another", "Close" };
			int choice = JOptionPane.showOptionDialog(this, "Open this transcript or preprocess another file?",
					"Continue", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (choice == 0) { // TODO constant
				openTranscript(destination);
				openAnother = false;
			}
			else if (choice == 1) {
				openAnother = true;
			}
			openAnother = false;
		}
	}

	private void displayErrorMessage(String message) {
		System.out.println("DEBUG: ERROR " + message); // TODO debug helper
		JOptionPane.showMessageDialog(this, ChildesBrowserConstants.ERROR + ": " + message,
				ChildesBrowserConstants.ERROR, JOptionPane.ERROR_MESSAGE);
	}

	private File chooseFile(String title, boolean showOpen) {
		return chooseFile(null, title, showOpen);
	}

	/** ... suggestedFile and/or title may be null. */
	private File chooseFile(String suggestedFile, String title, boolean showOpen) {
		String currentTitle = generalChooser.getDialogTitle(); // store original title
		if (title != null) {
			generalChooser.setDialogTitle(title);
		}
		if (suggestedFile != null) {
			generalChooser.setSelectedFile(new File(suggestedFile));
		}
		int returnVal;
		if (showOpen) {
			returnVal = generalChooser.showOpenDialog(this);
		}
		else {
			returnVal = generalChooser.showSaveDialog(this);
		}
		generalChooser.setDialogTitle(currentTitle); // restore original title
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = generalChooser.getSelectedFile();
			System.out.println("DEBUG Choosing file: " + file.getName() + ".");
			return file;
		}
		else {
			System.out.println("DEBUG File choice cancelled by user.");
			return null;
		}
	}

	// /////////////
	// MAIN METHOD
	// /////////////

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		} // crazy UIManager convention

		/*
		 * Schedule a job for the event-dispatching thread: creating and showing this application's GUI.
		 */
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	// //////////////////////////////////////
	// INNER CLASSES: Listeners and Actions
	// /////////////////////////////////////

	protected class SaveOnExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			/* Autosave to same file (if transcript open). */
			closeTranscript();
			System.exit(0);
		}
	}

	protected class OpenTranscriptAction extends AbstractAction {

		private static final long serialVersionUID = -5988211081264248284L;

		public OpenTranscriptAction(String text) {
			super(text); // , icon);
			// putValue(SHORT_DESCRIPTION, desc);
			// putValue(MNEMONIC_KEY, mnemonic);
		}

		public void actionPerformed(ActionEvent event) {
			closeTranscript();
			File file = chooseFile(MENU_OPEN_FILE, true);
			if (file != null) {
				openTranscript(file);
			}
		}
	}

}

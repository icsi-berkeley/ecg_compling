// =============================================================================
//File        : ChildesBrowser.java
//Author      : emok
//Change Log  : Created on Jul 25, 2005
//=============================================================================

package compling.gui.datagui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesAnnotation;
import compling.annotation.childes.ChildesIterator;
import compling.annotation.childes.ChildesTranscript;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesTranscript.ChildesEvent;
import compling.annotation.childes.ChildesTranscript.ChildesItem;
import compling.annotation.childes.ChildesUtilities.ChildesStatistics;
import compling.utterance.UtteranceAnnotation;

//=============================================================================

public class ChildesBrowser extends JFrame implements ActionListener {

	private static final long serialVersionUID = 4675714493255208235L;

	protected static final int HEIGHT = 600;
	protected static final int WIDTH = 800;
	protected static final int INSET = 250;

	protected Container mainPane;
	protected JLabel speakerLabel = new JLabel();
	protected JLabel utteranceLabel = new JLabel();
	protected JTextField vernField = new JTextField(80);

	// private JList list;
	// private DefaultListModel listModel;

	private ChildesTranscript transcript = null;
	private ChildesIterator iterator = null;
	private ChildesItem currentItem = null;
	private ChildesAnnotation currentAnnotation = null;
	private ChildesStatistics statistics = null;

	private String workingDir = ".\\";

	public ChildesBrowser() {
		this(null);
	}

	public ChildesBrowser(ChildesTranscript t) {
		super();
		initializeGraphics();

		openTranscript(t);

		addWindowListener(new WindowAdapter() {

			// Respond to window closing events.
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		repaint();

	}

	protected void initializeGraphics() {
		JFrame.setDefaultLookAndFeelDecorated(true);
		mainPane = getContentPane();

		// listModel = new DefaultListModel();
		// list = new JList(listModel);
		// list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// list.setSelectedIndex(0);
		// list.setVisibleRowCount(5);
		// list.setCellRenderer(new ChildesUtteranceRenderer());
		// JScrollPane listScrollPane = new JScrollPane(list);
		// mainPane.add(listScrollPane, BorderLayout.CENTER);

		JPanel utterancePanel = new JPanel();
		utterancePanel.add(speakerLabel, BorderLayout.WEST);
		utterancePanel.add(utteranceLabel, BorderLayout.EAST);

		JLabel vernLabel = new JLabel("vernacular:");
		vernField.setFont(new Font("MS Gothic", Font.PLAIN, 12));

		JPanel annotationPanel = new JPanel();
		annotationPanel.add(vernLabel, BorderLayout.WEST);
		annotationPanel.add(vernField, BorderLayout.EAST);

		JButton saveButton = new JButton("Save & next");
		saveButton.addActionListener(this);

		JButton prevButton = new JButton("Prev");
		prevButton.addActionListener(this);

		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(this);

		JButton outputButton = new JButton("Export to XML");
		outputButton.addActionListener(this);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(saveButton);
		buttonsPanel.add(prevButton);
		buttonsPanel.add(nextButton);
		buttonsPanel.add(outputButton);

		JMenuBar menu = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem("Open..."));
		fileMenu.add(new JMenuItem("Preprocess..."));
		fileMenu.add(new JMenuItem("Print Preview"));
		menu.add(fileMenu);
		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.add(new JMenuItem("Statistics"));
		menu.add(toolsMenu);
		setJMenuBar(menu);

		for (int i = 0; i < menu.getMenuCount(); i++) {
			JMenu m = menu.getMenu(i);
			for (int j = 0; j < m.getItemCount(); j++) {
				JMenuItem t = m.getItem(j);
				t.addActionListener(this);
			}
		}

		mainPane.add(utterancePanel, BorderLayout.NORTH);
		mainPane.add(annotationPanel, BorderLayout.CENTER);
		mainPane.add(buttonsPanel, BorderLayout.SOUTH);

		pack();
		setBounds(new Rectangle(INSET, INSET, WIDTH, HEIGHT));
		setSize(new Dimension(WIDTH, HEIGHT));
		setVisible(true);
		repaint();
	}

	/*
	 * public class ChildesUtteranceRenderer extends DefaultListCellRenderer { // not in use: can use this to modify the
	 * rendering of each JList item public Component getListCellRendererComponent(JList list, Object value, int index,
	 * boolean isSelected, boolean hasFocus) {
	 * 
	 * JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
	 * 
	 * return (label); } }
	 */

	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		// System.out.println(cmd + "\n");

		if (cmd.equalsIgnoreCase("Save & next")) {
			saveCurrentAnnotation();
			displayNextUtterance();
			vernField.setText("");
		}
		else if (cmd.equalsIgnoreCase("prev")) {
			displayPrevUtterance();
		}
		else if (cmd.equalsIgnoreCase("next")) {
			displayNextUtterance();
		}
		else if (cmd.equalsIgnoreCase("Export to XML")) {
			exportXML(transcript);
		}
		else if (cmd.equalsIgnoreCase("Open...")) {
			File file = selectFile();
			if (file != null) {
				setWorkingDir(file.getAbsolutePath());
				openTranscript(createTranscript(file));
			}
		}
		else if (cmd.equalsIgnoreCase("Preprocess...")) {
			File file = selectFile();
			ChildesTranscript t = createTranscript(file);
			if (t != null) {
				try {
					t.preprocessTranscript();
				}
				catch (AnnotationException ae) {
					alertMessage(ae.getMessage());
				}
				exportXML(t);
			}
		}
		else if (cmd.equalsIgnoreCase("Print Preview")) {
			newWindowMessage("Print Preview", transcript.toString());
		}
		else if (cmd.equalsIgnoreCase("Statistics")) {
			statistics = computeStatistics();
			if (statistics != null) {
				newWindowMessage("Corpus Statistics", statistics.toString());
			}
		}
		else {
			alertMessage("invalid action command");
		}
	}

	protected void alertMessage(String msg) {
		JOptionPane.showMessageDialog(mainPane, msg);
	}

	protected void newWindowMessage(String title, String msg) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JEditorPane text = new JEditorPane("text/html", msg);
		JScrollPane scrollPane = new JScrollPane(text);
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.setPreferredSize(new Dimension(600, 400));
		frame.pack();
		frame.setVisible(true);
	}

	protected void saveCurrentAnnotation() {
		String vern = vernField.getText();
		ChildesAnnotation ca = null;

		/*
		 * try { if (currentClause.hasAnnotation(UtteranceAnnotation.CHILDES)) { ca = (ChildesAnnotation)
		 * currentClause.getAnnotation(UtteranceAnnotation.CHILDES); } else { ca = new ChildesAnnotation(currentClause); }
		 * ca.setTier(AnnotationTierFactory.createTier(ca, ChildesTier.VERNACULAR, vern)); } catch (AnnotationException
		 * ae) { alertMessage(ae.getMessage()); }
		 */
	}

	protected void displayPrevUtterance() {
		if (iterator.hasPrev()) {
			currentItem = iterator.prev();
			displayItem(currentItem);
		}
	}

	protected void displayNextUtterance() {
		if (iterator.hasNext()) {
			currentItem = iterator.next();
			displayItem(currentItem);
		}
	}

	protected void displayItem(ChildesItem item) {
		if (item instanceof ChildesClause) {
			displayUtterance((ChildesClause) item);
		}
		else if (item instanceof ChildesEvent) {
			displayEvent((ChildesEvent) item);
		}
	}

	protected void displayUtterance(ChildesClause clause) {
		currentAnnotation = retrieveAnnotation(clause);
		utteranceLabel.setText(clause.toString());
		speakerLabel.setText("Speaker = " + clause.getSpeaker());
		vernField.setText(retrieveVernacular(currentAnnotation));
		repaint();
	}

	protected void displayEvent(ChildesEvent event) {

	}

	protected File selectFile() {
		// TODO: use .xml extension as default. FileFilter needed?
		final JFileChooser fc = new JFileChooser(new File(getWorkingDir()));
		fc.setMultiSelectionEnabled(false);
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	protected List<File> selectFiles() {
		final JFileChooser fc = new JFileChooser(new File(getWorkingDir()));
		fc.setMultiSelectionEnabled(true);
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return Arrays.asList(fc.getSelectedFiles());
		}
		return null;
	}

	public ChildesTranscript createTranscript(File file) {
		if (file != null) {
			try {
				return new ChildesTranscript(file);
			}
			catch (AnnotationException ae) {
				alertMessage(ae.getMessage());
			}
		}
		return null;
	}

	public void openTranscript(ChildesTranscript t) {
		if (t != null) {
			transcript = t;
			iterator = transcript.iterator();
			displayNextUtterance();
		}
	}

	protected ChildesAnnotation retrieveAnnotation(ChildesClause clause) {
		return ((ChildesAnnotation) clause.getAnnotation(UtteranceAnnotation.CHILDES));
	}

	protected String retrieveVernacular(ChildesAnnotation annotation) {
		try {
			return annotation.getVernacularTier().getContent();
		}
		catch (NullPointerException npe) {
			alertMessage("No vernacular annotation found");
		}
		return "";
	}

	protected void exportXML(ChildesTranscript t) {

		// TODO: use .xml extension as default. FileFilter needed?

		final JFileChooser fc = new JFileChooser(new File(getWorkingDir()));
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File xmlFile = fc.getSelectedFile();
			try {
				t.outputXML(xmlFile);
			}
			catch (IOException ioe) {
				alertMessage(ioe.getMessage());
			}
			setWorkingDir(xmlFile.getAbsolutePath());
		}

	}

	protected void setWorkingDir(String path) {
		workingDir = path;
	}

	protected String getWorkingDir() {
		return workingDir;
	}

	protected ChildesStatistics computeStatistics() {

		// FUTURE: cache the statistics and the files the stats are computed on
		// FUTURE: maybe run multiple Stats (one per transcript) and sum them
		// FUTURE: allow running stats on a subset of a transcript

		List<File> files = selectFiles();
		if (files == null)
			return null;

		String s = "<html>";
		for (File file : files) {
			s += file.getName() + "<br>";
		}
		s += "</html>";

		// HACK: The display of selected files should be in a confirmation window
		newWindowMessage("selected files", s);

		ChildesStatistics stat = new ChildesStatistics();

		for (File file : files) {
			ChildesTranscript transcript = createTranscript(file);
			stat.ProcessClause(transcript);
		}
		return stat;
	}

	public static void main(String argv[]) {

		ChildesBrowser browser = new ChildesBrowser();

		if (argv.length > 0) {
			if (argv[0].equalsIgnoreCase("-help")) {
				System.err.println("Usage: java ChildesBrowser [in_filename]");
				System.exit(1);
			}
			else {
				browser.openTranscript(browser.createTranscript(new File(argv[0])));
			}
		}
	}

}

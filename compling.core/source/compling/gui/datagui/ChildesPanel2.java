package compling.gui.datagui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import compling.annotation.childes.ChildesTranscript;

/**
 * Manages all display for one ChildesTranscript. TODO ask eva again: on open file, select first item for annotation or
 * what?
 * 
 * Split pane has two scroll panes. 1) Scroll pane "tableScroll" containing TranscriptTable JTable. 2) Scroll pane
 * "tabScroll" containing JTabbedPane. - tab for annotation - tab for corpus statistics when desired
 */
public class ChildesPanel2 extends JPanel implements ChildesBrowserConstants {

	private static final long serialVersionUID = -3426207054379871914L;
	private JSplitPane splitPane;
	private TranscriptTable table;
	private JTabbedPane tabPane;
	private Action previousAction, nextAction;

	public ChildesPanel2(ChildesTranscript transcript, Dimension maxDimension) {
		super(new BorderLayout());
		setBorder(BorderFactory.createLineBorder(Color.black));

		/* Set up the JTable. */
		table = new TranscriptTable(transcript);
		table.setTableHeader(null); // no header

		// Only one row can be selected at a time
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new SingleSelectionListener());

		/* Set up the right half of screen (annotation, etc). */
		tabPane = new JTabbedPane();
		nextAction = new NextAction(NEXT);
		previousAction = new PrevAction(PREV);

		/* Add everything as scroll panes to JSplitPane. */
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), new JScrollPane(tabPane));
		splitPane.setPreferredSize(maxDimension);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(0.20);

		setPreferredSize(maxDimension);
		setOpaque(true);
		add(splitPane, BorderLayout.WEST);

		/* Start with first annotation in tab. */
		if (table.getRowCount() > 0) {
			showAnnotationRow(0);
		}
	}

	/**
	 * Highlights <code>row</code> in table and presents it in annotation tab. row must be a valid table row number or
	 * -1. Deselects previously selected row(s).
	 */
	private void showAnnotationRow(int row) {
		if (row == TranscriptTable.NO_ROW_SELECTED) { // skip
			return;
		}
		if (!table.isRowSelected(row)) { // next/prev button pressed
			table.clearSelection();
			table.setRowSelectionInterval(row, row);
			Rectangle rect = table.getCellRect(row, 0, true);
			table.scrollRectToVisible(rect);
		}
		AnnotationPanel annoPanel = AnnotationPanel.newInstance(table.getClauseAt(row), previousAction, nextAction);
		int tabIndex = tabPane.indexOfTab(ITEM_TAB);
		if (tabPane.indexOfTab(ITEM_TAB) == -1) {
			tabPane.addTab(ITEM_TAB, null, annoPanel, ITEM_TAB_TOOLTIP);
		}
		else {
			finishAnnotation(tabIndex, false);
			tabPane.setComponentAt(tabIndex, annoPanel);
		}
	}

	/**
	 * TODO panel closing: updates the annotation and any other things to do before panel is closed.
	 * 
	 */
	public void finish() {
		saveAnnotation();
		// TODO save working directories info ??
	}

	public void saveAnnotation() {
		finishAnnotation(tabPane.indexOfTab(ITEM_TAB), false);
	}

	/** ... The Component at tabIndex must be an AnnotationPanel. */
	private void finishAnnotation(int tabIndex, boolean doRemoveTab) {
		if (tabIndex > -1) {
			tabPane.setSelectedIndex(tabIndex);
			AnnotationPanel annoPanel = (AnnotationPanel) tabPane.getSelectedComponent();
			annoPanel.updateAnnotation();
			if (doRemoveTab) {
				tabPane.remove(tabIndex);
			}
		}
	}

	class SingleSelectionListener implements ListSelectionListener {

		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return; // Ignore extra messages.
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();
			if (lsm.isSelectionEmpty()) {
				// no rows are selected
			}
			else {
				showAnnotationRow(table.getSelectedRow());
			}
		}
	}

	protected class NextAction extends AbstractAction {

		private static final long serialVersionUID = 6969849785701798455L;

		public NextAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
			super(text, icon);
			putValue(SHORT_DESCRIPTION, desc);
			putValue(MNEMONIC_KEY, mnemonic);
		}

		public NextAction(String text) {
			super(text);
		}

		public void actionPerformed(ActionEvent e) {
			int nextRow = table.getSelectedRow() + 1;
			if (nextRow < table.getRowCount()) {
				showAnnotationRow(nextRow);
			}
		}
	}

	protected class PrevAction extends AbstractAction {

		private static final long serialVersionUID = 8639497916293341278L;

		public PrevAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
			super(text, icon);
			putValue(SHORT_DESCRIPTION, desc);
			putValue(MNEMONIC_KEY, mnemonic);
		}

		public PrevAction(String text) {
			super(text);
		}

		public void actionPerformed(ActionEvent e) {
			int prevRow = table.getSelectedRow() - 1;
			if (prevRow >= 0) {
				showAnnotationRow(prevRow);
			}
		}
	}

	/*
	 * Example multiple select class MultiChoiceListener extends MouseAdapter { public void mouseClicked(MouseEvent e){
	 * if (e.getClickCount() == 2) { int[] rowsSelected = table.getSelectedRows(); StringBuffer nextText = new
	 * StringBuffer("DEBUG double clicked:"+"/n"); // TODO testing table selection JTextArea textTest = new JTextArea();
	 * textTest.setLineWrap(true);
	 * 
	 * for (int i : rowsSelected) { nextText.append(table.getClauseAt(i).getText(" ")+"/n"); }
	 * textTest.setText(nextText.toString()); setEditItemTab(textTest); } } }
	 */

	// Example: Double-click mouse
	/*
	 * table.addMouseListener(new SingleChoiceListener()); // Double click listener private class SingleChoiceListener
	 * extends MouseAdapter { public void mouseClicked(MouseEvent e){ if (e.getClickCount() == 2) {
	 * showAnnotationRow(table.getSelectedRow()); } } }
	 */

	// Example of doing stuff when row is selected (not necessarily clicked)
	/*
	 * In constructor: // Select row listener ListSelectionModel rowSM = table.getSelectionModel();
	 * rowSM.addListSelectionListener(new TranscriptSelectionListener());
	 * 
	 * class TranscriptSelectionListener implements ListSelectionListener { public void valueChanged(ListSelectionEvent
	 * e) { if (e.getValueIsAdjusting()) return; // Ignore extra messages. ListSelectionModel lsm = (ListSelectionModel)
	 * e.getSource(); if (lsm.isSelectionEmpty()) { // no rows are selected } else { StringBuffer nextText = new
	 * StringBuffer(); // TODO texttest // Find out which indexes are selected. int minIndex =
	 * lsm.getMinSelectionIndex(); int maxIndex = lsm.getMaxSelectionIndex(); for (int i = minIndex; i <= maxIndex; i++)
	 * { if (lsm.isSelectedIndex(i)) { // TODO clause formatter or what?
	 * nextText.append(table.getClauseAt(i).getText(SPACE_SEPARATOR)+NEWLINE); } } textTest.setText(nextText.toString());
	 * } } }
	 */

}
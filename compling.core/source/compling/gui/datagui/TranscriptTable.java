package compling.gui.datagui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import compling.annotation.childes.ChildesConstants;
import compling.annotation.childes.ChildesTranscript;
import compling.annotation.childes.ChildesTranscript.ChildesClause;

/*
 * Filters? No Filter; All Speakers, Mother, Child, All Items, Clause, Event Regex
 */
public class TranscriptTable extends JTable {

	private static final long serialVersionUID = -2705406800732508295L;

	public static final int NO_ROW_SELECTED = -1;

	private static final int NUM_COLUMNS = 2;
	private static final int COL_CLAUSE = 1;
	private static final int COL_SPEAKER = 0;

	private static final int CELL_MARGIN = 10;
	private static final String DEFAULT_FONT_FACE = "Times";
	private static final int DEFAULT_FONT_SIZE = 16;
	private static final Color DEFAULT_HIGHLIGHT_BACK = Color.YELLOW;
	private static final Color DEFAULT_HIGHLIGHT_FORE = Color.BLACK;

	public TranscriptTable(ChildesTranscript transcript) {
		super();
		// transcript.setFormatter(ChildesUtilities.newTextFormatter());
		TranscriptTableModel model = new TranscriptTableModel(transcript);
		setModel(model);
		/* Set cell renderer (by column). Set column widths. */
		HtmlPaneRenderer renderer = new HtmlPaneRenderer(CELL_MARGIN, DEFAULT_HIGHLIGHT_FORE, DEFAULT_HIGHLIGHT_BACK);
		Font defaultFont = renderer.getFont();
		defaultFont = new Font(DEFAULT_FONT_FACE, defaultFont.getStyle(), DEFAULT_FONT_SIZE);
		setFont(defaultFont);
		TableColumnModel colModel = getColumnModel();
		for (int c = 0; c < NUM_COLUMNS; c++) {
			TableColumn tc = colModel.getColumn(c);
			// columns.nextElement().setCellRenderer(renderer);
			tc.setCellRenderer(renderer);
			if (c == COL_SPEAKER) {
				// TODO display column header?
				String maxText = ChildesConstants.SPEAKER_MAX_PATTERN;
				JLabel defaultLabel = new JLabel(maxText);
				defaultLabel.setFont(defaultFont);
				FontMetrics fontMetrics = defaultLabel.getFontMetrics(defaultLabel.getFont());
				int maxWidth = SwingUtilities.computeStringWidth(fontMetrics, maxText) + (4 * colModel.getColumnMargin());
				tc.setMaxWidth(maxWidth);
			}
		}
		/* Remove lines between columns. */
		Dimension cellSpace = getIntercellSpacing();
		setIntercellSpacing(new Dimension((int) cellSpace.getWidth() - 2, (int) cellSpace.getHeight()));
		doLayout();
	}

	public ChildesClause getSelectedClause() {
		return getClauseAt(getSelectedRow());
	}

	public ChildesClause getClauseAt(int row) {
		return (ChildesClause) getValueAt(row, COL_CLAUSE);
	}

	/**
	 * 
	 * TODO there is only 1 column. maybe want 2? ID number and text
	 * 
	 */
	public class TranscriptTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 401175136631494425L;

		// ChildesTranscript transcript = null;
		List<ChildesClause> allClauses = new ArrayList<ChildesClause>(), currentClauses = new ArrayList<ChildesClause>();

		public TranscriptTableModel(ChildesTranscript transcript) {
			// this.transcript = transcript;
			allClauses = transcript.getAllClauses();
			currentClauses = allClauses;
		}

		public int getRowCount() {
			return currentClauses.size();
		}

		public int getColumnCount() {
			return NUM_COLUMNS;
		}

		public Object getValueAt(int row, int column) {
			if (currentClauses.isEmpty()) {
				return null; // dunno if guaranteed to receive valid row, column args
			}
			ChildesClause clause = currentClauses.get(row);
			if (column == COL_SPEAKER) {
				return clause.getSpeaker();
			}
			else if (column == COL_CLAUSE) {
				// TODO use a formatter?
				return clause;
			}
			return null;
		}
	}

}

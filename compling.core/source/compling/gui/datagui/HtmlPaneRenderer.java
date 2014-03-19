package compling.gui.datagui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * TODO testing cell text wrap. based on: http://www.javaspecialists.co.za/archive/Issue106.html * added margins
 * capability
 * 
 */
public class HtmlPaneRenderer extends JEditorPane implements TableCellRenderer {

	private static final long serialVersionUID = -4139585912170954986L;

	private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
	/** map from table to map of rows to map of column heights */
	private final HashMap<JTable, HashMap<Integer, HashMap<Integer, Integer>>> cellSizes = new HashMap<JTable, HashMap<Integer, HashMap<Integer, Integer>>>();
	private Color selectedFore, selectedBack;

	public HtmlPaneRenderer() {
		setEditable(false);
		setContentType("text/html");
	}

	/** fore and back can be null (use default colours). */
	public HtmlPaneRenderer(int margin, Color fore, Color back) {
		this();
		if (fore == null) {
			setForeground(adaptee.getForeground());
		}
		if (back == null) {
			setBackground(adaptee.getBackground());
		}
		selectedFore = fore;
		selectedBack = back;
		setMargin(new Insets(margin, margin, margin, margin));
	}

	public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// set the colours, etc. using the standard for that platform
		adaptee.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
		if (isSelected) {
			setForeground(selectedFore);
			setBackground(selectedBack);
		}
		else {
			setForeground(adaptee.getForeground());
			setBackground(adaptee.getBackground());
		}
		setBorder(adaptee.getBorder());
		setFont(adaptee.getFont());
		setText(adaptee.getText()); // christine: this calls ChildesFormater somehow

		// This line was very important to get it working with JDK1.4
		TableColumnModel columnModel = table.getColumnModel();
		setSize(columnModel.getColumn(column).getWidth(), 100000);
		int height_wanted = (int) getPreferredSize().getHeight();
		addSize(table, row, column, height_wanted);
		height_wanted = findTotalMaximumRowSize(table, row);
		if (height_wanted != table.getRowHeight(row)) {
			table.setRowHeight(row, height_wanted);
		}
		return this;
	}

	private void addSize(JTable table, int row, int column, int height) {
		HashMap<Integer, HashMap<Integer, Integer>> rows = cellSizes.get(table);
		if (rows == null) {
			cellSizes.put(table, rows = new HashMap<Integer, HashMap<Integer, Integer>>());
		}
		HashMap<Integer, Integer> rowheights = rows.get(row);
		if (rowheights == null) {
			rows.put(row, rowheights = new HashMap<Integer, Integer>());
		}
		rowheights.put(column, height);
	}

	/**
	 * Look through all columns and get the renderer. If it is also a TextAreaRenderer, we look at the maximum height in
	 * its hash table for this row.
	 */
	private int findTotalMaximumRowSize(JTable table, int row) {
		int maximum_height = 0;
		Enumeration columns = table.getColumnModel().getColumns();
		while (columns.hasMoreElements()) {
			TableColumn tc = (TableColumn) columns.nextElement();
			TableCellRenderer cellRenderer = tc.getCellRenderer();
			if (cellRenderer instanceof HtmlPaneRenderer) {
				HtmlPaneRenderer tar = (HtmlPaneRenderer) cellRenderer;
				maximum_height = Math.max(maximum_height, tar.findMaximumRowSize(table, row));
			}
		}
		return maximum_height;
	}

	private int findMaximumRowSize(JTable table, int row) {
		Map rows = (Map) cellSizes.get(table);
		if (rows == null)
			return 0;
		Map rowheights = (Map) rows.get(new Integer(row));
		if (rowheights == null)
			return 0;
		int maximum_height = 0;
		for (Iterator it = rowheights.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			int cellHeight = ((Integer) entry.getValue()).intValue();
			maximum_height = Math.max(maximum_height, cellHeight);
		}
		return maximum_height;
	}
}
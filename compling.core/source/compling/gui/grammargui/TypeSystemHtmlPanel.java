package compling.gui.grammargui;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Widget;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.GUIConstants;

/**
 * Displays Schemas or Constructions. 
 * @author   lucag
 */
public class TypeSystemHtmlPanel extends Composite implements GUIConstants {

	/** Used to represent a link to the corresponding specification */
	protected Link link;
	protected IGrammarBrowserController controller;
	protected TypeSystemNode node;
	protected Widget widget;
	
	/**
	 * Font shared by all views.
	 */
	protected static Font font;

//	private static final String TEST_HTML = "<html><header><title>Test</title><body><a href=\"/a/b/c/d\">Test</a></body></html>";

	/**
	 * This class defines the content of each tab item. It now contains an HTML
	 * browser widget that shows the content of a Construction or Schema.
	 *
	 * @param node -
	 * 			The node represented in this panel
	 * @param widget
	 * 			The SWT Widget that's associated with this panel
	 * @param controller -
	 * 			The controller of this panel. 
	 * @param parent -
	 * 			The parent Composite of this panel
	 * @param style - 
	 * 			The style of this panel.
	 * 
	 * @see TypeSystemHtmlPanel#notifyOpen 
	 * @see	TypeSystemHtmlPanel#notifyClose
	 * @see TypeSystemHtmlPanel#notifyNodeSelection
	 */
	public TypeSystemHtmlPanel(TypeSystemNode node, Widget widget,
			IGrammarBrowserController controller, Composite parent, int style) {
		super(parent, style);
		this.controller = controller;
		this.node = node;
		this.widget = widget;

		
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL);
		fillLayout.marginWidth = 3;
		fillLayout.marginHeight = 2;
		setLayout(fillLayout);

//		browser = new Browser(this, SWT.NONE);
//		browser.setText(htmlopen + node.toString() + htmlclose);
		link = new Link(this, SWT.NONE);
		link.setText(node.toString());
		if (font == null)
			font = new Font(getDisplay(), link.getFont().getFontData());
		link.setFont(font);
		
		// browser.setText(TEST_HTML);

		// Register some listeners

		//
		// This is for receiving updates from tabs. It catches the close event
		// that's generated when the user clicks on the close tab icon.
		//
		((CTabFolder) parent).addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				notifyClose();
			}
		});

		// Receives notification when disposed of programatically
		//
		widget.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				notifyClose();
			}
		});

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
//				System.out.printf("event: %s\n", event);
				notifyNodeSelection(event.text);
			}
		});

		// Set the background color to white
		setBackgroundColor();
		
		//
		// Notify the controller this component has been opened.
		//
		notifyOpen();
	}

	protected void setBackgroundColor() {
		link.setBackground(new Color(link.getDisplay(), 0xff, 0xff, 0xff));
	}
	

	/**
	 * Set the link object's font field.
	 * @param font
	 */
	public void setFont(Font font) {
		TypeSystemHtmlPanel.font = font;
		link.setFont(font);
	}
	
	/**
	 * @return The link object's font.
	 */
	@Override
	public Font getFont() {
		return link.getFont();
	}
	
	/**
	 * Used to separate the elements in the anchors' href attributes.
	 * 
	 * @see compling.gui.grammargui.GrammarBrowserTextPrinter
	 */
	protected static final Pattern splitter = Pattern.compile("[:/]");

	/**
	 * Notify the controller that a selection has been made, i.e., an HTML link
	 * has been clicked on. It expects the selection to be a string containing
	 * a TypeSytem descriptor and a descriptor name separated by a slash, e.g.
	 * "SCHEMA/AgreementFeatures".
	 * 
	 * @param selection -
	 *          The actual link selected
	 *          
	 * @see compling.gui.grammargui.GrammarBrowserTextPrinter
	 */
	protected void notifyNodeSelection(String selection) {
		assert selection.indexOf('@') == -1 : "selection: " + selection;

//		System.out.printf("selection: %s\n", selection);
		String[] elements = splitter.split(selection);

		assert elements.length > 1;
		controller.notifyActivationChanged(elements[0], elements[1]);
	}

	/**
	 * Notify the controller that this component has been opened.
	 */
	protected void notifyOpen() {
		controller.registerView(Util.toString(node), widget);
	}

	/**
	 * Notify the controller that this component has been closed.
	 */
	protected void notifyClose() {
		controller.unregisterView(Util.toString(node), widget);
	}

	/** 
	 * Free operating system resources. Crucial with SWT, since every Widget
	 * locks an operating system resource during its entire lifetime.
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
//		browser.dispose();
//		browser = null;
		link.dispose();
		link = null;
		
    super.dispose();
	}
}

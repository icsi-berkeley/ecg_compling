package compling.gui.grammargui.ui.views;


import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;

public class TokenView extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.tokenView";

	private TableViewer viewer;

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		ScrolledForm form = toolkit.createScrolledForm(parent);
		form.setText("Token Editor");
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		/*
		Hyperlink link = toolkit.createHyperlink(form.getBody(), 
				"Click here.", SWT.WRAP);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				System.out.println("Link activated!");
			}
		});
		*/
		
		
		layout.numColumns = 2;
		GridData gd = new GridData();
		gd.horizontalSpan = 2;

		Label tokenlabel = toolkit.createLabel(form.getBody(), "Enter Token:");
		Text tokenText = toolkit.createText(form.getBody(), "");
		tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd = new GridData();
		gd.horizontalSpan = 2;
		
		
		Label parentLabel = toolkit.createLabel(form.getBody(), "Enter Parent:");
		Text parentText = toolkit.createText(form.getBody(), "");
		parentText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button button = toolkit.createButton(form.getBody(), "I am a button, push me.", SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() { 
			public void widgetSelected(SelectionEvent e) {
				System.out.println("Test");
			}
		});
		button.setLayoutData(gd);
		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
	}
}

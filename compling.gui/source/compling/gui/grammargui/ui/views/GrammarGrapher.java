package compling.gui.grammargui.ui.views;


import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.gui.grammargui.model.PrefsManager;

public class GrammarGrapher extends ViewPart {
	public static final String ID = "compling.gui.grammargui.views.grammarGrapher";

	@Override
	public void createPartControl(Composite parent) {
		


		Canvas canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.setBackground(ColorConstants.white);
		final LightweightSystem lws = new LightweightSystem(canvas);
		final Figure contents = new Figure();
		final XYLayout contentsLayout = new XYLayout();
		contents.setLayoutManager(contentsLayout);
		
		//TODO: Add text input, so you can say which construction to generate info about
		//final TextFlow text = new TextFlow("ActiveTransitive");
		//contents.add(text);
		//contentsLayout.setConstraint(text, new Rectangle(10, 5, -1, -1));
		
		Button b = new Button("Go");
		//b.
		contents.add(b);
		contentsLayout.setConstraint(b, new Rectangle(5, 5, -1, -1));
		//b.add
		//b.addMouse
		b.addMouseListener(new MouseListener() {

			@Override
			public void mousePressed(MouseEvent me) {
				System.out.println("this");
				
			}

			@Override
			public void mouseReleased(MouseEvent me) {
				System.out.println("or this");
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseDoubleClicked(MouseEvent me) {
				//String cxnName = text.getText();
				String cxnName = "ActiveTransitive";
				Figure newContents = createContents(cxnName, contentsLayout, contents);
				lws.setContents(newContents);
				// TODO Auto-generated method stub
				
			}
			
		}
		);


		//final UMLClassFigure classFigure = createGrammarCxnFigure("ArgumentStructure");
		//final UMLClassFigure classFigure2 = createGrammarCxnFigure("ActiveTransitive");
		

		//contentsLayout.setConstraint(classFigure, new Rectangle(10,40,-1,-1));
		//contentsLayout.setConstraint(classFigure2, new Rectangle(200, 200, -1, -1));
		
//		/* Creating the connection */
//		PolylineConnection c = new PolylineConnection();
//		ChopboxAnchor sourceAnchor = new ChopboxAnchor(classFigure);
//		ChopboxAnchor targetAnchor = new ChopboxAnchor(classFigure2);
//		c.setSourceAnchor(sourceAnchor);
//		c.setTargetAnchor(targetAnchor);
//
//		ConnectionEndpointLocator relationshipLocator = 
//			new ConnectionEndpointLocator(c,true);
//		relationshipLocator.setUDistance(10);
//		relationshipLocator.setVDistance(-20);
//		Label relationshipLabel = new Label("subcase of ...");
//		c.add(relationshipLabel,relationshipLocator);

		//contents.add(classFigure);
		
		//contents.add(classFigure2);
		
		
		//contents.add(c);
		

		
		lws.setContents(contents);
		
	}
	
	private Figure createContents(String cxnName, XYLayout contentsLayout, Figure contents) {
		UMLClassFigure classFigure = createGrammarCxnFigure(cxnName);
		contentsLayout.setConstraint(classFigure, new Rectangle(10,40,-1,-1));
		contents.add(classFigure);
		
		//UMLClassFigure 
		
		return contents;
		
	}
	
	private UMLClassFigure createGrammarCxnFigure(String cxnName) {
		Grammar g = getGrammar();
		Construction c = getGrammar().getConstruction(cxnName);
		UMLClassFigure uml = new UMLClassFigure(new Label(cxnName));
		
		uml.getMethodsCompartment().add(new Label("Meaning: " + c.getMeaningBlock().getType()));
		
		for (Constraint constraint : c.getMeaningBlock().getConstraints()) {
			String toAdd = constraint.toString();
			if (!constraint.getSource().equals(cxnName)) {
				toAdd += " // inherited from " + constraint.getSource();
			}
			uml.getAttributesCompartment().add(new Label(toAdd));
		}
		//uml.getAttributesCompartment().add
		//c.
		for (Role r : c.getMeaningBlock().getEvokedElements()) {
			uml.getMethodsCompartment().add(new Label("evokes " + r.getTypeConstraint().getType() + " as " + r.getName()));
		}
		
		return uml;
	}
	
	/** Returns grammar. */
	protected Grammar getGrammar() {
		Grammar grammar = PrefsManager.getDefault().getGrammar();
		return grammar;
	}
	

	public class CompartmentFigure extends Figure {
	
	  public CompartmentFigure() {
	    ToolbarLayout layout = new ToolbarLayout();
	    layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
	    layout.setStretchMinorAxis(false);
	    layout.setSpacing(2);
	    setLayoutManager(layout);
	    setBorder(new CompartmentFigureBorder());
	  }
	    
	  public class CompartmentFigureBorder extends AbstractBorder {
	    public Insets getInsets(IFigure figure) {
	      return new Insets(1,0,0,0);
	    }
	    public void paint(IFigure figure, Graphics graphics, Insets insets) {
	      graphics.drawLine(getPaintRectangle(figure, insets).getTopLeft(),
	                        tempRect.getTopRight());
	    }
	  }
	}
	

	public class UMLClassFigure extends Figure {
		  public  Color classColor = new Color(null,255,255,206);
		  private CompartmentFigure attributeFigure = new CompartmentFigure();
		  private CompartmentFigure methodFigure = new CompartmentFigure();
		  public UMLClassFigure(Label name) {
		    ToolbarLayout layout = new ToolbarLayout();
		    setLayoutManager(layout);	
		    setBorder(new LineBorder(ColorConstants.black,1));
		    setBackgroundColor(classColor);
		    setOpaque(true);
			
		    add(name);	
		    add(attributeFigure);
		    add(methodFigure);
		  }
		  public CompartmentFigure getAttributesCompartment() {
		    return attributeFigure;
		  }
		  public CompartmentFigure getMethodsCompartment() {
		    return methodFigure;
		  }
	}
	

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}

package compling.gui.grammargui.ui.editors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.gui.grammargui.model.IAnalyzerEditorInput;
import compling.gui.grammargui.model.TypeSystemLabelProvider;
import compling.gui.grammargui.util.OutlineElementType;
import compling.gui.util.IParse;
import compling.parser.ecgparser.Analysis;

public class AnalysisOutline extends ContentOutlinePage {

	protected IAnalyzerEditorInput input;

	public class ElementLabelProvider extends TypeSystemLabelProvider {
		@Override public Image getImage(Object element) {
			return registry.get(((Element) element).getType().getPath()); }

		@Override public String getText(Object element) {
			return ((Element) element).getLabel(); }
	}

	public AnalysisOutline(IEditorInput input) {
		this.input = (IAnalyzerEditorInput) input;
	}

	public AnalysisOutline() {
		this(null);
	}
	
	public void setInput(IAnalyzerEditorInput input) {
		this.input = input;
		
		TreeViewer viewer = getTreeViewer();
		if (viewer != null) {
			viewer.setInput(input);
			viewer.refresh();
		}
		else 
			System.out.println("AnalysisOutline: viewer is null!");
	}

	public abstract class Element {
		Element parent;
		Element[] children;
		String label = "";

		public Element(Element parent) {
			this.parent = parent;
		}

		public Element[] getChildren() {
			return children;
		}

		public boolean hasChildren() {
			return children != null && children.length > 0;
		}

		public Element getParent() {
			return parent;
		}

		public String getLabel() {
			return label;
		}

		abstract public OutlineElementType getType();

	}

	private class SlotElement extends Element {

		protected OutlineElementType type;

		private SlotElement(Element parent) {
			super(parent);
		}

		private SlotElement(String roleName, Slot slot, Element parent, Set<Slot> done) {
			super(parent);

			this.type = OutlineElementType.valueOf(slot);
			this.label = String.format("%s: %s", roleName, slot.hasAtomicFiller() ? slot.getAtom() : slot.toString());

			if (! done.contains(slot)) {
			  done.add(slot);
        extract(slot.getFeatures(), done);
      }
		}

		protected void extract(final Map<Role, Slot> features, Set<Slot> done) {
			if (features != null) {
				int i = 0;
				this.children = new Element[features.size()];
				for (Entry<Role, Slot> e : features.entrySet())
					this.children[i++] = new SlotElement(e.getKey().getName(), e.getValue(), this, done);
			}
		}

		@Override
		public OutlineElementType getType() {
			return type;
		}

	}

	public class AnalysisElement extends SlotElement {

		public AnalysisElement(Analysis analysis, Element parent) {
			super(parent);

			Construction headCxn = analysis.getHeadCxn();
			this.label = String.format("Analysis: %s", headCxn.getName()); // More?
			this.type = OutlineElementType.valueOf(headCxn.getCxnTypeSystem());

			extract(analysis.getFeatureStructure().getMainRoot().getFeatures(), new HashSet<Slot>());
		}

	}

	public class ParseElement extends Element {

		public ParseElement(IParse parse, Element parent) {
			super(parent);

			this.label = String.format("Parse: cost %f", parse.getCost());

			int i = 0;
			Collection<Analysis> analyses = parse.getAnalyses();
			this.children = new Element[analyses.size()];
			for (Analysis a : analyses)
				this.children[i++] = new AnalysisElement(a, this);
		}

		@Override
		public OutlineElementType getType() {
			return OutlineElementType.PARSE;
		}

	}

	public class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// Empty
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			System.out.printf("Viewer: %s\n", viewer);
			System.out.printf("oldInput: %s\n", oldInput);
			System.out.printf("newInput: %s\n", newInput);
			
			if (newInput != null && ! newInput.equals(oldInput)) {
				// TODO:
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			System.out.printf("ContentProvider.inputElement: %s\n", inputElement);
			
			Collection<IParse> parses = ((IAnalyzerEditorInput) input).getParses();

			int i = 0;
			Object[] elements = new Element[parses.size()];
			for (IParse p : parses)
				elements[i++] = new ParseElement(p, null);

			return elements;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((Element) parentElement).getChildren();
		}

		@Override
		public Object getParent(Object element) {
			return ((Element) element).getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((Element) element).hasChildren();
		}

	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new ElementLabelProvider());

		if (input != null) {
			viewer.setInput(input);
			viewer.refresh();
		}
	}

}

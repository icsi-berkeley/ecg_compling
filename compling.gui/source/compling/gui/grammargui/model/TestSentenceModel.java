package compling.gui.grammargui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import compling.gui.grammargui.util.IComposite;
import compling.gui.grammargui.util.IElement;
import compling.gui.grammargui.util.Log;
import compling.util.CollectionUtils;


public class TestSentenceModel implements IResourceChangeListener {

	private static final String PARENT = "parent";

	protected Ini ini;
	
	protected IFile input;
	
	private Group rootElement = new Group(null, "none"); 

	public TestSentenceModel(IFile input) {
		this.input = input;
		
	   ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	   
	   try {
			load(input);
		}
		catch (InvalidFileFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.logError(e);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.logError(e);
		}
		catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.logError(e);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta[] changed = event.getDelta().getAffectedChildren(IResourceDelta.CHANGED);
		for (IResourceDelta d : changed) {
			IResource resource = d.getResource();
			if (resource.equals(input))
				try {
					load((IFile) resource);
					// TODO: Notify!
				}
				catch (InvalidFileFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	void load(IFile file) throws InvalidFileFormatException, IOException, CoreException {
		ini = new Ini(file.getContents());
		build();
	}
	
	void store() throws IOException {
		ini.store();
	}
	
	protected void build() {
		// XXX: do I need the meta section? Perhaps not?
//		Ini.Section meta = ini.get("meta");
//		int groupCount = meta.get("group.count", int.class, 0);		
//		int sentenceCount = meta.get("sentence.count", int.class, 0);	
		
		Map<String, AbstractElement> indexByName = new HashMap<String, AbstractElement>();
		Map<String, List<AbstractElement>> indexByParent = new HashMap<String, List<AbstractElement>>();
		
		indexByName.put("none", rootElement);
		
		for (Entry<String, Section> e : ini.entrySet()) {
			Section section = e.getValue();
			String id = section.getName();
			if (section.containsKey("text")) {
				indexByName.put(id, new Sentence(null, section.fetch("text"), 
						section.fetch("shouldParse", boolean.class, true)));
			}
			else {
				indexByName.put(id, new Group(null, section.fetch("name")));
			}
		}
		
		for (Entry<String, Section> e : ini.entrySet()) {
			Section section = e.getValue();
			for (String parent : section.getAll(PARENT, String[].class))
				CollectionUtils.addToValueList(indexByParent, parent, indexByName.get(e.getKey()));
		}
		
		// Now build children
		for(Entry<String, AbstractElement> entry : indexByName.entrySet()) {
			if (entry.getValue() instanceof Group) {
				Group group = (Group) entry.getValue();
				group.children = indexByParent.get(entry.getKey());
				
				if (group.children == null)
					group.children = new ArrayList<AbstractElement>();
			}
		}
		
		System.out.println(rootElement);
	}
	
	public Group getRootGroup() {
		return rootElement; 
	}
	
	public static abstract class AbstractElement implements IElement<String> {

		AbstractElement parent;
		
		public AbstractElement(AbstractElement parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return String.format("%s[%s]", getClass().getSimpleName(), content());
		}

		public boolean shouldParse() {
			if (parent != null)
				return parent.shouldParse();
			else
				return true;
		}

		@Override
		public AbstractElement parent() {
			return parent;
		}

	}

	public static class Sentence extends AbstractElement {

		String text;
		boolean shouldParse;

		public Sentence(AbstractElement parent, String text, boolean shouldParse) {
			super(parent);
			
			this.text = text;
			this.shouldParse = shouldParse;
		}

		@Override
		public String content() {
			return text;
		}

		@Override
		public boolean shouldParse() {
			return shouldParse;
		}


	}
	
	public static class Group extends AbstractElement implements IComposite<String> {

		String name;
		List<AbstractElement> children;
		
		public Group(AbstractElement parent, String name, List<AbstractElement> children) {
			super(parent);
				
			this.name = name;
			this.children = children;
		}

		public Group(AbstractElement parent, String name) {
			this(parent, name, null);
		}
		
		@Override
		public List<AbstractElement> children() {
			return children;
		}

		@Override
		public String content() {
			return name;
		}
		
	}

//	protected static IElement<String> build(Object item) {
//		if (item instanceof Element) {
//			return processElement((Element) item);
//		}
//		else if (item instanceof Text) {
//			return processText((Text) item);
//		}
//		else {
//			throw new RuntimeException(String.format("Eek! Don't know what to do with this: %s", item));
//		}
//	}

//	public static void main(String[] args) {
//		new TestSentence(new File(args[0]));
//	}
	
//	protected static IElement<String> processElement(Element element) {
//		if (element.getName().equals("sentence")) {
//			boolean shoulParse = Boolean.parseBoolean(element.getAttributeValue("shouldParse"));
//			int id = Integer.parseInt(element.getAttributeValue("id"));
//			return new Sentence(element.getText(), id, shoulParse);
//		}
//		else if (element.getName().equals("group")) {
//			return new Group(element);
//		}
//		else
//			return null;
//	}
}

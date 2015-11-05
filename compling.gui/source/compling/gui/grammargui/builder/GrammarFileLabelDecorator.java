/**
 * 
 */
package compling.gui.grammargui.builder;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import compling.gui.grammargui.Application;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.Constants.IImageKeys;

/**
 * @author lucag
 * 
 */
public class GrammarFileLabelDecorator extends BaseLabelProvider implements ILightweightLabelDecorator {

	private ImageDescriptor errorOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID,
			IImageKeys.PROBLEM_OVERLAY);

	private List<IResource> resources;

	private IResourceVisitor visitor = new IResourceVisitor() {
		public boolean visit(IResource resource) throws CoreException {
			resources.add(resource);
			return true;
		}
	};

	public static final String ID = "compling.gui.grammargui.decorators.problem";

	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			try {
				IMarker[] markers = resource.findMarkers(GrammarBuilder.MARKER_TYPE, false, IResource.DEPTH_ZERO);
				if (markers.length > 0) {
					decoration.addOverlay(errorOverlay, IDecoration.TOP_RIGHT);
					//propagateError(resource);
					//decoration.addOverlay(errorOverlay, IDecoration.UNDERLAY);
					//IContainer c = resource.getParent();
					//c.createMarker("compling.gui.grammarProblem");
				}
			}
			catch (CoreException e) { /* do nothing */
			}
		}
	}
	
	public void propagateError(IResource resource) {
		IContainer c = resource.getParent();
		try {
			if (c.exists()) {
	 			c.createMarker("compling.gui.grammarProblem");
				propagateError(c);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void update() {
		try {
			resources = new LinkedList<IResource>();
			PrefsManager.getDefault().getProject().getWorkspace().getRoot().accept(visitor);
			fireLabelProviderChanged(new LabelProviderChangedEvent(this, resources.toArray()));
		}
		catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resources = null;
	}
}

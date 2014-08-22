package compling.gui.grammargui.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.util.Log;

public class TypeSystemEditorInput extends PlatformObject implements IFileEditorInput, IPathEditorInput,
		ITypeSystemElement {

	private TypeSystemNode typeSystemNode;
	private IFile file;

	public TypeSystemEditorInput(TypeSystemNode typeSystemNode) {
		this.typeSystemNode = typeSystemNode;
		this.file = PrefsManager.getDefault().getFileFor(typeSystemNode);
		Assert.isNotNull(file);
	}

	public String getName() {
//		return typeSystemNode.getType();
		return file.getName();
	}

	public TypeSystemNode getTypeSystemNode() {
		return typeSystemNode;
	}

	@Override
	public boolean equals(Object object) {
		try {
			final TypeSystemEditorInput other = (TypeSystemEditorInput) object;
			return this.file.equals(other.file);
		}
		catch (ClassCastException x) {
			Log.logError(x);
			return false;
		}
	}

	public boolean exists() {
		return file.exists();
	}

	public ImageDescriptor getImageDescriptor() {
		IContentType contentType = IDE.getContentType(file);
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName(), contentType);
	}

	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getToolTipText() {
		return PrefsManager.getDefault().getContentAsText(typeSystemNode);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		if (IResource.class.equals(adapter))
			return file;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public IFile getFile() {
		return file;
	}

	public IStorage getStorage() throws CoreException {
		return file;
	}

	public IPath getPath() {
		IPath location = file.getLocation();
		if (location != null)
			return location;

		return null;
	}

}

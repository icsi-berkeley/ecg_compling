/**
 * 
 */
package compling.gui.grammargui.util;

import org.eclipse.core.resources.IFile;

import compling.grammar.ecg.ecgreader.IErrorListener;

/**
 * A generic interface to the {ECG|MiniOntology}Reader objects
 * 
 * @author lucag
 * 
 */
public interface ISpecificationReader {
	public Object read(IFile from, IErrorListener listener) throws Exception;
}

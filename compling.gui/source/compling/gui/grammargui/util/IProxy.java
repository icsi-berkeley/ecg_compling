/**
 * 
 */
package compling.gui.grammargui.util;

/**
 * A generic proxy interface. Used to lazily instantiate Analyzers.
 * 
 * @author lucag
 * 
 */
public interface IProxy {
	public Object get();
}

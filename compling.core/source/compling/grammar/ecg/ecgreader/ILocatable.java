package compling.grammar.ecg.ecgreader;

/**
 * A not-so-great name for things that are defined at some location in a file
 * 
 * @author lucag
 */
public interface ILocatable {

	/**
	 * It returns the file this <code>Primitive</code> was defined in.
	 * 
	 * @return the file name containing this <code>Primitive</code>'s definition.
	 */
	public abstract Location getLocation();

	/**
	 * @param definition
	 *           the location to set
	 */
	public abstract void setLocation(Location location);

}
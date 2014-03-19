package compling.gui.util;

import java.util.Collection;

import compling.parser.ecgparser.Analysis;

/**
 * A Simple representation of a parse
 * 
 * @author lucag
 */
public interface IParse {
	public double getCost();

	public Collection<Analysis> getAnalyses();
}

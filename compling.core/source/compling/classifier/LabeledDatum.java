
package compling.classifier;

/**
 * LabeledDatums add a label to the basic Datum interface.
 * 
 * @author Dan Klein
 */
public interface LabeledDatum extends Datum {

   String getLabel();
}

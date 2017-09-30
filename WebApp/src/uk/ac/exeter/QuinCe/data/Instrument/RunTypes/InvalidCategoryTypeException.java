package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * Exception for invalid Run Type Category types
 * @author Steve Jones
 *
 */
public class InvalidCategoryTypeException extends RunTypeCategoryException {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 1825197183543928356L;

	/**
	 * Simple constructor
	 * @param type The category type
	 */
	public InvalidCategoryTypeException(int type) {
		super("Invalid run type category type " + type);
	}
}

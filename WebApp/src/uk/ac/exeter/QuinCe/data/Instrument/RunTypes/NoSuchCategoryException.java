package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * Exception thrown if a specified run type category cannot be found
 * @author Steve Jones
 *
 */
public class NoSuchCategoryException extends Exception {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -1941769419137273289L;

	/**
	 * Basic constructor
	 * @param code The run type code that could not be found
	 */
	public NoSuchCategoryException(String code) {
		super("There is no Run Type Category with code " + code);
	}
}

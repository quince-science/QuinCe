package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception for invalid position formats
 * @author Steve Jones
 *
 */
public class InvalidPositionFormatException extends PositionException {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -2007521439422135974L;

	/**
	 * Basic constructor
	 * @param format The invalid format
	 */
	public InvalidPositionFormatException(int format) {
		super("The position format '" + format + "' is invalid");
	}

}

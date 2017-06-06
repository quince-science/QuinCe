package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * A basic exception for issues encountered while processing Instrument objects
 * @author Steve Jones
 * @see Instrument
 */
public class InstrumentException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 183475798941100074L;

	/**
	 * Simple constructor
	 * @param message The error message
	 */
	public InstrumentException(String message) {
		super(message);
	}

}

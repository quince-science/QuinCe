package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception class for unrecognised hemisphere values in positions
 * @author Steve Jones
 *
 */
public class InvalidHemisphereException extends PositionException {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -8277038090960751438L;

	/**
	 * Simple constructor
	 * @param hemisphere The invalid hemisphere value
	 */
	public InvalidHemisphereException(String hemisphere) {
		super("The hemisphere value '" + hemisphere + "' is invalid");
	}

}

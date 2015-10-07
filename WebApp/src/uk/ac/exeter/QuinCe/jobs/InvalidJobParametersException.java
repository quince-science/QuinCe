package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate that the parameters passed to
 * a specific job did not meet that job's requirements.
 * 
 * @author Steve Jones
 *
 */
public class InvalidJobParametersException extends Exception {

	private static final long serialVersionUID = 1495749648163507805L;

	public InvalidJobParametersException(String message) {
		super(message);
	}
	
}

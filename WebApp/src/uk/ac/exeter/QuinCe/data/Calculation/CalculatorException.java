package uk.ac.exeter.QuinCe.data.Calculation;

/**
 * Exception for errors encountered during data calculation
 * @author Steve Jones
 *
 */
public class CalculatorException extends Exception{

	/**
	 * Constructor for a simple error message
	 * @param message The error message
	 */
	public CalculatorException(String message) {
		super(message);
	}
	
	/**
	 * Constructor for wrapping a different exception 
	 * @param cause The underlying exception
	 */
	public CalculatorException(Throwable cause) {
		super("Error during calculation", cause);
	}
}

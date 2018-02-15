package uk.ac.exeter.QuinCe.data.Instrument;

import java.io.Serializable;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunType;

/**
 * Exception for missing run types in files.
 * @author Jonas F. Henriksen
 *
 */
public class MissingRunTypeException extends FileDefinitionException implements Serializable {


	/**
	 * SerialVersionUID
	 */
	private static final long serialVersionUID = 790385140808465553L;

	private RunType runType;

	public RunType getRunType() {
		return runType;
	}

	/**
	 * Simple error
	 * @param message The error message
	 */
	public MissingRunTypeException(String message, String runType) {
		super(message);
		this.runType = new RunType(runType);
	}
}

package uk.ac.exeter.QuinCe.jobs;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.MissingData;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

/**
 * Abstract class containing all the required parts for a backgorund job.
 * All jobs created for QuinCe must extend this class.
 * 
 * @author Steve Jones
 *
 */
public abstract class Job {
	
	/**
	 * Status indicating that the job has been constructed
	 * but has not yet been started
	 */
	public static final String WAITING_STATUS = "WAITING";
	
	/**
	 * Status indicating that the job is running
	 */
	public static final String RUNNING_STATUS = "RUNNING";
	
	/**
	 * Status indicating that the job has encountered an error
	 */
	public static final String ERROR_STATUS = "ERROR";
	
	/**
	 * Status indicating that the job has completed successfully
	 */
	public static final String FINISHED_STATUS = "FINISHED";
	
	/**
	 * The status of this job
	 */
	private String status = WAITING_STATUS;
	
	/**
	 * Flag to indicate whether or not the job has been destroyed.
	 */
	private boolean destroyed = false;
	
	/**
	 * The database connection to be used by the job
	 */
	protected Connection connection;
	
	/**
	 * The set of parameters passed to the job
	 */
	protected List<String> parameters;
	
	/**
	 * Constructs a job object, and validates the parameters passed to it
	 * @param connection A database connection
	 * @param parameters The parameters for the job
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 */
	public Job(Connection connection, List<String> parameters) throws MissingDataException, InvalidJobParametersException {
		
		MissingData.checkMissing(connection, "connection");
		
		this.connection = connection;
		this.parameters = parameters;
		validateParameters();
	}
	
	/**
	 * Determines whether or not this job object has been destroyed.
	 * Destroyed objects should be discarded.
	 * @return {@code true} if this Job object has been destroyed; {@code false} otherwise.
	 */
	protected boolean isDestroyed() {
		return destroyed;
	}
	
	/**
	 * Performs the job tasks
	 */
	protected abstract void run();
	
	/**
	 * Validate the parameters passed in to this job
	 * @throws InvalidJobParametersException If the parameters are invalid
	 */
	protected abstract void validateParameters() throws InvalidJobParametersException; 
	
	/**
	 * Destroys the job object, releasing the database connection.
	 */
	protected void destroy() {
		
		parameters = null;
		destroyed = true;
		try {
			connection.close();
		} catch (SQLException e) {
			// Not much we can do...
		}
	}
}

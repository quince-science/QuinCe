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
	 * The database connection to be used by the job
	 */
	private Connection connection;
	
	/**
	 * The set of parameters passed to the job
	 */
	@SuppressWarnings("unused")
	private List<String> parameters;
	
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
		try {
			connection.close();
		} catch (SQLException e) {
			// Not much we can do...
		}
	}
}
